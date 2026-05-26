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

## App Store Connect (antes do 1º upload)

- [ ] App criado com bundle `com.seuprojeto.primeprofast`
- [ ] Ícone 1024×1024 em `ios/PrimeProFast/PrimeProFast/Assets.xcassets/AppIcon.appiconset/`

## Gerar base64 no Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ios-prepare-github-secrets.ps1
```

## Rodar CI

**Actions** → **iOS App Store** → **Run workflow**
