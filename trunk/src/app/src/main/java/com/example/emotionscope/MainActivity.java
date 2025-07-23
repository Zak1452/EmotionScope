package com.example.emotionscope;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe principale du projet.
 *
 * @author Ada BOUAZO, Ahmed-Lamin CHABANE CHAOURAR, Zakaria CHAKER et Émile JIANG
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES = "MyPrefsFile";
    private static final String CLE_CGU = "termsAccepted";
    private static final String CLE_UID = "uid";
    private static final String CLE_PRENOM = "prenom";
    private static final String CLE_NOM = "nom";
    private static final String CLE_EMAIL = "email";
    private static final String CLE_CONNEXION = "estConnecte";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int MICROPHONE_PERMISSION_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 102;
    private static final int INTERNET_PERMISSION_REQUEST_CODE = 103;
    private Button btn_choix_analyse, btn_analyse_complete;
    private ImageButton btn_historique, btnProfil;
    private TextView tvBienvenue, tvStatut, tvDerniereAnalyse, tvCGU, tvPlatforme, tvAnalyses, tvSupressionCompte;
    private AlertDialog offlineDialog;
    private ConnectivityManager.NetworkCallback networkCallback;
    private LinearLayout btnDeconnexion;
    private View sidePanel;
    private AlertDialog onlineDialog;
    private boolean wasOffline = false;
    private DetailsAnalysesMain detailsAnalyses;


    private String uid, prenom, nom, email;
    private boolean estConnecte;

    private FirebaseDatabase database;
    private DatabaseReference utilisateursRef;

    /**
     * Méthode appelée lors de la création de l'activité principale.
     * Elle initialise les préférences, l'interface utilisateur, les connexions à Firebase,
     * gère l'état de connexion, les permissions, les événements de réseau, et configure les
     * actions des boutons de navigation dans l'application.
     *
     * @param savedInstanceState Données de l'état précédent de l'activité, si disponible.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        database  = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
        utilisateursRef = database.getReference("utilisateurs");

        verificationPermissions();

        SharedPreferences settings = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        boolean CGU_accepte = settings.getBoolean(CLE_CGU, false);
        Log.d("MainActivity", "CGU accepted: " + CGU_accepte);

        Intent intent = getIntent();
        boolean instanceEstConnecte = intent.getBooleanExtra("estConnecte", false);

        if(instanceEstConnecte)
        {
            uid = intent.getStringExtra("uid");
            prenom     = intent.getStringExtra("prenom");
            nom        = intent.getStringExtra("nom");
            email      = intent.getStringExtra("email");

            estConnecte = true;

            settings.edit().putString(CLE_UID, uid)
                    .putString(CLE_PRENOM, prenom)
                    .putString(CLE_NOM, nom)
                    .putString(CLE_EMAIL, email)
                    .putBoolean(CLE_CONNEXION, estConnecte)
                    .apply();
        }
        else
        {

            uid = settings.getString(CLE_UID, null);
            prenom      = settings.getString(CLE_PRENOM, null);
            nom         = settings.getString(CLE_NOM,    null);
            email       = settings.getString(CLE_EMAIL,  null);
            estConnecte = settings.getBoolean(CLE_CONNEXION, false);
        }

        if (!CGU_accepte) {
            CGU_Dialog();
        }

        btn_choix_analyse = findViewById(R.id.btn_choix_analyse);
        btn_analyse_complete = findViewById(R.id.btn_analyse_complete);
        btn_historique = findViewById(R.id.btnHistorique);
        btnProfil  = findViewById(R.id.btnProfil);
        tvBienvenue = findViewById(R.id.textViewBienvenue);
        tvStatut = findViewById(R.id.textViewEtatConnexion);
        sidePanel = findViewById(R.id.sidePanel);
        sidePanel.setVisibility(View.GONE);

        btnDeconnexion = findViewById(R.id.btnDeconnexion);

        if (estConnecte && prenom != null && nom != null && uid != null) {
            tvBienvenue.setText("Bienvenue, " + prenom + " !");
            tvStatut.setText("Connecté ✔");
            tvStatut.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            detailsAnalyses = new DetailsAnalysesMain(this, database, uid);
            detailsAnalyses.fetchAndDisplayLatestAnalyses(new DetailsAnalysesMain.Callback() {
                @Override
                public void onAllFetched() {
                    detailsAnalyses.startFloatingAnimation();
                }
                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this,
                            "Erreur chargement analyses: " + message,
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            tvBienvenue.setText("Bienvenue 👋");
            tvStatut.setText("Non connecté - Mode invité");
            tvStatut.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest req = new NetworkRequest.Builder().build();
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onLost(Network network) {
                wasOffline = true;
                runOnUiThread(() -> showOfflineDialog());
            }
            @Override public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (wasOffline) {
                        if (offlineDialog != null && offlineDialog.isShowing()) {
                            offlineDialog.dismiss();
                            offlineDialog = null;
                        }
                        showOnlineDialog();
                        wasOffline = false;
                    }
                    tvStatut.setText("Connecté ✔");
                    tvStatut.setTextColor(Color.GREEN);

                });
            }
        };
        cm.registerNetworkCallback(req, networkCallback);
        btn_choix_analyse.setOnClickListener(view -> {
            if (estConnecte && uid != null && prenom != null && nom != null && email != null) {
                Log.d("Choix analyse", "Données de l'utilisateur : " +
                        "UID: " + uid + ", Prénom: " + prenom + ", Nom: " + nom + ", Email: " + email);
                affichageBoiteDialogueChoixAnalyse();
            }
            else {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Oops !")
                        .setMessage("Vous n’avez pas l’air d’être connecté.\n\nConnectez-vous ou rejoignez l’expérience Emotion Scope !")
                        .setIcon(R.drawable.danger)
                        .setPositiveButton("M’identifier", (dialog, which) -> {
                            afficherBoiteDialogueIdentification();
                        })
                        .setNegativeButton("Annuler", null)
                        .setCancelable(true)
                        .show();
            }
        });

        btn_analyse_complete.setOnClickListener(view -> {
            if (estConnecte && uid != null && prenom != null && nom != null && email != null) {
                affichageBoiteDialogueAnalyseComplete(true, uid, prenom, nom, email);
            } else {
                new MaterialAlertDialogBuilder(com.example.emotionscope.MainActivity.this)
                        .setTitle("Oops !")
                        .setMessage("Vous n’avez pas l’air d’être connecté.\n\nConnectez-vous ou rejoignez l’expérience Emotion Scope !")
                        .setIcon(R.drawable.planetes)
                        .setPositiveButton("M’identifier", (dialog, which) -> {
                            afficherBoiteDialogueIdentification();
                        })
                        .setNegativeButton("Annuler", null)
                        .setCancelable(true)
                        .show();
            }
        });
        
        btn_historique.setOnClickListener(view -> {
            if (estConnecte && uid != null && prenom != null && nom != null && email != null) {
                Intent instance = new Intent(com.example.emotionscope.MainActivity.this, HistoriqueActivity.class);
                instance.putExtra("estConnecte", true);
                instance.putExtra("uid", uid);
                instance.putExtra("prenom", prenom);
                instance.putExtra("nom", nom);
                instance.putExtra("email", email);
                startActivityForResult(instance, 1234);
            } else {
                new MaterialAlertDialogBuilder(com.example.emotionscope.MainActivity.this)
                        .setTitle("Oops !")
                        .setMessage("Vous n’avez pas l’air d’être connecté.\n\nConnectez-vous ou rejoignez l’expérience Emotion Scope !")
                        .setIcon(R.drawable.planetes)
                        .setPositiveButton("M’identifier", (dialog, which) -> {
                            afficherBoiteDialogueIdentification();
                        })
                        .setNegativeButton("Annuler", null)
                        .setCancelable(true)
                        .show();
            }
        });

        btnDeconnexion.setOnClickListener(view -> afficherBoiteDialogueDeconnexion());
        btnProfil.setOnClickListener(view -> {
            if (estConnecte) {
                ((TextView) findViewById(R.id.textViewNomPrenom)).setText(prenom + " " + nom);
                ((TextView) findViewById(R.id.textViewMail)).setText(email);
                sidePanel.setVisibility(sidePanel.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                tvAnalyses = findViewById(R.id.textViewAnalyses);
                tvCGU = findViewById(R.id.textViewCGU);
                tvPlatforme = findViewById(R.id.textViewPlatforme);
                tvSupressionCompte = findViewById(R.id.textViewSuppresionCompte);

                tvCGU.setOnClickListener(v->{
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setIcon(R.drawable.planetes);
                    builder.setTitle("Conditions d'utilisation");
                    builder.setMessage("Veuillez lire et accepter les conditions d'utilisation pour continuer.\n\n" +
                            "Conditions Générales d'Utilisation :\n" +
                            "1. Cette application collecte des données personnelles pour améliorer votre expérience utilisateur.\n" +
                            "2. Nous respectons votre vie privée et nous engageons à protéger vos données conformément au Règlement Général sur la Protection des Données (RGPD).\n" +
                            "3. Vos données seront utilisées uniquement dans le cadre des services fournis par cette application et ne seront pas partagées avec des tiers sans votre consentement explicite.\n" +
                            "4. Vous avez le droit de demander l'accès, la rectification ou la suppression de vos données personnelles à tout moment."

                    );
                    builder.setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                    });
                    builder.show();
                });
                tvAnalyses.setOnClickListener(v->{
                    Intent instance = new Intent(MainActivity.this, HistoriqueActivity.class);
                    instance.putExtra("estConnecte", estConnecte);
                    instance.putExtra("uid", uid);
                    instance.putExtra("prenom", prenom);
                    instance.putExtra("nom", nom);
                    instance.putExtra("email", email);
                    startActivity(instance);
                });

                tvPlatforme.setOnClickListener(v->
                {
                    Intent browserIntent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://emotionscope.onrender.com/inscription-conn")
                    );
                    startActivity(browserIntent);
                });

                tvSupressionCompte.setOnClickListener(v->{
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Suppression de compte")
                            .setMessage("Êtes‑vous sûr de vouloir supprimer définitivement votre compte? Cette action est irréversible!")
                            .setIcon(R.drawable.danger)
                            .setPositiveButton("Oui, supprimer", (dialog, which) -> {

                                final EditText inputPwd = new EditText(this);
                                inputPwd.setHint("Votre mot de passe");
                                inputPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                                new MaterialAlertDialogBuilder(this)
                                        .setTitle("Vérification")
                                        .setMessage("Veuillez saisir votre mot de passe :")
                                        .setView(inputPwd)
                                        .setPositiveButton("Valider", (pwdDlg, pwdWhich) -> {
                                            String motSaisi = inputPwd.getText().toString().trim();
                                            if (motSaisi.isEmpty()) {
                                                Toast.makeText(this, "Entrez votre mot de passe.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            utilisateursRef.child(uid)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snap) {
                                                            if (!snap.exists()) {
                                                                Toast.makeText(MainActivity.this,
                                                                        "Utilisateur introuvable.", Toast.LENGTH_LONG).show();
                                                                return;
                                                            }
                                                            String hashBcrypt = snap.child("motDePasse").getValue(String.class);
                                                            if (hashBcrypt == null) {
                                                                Toast.makeText(MainActivity.this,
                                                                        "Aucun mot de passe enregistré.", Toast.LENGTH_LONG).show();
                                                                return;
                                                            }

                                                            if (!BCrypt.checkpw(motSaisi, hashBcrypt)) {
                                                                Toast.makeText(MainActivity.this,
                                                                        "Mot de passe incorrect.", Toast.LENGTH_LONG).show();
                                                                return;
                                                            }

                                                            utilisateursRef.child(uid).removeValue()
                                                                    .addOnCompleteListener(task -> {
                                                                        if (!task.isSuccessful()) {
                                                                            Toast.makeText(MainActivity.this,
                                                                                    "Erreur suppression : " + task.getException().getMessage(),
                                                                                    Toast.LENGTH_LONG).show();
                                                                            return;
                                                                        }
                                                                        SharedPreferences prefs = getSharedPreferences(
                                                                                PREFERENCES, MODE_PRIVATE);
                                                                        prefs.edit().clear().apply();
                                                                        Toast.makeText(MainActivity.this,
                                                                                "Compte supprimé avec succès.", Toast.LENGTH_LONG).show();
                                                                        finishAffinity();
                                                                    });
                                                        }
                                                        @Override public void onCancelled(@NonNull DatabaseError error) {
                                                            Toast.makeText(MainActivity.this,
                                                                    "Erreur lecture base : " + error.getMessage(),
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        })
                                        .setNegativeButton("Annuler", null)
                                        .show();

                            })
                            .setNegativeButton("Non", null)
                            .show();
                });
            } else {
                uid = null;
                afficherBoiteDialogueIdentification();
            }
        });

        TextView themeToggle = findViewById(R.id.textViewToggleTheme);
        if (themeToggle != null) {
            themeToggle.setOnClickListener(v -> {
                boolean isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
                AppCompatDelegate.setDefaultNightMode(
                        isDark ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
                );
                prefs.edit().putBoolean("dark_mode", !isDark).apply();
            });
        }
    }

    /**
     *  Appelée lors de la destruction de l'activité.
     * Cette méthode désenregistre le callback réseau pour éviter les fuites de mémoire.
     */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (networkCallback != null) cm.unregisterNetworkCallback(networkCallback);
    }

    private void showOfflineDialog() {
        if (offlineDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Connexion perdue");
            builder.setMessage("Vous êtes en mode hors connexion.");
            builder.setIcon(R.drawable.danger);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", null);
            offlineDialog = builder.create();
            offlineDialog.show();
            tvStatut.setText("Hors ligne");
            tvStatut.setTextColor(Color.RED);
        }
    }

    /**
     * Ferme la boîte de dialogue "hors ligne" si elle est visible et met à jour
     * l'état de connexion de l'utilisateur dans l'interface.
     * Affiche "Connecté ✔" ou "Non connecté" selon la présence du prénom en mémoire.
     */

    private void dismissOfflineDialog() {
        if (offlineDialog != null && offlineDialog.isShowing()) {
            offlineDialog.dismiss();
            offlineDialog = null;
            String prenom = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(CLE_PRENOM, null);
            if (prenom != null) {
                tvStatut.setText("Connecté ✔");
                tvStatut.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                tvStatut.setText("Non connecté");
            }
        }
    }


    /**
     * Affiche un message temporaire indiquant que la connexion a été rétablie.
     * Une boîte de dialogue personnalisée avec une icône et un texte s'affiche
     * automatiquement pendant 2 secondes.
     */

    private void showOnlineDialog() {
        if (onlineDialog != null && onlineDialog.isShowing()) return;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        int padding = (int)(24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        ImageView iv = new ImageView(this);
        iv.setImageResource(R.drawable.satellite);   // remplace par ton drawable
        int size = (int)(150 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(size, size);
        ivParams.bottomMargin = (int)(16 * getResources().getDisplayMetrics().density);
        iv.setLayoutParams(ivParams);
        container.addView(iv);

        TextView tv = new TextView(this);
        tv.setText("Connexion rétablie");
        tv.setTextSize(20);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        container.addView(tv);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(container)
                .setCancelable(true);

        onlineDialog = builder.create();
        onlineDialog.show();

        container.postDelayed(() -> {
            if (onlineDialog.isShowing()) onlineDialog.dismiss();
        }, 2000);
    }


    /**
     * Méthode qui permet de faire appel à la boite de dialogue,  permettant à l'utilisateur de choisir un
     * type d'analyse à effectuer (faciale, vocale, cognitive)
     * Chaque bouton lance l’analyse correspondante après fermeture du dialogue.
     * Cette boite de dialogue permettera à l'user de choisir un type d'analyse à effectuer...
     *
     * @author: Chaker Zakaria
     */
    private void affichageBoiteDialogueChoixAnalyse() {

        View dialogView = LayoutInflater.from(this).inflate(R.layout.boite_dialogue_choix_analyse, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choisissez un type d'analyse");
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button btnFaciale = dialogView.findViewById(R.id.btn_analyse_faciale);
        Button btnVocale = dialogView.findViewById(R.id.btn_analyse_vocale);
        Button btnCognitive = dialogView.findViewById(R.id.btn_analyse_cognitive);

        btnFaciale.setOnClickListener(v -> {
            Log.d("BoiteDialogue", "Bouton cliqué analyse faciale");
            dialog.dismiss();
            analyseFaciale();
        });

        btnVocale.setOnClickListener(v -> {
            dialog.dismiss();
            analyseVocale();
        });

        btnCognitive.setOnClickListener(v -> {
            dialog.dismiss();
            analyseCognitive();
        });

        dialog.show();
    }

    /**
     * Affiche une boîte de dialogue pour lancer une analyse complète.
     * Le bouton de démarrage déclenche le processus complet (non encore implémenté).
     */

    private void affichageBoiteDialogueAnalyseComplete(boolean connexion, String uid, String nom, String prenom, String email) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.boite_dialogue_analyse_complete, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Button btndemarrer = dialogView.findViewById(R.id.btn_demarrer_analyse_complete);
        btndemarrer.setOnClickListener(v -> {
            Log.d("BoiteDialogue_analyse_complete", "Bouton cliqué analyse complete");
            Intent instance = new Intent(MainActivity.this, AnalyseCompleteActivity.class);
            instance.putExtra("estConnecte", true);
            instance.putExtra("uid", uid);
            instance.putExtra("prenom", prenom);
            instance.putExtra("nom", nom);
            instance.putExtra("email", email);
            startActivity(instance);
            dialog.dismiss();
        });

    }
    /**
     * Méthode qui permet de faire l'affoche de la boite de dialogue, permettant la connexion de l'utilisateur
     * via un email et un mot de passe.
     * En cas d'échec
     * (email non trouvé ou mot de passe incorrect), un message d'erreur est affiché.
     * Elle permet à l'user de saisir ses ientifiants ou de s'inscrire en cas de compte non créé.
     *
     * @author: Chaker Zakaria
     */
    private void afficherBoiteDialogueIdentification() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.boite_dialogue_identification, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        Button btnConnexion       = dialogView.findViewById(R.id.btnConnexion);
        EditText champUtilisateur = dialogView.findViewById(R.id.saisieMail);
        EditText champMotDePasse  = dialogView.findViewById(R.id.saisieMDP);
        Button btnInscription     = dialogView.findViewById(R.id.btninscription);
        TextView titre            = dialogView.findViewById(R.id.affichageTexte);

        dialog.setCanceledOnTouchOutside(true);

        btnConnexion.setOnClickListener(v -> {
            String utilisateur = champUtilisateur.getText().toString().trim();
            String motDePasse  = champMotDePasse.getText().toString();

            if (utilisateur.isEmpty() || motDePasse.isEmpty()) {
                titre.setText("Veuillez remplir tous les champs");
                titre.setTextColor(Color.RED);
                return;
            }

            titre.setText("Vérification…");
            titre.setTextColor(Color.DKGRAY);

            Query query = utilisateursRef
                    .orderByChild("email")
                    .equalTo(utilisateur);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    if (!snap.exists()) {
                        titre.setText("Email introuvable");
                        titre.setTextColor(Color.RED);
                        return;
                    }

                    for (DataSnapshot userSnap : snap.getChildren()) {
                        String hash = userSnap.child("motDePasse")
                                .getValue(String.class);

                        if (hash == null || !BCrypt.checkpw(motDePasse, hash)) {
                            titre.setText("Mot de passe incorrect");
                            titre.setTextColor(Color.RED);
                            return;
                        }

                        String uidFirebase = userSnap.getKey();
                        String emailFirebase  = userSnap.child("email")
                                .getValue(String.class);
                        String prenomFirebase = userSnap.child("prenom")
                                .getValue(String.class);
                        String nomFirebase    = userSnap.child("nom")
                                .getValue(String.class);


                        SharedPreferences prefs = getSharedPreferences(
                                PREFERENCES, MODE_PRIVATE
                        );
                        prefs.edit()
                                .putBoolean(CLE_CONNEXION, true)
                                .putString(CLE_UID,uidFirebase)
                                .putString(CLE_PRENOM, prenomFirebase)
                                .putString(CLE_NOM, nomFirebase)
                                .putString(CLE_EMAIL, emailFirebase)
                                .apply();

                        tvBienvenue.setText("Bienvenue, " + prenomFirebase + " !");
                        tvStatut.setText("Connecté ✔");
                        setCleConnexion(true);
                        setPrenom(prenomFirebase);
                        setNom(nomFirebase);
                        setMail(emailFirebase);
                        setUid(uidFirebase);
                        tvStatut.setTextColor(ContextCompat.getColor(
                                MainActivity.this, android.R.color.holo_green_dark
                        ));

                        if (detailsAnalyses == null) {
                            detailsAnalyses = new DetailsAnalysesMain(
                                    MainActivity.this,
                                    database,
                                    uid
                            );
                        }
                        detailsAnalyses.fetchAndDisplayLatestAnalyses(new DetailsAnalysesMain.Callback() {
                            @Override public void onAllFetched() {
                                detailsAnalyses.startFloatingAnimation();
                            }
                            @Override public void onError(String message) {
                                Toast.makeText(MainActivity.this,
                                        "Erreur chargement analyses: " + message,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        dialog.dismiss();
                        return;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    titre.setText("Erreur réseau. Réessayez.");
                    titre.setTextColor(Color.RED);
                }
            });
        });
        btnInscription.setOnClickListener(v -> {
            Intent instance = new Intent(this, InscriptionUtilisateur.class);
            startActivityForResult(instance, 1);
            dialog.dismiss();
        });

        dialog.show();
    }


    /**
     * Lance l'activité Analyse Faciale si l'utilisateur est connecté
     * et que ses données sont valides. Sinon, affiche un message d'avertissement.
     *
     * Nécessite les informations de l'utilisateur : uid, prénom, nom, email.
     * Utilise un {@link MaterialAlertDialogBuilder} pour afficher une alerte en cas de non-connexion.
     *
     * @author Chaker Zakaria
     */

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyseFaciale() {
        if (estConnecte && uid != null && prenom != null && nom != null && email != null) {
            Log.d("AnalyseFaciale", "Données de l'utilisateur : " +
                    "UID: " + uid + ", Prénom: " + prenom + ", Nom: " + nom + ", Email: " + email);

            Intent intent = new Intent(MainActivity.this, AnalyseFacialeActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("prenom", prenom);
            intent.putExtra("nom", nom);
            intent.putExtra("email", email);
            startActivity(intent);
        }
        else {
            //L'utilisateur n'est pas connecté, on affiche une alerte ou un message
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Oops !")
                    .setMessage("Vous n’avez pas l’air d’être connecté.\n\nConnectez-vous ou rejoignez l’expérience Emotion Scope !")
                    .setIcon(R.drawable.danger)
                    .setPositiveButton("M’identifier", (dialog, which) -> {
                        afficherBoiteDialogueIdentification();
                    })
                    .setNegativeButton("Annuler", null)
                    .setCancelable(true)
                    .show();
        }
    }

    /**
     * (Méthode prévue pour lancer l'analyse vocale mais actuellement désactivée)
     * Cette méthode pourrait être utilisée pour démarrer une activité d’analyse vocale.
     *
     * @author Chaker Zakaria , Chabane Chaourar Ahmed-Lamin
     */

    private void analyseVocale() {
        if (estConnecte && uid != null && prenom != null && nom != null && email != null) {
            Log.d("AnalyseVocale", "Données de l'utilisateur : " +
                    "UID: " + uid + ", Prénom: " + prenom + ", Nom: " + nom + ", Email: " + email);

            Intent intent = new Intent(MainActivity.this, AnalyseVocaleActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("prenom", prenom);
            intent.putExtra("nom", nom);
            intent.putExtra("email", email);
            startActivity(intent);
        } else {
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Oops !")
                    .setMessage("Vous n’avez pas l’air d’être connecté.\n\nConnectez-vous ou rejoignez l’expérience Emotion Scope !")
                    .setIcon(R.drawable.danger)
                    .setPositiveButton("M’identifier", (dialog, which) -> {
                        afficherBoiteDialogueIdentification();
                    })
                    .setNegativeButton("Annuler", null)
                    .setCancelable(true)
                    .show();
        }
    }


    /**
     *
     * @author: Chaker Zakaria
     */
    private void verificationPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     *
     * @param requestCode The request code passed in {@link #//requestPermissions(
     * android.app.Activity, String[], int)}
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     * @author: Jiang Emile
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission accordée pour la caméra
                    //Tu peux accéder à la caméra ici
                } else {
                    //ermission refusée pour la caméra
                    Toast.makeText(this, "La permission de la caméra est requise", Toast.LENGTH_SHORT).show();
                }
                break;

            case MICROPHONE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    Toast.makeText(this, "La permission du microphone est requise", Toast.LENGTH_SHORT).show();
                }
                break;

            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "La permission du stockage est requise", Toast.LENGTH_SHORT).show();
                }
                break;

            case INTERNET_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "La permission internet est requise", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Affiche une boîte de dialogue demandant à l'utilisateur d'accepter
     * les Conditions Générales d'Utilisation (CGU). L'acceptation est obligatoire pour continuer.
     * Enregistre la décision dans les {@link SharedPreferences}.
     *
     *   @author Jiang Emile
     */
    private void CGU_Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conditions d'utilisation");
        builder.setMessage("Veuillez lire et accepter les conditions d'utilisation pour continuer.\n\n" +
                "Conditions Générales d'Utilisation :\n" +
                "1. Cette application collecte des données personnelles pour améliorer votre expérience utilisateur.\n" +
                "2. Nous respectons votre vie privée et nous engageons à protéger vos données conformément au Règlement Général sur la Protection des Données (RGPD).\n" +
                "3. Vos données seront utilisées uniquement dans le cadre des services fournis par cette application et ne seront pas partagées avec des tiers sans votre consentement explicite.\n" +
                "4. Vous avez le droit de demander l'accès, la rectification ou la suppression de vos données personnelles à tout moment."

        );


        RadioButton acceptRadioButton = new RadioButton(this);
        acceptRadioButton.setText("J'ai lu et accepté les conditions d'utilisation");


        builder.setView(acceptRadioButton);


        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (acceptRadioButton.isChecked()) {

                    SharedPreferences settings = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(CLE_CGU, true);
                    editor.apply();
                    dialog.dismiss();
                }
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setEnabled(false);

                acceptRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    okButton.setEnabled(isChecked);
                });
            }
        });

        dialog.show();
    }

    /**
     * Méthode appelée à la suite d'une activité qui a renvoyé un résultat.
     * Utilisée ici pour récupérer l'état de connexion (mode hors ligne).
     *
     * @param requestCode Le code de la requête.
     * @param resultCode Le code du résultat.
     * @param data Les données retournées par l'activité.
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1234 && resultCode == RESULT_OK && data != null) {
            boolean horsLigne = data.getBooleanExtra("hors_ligne", false);
            if (horsLigne) {
                tvStatut.setText("Hors ligne");
                tvStatut.setTextColor(Color.RED);
                estConnecte = false;
            }
        }
    }

    /**
     * Affiche une boîte de dialogue de confirmation de déconnexion avec animation GIF.
     * Si l'utilisateur confirme, la méthode {@link #deconnecterUtilisateur()} est appelée.
     */

    private void afficherBoiteDialogueDeconnexion()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.boite_dialogue_deconnexion, null);

        ImageView ivLogoutGif = dialogView.findViewById(R.id.imageDeconnexion);

        Glide.with(this)
                .asGif()
                .load(R.drawable.eclipse)
                .into(ivLogoutGif);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnOui = dialogView.findViewById(R.id.btnOui);
        Button btnNon  = dialogView.findViewById(R.id.btnNon);


        btnOui.setOnClickListener(v -> {
            deconnecterUtilisateur();
            dialog.dismiss();
        });

        btnNon.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    /**
     * Déconnecte l'utilisateur en effaçant ses données des {@link SharedPreferences}
     * puis redirige vers la {@link MainActivity}. Affiche un Toast confirmant la déconnexion.
     */


    private void deconnecterUtilisateur() {
        SharedPreferences preferences = getSharedPreferences("MyPrefsFile", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
    }

    /**
     * Lance l'activité Analyse Cognitive si l'utilisateur est connecté.
     * Sinon, affiche une alerte invitant à se connecter.
     *
     * Nécessite : uid, prénom, nom, email.
     * Affiche une alerte via {@link MaterialAlertDialogBuilder} si non connecté.
     *
     * @author BOUAZO Ada
     */

    private void analyseCognitive() {
        if (estConnecte && uid != null && prenom != null && nom != null && email != null) {
            Log.d("AnalyseCognitive", "Données de l'utilisateur : " +
                    "UID: " + uid + ", Prénom: " + prenom + ", Nom: " + nom + ", Email: " + email);

            Intent intent = new Intent(MainActivity.this, AnalyseCognitiveActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("prenom", prenom);
            intent.putExtra("nom", nom);
            intent.putExtra("email", email);
            startActivity(intent);
        }
        else {
            //L'utilisateur n'est pas connecté, on affiche une alerte ou un message
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Oops !")
                    .setMessage("Vous n’avez pas l’air d’être connecté.\n\nConnectez-vous ou rejoignez l’expérience Emotion Scope !")
                    .setIcon(R.drawable.danger)
                    .setPositiveButton("M’identifier", (dialog, which) -> {
                        afficherBoiteDialogueIdentification();
                    })
                    .setNegativeButton("Annuler", null)
                    .setCancelable(true)
                    .show();
        }
    }

    /**
     * Définit l'état de connexion de l'utilisateur.
     *
     * @param val true si l'utilisateur est connecté, false sinon.
     */

    public void setCleConnexion(boolean val)
    {
        this.estConnecte = val;
    }

    /**
     * Définit l'identifiant utilisateur.
     *
     * @param val UID de l'utilisateur.
     */

    public void setUid(String val){this.uid = val;}


    /**
     * Définit le prénom de l'utilisateur.
     *
     * @param val Prénom de l'utilisateur.
     */

    public void setPrenom(String val)
    {
        this.prenom = val;
    }

    /**
     * Définit le nom de l'utilisateur.
     *
     * @param val Nom de l'utilisateur.
     */
    public void setNom(String val)
    {
        this.nom = val;
    }


    /**
     * Définit l'adresse e-mail de l'utilisateur.
     *
     * @param val Email de l'utilisateur.
     */

    public void setMail(String val)
    {
        this.email = val;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        estConnecte = settings.getBoolean(CLE_CONNEXION, false);
        uid         = settings.getString(CLE_UID, null);

        Log.d("MainActivity", "onResume() – estConnecte=" + estConnecte + ", uid=" + uid);
        if (estConnecte && uid != null) {
            if (detailsAnalyses == null) {
                detailsAnalyses = new DetailsAnalysesMain(this, database, uid);
            }
            detailsAnalyses.fetchAndDisplayLatestAnalyses(new DetailsAnalysesMain.Callback() {
                @Override public void onAllFetched() {
                    detailsAnalyses.startFloatingAnimation();
                }
                @Override public void onError(String message) {
                    Toast.makeText(MainActivity.this,
                            "Erreur chargement analyses: " + message,
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}

