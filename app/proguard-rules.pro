# Keep classes referenced from AndroidManifest by name (Activities, etc.)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Compose runtime relies on reflection in a few corners — keep stability info.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Kotlin coroutines — let R8 do its thing but keep service loaders.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Our own model classes — small, safe to keep
-keep class com.pingsama.puzzlequest.game.** { *; }
