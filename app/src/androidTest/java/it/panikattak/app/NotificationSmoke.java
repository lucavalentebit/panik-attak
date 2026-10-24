package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.graphics.*;
import android.service.notification.StatusBarNotification;
import java.time.*;
import java.util.*;
import java.io.*;
import org.json.*;

/** Restores user settings after real alarm delivery, status lifecycle and swipe tests. */
public class NotificationSmoke extends ConfigSmoke {
    NotificationManager manager;
    int verifiedSwipeY;
    @Override public void onCreate(Bundle args){verifiedSwipeY=Integer.parseInt(args.getString("swipeY","0"));super.onCreate(args);}
    StatusBarNotification status(int id){for(var n:manager.getActiveNotifications())if(n.getId()==id)return n;return null;}
    void settle(){waitForIdleSync();SystemClock.sleep(250);}
    void screen(String name)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();if(b==null)return;try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}}
    @Override public void onStart(){
        context=getTargetContext();manager=context.getSystemService(NotificationManager.class);var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity main=null;String swipe="not tested";
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: existing alarm active.\n");finish(code,out);return;}
        try{
            check(manager.areNotificationsEnabled(),"notification permission already granted");
            runOnMainSync(()->{Scheduler.cancel(context);prefs.edit().clear().commit();});
            Plan p=new Plan();LocalTime clock=LocalTime.now();p.hour=clock.getHour();p.minute=clock.getMinute();Store.save(context,p);Store.rename(context,Store.DEFAULT,"Test notifica");Store.setEnabled(context,Store.DEFAULT,true);prefs.edit().putBoolean("vibration",false).commit();
            final long start=LocalDate.now().atTime(p.hour,p.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            main=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            check(status(100)==null,"enabled future recalls alone do not create running status");
            runOnMainSync(()->{SequenceStatus.delivered(context,Store.DEFAULT,start,start,0);SequenceStatus.refresh(context);});settle();
            check(status(100)!=null,"status displayed between recalls");Notification n=status(100).getNotification();
            check((n.flags&Notification.FLAG_ONGOING_EVENT)!=0&&(n.flags&Notification.FLAG_NO_CLEAR)!=0,"ongoing and clear-all protection requested");
            check((n.flags&Notification.FLAG_AUTO_CANCEL)==0,"tap does not automatically cancel");check(n.fullScreenIntent==null,"status does not wake full screen");
            check(manager.getNotificationChannel(SequenceStatus.CHANNEL).getImportance()==NotificationManager.IMPORTANCE_LOW&&manager.getNotificationChannel(SequenceStatus.CHANNEL).getSound()==null,"status channel silent");
            check(SequenceStatus.read(context).size()==1,"running occurrence persisted");check(n.publicVersion!=null&&!n.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("Test notifica"),"lock screen public content omits routine name");
            final MainActivity a=main;runOnMainSync(()->{Store.select(context,Store.DEFAULT);SequenceStatus.openFromNotification(a,new Intent().putExtra(SequenceStatus.ROUTINE,Store.DEFAULT).putExtra("statusStart",start));});check(Store.selected(context).equals(Store.DEFAULT),"notification routes to matching routine");
            check(!context.getSystemService(KeyguardManager.class).isKeyguardLocked(),"device unlocked for actual swipe test");
            getUiAutomation().executeShellCommand("cmd statusbar expand-notifications").close();SystemClock.sleep(800);
            var root=getUiAutomation().getRootInActiveWindow();var nodes=root==null?Collections.<AccessibilityNodeInfo>emptyList():root.findAccessibilityNodeInfosByText("Test notifica");
            screen("notification-before-swipe");check(!nodes.isEmpty()||verifiedSwipeY>0,"status located by accessibility or screenshot-verified swipe coordinate");Rect bounds=new Rect();if(!nodes.isEmpty())nodes.get(0).getBoundsInScreen(bounds);else bounds.set(200,verifiedSwipeY-1,400,verifiedSwipeY+1);
            int width=context.getResources().getDisplayMetrics().widthPixels;float x=bounds.centerX(),y=bounds.centerY();long down=SystemClock.uptimeMillis();touch(down,MotionEvent.ACTION_DOWN,x,y);for(int i=1;i<=15;i++){touch(down,MotionEvent.ACTION_MOVE,x+(width-8-x)*i/15,y);SystemClock.sleep(15);}touch(down,MotionEvent.ACTION_UP,width-8,y);SystemClock.sleep(700);
            swipe=status(100)==null?"dismissed by Android while unlocked":"retained after swipe";
            screen("notification-after-swipe");
            check(Store.enabled(context,Store.DEFAULT)&&SequenceStatus.current(context,System.currentTimeMillis()).size()==1&&prefs.getLong("next",0)>System.currentTimeMillis(),"swiping cannot stop routine or cancel its next alarm");
            getUiAutomation().executeShellCommand("cmd statusbar collapse").close();
            // Two real occurrences stay independent, including stop and pause.
            String second=Store.create(context,"Seconda routine",true);Store.setEnabled(context,second,true);
            runOnMainSync(()->{SequenceStatus.delivered(context,second,start,start,0);SequenceStatus.refresh(context);});settle();check(status(100).getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("2"),"overlapping running routines summarized");
            runOnMainSync(()->{Puzzle hard=Store.newPuzzle(context,0,2);check(Scheduler.pause(context,second,hard,hard.solution()),"pause protected action accepted");});settle();check(status(100)!=null&&SequenceStatus.current(context,System.currentTimeMillis()).size()==1,"pausing second retains first status");
            runOnMainSync(()->{AlarmQueue.offer(context,Store.DEFAULT,start,start,0,false);SequenceStatus.refresh(context);});settle();check(status(100)==null,"ringing routine not duplicated in silent status");
            runOnMainSync(()->{Puzzle puzzle=Store.activePuzzle(context);check(Scheduler.dismissCurrent(context,Store.DEFAULT,start,puzzle,puzzle.solution()),"recall dismissal accepted");});settle();check(status(100)!=null,"solving recall keeps status until subsequent recalls");
            runOnMainSync(()->{AlarmQueue.offer(context,Store.DEFAULT,start,start,0,false);Puzzle hard=Store.newPuzzle(context,0,2);check(Scheduler.dismissMorning(context,Store.DEFAULT,start,false,hard,hard.solution()),"stop occurrence accepted");});settle();check(status(100)==null&&SequenceStatus.current(context,System.currentTimeMillis()).isEmpty(),"stop occurrence removes status");
            prefs.edit().remove("skip").commit();
            runOnMainSync(()->{SequenceStatus.delivered(context,Store.DEFAULT,start,start,0);SequenceStatus.refresh(context,start+24*60*60*1000L);});settle();check(status(100)==null,"finished past occurrence removed");
            runOnMainSync(()->{AlarmQueue.offer(context,Store.DEFAULT,start,start,3,false);SequenceStatus.refresh(context,start+24*60*60*1000L);});check(SequenceStatus.read(context).size()==1,"last unresolved alarm remains running past end");
            runOnMainSync(()->{AlarmQueue.clear(context);prefs.edit().remove(SequenceStatus.KEY).commit();AlarmQueue.offer(context,Store.DEFAULT,start,start,3,true);SequenceStatus.refresh(context);});check(SequenceStatus.read(context).isEmpty(),"test alarm does not create a real running occurrence");
            runOnMainSync(()->{AlarmQueue.clear(context);Scheduler.cancel(context);});
            // Exercise AlarmManager -> receiver -> foreground sound notification -> silent gap.
            long at=System.currentTimeMillis()+3000;JSONArray batch=new JSONArray().put(new JSONObject().put("id",Store.DEFAULT).put("at",at).put("start",start).put("phase",0));
            runOnMainSync(()->{prefs.edit().putLong("next",at).putString("nextBatch",batch.toString()).commit();Scheduler.set(context,at,start,0,false);});
            long deadline=SystemClock.uptimeMillis()+10000;while(status(99)==null&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(200);settle();
            check(prefs.getBoolean("ringing",false)&&status(99)!=null,"real alarm delivered and foreground notification posted; ringing="+prefs.getBoolean("ringing",false)+", scheduled="+prefs.getLong("next",0)+", expected="+at);check((status(99).getNotification().flags&Notification.FLAG_ONGOING_EVENT)!=0,"ringing notification ongoing");
            runOnMainSync(()->{Puzzle puzzle=Store.activePuzzle(context);check(Scheduler.dismissCurrent(context,Store.DEFAULT,at,puzzle,puzzle.solution()),"real delivered alarm solved");});settle();check(status(99)==null&&status(100)!=null,"foreground sound ends and silent status remains");
            runOnMainSync(()->{Puzzle hard=Store.newPuzzle(context,0,2);Scheduler.pause(context,Store.DEFAULT,hard,hard.solution());});settle();check(status(100)==null,"pause removes final status");
            out.putString("stream","PASS: "+checks+" notification checks. Swipe observation: "+swipe+".\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL after "+checks+" checks: "+e+"; swipe="+swipe+"\n");}
        finally{
            try{getUiAutomation().executeShellCommand("cmd statusbar collapse").close();}catch(IOException ignored){}if(main!=null){final Activity a=main;runOnMainSync(a::finish);}runOnMainSync(()->{AlarmQueue.clear(context);context.stopService(new Intent(context,AlarmService.class));Scheduler.cancel(context);manager.cancel(100);restore(before);});
        }
        finish(code,out);
    }
}
