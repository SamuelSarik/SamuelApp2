package sk.sarik.samuel.samuelapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context aContext, Intent aIntent) {

        // kde sa zacina servis
        aContext.startService(new Intent(aContext, Recorder.class));
    }

}
