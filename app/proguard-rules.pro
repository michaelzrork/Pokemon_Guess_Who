# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# For using GSON @Expose annotation
-keepattributes Exceptions

# Gson specific classes
-dontwarn sun.misc.**

# Application classes that will be serialized/deserialized over Gson
-keep class com.example.pokemonguesswho.data.** { *; }
-keep class com.example.pokemonguesswho.network.** { *; }

# Preserve line numbers for debugging stack traces
-renamesourcefileattribute SourceFile
