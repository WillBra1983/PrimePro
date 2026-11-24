import Foundation

enum PPFNative {
    private static let nativeQueue = DispatchQueue(label: "com.seuprojeto.primeprofast.native", qos: .userInitiated)

    static func calcularPrimos(_ n: Int64) -> String {
        withCStringResult { ppf_calcular_primos(n) }
    }

    static func gerarPrimosGrandes(bits: Int32, quantidade: Int32, arquivo: String?, salvar: Bool) -> String {
        if let arquivo {
            return arquivo.withCString { path in
                withCStringResult { ppf_gerar_primos_grandes(bits, quantidade, path, salvar) }
            }
        }
        return withCStringResult { ppf_gerar_primos_grandes(bits, quantidade, nil, salvar) }
    }

    static func superPrimos(bits: Int32, quantidade: Int32, threads: Int32, caminho: String) -> String {
        caminho.withCString { path in
            withCStringResult { ppf_super_primos_nativo(bits, quantidade, threads, path) }
        }
    }

    static func testarPrimalidade(_ numero: String, repeticoes: Int32 = 25) -> Bool {
        numero.withCString { ptr in
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
        shouldCancel: @escaping () -> Bool
    ) -> String {
        let path = caminho.path
        iniciarJob(
            bits: Int32(bits),
            quantidade: Int32(quantidade),
            threads: Int32(max(1, threads)),
            caminho: path
        )
        while !jobConcluido() {
            if shouldCancel() {
                cancelarJob()
                return "Operação cancelada."
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
