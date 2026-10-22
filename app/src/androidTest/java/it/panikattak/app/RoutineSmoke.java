package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.util.*;
import org.json.*;

/** Multi-profile migration, CRUD, isolated state, overlapping delivery, and restoration. */
public class RoutineSmoke extends UpgradeSmoke {
    @Override public void callActivityOnCreate(Activity a,Bundle b){super.callActivityOnCreate(a,b);a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(Build.VERSION.SDK_INT>=27){a.setShowWhenLocked(true);a.setTurnScreenOn(true);}}
    @Override public void onStart(){
        context=getTargetContext();var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity main=null;ChallengeActivity gate=null;AlarmActivity alarm=null;
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: existing alarm active.\n");finish(code,out);return;}
        ActivityMonitor monitor=addMonitor(AlarmActivity.class.getName(),null,false);
        try{
            runOnMainSync(()->{Scheduler.cancel(context);prefs.edit().clear().putInt("hour",6).putInt("minute",45).putInt("duration3",50).putBoolean("enabled",true).putBoolean("vibration",false).commit();});
            check(Store.routines(context).size()==1,"legacy routine migrated once");check(Store.plan(context).hour==6&&Store.plan(context).minute==45&&Store.plan(context).durations[3]==50&&Store.enabled(context),"legacy values retained");
            Store.rename(context,Store.DEFAULT,"Lavoro");String second=Store.create(context,"Weekend",true);check(Store.routines(context).size()==2&&!Store.enabled(context,second),"copy created paused");
            Plan weekend=Store.plan(context);check(weekend.hour==6&&weekend.durations[3]==50,"copy retains all settings");weekend.hour=9;weekend.days=96;weekend.puzzleType=Puzzle.COLORS;Store.save(context,weekend);
            check(Store.plan(context,Store.DEFAULT).hour==6&&Store.plan(context,Store.DEFAULT).puzzleType==0,"editing copied routine isolated");
            String third=Store.create(context,"Palestra",false);check(Store.plan(context).hour==7&&!Store.enabled(context),"new routine has defaults and is paused");
            Store.select(context,Store.DEFAULT);check(Store.enabled(context)&&!Store.enabled(context,second),"selection does not toggle routines");
            Store.setEnabled(context,second,true);check(Store.enabled(context,Store.DEFAULT)&&Store.enabled(context,second),"multiple routines enabled");
            check(!Store.nameAvailable(context,"lavoro",second),"duplicate names rejected");
            check(!Store.delete(context,second),"enabled routine cannot be deleted without challenge");
            main=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();capture(main,"routines-v21");
            gate=(ChallengeActivity)startActivitySync(ChallengeActivity.intent(context,ChallengeActivity.PAUSE,second).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final ChallengeActivity paused=gate;
            check(gate.challenge.type==Puzzle.COLORS&&gate.routine.equals(second),"pause uses target routine puzzle even when another is selected");
            runOnMainSync(()->paused.apply("wrong"));check(Store.enabled(context,second),"wrong answer cannot pause");
            Store.select(context,third);runOnMainSync(()->paused.apply(paused.challenge.solution()));waitForIdleSync();
            check(!Store.enabled(context,second)&&Store.enabled(context,Store.DEFAULT),"correct pause affects only captured routine");
            Store.select(context,second);Store.setEnabled(context,second,true);
            final String b=second;final long start=System.currentTimeMillis();
            runOnMainSync(()->{
                AlarmQueue.clear(context);prefs.edit().putBoolean(Store.key(b,"escalating"),true).commit();
                AlarmQueue.offer(context,Store.DEFAULT,start,start,0,false);AlarmQueue.offer(context,b,start,start,0,false);
                check(AlarmQueue.read(context).size()==2,"simultaneous recalls both retained");
                check(AlarmQueue.read(context).get(1).routine.equals(b),"queue retains routine identity");
                Puzzle first=Store.activePuzzle(context);check(!Scheduler.dismissCurrent(context,b,start,first,first.solution()),"other routine cannot dismiss same-time alarm");
                Puzzle hard=Store.newPuzzle(context,0,2);check(Scheduler.dismissMorning(context,Store.DEFAULT,start,false,hard,hard.solution()),"stop first morning accepted");
                check(Store.activeRoutine(context).equals(b)&&Store.prefs(context).getBoolean("ringing",false),"second routine remains ringing after stopping first");
                check(Store.skipped(context,b)==-1&&Store.skipped(context,Store.DEFAULT)==start,"morning skip isolated");
                AlarmQueue.offer(context,b,start+60000,start,1,false);AlarmQueue.offer(context,b,start+120000,start,1,false);
                check(AlarmQueue.read(context).size()==1&&Store.activePuzzle(context).level==1,"ignored counter and latest recall coalesce per routine");
                check(prefs.getInt(Store.key(Store.DEFAULT,"ignored"),0)==0,"ignored counters isolated");
                check(!Scheduler.dismissCurrent(context,b,start,first,first.solution()),"stale recall rejected");
                Puzzle current=Store.activePuzzle(context);check(Scheduler.dismissCurrent(context,b,start+120000,current,current.solution()),"latest recall solved");
                check(!prefs.getBoolean("ringing",false),"queue empty after final recall");
            });waitForIdleSync();
            check(Store.delete(context,third)&&Store.routines(context).size()==2,"paused routine deleted independently");
            // Deliver two simultaneous recalls through Android's real alarm broadcast path.
            runOnMainSync(()->{AlarmQueue.clear(context);prefs.edit().remove("skip").remove(Store.key(second,"skip")).putBoolean("vibration",false).putBoolean(Store.key(second,"vibration"),false).commit();Store.setEnabled(context,Store.DEFAULT,true);Store.setEnabled(context,second,true);});
            long at=System.currentTimeMillis()+4000;JSONArray batch=new JSONArray().put(new JSONObject().put("id",Store.DEFAULT).put("at",at).put("start",at).put("phase",0)).put(new JSONObject().put("id",second).put("at",at).put("start",at).put("phase",0));
            runOnMainSync(()->{Scheduler.cancel(context);prefs.edit().putLong("next",at).putString("nextBatch",batch.toString()).commit();Scheduler.set(context,at,at,0,false);});
            getUiAutomation().executeShellCommand("input keyevent 223").close();SystemClock.sleep(5500);
            check(AlarmQueue.read(context).size()==2,"real same-time delivery retained both alarms");
            check(Puzzle.decode(AlarmQueue.read(context).get(0).puzzle).type==Puzzle.MATH&&Puzzle.decode(AlarmQueue.read(context).get(1).puzzle).type==Puzzle.COLORS,"delivered alarms use their own puzzles");
            alarm=(AlarmActivity)monitor.getLastActivity();if(alarm==null||alarm.isFinishing()){context.startActivity(new Intent(context,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));alarm=(AlarmActivity)waitForMonitorWithTimeout(monitor,3000);}removeMonitor(monitor);check(alarm!=null&&!alarm.isFinishing(),"overlapping alarm UI available above lock screen");waitForIdleSync();SystemClock.sleep(400);capture(alarm,"routines-overlap-v21");check(alarm.waitingLabel.getVisibility()==View.VISIBLE,"pending routine count shown");
            runOnMainSync(()->{Puzzle first=Store.activePuzzle(context);check(Scheduler.dismissCurrent(context,Store.DEFAULT,at,first,first.solution()),"first real recall dismissed");});waitForIdleSync();
            check(Store.activeRoutine(context).equals(second)&&prefs.getBoolean("ringing",false),"second real recall remains active");
            final AlarmActivity currentScreen=alarm;runOnMainSync(currentScreen::refresh);check(currentScreen.routine.equals(second)&&currentScreen.puzzle.type==Puzzle.COLORS,"alarm UI advances to second routine");check(currentScreen.waitingLabel.getVisibility()==View.GONE,"pending count clears after promotion");
            runOnMainSync(()->{Puzzle current=Store.activePuzzle(context);check(Scheduler.dismissCurrent(context,second,at,current,current.solution()),"second real recall dismissed");});waitForIdleSync();
            check(!prefs.getBoolean("ringing",false)&&prefs.getLong("next",0)>at,"all routines rescheduled after solving");
            out.putString("stream","PASS: "+checks+" multi-routine device checks, including actual coincident alarm delivery with screen off.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL after "+checks+" checks: "+e+"\n");}
        finally{
            if(alarm!=null){final Activity a=alarm;runOnMainSync(a::finish);}if(gate!=null){final Activity a=gate;runOnMainSync(a::finish);}if(main!=null){final Activity a=main;runOnMainSync(a::finish);}waitForIdleSync();
            runOnMainSync(()->{AlarmQueue.clear(context);context.stopService(new Intent(context,AlarmService.class));Scheduler.cancel(context);restore(before);});
        }
        finish(code,out);
    }
}
