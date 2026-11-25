import SwiftUI
import UIKit
import WebKit

struct PPFReportScreen: View {
    let report: PPFReport
    @Environment(\.dismiss) private var dismiss
    @State private var pdfURL: URL?
    @State private var export: PPFReportExport?
    @State private var preparing = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            PPFDocumentPreview(url: report.url)
                .navigationTitle(report.title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        if preparing {
                            ProgressView().accessibilityLabel("Preparando PDF")
                        } else {
                            Menu {
                                Button {
                                    preparePDF(save: true)
                                } label: {
                                    Label("Salvar PDF", systemImage: "folder")
                                }
                                Button {
                                    preparePDF(save: false)
                                } label: {
                                    Label("Compartilhar PDF", systemImage: "square.and.arrow.up")
                                }
                            } label: {
                                Label("PDF", systemImage: "doc")
                            }
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Fechar") { dismiss() }
                            .disabled(preparing)
                    }
                }
        }
        .sheet(item: $export) { item in
            PPFReportExportSheet(export: item) { export = nil }
        }
        .alert("Falha ao preparar PDF", isPresented: Binding(
            get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func preparePDF(save: Bool) {
        if let pdfURL {
            export = PPFReportExport(url: pdfURL, save: save)
            return
        }
        preparing = true
        let source = report
        Task { @MainActor in
            defer { preparing = false }
            do {
                let url = try await Task.detached(priority: .userInitiated) {
                    try PPFResultFiles.writePDF(source)
                }.value
                pdfURL = url
                export = PPFReportExport(url: url, save: save)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private struct PPFReportExport: Identifiable {
    let id = UUID()
    let url: URL
    let save: Bool
}

private struct PPFReportExportSheet: UIViewControllerRepresentable {
    let export: PPFReportExport
    let onFinish: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        if export.save {
            let picker = UIDocumentPickerViewController(forExporting: [export.url], asCopy: true)
            picker.delegate = context.coordinator
            return picker
        }
        let share = UIActivityViewController(activityItems: [export.url], applicationActivities: nil)
        share.completionWithItemsHandler = { _, _, _, _ in onFinish() }
        return share
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onFinish: onFinish) }

    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        let onFinish: () -> Void

        init(onFinish: @escaping () -> Void) { self.onFinish = onFinish }

        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) { onFinish() }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            onFinish()
        }
    }
}

/// Renderiza o arquivo inteiro. O Quick Look pode limitar resultados muito extensos.
struct PPFDocumentPreview: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = UIViewController()
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.isOpaque = false
        webView.backgroundColor = .systemBackground
        controller.view.addSubview(webView)
        NSLayoutConstraint.activate([
            webView.leadingAnchor.constraint(equalTo: controller.view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: controller.view.trailingAnchor),
            webView.topAnchor.constraint(equalTo: controller.view.topAnchor),
            webView.bottomAnchor.constraint(equalTo: controller.view.bottomAnchor)
        ])
        context.coordinator.webView = webView
        context.coordinator.load(url)
        return controller
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {
        context.coordinator.load(url)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    final class Coordinator: NSObject {
        weak var webView: WKWebView?
        private var loadedURL: URL?

        func load(_ url: URL) {
            guard loadedURL != url, let webView else { return }
            loadedURL = url
            webView.loadFileURL(
                url,
                allowingReadAccessTo: url.deletingLastPathComponent()
            )
        }
    }
}
