#!/usr/bin/env python3
"""Build native UI checks for a disposable Android emulator (not a release asset)."""
import os
import shutil
import zipfile
from pathlib import Path
from build_apk import ROOT, run

sdk_value=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
if not sdk_value:
    raise SystemExit('Set ANDROID_HOME to Android SDK 35.')
sdk=Path(sdk_value);tools=sdk/'build-tools/35.0.0';platform=sdk/'platforms/android-35/android.jar'
work=ROOT/'app/build/ui-checks'
if work.exists():shutil.rmtree(work)
for name in ['classes','dex']:(work/name).mkdir(parents=True)
run(tools/'aapt2','link','-I',platform,'--manifest',ROOT/'tests/android/AndroidManifest.xml','-o',work/'unsigned.apk')
compiler=[shutil.which('javac')] if shutil.which('javac') else ['java','com.sun.tools.javac.Main']
run(*compiler,'--release','17','-classpath',platform,'-d',work/'classes',ROOT/'tests/android/UiChecks.java')
with zipfile.ZipFile(work/'classes.jar','w') as archive:
    for path in (work/'classes').rglob('*.class'):archive.write(path,path.relative_to(work/'classes'))
run('java','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',platform,'--output',work/'dex',work/'classes.jar')
with zipfile.ZipFile(work/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as archive:
    for path in (work/'dex').glob('*.dex'):archive.write(path,path.name)
run(tools/'zipalign','-f','4',work/'unsigned.apk',work/'aligned.apk')
key=Path(os.environ.get('UPDATE_KEYSTORE_PATH',str(Path.home()/'.android/debug.keystore')))
run('java','-jar',tools/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','pass:android','--key-pass','pass:android','--out',work/'checks.apk',work/'aligned.apk')
print('Built '+str(work/'checks.apk'))
