package com.jack.bigphoneplayer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;

/** Vector transport symbols with native button focus, ripple and accessibility. */
final class TransportButton extends Button {
    enum Symbol { PREVIOUS, PLAY, PAUSE, NEXT }
    private Symbol symbol;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

    TransportButton(Context context, Symbol symbol, boolean primary, AppPalette palette) {
        super(context); this.symbol = symbol;
        setText(""); setMinWidth(0); setMinimumWidth(0); setMinHeight(dp(64));
        setPadding(0,0,0,0); setAllCaps(false);
        int base=primary ? palette.accent : palette.panel;
        int[] colors={AppPalette.blend(base,palette.text,.04f),base};
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,colors);
        shape.setCornerRadius(dp(36)); shape.setStroke(dp(1),primary ? palette.accent : palette.stroke);
        setBackground(new RippleDrawable(ColorStateList.valueOf(palette.dark ? Color.argb(40,255,255,255) : Color.argb(25,0,0,0)),shape,null));
        setElevation(dp(2)); ink.setColor(primary ? palette.onAccent : palette.text);
    }

    void setSymbol(Symbol symbol) { if (this.symbol != symbol) { this.symbol=symbol; invalidate(); } }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth()*.48f,getHeight()*.48f);
        float cx=getWidth()/2f, cy=getHeight()/2f;
        canvas.save(); canvas.translate(cx,cy);
        if (symbol == Symbol.PAUSE) {
            canvas.drawRoundRect(-size*.34f,-size*.42f,-size*.10f,size*.42f,dp(2),dp(2),ink);
            canvas.drawRoundRect(size*.10f,-size*.42f,size*.34f,size*.42f,dp(2),dp(2),ink);
        } else if (symbol == Symbol.PLAY) {
            triangle(canvas,-size*.27f,-size*.44f,size*.44f,0,-size*.27f,size*.44f);
        } else {
            // Previous is exactly the horizontal reflection of Next.
            if (symbol == Symbol.PREVIOUS) canvas.scale(-1,1);
            triangle(canvas,-size*.40f,-size*.40f,size*.20f,0,-size*.40f,size*.40f);
            canvas.drawRoundRect(size*.24f,-size*.40f,size*.40f,size*.40f,dp(1),dp(1),ink);
        }
        canvas.restore();
    }

    private void triangle(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3) {
        Path p=new Path(); p.moveTo(x1,y1); p.lineTo(x2,y2); p.lineTo(x3,y3); p.close(); c.drawPath(p,ink);
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
}
