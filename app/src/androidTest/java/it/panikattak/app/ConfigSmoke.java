package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.android.material.timepicker.*;
import com.google.android.material.textfield.TextInputLayout;
import java.util.*;

public class ConfigSmoke extends UpgradeSmoke {
    @Override public void callActivityOnCreate(Activity activity,Bundle state){super.callActivityOnCreate(activity,state);activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);if(Build.VERSION.SDK_INT>=27){activity.setShowWhenLocked(true);activity.setTurnScreenOn(true);}}

    View text(View root,String value){
        if(root instanceof TextView&&((TextView)root).getText().toString().equals(value))return root;
        if(root instanceof android.view.ViewGroup){var group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=text(group.getChildAt(i),value);if(found!=null)return found;}}
        return null;
    }
    EditText edit(View root){if(root instanceof EditText)return (EditText)root;if(root instanceof android.view.ViewGroup){var group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++){EditText found=edit(group.getChildAt(i));if(found!=null)return found;}}return null;}
    void choose(MainActivity a,View button,int hour,int minute){
        runOnMainSync(button::performClick);waitForIdleSync();
        MaterialTimePicker picker=(MaterialTimePicker)a.getSupportFragmentManager().findFragmentByTag("timePicker");
        check(picker.getInputMode()==MaterialTimePicker.INPUT_MODE_CLOCK,"dial is initial mode");
        runOnMainSync(()->picker.getDialog().findViewById(com.google.android.material.R.id.material_timepicker_mode_button).performClick());waitForIdleSync();
        check(picker.getInputMode()==MaterialTimePicker.INPUT_MODE_KEYBOARD,"numeric fallback opens");
        runOnMainSync(()->{
            edit(picker.getDialog().findViewById(com.google.android.material.R.id.material_hour_text_input)).setText(String.format(Locale.ITALIAN,"%02d",hour));
            edit(picker.getDialog().findViewById(com.google.android.material.R.id.material_minute_text_input)).setText(String.format(Locale.ITALIAN,"%02d",minute));
            picker.getDialog().findViewById(com.google.android.material.R.id.material_timepicker_ok_button).performClick();
        });waitForIdleSync();
    }
    @Override public void onStart(){
        context=getTargetContext();var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity a=null;final AlertDialog[] dialog={null};
        if(prefs.getBoolean("ringing",false)){out.putString("stream","SKIP: real alarm active\n");finish(code,out);return;}
        try{
            prefs.edit().putBoolean("enabled",false).commit();Scheduler.cancel(context);Store.save(context,new Plan());
            a=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final MainActivity main=a;waitForIdleSync();capture(a,"main-v2");
            runOnMainSync(()->dialog[0]=PhaseEditor.show(main,1));waitForIdleSync();
            View root=dialog[0].getWindow().getDecorView();View fixed=text(root,context.getString(R.string.fixed_mode));check(fixed!=null&&fixed.isEnabled(),"fixed available in regular phase");runOnMainSync(fixed::performClick);waitForIdleSync();
            choose(a,text(root,context.getString(R.string.at_time,"08:00")),9,0);
            runOnMainSync(()->dialog[0].getButton(-1).performClick());check(dialog[0].isShowing()&&!Store.plan(context).fixed[1],"invalid fixed time blocks save");
            check(text(root,context.getString(R.string.invalid_fixed))!=null,"red validation message shown");
            choose(a,text(root,context.getString(R.string.at_time,"09:00")),8,10);
            SystemClock.sleep(350);capture(root,"phase-fixed");runOnMainSync(()->dialog[0].getButton(-1).performClick());waitForIdleSync();
            check(Store.plan(context).fixed[1]&&Store.plan(context).fixedMinutes[1]==490,"fixed time saved");
            runOnMainSync(()->dialog[0]=PhaseEditor.show(main,3));waitForIdleSync();root=dialog[0].getWindow().getDecorView();check(!text(root,context.getString(R.string.fixed_mode)).isEnabled(),"panic fixed disabled");runOnMainSync(()->dialog[0].dismiss());
            runOnMainSync(()->main.pickTime(R.string.first_alarm,450,m->{}));waitForIdleSync();
            MaterialTimePicker picker=(MaterialTimePicker)a.getSupportFragmentManager().findFragmentByTag("timePicker");capture(picker.getDialog().getWindow().getDecorView(),"time-dial");runOnMainSync(picker::dismissAllowingStateLoss);
            out.putString("stream","PASS: "+checks+" configuration checks: dial, keyboard fallback, invalid fixed time blocked, valid fixed time saved, Panic Mode restriction.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){out.putString("stream","FAIL after "+checks+": "+e+"\n");}
        finally{if(dialog[0]!=null)runOnMainSync(()->dialog[0].dismiss());if(a!=null){final MainActivity main=a;runOnMainSync(main::finish);}waitForIdleSync();restore(before);}
        finish(code,out);
    }
}
