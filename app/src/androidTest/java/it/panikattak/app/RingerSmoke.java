package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

/** Verify actual sound-mode transition and preservation of the user's DND policy. */
public class RingerSmoke extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Context c=getTargetContext();AudioManager a=c.getSystemService(AudioManager.class);NotificationManager n=c.getSystemService(NotificationManager.class);
        int mode=a.getRingerMode(),volume=a.getStreamVolume(AudioManager.STREAM_RING),filter=n.getCurrentInterruptionFilter();
        NotificationManager.Policy policy=CallRinger.policy(n);Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;
        try{
            CallRinger.Result result=CallRinger.enable(c);Thread.sleep(400);
            if(result!=CallRinger.Result.READY)throw new AssertionError("Result="+result+" mode="+a.getRingerMode()+" volume="+a.getStreamVolume(AudioManager.STREAM_RING)+" muted="+a.isStreamMute(AudioManager.STREAM_RING));
            if(a.isStreamMute(AudioManager.STREAM_RING)||a.getStreamVolume(AudioManager.STREAM_RING)<Math.ceil(a.getStreamMaxVolume(AudioManager.STREAM_RING)*0.6))throw new AssertionError("Ringer still muted or too quiet");
            if(n.getCurrentInterruptionFilter()!=filter||!policy.equals(CallRinger.policy(n)))throw new AssertionError("DND filter or allowed callers changed");
            out.putString("stream","PASS: ringer audible at "+a.getStreamVolume(AudioManager.STREAM_RING)+"/"+a.getStreamMaxVolume(AudioManager.STREAM_RING)+"; DND filter and complete policy unchanged.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL: "+e+"\n");}
        finally{
            // Restore the initial user sound mode; this is test cleanup, never production behavior.
            a.setStreamVolume(AudioManager.STREAM_RING,volume,0);a.setRingerMode(mode);
        }
        finish(code,out);
    }
}
