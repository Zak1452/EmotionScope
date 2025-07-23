package com.example.emotionscope;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Classe qui permet de gérer l'affichage des résultats réalisés à unjour donné.
 * Elle récupère les données d'analyses depuis Firebase et les affiche dans un graphique et des cartes.
 *
 * @author: CHAKER Zakaria
 */
public class DetailsAnalysesHistorique extends AppCompatActivity {

    private BarChart chart;
    private LinearLayout container;
    private String prenom, nom, email, date, uid, dateIso;

    private FirebaseDatabase database;
    private DatabaseReference utilisateurRef;

    private int totalAAnalyser = 4;
    private int totalAnalyses = 0;

    /**
     * Méthode appelée à la création de l'activité.
     * Elle initialise l'interface et déclenche le chargement des données.
     *
     * @param savedInstanceState l'état sauvegardé de l'activité
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.historique_details);

        date = getIntent().getStringExtra("date");
        uid  = getIntent().getStringExtra("uid");
        nom = getIntent().getStringExtra("nom");
        prenom  = getIntent().getStringExtra("prenom");
        email = getIntent().getStringExtra("email");

        if (date == null || uid == null) {
            Toast.makeText(this, "Données manquantes.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DateTimeFormatter inFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            LocalDate d = LocalDate.parse(date, inFmt);
            dateIso = d.format(outFmt);
        } catch (DateTimeParseException e) {
            Log.e("DateParse", "Erreur parsing date “" + date + "”", e);
            dateIso = date;
        }

        chart = findViewById(R.id.graphiqueAnalyses);
        container = findViewById(R.id.contenantAnalyses);

        database       = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
        utilisateurRef = database.getReference("utilisateurs").child(uid);

        initialisationChart();
        chargementDonneesFirebase();
    }

    /**
     * Initialise les paramètres de configuration du graphique (BarChart).
     */


    private void initialisationChart() {
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 0: return "Faciale";
                    case 1: return "Vocale";
                    case 2: return "Cog.";
                    case 3: return "Globale";
                    default: return "";
                }
            }
        });
        chart.setFitBars(true);
    }

    /**
     * Charge les données d'analyse à partir de Firebase pour la date sélectionnée.
     * Trie et formate les résultats pour affichage dans des cartes et le graphique.
     */
    private void chargementDonneesFirebase() {
        Map<String,Integer> counts = new HashMap<>();
        counts.put("analysesFaciale",   0);
        counts.put("analysesVocale",    0);
        counts.put("analysesCognitive", 0);
        counts.put("analysesGlobale",   0);

        for (String node : counts.keySet()) {
            Log.d("Firebase", "Lecture de “" + node + "” pour dateIso=" + dateIso);
            utilisateurRef.child(node)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(DataSnapshot snap) {
                            int cnt = 0;
                            for (DataSnapshot child : snap.getChildren()) {
                                Analyse analyse = null;
                                String dateAnalyse = child.child("date").getValue(String.class);
                                String result;

                                if (node.equals("analysesFaciale")) {
                                    AnalyseFaciale aF = child.getValue(AnalyseFaciale.class);
                                    if (aF == null || aF.getDate() == null || !aF.getDate().startsWith(dateIso)) continue;
                                    analyse = aF;
                                    String emoji;
                                    switch (aF.emotionDominante != null ? aF.emotionDominante : "") {
                                        case "Joie": emoji = "😊"; break;
                                        case "Tristesse": emoji = "😢"; break;
                                        case "Colère": emoji = "😡"; break;
                                        case "Surprise": emoji = "😲"; break;
                                        case "Peur": emoji = "😱"; break;
                                        case "Dégoût": emoji = "🤢"; break;
                                        default: emoji = "🙂"; break;
                                    }
                                    result = aF.emotionDominante != null
                                            ? aF.emotionDominante + " " + emoji
                                            : "Aucune émotion";
                                }
                                else if (node.equals("analysesCognitive")) {
                                    AnalyseCognitive aC = child.getValue(AnalyseCognitive.class);
                                    if (aC == null || aC.getDate() == null || !aC.getDate().startsWith(dateIso)) continue;
                                    analyse = aC;
                                    double m = aC.moyennes.getOrDefault("memoire", 0);
                                    double p = aC.moyennes.getOrDefault("perception", 0);
                                    double r = aC.moyennes.getOrDefault("raisonnement", 0);
                                    result = String.format(Locale.FRANCE,
                                            "🧠 %.0f%%   👁 %.0f%%   🧩 %.0f%%",
                                            m, p, r);
                                }

                                else if (node.equals("analysesVocale")) {
                                    AnalyseVocale aV = child.getValue(AnalyseVocale.class);
                                    if (aV == null || aV.getDate() == null || !aV.getDate().startsWith(dateIso)) continue;
                                    analyse = aV;
                                    String emoji;
                                    switch (aV.emotionDominante != null ? aV.emotionDominante : "") {
                                        case "Joie","Happy": emoji = "😊"; break;
                                        case "Tristesse","Sad": emoji = "😢"; break;
                                        case "Colère","Angry": emoji = "😡"; break;
                                        case "Surprise","Surprised": emoji = "😲"; break;
                                        case "Peur","Fear": emoji = "😱"; break;
                                        case "Dégoût","Disgust": emoji = "🤢"; break;
                                        default: emoji = "🙂"; break;
                                    }
                                    result = aV.emotionDominante != null
                                            ? aV.emotionDominante + " " + emoji
                                            : "Aucune émotion";
                                } else if(node.equals("analysesGlobale"))
                                {
                                    AnalyseGlobale ag = child.getValue(AnalyseGlobale.class);
                                    if (ag == null || ag.getDate() == null || !ag.getDate().startsWith(dateIso)) continue;
                                    analyse = ag;
                                    String facePart = ag.faciale.emotionDominante != null
                                            ? "😃 " + ag.faciale.emotionDominante : "😃 —";
                                    String vocPart  = ag.vocale.emotionDominante != null
                                            ? "🎤 " + ag.vocale.emotionDominante  : "🎤 —";
                                    double mem = ag.cognitive.moyennes.getOrDefault("memoire",    0);
                                    double per = ag.cognitive.moyennes.getOrDefault("perception",  0);
                                    double rai = ag.cognitive.moyennes.getOrDefault("raisonnement",0);
                                    String cogPart = String.format(Locale.FRANCE,
                                            "🧠 %.0f%%  👁 %.0f%%  🧩 %.0f%%",
                                            mem, per, rai);
                                    result = String.join("   ", facePart, vocPart, cogPart);
                                }
                                else {
                                    String tmp = child.child("resultat").getValue(String.class);
                                    result = tmp != null ? tmp : "Aucun résultat";
                                }

                                cnt++;
                                addAnalyseCard(cnt, true, friendlyName(node), result, accentColor(node), iconRes(node), node, child.getKey(), analyse);
                            }
                            counts.put(node, cnt);
                            totalAnalyses += cnt;

                            totalAAnalyser--;
                            Log.d("Firebase", "Total " + node + " = " + cnt);
                            updateChart(counts);
                            if (totalAAnalyser == 0) {
                                if (totalAnalyses == 0) {
                                    affichageSansAnalyse();
                                }
                            }
                        }
                        @Override public void onCancelled(DatabaseError e) {
                            Log.e("Firebase", "Erreur sur " + node + ": " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Affiche une vue vide quand aucune analyse n'est disponible pour la date donnée.
     */


    private void affichageSansAnalyse() {
        chart.setVisibility(View.GONE);
        container.setVisibility(View.GONE);

        LinearLayout root = findViewById(R.id.rootLayout);
        View empty = getLayoutInflater().inflate(R.layout.historique_details_vide, root, false);
        root.addView(empty);

        ImageView img = empty.findViewById(R.id.telescope);
        if (img != null) {

            Glide.with(this)
                    .asGif()
                    .load(R.drawable.telescope)
                    .into(img);
        } else {
            Log.e("EmptyState", "ImageView imgTelescope introuvable dans empty_state !");
        }
    }

    /**
     * Met à jour le graphique avec les données agrégées récupérées.
     *
     * @param counts une map contenant les totaux pour chaque type d’analyse
     */
    private void updateChart(Map<String, Integer> counts) {
        Log.d("Chart", "updateChart with counts: " + counts);
        List<BarEntry> donnees = Arrays.asList(
                new BarEntry(0f, counts.get("analysesFaciale")),
                new BarEntry(1f, counts.get("analysesVocale")),
                new BarEntry(2f, counts.get("analysesCognitive")),
                new BarEntry(3f, counts.get("analysesGlobale"))
        );
        BarDataSet set = new BarDataSet(donnees, "Analyses du " + date);
        BarData data = new BarData(set);
        data.setBarWidth(0.9f);
        chart.setData(data);
        chart.invalidate();
    }

    /**
     * Convertit un nom de nœud Firebase en une étiquette lisible pour l'utilisateur.
     *
     * @param val le nom du nœud (ex: analysesFaciale)
     * @return le nom lisible (ex: Faciale)
     */
    private String friendlyName(String val) {
        switch (val) {
            case "analysesFaciale": return "Faciale";
            case "analysesVocale": return "Vocale";
            case "analysesCognitive": return "Cognitive";
            case "analysesGlobale": return "Globale";
            default:  return "";
        }
    }

    /**
     * Retourne une couleur d'accent associée à un type d’analyse.
     *
     * @param val le nom du nœud d’analyse
     * @return la ressource de couleur associée
     */

    @ColorRes
    private int accentColor(String val) {
        switch (val) {
            case "analysesFaciale": return R.color.teal_700;
            case "analysesVocale": return R.color.purple_700;
            case "analysesCognitive": return R.color.holo_orange_dark;
            case "analysesGlobale": return R.color.holo_blue_dark;
            default: return R.color.black;
        }
    }

    /**
     * Retourne l'icône correspondant à un type d’analyse.
     *
     * @param val le nom du nœud d’analyse
     * @return la ressource de l’image drawable associée
     */

    @DrawableRes
    private int iconRes(String val) {
        switch (val) {
            case "analysesFaciale": return R.drawable.analyse_faciale;
            case "analysesVocale": return R.drawable.analyse_vocale;
            case "analysesCognitive": return R.drawable.analyse_cognitive;
            case "analysesGlobale": return R.drawable.analyse_complete;
            default: return R.drawable.cadenas;
        }
    }

    /**
     * Ajoute dynamiquement une carte représentant une analyse à l’interface utilisateur.
     *
     * @param ordre       numéro d'ordre de l’analyse
     * @param present     si l’analyse doit être affichée
     * @param type        type d’analyse (faciale, vocale, etc.)
     * @param result      résumé des résultats
     * @param accentColor couleur d'accent à utiliser
     * @param iconRes     icône représentant l’analyse
     */

    private void addAnalyseCard(int ordre, boolean present, String type, String result, @ColorRes int accentColor, @DrawableRes int iconRes, String node, String childKey, Analyse analyse) {
        if (!present) return;
        View card = getLayoutInflater().inflate(R.layout.card_analyse, container, false);
        
        View bar = card.findViewById(R.id.accentColor);
        TextView tvOrd = card.findViewById(R.id.ordreAnalyses);
        TextView tvT = card.findViewById(R.id.detailAnalyse);
        TextView tvR = card.findViewById(R.id.resultatBrefAnalyse);
        ImageButton btnPdf = card.findViewById(R.id.pdf);
        ImageButton btnDelete = card.findViewById(R.id.effacer);

        bar.setBackgroundResource(accentColor);
        tvOrd.setText(String.valueOf(ordre));
        tvT.setText(type);
        tvR.setText(result);


        btnPdf.setOnClickListener(v -> {
            File pdfFile;
            switch (node) {
                case "analysesFaciale":
                    AnalyseFaciale af = (AnalyseFaciale) analyse;
                    pdfFile = ClasseUtilitaire.buildPdfFaciale( DetailsAnalysesHistorique.this, prenom, nom, email, af.date, af.emotionDominante, af.moyennes);
                    break;
                case "analysesVocale":
                    AnalyseVocale av = (AnalyseVocale) analyse;
                    pdfFile = ClasseUtilitaire.buildPdfVocale(DetailsAnalysesHistorique.this, prenom, nom, email, av.date, av.emotionDominante, av.moyennes);
                    break;
                case "analysesCognitive":
                    AnalyseCognitive ac = (AnalyseCognitive) analyse;
                    pdfFile = ClasseUtilitaire.buildPdfCognitive(DetailsAnalysesHistorique.this, prenom, nom, email,ac.date, ac.moyennes);
                    break;
                case "analysesGlobale":
                    AnalyseGlobale ag = (AnalyseGlobale) analyse;
                    pdfFile = ClasseUtilitaire.buildPdfComplete(DetailsAnalysesHistorique.this, prenom, nom, email, ag.date, ag.faciale.emotionDominante, ag.faciale.moyennes, ag.vocale.emotionDominante, ag.vocale.moyennes, ag.cognitive.moyennes);
                    break;
                default:
                    Toast.makeText(this, "Type inconnu", Toast.LENGTH_SHORT).show();
                    return;
                    }




            new AlertDialog.Builder(this)
                    .setTitle("Votre rapport est prêt")
                    .setMessage("Voulez-vous sauvegarder votre rapport dans Téléchargements?")
                    .setNegativeButton("Télécharger", (d, which) ->
                            telechargementPdf(pdfFile, pdfFile.getName())
                    )
                    .setNeutralButton("Annuler", null)
                    .show();
        });




        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer")
                    .setMessage("Voulez-vous vraiment supprimer cette analyse ?")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Supprimer", (d, which) -> {
                        utilisateurRef
                                .child(node)
                                .child(childKey)
                                .removeValue()
                                .addOnSuccessListener(a -> {
                                    container.removeView(card);
                                    Toast.makeText(this, "Analyse supprimée", Toast.LENGTH_SHORT).show();
                                    mAJ(node);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Échec suppression : " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .show();
        });
        container.addView(card);
    }

    private void mAJ(String node) {
        int index;
        switch (node) {
            case "analysesFaciale":   index = 0; break;
            case "analysesVocale":    index = 1; break;
            case "analysesCognitive": index = 2; break;
            case "analysesGlobale":   index = 3; break;
            default:                  return;
        }

        BarData data = chart.getData();
        if (data == null) return;
        BarDataSet set = (BarDataSet) data.getDataSetByIndex(0);
        if (set == null) return;

        BarEntry entry = set.getEntryForIndex(index);
        if (entry == null) return;
        float newY = Math.max(0, entry.getY() - 1);
        entry.setY(newY);

        set.notifyDataSetChanged();
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();

        totalAnalyses = Math.max(0, totalAnalyses - 1);
    }

private void telechargementPdf(File sourceFile, String displayName) {
    ContentValues values = new ContentValues();
    values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
    values.put(MediaStore.Downloads.IS_PENDING, 1);

    ContentResolver resolver = getContentResolver();
    Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    Uri destUri = resolver.insert(collection, values);
    if (destUri == null) {
        Toast.makeText(this, "Erreur lors de la création du fichier dans Téléchargements", Toast.LENGTH_LONG).show();
        return;
    }

    try (InputStream in = new FileInputStream(sourceFile);
         OutputStream out = resolver.openOutputStream(destUri)) {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "Échec du téléchargement  " + e.getMessage(), Toast.LENGTH_LONG).show();
        return;
    }

    values.clear();
    values.put(MediaStore.Downloads.IS_PENDING, 0);
    resolver.update(destUri, values, null, null);

    Toast.makeText(this, "PDF enregistré dans Téléchargements", Toast.LENGTH_LONG).show();
}

    interface Analyse { String getDate(); }

        public static class AnalyseFaciale implements Analyse {
            public String date;
            public String emotionDominante;
            public List<Double> moyennes;
            @Override public String getDate() { return date; }
        }

        public static class AnalyseVocale implements Analyse {
            public String date;
            public String emotionDominante;
            public List<Double> moyennes;
            @Override public String getDate() { return date; }
        }

    public static class AnalyseCognitive implements Analyse {
            public String date;
            public Map<String, Integer> moyennes;
            @Override public String getDate() { return date; }
        }

    public static class AnalyseGlobale implements Analyse {
            public String date;
            public AnalyseFaciale  faciale;
            public AnalyseVocale vocale;
            public AnalyseCognitive cognitive;
            @Override public String getDate() { return date; }
        }


}
