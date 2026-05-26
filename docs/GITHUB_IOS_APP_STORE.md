# iOS na App Store sem Mac — GitHub Actions

O workflow **iOS App Store** (`.github/workflows/ios-appstore-release.yml`) compila o app **nativo** (SwiftUI + C++/GMP), gera o **IPA** e envia ao **TestFlight**.

Configure no **Windows** (portal Apple + GitHub Secrets). Não precisa de Xcode no PC.

---

## Visão geral

1. Criar certificado **Apple Distribution** + perfil **App Store** para `com.seuprojeto.primeprofast`.
2. Criar **chave de API** no App Store Connect.
3. Colar secrets no GitHub.
4. **Actions** → **iOS App Store** → **Run workflow**.

---

## 1. Team ID

Secret: `APPLE_TEAM_ID` = seu Team ID (ex.: `BDAN6452VU` — mesma conta da Bíblia DC, se for o mesmo desenvolvedor).

---

## 2. Certificado Distribution (.p12)

```powershell
powershell -ExecutionPolicy Bypass -File scripts/apple-gerar-csr.ps1
```

No [developer.apple.com](https://developer.apple.com/account): **Certificados** → **Apple Distribution** → enviar CSR → baixar `.cer`.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/apple-cer-para-p12.ps1
```

Secrets:

| Secret | Valor |
|--------|--------|
| `APPLE_CERTIFICATE_BASE64` | `.p12` em base64 (uma linha) |
| `APPLE_CERTIFICATE_PASSWORD` | senha do `.p12` |

---

## 3. Perfil App Store

1. **Identificadores** → registrar `com.seuprojeto.primeprofast` (se ainda não existir).
2. **Perfis** → **App Store Connect** → app acima → certificado Distribution.
3. Baixar `.mobileprovision`.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/apple-mobileprovision-base64.ps1
```

Secret: `APPLE_PROVISION_PROFILE_BASE64`

---

## 4. Chave API App Store Connect

[appstoreconnect.apple.com](https://appstoreconnect.apple.com) → **Integrações** → **Chaves de API**

| Secret | Valor |
|--------|--------|
| `APPSTORE_ISSUER_ID` | Issuer ID |
| `APPSTORE_API_KEY_ID` | Key ID |
| `APPSTORE_API_PRIVATE_KEY` | conteúdo do `.p8` |

---

## 5. Outros secrets

| Secret | Exemplo |
|--------|---------|
| `KEYCHAIN_PASSWORD` | senha aleatória (só no runner CI) |

**Não** são necessários secrets Firebase/Vite (diferente do projeto Salvation).

Atalho no Windows (lista + base64 interativo):

```powershell
npm run ios:github-secrets
```

Checklist: [.github/APPLE_SECRETS_CHECKLIST.md](../.github/APPLE_SECRETS_CHECKLIST.md)

---

## 6. App Store Connect — criar o app

1. **Apps** → **+** → **Novo app** → iOS.
2. **Bundle ID:** `com.seuprojeto.primeprofast`.
3. **SKU:** ex. `primeprofast-ios-2026`.

Metadados: ver [APP_STORE_PUBLICACAO.md](../APP_STORE_PUBLICACAO.md).

---

## 7. Rodar o workflow

1. Push com pasta `.github/workflows/`.
2. GitHub → **Actions** → **iOS App Store** → **Run workflow**.
3. Artefato: `primeprofast-ios-ipa`.
4. TestFlight em ~15–30 min.

---

## Build local (com Mac)

```bash
bash scripts/build-gmp-ios.sh
bash scripts/build-ios-native.sh
open ios/PrimeProFast/PrimeProFast.xcodeproj
```

---

## Problemas comuns

| Erro | Solução |
|------|---------|
| `GMP iOS não encontrado` | Rode `scripts/build-gmp-ios.sh` no Mac |
| `libppf_core.a` não encontrado | Rode `scripts/build-ios-native.sh` antes do Xcode |
| *No suitable application records* | Crie o app no App Store Connect (etapa 6) |
| Signing / profile | Perfil App Store + UUID no `ExportOptions-appstore.plist` |

---

*Bundle: com.seuprojeto.primeprofast · Engine: C++/GMP (Algoritmo Especializado)*
