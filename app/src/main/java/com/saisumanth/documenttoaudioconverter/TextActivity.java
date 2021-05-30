package com.saisumanth.documenttoaudioconverter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class TextActivity extends AppCompatActivity {

    public TextView textview,title;

    public Button editFileButton;

    FloatingActionButton button;

    public boolean speechtype;

    public TextToSpeech engine;

    public SeekBar mSeekBarPitch, mSeekBarSpeed;

    public Switch voiceSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_DocumentToAudioConverter);
        setContentView(R.layout.activity_text);
        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.black));
        }

        textview = findViewById(R.id.pdf_text);

        title = findViewById(R.id.text_title);

        button = findViewById(R.id.play);

        mSeekBarPitch = findViewById(R.id.seek_bar_pitch_text);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed_text);
        editFileButton = findViewById(R.id.edit_file);
        voiceSwitch = findViewById(R.id.voice_switch);

        button.setEnabled(false);

        speechtype = false;

        final Intent intent = getIntent();

        int pos = intent.getIntExtra("position",0);

        title.setText(FilesActivity.items.get(pos).getFilename());

        Set<String> a = new HashSet<>();
        a.add("male");
        a.add("female");

        Voice male = new Voice("en-in-x-ene-local",new Locale("en","IN"),400,200,false,a);

        Voice female = new Voice("en-IN-language",new Locale("en","IN"),400,200,false,a);


        engine = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if(status == TextToSpeech.SUCCESS){

                    int result = engine.setLanguage(Locale.getDefault());

                    engine.setVoice(female);

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

        voiceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(voiceSwitch.isChecked()){

                    engine.setVoice(male);

                }else {

                    engine.setVoice(female);

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
                   button.setImageResource(R.drawable.ic_baseline_stop_24);
                   voiceSwitch.setEnabled(false);
                   mSeekBarPitch.setEnabled(false);
                   mSeekBarSpeed.setEnabled(false);
                   speechtype = true;
               }else{

                   engine.stop();
                   button.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                   voiceSwitch.setEnabled(true);
                   mSeekBarPitch.setEnabled(true);
                   mSeekBarSpeed.setEnabled(true);
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