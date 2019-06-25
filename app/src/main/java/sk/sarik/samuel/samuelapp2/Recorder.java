package sk.sarik.samuel.samuelapp2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Recorder extends Service {

    //toto sluzi na neustale pocuvanie, dokonca aj ked je telefon v rezime sleep
    public static PowerManager mgr;
    //notifikacia v top bare
    private static int FOREGROUND_ID = 1338;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        mAudioManager.adjustVolume(AudioManager.STREAM_NOTIFICATION,AudioManager.ADJUST_MUTE);
        mAudioManager.adjustVolume(AudioManager.STREAM_SYSTEM,AudioManager.ADJUST_MUTE);
        mAudioManager.adjustVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_MUTE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
    }

    //metody
    //vytvorenie notifikacie
    private Notification buildForegroundNotification(String filename) {
        //inicializacia buildera pre Notifikaciu
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        //po kliknuti na ikonku notifikacie
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        //zapnut notifikaciu
        b.setOngoing(true);
        //detaily notifikacie
        b.setContentTitle(getString(R.string.working))
                .setContentText(filename)
                .setSmallIcon(R.drawable.phonefinder)
                .setTicker(getString(R.string.working));
        b.setContentIntent(resultPendingIntent);

        //buildovanie notifikacie
        return(b.build());
    }

    //pouzite na kontrolu zvuku
    static public AudioManager mAudioManager;
    //pouzite pre hlasovy vstup
    protected SpeechRecognizer mSpeechRecognizer;
    //intent pre ziskanie dat
    protected Intent mSpeechRecognizerIntent;
    //sluzi na ziskavanie sprav z poutivatelskeho vstupu
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));
    //pravdiva alebo nepravdiva hodnota, ci aplikacia pocuva alebo nie
    protected boolean mIsListening;
    //vytvorenie timeru, ktory urcuje ako dlho aplikacia caka na hodnotu true alebo false
    protected volatile boolean mIsCountDownOn;
    //pouzite pre message processing
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;

    protected static class IncomingHandler extends Handler {

        private WeakReference<Recorder> mtarget;

        IncomingHandler(Recorder target)
        {
            mtarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {

            final Recorder target = mtarget.get();

            switch (msg.what) {

                case MSG_RECOGNIZER_START_LISTENING:
                    //ak sa nic nestalo
                    if (!target.mIsListening) {

                        //zaciatok pocuvania vstupu
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);

                        //teraz aplikacia vie, ze pocuva
                        target.mIsListening = true;
                    }
                    break;

                //ak sa sprava prerusila
                case MSG_RECOGNIZER_CANCEL:
                    //nepocuva
                    target.mSpeechRecognizer.cancel();
                    //nastavenie hodnoty false
                    target.mIsListening = false;
                    //pockaj sekundu
                    try {
                        Thread.sleep(( 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    //timer ktory caka pocas vstupu
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000) {

        @Override
        public void onTick(long millisUntilFinished) { }
        //ak je sprava nahrana
        @Override
        public void onFinish() {
            //stop pocuvania
            mIsCountDownOn = false;
            //dostan aktualnu spravu
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };

    //ked start button je kliknuty
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        //inicializacia, aby servis bezal
        mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        //zapni wakelock
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
        //start notifikacie
        startForeground(FOREGROUND_ID,
                buildForegroundNotification("Phone Finder"));
        try {
            //zacni pocuvat
            Message msg = new Message();
            msg.what = MSG_RECOGNIZER_START_LISTENING;
            mServerMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //neustale bezi servis
        return  START_STICKY;
    }

    //ked servis je stopnuty
    @Override
    public void onDestroy() {
        super.onDestroy();
        //dostan zvyok mute
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false );
        //stop vsetkeho pocuvania
        if (mIsCountDownOn) {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }

    //pochopenie spravy
    protected class SpeechRecognitionListener implements RecognitionListener {

        //sound pool pre pipnutie
        SoundPool sp;
        //sound pool je ready
        int sound = 0;
        //ked osoba zacne rozpravat
        @Override
        public void onBeginningOfSpeech() {
            //audiomanager reset
            mAudioManager = null;
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //aby aplikacia nenahravala pocas pocuvania
            if (mIsCountDownOn) {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }

        }
        //ked informacia je prijata
        @Override
        public void onBufferReceived(byte[] buffer) { }

        //ked zdroj vstupu prestane hovorit
        @Override
        public void onEndOfSpeech() {
            //reset audiomanager
            mAudioManager = null;
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        //ak aplikacia nerozumie co si povedal, alebo si zle nieco povedal
        @Override
        public void onError(int error) {
            //reset audio manager
            mAudioManager = null;
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            if (mIsCountDownOn) {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }

            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try {
                mIsListening = false;
                mServerMessenger.send(message);
//                mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        //nie je dolezite
        @Override
        public void onEvent(int eventType, Bundle params) { }

        //nie je dolezite
        @Override
        public void onPartialResults(Bundle partialResults) { }

        //pripraveny zacat pocuvat
        @Override
        public void onReadyForSpeech(Bundle params) {

            //Build.VERSION_CODES.JELLY_BEAN)
            if (Build.VERSION.SDK_INT >= 16) {
                //zacat pocuvat
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();
            }

        }

        //ked dostaneme akualne slova zo vstupu
        @Override
        public void onResults(Bundle results) {
            //reset audiomanager
            mAudioManager = null;
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            //start prace so vstupom
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            //prirad zvuk zvukovemu fondu
            sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            //nacitaj zvuk pre buduce pouzitie
            sound = sp.load(Recorder.this, R.raw.sound1, 1);
            //vytvor retazec na ukladanie povedaneho textu
            String result;
            //retazce ktore budu pouzite pri porovnavani
            String foto = "foto";
            String fot = "fot";
            String hovor = "hovor";
            String hov = "hov";
            //retazec ktory obsahuje to co sme povedali
            result = data.get(0);

            if (result.toLowerCase().contains(foto.toLowerCase()) || result.toLowerCase().contains(fot.toLowerCase())) {
                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                Intent ocrApp = new Intent(Recorder.this, sk.sarik.samuel.samuelapp2.OcrActivity.class);
                ocrApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ocrApp);
            } else if (result.toLowerCase().contains(hovor.toLowerCase()) || result.toLowerCase().contains(hov.toLowerCase())) {
                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                Intent voiceApp = new Intent(Recorder.this, sk.sarik.samuel.samuelapp2.VoiceActivity.class);
                voiceApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                startActivity(voiceApp);
            }

            //potom pocuvaj znovu
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try {
                mServerMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        //nieje dolezite
        @Override
        public void onRmsChanged(float rmsdB) { }

    }

    //pouzite pre servis
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
