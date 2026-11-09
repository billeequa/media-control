package com.jack.bigphoneplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Image, private text label, loading and empty state share one bounded footprint. */
final class CoverArtwork extends FrameLayout {
    private final ImageView image;
    private final TextView loading;
    private final LinearLayout label;
    private final TextView labelArtist,labelAlbum,labelSong;
    private final Drawable placeholder;
    private boolean grayscale,textOnly,compact;
    CoverArtwork(Context context,AppPalette palette) {
        super(context); setClipToOutline(true); setBackgroundColor(palette.panel);
        image=new ImageView(context); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(image,new LayoutParams(-1,-1));
        loading=new TextView(context); loading.setTextSize(20); loading.setTextColor(palette.muted); loading.setGravity(Gravity.CENTER);
        loading.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        loading.setVisibility(GONE); addView(loading,new LayoutParams(-1,-1));
        label=new LinearLayout(context); label.setOrientation(LinearLayout.VERTICAL); label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(18),dp(10),dp(18),dp(10));
        labelArtist=labelLine(20,palette.text); labelAlbum=labelLine(17,palette.muted); labelSong=labelLine(22,palette.text);
        labelSong.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        for(TextView line:new TextView[]{labelArtist,labelAlbum,labelSong}) label.addView(line,new LinearLayout.LayoutParams(-1,-2));
        label.setVisibility(GONE); addView(label,new LayoutParams(-1,-1));
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
                outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(18));
            }
        });
    }
    private TextView labelLine(int size,int color) {
        TextView line=new TextView(getContext()); line.setTextSize(size); line.setTextColor(color);
        line.setSingleLine(true); line.setEllipsize(TextUtils.TruncateAt.END); return line;
    }
    void setPresentation(boolean grayscale,boolean textOnly,boolean compact) {
        this.grayscale=grayscale; this.textOnly=textOnly; this.compact=compact;
        ColorMatrix matrix=new ColorMatrix(); matrix.setSaturation(0);
        image.setColorFilter(grayscale ? new ColorMatrixColorFilter(matrix) : null);
        requestLayout();
    }
    void showContent(Bitmap bitmap,boolean buffering,boolean available,String artist,String album,String song) {
        // Clear hidden artwork as well as hiding its view; text mode never retains a drawable.
        image.setImageDrawable(null); image.setVisibility(INVISIBLE); label.setVisibility(GONE); loading.setVisibility(GONE);
        labelArtist.setText(""); labelAlbum.setText(""); labelSong.setText("");
        if (!available || buffering) {
            String message=available ? "Loading…" : "No player found";
            loading.setText(message); loading.setVisibility(VISIBLE); setContentDescription(message); return;
        }
        if (textOnly || compact) {
            labelArtist.setText(empty(artist,"Unknown artist")); labelAlbum.setText(empty(album,"Unknown album")); labelSong.setText(empty(song,"Untitled track"));
            label.setVisibility(VISIBLE); setContentDescription("Artist: "+labelArtist.getText()+". Album: "+labelAlbum.getText()+". Song: "+labelSong.getText());
        } else {
            image.setImageDrawable(bitmap==null ? placeholder : null);
            if(bitmap!=null) image.setImageBitmap(bitmap);
            image.setVisibility(VISIBLE); setContentDescription(bitmap==null ? "Album artwork unavailable" : "Album artwork");
        }
    }
    // Shared rendering path used by image/loading geometry checks.
    void showArtwork(Bitmap bitmap,boolean buffering) { showContent(bitmap,buffering,true,"","",""); }
    private String empty(String value,String fallback) { return value==null || value.trim().isEmpty() ? fallback : value; }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int available=MeasureSpec.getSize(widthSpec);
        int window=getRootView().getWidth();
        if(window<=0) window=getResources().getDisplayMetrics().widthPixels;
        int width=Math.max(1,Math.min(available,Math.round(window*.8f)));
        int height=compact ? dp(112) : width;
        if(MeasureSpec.getMode(heightSpec)!=MeasureSpec.UNSPECIFIED) height=Math.min(height,MeasureSpec.getSize(heightSpec));
        if(!compact) width=height;
        super.onMeasure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,height),MeasureSpec.EXACTLY));
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
