package com.example.ocr_speechoutput;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.Locale;





public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int TTS_CHECK_REQUEST_CODE = 101;

    private SurfaceView cameraView;
    private TextView recognizedTextView;
    private Button recognizeButton;

    private TessBaseAPI tessBaseAPI;

    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_preview);
        recognizedTextView = findViewById(R.id.recognized_text_view);
        recognizeButton = findViewById(R.id.btn_recognize);

        // Request camera permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }

        // Check for Text-to-Speech engine availability
        Intent checkTtsIntent = new Intent();
        checkTtsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTtsIntent, TTS_CHECK_REQUEST_CODE);

        // Initialize the Tesseract OCR engine
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(getExternalFilesDir(null).getAbsolutePath(), "eng");

        // Initialize the Text-to-Speech engine
        textToSpeech = new TextToSpeech(this, this);


    }



    public void recognizeText(View view) {
        // Capture an image of the text to recognize
        cameraView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(cameraView.getDrawingCache());
        cameraView.setDrawingCacheEnabled(false);

        // Process the image with the Tesseract OCR engine
        tessBaseAPI.setImage(bitmap);
        String recognizedText = tessBaseAPI.getUTF8Text();
        recognizedTextView.setText(recognizedText);

        // Read the recognized text out loud using the Text-to-Speech engine
        textToSpeech.speak(recognizedText, TextToSpeech.QUEUE_FLUSH, null, null);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CHECK_REQUEST_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // Text-to-Speech engine data is available, so initialize it
                textToSpeech = new TextToSpeech(this, this);
            } else {
                // Text-to-Speech engine data is not available, so prompt the user to install it
                Intent installTtsIntent = new Intent();
                installTtsIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTtsIntent);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the Text-to-Speech engine language to English
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Text-to-Speech language not supported");
            }
        } else {
            Log.e(TAG, "Text-to-Speech initialization failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release resources used by the Tesseract OCR engine
        tessBaseAPI.end();

        // Shutdown the Text-to-Speech engine
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


}