#include "ppf_c_api.h"
#include "gmp/include/gmp.h"
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

namespace {
constexpr size_t maxDigits = 10000;
constexpr size_t maxBits = 33220;

struct Integer {
    mpz_t value;
    Integer() { mpz_init(value); }
    ~Integer() { mpz_clear(value); }
    Integer(const Integer&) = delete;
    Integer& operator=(const Integer&) = delete;
};

class Expression {
public:
    explicit Expression(const char* input) : current(input) {}

    bool evaluate(mpz_t out) {
        return sum(out, 0) && (space(), *current == '\0') &&
               mpz_sizeinbase(out, 10) <= maxDigits + 1 &&
               decimal(out).size() - (mpz_sgn(out) < 0 ? 1 : 0) <= maxDigits;
    }

    static std::string decimal(const mpz_t value) {
        std::vector<char> buffer(mpz_sizeinbase(value, 10) + 3);
        return mpz_get_str(buffer.data(), 10, value);
    }

private:
    const char* current;

    void space() {
        while (*current == ' ' || *current == '\t' || *current == '\n' || *current == '\r') ++current;
    }

    bool take(char token) {
        space();
        if (*current != token) return false;
        ++current;
        return true;
    }

    bool sum(mpz_t out, unsigned depth) {
        if (!product(out, depth)) return false;
        Integer rhs;
        while (true) {
            const bool add = take('+');
            if (!add && !take('-')) return true;
            if (!product(rhs.value, depth)) return false;
            if (add) mpz_add(out, out, rhs.value);
            else mpz_sub(out, out, rhs.value);
            if (mpz_sizeinbase(out, 2) > maxBits) return false;
        }
    }

    bool product(mpz_t out, unsigned depth) {
        if (!unary(out, depth)) return false;
        Integer rhs;
        while (true) {
            const bool multiply = take('*');
            if (!multiply && !take('/')) return true;
            if (!unary(rhs.value, depth)) return false;
            if (multiply) {
                mpz_mul(out, out, rhs.value);
            } else {
                if (mpz_sgn(rhs.value) == 0 || !mpz_divisible_p(out, rhs.value)) return false;
                mpz_divexact(out, out, rhs.value);
            }
            if (mpz_sizeinbase(out, 2) > maxBits) return false;
        }
    }

    bool unary(mpz_t out, unsigned depth) {
        if (depth > 64) return false;
        if (take('+')) return unary(out, depth + 1);
        if (take('-')) {
            if (!unary(out, depth + 1)) return false;
            mpz_neg(out, out);
            return true;
        }
        if (!atom(out, depth)) return false;
        if (!take('^')) return true;
        Integer exponent;
        if (!unary(exponent.value, depth + 1) || mpz_sgn(exponent.value) < 0 ||
            mpz_cmp_ui(exponent.value, maxBits) > 0) return false;
        const auto power = mpz_get_ui(exponent.value);
        if (mpz_cmpabs_ui(out, 1) > 0 &&
            (mpz_sizeinbase(out, 2) - 1) * power >= maxBits) return false;
        mpz_pow_ui(out, out, power);
        return mpz_sizeinbase(out, 2) <= maxBits;
    }

    bool atom(mpz_t out, unsigned depth) {
        if (take('(')) return sum(out, depth + 1) && take(')');
        const char* start = current;
        while (*current >= '0' && *current <= '9') ++current;
        const size_t length = current - start;
        return length > 0 && length <= maxDigits &&
               mpz_set_str(out, std::string(start, length).c_str(), 10) == 0;
    }
};
}

extern "C" char* ppf_ios_evaluate(const char* expression) {
    if (!expression || std::strlen(expression) > 20000) return nullptr;
    Integer result;
    if (!Expression(expression).evaluate(result.value)) return nullptr;
    return strdup(Expression::decimal(result.value).c_str());
}

extern "C" char* ppf_ios_small_factors(const char* number) {
    Integer remaining;
    if (!number || mpz_set_str(remaining.value, number, 10) != 0 ||
        mpz_cmp_ui(remaining.value, 2) < 0) return nullptr;
    std::string report;
    for (unsigned long divisor = 2; divisor <= 10000; ++divisor) {
        if (mpz_cmp_ui(remaining.value, 1) == 0) break;
        unsigned long exponent = 0;
        while (mpz_divisible_ui_p(remaining.value, divisor)) {
            mpz_divexact_ui(remaining.value, remaining.value, divisor);
            ++exponent;
        }
        if (exponent == 0) continue;
        if (!report.empty()) report += " x ";
        report += std::to_string(divisor);
        if (exponent > 1) report += "^" + std::to_string(exponent);
    }
    if (report.empty()) return nullptr;
    if (mpz_cmp_ui(remaining.value, 1) > 0) {
        report += "\nCofator: " + Expression::decimal(remaining.value);
    }
    return strdup(report.c_str());
}

extern "C" void ppf_ios_free(char* value) {
    std::free(value);
}
