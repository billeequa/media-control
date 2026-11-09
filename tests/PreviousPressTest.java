package com.jack.bigphoneplayer;

public class PreviousPressTest {
    private static int checks;
    private static void expect(PreviousPress.Action want,PreviousPress.Action got) {
        checks++; if (want!=got) throw new AssertionError("Press check "+checks+": expected "+want+", got "+got);
    }
    public static void main(String[] args) {
        PreviousPress p=new PreviousPress();
        expect(PreviousPress.Action.RESTART,p.press(0,"a","song1",true,true));
        expect(PreviousPress.Action.PREVIOUS,p.press(400,"a","song1",true,true));
        expect(PreviousPress.Action.RESTART,p.press(500,"a","song1",true,true));
        expect(PreviousPress.Action.RESTART,p.press(1600,"a","song1",true,true));
        expect(PreviousPress.Action.RESTART,p.press(1700,"b","song1",true,true));
        expect(PreviousPress.Action.RESTART,p.press(1800,"b","song2",true,true));
        expect(PreviousPress.Action.PREVIOUS,p.press(2800,"b","song2",true,true));
        expect(PreviousPress.Action.PREVIOUS,p.press(3000,"b","stream",false,true));
        expect(PreviousPress.Action.NONE,p.press(3100,"b","stream",false,false));
        expect(PreviousPress.Action.RESTART,p.press(3200,"b","song2",true,false));
        expect(PreviousPress.Action.RESTART,p.press(3300,"b","song2",true,false));
        p.reset(); // Next, source selection and session destruction disarm Previous.
        expect(PreviousPress.Action.RESTART,p.press(3400,"b","song2",true,true));
        expect(PreviousPress.Action.RESTART,p.press(3350,"b","song2",true,true));
        System.out.println("PASS: "+checks+" previous/restart timing, source, track and capability checks");
    }
}
