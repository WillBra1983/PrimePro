#include "ppf_prime_cards.h"
#include "ppf_c_api.h"

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdlib>
#include <iomanip>
#include <random>
#include <sstream>
#include <unordered_set>
#include <vector>

namespace {
std::vector<bool> prime_flags(int limit) {
    std::vector<bool> flags(static_cast<std::size_t>(limit) + 1, true);
    flags[0] = flags[1] = false;
    for (int p = 2; p <= limit / p; ++p) {
        if (!flags[p]) continue;
        for (int i = p * p; i <= limit; i += p) flags[i] = false;
    }
    return flags;
}

std::uint64_t partner(int family, std::uint64_t p) {
    switch (family) {
        case PPF_SPECIAL_TWINS: return p + 2;
        case PPF_SPECIAL_SOPHIE: return 2 * p + 1;
        case PPF_SPECIAL_COUSINS: return p + 4;
        case PPF_SPECIAL_SEXY: return p + 6;
        default: return 0;
    }
}

std::vector<int> short_primes(int bits) {
    if (bits < 2 || bits > PPF_SHORT_PRIME_MAX_BITS) return {};
    const auto flags = prime_flags((1 << bits) - 1);
    std::vector<int> primes;
    for (int p = 1 << (bits - 1); p < (1 << bits); ++p) {
        if (flags[p]) primes.push_back(p);
    }
    return primes;
}

using u64 = std::uint64_t;
constexpr std::int64_t max_legendre_n = 3037000498LL;

u64 mul_mod(u64 a, u64 b, u64 modulus) {
    return static_cast<u64>((static_cast<__uint128_t>(a) * b) % modulus);
}

u64 pow_mod(u64 base, u64 exponent, u64 modulus) {
    u64 result = 1;
    while (exponent != 0) {
        if ((exponent & 1) != 0) result = mul_mod(result, base, modulus);
        exponent >>= 1;
        if (exponent != 0) base = mul_mod(base, base, modulus);
    }
    return result;
}

bool is_prime_u64(u64 n) {
    if (n < 2) return false;
    for (const u64 p : {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37}) {
        if (n % p == 0) return n == p;
    }
    u64 d = n - 1;
    int s = 0;
    while ((d & 1) == 0) {
        d >>= 1;
        ++s;
    }
    // This witness set makes Miller-Rabin deterministic for unsigned 64-bit inputs.
    constexpr u64 bases[] = {2, 325, 9375, 28178, 450775, 9780504, 1795265022};
    for (const auto base : bases) {
        if (base % n == 0) continue;
        auto x = pow_mod(base % n, d, n);
        if (x == 1 || x == n - 1) continue;
        bool passed = false;
        for (int r = 1; r < s; ++r) {
            x = mul_mod(x, x, n);
            if (x == n - 1) {
                passed = true;
                break;
            }
        }
        if (!passed) return false;
    }
    return true;
}

u64 odd_palindrome(u64 prefix) {
    u64 result = prefix;
    for (auto x = prefix / 10; x != 0; x /= 10) result = result * 10 + x % 10;
    return result;
}

std::vector<u64> sample_special_primes(int family, u64 limit, int count, std::mt19937_64& rng) {
    const bool palindromes = family == PPF_SPECIAL_PALINDROMES;
    struct PrefixRange { u64 first, size; };
    std::vector<PrefixRange> ranges;
    u64 population = 0;
    if (palindromes) {
        for (u64 scale = 1; scale <= 1000000000; scale *= 10) {
            if (odd_palindrome(scale) > limit) break;
            auto last = std::min(scale * 10 - 1, limit / scale);
            if (odd_palindrome(last) > limit) --last;
            const auto size = last - scale + 1;
            ranges.push_back({scale, size});
            population += size;
        }
        ++population;  // 11 is the only prime palindrome with even length.
    } else {
        const u64 max_p = family == PPF_SPECIAL_SOPHIE ? (limit - 1) / 2
                                                      : limit - (partner(family, 3) - 3);
        population = (max_p - 1) / 2 + (family == PPF_SPECIAL_SOPHIE);
    }
    std::uniform_int_distribution<u64> choose(0, population - 1);
    std::unordered_set<u64> seen;
    std::vector<u64> result;
    result.reserve(count);
    const int budget = std::min(4000000, std::max(100000, count * 5000));
    for (int i = 0; i < budget && static_cast<int>(result.size()) < count; ++i) {
        auto slot = choose(rng);
        u64 p;
        if (palindromes) {
            p = 11;
            for (const auto& range : ranges) {
                if (slot < range.size) {
                    p = odd_palindrome(range.first + slot);
                    break;
                }
                slot -= range.size;
            }
        } else {
            p = family == PPF_SPECIAL_SOPHIE && slot == population - 1 ? 2 : 2 * slot + 3;
        }
        if (seen.count(p) != 0) continue;
        const auto q = partner(family, p);
        if (q != 0 && (!is_prime_u64(q) || q > limit)) continue;
        if (!is_prime_u64(p)) continue;
        seen.insert(p);
        result.push_back(p);
    }
    return result;
}

double logarithmic_integral_from_two(double limit) {
    constexpr int steps = 2048;
    const double start = std::log(2.0);
    const double end = std::log(limit);
    const double step = (end - start) / steps;
    double sum = 0;
    // Substitute u = ln(t) before Simpson integration to resolve the far end for large N.
    for (int i = 0; i <= steps; ++i) {
        const double u = start + i * step;
        const double weight = i == 0 || i == steps ? 1 : (i % 2 == 0 ? 2 : 4);
        sum += weight * std::exp(u) / u;
    }
    return sum * step / 3;
}

double large_logarithmic_integral_ratio(double log_limit) {
    // After division by N, integrate exp(-v)/(log(N)-v), where v = log(N/t).
    // Beyond v = 48 the omitted contribution is at most exp(-48)/log(2).
    constexpr int steps = 8192;
    constexpr double step = 48.0 / steps;
    double sum = 0;
    for (int i = 0; i <= steps; ++i) {
        const double v = i * step;
        const int weight = i == 0 || i == steps ? 1 : (i % 2 == 0 ? 2 : 4);
        sum += weight * std::exp(-v) / (log_limit - v);
    }
    return sum * step / 3.0;
}

std::string scaled_scientific(double mantissa, int exponent) {
    const int shift = static_cast<int>(std::floor(std::log10(mantissa)));
    std::ostringstream out;
    out << std::setprecision(11) << mantissa / std::pow(10.0, shift) << "e+" << exponent + shift;
    return out.str();
}
}

int ppf::short_prime_count(int bits) {
    return static_cast<int>(short_primes(bits).size());
}

std::string ppf::short_primes_report(int bits, int count) {
    if (count < 1) return {};
    auto primes = short_primes(bits);
    if (primes.size() < static_cast<std::size_t>(count)) return {};
    std::mt19937 rng(std::random_device{}());
    std::shuffle(primes.begin(), primes.end(), rng);
    std::string report = "PRIMOS ALEAT\u00d3RIOS\n\nBits: " + std::to_string(bits)
        + "\nQuantidade: " + std::to_string(count) + "\n";
    for (int i = 0; i < count; ++i) report += "\n" + std::to_string(primes[i]);
    return report;
}

bool ppf::special_primes_supported(int family, std::int64_t limit, int count) {
    return family >= PPF_SPECIAL_TWINS && family <= PPF_SPECIAL_FERMAT && count > 0
        && (family == PPF_SPECIAL_FERMAT
            || (limit >= 2 && count <= PPF_SPECIAL_MAX_COUNT));
}

std::string ppf::special_primes_report(int family, std::int64_t limit, int count) {
    if (!special_primes_supported(family, limit, count)) return {};
    if (family == PPF_SPECIAL_FERMAT) {
        constexpr std::array<int, 5> known = {3, 5, 17, 257, 65537};
        const int size = std::min(count, static_cast<int>(known.size()));
        std::string report = "Primos de Fermat conhecidos (" + std::to_string(size) + " de 5)\n";
        for (int i = 0; i < size; ++i) {
            report += "\nF" + std::to_string(i) + " = " + std::to_string(known[i]);
        }
        return report;
    }
    const bool exhaustive = limit <= PPF_SPECIAL_SIEVE_LIMIT;
    std::mt19937_64 rng(std::random_device{}());
    std::vector<u64> sample;
    sample.reserve(count);
    int matches = 0;
    if (!exhaustive) {
        sample = sample_special_primes(family, static_cast<u64>(limit), count, rng);
    } else {
        const auto sieve_limit = static_cast<int>(limit);
        const auto flags = prime_flags(sieve_limit);
        // Reservoir sampling avoids retaining the full list and gives each match the same chance.
        for (int p = 2; p <= sieve_limit; ++p) {
            if (!flags[p]) continue;
            const auto q = partner(family, p);
            if (q != 0) {
                if (q > static_cast<u64>(sieve_limit) || !flags[q]) continue;
            } else {
                const auto decimal = std::to_string(p);
                if (!std::equal(decimal.begin(), decimal.end(), decimal.rbegin())) continue;
            }
            ++matches;
            if (static_cast<int>(sample.size()) < count) {
                sample.push_back(p);
            } else {
                const int index = std::uniform_int_distribution<int>(0, matches - 1)(rng);
                if (index < count) sample[index] = p;
            }
        }
    }
    if (sample.empty() && exhaustive) return "Nenhum resultado at\u00e9 " + std::to_string(limit) + ".";
    std::shuffle(sample.begin(), sample.end(), rng);
    constexpr std::array<const char*, 5> names = {
        "G\u00eameos", "Sophie Germain", "Cousins", "Sexy", "Pal\u00edndromos"
    };
    std::string report = std::string(names[family]) + " | " + std::to_string(sample.size())
        + " de " + std::to_string(exhaustive ? matches : count) + " at\u00e9 " + std::to_string(limit);
    if (!exhaustive) report += " | busca por amostragem";
    report += '\n';
    for (const auto p : sample) {
        const auto text = std::to_string(p);
        const auto q = partner(family, p);
        if (family == PPF_SPECIAL_SOPHIE) {
            report += "\n" + text + " (2 \u00d7 " + text + " + 1 = " + std::to_string(q) + ")";
        } else if (q != 0) {
            report += "\n(" + text + ", " + std::to_string(q) + ")";
        } else {
            report += "\n" + text;
        }
    }
    if (!exhaustive && static_cast<int>(sample.size()) < count) {
        report += "\n\nA amostra encontrou menos resultados que o solicitado; outros podem existir no intervalo.";
    }
    return report;
}

std::int64_t ppf_legendre_max_n(void) {
    return max_legendre_n;
}

bool ppf_is_prime_u64(std::uint64_t number) {
    return is_prime_u64(number);
}

std::int64_t ppf_next_prime_between(std::int64_t lower, std::int64_t upper) {
    if (lower < 0 || upper <= lower) return -1;
    if (lower < 2 && upper > 2) return 2;
    const u64 end = static_cast<u64>(upper);
    for (u64 n = static_cast<u64>(lower + 1) | 1; n < end; n += 2) {
        if (is_prime_u64(n)) return static_cast<std::int64_t>(n);
    }
    return 0;
}

static bool legendre_full_report_supported(std::int64_t first, std::int64_t last) {
    return first >= 1 && last >= first && last <= 1000000
        && last - first < 100 && (last - first + 1) * (2 * last + 1) <= 5000000;
}

bool ppf::legendre_report_supported(std::int64_t first, std::int64_t last) {
    return (first >= 1 && first == last && first <= max_legendre_n)
        || legendre_full_report_supported(first, last);
}

std::string ppf::legendre_report(std::int64_t first, std::int64_t last) {
    if (!legendre_report_supported(first, last)) return {};
    if (!legendre_full_report_supported(first, last)) {
        const auto a = first * first;
        const auto b = (first + 1) * (first + 1);
        const auto witness = ppf_next_prime_between(a, b);
        std::ostringstream report;
        report << "CONJECTURA DE LEGENDRE\n\nn = " << first
               << " | Intervalo (" << a << ", " << b << ")\n";
        if (witness > 0) report << "Primeiro primo encontrado: " << witness << '.';
        else report << "Nenhum primo encontrado na busca exata.";
        return report.str();
    }
    const auto lo = first * first + 1;
    const auto hi = (last + 1) * (last + 1) - 1;
    std::vector<bool> flags(static_cast<std::size_t>(hi - lo + 1), true);
    const auto bases = prime_flags(static_cast<int>(last + 1));
    for (std::int64_t p = 2; p <= last + 1; ++p) {
        if (!bases[p]) continue;
        const auto start = std::max(p * p, ((lo + p - 1) / p) * p);
        for (auto multiple = start; multiple <= hi; multiple += p) flags[multiple - lo] = false;
    }
    std::ostringstream report;
    report << "CONJECTURA DE LEGENDRE";
    for (auto n = first; n <= last; ++n) {
        const auto a = n * n;
        const auto b = (n + 1) * (n + 1);
        int count = 0;
        for (auto p = a + 1; p < b; ++p) if (flags[p - lo]) ++count;
        report << "\n\nn = " << n << " | Intervalo (" << a << ", " << b
               << ")\nPrimos encontrados: " << count << "\n";
        bool first_prime = true;
        for (auto p = a + 1; p < b; ++p) {
            if (!flags[p - lo]) continue;
            if (!first_prime) report << ", ";
            report << p;
            first_prime = false;
        }
    }
    return report.str();
}

extern "C" std::int64_t ppf_exact_stats_max() {
    return 10000000000LL;
}

std::string ppf::prime_stats_report(std::int64_t limit) {
    if (limit < 2 || limit > ppf_exact_stats_max()) return {};
    const auto start = std::chrono::steady_clock::now();
    std::array<std::int64_t, 10> endings{};
    std::int64_t count = 1, previous = 2, smallest_gap = limit, largest_gap = 0, twins = 0;
    endings[2] = 1;
    const auto record = [&](std::int64_t p) {
        ++count;
        ++endings[p % 10];
        const auto gap = p - previous;
        smallest_gap = std::min(smallest_gap, gap);
        largest_gap = std::max(largest_gap, gap);
        if (gap == 2) ++twins;
        previous = p;
    };
    constexpr std::int64_t block_odds = 1 << 19;
    const int root = static_cast<int>(std::sqrt(static_cast<double>(limit)));
    const auto base_flags = prime_flags(std::max(root, 2));
    std::vector<int> bases;
    for (int p = 3; p <= root; p += 2) {
        if (base_flags[p]) bases.push_back(p);
    }
    std::vector<unsigned char> composite(static_cast<std::size_t>(block_odds));
    for (std::int64_t low = 3; low <= limit; low += 2 * block_odds) {
        const auto high = std::min(limit, low + 2 * (block_odds - 1));
        const auto count_odds = (high - low) / 2 + 1;
        std::fill_n(composite.begin(), count_odds, 0);
        for (std::int64_t p : bases) {
            if (p * p > high) break;
            auto first = std::max(p * p, ((low + p - 1) / p) * p);
            if (first % 2 == 0) first += p;
            for (auto idx = (first - low) / 2; idx < count_odds; idx += p) composite[idx] = 1;
        }
        for (std::int64_t idx = 0; idx < count_odds; ++idx) {
            if (!composite[idx]) record(low + 2 * idx);
        }
    }
    const double estimate = logarithmic_integral_from_two(limit);
    const auto elapsed = std::chrono::duration<double>(std::chrono::steady_clock::now() - start);
    std::ostringstream report;
    report << "ESTAT\u00cdSTICAS COMPLETAS DE PRIMOS\n\nLimite: " << limit
           << "\nTotal de primos: " << count << "\nMaior primo: " << previous
           << "\nDensidade: " << std::fixed << std::setprecision(4) << count * 100.0 / limit
           << "%\nPares de primos g\u00eameos: " << twins << "\n\nDistribui\u00e7\u00e3o por \u00faltimo d\u00edgito:\n";
    for (std::size_t digit = 0; digit < endings.size(); ++digit) {
        if (endings[digit] > 0) report << digit << ": " << endings[digit] << '\n';
    }
    if (count > 1) {
        report << "\nDist\u00e2ncia m\u00ednima: " << smallest_gap << "\nDist\u00e2ncia m\u00e1xima: " << largest_gap
               << "\nDist\u00e2ncia m\u00e9dia: " << std::setprecision(2) << (previous - 2.0) / (count - 1) << '\n';
    }
    report << "\nIntegral de 2 at\u00e9 N de 1 / ln(t): " << std::setprecision(3) << estimate
           << "\nDiferen\u00e7a em rela\u00e7\u00e3o ao total real: " << std::setprecision(2)
           << std::abs(count - estimate) * 100.0 / count << "%\nTempo: "
           << std::setprecision(3) << elapsed.count() << " s";
    return report.str();
}

std::string ppf::prime_approximation_report(const std::string& decimal) {
    if (decimal.empty() || decimal.size() > PPF_APPROXIMATION_MAX_DIGITS ||
        decimal.front() == '0' || decimal == "1" ||
        !std::all_of(decimal.begin(), decimal.end(), [](char c) { return c >= '0' && c <= '9'; })) return {};
    std::ostringstream report;
    report << "APROXIMA\u00c7\u00c3O LOGAR\u00cdTMICA DE PRIMOS\n\nN = " << decimal;
    if (decimal.size() <= 100) {
        const double limit = std::strtod(decimal.c_str(), nullptr);
        const double estimate = logarithmic_integral_from_two(limit);
        report << std::setprecision(16) << "\n\nEstimativa N / ln(N): " << limit / std::log(limit)
               << "\nIntegral de 2 at\u00e9 N de 1 / ln(t): " << estimate
               << "\nDensidade estimada: " << estimate * 100.0 / limit << '%';
    } else {
        const int exponent = static_cast<int>(decimal.size()) - 1;
        const double mantissa = std::strtod(decimal.substr(0, 16).c_str(), nullptr) / 1e15;
        const double log_limit = exponent * std::log(10.0) + std::log(mantissa);
        const double ratio = large_logarithmic_integral_ratio(log_limit);
        report << "\n\nEstimativa N / ln(N): " << scaled_scientific(mantissa / log_limit, exponent)
               << "\nIntegral de 2 at\u00e9 N de 1 / ln(t): " << scaled_scientific(mantissa * ratio, exponent)
               << "\nDensidade estimada: " << std::setprecision(11) << ratio * 100.0 << '%';
    }
    report << "\n\nEstimativas, n\u00e3o contagem exata.";
    return report.str();
}
