package com.jack.bigphoneplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

/** Anchored controls with vertical-only touch extensions and a brief opening hint. */
final class TransportDock extends LinearLayout {
    private final List<Rect> hitAreas=new ArrayList<>();
    private final Paint hintPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator hintAnimation;
    private float hintAlpha;
    TransportDock(Context context,AppPalette palette) {
        super(context); setOrientation(HORIZONTAL); setPadding(dp(20),dp(18),dp(20),dp(18));
        hintPaint.setColor(palette.accent); hintPaint.setStyle(Paint.Style.STROKE); hintPaint.setStrokeWidth(dp(1));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom) {
        super.onLayout(changed,left,top,right,bottom);
        hitAreas.clear();
        List<TouchDelegate> delegates=new ArrayList<>();
        for (int i=0;i<getChildCount();i++) {
            Rect bounds=new Rect(); getChildAt(i).getHitRect(bounds);
            // Preserve the exact horizontal bounds, including the gaps between buttons.
            bounds.top=Math.max(0,bounds.top-dp(14)); bounds.bottom=Math.min(getHeight(),bounds.bottom+dp(14));
            hitAreas.add(bounds); delegates.add(new TouchDelegate(bounds,getChildAt(i)));
        }
        setTouchDelegate(new TouchDelegate(new Rect(),this) {
            private TouchDelegate active;
            @Override public boolean onTouchEvent(MotionEvent event) {
                int action=event.getActionMasked();
                if (action==MotionEvent.ACTION_DOWN) {
                    active=null;
                    for (int i=0;i<hitAreas.size();i++) if (hitAreas.get(i).contains((int)event.getX(),(int)event.getY())) {
                        active=delegates.get(i); break;
                    }
                }
                boolean consumed=active!=null && active.onTouchEvent(event);
                if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) active=null;
                return consumed;
            }
        });
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
