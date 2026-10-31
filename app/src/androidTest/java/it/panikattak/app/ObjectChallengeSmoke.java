package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;

/** Silent integration checks; no photographs, sound or production history fixtures. */
public final class ObjectChallengeSmoke extends Instrumentation {
    private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private TextView text(View view,String label){
        if(view instanceof TextView&&((TextView)view).getText().toString().equals(label))return (TextView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView found=text(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    private Intent photo(Puzzle puzzle){return new Intent().putExtra("puzzle",puzzle.encode()).putExtra("answer",puzzle.solution());}
    @Override public void onStart(){
        Context c=getTargetContext();var prefs=Store.prefs(c);Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: real alarm active.\n");finish(code,out);return;}
        Map<String,?> before=new HashMap<>(prefs.getAll());ChallengeActivity screen=null;AlarmActivity alarm=null;
        try{
            // Disable recording before setting up synthetic recalls; restore in finally.
            prefs.edit().clear().putBoolean(HabitHistory.ENABLED,false).commit();
            Plan plan=new Plan();plan.puzzleType=Puzzle.CAMERA;Store.save(c,plan);
            check(Store.plan(c).puzzleType==Puzzle.CAMERA,"camera selection persists");
            check(c.getResources().getStringArray(R.array.puzzle_types)[Puzzle.CAMERA].equals("Trova la posata"),"camera appears in routine choices");
            for(String purpose:new String[]{ChallengeActivity.PREVIEW,ChallengeActivity.EDIT,ChallengeActivity.PAUSE}){
                Store.setEnabled(c,Store.DEFAULT,true);
                screen=(ChallengeActivity)startActivitySync(ChallengeActivity.intent(c,purpose,Store.DEFAULT).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                ChallengeActivity gate=screen;Puzzle stale=Puzzle.create(Puzzle.CAMERA,gate.challenge.level,gate.challenge.encode());
                runOnMainSync(()->gate.onActivityResult(PuzzleView.CAMERA_REQUEST,Activity.RESULT_OK,photo(stale)));
                check(!gate.isFinishing()&&Store.enabled(c),"stale photo cannot complete "+purpose);
                runOnMainSync(()->gate.onActivityResult(PuzzleView.CAMERA_REQUEST,Activity.RESULT_CANCELED,null));
                check(!gate.isFinishing()&&Store.enabled(c),"cancelling camera preserves "+purpose);
                runOnMainSync(()->gate.onActivityResult(PuzzleView.CAMERA_REQUEST,Activity.RESULT_OK,photo(gate.challenge)));waitForIdleSync();
                check(gate.isFinishing(),"matching photo completes "+purpose);
                check(Store.enabled(c)==!purpose.equals(ChallengeActivity.PAUSE),"only pause disables routine");
            }
            for(boolean fallback:new boolean[]{false,true}){
                Puzzle current=Puzzle.create(Puzzle.CAMERA,0,"");long at=System.currentTimeMillis();
                AlarmQueue.write(c,Arrays.asList(new AlarmQueue.Entry(Store.DEFAULT,at,at,0,true,current.encode())));
                alarm=(AlarmActivity)startActivitySync(new Intent(c,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));AlarmActivity ringing=alarm;
                runOnMainSync(()->ringing.onActivityResult(PuzzleView.CAMERA_REQUEST,Activity.RESULT_OK,photo(Puzzle.create(Puzzle.CAMERA,0,current.encode()))));
                check(ringing.solvedAnswer==null&&prefs.getBoolean("ringing",false),"stale photo leaves recall unresolved");
                if(fallback){
                    runOnMainSync(()->text(ringing.puzzleView,"Alternativa · Matematica difficile").performClick());
                    PuzzleView math=(PuzzleView)ringing.puzzleView.getChildAt(2);String answer=math.puzzle.solution();
                    runOnMainSync(()->{math.key("0");math.key("→");});check(ringing.solvedAnswer==null,"wrong fallback leaves recall unresolved");
                    runOnMainSync(()->{for(char digit:answer.toCharArray())math.key(Character.toString(digit));math.key("→");});
                }else runOnMainSync(()->ringing.onActivityResult(PuzzleView.CAMERA_REQUEST,Activity.RESULT_OK,photo(current)));
                check(ringing.solvedAnswer!=null&&prefs.getBoolean("ringing",false),"solution waits for explicit dismissal");
                check(Scheduler.dismissCurrent(c,Store.DEFAULT,at,current,ringing.solvedAnswer),"validated solution dismisses current recall");
                runOnMainSync(ringing::finish);waitForIdleSync();
            }
            out.putString("stream","PASS: "+checks+" camera challenge integration checks (synthetic photo outcomes).\n");code=Activity.RESULT_OK;
        }catch(Throwable error){out.putString("stream","FAIL after "+checks+" checks: "+error+"\n");}
        finally{
            if(screen!=null){Activity last=screen;runOnMainSync(last::finish);}if(alarm!=null){Activity last=alarm;runOnMainSync(last::finish);}waitForIdleSync();
            var restore=prefs.edit().clear();for(var entry:before.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof Boolean)restore.putBoolean(k,(Boolean)v);else if(v instanceof Integer)restore.putInt(k,(Integer)v);else if(v instanceof Long)restore.putLong(k,(Long)v);else if(v instanceof Float)restore.putFloat(k,(Float)v);else if(v instanceof String)restore.putString(k,(String)v);else if(v instanceof Set)restore.putStringSet(k,new HashSet<>((Set<String>)v));}restore.commit();Scheduler.schedule(c);
        }
        finish(code,out);
    }
}
