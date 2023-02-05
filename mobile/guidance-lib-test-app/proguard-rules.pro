# Based on Navi proguard-rules.pro

-dontobfuscate

# http://proguard.sourceforge.net/manual/examples.html#stacktrace
-keepattributes SourceFile,LineNumberTable,LocalVariableTable

# http://proguard.sourceforge.net/manual/troubleshooting.html#attributes
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Navikit
-keep class ru.yandex.yandexnavi.** { *; }
-keep class com.yandex.navistylekit.** { *; }

# Mapkit
-keep class
    !com.yandex.mapkit.atom.**,
    !com.yandex.mapkit.coverage.**,
    !com.yandex.mapkit.direct.**,
    !com.yandex.mapkit.mapview.**,
    !com.yandex.mapkit.masstransit.**,
    !com.yandex.mapkit.panorama.**,
    !com.yandex.mapkit.photos.**,
    !com.yandex.mapkit.render.**,
    !com.yandex.mapkit.reviews.**,
    !com.yandex.mapkit.taxi.**,
    !com.yandex.mapkit.test_support.**,
    !com.yandex.mapkit.**test**,
    com.yandex.mapkit.** { *; }

# Runtime, maps, datasynk
-keep class !com.yandex.runtime.**test**, com.yandex.runtime.** { *; }
-keep class !com.yandex.maps.**test**, com.yandex.maps.** { *; }
-keep class com.yandex.datasync.** { *; }
-dontwarn 
    com.yandex.maps.**,
    com.google.android.libraries.car.app.**

# Quick fix for warnings
# https://github.com/Kotlin/kotlinx.coroutines/issues/1270
# https://youtrack.jetbrains.com/issue/KT-31242
-dontwarn **inlined**
