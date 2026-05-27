# Primeiro push para o GitHub (habilitar iOS CI)

O repositório local já tem `git init`. Falta criar o remoto e enviar o código.

## Repositório

**[WillBra1983/PrimePro](https://github.com/WillBra1983/PrimePro)**

O commit inicial já foi feito localmente. Falta apenas o **push** (login GitHub).

## Push (PowerShell na raiz do projeto)

```powershell
cd C:\PrimeProFastNative

git remote -v
# deve mostrar origin → https://github.com/WillBra1983/PrimePro.git

git push -u origin main
```

Se pedir login: use **GitHub CLI** (`gh auth login`) ou o gerenciador de credenciais do Git for Windows.

## 3. Secrets e workflow

1. `npm run ios:github-secrets` (ou siga [GITHUB_IOS_APP_STORE.md](GITHUB_IOS_APP_STORE.md))
2. Marque [.github/APPLE_SECRETS_CHECKLIST.md](../.github/APPLE_SECRETS_CHECKLIST.md)
3. **Actions** → **iOS App Store** → **Run workflow**
