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
        "html"
    }

    static func escapeHTML(_ value: String) -> String {
        value
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
            .replacingOccurrences(of: "'", with: "&#39;")
    }

    static func titleForPrefix(_ prefix: String) -> String {
        if prefix.contains("primos_aleatorios") { return "Primos Aleatórios" }
        if prefix.contains("conjectura_legendre") { return "Conjectura de Legendre" }
        if prefix.contains("mersenne") { return "Números de Mersenne" }
        if prefix.contains("perfeitos") { return "Números Perfeitos" }
        if prefix.contains("seguranca") || prefix.contains("criptografia") || prefix.contains("hash") || prefix.contains("assinatura") { return "Segurança Digital" }
        if prefix.contains("estatisticas") || prefix.contains("aproximacao") { return "Estatísticas" }
        if prefix.contains("primalidade") { return "Teste de Primalidade" }
        if prefix.contains("intervalo") || prefix.contains("primos_temp") { return "Primos por Intervalo" }
        return "Resultado"
    }

    static func typeForPrefix(_ prefix: String) -> String {
        let normalized = prefix.replacingOccurrences(of: "_", with: " ").trimmingCharacters(in: .whitespacesAndNewlines)
        return normalized.isEmpty ? "Relatório gerado" : normalized
    }

    static func makeResultHTML(title: String, result: String, type: String) -> String {
        let generatedAtFormatter = DateFormatter()
        generatedAtFormatter.locale = Locale(identifier: "pt_BR")
        generatedAtFormatter.dateFormat = "dd/MM/yyyy HH:mm"
        let generatedAt = generatedAtFormatter.string(from: Date())
        let count = NumberFormatter.localizedString(from: NSNumber(value: result.count), number: .decimal)
        return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>\(escapeHTML(title)) - PrimeProFast</title>
          <style>
            :root { color-scheme: light dark; --bg:#f6f8fb; --bg2:#e8eff7; --surface:#ffffff; --surface-2:#f7fafc; --text:#142033; --muted:#5c697a; --border:#d6e1ec; --brand:#1d4e89; --accent:#2e7d6f; --code:#0f1720; --code-text:#e8edf3; }
            @media (prefers-color-scheme: dark) { :root { --bg:#0e131a; --bg2:#161e29; --surface:#1a222e; --surface-2:#202a39; --text:#eef3f8; --muted:#aab6c5; --border:#364458; --code:#0b1118; --code-text:#edf4fb; } }
            * { box-sizing: border-box; }
            body { margin:0; min-height:100vh; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif; color:var(--text); background:linear-gradient(180deg,var(--bg),var(--bg2)); }
            .progress { position:fixed; top:0; left:0; right:0; height:4px; z-index:10; }
            .progress span { display:block; width:0%; height:100%; background:linear-gradient(90deg,var(--brand),var(--accent)); }
            .shell { width:min(1100px,100%); margin:0 auto; padding:18px; }
            .hero { background:linear-gradient(135deg,var(--brand),#266276,var(--accent)); color:white; border-radius:8px; padding:22px; box-shadow:0 12px 28px rgba(15,23,42,.18); }
            .brand { font-size:.78rem; font-weight:700; letter-spacing:.08em; text-transform:uppercase; opacity:.82; }
            h1 { margin:.35rem 0 .25rem; font-size:clamp(1.45rem,4vw,2.4rem); line-height:1.08; }
            .subtitle { margin:0; color:rgba(255,255,255,.86); }
            .meta { display:flex; flex-wrap:wrap; gap:8px; margin-top:16px; }
            .chip { border:1px solid rgba(255,255,255,.28); background:rgba(255,255,255,.12); color:white; border-radius:999px; padding:7px 10px; font-size:.86rem; }
            .panel { margin-top:14px; border:1px solid var(--border); background:var(--surface); border-radius:8px; overflow:hidden; box-shadow:0 8px 20px rgba(15,23,42,.08); }
            .panel-head { display:flex; gap:12px; align-items:center; justify-content:space-between; padding:14px 16px; border-bottom:1px solid var(--border); background:var(--surface-2); }
            .panel-title { font-weight:700; }
            button { border:1px solid var(--border); background:var(--surface); color:var(--text); border-radius:8px; padding:9px 12px; font-weight:700; }
            pre { margin:0; padding:16px; background:var(--code); color:var(--code-text); overflow:auto; white-space:pre-wrap; overflow-wrap:anywhere; word-break:break-word; font:13px/1.55 'SFMono-Regular',Consolas,'Liberation Mono',monospace; }
            .footer { color:var(--muted); text-align:center; padding:16px 8px 4px; font-size:.86rem; }
            @media (max-width:600px) { .shell { padding:10px; } .hero { padding:18px; } .panel-head { align-items:flex-start; flex-direction:column; } button { width:100%; } pre { font-size:12px; padding:13px; } }
          </style>
        </head>
        <body>
          <div class="progress"><span id="progress"></span></div>
          <main class="shell">
            <header class="hero">
              <div class="brand">PrimeProFast</div>
              <h1>\(escapeHTML(title))</h1>
              <p class="subtitle">\(escapeHTML(type))</p>
              <div class="meta">
                <span class="chip">Gerado em \(escapeHTML(generatedAt))</span>
                <span class="chip">\(escapeHTML(count)) caracteres</span>
              </div>
            </header>
            <section class="panel">
              <div class="panel-head">
                <div class="panel-title">Resultado completo</div>
                <button type="button" onclick="copyResult()">Copiar resultado</button>
              </div>
              <pre id="raw">\(escapeHTML(result))</pre>
            </section>
            <div class="footer">PrimeProFast - visualização otimizada para leitura, seleção e compartilhamento</div>
          </main>
          <script>
            window.addEventListener('scroll', () => {
              const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
              const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
              const scrollPercent = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 100;
              document.getElementById('progress').style.width = scrollPercent + '%';
            });
            function copyResult() { const text = document.getElementById('raw').innerText; if (navigator.clipboard) { navigator.clipboard.writeText(text); } }
          </script>
        </body>
        </html>
        """
    }

    @discardableResult
    static func writeText(_ content: String, to url: URL) throws -> URL {
        try content.write(to: url, atomically: true, encoding: .utf8)
        return url
    }

    /// `salvarResultadoTemporario` — cache/temp_primos/{prefix}_{timestamp}.html
    static func writeTemporaryResult(_ content: String, prefix: String) throws -> URL {
        let ext = extensionForPrefix(prefix)
        let name = "\(prefix)_\(timestamp()).\(ext)"
        let url = try tempDirectory().appendingPathComponent(name)
        let finalContent: String
        if prefix.hasPrefix("tutorial_") {
            finalContent = content
        } else {
            finalContent = makeResultHTML(title: titleForPrefix(prefix), result: content, type: typeForPrefix(prefix))
        }
        return try writeText(finalContent, to: url)
    }

    /// Arquivo temporário para primos gigantes (job nativo / RSA).
    static func createTempGiganticPrimesFile(prefix: String = "primos_gigantes_") throws -> URL {
        let name = "\(prefix)\(timestamp()).txt"
        let url = try tempDirectory().appendingPathComponent(name)
        FileManager.default.createFile(atPath: url.path, contents: nil)
        return url
    }

    /// Botão “Calcular e Mostrar na Tela” — `primos_temp_{timestamp}.html`
    static func writeIntervalTempViewFile(_ content: String) throws -> URL {
        let name = "primos_temp_\(timestamp()).html"
        let url = try tempDirectory().appendingPathComponent(name)
        let finalContent = makeResultHTML(title: "Primos por Intervalo", result: content, type: "primos temp")
        return try writeText(finalContent, to: url)
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
