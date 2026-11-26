#include "ppf_c_api.h"
#include "ppf_prime_cards.h"

#include <gmpxx.h>
#include <sstream>

namespace {
void append_factor(std::ostringstream& facts, bool& first, std::uint64_t factor, unsigned exponent) {
    if (exponent == 0) return;
    if (!first) facts << " x ";
    first = false;
    facts << factor;
    if (exponent > 1) facts << '^' << exponent;
}

void factor_15_digits(std::uint64_t value, std::ostringstream& facts, bool& first) {
    auto divide = [&](std::uint64_t p) {
        unsigned exponent = 0;
        while (value % p == 0) {
            value /= p;
            ++exponent;
        }
        append_factor(facts, first, p, exponent);
        if (exponent > 0 && value > 1 && ppf_is_prime_u64(value)) {
            append_factor(facts, first, value, 1);
            value = 1;
        }
    };

    if (ppf_is_prime_u64(value)) {
        append_factor(facts, first, value, 1);
        return;
    }
    divide(2);
    divide(3);
    for (std::uint64_t p = 5, step = 2; p <= value / p; p += step, step = 6 - step) {
        divide(p);
    }
    if (value > 1) append_factor(facts, first, value, 1);
}
}

std::string ppf::factor_report(const char* number) {
    mpz_class remaining;
    if (number == nullptr || remaining.set_str(number, 10) != 0 || remaining < 2) return {};

    const mpz_class complete_limit("999999999999999");
    std::ostringstream factors;
    bool first = true;
    for (unsigned long p = 2; remaining > complete_limit && p <= PPF_TRIAL_FACTOR_MAX; ++p) {
        if (!ppf_is_prime_u64(p)) continue;
        unsigned exponent = 0;
        while (mpz_divisible_ui_p(remaining.get_mpz_t(), p)) {
            mpz_divexact_ui(remaining.get_mpz_t(), remaining.get_mpz_t(), p);
            ++exponent;
        }
        append_factor(factors, first, p, exponent);
    }

    if (remaining <= complete_limit) {
        if (remaining > 1) factor_15_digits(std::stoull(remaining.get_str()), factors, first);
        return "Fatores primos: " + factors.str() + "\nFatora\u00e7\u00e3o completa.";
    }
    if (first) {
        return "Nenhum fator primo at\u00e9 10.000 foi encontrado.\n"
               "Isso n\u00e3o significa que o n\u00famero n\u00e3o tenha fatores maiores.";
    }
    return "Fatores primos encontrados: " + factors.str() + "\nCofator restante: "
        + remaining.get_str() + "\nFatora\u00e7\u00e3o parcial.";
}
