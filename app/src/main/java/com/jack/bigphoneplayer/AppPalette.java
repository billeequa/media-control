package com.jack.bigphoneplayer;

/** Stable saved theme IDs; opaque ARGB colors. */
public final class AppPalette {
    public static final AppPalette[] THEMES = {
        new AppPalette("Classic Green",true,0xff10120f,0xff252922,0xffd4f66b,0xfff5f7ef,0xffa3ac9b,0xff10120f,0xff374030),
        new AppPalette("AMOLED",true,0xff000000,0xff121a15,0xff75f5ae,0xfff1fff6,0xff9fb7a8,0xff001b0e,0xff29392f),
        new AppPalette("Midnight",true,0xff0b1326,0xff192640,0xff8ab4f8,0xfff0f4ff,0xffa5b4d0,0xff071321,0xff304462),
        new AppPalette("Paper",false,0xfffaf8f2,0xffe8e4da,0xff25624e,0xff202921,0xff606b61,0xffffffff,0xffcccfc4)
    };
    public final String name;
    public final boolean dark;
    public final int background,panel,accent,text,muted,onAccent,stroke;
    private AppPalette(String name,boolean dark,int background,int panel,int accent,int text,int muted,int onAccent,int stroke) {
        this.name=name; this.dark=dark; this.background=background; this.panel=panel; this.accent=accent;
        this.text=text; this.muted=muted; this.onAccent=onAccent; this.stroke=stroke;
    }
    public static AppPalette fromId(int id) { return THEMES[id>=0 && id<THEMES.length ? id : 0]; }
    public static int blend(int first,int second,float amount) {
        float t=Math.max(0,Math.min(1,amount));
        int r=Math.round(((first>>16)&255)*(1-t)+((second>>16)&255)*t);
        int g=Math.round(((first>>8)&255)*(1-t)+((second>>8)&255)*t);
        int b=Math.round((first&255)*(1-t)+(second&255)*t);
        return 0xff000000 | (r<<16) | (g<<8) | b;
    }
}
