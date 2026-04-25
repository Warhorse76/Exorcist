# Exorcist ProGuard/R8 Rules

# Preserve Shizuku API
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep public class * extends rikka.shizuku.ShizukuProvider

# Preserve SQLCipher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }

# Preserve Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase { *; }

# Preserve Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName *;
}

# Preserve AIDL generated classes
-keep class com.example.exorcist.IExorcistService { *; }
-keep class com.example.exorcist.IExorcistService$Stub { *; }

# General Obfuscation Settings
-repackageclasses ''
-allowaccessmodification
-printmapping mapping.txt
