package it.panikattak.app;

import android.app.AlertDialog;
import android.text.InputFilter;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class StateEditor {
    static void choose(MainActivity a, Plan plan, String current, Consumer<String> chosen) {
        List<String> ids = plan.states();
        List<String> names = new ArrayList<>();
        for (String id : ids) names.add(plan.stateName(id));
        names.add(a.getString(R.string.state_new));
        Ui.dialog(a).setTitle(R.string.state_label)
                .setSingleChoiceItems(names.toArray(new String[0]), ids.indexOf(current), (dialog, item) -> {
                    dialog.dismiss();
                    if (item == ids.size()) name(a, plan, null, chosen);
                    else chosen.accept(ids.get(item));
                }).setNegativeButton(R.string.cancel, null).show();
    }

    static void manage(MainActivity a, Plan plan, Consumer<Plan> save) {
        List<String> ids = plan.states();
        List<String> names = new ArrayList<>();
        for (String id : ids) names.add(plan.stateName(id));
        Ui.dialog(a).setTitle(R.string.state_manage)
                .setItems(names.toArray(new String[0]), (dialog, item) -> configure(a, plan, ids.get(item), save))
                .setNeutralButton(R.string.state_new, (dialog, action) -> name(a, plan, null, id -> save.accept(plan)))
                .setNegativeButton(R.string.cancel, null).show();
    }

    private static void configure(MainActivity a, Plan plan, String id, Consumer<Plan> save) {
        String[] puzzles = a.getResources().getStringArray(R.array.puzzle_types);
        String[] choices = new String[puzzles.length + 1];
        choices[0] = a.getString(R.string.state_puzzle_default, puzzles[plan.puzzleType]);
        System.arraycopy(puzzles, 0, choices, 1, puzzles.length);
        int[] selected = {plan.statePuzzles.getOrDefault(id, Plan.INHERIT) + 1};
        AlertDialog.Builder dialog = Ui.dialog(a).setTitle(plan.stateName(id))
                .setSingleChoiceItems(choices, selected[0], (window, item) -> selected[0] = item)
                .setPositiveButton(R.string.save, (window, action) -> {
                    if (selected[0] == 0) plan.statePuzzles.remove(id);
                    else plan.statePuzzles.put(id, selected[0] - 1);
                    save.accept(plan);
                }).setNegativeButton(R.string.cancel, null);
        if (plan.customStates.containsKey(id)) {
            dialog.setNeutralButton(R.string.state_rename, (window, action) -> name(a, plan, id, renamed -> save.accept(plan)));
        }
        dialog.show();
    }

    private static void name(MainActivity a, Plan plan, String id, Consumer<String> chosen) {
        LinearLayout fields = Ui.column(a);
        fields.setPadding(Ui.dp(a, 20), Ui.dp(a, 8), Ui.dp(a, 20), 0);
        EditText input = new EditText(a);
        input.setSingleLine(true);
        input.setHint(R.string.state_name);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        if (id != null) input.setText(plan.stateName(id));
        fields.addView(input);
        TextView error = Ui.text(a, "", 13, Ui.RED);
        fields.addView(error);
        AlertDialog dialog = Ui.dialog(a).setTitle(id == null ? R.string.state_new : R.string.state_rename)
                .setView(fields).setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null).create();
        dialog.setOnShowListener(window -> {
            Ui.styleDialog(dialog);
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                String name = input.getText().toString().trim();
                if (!plan.stateNameAvailable(name, id)) {
                    error.setText(R.string.state_name_error);
                    return;
                }
                String key = id == null ? UUID.randomUUID().toString() : id;
                plan.customStates.put(key, name);
                dialog.dismiss();
                chosen.accept(key);
            });
        });
        dialog.show();
    }
}
