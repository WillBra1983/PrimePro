import SwiftUI

@MainActor
extension AppState {
  /// Espelha `salvarResultadoTemporario` + `openFileWithHtmlViewer`.
  func saveTemporaryResultAndOpenViewer(
    _ content: String,
    prefix: String,
    statusMessage: Binding<String>? = nil
  ) {
    do {
      let url = try PPFResultFiles.writeTemporaryResult(content, prefix: prefix)
      trackTemporaryResult(url)
      statusMessage?.wrappedValue = "Resultado gerado com sucesso! Abrindo visualizador..."
      presentDocumentPreview(url, title: PPFResultFiles.titleForPrefix(prefix), text: content)
    } catch {
      statusMessage?.wrappedValue = "Erro ao salvar arquivo temporário: \(error.localizedDescription)"
    }
  }

  /// Abre arquivo já gravado (ex.: primos_temp_… do intervalo).
  func openExistingResultViewer(
    url: URL,
    title: String,
    text: String,
    statusMessage: Binding<String>? = nil
  ) {
    trackTemporaryResult(url)
    statusMessage?.wrappedValue = "Resultado gerado com sucesso! Abrindo visualizador..."
    presentDocumentPreview(url, title: title, text: text)
  }

  private func presentDocumentPreview(_ url: URL, title: String, text: String) {
    documentReport = PPFReport(url: url, title: title, text: text)
    showDocumentPreview = true
  }

  func trackTemporaryResult(_ url: URL) {
    if !temporaryResultFiles.contains(url) {
      temporaryResultFiles.append(url)
    }
  }
}
