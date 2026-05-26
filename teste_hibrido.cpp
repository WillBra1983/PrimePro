#include <iostream>
#include <vector>
#include <chrono>
#include <thread>
#include <mutex>
#include <atomic>
#include <gmpxx.h>

// Classe para geração híbrida de primos aleatórios (versão standalone)
class GeradorPrimosHibridoStandalone {
private:
    int num_threads;
    std::atomic<bool> parar_threads{false};
    
public:
    GeradorPrimosHibridoStandalone(int threads = 3) : num_threads(threads) {}
    
    // Estratégia híbrida: múltiplas threads com diferentes estratégias
    std::vector<mpz_class> gerarPrimosHibrido(int quantidade, int digitos, int max_tentativas = 1000000) {
        std::vector<mpz_class> primos;
        std::mutex mtx;
        
        // Calcular bits aproximados para dígitos
        int bits_aproximados = (int)(digitos * 3.32);
        
        std::cout << "🎯 Iniciando geração híbrida de " << quantidade << " primos de " << digitos << " dígitos (" << bits_aproximados << " bits)" << std::endl;
        std::cout << "🚀 Usando " << num_threads << " threads especializadas..." << std::endl;
        
        // Thread 1: C++ usando mpz_nextprime (mais eficiente para candidatos próximos)
        auto thread_nextprime = [&]() {
            std::cout << "🔄 Thread 1 (mpz_nextprime): Iniciada" << std::endl;
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
                            std::cout << "✅ Thread 1 encontrou primo #" << primos.size() << " (" << proximo_str.length() << " dígitos)" << std::endl;
                            
                            if (primos.size() >= (size_t)quantidade) {
                                parar_threads = true;
                                break;
                            }
                        }
                    }
                }
                
                // Log de progresso
                if (tentativas % 1000 == 0) {
                    std::cout << "🔄 Thread 1: " << tentativas << " tentativas, " << primos.size() << " primos encontrados" << std::endl;
                }
            }
            std::cout << "🏁 Thread 1 finalizada com " << tentativas << " tentativas" << std::endl;
        };
        
        // Thread 2: Busca sequencial otimizada (para candidatos aleatórios)
        auto thread_sequencial = [&]() {
            std::cout << "🔄 Thread 2 (sequencial): Iniciada" << std::endl;
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
                        std::cout << "✅ Thread 2 encontrou primo #" << primos.size() << " (" << num_str.length() << " dígitos)" << std::endl;
                        
                        if (primos.size() >= (size_t)quantidade) {
                            parar_threads = true;
                            break;
                        }
                    }
                }
                
                // Log de progresso
                if (tentativas % 1000 == 0) {
                    std::cout << "🔄 Thread 2: " << tentativas << " tentativas, " << primos.size() << " primos encontrados" << std::endl;
                }
            }
            std::cout << "🏁 Thread 2 finalizada com " << tentativas << " tentativas" << std::endl;
        };
        
        // Thread 3: Busca por incremento inteligente
        auto thread_incremento = [&]() {
            std::cout << "🔄 Thread 3 (incremento): Iniciada" << std::endl;
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
                            std::cout << "✅ Thread 3 encontrou primo #" << primos.size() << " (" << num_str.length() << " dígitos)" << std::endl;
                            
                            if (primos.size() >= (size_t)quantidade) {
                                parar_threads = true;
                                break;
                            }
                        }
                        break; // Encontrou um primo, tentar próximo candidato base
                    }
                }
                
                // Log de progresso
                if (tentativas % 100 == 0) {
                    std::cout << "🔄 Thread 3: " << tentativas << " tentativas, " << primos.size() << " primos encontrados" << std::endl;
                }
            }
            std::cout << "🏁 Thread 3 finalizada com " << tentativas << " tentativas" << std::endl;
        };
        
        // Criar e executar threads
        std::vector<std::thread> threads;
        
        std::cout << "🚀 Iniciando threads..." << std::endl;
        threads.emplace_back(thread_nextprime);
        threads.emplace_back(thread_sequencial);
        threads.emplace_back(thread_incremento);
        
        // Aguardar threads terminarem ou timeout
        long timeout_start = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::high_resolution_clock::now().time_since_epoch()).count();
        long timeout = 120000; // 2 minutos
        
        std::cout << "⏱️ Aguardando threads com timeout de " << (timeout/1000) << " segundos..." << std::endl;
        
        while (primos.size() < (size_t)quantidade && 
               (std::chrono::duration_cast<std::chrono::milliseconds>(
                   std::chrono::high_resolution_clock::now().time_since_epoch()).count() - timeout_start) < timeout) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
        
        // Parar threads
        std::cout << "🛑 Parando threads..." << std::endl;
        parar_threads = true;
        for (auto& thread : threads) {
            if (thread.joinable()) {
                thread.join();
            }
        }
        
        std::cout << "🎯 Geração finalizada! " << primos.size() << " primos encontrados" << std::endl;
        return primos;
    }
};

// Função principal de teste
int main() {
    std::cout << "🔥 TESTE DA IMPLEMENTAÇÃO HÍBRIDA DE PRIMOS ALEATÓRIOS" << std::endl;
    std::cout << "=====================================================" << std::endl;
    
    // Configurações de teste
    int quantidade = 3;  // Quantidade de primos a gerar
    int digitos = 100;   // Número de dígitos
    int max_tentativas = 100000;
    
    std::cout << "📊 Parâmetros de teste:" << std::endl;
    std::cout << "- Quantidade: " << quantidade << " primos" << std::endl;
    std::cout << "- Dígitos: " << digitos << std::endl;
    std::cout << "- Máximo de tentativas: " << max_tentativas << std::endl;
    std::cout << std::endl;
    
    // Iniciar cronômetro
    auto start_time = std::chrono::high_resolution_clock::now();
    
    // Criar gerador e executar
    GeradorPrimosHibridoStandalone gerador(3);
    std::vector<mpz_class> primos = gerador.gerarPrimosHibrido(quantidade, digitos, max_tentativas);
    
    // Calcular tempo total
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
    double tempo_total = duration.count() / 1000.0;
    
    // Exibir resultados
    std::cout << std::endl;
    std::cout << "🎯 RESULTADOS FINAIS" << std::endl;
    std::cout << "=====================" << std::endl;
    std::cout << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_total << " segundos" << std::endl;
    std::cout << "Primos solicitados: " << quantidade << std::endl;
    std::cout << "Primos encontrados: " << primos.size() << std::endl;
    std::cout << "Dígitos por primo: " << digitos << std::endl;
    std::cout << std::endl;
    
    if (!primos.empty()) {
        std::cout << "📝 PRIMOS ENCONTRADOS:" << std::endl;
        std::cout << "=====================" << std::endl;
        
        for (size_t i = 0; i < primos.size(); i++) {
            std::cout << "Primo #" << (i + 1) << ":" << std::endl;
            std::cout << primos[i].get_str() << std::endl;
            std::cout << "(" << primos[i].get_str().length() << " dígitos)" << std::endl;
            std::cout << "---------------------" << std::endl;
        }
    } else {
        std::cout << "❌ Nenhum primo foi encontrado no tempo limite" << std::endl;
    }
    
    std::cout << std::endl;
    std::cout << "🏁 Teste concluído!" << std::endl;
    
    return 0;
} 