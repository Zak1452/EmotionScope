package com.example.emotionscope;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe qui permet de gérer l'affichage des derniers résultats obtenus dans le menu principal.
 * Elle récupère directement depuis Firebase les dernières analyses effectuées.
 * Si aucune analyse n'a ete effectuée ce jour là il n y a aucun affichage.
 *
 * @author: CHAKER Zakaria
 */
public class DetailsAnalysesMain {

    public interface Callback {
        void onAllFetched();
        void onError(String message);
    }

    private final Activity activity;
    private final DatabaseReference userRef;
    private final int[] viewIds = {R.id.carreAnalyseFaciale, R.id.carreAnalyseVocale, R.id.carreAnalyseCognitive, R.id.carreAnalyseComplete};
    private final String[] nodes = {"analysesFaciale", "analysesVocale", "analysesCognitive", "analysesGlobale"};
    private final String[] titles = {"Analyse Faciale", "Analyse Vocale", "Analyse Cognitive", "Analyse Complète"};

    /**
     * Constructeur de base.
     *
     * @param activity
     * @param database
     * @param uid
     */
    public DetailsAnalysesMain(Activity activity, FirebaseDatabase database, String uid) {
        this.activity = activity;
        this.userRef  = database.getReference("utilisateurs").child(uid);
    }

    /**
     *
     * @param callback
     */
    public void fetchAndDisplayLatestAnalyses(Callback callback) {
        String todayIso = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        AtomicInteger pending = new AtomicInteger(nodes.length);

        for (int i = 0; i < nodes.length; i++) {
            final String node  = nodes[i];
            final int viewId   = viewIds[i];
            final String title = titles[i];

            userRef.child(node)
                    .orderByChild("date")
                    .startAt(todayIso)
                    .endAt(todayIso + "\uf8ff")
                    .limitToLast(1)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snap) {
                            if (!snap.exists())
                            {
                                displayResults(viewId, title, Collections.emptyList());
                            } else
                            {
                                DataSnapshot last = snap.getChildren().iterator().next();
                                String result = parseResult(node, last);
                                displayResults(viewId, title, Collections.singletonList(result));
                            }
                            if (pending.decrementAndGet() == 0)
                            {
                                callback.onAllFetched();
                            }
                        }
                        @Override public void onCancelled(DatabaseError e) {
                            callback.onError(e.getMessage());
                        }
                    });
        }
    }

    /**
     * Cette méthode permet de retrouner une chaine de caracteres. Cette chaine sera par la suite chargée dans chacun des carrés.
     *
     * @param node
     * @param child
     * @return chaine de caracteres qui permet d'afficher les résultats en fonction des données renvoyées par Firebase.
     * @author: CHAKER Zakaria
     */
    private String parseResult(String node, DataSnapshot child) {
        switch (node) {
            case "analysesFaciale":
            case "analysesVocale":
                String emo = child.child("emotionDominante")
                        .getValue(String.class);
                String emoji = switch (emo) {
                    case "Joie", "Happy" -> "😊";
                    case "Tristesse", "Sad" -> "😢";
                    case "Colère", "Angry" -> "😡";
                    case "Surprise", "Surprised" -> "😲";
                    case "Peur", "Fear" -> "😱";
                    case "Dégoût", "Disgust" -> "🤢";
                    default -> "🙂";
                };
                return (emo != null ? emo + " " + emoji : "Aucune émotion");

            case "analysesCognitive":
                Double m = child.child("moyennes").child("memoire").getValue(Double.class);
                Double p = child.child("moyennes").child("perception").getValue(Double.class);
                Double r = child.child("moyennes").child("raisonnement").getValue(Double.class);
                return String.format(
                        "🧠 Mémoire: %.0f%%\n👁 Perception: %.0f%%\n🧩 Raisonnement: %.0f%%",
                        m != null ? m : 0,
                        p != null ? p : 0,
                        r != null ? r : 0
                );

            case "analysesGlobale":
                DataSnapshot faciale = child.child("faciale");
                DataSnapshot vocale = child.child("vocale");
                DataSnapshot cognitive = child.child("cognitive").child("moyennes");

                String emoF = faciale.child("emotionDominante").getValue(String.class);
                String emoFA = switch(emoF)
                {
                    case "Joie","Happy"    -> "😊";
                    case "Tristesse","Sad" -> "😢";
                    case "Colère","Angry"  -> "😡";
                    case "Surprise","Surprised" -> "😲";
                    case "Peur","Fear"     -> "😱";
                    case "Dégoût","Disgust"-> "🤢";
                    default                 -> "🙂";
                };

                String emoV = vocale.child("emotionDominante").getValue(String.class);
                String emoVA = switch(emoV)
                {
                    case "Joie","Happy"    -> "😊";
                    case "Tristesse","Sad" -> "😢";
                    case "Colère","Angry"  -> "😡";
                    case "Surprise","Surprised" -> "😲";
                    case "Peur","Fear"     -> "😱";
                    case "Dégoût","Disgust"-> "🤢";
                    default                 -> "🙂";
                };

                Double memoire    = cognitive.child("memoire").getValue(Double.class);
                Double perception = cognitive.child("perception").getValue(Double.class);
                Double raisonnement = cognitive.child("raisonnement").getValue(Double.class);

                return String.format(
                        "🔵 Faciale: %s %s    " +
                                "🎤 Vocale: %s %s    " +
                                "  Cognitive: 🧠 %.0f%%  👁️ %.0f%%  🧩 %.0f%%",
                        emoF  != null ? emoF  : "-", emoFA,
                        emoV  != null ? emoV  : "-", emoVA,
                        memoire  != null ? memoire  : 0,
                        perception  != null ? perception  : 0,
                        raisonnement  != null ? raisonnement  : 0
                );

            default:
                return "Résultat inconnu";
        }
    }

    /**
     *
     * @param viewId
     * @param title
     * @param results
     */
    private void displayResults(int viewId, String title, List<String> results) {
        FrameLayout container = activity.findViewById(viewId);
        activity.runOnUiThread(() -> {
            if (results.isEmpty()) {
                container.setVisibility(View.INVISIBLE);
                return;
            }

            container.setVisibility(View.VISIBLE);
            container.removeAllViews();

            int index = 0;
            for (int j = 0; j < viewIds.length; j++) {
                if (viewIds[j] == viewId) {index = j; break; }
            }

            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.setPadding(16, 16, 16, 16);

            TextView tvTitle = new TextView(activity);
            String titleText = title;
            if (index < 2) titleText += "\n\n";
            tvTitle.setText(titleText);
            tvTitle.setTextSize((index == 2 || index ==1) ? 15 : 18 );
            tvTitle.setGravity(Gravity.CENTER);
            tvTitle.setTextColor(Color.BLACK);
            tvTitle.setTypeface(null, Typeface.BOLD);
            layout.addView(tvTitle);

            for (String line: results) {
                TextView tvLine = new TextView(activity);
                tvLine.setText(line);
                tvLine.setTextSize((index == 2 || index ==1) ? 10 : 14 );
                tvLine.setTextColor(Color.BLACK);
                tvLine.setGravity(Gravity.CENTER);
                tvLine.setPadding(0, 8, 0, 0);
                layout.addView(tvLine);
            }
            layout.setAlpha(0f);
            container.addView(layout);
            layout.animate().alpha(1f).setDuration(300).start();
        });
    }

    /**
     * Méthode qui permet d'ajouter un effet de flottement aux carrés du menu.
     */
    public void startFloatingAnimation() {
        long[] delays = {0L, 300L, 600L, 900L};

        for (int i = 0; i < viewIds.length; i++) {
            View v = activity.findViewById(viewIds[i]);
            if (v == null) continue;

            Animator levitate = AnimatorInflater.loadAnimator(activity, R.animator.flottement);
            levitate.setTarget(v);
            levitate.setStartDelay(delays[i]);
            levitate.start();
        }
    }
}
