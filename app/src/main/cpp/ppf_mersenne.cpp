#include "ppf_c_api.h"
#include "ppf_engine.h"
#include "ppf_prime_cards.h"

#include <cmath>
#include <algorithm>
#include <gmpxx.h>
#include <iterator>
#include <sstream>
#include <vector>

namespace {
constexpr int reference_exponents[] = {
    2, 3, 5, 7, 13, 17, 19, 31, 61, 89, 107, 127, 521, 607, 1279, 2203, 2281,
    3217, 4253, 4423, 9689, 9941, 11213, 19937, 21701, 23209, 44497, 86243,
    110503, 132049, 216091, 756839, 859433, 1257787, 1398269, 2976221, 3021377,
    6972593, 13466917, 20996011, 24036583, 25964951, 30402457, 32582657, 37156667,
    42643801, 43112609, 57885161, 74207281, 77232917, 82589933, 136279841
};
constexpr int mersenne_decimal_max = 1000000;
constexpr int mersenne_test_max = 100000;

bool known_mersenne(int p) {
    return std::binary_search(std::begin(reference_exponents), std::end(reference_exponents), p);
}

std::string decimal_mersenne(int exponent) {
    mpz_class value = 0;
    mpz_setbit(value.get_mpz_t(), static_cast<mp_bitcnt_t>(exponent));
    value -= 1;
    return value.get_str();
}

bool test_mersenne(int exponent, const std::string& decimal) {
    if (exponent < 64) {
        return ppf_is_prime_u64((UINT64_C(1) << exponent) - 1);
    }
    return ppf::testar_primalidade_gigante(decimal, 25);
}

bool lucas_lehmer(int exponent, const mpz_class& modulus) {
    if (exponent == 2) return true;
    mpz_class state = 4, high, low;
    for (int i = 0; i < exponent - 2; ++i) {
        state = state * state - 2;
        if (state < 0) state += modulus;
        // 2^p is congruent to 1 modulo 2^p - 1.
        mpz_tdiv_q_2exp(high.get_mpz_t(), state.get_mpz_t(), exponent);
        mpz_tdiv_r_2exp(low.get_mpz_t(), state.get_mpz_t(), exponent);
        state = high + low;
        if (state >= modulus) state -= modulus;
    }
    return state == 0;
}

mpz_class even_perfect(int exponent) {
    mpz_class value = 0;
    mpz_setbit(value.get_mpz_t(), static_cast<mp_bitcnt_t>(exponent));
    value -= 1;
    value <<= static_cast<mp_bitcnt_t>(exponent - 1);
    return value;
}

std::string verify_perfect(std::int64_t value) {
    if (value < 1 || value > ppf_perfect_verify_max()) return {};
    const bool list_divisors = value < 100000000;
    std::vector<std::int64_t> divisors;
    std::int64_t sum = 0;
    auto record = [&](std::int64_t factor) {
        sum += factor;
        if (list_divisors) divisors.push_back(factor);
    };
    for (std::int64_t d = 1; d <= value / d; ++d) {
        if (value % d != 0) continue;
        const auto paired = value / d;
        if (d != value) record(d);
        if (paired != value && paired != d) record(paired);
    }
    std::ostringstream report;
    report << "VERIFICA\u00c7\u00c3O DE N\u00daMERO PERFEITO\n\nN\u00famero: " << value
           << "\nParidade: " << (value % 2 == 0 ? "par" : "\u00edmpar");
    if (list_divisors && !divisors.empty()) {
        std::sort(divisors.begin(), divisors.end());
        report << "\nDivisores pr\u00f3prios: ";
        for (std::size_t i = 0; i < divisors.size(); ++i) {
            if (i != 0) report << ", ";
            report << divisors[i];
        }
    }
    report << "\nSoma dos divisores pr\u00f3prios: " << sum << "\n\n";
    if (sum == value) {
        report << "\u00c9 um n\u00famero perfeito.";
    } else {
        report << "N\u00e3o \u00e9 perfeito. \u00c9 " << (sum > value ? "abundante" : "deficiente")
               << " (diferen\u00e7a entre a soma e o n\u00famero: " << sum - value << ").";
    }
    report << "\n\nRefer\u00eancias de n\u00fameros perfeitos pares:";
    for (int i = 0; i < 10; ++i) report << '\n' << even_perfect(reference_exponents[i]).get_str();
    return report.str();
}

const std::vector<int>& odd_trial_primes() {
    static const auto primes = []() {
        constexpr int limit = 1000000;
        std::vector<bool> composite(limit + 1);
        std::vector<int> result;
        for (int p = 3; p <= limit; p += 2) {
            if (composite[p]) continue;
            result.push_back(p);
            if (p > limit / p) continue;
            for (int i = p * p; i <= limit; i += 2 * p) composite[i] = true;
        }
        return result;
    }();
    return primes;
}

bool is_odd_perfect_exact(std::int64_t n) {
    if (n <= 1 || ppf_is_prime_u64(n)) return false;
    std::int64_t remaining = n, sigma = 1;
    for (const auto p : odd_trial_primes()) {
        if (p > remaining / p) break;
        if (remaining % p != 0) continue;
        std::int64_t power = 1, term = 1;
        do {
            remaining /= p;
            power *= p;
            term += power;
        } while (remaining % p == 0);
        sigma *= term;
    }
    if (remaining > 1) sigma *= remaining + 1;
    return sigma == 2 * n;
}
}

std::int64_t ppf_perfect_verify_max(void) {
    return 1000000000000LL;
}

extern "C" PPFOddPerfectBatch ppf_scan_odd_perfect(std::int64_t first, std::int64_t last, int attempts) {
    if (first < 1 || last < first || last >= ppf_perfect_verify_max() || attempts < 1 || attempts > 64) {
        return {};
    }
    try {
        PPFOddPerfectBatch result{first | 1, 0, 0};
        while (result.next <= last && result.tested < attempts) {
            const auto candidate = result.next;
            result.next += 2;
            ++result.tested;
            if (is_odd_perfect_exact(candidate)) {
                result.found = candidate;
                break;
            }
        }
        return result;
    } catch (...) {
        return {};
    }
}

std::string ppf::perfect_verify_decimal(const std::string& decimal) {
    if (decimal.empty() || decimal.size() > 10000 || decimal.front() == '0' ||
        !std::all_of(decimal.begin(), decimal.end(), [](char c) { return c >= '0' && c <= '9'; })) return {};
    if (decimal.size() <= 13) {
        const auto n = std::stoll(decimal);
        if (n <= ppf_perfect_verify_max()) return verify_perfect(n);
    }
    const mpz_class number(decimal);
    std::ostringstream report;
    report << "VERIFICA\u00c7\u00c3O DE N\u00daMERO PERFEITO\n\nN\u00famero: " << decimal
           << "\nD\u00edgitos: " << decimal.size() << '\n';
    if (mpz_odd_p(number.get_mpz_t())) {
        report << "\nResultado inconclusivo: a verifica\u00e7\u00e3o exata de divisores de um"
               << " \u00edmpar deste tamanho est\u00e1 fora do limite do app.";
        return report.str();
    }
    const auto trailing = mpz_scan1(number.get_mpz_t(), 0);
    const int exponent = static_cast<int>(trailing + 1);
    if (number != even_perfect(exponent) || !ppf_is_prime_u64(exponent)) {
        report << "\nN\u00e3o \u00e9 perfeito: n\u00e3o tem a forma 2^(p-1) x (2^p - 1)"
               << " com expoente primo, exigida para um perfeito par.";
        return report.str();
    }
    report << "\nExpoente de Mersenne: " << exponent << "\n";
    if (known_mersenne(exponent)) {
        report << "\n\u00c9 um n\u00famero perfeito par conhecido.";
    } else if (!test_mersenne(exponent, decimal_mersenne(exponent))) {
        report << "\nN\u00e3o \u00e9 perfeito: 2^p - 1 \u00e9 composto.";
    } else {
        report << "\nCandidato a perfeito par: 2^p - 1 passou no teste probabil\u00edstico,"
               << " mas precisa de uma prova de primalidade.";
    }
    return report.str();
}

std::string ppf::perfect_report(int mode, std::int64_t value) {
    if (mode == PPF_PERFECT_VERIFY) return verify_perfect(value);
    if ((mode != PPF_PERFECT_BY_LIMIT && mode != PPF_PERFECT_BY_DIGITS) || value < 1) return {};
    if (mode == PPF_PERFECT_BY_DIGITS && value > 10000) return {};

    std::ostringstream report;
    report << "N\u00daMEROS PERFEITOS PARES\n\n" << (mode == PPF_PERFECT_BY_LIMIT ? "Limite: " : "D\u00edgitos: ")
           << value << "\n\n";
    const mpz_class limit(std::to_string(value));
    int matches = 0;
    for (const int p : reference_exponents) {
        if (mode == PPF_PERFECT_BY_DIGITS && (2.0 * p - 2) * std::log10(2.0) >= value) break;
        const auto perfect = even_perfect(p);
        if (mode == PPF_PERFECT_BY_LIMIT && perfect > limit) break;
        const auto decimal = perfect.get_str();
        if (mode == PPF_PERFECT_BY_DIGITS && static_cast<std::int64_t>(decimal.size()) != value) continue;
        report << "Expoente de Mersenne: " << p << "\n" << decimal << "\n\n";
        ++matches;
    }
    report << "Resultados: " << matches << "\nCalculados pela f\u00f3rmula 2^(p-1) x (2^p - 1).";
    return report.str();
}

std::string ppf::mersenne_report(int exponent, bool list) {
    if (exponent < 2 || (list && exponent > PPF_MERSENNE_LIST_MAX)) return {};
    std::ostringstream report;
    if (list) {
        report << "PRIMOS DE MERSENNE AT\u00c9 EXPOENTE " << exponent << "\n\n";
        int matches = 0;
        for (int p = 2; p <= exponent; ++p) {
            if (!ppf_is_prime_u64(p)) continue;
            const auto decimal = decimal_mersenne(p);
            if (!known_mersenne(p) && (!test_mersenne(p, decimal) || !lucas_lehmer(p, mpz_class(decimal)))) continue;
            report << "M(" << p << ") = " << decimal << '\n';
            ++matches;
        }
        report << "\nResultados: " << matches
               << "\nFonte dos primos conhecidos: GIMPS. Candidatos fora da refer\u00eancia s\u00e3o testados por Lucas-Lehmer.";
        return report.str();
    }

    const auto decimal = exponent <= mersenne_decimal_max ? decimal_mersenne(exponent) : std::string{};
    const bool prime_exponent = ppf_is_prime_u64(exponent);
    report << "N\u00daMEROS DE MERSENNE\n\nM(" << exponent << ") = 2^" << exponent << " - 1\n\n";
    if (!decimal.empty()) {
        report << decimal << "\n\nD\u00edgitos: " << decimal.size() << '\n';
    } else {
        report << "D\u00edgitos: " << static_cast<std::int64_t>(std::floor(exponent * std::log10(2.0))) + 1
               << "\nO decimal completo est\u00e1 dispon\u00edvel at\u00e9 p = 1.000.000.\n";
    }
    if (!prime_exponent) {
        report << "Composto: o expoente " << exponent << " \u00e9 composto.\n";
    } else if (exponent > PPF_MERSENNE_LIST_MAX && known_mersenne(exponent)) {
        report << "Primo conhecido (refer\u00eancia GIMPS).\n";
    } else if (exponent > mersenne_test_max) {
        report << "Primalidade n\u00e3o determinada: o teste local vai at\u00e9 p = 100.000.\n";
    } else {
        const bool prime = test_mersenne(exponent, decimal) && lucas_lehmer(exponent, mpz_class(decimal));
        report << (prime ? "Primo (teste de Lucas-Lehmer).\n" : "Composto.\n");
    }
    if (exponent == 31) report << "M(31) \u00e9 o maior primo que cabe em 32 bits assinados.\n";

    report << "\nExpoentes de refer\u00eancia GIMPS:\n";
    for (const int p : reference_exponents) {
        report << "M(" << p << ") = ";
        if (p <= 127) {
            report << decimal_mersenne(p);
        } else {
            report << '[' << static_cast<int>(std::floor(p * std::log10(2.0))) + 1 << " d\u00edgitos]";
        }
        report << '\n';
    }
    return report.str();
}
