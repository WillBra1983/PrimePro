#include "ppf_c_api.h"

#include "ppf_engine.h"
#include "ppf_standalone_engine.h"

#include <mutex>
#include <string>
#include <vector>

namespace {

std::mutex g_string_mutex;
std::vector<std::string> g_string_pool;

const char* retain_string(std::string value) {
    std::lock_guard<std::mutex> lock(g_string_mutex);
    g_string_pool.push_back(std::move(value));
    return g_string_pool.back().c_str();
}

}  // namespace

extern "C" const char* ppf_calcular_primos(int64_t n) {
    return retain_string(ppf::calcular_primos(n));
}

extern "C" const char* ppf_gerar_primos_grandes(int bits, int quantidade, const char* nome_arquivo, bool salvar_arquivo) {
    std::string path = nome_arquivo ? nome_arquivo : "";
    return retain_string(ppf::gerar_primos_grandes(bits, quantidade, path, salvar_arquivo));
}

extern "C" const char* ppf_super_primos_nativo(int bits, int quantidade, int threads, const char* caminho_arquivo) {
    std::string path = caminho_arquivo ? caminho_arquivo : "";
    return retain_string(ppf::super_primos_nativo(bits, quantidade, threads, path));
}

extern "C" bool ppf_testar_primalidade_gigante(const char* numero, int repeticoes) {
    if (!numero) {
        return false;
    }
    return ppf::testar_primalidade_gigante(numero, repeticoes);
}

extern "C" void ppf_iniciar_geracao_job(int bits, int quantidade, int threads, const char* caminho_arquivo) {
    std::string path = caminho_arquivo ? caminho_arquivo : "";
    ppf::iniciar_geracao_primos_gigantes_job(bits, quantidade, threads, path);
}

extern "C" const char* ppf_obter_status_job(void) {
    return retain_string(ppf::obter_status_geracao_primos_gigantes_job());
}

extern "C" bool ppf_geracao_job_concluido(void) {
    return ppf::geracao_primos_gigantes_job_concluido();
}

extern "C" const char* ppf_obter_resultado_job(void) {
    return retain_string(ppf::obter_resultado_geracao_primos_gigantes_job());
}

extern "C" void ppf_cancelar_job(void) {
    ppf::cancelar_geracao_primos_gigantes_job();
}

extern "C" void ppf_release_string(const char* ptr) {
    if (!ptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(g_string_mutex);
    for (auto it = g_string_pool.begin(); it != g_string_pool.end(); ++it) {
        if (it->c_str() == ptr) {
            g_string_pool.erase(it);
            break;
        }
    }
}
