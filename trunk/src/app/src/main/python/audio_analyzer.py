import numpy as np
import librosa
import tensorflow.lite as tflite

def analyze_audio(audio_path, model_path):
    # 1. Charger l'audio avec Librosa
    signal, sr = librosa.load(audio_path, sr=22050)

    # 2. Extraire les MFCC
    mfcc = librosa.feature.mfcc(y=signal, sr=sr, n_mfcc=39)

    # 3. Normaliser la taille : 39 x 216
    mfcc_padded = np.zeros((39, 216))
    mfcc_trimmed = mfcc[:, :216]
    mfcc_padded[:, :mfcc_trimmed.shape[1]] = mfcc_trimmed

    # 4. Reshape (1, 39, 216, 1) pour TFLite
    input_tensor = mfcc_padded.reshape(1, 39, 216, 1).astype(np.float32)

    # 5. Charger le modèle TFLite depuis le chemin donné
    interpreter = tflite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    # 6. Analyse
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    interpreter.set_tensor(input_details[0]['index'], input_tensor)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])

    return output[0].tolist()
