package it.panikattak.app;
import android.content.*;
public class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(i.getAction())){
            for(SequenceStatus.Run run:SequenceStatus.read(c).values())HabitHistory.runningEvent(c,run.routine,"interrupted","recall");AlarmQueue.clear(c);
        }
        if(Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction()))for(SequenceStatus.Run run:SequenceStatus.read(c).values())HabitHistory.runningEvent(c,run.routine,"partial","recall");
        Scheduler.schedule(c);
        if(HabitHistory.unlocked(c)){PendingResult pending=goAsync();HabitHistory.IO.execute(()->{try{HabitHistory.drain(c);}finally{pending.finish();}});}
    }
}
