package it.panikattak.app;

import android.animation.ValueAnimator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

final class PhaseEditor {
    static BottomSheetDialog show(MainActivity a, int phase) {
        String routine = Store.selected(a);
        Plan candidate = Store.plan(a, routine);
        boolean adding = phase == candidate.phaseCount();
        if (adding) candidate.append(candidate.states().get(0), 10);
        String intervalId = candidate.intervalIds[phase];

        BottomSheetDialog sheet = new BottomSheetDialog(a);
        boolean animate = ValueAnimator.areAnimatorsEnabled();
        sheet.setDismissWithAnimation(animate);
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        sheet.getBehavior().setSkipCollapsed(true);
        if (!animate) sheet.getBehavior().setDraggable(false);

        LinearLayout content = Ui.column(a);
        content.setPadding(Ui.dp(a,20), Ui.dp(a,22), Ui.dp(a,20), Ui.dp(a,20));
        TextView error = Ui.text(a, "", 13, Ui.RED);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        TextView title = Ui.bold(a, "", 22, Ui.TEXT);
        ViewCompat.setAccessibilityHeading(title, true);
        content.addView(title);
        Ui.space(content, 16);

        TextView state = Ui.button(a,"",Ui.CARD,Ui.TEXT,()->{});
        TextView start = Ui.button(a,"",Ui.CARD,Ui.TEXT,()->{});
        TextView end = Ui.button(a,"",Ui.CARD,Ui.TEXT,()->{});
        TextView fixedTime = Ui.button(a,"",Ui.CARD,Ui.LIME,()->{});
        EditText duration = new EditText(a);
        duration.setSingleLine(true);
        duration.setInputType(InputType.TYPE_CLASS_NUMBER);
        duration.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        duration.setText(a.getString(R.string.phase_duration_value,candidate.durations[phase]));
        duration.setId(View.generateViewId());
        TextView durationLabel = Ui.text(a,a.getString(R.string.phase_duration),14,Ui.MUTED);
        durationLabel.setLabelFor(duration.getId());

        RadioGroup modes = new RadioGroup(a);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton intervalMode = mode(a, modes, R.string.interval_mode);
        RadioButton fixedMode = mode(a, modes, R.string.fixed_mode);
        NumberPicker cadence = new NumberPicker(a);
        cadence.setMinValue(1);
        cadence.setMaxValue(30);
        cadence.setValue(candidate.intervals[phase]);
        cadence.setWrapSelectorWheel(false);
        LinearLayout cadenceField = Ui.column(a);
        cadenceField.addView(Ui.text(a,a.getString(R.string.interval_minutes),14,Ui.MUTED));
        cadenceField.addView(cadence);
        TextView panic = Ui.text(a,a.getString(R.string.panic_interval),12,Ui.MUTED);
        NestedScrollView scroll = new NestedScrollView(a);
        scroll.addView(content);

        Runnable update = () -> {
            int base = candidate.hour * 60 + candidate.minute;
            int begin = candidate.offset(phase);
            String name = candidate.phaseName(phase);
            title.setText(adding ? a.getString(R.string.phase_add_title) : name);
            ViewCompat.setAccessibilityPaneTitle(scroll, title.getText());
            state.setText(a.getString(R.string.state_value,name));
            start.setText(a.getString(R.string.phase_editor_time,a.getString(R.string.start_time),MainActivity.formatMinutes(base+begin)));
            end.setText(a.getString(R.string.phase_editor_time,a.getString(R.string.end_time),MainActivity.formatMinutes(base+begin+candidate.durations[phase])));
            fixedTime.setText(a.getString(R.string.at_time, MainActivity.formatMinutes(candidate.fixedMinutes[phase])));
            boolean isPanic = candidate.phaseLevel(phase) == Plan.PANIC;
            fixedMode.setEnabled(!isPanic);
            panic.setVisibility(isPanic ? View.VISIBLE : View.GONE);
            fixedTime.setVisibility(candidate.fixed[phase] ? View.VISIBLE : View.GONE);
            cadenceField.setVisibility(candidate.fixed[phase] ? View.GONE : View.VISIBLE);
            error.setText(error(a, candidate, phase));
        };
        duration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                candidate.durations[phase] = s.length()==0 ? 0 : Integer.parseInt(s.toString());
                update.run();
            }
            @Override public void afterTextChanged(Editable s){}
        });
        state.setOnClickListener(v -> StateEditor.choose(a,candidate,candidate.stateId(phase),id -> {
            candidate.setState(phase,id);
            modes.check(candidate.fixed[phase] ? fixedMode.getId() : intervalMode.getId());
            if (adding) cadence.setValue(candidate.defaultCadence(phase));
            update.run();
        }));
        start.setOnClickListener(v -> a.pickTime(R.string.start_time, candidate.hour*60+candidate.minute+candidate.offset(phase), m -> {
            int old = candidate.hour*60+candidate.minute;
            if (phase==0) {
                candidate.hour=m/60;
                candidate.minute=m%60;
            } else {
                int previousDuration=Math.floorMod(m-old,1440)-candidate.offset(phase-1);
                if (previousDuration<1||previousDuration>Plan.MAX_DURATION) {
                    error.setText(R.string.invalid_duration);
                    return;
                }
                candidate.durations[phase-1]=previousDuration;
            }
            update.run();
        }));
        end.setOnClickListener(v -> a.pickTime(R.string.end_time,
                candidate.hour*60+candidate.minute+candidate.offset(phase)+candidate.durations[phase], m -> {
            int begin = candidate.offset(phase);
            int newDuration = Math.floorMod(m-(candidate.hour*60+candidate.minute)-begin,1440);
            if(newDuration<1||newDuration>Plan.MAX_DURATION){
                error.setText(R.string.invalid_duration);
                return;
            }
            duration.setText(a.getString(R.string.phase_duration_value,newDuration));
        }));
        fixedTime.setOnClickListener(v -> a.pickTime(R.string.fixed_mode,candidate.fixedMinutes[phase],m -> {
            candidate.fixedMinutes[phase]=m;
            update.run();
        }));
        modes.check(candidate.fixed[phase] ? fixedMode.getId() : intervalMode.getId());
        modes.setOnCheckedChangeListener((group,id) -> {
            candidate.fixed[phase]=id==fixedMode.getId()&&candidate.phaseLevel(phase)!=Plan.PANIC;
            update.run();
        });

        content.addView(state);
        Ui.space(content,12);
        content.addView(durationLabel);
        content.addView(duration);
        if (!adding) {
            content.addView(start);
            Ui.space(content,8);
            content.addView(end);
            Ui.space(content,8);
            content.addView(Ui.text(a,a.getString(R.string.phase_times_hint),12,Ui.MUTED));
        }
        content.addView(modes);
        content.addView(panic);
        content.addView(cadenceField);
        content.addView(fixedTime);
        content.addView(error);
        Ui.space(content,12);

        boolean[] changed = {false};
        TextView save = Ui.button(a,a.getString(adding ? R.string.phase_create : R.string.save),
                Ui.LIME,Ui.INK,()->{});
        save.setId(android.R.id.button1);
        save.setOnClickListener(v -> {
            cadence.clearFocus();
            candidate.intervals[phase]=cadence.getValue();
            if (!candidate.valid()) {
                error.setText(error(a,candidate,phase));
                return;
            }
            if (!canSave(a,routine,phase,intervalId,adding)) {
                sheet.cancel();
                return;
            }
            save.setEnabled(false);
            changed[0]=true;
            if (adding) a.revealFromPhase=a.routinePhase;
            apply(a,candidate,phase,sheet);
        });
        if(!adding&&candidate.phaseCount()>1) {
            TextView remove=Ui.button(a,a.getString(R.string.phase_remove),0x12FFFFFF,Ui.RED,()->{});
            remove.setOnClickListener(v -> Ui.dialog(a).setTitle(R.string.phase_remove).setMessage(R.string.phase_remove_detail)
                    .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.phase_remove,(dialog,action) -> {
                        if (!canSave(a,routine,phase,intervalId,false)) {
                            sheet.cancel();
                            return;
                        }
                        Plan remaining=Store.plan(a,routine);
                        if (!remaining.remove(phase)||!remaining.valid()) {
                            error.setText(R.string.phase_invalid_remove);
                            return;
                        }
                        save.setEnabled(false);
                        remove.setEnabled(false);
                        changed[0]=true;
                        apply(a,remaining,Math.min(phase,remaining.phaseCount()-1),sheet);
                    }).show());
            content.addView(remove);
            Ui.space(content,8);
        }
        LinearLayout actions = Ui.row(a);
        Ui.weighted(actions,Ui.button(a,a.getString(R.string.cancel),0x12FFFFFF,Ui.TEXT,sheet::cancel));
        actions.addView(new View(a),new LinearLayout.LayoutParams(Ui.dp(a,8),1));
        Ui.weighted(actions,save);
        content.addView(actions);
        update.run();

        sheet.setContentView(scroll);
        sheet.setOnDismissListener(dialog -> {
            if (a.isFinishing()||a.isDestroyed()||a.destination!=MainActivity.DETAIL||!routine.equals(a.detailRoutine)) return;
            if (changed[0]) a.render();
            View focus=a.shell.body.findViewWithTag(adding&&!changed[0] ? PhaseCarousel.ADD_TAG : PhaseCarousel.editTag(a.routinePhase));
            if (focus!=null) focus.post(focus::requestFocus);
        });
        sheet.getWindow().setDimAmount(.36f);
        sheet.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (!animate) sheet.getWindow().setWindowAnimations(0);
        sheet.show();
        return sheet;
    }

    private static RadioButton mode(MainActivity a, RadioGroup group, int label) {
        RadioButton button=new RadioButton(a);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setButtonTintList(Ui.controlTint());
        group.addView(button);
        return button;
    }

    private static String error(MainActivity a, Plan plan, int phase) {
        if(plan.durations[phase]<1||plan.durations[phase]>Plan.MAX_DURATION) return a.getString(R.string.invalid_duration);
        if(plan.total()>1440) return a.getString(R.string.phase_total_limit);
        return plan.valid() ? "" : a.getString(R.string.invalid_fixed);
    }

    private static boolean canSave(MainActivity a,String routine,int phase,String intervalId,boolean adding){
        if (!routine.equals(Store.selected(a))||!Store.exists(a,routine)) return false;
        Plan current=Store.plan(a,routine);
        return adding ? phase==current.phaseCount() : phase<current.phaseCount()&&intervalId.equals(current.intervalIds[phase]);
    }

    private static void apply(MainActivity a,Plan plan,int phase,BottomSheetDialog sheet){
        a.plan=plan;
        a.routinePhase=phase;
        a.savePlan();
        sheet.cancel();
    }
}
