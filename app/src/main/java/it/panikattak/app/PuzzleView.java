package it.panikattak.app;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** One renderer shared by alarm, protected actions and the silent preview. */
final class PuzzleView extends LinearLayout {
    interface Solved { void accept(String answer); }
    final Puzzle puzzle; final Solved solved; final Handler handler=new Handler(Looper.getMainLooper());
    final ArrayList<Integer> entered=new ArrayList<>();
    final int[] order; String answer=""; boolean showing,complete; int selected=-1;
    TextView feedback,input; LinearLayout board; final ArrayList<TextView> cells=new ArrayList<>();
    static final int[] COLORS={0xFFF24C5A,0xFF3989FF,0xFFFFDF36,0xFF23BD79,0xFFAE63E8,0xFFFF932F};
    static final String[] FACES={"😄","🙂","😐","🙁","😢"};
    PuzzleView(Context c,Puzzle puzzle,Solved solved){super(c);this.puzzle=puzzle;this.solved=solved;order=puzzle.data.clone();setOrientation(VERTICAL);render();}
    String s(int id){return getContext().getString(id);}
    TextView label(String value){TextView t=Ui.bold(getContext(),value,21,Ui.TEXT);t.setGravity(Gravity.CENTER);addView(t);return t;}
    TextView button(String value,Runnable action){return Ui.button(getContext(),value,Ui.CARD,Ui.TEXT,action);}
    void render(){
        removeAllViews();cells.clear();
        if(puzzle.type==Puzzle.MATH){label(puzzle.question());Ui.space(this,12);input=label(s(R.string.answer));}
        else label(s(puzzle.type==Puzzle.ODD?R.string.odd_hint:puzzle.type==Puzzle.ORDER?(puzzle.level==0?R.string.order_numbers:R.string.order_faces):R.string.watch));
        Ui.space(this,12);feedback=Ui.text(getContext(),s(R.string.solve_hint),13,Ui.MUTED);feedback.setGravity(Gravity.CENTER);addView(feedback);Ui.space(this,12);
        board=Ui.column(getContext());addView(board);
        if(puzzle.type==Puzzle.MATH){
            for(String[] row:new String[][]{{"1","2","3"},{"4","5","6"},{"7","8","9"},{"⌫","0","→"}}){LinearLayout line=Ui.row(getContext());board.addView(line);for(String key:row){TextView b=button(key,()->key(key));b.setTextSize(24);if(key.equals("⌫"))b.setContentDescription(s(R.string.delete_digit));if(key.equals("→"))b.setContentDescription(s(R.string.confirm));addCell(line,b,56);}}
        }else if(puzzle.type==Puzzle.ORDER){renderOrder();Ui.space(this,12);addView(Ui.text(getContext(),s(R.string.order_accessibility),12,Ui.MUTED));Ui.space(this,12);addView(button(s(R.string.confirm),()->submit(Puzzle.join(order))));}
        else if(puzzle.type==Puzzle.ODD){
            int size=puzzle.level==0?4:5;
            for(int row=0;row<size;row++){LinearLayout line=Ui.row(getContext());board.addView(line);for(int col=0;col<size;col++){
                final int n=row*size+col;boolean odd=n==puzzle.data[0];
                String[] shapes={"●","◆","■","▲"};String glyph=puzzle.level==0?shapes[(puzzle.data[1]+(odd?1:0))%4]:"➤";
                TextView b=new Symbol(getContext(),glyph,puzzle.level>0?puzzle.data[1]*90+(odd?25:0):0);b.setOnClickListener(v->submit(Integer.toString(n)));b.setContentDescription(getContext().getString(R.string.symbol,n+1));addCell(line,b,52);
            }}
        }else{
            if(puzzle.type==Puzzle.COLORS){LinearLayout strip=Ui.row(getContext());board.addView(strip);for(int v:puzzle.data){TextView t=Ui.bold(getContext(),"●",30,COLORS[v]);t.setGravity(Gravity.CENTER);strip.addView(t,new LinearLayout.LayoutParams(0,Ui.dp(getContext(),52),1));cells.add(t);}Ui.space(board,10);}
            int count=puzzle.type==Puzzle.COLORS?6:9;
            for(int row=0;row<count/3;row++){LinearLayout line=Ui.row(getContext());board.addView(line);for(int col=0;col<3;col++){final int n=row*3+col;TextView b=button(puzzle.type==Puzzle.COLORS?"●":"",()->tap(n));b.setTextSize(38);b.setTextColor(COLORS[n%6]);b.setContentDescription(puzzle.type==Puzzle.COLORS?getResources().getStringArray(R.array.color_names)[n]:getContext().getString(R.string.tile,n+1));addCell(line,b,58);if(puzzle.type==Puzzle.TILES)cells.add(b);}}
            Ui.space(this,10);addView(button(s(R.string.replay),this::play));play();
        }
    }
    static final class Symbol extends androidx.appcompat.widget.AppCompatTextView {
        final float angle;
        Symbol(Context context,String glyph,float angle){super(context);this.angle=angle;setText(glyph);setTextSize(28);setTextColor(Ui.LILAC);setGravity(Gravity.CENTER);setBackground(Ui.shape(Ui.CARD,Ui.dp(context,18)));setClickable(true);setFocusable(true);}
        @Override protected void onDraw(android.graphics.Canvas canvas){canvas.save();canvas.rotate(angle,getWidth()/2f,getHeight()/2f);super.onDraw(canvas);canvas.restore();}
    }
    void addCell(LinearLayout row,TextView t,int height){
        t.setPadding(0,0,0,0);t.setMinWidth(Ui.dp(getContext(),48));t.setMinHeight(Ui.dp(getContext(),48));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(getContext(),height),1);lp.setMargins(Ui.dp(getContext(),1),Ui.dp(getContext(),3),Ui.dp(getContext(),1),Ui.dp(getContext(),3));row.addView(t,lp);
    }
    void key(String key){
        if(complete)return;
        if(key.equals("⌫")){if(!answer.isEmpty())answer=answer.substring(0,answer.length()-1);}
        else if(key.equals("→")){submit(answer);if(!complete)answer="";}
        else if(answer.length()<3)answer+=key;
        input.setText(answer.isEmpty()?s(R.string.answer):answer);
    }
    void play(){
        if(complete)return;handler.removeCallbacksAndMessages(null);entered.clear();showing=true;feedback.setText(s(R.string.watch));
        if(puzzle.type==Puzzle.COLORS){for(TextView c:cells)c.setVisibility(VISIBLE);handler.postDelayed(()->{for(TextView c:cells)c.setVisibility(INVISIBLE);ready();},puzzle.level==0?2000:puzzle.level==1?1750:1500);}
        else {for(TextView c:cells)c.setBackground(Ui.shape(Ui.CARD,Ui.dp(getContext(),18)));
            int step=puzzle.level==0?700:500;
            for(int i=0;i<puzzle.data.length;i++){final TextView c=cells.get(puzzle.data[i]);handler.postDelayed(()->c.setBackground(Ui.shape(Ui.LIME,Ui.dp(getContext(),18))),i*step+250);handler.postDelayed(()->c.setBackground(Ui.shape(Ui.CARD,Ui.dp(getContext(),18))),i*step+250+step-150);}
            handler.postDelayed(this::ready,puzzle.data.length*step+250);
        }
    }
    void ready(){showing=false;feedback.setText(s(R.string.repeat_sequence));}
    void tap(int n){
        if(showing||complete)return;
        if(n!=puzzle.data[entered.size()]){wrong();return;}
        entered.add(n);StringBuilder progress=new StringBuilder();for(int i=0;i<puzzle.data.length;i++)progress.append(i<entered.size()?"● ":"○ ");feedback.setText(progress.toString());
        if(entered.size()==puzzle.data.length)submit(Puzzle.join(entered.stream().mapToInt(Integer::intValue).toArray()));
    }
    final ArrayList<TextView> orderCells=new ArrayList<>();
    boolean settling;int dragFrom=-1;
    void renderOrder(){
        for(TextView cell:orderCells)cell.animate().cancel();orderCells.clear();
        board.removeAllViews();LinearLayout row=new LinearLayout(getContext()){ {setChildrenDrawingOrderEnabled(true);} @Override protected int getChildDrawingOrder(int count,int position){int raised=dragFrom>=0?dragFrom:selected;if(raised<0)return position;if(position==count-1)return raised;return position>=raised?position+1:position;} };row.setGravity(Gravity.CENTER_VERTICAL);row.setClipChildren(false);board.setClipChildren(false);board.addView(row);
        for(int i=0;i<order.length;i++){final int pos=i;String value=puzzle.level==0?Integer.toString(order[i]):FACES[order[i]];
            TextView t=button(value,()->{if(complete||settling)return;if(selected<0){selected=pos;highlightOrder();}else swap(selected,pos);});
            t.setTextSize(puzzle.level==0?25:27);t.setContentDescription(getContext().getString(R.string.tile_value,value));
            t.setOnTouchListener(new OnTouchListener(){
                float downX,downY;boolean dragging,abandoned;
                @Override public boolean onTouch(View v,MotionEvent e){
                    if(complete||settling)return true;
                    switch(e.getActionMasked()){
                        case MotionEvent.ACTION_DOWN:
                            downX=e.getRawX();downY=e.getRawY();dragging=false;abandoned=false;v.setPressed(true);v.getParent().requestDisallowInterceptTouchEvent(true);return true;
                        case MotionEvent.ACTION_MOVE:
                            if(abandoned)return true;
                            float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
                            int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                            if(!dragging&&Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){abandoned=true;v.setPressed(false);v.getParent().requestDisallowInterceptTouchEvent(false);return true;}
                            if(!dragging&&Math.abs(dx)>slop){dragging=true;dragFrom=pos;selected=-1;highlightOrder();v.setPressed(false);v.setBackground(Ui.shape(Ui.LILAC,Ui.dp(getContext(),20)));row.invalidate();}
                            if(dragging)v.setTranslationX(Math.max(-v.getLeft(),Math.min(dx,row.getWidth()-v.getRight())));
                            return true;
                        case MotionEvent.ACTION_UP:
                            v.setPressed(false);v.getParent().requestDisallowInterceptTouchEvent(false);
                            if(dragging){int target=pos;float center=v.getLeft()+v.getWidth()/2f+v.getTranslationX(),distance=Float.MAX_VALUE;
                                for(int j=0;j<orderCells.size();j++){TextView candidate=orderCells.get(j);float d=Math.abs(center-(candidate.getLeft()+candidate.getWidth()/2f));if(d<distance){distance=d;target=j;}}
                                dragFrom=-1;swap(pos,target);
                            }else if(!abandoned)v.performClick();return true;
                        case MotionEvent.ACTION_CANCEL:
                            v.setPressed(false);v.getParent().requestDisallowInterceptTouchEvent(false);dragFrom=-1;v.animate().translationX(0).setDuration(180).withEndAction(()->{highlightOrder();}).start();return true;
                        default:return true;
                    }
                }
            });addCell(row,t,64);orderCells.add(t);
        }
        highlightOrder();
    }
    void highlightOrder(){for(int i=0;i<orderCells.size();i++)orderCells.get(i).setBackground(Ui.shape(selected==i?Ui.LILAC:Ui.CARD,Ui.dp(getContext(),20)));}
    void swap(int a,int b){
        if(complete||settling)return;selected=-1;settling=true;
        TextView first=orderCells.get(a),second=orderCells.get(b);float offset=second.getLeft()-first.getLeft();
        int value=order[a];order[a]=order[b];order[b]=value;
        if(a!=b)second.animate().translationX(-offset).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        first.animate().translationX(offset).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(()->{settling=false;renderOrder();}).start();
    }
    void submit(String value){if(complete||settling||dragFrom>=0)return;if(puzzle.accepts(value)){complete=true;handler.removeCallbacksAndMessages(null);feedback.setText(s(R.string.correct));feedback.setTextColor(Ui.LIME);solved.accept(value);}else wrong();}
    void wrong(){
        feedback.setText(s(R.string.wrong));feedback.setTextColor(Ui.RED);android.graphics.drawable.GradientDrawable border=Ui.shape(Ui.BG,Ui.dp(getContext(),18));border.setStroke(Ui.dp(getContext(),2),Ui.RED);setBackground(border);
        android.os.Vibrator vibrator=getContext().getSystemService(android.os.Vibrator.class);if(vibrator!=null&&Store.prefs(getContext()).getBoolean("vibration",true))vibrator.vibrate(VibrationEffect.createOneShot(45,VibrationEffect.DEFAULT_AMPLITUDE));
        if(puzzle.type==Puzzle.COLORS||puzzle.type==Puzzle.TILES)play();
        if(puzzle.type==Puzzle.ORDER){System.arraycopy(puzzle.data,0,order,0,order.length);selected=-1;renderOrder();}
        handler.postDelayed(()->{setBackgroundColor(Ui.BG);feedback.setTextColor(Ui.MUTED);},700);
    }
    void suspend(){handler.removeCallbacksAndMessages(null);if(puzzle.type==Puzzle.ORDER){for(TextView cell:orderCells)cell.animate().cancel();settling=false;dragFrom=-1;selected=-1;renderOrder();}}
    void resume(){if(!complete&&(puzzle.type==Puzzle.COLORS||puzzle.type==Puzzle.TILES))play();}
    @Override protected void onDetachedFromWindow(){suspend();super.onDetachedFromWindow();}
}
