package com.example.emotionscope;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.util.Log;

public class ClasseUtilitaire {


    public static File buildPdfFaciale(Context context, String prenom, String nom, String email, String dateAnalyse,
                                       String emotionDominante, List<Double> moyennesFaciale)
    {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int x = 40, y = 60;

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.planetes);
        canvas.drawBitmap(
                Bitmap.createScaledBitmap(logo, 40, 40, false),
                x, y - 30, paint
        );

        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        canvas.drawText("Emotion Scope", x + 50, y, paint);

        paint.setStrokeWidth(2);
        canvas.drawLine(x, y + 12, pageInfo.getPageWidth() - x, y + 12, paint);

        paint.setStrokeWidth(0);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
        y += 25;
        canvas.drawText("Email : " + email, x, y, paint);


        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        y += 40;
        canvas.drawText("Analyse faciale", x, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(16);
        y += 25;
        canvas.drawText("Emotion dominante: " + emotionDominante, x, y, paint);
        List<String> labels = Arrays.asList(
                "Colère", "Dégoût", "Peur", "Joie",
                "Neutre", "Tristesse", "Surprise"
        );

        for (int i = 0; i < moyennesFaciale.size() && i < labels.size(); i++) {
            double pct = moyennesFaciale.get(i);
            y += 25;
            canvas.drawText(
                    String.format("%s : %.1f%%", labels.get(i), pct),
                    x, y, paint
            );
        }

        paint.setTextSize(12);
        y += 40;
        canvas.drawText("Date de l'analyse : " + dateAnalyse, x, y, paint);

        pdf.finishPage(page);

        String fileName = String.format("EmotionScope_Analyse_Faciale_%s_%s_%s.pdf",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()),
                nom.toUpperCase(),
                prenom.toUpperCase());

        File cacheFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            pdf.writeTo(out);
        } catch (IOException e) {
            e.printStackTrace();
            cacheFile = null;
        } finally {
            pdf.close();
        }

        return cacheFile;
    }

    public static File buildPdfVocale(Context context, String prenom, String nom, String email, String dateAnalyse,
                                String emotionDominante,List<Double> moyennesVocale)
    {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int x = 40, y = 60;

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.planetes);
        canvas.drawBitmap(
                Bitmap.createScaledBitmap(logo, 40, 40, false),
                x, y - 30, paint
        );

        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        canvas.drawText("Emotion Scope", x + 50, y, paint);

        paint.setStrokeWidth(2);
        canvas.drawLine(x, y + 12, pageInfo.getPageWidth() - x, y + 12, paint);

        paint.setStrokeWidth(0);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
        y += 25;
        canvas.drawText("Email : " + email, x, y, paint);


        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        y += 40;
        canvas.drawText("Analyse vocale", x, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(16);
        y += 25;
        canvas.drawText("Emotion dominante: " + emotionDominante, x, y, paint);

        List<String> labels = Arrays.asList(
                "Angry", "Fear", "Disgust", "Happy", "Sad", "Surprised", "Neutral"
        );

        for (int i = 0; i < moyennesVocale.size() && i < labels.size(); i++) {
            double pct = moyennesVocale.get(i);
            y += 25;
            canvas.drawText(
                    String.format("%s : %.1f%%", labels.get(i), pct),
                    x, y, paint
            );
        }
        paint.setTextSize(12);
        y += 40;
        canvas.drawText("Date de l'analyse : " + dateAnalyse, x, y, paint);

        pdf.finishPage(page);

        String fileName = String.format("EmotionScope_Analyse_Vocale_%s_%s_%s.pdf",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()),
                nom.toUpperCase(),
                prenom.toUpperCase());

        File cacheFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            pdf.writeTo(out);
        } catch (IOException e) {
            e.printStackTrace();
            cacheFile = null;
        } finally {
            pdf.close();
        }

        return cacheFile;

    }

    public static File buildPdfCognitive(Context context, String prenom, String nom, String email, String date, Map<String, Integer> moyennesCognitive)
    {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int x = 40, y = 60;

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.planetes);
        canvas.drawBitmap(
                Bitmap.createScaledBitmap(logo, 40, 40, false),
                x, y - 30, paint
        );

        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        canvas.drawText("Emotion Scope", x + 50, y, paint);

        paint.setStrokeWidth(2);
        canvas.drawLine(x, y + 12, pageInfo.getPageWidth() - x, y + 12, paint);

        paint.setStrokeWidth(0);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
        y += 25;
        canvas.drawText("Email : " + email, x, y, paint);


        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        y += 40;
        canvas.drawText("Analyse cognitive", x, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(16);
        y += 30;
        canvas.drawText("🧠 Mémoire    : " + moyennesCognitive.get("memoire") + " %", x, y, paint);
        y += 25;
        canvas.drawText("👁️ Perception: " + moyennesCognitive.get("perception") + " %", x, y, paint);
        y += 25;
        canvas.drawText("🧩 Raisonnement: " + moyennesCognitive.get("perception") + " %", x, y, paint);

        paint.setTextSize(12);
        y += 40;
        canvas.drawText("Date : " + date, x, y, paint);

        pdf.finishPage(page);

        String fileName = String.format("EmotionScope_Analyse_Cognitive_%s_%s_%s.pdf",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()),
                nom.toUpperCase(),
                prenom.toUpperCase());

        File cacheFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            pdf.writeTo(out);
        } catch (IOException e) {
            e.printStackTrace();
            cacheFile = null;
        } finally {
            pdf.close();
        }

        return cacheFile;


    }


    public static File buildPdfComplete(Context context, String prenom, String nom, String email, String date, String emotionFaciale, List<Double> moyennesF, String emotionVocale,  List<Double> moyennesV, Map<String, Integer> moyennesCognitive)
    {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int x = 40, y = 60;

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.planetes);
        canvas.drawBitmap(
                Bitmap.createScaledBitmap(logo, 40, 40, false),
                x, y - 30, paint
        );

        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        canvas.drawText("Emotion Scope", x + 50, y, paint);

        paint.setStrokeWidth(2);
        canvas.drawLine(x, y + 12, pageInfo.getPageWidth() - x, y + 12, paint);

        paint.setStrokeWidth(0);
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText("Votre rapport d'analyses global", x, y, paint);

        paint.setStrokeWidth(0);
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText("Utilisateur : " + prenom + " " + nom, x, y, paint);
        y += 25;
        canvas.drawText("Email : " + email, x, y, paint);

        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        y += 40;
        canvas.drawText("Analyse faciale", x, y, paint);

        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText("Emotion dominante: " + emotionFaciale, x, y, paint);

        List<String> labelsFac = Arrays.asList(
                "Joie",
                "Tristesse",
                "Peur",
                "Neutre",
                "Dégoût",
                "Surprise",
                "Colère"
        );

        if (moyennesF != null && !moyennesF.isEmpty()) {
            canvas.drawText("Emotion dominante: " + emotionFaciale, x, y, paint);

            for (int i = 0; i < moyennesF.size() && i < labelsFac.size(); i++) {
                double pct = moyennesF.get(i);
                y += 25;
                canvas.drawText(
                        String.format("%s : %.1f%%", labelsFac.get(i), pct),
                        x, y, paint
                );
            }
        } else {
            y += 25;
            canvas.drawText("Aucune donnée pour l'analyse faciale.", x, y, paint);
        }
        

        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        y += 40;
        canvas.drawText("Analyse Vocale", x, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        y += 40;
        canvas.drawText(("Emotion dominante: " + emotionVocale), x, y, paint);


        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        y += 40;
        canvas.drawText("Analyse cognitive", x, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(16);
        if (moyennesCognitive != null && moyennesCognitive.size() == 3) {
            y += 30;
            canvas.drawText("🧠 Mémoire    : " + moyennesCognitive.get("memoire") + " %", x, y, paint);
            y += 25;
            canvas.drawText("👁️ Perception: " + moyennesCognitive.get("perception") + " %", x, y, paint);
            y += 25;
            canvas.drawText("🧩 Raisonnement: " + moyennesCognitive.get("raisonnement") + " %", x, y, paint);
        } else {
            y += 25;
            canvas.drawText("Aucune donnée pour l'analyse cognitive.", x, y, paint);
        }

        paint.setTextSize(12);
        y += 40;
        canvas.drawText("Date : " + date, x, y, paint);

        pdf.finishPage(page);

        String fileName = String.format("EmotionScope_Analyse_Complete_%s_%s_%s.pdf",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()),
                nom.toUpperCase(),
                prenom.toUpperCase());
        File cacheFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            pdf.writeTo(out);
        } catch (IOException e) {
            e.printStackTrace();
            cacheFile = null;
        } finally {
            pdf.close();
        }

        return cacheFile;


    }


}
