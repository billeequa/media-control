package com.jack.bigphoneplayer.uitest;
import android.app.*;
import android.animation.ValueAnimator;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.List;

public class UiChecks extends Instrumentation {
    int checks=0;
    Object field(Object o,String name) throws Exception { Field f=o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o); }
    Object uncheckedField(Object o,String name) { try{return field(o,name);}catch(Exception e){throw new RuntimeException(e);} }
    void ui(Runnable r) throws Exception { Throwable[] error={null}; runOnMainSync(()->{try{r.run();}catch(Throwable t){error[0]=t;}}); if(error[0]!=null)throw new Exception(error[0]); }
    void check(boolean value,String message) { checks++; if(!value)throw new AssertionError(message); }
    Rect rect(View v) { Rect r=new Rect(); v.getGlobalVisibleRect(r); return r; }
    void settle() throws Exception { waitForIdleSync(); Thread.sleep(350); }
    Activity open() { return startActivitySync(new Intent().setClassName("com.jack.bigphoneplayer","com.jack.bigphoneplayer.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); }
    void tap(View row,float x,float y) throws Exception {
        ui(()->{long t=SystemClock.uptimeMillis(); MotionEvent d=MotionEvent.obtain(t,t,0,x,y,0),u=MotionEvent.obtain(t,t+40,1,x,y,0); row.dispatchTouchEvent(d);row.dispatchTouchEvent(u);d.recycle();u.recycle();}); settle();
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            getTargetContext().getSharedPreferences("appearance",0).edit().putInt("theme",0).commit();
            Activity a=open(); settle();
            View previous=(View)field(a,"previous"),toggle=(View)field(a,"toggle"),next=(View)field(a,"next");
            Activity initial=a;
            ui(()->{
                ((View)uncheckedField(initial,"access")).setVisibility(View.GONE);
                ((View)uncheckedField(initial,"notifications")).setVisibility(View.GONE);
                ((View)uncheckedField(initial,"status")).setVisibility(View.GONE);
            });settle();
            Rect p0=rect(previous),t0=rect(toggle),n0=rect(next);
            TextView clock=(TextView)field(a,"timing"),total=(TextView)field(a,"totalTime");
            check(rect(clock).height()==clock.getHeight()&&rect(total).height()==total.getHeight(),"Larger time display clipped in normal player layout");
            check(rect(clock).bottom<p0.top,"Raised playback row covers visible time display");
            TextView title=(TextView)field(a,"title"),artist=(TextView)field(a,"artist");
            View cover=(View)field(a,"artwork"),dock=(View)field(a,"controlsDock");
            int titleHeight=title.getHeight(),artistHeight=artist.getHeight(),coverSize=cover.getHeight();
            ui(()->{title.setText("A very long song title that must remain on one line and scroll through completely twice without moving the controls");artist.setText("An extremely long artist name with extra collaborators that must also scroll twice");});settle();
            check(p0.equals(rect(previous))&&t0.equals(rect(toggle))&&n0.equals(rect(next)),"Long metadata moved transport controls");
            check(title.getLineCount()==1&&artist.getLineCount()==1,"Metadata wrapped");
            check(title.getHeight()==titleHeight&&artist.getHeight()==artistHeight,"Metadata line height changed");
            Method show=cover.getClass().getDeclaredMethod("showArtwork",Bitmap.class,boolean.class);show.setAccessible(true);
            ui(()->{try{show.invoke(cover,null,true);}catch(Exception e){throw new RuntimeException(e);}});settle();
            check(cover.getHeight()==coverSize&&cover.getWidth()==coverSize,"Loading changed square aspect ratio");
            check(((ViewGroup)cover).getChildAt(0).getVisibility()==View.INVISIBLE&&((ViewGroup)cover).getChildAt(1).getVisibility()==View.VISIBLE,"Loading did not replace art");
            check(p0.equals(rect(previous))&&t0.equals(rect(toggle)),"Loading moved controls");
            ui(()->{try{show.invoke(cover,Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888),false);title.setText("");artist.setText("");}catch(Exception e){throw new RuntimeException(e);}});settle();
            check(title.getHeight()==titleHeight&&artist.getHeight()==artistHeight,"Empty metadata changed line height");
            check(p0.equals(rect(previous))&&t0.equals(rect(toggle))&&n0.equals(rect(next)),"Empty metadata/art moved controls");
            ui(()->title.setText("A second very long metadata value that should animate twice and then remain ellipsized in one reserved line"));settle();
            ValueAnimator animator=(ValueAnimator)field(title,"animation");check(animator!=null,"Overflow did not start scrolling");
            Bitmap[] frames=new Bitmap[2];
            ui(()->{
                animator.setCurrentPlayTime(animator.getDuration()-1);
                frames[0]=Bitmap.createBitmap(title.getWidth(),title.getHeight(),Bitmap.Config.ARGB_8888);
                title.draw(new Canvas(frames[0]));
                animator.end();
                frames[1]=Bitmap.createBitmap(title.getWidth(),title.getHeight(),Bitmap.Config.ARGB_8888);
                title.draw(new Canvas(frames[1]));
            });settle();
            int different=0,pixels=frames[0].getWidth()*frames[0].getHeight();
            for(int y=0;y<frames[0].getHeight();y++)for(int x=0;x<frames[0].getWidth();x++)
                if(frames[0].getPixel(x,y)!=frames[1].getPixel(x,y))different++;
            check(different<pixels*.02,"Final scrolling copy jumped when changing to the static line");
            frames[0].recycle();frames[1].recycle();
            check(field(title,"animation")==null,"Scrolling did not finish");
            ui(()->title.setText(title.getText().toString()));settle();
            check(field(title,"animation")==null,"Equal metadata restarted completed scrolling");
            int[] hits={0}; ui(()->{toggle.setEnabled(true);toggle.setOnClickListener(v->hits[0]++);});
            Rect button=new Rect(); toggle.getDrawingRect(button); ((ViewGroup)dock).offsetDescendantRectToMyCoords(toggle,button);
            float density=a.getResources().getDisplayMetrics().density;
            @SuppressWarnings("unchecked") List<Rect> areas=(List<Rect>)field(dock,"hitAreas");
            Rect hit=areas.get(1);
            check(hit.left==button.left&&hit.right==button.right,"Target changed horizontal bounds");
            check(hit.top==Math.max(0,button.top-button.height()),"Top target is not one button height");
            check(hit.bottom==Math.min(dock.getHeight(),button.bottom+button.height()),"Bottom target is not clamped at player boundary");
            tap(dock,button.centerX(),button.top-button.height()*.9f);check(hits[0]==1,"Top vertical touch extension failed");
            tap(dock,button.centerX(),Math.min(hit.bottom-1,button.bottom+button.height()*.9f));check(hits[0]==2,"Bottom vertical touch extension failed");
            tap(dock,button.left-2*density,button.top-button.height()*.9f);check(hits[0]==2,"Hitbox extended horizontally");
            tap(dock,button.centerX(),hit.top-1);check(hits[0]==2,"Hitbox exceeded vertical allowance");
            View playerTab=(View)field(a,"playerTab"),settingsTab=(View)field(a,"settingsTab");
            check(rect(dock).bottom<=rect(playerTab).top&&rect(dock).bottom<=rect(settingsTab).top,"Targets overlap navigation tabs");
            View root=((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0);
            Rect global=rect(toggle),rootRect=rect(root);
            tap(root,global.centerX()-rootRect.left,global.top-rootRect.top-toggle.getHeight()*.8f);
            check(hits[0]==3,"Overlay did not route an expanded tap through the real player hierarchy");
            int[] tabHits={0};ui(()->settingsTab.setOnClickListener(v->tabHits[0]++));
            Rect tabRect=rect(settingsTab);tap(root,tabRect.centerX()-rootRect.left,tabRect.centerY()-rootRect.top);
            check(tabHits[0]==1&&hits[0]==3,"Transport stole Settings tap");
            Method hint=dock.getClass().getDeclaredMethod("showTouchHints");hint.setAccessible(true);
            ui(()->{try{hint.invoke(dock);}catch(Exception e){throw new RuntimeException(e);}});settle();
            check((Float)field(dock,"hintAlpha")>0,"Opening hint did not show");Thread.sleep(1400);
            check((Float)field(dock,"hintAlpha")==0,"Opening hint did not fade out");
            String[] names={"AMOLED","Midnight","Paper","Amber","Rose","Ocean","Violet","Monochrome","Cloud"};int[] backgrounds={0xff000000,0xff0b1326,0xfffaf8f2,0xff171109,0xff1b1019,0xff071a20,0xff151022,0xff080808,0xfff3f7ff};
            for(int i=1;i<=names.length;i++) {
                getTargetContext().getSharedPreferences("appearance",0).edit().putInt("theme",i).commit();
                final Activity old=a;ui(old::finish);settle();a=open();settle();
                Object palette=field(a,"palette");Field name=palette.getClass().getField("name");
                check(names[i-1].equals(name.get(palette)),"Theme selection did not persist");
                View themedRoot=((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0);
                check(((ColorDrawable)themedRoot.getBackground()).getColor()==backgrounds[i-1],"Theme background not applied");
            }
            result.putString("ui_checks","passed");result.putInt("checks",checks);finish(Activity.RESULT_OK,result);
        } catch(Throwable t) { result.putString("ui_checks","FAILED");result.putString("failure",android.util.Log.getStackTraceString(t));result.putInt("checks",checks);finish(Activity.RESULT_CANCELED,result); }
    }
}
