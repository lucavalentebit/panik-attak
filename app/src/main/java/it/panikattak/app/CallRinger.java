package it.panikattak.app;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

/** Restore audible calls through the ring stream; never disable or rewrite DND. */
final class CallRinger {
    enum Result { READY, NEED_ACCESS, DND_BLOCKS_CALLS, UNSUPPORTED }
    static NotificationManager.Policy policy(NotificationManager n){
        return Build.VERSION.SDK_INT>=30?n.getConsolidatedNotificationPolicy():n.getNotificationPolicy();
    }
    static Result enable(Context c) {
        AudioManager audio=c.getSystemService(AudioManager.class);
        NotificationManager notifications=c.getSystemService(NotificationManager.class);
        int filter=notifications.getCurrentInterruptionFilter();
        if(filter==NotificationManager.INTERRUPTION_FILTER_ALARMS||filter==NotificationManager.INTERRUPTION_FILTER_NONE)return Result.DND_BLOCKS_CALLS;
        if(!notifications.isNotificationPolicyAccessGranted())return Result.NEED_ACCESS;
        NotificationManager.Policy before=policy(notifications);
        if(filter==NotificationManager.INTERRUPTION_FILTER_PRIORITY&&(before.priorityCategories&NotificationManager.Policy.PRIORITY_CATEGORY_CALLS)==0)return Result.DND_BLOCKS_CALLS;
        if(audio.isVolumeFixed())return Result.UNSUPPORTED;
        int target=Math.max(audio.getStreamVolume(AudioManager.STREAM_RING),(int)Math.ceil(audio.getStreamMaxVolume(AudioManager.STREAM_RING)*0.6));
        try {
            // setRingerMode(NORMAL) can turn DND off on Android. A positive ring-stream
            // volume instead changes the internal ringer mode and retains priority DND.
            audio.setStreamVolume(AudioManager.STREAM_RING,target,AudioManager.FLAG_ALLOW_RINGER_MODES);
        }catch(SecurityException e){return Result.NEED_ACCESS;}
        if(notifications.getCurrentInterruptionFilter()!=filter||!before.equals(policy(notifications)))return Result.UNSUPPORTED;
        return audio.getStreamVolume(AudioManager.STREAM_RING)>0&&!audio.isStreamMute(AudioManager.STREAM_RING)?Result.READY:Result.UNSUPPORTED;
    }
}
