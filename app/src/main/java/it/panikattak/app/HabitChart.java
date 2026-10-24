package it.panikattak.app;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Small, dependency-free chart using the app palette. The page provides a full text alternative. */
final class HabitChart extends View {
    interface Selection {void open(HabitStats.Session session);}
    static final class Point {final HabitStats.Session session;final double x,y;final boolean end;float px,py;Point(HabitStats.Session s,double x,double y,boolean end){session=s;this.x=x;this.y=y;this.end=end;}}
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    final List<Point> points=new ArrayList<>();final int[] bins;
    final boolean clock;final LocalDate today;final int days;final Selection selection;
    HabitChart(Context c,List<HabitStats.Session> sessions,LocalDate today,int days,boolean clock,Selection selection){
        super(c);this.today=today;this.days=days;this.clock=clock;this.selection=selection;bins=null;
        for(HabitStats.Session s:sessions)if(s.complete()){
            add(s,s.response,false);if(s.outcome.equals("stop")||s.outcome.equals("finished"))add(s,s.ending,true);
        }
        setMinimumHeight(Ui.dp(c,230));setContentDescription("Grafico delle risposte e delle conclusioni. Cerchio: prima risposta; rombo: fine. Usa Mostra i dati per la versione testuale e i dettagli.");setFocusable(true);setClickable(true);
    }
    HabitChart(Context c,int[] bins){super(c);this.bins=bins;today=LocalDate.now();days=1;clock=false;selection=null;setMinimumHeight(Ui.dp(c,150));setContentDescription("Distribuzione per fasce di trenta minuti. Conteggi disponibili nel testo sotto il grafico.");}
    void add(HabitStats.Session s,HabitStats.Event e,boolean end){
        Long delay=s.delay(e);if(delay==null)return;double value=delay/60000d;
        if(clock){OffsetDateTime local=Instant.ofEpochMilli(e.wall).atOffset(ZoneOffset.ofTotalSeconds(e.offset));value=ChronoUnit.DAYS.between(s.date(),local.toLocalDate())*1440+local.getHour()*60+local.getMinute()+local.getSecond()/60d;}
        points.add(new Point(s,ChronoUnit.DAYS.between(today.minusDays(days-1),s.date()),value,end));
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){setMeasuredDimension(MeasureSpec.getSize(widthSpec),resolveSize(getSuggestedMinimumHeight(),heightSpec));}
    float dp(float value){return Ui.dp(getContext(),value);}
    void text(Canvas c,String s,float x,float y,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setTextSize(dp(10)*getResources().getConfiguration().fontScale);c.drawText(s,x,y,paint);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bins!=null){histogram(canvas);return;}
        float left=dp((clock?58:43)*Math.min(1.6f,getResources().getConfiguration().fontScale)),right=getWidth()-dp(12),top=dp(15),bottom=getHeight()-dp(30);double min=0,max=15;
        if(!points.isEmpty()){if(clock)min=Math.floor(points.stream().mapToDouble(p->p.y).min().orElse(0)/60)*60;max=Math.max(min+30,Math.ceil(points.stream().mapToDouble(p->p.y).max().orElse(15)/15)*15);}
        for(int n=0;n<4;n++){float y=top+(bottom-top)*n/3;double value=max-(max-min)*n/3;paint.setColor(0x229FA695);paint.setStrokeWidth(dp(1));canvas.drawLine(left,y,right,y,paint);text(canvas,clock?clockLabel(value):Math.round(value)+"′",0,y+dp(4),Ui.MUTED);}
        for(Point point:points){point.px=left+(right-left)*(float)(point.x/Math.max(1,days-1));point.py=bottom-(bottom-top)*(float)((point.y-min)/(max-min));paint.setColor(point.end?Ui.LIME:Ui.MUTED);paint.setStrokeWidth(dp(2));
            if(point.end){paint.setStyle(Paint.Style.FILL);Path diamond=new Path();diamond.moveTo(point.px,point.py-dp(6));diamond.lineTo(point.px+dp(6),point.py);diamond.lineTo(point.px,point.py+dp(6));diamond.lineTo(point.px-dp(6),point.py);diamond.close();canvas.drawPath(diamond,paint);}else{paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(point.px,point.py,dp(4),paint);}paint.setStyle(Paint.Style.FILL);
        }
        DateTimeFormatter date=DateTimeFormatter.ofPattern("dd/MM");text(canvas,today.minusDays(days-1).format(date),left,getHeight()-dp(7),Ui.MUTED);String end=today.format(date);paint.setTextSize(dp(10)*getResources().getConfiguration().fontScale);text(canvas,end,right-paint.measureText(end),getHeight()-dp(7),Ui.MUTED);
    }
    static String clockLabel(double value){int minutes=(int)Math.round(value);int day=Math.floorDiv(minutes,1440);int clock=Math.floorMod(minutes,1440);return String.format(Locale.ITALIAN,"%02d:%02d",clock/60,clock%60)+(day==0?"":(day>0?" +":" ")+day);}
    void histogram(Canvas canvas){
        int maximum=1;for(int n:bins)maximum=Math.max(maximum,n);float top=dp(14),bottom=getHeight()-dp(26),width=getWidth()/48f;
        paint.setColor(0x229FA695);canvas.drawRect(0,bottom,getWidth(),bottom+dp(1),paint);
        for(int n=0;n<48;n++)if(bins[n]>0){paint.setColor(Ui.LIME);float height=(bottom-top)*bins[n]/maximum;canvas.drawRoundRect(n*width+dp(1),bottom-height,(n+1)*width-dp(1),bottom,dp(2),dp(2),paint);}
        for(int hour:new int[]{0,6,12,18})text(canvas,String.format(Locale.ITALIAN,"%02d",hour),getWidth()*hour/24f,getHeight()-dp(6),Ui.MUTED);
        paint.setTextSize(dp(10)*getResources().getConfiguration().fontScale);text(canvas,"24",getWidth()-paint.measureText("24"),getHeight()-dp(6),Ui.MUTED);text(canvas,"max "+maximum,0,dp(10),Ui.MUTED);
    }
    @Override public boolean onTouchEvent(MotionEvent event){if(selection==null)return super.onTouchEvent(event);if(event.getAction()==MotionEvent.ACTION_UP){performClick();Point nearest=null;float best=dp(32);for(Point p:points){float distance=(float)Math.hypot(event.getX()-p.px,event.getY()-p.py);if(distance<best){best=distance;nearest=p;}}if(nearest!=null)selection.open(nearest.session);return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
