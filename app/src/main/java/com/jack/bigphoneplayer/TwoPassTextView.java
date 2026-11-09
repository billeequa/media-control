package com.jack.bigphoneplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

/** One fixed-height metadata line; equal metadata never restarts scrolling. */
final class TwoPassTextView extends TextView {
    private boolean ready,pending=true,scrolling;
    private ValueAnimator animation;
    private StaticLayout fullText,restingText;
    private float period;
    private boolean rtl;
    private float offset;
    private final Runnable begin=this::beginIfNeeded;
    TwoPassTextView(Context context) {
        super(context); setSingleLine(true); setEllipsize(TextUtils.TruncateAt.END);
        setGravity(Gravity.START|Gravity.CENTER_VERTICAL); setLineSpacing(0,1); ready=true;
    }
    @Override public void setText(CharSequence text,BufferType type) {
        if (!ready) { super.setText(text,type); return; }
        if (TextUtils.equals(getText(),text)) return;
        stop(); super.setText(text,type); pending=true; post(begin);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        if (ready && w!=oldw) { stop(); pending=true; post(begin); }
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); post(begin); }
    @Override protected void onDetachedFromWindow() { removeCallbacks(begin); stop(); pending=true; super.onDetachedFromWindow(); }
    @Override protected void onVisibilityChanged(View changed,int visibility) {
        super.onVisibilityChanged(changed,visibility);
        if (!ready) return;
        if (!isShown()) { stop(); pending=true; } else post(begin);
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (!ready) return;
        if (visibility!=VISIBLE) { stop(); pending=true; } else post(begin);
    }
    private void beginIfNeeded() {
        if (!pending || !isAttachedToWindow() || !isShown() || getWindowVisibility()!=VISIBLE) return;
        int available=getWidth()-getPaddingLeft()-getPaddingRight();
        if (available<=0) return;
        pending=false;
        CharSequence value=getTransformationMethod()==null ? getText() : getTransformationMethod().getTransformation(getText(),this);
        int width=(int)Math.ceil(Layout.getDesiredWidth(value,getPaint()));
        if (width<=available || !ValueAnimator.areAnimatorsEnabled()) { invalidate(); return; }
        fullText=StaticLayout.Builder.obtain(value,0,value.length(),getPaint(),width+1)
            .setIncludePad(getIncludeFontPadding()).setMaxLines(1).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        rtl=fullText.getParagraphDirection(0)<0;
        period=fullText.getWidth()+48*getResources().getDisplayMetrics().density;
        restingText=StaticLayout.Builder.obtain(value,0,value.length(),getPaint(),available)
            .setIncludePad(getIncludeFontPadding()).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END)
            .setEllipsizedWidth(available).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        MarqueeCycle cycle=new MarqueeCycle(period,getResources().getDisplayMetrics().density);
        ValueAnimator run=ValueAnimator.ofFloat(0,1); animation=run; scrolling=true;
        offset=0;
        run.setDuration(cycle.totalMs()); run.setInterpolator(new LinearInterpolator());
        run.addUpdateListener(frame -> {
            float fraction=cycle.fraction(Math.round(frame.getAnimatedFraction()*cycle.totalMs()));
            offset=period*fraction; invalidate();
        });
        run.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animator) {
                if (animation!=animator) return;
                animation=null; scrolling=false; fullText=null; restingText=null; offset=0; invalidate();
            }
        });
        run.start();
    }
    private void stop() {
        removeCallbacks(begin);
        ValueAnimator old=animation; animation=null;
        if (old!=null) old.cancel();
        scrolling=false; fullText=null; restingText=null; offset=0; invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        if (!scrolling || fullText==null) { super.onDraw(canvas); return; }
        getPaint().setColor(getCurrentTextColor());
        // Match the native TextView baseline so the final handoff cannot jump vertically.
        float y=getBaseline()-fullText.getLineBaseline(0);
        int available=getWidth()-getPaddingLeft()-getPaddingRight();
        canvas.save(); canvas.clipRect(getPaddingLeft(),getPaddingTop(),getWidth()-getPaddingRight(),getHeight()-getPaddingBottom());
        for (int copy=0;copy<=2;copy++) {
            StaticLayout layout=copy==2 ? restingText : fullText;
            float x=rtl ? available-layout.getWidth()+offset-copy*period : copy*period-offset;
            if (x+layout.getWidth()<0 || x>available) continue;
            canvas.save(); canvas.translate(getPaddingLeft()+x,y); layout.draw(canvas); canvas.restore();
        }
        canvas.restore();
    }
}
