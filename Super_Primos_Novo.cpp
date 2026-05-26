#include <jni.h>
#include <gmpxx.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <fstream>
#include <iomanip>
#include <mutex>
#include <random>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

// ============================================================
// ESTRUTURAS
// ============================================================

struct PrimoMpzEntry {
    mpz_class valor;
    int thread_id = -1;
    std::string modelo;
    mpz_class offset = 0;
    bool exato = false;
};

struct PrimeJobState {
    std::mutex mutex;
    std::thread worker;

    std::atomic<bool> running{false};
    std::atomic<bool> completed{false};
    std::atomic<bool> stop_requested{false};
    std::atomic<long long> attempts{0};
    std::atomic<int> found_count{0};

    int bits = 0;
    int quantity = 0;
    int threads = 0;

    std::string caminho_saida;
    std::string error;
    std::string ultimo_resumo_preview;

    std::vector<PrimoMpzEntry> primos_mpz;

    std::chrono::steady_clock::time_point start_time{};
    std::chrono::steady_clock::time_point end_time{};
};

static PrimeJobState g_prime_job;

// ============================================================
// UTILITÁRIOS
// ============================================================

static std::string mpz_to_string(const mpz_class& x) {
    return x.get_str();
}

static std::string resumir_mpz(const mpz_class& x) {
    std::string s = x.get_str();
    if (s.size() <= 24) return s;
    return s.substr(0, 12) + "..." + s.substr(s.size() - 12);
}

static int threads_adaptativas(int bits) {
    unsigned hw = std::thread::hardware_concurrency();
    int max_threads = hw == 0 ? 4 : static_cast<int>(hw);

    if (bits <= 2048) return std::max(1, std::min(4, max_threads));
    if (bits <= 4096) return std::max(1, std::min(6, max_threads));
    if (bits <= 8192) return std::max(1, std::min(8, max_threads));
    if (bits <= 16384) return std::max(1, std::min(10, max_threads));
    return std::max(1, std::min(12, max_threads));
}

static int escolher_threads_job(int bits, int pedido) {
    int auto_threads = threads_adaptativas(bits);
    if (pedido <= 0) return auto_threads;
    return std::max(1, std::min(pedido, auto_threads));
}

static void cancelar_job_nativo_se_necessario() {
    g_prime_job.stop_requested.store(true);
    if (g_prime_job.worker.joinable()) {
        g_prime_job.worker.join();
    }
    g_prime_job.stop_requested.store(false);
}

static std::string montar_status_job() {
    std::lock_guard<std::mutex> lock(g_prime_job.mutex);

    std::ostringstream oss;
    oss << "running=" << (g_prime_job.running.load() ? "true" : "false")
        << " | completed=" << (g_prime_job.completed.load() ? "true" : "false")
        << " | bits=" << g_prime_job.bits
        << " | quantity=" << g_prime_job.quantity
        << " | threads=" << g_prime_job.threads
        << " | attempts=" << g_prime_job.attempts.load()
        << " | found=" << g_prime_job.found_count.load();

    if (!g_prime_job.ultimo_resumo_preview.empty()) {
        oss << " | preview=" << g_prime_job.ultimo_resumo_preview;
    }

    if (!g_prime_job.error.empty()) {
        oss << " | error=" << g_prime_job.error;
    }

    return oss.str();
}

static std::string montar_resultado_job(const PrimeJobState& job) {
    std::ostringstream oss;
    oss << "RESULTADO GERACAO PRIMOS GIGANTES\n";
    oss << "bits=" << job.bits << "\n";
    oss << "quantity=" << job.quantity << "\n";
    oss << "threads=" << job.threads << "\n";
    oss << "attempts=" << job.attempts.load() << "\n";
    oss << "found=" << job.found_count.load() << "\n";

    if (!job.error.empty()) {
        oss << "error=" << job.error << "\n";
    }

    if (job.start_time.time_since_epoch().count() != 0 &&
        job.end_time.time_since_epoch().count() != 0) {
        auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(job.end_time - job.start_time).count();
        oss << "elapsed_ms=" << ms << "\n";
    }

    oss << "\nPRIMOS:\n";
    for (size_t i = 0; i < job.primos_mpz.size(); ++i) {
        const auto& e = job.primos_mpz[i];
        oss << "[" << i + 1 << "] "
            << e.valor.get_str()
            << " | thread=" << e.thread_id
            << " | modelo=" << e.modelo
            << " | offset=" << e.offset.get_str()
            << " | exato=" << (e.exato ? "SIM" : "NAO")
            << "\n";
    }
    return oss.str();
}

static bool salvar_primos_txt(const std::string& caminho, const std::vector<PrimoMpzEntry>& primos) {
    if (caminho.empty()) return true;

    std::ofstream arq(caminho.c_str(), std::ios::out | std::ios::trunc);
    if (!arq) return false;

    for (const auto& e : primos) {
        arq << e.valor.get_str()
            << " | modelo=" << e.modelo
            << " | offset=" << e.offset.get_str()
            << " | exato=" << (e.exato ? "SIM" : "NAO")
            << "\n";
    }
    arq.flush();
    return true;
}

// ============================================================
// PRIMALIDADE E FILTROS
// ============================================================

static const unsigned long SMALL_FILTERS[] = {
    3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47,
    53, 59, 61, 67, 71, 73, 79, 83, 89, 97
};

static bool passa_filtros_rapidos_mpz(const mpz_class& n) {
    if (n < 2) return false;
    if (n == 2 || n == 3 || n == 5) return true;
    if (mpz_even_p(n.get_mpz_t())) return false;

    for (unsigned long p : SMALL_FILTERS) {
        if (n == p) return true;
        if (mpz_divisible_ui_p(n.get_mpz_t(), p)) return false;
    }

    unsigned long mod30 = mpz_fdiv_ui(n.get_mpz_t(), 30);
    switch (mod30) {
        case 1: case 7: case 11: case 13:
        case 17: case 19: case 23: case 29:
            return true;
        default:
            return false;
    }
}

static bool eh_primo_gmp(const mpz_class& n, int repeticoes) {
    if (n < 2) return false;
    return mpz_probab_prime_p(n.get_mpz_t(), repeticoes) > 0;
}

// ============================================================
// DENSIDADE
// ============================================================

struct DensityInfoCpp {
    long floor_ln;
    long round_ln;
    long ceil_ln;
};

static DensityInfoCpp density_info_mpz(const mpz_class& n) {
    // Aproxima ln(n) via bit_length * ln(2), que é estável e leve.
    int bits = static_cast<int>(mpz_sizeinbase(n.get_mpz_t(), 2));
    double ln_approx = bits * std::log(2.0);

    long fl = std::max(1L, static_cast<long>(std::floor(ln_approx)));
    long rd = std::max(1L, static_cast<long>(std::llround(ln_approx)));
    long cl = std::max(1L, static_cast<long>(std::ceil(ln_approx)));

    return {fl, rd, cl};
}

static bool valid_offset_for_prime(const mpz_class& base, long offset) {
    mpz_class cand = base + offset;
    return !mpz_even_p(cand.get_mpz_t());
}

static void add_offset_if_valid(std::vector<long>& offsets, const mpz_class& base, long k) {
    if (k <= 0) return;
    if (valid_offset_for_prime(base, +k)) offsets.push_back(+k);
    if (valid_offset_for_prime(base, -k)) offsets.push_back(-k);
}

static std::vector<long> build_density_offsets(const mpz_class& base, const DensityInfoCpp& d) {
    std::vector<long> offsets;

    // camada 1: curtos
    add_offset_if_valid(offsets, base, 1);
    add_offset_if_valid(offsets, base, 2);

    std::vector<long> anchors = {d.floor_ln, d.round_ln, d.ceil_ln};
    std::sort(anchors.begin(), anchors.end());
    anchors.erase(std::unique(anchors.begin(), anchors.end()), anchors.end());

    // camada 2: âncoras
    for (long a : anchors) {
        add_offset_if_valid(offsets, base, a);
    }

    // camada 3: vizinhança
    for (long a : anchors) {
        add_offset_if_valid(offsets, base, a - 1);
        add_offset_if_valid(offsets, base, a + 1);
        add_offset_if_valid(offsets, base, a - 2);
        add_offset_if_valid(offsets, base, a + 2);
    }

    // camada 4: preenchimento até ceil(ln)
    for (long k = 1; k <= d.ceil_ln; ++k) {
        add_offset_if_valid(offsets, base, k);
    }

    std::sort(offsets.begin(), offsets.end(), [](long a, long b) {
        long aa = std::labs(a), bb = std::labs(b);
        if (aa != bb) return aa < bb;
        return a > b;
    });
    offsets.erase(std::unique(offsets.begin(), offsets.end()), offsets.end());
    return offsets;
}

static bool buscar_primo_proximo(
    const mpz_class& base,
    mpz_class& primo_encontrado,
    mpz_class& offset_encontrado,
    int reps_mr
) {
    DensityInfoCpp d = density_info_mpz(base);
    std::vector<long> offsets = build_density_offsets(base, d);

    for (long k : offsets) {
        mpz_class cand = base + k;
        if (cand < 2) continue;
        if (!passa_filtros_rapidos_mpz(cand)) continue;
        if (eh_primo_gmp(cand, reps_mr)) {
            primo_encontrado = cand;
            offset_encontrado = k;
            return true;
        }
    }
    return false;
}

// ============================================================
// BASES: ÍMPARES TERMINADOS EM 1
// ============================================================

static inline void forcar_impar_final_1(mpz_class& x) {
    if (x < 11) x = 11;
    if (mpz_even_p(x.get_mpz_t())) x += 1;

    unsigned long mod10 = mpz_fdiv_ui(x.get_mpz_t(), 10);
    while (mod10 != 1) {
        x += 2;
        mod10 = mpz_fdiv_ui(x.get_mpz_t(), 10);
    }
}

static mpz_class gerar_impar_final_1_bits(gmp_randclass& rng, int bits) {
    if (bits < 4) bits = 4;

    while (true) {
        mpz_class x = rng.get_z_bits(bits);
        x |= (mpz_class(1) << (bits - 1)); // garante bit alto
        x |= 1;                             // garante ímpar
        forcar_impar_final_1(x);

        if (static_cast<int>(mpz_sizeinbase(x.get_mpz_t(), 2)) == bits) {
            return x;
        }
    }
}

// ============================================================
// MODELOS DO MÉTODO
// ============================================================

static void construir_modelos(
    const mpz_class& p,
    const mpz_class& q,
    std::vector<std::pair<std::string, mpz_class>>& modelos
) {
    mpz_class pq = p * q;
    modelos.clear();

    modelos.push_back({"P1 pq-2", pq - 2});
    modelos.push_back({"P2 pq+2", pq + 2});

    modelos.push_back({"M1 pq+p+q-4", pq + p + q - 4});
    modelos.push_back({"M2 pq+p+q",   pq + p + q});
    modelos.push_back({"M3 pq+p+q+4", pq + p + q + 4});

    modelos.push_back({"F1 pq+p+3q-4", pq + p + 3 * q - 4});
    modelos.push_back({"F2 pq+p+3q",   pq + p + 3 * q});
    modelos.push_back({"F3 pq+p+3q+4", pq + p + 3 * q + 4});
}

static bool encontrar_primo_no_modelo(
    int bits_alvo,
    gmp_randclass& rng,
    int reps_mr,
    PrimoMpzEntry& saida
) {
    int bits_p = bits_alvo / 2;
    int bits_q = bits_alvo - bits_p;

    mpz_class p = gerar_impar_final_1_bits(rng, bits_p);
    mpz_class q = gerar_impar_final_1_bits(rng, bits_q);

    std::vector<std::pair<std::string, mpz_class>> modelos;
    construir_modelos(p, q, modelos);

    // prioriza modelos mais próximos do alvo em bits
    std::sort(modelos.begin(), modelos.end(),
        [bits_alvo](const auto& a, const auto& b) {
            int ba = static_cast<int>(mpz_sizeinbase(a.second.get_mpz_t(), 2));
            int bb = static_cast<int>(mpz_sizeinbase(b.second.get_mpz_t(), 2));
            int da = std::abs(ba - bits_alvo);
            int db = std::abs(bb - bits_alvo);
            if (da != db) return da < db;
            return a.first < b.first;
        }
    );

    for (const auto& item : modelos) {
        const std::string& nome = item.first;
        const mpz_class& base = item.second;

        int bits_base = static_cast<int>(mpz_sizeinbase(base.get_mpz_t(), 2));
        if (bits_base != bits_alvo) {
            continue;
        }

        if (passa_filtros_rapidos_mpz(base) && eh_primo_gmp(base, reps_mr)) {
            saida.valor = base;
            saida.modelo = nome;
            saida.offset = 0;
            saida.exato = true;
            return true;
        }

        mpz_class primo_final;
        mpz_class offset_final = 0;
        if (buscar_primo_proximo(base, primo_final, offset_final, reps_mr)) {
            if (static_cast<int>(mpz_sizeinbase(primo_final.get_mpz_t(), 2)) == bits_alvo) {
                saida.valor = primo_final;
                saida.modelo = nome;
                saida.offset = offset_final;
                saida.exato = false;
                return true;
            }
        }
    }

    return false;
}

// ============================================================
// TESTE NATIVO DE PRIMALIDADE
// ============================================================

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

    bool eh_primo = false;

    try {
        mpz_class candidato;
        if (candidato.set_str(numero_c, 10) != 0) {
            env->ReleaseStringUTFChars(numero, numero_c);
            return JNI_FALSE;
        }

        int reps = repeticoes > 0 ? repeticoes : 25;
        if (passa_filtros_rapidos_mpz(candidato)) {
            eh_primo = eh_primo_gmp(candidato, reps);
        } else {
            eh_primo = false;
        }
    } catch (...) {
        eh_primo = false;
    }

    env->ReleaseStringUTFChars(numero, numero_c);
    return eh_primo ? JNI_TRUE : JNI_FALSE;
}

// ============================================================
// GERAÇÃO SÍNCRONA
// ============================================================

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_gerarPrimosGrandes(
    JNIEnv* env,
    jobject /* this */,
    jint num_bits,
    jint num_primos,
    jstring nome_arquivo,
    jboolean salvar_arquivo
) {
    auto start_time = std::chrono::high_resolution_clock::now();

    const char* nome_arquivo_c = env->GetStringUTFChars(nome_arquivo, 0);
    std::string nome_arquivo_str(nome_arquivo_c ? nome_arquivo_c : "");
    env->ReleaseStringUTFChars(nome_arquivo, nome_arquivo_c);

    const int primos_necessarios = num_primos > 0 ? num_primos : 1;
    std::vector<PrimoMpzEntry> primos_encontrados;
    std::mutex mtx;
    std::atomic<bool> parar_threads(false);
    const int threads_totais = threads_adaptativas(num_bits);

    auto thread_func = [&](int thread_id) {
        gmp_randclass rng(gmp_randinit_default);
        std::random_device rd;
        rng.seed(rd() + thread_id + static_cast<unsigned long>(
            std::chrono::high_resolution_clock::now().time_since_epoch().count()));

        const int reps_mr = (num_bits > 8192) ? 2 : 3;

        while (!parar_threads.load()) {
            {
                std::lock_guard<std::mutex> lock(mtx);
                if ((int)primos_encontrados.size() >= primos_necessarios) {
                    parar_threads.store(true);
                    break;
                }
            }

            PrimoMpzEntry entrada;
            if (!encontrar_primo_no_modelo(num_bits, rng, reps_mr, entrada)) {
                continue;
            }

            entrada.thread_id = thread_id;

            {
                std::lock_guard<std::mutex> lock(mtx);
                if ((int)primos_encontrados.size() < primos_necessarios) {
                    primos_encontrados.push_back(std::move(entrada));
                    if ((int)primos_encontrados.size() >= primos_necessarios) {
                        parar_threads.store(true);
                    }
                }
            }
        }
    };

    std::vector<std::thread> threads;
    threads.reserve((size_t)threads_totais);
    for (int i = 0; i < threads_totais; ++i) {
        threads.emplace_back(thread_func, i);
    }

    for (auto& thread : threads) {
        if (thread.joinable()) {
            thread.join();
        }
    }

    std::ostringstream resultado;
    auto end_time = std::chrono::high_resolution_clock::now();
    auto dur_ms = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();

    resultado << "GERACAO DE PRIMOS GRANDES (MODO MODELO)\n";
    resultado << "bits=" << num_bits << "\n";
    resultado << "threads=" << threads_totais << "\n";
    resultado << "quantidade=" << primos_necessarios << "\n";
    resultado << "tempo_ms=" << dur_ms << "\n\n";

    for (size_t i = 0; i < primos_encontrados.size(); ++i) {
        const auto& e = primos_encontrados[i];
        resultado << "[" << i + 1 << "] "
                  << e.valor.get_str()
                  << " | thread=" << e.thread_id
                  << " | modelo=" << e.modelo
                  << " | offset=" << e.offset.get_str()
                  << " | exato=" << (e.exato ? "SIM" : "NAO")
                  << "\n";
    }

    if (salvar_arquivo == JNI_TRUE && !nome_arquivo_str.empty()) {
        salvar_primos_txt(nome_arquivo_str, primos_encontrados);
    }

    std::string resultado_str = resultado.str();
    return env->NewStringUTF(resultado_str.c_str());
}

// ============================================================
// JOB ASSÍNCRONO
// ============================================================

extern "C" JNIEXPORT void JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_iniciarGeracaoPrimosGigantesJob(
    JNIEnv* env,
    jobject /* this */,
    jint num_bits,
    jint num_primos,
    jint num_threads,
    jstring caminho_arquivo
) {
    cancelar_job_nativo_se_necessario();

    std::string caminho_out;
    if (caminho_arquivo != nullptr) {
        const char* p = env->GetStringUTFChars(caminho_arquivo, nullptr);
        if (p != nullptr) {
            caminho_out.assign(p);
            env->ReleaseStringUTFChars(caminho_arquivo, p);
        }
    }

    {
        std::lock_guard<std::mutex> lock(g_prime_job.mutex);
        g_prime_job.bits = num_bits;
        g_prime_job.quantity = num_primos > 0 ? num_primos : 1;
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
                rng.seed(rd() + thread_id + static_cast<unsigned long>(
                    std::chrono::high_resolution_clock::now().time_since_epoch().count()));

                const int reps_mr = (bits_locais > 8192) ? 2 : 3;

                while (!parar_threads.load() && !g_prime_job.stop_requested.load()) {
                    if (g_prime_job.found_count.load() >= quantidade_local) {
                        parar_threads.store(true);
                        break;
                    }

                    g_prime_job.attempts.fetch_add(1);

                    PrimoMpzEntry entrada;
                    if (!encontrar_primo_no_modelo(bits_locais, rng, reps_mr, entrada)) {
                        continue;
                    }

                    entrada.thread_id = thread_id;

                    std::lock_guard<std::mutex> lock(resultados_mutex);
                    if ((int)resultados_locais.size() < quantidade_local) {
                        resultados_locais.push_back(std::move(entrada));
                        g_prime_job.found_count.store((int)resultados_locais.size());

                        {
                            std::lock_guard<std::mutex> lock_preview(g_prime_job.mutex);
                            const auto& ultimo = resultados_locais.back();
                            g_prime_job.ultimo_resumo_preview =
                                resumir_mpz(ultimo.valor) +
                                " | modelo=" + ultimo.modelo +
                                " | off=" + ultimo.offset.get_str() +
                                " | exato=" + std::string(ultimo.exato ? "SIM" : "NAO");
                        }

                        if ((int)resultados_locais.size() >= quantidade_local) {
                            parar_threads.store(true);
                        }
                    }
                }
            };

            std::vector<std::thread> threads;
            threads.reserve((size_t)threads_locais);
            for (int i = 0; i < threads_locais; ++i) {
                threads.emplace_back(thread_func, i);
            }

            for (auto& thread : threads) {
                if (thread.joinable()) {
                    thread.join();
                }
            }

            if (!caminho_copy.empty()) {
                if (!salvar_primos_txt(caminho_copy, resultados_locais)) {
                    std::lock_guard<std::mutex> lock(g_prime_job.mutex);
                    g_prime_job.error = "Nao foi possivel gravar o arquivo de saida: " + caminho_copy;
                }
            }

            {
                std::lock_guard<std::mutex> lock(g_prime_job.mutex);
                g_prime_job.primos_mpz = std::move(resultados_locais);
            }
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_obterStatusGeracaoPrimosGigantesJob(
    JNIEnv* env,
    jobject /* this */
) {
    std::string status = montar_status_job();
    return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_geracaoPrimosGigantesJobConcluido(
    JNIEnv* /* env */,
    jobject /* this */
) {
    return g_prime_job.completed.load() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_obterResultadoGeracaoPrimosGigantesJob(
    JNIEnv* env,
    jobject /* this */
) {
    if (g_prime_job.worker.joinable() && g_prime_job.completed.load()) {
        g_prime_job.worker.join();
    }

    std::lock_guard<std::mutex> lock(g_prime_job.mutex);
    std::string resultado = montar_resultado_job(g_prime_job);
    return env->NewStringUTF(resultado.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_cancelarGeracaoPrimosGigantesJob(
    JNIEnv* /* env */,
    jobject /* this */
) {
    cancelar_job_nativo_se_necessario();
    g_prime_job.running.store(false);
    g_prime_job.completed.store(true);
    g_prime_job.end_time = std::chrono::steady_clock::now();
}

// ============================================================
// JNI_OnLoad
// ============================================================

jint JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    (void)vm;
    return JNI_VERSION_1_6;
}