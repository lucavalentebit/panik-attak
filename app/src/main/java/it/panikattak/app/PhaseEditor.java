package it.panikattak.app;
import android.app.AlertDialog;
import android.view.View;
import android.widget.*;
final class PhaseEditor {
    static AlertDialog show(MainActivity a,int phase){
        Plan candidate=Store.plan(a);
        LinearLayout content=Ui.column(a);content.setPadding(Ui.dp(a,18),Ui.dp(a,10),Ui.dp(a,18),Ui.dp(a,10));
        TextView error=Ui.text(a,"",13,Ui.RED);
        TextView start=Ui.button(a,"",Ui.CARD,Ui.TEXT,()->{}),end=Ui.button(a,"",Ui.CARD,Ui.TEXT,()->{}),fixedTime=Ui.button(a,"",Ui.CARD,Ui.LIME,()->{});
        RadioGroup modes=new RadioGroup(a);modes.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton intervalMode=new RadioButton(a);intervalMode.setId(View.generateViewId());intervalMode.setText(R.string.interval_mode);modes.addView(intervalMode);
        RadioButton fixedMode=new RadioButton(a);fixedMode.setId(View.generateViewId());fixedMode.setText(R.string.fixed_mode);fixedMode.setEnabled(phase!=3);modes.addView(fixedMode);
        android.content.res.ColorStateList tint=new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.LIME,Ui.MUTED});intervalMode.setButtonTintList(tint);fixedMode.setButtonTintList(tint);
        NumberPicker interval=new NumberPicker(a);interval.setMinValue(1);interval.setMaxValue(30);interval.setValue(candidate.intervals[phase]);interval.setWrapSelectorWheel(false);
        LinearLayout intervalField=Ui.column(a);intervalField.addView(Ui.text(a,a.getString(R.string.interval_minutes),14,Ui.MUTED));intervalField.addView(interval);
        Runnable update=()->{
            int base=candidate.hour*60+candidate.minute;
            start.setText(a.getString(R.string.start_time)+" · "+MainActivity.formatMinutes(base+candidate.offset(phase)));
            end.setText(a.getString(R.string.end_time)+" · "+MainActivity.formatMinutes(base+candidate.offset(phase)+candidate.durations[phase]));
            fixedTime.setText(a.getString(R.string.at_time,MainActivity.formatMinutes(candidate.fixedMinutes[phase])));
            fixedTime.setVisibility(candidate.fixed[phase]?View.VISIBLE:View.GONE);intervalField.setVisibility(candidate.fixed[phase]?View.GONE:View.VISIBLE);
            error.setText(candidate.valid()?"":a.getString(R.string.invalid_fixed));
        };
        start.setOnClickListener(v->a.pickTime(R.string.start_time,candidate.hour*60+candidate.minute+candidate.offset(phase),m->{
            int old=candidate.hour*60+candidate.minute;
            if(phase==0){candidate.hour=m/60;candidate.minute=m%60;}
            else {int boundary=Math.floorMod(m-old,1440),previous=candidate.offset(phase-1);
                if(boundary-previous<1||boundary-previous>120){error.setText(R.string.invalid_duration);return;}
                candidate.durations[phase-1]=boundary-previous;
            }update.run();
        }));
        end.setOnClickListener(v->a.pickTime(R.string.end_time,candidate.hour*60+candidate.minute+candidate.offset(phase)+candidate.durations[phase],m->{
            int boundary=Math.floorMod(m-(candidate.hour*60+candidate.minute),1440),begin=candidate.offset(phase);
            if(boundary-begin<1||boundary-begin>120){error.setText(R.string.invalid_duration);return;}
            candidate.durations[phase]=boundary-begin;update.run();
        }));
        fixedTime.setOnClickListener(v->a.pickTime(R.string.fixed_mode,candidate.fixedMinutes[phase],m->{candidate.fixedMinutes[phase]=m;update.run();}));
        modes.check(candidate.fixed[phase]?fixedMode.getId():intervalMode.getId());
        modes.setOnCheckedChangeListener((group,id)->{candidate.fixed[phase]=phase!=3&&id==fixedMode.getId();update.run();});
        content.addView(start);Ui.space(content,8);content.addView(end);Ui.space(content,8);content.addView(Ui.text(a,a.getString(R.string.phase_times_hint),12,Ui.MUTED));content.addView(modes);
        if(phase==3)content.addView(Ui.text(a,a.getString(R.string.panic_interval),12,Ui.MUTED));
        content.addView(intervalField);content.addView(fixedTime);content.addView(error);update.run();
        ScrollView scroll=new ScrollView(a);scroll.addView(content);
        AlertDialog dialog=Ui.dialog(a).setTitle(Plan.NAMES[phase]).setView(scroll).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(v->{Ui.styleDialog(dialog);dialog.getButton(-1).setOnClickListener(b->{interval.clearFocus();candidate.intervals[phase]=interval.getValue();if(!candidate.valid()){error.setText(R.string.invalid_fixed);return;}a.plan=candidate;a.save();dialog.dismiss();});});dialog.show();dialog.getWindow().setBackgroundDrawable(Ui.shape(Ui.CARD,Ui.dp(a,24)));return dialog;
    }
}
