package com.example.emotionscope;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activité affichant l'historique des analyses journalières et hebdomadaires d'un utilisateur.
 * Elle récupère les données depuis Firebase Realtime Database.
 *
 * Les données issues des différentes analyses seront ensuite affochées et gérées dans d'autres classes.
 *
 * @author: CHAKER Zakaria
 */

public class HistoriqueActivity extends AppCompatActivity {

    private String uid, prenom, nom, email;

    private FirebaseDatabase database;
    private DatabaseReference utilisateursRef;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private AlertDialog offlineDialog;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private AdaptationHistorique historiqueAdaptation;

    private  List<DetailsHistorique> historiqueJournalier = new ArrayList<>();
    private List<DetailsHistorique> historiqueHebdomadaire = new ArrayList<>();

    /**
     * Méthode appelée à la création de l'activité.
     * @param savedInstanceState instance sauvegardée (non utilisée ici)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.historique);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder().build();
        networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> showOfflineDialog());
            }

        };

        connectivityManager.registerNetworkCallback(request, networkCallback);

        Intent instance = getIntent();
        uid = instance.getStringExtra("uid");
        prenom = instance.getStringExtra("prenom");
        nom = instance.getStringExtra("nom");
        email = instance.getStringExtra("email");

        if (uid == null || prenom == null || nom == null || email == null) {
            Toast.makeText(this, "Erreur: Informations utilisateur manquantes.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        database = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
        utilisateursRef = database.getReference("utilisateurs");

        tabLayout = findViewById(R.id.tab_navigation);
        recyclerView = findViewById(R.id.recycler_view_analyses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historiqueAdaptation = new AdaptationHistorique(uid, prenom, nom, email);
        recyclerView.setAdapter(historiqueAdaptation);

        historiqueAdaptation.setModeSemaine(true);

        ImageButton btnRetour = findViewById(R.id.btn_retour);
        btnRetour.setOnClickListener(v -> finish());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    historiqueAdaptation.submitList(historiqueHebdomadaire);
                } else {
                    historiqueAdaptation.submitList(historiqueJournalier);
                }

                historiqueAdaptation.setModeSemaine(tab.getPosition() ==0);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        chargementHistorique();


        }


    /**
     * Charge les données d'historique de l'utilisateur depuis Firebase
     * et les répartit en analyses journalières et hebdomadaires.
     */

    private void chargementHistorique() {
        utilisateursRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snap) {
                    if (!snap.exists()) {return;}

                String dateCreationStr = snap.child("dateCreation").getValue(String.class);
                LocalDate startDate;
                if (dateCreationStr == null) {
                    startDate = LocalDate.now();
                }
                else {
                    try {
                        startDate = LocalDate.parse(
                                dateCreationStr,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        );
                    } catch (DateTimeParseException e) {
                        startDate = LocalDate.now();
                    }
                }

                DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                Map<String, List<LocalDateTime>> mapAnalyses = new HashMap<>();

                for (String type : Arrays.asList("analysesFaciale",
                        "analysesVocale",
                        "analysesCognitive",
                        "analysesGlobale")) {
                    List<LocalDateTime> list = new ArrayList<>();
                    for (DataSnapshot child : snap.child(type).getChildren()) {
                        String dateStr = child.child("date").getValue(String.class);
                        LocalDateTime dt = LocalDateTime.parse(dateStr,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        list.add(dt);
                    }
                    mapAnalyses.put(type, list);
                }
                
                LocalDate today = LocalDate.now();
                for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
                    DetailsHistorique item = new DetailsHistorique();
                    item.jourOuSemaine = d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    item.hasFaciale    = containsDate(mapAnalyses.get("analysesFaciale"), d);
                    item.hasVocale    = containsDate(mapAnalyses.get("analysesVocale"), d);
                    item.hasCognitive = containsDate(mapAnalyses.get("analysesCognitive"), d);
                    item.hasGlobale    = containsDate(mapAnalyses.get("analysesGlobale"), d);
                    historiqueJournalier.add(item);
                }

                Collections.reverse(historiqueJournalier);

                LocalDate firstMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate lastSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

                for (LocalDate weekStart = firstMonday;
                     !weekStart.isAfter(lastSunday);
                     weekStart = weekStart.plusWeeks(1)) {

                    LocalDate weekEnd = weekStart.plusDays(6);
                    DetailsHistorique item = new DetailsHistorique();
                    item.jourOuSemaine = String.format(Locale.getDefault(), "Semaine %d (%s–%s)", weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), weekStart.format(DateTimeFormatter.ofPattern("dd/MM")), weekEnd .format(DateTimeFormatter.ofPattern("dd/MM")));
                    item.hasFaciale    = containsInRange(mapAnalyses.get("analysesFaciale"),    weekStart, weekEnd);
                    item.hasVocale    = containsInRange(mapAnalyses.get("analysesVocale"),    weekStart, weekEnd);
                    item.hasCognitive = containsInRange(mapAnalyses.get("analysesCognitive"), weekStart, weekEnd);
                    item.hasGlobale    = containsInRange(mapAnalyses.get("analysesGlobale"),   weekStart, weekEnd);
                    historiqueHebdomadaire.add(item);
                }
                    Collections.reverse(historiqueHebdomadaire);

                    if (tabLayout.getSelectedTabPosition() == 0) {
                        historiqueAdaptation.submitList(historiqueHebdomadaire);
                    } else {
                        historiqueAdaptation.submitList(historiqueJournalier);
                    }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    /**
     * Vérifie si une date existe dans une liste de LocalDateTime pour un jour donné.
     *
     * @param list la liste de dates
     * @param day la date à rechercher
     * @return true si au moins une date correspond au jour
     */

    private boolean containsDate(List<LocalDateTime> list, LocalDate day) {
        return list.stream().anyMatch(dt -> dt.toLocalDate().equals(day));
    }


    /**
     * Vérifie si une des dates de la liste se situe dans une période donnée.
     *
     * @param list la liste de dates
     * @param start date de début (incluse)
     * @param end date de fin (incluse)
     * @return true si au moins une date est dans la période
     */

    private boolean containsInRange(List<LocalDateTime> list, LocalDate start, LocalDate end) {
        return list.stream().anyMatch(dt -> {
            LocalDate d = dt.toLocalDate();
            return (!d.isBefore(start) && !d.isAfter(end));
        });
    }

    /**
     * Affiche une boîte de dialogue lorsque la connexion est perdue.
     */

    private void showOfflineDialog() {
        if (offlineDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.satellite);
            builder.setTitle("Connexion perdue");
            builder.setMessage("L'accès à vos données est impossible");
            builder.setCancelable(false);
            builder.setPositiveButton("Retour au Menu", (dialog, which) -> {
                Intent result = new Intent();
                setResult(RESULT_OK, result);
                finish();
            });

            offlineDialog = builder.create();
            offlineDialog.show();
        }
    }

    /**
     * Nettoyage de l'activité, notamment la déconnexion du callback réseau.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }


}