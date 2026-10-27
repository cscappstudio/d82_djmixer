# ============================================================
# ProGuard / R8 Rules for DJ Mixer
# com.audioeffect.djmixer.music.mixer
# ============================================================

# ---------------------- General Optimization & Debug ----------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions

# ---------------------- Kotlin & Coroutines ----------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------- Android Views & Lifecycle ----------------------
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

# ---------------------- Parcelable, Serializable & Enums ----------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------------- Native Methods ----------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------------------- BASS Audio Engine ----------------------
-keep class com.un4seen.bass.** { *; }
-keepclassmembers class com.un4seen.bass.** { *; }
-dontwarn com.un4seen.bass.**

# ---------------------- CSC App Library & Configs ----------------------
-keep class com.cscapp.** { *; }
-keep class com.cscmobi.libraryads.** { *; }
-keep interface com.cscmobi.libraryads.** { *; }
-keepclassmembers class * implements com.cscmobi.libraryads.commons.remote.CSCKonfigModel { *; }
-keep class com.audioeffect.djmixer.music.mixer.core.ads.** { *; }
-keepclassmembers class com.audioeffect.djmixer.music.mixer.core.ads.** { *; }
-dontwarn com.cscapp.**
-dontwarn com.cscmobi.libraryads.**

# ---------------------- Ads Mediation Adapters (Matching d75_themeapp) ----------------------
# Meta / Facebook Audience Network
-keep class com.google.ads.mediation.facebook.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# AppLovin Mediation
-keep class com.google.ads.mediation.applovin.** { *; }
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# InMobi Mediation
-keep class com.google.ads.mediation.inmobi.** { *; }
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-dontwarn com.squareup.picasso.**

# Pangle (Bytedance) & Adapter
-keep class com.bytedance.sdk.** { *; }
-keep interface com.bytedance.sdk.** { *; }
-keep class com.bytedance.adsdk.** { *; }
-keep class com.bytedance.mobsec.** { *; }
-keep class com.bytedance.applog.** { *; }
-keep class com.apm.insight.** { *; }
-keep class com.bykv.vk.** { *; }
-keep interface com.bykv.vk.** { *; }
-keep class com.com.bytedance.overseas.sdk.** { *; }
-keep class com.iab.omid.library.bytedance2.** { *; }
-keep class com.pgl.ssdk.** { *; }
-keep class com.tiktok.** { *; }
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.bykv.vk.openvk.** { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Pangle JNI & Native Security (Crucial for Bidding Token Generation)
-keep class ms.bd.o.Pgl.** { *; }
-keep class ms.bd.c.** { *; }
-keep class ms.bd.o.** { *; }
-keep class com.pgl.sys.ces.** { *; }

# Google AdMob Mediation Adapter for Pangle
-keep class com.google.ads.mediation.pangle.** { *; }

-dontwarn com.bytedance.**
-dontwarn com.bykv.**
-dontwarn com.pgl.**
-dontwarn ms.bd.**
-dontwarn com.iab.omid.**
-dontwarn com.tiktok.**
-dontwarn com.google.ads.mediation.pangle.**

# MINTEGRAL SDK
-keep class com.mbridge.** { *; }
-dontwarn com.mbridge.**
-keep interface com.mbridge.** { *; }
-keep class * extends com.mbridge.msdk.out.BaseExtraInterfaceForHandler { *; }
-keep class com.google.ads.mediation.mintegral.** { *; }

# Unity Ads Mediation
-keep class com.google.ads.mediation.unity.** { *; }
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.services.**

# Pubscale Adapter
-keep class com.pubscale.** { *; }
-dontwarn com.pubscale.**

# Google Next-Gen Mobile Ads SDK & Ad Inspector
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-dontwarn com.google.android.libraries.ads.mobile.sdk.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-dontwarn com.bumptech.glide.**
