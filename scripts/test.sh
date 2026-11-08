#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
if command -v javac >/dev/null 2>&1; then
    compiler=(javac)
else
    compiler=(java com.sun.tools.javac.Main)
fi
"${compiler[@]}" -d "$test_dir" app/src/main/java/com/jack/bigphoneplayer/PlaybackMath.java tests/PlaybackMathTest.java
java -cp "$test_dir" com.jack.bigphoneplayer.PlaybackMathTest
