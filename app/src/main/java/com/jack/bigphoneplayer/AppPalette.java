package com.jack.bigphoneplayer;

/** Stable saved theme IDs; opaque ARGB colors. */
public final class AppPalette {
    public static final AppPalette[] THEMES = {
        new AppPalette("Classic Green",true,0xff10120f,0xff252922,0xffd4f66b,0xfff5f7ef,0xffa3ac9b,0xff10120f,0xff374030),
        new AppPalette("AMOLED",true,0xff000000,0xff121a15,0xff75f5ae,0xfff1fff6,0xff9fb7a8,0xff001b0e,0xff29392f),
        new AppPalette("Midnight",true,0xff0b1326,0xff192640,0xff8ab4f8,0xfff0f4ff,0xffa5b4d0,0xff071321,0xff304462),
        new AppPalette("Paper",false,0xfffaf8f2,0xffe8e4da,0xff25624e,0xff202921,0xff606b61,0xffffffff,0xffcccfc4),
        new AppPalette("Amber",true,0xff171109,0xff302416,0xffffc46b,0xfffff3df,0xffc7b49a,0xff271704,0xff57412a),
        new AppPalette("Rose",true,0xff1b1019,0xff32202d,0xffffa7ce,0xffffeff7,0xffc6adbd,0xff310b22,0xff57384c),
        new AppPalette("Ocean",true,0xff071a20,0xff12323a,0xff63deec,0xffe9fcff,0xffa2c2c8,0xff00292f,0xff28505b),
        new AppPalette("Violet",true,0xff151022,0xff2b2040,0xffc5a6ff,0xfff5efff,0xffbcb0cf,0xff211039,0xff49365e),
        new AppPalette("Monochrome",true,0xff080808,0xff242424,0xffeeeeee,0xfffafafa,0xffb5b5b5,0xff111111,0xff484848),
        new AppPalette("Cloud",false,0xfff3f7ff,0xffe0e9f8,0xff285da8,0xff162b48,0xff4e627e,0xffffffff,0xffbacbe2)
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
