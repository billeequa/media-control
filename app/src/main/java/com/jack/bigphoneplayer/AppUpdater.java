package com.jack.bigphoneplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-initiated public GitHub release updater. No token or background checks. */
final class AppUpdater {
    private static final String ENDPOINT = "https://api.github.com/repos/billeequa/media-control/releases/latest";
    private static final long MAX_APK = 100L * 1024 * 1024;
    private final Activity activity;
    private final Button check, install;
    private final TextView status;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final File downloaded;
    private boolean busy;
    private volatile boolean closed;

    AppUpdater(Activity activity, Button check, Button install, TextView status) {
        this.activity = activity; this.check = check; this.install = install; this.status = status;
        downloaded = new File(activity.getCacheDir(), "updates/update.apk");
        install.setVisibility(View.GONE);
        check.setOnClickListener(v -> check());
        install.setOnClickListener(v -> install());
        if (downloaded.exists()) {
            try { validateApk(downloaded); install.setVisibility(View.VISIBLE); status.setText("A downloaded update is ready to install."); }
            catch (Exception ignored) { downloaded.delete(); }
        }
    }

    void close() { closed = true; worker.shutdownNow(); }
    private void ui(Runnable work) { activity.runOnUiThread(() -> { if (!closed && !activity.isDestroyed()) work.run(); }); }
    private void start(String message) { busy = true; check.setEnabled(false); install.setEnabled(false); status.setText(message); }
    private void finish(String message) { busy = false; check.setEnabled(true); install.setEnabled(true); status.setText(message); }
    private String currentVersion() throws PackageManager.NameNotFoundException {
        return activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
    }

    private void check() {
        if (busy) return;
        start("Checking GitHub…");
        worker.execute(() -> {
            try {
                JSONObject release = new JSONObject(readRelease());
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) throw new IOException("No stable update is available.");
                String version = release.getString("tag_name");
                if (UpdateRules.compareVersions(version,currentVersion()) <= 0) {
                    ui(() -> finish("You have the latest version.")); return;
                }
                JSONArray assets = release.getJSONArray("assets");
                JSONObject apk = null;
                for (int i=0; i<assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    if ("big-phone-player.apk".equals(asset.optString("name"))) { apk = asset; break; }
                }
                if (apk == null) throw new IOException("The latest release has no APK attached yet.");
                String url = apk.getString("browser_download_url");
                long size = apk.optLong("size",-1);
                String digest = apk.optString("digest", "");
                if (!UpdateRules.isReleaseAsset(url)) throw new IOException("The release APK address is invalid.");
                ui(() -> {
                    finish("Version " + version + " is available.");
                    new AlertDialog.Builder(activity).setTitle("Update available")
                        .setMessage("Download " + version + " from GitHub? Android will ask you to approve installation.")
                        .setPositiveButton("Download",(dialog,which) -> download(url,size,digest))
                        .setNegativeButton("Later",null).show();
                });
            } catch (Exception exception) { ui(() -> finish(errorMessage(exception))); }
        });
    }

    private String readRelease() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(15000); connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept","application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        connection.setRequestProperty("User-Agent","BigPhonePlayer");
        try {
            int code = connection.getResponseCode();
            if (code == 404) throw new IOException("No public release found. The repository must be public and have a published release with an APK.");
            if (code == 403 || code == 429) throw new IOException("GitHub's request limit was reached. Try again later.");
            if (code != 200) throw new IOException("GitHub could not check updates (HTTP " + code + ").");
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (closed) throw new IOException("Update check cancelled.");
                    if (bytes.size() + count > 1024*1024) throw new IOException("The release response is too large.");
                    bytes.write(buffer,0,count);
                }
                return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
    }

    private HttpURLConnection openDownload(String address) throws IOException {
        for (int redirects=0; redirects<6; redirects++) {
            if (!UpdateRules.allowedDownload(address)) throw new IOException("The download left GitHub's release servers.");
            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent","BigPhonePlayer");
            int code;
            try { code = connection.getResponseCode(); } catch (IOException failure) { connection.disconnect(); throw failure; }
            if (code == 200) return connection;
            String location = connection.getHeaderField("Location"); connection.disconnect();
            if ((code == 301 || code == 302 || code == 303 || code == 307 || code == 308) && location != null) {
                address = new URL(new URL(address),location).toString();
            } else { throw new IOException("The APK could not be downloaded (HTTP " + code + ")."); }
        }
        throw new IOException("The download redirected too many times.");
    }

    private void download(String url, long expectedSize, String expectedDigest) {
        if (busy) return;
        start("Downloading update…"); install.setVisibility(View.GONE);
        worker.execute(() -> {
            File partial = new File(downloaded.getParentFile(),"update.apk.part");
            try {
                if (expectedSize > MAX_APK) throw new IOException("The update is too large.");
                Files.createDirectories(downloaded.getParentFile().toPath());
                HttpURLConnection connection = openDownload(url);
                long total = 0;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(partial)) {
                    byte[] buffer = new byte[32768]; int count;
                    while ((count=input.read(buffer)) != -1) {
                        if (closed || Thread.currentThread().isInterrupted()) throw new IOException("Download cancelled.");
                        total += count;
                        if (total > MAX_APK) throw new IOException("The update is too large.");
                        output.write(buffer,0,count); digest.update(buffer,0,count);
                    }
                } finally { connection.disconnect(); }
                if (expectedSize > 0 && total != expectedSize) throw new IOException("The download is incomplete. Try again.");
                if (expectedDigest.startsWith("sha256:") && !expectedDigest.substring(7).equalsIgnoreCase(hex(digest.digest()))) {
                    throw new IOException("The update checksum did not match. Try again.");
                }
                validateApk(partial);
                Files.move(partial.toPath(),downloaded.toPath(),StandardCopyOption.REPLACE_EXISTING);
                ui(() -> {
                    finish("Update downloaded. Tap below to install."); install.setVisibility(View.VISIBLE);
                    new AlertDialog.Builder(activity).setTitle("Ready to update")
                        .setMessage("Open Android's installer to finish the update.")
                        .setPositiveButton("Install",(dialog,which) -> install()).setNegativeButton("Later",null).show();
                });
            } catch (Exception exception) { partial.delete(); ui(() -> finish(errorMessage(exception))); }
        });
    }

    private void validateApk(File file) throws IOException, PackageManager.NameNotFoundException {
        PackageManager manager = activity.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate = manager.getPackageArchiveInfo(file.getAbsolutePath(),flags);
        PackageInfo installed = manager.getPackageInfo(activity.getPackageName(),flags);
        if (candidate == null || !activity.getPackageName().equals(candidate.packageName)) throw new IOException("The download is not a Big Phone Player update.");
        long newCode, oldCode;
        if (Build.VERSION.SDK_INT >= 28) { newCode=candidate.getLongVersionCode(); oldCode=installed.getLongVersionCode(); }
        else { newCode=candidate.versionCode; oldCode=installed.versionCode; }
        if (newCode <= oldCode) throw new IOException("This APK is not newer than the installed app.");
        Signature[] next, current;
        if (Build.VERSION.SDK_INT >= 28) {
            next=candidate.signingInfo == null ? null : candidate.signingInfo.getApkContentsSigners();
            current=installed.signingInfo == null ? null : installed.signingInfo.getApkContentsSigners();
        } else { next=candidate.signatures; current=installed.signatures; }
        if (next == null || current == null || next.length == 0 || next.length != current.length || !Arrays.asList(next).containsAll(Arrays.asList(current))) {
            throw new IOException("The update uses a different signing key and cannot replace this installation.");
        }
    }

    private void install() {
        try {
            validateApk(downloaded);
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                status.setText("Allow updates from Big Phone Player, return here, then tap Install downloaded update.");
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:" + activity.getPackageName()))); return;
            }
            Uri uri = Uri.parse("content://" + activity.getPackageName() + ".updates/update.apk");
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(android.content.ClipData.newRawUri("Update APK",uri));
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) { status.setText("Android's installer or update settings are unavailable on this device."); }
        catch (Exception exception) { status.setText(errorMessage(exception)); }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(); for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT,"%02x",b & 255)); return result.toString();
    }
    private static String errorMessage(Exception exception) {
        if (exception instanceof IOException || exception instanceof IllegalArgumentException) return exception.getMessage() == null ? "The update failed. Try again." : exception.getMessage();
        return "The update could not be checked or downloaded. Try again later.";
    }
}
