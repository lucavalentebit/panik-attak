package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Real device interaction and alarm delivery, with original preferences restored. */
public class UpgradeSmoke extends Instrumentation {
    int checks;Context context;
    @Override public void runOnMainSync(Runnable action){
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        super.runOnMainSync(()->{try{action.run();}catch(Throwable e){failure.set(e);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    void capture(Activity a,String name){capture(a.getWindow().getDecorView(),name);}
    void capture(View view,String name){
        runOnMainSync(()->{
            if(view.getWidth()==0||view.getHeight()==0)return;
            Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));
            try(FileOutputStream stream=new FileOutputStream(new File(context.getExternalFilesDir(null),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}catch(IOException e){throw new RuntimeException(e);}finally{bitmap.recycle();}
        });
    }
    void restore(Map<String,?> before){
        var edit=Store.prefs(context).edit().clear();for(var entry:before.entrySet()){Object value=entry.getValue();String key=entry.getKey();if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);}edit.commit();Scheduler.schedule(context);
    }
    void touch(long down,int action,float x,float y){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(event,true);event.recycle();}
    void drag(View from,View to){
        int[] a=new int[2],b=new int[2];runOnMainSync(()->{from.getLocationOnScreen(a);to.getLocationOnScreen(b);});
        float x=a[0]+from.getWidth()/2f,y=a[1]+from.getHeight()/2f,tx=b[0]+to.getWidth()/2f,ty=b[1]+to.getHeight()/2f;long down=SystemClock.uptimeMillis();
        touch(down,MotionEvent.ACTION_DOWN,x,y);SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+250);
        for(int i=1;i<=16;i++){touch(down,MotionEvent.ACTION_MOVE,x+(tx-x)*i/16,y+(ty-y)*i/16);SystemClock.sleep(30);}
        touch(down,MotionEvent.ACTION_UP,tx,ty);waitForIdleSync();SystemClock.sleep(300);
    }
    @Override public void onStart(){
        context=getTargetContext();var prefs=Store.prefs(context);Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;Activity screen=null;
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: active real alarm.\n");finish(code,out);return;}
        Map<String,?> before=new HashMap<>(prefs.getAll());
        try{
            prefs.edit().putBoolean("enabled",false).putBoolean("vibration",false).commit();Scheduler.cancel(context);
            for(int type=0;type<5;type++)for(int level:new int[]{0,2}){
                prefs.edit().putInt("puzzleType",type).commit();
                ChallengeActivity a=(ChallengeActivity)startActivitySync(new Intent(context,ChallengeActivity.class).putExtra("purpose",ChallengeActivity.PREVIEW).putExtra("level",level).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));screen=a;waitForIdleSync();SystemClock.sleep(300);capture(a,"puzzle-"+type+"-"+level);
                PuzzleView view=a.puzzleView;
                if(type==Puzzle.COLORS||type==Puzzle.TILES){
                    SystemClock.sleep(3600);check(!view.showing,"memory playback completes");
                    runOnMainSync(()->view.tap((a.challenge.data[0]+1)%(a.challenge.type==Puzzle.COLORS?6:9)));check(!a.isFinishing()&&view.showing,"wrong memory input replays same challenge");
                    SystemClock.sleep(3600);runOnMainSync(()->{for(int n:a.challenge.data)view.tap(n);});
                }else if(type==Puzzle.MATH){runOnMainSync(()->{for(char digit:a.challenge.solution().toCharArray())view.key(Character.toString(digit));view.key("→");});}
                else if(type==Puzzle.ODD){runOnMainSync(()->{int size=level==0?4:5,index=a.challenge.data[0];((LinearLayout)view.board.getChildAt(index/size)).getChildAt(index%size).performClick();});}
                else{
                    LinearLayout row=(LinearLayout)view.board.getChildAt(0);int first=view.order[0];drag(row.getChildAt(0),row.getChildAt(1));check(view.order[1]==first,"drag reorders tiles");
                    runOnMainSync(()->{int[] sorted=view.order.clone();Arrays.sort(sorted);for(int i=0;i<sorted.length;i++)for(int j=i;j<sorted.length;j++)if(view.order[j]==sorted[i]){view.swap(i,j);break;}view.submit(Puzzle.join(view.order));});
                }
                waitForIdleSync();check(a.isFinishing(),"real puzzle inputs solve preview");
            }
            prefs.edit().putInt("puzzleType",Puzzle.COLORS).putBoolean("vibration",false).commit();
            ActivityMonitor alarmMonitor=addMonitor(AlarmActivity.class.getName(),null,false);
            runOnMainSync(()->Scheduler.test(context));
            getUiAutomation().executeShellCommand("input keyevent 223").close();SystemClock.sleep(14500);
            check(prefs.getBoolean("ringing",false)&&prefs.getBoolean("activeTest",false),"alarm delivered with screen off");
            boolean foreground=false;for(ActivityManager.RunningServiceInfo info:context.getSystemService(ActivityManager.class).getRunningServices(100))if(info.service.getClassName().equals(AlarmService.class.getName())&&info.foreground)foreground=true;
            check(foreground,"foreground sound service running");check(Store.activePuzzle(context).type==Puzzle.COLORS,"alarm uses selected puzzle");
            AlarmActivity alarm=(AlarmActivity)alarmMonitor.getLastActivity();removeMonitor(alarmMonitor);check(alarm!=null,"full-screen alarm opened automatically");screen=alarm;waitForIdleSync();capture(alarm,"alarm-colors");
            long at=prefs.getLong("activeAt",0);Puzzle active=Store.activePuzzle(context);check(!Scheduler.dismissCurrent(context,at,active,"wrong"),"wrong response keeps real alarm running");check(Scheduler.dismissCurrent(context,at,active,active.solution()),"correct response stops real alarm");
            runOnMainSync(alarm::finish);waitForIdleSync();
            out.putString("stream","PASS: "+checks+" interaction checks, real memory replay, drag, all puzzle solutions, screen-off alarm delivery and foreground service.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL after "+checks+" checks: "+e+"\n");}
        finally{
            if(screen!=null){final Activity a=screen;runOnMainSync(a::finish);}waitForIdleSync();
            context.stopService(new Intent(context,AlarmService.class));context.getSystemService(AlarmManager.class).cancel(Scheduler.pending(context,0,0,0,true));restore(before);
        }
        finish(code,out);
    }
}
