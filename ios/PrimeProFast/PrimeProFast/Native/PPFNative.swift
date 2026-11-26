import Foundation

enum PPFNative {
    final class Cancellation {
        private let lock = NSLock()
        private var requested = false

        var isRequested: Bool {
            lock.lock()
            defer { lock.unlock() }
            return requested
        }

        func request() {
            lock.lock()
            defer { lock.unlock() }
            requested = true
        }
    }

    static func evaluateInteger(_ expression: String) -> String? {
        guard expression.utf8.count <= 200_000, !expression.utf8.contains(0) else { return nil }
        var normalized = ""
        for scalar in expression.unicodeScalars {
            if (128...0xFFFF).contains(scalar.value), CharacterSet.decimalDigits.contains(scalar),
               let digit = Character(String(scalar)).wholeNumberValue {
                normalized.append(String(digit))
            } else {
                normalized.unicodeScalars.append(scalar)
            }
        }
        return normalized.withCString { pointer in
            takeOwnedString(ppf_evaluate_integer(pointer))
        }
    }

    static func smallFactors(_ number: String) -> String? {
        number.withCString { pointer in
            let report = withCStringResult { ppf_factor_report(pointer) }
            return report.isEmpty ? nil : report
        }
    }

    private static func takeOwnedString(_ pointer: UnsafeMutablePointer<CChar>?) -> String? {
        guard let pointer else { return nil }
        defer { ppf_free_integer(pointer) }
        return String(cString: pointer)
    }
    private static let nativeQueue = DispatchQueue(label: "com.seuprojeto.primeprofast.native", qos: .userInitiated)

    static func calcularPrimos(_ n: Int64) -> String {
        withCStringResult { ppf_calcular_primos(n) }
    }

    static func specialPrimes(family: Int32, limit: Int, count: Int) -> String {
        withCStringResult { ppf_special_primes(family, Int64(limit), Int32(clamping: count)) }
    }

    static func shortPrimes(bits: Int, count: Int) -> String {
        withCStringResult { ppf_short_primes(Int32(bits), Int32(count)) }
    }

    static func legendreReport(first: String, last: String) -> String? {
        let report = first.withCString { a in
            last.withCString { b in
                withCStringResult { ppf_legendre_report_decimal(a, b) }
            }
        }
        return report.isEmpty ? nil : report
    }

    static func legendreWitness(_ n: String) -> String {
        n.withCString { decimal in
            withCStringResult { ppf_legendre_witness_decimal(decimal) }
        }
    }

    static func primeStats(limit: Int) -> String {
        withCStringResult { ppf_prime_stats(Int64(limit)) }
    }

    static func primeApproximation(limit: String) -> String {
        limit.withCString { decimal in
            withCStringResult { ppf_prime_approximation(decimal) }
        }
    }

    static func mersenneReport(exponent: Int, list: Bool) -> String {
        withCStringResult { ppf_mersenne_report(Int32(exponent), list) }
    }

    static func perfectReport(mode: Int32, value: Int) -> String {
        withCStringResult { ppf_perfect_report(mode, Int64(value)) }
    }

    static func verifyPerfect(_ number: String) -> String {
        number.withCString { decimal in
            withCStringResult { ppf_perfect_verify_decimal(decimal) }
        }
    }

    static func superPrimos(bits: Int32, quantidade: Int32, threads: Int32, caminho: String) -> String {
        caminho.withCString { path in
            withCStringResult { ppf_super_primos_nativo(bits, quantidade, threads, path) }
        }
    }

    static func testarPrimalidade(_ numero: String, repeticoes: Int32 = 25) -> Bool {
        if let value = UInt64(numero) { return ppf_is_prime_u64(value) }
        return numero.withCString { ptr in
            ppf_testar_primalidade_gigante(ptr, repeticoes)
        }
    }

    static func iniciarJob(bits: Int32, quantidade: Int32, threads: Int32, caminho: String) {
        caminho.withCString { ptr in
            ppf_iniciar_geracao_job(bits, quantidade, threads, ptr)
        }
    }

    static func statusJob() -> String {
        withCStringResult { ppf_obter_status_job() }
    }

    static func jobConcluido() -> Bool {
        ppf_geracao_job_concluido()
    }

    static func resultadoJob() -> String {
        withCStringResult { ppf_obter_resultado_job() }
    }

    static func cancelarJob() {
        ppf_cancelar_job()
    }

    static func executarJob(
        bits: Int,
        quantidade: Int,
        threads: Int,
        caminho: URL,
        onStatus: @escaping (String) -> Void,
        shouldCancel: @escaping () -> Bool,
        randomCard: Bool = false
    ) -> String {
        guard !shouldCancel() else { return "Operação cancelada." }
        let path = caminho.path
        if randomCard {
            let started = path.withCString {
                ppf_iniciar_primos_aleatorios_job(Int32(bits), Int32(quantidade), $0)
            }
            guard started else { return "Não foi possível iniciar a geração." }
        } else {
            iniciarJob(
                bits: Int32(bits),
                quantidade: Int32(quantidade),
                threads: Int32(clamping: max(0, threads)),
                caminho: path
            )
        }
        while !jobConcluido() {
            if shouldCancel() {
                cancelarJob()
                return randomCard ? resultadoJob() : "Operação cancelada."
            }
            onStatus(statusJob())
            Thread.sleep(forTimeInterval: 0.25)
        }
        return resultadoJob()
    }

    static func run<T>(_ work: @escaping () -> T) async -> T {
        await withCheckedContinuation { continuation in
            nativeQueue.async {
                continuation.resume(returning: work())
            }
        }
    }

    private static func withCStringResult(_ block: () -> UnsafePointer<CChar>?) -> String {
        guard let c = block() else { return "" }
        defer { ppf_release_string(c) }
        return String(cString: c)
    }
}
