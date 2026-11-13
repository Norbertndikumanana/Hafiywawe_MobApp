# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class rw.hafiyawe.store.**$$serializer { *; }
-keepclassmembers class rw.hafiyawe.store.** {
    *** Companion;
}
-keepclasseswithmembers class rw.hafiyawe.store.** {
    kotlinx.serialization.KSerializer serializer(...);
}
