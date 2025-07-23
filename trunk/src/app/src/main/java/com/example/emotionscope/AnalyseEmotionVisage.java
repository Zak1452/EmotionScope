package com.example.emotionscope;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Classe permettant de récupérer les différentes émotions détectées sur une photo envoyée par la classe AnalyseFacialeActivity.
 * Dans cette classe, chaque image est envoyée dans un modèle pour être analysée.
 * Par la suite, une émotion est détectée avec plusieurs pourcentages de confiance.
 * Le modèle utilisé, nommé "model.tflite", a été entièrement conçu par moi-même.
 * Ce modèle, entraîné avec le jeu de données FER2013, a été converti en format .tflite et intégré à ce projet.
 * Les résultats seront ensuite envoyés dans notre base de données.
 *
 * @Author Chaker Zakaria
 */
public class AnalyseEmotionVisage implements ImageAnalysis.Analyzer {

    private final Interpreter tflite;
    private final String[] emotions = {"Colère", "Degout", "Peur", "Joie", "Neutre", "Tristesse", "Surprise"};
    private final Context context;
    private final EmotionResultListener listener;

    public interface EmotionResultListener {
        void onEmotionDetected(Map<String, Integer> pourcentages);
    }

    public AnalyseEmotionVisage(Context context, EmotionResultListener listener) {
        this.context = context;
        this.listener = listener;

        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, "model.tflite");
            tflite = new Interpreter(model);
        } catch (IOException e) {
            throw new RuntimeException("Erreur chargement modèle!", e);
        }
    }

    /**
     * Méthode qui récupère une image prise lros de l'analyse en continu. Transrofmée en bimap (format photo) puis analysée.
     * L'image transformée en bitmap est analysée et les emotions stoquées dans notre Map.
     *
     * @param imageProxy The image to analyze
     * @Author: Chaker Zakaria
     */
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = toBitmap(imageProxy);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true); // Redimensionnement à 224x224

        if (bitmap == null) {
            imageProxy.close();
            return;
        }

        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        float[][] output = new float[1][7];
        tflite.run(inputBuffer, output);

        int maxIndex = 0;
        for (int i = 1; i < 7; i++) {
            if (output[0][i] > output[0][maxIndex]) {
                maxIndex = i;
            }
        }

        Log.d("EmotionScores", Arrays.toString(output[0]));
        String predictedEmotion = emotions[maxIndex];
        Map<String, Integer> percentages = getEmotionPourcentages(output);
        listener.onEmotionDetected(percentages);

        imageProxy.close();
    }

    /**
     * Méthode qui permet de calculer et synthétiser le pourcentage de l'emotion concernée.
     * Retourne une Map avec emotion et pourcentage.
     *
     * @author Chaker Zakaria
     * @param output
     * @return {p}Map<String, Integer>{p}
     */
    private Map<String, Integer> getEmotionPourcentages(float[][] output) {
        Map<String, Integer> emotionMap = new LinkedHashMap<>();
        float[] scores = output[0];
        float total = 0f;

        for (float score : scores) total += score;

        for (int i = 0; i < scores.length; i++) {
            int pourcentage = Math.round((scores[i] / total) * 100);
            emotionMap.put(emotions[i], pourcentage);
        }

        return emotionMap;
    }

    /**
     * Méthode qui permet de convertir une image en bitmap.
     *
     * @author Chaker Zakaria
     * @param imageProxy
     * @return image en bitmap
     */
    private Bitmap toBitmap(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();
        if (image == null) return null;


        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    /**
     *
     * @param bitmap
     * @return
     */
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3); // 4 bytes par float, 224x224 image avec 3 canaux
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[224 * 224];
        bitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224);

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            //Mise à l'échelle des valeurs RGB entre 0 et 1
            byteBuffer.putFloat(r / 255.0f);
            byteBuffer.putFloat(g / 255.0f);
            byteBuffer.putFloat(b / 255.0f);
        }

        return byteBuffer;
    }
}