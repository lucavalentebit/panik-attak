package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Isolated fixture + real alarm callbacks. No synthetic data enters the user's history. */
public class HabitsSmoke extends NavigationSmoke {
    void loaded(){for(int n=0;n<30;n++){settle();if(screen.shell.body.findViewWithTag("habitsLoaded")!=null)return;}throw new AssertionError("History load timed out");}
    List<HabitStats.Session> sessions(){return HabitStats.sessions(HabitHistory.events(context));}
    HabitStats.Event event(String id,long start,String kind,int minute){
        HabitStats.Event e=new HabitStats.Event();e.routine=id;e.name=id.equals("work")?"Lavoro":"Weekend";e.session=id+":"+start;e.id=e.session+":"+kind;e.kind=kind;e.start=start;e.first=start;e.last=start;e.scheduled=start;e.expected=1;e.wall=start+minute*60000L;e.elapsed=86400000L+minute*60000L;e.boot="fixture";e.zone=ZoneId.systemDefault().getId();e.offset=Instant.ofEpochMilli(e.wall).atZone(ZoneId.systemDefault()).getOffset().getTotalSeconds();return e;
    }
    void add(String id,long start,String kind,int minute){HabitHistory.append(context,event(id,start,kind,minute),false);}
    void fixture(){
        LocalDate today=LocalDate.now();for(int n=0;n<6;n++){long start=today.minusDays(6-n).atTime(8,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();HabitStats.Event delivery=event("work",start,"delivered",0);delivery.phase=n==4?3:0;HabitHistory.append(context,delivery,false);add("work",start,"dismissed",n==5?3:n*2+2);add("work",start,n==5?"finished":"stop",n==5?3:12+n*2);}
        long start=today.atTime(9,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();add("weekend",start,"delivered",0);add("weekend",start,"pause",10);
        start=today.atTime(10,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();HabitStats.Event partial=event("weekend",start,"delivered",0);partial.covered=false;HabitHistory.append(context,partial,false);add("weekend",start,"stop",12);
    }
    @Override public void onStart(){
        context=getTargetContext();var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;AlarmActivity alarm=null;
        if(prefs.getBoolean("ringing",false)){result.putString("stream","SKIP: existing alarm active.\n");finish(code,result);return;}
        try{
            runOnMainSync(()->{Scheduler.cancel(context);prefs.edit().clear().putLong(HabitHistory.SINCE,0).putBoolean("history.introSeen",true).commit();HabitHistory.erase(context);});
            screen=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));tab(MainActivity.HABITS);loaded();
            check(text(screen.shell.body,context.getString(R.string.habits_empty_hint))!=null&&sessions().isEmpty(),"empty state has no invented past data");capture(screen,"habits-empty-alpha5");
            fixture();runOnMainSync(screen::render);loaded();
            HabitStats.Summary summary=new HabitStats.Summary(sessions());check(summary.complete==7&&summary.panic==1&&summary.responses.size()==6,"fixture denominators and exclusions");
            check(text(screen.shell.body,"5′ 00″")!=null&&text(screen.shell.body,"16′ 00″")!=null,"rendered medians match hand calculations");
            HabitChart chart=screen.shell.body.findViewWithTag("habitScatter");check(chart!=null&&chart.points.size()==12,"scatter shows response and stop/finish separately");capture(screen,"habits-summary-alpha5");
            runOnMainSync(()->screen.shell.scroll.scrollTo(0,Ui.dp(screen,600)));settle();capture(screen,"habits-chart-alpha5");
            runOnMainSync(()->screen.shell.scroll.scrollTo(0,Ui.dp(screen,1100)));settle();capture(screen,"habits-distribution-alpha5");
            runOnMainSync(()->screen.shell.scroll.scrollTo(0,screen.shell.body.getHeight()));settle();capture(screen,"habits-history-alpha5");
            runOnMainSync(()->screen.shell.body.findViewWithTag("period:7").performClick());loaded();check(HabitsPage.days(context)==7,"period chip persists selection");
            runOnMainSync(()->HabitsPage.preference(screen,HabitsPage.ROUTINE,"weekend"));loaded();check(HabitsPage.selected(context,sessions()).size()==2,"routine filter includes archived routine independently");
            check(text(screen.shell.body,"Prima risposta")!=null&&text(screen.shell.body,"5′ 00″")==null,"small samples don't display a stable summary");
            recreate(false);loaded();check(HabitsPage.routine(context).equals("weekend")&&HabitsPage.days(context)==7,"filters and page survive recreation");
            runOnMainSync(()->{prefs.edit().putBoolean(HabitsPage.CLOCK,true).putString(HabitsPage.ROUTINE,"").apply();screen.render();});loaded();chart=screen.shell.body.findViewWithTag("habitScatter");check(chart.clock,"local-clock chart switch persisted");
            IntentFilter exportFilter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);exportFilter.addCategory(Intent.CATEGORY_OPENABLE);exportFilter.addDataType("text/csv");ActivityMonitor picker=addMonitor(exportFilter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            runOnMainSync(()->screen.exportHabits(true));check(picker.getHits()==1&&screen.exportDays==7,"filtered CSV opens Android document saver");removeMonitor(picker);
            File csv=new File(context.getCacheDir(),"habits-test-export.csv");runOnMainSync(()->screen.onActivityResult(84,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(csv))));HabitHistory.IO.submit(()->{}).get(5,TimeUnit.SECONDS);String exported=new String(java.nio.file.Files.readAllBytes(csv.toPath()),java.nio.charset.StandardCharsets.UTF_8);check(exported.split("\r\n").length==9&&exported.contains("explicit_stop_ms"),"export writes selected sessions and eligibility columns");
            runOnMainSync(()->HabitsPage.detail(screen,sessions().get(0)));settle();getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);settle();
            settings();check(screen.shell.body.findViewWithTag(HabitHistory.ENABLED)!=null,"history controls live in Settings");back();loaded();
            android.content.res.Configuration original=new android.content.res.Configuration(screen.getResources().getConfiguration());
            runOnMainSync(()->{android.content.res.Configuration large=new android.content.res.Configuration(original);large.fontScale=1.5f;screen.getResources().updateConfiguration(large,screen.getResources().getDisplayMetrics());screen.render();});loaded();capture(screen,"habits-large-text-alpha5");
            check(screen.shell.body.findViewWithTag("period:7").getHeight()>=Ui.dp(screen,48),"large text keeps usable filter touch targets");
            runOnMainSync(()->{screen.getResources().updateConfiguration(original,screen.getResources().getDisplayMetrics());screen.render();});loaded();
            // Real queue/solution/dismissal callbacks, in the isolated history database.
            runOnMainSync(()->{HabitHistory.erase(context);prefs.edit().putLong(HabitHistory.SINCE,0).remove(SequenceStatus.KEY).commit();Plan p=new Plan();LocalTime time=LocalTime.now();p.hour=time.getHour();p.minute=time.getMinute();Store.save(context,p);Store.rename(context,Store.DEFAULT,"Routine vera");Store.setEnabled(context,Store.DEFAULT,true);prefs.edit().putBoolean("vibration",false).commit();});
            Plan p=Store.plan(context);long start=LocalDate.now().atTime(p.hour,p.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            runOnMainSync(()->{AlarmQueue.offer(context,Store.DEFAULT,start,start,0,false);AlarmQueue.offer(context,Store.DEFAULT,start,start,0,false);});
            check(HabitHistory.events(context).stream().filter(e->e.kind.equals("delivered")).count()==1,"duplicate queue delivery recorded once");
            alarm=(AlarmActivity)startActivitySync(new Intent(context,AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();final AlarmActivity actual=alarm;
            runOnMainSync(()->{actual.puzzleView.submit("incorrect");actual.puzzleView.submit("incorrect");actual.puzzleView.submit(actual.puzzle.solution());});
            check(sessions().get(0).response==null&&sessions().get(0).outcome.equals("running"),"solving alone doesn't count as a dismissed alarm");check(sessions().get(0).wrong==2,"wrong attempts aggregated without answer text");
            check(!Scheduler.dismissCurrent(context,Store.DEFAULT,start-1,actual.puzzle,actual.puzzle.solution()),"stale dismissal rejected");
            View dismiss=text(actual.getWindow().getDecorView(),context.getString(R.string.dismiss_current));runOnMainSync(dismiss::performClick);settle();alarm=null;
            check(sessions().get(0).response!=null&&sessions().get(0).outcome.equals("running"),"accepted recall dismissal leaves later recalls running");
            runOnMainSync(()->{Store.rename(context,Store.DEFAULT,"Nome nuovo");Puzzle hard=Store.newPuzzle(context,0,2);Scheduler.pause(context,Store.DEFAULT,hard,hard.solution());});
            check(sessions().get(0).outcome.equals("pause")&&sessions().get(0).name.equals("Routine vera"),"pause closes same session and rename preserves snapshot");
            int size=HabitHistory.events(context).size();runOnMainSync(()->{AlarmQueue.offer(context,Store.DEFAULT,start+1000,start+1000,3,true);HabitHistory.current(context,"solved");AlarmQueue.clear(context);});check(HabitHistory.events(context).size()==size,"12-second test events excluded");
            HabitHistory.setEnabled(context,false);HabitHistory.append(context,event("off",start,"delivered",0),false);check(HabitHistory.events(context).size()==size,"opt-out prevents further records");HabitHistory.setEnabled(context,true);
            // Import an outbox event twice, as after a crash between import and acknowledgment.
            HabitStats.Event pending=event("outbox",start,"delivered",0);pending.name="";pending.config="";pending.covered=false;
            try(HabitHistory.Database outbox=new HabitHistory.Database(context,true)){HabitHistory.insert(outbox.getWritableDatabase(),pending,false);}HabitHistory.drain(context);
            try(HabitHistory.Database outbox=new HabitHistory.Database(context,true)){HabitHistory.insert(outbox.getWritableDatabase(),pending,false);}HabitHistory.drain(context);
            check(HabitHistory.events(context).stream().filter(e->e.id.equals(pending.id)).count()==1,"outbox replay is idempotent");
            check(sessions().stream().filter(s->s.routine.equals("outbox")).allMatch(s->s.partial),"before-unlock events don't invent full coverage or name");
            long old=System.currentTimeMillis()-40*86400000L;add("old",old,"delivered",0);HabitHistory.retain(context,30);check(sessions().stream().noneMatch(s->s.routine.equals("old")),"shorter retention deletes old sessions");
            try(HabitHistory.Database outbox=new HabitHistory.Database(context,true)){HabitHistory.insert(outbox.getWritableDatabase(),event("erase-pending",start,"delivered",0),false);}HabitHistory.erase(context);HabitHistory.drain(context);check(HabitHistory.events(context).isEmpty(),"erase removes history and pending outbox without resurrection");
            // Natural completion is recorded only after accepted dismissal of the final pending recall.
            runOnMainSync(()->{prefs.edit().putLong(HabitHistory.SINCE,0).remove(SequenceStatus.KEY).commit();Plan shortPlan=new Plan();LocalTime time=LocalTime.now().minusMinutes(4);shortPlan.hour=time.getHour();shortPlan.minute=time.getMinute();Arrays.fill(shortPlan.durations,1);Arrays.fill(shortPlan.intervals,1);Store.save(context,shortPlan);Store.setEnabled(context,Store.DEFAULT,true);long anchor=LocalDate.now().atTime(shortPlan.hour,shortPlan.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();if(anchor>System.currentTimeMillis())anchor-=86400000L;for(int n=0;n<4;n++)AlarmQueue.offer(context,Store.DEFAULT,anchor+n*60000L,anchor,n,false);Puzzle current=Store.activePuzzle(context);Scheduler.dismissCurrent(context,Store.DEFAULT,anchor+180000L,current,current.solution());});
            check(sessions().get(0).outcome.equals("finished")&&sessions().get(0).complete()&&sessions().get(0).superseded==3,"final dismissal produces completion, superseded recalls preserved");
            check(!HabitHistory.json(HabitHistory.events(context).get(0)).has("answer"),"history doesn't store answers");
            long midStart=start-120000L;
            runOnMainSync(()->{Plan mid=Store.plan(context);LocalTime time=Instant.ofEpochMilli(midStart).atZone(ZoneId.systemDefault()).toLocalTime();mid.hour=time.getHour();mid.minute=time.getMinute();Store.save(context,mid);prefs.edit().putLong(HabitHistory.SINCE,0).commit();AlarmQueue.offer(context,Store.DEFAULT,midStart,midStart,0,false);HabitHistory.setEnabled(context,false);});
            check(prefs.getBoolean("ringing",false)&&Store.enabled(context,Store.DEFAULT),"mid-session opt-out leaves routine and alarm running");
            HabitHistory.setEnabled(context,true);runOnMainSync(()->{Puzzle hard=Store.newPuzzle(context,0,2);Scheduler.pause(context,Store.DEFAULT,hard,hard.solution());});check(sessions().stream().anyMatch(s->s.start==midStart&&s.partial&&!s.complete()),"opt-out marks the interrupted recording interval partial");
            runOnMainSync(()->AlarmQueue.clear(context));HabitHistory.IO.submit(()->{}).get(5,TimeUnit.SECONDS);
            String normalNamespace=HabitHistory.testNamespace;HabitHistory.testNamespace="instrumentation-failure-";
            File blockedHistory=context.getDatabasePath(HabitHistory.testNamespace+HabitHistory.DATABASE),blockedOutbox=context.createDeviceProtectedStorageContext().getDatabasePath(HabitHistory.testNamespace+HabitHistory.OUTBOX);blockedHistory.mkdir();blockedOutbox.mkdir();
            try{
                runOnMainSync(()->AlarmQueue.offer(context,Store.DEFAULT,start+2000,start+2000,0,false));check(prefs.getBoolean("ringing",false),"history storage failure cannot prevent queue delivery");
                Puzzle current=Store.activePuzzle(context);runOnMainSync(()->check(Scheduler.dismissCurrent(context,Store.DEFAULT,start+2000,current,current.solution()),"history storage failure cannot block valid dismissal"));
            }finally{HabitHistory.testNamespace=normalNamespace;blockedHistory.delete();blockedOutbox.delete();}
            result.putString("stream","PASS: "+checks+" habits checks, including real solution/dismissal callbacks, charts, filters, export, opt-out, retention and outbox replay.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){result.putString("stream","FAIL after "+checks+" checks: "+e+"\n");e.printStackTrace();}
        finally{if(alarm!=null){final Activity ended=alarm;runOnMainSync(ended::finish);}if(screen!=null)runOnMainSync(screen::finish);settle();runOnMainSync(()->{AlarmQueue.clear(context);context.stopService(new Intent(context,AlarmService.class));Scheduler.cancel(context);HabitHistory.erase(context);restore(before);});}
        finish(code,result);
    }
}
