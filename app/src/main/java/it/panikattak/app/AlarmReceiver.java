package it.panikattak.app;
import android.content.*;
import android.util.Log;
import org.json.*;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        boolean test=i.getBooleanExtra("test",false);long at=i.getLongExtra("at",0);
        if(test){String id=i.getStringExtra("routine");if(id==null)id=Store.DEFAULT;if(!Store.exists(c,id))return;AlarmQueue.offer(c,id,at,i.getLongExtra("start",at),i.getIntExtra("phase",3),true);}
        else{
            if(at!=Store.prefs(c).getLong("next",-1))return;
            try{
                String raw=Store.prefs(c).getString("nextBatch",null);
                if(raw==null){if(Store.enabled(c,Store.DEFAULT))AlarmQueue.offer(c,Store.DEFAULT,at,i.getLongExtra("start",at),i.getIntExtra("phase",0),false);}
                else {JSONArray batch=new JSONArray(raw);for(int n=0;n<batch.length();n++){JSONObject e=batch.getJSONObject(n);String id=e.getString("id");if(Store.enabled(c,id)&&e.getLong("start")!=Store.skipped(c,id))AlarmQueue.offer(c,id,e.getLong("at"),e.getLong("start"),e.getInt("phase"),false);}}
            }catch(JSONException e){Log.e("PanikAttak","Cannot read scheduled routines",e);}
            Scheduler.schedule(c,at);
        }
        try{Scheduler.syncSound(c,true);}catch(Exception e){Log.e("PanikAttak","Unable to start alarm",e);}
    }
}
