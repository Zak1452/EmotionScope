package com.example.emotionscope;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activité qui gère l'analyse cognitive de l'utilisateur à travers trois mini-jeux :
 * mémoire, perception et raisonnement. Affiche un résumé des scores
 * et permet d'enregistrer les résultats dans Firebase.
 * 
 * Cette activité attend les données exterieures suivantes:
 * - "uid": identifiant utilisateur
 * - "prenom": prénom de l'utilisateur
 * - "nom": nom de l'utilisateur
 * - "email": email de l'utilisateur
 *
 * Les résultats sont ensuite enregistrés dans Firebase à l'issue des 3 tests.
 * @author: CHAKER Zakaria
 */

public class AnalyseCognitiveActivity extends AppCompatActivity {

    private static final int REQ_MEMOIRE = 100;
    private static final int REQ_PERCEPTION = 101;
    private static final int REQ_RAISONNEMENT  = 102;

    private Button btnMemoire;
    private Button btnPerception;
    private Button btnRaisonnement;
    private CardView cardMemoire;
    private CardView cardPerception;
    private CardView cardRaisonnement;
    private ImageButton btnMenuPrincipal, btnSuivantScreen;
    private LinearLayout layoutContinuer;

    private boolean doneMemoire = false, donePerception = false, doneRaisonnement = false,
    isMultiStep = false, analyseTerminee = false;

    private static final int TOTAL_MEMOIRE = 3;
    private static final int TOTAL_PERCEPTION = 5;
    private static final int TOTAL_RAISONNEMENT = 3;

    private int memScore = -1, percScore = -1, raisScore = -1;

    private String uid, prenom, nom, email;
    private FirebaseDatabase database;
    private DatabaseReference utilisateursRef;

    /**
     * Méthode appelée à la création de l'activité.
     * Initialise l'interface utilisateur, les références Firebase, et les listeners.
     * Donc, si il y a déjà eu une 1ère connexion de la part de l'utilisateur.
     *
     * @param savedInstanceState L'état précédemment sauvegardé de l'activité, s’il existe.
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_cognitive_activity);

        Intent instance = getIntent();
        uid = instance.getStringExtra("uid");
        prenom = instance.getStringExtra("prenom");
        nom = instance.getStringExtra("nom");
        email = instance.getStringExtra("email");
        isMultiStep = instance.getBooleanExtra("isMultiStep", false);

        if (uid == null || prenom == null || nom == null || email == null) {
            Toast.makeText(this, "Erreur : Informations utilisateur manquantes.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        database  = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
        utilisateursRef = database.getReference("utilisateurs");

        btnMemoire = findViewById(R.id.btnMemory);
        btnPerception = findViewById(R.id.btnPerception);
        btnRaisonnement = findViewById(R.id.btnReasoning);
        btnMenuPrincipal = findViewById(R.id.btn_menu_principal);

        cardMemoire = findViewById(R.id.cardMemoire);
        cardPerception = findViewById(R.id.cardPerception);
        cardRaisonnement = findViewById(R.id.cardRaisonnement);

        btnMemoire.setOnClickListener(v -> launchTest(MemoireTestActivity.class, REQ_MEMOIRE));
        btnPerception.setOnClickListener(v -> launchTest(PerceptionTestActivity.class, REQ_PERCEPTION));
        btnRaisonnement.setOnClickListener(v -> launchTest(RaisonnementTestActivity.class, REQ_RAISONNEMENT));
        layoutContinuer = findViewById(R.id.layout_continuer);
        layoutContinuer.setVisibility(View.GONE);
        btnSuivantScreen = findViewById(R.id.btn_continuer);
        btnMenuPrincipal.setOnClickListener(v -> {
            if (isMultiStep)
            {
                confirmationQuitterAnalyse();
            }
            else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        btnSuivantScreen.setOnClickListener(v ->{
            if(analyseTerminee && memScore != -1 && percScore != -1 && raisScore !=-1)
            {
                affichageBoiteDialogueResultats();
            }
        });
    }

     /**
     * Lance l'activité de test cognitif en fonction de la classe passée.
     *
     * @param activityClass La classe de l'activité à lancer.
     * @param requestCode   Le code de requête utilisé pour identifier la réponse.
     */
    private void launchTest(Class<?> activityClass, int requestCode) {
        Intent intent = new Intent(this, activityClass);
        startActivityForResult(intent, requestCode);
    }

    /**
     * Récupère les résultats renvoyés par les mini-jeux et met à jour l'état interne.
     *
     *
     * @param requestCode Code de requête envoyé à l'activité.
     * @param resultCode  Code de résultat renvoyé par l'activité.
     * @param data Données renvoyées par l'activité.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_MEMOIRE:
                    markDone(cardMemoire, btnMemoire);
                    doneMemoire = true;
                    this.memScore = data.getIntExtra("MEMORY_SCORE", 0);
                    break;
                case REQ_PERCEPTION:
                    markDone(cardPerception, btnPerception);
                    donePerception = true;
                    this.percScore = data.getIntExtra("PERCEPTION_SCORE", 0);
                    break;
                case REQ_RAISONNEMENT:
                    markDone(cardRaisonnement, btnRaisonnement);
                    doneRaisonnement = true;
                    this.raisScore = data.getIntExtra("RAISONNEMENT_SCORE", 0);
                    break;
            }
            checkAllDone();
        }
    }

    /**
     * Marque un test comme terminé visuellement :
     * - désactive le bouton correspondant
     * - colore en vert
     *
     * @param card La carte du test concerné.
     * @param btn  Le bouton du test concerné.
     */
    private void markDone(CardView card, Button btn) {
        btn.setEnabled(false);
        card.setCardBackgroundColor(Color.parseColor("#C8E6C9"));
    }

    /**
     * Vérifie si tous les tests (mémoire, perception, raisonnement) sont terminés.
     * Si c'est le cas, affiche un résumé sous forme de boîte de dialogue.
     *
     */
    private void checkAllDone() {
        if (doneMemoire && donePerception && doneRaisonnement) {
            layoutContinuer.setVisibility(View.VISIBLE);
            analyseTerminee = true;
            affichageBoiteDialogueResultats();
        }
    }

    /**
     * Affiche une boîte de dialogue contenant les résultats des tests cognitifs.
     * Permet à l'utilisateur de:
     * - Fermer et revenir au menu principal
     * - Enregistrer les résultats dans Firebase
     * - Télécharger les résultats (non implémenté)
     */


    private void affichageBoiteDialogueResultats() {
        int pctM = this.memScore * 100 / TOTAL_MEMOIRE;
        int pctP = this.percScore * 100 / TOTAL_PERCEPTION;
        int pctR = this.raisScore * 100 / TOTAL_RAISONNEMENT;

        View dialogView = getLayoutInflater().inflate(R.layout.boite_dialogue_resultats_analyse_cognitive, null);

        TextView tvMemoire   = dialogView.findViewById(R.id.tvScoreMempire);
        TextView tvPerception   = dialogView.findViewById(R.id.tvScorePerception);
        TextView tvRaisonnement  = dialogView.findViewById(R.id.tvScoreRaisonnement);
        ImageButton btnFermer = dialogView.findViewById(R.id.btnFermer);
        ImageButton btnEnregistrer = dialogView.findViewById(R.id.btnEnregistrer);
        ImageButton btnTelecharger = dialogView.findViewById(R.id.btnTelecharger);

        LinearLayout layoutFermer = dialogView.findViewById(R.id.layout_fermer);
        LinearLayout layoutTelecharger = dialogView.findViewById(R.id.layout_telecharger);
        LinearLayout layoutEnregistrer = dialogView.findViewById(R.id.layout_enregistrer);


        LinearLayout etape_suiv = dialogView.findViewById(R.id.etape_suivante);

        tvMemoire.setText("🧠 Mémoire : " + pctM + " %");
        tvPerception.setText("👁️ Perception : " + pctP + " %");
        tvRaisonnement.setText("🧩 Raisonnement : " + pctR + " %");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (isMultiStep) {
            layoutEnregistrer.setVisibility(View.GONE);
            layoutTelecharger.setVisibility(View.GONE);
            layoutFermer.setVisibility(View.GONE);
            etape_suiv.setVisibility(View.VISIBLE);
            etape_suiv.setOnClickListener(v -> {
                HashMap<String, Integer> detailCogMap = new HashMap<>();
                detailCogMap.put("memoire", pctM);
                detailCogMap.put("perception", pctP);
                detailCogMap.put("raisonnement", pctR);
                Intent result = new Intent();
                result.putExtra("detailsAnalyseCognitive", detailCogMap);
                setResult(RESULT_OK, result);
                dialog.dismiss();
                finish();
            });

        }
        else {
            etape_suiv.setVisibility(View.GONE);
            btnEnregistrer.setOnClickListener(v -> {
                if (uid == null || uid.isEmpty() || email == null || email.isEmpty()) {
                    Toast.makeText(this, "UID ou email manquant. Impossible d'enregistrer les résultats.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                Map<String, Object> moyennes = new HashMap<>();
                moyennes.put("memoire", pctM);
                moyennes.put("perception", pctP);
                moyennes.put("raisonnement", pctR);

                Map<String, Object> analyseData = new HashMap<>();
                analyseData.put("date", date);
                analyseData.put("moyennes", moyennes);

                DatabaseReference userAnalysesRef = utilisateursRef
                        .child(uid)
                        .child("analysesCognitive");

                String analysisId = userAnalysesRef.push().getKey();
                if (analysisId == null) {
                    Toast.makeText(this,
                            "Erreur: impossible de générer l’ID de l’analyse.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                userAnalysesRef.child(analysisId)
                        .setValue(analyseData)
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this,
                                    "Résultats enregistrés avec succès !",
                                    Toast.LENGTH_SHORT).show();
                            btnEnregistrer.setEnabled(false);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this,
                                    "Erreur lors de l'enregistrement : " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            });

            btnFermer.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
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
                int logoWidth = 40;
                int logoHeight = 040;
                int logoX = x;
                int logoY = y - 30;

                canvas.drawBitmap(Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, false), logoX, logoY, paint);

                paint.setTextSize(30);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                canvas.drawText(" Emotion Scope  ", logoX + logoWidth + 10, y, paint); // Décale le te

                paint.setStrokeWidth(2);
                canvas.drawLine(x, y + 12, pageInfo.getPageWidth() - x, y + 12, paint);

                paint.setStrokeWidth(0);
                paint.setTextSize(18);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                y += 40;
                canvas.drawText("Votre rapport d'analyses", x, y, paint);

                canvas.drawText("", x, y, paint);
                paint.setTextSize(16);
                y += 30;
                canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
                y += 25;
                canvas.drawText("Email : " + email, x, y, paint);

                paint.setTextSize(18);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                y += 40;
                canvas.drawText("Analyse cognitive", x, y, paint);

                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(16);
                y += 30;
                canvas.drawText("🧠 Mémoire    : " + pctM + " %", x, y, paint);
                y += 25;
                canvas.drawText("👁️ Perception: " + pctP + " %", x, y, paint);
                y += 25;
                canvas.drawText("🧩 Raisonnement: " + pctR + " %", x, y, paint);

                paint.setTextSize(12);
                y += 40;
                canvas.drawText("Date : " + dateStr, x, y, paint);

                pdf.finishPage(page);

                String fileName = "EmotionScope_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + "_" + (nom).toUpperCase() + ".pdf";
                File cacheFile = new File(getCacheDir(), fileName);
                try (FileOutputStream out = new FileOutputStream(cacheFile)) {
                    pdf.writeTo(out);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erreur PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                        .setNegativeButton("Télécharger", (d, which) -> {
                            telechargementPdf(cacheFile, fileName);
                        })
                        .setNeutralButton("Annuler", null)
                        .show();
            });
        }

        dialog.show();
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

    private void confirmationQuitterAnalyse()
    {
        new AlertDialog.Builder(this)
                .setTitle("Quitter l’analyse")
                .setIcon(R.drawable.danger)
                .setMessage("Voulez‑vous vraiment arrêter l’analyse globale et retourner au menu principal?\n\n" +
                        "Les données en cours ne seront pas enregistrées et seront définitivement supprimées.")
                .setPositiveButton("Oui", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Non", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void onBackPressed() {
        if(isMultiStep)
            confirmationQuitterAnalyse();
    }
}
