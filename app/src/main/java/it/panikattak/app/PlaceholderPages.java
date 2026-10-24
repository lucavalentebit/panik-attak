package it.panikattak.app;

import android.widget.LinearLayout;

final class PlaceholderPages {
    static void habits(MainActivity a,LinearLayout page){
        page.addView(Ui.bold(a,a.getString(R.string.page_habits),28,Ui.TEXT));Ui.space(page,22);
        LinearLayout card=Ui.card(a,Ui.CARD);card.addView(Ui.bold(a,a.getString(R.string.habits_empty_title),20,Ui.TEXT));Ui.space(card,12);
        card.addView(Ui.text(a,a.getString(R.string.habits_empty_hint),14,Ui.MUTED));page.addView(card);
    }
}
