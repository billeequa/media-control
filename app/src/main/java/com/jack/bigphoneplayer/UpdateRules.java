package com.jack.bigphoneplayer;

import java.net.URI;

/** Update validation shared by Android and plain Java regression tests. */
public final class UpdateRules {
    private UpdateRules() { }

    public static int compareVersions(String left, String right) {
        int[] a = version(left), b = version(right);
        for (int i = 0; i < a.length; i++) {
            int result = Integer.compare(a[i], b[i]);
            if (result != 0) return result;
        }
        return 0;
    }

    private static int[] version(String value) {
        if (value == null || !value.matches("v?\\d{1,6}\\.\\d{1,6}\\.\\d{1,6}")) {
            throw new IllegalArgumentException("The release version is not valid.");
        }
        String[] parts = value.replaceFirst("^v", "").split("\\.");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }

    public static boolean allowedDownload(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && ("github.com".equals(host) || "release-assets.githubusercontent.com".equals(host)
                    || "objects.githubusercontent.com".equals(host));
        } catch (IllegalArgumentException exception) { return false; }
    }

    public static boolean isReleaseAsset(String url) {
        if (!allowedDownload(url)) return false;
        URI uri = URI.create(url);
        return "github.com".equals(uri.getHost())
            && uri.getPath().startsWith("/billeequa/media-control/releases/download/")
            && uri.getPath().endsWith("/big-phone-player.apk");
    }
}
