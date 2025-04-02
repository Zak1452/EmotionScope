package com.example.emotionscope;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AnalyseCognitiveActivity extends AppCompatActivity {

    private TextView questionText;

    private Button[] buttons;
    //Tableau contenant les boutons

    private int currentQuestionIndex = 0;
    //Indice de la question courante

    private int[] userAnswers = new int[10];
    // Stocker les réponses de l'utilisateur sous forme de chiffre dans un tableau

    private String[] questions = {
            "Question 1: Comment vous sentez-vous aujoud'hui ? ?",
            "Question 2: Vous sentez-vous stresser ?",
            "Question 3: Vous ressassez souvent les expériences passées et vous rêvez de conclusions différentes ?",
            "Question 4: Vos moments de bonheur sont souvent assombris par vos inquiétudes au sujet de l’avenir ?",
            "Question 5: Vous vous éloignez de vos amis et des membres de votre famille lorsque vous vivez des moments difficiles ?",
            "Question 6: Vous croyez que vous pouvez apprendre des périodes difficiles que vous vivez ?",
            "Question 7: Vous faites de l’exercice régulièrement et vous mangez bien même quand vous êtes très occupé ou stressé ?",
            "Question 8: Vous trouvez toujours du temps pour vos passe-temps ?",
            "Question 9: Vous utilisez régulièrement une méthode de relaxation ?",
            "Question 10: Vous avez une bonne estime de vous-même ?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_cognitive_activity);

        questionText = findViewById(R.id.questionText);

        buttons = new Button[10];
        for (int i = 0; i < 10; i++) {
            int buttonId = getResources().getIdentifier("btn" + (i + 1), "id", getPackageName()); //identifiant du bouton
            buttons[i] = findViewById(buttonId);
            final int score = i + 1; // Echelle entre 1 et 10

            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Enregistrement la réponse de l'utilisateur
                    userAnswers[currentQuestionIndex] = score;

                    // Affichage de  la réponse, avec l'utilisateur un  (petit message qui apparaît à l'écran pour une courte période afin de fournir un retour à l'utilisateur)
                    Toast.makeText(AnalyseCognitiveActivity.this, "Réponse de l'utilisateur : " + score, Toast.LENGTH_SHORT).show();

                    // Passer à la question suivante
                    currentQuestionIndex++;

                    if (currentQuestionIndex < questions.length) {
                        // Affichage la nouvelle question
                        questionText.setText(questions[currentQuestionIndex]);
                    } else {
                        // Questionnaire terminé
                        Toast.makeText(AnalyseCognitiveActivity.this, " Questionnaire terminé !", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        // Initialisation de la question en cours
        questionText.setText(questions[currentQuestionIndex]);
    }
}
