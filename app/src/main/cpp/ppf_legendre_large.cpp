#include "ppf_c_api.h"
#include "ppf_prime_cards.h"

#include <algorithm>
#include <chrono>
#include <gmpxx.h>
#include <sstream>

namespace {
using Clock = std::chrono::steady_clock;

bool read_n(const char* text, mpz_class& n) {
    if (!text) return false;
    const std::string decimal(text);
    return !decimal.empty() && decimal.size() <= PPF_LEGENDRE_MAX_DIGITS && decimal.front() != '0'
        && std::all_of(decimal.begin(), decimal.end(), [](char c) { return c >= '0' && c <= '9'; })
        && n.set_str(decimal, 10) == 0;
}

struct Witness {
    std::string decimal;
    bool exact;
};

Witness search(const mpz_class& low, const mpz_class& high, Clock::time_point deadline) {
    if (mpz_sizeinbase(high.get_mpz_t(), 2) <= 63) {
        const auto p = ppf_next_prime_between(std::stoll(low.get_str()), std::stoll(high.get_str()));
        return {p > 0 ? std::to_string(p) : std::string{}, true};
    }
    mpz_class candidate = low + 1;
    mpz_setbit(candidate.get_mpz_t(), 0);
    const auto bits = mpz_sizeinbase(high.get_mpz_t(), 2);
    const int budget = bits <= 1024 ? 8192 : (bits <= 8192 ? 2048 : 256);
    for (int checked = 0; checked < budget && candidate < high && Clock::now() < deadline;
         ++checked, candidate += 2) {
        if (mpz_sizeinbase(candidate.get_mpz_t(), 2) <= 64) {
            const auto value = candidate.get_str();
            if (ppf_is_prime_u64(std::stoull(value))) return {value, true};
        } else if (mpz_probab_prime_p(candidate.get_mpz_t(), 25) > 0) {
            return {candidate.get_str(), false};
        }
    }
    return {{}, candidate >= high && bits <= 64};
}
}

std::string ppf::legendre_witness_decimal(const char* first) {
    mpz_class n;
    if (!read_n(first, n)) return {};
    const mpz_class low = n * n, next = n + 1, high = next * next;
    return search(low, high, Clock::now() + std::chrono::seconds(2)).decimal;
}

std::string ppf::legendre_report_decimal(const char* first, const char* last) {
    mpz_class n, end;
    if (!read_n(first, n) || !read_n(last, end) || end < n) return {};
    if (end <= mpz_class(std::to_string(ppf_legendre_max_n()))) {
        const auto regular = legendre_report(std::stoll(n.get_str()), std::stoll(end.get_str()));
        if (!regular.empty()) return regular;
    }
    std::ostringstream report;
    report << "CONJECTURA DE LEGENDRE - BUSCA POR TESTEMUNHAS\n"
           << "\nn inicial: " << n << "\nn final solicitado: " << end << '\n'
           << "Busca pelo primeiro primo ou prov\u00e1vel primo por intervalo; n\u00e3o lista todos os primos.\n";
    const auto deadline = Clock::now() + std::chrono::seconds(6);
    int checked = 0;
    for (; n <= end && checked < 100 && Clock::now() < deadline; ++n, ++checked) {
        const mpz_class low = n * n, next = n + 1, high = next * next;
        const auto witness = search(low, high, std::min(deadline, Clock::now() + std::chrono::seconds(2)));
        report << "\n\nn = " << n << " | Intervalo (" << low << ", " << high << ")\n";
        if (!witness.decimal.empty()) {
            report << (witness.exact ? "Primo encontrado: " : "Prov\u00e1vel primo encontrado: ") << witness.decimal;
        } else {
            report << "Nenhum primo encontrado nesta busca. "
                   << (witness.exact ? "Confirme por um m\u00e9todo independente."
                                     : "A busca foi parcial; o resultado \u00e9 inconclusivo.");
        }
    }
    report << "\n\nIntervalos examinados: " << checked;
    if (n <= end) report << "\nBusca parcial. Pr\u00f3ximo n: " << n;
    return report.str();
}
