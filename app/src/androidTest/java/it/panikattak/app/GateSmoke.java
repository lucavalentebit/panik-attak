package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.os.*;
import java.time.*;
import java.util.*;

/** Silent device test. Restores the user's settings and schedule in finally. */
public class GateSmoke extends Instrumentation {
    int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void check(boolean condition,String description){checks++;if(!condition)throw new AssertionError(description);}
    ChallengeActivity open(Context c,String purpose){return (ChallengeActivity)startActivitySync(ChallengeActivity.intent(c,purpose).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    void solve(ChallengeActivity a){a.answer=a.challenge.solution();a.key("→");}
    @Override public void onStart(){
        Context c=getTargetContext();var prefs=Store.prefs(c);Bundle result=new Bundle();
        if(prefs.getBoolean("ringing",false)){result.putString("stream","SKIP: a real alarm is active; no settings changed.\n");finish(Activity.RESULT_CANCELED,result);return;}
        Map<String,?> before=new HashMap<>(prefs.getAll());ChallengeActivity screen=null;MainActivity main=null;int code=Activity.RESULT_CANCELED;
        try{
            prefs.edit().putInt("puzzleType",0).putBoolean("enabled",true).remove("skip").commit();Scheduler.schedule(c);long next=prefs.getLong("next",0);
            WakeChallenge problem=new WakeChallenge(17,4,26);
            check(!Scheduler.pause(c,problem,"93"),"wrong pause rejected at scheduling boundary");
            check(Store.enabled(c)&&prefs.getLong("next",0)==next,"wrong pause preserves next alarm");
            screen=open(c,ChallengeActivity.PAUSE);final ChallengeActivity cancelled=screen;
            runOnMainSync(()->{cancelled.key("0");cancelled.key("→");});
            check(Store.enabled(c)&&!screen.isFinishing(),"wrong UI answer cannot pause");
            check(screen.answer.isEmpty(),"wrong UI answer clears input");
            runOnMainSync(cancelled::finish);waitForIdleSync();check(Store.enabled(c),"cancel preserves alarms");
            screen=open(c,ChallengeActivity.PAUSE);final ChallengeActivity accepted=screen;runOnMainSync(()->solve(accepted));waitForIdleSync();
            check(!Store.enabled(c)&&!prefs.contains("next"),"correct UI answer pauses all alarms");

            Plan plan=Store.plan(c);long start=plan.events(LocalDate.now(),ZoneId.systemDefault()).get(0).start;
            prefs.edit().putBoolean("enabled",true).putBoolean("ringing",true).putBoolean("activeTest",false).putLong("activeStart",start).remove("skip").commit();
            check(!Scheduler.dismissMorning(c,start,false,problem,"0"),"wrong morning solution rejected");
            check(prefs.getBoolean("ringing",false)&&!prefs.contains("skip"),"wrong answer leaves current and future alarms");
            check(!Scheduler.dismissMorning(c,start+1,false,problem,"94"),"stale challenge cannot stop a different sequence");
            screen=open(c,ChallengeActivity.MORNING);final ChallengeActivity morning=screen;runOnMainSync(()->solve(morning));waitForIdleSync();
            check(Store.enabled(c),"stopping morning retains daily repeat");
            check(prefs.getLong("skip",-1)==start&&!prefs.getBoolean("ringing",true),"correct answer stops only current morning");

            main=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            var monitor=addMonitor(ChallengeActivity.class.getName(),null,false);final MainActivity owner=main;
            runOnMainSync(()->owner.requestEdit(0));screen=(ChallengeActivity)waitForMonitorWithTimeout(monitor,3000);removeMonitor(monitor);
            check(screen!=null&&ChallengeActivity.EDIT.equals(screen.purpose),"enabled plan edits open challenge");
            int hour=Store.plan(c).hour;final ChallengeActivity edit=screen;runOnMainSync(edit::finish);waitForIdleSync();
            check(Store.plan(c).hour==hour&&owner.pendingEdit==-1,"cancelled edit keeps original plan");
            // Exercise all renderers through the same pause boundary, including wrong responses.
            for(int type=0;type<5;type++){
                prefs.edit().putInt("puzzleType",type).putBoolean("enabled",true).putBoolean("ringing",false).commit();
                screen=open(c,ChallengeActivity.PAUSE);final ChallengeActivity visual=screen;
                check(visual.challenge.type==type&&visual.challenge.level==2,"selected difficult puzzle type");
                runOnMainSync(()->visual.puzzleView.submit("wrong"));check(Store.enabled(c),"visual wrong answer preserves alarms");
                runOnMainSync(()->visual.puzzleView.submit(visual.challenge.solution()));waitForIdleSync();
                check(!Store.enabled(c),"visual solution pauses sequence");
                for(int level:new int[]{0,2}){
                    screen=(ChallengeActivity)startActivitySync(new Intent(c,ChallengeActivity.class).putExtra("purpose",ChallengeActivity.PREVIEW).putExtra("level",level).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    final ChallengeActivity sample=screen;
                    check(sample.challenge.level==level,"preview difficulty");
                    if(type==Puzzle.COLORS||type==Puzzle.TILES){runOnMainSync(()->sample.puzzleView.tap(sample.challenge.data[0]));check(sample.puzzleView.entered.isEmpty(),"memory input locked during display");}
                    if(type==Puzzle.ORDER){runOnMainSync(()->sample.puzzleView.swap(0,1));check(sample.puzzleView.order[1]==sample.challenge.data[0],"order gesture swaps tiles");}
                    runOnMainSync(sample::finish);waitForIdleSync();
                }
            }
            prefs.edit().putBoolean("enabled",false).putBoolean("ringing",false).commit();
            Plan fixedPlan=new Plan();fixedPlan.fixed[1]=true;fixedPlan.fixedMinutes[1]=490;fixedPlan.puzzleType=4;fixedPlan.escalating=true;Store.save(c,fixedPlan);
            Plan restored=Store.plan(c);check(restored.fixed[1]&&restored.fixedMinutes[1]==490&&restored.puzzleType==4&&restored.escalating,"v2 settings persist");
            Puzzle current=Store.newPuzzle(c,0,0);prefs.edit().putBoolean("ringing",true).putLong("activeAt",123).putString("activePuzzle",current.encode()).commit();
            check(!Scheduler.dismissCurrent(c,122,current,current.solution()),"stale simple challenge rejected");
            check(!Scheduler.dismissCurrent(c,123,current,"wrong"),"wrong simple answer rejected");
            check(Scheduler.dismissCurrent(c,123,current,current.solution()),"current solved challenge accepted");
            runOnMainSync(()->owner.pickTime(R.string.first_alarm,450,m->{}));waitForIdleSync();
            check(owner.getSupportFragmentManager().findFragmentByTag("timePicker")!=null,"Material dial opens");
            runOnMainSync(()->((androidx.fragment.app.DialogFragment)owner.getSupportFragmentManager().findFragmentByTag("timePicker")).dismissAllowingStateLoss());waitForIdleSync();
            result.putString("stream","PASS: "+checks+" device checks: all puzzle types, preview, wrong answers, pause, stop morning, stale challenges, protected editing, settings persistence, Material dial.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n");}
        finally{
            if(screen!=null){final Activity a=screen;runOnMainSync(a::finish);}if(main!=null){final Activity a=main;runOnMainSync(a::finish);}waitForIdleSync();
            var restore=prefs.edit().clear();for(var entry:before.entrySet()){Object v=entry.getValue();String key=entry.getKey();if(v instanceof Boolean)restore.putBoolean(key,(Boolean)v);else if(v instanceof Integer)restore.putInt(key,(Integer)v);else if(v instanceof Long)restore.putLong(key,(Long)v);else if(v instanceof String)restore.putString(key,(String)v);else if(v instanceof Float)restore.putFloat(key,(Float)v);}restore.commit();Scheduler.schedule(c);
        }
        finish(code,result);
    }
}
