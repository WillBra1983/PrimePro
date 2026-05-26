#include "ppf_engine.h"

#include <string>
#include <vector>
#include <sstream>
#include <chrono>













#include <iomanip>
#include <cmath>
#include <thread>
#include <mutex>
#include <atomic>
#include <random>
#include <fstream>
#include <iostream>
#include <gmpxx.h>

namespace ppf {

static std::string calcular_primos_segmentado(int64_t n);
static std::string calcular_primos_crivo_simples(int64_t n);

struct PrimoMpzEntry {
    mpz_class valor;
    int thread_id = 0;
};

// Alinhado ao Java (Primos Aleatórios): Math.min(availableProcessors(), 8) na faixa grande.
// Antes: 8193–16384 usava só 2 threads — mesma busca que Java faz com até 8, daí tempos irreais.
static int threads_adaptativas(int num_bits) {
    unsigned hc = std::thread::hardware_concurrency();
    if (hc == 0) {
        hc = 4;
    }
    if (num_bits <= 4096) {
        return (int)std::min<unsigned>(hc, 4u);
    }
    if (num_bits <= 8192) {
        return std::min(3, (int)hc);
    }
    if (num_bits <= 16384) {
        return (int)std::min<unsigned>(hc, 8u);
    }
    if (num_bits <= 32768) {
        return std::min(2, (int)hc);
    }
    return 1;
}

static int escolher_threads_job(int num_bits, int solicitado) {
    if (solicitado <= 0) {
        return threads_adaptativas(num_bits);
    }
    unsigned hc = std::thread::hardware_concurrency();
    if (hc == 0) {
        hc = 4;
    }
    int t = std::max(1, solicitado);
    t = std::min(t, (int)hc);
    int cap = threads_adaptativas(num_bits);
    if (t > cap + 2) {
        t = cap;
    }
    return std::max(1, t);
}

struct PrimeGenerationJob {
    std::atomic<bool> running{false};
    std::atomic<bool> completed{true};
    std::atomic<bool> stop_requested{false};
    std::atomic<int> found_count{0};
    std::atomic<int> attempts{0};
    int bits = 0;
    int quantity = 0;
    int threads = 0;
    std::string error;
    std::string caminho_saida;
    std::vector<PrimoMpzEntry> primos_mpz;
    std::string ultimo_resumo_preview;
    std::chrono::steady_clock::time_point start_time{};
    std::chrono::steady_clock::time_point end_time{};
    std::mutex mutex;
    std::thread worker;
};

static PrimeGenerationJob g_prime_job;

static std::string resumir_numero(const std::string& numero) {
    if (numero.size() <= 32) {
        return numero;
    }
    return numero.substr(0, 16) + "..." + numero.substr(numero.size() - 16);
}

static std::string resumir_mpz(const mpz_class& z) {
    return resumir_numero(z.get_str());
}

static std::string montar_resultado_job(const PrimeGenerationJob& job) {
    std::stringstream resultado;
    double tempo_total = std::chrono::duration_cast<std::chrono::milliseconds>(
        job.end_time - job.start_time
    ).count() / 1000.0;

    if (!job.error.empty()) {
        resultado << "ERRO NA GERACAO NATIVA\n";
        resultado << "======================\n";
        resultado << "Mensagem: " << job.error << "\n";
        resultado << "Bits: " << job.bits << "\n";
        resultado << "Quantidade solicitada: " << job.quantity << "\n";
        resultado << "Quantidade encontrada: " << job.primos_mpz.size() << "\n";
        return resultado.str();
    }

    resultado << "GERACAO DE PRIMOS GRANDES CONCLUIDA!\n";
    resultado << "==========================================\n";
    resultado << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos\n";
    resultado << "Bits: " << job.bits << "\n";
    resultado << "Quantidade solicitada: " << job.quantity << "\n";
    resultado << "Quantidade encontrada: " << job.primos_mpz.size() << "\n";
    resultado << "Threads utilizadas: " << job.threads << "\n";
    resultado << "Tentativas realizadas: " << job.attempts.load() << "\n";
    if (!job.caminho_saida.empty()) {
        resultado << "Arquivo com primos completos (decimal): " << job.caminho_saida << "\n";
    }
    resultado << "==========================================\n";
    resultado << "PRIMOS (resumo — decimal completo no arquivo acima):\n";
    resultado << "==========================================\n";

    for (size_t i = 0; i < job.primos_mpz.size(); i++) {
        const auto& e = job.primos_mpz[i];
        size_t nd = mpz_sizeinbase(e.valor.get_mpz_t(), 10);
        resultado << "Primo #" << (i + 1) << " (Thread " << e.thread_id << "):\n";
        resultado << resumir_mpz(e.valor) << "\n";
        resultado << "(" << job.bits << " bits, " << nd << " digitos decimais)\n";
        resultado << "------------------------------------------\n";
    }

    return resultado.str();
}

static std::string montar_status_job() {
    std::lock_guard<std::mutex> lock(g_prime_job.mutex);
    std::stringstream status;
    auto referencia_fim = g_prime_job.running.load() ? std::chrono::steady_clock::now() : g_prime_job.end_time;
    double tempo = 0.0;
    if (g_prime_job.start_time.time_since_epoch().count() != 0) {
        tempo = std::chrono::duration_cast<std::chrono::milliseconds>(
            referencia_fim - g_prime_job.start_time
        ).count() / 1000.0;
    }

    status << "⏳ BUSCA NATIVA EM ANDAMENTO\n";
    status << "   • Bits: " << g_prime_job.bits << "\n";
    status << "   • Quantidade solicitada: " << g_prime_job.quantity << "\n";
    status << "   • Primos encontrados: " << g_prime_job.found_count.load() << "\n";
    status << "   • Tentativas realizadas: " << g_prime_job.attempts.load() << "\n";
    status << "   • Threads utilizadas: " << g_prime_job.threads << "\n";
    status << "   • Tempo decorrido: " << std::fixed << std::setprecision(1) << tempo << " s";

    if (!g_prime_job.ultimo_resumo_preview.empty()) {
        status << "\n   • Ultimo primo: " << g_prime_job.ultimo_resumo_preview;
    }

    if (!g_prime_job.error.empty()) {
        status << "\n   • Erro: " << g_prime_job.error;
    }

    return status.str();
}

static void cancelar_job_nativo_se_necessario() {
    g_prime_job.stop_requested.store(true);
    if (g_prime_job.worker.joinable()) {
        g_prime_job.worker.join();
    }
    g_prime_job.stop_requested.store(false);
}

void iniciar_geracao_primos_gigantes_job(
    int num_bits, int num_primos, int num_threads, const std::string& caminho_arquivo) {

    cancelar_job_nativo_se_necessario();

    std::string caminho_out = caminho_arquivo;

    {
        std::lock_guard<std::mutex> lock(g_prime_job.mutex);
        g_prime_job.bits = num_bits;
        g_prime_job.quantity = num_primos;
        g_prime_job.threads = escolher_threads_job(num_bits, num_threads);
        g_prime_job.caminho_saida = std::move(caminho_out);
        g_prime_job.error.clear();
        g_prime_job.primos_mpz.clear();
        g_prime_job.ultimo_resumo_preview.clear();
        g_prime_job.attempts.store(0);
        g_prime_job.found_count.store(0);
        g_prime_job.stop_requested.store(false);
        g_prime_job.running.store(true);
        g_prime_job.completed.store(false);
        g_prime_job.start_time = std::chrono::steady_clock::now();
        g_prime_job.end_time = {};
    }

    g_prime_job.worker = std::thread([]() {
        std::vector<PrimoMpzEntry> resultados_locais;
        std::mutex resultados_mutex;
        std::atomic<bool> parar_threads(false);

        try {
            int bits_locais;
            int quantidade_local;
            int threads_locais;
            std::string caminho_copy;
            {
                std::lock_guard<std::mutex> lock(g_prime_job.mutex);
                bits_locais = g_prime_job.bits;
                quantidade_local = g_prime_job.quantity;
                threads_locais = g_prime_job.threads;
                caminho_copy = g_prime_job.caminho_saida;
            }

            auto thread_func = [&](int thread_id) {
                gmp_randclass rng(gmp_randinit_default);
                std::random_device rd;
                rng.seed(rd() + thread_id + std::chrono::high_resolution_clock::now().time_since_epoch().count());

                while (!parar_threads.load() && !g_prime_job.stop_requested.load()) {
                    // Evita mutex no caminho quente: found_count atualiza junto com cada primo aceito.
                    if (g_prime_job.found_count.load() >= quantidade_local) {
                        parar_threads.store(true);
                        break;
                    }

                    mpz_class candidato = rng.get_z_bits(bits_locais);
                    candidato |= (mpz_class(1) << (bits_locais - 1));
                    candidato |= 1;

                    // Filtros pequenos (rápidos) antes do MR; extras 17–97 reduzem chamadas caras ao MR.
                    static const int primos_filtro[] = {3, 5, 7, 11, 13};
                    bool divisivel = false;
                    for (int primo : primos_filtro) {
                        if (mpz_divisible_ui_p(candidato.get_mpz_t(), primo)) {
                            divisivel = true;
                            break;
                        }
                    }
                    if (divisivel) {
                        g_prime_job.attempts.fetch_add(1);
                        continue;
                    }

                    static const unsigned long filtros_medios[] = {
                        17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97
                    };
                    bool composto_pequeno = false;
                    for (unsigned long p : filtros_medios) {
                        if (mpz_fdiv_ui(candidato.get_mpz_t(), p) == 0) {
                            composto_pequeno = true;
                            break;
                        }
                    }
                    if (composto_pequeno) {
                        g_prime_job.attempts.fetch_add(1);
                        continue;
                    }

                    g_prime_job.attempts.fetch_add(1);
                    // bits > 8192: MR com 2 rodadas (GMP) — menos trabalho por candidato; ainda teste probabilístico.
                    const int reps_mr = (bits_locais > 8192) ? 2 : 3;
                    if (mpz_probab_prime_p(candidato.get_mpz_t(), reps_mr) > 0) {
                        std::lock_guard<std::mutex> lock(resultados_mutex);
                        if ((int)resultados_locais.size() < quantidade_local) {
                            PrimoMpzEntry entrada;
                            entrada.valor = candidato;
                            entrada.thread_id = thread_id;
                            resultados_locais.push_back(std::move(entrada));
                            g_prime_job.found_count.store((int)resultados_locais.size());
                            {
                                std::lock_guard<std::mutex> lock_preview(g_prime_job.mutex);
                                g_prime_job.ultimo_resumo_preview =
                                    resumir_mpz(resultados_locais.back().valor);
                            }
                            if ((int)resultados_locais.size() >= quantidade_local) {
                                parar_threads.store(true);
                            }
                        }
                    }
                }
            };

            std::vector<std::thread> threads;
            threads.reserve((size_t)threads_locais);
            for (int i = 0; i < threads_locais; i++) {
                threads.emplace_back(thread_func, i);
            }

            for (auto& thread : threads) {
                if (thread.joinable()) {
                    thread.join();
                }
            }

            if (!caminho_copy.empty()) {
                std::ofstream arq(caminho_copy.c_str(), std::ios::out | std::ios::trunc);
                if (!arq) {
                    std::lock_guard<std::mutex> lock(g_prime_job.mutex);
                    g_prime_job.error = "Nao foi possivel gravar o arquivo de saida: " + caminho_copy;
                } else {
                    for (const auto& e : resultados_locais) {
                        arq << e.valor.get_str() << "\n";
                    }
                    arq.flush();
                }
            }

            std::lock_guard<std::mutex> lock(g_prime_job.mutex);
            g_prime_job.primos_mpz = std::move(resultados_locais);
        } catch (const std::exception& e) {
            std::lock_guard<std::mutex> lock(g_prime_job.mutex);
            g_prime_job.error = e.what();
        } catch (...) {
            std::lock_guard<std::mutex> lock(g_prime_job.mutex);
            g_prime_job.error = "Falha inesperada na geracao nativa";
        }

        g_prime_job.end_time = std::chrono::steady_clock::now();
        g_prime_job.running.store(false);
        g_prime_job.completed.store(true);
    });
}

std::string obter_status_geracao_primos_gigantes_job() {
    return montar_status_job();
}

bool geracao_primos_gigantes_job_concluido() {
    return g_prime_job.completed.load();
}

std::string obter_resultado_geracao_primos_gigantes_job() {
    if (g_prime_job.worker.joinable() && g_prime_job.completed.load()) {
        g_prime_job.worker.join();
    }
    std::lock_guard<std::mutex> lock(g_prime_job.mutex);
    return montar_resultado_job(g_prime_job);
}

void cancelar_geracao_primos_gigantes_job() {
    cancelar_job_nativo_se_necessario();
    g_prime_job.running.store(false);
    g_prime_job.completed.store(true);
    g_prime_job.end_time = std::chrono::steady_clock::now();
}

// Gera uma lista de primos pequenos até um limite usando Crivo de Eratóstenes
std::vector<int> gerar_primos_pequenos(int limite) {
    std::vector<bool> eh_primo(limite + 1, true);
    std::vector<int> primos;
    eh_primo[0] = eh_primo[1] = false;
    for (int i = 2; i <= limite; ++i) {
        if (eh_primo[i]) {
            primos.push_back(i);
            for (int j = i * 2; j <= limite; j += i) {
                eh_primo[j] = false;
            }
        }
    }
    return primos;
}

// Teste nativo de primalidade para números gigantes.
// Usa GMP para evitar o limite de modPow/BigInteger em alguns celulares.
bool testar_primalidade_gigante(const std::string& numero, int repeticoes) {
    if (numero.empty()) {
        return false;
    }

    bool eh_primo = false;

    try {
        mpz_class candidato;
        if (candidato.set_str(numero.c_str(), 10) != 0) {
            return false;
        }

        if (candidato < 2) {
            eh_primo = false;
        } else if (candidato == 2 || candidato == 3) {
            eh_primo = true;
        } else if (mpz_even_p(candidato.get_mpz_t())) {
            eh_primo = false;
        } else {
            // Pré-filtros rápidos compatíveis com a lógica já usada no projeto.
            static const unsigned long primos_filtro[] = {
                3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47,
                53, 59, 61, 67, 71, 73, 79, 83, 89, 97
            };

            bool divisivel = false;
            for (unsigned long primo : primos_filtro) {
                if (candidato == primo) {
                    divisivel = false;
                    break;
                }
                if (mpz_divisible_ui_p(candidato.get_mpz_t(), primo)) {
                    divisivel = true;
                    break;
                }
            }

            if (!divisivel) {
                int reps = repeticoes > 0 ? repeticoes : 25;
                eh_primo = mpz_probab_prime_p(candidato.get_mpz_t(), reps) > 0;
            }
        }
    } catch (...) {
        eh_primo = false;
    }

    return eh_primo;
}

std::string gerar_primos_grandes(int num_bits, int num_primos, const std::string& nome_arquivo_str, bool salvar_arquivo) {
    auto start_time = std::chrono::high_resolution_clock::now();

    const int primos_necessarios = num_primos;
    std::vector<PrimoMpzEntry> primos_encontrados;
    std::mutex mtx;
    bool parar_threads = false;
    const int threads_totais = threads_adaptativas(num_bits);

    auto thread_func = [&](int thread_id) {
        gmp_randclass rng(gmp_randinit_default);
        std::random_device rd;
        rng.seed(rd() + thread_id + std::chrono::high_resolution_clock::now().time_since_epoch().count());

        while (!parar_threads) {
            {
                std::lock_guard<std::mutex> lock(mtx);
                if ((int)primos_encontrados.size() >= primos_necessarios) {
                    parar_threads = true;
                    break;
                }
            }

            mpz_class candidato = rng.get_z_bits(num_bits);
            candidato |= (mpz_class(1) << (num_bits - 1));
            candidato |= 1;

            static const int primos_filtro[] = {3, 5, 7, 11, 13};
            static const int num_primos_filtro = 5;
            bool divisivel = false;
            for (int i = 0; i < num_primos_filtro; ++i) {
                if (mpz_divisible_ui_p(candidato.get_mpz_t(), primos_filtro[i])) {
                    divisivel = true;
                    break;
                }
            }

            if (divisivel) {
                continue;
            }

            unsigned long mod97 = mpz_fdiv_ui(candidato.get_mpz_t(), 97);
            if (mod97 == 0) {
                continue;
            }

            unsigned long mod101 = mpz_fdiv_ui(candidato.get_mpz_t(), 101);
            if (mod101 == 0) {
                continue;
            }

            unsigned long mod103 = mpz_fdiv_ui(candidato.get_mpz_t(), 103);
            if (mod103 == 0) {
                continue;
            }

            unsigned long mod107 = mpz_fdiv_ui(candidato.get_mpz_t(), 107);
            if (mod107 == 0) {
                continue;
            }

            unsigned long mod109 = mpz_fdiv_ui(candidato.get_mpz_t(), 109);
            if (mod109 == 0) {
                continue;
            }

            unsigned long mod113 = mpz_fdiv_ui(candidato.get_mpz_t(), 113);
            if (mod113 == 0) {
                continue;
            }

            unsigned long mod127 = mpz_fdiv_ui(candidato.get_mpz_t(), 127);
            if (mod127 == 0) {
                continue;
            }

            unsigned long mod131 = mpz_fdiv_ui(candidato.get_mpz_t(), 131);
            if (mod131 == 0) {
                continue;
            }

            unsigned long mod137 = mpz_fdiv_ui(candidato.get_mpz_t(), 137);
            if (mod137 == 0) {
                continue;
            }

            unsigned long mod139 = mpz_fdiv_ui(candidato.get_mpz_t(), 139);
            if (mod139 == 0) {
                continue;
            }

            unsigned long mod149 = mpz_fdiv_ui(candidato.get_mpz_t(), 149);
            if (mod149 == 0) {
                continue;
            }

            unsigned long mod151 = mpz_fdiv_ui(candidato.get_mpz_t(), 151);
            if (mod151 == 0) {
                continue;
            }

            unsigned long mod157 = mpz_fdiv_ui(candidato.get_mpz_t(), 157);
            if (mod157 == 0) {
                continue;
            }

            unsigned long mod163 = mpz_fdiv_ui(candidato.get_mpz_t(), 163);
            if (mod163 == 0) {
                continue;
            }

            unsigned long mod167 = mpz_fdiv_ui(candidato.get_mpz_t(), 167);
            if (mod167 == 0) {
                continue;
            }

            unsigned long mod173 = mpz_fdiv_ui(candidato.get_mpz_t(), 173);
            if (mod173 == 0) {
                continue;
            }

            unsigned long mod179 = mpz_fdiv_ui(candidato.get_mpz_t(), 179);
            if (mod179 == 0) {
                continue;
            }

            unsigned long mod181 = mpz_fdiv_ui(candidato.get_mpz_t(), 181);
            if (mod181 == 0) {
                continue;
            }

            unsigned long mod191 = mpz_fdiv_ui(candidato.get_mpz_t(), 191);
            if (mod191 == 0) {
                continue;
            }

            unsigned long mod193 = mpz_fdiv_ui(candidato.get_mpz_t(), 193);
            if (mod193 == 0) {
                continue;
            }

            unsigned long mod197 = mpz_fdiv_ui(candidato.get_mpz_t(), 197);
            if (mod197 == 0) {
                continue;
            }

            unsigned long mod199 = mpz_fdiv_ui(candidato.get_mpz_t(), 199);
            if (mod199 == 0) {
                continue;
            }

            if (mpz_probab_prime_p(candidato.get_mpz_t(), 5) > 0) {
                std::lock_guard<std::mutex> lock(mtx);
                if ((int)primos_encontrados.size() < primos_necessarios) {
                    PrimoMpzEntry entrada;
                    entrada.valor = std::move(candidato);
                    entrada.thread_id = thread_id;
                    primos_encontrados.push_back(std::move(entrada));
                    if ((int)primos_encontrados.size() >= primos_necessarios) {
                        parar_threads = true;
                    }
                }
            }
        }
    };

    std::vector<std::thread> threads;
    for (int i = 0; i < threads_totais; i++) {
        threads.emplace_back(thread_func, i);
    }
    for (auto& thread : threads) {
        thread.join();
    }

    auto end_time = std::chrono::high_resolution_clock::now();
    double tempo_total = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count() / 1000.0;

    std::string err_arquivo;
    const bool deve_gravar = salvar_arquivo || !nome_arquivo_str.empty();
    if (deve_gravar && !nome_arquivo_str.empty()) {
        std::ofstream arq(nome_arquivo_str.c_str(), std::ios::out | std::ios::trunc);
        if (!arq) {
            err_arquivo = " (aviso: nao foi possivel gravar " + nome_arquivo_str + ")";
        } else {
            for (const auto& e : primos_encontrados) {
                arq << e.valor.get_str() << "\n";
            }
        }
    }

    std::stringstream resultado;
    resultado << "GERACAO DE PRIMOS GRANDES CONCLUIDA!\n";
    resultado << "==========================================\n";
    resultado << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos\n";
    resultado << "Bits: " << num_bits << "\n";
    resultado << "Quantidade solicitada: " << primos_necessarios << "\n";
    resultado << "Quantidade encontrada: " << primos_encontrados.size() << "\n";
    resultado << "Threads utilizadas: " << threads_totais << "\n";
    if (!nome_arquivo_str.empty() && deve_gravar) {
        resultado << "Arquivo (decimal completo): " << nome_arquivo_str << err_arquivo << "\n";
    }
    resultado << "==========================================\n";
    resultado << "PRIMOS (resumo; decimal completo no arquivo quando indicado):\n";
    resultado << "==========================================\n";
    for (size_t i = 0; i < primos_encontrados.size(); i++) {
        const auto& e = primos_encontrados[i];
        size_t nd = mpz_sizeinbase(e.valor.get_mpz_t(), 10);
        resultado << "Primo #" << (i + 1) << " (Thread " << e.thread_id << "):\n";
        resultado << resumir_mpz(e.valor) << "\n";
        resultado << "(" << num_bits << " bits, " << nd << " digitos decimais)\n";
        resultado << "------------------------------------------\n";
    }
    resultado << "==========================================\n";

    return resultado.str();
}

// Crivo de Eratóstenes clássico — muito mais rápido que divisão por primos até √n para N até ~10⁷
static std::string calcular_primos_crivo_simples(int64_t n) {
    auto start_time = std::chrono::high_resolution_clock::now();
    long nn = static_cast<long>(n);
    if (nn < 2) {
        return "Tempo: 0.000 segundos\nTotal de primos encontrados: 0\nNúmeros primos até 0:\n";
    }
    std::vector<bool> comp(static_cast<size_t>(nn) + 1, false);
    comp[0] = comp[1] = true;
    long lim = (long)std::sqrt((double)nn);
    for (long i = 2; i <= lim; i++) {
        if (!comp[static_cast<size_t>(i)]) {
            for (long j = i * i; j <= nn; j += i) {
                comp[static_cast<size_t>(j)] = true;
            }
        }
    }
    long count = 0;
    for (long i = 2; i <= nn; i++) {
        if (!comp[static_cast<size_t>(i)]) count++;
    }
    auto end_time = std::chrono::high_resolution_clock::now();
    double seconds = std::chrono::duration_cast<std::chrono::microseconds>(end_time - start_time).count() / 1e6;
    std::stringstream final_result;
    final_result << std::fixed << std::setprecision(3);
    final_result << "Tempo: " << seconds << " segundos\n";
    final_result << "Total de primos encontrados: " << count << "\n";
    final_result << "Números primos até " << nn << ":\n";
    bool first = true;
    for (long i = 2; i <= nn; i++) {
        if (!comp[static_cast<size_t>(i)]) {
            if (!first) final_result << " ";
            final_result << i;
            first = false;
        }
    }
    return final_result.str();
}

std::string calcular_primos(int64_t n) {
    if (n < 2) {
        return "Tempo: 0.000 segundos\nTotal de primos encontrados: 0\nNúmeros primos até 0:\n";
    }
    // Até 10 milhões: crivo clássico (ex.: 1..1.000.000 em poucos ms)
    if (n <= 10000000L) {
        return calcular_primos_crivo_simples(n);
    }
    // Acima de 100 milhões: crivo segmentado
    if (n > 100000000L) {
        return calcular_primos_segmentado(n);
    }

    auto start_time = std::chrono::high_resolution_clock::now();
    
    // Pré-alocar vetor para evitar realocações (10M < n <= 100M)
    std::vector<long> primos;
    primos.reserve(n / 10);
    
    // Adicionar os primeiros primos manualmente
    if (n >= 2) primos.push_back(2);
    if (n >= 3) primos.push_back(3);
    if (n >= 5) primos.push_back(5);
    
    // Loop principal otimizado para números grandes
    for (long x = 7; x <= n; x += 2) {
        // Restrições modulares: números terminando em {1, 3, 7, 9}
        long mod10 = x % 10;
        if (mod10 != 1 && mod10 != 3 && mod10 != 7 && mod10 != 9) continue;
        
        // Verificar primalidade usando os primos já encontrados
        bool ehPrimo = true;
        long limite = sqrt(x);
        
        for (size_t i = 0; i < primos.size() && primos[i] <= limite; i++) {
            if (x % primos[i] == 0) {
                ehPrimo = false;
                break;
            }
        }
        
        if (ehPrimo) {
            primos.push_back(x);
        }
    }
    
    // Calcular tempo de execução em segundos
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
    double seconds = duration.count() / 1000.0;
    
    // Formatar resultado com tempo no início
    std::stringstream final_result;
    final_result << "Tempo: " << std::fixed << std::setprecision(3) << seconds << " segundos\n";
    final_result << "Total de primos encontrados: " << primos.size() << "\n";
    final_result << "Números primos até " << static_cast<long>(n) << ":\n";
    
    // Mostrar primos em formato corrido
    for (size_t i = 0; i < primos.size(); i++) {
        final_result << primos[i];
        if (i < primos.size() - 1) {
            final_result << " ";
        }
    }
    
    return final_result.str();
}

static std::string calcular_primos_segmentado(int64_t n) {
    auto start_time = std::chrono::high_resolution_clock::now();
    
    // Para números muito grandes, usar Crivo segmentado
    const long segment_size = 10000000; // 10 milhões por segmento
    std::vector<long> primos;
    
    // Primeiro, gerar primos pequenos até sqrt(n)
    long sqrt_n = sqrt(static_cast<long>(n));
    std::vector<bool> is_prime_small(sqrt_n + 1, true);
    is_prime_small[0] = is_prime_small[1] = false;
    
    for (long i = 2; i * i <= sqrt_n; i++) {
        if (is_prime_small[i]) {
            for (long j = i * i; j <= sqrt_n; j += i) {
                is_prime_small[j] = false;
            }
        }
    }
    
    // Coletar primos pequenos
    std::vector<long> primos_pequenos;
    for (long i = 2; i <= sqrt_n; i++) {
        if (is_prime_small[i]) {
            primos_pequenos.push_back(i);
        }
    }
    
    // Adicionar primos pequenos ao resultado
    for (long primo : primos_pequenos) {
        if (primo <= static_cast<long>(n)) {
            primos.push_back(primo);
        }
    }
    
    // Processar segmentos para números grandes
    for (long segment_start = sqrt_n + 1; segment_start <= static_cast<long>(n); segment_start += segment_size) {
        long segment_end = std::min(segment_start + segment_size - 1, static_cast<long>(n));
        
        // Criar segmento
        std::vector<bool> is_prime_segment(segment_size, true);
        
        // Marcar múltiplos dos primos pequenos no segmento
        for (long primo : primos_pequenos) {
            long first_multiple = ((segment_start + primo - 1) / primo) * primo;
            if (first_multiple % 2 == 0) first_multiple += primo;
            
            for (long multiple = first_multiple; multiple <= segment_end; multiple += 2 * primo) {
                if (multiple >= segment_start) {
                    is_prime_segment[multiple - segment_start] = false;
                }
            }
        }
        
        // Adicionar primos do segmento ao resultado
        for (long i = 0; i < segment_size && segment_start + i <= segment_end; i++) {
            if (is_prime_segment[i] && (segment_start + i) % 2 == 1) {
                // Verificação adicional para números muito grandes
                bool eh_primo = true;
                long num = segment_start + i;
                
                // Verificar divisibilidade pelos primos pequenos
                for (long primo : primos_pequenos) {
                    if (primo * primo > num) break;
                    if (num % primo == 0) {
                        eh_primo = false;
                        break;
                    }
                }
                
                if (eh_primo) {
                    primos.push_back(num);
                }
            }
        }
    }
    
    // Calcular tempo de execução
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
    double seconds = duration.count() / 1000.0;
    
    // Formatar resultado
    std::stringstream final_result;
    final_result << "Tempo: " << std::fixed << std::setprecision(3) << seconds << " segundos\n";
    final_result << "Total de primos encontrados: " << primos.size() << "\n";
    final_result << "Números primos até " << static_cast<long>(n) << ":\n";
    
    // Mostrar primos em formato corrido
    for (size_t i = 0; i < primos.size(); i++) {
        final_result << primos[i];
        if (i < primos.size() - 1) {
            final_result << " ";
        }
    }
    
    return final_result.str();
}

}  // namespace ppf