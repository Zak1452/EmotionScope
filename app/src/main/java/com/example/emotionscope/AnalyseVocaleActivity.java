package com.example.emotionscope;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

public class AnalyseVocaleActivity extends AppCompatActivity {

    /*
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private MediaRecorder recorder;
    private String fileName;
    private Interpreter tflite;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocal_analysis);

        resultTextView = findViewById(R.id.resultTextView);
        Button recordButton = findViewById(R.id.recordButton);
        Button stopButton = findViewById(R.id.stopButton);
        Button analyzeButton = findViewById(R.id.analyzeButton);

        fileName = getExternalFilesDir(null).getAbsolutePath() + "/audio.wav";

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        recordButton.setOnClickListener(v -> startRecording());
        stopButton.setOnClickListener(v -> stopRecording());
        analyzeButton.setOnClickListener(v -> {
            float[] mfcc = extractMFCC(fileName);
            if (mfcc != null) {
                float[][] input = new float[1][13];
                input[0] = mfcc;
                float[][] output = new float[1][1];
                tflite.run(input, output);
                resultTextView.setText("Prediction: " + output[0][0]);
            } else {
                resultTextView.setText("Erreur lors de l'analyse.");
            }
        });

        // Tester FFmpegKit pour une tâche simple (vérification de la version FFmpeg)
        FFmpegSession session = FFmpegKit.execute("-version");
        ReturnCode returnCode = session.getReturnCode();
        if (ReturnCode.isSuccess(returnCode)) {
            resultTextView.setText("FFmpeg est disponible !");
        } else {
            resultTextView.setText("Erreur FFmpeg.");
        }
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // enregistre en .3gp
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioChannels(1);

        try {
            recorder.prepare();
            recorder.start();
            resultTextView.setText("Enregistrement...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
        resultTextView.setText("Enregistrement terminé.");
    }

    private float[] extractMFCC(String audioPath) {
        final List<float[]> mfccList = new ArrayList<>();
        int sampleRate = 16000;
        int bufferSize = 512;
        int bufferOverlap = 256;
        int numCoefficients = 13;

        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(
                    audioPath, sampleRate, bufferSize, bufferOverlap);

            MFCC mfcc = new MFCC(bufferSize, sampleRate, numCoefficients, 40, 300, 3000);
            dispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    mfcc.process(audioEvent);
                    float[] mfccs = mfcc.getMFCC();
                    mfccList.add(mfccs.clone());
                    return true;
                }

                @Override
                public void processingFinished() {}
            });

            Thread audioThread = new Thread(dispatcher, "Audio Dispatcher");
            audioThread.start();
            audioThread.join(); // Attendre la fin

            float[] moyenne = new float[numCoefficients];
            for (float[] frame : mfccList) {
                for (int i = 0; i < numCoefficients; i++) {
                    moyenne[i] += frame[i];
                }
            }
            for (int i = 0; i < numCoefficients; i++) {
                moyenne[i] /= mfccList.size();
            }

            return moyenne;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        File file = new File(getApplicationContext().getFilesDir(), "model.tflite");
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

     */
}
