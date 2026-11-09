#!/usr/bin/env python3
"""Reject a release signed with any key other than the persistent app key."""
import argparse
import hashlib
import os
import re
import shutil
import subprocess
from pathlib import Path

EXPECTED_CERT = 'c20208114802163760627405beed656a63a997a8186da74d77d929818e90a51b'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('apk', nargs='?', default='app/build/outputs/apk/debug/app-debug.apk')
    args = parser.parse_args()
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise SystemExit('ANDROID_HOME is required.')
    keystore = Path(os.environ.get('UPDATE_KEYSTORE_PATH', str(Path.home() / '.android/debug.keystore')))
    keytool = shutil.which('keytool')
    if not keytool:
        raise SystemExit('keytool from JDK 17 is required.')
    certificate = subprocess.run([keytool, '-exportcert', '-keystore', str(keystore),
        '-storepass', 'android', '-alias', 'androiddebugkey'], check=True, capture_output=True).stdout
    if hashlib.sha256(certificate).hexdigest() != EXPECTED_CERT:
        raise SystemExit('The configured keystore is not the persistent Big Phone Player signing key.')
    output = subprocess.run(['java', '-jar', str(Path(sdk) / 'build-tools/35.0.0/lib/apksigner.jar'),
        'verify', '--print-certs', args.apk], check=True, capture_output=True, text=True).stdout
    certificates = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)', output)
    if certificates != [EXPECTED_CERT]:
        raise SystemExit('APK signing certificate differs from the persistent key. Publication blocked.')
    print('PASS: APK signature verifies and matches the persistent signing certificate: ' + EXPECTED_CERT)


if __name__ == '__main__':
    main()
