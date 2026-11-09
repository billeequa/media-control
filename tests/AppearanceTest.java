package com.jack.bigphoneplayer;

public class AppearanceTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Appearance check "+checks+" failed"); }
    private static void close(float a,float b) { check(Math.abs(a-b)<.001f); }
    private static double channel(int value) { double s=value/255d; return s<=.04045 ? s/12.92 : Math.pow((s+.055)/1.055,2.4); }
    private static double light(int c) { return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255); }
    private static double contrast(int a,int b) { double x=light(a),y=light(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); }
    public static void main(String[] args) {
        MarqueeCycle cycle=new MarqueeCycle(60,1); // Two seconds per full loop.
        check(cycle.totalMs()==5000);
        close(cycle.fraction(0),0); close(cycle.fraction(999),0); close(cycle.fraction(1000),0);
        close(cycle.fraction(2000),.5f); close(cycle.fraction(3000),1);
        close(cycle.fraction(4000),1.5f); close(cycle.fraction(5000),2);
        check(!cycle.finished(4999)); check(cycle.finished(5000)); close(cycle.fraction(9000),2);
        float last=0;
        for (long time=0;time<=cycle.totalMs();time+=10) {
            float now=cycle.fraction(time);
            check(now>=last && now-last<.01f); last=now; // No reset/snap at a loop boundary.
        }
        check(cycle.fraction(4990)-cycle.fraction(4980)<cycle.fraction(4500)-cycle.fraction(4490));
        check(new MarqueeCycle(120,2).totalMs()==cycle.totalMs());
        for (float[] invalid:new float[][]{{0,1},{-1,1},{Float.NaN,1},{1,0},{1,Float.POSITIVE_INFINITY}}) {
            boolean rejected=false; try { new MarqueeCycle(invalid[0],invalid[1]); } catch (IllegalArgumentException expected) { rejected=true; } check(rejected);
        }
        for (AppPalette palette:AppPalette.THEMES) {
            check(contrast(palette.text,palette.background)>=4.5);
            check(contrast(palette.muted,palette.background)>=4.5);
            check(contrast(palette.onAccent,palette.accent)>=4.5);
            check(contrast(palette.onAccent,AppPalette.blend(palette.accent,palette.text,.04f))>=4.5);
        }
        check(AppPalette.fromId(-1)==AppPalette.THEMES[0]); check(AppPalette.fromId(99)==AppPalette.THEMES[0]);
        check(AppPalette.fromId(3).name.equals("Paper") && !AppPalette.fromId(3).dark);
        System.out.println("PASS: "+checks+" continuous scrolling and theme readability checks");
    }
}
