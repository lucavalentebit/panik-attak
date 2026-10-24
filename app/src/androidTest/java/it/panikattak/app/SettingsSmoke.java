package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import androidx.appcompat.widget.SwitchCompat;
import java.time.*;
import java.util.*;

/** Settings change presentation, never alarm identity, schedule, sound volume or DND. */
public class SettingsSmoke extends NavigationSmoke {
    NotificationManager manager;
    Notification notification(int id){for(var entry:manager.getActiveNotifications())if(entry.getId()==id)return entry.getNotification();return null;}
    SwitchCompat toggle(String key){return screen.shell.body.findViewWithTag(key);}
    void clickToggle(String key){runOnMainSync(()->toggle(key).performClick());settle();}
    @Override public void onStart(){
        context=getTargetContext();manager=context.getSystemService(NotificationManager.class);var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;
        AudioManager audio=context.getSystemService(AudioManager.class);int alarmVolume=audio.getStreamVolume(AudioManager.STREAM_ALARM),ringVolume=audio.getStreamVolume(AudioManager.STREAM_RING),filter=manager.getCurrentInterruptionFilter();
        NotificationManager.Policy policy=manager.isNotificationPolicyAccessGranted()?CallRinger.policy(manager):null;
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: existing alarm active.\n");finish(code,out);return;}
        try{
            runOnMainSync(()->{Scheduler.cancel(context);prefs.edit().clear().commit();});
            check(AppSettings.showStatus(context)&&!AppSettings.showNames(context),"migration defaults: status on, names private");
            Plan plan=new Plan();LocalTime time=LocalTime.now();plan.hour=time.getHour();plan.minute=time.getMinute();Store.save(context,plan);Store.rename(context,Store.DEFAULT,"Routine riservata");Store.setEnabled(context,Store.DEFAULT,true);prefs.edit().putBoolean("vibration",false).commit();
            long start=LocalDate.now().atTime(plan.hour,plan.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            screen=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));runOnMainSync(()->screen.openRoutine(Store.DEFAULT));settle();settings();
            check(toggle(AppSettings.STATUS).isChecked()&&!toggle(AppSettings.LOCK_NAMES).isChecked(),"controls show persisted defaults");
            check(screen.countdown==null&&screen.destination==MainActivity.SETTINGS,"settings route has no countdown");
            int maximum=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);check(text(screen.shell.body,alarmVolume==0?context.getString(R.string.settings_volume_zero):context.getString(R.string.settings_volume_level,alarmVolume,maximum))!=null,"alarm volume reflects actual system value");
            check(text(screen.shell.body,context.getString(R.string.settings_exact))!=null&&text(screen.shell.body,context.getString(R.string.settings_battery))!=null,"reliability rows visible");capture(screen,"settings-top-alpha4");
            runOnMainSync(()->screen.shell.scroll.smoothScrollTo(0,screen.shell.body.getHeight()));SystemClock.sleep(650);capture(screen,"settings-bottom-alpha4");
            long next=prefs.getLong("next",0);runOnMainSync(()->{SequenceStatus.delivered(context,Store.DEFAULT,start,start,0);SequenceStatus.refresh(context);});settle();
            check(notification(100)!=null,"running status appears");clickToggle(AppSettings.STATUS);
            check(!AppSettings.showStatus(context)&&notification(100)==null,"status switch removes only silent card");check(Store.enabled(context,Store.DEFAULT)&&prefs.getLong("next",0)==next&&SequenceStatus.read(context).size()==1,"hiding status preserves occurrence and scheduled alarm");
            clickToggle(AppSettings.STATUS);check(notification(100)!=null,"re-enabling restores current running status");
            clickToggle(AppSettings.LOCK_NAMES);check(AppSettings.showNames(context)&&notification(100).visibility==Notification.VISIBILITY_PUBLIC,"name switch updates existing status notification");
            clickToggle(AppSettings.LOCK_NAMES);Notification status=notification(100);check(status.visibility==Notification.VISIBILITY_PRIVATE&&status.publicVersion!=null&&!status.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString().contains("riservata"),"private lock-screen content omits routine name");
            runOnMainSync(()->{AlarmQueue.offer(context,Store.DEFAULT,start,start,0,false);SequenceStatus.refresh(context);context.startService(new Intent(context,AlarmService.class));});settle();
            String puzzle=Store.activePuzzle(context).encode();check(notification(99)!=null&&notification(99).visibility==Notification.VISIBILITY_PRIVATE,"real foreground alarm uses private default");
            clickToggle(AppSettings.STATUS);check(notification(99)!=null&&prefs.getBoolean("ringing",false),"status off keeps foreground alarm ringing");
            clickToggle(AppSettings.LOCK_NAMES);check(notification(99).visibility==Notification.VISIBILITY_PUBLIC&&Store.activePuzzle(context).encode().equals(puzzle)&&prefs.getLong("next",0)==next,"live privacy update retains puzzle and schedule");
            check(notification(99).fullScreenIntent!=null&&(notification(99).flags&Notification.FLAG_ONGOING_EVENT)!=0,"alarm full-screen route and ongoing flags retained");
            runOnMainSync(()->{AlarmQueue.clear(context);context.stopService(new Intent(context,AlarmService.class));});settle();
            recreate(false);check(!toggle(AppSettings.STATUS).isChecked()&&toggle(AppSettings.LOCK_NAMES).isChecked(),"preferences survive recreation");back();check(screen.destination==MainActivity.DETAIL&&screen.detailRoutine.equals(Store.DEFAULT),"Back returns to correct routine");settings();
            for(SettingsPage.Target target:new SettingsPage.Target[]{SettingsPage.Target.SOUND,SettingsPage.Target.DND,SettingsPage.Target.NOTIFICATIONS,SettingsPage.Target.EXACT,SettingsPage.Target.FULLSCREEN,SettingsPage.Target.BATTERY}){
                Intent expected=SettingsPage.target(screen,target);IntentFilter match=new IntentFilter(expected.getAction());if(expected.getData()!=null)match.addDataScheme(expected.getData().getScheme());
                ActivityMonitor monitor=addMonitor(match,new ActivityResult(Activity.RESULT_CANCELED,null),true);View row=screen.shell.body.findViewWithTag(target);check(row!=null,"settings row available: "+target);
                runOnMainSync(row::performClick);check(monitor.getHits()==1,"row launches Android settings action: "+target);removeMonitor(monitor);
            }
            Intent channel=SettingsPage.target(screen,SettingsPage.Target.NOTIFICATIONS);check(context.getPackageName().equals(channel.getStringExtra(Settings.EXTRA_APP_PACKAGE))&&"alarm".equals(channel.getStringExtra(Settings.EXTRA_CHANNEL_ID)),"alarm channel link carries correct package and channel");
            IntentFilter fallback=new IntentFilter(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);fallback.addDataScheme("package");ActivityMonitor monitor=addMonitor(fallback,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            runOnMainSync(()->SettingsPage.open(screen,new Intent("it.panikattak.NO_SUCH_SETTINGS")));check(monitor.getHits()==1,"unsupported Android action falls back to app information");removeMonitor(monitor);
            check(audio.getStreamVolume(AudioManager.STREAM_ALARM)==alarmVolume&&audio.getStreamVolume(AudioManager.STREAM_RING)==ringVolume,"visiting settings does not change system volume");
            check(manager.getCurrentInterruptionFilter()==filter&&(policy==null||policy.equals(CallRinger.policy(manager))),"visiting settings does not change DND or exceptions");
            out.putString("stream","PASS: "+checks+" settings checks, including live foreground notification updates and Android intent routing.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL after "+checks+" checks: "+e+"\n");}
        finally{if(screen!=null)runOnMainSync(screen::finish);settle();runOnMainSync(()->{AlarmQueue.clear(context);context.stopService(new Intent(context,AlarmService.class));Scheduler.cancel(context);manager.cancel(99);manager.cancel(100);restore(before);});}
        finish(code,out);
    }
}
