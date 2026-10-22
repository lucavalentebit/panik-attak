package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int BG=Color.rgb(17,19,16),CARD=Color.rgb(29,32,26),TEXT=Color.rgb(246,247,239),MUTED=Color.rgb(159,166,149),LIME=Color.rgb(220,250,132),INK=Color.rgb(27,36,16),LILAC=Color.rgb(203,190,255),ORANGE=Color.rgb(255,185,119),RED=Color.rgb(255,130,116);
    static final int[] COLORS={LIME,LILAC,ORANGE,RED};
    static int dp(Context c,float x){return (int)(x*c.getResources().getDisplayMetrics().density+0.5f);}
    static GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static TextView text(Context c,String s,float size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");t.setIncludeFontPadding(false);return t;}
    static TextView bold(Context c,String s,float size,int color){TextView t=text(c,s,size,color);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    static void space(LinearLayout l,int h){View v=new View(l.getContext());l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),h)));}
    static LinearLayout card(Context c,int color){LinearLayout l=column(c);l.setPadding(dp(c,22),dp(c,22),dp(c,22),dp(c,22));l.setBackground(shape(color,dp(c,28)));return l;}
    static TextView button(Context c,String s,int color,int ink,Runnable action){TextView t=bold(c,s,15,ink);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,56));t.setPadding(dp(c,16),dp(c,14),dp(c,16),dp(c,14));t.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22FFFFFF),shape(color,dp(c,20)),null));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->action.run());return t;}
    static LinearLayout shell(Activity a,boolean scroll){
        a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);
        LinearLayout outer=column(a);outer.setBackgroundColor(BG);outer.setOnApplyWindowInsetsListener((v,in)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}else v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});
        LinearLayout body=column(a);body.setPadding(dp(a,24),dp(a,22),dp(a,24),dp(a,26));
        if(scroll){ScrollView sv=new ScrollView(a);sv.setFillViewport(true);sv.setClipToPadding(false);sv.setVerticalScrollBarEnabled(false);sv.addView(body);outer.addView(sv,new LinearLayout.LayoutParams(-1,-1));}else outer.addView(body,new LinearLayout.LayoutParams(-1,-1));a.setContentView(outer);outer.requestApplyInsets();return body;
    }
    static void weighted(LinearLayout row,View child){row.addView(child,new LinearLayout.LayoutParams(0,-2,1));}
    static AlertDialog.Builder dialog(Context c){return new AlertDialog.Builder(c){
        @Override public AlertDialog create(){AlertDialog d=super.create();d.setOnShowListener(v->styleDialog(d));return d;}
    };}
    static android.content.res.ColorStateList controlTint(){return new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0x669FA695,LIME,MUTED});}
    static void dialogButton(TextView button,boolean primary){
        if(button==null)return;Context c=button.getContext();button.setTextColor(primary?INK:MUTED);
        button.setMinHeight(dp(c,48));button.setMinimumHeight(dp(c,48));button.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));
        button.setElevation(0);button.setStateListAnimator(null);
        // Framework AlertDialog may inflate a MaterialButton with its background already
        // replaced by the platform. Own the drawable for both kinds of dialog button.
        GradientDrawable bg=shape(primary?LIME:Color.TRANSPARENT,dp(c,12));if(!primary)bg.setStroke(dp(c,1),0x669FA695);
        button.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x229FA695),new android.graphics.drawable.InsetDrawable(bg,0,dp(c,4),0,dp(c,4)),null));
        button.setBackgroundTintList(null);
        if(button.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){var lp=(ViewGroup.MarginLayoutParams)button.getLayoutParams();lp.setMarginStart(dp(c,4));lp.setMarginEnd(dp(c,4));button.setLayoutParams(lp);}
    }
    static void styleDialog(AlertDialog d){
        dialogButton(d.getButton(-1),true);dialogButton(d.getButton(-2),false);dialogButton(d.getButton(-3),false);
        ListView list=d.getListView();if(list==null)return;
        if(list.getChoiceMode()==ListView.CHOICE_MODE_NONE){list.setDivider(new android.graphics.drawable.ColorDrawable(0x229FA695));list.setDividerHeight(dp(list.getContext(),1));}
        list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){public void onChildViewAdded(View parent,View child){styleOption(child);}public void onChildViewRemoved(View parent,View child){}});
        for(int i=0;i<list.getChildCount();i++)styleOption(list.getChildAt(i));
    }
    static void styleOption(View child){
        int height=dp(child.getContext(),52);child.setMinimumHeight(height);
        if(child.getLayoutParams()!=null&&child.getLayoutParams().height>0){var lp=child.getLayoutParams();lp.height=Math.max(lp.height,height);child.setLayoutParams(lp);}
        if(child instanceof TextView)((TextView)child).setTextColor(TEXT);
        if(child instanceof CheckedTextView)((CheckedTextView)child).setCheckMarkTintList(controlTint());
    }
}
