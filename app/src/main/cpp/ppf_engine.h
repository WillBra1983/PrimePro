#ifndef PPF_ENGINE_H
#define PPF_ENGINE_H

#include <cstdint>
#include <string>

namespace ppf {

std::string calcular_primos(int64_t n);
std::string gerar_primos_grandes(int bits, int quantidade, const std::string& nome_arquivo, bool salvar_arquivo);
bool testar_primalidade_gigante(const std::string& numero, int repeticoes);

void iniciar_geracao_primos_gigantes_job(int bits, int quantidade, int threads, const std::string& caminho_arquivo);
std::string obter_status_geracao_primos_gigantes_job();
bool geracao_primos_gigantes_job_concluido();
std::string obter_resultado_geracao_primos_gigantes_job();
void cancelar_geracao_primos_gigantes_job();

}  // namespace ppf

#endif
