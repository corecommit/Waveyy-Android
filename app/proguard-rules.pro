# Waveyy release R8 rules. Minification stays off until signing is configured
# (see the release signing template in app/build.gradle.kts); these rules
# take effect the moment `isMinifyEnabled` flips on.

# --- Retrofit + OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- kotlinx-serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class * {
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclasseswithmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**
-keepclassmembers class * {
    @androidx.room.Query *;
}

# --- Supabase Kotlin + Ktor ---
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**
-keep class io.github.jan.supabase.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod

# --- Socket.IO (engine.io) ---
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-dontwarn org.json.**

# --- kotlinx-coroutines ---
-dontwarn kotlinx.coroutines.**
-keepattributes Signature, InnerClasses, EnclosingMethod

# --- Coil ---
-dontwarn coil3.**
-keep class coil3.** { *; }
