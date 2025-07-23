package com.example.emotionscope;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activité représentant un test de raisonnement logique.
 * L'utilisateur doit compléter une série de suites logiques à choix multiples.
 * Le score final est retourné à l'activité précédente.
 * 
 * @author  Ada BOUAZO
 */

public class RaisonnementTestActivity extends AppCompatActivity {

    /** Zone d'instruction au début du test */
    private TextView tvInstruction, tvSequence, tvResult;

    /** Boutons pour les 4 réponses et les contrôles de jeu */
    private Button btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4, btnPlayReason, btnFinish;

    /** Score obtenu par l'utilisateur */
    int scoreRaisonnement = 0;

    /** Liste des séquences logiques à compléter */
    private String[] sequences = {
            "2, 4, 8, 16",     // Réponse : 32
            "1, 1, 2, 3, 5",   // Réponse : 8
            "6, 11, 18, 27"    // Réponse : 38
    };


    /** Réponses correctes pour chaque séquence */
    private int[] correctAnswers = {32, 8, 38};

    private int[][] options = {
            {22, 52, 42, 32},   // Bonne Réponse : 32
            {8, 7, 9, 10},      // Bonne Réponse : 8
            {42, 38, 40, 36}    // Bonne Réponse : 38
    };

    /** Indice de la séquence actuellement affichée */
    private int currentSequenceIndex = 0;

    /** Indique si le test a commencé */
    private boolean testStarted = false;

    /**
     * Méthode appelée lors de la création de l'activité.
     * Initialise l'interface utilisateur et configure les boutons de réponse.
     *
     * @param savedInstanceState État précédent de l'activité, s'il existe.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_reason);

        tvInstruction = findViewById(R.id.tvInstruction);
        tvSequence = findViewById(R.id.tvSequence);
        tvResult = findViewById(R.id.tvResult);
        btnAnswer1 = findViewById(R.id.btnAnswer1);
        btnAnswer2 = findViewById(R.id.btnAnswer2);
        btnAnswer3 = findViewById(R.id.btnAnswer3);
        btnAnswer4 = findViewById(R.id.btnAnswer4);
        btnPlayReason = findViewById(R.id.btnPlayReason);
        btnFinish = findViewById(R.id.btnFinish3);
        btnFinish.setOnClickListener(v -> returnToAnalysis());

        // Masquer tous les éléments sauf le bouton de démarrage
        hideQuizElements();

        btnPlayReason.setOnClickListener(v -> {
            if (!testStarted) {
                testStarted = true;
                btnPlayReason.setVisibility(View.GONE);
                tvInstruction.setVisibility(View.GONE);
                showSequence();
            }
        });

        // Boutons de réponse
        btnAnswer1.setOnClickListener(v -> checkAnswer(options[currentSequenceIndex][0]));
        btnAnswer2.setOnClickListener(v -> checkAnswer(options[currentSequenceIndex][1]));
        btnAnswer3.setOnClickListener(v -> checkAnswer(options[currentSequenceIndex][2]));
        btnAnswer4.setOnClickListener(v -> checkAnswer(options[currentSequenceIndex][3]));
    }

    /**
     * Affiche la séquence actuelle et les options de réponse.
     */

    private void showSequence() {
        tvSequence.setVisibility(View.VISIBLE);
        btnAnswer1.setVisibility(View.VISIBLE);
        btnAnswer2.setVisibility(View.VISIBLE);
        btnAnswer3.setVisibility(View.VISIBLE);
        btnAnswer4.setVisibility(View.VISIBLE);
        tvResult.setVisibility(View.GONE);

        btnAnswer1.setEnabled(true);
        btnAnswer2.setEnabled(true);
        btnAnswer3.setEnabled(true);
        btnAnswer4.setEnabled(true);

        tvSequence.setText("Complétez la suite : " + sequences[currentSequenceIndex]);

        // Mise à jour du texte des boutons de réponse
        btnAnswer1.setText(String.valueOf(options[currentSequenceIndex][0]));
        btnAnswer2.setText(String.valueOf(options[currentSequenceIndex][1]));
        btnAnswer3.setText(String.valueOf(options[currentSequenceIndex][2]));
        btnAnswer4.setText(String.valueOf(options[currentSequenceIndex][3]));
    }

    /**
     * Vérifie si la réponse sélectionnée est correcte.
     *
     * @param selectedAnswer Réponse choisie par l'utilisateur.
     */

    private void checkAnswer(int selectedAnswer) {
        int correct = correctAnswers[currentSequenceIndex];

        btnAnswer1.setEnabled(false);
        btnAnswer2.setEnabled(false);
        btnAnswer3.setEnabled(false);
        btnAnswer4.setEnabled(false);

        tvResult.setVisibility(View.VISIBLE);

        if (selectedAnswer == correct) {
            tvResult.setText("✔️ Bonne réponse !");
            tvResult.setTextColor(Color.parseColor("#388E3C")); // Vert
            scoreRaisonnement++;
        } else {
            tvResult.setText("❌ Mauvaise réponse. Réponse correcte : " + correct);
            tvResult.setTextColor(Color.parseColor("#D32F2F")); // Rouge
        }

        // Avancer à la prochaine séquence ou terminer
        currentSequenceIndex++;
        if (currentSequenceIndex < sequences.length) {
            btnPlayReason.setText("Continuer");
            btnPlayReason.setVisibility(View.VISIBLE);
            btnPlayReason.setOnClickListener(v -> {
                tvResult.setVisibility(View.GONE);
                btnPlayReason.setVisibility(View.GONE);
                showSequence();
            });
        } else {
            // Fin du test
            btnAnswer1.setVisibility(View.GONE);
            btnAnswer2.setVisibility(View.GONE);
            btnAnswer3.setVisibility(View.GONE);
            btnAnswer4.setVisibility(View.GONE);
            btnPlayReason.setVisibility(View.GONE);
            tvSequence.setText("✅ Test terminé !");
            btnFinish.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Masque les éléments liés au test au démarrage de l'activité.
     */
    private void hideQuizElements() {
        tvSequence.setVisibility(View.GONE);
        tvResult.setVisibility(View.GONE);
        btnAnswer1.setVisibility(View.GONE);
        btnAnswer2.setVisibility(View.GONE);
        btnAnswer3.setVisibility(View.GONE);
        btnAnswer4.setVisibility(View.GONE);
    }

    /**
     * Retourne le score et l'état du test à l'activité précédente.
     * Ajoute des extras à l'intent avec les résultats, c'est-à-dire on ajoute des données supplémentaires à un Intent.
     */
    private void returnToAnalysis() {
        Intent result = new Intent();
        result.putExtra("RAISONNEMENT_TEST_DONE", true);
        result.putExtra("RAISONNEMENT_SCORE", scoreRaisonnement);
        setResult(RESULT_OK, result);
        finish();
    }
}
