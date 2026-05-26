#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <random>
#include <chrono>
#include <sstream>
#include <iomanip>
#include <gmpxx.h>
#include <atomic>

// Classe para geração híbrida de primos aleatórios
class GeradorPrimosHibrido {
private:
    int num_threads;
    std::atomic<bool> parar_threads{false};
    
public:
    GeradorPrimosHibrido(int threads = 8) : num_threads(threads) {}
    
    // Estratégia híbrida: Java gera candidatos, C++ usa mpz_nextprime
    std::vector<mpz_class> gerarPrimosHibrido(int quantidade, int digitos, int max_tentativas = 1000000) {
        std::vector<mpz_class> primos;
        std::mutex mtx;
        
        // Calcular bits aproximados para dígitos
        int bits_aproximados = (int)(digitos * 3.32);
        
        // Thread para C++ usando mpz_nextprime
        auto thread_cpp = [&]() {
            gmp_randclass rng(gmp_randinit_default);
            std::random_device rd;
            rng.seed(rd() + std::chrono::high_resolution_clock::now().time_since_epoch().count());
            
            int tentativas = 0;
            
            while (!parar_threads && primos.size() < (size_t)quantidade && tentativas < max_tentativas) {
                // Gerar candidato aleatório
                mpz_class candidato = rng.get_z_bits(bits_aproximados);
                candidato |= (mpz_class(1) << (bits_aproximados - 1));  // MSB = 1
                candidato |= 1;  // LSB = 1 (ímpar)
                
                tentativas++;
                
                // Verificar se tem o número exato de dígitos
                std::string num_str = candidato.get_str();
                if (num_str.length() != digitos) {
                    continue;
                }
                
                // Usar mpz_nextprime para encontrar o próximo primo
                mpz_class proximo_primo = candidato;
                mpz_nextprime(proximo_primo.get_mpz_t(), candidato.get_mpz_t());
                
                // Verificar se o próximo primo ainda tem o mesmo número de dígitos
                std::string proximo_str = proximo_primo.get_str();
                if (proximo_str.length() == digitos) {
                    // Verificar primalidade com alta confiança
                    if (mpz_probab_prime_p(proximo_primo.get_mpz_t(), 10) > 0) {
                        std::lock_guard<std::mutex> lock(mtx);
                        if (primos.size() < (size_t)quantidade && !parar_threads) {
                            primos.push_back(proximo_primo);
                            
                            if (primos.size() >= (size_t)quantidade) {
                                parar_threads = true;
                                break;
                            }
                        }
                    }
                }
            }
        };
        
        // Thread para busca sequencial otimizada
        auto thread_sequencial = [&]() {
            gmp_randclass rng(gmp_randinit_default);
            std::random_device rd;
            rng.seed(rd() + 1000 + std::chrono::high_resolution_clock::now().time_since_epoch().count());
            
            int tentativas = 0;
            
            while (!parar_threads && primos.size() < (size_t)quantidade && tentativas < max_tentativas) {
                // Gerar candidato aleatório
                mpz_class candidato = rng.get_z_bits(bits_aproximados);
                candidato |= (mpz_class(1) << (bits_aproximados - 1));  // MSB = 1
                candidato |= 1;  // LSB = 1 (ímpar)
                
                tentativas++;
                
                // Verificar se tem o número exato de dígitos
                std::string num_str = candidato.get_str();
                if (num_str.length() != digitos) {
                    continue;
                }
                
                // Teste de primalidade direto (mais rápido para candidatos próximos)
                if (mpz_probab_prime_p(candidato.get_mpz_t(), 5) > 0) {
                    std::lock_guard<std::mutex> lock(mtx);
                    if (primos.size() < (size_t)quantidade && !parar_threads) {
                        primos.push_back(candidato);
                        
                        if (primos.size() >= (size_t)quantidade) {
                            parar_threads = true;
                            break;
                        }
                    }
                }
            }
        };
        
        // Thread para busca por incremento inteligente
        auto thread_incremento = [&]() {
            gmp_randclass rng(gmp_randinit_default);
            std::random_device rd;
            rng.seed(rd() + 2000 + std::chrono::high_resolution_clock::now().time_since_epoch().count());
            
            int tentativas = 0;
            
            while (!parar_threads && primos.size() < (size_t)quantidade && tentativas < max_tentativas) {
                // Gerar candidato base
                mpz_class candidato = rng.get_z_bits(bits_aproximados);
                candidato |= (mpz_class(1) << (bits_aproximados - 1));  // MSB = 1
                candidato |= 1;  // LSB = 1 (ímpar)
                
                tentativas++;
                
                // Buscar primo próximo incrementando
                for (int offset = 0; offset < 1000 && !parar_threads; offset += 2) {
                    mpz_class candidato_atual = candidato + offset;
                    
                    // Verificar se ainda tem o número de dígitos
                    std::string num_str = candidato_atual.get_str();
                    if (num_str.length() != digitos) {
                        break;
                    }
                    
                    // Teste rápido de primalidade
                    if (mpz_probab_prime_p(candidato_atual.get_mpz_t(), 3) > 0) {
                        std::lock_guard<std::mutex> lock(mtx);
                        if (primos.size() < (size_t)quantidade && !parar_threads) {
                            primos.push_back(candidato_atual);
                            
                            if (primos.size() >= (size_t)quantidade) {
                                parar_threads = true;
                                break;
                            }
                        }
                        break; // Encontrou um primo, tentar próximo candidato base
                    }
                }
            }
        };
        
        // Criar e executar threads
        std::vector<std::thread> threads;
        
        // Thread principal com mpz_nextprime
        threads.emplace_back(thread_cpp);
        
        // Thread sequencial para candidatos próximos
        threads.emplace_back(thread_sequencial);
        
        // Thread de incremento inteligente
        threads.emplace_back(thread_incremento);
        
        // Aguardar threads terminarem ou timeout
        long timeout_start = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::high_resolution_clock::now().time_since_epoch()).count();
        long timeout = 120000; // 2 minutos
        
        while (primos.size() < (size_t)quantidade && 
               (std::chrono::duration_cast<std::chrono::milliseconds>(
                   std::chrono::high_resolution_clock::now().time_since_epoch()).count() - timeout_start) < timeout) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
        
        // Parar threads
        parar_threads = true;
        for (auto& thread : threads) {
            if (thread.joinable()) {
                thread.join();
            }
        }
        
        return primos;
    }
    
    // Estratégia híbrida por bits (para comparação)
    std::vector<mpz_class> gerarPrimosHibridoPorBits(int quantidade, int bits, int max_tentativas = 1000000) {
        std::vector<mpz_class> primos;
        std::mutex mtx;
        
        // Thread para C++ usando mpz_nextprime
        auto thread_cpp = [&]() {
            gmp_randclass rng(gmp_randinit_default);
            std::random_device rd;
            rng.seed(rd() + std::chrono::high_resolution_clock::now().time_since_epoch().count());
            
            int tentativas = 0;
            
            while (!parar_threads && primos.size() < (size_t)quantidade && tentativas < max_tentativas) {
                // Gerar candidato aleatório
                mpz_class candidato = rng.get_z_bits(bits);
                candidato |= (mpz_class(1) << (bits - 1));  // MSB = 1
                candidato |= 1;  // LSB = 1 (ímpar)
                
                tentativas++;
                
                // Usar mpz_nextprime para encontrar o próximo primo
                mpz_class proximo_primo = candidato;
                mpz_nextprime(proximo_primo.get_mpz_t(), candidato.get_mpz_t());
                
                // Verificar se o próximo primo ainda tem o mesmo número de bits
                if (proximo_primo.get_str(2).length() == bits) {
                    // Verificar primalidade com alta confiança
                    if (mpz_probab_prime_p(proximo_primo.get_mpz_t(), 10) > 0) {
                        std::lock_guard<std::mutex> lock(mtx);
                        if (primos.size() < (size_t)quantidade && !parar_threads) {
                            primos.push_back(proximo_primo);
                            
                            if (primos.size() >= (size_t)quantidade) {
                                parar_threads = true;
                                break;
                            }
                        }
                    }
                }
            }
        };
        
        // Thread para busca direta
        auto thread_direto = [&]() {
            gmp_randclass rng(gmp_randinit_default);
            std::random_device rd;
            rng.seed(rd() + 1000 + std::chrono::high_resolution_clock::now().time_since_epoch().count());
            
            int tentativas = 0;
            
            while (!parar_threads && primos.size() < (size_t)quantidade && tentativas < max_tentativas) {
                // Gerar candidato aleatório
                mpz_class candidato = rng.get_z_bits(bits);
                candidato |= (mpz_class(1) << (bits - 1));  // MSB = 1
                candidato |= 1;  // LSB = 1 (ímpar)
                
                tentativas++;
                
                // Teste de primalidade direto
                if (mpz_probab_prime_p(candidato.get_mpz_t(), 5) > 0) {
                    std::lock_guard<std::mutex> lock(mtx);
                    if (primos.size() < (size_t)quantidade && !parar_threads) {
                        primos.push_back(candidato);
                        
                        if (primos.size() >= (size_t)quantidade) {
                            parar_threads = true;
                            break;
                        }
                    }
                }
            }
        };
        
        // Criar e executar threads
        std::vector<std::thread> threads;
        threads.emplace_back(thread_cpp);
        threads.emplace_back(thread_direto);
        
        // Aguardar threads terminarem
        for (auto& thread : threads) {
            thread.join();
        }
        
        return primos;
    }
};

// Interface JNI para geração híbrida por dígitos
extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_gerarPrimosAleatoriosHibrido(
    JNIEnv* env, jobject /* this */, jint quantidade, jint digitos, jint max_tentativas) {
    
    auto start_time = std::chrono::high_resolution_clock::now();
    
    GeradorPrimosHibrido gerador(3); // 3 threads especializadas
    std::vector<mpz_class> primos = gerador.gerarPrimosHibrido(quantidade, digitos, max_tentativas);
    
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
    double tempo_total = duration.count() / 1000.0;
    
    // Formatar resultado
    std::stringstream resultado;
    resultado << "🚀 PRIMOS ALEATÓRIOS HÍBRIDOS (C++ + GMP + mpz_nextprime)\n";
    resultado << "========================================================\n";
    resultado << "Estratégia: Java + C++ nativo com mpz_nextprime()\n";
    resultado << "Threads: 3 especializadas (nextprime, sequencial, incremento)\n";
    resultado << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos\n";
    resultado << "Dígitos: " << digitos << "\n";
    resultado << "Quantidade solicitada: " << quantidade << "\n";
    resultado << "Quantidade encontrada: " << primos.size() << "\n";
    resultado << "========================================================\n";
    resultado << "PRIMOS ENCONTRADOS:\n";
    resultado << "========================================================\n";
    
    for (size_t i = 0; i < primos.size(); i++) {
        resultado << "Primo #" << (i + 1) << ":\n";
        resultado << primos[i].get_str() << "\n";
        resultado << "(" << primos[i].get_str().length() << " digitos)\n";
        resultado << "----------------------------------------\n";
    }
    
    resultado << "\n========================================================\n";
    resultado << "Total de primos: " << primos.size() << "\n";
    resultado << "Dígitos por primo: " << digitos << "\n";
    resultado << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos\n";
    resultado << "Estratégia: Híbrida Java + C++ nativo\n";
    resultado << "========================================================\n";
    
    return env->NewStringUTF(resultado.str().c_str());
}

// Interface JNI para geração híbrida por bits
extern "C" JNIEXPORT jstring JNICALL
Java_com_seuprojeto_primeprofast_MainActivity_gerarPrimosAleatoriosHibridoPorBits(
    JNIEnv* env, jobject /* this */, jint quantidade, jint bits, jint max_tentativas) {
    
    auto start_time = std::chrono::high_resolution_clock::now();
    
    GeradorPrimosHibrido gerador(2); // 2 threads especializadas
    std::vector<mpz_class> primos = gerador.gerarPrimosHibridoPorBits(quantidade, bits, max_tentativas);
    
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
    double tempo_total = duration.count() / 1000.0;
    
    // Formatar resultado
    std::stringstream resultado;
    resultado << "🚀 PRIMOS ALEATÓRIOS HÍBRIDOS POR BITS (C++ + GMP + mpz_nextprime)\n";
    resultado << "==================================================================\n";
    resultado << "Estratégia: Java + C++ nativo com mpz_nextprime()\n";
    resultado << "Threads: 2 especializadas (nextprime, direto)\n";
    resultado << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos\n";
    resultado << "Bits: " << bits << "\n";
    resultado << "Quantidade solicitada: " << quantidade << "\n";
    resultado << "Quantidade encontrada: " << primos.size() << "\n";
    resultado << "==================================================================\n";
    resultado << "PRIMOS ENCONTRADOS:\n";
    resultado << "==================================================================\n";
    
    for (size_t i = 0; i < primos.size(); i++) {
        resultado << "Primo #" << (i + 1) << ":\n";
        resultado << primos[i].get_str() << "\n";
        resultado << "(" << bits << " bits, " << primos[i].get_str().length() << " digitos)\n";
        resultado << "----------------------------------------\n";
    }
    
    resultado << "\n==================================================================\n";
    resultado << "Total de primos: " << primos.size() << "\n";
    resultado << "Bits por primo: " << bits << "\n";
    resultado << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos\n";
    resultado << "Estratégia: Híbrida Java + C++ nativo\n";
    resultado << "==================================================================\n";
    
    return env->NewStringUTF(resultado.str().c_str());
} 