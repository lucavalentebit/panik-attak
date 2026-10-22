package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
public class CountdownSmoke extends ConfigSmoke {
    @Override public void onStart(){context=getTargetContext();var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity screen=null;
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: real alarm active\n");finish(code,out);return;}
        try{
            check(MainActivity.remaining(3661000,0).equals("01:01:01"),"hours minutes seconds");check(MainActivity.remaining(90000000,0).equals("25:00:00"),"hours do not wrap at midnight");check(MainActivity.remaining(0,1000).equals("00:00:00"),"no negative time");
            for(Store.Routine r:Store.routines(context))Store.setEnabled(context,r.id,false);String id=Store.create(context,"Test countdown",false);Plan plan=Store.plan(context);var time=java.time.LocalTime.now().plusHours(2);plan.hour=time.getHour();plan.minute=time.getMinute();Store.save(context,plan);Store.setEnabled(context,id,true);
            screen=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final MainActivity a=screen;waitForIdleSync();String first=a.countdown.getText().toString();check(first.matches("[0-9]{2,}:[0-9]{2}:[0-9]{2}"),"countdown visible");SystemClock.sleep(1200);check(!first.equals(a.countdown.getText().toString()),"countdown ticks");capture(a,"countdown-v23");
            runOnMainSync(()->{Store.setEnabled(context,id,false);a.render();});check(a.countdown.getText().toString().equals("--:--:--"),"paused routine has no countdown");
            out.putString("stream","PASS: "+checks+" countdown checks.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL: "+e+"\n");}finally{if(screen!=null){final MainActivity a=screen;runOnMainSync(a::finish);}waitForIdleSync();restore(before);}finish(code,out);
    }
}
