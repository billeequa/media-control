package com.jack.bigphoneplayer;

public class AppearanceTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Appearance check "+checks+" failed"); }
    private static void close(float a,float b) { check(Math.abs(a-b)<.001f); }
    private static double channel(int value) { double s=value/255d; return s<=.04045 ? s/12.92 : Math.pow((s+.055)/1.055,2.4); }
    private static double light(int c) { return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255); }
    private static double contrast(int a,int b) { double x=light(a),y=light(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); }
    public static void main(String[] args) {
        MarqueeCycle cycle=new MarqueeCycle(60,1); // 2 seconds of motion per pass.
        check(cycle.totalMs()==7600);
        close(cycle.fraction(0),0); close(cycle.fraction(999),0); close(cycle.fraction(1000),0);
        close(cycle.fraction(2000),.5f); close(cycle.fraction(3000),1); close(cycle.fraction(3799),1);
        close(cycle.fraction(3800),0); close(cycle.fraction(5800),.5f); close(cycle.fraction(6800),1);
        check(!cycle.finished(7599)); check(cycle.finished(7600)); close(cycle.fraction(7600),0);
        check(new MarqueeCycle(120,2).totalMs()==cycle.totalMs());
        for (float[] invalid:new float[][]{{-1,1},{Float.NaN,1},{1,0},{1,Float.POSITIVE_INFINITY}}) {
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
        System.out.println("PASS: "+checks+" two-pass scrolling and theme readability checks");
    }
}
