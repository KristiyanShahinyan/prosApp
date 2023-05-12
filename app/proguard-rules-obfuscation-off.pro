# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-dontpreverify
#-repackageclasses ''
#-allowaccessmodification
#-optimizations !code/simplification/arithmetic

-dontobfuscate
-dontoptimize

-flattenpackagehierarchy digital.paynetics.phos

-keep public class okhttp3.OkHttpClient* { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.codehaus.mojo.**

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation interface com.google.gson.annotations.SerializedName

# Fix buildtime crashes
-dontwarn java.awt.**
-dontwarn build.IgnoreJava8API
-keep class com.sun.jna.** { *; }

# Fix runtime crashes because of enums in the kernel modules
-keepclassmembers enum * { *; }

-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-dontwarn ch.qos.logback.core.net.*
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Square Otto specific rules, used in the reader
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

# detect() is called via reflection so it needs to be kept in release builds
-keepclassmembers class com.framgia.android.emulator.EmulatorDetector {
  private boolean detect();
}

-keep, allowobfuscation class digital.paynetics.phos.security.checks.DexProtectorManager { *; }

-keep class digital.paynetics.phos.numbers.Numbers { *; }

# Keep the entire SecureCell on purpose. Without keep proguard will strip all
# unused code and will make SecureCell smaller. More code makes it harder for
# the attacker to understand the logic.
-keep, allowobfuscation class digital.paynetics.phos.themis.SecureCell { *; }