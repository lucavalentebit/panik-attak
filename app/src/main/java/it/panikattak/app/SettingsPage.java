package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.appcompat.widget.SwitchCompat;

final class SettingsPage {
    enum Target { SOUND, DND, NOTIFICATIONS, SEQUENCE_CHANNEL, EXACT, FULLSCREEN, BATTERY }
    static boolean channelEnabled(Context c,String id){NotificationChannel channel=c.getSystemService(NotificationManager.class).getNotificationChannel(id);return channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    static boolean alarmNotifications(Context c){return c.getSystemService(NotificationManager.class).areNotificationsEnabled()&&channelEnabled(c,"alarm");}
    static Intent appInfo(Context c){return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+c.getPackageName()));}
    static Intent target(Context c,Target target){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        switch(target){
            case SOUND:return new Intent(Settings.ACTION_SOUND_SETTINGS);
            case DND:return new Intent(manager.isNotificationPolicyAccessGranted()?Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS:Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            case NOTIFICATIONS:case SEQUENCE_CHANNEL:
                if(!manager.areNotificationsEnabled())return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.getPackageName());
                return new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,target==Target.NOTIFICATIONS?"alarm":SequenceStatus.CHANNEL);
            case EXACT:if(Build.VERSION.SDK_INT>=31)return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+c.getPackageName()));break;
            case FULLSCREEN:if(Build.VERSION.SDK_INT>=34)return new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+c.getPackageName()));break;
            case BATTERY:break;
        }
        return appInfo(c);
    }
    static void open(MainActivity a,Target target){open(a,target(a,target));}
    static void open(MainActivity a,Intent intent){
        try{a.startActivity(intent);}catch(ActivityNotFoundException|SecurityException missing){
            try{a.startActivity(appInfo(a));}catch(ActivityNotFoundException|SecurityException fallback){Toast.makeText(a,R.string.settings_unavailable,Toast.LENGTH_LONG).show();}
        }
    }
    static LinearLayout section(MainActivity a,LinearLayout page,int title){
        LinearLayout card=Ui.card(a,Ui.CARD);card.addView(Ui.bold(a,a.getString(title),18,Ui.TEXT));Ui.space(card,8);page.addView(card);Ui.space(page,16);return card;
    }
    static void separator(LinearLayout card){View line=new View(card.getContext());line.setBackgroundColor(0x229FA695);card.addView(line,new LinearLayout.LayoutParams(-1,Ui.dp(card.getContext(),1)));}
    static LinearLayout labels(MainActivity a,String title,String summary){
        LinearLayout words=Ui.column(a);words.addView(Ui.bold(a,title,15,Ui.TEXT));Ui.space(words,6);words.addView(Ui.text(a,summary,12,Ui.MUTED));words.setPadding(0,Ui.dp(a,14),Ui.dp(a,12),Ui.dp(a,14));return words;
    }
    static void link(MainActivity a,LinearLayout card,int title,String summary,Target target){
        LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,64));Ui.weighted(row,labels(a,a.getString(title),summary));row.addView(Ui.text(a,"↗",20,Ui.LIME));
        row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x229FA695),Ui.shape(android.graphics.Color.TRANSPARENT,Ui.dp(a,12)),null));
        row.setFocusable(true);row.setContentDescription(a.getString(title)+". "+summary);row.setTag(target);row.setOnClickListener(v->open(a,target));card.addView(row);
    }
    static void toggle(MainActivity a,LinearLayout card,int title,int hint,String key,boolean value){
        LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,64));Ui.weighted(row,labels(a,a.getString(title),a.getString(hint)));
        SwitchCompat toggle=new SwitchCompat(a);toggle.setTag(key);toggle.setContentDescription(a.getString(title));toggle.setMinHeight(Ui.dp(a,48));toggle.setChecked(value);toggle.setThumbTintList(Ui.controlTint());
        toggle.setOnCheckedChangeListener((button,on)->AppSettings.set(a,key,on));row.addView(toggle);row.setOnClickListener(v->toggle.performClick());card.addView(row);
    }
    static void add(MainActivity a,LinearLayout page){
        AlarmService.ensureChannel(a);SequenceStatus.ensureChannel(a);
        NotificationManager manager=a.getSystemService(NotificationManager.class);AudioManager audio=a.getSystemService(AudioManager.class);
        LinearLayout sound=section(a,page,R.string.settings_sound);
        int volume=audio.getStreamVolume(AudioManager.STREAM_ALARM),max=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        link(a,sound,R.string.settings_alarm_volume,volume==0?a.getString(R.string.settings_volume_zero):a.getString(R.string.settings_volume_level,volume,max),Target.SOUND);separator(sound);
        String dnd=manager.isNotificationPolicyAccessGranted()?a.getString(manager.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL?R.string.settings_dnd_off:R.string.settings_dnd_check):a.getString(R.string.settings_dnd_access);
        link(a,sound,R.string.settings_calls,dnd,Target.DND);Ui.space(sound,8);sound.addView(Ui.text(a,a.getString(R.string.settings_calls_hint),12,Ui.MUTED));

        LinearLayout notifications=section(a,page,R.string.settings_notifications);
        link(a,notifications,R.string.settings_alarm_notifications,a.getString(alarmNotifications(a)?R.string.settings_allowed:R.string.settings_allow),Target.NOTIFICATIONS);separator(notifications);
        toggle(a,notifications,R.string.settings_sequence_status,R.string.settings_sequence_hint,AppSettings.STATUS,AppSettings.showStatus(a));
        if(!manager.areNotificationsEnabled()||!channelEnabled(a,SequenceStatus.CHANNEL))link(a,notifications,R.string.settings_sequence_channel,a.getString(R.string.settings_blocked_android),Target.SEQUENCE_CHANNEL);
        separator(notifications);toggle(a,notifications,R.string.settings_lock_names,R.string.settings_lock_hint,AppSettings.LOCK_NAMES,AppSettings.showNames(a));

        LinearLayout reliability=section(a,page,R.string.settings_reliability);
        if(Build.VERSION.SDK_INT>=31){link(a,reliability,R.string.settings_exact,a.getString(Scheduler.allowed(a)?R.string.settings_allowed:R.string.settings_allow),Target.EXACT);separator(reliability);}
        if(Build.VERSION.SDK_INT>=34){link(a,reliability,R.string.settings_fullscreen,a.getString(a.fullscreen()?R.string.settings_allowed:R.string.settings_allow),Target.FULLSCREEN);separator(reliability);}
        boolean unrestricted=a.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(a.getPackageName());
        link(a,reliability,R.string.settings_battery,a.getString(unrestricted?R.string.settings_battery_exempt:R.string.settings_battery_check),Target.BATTERY);Ui.space(reliability,8);reliability.addView(Ui.text(a,a.getString(R.string.settings_battery_hint),12,Ui.MUTED));
        HabitsPage.settings(a,page);
        try{TextView version=Ui.text(a,"Panik Attak · "+a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName,12,Ui.MUTED);version.setGravity(Gravity.CENTER);page.addView(version);}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
    }
}
