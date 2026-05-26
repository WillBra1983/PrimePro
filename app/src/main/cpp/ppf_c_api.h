#ifndef PPF_C_API_H
#define PPF_C_API_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

const char* ppf_calcular_primos(int64_t n);
const char* ppf_gerar_primos_grandes(int bits, int quantidade, const char* nome_arquivo, bool salvar_arquivo);
const char* ppf_super_primos_nativo(int bits, int quantidade, int threads, const char* caminho_arquivo);
bool ppf_testar_primalidade_gigante(const char* numero, int repeticoes);

void ppf_iniciar_geracao_job(int bits, int quantidade, int threads, const char* caminho_arquivo);
const char* ppf_obter_status_job(void);
bool ppf_geracao_job_concluido(void);
const char* ppf_obter_resultado_job(void);
void ppf_cancelar_job(void);

void ppf_release_string(const char* ptr);

#ifdef __cplusplus
}
#endif

#endif
