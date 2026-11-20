package it.panikattak.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class TodayWidget extends AppWidgetProvider {
    private static final String REFRESH = "it.panikattak.REFRESH_WIDGET";

    @Override public void onReceive(Context context, Intent intent) {
        if (REFRESH.equals(intent.getAction())) refresh(context);
        else super.onReceive(context, intent);
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        refresh(context);
    }

    @Override public void onDisabled(Context context) {
        context.getSystemService(AlarmManager.class).cancel(refreshIntent(context));
    }

    static void refresh(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, TodayWidget.class));
            if (widgetIds.length == 0) return;

            long now = System.currentTimeMillis();
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
            List<SequenceStatus.Run> runs = SequenceStatus.current(context, now);
            List<TodayPage.Upcoming> upcoming = TodayPage.upcoming(context, now, zone);
            TodayPage.Upcoming next = upcoming.isEmpty() ? null : upcoming.get(0);
            LocalDate nextDay = next == null ? null : Instant.ofEpochMilli(next.event.at).atZone(zone).toLocalDate();
            long refreshAt = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
            String title;
            String detail = null;

            if (Store.prefs(context).getBoolean("ringing", false)) {
                title = context.getString(R.string.alarm_public_title);
                detail = context.getString(R.string.recall_pending);
            } else if (!runs.isEmpty()) {
                SequenceStatus.Run run = runs.get(0);
                title = runs.size() == 1 ? context.getString(R.string.sequence_running, Store.name(context, run.routine))
                        : context.getString(R.string.sequences_running, runs.size());
                if (runs.size() == 1) {
                    detail = run.nextAt > 0 ? context.getString(R.string.recall_at, Store.time(run.nextAt))
                            : context.getString(R.string.sequence_ends_at, Store.time(run.endAt));
                }
                for (SequenceStatus.Run active : runs) refreshAt = Math.min(refreshAt, active.endAt);
            } else if (RoutineSchedule.freeWeekend(today, false, nextDay)) {
                title = context.getString(R.string.good_weekend);
            } else if (next == null) {
                title = context.getString(R.string.widget_no_attack);
            } else {
                title = context.getString(R.string.widget_next);
                detail = context.getString(R.string.widget_alarm_time, TodayPage.date(nextDay, today), Store.time(next.event.at));
            }

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.today_widget);
            views.setTextViewText(R.id.widget_title, title);
            views.setTextViewText(R.id.widget_detail, detail);
            views.setViewVisibility(R.id.widget_detail, detail == null ? View.GONE : View.VISIBLE);
            Intent open = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.OPEN_TODAY, true);
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 22, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            manager.updateAppWidget(widgetIds, views);

            // Reconcile the display near midnight or sequence end without waking the device.
            context.getSystemService(AlarmManager.class).set(AlarmManager.RTC, refreshAt, refreshIntent(context));
        } catch (RuntimeException error) {
            Log.e("PanikAttak", "Unable to update the home screen widget", error);
        }
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent intent = new Intent(context, TodayWidget.class).setAction(REFRESH);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }
}
