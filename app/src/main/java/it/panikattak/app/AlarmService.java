package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.net.Uri;

public class AlarmService extends Service {
    MediaPlayer player; Vibrator vibrator; PowerManager.WakeLock wake; AudioManager audio; AudioFocusRequest focus;
    final Handler handler=new Handler(); int level; float volume;
    final Runnable ramp=new Runnable(){public void run(){volume=Math.min(1f,volume+0.08f);if(player!=null){try{player.setVolume(volume,volume);}catch(Exception ignored){}}if(volume<1f)handler.postDelayed(this,4000);}};
    @Override public int onStartCommand(Intent i,int flags,int id) {
        level=Store.prefs(this).getInt("activePhase",0);for(AlarmQueue.Entry entry:AlarmQueue.read(this))level=Math.max(level,entry.phase);
        var nm=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel("alarm","Sveglie Panik Attak",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.alarm_channel));channel.setSound(null,null);channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);nm.createNotificationChannel(channel);
        Intent open=new Intent(this,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        var pi=PendingIntent.getActivity(this,20,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=new Notification.Builder(this,"alarm").setSmallIcon(it.panikattak.app.R.drawable.ic_alarm).setContentTitle(Store.name(this,Store.activeRoutine(this))+" · "+Plan.NAMES[level]).setContentText(getString(R.string.alarm_notification)).setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(true).setContentIntent(pi);
        if(Store.prefs(this).getBoolean("ringing",false))builder.setFullScreenIntent(pi,true);
        if(Build.VERSION.SDK_INT>=31)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        startForeground(99,builder.build());
        if(!Store.prefs(this).getBoolean("ringing",false)){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
        if(wake==null){wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PanikAttak:alarm");wake.acquire(4*60*60*1000L);}
        startSound();
        sendBroadcast(new Intent("it.panikattak.REFRESH").setPackage(getPackageName()));
        return START_STICKY;
    }
    void startSound(){
        handler.removeCallbacks(ramp);if(player!=null){player.release();player=null;}
        AudioAttributes attr=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        audio=getSystemService(AudioManager.class);if(focus!=null)audio.abandonAudioFocusRequest(focus);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attr).setOnAudioFocusChangeListener(change->{}).build();audio.requestAudioFocus(focus);
        volume=level==3?1f:0.4f+level*0.2f;
        Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        try{play(uri,attr);}catch(Exception e){try{play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),attr);}catch(Exception ignored){}}
        handler.postDelayed(ramp,4000);
        vibrator=getSystemService(Vibrator.class);vibrator.cancel();
        boolean vibrate=false;for(AlarmQueue.Entry entry:AlarmQueue.read(this))if(Store.vibration(this,entry.routine))vibrate=true;
        if(vibrate)vibrator.vibrate(VibrationEffect.createWaveform(level==3?new long[]{0,650,180,650,180,650,500}:new long[]{0,450,650},0),attr);
    }
    void play(Uri uri,AudioAttributes attr)throws Exception{if(player!=null)player.release();player=new MediaPlayer();player.setAudioAttributes(attr);player.setDataSource(this,uri);player.setLooping(true);player.setVolume(volume,volume);player.prepare();player.start();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(player!=null)player.release();if(vibrator!=null)vibrator.cancel();if(wake!=null&&wake.isHeld())wake.release();if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
