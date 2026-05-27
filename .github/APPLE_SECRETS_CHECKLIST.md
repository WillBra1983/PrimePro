# Checklist — GitHub Secrets (iOS PrimeProFast)

Marque ao configurar em **Settings → Secrets and variables → Actions**.

## Obrigatórios (build + assinatura)

- [ ] `APPLE_TEAM_ID`
- [ ] `APPLE_CERTIFICATE_BASE64` (`.p12` Distribution em base64, uma linha)
- [ ] `APPLE_CERTIFICATE_PASSWORD`
- [ ] `APPLE_PROVISION_PROFILE_BASE64` (perfil **App Store** para `com.seuprojeto.primeprofast`)
- [ ] `KEYCHAIN_PASSWORD` (qualquer senha forte; só usada no runner)

## TestFlight (opcional no workflow)

- [ ] `APPSTORE_ISSUER_ID`
- [ ] `APPSTORE_API_KEY_ID`
- [ ] `APPSTORE_API_PRIVATE_KEY` (arquivo `.p8` completo)

## App Store Connect (obrigatório antes do 1º upload TestFlight)

Sem isso o passo `upload-testflight-build` falha com: *Cannot determine the Apple ID from Bundle ID*.

- [ ] Em [developer.apple.com](https://developer.apple.com/account/resources/identifiers/list): identificador **App** `com.seuprojeto.primeprofast`
- [ ] Em [appstoreconnect.apple.com](https://appstoreconnect.apple.com) → **Apps** → **+** → **Novo app** → iOS → bundle `com.seuprojeto.primeprofast` (SKU ex.: `primeprofast-ios-2026`)
- [ ] Ícone 1024×1024 em `ios/PrimeProFast/PrimeProFast/Assets.xcassets/AppIcon.appiconset/`

## Gerar base64 no Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ios-prepare-github-secrets.ps1
```

## Rodar CI

**Actions** → **iOS App Store** → **Run workflow**
