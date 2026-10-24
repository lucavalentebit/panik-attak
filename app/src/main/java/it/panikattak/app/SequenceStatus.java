package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.util.Log;
import org.json.*;
import java.time.ZoneId;
import java.util.*;

/** Occurrences that have actually delivered a recall, including the silent gaps. */
final class SequenceStatus {
    static final String KEY="sequenceRuns", CHANNEL="sequence_status", ROUTINE="statusRoutine";
    static final int NOTIFICATION_ID=100;
    static final class Run {
        final String routine;final long start,lastAt;final int phase;
        long nextAt;boolean ringing;
        Run(String routine,long start,long lastAt,int phase){this.routine=routine;this.start=start;this.lastAt=lastAt;this.phase=phase;}
    }
    static LinkedHashMap<String,Run> read(Context c){
        LinkedHashMap<String,Run> runs=new LinkedHashMap<>();
        try{JSONArray a=new JSONArray(Store.prefs(c).getString(KEY,"[]"));for(int n=0;n<a.length();n++){JSONObject o=a.getJSONObject(n);Run r=new Run(o.getString("routine"),o.getLong("start"),o.getLong("lastAt"),o.getInt("phase"));runs.put(r.routine,r);}}
        catch(JSONException e){Log.e("PanikAttak","Cannot restore sequence status",e);}
        return runs;
    }
    static void write(Context c,Collection<Run> runs){
        JSONArray a=new JSONArray();try{for(Run r:runs)a.put(new JSONObject().put("routine",r.routine).put("start",r.start).put("lastAt",r.lastAt).put("phase",r.phase));}catch(JSONException e){throw new IllegalStateException(e);}
        String value=a.toString();if(!value.equals(Store.prefs(c).getString(KEY,"[]")))Store.prefs(c).edit().putString(KEY,value).commit();
    }
    static void delivered(Context c,String id,long start,long at,int phase){
        try{var runs=read(c);runs.put(id,new Run(id,start,at,phase));write(c,runs.values());}
        catch(RuntimeException e){Log.e("PanikAttak","Unable to record sequence notification state",e);}
    }
    static List<Run> current(Context c,long now){
        var runs=read(c);List<AlarmQueue.Entry> queue=AlarmQueue.read(c);
        // Also covers updating the app while an older-version alarm is already ringing.
        for(var entry:queue)if(!entry.test)runs.put(entry.routine,new Run(entry.routine,entry.start,entry.at,entry.phase));
        Iterator<Run> iterator=runs.values().iterator();while(iterator.hasNext()){
            Run r=iterator.next();
            if(!Store.enabled(c,r.routine)||Store.skipped(c,r.routine)==r.start){iterator.remove();continue;}
            for(var entry:queue)if(!entry.test&&entry.routine.equals(r.routine)&&entry.start==r.start)r.ringing=true;
            Plan.Event next=Store.plan(c,r.routine).next(Math.max(now,r.lastAt),Store.skipped(c,r.routine),ZoneId.systemDefault());
            if(next!=null&&next.start==r.start)r.nextAt=next.at;
            if(!r.ringing&&r.nextAt==0)iterator.remove();
        }
        write(c,runs.values());return new ArrayList<>(runs.values());
    }
    static void refresh(Context c){
        // Status must never block delivery, dismissal or scheduling of an actual alarm.
        try{refresh(c,System.currentTimeMillis());}catch(RuntimeException e){Log.e("PanikAttak","Unable to update sequence notification",e);}
    }
    static void refresh(Context c,long now){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        List<Run> waiting=new ArrayList<>();for(Run r:current(c,now))if(!r.ringing)waiting.add(r);
        if(waiting.isEmpty()){manager.cancel(NOTIFICATION_ID);return;}
        NotificationChannel channel=new NotificationChannel(CHANNEL,c.getString(R.string.sequence_channel),NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(c.getString(R.string.sequence_channel_description));channel.setSound(null,null);channel.enableVibration(false);channel.setShowBadge(false);manager.createNotificationChannel(channel);
        waiting.sort(Comparator.comparingLong(r->r.nextAt));Run first=waiting.get(0);
        String title=waiting.size()==1?c.getString(R.string.sequence_running,Store.name(c,first.routine)):c.getString(R.string.sequences_running,waiting.size());
        String text=c.getString(R.string.sequence_next,Plan.NAMES[first.phase],Store.time(first.nextAt));
        Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(waiting.size()==1)open.putExtra(ROUTINE,first.routine).putExtra("statusStart",first.start);
        PendingIntent intent=PendingIntent.getActivity(c,21,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.InboxStyle lines=new Notification.InboxStyle();long expires=now;
        for(Run r:waiting){lines.addLine(c.getString(R.string.sequence_waiting_line,Store.name(c,r.routine),Store.time(r.nextAt)));expires=Math.max(expires,r.start+Store.plan(c,r.routine).total()*60000L);}
        Notification publicVersion=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_alarm).setContentTitle(c.getString(R.string.sequence_generic)).setContentText(c.getString(R.string.sequence_open)).setOngoing(true).build();
        Notification notification=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_alarm).setContentTitle(title).setContentText(text).setStyle(lines)
            .setCategory(Notification.CATEGORY_STATUS).setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true).setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publicVersion).setContentIntent(intent)
            .setTimeoutAfter(Math.max(1,expires-now)).build();
        // Requests the strongest ordinary ongoing behavior. Android 14+ still permits
        // individual swipes while unlocked; no fake call/media style or repost loop.
        notification.flags|=Notification.FLAG_NO_CLEAR;
        manager.notify(NOTIFICATION_ID,notification);
    }
    static void openFromNotification(MainActivity activity,Intent intent){
        String id=intent.getStringExtra(ROUTINE);if(id==null)return;
        for(Run run:current(activity,System.currentTimeMillis()))if(run.routine.equals(id)&&run.start==intent.getLongExtra("statusStart",-1)){Store.select(activity,id);break;}
        intent.removeExtra(ROUTINE);intent.removeExtra("statusStart");
    }
}
