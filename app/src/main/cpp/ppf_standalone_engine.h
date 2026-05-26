#ifndef PPF_STANDALONE_ENGINE_H
#define PPF_STANDALONE_ENGINE_H

#include <string>

namespace ppf {

std::string super_primos_nativo(int bits, int quantidade, int threads, const std::string& caminho_arquivo);

}  // namespace ppf

#endif
