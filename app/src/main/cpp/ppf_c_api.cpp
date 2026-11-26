#include "ppf_c_api.h"

#include "ppf_engine.h"
#include "ppf_prime_cards.h"
#include "ppf_standalone_engine.h"

#include <mutex>
#include <list>
#include <string>

namespace {

std::mutex g_string_mutex;
std::list<std::string> g_string_pool;

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

extern "C" bool ppf_special_primes_supported(int family, int64_t limit, int count) {
    return ppf::special_primes_supported(family, limit, count);
}

extern "C" const char* ppf_special_primes(int family, int64_t limit, int count) {
    try {
        return retain_string(ppf::special_primes_report(family, limit, count));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_short_primes(int bits, int count) {
    try {
        return retain_string(ppf::short_primes_report(bits, count));
    } catch (...) {
        return nullptr;
    }
}

extern "C" int ppf_short_prime_count(int bits) {
    try {
        return ppf::short_prime_count(bits);
    } catch (...) {
        return -1;
    }
}

extern "C" bool ppf_legendre_report_supported(int64_t first, int64_t last) {
    return ppf::legendre_report_supported(first, last);
}

extern "C" const char* ppf_legendre_report_decimal(const char* first, const char* last) {
    try {
        return retain_string(ppf::legendre_report_decimal(first, last));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_legendre_witness_decimal(const char* n) {
    try {
        return retain_string(ppf::legendre_witness_decimal(n));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_legendre_report(int64_t first, int64_t last) {
    try {
        return retain_string(ppf::legendre_report(first, last));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_prime_stats(int64_t limit) {
    try {
        return retain_string(ppf::prime_stats_report(limit));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_prime_approximation(const char* limit) {
    if (!limit) return nullptr;
    try {
        return retain_string(ppf::prime_approximation_report(limit));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_mersenne_report(int exponent, bool list) {
    try {
        return retain_string(ppf::mersenne_report(exponent, list));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_perfect_verify_decimal(const char* number) {
    if (!number) return nullptr;
    try {
        return retain_string(ppf::perfect_verify_decimal(number));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_factor_report(const char* number) {
    try {
        return retain_string(ppf::factor_report(number));
    } catch (...) {
        return nullptr;
    }
}

extern "C" const char* ppf_perfect_report(int mode, int64_t value) {
    try {
        return retain_string(ppf::perfect_report(mode, value));
    } catch (...) {
        return nullptr;
    }
}

extern "C" void ppf_iniciar_geracao_job(int bits, int quantidade, int threads, const char* caminho_arquivo) {
    std::string path = caminho_arquivo ? caminho_arquivo : "";
    ppf::iniciar_geracao_primos_gigantes_job(bits, quantidade, threads, path);
}

extern "C" bool ppf_iniciar_primos_aleatorios_job(int bits, int quantidade, const char* caminho) {
    if (bits <= PPF_SHORT_PRIME_MAX_BITS || bits > 33000 || quantidade < 1 || quantidade > 50 ||
        !caminho || !*caminho) return false;
    try {
        ppf::iniciar_geracao_primos_gigantes_job(bits, quantidade, 0, caminho, bits <= 8192);
        return true;
    } catch (...) {
        return false;
    }
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
