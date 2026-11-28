#include "../app/src/main/cpp/ppf_c_api.h"

#include <cassert>
#include <chrono>
#include <fstream>
#include <gmpxx.h>
#include <set>
#include <string>
#include <thread>

namespace {
std::string evaluate(const char* expression) {
    char* decimal = ppf_evaluate_integer(expression);
    assert(decimal != nullptr);
    const std::string text(decimal);
    ppf_free_integer(decimal);
    return text;
}

std::string copy_report(const char* report) {
    assert(report != nullptr);
    const std::string text(report);
    ppf_release_string(report);
    return text;
}

std::string wait_for_prime_job() {
    const auto until = std::chrono::steady_clock::now() + std::chrono::seconds(120);
    while (!ppf_geracao_job_concluido() && std::chrono::steady_clock::now() < until) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    if (!ppf_geracao_job_concluido()) {
        ppf_cancelar_job();
        assert(false && "Prime generation timed out in test");
    }
    return copy_report(ppf_obter_resultado_job());
}
}

int main(int argc, char** argv) {
    assert(evaluate("2^31-1") == "2147483647");
    assert(evaluate("2^3^2") == "512");
    assert(evaluate("-2^2") == "4");
    assert(evaluate("-(2^2)") == "-4");
    assert(evaluate("-7 / 3") == "-2");
    assert(evaluate("-7 % 3") == "2");
    assert(evaluate("2E+2k + 1M + 1g") == "1001200000");
    assert(evaluate("(-1)^99999999999999999999999") == "-1");
    assert(evaluate("10^9999") == "1" + std::string(9999, '0'));
    for (const char* invalid : {"", "1.5", "1 / 0", "10 % 0", "10 % (-3)", "0^0",
                                "2^-1", "1e-1", "1e+", "2^332193", "(1+2", "2(3)"}) {
        assert(ppf_evaluate_integer(invalid) == nullptr);
    }
    const std::string nested = std::string(65, '(') + "1" + std::string(65, ')');
    assert(ppf_evaluate_integer(nested.c_str()) == nullptr);
    assert(ppf_testar_primalidade_gigante("170141183460469231731687303715884105727", 25));
    assert(!ppf_testar_primalidade_gigante("340282366920938463463374607431768211454", 25));
    if (argc > 1) {
        assert(!ppf_iniciar_primos_aleatorios_job(12, 1, argv[1]));
        assert(!ppf_iniciar_primos_aleatorios_job(33001, 1, argv[1]));
        assert(!ppf_iniciar_primos_aleatorios_job(13, 51, argv[1]));
        assert(ppf_iniciar_primos_aleatorios_job(13, 50, argv[1]));
        const auto generated = wait_for_prime_job();
        assert(generated.find("Quantidade encontrada: 50") != std::string::npos);
        {
            std::ifstream full(argv[1]);
            std::set<unsigned long> unique;
            for (unsigned long p; full >> p;) {
                assert(p >= 4096 && p < 8192 && ppf_is_prime_u64(p));
                assert(unique.insert(p).second);
            }
            assert(unique.size() == 50);
        }
        assert(ppf_iniciar_primos_aleatorios_job(4096, 1, argv[1]));
        assert(wait_for_prime_job().find("Quantidade encontrada: 1\n") != std::string::npos);
        {
            std::ifstream full(argv[1]);
            std::string decimal, extra;
            assert(full >> decimal);
            assert(!(full >> extra));
            assert(decimal.size() == 1233 || decimal.size() == 1234);
            const mpz_class prime(decimal);
            assert(mpz_sizeinbase(prime.get_mpz_t(), 2) == 4096);
            assert(ppf_testar_primalidade_gigante(decimal.c_str(), 25));
        }
        for (int bits : {8192, 8193}) {
            assert(ppf_iniciar_primos_aleatorios_job(bits, 50, argv[1]));
            ppf_cancelar_job();
            assert(ppf_geracao_job_concluido());
            const auto stopped = copy_report(ppf_obter_resultado_job());
            assert(stopped.find("Bits: " + std::to_string(bits) + "\n") != std::string::npos);
            assert(stopped.find("RESULTADO PARCIAL") != std::string::npos);
        }
    }
    const auto mersenne5 = copy_report(ppf_mersenne_report(5, false));
    assert(mersenne5.find("M(5) = 2^5 - 1\n\n31") != std::string::npos);
    const auto mersenne4 = copy_report(ppf_mersenne_report(4, false));
    assert(mersenne4.find("M(4) = 2^4 - 1\n\n15") != std::string::npos);
    assert(mersenne4.find("Composto: o expoente 4") != std::string::npos);
    const auto mersenne11 = copy_report(ppf_mersenne_report(11, false));
    assert(mersenne11.find("2047\n\nD\u00edgitos: 4\nComposto.") != std::string::npos);
    assert(copy_report(ppf_mersenne_report(127, false)).find("teste de Lucas-Lehmer") != std::string::npos);
    mpz_class largeMersenne;
    mpz_ui_pow_ui(largeMersenne.get_mpz_t(), 2, 44497);
    --largeMersenne;
    const auto mersenneLarge = copy_report(ppf_mersenne_report(44497, false));
    assert(mersenneLarge.find(largeMersenne.get_str()) != std::string::npos);
    assert(mersenneLarge.find("D\u00edgitos: 13395") != std::string::npos);
    assert(mersenneLarge.find("Primo conhecido") != std::string::npos);
    const auto reference = copy_report(ppf_mersenne_report(136279841, false));
    assert(reference.find("D\u00edgitos: 41024320") != std::string::npos);
    assert(reference.find("Primo conhecido") != std::string::npos);
    assert(copy_report(ppf_mersenne_report(100003, false)).find("Primalidade n\u00e3o determinada") != std::string::npos);
    const auto listed = copy_report(ppf_mersenne_report(13, true));
    assert(listed.find("Resultados: 5") != std::string::npos);
    assert(listed.find("M(11)") == std::string::npos);

    assert(copy_report(ppf_legendre_report_decimal("3", "3")).find("11, 13") != std::string::npos);
    assert(copy_report(ppf_legendre_report_decimal("100", "200")).find("Pr\u00f3ximo n: 200") != std::string::npos);
    assert(copy_report(ppf_legendre_report_decimal("9", "8")).empty());
    const mpz_class n("4294967297"), following = n + 1;
    const auto text = copy_report(ppf_legendre_witness_decimal("4294967297"));
    assert(!text.empty());
    const mpz_class candidate(text);
    assert(candidate > n * n && candidate < following * following);
    assert(mpz_probab_prime_p(candidate.get_mpz_t(), 25) > 0);
    assert(copy_report(ppf_legendre_report_decimal("4294967297", "4294967297"))
        .find("Prov\u00e1vel primo encontrado: " + text) != std::string::npos);

    assert(copy_report(ppf_factor_report("0")).empty());
    assert(copy_report(ppf_factor_report("360")).find("Fatores primos: 2^3 x 3^2 x 5") != std::string::npos);
    assert(copy_report(ppf_factor_report("221")).find("Fatores primos: 13 x 17") != std::string::npos);
    const auto partial = copy_report(ppf_factor_report("171672454111613454817272489449327062678543"));
    assert(partial.find("Fatores primos encontrados: 1009") != std::string::npos);
    assert(partial.find("Cofator restante: 170141183460469231731687303715884105727") != std::string::npos);

    const auto perfect = copy_report(ppf_perfect_report(PPF_PERFECT_VERIFY, 28));
    assert(perfect.find("Divisores pr\u00f3prios: 1, 2, 4, 7, 14") != std::string::npos);
    assert(perfect.find("Soma dos divisores pr\u00f3prios: 28") != std::string::npos);
    const auto deficient = copy_report(ppf_perfect_report(PPF_PERFECT_VERIFY, 27));
    assert(deficient.find("deficiente") != std::string::npos);
    assert(copy_report(ppf_perfect_verify_decimal("2305843008139952128")).find("par conhecido") != std::string::npos);
    assert(copy_report(ppf_perfect_verify_decimal("1000000000001")).find("inconclusivo") != std::string::npos);
    assert(copy_report(ppf_perfect_verify_decimal("1000000000002")).find("n\u00e3o tem a forma") != std::string::npos);
    assert(copy_report(ppf_perfect_verify_decimal("-1")).empty());
    const auto firstOdd = ppf_scan_odd_perfect(2, 50, 2);
    assert(firstOdd.tested == 2 && firstOdd.next == 7 && firstOdd.found == 0);
    const auto resumedOdd = ppf_scan_odd_perfect(firstOdd.next, 15, 64);
    assert(resumedOdd.tested == 5 && resumedOdd.next == 17 && resumedOdd.found == 0);
    const auto lastOdd = ppf_scan_odd_perfect(999999999997LL, 999999999999LL, 64);
    assert(lastOdd.tested == 2 && lastOdd.next == 1000000000001LL && lastOdd.found == 0);
    assert(ppf_scan_odd_perfect(999999999999LL, 1000000000001LL, 64).next == 0);
    assert(copy_report(ppf_perfect_report(PPF_PERFECT_VERIFY, ppf_perfect_verify_max() + 1)).empty());
    const auto bounded = copy_report(ppf_perfect_report(PPF_PERFECT_BY_LIMIT, 500));
    assert(bounded.find("\n496\n") != std::string::npos);
    assert(bounded.find("Resultados: 3") != std::string::npos);
    const auto digits = copy_report(ppf_perfect_report(PPF_PERFECT_BY_DIGITS, 2));
    assert(digits.find("\n28\n") != std::string::npos);
    assert(digits.find("Resultados: 1") != std::string::npos);
}
