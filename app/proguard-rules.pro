# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 保留行号信息，便于调试
-keepattributes SourceFile,LineNumberTable

# 隐藏原始源文件名
-renamesourcefileattribute SourceFile

# Glide 图片加载库
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# 保持 Glide 生成的 API
-keep class * extends com.bumptech.glide.GeneratedAppGlideModule

# OkHttp3
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 保持所有实体类不被混淆
-keep class com.example.simpleeditingpictureapp.model.** { *; }
-keep class com.example.simpleeditingpictureapp.viewmodel.** { *; }
-keep class com.example.simpleeditingpictureapp.activity.** { *; }
-keep class com.example.simpleeditingpictureapp.fragment.** { *; }
-keep class com.example.simpleeditingpictureapp.opengl_es.** { *; }

# 保持所有实现了 Parcelable 接口的类
-keep class * implements android.os.Parcelable {
  public static final ** CREATOR;
}

# 保持所有实现了 Serializable 接口的类
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保持所有 View 相关的类
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 保持所有自定义 View
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# 保持所有枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持所有 R 类中的资源
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保持所有 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}