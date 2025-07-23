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
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.FileProvider;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * L'objectif de cette classe est de lancer une analyse vocale puis de l'analyser grâce à un code Python
 * et d'afficher les résultats.
 * @author : CHABANE CHAOURAR Ahmed-Lamin
 */

public class AnalyseVocaleActivity extends AppCompatActivity {

    private static final String[] EMOTION_LABELS = {
            "Colère", "Peur", "Degoût", "Joie", "Tristesse", "Surprise", "Neutre"
    };

    private MediaRecorder mediaRecorder;
    private String currentAudioPath;
    private ProgressBar progressBar;
    private TextView resultTextView;
    private String uid, prenom , nom , email, textRes;
    private List<Double> emotions;
    private boolean isMultiStep = false, analyseTerminee = false;
    ImageButton btnSuivantScreen, btnMenuPrincipal;
    private LinearLayout layoutContinuer;

    private DatabaseReference utilisateursRef;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocal_analysis);

        // Récupération des infos utilisateur
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");
        prenom = intent.getStringExtra("prenom");
        nom = intent.getStringExtra("nom");
        email = intent.getStringExtra("email");
        isMultiStep = intent.getBooleanExtra("isMultiStep", false);

        if (uid == null || prenom == null || nom == null || email == null) {
            Toast.makeText(this, "Erreur : Informations utilisateur manquantes.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        //Initialisation Firebase
        database = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
        utilisateursRef = database.getReference("utilisateurs");

        //Initialisation Python
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        copyModelToExternalStorage();

        // UI Initialization
        progressBar = findViewById(R.id.progressBar);
        resultTextView = findViewById(R.id.resultTextView);
        Button recordBtn = findViewById(R.id.recordButton);
        Button stopBtn = findViewById(R.id.stopButton);
        Button analyzeBtn = findViewById(R.id.analyzeButton);
        Button loadBtn = findViewById(R.id.loadFileButton);

        layoutContinuer = findViewById(R.id.layout_continuer);
        layoutContinuer.setVisibility(View.GONE);
        btnSuivantScreen = findViewById(R.id.btn_continuer);
        btnMenuPrincipal = findViewById(R.id.btn_menu_principal);
        btnMenuPrincipal.setOnClickListener(v -> {
                    if (isMultiStep) {
                        confirmationQuitterAnalyse();
                    } else {
                        Intent instance = new Intent(this, MainActivity.class);
                        instance.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(instance);
                        finish();
                    }
                });


        btnSuivantScreen.setOnClickListener(v ->{
            if(analyseTerminee)
            {
                afficherBoiteDeDialogueResultatsVocales(textRes, emotions);
            }
        });
        recordBtn.setOnClickListener(v -> startRecording());
        stopBtn.setOnClickListener(v -> stopRecording());
        analyzeBtn.setOnClickListener(v -> analyzeAudio());
        loadBtn.setOnClickListener(v -> loadAudioFile());
    }

    private void startRecording() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File audioFile = File.createTempFile("AUDIO_" + timeStamp + "_", ".wav", storageDir);
            currentAudioPath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(currentAudioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            Toast.makeText(this, "Enregistrement démarré", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("AudioRecord", "Échec de l'enregistrement", e);
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Toast.makeText(this, "Enregistrement terminé", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try {
                File tempFile = new File(getCacheDir(), "temp_audio.wav");
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();

                currentAudioPath = tempFile.getAbsolutePath();
                Toast.makeText(this, "Fichier chargé", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeAudio() {
        if (currentAudioPath == null || currentAudioPath.isEmpty()) {
            Toast.makeText(this, "Aucun audio à analyser", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("Conversion en cours...");

        File wavFile = new File(getExternalFilesDir(null), "converted.wav");
        String wavPath = wavFile.getAbsolutePath();

        String cmd = String.format("-y -i \"%s\" -ar 22050 -ac 1 -f wav \"%s\"", currentAudioPath, wavPath);

        FFmpegKit.executeAsync(cmd, session -> {
            if (session.getReturnCode().isValueSuccess()) {
                runOnUiThread(() -> resultTextView.setText("Analyse en cours..."));

                new Thread(() -> {
                    try {
                        File modelFile = new File(getExternalFilesDir(null), "modele_converti.tflite");
                        String modelPath = modelFile.getAbsolutePath();

                        Python py = Python.getInstance();
                        PyObject result = py.getModule("audio_analyzer")
                                .callAttr("analyze_audio", wavPath, modelPath);

                        List<Double> emotionScores = new ArrayList<>();
                        for (PyObject item : result.asList()) {
                            emotionScores.add(item.toDouble());
                        }

                        StringBuilder resultTextBuilder = new StringBuilder();
                        for (int i = 0; i < emotionScores.size(); i++) {
                            resultTextBuilder.append(String.format(Locale.FRENCH,
                                    "%s: %.1f%%\n",
                                    EMOTION_LABELS[i],
                                    emotionScores.get(i) * 100));
                        }

                        final String resultText = resultTextBuilder.toString().trim();
                        this.textRes = resultText;
                        this.emotions = emotionScores;

                        runOnUiThread(() -> {
                            resultTextView.setText(resultText);
                            progressBar.setVisibility(View.GONE);
                            analyseTerminee = true;
                            layoutContinuer.setVisibility(View.VISIBLE);
                            afficherBoiteDeDialogueResultatsVocales(resultText, emotionScores);
                        });

                    } catch (Exception e) {
                        Log.e("AnalyseAudio", "Erreur analyse", e);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Erreur d'analyse", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                }).start();

            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Erreur de conversion audio", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void afficherBoiteDeDialogueResultatsVocales(String texteResultats, List<Double> emotionScores) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.boite_dialogue_resultats_analyse_vocale, null);

        TextView titreResultats = dialogView.findViewById(R.id.titreResultats);
        TextView emotionDominante = dialogView.findViewById(R.id.emotionDominante);
        TextView emotionsMoyennes = dialogView.findViewById(R.id.emotionsMoyennes);

        ImageButton btnTelecharger = dialogView.findViewById(R.id.btnTelecharger);
        LinearLayout layoutFermer = dialogView.findViewById(R.id.layout_fermer);
        LinearLayout layoutTelecharger = dialogView.findViewById(R.id.layout_telecharger);
        LinearLayout layoutEnregistrer = dialogView.findViewById(R.id.layout_enregistrer);

        LinearLayout etape_suiv = dialogView.findViewById(R.id.etape_suivante);

        // Trouver l'émotion dominante
        int maxIndex = 0;
        for (int i = 1; i < emotionScores.size(); i++) {
            if (emotionScores.get(i) > emotionScores.get(maxIndex)) {
                maxIndex = i;
            }
        }
        String dominantEmotion = EMOTION_LABELS[maxIndex];

        HashMap<String, Double> detailMapPourcent = new HashMap<>();
        for (int i = 0; i < EMOTION_LABELS.length; i++) {
            double pct = emotionScores.get(i) * 100.0;
            pct = Math.round(pct * 10.0) / 10.0;
            detailMapPourcent.put(EMOTION_LABELS[i], pct);
        }

        titreResultats.setText("✨ Résultats de votre analyse ✨");
        emotionDominante.setText("Emotion Dominante: " + dominantEmotion);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : detailMapPourcent.entrySet()) {
            sb.append("• ").append(e.getKey())
                    .append(" : ")
                    .append(String.format(Locale.FRANCE, "%.1f%%", e.getValue()))
                    .append("\n");
        }
        emotionsMoyennes.setText(sb.toString().trim());

        // Boutons
        AppCompatImageButton btnFermer = dialogView.findViewById(R.id.btnFermer);
        AppCompatImageButton btnEnregistrer = dialogView.findViewById(R.id.btnEnregistrer);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();


        if (isMultiStep) {
            layoutEnregistrer.setVisibility(View.GONE);
            layoutTelecharger.setVisibility(View.GONE);
            layoutFermer.setVisibility(View.GONE);
            etape_suiv.setVisibility(View.VISIBLE);
            etape_suiv.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra("emotionFinaleVocale", dominantEmotion);
                result.putExtra("detailsEmotionsVocales", detailMapPourcent);
                setResult(RESULT_OK, result);
                alertDialog.dismiss();
                finish();
            });
        } else {
            etape_suiv.setVisibility(View.GONE);
            btnFermer.setOnClickListener(v -> alertDialog.dismiss());
            btnEnregistrer.setOnClickListener(v -> {
                        enregistrerAnalyseVocale(emotionScores);
                        alertDialog.dismiss();
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
                canvas.drawText("Votre rapport d'analyses", x, y, paint);

                paint.setTextSize(16);
                y += 30;
                canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
                y += 25;
                canvas.drawText("Email : " + email, x, y, paint);

                paint.setTextSize(18);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                y += 40;
                canvas.drawText("Analyse vocale", x, y, paint);

                y += 40;
                canvas.drawText(("Emotion dominante: " + dominantEmotion), x, y, paint);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(16);
                for (int i = 0; i < emotionScores.size(); i++) {
                    y += 25;
                    canvas.drawText(
                            EMOTION_LABELS[i] + " : " +
                                    String.format(Locale.FRANCE, "%.1f%%", emotionScores.get(i) * 100),
                            x, y, paint
                    );
                }


                paint.setTextSize(12);
                    y += 40;
                    canvas.drawText("Date : " + dateStr, x, y, paint);

                    pdf.finishPage(page);


                    String fileName = "EmotionScope_" + "Analyse_vocale_"+
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

                    new AlertDialog.Builder(this)
                            .setTitle("Votre rapport est prêt")
                            .setMessage("Voulez-vous sauvegarder votre rapport dans Téléchargements?")
                            .setNegativeButton("Télécharger", (d, which) ->
                                    telechargementPdf(cacheFile, fileName)
                            )
                            .setNeutralButton("Annuler", null)
                            .show();
                });
        }

        alertDialog.show();
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


    private void enregistrerAnalyseVocale(List<Double> emotionScores) {
        if (uid == null || email == null) {
            Toast.makeText(this, "Erreur : Identifiant utilisateur manquant", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir les scores en pourcentages (0-100)
        List<Double> percentages = new ArrayList<>();
        for (Double score : emotionScores) {
            percentages.add(score * 100);
        }

        // Trouver l'émotion dominante
        int maxIndex = 0;
        for (int i = 1; i < emotionScores.size(); i++) {
            if (emotionScores.get(i) > emotionScores.get(maxIndex)) {
                maxIndex = i;
            }
        }
        String emotionDominante = EMOTION_LABELS[maxIndex];

        // Préparer les données
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> analyseData = new HashMap<>();
        analyseData.put("date", date);
        analyseData.put("emotionDominante", emotionDominante);
        analyseData.put("moyennes", percentages);

        // Enregistrement
        DatabaseReference analysesRef = utilisateursRef.child(uid).child("analysesVocale");
        String analysisId = analysesRef.push().getKey();

        if (analysisId != null) {
            analysesRef.child(analysisId).setValue(analyseData)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Analyse enregistrée avec succès", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erreur d'enregistrement: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


    private void copyModelToExternalStorage() {
        try {
            File modelFile = new File(getExternalFilesDir(null), "modele_converti.tflite");
            if (!modelFile.exists()) {
                InputStream modelInputStream = getAssets().open("modele_converti.tflite");
                FileOutputStream outputStream = new FileOutputStream(modelFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = modelInputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                modelInputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
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

}



