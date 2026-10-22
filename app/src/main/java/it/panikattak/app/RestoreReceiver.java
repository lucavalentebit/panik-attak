package it.panikattak.app;
import android.content.*;
public class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(i.getAction()))AlarmQueue.clear(c);
        Scheduler.schedule(c);
    }
}
