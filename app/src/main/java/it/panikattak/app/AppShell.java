package it.panikattak.app;

import android.content.res.ColorStateList;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/** Shared chrome; only the page content changes during navigation. */
final class AppShell {
    final MainActivity activity;
    final LinearLayout header,body;
    final ScrollView scroll;
    final BottomNavigationView navigation;
    final Bundle positions;
    String renderedKey;
    int generation;
    boolean updating,restoringScroll;

    AppShell(MainActivity a,Bundle savedPositions){
        activity=a;positions=savedPositions==null?new Bundle():savedPositions;
        a.getWindow().setStatusBarColor(Ui.BG);a.getWindow().setNavigationBarColor(Ui.CARD);
        LinearLayout outer=Ui.column(a);outer.setBackgroundColor(Ui.BG);
        outer.setOnApplyWindowInsetsListener((v,in)->{
            if(Build.VERSION.SDK_INT>=30){var bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsets.CONSUMED;}
            v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in.consumeSystemWindowInsets();
        });
        header=Ui.row(a);header.setMinimumHeight(Ui.dp(a,64));header.setPadding(Ui.dp(a,12),Ui.dp(a,4),Ui.dp(a,12),Ui.dp(a,4));outer.addView(header,new LinearLayout.LayoutParams(-1,-2));
        scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        body=Ui.column(a);body.setPadding(Ui.dp(a,24),Ui.dp(a,16),Ui.dp(a,24),Ui.dp(a,26));scroll.addView(body);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        navigation=new BottomNavigationView(a);navigation.setElevation(0);navigation.setBackgroundColor(Ui.CARD);
        ColorStateList tint=new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.LIME,Ui.MUTED});
        navigation.setItemIconTintList(tint);navigation.setItemTextColor(tint);navigation.setItemActiveIndicatorColor(ColorStateList.valueOf(0x24DCFA84));navigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        navigation.getMenu().add(0,MainActivity.TODAY,0,R.string.page_today).setIcon(R.drawable.ic_today);
        navigation.getMenu().add(0,MainActivity.ROUTINES,1,R.string.page_routines).setIcon(R.drawable.ic_routines);
        navigation.getMenu().add(0,MainActivity.HABITS,2,R.string.page_habits).setIcon(R.drawable.ic_habits);
        navigation.setOnItemSelectedListener(item->{if(!updating)a.navigate(item.getItemId());return true;});
        navigation.setOnItemReselectedListener(item->{if(!updating)a.navigate(item.getItemId());});
        outer.addView(navigation,new LinearLayout.LayoutParams(-1,-2));a.setContentView(outer);outer.requestApplyInsets();
    }
    ImageButton icon(int drawable,String label,Runnable action){
        ImageButton b=new ImageButton(activity);b.setImageResource(drawable);b.setImageTintList(ColorStateList.valueOf(Ui.LIME));b.setContentDescription(label);
        b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x229FA695),Ui.shape(android.graphics.Color.TRANSPARENT,Ui.dp(activity,16)),null));
        b.setOnClickListener(v->action.run());b.setPadding(Ui.dp(activity,12),Ui.dp(activity,12),Ui.dp(activity,12),Ui.dp(activity,12));b.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity,48),Ui.dp(activity,48)));return b;
    }
    void remember(){if(renderedKey!=null&&!restoringScroll)positions.putInt(renderedKey,scroll.getScrollY());}
    LinearLayout show(String key,String title,boolean back,int selectedTab){
        remember();renderedKey=key;int current=++generation;restoringScroll=true;
        body.removeAllViews();header.removeAllViews();
        if(back)header.addView(icon(R.drawable.ic_back,activity.getString(R.string.go_back),activity::goBack));
        else {TextView bolt=Ui.bold(activity,"ϟ",30,Ui.LIME);bolt.setGravity(Gravity.CENTER);header.addView(bolt,new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,48)));}
        TextView heading=Ui.bold(activity,title,20,Ui.TEXT);androidx.core.view.ViewCompat.setAccessibilityHeading(heading,true);Ui.weighted(header,heading);
        if(activity.destination!=MainActivity.SETTINGS)header.addView(icon(R.drawable.ic_settings,activity.getString(R.string.page_settings),activity::openSettings));
        updating=true;if(navigation.getSelectedItemId()!=selectedTab)navigation.setSelectedItemId(selectedTab);updating=false;
        scroll.post(()->{if(generation==current){scroll.scrollTo(0,positions.getInt(key,0));restoringScroll=false;}});return body;
    }
}
