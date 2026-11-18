package it.panikattak.app;
import android.animation.ValueAnimator;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.View;
import android.widget.*;
final class PhaseEditor {
    static BottomSheetDialog show(MainActivity a,int phase){
        String routine = Store.selected(a);
        Plan candidate=Store.plan(a);
        LinearLayout content=Ui.column(a);content.setPadding(Ui.dp(a,20),Ui.dp(a,22),Ui.dp(a,20),Ui.dp(a,20));
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
        BottomSheetDialog sheet = new BottomSheetDialog(a);
        boolean animate = ValueAnimator.areAnimatorsEnabled();
        sheet.setDismissWithAnimation(animate);
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        sheet.getBehavior().setSkipCollapsed(true);
        if (!animate) sheet.getBehavior().setDraggable(false);

        Ui.space(content, 12);
        TextView title = Ui.bold(a, Plan.NAMES[phase], 22, Ui.TEXT);
        ViewCompat.setAccessibilityHeading(title, true);
        content.addView(title, 0);
        View headingSpace = new View(a);
        content.addView(headingSpace, 1, new LinearLayout.LayoutParams(1, Ui.dp(a, 16)));
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        LinearLayout actions = Ui.row(a);
        TextView cancel = Ui.button(a, a.getString(R.string.cancel), 0x12FFFFFF, Ui.TEXT, sheet::cancel);
        Ui.weighted(actions, cancel);
        actions.addView(new View(a), new LinearLayout.LayoutParams(Ui.dp(a, 8), 1));
        TextView save = Ui.button(a, a.getString(R.string.save), Ui.LIME, Ui.INK, () -> {
            interval.clearFocus();
            candidate.intervals[phase] = interval.getValue();
            if (!candidate.valid()) {
                error.setText(R.string.invalid_fixed);
                return;
            }
            if (!routine.equals(Store.selected(a)) || !Store.exists(a, routine)) {
                sheet.cancel();
                return;
            }
            sheet.findViewById(android.R.id.button1).setEnabled(false);
            a.plan = candidate;
            a.save();
            sheet.cancel();
        });
        save.setId(android.R.id.button1);
        Ui.weighted(actions, save);
        content.addView(actions);

        NestedScrollView scroll = new NestedScrollView(a);
        ViewCompat.setAccessibilityPaneTitle(scroll, Plan.NAMES[phase]);
        scroll.addView(content);
        sheet.setContentView(scroll);
        sheet.setOnDismissListener(dialog -> {
            if (a.destination != MainActivity.DETAIL || !routine.equals(a.detailRoutine)) return;
            View opener = a.shell.body.findViewWithTag(PhaseCarousel.editTag(phase));
            if (opener != null && a.routinePhase == phase) opener.requestFocus();
        });
        sheet.getWindow().setDimAmount(.36f);
        if (!animate) sheet.getWindow().setWindowAnimations(0);
        sheet.show();
        return sheet;
    }
}
