package com.example.emotionscope;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

/**
 * Activité représentant un mini-jeu de perception visuelle.
 * L'utilisateur doit identifier le caractère différent dans une séquence.
 * 5 questions sont posées automatiquement à la suite.
 *
 * @author Ada BOUAZO
 * @version 1.0
 */

public class PerceptionTestActivity extends AppCompatActivity {

    private TextView tvInstruction;
    private Button btnPlayPerception, btnFinish;
    private Random random = new Random();
    private int scorePerception = 0;
    private int totalPerception = 5;
    private int currentRound = 0;

    /**
     * Méthode appelée lors de la création de l'activité.
     *
     * @param savedInstanceState L'état précédent de l'activité (null si premier lancement).
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_perception);

        tvInstruction = findViewById(R.id.tvInstruction);
        btnPlayPerception = findViewById(R.id.btnPlayPerception);

        btnPlayPerception.setOnClickListener(v -> jouerPerception());
        btnFinish = findViewById(R.id.btnFinish2);
        btnFinish.setOnClickListener(v-> returnToAnalysis());
    }

    /**
     * Lance un tour du jeu de perception, ou affiche le score final si le jeu est terminé.
     */


    private void jouerPerception() {
        if (currentRound >= totalPerception) {
            showFinalScore();
            btnFinish.setVisibility(View.VISIBLE);
            return;
        }

        int taille = 10;
        int diffIndex = random.nextInt(taille);
        StringBuilder ligne = new StringBuilder();
        for (int i = 0; i < taille; i++) {
            ligne.append(i == diffIndex ? "O" : "0").append(" ");
        }

        final EditText input = new EditText(this);
        input.setHint("Position de l'intrus (1 à " + taille + ")");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("🧠 Jeu de Perception – Question " + (currentRound + 1))
                .setMessage("Trouvez le caractère différent :\n\n" + ligne.toString())
                .setView(input)
                .setPositiveButton("Valider", (dialog, which) -> {
                    try {
                        int position = Integer.parseInt(input.getText().toString().trim()) - 1;
                        if (position == diffIndex) {
                            Toast.makeText(this, "✅ Bonne vue !", Toast.LENGTH_SHORT).show();
                            scorePerception++;
                        } else {
                            Toast.makeText(this, "❌ Mauvaise réponse. C'était la position " + (diffIndex + 1), Toast.LENGTH_SHORT).show();
                        }
                        currentRound++;
                        jouerPerception(); // enchaîne automatiquement la prochaine
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "⚠️ Entrée invalide", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    /**
     * Affiche une boîte de dialogue avec le score final du joueur.
     */
    private void showFinalScore() {
        new AlertDialog.Builder(this)
                .setTitle("🎯 Résultat du jeu de perception")
                .setMessage("Votre score : " + scorePerception + " / " + totalPerception)
                .setPositiveButton("OK", (dialog, which) -> returnToAnalysis())
                .show();
    }

    /**
     * Termine l'activité en renvoyant le score de perception à l'activité précédente via un Intent.
     * @see android.app.Activity#setResult(int, Intent)
     */

    private void returnToAnalysis() {
        Intent result = new Intent();
        result.putExtra("PERCEPTION_SCORE_DONE", true);
        result.putExtra("PERCEPTION_SCORE", scorePerception);
        setResult(RESULT_OK, result);
        finish();
    }
}
