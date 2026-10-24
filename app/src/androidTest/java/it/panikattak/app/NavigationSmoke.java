package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.*;
import android.view.*;
import java.time.*;
import java.util.*;

/** Exercises navigation against real views and recreation, then restores user routines. */
public class NavigationSmoke extends ConfigSmoke {
    MainActivity screen;
    void settle(){waitForIdleSync();SystemClock.sleep(400);}
    View description(View view,String label){
        if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=description(group.getChildAt(i),label);if(found!=null)return found;}}return null;
    }
    void tab(int destination){runOnMainSync(()->screen.shell.navigation.setSelectedItemId(destination));settle();}
    void click(String label){View button=text(screen.shell.body,label);check(button!=null,"button exists: "+label);runOnMainSync(button::performClick);settle();}
    void settings(){View button=description(screen.shell.header,"Impostazioni");check(button!=null,"settings entry exists");runOnMainSync(button::performClick);settle();}
    void back(){runOnMainSync(()->screen.getOnBackPressedDispatcher().onBackPressed());settle();}
    void recreate(boolean landscape){
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);MainActivity previous=screen;
        runOnMainSync(()->{if(landscape)previous.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);else previous.recreate();});
        Activity replacement=waitForMonitorWithTimeout(monitor,5000);removeMonitor(monitor);check(replacement instanceof MainActivity&&replacement!=previous,"new activity created");screen=(MainActivity)replacement;settle();
    }
    @Override public void onStart(){
        context=getTargetContext();var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;ChallengeActivity gate=null;
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: existing alarm active.\n");finish(code,out);return;}
        try{
            runOnMainSync(()->{Scheduler.cancel(context);prefs.edit().clear().commit();});
            Plan early=new Plan();LocalTime clock=LocalTime.now().plusHours(2);early.hour=clock.getHour();early.minute=clock.getMinute();Store.save(context,early);Store.rename(context,Store.DEFAULT,"Lavoro");Store.setEnabled(context,Store.DEFAULT,true);
            String later=Store.create(context,"Weekend",true);Plan late=Store.plan(context);clock=LocalTime.now().plusHours(4);late.hour=clock.getHour();late.minute=clock.getMinute();Store.save(context,late);Store.setEnabled(context,later,true);
            Store.select(context,Store.DEFAULT);String tied=Store.create(context,"Studio",true);Store.setEnabled(context,tied,true);Store.select(context,later);
            screen=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            check(screen.destination==MainActivity.TODAY,"launch opens Today");
            var next=TodayPage.upcoming(screen,System.currentTimeMillis());check(next.size()==3&&next.get(0).routine.id.equals(Store.DEFAULT)&&next.get(0).event.at==next.get(1).event.at,"Today includes earliest routine and equal-time tie");
            check(Store.selected(context).equals(later),"dashboard does not change selected routine");check(text(screen.shell.body,context.getString(R.string.more_routines,1))!=null,"tie count is visible");
            String countdown=screen.countdown.getText().toString();SystemClock.sleep(1200);check(!screen.countdown.getText().toString().equals(countdown),"global countdown ticks");capture(screen,"pages-today");
            runOnMainSync(()->{Store.setEnabled(context,tied,false);screen.render();});settle();click(context.getString(R.string.open_routine));
            check(screen.destination==MainActivity.DETAIL&&screen.detailRoutine.equals(Store.DEFAULT),"dashboard opens correct routine");check(text(screen.shell.body,"Con calma")!=null,"phase editor reachable in detail");
            tab(MainActivity.ROUTINES);check(screen.destination==MainActivity.ROUTINES&&text(screen.shell.body,"Con calma")==null,"routine list is separate from editor");capture(screen,"pages-routines");
            View row=description(screen.shell.body,"Apri routine Weekend");check(row!=null,"routine row accessible");runOnMainSync(row::performClick);settle();check(screen.destination==MainActivity.DETAIL&&screen.detailRoutine.equals(later),"routine row opens its own editor");
            ActivityMonitor gateMonitor=addMonitor(ChallengeActivity.class.getName(),null,false);runOnMainSync(()->screen.editTime());gate=(ChallengeActivity)waitForMonitorWithTimeout(gateMonitor,3000);removeMonitor(gateMonitor);
            check(gate!=null&&gate.routine.equals(later)&&gate.challenge.level==2,"active editor still uses target routine hard puzzle");final ChallengeActivity challenge=gate;runOnMainSync(()->challenge.apply("wrong"));check(!gate.isFinishing(),"wrong answer does not unlock editing");runOnMainSync(gate::finish);settle();gate=null;
            check(Store.plan(context,later).hour==late.hour&&screen.destination==MainActivity.DETAIL,"cancelled gate preserves configuration and route");
            runOnMainSync(()->screen.shell.scroll.scrollTo(0,Ui.dp(screen,350)));settle();int scroll=screen.shell.scroll.getScrollY();check(scroll>0,"detail scrolls independently");
            settings();check(screen.destination==MainActivity.SETTINGS&&screen.shell.body.getChildCount()==0,"settings has an empty body");capture(screen,"pages-settings");
            recreate(false);check(screen.destination==MainActivity.SETTINGS&&screen.shell.body.getChildCount()==0,"empty Settings survives recreation");back();
            check(screen.destination==MainActivity.DETAIL&&screen.detailRoutine.equals(later),"Back restores exact routine after recreation");check(Math.abs(screen.shell.scroll.getScrollY()-scroll)<5,"detail scroll restored");
            recreate(false);check(screen.destination==MainActivity.DETAIL&&Math.abs(screen.shell.scroll.getScrollY()-scroll)<5,"detail route and scroll survive recreation directly");
            capture(screen,"pages-detail");back();check(screen.destination==MainActivity.ROUTINES,"detail Back returns to list");back();check(screen.destination==MainActivity.TODAY,"list Back returns to Today");
            tab(MainActivity.HABITS);check(text(screen.shell.body,context.getString(R.string.habits_empty_hint))!=null&&screen.countdown==null,"Habits is an honest empty state without countdown loop");capture(screen,"pages-habits");settings();back();check(screen.destination==MainActivity.HABITS,"Settings Back returns to originating tab");
            recreate(true);check(screen.destination==MainActivity.HABITS,"destination survives landscape change");check(screen.shell.navigation.getHeight()>0&&screen.shell.scroll.getHeight()>0,"landscape keeps navigation and content available");capture(screen,"pages-landscape");
            // Simulate status entry while another page is open; identity is still validated.
            Plan running=new Plan();clock=LocalTime.now();running.hour=clock.getHour();running.minute=clock.getMinute();Store.save(context,Store.DEFAULT,running);
            long anchor=LocalDate.now().atTime(running.hour,running.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            runOnMainSync(()->{SequenceStatus.delivered(context,Store.DEFAULT,anchor,anchor,0);screen.onNewIntent(new Intent().putExtra(MainActivity.OPEN_TODAY,true).putExtra(SequenceStatus.ROUTINE,Store.DEFAULT).putExtra("statusStart",anchor));});settle();
            check(screen.destination==MainActivity.DETAIL&&screen.detailRoutine.equals(Store.DEFAULT),"single notification opens matching routine detail");
            runOnMainSync(()->screen.onNewIntent(new Intent().putExtra(MainActivity.OPEN_TODAY,true)));settle();check(screen.destination==MainActivity.TODAY,"summary notification opens Today");check(text(screen.shell.body,context.getString(R.string.running_count,1))!=null,"running sequence visible on Today");
            runOnMainSync(()->{screen.openRoutine(later);screen.onNewIntent(new Intent().putExtra(MainActivity.OPEN_TODAY,true).putExtra(SequenceStatus.ROUTINE,Store.DEFAULT).putExtra("statusStart",anchor-1));});settle();check(screen.destination==MainActivity.TODAY,"stale notification cannot open a different occurrence");
            runOnMainSync(()->{for(var routine:Store.routines(context))Store.setEnabled(context,routine.id,false);Scheduler.schedule(context);screen.navigate(MainActivity.TODAY);});settle();check(text(screen.shell.body,context.getString(R.string.no_active_routines))!=null&&screen.countdown==null,"no routines gives empty Today");
            out.putString("stream","PASS: "+checks+" navigation checks, including recreation, landscape, countdown, protected edits and notification routes.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL after "+checks+" checks: "+e+"\n");}
        finally{if(gate!=null){final Activity a=gate;runOnMainSync(a::finish);}if(screen!=null)runOnMainSync(screen::finish);settle();runOnMainSync(()->{AlarmQueue.clear(context);context.stopService(new Intent(context,AlarmService.class));Scheduler.cancel(context);restore(before);});}
        finish(code,out);
    }
}
