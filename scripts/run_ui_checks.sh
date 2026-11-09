#!/usr/bin/env bash
# Run only on a disposable emulator. Keep captured results as Actions artifacts.
set -euo pipefail
cd "$(dirname "$0")/.."
adb install -r app/build/ui-checks/checks.apk
mkdir -p app/build/ui-checks
# Kill any stale instrumentation; am instrument prints failures with exit code 0.
timeout 180 adb shell am instrument -w com.jack.bigphoneplayer.uitest/.UiChecks | tee app/build/ui-checks/results.txt
adb exec-out screencap -p > app/build/ui-checks/results-screen.png
if ! grep -q '^INSTRUMENTATION_RESULT: ui_checks=passed$' app/build/ui-checks/results.txt; then
    adb logcat -d -s AndroidRuntime >> app/build/ui-checks/results.txt
    exit 1
fi
