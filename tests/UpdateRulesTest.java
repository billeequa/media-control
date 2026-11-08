package com.jack.bigphoneplayer;

public class UpdateRulesTest {
    private static int checks;
    private static void check(boolean result) { checks++; if (!result) throw new AssertionError("Update check " + checks + " failed"); }
    public static void main(String[] args) {
        check(UpdateRules.compareVersions("v0.2.0","0.1.9") > 0);
        check(UpdateRules.compareVersions("v0.10.0","0.9.9") > 0);
        check(UpdateRules.compareVersions("1.0.0","0.99.99") > 0);
        check(UpdateRules.compareVersions("v0.2.0","0.2.0") == 0);
        check(UpdateRules.compareVersions("v0.1.0","0.2.0") < 0);
        for (String invalid : new String[]{"v1.2.3-beta","1.2","999999999.1.1","1.-1.1","<html>"}) {
            boolean rejected=false; try { UpdateRules.compareVersions(invalid,"0.2.0"); } catch (IllegalArgumentException expected) { rejected=true; } check(rejected);
        }
        check(UpdateRules.isReleaseAsset("https://github.com/billeequa/media-control/releases/download/v0.3.0/big-phone-player.apk"));
        check(!UpdateRules.isReleaseAsset("https://github.com/other/repo/releases/download/v0.3.0/big-phone-player.apk"));
        check(!UpdateRules.isReleaseAsset("https://github.com/billeequa/media-control/releases/download/v0.3.0/wrong.apk"));
        check(UpdateRules.allowedDownload("https://release-assets.githubusercontent.com/file?token=opaque"));
        check(UpdateRules.allowedDownload("https://objects.githubusercontent.com/file"));
        for (String invalid : new String[]{"http://github.com/file","https://github.com.evil.example/file","https://github.com@evil.example/file","https://evil.example@github.com/file","file:///tmp/app.apk","https://github.com:444/file","https://api.github.com/file","not a URL"}) check(!UpdateRules.allowedDownload(invalid));
        System.out.println("PASS: " + checks + " update-version and download-origin checks");
    }
}
