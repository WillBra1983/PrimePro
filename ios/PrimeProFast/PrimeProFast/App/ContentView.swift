import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        NavigationStack {
            MenuView()
                .navigationTitle("PrimeProFast")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            appState.darkMode.toggle()
                        } label: {
                            Image(systemName: appState.darkMode ? "sun.max.fill" : "moon.fill")
                        }
                    }
                }
        }
        .preferredColorScheme(appState.darkMode ? .dark : .light)
        .fullScreenCover(isPresented: $appState.showDocumentPreview) {
            if let url = appState.documentPreviewURL {
                NavigationStack {
                    PPFDocumentPreview(url: url)
                        .ignoresSafeArea()
                        .toolbar {
                            ToolbarItem(placement: .topBarTrailing) {
                                Button("Fechar") {
                                    appState.showDocumentPreview = false
                                }
                            }
                        }
                }
            }
        }
    }
}
