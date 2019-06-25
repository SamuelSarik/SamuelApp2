package sk.sarik.samuel.samuelapp2;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

    //used to unmute the phone's audio system
    public static AudioManager mAudioManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO }, 2);
        }

        //nothing to do here except set the content view
//        Intent i = new Intent(this, Recorder.class);
//        startService(i);
//        startService(new Intent(this, Recorder.class));
    }

    //zapnutie servisu
    public void onClickStartService(View V) {
        //start servisu
        Intent i = new Intent(MainActivity.this, Recorder.class);
        startService(i);
    }

    //stop servisu
    public void onClickStopService(View V) {
        //activate the audiomanager in order to control the audio of the system
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //Stop the running service from here
        stopService(new Intent(this, Recorder.class));
        //unmutes any sound that might have been muted in the process of this application
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false  );
        mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
    }

    //when the app is returned from after being put in the background
    @Override
    public void onResume(){
        super.onResume();
        //we want to stop app because when the user goes to app, most likely they will want to stop app
        stopService(new Intent(this, Recorder.class));
    }

}
