#!/usr/bin/env bash
# Builds DataBudget-v2.3.apk without Gradle. Needs: openjdk-21-jdk-headless aapt apksigner android-sdk-platform-23 dalvik-exchange zipalign
set -e
AJ=/usr/lib/android-sdk/platforms/android-23/android.jar
rm -rf obj out gen && mkdir -p obj out gen
aapt package -f -m -J gen -M AndroidManifest.xml -S res -I "$AJ" -F out/unaligned.apk
javac --release 8 -Xlint:-options -classpath "$AJ" -d obj src/com/databudget/*.java gen/com/databudget/R.java
java -cp obj com.databudget.BudgetTest
rm obj/com/databudget/BudgetTest.class
dalvik-exchange --dex --output=out/classes.dex obj
(cd out && aapt add unaligned.apk classes.dex >/dev/null)
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias key -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=DataBudget" >/dev/null 2>&1
zipalign -f 4 out/unaligned.apk out/aligned.apk
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out DataBudget-v2.3.apk out/aligned.apk
apksigner verify -v DataBudget-v2.3.apk | head -3
aapt dump badging DataBudget-v2.3.apk | grep -E "package:|sdkVersion|uses-permission|launchable"
ls -l DataBudget-v2.3.apk
