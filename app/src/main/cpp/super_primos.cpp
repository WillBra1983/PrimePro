#include <jni.h>

#include "ppf_engine.h"

#include <string>

extern "C" JNIEXPORT void JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_iniciarGeracaoPrimosGigantesJob(
    JNIEnv* env, jobject /* this */, jint num_bits, jint num_primos, jint num_threads, jstring caminho_arquivo) {
    std::string caminho_out;
    if (caminho_arquivo != nullptr) {
        const char* p = env->GetStringUTFChars(caminho_arquivo, nullptr);
        if (p != nullptr) {
            caminho_out.assign(p);
            env->ReleaseStringUTFChars(caminho_arquivo, p);
        }
    }
    ppf::iniciar_geracao_primos_gigantes_job(num_bits, num_primos, num_threads, caminho_out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_obterStatusGeracaoPrimosGigantesJob(
    JNIEnv* env, jobject /* this */) {
  std::string status = ppf::obter_status_geracao_primos_gigantes_job();
  return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_geracaoPrimosGigantesJobConcluido(
    JNIEnv* /* env */, jobject /* this */) {
  return ppf::geracao_primos_gigantes_job_concluido() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_obterResultadoGeracaoPrimosGigantesJob(
    JNIEnv* env, jobject /* this */) {
  std::string resultado = ppf::obter_resultado_geracao_primos_gigantes_job();
  return env->NewStringUTF(resultado.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_cancelarGeracaoPrimosGigantesJob(
    JNIEnv* /* env */, jobject /* this */) {
  ppf::cancelar_geracao_primos_gigantes_job();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_testarPrimalidadeGiganteNativo(
    JNIEnv* env, jobject /* this */, jstring numero, jint repeticoes) {
  if (numero == nullptr) {
    return JNI_FALSE;
  }
  const char* numero_c = env->GetStringUTFChars(numero, 0);
  if (numero_c == nullptr) {
    return JNI_FALSE;
  }
  bool eh_primo = ppf::testar_primalidade_gigante(numero_c, repeticoes);
  env->ReleaseStringUTFChars(numero, numero_c);
  return eh_primo ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_gerarPrimosGrandes(
    JNIEnv* env, jobject /* this */, jint num_bits, jint num_primos, jstring nome_arquivo, jboolean salvar_arquivo) {
  std::string nome_arquivo_str;
  if (nome_arquivo != nullptr) {
    const char* nome_arquivo_c = env->GetStringUTFChars(nome_arquivo, 0);
    if (nome_arquivo_c != nullptr) {
      nome_arquivo_str.assign(nome_arquivo_c);
      env->ReleaseStringUTFChars(nome_arquivo, nome_arquivo_c);
    }
  }
  std::string resultado = ppf::gerar_primos_grandes(
      num_bits, num_primos, nome_arquivo_str, salvar_arquivo != JNI_FALSE);
  return env->NewStringUTF(resultado.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_calcularPrimos(JNIEnv* env, jobject /* this */, jlong n) {
  std::string resultado = ppf::calcular_primos(n);
  return env->NewStringUTF(resultado.c_str());
}
