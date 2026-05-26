# Primeiro push para o GitHub (habilitar iOS CI)

O repositório local já tem `git init`. Falta criar o remoto e enviar o código.

## 1. Criar repositório vazio no GitHub

Repositório: **[WillBra1983/PrimePro](https://github.com/WillBra1983/PrimePro)** (vazio, sem README).

## 2. No PowerShell (raiz do projeto)

```powershell
cd "c:\Users\Pr Wilson Lucas\Desktop\PrimeProFastNative"

git add .
git status

git commit -m "PrimeProFast: Android nativo + iOS (SwiftUI/C++) e CI App Store"

git branch -M main
git remote add origin https://github.com/WillBra1983/PrimePro.git
git push -u origin main
```

Substitua `SEU_USUARIO` pelo seu login GitHub.

## 3. Secrets e workflow

1. `npm run ios:github-secrets` (ou siga [GITHUB_IOS_APP_STORE.md](GITHUB_IOS_APP_STORE.md))
2. Marque [.github/APPLE_SECRETS_CHECKLIST.md](../.github/APPLE_SECRETS_CHECKLIST.md)
3. **Actions** → **iOS App Store** → **Run workflow**
