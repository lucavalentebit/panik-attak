package it.panikattak.app;

import android.app.AlertDialog;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;
import androidx.appcompat.widget.SwitchCompat;
import java.util.*;

final class RoutineUi {
    static void add(MainActivity a,LinearLayout page){
        LinearLayout panel=Ui.card(a,Ui.CARD);panel.addView(Ui.bold(a,a.getString(R.string.routines_title),21,Ui.TEXT));Ui.space(panel,8);
        panel.addView(Ui.text(a,a.getString(R.string.routines_hint),13,Ui.MUTED));Ui.space(panel,12);
        String selected=Store.selected(a);
        for(Store.Routine routine:Store.routines(a)){
            LinearLayout row=Ui.row(a);row.setPadding(Ui.dp(a,10),Ui.dp(a,8),Ui.dp(a,6),Ui.dp(a,8));
            row.setBackground(Ui.shape(routine.id.equals(selected)?0x1ADCFA84:0x0AFFFFFF,Ui.dp(a,16)));
            LinearLayout words=Ui.column(a);words.addView(Ui.bold(a,routine.name,17,routine.id.equals(selected)?Ui.LIME:Ui.TEXT));Ui.space(words,7);
            Plan plan=Store.plan(a,routine.id);List<String> days=new ArrayList<>();String[] labels=a.getResources().getStringArray(R.array.routine_days);for(int d=0;d<7;d++)if((plan.days&(1<<d))!=0)days.add(labels[d]);
            words.addView(Ui.text(a,a.getString(R.string.routine_summary,MainActivity.formatMinutes(plan.hour*60+plan.minute),String.join(" · ",days)),11,Ui.MUTED));
            words.setPadding(0,Ui.dp(a,5),Ui.dp(a,8),Ui.dp(a,5));words.setMinimumHeight(Ui.dp(a,56));words.setOnClickListener(v->{Store.select(a,routine.id);a.render();});words.setFocusable(true);Ui.weighted(row,words);
            SwitchCompat toggle=new SwitchCompat(a);toggle.setMinHeight(Ui.dp(a,56));toggle.setChecked(Store.enabled(a,routine.id));toggle.setContentDescription(a.getString(R.string.routine_switch,routine.name));
            toggle.setOnClickListener(v->{toggle.setChecked(Store.enabled(a,routine.id));if(Store.enabled(a,routine.id))a.startActivity(ChallengeActivity.intent(a,ChallengeActivity.PAUSE,routine.id));else a.activate(routine.id);});row.addView(toggle);panel.addView(row);Ui.space(panel,8);
        }
        panel.addView(Ui.button(a,a.getString(R.string.new_routine),0x12FFFFFF,Ui.LIME,()->nameDialog(a,0)));
        Ui.space(panel,8);panel.addView(Ui.button(a,a.getString(R.string.manage_routine),0x12FFFFFF,Ui.TEXT,()->manage(a)));page.addView(panel);Ui.space(page,24);
    }
    static void manage(MainActivity a){Ui.dialog(a).setTitle(Store.name(a,Store.selected(a))).setItems(new String[]{a.getString(R.string.rename_routine),a.getString(R.string.duplicate_routine),a.getString(R.string.delete_routine)},(d,n)->{if(n<2)nameDialog(a,n==0?1:2);else delete(a);}).setNegativeButton(R.string.cancel,null).show();}
    static void nameDialog(MainActivity a,int mode){
        String id=Store.selected(a);LinearLayout content=Ui.column(a);content.setPadding(Ui.dp(a,20),Ui.dp(a,8),Ui.dp(a,20),0);
        EditText input=new EditText(a);input.setSingleLine(true);input.setHint(R.string.routine_name_hint);input.setTextColor(Ui.TEXT);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        if(mode==1)input.setText(Store.name(a,id));if(mode==2)input.setText(a.getString(R.string.routine_copy,Store.name(a,id)));input.setSelectAllOnFocus(true);content.addView(input);
        TextView error=Ui.text(a,"",13,Ui.RED);content.addView(error);
        AlertDialog dialog=Ui.dialog(a).setTitle(mode==1?R.string.rename_routine:mode==2?R.string.duplicate_routine:R.string.new_routine).setView(content).setPositiveButton(R.string.save,null).setNegativeButton(R.string.cancel,null).create();
        dialog.setOnShowListener(v->{input.requestFocus();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);Ui.styleDialog(dialog);dialog.getButton(-1).setOnClickListener(b->{
            String name=input.getText().toString().trim();if(name.isEmpty()||!Store.nameAvailable(a,name,mode==1?id:"")){error.setText(R.string.routine_name_error);return;}
            if(mode==1)Store.rename(a,id,name);else{Store.create(a,name,mode==2);Toast.makeText(a,R.string.routine_created,Toast.LENGTH_LONG).show();}dialog.dismiss();a.render();
        });});dialog.show();
    }
    static void delete(MainActivity a){
        String id=Store.selected(a);if(Store.routines(a).size()==1){Toast.makeText(a,R.string.last_routine,Toast.LENGTH_LONG).show();return;}
        Ui.dialog(a).setTitle(R.string.delete_routine).setMessage(a.getString(R.string.delete_routine_message,Store.name(a,id))).setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.delete_routine,(d,w)->{
            if(Store.enabled(a,id)||AlarmQueue.contains(a,id)){a.pendingDelete=id;a.startActivityForResult(ChallengeActivity.intent(a,ChallengeActivity.PAUSE,id),82);}
            else{Store.delete(a,id);Scheduler.schedule(a);a.render();}
        }).show();
    }
}
