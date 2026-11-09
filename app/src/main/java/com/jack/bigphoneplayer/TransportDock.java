package com.jack.bigphoneplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/** Transparent player overlay: fixed controls, tall vertical targets, tabs outside. */
final class TransportDock extends FrameLayout {
    private final LinearLayout row;
    private final int buttonHeight,bottomSpace;
    private final List<Rect> hitAreas=new ArrayList<>();
    private final List<TouchDelegate> delegates=new ArrayList<>();
    private final Paint hintPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private TouchDelegate active;
    private ValueAnimator hintAnimation;
    private float hintAlpha;
    TransportDock(Context context,AppPalette palette,int buttonHeight) {
        super(context); this.buttonHeight=buttonHeight;
        bottomSpace=Math.round(buttonHeight*.4f)+dp(18);
        row=new LinearLayout(context); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(20),0,dp(20),0);
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-1,buttonHeight,Gravity.BOTTOM);
        params.bottomMargin=bottomSpace; addView(row,params);
        hintPaint.setColor(palette.accent); hintPaint.setStyle(Paint.Style.STROKE); hintPaint.setStrokeWidth(dp(1));
    }
    void addControl(View control,LinearLayout.LayoutParams params) { row.addView(control,params); }
    int reservedHeight() { return buttonHeight+bottomSpace+dp(10); }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom) {
        super.onLayout(changed,left,top,right,bottom);
        hitAreas.clear(); delegates.clear(); active=null;
        for (int i=0;i<row.getChildCount();i++) {
            View button=row.getChildAt(i);
            Rect bounds=new Rect(); button.getDrawingRect(bounds); offsetDescendantRectToMyCoords(button,bounds);
            // One button height in either vertical direction; never change side gaps.
            // The overlay ends at the player page boundary, before both navigation tabs.
            bounds.top=Math.max(0,bounds.top-button.getHeight());
            bounds.bottom=Math.min(getHeight(),bounds.bottom+button.getHeight());
            hitAreas.add(bounds); delegates.add(new TouchDelegate(bounds,button));
        }
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN) {
            active=null;
            for (int i=0;i<hitAreas.size();i++) {
                if (row.getChildAt(i).isEnabled() && hitAreas.get(i).contains((int)event.getX(),(int)event.getY())) {
                    active=delegates.get(i); break;
                }
            }
        }
        // Outside the targets, allow the underlying artwork/metadata to scroll normally.
        if (active==null) return false;
        MotionEvent copy=MotionEvent.obtain(event);
        boolean consumed=active.onTouchEvent(copy); copy.recycle();
        if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) active=null;
        return consumed;
    }
    void showTouchHints() {
        if (hintAnimation!=null) hintAnimation.cancel();
        if (!ValueAnimator.areAnimatorsEnabled()) { hintAlpha=0; invalidate(); return; }
        hintAlpha=1; invalidate();
        hintAnimation=ValueAnimator.ofFloat(1,0); hintAnimation.setStartDelay(300); hintAnimation.setDuration(1000);
        hintAnimation.addUpdateListener(frame -> { hintAlpha=(float)frame.getAnimatedValue(); invalidate(); });
        hintAnimation.start();
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (hintAlpha<=0) return;
        hintPaint.setAlpha(Math.round(90*hintAlpha));
        for (Rect area:hitAreas) canvas.drawRoundRect(area.left+1,area.top+1,area.right-1,area.bottom-1,dp(36),dp(36),hintPaint);
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility!=VISIBLE && hintAnimation!=null) { hintAnimation.cancel(); hintAlpha=0; }
    }
    @Override protected void onDetachedFromWindow() {
        if (hintAnimation!=null) hintAnimation.cancel();
        super.onDetachedFromWindow();
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
