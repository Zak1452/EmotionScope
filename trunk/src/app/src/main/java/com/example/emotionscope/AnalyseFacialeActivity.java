package com.example.emotionscope;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.apache.commons.logging.LogFactory;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * L'objectif de cette classe est de permettre l'analyse faciale en temps reel.
 * L'utilisateur pourra lancer l'analyse faciale en cliquant sur le bouton.
 * Pour plus de cohérence et de fluidité la camera sera constamment active...
 *
 * @author: CHAKER Zakaria
 */
@OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class AnalyseFacialeActivity extends AppCompatActivity implements AnalyseEmotionVisage.EmotionResultListener {

    private static final org.apache.commons.logging.Log log = LogFactory.getLog(AnalyseFacialeActivity.class);
    private Button btnAnalyseFaciale;
    private ImageButton btnRetourMenu, btnSuivantScreen;
    private PreviewView previewView;
    private LinearLayout layoutContinuer;
    private FaceDetector analyseVisage;
    private SuperpositionVisage superpositionVisage;
    private TextView textViewDecompte;
    private Map<String, Integer> emotionsDetectees = new HashMap<>();
    private Interpreter tflite;
    private AnalyseEmotionVisage emotionAnalyzer;
    private TextView textViewAnalyse;
    private boolean erreurSurvenue = false, analyseActive = false, isMultiStep = false, analyseTerminee = false;
    private List<Map<String, Integer>> emotionsParSeconde = new ArrayList<>();
    private CountDownTimer currentTimer = null;
    private String emotionFinale = null;
    private List<Double> moyennes = new ArrayList<>();
    private String uid, prenom, nom, email;
    private FirebaseDatabase database;
    private DatabaseReference utilisateursRef;

    /**
     * Methode qui permet de créé une instance de la classe AnalyseFacialeActvity.
     * Vérifie aussi si les permissions ont été accordées ou pas et gère les textView et boutons necessaires
     *
     * @author CHAKER Zakaria
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_faciale_activity);
        superpositionVisage = findViewById(R.id.superpositionVisage);

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            configurationVisage();
            lancementCamera();
        }

        previewView = findViewById(R.id.previewView);

        btnAnalyseFaciale = findViewById(R.id.btn_analyse_faciale);
        textViewDecompte = findViewById(R.id.textViewDecompte);
        btnRetourMenu = findViewById(R.id.btn_menu_principal);
        textViewAnalyse = findViewById(R.id.textViewEtatAnalyse);
        emotionAnalyzer = new AnalyseEmotionVisage(this, this);
        layoutContinuer = findViewById(R.id.layout_continuer);
        layoutContinuer.setVisibility(View.GONE);
        btnSuivantScreen = findViewById(R.id.btn_continuer);
        btnRetourMenu.setOnClickListener(view -> {

            if (isMultiStep)
            {
                confirmationQuitterAnalyse();
            }
            else {
                fermerRessources();
                Intent intent = new Intent(AnalyseFacialeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnAnalyseFaciale.setOnClickListener(
                view -> {
                    if (analyseActive) return;
                    btnAnalyseFaciale.setEnabled(false);
                    emotionsParSeconde.clear();
                    emotionsDetectees.clear();
                    lancementAnalyseFaciale();
                });

        btnSuivantScreen.setOnClickListener(v -> {
            if (analyseTerminee && moyennes != null && emotionFinale != null) {
                afficherBoiteDeDialogueResultats(moyennes, emotionFinale);
            }
        });
    }

    /**
     * Gère la configuration du visage et initialise le détecteur de visages de la caméra en utilisant les options spécifiques définies
     *
     * @author: CHAKER Zakaria
     */
    private void configurationVisage() {
        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        analyseVisage = FaceDetection.getClient(faceDetectorOptions);
    }

    /**
     * Méthode qui permet de lancer la camera frontale.
     * Elle configure plusieurs aspects de la caméra, mais aussi responsable de l'affichage du flux vidéo et l'analyse des images en temps réel.
     *
     * @author: CHAKER Zakaria
     */
    private void lancementCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    analyseImageCapturee(image);
                });
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception erreur) {
                textViewAnalyse.setText("Erreur lors du lancement de la caméra");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Méthode qui permet de lancer le décompte de l'analyse faciale.
     * Ce décompte permettera d'affiner le résultat de l'analyse en récuperant chaque donnée a la seconde
     *
     * @author: CHAKER Zakaria
     */

    private void lancementAnalyseFaciale() {

        arreterAnalyse();
        analyseActive = true;

        mettreAJourTextViewAnalyse("Analyse en cours...", Color.BLUE, true);
        new CountDownTimer(7000, 1000) {
            @Override
            public void onTick(long tempsRestant) {
                runOnUiThread(() -> btnAnalyseFaciale.setEnabled(false));
                if (!analyseActive || erreurSurvenue) {
                    Log.e("Décompte", "Erreur détectée pendant le décompte. Arrêt du décompte.");
                    cancel();
                    mettreAJourTextViewAnalyse("Erreur détectée! Veuillez réessayer...", Color.RED, true);
                    arreterAnalyse();
                    return;
                }
                Log.d("Décompte", "Temps restant : " + tempsRestant / 1000);
                textViewDecompte.setText("00:0" + tempsRestant / 1000);

                switch ((int) tempsRestant/1000){
                    case 5:
                        mettreAJourTextViewAnalyse("Votre charme est indéniable, patience!", Color.MAGENTA, true);
                        break;
                    case 4:
                        mettreAJourTextViewAnalyse("Votre charme est indéniable, patience!", Color.MAGENTA, true);
                        break;
                    case 3:
                        mettreAJourTextViewAnalyse("Votre charme est indéniable, patience!", Color.MAGENTA, true);
                        break;
                    case 2:
                        mettreAJourTextViewAnalyse("Finalisation de l'analyse, prêt pour le verdict!", Color.MAGENTA, true);
                        break;
                    case 1:
                        mettreAJourTextViewAnalyse("Finalisation de l'analyse, prêt pour le verdict!", Color.MAGENTA, true);
                        break;
                    case 0:
                        mettreAJourTextViewAnalyse("Finalisation de l'analyse, prêt pour le verdict!", Color.MAGENTA, true);
                        break;
                }

                if (!emotionsDetectees.isEmpty()) {
                    emotionsParSeconde.add(new HashMap<>(emotionsDetectees));
                    Log.d("Emotions", "Emotion à la seconde " + (7 - tempsRestant / 1000) + ": " + emotionsDetectees.toString());
                }

            }

            @Override
            public void onFinish() {

                if(erreurSurvenue)
                {
                    Log.e("Décompte", "Une erreur est survenue. Décompte annulé.");
                    mettreAJourTextViewAnalyse("Erreur détectée! Veuillez réessayer...", Color.RED, true);
                    arreterAnalyse();
                    return;
                }
                Log.d("Décompte", "Décompte terminé !");
                mettreAJourTextViewAnalyse("Résultats en cours d'intégration...", Color.GREEN, false);
                lancementCamera();
                calculerEmotionFinale();
                analyseActive = false;
                currentTimer = null;
                btnAnalyseFaciale.setEnabled(true);

                if (moyennes != null && !moyennes.isEmpty() && emotionFinale != null) {
                    afficherBoiteDeDialogueResultats(moyennes, emotionFinale);
                    layoutContinuer.setVisibility(View.VISIBLE);
                    mettreAJourTextViewAnalyse(("Emotion détectée: " + emotionFinale + "... Pour relancer l'anayse cliquez sur Analyser."), Color.BLUE, false);
                }
                else {
                    layoutContinuer.setVisibility(View.GONE);
                    mettreAJourTextViewAnalyse("Aucun résultat valide. Veuillez réessayer.", Color.RED, true);
                }
                analyseTerminee = true;

            }
        }.start();

    }

    private Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Méthode qui permet de capturer plusieurs images prises lors de la présence d'un visage.
     *
     * @author CHAKER Zakaria
     * @param imProxy
     */

    private void analyseImageCapturee(ImageProxy imProxy) {
        executor.execute(() -> {
            try {
                //Si l'image est déjà fermée, on évite l'analyse
                if (imProxy.getImage() == null)
                {
                    imProxy.close();
                    return;
                }
                //Création de l'inputImage à partir de l'image capturée

                InputImage inputImg = InputImage.fromMediaImage(imProxy.getImage(), imProxy.getImageInfo().getRotationDegrees());
                analyseVisage.process(inputImg).addOnSuccessListener(faces -> {
                    //Si aucun visage detecté
                    if (faces.isEmpty()) {
                        runOnUiThread(() -> {
                            mettreAJourTextViewAnalyse("Pas de visage détecté! Veuillez recentrer votre tête.", Color.RED, false);
                        });
                        erreurSurvenue = true;
                        arreterAnalyse();
                    }
                    //S'il y a plus d'un visage
                    else if (faces.size() > 1) {
                        runOnUiThread(() -> {
                            mettreAJourTextViewAnalyse("Plusieurs visages détectés! Veuillez vous recentrer seul dans le cadre.", Color.RED, false);
                            superpositionVisage.mettreAJourRectangleVisage(null); // Cache le rectangle
                            superpositionVisage.invalidate();
                        });
                        erreurSurvenue = true;
                        arreterAnalyse();
                    }
                    //Sinon si y a 1 seul visage
                    else {
                        Face f = faces.get(0);
                        Rect visageRect = new Rect(
                                (int) f.getBoundingBox().left,
                                (int) f.getBoundingBox().top,
                                (int) f.getBoundingBox().right,
                                (int) f.getBoundingBox().bottom
                        );
                        Log.d("DEBUG_RECT", "Coordonnées rectangle: Left=" + f.getBoundingBox().left +
                                " Top=" + f.getBoundingBox().top +
                                " Right=" + f.getBoundingBox().right +
                                " Bottom=" + f.getBoundingBox().bottom);

                        //Mise à jour du rectangle
                        runOnUiThread(() -> {
                            superpositionVisage.mettreAJourRectangleVisage(visageRect);
                            superpositionVisage.invalidate();
                        });

                        //Analyse émotionnelle si un seul visage est détecté
                        if (emotionAnalyzer != null) {
                            emotionAnalyzer.analyze(imProxy);
                        } else {
                            Log.e("AnalyseFacialeActivity", "Emotion analyzer non initialisé");
                        }
                    }
                    imProxy.close();
                }).addOnFailureListener(erreur -> {
                    //Si une erreur se produit on informe l'utilisateur et fermer l'image
                    runOnUiThread(() -> {
                        textViewDecompte.setText("Erreur lors de l'analyse des visages.");
                    });
                    imProxy.close();
                });
            } catch (Exception erreur) {
                Log.e("AnalyseFacialeActivity", "Erreur lors de l'analyse : ", erreur);
                if (imProxy != null) {
                    imProxy.close();
                }
            }
        });
    }

    /**
     *
     * @author: CHAKER Zakaria
     * @param pourcentages
     */
    @Override
    public void onEmotionDetected(Map<String, Integer> pourcentages) {
        runOnUiThread(() -> {

            if (emotionsDetectees.isEmpty()) {
                emotionsDetectees.putAll(pourcentages);
            }
            else {
                emotionsDetectees.clear();
                emotionsDetectees.putAll(pourcentages);
            }
            TableRow tableRow = new TableRow(AnalyseFacialeActivity.this);
            for (Map.Entry<String, Integer> entry : pourcentages.entrySet()) {
                TextView textView = new TextView(AnalyseFacialeActivity.this);

                String resPourcentage = String.format("%.2f", (double) entry.getValue() / 100.0);


                textView.setText(resPourcentage + "%");
                textView.setTextSize(14f);
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(8, 8, 8, 8);

                TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                textView.setLayoutParams(params);


                tableRow.addView(textView);
            }
            /**
             * Mise à jour du tableau:
             * - Ecriture dans le tableau
             * - Supression des dernières données
             */
            TableLayout tableLayout = findViewById(R.id.tableLayoutResultats);
            TableRow rowPourcentages = findViewById(R.id.tableRowPourcentages);
            rowPourcentages.removeAllViews();
            rowPourcentages.addView(tableRow);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        superpositionVisage.mettreAJourRectangleVisage(null);
    }

    /**
     * Méthode qui permet de calculer l'emotion dominante.
     * Ce calcul permettera de déduire l'emotion finale à partir des émotions enregistrées durant les 7secondes.
     *
     * @author: CHAKER Zakaria
     */
    private void calculerEmotionFinale() {
        if (emotionsParSeconde.isEmpty()) {
            Log.e("EmotionFinale", "Aucune donnée d'émotion disponible.");
            mettreAJourTextViewAnalyse("Aucune donnée d'émotion détectée.", Color.RED, true);
            return;
        }
        moyennes = null;
        List<Double> moyennesEmotions = new ArrayList<>(Collections.nCopies(7, 0.0));
        int nombreSecondes = emotionsParSeconde.size();


        for (Map<String, Integer> emotions : emotionsParSeconde) {
            for (int i = 0; i < 7; i++) {
                String emotion = getEmotionByIndex(i); // Récupérer le nom de l'émotion
                moyennesEmotions.set(i, moyennesEmotions.get(i) + emotions.getOrDefault(emotion, 0));
            }
        }

        int indexEmotionDominante = 0;
        double valeurMax = 0.0;
        for (int i = 0; i < 7; i++) {
            if (moyennesEmotions.get(i) > valeurMax) {
                valeurMax = moyennesEmotions.get(i);
                indexEmotionDominante = i;
            }
        }

        for (int i = 0; i < 7; i++) {
            moyennesEmotions.set(i, moyennesEmotions.get(i) / nombreSecondes);
        }

        moyennes = moyennesEmotions;
        emotionFinale = getEmotionByIndex(indexEmotionDominante);
        Log.d("EmotionFinale", "Émotion dominante : " + emotionFinale);

    }

    private String getEmotionByIndex(int index) {
        String[] emotions = {"Colère", "Dégoût", "Peur", "Joie", "Neutre", "Tristesse", "Surprise"};
        return emotions[index];
    }

    /**
     * Méthode qui permet de mettre à jour le texte d'informations situé en dessous de la camera
     *
     * @author: CHAKER Zakaria
     * @param message
     * @param couleur
     * @param clignotement
     */
    private void mettreAJourTextViewAnalyse(String message, int couleur, boolean clignotement) {
        runOnUiThread(() -> {
            textViewAnalyse.setText(message);
            textViewAnalyse.setTextColor(couleur);
            if (clignotement) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(textViewAnalyse, "alpha", 0f, 1f);
                animator.setDuration(500);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.start();
            } else {
                textViewAnalyse.clearAnimation();
                textViewAnalyse.setAlpha(1f);
            }
        });
    }

    /**
     * Méthode qui permet de fermer les ressources.
     *
     * @author: CHAKER Zakaria
     */
    private void fermerRessources() {
        if (analyseVisage != null) {
            analyseVisage.close();
            analyseVisage = null;
        }
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (emotionAnalyzer != null) {
            emotionAnalyzer = null;
        }
    }

    /*
     * Méthode qui permet de fermer les ressources et reinitilaiser les parametres à 0.
     *
     *
     */
    private void arreterAnalyse() {
        analyseActive = false;
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
        if (!emotionsParSeconde.isEmpty()) {
            emotionsParSeconde.clear();
        }
        runOnUiThread(() ->
        {
            textViewDecompte.setText("00:07");
            erreurSurvenue = false;
            btnAnalyseFaciale.setEnabled(true);
        });
        Log.d("AnalyseFacialeActivity", "Analyse arrêtée et historique nettoyé.");
    }


    /**
    * Affiche une boîte de dialogue contenant les résultats d'une analyse faciale.
    * Les résultats incluent l'émotion dominante et les pourcentages moyens pour chaque émotion.
    * 
    * @param moyennesAnalyse Liste des moyennes pour chaque émotion détectée lors de l'analyse faciale.
    *                        L'ordre est : Colère, Dégoût, Peur, Joie, Neutre, Tristesse, Surprise.
    * @param emotion         L'émotion détectée comme étant la plus dominante.
    *
    * @author: CHAKER Zakaria
    */

    private void afficherBoiteDeDialogueResultats(List<Double> moyennesAnalyse, String emotion) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View vueDialogue = getLayoutInflater().inflate(R.layout.boite_dialogue_resultats_analyse_faciale, null);

        builder.setView(vueDialogue);
        AlertDialog dialogue = builder.create();

        TextView emotionDominanteView = vueDialogue.findViewById(R.id.emotionDominante);
        TextView emotionsMoyennesView = vueDialogue.findViewById(R.id.emotionsMoyennes);

        LinearLayout layoutFermer = vueDialogue.findViewById(R.id.layout_fermer);
        LinearLayout layoutTelecharger = vueDialogue.findViewById(R.id.layout_telecharger);
        LinearLayout layoutEnregistrer = vueDialogue.findViewById(R.id.layout_enregistrer);

        ImageButton btnEnregistrer = vueDialogue.findViewById(R.id.btnEnregistrer);
        ImageButton btnTelecharger = vueDialogue.findViewById(R.id.btnTelecharger);
        ImageButton btnFermer = vueDialogue.findViewById(R.id.btnFermer);

        LinearLayout analyse_suiv = vueDialogue.findViewById(R.id.analyse_suivante);

        emotionDominanteView.setText("Emotion Dominante: " + emotion);

        String[] nomsEmotions = {"😡 Colère", "🤢 Dégoût", "😨 Peur", "😊 Joie", "😐 Neutre", "😢 Tristesse", "😲 Surprise"};
        StringBuilder texteMoyennes = new StringBuilder();

        for (int i = 0; i < moyennesAnalyse.size(); i++) {
            texteMoyennes.append(nomsEmotions[i]).append(": ")
                    .append(String.format("%.1f", moyennesAnalyse.get(i)))
                    .append("%\n");
        }
        emotionsMoyennesView.setText(texteMoyennes.toString());

        if (isMultiStep) {
            layoutEnregistrer.setVisibility(View.GONE);
            layoutTelecharger.setVisibility(View.GONE);
            layoutFermer.setVisibility(View.GONE);
            analyse_suiv.setVisibility(View.VISIBLE);
            analyse_suiv.setOnClickListener(v -> {
                String[] keys = {"Colère","Dégoût","Peur","Joie","Neutre","Tristesse","Surprise"};
                HashMap<String, Double> detailMap = new HashMap<>();
                for (int i = 0; i < keys.length && i < moyennesAnalyse.size(); i++) {
                    detailMap.put(keys[i], moyennesAnalyse.get(i));
                }
                Intent result = new Intent();
                result.putExtra("emotionFinaleFaciale", emotion);
                result.putExtra("detailsEmotionsFaciales", detailMap);
                setResult(RESULT_OK, result);
                dialogue.dismiss();
                finish();
            });


        } else {
            analyse_suiv.setVisibility(View.GONE);
            btnEnregistrer.setOnClickListener(v -> {
                if (uid == null || uid.isEmpty()) {
                    Toast.makeText(this, "UID manquant. Impossible d'enregistrer les résultats.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (email == null || email.isEmpty()) {
                    Toast.makeText(this, "Email manquant. Impossible d'enregistrer les résultats.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                Map<String, Object> analyseData = new HashMap<>();
                analyseData.put("date", date);
                analyseData.put("emotionDominante", emotion);
                analyseData.put("moyennes", moyennesAnalyse);
                String analysisId = utilisateursRef.child("utilisateurs").child(uid).child("analysesFaciale").push().getKey();

                if (analysisId != null) {
                    utilisateursRef.child(uid).child("analysesFaciale").child(analysisId)
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
                canvas.drawText("Votre rapport d'analyses", x, y, paint);

                paint.setTextSize(16);
                y += 30;
                canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
                y += 25;
                canvas.drawText("Email       : " + email, x, y, paint);

                paint.setTextSize(18);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                y += 40;
                canvas.drawText("Analyse faciale", x, y, paint);

                y += 40;
                canvas.drawText(("Emotion dominante: " + emotion), x, y, paint);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(16);
                for (int i = 0; i < moyennesAnalyse.size(); i++) {
                    y += 25;
                    canvas.drawText(
                            nomsEmotions[i] + " : " +
                                    String.format(Locale.FRANCE, "%.1f", moyennesAnalyse.get(i)) + " %",
                            x, y, paint
                    );
                }

                paint.setTextSize(12);
                y += 40;
                canvas.drawText("Date : " + dateStr, x, y, paint);

                pdf.finishPage(page);


                String fileName = "EmotionScope_" +
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

            btnFermer.setOnClickListener(v -> dialogue.dismiss());

        }
        dialogue.show();
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
                    fermerRessources();
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

