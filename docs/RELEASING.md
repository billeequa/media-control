# Publish app updates

The repository is public and its persistent signing key is configured as the
Actions secret `ANDROID_DEBUG_KEYSTORE_BASE64`. Never commit or publish that
secret, its base64 text, or the keystore. Keep the private signing backup safe.

The public signing-certificate SHA-256 fingerprint is:

```
c20208114802163760627405beed656a63a997a8186da74d77d929818e90a51b
```

The workflow sets `UPDATE_KEYSTORE_PATH`, Gradle explicitly uses that keystore,
and `scripts/verify_signing.py` checks both the configured key and the APK against
this fingerprint before publishing. Release packaging bypasses the build cache.
A successful build alone is not proof of the correct signing identity.

## Publish the next version

1. Increase both `versionCode` and `versionName` in `app/build.gradle`.
2. Push changes and wait for **both build and ui jobs** in Android build to pass.
   The ui job runs the native instrumentation checks on an Android 35 emulator.
3. Push a new matching version tag (for example, `git tag v0.5.0` followed by
   `git push origin v0.5.0`), or run **Publish APK** manually on `main` in Actions.
   Both paths test, build, lint, verify the persistent signing certificate, and
   attach `big-phone-player.apk` to a new release. A pushed tag must exactly
   match `versionName`; existing releases/tags are never replaced.
4. Check the published asset's SHA-256 digest and certificate after download.

The Settings update button checks the latest stable GitHub release, downloads
its APK, checks its package/version/certificate, and opens Android's installer.
It does not use Actions artifacts or install silently. Android asks the user to
approve installation. When prompted, allow installs from Big Phone Player, then
return to Settings and tap **Install downloaded update**.

## Initial installation

Install **v0.2.1 or later** from Releases to join the persistent signing-key
series. The first v0.2.0 publication used a temporary runner key; its tag is kept
for history and should not be used as the update baseline. If an early test APK
is already installed, Android may require uninstalling that test app first.
Subsequent releases in the persistent-key series update in place.

Current builds are personal sideloading debug builds, not store releases. The
keystore alias is `androiddebugkey` and its store/key passwords are `android`;
the private keystore itself must remain secret.

## Restoring the key

Use `media-control.keystore` from the private signing backup. Set the repository
Actions secret `ANDROID_DEBUG_KEYSTORE_BASE64` to the exact contents of the
backup's `signing-key-base64.txt`. The secret is base64 of the existing key,
not a new generated key. Never replace it with a fresh key if preserving update
compatibility. The verification step intentionally blocks a different key.

For a local Gradle build, set `UPDATE_KEYSTORE_PATH` to that keystore. A fresh
local Android debug key can produce a standalone test installation, but cannot
replace the signed release installation.
