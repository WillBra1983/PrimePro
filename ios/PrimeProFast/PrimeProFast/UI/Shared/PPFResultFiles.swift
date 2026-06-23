import Foundation

/// Armazenamento de resultados — espelha `temp_primos` e `Downloads/PrimeProFast` do Android.
enum PPFResultFiles {
    static let tempSubfolder = "temp_primos"
    static let permanentFolderName = "PrimeProFast"

    static func timestamp() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "yyyyMMdd_HHmmss"
        return f.string(from: Date())
    }

    static func tempDirectory() throws -> URL {
        let base = FileManager.default.temporaryDirectory
        let dir = base.appendingPathComponent(tempSubfolder, isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    /// Equivalente a `getPrimeProFastDirectory()` — pasta persistente visível no app Arquivos.
    static func permanentDirectory() throws -> URL {
        let docs = try FileManager.default.url(
            for: .documentDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let dir = docs.appendingPathComponent(permanentFolderName, isDirectory: true)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    static func extensionForPrefix(_ prefix: String) -> String {
        prefix.hasPrefix("tutorial_") ? "html" : "txt"
    }

    @discardableResult
    static func writeText(_ content: String, to url: URL) throws -> URL {
        try content.write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    /// `salvarResultadoTemporario` — cache/temp_primos/{prefix}_{timestamp}.txt|.html
    static func writeTemporaryResult(_ content: String, prefix: String) throws -> URL {
        let ext = extensionForPrefix(prefix)
        let name = "\(prefix)_\(timestamp()).\(ext)"
        let url = try tempDirectory().appendingPathComponent(name)
        return try writeText(content, to: url)
    }

    /// Arquivo temporário para primos gigantes (job nativo / RSA).
    static func createTempGiganticPrimesFile(prefix: String = "primos_gigantes_") throws -> URL {
        let name = "\(prefix)\(timestamp()).txt"
        let url = try tempDirectory().appendingPathComponent(name)
        FileManager.default.createFile(atPath: url.path, contents: nil)
        return url
    }

    /// Botão “Calcular e Mostrar na Tela” — `primos_temp_{timestamp}.txt`
    static func writeIntervalTempViewFile(_ content: String) throws -> URL {
        let name = "primos_temp_\(timestamp()).txt"
        let url = try tempDirectory().appendingPathComponent(name)
        return try writeText(content, to: url)
    }

    /// Botão “Calcular e Salvar em TXT” — `primos_{n}_{timestamp}.txt` permanente.
    static func writeIntervalPermanentFile(_ content: String, n: Int64) throws -> URL {
        let name = "primos_\(n)_\(timestamp()).txt"
        let url = try permanentDirectory().appendingPathComponent(name)
        return try writeText(content, to: url)
    }

    static func readUTF8(at url: URL) throws -> String {
        try String(contentsOf: url, encoding: .utf8)
    }

    /// Mesmo padrão do Android: relatório + bloco com decimais completos do arquivo.
    static func appendFullDecimals(from fileURL: URL, to report: String) -> String {
        var full = report
        full += "\n\n--- PRIMOS (DECIMAL COMPLETO) ---\n"
        do {
            full += try readUTF8(at: fileURL)
        } catch {
            full += "(erro ao ler arquivo: \(error.localizedDescription))\n"
        }
        return full
    }

    static func purgeOldTemporaryResults() {
        guard let dir = try? tempDirectory() else { return }
        guard let items = try? FileManager.default.contentsOfDirectory(
            at: dir,
            includingPropertiesForKeys: nil
        ) else { return }
        for item in items {
            try? FileManager.default.removeItem(at: item)
        }
    }
}
