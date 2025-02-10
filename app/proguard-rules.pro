
# Keep Important Attributes
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions, *Annotation*

# Keep RxJava
-dontwarn rx.**
-keep class rx.** { *; }
-keep class io.reactivex.** { *; }
-dontwarn io.reactivex.**
-keepclassmembers class io.reactivex.** { *; }

# Keep HiveMQ and MQTT
-keep class com.hivemq.** { *; }
-keep class org.mqtt.** { *; }
-keepclassmembers class com.hivemq.** { *; }
-keepclassmembers class org.mqtt.** { *; }
-dontwarn com.hivemq.**
-dontwarn org.mqtt.**

# Spezifisch für das HiveMQ-Problem
-keep class **.MpscArrayQueue { *; }
-keep class **.SpscArrayQueue { *; }
-keepclassmembers class **.MpscArrayQueue { *; }
-keepclassmembers class **.SpscArrayQueue { *; }

# Netty
-keepclassmembers class io.netty.** { *; }
-keepnames class io.netty.** { *; }
-dontwarn io.netty.**
-dontwarn reactor.netty.**

# Keep Atomic* and Concurrent classes
-keepclassmembers class * {
    volatile long *;
    volatile int *;
    volatile boolean *;
    volatile byte *;
    java.util.concurrent.atomic.AtomicInteger *;
    java.util.concurrent.atomic.AtomicLong *;
    java.util.concurrent.atomic.AtomicBoolean *;
    java.util.concurrent.atomic.AtomicReference *;
    java.util.concurrent.ConcurrentHashMap *;
    java.util.concurrent.ConcurrentLinkedQueue *;
    java.util.concurrent.ConcurrentLinkedDeque *;
}

# Keep specific fields
-keepclassmembers class ** {
    private static final % consumerIndex;
    private static final % producerIndex;
    private static final % producerLimit;
    private static final % consumerLimit;
}

# Keep all enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Dagger
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }

# Additional Netty specific rules
-dontwarn org.jboss.netty.**
-dontwarn io.netty.buffer.**
-dontwarn io.netty.handler.**
-dontwarn io.netty.util.**

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Metadata
-keepattributes RuntimeVisible*Annotations
-keepattributes AnnotationDefault

# Location Services
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Additional don't warn rules
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn io.netty.internal.tcnative.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**