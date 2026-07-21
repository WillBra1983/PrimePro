# ProGuard rules for PrimeProFast - Proteção Básica
# ==================================================

# Manter classes essenciais
-keep class com.seuprojeto.primeprofast.MainActivity {
    public <init>(...);
    public void onCreate(...);
    public native <methods>;
}

# Manter métodos nativos
-keepclasseswithmembernames class * {
    native <methods>;
}

# Remover logs em release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Manter atributos essenciais
-keepattributes InnerClasses,EnclosingMethod,Signature,*Annotation*

# Remover classes não utilizadas
-dontwarn **
-ignorewarnings
