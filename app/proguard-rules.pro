# ── Google Gemini AI SDK ────────────────────────────────────────────────
-keep class com.google.ai.** { *; }
-dontwarn com.google.ai.**

# ── Room Database ───────────────────────────────────────────────────────
-keep class com.adonnis.app.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# ── Kotlinx Serialization ───────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.adonnis.app.**$$serializer { *; }
-keepclassmembers class com.adonnis.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.adonnis.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Coroutines ──────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Compose ─────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ── AndroidX Security (EncryptedSharedPreferences) ──────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── General Android ─────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod

# Keep all app model/entity classes
-keep class com.adonnis.app.data.model.** { *; }

# Keep enum classes
-keepclassmembers enum * { *; }
