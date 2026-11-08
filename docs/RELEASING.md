# Publish app updates

The Settings update button checks the latest stable GitHub release, downloads its
`big-phone-player.apk` asset, validates the package/version/signing certificate,
and opens Android's installer. It does not use GitHub Actions artifacts or install
silently. APKs must be signed with the same key as the installed app.

## One-time setup

1. Make **billeequa/media-control** public if you want the current no-login updater
   to work. Private releases require authentication; no token is embedded in the app.
   The updater explains when the repository is private or no release exists.
2. Create and securely back up one persistent signing keystore. For this personal
   debug-build distribution, use the Android debug alias/password so Gradle can
   use it without additional signing configuration:

   ```sh
   keytool -genkeypair -keystore media-control.keystore -alias androiddebugkey \
     -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
     -dname 'CN=Big Phone Player,O=Personal,C=US'
   ```

   You can instead reuse the exact `~/.android/debug.keystore` that signed an
   already-installed local build. Do not generate a new key if preserving that
   installation's update compatibility. Never commit the keystore.
3. Base64-encode the keystore and add it as the repository Actions secret
   **ANDROID_DEBUG_KEYSTORE_BASE64** at Settings → Secrets and variables → Actions.

   Linux/macOS:

   ```sh
   base64 < media-control.keystore | tr -d '\n' > signing-key-base64.txt
   ```

   Windows PowerShell:

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path 'media-control.keystore'))) | Set-Content -NoNewline signing-key-base64.txt
   ```

   Paste that file's contents as the secret, keep the original key in a secure
   backup, and remove the temporary text file. The secret is the private key;
   do not put it in the repository or release assets. The connected GitHub
   integration cannot configure repository secrets, so this step is manual.
4. In Actions, run **Publish APK** on `main`. It builds and lints, creates a
   `v0.2.0` release from the current source version and attaches the APK. Install
   this release APK for a baseline that future releases can update in place.

Normal CI also uses that secret when available. Without it, CI artifacts use
temporary runner keys and are development builds; installing one may require
uninstalling it before switching to the persistent-key release series.

## Subsequent updates

1. Increase both `versionCode` and `versionName` in `app/build.gradle`.
2. Push changes and wait for the Android build checks to pass.
3. Run **Publish APK**. Existing releases are never overwritten.
4. On the phone, open Settings → Check for updates → Download → Install. If
   Android asks to allow installs from Big Phone Player, enable it, return to
   Settings and tap **Install downloaded update**. Android asks you to confirm.

Current release builds are for personal sideloading and remain debug builds.
Store distribution and production signing would be a separate setup.
