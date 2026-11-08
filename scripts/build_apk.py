#!/usr/bin/env python3
"""Build a debug APK using only JDK 17 + Android SDK 35 tools and Python 3.

Useful in restricted/offline environments once the SDK is installed. Normal
development and CI use Gradle, which also runs Android Lint.
"""
import os
import re
import shutil
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    subprocess.run([str(arg) for arg in args], check=True, cwd=ROOT)


def main():
    sdk_value = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk_value:
        raise SystemExit("Set ANDROID_HOME to an installed Android SDK.")
    sdk = Path(sdk_value).resolve()
    config = (ROOT / "app/build.gradle").read_text()
    value = lambda key: re.search(r"\b" + key + r"\s+['\"]?([^\s'\"]+)", config).group(1)
    platform = sdk / "platforms" / ("android-" + value("compileSdk")) / "android.jar"
    tools = sdk / "build-tools" / "35.0.0"
    if not platform.is_file() or not (tools / "aapt2").is_file():
        raise SystemExit("Install Android SDK Platform 35 and Build Tools 35.0.0.")
    java = shutil.which("java")
    if not java:
        raise SystemExit("JDK 17 is required.")
    work = ROOT / "app/build/sdk-debug"
    if work.exists():
        shutil.rmtree(work)
    for name in ("generated", "classes", "dex"):
        (work / name).mkdir(parents=True)
    resources = work / "resources.zip"
    run(tools / "aapt2", "compile", "--dir", ROOT / "app/src/main/res", "-o", resources)
    unsigned = work / "unsigned.apk"
    # Gradle normally adds the namespace during manifest merging.
    manifest = work / "AndroidManifest.xml"
    manifest.write_text((ROOT / "app/src/main/AndroidManifest.xml").read_text().replace(
        "<manifest ", '<manifest package="' + value("namespace") + '" ', 1).replace("${applicationId}", value("applicationId")))
    run(tools / "aapt2", "link", "-I", platform, "--manifest", manifest,
        "--java", work / "generated", "--min-sdk-version", value("minSdk"),
        "--target-sdk-version", value("targetSdk"), "--version-code", value("versionCode"),
        "--version-name", value("versionName"), "-o", unsigned, resources)
    sources = sorted((ROOT / "app/src/main/java").rglob("*.java")) + sorted((work / "generated").rglob("*.java"))
    compiler = [shutil.which("javac")] if shutil.which("javac") else [java, "com.sun.tools.javac.Main"]
    run(*compiler, "--release", "17", "-encoding", "UTF-8", "-classpath", platform,
        "-d", work / "classes", *sources)
    classes = work / "classes.jar"
    with zipfile.ZipFile(classes, "w") as archive:
        for item in sorted((work / "classes").rglob("*.class")):
            archive.write(item, item.relative_to(work / "classes"))
    run(java, "-cp", tools / "lib/d8.jar", "com.android.tools.r8.D8", "--min-api", value("minSdk"),
        "--lib", platform, "--output", work / "dex", classes)
    with zipfile.ZipFile(unsigned, "a", zipfile.ZIP_DEFLATED) as archive:
        for item in sorted((work / "dex").glob("*.dex")):
            archive.write(item, item.name)
    aligned = work / "aligned.apk"
    run(tools / "zipalign", "-f", "4", unsigned, aligned)
    # Reuse the user's normal Android debug key. Never store keys in the repo.
    key = Path.home() / ".android/debug.keystore"
    if not key.exists():
        key.parent.mkdir(parents=True, exist_ok=True)
        keytool = shutil.which("keytool") or str(Path(java).resolve().parent / "keytool")
        run(keytool, "-genkeypair", "-keystore", key, "-storepass", "android", "-alias", "androiddebugkey",
            "-keypass", "android", "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US")
    output = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    output.parent.mkdir(parents=True, exist_ok=True)
    signer = (java, "-jar", tools / "lib/apksigner.jar")
    run(*signer, "sign", "--ks", key, "--ks-key-alias", "androiddebugkey", "--ks-pass", "pass:android",
        "--key-pass", "pass:android", "--out", output, aligned)
    run(*signer, "verify", "--verbose", output)
    print(f"Built {output}")


if __name__ == "__main__":
    main()
