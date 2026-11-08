package com.jack.bigphoneplayer;
public class PlaybackMathTest {
    private static void equal(long want,long actual) { if (want!=actual) throw new AssertionError("Expected "+want+", got "+actual); }
    private static void equal(String want,String actual) { if (!want.equals(actual)) throw new AssertionError("Expected "+want+", got "+actual); }
    public static void main(String[] args) {
        equal(12000,PlaybackMath.position(10000,1000,3000,1,true,20000));
        equal(10000,PlaybackMath.position(10000,1000,3000,1,false,20000));
        equal(14000,PlaybackMath.position(10000,1000,3000,2,true,20000));
        equal(20000,PlaybackMath.position(19000,1000,5000,1,true,20000));
        equal(-1,PlaybackMath.position(-1,1000,3000,1,true,20000));
        equal(10000,PlaybackMath.position(10000,0,3000,1,true,-1));
        equal(10000,PlaybackMath.position(10000,5000,3000,1,true,-1));
        equal(0,PlaybackMath.position(1000,1000,4000,-1,true,-1));
        equal("—",PlaybackMath.time(-1)); equal("2:03",PlaybackMath.time(123000)); equal("1:02:03",PlaybackMath.time(3723000));
        System.out.println("PASS: 11 playback-position and time-format checks");
    }
}
