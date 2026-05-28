# Preserve generic type info for Retrofit's reflective Call/Response handling.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# CameraX — Camera2Config is loaded reflectively as the CameraXConfig provider.
-keep class androidx.camera.camera2.Camera2Config { *; }
-keep class androidx.camera.camera2.Camera2Config$* { *; }
-keep class * implements androidx.camera.core.CameraXConfig$Provider { *; }

# ML Kit — barcode scanning component is discovered through Firebase's
# ComponentRegistrar reflection. Keep all implementations.
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.mlkit.vision.barcode.internal.** { *; }
-keep class com.google.mlkit.common.** { *; }

# QrData is annotated @Keep (see data/QrData.kt); R8 keeps it via the consumer rule
# bundled with androidx.annotation, so no extra rule is required here.

# Keep crash-friendly stack traces.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
