# PrimeProFast (FastNative) - Calculadora de Números Primos

Repositório: [github.com/WillBra1983/PrimePro](https://github.com/WillBra1983/PrimePro)

## Visão geral

O **PrimeProFast** é um aplicativo **nativo** (Android e iOS) para análise de números primos e teoria dos números. No Android, a interface é Java (Views); no iOS, SwiftUI. Os cálculos pesados usam o **mesmo núcleo C++/GMP** (algoritmo especializado), via JNI no Android e bridge C no iOS.

Não há versão web nem Capacitor — builds oficiais: `app/` (Android) e `ios/PrimeProFast/` (iOS).

## Estrutura do projeto

```
PrimeProFastNative/
├── app/                    # App Android nativo
│   └── src/main/cpp/       # Núcleo C++ (ppf_engine + JNI)
├── ios/PrimeProFast/       # App iOS (SwiftUI + mesmo núcleo)
├── scripts/                # build-gmp-ios.sh, build-ios-native.sh, Apple CSR
├── .github/workflows/      # ios-appstore-release.yml
├── build.gradle
└── README.md
```

## Como construir o APK

Sempre na **raiz** do projeto (`c:\PrimeProFastNative`):

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
```

APK gerado:

- `app\build\outputs\apk\debug\app-debug.apk`

## Funcionalidades principais (app nativo)

- **Tutorial** e **Teste de Primalidade**
- **Primos por intervalo** (engine nativa)
- **Primos especiais** (Legendre, Mersenne, etc.)
- **Geração de primos grandes** (bits configuráveis, multithread)
- Exportação de resultados, tema claro/escuro, limites gratuitos/premium

## Tecnologias

- **Android:** Java, AppCompat, Views nativas, NDK/JNI
- **iOS:** SwiftUI, Objective-C++ bridge, GMP estático
- **Núcleo:** C++ (`ppf_engine.cpp`) compartilhado
- **Build:** Gradle 8.x (Android) · Xcode 16+ / CMake (iOS)

## Apple App Store (iOS)

App iOS nativo em `ios/PrimeProFast/` — **mesmo núcleo C++/GMP** do Android (algoritmo especializado).

| Documento | Conteúdo |
|-----------|----------|
| [APP_STORE_PUBLICACAO.md](APP_STORE_PUBLICACAO.md) | Visão geral e metadados |
| [docs/GITHUB_IOS_APP_STORE.md](docs/GITHUB_IOS_APP_STORE.md) | CI sem Mac (GitHub Actions) |

**Sem Mac:** use GitHub Actions — [docs/GITHUB_IOS_APP_STORE.md](docs/GITHUB_IOS_APP_STORE.md) · primeiro push: [docs/GIT_GITHUB_PRIMEIRO_PUSH.md](docs/GIT_GITHUB_PRIMEIRO_PUSH.md)

```powershell
npm run ios:github-secrets
```

Publicação Android: [GOOGLE_PLAY_PUBLISHING.md](GOOGLE_PLAY_PUBLISHING.md).

## Remoção da parte “web” e do Capacitor

A antiga interface em `www/` (HTML/JS) e o projeto **Capacitor** em `android/` foram descontinuados para evitar duas bases (web limitada + APK rápido). O repositório mantém apenas o app em `app/`.

Se ainda existir a pasta `android/` no seu disco (por exemplo, por falha de exclusão por caminhos longos no Windows), você pode removê-la manualmente:

1. Feche o Android Studio e qualquer processo que use a pasta `android`.
2. Exclua a pasta `c:\PrimeProFastNative\android` pelo Explorador de Arquivos ou, no PowerShell, com cuidado com caminhos longos (por exemplo, usar `robocopy` para “espelhar” uma pasta vazia e depois remover, ou habilitar suporte a caminhos longos no Windows).

---

*PrimeProFast* – uma única base, um único build, engine nativa.
