import CryptoKit
import Security
import SwiftUI

private enum RSADemo: String {
    case keys = "Gerar chaves RSA"
    case encryption = "Criptografar e descriptografar"
    case signature = "Assinar e verificar"

    var reportPrefix: String {
        switch self {
        case .keys: return "seguranca_digital_rsa"
        case .encryption: return "criptografia_rsa"
        case .signature: return "assinatura_rsa"
        }
    }
}

private struct RSADemoError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

private struct RSAKeyPair {
    let bits: Int
    let privateKey: SecKey
    let publicKey: SecKey

    init(bits: Int) throws {
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeySizeInBits as String: bits,
            kSecAttrIsPermanent as String: false
        ]
        var error: Unmanaged<CFError>?
        guard let key = SecKeyCreateRandomKey(attributes as CFDictionary, &error) else {
            throw Self.failure(error, "Falha ao gerar a chave RSA.")
        }
        guard let publicKey = SecKeyCopyPublicKey(key) else {
            throw RSADemoError(message: "Falha ao obter a chave pública.")
        }
        self.bits = bits
        self.privateKey = key
        self.publicKey = publicKey
    }

    func report(for demo: RSADemo, message: Data) throws -> String {
        var error: Unmanaged<CFError>?
        switch demo {
        case .keys:
            guard let publicData = SecKeyCopyExternalRepresentation(publicKey, &error) else {
                throw Self.failure(error, "Falha ao exportar a chave pública.")
            }
            return """
            PAR DE CHAVES RSA

            Tamanho do módulo: \(bits) bits
            Chave pública (DER PKCS #1 em Base64):
            \((publicData as Data).base64EncodedString())

            Chave privada disponível durante esta sessão.
            """
        case .encryption:
            let algorithm: SecKeyAlgorithm = .rsaEncryptionOAEPSHA256
            let limit = SecKeyGetBlockSize(publicKey) - 2 * SHA256.Digest.byteCount - 2
            guard (1...limit).contains(message.count) else {
                throw RSADemoError(message: "Para RSA de \(bits) bits, informe uma mensagem de até \(limit) bytes UTF-8.")
            }
            guard let encrypted = SecKeyCreateEncryptedData(publicKey, algorithm, message as CFData, &error) else {
                throw Self.failure(error, "Falha ao criptografar.")
            }
            guard let decrypted = SecKeyCreateDecryptedData(privateKey, algorithm, encrypted, &error) else {
                throw Self.failure(error, "Falha ao descriptografar.")
            }
            guard decrypted as Data == message else {
                throw RSADemoError(message: "Os bytes recuperados diferem da entrada.")
            }
            return """
            CRIPTOGRAFIA RSA-OAEP COM SHA-256

            Conteúdo cifrado (Base64):
            \((encrypted as Data).base64EncodedString())

            Verificação: os \(message.count) bytes recuperados são idênticos aos originais.
            """
        case .signature:
            let algorithm: SecKeyAlgorithm = .rsaSignatureMessagePSSSHA256
            guard let signature = SecKeyCreateSignature(privateKey, algorithm, message as CFData, &error) else {
                throw Self.failure(error, "Falha ao assinar.")
            }
            guard SecKeyVerifySignature(publicKey, algorithm, message as CFData, signature, &error) else {
                throw Self.failure(error, "Falha ao verificar a assinatura original.")
            }
            var modified = message
            modified.append(0)
            let acceptsModified = SecKeyVerifySignature(publicKey, algorithm, modified as CFData, signature, nil)
            guard !acceptsModified else {
                throw RSADemoError(message: "A verificação aceitou os dados alterados.")
            }
            return """
            ASSINATURA RSA-PSS COM SHA-256

            Assinatura (Base64):
            \((signature as Data).base64EncodedString())

            Mensagem original: assinatura válida.
            Mensagem alterada: assinatura rejeitada.
            """
        }
    }

    private static func failure(_ error: Unmanaged<CFError>?, _ fallback: String) -> RSADemoError {
        guard let error else { return RSADemoError(message: fallback) }
        let description = CFErrorCopyDescription(error.takeRetainedValue()) as String
        return RSADemoError(message: description)
    }
}

struct SecurityView: View {
    @State private var bits = "2048"
    @State private var message = ""
    @State private var demoBits = 2048
    @State private var keyPair: RSAKeyPair?
    @State private var primeJob: PPFNative.Cancellation?
    @EnvironmentObject private var appState: AppState

    var body: some View {
        FeatureScaffold(
            title: "Segurança Digital",
            subtitle: "Chaves RSA, assinatura digital e hashes."
        ) { result, loading in
            Picker("Tamanho da chave RSA", selection: $demoBits) {
                ForEach([2048, 3072, 4096], id: \.self) { size in
                    Text("\(size) bits").tag(size)
                }
            }
            .pickerStyle(.segmented)
            .disabled(loading.wrappedValue)
            rsaButton(.keys, result: result, loading: loading)

            TextField("Mensagem de exemplo", text: $message, axis: .vertical)
                .textFieldStyle(.roundedBorder)
            rsaButton(.encryption, result: result, loading: loading)
            rsaButton(.signature, result: result, loading: loading)
            Button("Demonstrar hash criptográfico") {
                showHashes(result: result)
            }
            .buttonStyle(.bordered)
            .disabled(loading.wrappedValue || message.isEmpty)

            Divider()
            TextField("Tamanho para o par de primos", text: $bits)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button("Gerar par de primos (nativo)") {
                Task { await gerarRSA(result: result, loading: loading) }
            }
            .buttonStyle(.borderedProminent)
            .disabled(loading.wrappedValue)
            if let primeJob {
                Button("Cancelar geração") { primeJob.request() }
                    .buttonStyle(.bordered)
            }
        }
        .onDisappear { primeJob?.request() }
    }

    private func rsaButton(_ demo: RSADemo, result: Binding<String>, loading: Binding<Bool>) -> some View {
        Button(demo.rawValue) {
            Task { await runDemo(demo, result: result, loading: loading) }
        }
        .buttonStyle(.bordered)
        .disabled(loading.wrappedValue || (demo != .keys && message.isEmpty))
    }

    @MainActor
    private func runDemo(_ demo: RSADemo, result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        let data = Data(message.utf8)
        let size = demoBits
        let existing = demo != .keys && keyPair?.bits == size ? keyPair : nil
        loading.wrappedValue = true
        result.wrappedValue = "Processando RSA…"
        defer { loading.wrappedValue = false }

        let output: Result<(RSAKeyPair, String), Error> = await PPFNative.run {
            Result {
                let pair = try existing ?? RSAKeyPair(bits: size)
                return (pair, try pair.report(for: demo, message: data))
            }
        }
        switch output {
        case let .success((pair, report)):
            keyPair = pair
            appState.saveTemporaryResultAndOpenViewer(report, prefix: demo.reportPrefix, statusMessage: result)
        case let .failure(error):
            result.wrappedValue = error.localizedDescription
        }
    }

    private func showHashes(result: Binding<String>) {
        let data = Data(message.utf8)
        let modified = Data((message + "!").utf8)
        func hex<D: Digest>(_ digest: D) -> String {
            digest.map { String(format: "%02x", $0) }.joined()
        }

        let originalHash = hex(SHA256.hash(data: data))
        let modifiedHash = hex(SHA256.hash(data: modified))
        let different = zip(originalHash.utf8, modifiedHash.utf8).filter { $0.0 != $0.1 }.count
        let percentage = different * 100 / originalHash.utf8.count
        let report = """
        HASHES CRIPTOGRÁFICOS

        MD5 (legado): \(hex(Insecure.MD5.hash(data: data)))
        SHA-1 (legado): \(hex(Insecure.SHA1.hash(data: data)))
        SHA-256: \(originalHash)
        SHA-512: \(hex(SHA512.hash(data: data)))

        SHA-256 após acrescentar "!":
        \(modifiedHash)
        Diferença nos caracteres hexadecimais: \(percentage)%

        MD5 e SHA-1 são apresentados apenas para comparação histórica.
        """
        appState.saveTemporaryResultAndOpenViewer(report, prefix: "seguranca_hash", statusMessage: result)
    }

    @MainActor
    private func gerarRSA(result: Binding<String>, loading: Binding<Bool>) async {
        guard !loading.wrappedValue else { return }
        guard let b = Int(bits), b >= 512, b <= 16384 else {
            result.wrappedValue = "Use entre 512 e 16384 bits."
            return
        }
        let half = b / 2
        let token = PPFNative.Cancellation()
        primeJob = token
        loading.wrappedValue = true
        result.wrappedValue = "⏳ Gerando dois primos de ~\(half) bits…"

        let report = await PPFNative.run {
            let file: URL
            do {
                file = try PPFResultFiles.createTempGiganticPrimesFile(prefix: "primos_nativo_rsa_")
            } catch {
                return "Erro ao criar arquivo: \(error.localizedDescription)"
            }
            let relatorio = PPFNative.executarJob(
                bits: half,
                quantidade: 2,
                threads: ProcessInfo.processInfo.processorCount,
                caminho: file,
                onStatus: { status in
                    Task { @MainActor in
                        if primeJob === token && !token.isRequested {
                            result.wrappedValue = status
                        }
                    }
                },
                shouldCancel: { token.isRequested }
            )
            guard !token.isRequested else { return relatorio }
            return PPFResultFiles.appendFullDecimals(from: file, to: relatorio)
        }

        await MainActor.run {
            primeJob = nil
            loading.wrappedValue = false
            if token.isRequested {
                result.wrappedValue = "Operação cancelada."
                return
            }
            appState.saveTemporaryResultAndOpenViewer(
                report,
                prefix: "seguranca_digital_rsa",
                statusMessage: result
            )
        }
    }
}
