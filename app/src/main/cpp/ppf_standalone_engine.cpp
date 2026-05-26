// super_primos_standalone.cpp
// Versão standalone do super_primos para Android via JNI
// Baseado no super_primos.cpp original

#include "ppf_standalone_engine.h"

#include <gmpxx.h>
#include <vector>
#include <random>
#include <fstream>
#include <iostream>
#include <chrono>
#include <thread>
#include <mutex>
#include <string>
#include <sstream>
#include <iomanip> // Added for std::fixed and std::setprecision

// Gera uma lista de primos pequenos até um limite usando Crivo de Eratóstenes
std::vector<int> gerar_primos_pequenos_standalone(int limite) {
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

namespace ppf {

std::string super_primos_nativo(int bits, int quantidade, int threads, const std::string& caminho_str) {
    std::vector<mpz_class> primos_encontrados;
    auto inicio = std::chrono::high_resolution_clock::now();
    
    std::mutex mtx;
    bool parar_threads = false;
    
    // Função para cada thread
    auto thread_func = [&](int thread_id) {
        gmp_randclass rng(gmp_randinit_default);
        std::random_device rd;
        rng.seed(rd() + thread_id);
        
        while (!parar_threads) {
            {
                std::lock_guard<std::mutex> lock(mtx);
                if ((int)primos_encontrados.size() >= quantidade) {
                    parar_threads = true;
                    break;
                }
            }
            
            mpz_class candidato = rng.get_z_bits(bits);
            // Garante MSB e LSB ligados
            candidato |= (mpz_class(1) << (bits - 1));
            candidato |= 1;
            
            // Pré-filtro otimizado (apenas primos críticos)
            static const int primos_filtro[] = {3, 5, 7, 11, 13};
            static const int num_primos_filtro = 5;
            
            bool divisivel = false;
            for (int i = 0; i < num_primos_filtro; ++i) {
                if (mpz_divisible_ui_p(candidato.get_mpz_t(), primos_filtro[i])) {
                    divisivel = true;
                    break;
                }
            }
            if (divisivel) continue;
            
            // Teste de primalidade ultra otimizado (3 repetições para máxima performance)
            if (mpz_probab_prime_p(candidato.get_mpz_t(), 3) > 0) {
                std::lock_guard<std::mutex> lock(mtx);
                if ((int)primos_encontrados.size() < quantidade) {
                    primos_encontrados.push_back(candidato);
                    
                    // Verificar se já temos todos os primos necessários
                    if ((int)primos_encontrados.size() >= quantidade) {
                        parar_threads = true;
                    }
                }
                // Continuar procurando mais primos (método n/12)
            }
        }
    };
    
    // Criar threads
    std::vector<std::thread> thread_list;
    for (int i = 0; i < threads; i++) {
        thread_list.emplace_back(thread_func, i);
    }
    
    // Aguardar threads
    for (auto& t : thread_list) {
        t.join();
    }
    
    auto fim = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double> tempo_execucao = fim - inicio;
    
    // Salvar primos em arquivo (evita gargalo de conversão para Java)
    std::ofstream arquivo(caminho_str);
    arquivo << "🚀 SUPER PRIMOS NATIVO (OTIMIZADO)\n";
    arquivo << "==========================================\n";
    arquivo << "Bits: " << bits << "\n";
    arquivo << "Quantidade: " << quantidade << "\n";
    arquivo << "Threads: " << threads << " (ULTRA OTIMIZADO)\n";
    arquivo << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_execucao.count() << " segundos\n";
    arquivo << "==========================================\n";
    arquivo << "PRIMOS ENCONTRADOS:\n";
    arquivo << "==========================================\n";
    
    for (size_t i = 0; i < primos_encontrados.size(); i++) {
        arquivo << "Primo #" << (i + 1) << ":\n";
        arquivo << primos_encontrados[i].get_str() << "\n";
        arquivo << "(" << bits << " bits, " << primos_encontrados[i].get_str().length() << " digitos)\n";
        arquivo << "------------------------------------------\n";
    }
    arquivo.close();
    
    // Retornar apenas estatísticas para Java (evita gargalo)
    std::stringstream resultado_estatisticas;
    resultado_estatisticas << "🚀 SUPER PRIMOS NATIVO CONCLUÍDO!\n";
    resultado_estatisticas << "==========================================\n";
    resultado_estatisticas << "Bits: " << bits << "\n";
    resultado_estatisticas << "Quantidade: " << quantidade << "\n";
    resultado_estatisticas << "Threads: " << threads << "\n";
    resultado_estatisticas << "Tempo total: " << std::fixed << std::setprecision(4) << tempo_execucao.count() << " segundos\n";
    resultado_estatisticas << "Arquivo salvo: " << caminho_str << "\n";
    resultado_estatisticas << "==========================================\n";
    resultado_estatisticas << "SUCESSO - Abrindo visualizador...";
    
    return resultado_estatisticas.str();
}

}  // namespace ppf