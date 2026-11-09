package com.jack.bigphoneplayer;

import android.content.Context;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

/** Square album sleeve, centered by its parent at 80% of the window width. */
final class CoverArtwork extends ImageView {
    CoverArtwork(Context context) {
        super(context); setScaleType(ScaleType.FIT_CENTER); setClipToOutline(true);
        setBackgroundColor(0xff252922);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view,Outline outline) {
                outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),18*getResources().getDisplayMetrics().density);
            }
        });
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int available=MeasureSpec.getSize(widthSpec);
        View parent=getParent() instanceof View ? (View)getParent() : null;
        int padding=parent == null ? 0 : parent.getPaddingLeft()+parent.getPaddingRight();
        int size=Math.max(1,Math.min(available,Math.round((available+padding)*.8f)));
        setMeasuredDimension(size,size);
    }
}
