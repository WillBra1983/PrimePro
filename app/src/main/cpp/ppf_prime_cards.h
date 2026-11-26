#ifndef PPF_PRIME_CARDS_H
#define PPF_PRIME_CARDS_H

#include <string>
#include <cstdint>

namespace ppf {
std::string short_primes_report(int bits, int count);
int short_prime_count(int bits);
bool special_primes_supported(int family, std::int64_t limit, int count);
std::string special_primes_report(int family, std::int64_t limit, int count);
bool legendre_report_supported(std::int64_t first, std::int64_t last);
std::string legendre_report(std::int64_t first, std::int64_t last);
std::string legendre_report_decimal(const char* first, const char* last);
std::string legendre_witness_decimal(const char* first);
std::string prime_stats_report(std::int64_t limit);
std::string prime_approximation_report(const std::string& limit);
std::string mersenne_report(int exponent, bool list);
std::string factor_report(const char* number);
std::string perfect_report(int mode, std::int64_t value);
std::string perfect_verify_decimal(const std::string& number);
}

#endif
