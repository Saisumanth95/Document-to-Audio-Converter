package com.saisumanth.documenttoaudioconverter;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class TextActivity extends AppCompatActivity {

    public TextView textview;

    public Button button,editFileButton;

    public boolean speechtype;

    public TextToSpeech engine;

    public SeekBar mSeekBarPitch, mSeekBarSpeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        textview = findViewById(R.id.pdf_text);

        button = findViewById(R.id.play);

        mSeekBarPitch = findViewById(R.id.seek_bar_pitch_text);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed_text);
        editFileButton = findViewById(R.id.edit_file);

        button.setEnabled(false);

        speechtype = false;

        final Intent intent = getIntent();

        int pos = intent.getIntExtra("position",0);

        engine = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if(status == TextToSpeech.SUCCESS){

                    int result = engine.setLanguage(Locale.US);
                    
                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){

                        Log.e("TTS", "Language not supported");

                    }else{

                        button.setEnabled(true);

                    }

                }else{

                    Log.e("TTS", "Initialization failed");

                }

            }
        });


        new Thread(new Runnable(){

            public void run(){

                String text = "";

                try {

                    URL url = new URL(FilesActivity.items.get(pos).getLink());

                    HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(60000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String str;
                    while ((str = in.readLine()) != null) {
                        text += str;
                    }
                    in.close();
                } catch (Exception e) {
                    Log.d("Connection",e.toString());
                }

                String finalText = text;
                TextActivity.this.runOnUiThread(new Runnable(){
                    public void run(){
                        textview.setText(finalText);
                    }
                });

            }
        }).start();


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("This is test", "onClick: Play");

               if(!speechtype){
                   speak();
                   button.setText("STOP");
                   speechtype = true;
               }else{

                   engine.stop();
                   button.setText("PLAY");
                   speechtype = false;
               }

            }

        });

        editFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                engine.stop();
                engine.shutdown();

                Intent intent = new Intent(TextActivity.this,EditActivity.class);

                intent.putExtra("position",pos);

                startActivity(intent);

                finish();


            }
        });

    }


    private void speech(String charSequence) {

        int position = 0;

        int speechLength =  3000;

        int sizeOfChar = charSequence.length();
        String testStri = charSequence.substring(position, sizeOfChar);


        int next = speechLength;
        int pos = 0;
        while (true) {
            String temp = "";

            try {

                temp = testStri.substring(pos, next);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, temp);
                Log.d("tts", "speech: 1");
                engine.speak(temp, TextToSpeech.QUEUE_ADD, params);
                Log.d("tts", "speech: 2");

                pos = pos + speechLength;
                next = next + speechLength;

            } catch (Exception e) {
                temp = testStri.substring(pos, testStri.length());
                engine.speak(temp, TextToSpeech.QUEUE_ADD, null);
                Log.d("This is test", "speech: " + e.getMessage());
                break;

            }

        }


    }

    private void speak() {
        float pitch = (float) mSeekBarPitch.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = (float) mSeekBarSpeed.getProgress() / 50;
        if (speed < 0.1) speed = 0.1f;
        engine.setPitch(pitch);
        engine.setSpeechRate(speed);
        speech(textview.getText().toString());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        engine.stop();
        engine.shutdown();

    }
}