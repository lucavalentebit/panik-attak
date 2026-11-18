package it.panikattak.app;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import java.util.ArrayList;

// The carousel is constructed with a live routine and never inflated from XML.
@SuppressLint("ViewConstructor")
final class PhaseCarousel extends FrameLayout {
    static final String ADD_TAG = "addInterval";
    private static final long SNAP_MS = 200;
    private static final PathInterpolator EASE_OUT = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final MainActivity activity;
    private final TextView previous, next, counter;
    private final int touchSlop, minimumFling;
    private ValueAnimator snap;
    private VelocityTracker velocity;
    private float downX, downY;
    private int width, dragStart, anchor, pointerId;
    private boolean dragging;
    private int revealFrom;

    static void add(MainActivity activity, LinearLayout page) {
        LinearLayout controls = Ui.row(activity);
        controls.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        TextView counter = Ui.bold(activity, "", 13, Ui.MUTED);
        counter.setGravity(Gravity.CENTER);
        counter.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);

        PhaseCarousel carousel = new PhaseCarousel(activity, counter);
        controls.addView(carousel.previous);
        Ui.weighted(controls, counter);
        controls.addView(carousel.next);
        page.addView(carousel, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        Ui.space(page, 8);
        page.addView(controls);
    }

    private PhaseCarousel(MainActivity activity, TextView counter) {
        super(activity);
        this.activity = activity;
        this.counter = counter;
        revealFrom = activity.revealFromPhase;
        activity.revealFromPhase = -1;
        ViewConfiguration configuration = ViewConfiguration.get(activity);
        touchSlop = configuration.getScaledTouchSlop();
        minimumFling = Math.max(configuration.getScaledMinimumFlingVelocity(), Ui.dp(activity, 450));
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        String[] puzzles = activity.getResources().getStringArray(R.array.puzzle_types);
        for (int phase = 0; phase < activity.plan.phaseCount(); phase++) {
            addView(card(phase,puzzles), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        previous = arrow("‹", R.string.phase_previous, -1);
        next = arrow("›", R.string.phase_next, 1);
        updateControls();
    }

    private TextView arrow(String glyph, int label, int delta) {
        TextView button = Ui.button(activity, glyph, Ui.CARD, Ui.LIME,
                () -> goTo(activity.routinePhase + delta));
        button.setContentDescription(activity.getString(label));
        button.setTextSize(22);
        button.setPadding(0, 0, 0, 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(activity, 56), Ui.dp(activity, 48)));
        return button;
    }

    private View card(int phase, String[] puzzles) {
        Plan plan = activity.plan;
        int offset = plan.offset(phase);
        LinearLayout card = Ui.card(activity, Ui.CARD);
        TextView title = Ui.bold(activity, plan.phaseName(phase), 22, Ui.COLORS[plan.phaseLevel(phase)]);
        ViewCompat.setAccessibilityHeading(title, true);
        card.addView(title);
        Ui.space(card, 10);
        card.addView(Ui.text(activity, activity.getString(R.string.phase_window,
                activity.timeAt(offset), activity.timeAt(offset + plan.durations[phase]),
                plan.durations[phase]), 14, Ui.MUTED));
        Ui.space(card, 10);
        card.addView(Ui.bold(activity, activity.cadence(phase), 15, Ui.TEXT));
        Ui.space(card, 6);
        String puzzle = puzzles[plan.puzzleFor(phase)];
        card.addView(Ui.text(activity, activity.getString(R.string.state_puzzle_value, puzzle), 13, Ui.MUTED));
        Ui.space(card, 16);
        TextView edit = Ui.button(activity, activity.getString(R.string.phase_edit),
                0x12FFFFFF, Ui.TEXT, () -> {
                    goTo(phase);
                    activity.editPhase(phase);
                });
        edit.setTag(editTag(phase));
        card.addView(edit);
        return card;
    }

    static String editTag(int phase) {
        return "editPhase:" + phase;
    }

    private void updateControls() {
        int phase = activity.routinePhase;
        int count = getChildCount();
        counter.setText(activity.getString(R.string.phase_position, phase + 1, count));
        previous.setEnabled(phase > 0);
        next.setEnabled(phase + 1 < count);
        previous.setAlpha(previous.isEnabled() ? 1f : .45f);
        next.setAlpha(next.isEnabled() ? 1f : .45f);
        for (int i = 0; i < count; i++) {
            getChildAt(i).setImportantForAccessibility(i == phase
                    ? IMPORTANT_FOR_ACCESSIBILITY_AUTO : IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int newWidth = right - left;
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).offsetLeftAndRight(i * newWidth);
        if (width != newWidth) {
            width = newWidth;
            stopSnap();
            int selected = activity.routinePhase;
            boolean reveal = revealFrom >= 0 && revealFrom < getChildCount() && ValueAnimator.areAnimatorsEnabled();
            boolean adjacent = reveal && Math.abs(revealFrom - selected) == 1;
            scrollTo((adjacent ? revealFrom : selected) * width, 0);
            if (adjacent) post(() -> { if (isAttachedToWindow() && !dragging) goTo(activity.routinePhase); });
            else if (reveal) {
                View card = getChildAt(selected);
                card.setAlpha(0f);
                card.animate().alpha(1f).setDuration(SNAP_MS).setInterpolator(EASE_OUT).start();
            }
            revealFrom = -1;
        }
    }

    // Only the selected card can contribute keyboard focus or accessibility actions.
    @Override public void addFocusables(ArrayList<View> views, int direction, int mode) {
        View selected = getChildAt(activity.routinePhase);
        if (selected != null) selected.addFocusables(views, direction, mode);
    }

    private int boundedPhase(int phase) {
        return Math.max(0, Math.min(getChildCount() - 1, phase));
    }

    private void goTo(int phase) {
        stopSnap();
        int target = boundedPhase(phase);
        if (activity.routinePhase != target) {
            activity.routinePhase = target;
            updateControls();
        }
        int destination = target * width;
        if (getScrollX() == destination || !ValueAnimator.areAnimatorsEnabled()) {
            scrollTo(destination, 0);
            return;
        }
        snap = ValueAnimator.ofInt(getScrollX(), destination);
        snap.setDuration(SNAP_MS);
        snap.setInterpolator(EASE_OUT);
        snap.addUpdateListener(animation -> scrollTo((int) animation.getAnimatedValue(), 0));
        snap.start();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            releaseVelocity();
            velocity = VelocityTracker.obtain();
            downX = event.getX();
            downY = event.getY();
            pointerId = event.getPointerId(0);
        }
        if (velocity != null) velocity.addMovement(event);
        boolean handled = super.dispatchTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) releaseVelocity();
        return handled;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        return event.getActionMasked() == MotionEvent.ACTION_MOVE && beginDrag(event);
    }

    private boolean beginDrag(MotionEvent event) {
        int pointer = event.findPointerIndex(pointerId);
        if (pointer < 0 || width == 0) return false;
        float dx = Math.abs(event.getX(pointer) - downX);
        float dy = Math.abs(event.getY(pointer) - downY);
        if (dx <= touchSlop || dx <= dy) return false;
        dragging = true;
        stopSnap();
        dragStart = getScrollX();
        anchor = boundedPhase(Math.round((float) dragStart / width));
        getParent().requestDisallowInterceptTouchEvent(true);
        updateDrag(event.getX(pointer));
        return true;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int pointer = event.findPointerIndex(pointerId);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging && pointer >= 0) updateDrag(event.getX(pointer));
                else if (!dragging) beginDrag(event);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == pointerId) finishDrag(activity.routinePhase);
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging && pointer >= 0) finishDrag(swipeTarget(event.getX(pointer)));
                else performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishDrag(activity.routinePhase);
                return true;
            default:
                return true;
        }
    }

    @Override public boolean performClick() {
        return super.performClick();
    }

    private void updateDrag(float x) {
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        int lower = boundedPhase(anchor - 1) * width;
        int upper = boundedPhase(anchor + 1) * width;
        scrollTo(Math.max(lower, Math.min(upper, Math.round(dragStart + downX - x))), 0);
    }

    private int swipeTarget(float x) {
        float distance = downX - x;
        velocity.computeCurrentVelocity(1000);
        float speed = -velocity.getXVelocity(pointerId);
        boolean flick = Math.abs(speed) >= minimumFling && distance * speed > 0;
        return Math.abs(distance) >= width / 4f || flick
                ? anchor + (distance > 0 ? 1 : -1)
                : activity.routinePhase;
    }

    private void finishDrag(int target) {
        if (!dragging) return;
        dragging = false;
        getParent().requestDisallowInterceptTouchEvent(false);
        goTo(target);
    }

    private void stopSnap() {
        if (snap != null) {
            snap.cancel();
            snap = null;
        }
    }

    private void releaseVelocity() {
        if (velocity != null) {
            velocity.recycle();
            velocity = null;
        }
    }

    @Override protected void onDetachedFromWindow() {
        stopSnap();
        scrollTo(activity.routinePhase * width, 0);
        releaseVelocity();
        for (int i = 0; i < getChildCount(); i++) {
            View card=getChildAt(i);
            card.animate().cancel();
            card.setAlpha(1f);
        }
        dragging = false;
        super.onDetachedFromWindow();
    }
}
