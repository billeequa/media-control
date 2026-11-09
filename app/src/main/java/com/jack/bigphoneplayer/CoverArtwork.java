package com.jack.bigphoneplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** One square footprint shared by the album image, placeholder and loading text. */
final class CoverArtwork extends FrameLayout {
    private final ImageView image;
    private final TextView loading;
    private final Drawable placeholder;
    CoverArtwork(Context context,AppPalette palette) {
        super(context); setClipToOutline(true); setBackgroundColor(palette.panel);
        image=new ImageView(context); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(image,new LayoutParams(-1,-1));
        loading=new TextView(context); loading.setText("Loading…"); loading.setTextSize(20);
        loading.setTextColor(palette.muted); loading.setGravity(Gravity.CENTER);
        loading.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        loading.setVisibility(GONE); addView(loading,new LayoutParams(-1,-1));
        placeholder=new Drawable() {
            private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                float cx=getBounds().exactCenterX(),cy=getBounds().exactCenterY();
                float radius=Math.min(getBounds().width(),getBounds().height())*.21f;
                paint.setColor(palette.accent); paint.setPathEffect(new CornerPathEffect(radius*.13f));
                Path path=new Path(); path.moveTo(cx-radius*.55f,cy-radius); path.lineTo(cx+radius,cy);
                path.lineTo(cx-radius*.55f,cy+radius); path.close(); canvas.drawPath(path,paint);
            }
            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
            @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view,Outline outline) {
                outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),18*getResources().getDisplayMetrics().density);
            }
        });
    }
    void showArtwork(Bitmap bitmap,boolean buffering) {
        if (buffering) {
            image.setImageDrawable(null); image.setVisibility(INVISIBLE); loading.setVisibility(VISIBLE);
            setContentDescription("Loading album artwork");
        } else {
            if (bitmap==null) image.setImageDrawable(placeholder); else image.setImageBitmap(bitmap);
            loading.setVisibility(GONE); image.setVisibility(VISIBLE);
            setContentDescription(bitmap==null ? "Album artwork unavailable" : "Album artwork");
        }
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int available=MeasureSpec.getSize(widthSpec);
        View parent=getParent() instanceof View ? (View)getParent() : null;
        int padding=parent==null ? 0 : parent.getPaddingLeft()+parent.getPaddingRight();
        int size=Math.max(1,Math.min(available,Math.round((available+padding)*.8f)));
        super.onMeasure(MeasureSpec.makeMeasureSpec(size,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(size,MeasureSpec.EXACTLY));
    }
}
