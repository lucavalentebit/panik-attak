package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.os.*;

/** Device smoke test for the real AlarmManager -> receiver -> foreground service path. */
public class AlarmSmoke extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Context c=getTargetContext();
        try {
            if(!Scheduler.allowed(c))throw new AssertionError("Exact alarm permission missing");
            runOnMainSync(()->Scheduler.test(c));
            Thread.sleep(15000);
            if(!Store.prefs(c).getBoolean("ringing",false))throw new AssertionError("Alarm did not fire");
            boolean running=false;for(ActivityManager.RunningServiceInfo info:c.getSystemService(ActivityManager.class).getRunningServices(100))if(info.service.getClassName().equals(AlarmService.class.getName())&&info.foreground)running=true;
            if(!running)throw new AssertionError("Alarm is not a foreground service");
            result.putString("stream","PASS: exact alarm fired and foreground sound service is running. Alarm left ringing for UI verification.\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
}
