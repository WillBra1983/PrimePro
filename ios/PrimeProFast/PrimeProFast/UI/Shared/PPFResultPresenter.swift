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
      presentDocumentPreview(url)
    } catch {
      statusMessage?.wrappedValue = "Erro ao salvar arquivo temporário: \(error.localizedDescription)"
    }
  }

  /// Abre arquivo já gravado (ex.: primos_temp_… do intervalo).
  func openExistingResultViewer(
    url: URL,
    statusMessage: Binding<String>? = nil
  ) {
    trackTemporaryResult(url)
    statusMessage?.wrappedValue = "Resultado gerado com sucesso! Abrindo visualizador..."
    presentDocumentPreview(url)
  }

  func presentDocumentPreview(_ url: URL) {
    documentPreviewURL = url
    showDocumentPreview = true
  }

  func trackTemporaryResult(_ url: URL) {
    if !temporaryResultFiles.contains(url) {
      temporaryResultFiles.append(url)
    }
  }
}
