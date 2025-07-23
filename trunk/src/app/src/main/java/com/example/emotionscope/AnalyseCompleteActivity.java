package com.example.emotionscope;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Classe qui permet de gerer l'analyse complète. Cette classe joue le role de "chef d'orchestre" elle permet de lancer chaque analyse.
 * Cette classe récupère les résultats des analyses, stocke les résultats à la fois.
 * Et à la fin on exploite ces données, soit e telechargeanet un rapport global, soit en enregistrant dans la BD.
 *
 * @author CHAKER Zakaria
 */
public class AnalyseCompleteActivity extends AppCompatActivity {

    private static final int REQ_FACIAL = 1001;
    private static final int REQ_VOCALE = 1002;
    private static final int REQ_COGNITIVE = 1003;
    private String uid, prenom, nom, email;
    private FirebaseDatabase database;
    private DatabaseReference utilisateursRef;

    private String emotionFacialeDominante = null, emotionVocaleDominante = "Angry";
    private Map<String, Double> detailsEmotionsFaciales = null, detailsEmotionsVocales = null;
    private List<Double> moyennesF, moyennesV;
    private HashMap<String, Integer> detailsAnalyseCognitive;
    private List<Integer> moyennesC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent in = getIntent();
        uid    = in.getStringExtra("uid");
        prenom = in.getStringExtra("prenom");
        nom    = in.getStringExtra("nom");
        email  = in.getStringExtra("email");

        if (uid == null || prenom == null || nom == null || email == null) {
            Toast.makeText(this, "Infos utilisateur manquantes.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        database  = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
        utilisateursRef = database.getReference("utilisateurs");

        lancementAnalyseFaciale();

    }

    private void lancementAnalyseFaciale() {
        Intent intent = new Intent(this, AnalyseFacialeActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("prenom", prenom);
        intent.putExtra("nom", nom);
        intent.putExtra("email", email);
        intent.putExtra("isMultiStep", true);
        startActivityForResult(intent, REQ_FACIAL);
    }

    private void lancementAnalyseVocale() {

       Intent intent = new Intent(this, AnalyseVocaleActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("prenom", prenom);
        intent.putExtra("nom", nom);
        intent.putExtra("email", email);
        intent.putExtra("isMultiStep", true);
        startActivityForResult(intent, REQ_VOCALE);


    }

    private void lancementAnalyseCognitive()
    {
        Intent intent = new Intent(this, AnalyseCognitiveActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("prenom", prenom);
        intent.putExtra("nom", nom);
        intent.putExtra("email", email);
        intent.putExtra("isMultiStep", true);
        startActivityForResult(intent, REQ_COGNITIVE);
    }


    @Override
    protected void onActivityResult(int codeRequis, int codeResultat, Intent data) {
        super.onActivityResult(codeRequis, codeResultat, data);
        if (codeRequis == REQ_FACIAL && codeResultat == RESULT_OK) {
            this.emotionFacialeDominante = data.getStringExtra("emotionFinaleFaciale");
            @SuppressWarnings("unchecked")
            HashMap<String, Double> detailMap =
                    (HashMap<String, Double>) data.getSerializableExtra("detailsEmotionsFaciales");
            this.detailsEmotionsFaciales = detailMap;

            if (detailsEmotionsFaciales != null) {
                moyennesF = new ArrayList<>(detailsEmotionsFaciales.values());
            }

            lancementAnalyseVocale();
        }
        else if (codeRequis == REQ_VOCALE) {
            this.emotionVocaleDominante = data.getStringExtra("emotionFinaleVocale");

            @SuppressWarnings("unchecked")
            HashMap<String, Double> detailMap =
                    (HashMap<String, Double>) data.getSerializableExtra("detailsEmotionsVocales");

            if (detailMap != null) {
                detailsEmotionsVocales = detailMap;

                List<String> labelsV = Arrays.asList(
                        "Angry", "Fear", "Disgust",
                        "Happy", "Sad", "Surprised", "Neutral"
                );
                moyennesV = new ArrayList<>();
                for (String label : labelsV) {
                    Double v = detailMap.get(label);
                    moyennesV.add(v != null ? v : 0.0);
                }
            }

            lancementAnalyseCognitive();
        }


        else if (codeRequis == REQ_COGNITIVE) {
            @SuppressWarnings("unchecked")
            HashMap<String, Integer> detailCogMap =
                    (HashMap<String, Integer>) data.getSerializableExtra("detailsAnalyseCognitive");
            this.detailsAnalyseCognitive = detailCogMap;

            if (detailsAnalyseCognitive != null) {
                moyennesC= new ArrayList<>(detailsAnalyseCognitive.values());
            }
            afficherResultatsAnalyseComplete();
        }
    }

    private void afficherResultatsAnalyseComplete()
    {
        setContentView(R.layout.boite_dialogue_resultats_analyse_complete);

        TextView resultatsAnalyseFaciale = findViewById(R.id.resultatsAnalyseFaciale);
        TextView resultatsAnalyseCognitive = findViewById(R.id.resultatsAnalyseCognitive);
        TextView resultatsAnalyseVocale = findViewById(R.id.resultatsAnalyseVocale);

        ImageButton btnEnregistrer = findViewById(R.id.btnEnregistrer);
        ImageButton btnTelecharger = findViewById(R.id.btnTelecharger);
        ImageButton btnFermer = findViewById(R.id.btnFermer);

        if (detailsEmotionsFaciales != null && emotionFacialeDominante != null) {
            Log.d("AnalyseFaciale", "Traitement des données faciales");
            StringBuilder sbFaciale = new StringBuilder();
            sbFaciale.append("Emotion dominante: ").append(emotionFacialeDominante).append("\nRépartition:\n");

            for (Map.Entry<String, Double> entry : detailsEmotionsFaciales.entrySet()) {
                sbFaciale.append(entry.getKey()).append(": ").append(entry.getValue()).append("%\n");
            }
            resultatsAnalyseFaciale.setText(sbFaciale.toString());
        }


        else {
            resultatsAnalyseFaciale.setText("Aucune donnée pour l'analyse faciale.");
        }

        if (detailsAnalyseCognitive != null) {
            StringBuilder sbCognitive = new StringBuilder();
            sbCognitive.append("Scores:\n");

            for (Map.Entry<String, Integer> entry : detailsAnalyseCognitive.entrySet()) {
                sbCognitive.append(entry.getKey()).append(": ").append(entry.getValue()).append("%\n");
            }
            resultatsAnalyseCognitive.setText(sbCognitive.toString());
        } else {
            resultatsAnalyseCognitive.setText("Aucune donnée pour l'analyse cognitive.");
        }

        if (detailsEmotionsVocales != null && !detailsEmotionsVocales.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Emotion dominante: ")
                    .append(emotionVocaleDominante)
                    .append("\n\nScores:\n");
            for (Map.Entry<String, Double> e : detailsEmotionsVocales.entrySet()) {
                sb.append("• ")
                        .append(e.getKey())
                        .append(" : ")
                        .append(String.format(Locale.FRANCE, "%.1f%%", e.getValue()))
                        .append("\n");
            }
            resultatsAnalyseVocale.setText(sb.toString().trim());
        } else {
            resultatsAnalyseVocale.setText("Aucune donnée pour l'analyse vocale.");
        }

        btnEnregistrer.setOnClickListener(v -> {
            Log.d("Enregistrement", "Bouton Enregistrer cliqué");
            if (uid == null || uid.isEmpty()) {
                Log.e("Enregistrement", "UID manquant");
                Toast.makeText(this, "UID manquant. Impossible d'enregistrer les résultats.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "Email manquant. Impossible d'enregistrer les résultats.", Toast.LENGTH_SHORT).show();
                return;
            }

            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            Map<String, Object> facialeObj = new HashMap<>();
            facialeObj.put("emotionDominante", emotionFacialeDominante);

            Map<String, Object> vocaleObj  = new HashMap<>();
            vocaleObj.put("emotionDominante", emotionVocaleDominante);


            Map<String, Double> mapF = new HashMap<>();
            for(int i = 0; i < moyennesF.size(); i++) {
                mapF.put(String.valueOf(i), moyennesF.get(i));
            }

            facialeObj.put("moyennes", mapF);

            Map<String, Double> mapV = new HashMap<>();
            for(int i = 0; i < moyennesV.size(); i++) {
                mapV.put(String.valueOf(i), moyennesV.get(i));
            }
            vocaleObj.put("moyennes", mapV);

            Map<String, Object> analyseData = new HashMap<>();
            analyseData.put("date", date);
            analyseData.put("faciale", facialeObj);
            analyseData.put("vocale", vocaleObj);

            Map<String, Object> cognitiveObj = new HashMap<>();
            Map<String, Integer> moyC = new HashMap<>();
            moyC.put("perception", detailsAnalyseCognitive.get("perception"));
            moyC.put("memoire",   detailsAnalyseCognitive.get("memoire"));
            moyC.put("raisonnement", detailsAnalyseCognitive.get("raisonnement"));
            cognitiveObj.put("moyennes", moyC);

            analyseData.put("cognitive", cognitiveObj);

            DatabaseReference userAnalysesRef = utilisateursRef
                    .child(uid)
                    .child("analysesGlobale");

            String analysisId = userAnalysesRef.push().getKey();

            if (analysisId != null) {
                if (moyennesF == null || moyennesF.size() != 7) {
                    Log.e("Enregistrement", "Données faciales incomplètes");
                    Toast.makeText(this, "Les données faciales ne sont pas complètes.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (moyennesV == null || moyennesV.size() != 7) {
                    Log.e("Enregistrement", "Données vocales incomplètes");
                    Toast.makeText(this, "Les données faciales ne sont pas complètes.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("Enregistrement", "AnalyseData : " + analyseData.toString());
                userAnalysesRef.child(analysisId)
                        .setValue(analyseData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Résultats enregistrés avec succès!", Toast.LENGTH_SHORT).show();
                            btnEnregistrer.setEnabled(false);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur lors de l'enregistrement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Erreur: Impossible de générer l'identifiant de l'analyse.", Toast.LENGTH_SHORT).show();
            }
        });

        btnTelecharger.setOnClickListener(v -> {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            PdfDocument pdf = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            int x = 40;
            int y = 60;

            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.planetes);
            canvas.drawBitmap(Bitmap.createScaledBitmap(logo, 40, 40, false), x, y - 30, paint);
            paint.setTextSize(30);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            canvas.drawText(" Emotion Scope ", x + 50, y, paint);

            paint.setStrokeWidth(2);
            canvas.drawLine(x, y + 12, pageInfo.getPageWidth() - x, y + 12, paint);

            paint.setStrokeWidth(0);
            paint.setTextSize(18);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            y += 40;
            canvas.drawText("Votre rapport d'analyses global", x, y, paint);

            paint.setTextSize(16);
            y += 30;
            canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
            y += 25;
            canvas.drawText("Email       : " + email, x, y, paint);

            paint.setTextSize(18);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            y += 40;
            canvas.drawText("Analyse faciale", x, y, paint);

            paint.setTextSize(16);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            y += 40;
            canvas.drawText(("Emotion dominante: " + emotionFacialeDominante), x, y, paint);
            paint.setTextSize(16);
            if (detailsEmotionsFaciales != null && !detailsEmotionsFaciales.isEmpty()) {
                for (Map.Entry<String, Double> entry : detailsEmotionsFaciales.entrySet()) {
                    y += 25;
                    canvas.drawText(entry.getKey() + " : " + String.format(Locale.FRANCE, "%.1f", entry.getValue()) + " %", x, y, paint);
                }
            } else {
                y += 25;
                canvas.drawText("Aucune donnée pour l'analyse faciale.", x, y, paint);
            }

            paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            y += 40;
            canvas.drawText("Analyse Vocale", x, y, paint);

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            y += 40;
            canvas.drawText(("Emotion dominante: " + emotionVocaleDominante), x, y, paint);
            paint.setTextSize(16);
            if (detailsEmotionsVocales != null && !detailsEmotionsVocales.isEmpty()) {
                for (Map.Entry<String, Double> entry : detailsEmotionsVocales.entrySet()) {
                    y += 25;
                    canvas.drawText(entry.getKey() + " : " + String.format(Locale.FRANCE, "%.1f", entry.getValue()) + " %", x, y, paint);
                }
            } else {
                y += 25;
                canvas.drawText("Aucune donnée pour l'analyse vocale.", x, y, paint);
            }

            paint.setTextSize(18);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            y += 40;
            canvas.drawText("Analyse cognitive", x, y, paint);

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextSize(16);
            if (moyennesC != null && moyennesC.size() == 3) {
                y += 30;
                canvas.drawText("🧠 Mémoire    : " + moyennesC.get(0) + " %", x, y, paint);
                y += 25;
                canvas.drawText("👁️ Perception: " + moyennesC.get(1) + " %", x, y, paint);
                y += 25;
                canvas.drawText("🧩 Raisonnement: " + moyennesC.get(2) + " %", x, y, paint);
            } else {
                y += 25;
                canvas.drawText("Aucune donnée pour l'analyse cognitive.", x, y, paint);
            }

            paint.setTextSize(12);
            y += 40;
            canvas.drawText("Date : " + dateStr, x, y, paint);

            pdf.finishPage(page);

            String fileName = "EmotionScope_Analyse_Complete" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                    + "_" + nom.toUpperCase() + ".pdf";
            File cacheFile = new File(getCacheDir(), fileName);
            try (FileOutputStream out = new FileOutputStream(cacheFile)) {
                pdf.writeTo(out);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erreur PDF : " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            } finally {
                pdf.close();
            }

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    cacheFile
            );

            new AlertDialog.Builder(this)
                    .setTitle("Votre rapport est prêt")
                    .setMessage("Voulez-vous sauvegarder votre rapport dans Téléchargements?")
                    .setNegativeButton("Télécharger", (d, which) ->
                            telechargementPdf(cacheFile, fileName)
                    )
                    .setNeutralButton("Annuler", null)
                    .show();
        });

        btnFermer.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

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



}
