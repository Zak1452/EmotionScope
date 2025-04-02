package com.example.emotionscope;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe principale qui permet de generer le menu de l'application
 * Dans ce menu, un bouton qui permet de choisir entre les 3 choix d'analyse
 * Un pop up s'affichera lorsque l'utilisateur ouvre l'application pour la première fois et jusqu'à qu'il accepte les conditions générales d'utilisation (CGU)
 *
 * @Author : Chaker Zakaria et Jiang Emile
 */
public class MainActivity extends AppCompatActivity {

    private static final String preference = "MyPrefsFile"; 
    private static final String CGU = "termsAccepted"; //GGU = conditions générales d'utilisation

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int MICROPHONE_PERMISSION_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //Verification des permissions
        verificationPermissions();

        // Vérifier si les conditions ont déjà été acceptées
        //shared preferences est une interface qui stocke des données dans fichier, "MyPrefsFile" est le nom du fichier, MODE_PRIVATE signifie que le fichier est accesible uniquement par l'application qui l'a créé en lecture et écriture
        //vérifie si l'utilisateur a accepté les CGU dans le fichier, true si l'utilisateur a accepté false sinon
        SharedPreferences settings = getSharedPreferences(preference, MODE_PRIVATE);
        boolean CGU_accepte = settings.getBoolean(CGU, false);
        Log.d("MainActivity", "CGU accepted: " + CGU_accepte);

        if (!CGU_accepte) {
            CGU_Dialog();
        }

        Button btnMenu = findViewById(R.id.btn_menu_principal);

        btnMenu.setOnClickListener(view -> {
            affichageBoiteDialogueChoixAnalyse();
        });
    }

    /**
     * Méthode qui permet de faire appel à la boite de dialogue.
     * Cette boite de dialogue permettera à l'user de choisir un type d'analyse à effectuer...
     *
     *
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

        // Afficher la boîte de dialogue
        dialog.show();
    }

    /**
     * Méthode qui permet de lancer l'activité Analyse faciale
     *
     */
    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analyseFaciale() {


        Intent instance = new Intent(this, AnalyseFacialeActivity.class);
        startActivity(instance);


    }

    private void analyseVocale() {


























        //TODO
    }

    private void analyseCognitive() {
        Intent instance = new Intent(this, AnalyseCognitiveActivity.class);
        startActivity(instance);
    }

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

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée pour la caméra
                    // Tu peux accéder à la caméra ici
                } else {
                    // Permission refusée pour la caméra
                    Toast.makeText(this, "La permission de la caméra est requise", Toast.LENGTH_SHORT).show();
                }
                break;

            case MICROPHONE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée pour le microphone
                } else {
                    // Permission refusée pour le microphone
                    Toast.makeText(this, "La permission du microphone est requise", Toast.LENGTH_SHORT).show();
                }
                break;

            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée pour le stockage
                } else {
                    // Permission refusée pour le stockage
                    Toast.makeText(this, "La permission du stockage est requise", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //fonction qui affiche le pop sur les conditions générales d'utilisation
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

        // Créer un RadioButton
        RadioButton acceptRadioButton = new RadioButton(this);
        acceptRadioButton.setText("J'ai lu et accepté les conditions d'utilisation");

        // Ajouter le RadioButton au dialogue
        builder.setView(acceptRadioButton);

        // Ajouter un bouton OK
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Vérifier si le RadioButton est coché
                if (acceptRadioButton.isChecked()) {
                    // L'utilisateur a accepté les conditions
                    SharedPreferences settings = getSharedPreferences(preference, MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(CGU, true);
                    editor.apply();
                    dialog.dismiss();
                }
            }
        });

        // Créer le dialogue
        final AlertDialog dialog = builder.create();

        // Empêcher la fermeture du dialogue si le RadioButton n'est pas coché
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setEnabled(false); // Désactiver le bouton OK par défaut

                // Activer le bouton OK uniquement si le RadioButton est coché
                acceptRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    okButton.setEnabled(isChecked);
                });
            }
        });

        // Afficher le dialogue
        dialog.show();
    }
}



