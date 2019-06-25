package sk.sarik.samuel.samuelapp2;

import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;

import static android.media.AudioManager.STREAM_MUSIC;
import static sk.sarik.samuel.samuelapp2.Recorder.mAudioManager;

public class VoiceActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    TextView txtContent;
    TextToSpeech textToSpeech;
    Button btnRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        txtContent = findViewById(R.id.txtContent);
        textToSpeech = new TextToSpeech(VoiceActivity.this, VoiceActivity.this);
        btnRead = findViewById(R.id.btnRead);
        btnRead.setVisibility(View.INVISIBLE);
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtContent.setText(FileHelper.readExternalStorage());
                TextToSpeechFunction();
            }
        });

    }

    @Override
    protected void onStart() {
        TextToSpeechFunction();
        super.onStart();
    }

    public void TextToSpeechFunction() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");

        String textholder = (FileHelper.readExternalStorage());
        textToSpeech.speak(textholder, TextToSpeech.QUEUE_FLUSH, map);
        txtContent.setText(textholder);
    }

    @Override
    public void onInit(int Text2SpeechCurrentStatus) {
        if (Text2SpeechCurrentStatus == TextToSpeech.SUCCESS) {
            Locale loc = new Locale ("sk", "SK");
            textToSpeech.setLanguage(loc);
            btnRead.setEnabled(true);
            TextToSpeechFunction();

            Intent intent1 = new Intent(Intent.ACTION_MAIN);
            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent1.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent1);
            Toast.makeText(VoiceActivity.this,"hovori",Toast.LENGTH_LONG).show();
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
//                    Toast.makeText(VoiceActivity.this,"asfkdkfdsal",Toast.LENGTH_LONG).show();
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);

                }

                @Override
                public void onError(String utteranceId) {

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textToSpeech.shutdown();
    }


}
