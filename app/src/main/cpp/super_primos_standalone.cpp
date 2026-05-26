#include <jni.h>

#include "ppf_standalone_engine.h"

#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_superPrimosNativo(
    JNIEnv* env, jobject, jint bits, jint quantidade, jint threads, jstring caminho_arquivo) {
  std::string caminho_str;
  if (caminho_arquivo != nullptr) {
    const char* caminho_c = env->GetStringUTFChars(caminho_arquivo, 0);
    if (caminho_c != nullptr) {
      caminho_str.assign(caminho_c);
      env->ReleaseStringUTFChars(caminho_arquivo, caminho_c);
    }
  }
  std::string resultado = ppf::super_primos_nativo(bits, quantidade, threads, caminho_str);
  return env->NewStringUTF(resultado.c_str());
}
