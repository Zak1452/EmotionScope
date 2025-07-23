package com.example.emotionscope;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.Random;

/**
 * Activité représentant un test de mémoire à court terme.
 * L'utilisateur doit mémoriser des séquences de chiffres et les reproduire après quelques secondes.
 *
 * @author Ada BOUAZO
 * @version 1.0
 * @see AppCompatActivity
 */

public class MemoireTestActivity extends AppCompatActivity {

    /** Zone de texte affichant la séquence ou les instructions */
    private TextView tvSequence, tvMessage, tvInstruction;

    /** Champ de saisie pour la réponse utilisateur */
    private EditText etResponse;

    /** Bouton principal de contrôle du jeu (Commencer / Valider / Continuer) */
    private Button btnAction;

    /** Bouton pour quitter le test et envoyer les résultats */
    private Button btnFinish;

    /** Score de mémoire de l'utilisateur */
    private int scoreMemoire = 0;

    /** Séquence actuelle à mémoriser */
    private int[] sequence = new int[5];

    /** Indique si la séquence a été affichée */
    private boolean sequenceShown = false;

    /**
     * État du bouton :
     * 0 - Commencer,
     * 1 - Valider,
     * 2 - Continuer
     */
    private int currentStep = 0; 

    /** Compteur de séquences jouées */
    private int sequenceCount = 0;

    /** Nombre maximum de séquences dans le test */
    private final int maxSequences = 3;

    /** Indique si l'instruction de départ a été affichée */
    private boolean instructionShown = false;


        /**
     * Méthode appelée à la création de l'activité.
     * Initialise les vues et configure les écouteurs de boutons.
     *
     * @param savedInstanceState état sauvegardé de l'activité, s'il y en a un
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_memory);

        tvSequence = findViewById(R.id.tvSequence);
        tvMessage = findViewById(R.id.tvMessage);
        tvInstruction = findViewById(R.id.tvInstruction);
        etResponse = findViewById(R.id.etResponse);
        btnAction = findViewById(R.id.btnStart);
        btnFinish = findViewById(R.id.btnFinish);
        btnAction.setOnClickListener(v -> handleButtonClick());
        btnFinish.setOnClickListener(v -> returnToAnalysis());

        // Affiche l'instruction uniquement au début
        tvInstruction.setVisibility(View.VISIBLE);
    }


    private void handleButtonClick() {

        switch (currentStep) {
            case 0: // Commencer
                tvInstruction.setVisibility(View.GONE); // cacher après 1ère fois
                etResponse.setVisibility(View.GONE);
                tvMessage.setText("");
                generateSequence();
                showSequenceTemporarily();
                break;

            case 1: // Valider
                checkUserResponse();
                break;

            case 2: // Continuer
                if (sequenceCount < maxSequences) {
                    etResponse.setText("");
                    etResponse.setVisibility(View.GONE);
                    tvMessage.setText("");
                    generateSequence();
                    showSequenceTemporarily();
                }
                break;
        }
    }

    private void generateSequence() {
        Random random = new Random();
        for (int i = 0; i < sequence.length; i++) {
            sequence[i] = random.nextInt(9);
        }
        sequenceShown = true;
    }

    /**
     * Affiche temporairement la séquence générée à l'écran, puis la cache et invite l'utilisateur à répondre.
     * La durée d'affichage varie en fonction de la progression.
     * @see Handler
     */

    private void showSequenceTemporarily() {
        currentStep = -1; // désactiver temporairement le bouton
        btnAction.setVisibility(View.GONE);

        StringBuilder sb = new StringBuilder();
        for (int num : sequence) {
            sb.append(num).append(" ");
        }
        tvSequence.setText(sb.toString().trim());
        tvMessage.setText("Retenez la séquence...");

        int delay = (sequenceCount == 0) ? 5000 : 3000; // 5s d'abord, puis 3s
        new Handler().postDelayed(() -> {
            tvSequence.setText("Séquence cachée. Entrez-la maintenant !");
            etResponse.setVisibility(View.VISIBLE);
            btnAction.setText("Valider");
            btnAction.setVisibility(View.VISIBLE);
            currentStep = 1;
        }, delay);
    }

    /**
     * Vérifie la réponse de l'utilisateur et met à jour le score.
     * Gère la transition vers la prochaine séquence ou la fin du test.
     */

    private void checkUserResponse() {
        String response = etResponse.getText().toString().replaceAll("\\s+", "");
        boolean correct = response.length() == sequence.length;

        if (correct) {
            for (int i = 0; i < sequence.length; i++) {
                if (response.charAt(i) - '0' != sequence[i]) {
                    correct = false;
                    break;
                }
            }
        }

        if (correct) {
            scoreMemoire++;
            tvMessage.setText("✅ Bonne mémoire !");
        } else {
            tvMessage.setText("❌ Mauvaise réponse. La bonne séquence était : " + Arrays.toString(sequence));
        }

        sequenceCount++;
        if (sequenceCount < maxSequences) {
            btnAction.setText("Continuer");
            currentStep = 2;
        } else {
            btnAction.setVisibility(View.GONE);
            etResponse.setVisibility(View.GONE);
            tvMessage.append("\n✅ Test terminé !");
            btnFinish.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Termine l'activité en renvoyant le score de mémoire à l'activité précédente via un Intent.
     *
     * @see android.app.Activity#setResult(int, Intent)
     * @see android.app.Activity#finish()
     */
    private void returnToAnalysis() {
        Intent result = new Intent();
        result.putExtra("MEMORY_TEST_DONE", true);
        result.putExtra("MEMORY_SCORE", scoreMemoire);
        setResult(RESULT_OK, result);
        finish();
    }



}