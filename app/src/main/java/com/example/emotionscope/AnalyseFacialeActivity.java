package com.example.emotionscope;
import android.Manifest;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * L'objectif de cette classe est de permettre l'analyse faciale en temps reel.
 * L'utilisateur pourra lancer l'analyse faciale en clquant sur le bouton. Pour plus de cohérence et de fluidité la camera sera constamment active...
 *
 * @Author: Zakaria Chaker et Ahmed-Lamin CHABANE
 */
@OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class AnalyseFacialeActivity extends AppCompatActivity {

    //Permet de supporter la camera
    private TextView textViewResultats;
    private Button btnAnalayseFaciale;
    private Button btnRetourMenu;
    private PreviewView previewView;
    private FaceDetector analyseVisage;
    private SuperpositionVisage superpositionVisage;
    private TextView textViewDecompte;
    private List<String> emotionsDetectees = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_faciale_activity);

        superpositionVisage = findViewById(R.id.superpositionVisage);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
        else
        {
            configurationVisage();
            lancementCamera();
        }

        previewView = findViewById(R.id.previewView);
        textViewResultats = findViewById(R.id.textViewResultats);
        btnAnalayseFaciale = findViewById(R.id.btn_analayse_faciale);
        textViewDecompte = findViewById(R.id.textViewDecompte);
        btnRetourMenu = findViewById(R.id.btn_menu_principal);

        btnRetourMenu.setOnClickListener(view ->
        {
            if(analyseVisage != null)
            {
                analyseVisage.close();
            }
            Intent intent = new Intent(AnalyseFacialeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        });
        btnAnalayseFaciale.setOnClickListener(view -> lancementAnalyseFaciale());


    }

    /**
     * Permet de configurer l'analyse du visage
     */
    private void configurationVisage() {
        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build();
        analyseVisage = FaceDetection.getClient(faceDetectorOptions);
    }

    /**
     * Permet de lancer la caméra et de l'activer en vue de l'analyse du visage.
     *
     *
     */
    private void lancementCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() ->
        {
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
                textViewResultats.setText("Erreur lors du lancement de la caméra: " + erreur.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
        }

    /**
     * Méthode qui annonce/informe l'utilisateur et donne des détails sur l'analyse faciale en cours
     *
     */

    private void lancementAnalyseFaciale() {
        textViewResultats.setText("Analyse en cours...");

        new CountDownTimer(7000, 1000) {
            @Override
            public void onTick(long tempsRestant) {
                Log.d("Décompte", "Temps restant : " + tempsRestant / 1000);
                textViewDecompte.setText("00:0" + tempsRestant / 1000);
            }

            @Override
            public void onFinish() {
                Log.d("Décompte", "Décompte terminé !");
                textViewResultats.setText("Analyse terminée. Résultats en cours d'intégration :)");
                lancementCamera();
                String emotionFinale = calculerEmotionFinale();
                emotionsDetectees.clear();

                TextView textViewEmotionFinale = findViewById(R.id.textViewEmotionFinale);
                textViewEmotionFinale.setText("Émotion finale : " + emotionFinale);// Lance la caméra après le décompte
            }
        }.start();
    }

    /**
     * Dans cette méthode, on va commencer par recuperer les différentes probabilités
     * Puis nous exploiterons ces probabilités, par la suite les émptionsseront reconnues...
     *
     * @param imProxy
     */

    private Executor executor = Executors.newSingleThreadExecutor();
    private void analyseImageCapturee(ImageProxy imProxy)
        {
            executor.execute(() -> {
            try
            {
                InputImage inputImg = InputImage.fromMediaImage(imProxy.getImage(), imProxy.getImageInfo().getRotationDegrees());
                analyseVisage.process(inputImg).addOnSuccessListener(faces ->
                {
                    Log.d("DEBUG_VISA", "Nombre de visages détectés: " + faces.size());
                    for(Face f: faces)
                    {


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
                        runOnUiThread(() -> {
                            superpositionVisage.mettreAJourRectangleVisage(visageRect);
                            superpositionVisage.invalidate();
                        });
                        double probaSourire = ((f.getSmilingProbability() != null)? f.getSmilingProbability() : 0.0);
                        double probaOeilDroit = ((f.getRightEyeOpenProbability() != null)? f.getRightEyeOpenProbability() : 0.0);
                        double probaOeilGauche = ((f.getLeftEyeOpenProbability() != null)? f.getLeftEyeOpenProbability() : 0.0);

                        String emotionGeneree = calculEmotion(probaSourire, probaOeilDroit, probaOeilGauche);

                        emotionsDetectees.add(emotionGeneree);
                        //Récuperons les résultats dans une BD
                        //TODO

                        //Pour le moment les résultats sont à consulter dans le textView
                        String res = "Sourire: " + (probaSourire*100)+"%\n"
                                +"Oeil droit: "+ (probaOeilDroit*100)+"%\n"
                                +"Oeil gauche: "+ (probaOeilGauche*100)+"%\n"+
                                "Emotion: " + emotionGeneree;

                        textViewResultats.setText(res);


                    }
                    imProxy.close();
                }).addOnFailureListener(
                        erreur ->
                        {
                            textViewResultats.setText("Erreur analyse");
                            imProxy.close();
                        }
                );
            }catch (Exception erreur)
            {
                erreur.printStackTrace();
                imProxy.close();
            }
        });
        }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        superpositionVisage.mettreAJourRectangleVisage(null);
    }

    /**
     * Méthode qui prend en en entrée les probabilites renvoyées par le prototype et renvoie l'emotion concernée...
     *
     * @param pSourire
     * @param pOeilDroit
     * @param pOeilGauche
     * @return emotion génerée
     */

    private String calculEmotion(double pSourire, double pOeilDroit, double pOeilGauche) {
        if (pSourire > 0.40) {
            return "Joie";
        }
        if (pSourire < 0.009) {
            return "Tristesse";
        }
        if (pSourire >= 0.009 && pSourire <= 0.016 && pOeilDroit > 0.95 && pOeilGauche > 0.95) {
            return "Neutre";
        }
        if (pSourire >= 0.009 && pSourire <= 0.025 && (pOeilDroit < 0.60 || pOeilGauche < 0.60)) {
            return "Colère";
        }
        if (pOeilDroit < 0.2 && pOeilGauche < 0.2) {
            return "Fatigue";
        }
        return "Emotion inconnue";
    }

    private String calculerEmotionFinale() {
        if (emotionsDetectees.isEmpty()) return "Aucune analyse effectuée";

        Map<String, Integer> compteur = new HashMap<>();
        for (String emotion : emotionsDetectees) {
            compteur.put(emotion, compteur.getOrDefault(emotion, 0) + 1);
        }

        return Collections.max(compteur.entrySet(), Map.Entry.comparingByValue()).getKey();
    }


    }

