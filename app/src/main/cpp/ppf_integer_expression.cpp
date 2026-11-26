#include "ppf_c_api.h"
#include "gmp.h"

#include <cstring>
#include <cstdlib>
#include <string>

namespace {
constexpr size_t maxInput = 200000;
constexpr size_t maxBits = 332193;  // About 100,000 decimal digits.
constexpr unsigned maxDepth = 64;

struct Integer {
    mpz_t value;
    Integer() { mpz_init(value); }
    ~Integer() { mpz_clear(value); }
    Integer(const Integer&) = delete;
    Integer& operator=(const Integer&) = delete;
};

bool digit(char c) { return c >= '0' && c <= '9'; }

bool bounded(const mpz_t value) { return mpz_sizeinbase(value, 2) <= maxBits; }

bool power(mpz_t out, const mpz_t exponent) {
    if (mpz_sgn(exponent) < 0) return false;
    if (mpz_sgn(exponent) == 0) {
        if (mpz_sgn(out) == 0) return false;
        mpz_set_ui(out, 1);
        return true;
    }
    if (mpz_cmpabs_ui(out, 1) <= 0) {
        if (mpz_sgn(out) < 0 && mpz_even_p(exponent)) mpz_set_ui(out, 1);
        return true;
    }
    const auto maxExponent = (maxBits - 1) / (mpz_sizeinbase(out, 2) - 1);
    if (mpz_cmp_ui(exponent, maxExponent) > 0) return false;
    mpz_pow_ui(out, out, mpz_get_ui(exponent));
    return bounded(out);
}

class Expression {
public:
    explicit Expression(const char* input) : current(input) {}

    bool evaluate(mpz_t out) { return sum(out, 0) && *current == '\0'; }

private:
    const char* current;

    bool take(char token) {
        if (*current != token) return false;
        ++current;
        return true;
    }

    bool sum(mpz_t out, unsigned depth) {
        if (!product(out, depth)) return false;
        Integer rhs;
        while (*current == '+' || *current == '-') {
            const char op = *current++;
            if (!product(rhs.value, depth)) return false;
            if (op == '+') mpz_add(out, out, rhs.value);
            else mpz_sub(out, out, rhs.value);
            if (!bounded(out)) return false;
        }
        return true;
    }

    bool product(mpz_t out, unsigned depth) {
        if (!exponent(out, depth)) return false;
        Integer rhs;
        while (*current == '*' || *current == '/' || *current == '%') {
            const char op = *current++;
            if (!exponent(rhs.value, depth)) return false;
            if (op == '*') {
                mpz_mul(out, out, rhs.value);
                if (!bounded(out)) return false;
            } else if (op == '/') {
                if (mpz_sgn(rhs.value) == 0) return false;
                mpz_tdiv_q(out, out, rhs.value);
            } else {
                if (mpz_sgn(rhs.value) <= 0) return false;
                mpz_mod(out, out, rhs.value);
            }
        }
        return true;
    }

    bool exponent(mpz_t out, unsigned depth) {
        if (depth > maxDepth || !unary(out, depth)) return false;
        if (!take('^')) return true;
        Integer rhs;
        return exponent(rhs.value, depth + 1) && power(out, rhs.value);
    }

    bool unary(mpz_t out, unsigned depth) {
        // Preserve the Android grammar: the sign binds before exponentiation.
        bool negative = false;
        while (*current == '+' || *current == '-') {
            if (*current++ == '-') negative = !negative;
        }
        if (take('(')) {
            if (!sum(out, depth + 1) || !take(')')) return false;
        } else if (!number(out)) {
            return false;
        }
        if (negative) mpz_neg(out, out);
        return true;
    }

    bool number(mpz_t out) {
        const char* start = current;
        while (digit(*current)) ++current;
        if (current == start || mpz_set_str(out, std::string(start, current).c_str(), 10) != 0 ||
            !bounded(out)) return false;

        unsigned long scale = 0;
        if (take('e') || take('E')) {
            if (take('-')) return false;
            take('+');
            if (!digit(*current)) return false;
            while (digit(*current)) {
                scale = scale * 10 + (*current++ - '0');
                if (scale > maxBits) return false;
            }
        }
        if (take('k') || take('K')) scale += 3;
        else if (take('m') || take('M')) scale += 6;
        else if (take('g') || take('G')) scale += 9;
        if (scale == 0 || mpz_sgn(out) == 0) return true;
        Integer multiplier;
        mpz_set_ui(multiplier.value, 10);
        Integer scaleValue;
        mpz_set_ui(scaleValue.value, scale);
        if (!power(multiplier.value, scaleValue.value)) return false;
        mpz_mul(out, out, multiplier.value);
        return bounded(out);
    }
};
}

extern "C" char* ppf_evaluate_integer(const char* input) {
    if (!input || std::strlen(input) > maxInput) return nullptr;
    try {
        std::string expression;
        for (const char* c = input; *c; ++c) {
            if (*c != ' ' && !(*c >= '\t' && *c <= '\r')) expression += *c;
        }
        Integer value;
        if (!Expression(expression.c_str()).evaluate(value.value)) return nullptr;
        auto* result = static_cast<char*>(std::malloc(mpz_sizeinbase(value.value, 10) + 2));
        return result ? mpz_get_str(result, 10, value.value) : nullptr;
    } catch (...) {
        return nullptr;
    }
}

extern "C" void ppf_free_integer(char* value) {
    std::free(value);
}
