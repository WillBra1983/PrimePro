#include "../app/src/main/cpp/ppf_c_api.h"
#include "../app/src/main/cpp/ppf_prime_cards.h"

#include <cassert>
#include <set>
#include <sstream>

namespace {
std::set<std::string> report_lines(const std::string& report, std::size_t start) {
    assert(start != std::string::npos);
    std::istringstream input(report.substr(start));
    std::set<std::string> result;
    for (std::string line; std::getline(input, line);) {
        if (!line.empty()) {
            const auto inserted = result.insert(line).second;
            assert(inserted);
        }
    }
    return result;
}
}

int main() {
    assert(ppf::short_primes_report(1, 1).empty());
    assert(ppf::short_primes_report(4, 3).empty());
    assert(ppf::short_prime_count(5) == 5);
    assert(ppf::short_prime_count(8) == 23);
    assert(ppf::short_prime_count(12) == 255);
    assert(ppf::short_prime_count(13) == 0);
    assert(ppf::short_primes_report(5, 6).empty());
    const std::set<std::string> expected[] = {{"2", "3"}, {"5", "7"}, {"11", "13"}};
    for (int bits = 2; bits <= 4; ++bits) {
        const auto report = ppf::short_primes_report(bits, 2);
        const auto quantity = std::string("Quantidade: 2\n");
        const auto start = report.find(quantity);
        assert(start != std::string::npos);
        assert(report_lines(report, start + quantity.size()) == expected[bits - 2]);
    }

    const auto twins = ppf::special_primes_report(PPF_SPECIAL_TWINS, 7, 3);
    const auto pairs = std::set<std::string>{"(3, 5)", "(5, 7)"};
    assert(report_lines(twins, twins.find('\n')) == pairs);
    assert(ppf::special_primes_report(PPF_SPECIAL_TWINS, 4, 1).find("Nenhum") == 0);
    assert(ppf::special_primes_report(PPF_SPECIAL_SOPHIE, 5, 1).find("2 (2 \u00d7 2 + 1 = 5)") != std::string::npos);
    assert(ppf::special_primes_report(PPF_SPECIAL_COUSINS, 7, 1).find("(3, 7)") != std::string::npos);
    assert(ppf::special_primes_report(PPF_SPECIAL_SEXY, 11, 1).find("(5, 11)") != std::string::npos);
    assert(ppf::special_primes_report(PPF_SPECIAL_PALINDROMES, 11, 10).find("\n11") != std::string::npos);
    assert(ppf::special_primes_report(PPF_SPECIAL_FERMAT, 0, 1001).find("F4 = 65537") != std::string::npos);
    assert(ppf::special_primes_supported(PPF_SPECIAL_TWINS, INT64_MAX, PPF_SPECIAL_MAX_COUNT));
    assert(ppf::special_primes_report(PPF_SPECIAL_TWINS, PPF_SPECIAL_SIEVE_LIMIT + 1, 1)
               .find("busca por amostragem") != std::string::npos);
    const auto palindromes = ppf::special_primes_report(PPF_SPECIAL_PALINDROMES, INT64_MAX, 25);
    const auto large_sample = report_lines(palindromes, palindromes.find('\n'));
    assert(large_sample.size() == 25);
    for (const auto& decimal : large_sample) {
        assert(decimal == std::string(decimal.rbegin(), decimal.rend()));
        assert(ppf_is_prime_u64(std::stoull(decimal)));
    }
    assert(ppf::special_primes_report(PPF_SPECIAL_SEXY, 100, PPF_SPECIAL_MAX_COUNT + 1).empty());

    assert(ppf_is_prime_u64(2));
    assert(ppf_is_prime_u64(UINT64_C(18446744073709551557)));
    assert(!ppf_is_prime_u64(UINT64_C(341550071728321)));
    assert(!ppf_is_prime_u64(UINT64_MAX));
    assert(ppf_next_prime_between(1, 4) == 2);
    assert(ppf_next_prime_between(9, 16) == 11);
    assert(ppf_next_prime_between(11, 13) == 0);
    assert(ppf_next_prime_between(13, 11) == -1);
    const auto max_n = ppf_legendre_max_n();
    assert(max_n == 3037000498LL);
    const auto witness = ppf_next_prime_between(max_n * max_n, (max_n + 1) * (max_n + 1));
    assert(witness > max_n * max_n && witness < (max_n + 1) * (max_n + 1));
    assert(ppf::legendre_report(1, 3).find("Primos encontrados: 2\n11, 13") != std::string::npos);
    assert(ppf::legendre_report(3, 3) == "CONJECTURA DE LEGENDRE\n\nn = 3 | Intervalo (9, 16)\nPrimos encontrados: 2\n11, 13");
    assert(ppf::legendre_report_supported(1000000, 1000000));
    assert(ppf::legendre_report(max_n, max_n).find("Primeiro primo encontrado: ") != std::string::npos);
    assert(!ppf::legendre_report_supported(1, 101));

    const auto stats = ppf::prime_stats_report(100);
    assert(stats.find("Total de primos: 25\nMaior primo: 97") != std::string::npos);
    assert(stats.find("Pares de primos g\u00eameos: 8") != std::string::npos);
    assert(stats.find("Dist\u00e2ncia m\u00e1xima: 8") != std::string::npos);
    assert(ppf::prime_stats_report(2).find("Dist\u00e2ncia m\u00e9dia") == std::string::npos);
    assert(ppf::prime_stats_report(ppf_exact_stats_max() + 1).empty());
    assert(ppf::prime_stats_report(10000000).find("Total de primos: 664579\n") != std::string::npos);
    assert(ppf::prime_approximation_report("100").find("Estimativa N / ln(N): 21.714") != std::string::npos);
    const std::string huge = "1" + std::string(500, '0');
    const auto huge_report = ppf::prime_approximation_report(huge);
    assert(huge_report.find("N = " + huge) != std::string::npos);
    assert(huge_report.find("e+496") != std::string::npos);
    assert(huge_report.find("inf") == std::string::npos);
    assert(ppf::prime_approximation_report("1").empty());
    assert(ppf::prime_approximation_report("2x").empty());
}
