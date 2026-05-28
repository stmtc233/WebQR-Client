# Preserve generic type info for Retrofit's reflective Call/Response handling.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# QrData is annotated @Keep (see data/QrData.kt); R8 keeps it via the consumer rule
# bundled with androidx.annotation, so no extra rule is required here.

# Keep crash-friendly stack traces.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
