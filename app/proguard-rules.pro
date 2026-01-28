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
# Keep classes that are referenced dynamically
# Keep data classes used by Firebase and Google Sign-In
# This is often the missing piece that causes sign-in to fail silently.
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.firebase.auth.** { *; }

# Keep the model classes that are used to serialize/deserialize data.
# The following rule is a general safeguard for any data classes.
-keepattributes Signature
-keepclassmembers class * extends java.lang.Object {
    @com.google.firebase.database.PropertyName <fields>;
}

# Keep public constructors for activities, services etc.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
# Google Sign-In and Firebase Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.firebase.ui.auth.** { *; }
-keep class com.shobhitpuri.custombuttons.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class org.apache.** { *; }
-keep class javax.** { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Entity { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}

# Keep ViewModel and LiveData
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.LiveData { *; }

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep your application package
-keep class com.example.monktemple.** { *; }

# Keep MPAndroidChart library
-keep class com.github.mikephil.charting.** { *; }

# Keep parcelable classes
-keep class * implements android.os.Parcelable { *; }
# Keep Statistics class and related methods
-keep class com.example.monktemple.Statistics { *; }