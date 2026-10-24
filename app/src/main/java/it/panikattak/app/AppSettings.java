package it.panikattak.app;

import android.app.*;
import android.content.Context;

/** Global presentation preferences; never alter routine schedules or puzzle gates. */
final class AppSettings {
    static final String STATUS="settings.sequenceStatus",LOCK_NAMES="settings.lockScreenNames";
    static boolean showStatus(Context c){return Store.prefs(c).getBoolean(STATUS,true);}
    static boolean showNames(Context c){return Store.prefs(c).getBoolean(LOCK_NAMES,false);}
    static void set(Context c,String key,boolean value){
        if(!STATUS.equals(key)&&!LOCK_NAMES.equals(key))throw new IllegalArgumentException("Unknown setting");
        Store.prefs(c).edit().putBoolean(key,value).apply();SequenceStatus.refresh(c);
        // Refresh presentation only: do not restart playback or regenerate a puzzle.
        if(LOCK_NAMES.equals(key)&&Store.prefs(c).getBoolean("ringing",false)){
            try{NotificationManager manager=c.getSystemService(NotificationManager.class);
                for(var notification:manager.getActiveNotifications())if(notification.getId()==99){manager.notify(99,AlarmService.notification(c));break;}
            }catch(RuntimeException e){android.util.Log.e("PanikAttak","Unable to refresh alarm notification privacy",e);}
        }
    }
}
