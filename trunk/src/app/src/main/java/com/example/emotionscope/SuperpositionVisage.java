package com.example.emotionscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Classe qui permet de generer un cadre autour du visage facilitant la detection.
 *
 * @author Zakaria Chaker
 */
public class SuperpositionVisage extends View {

    private Paint pinceau;
    private Rect rectangleVisage;
    private boolean visageDetecte = false;


    /**
     * Constructeur prend en param le contexte
     *
     * @param contexte
     */
    public SuperpositionVisage(Context contexte) {
        super(contexte);
        init();
    }

    public SuperpositionVisage(Context contexte, AttributeSet attrs) {
        super(contexte, attrs);
        init();
    }

    // Méthode d'initialisation pour éviter la duplication du code
    private void init() {
        pinceau = new Paint();
        pinceau.setColor(Color.RED); // Rouge par défaut
        pinceau.setStyle(Paint.Style.STROKE);
        pinceau.setStrokeWidth(5);
    }

    /*
     * Méthode qui permet de mettre à jour les coordonnées du visage
     * Mais aussi de detecter ou non le visage vert ou rouge
     *
     * @param rectangle
     */
    public void mettreAJourRectangleVisage(Rect rectangle) {
        if (rectangle != null && rectangle.width() > 0 && rectangle.height() > 0) {
            this.rectangleVisage = rectangle;
            visageDetecte = true;
            pinceau.setColor(Color.GREEN);
            Log.d("SuperpositionVisage", "Rectangle mis à jour: " + rectangle.toString());
        } else {
            visageDetecte = false;
            pinceau.setColor(Color.RED);
        }
        invalidate();
    }

    /**
     * Méthode qui permet de dessiner le rectangle autour du visage
     *
     * @param canevas
     */
    @Override
    protected void onDraw(Canvas canevas) {
        super.onDraw(canevas);
        Log.d("SuperpositionVisage", "onDraw exécuté");

        if (rectangleVisage != null) {
            Log.d("SuperpositionVisage", "Dessine rectangle : " + rectangleVisage.toString());
            canevas.drawRect(rectangleVisage, pinceau);
        }
    }
}