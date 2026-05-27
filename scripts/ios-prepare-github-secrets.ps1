# Gera base64 e checklist para GitHub Actions (iOS App Store - PrimeProFast).
# Uso: powershell -ExecutionPolicy Bypass -File scripts/ios-prepare-github-secrets.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

function Show-B64File {
  param(
    [string]$Label,
    [string]$Path
  )
  if (-not (Test-Path $Path)) {
    Write-Host "[!] $Label - arquivo nao encontrado: $Path" -ForegroundColor Yellow
    return $false
  }
  $bytes = [IO.File]::ReadAllBytes($Path)
  $b64 = [Convert]::ToBase64String($bytes)
  Write-Host ""
  Write-Host "=== $Label ===" -ForegroundColor Cyan
  Write-Host "Arquivo: $Path"
  Write-Host "GitHub: Settings > Secrets and variables > Actions > New repository secret"
  Write-Host "Cole o valor em UMA linha (sem quebras):"
  Write-Host ""
  Write-Host $b64
  $clip = Read-Host "Copiar base64 para a area de transferencia? [S/n]"
  if ($clip -ne 'n' -and $clip -ne 'N') {
    $b64 | Set-Clipboard
    Write-Host "Copiado." -ForegroundColor Green
  }
  return $true
}

Write-Host "PrimeProFast - secrets GitHub (iOS App Store)" -ForegroundColor Green
Write-Host ""
Write-Host "Bundle: com.seuprojeto.primeprofast"
Write-Host "Guia: docs\GITHUB_IOS_APP_STORE.md"
Write-Host ""

$p12 = Read-Host "Caminho do certificado .p12 (Distribution) [Enter pula]"
if ($p12) { Show-B64File -Label 'APPLE_CERTIFICATE_BASE64' -Path $p12 | Out-Null }

$pp = Read-Host "Caminho do .mobileprovision App Store [Enter pula]"
if ($pp) { Show-B64File -Label 'APPLE_PROVISION_PROFILE_BASE64' -Path $pp | Out-Null }

Write-Host ""
Write-Host "=== Secrets de texto (New repository secret no GitHub) ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "  APPLE_TEAM_ID"
Write-Host "    developer.apple.com > Membership"
Write-Host ""
Write-Host "  APPLE_CERTIFICATE_PASSWORD"
Write-Host "    senha do arquivo .p12"
Write-Host ""
Write-Host "  KEYCHAIN_PASSWORD"
Write-Host "    invente uma senha (ex. Ci-local-2026!) - so usada no runner CI"
Write-Host ""
Write-Host "  APPSTORE_ISSUER_ID"
Write-Host "    App Store Connect > Integracoes > Chaves de API (Issuer ID no topo)"
Write-Host ""
Write-Host "  APPSTORE_API_KEY_ID"
Write-Host "    Key ID da chave .p8"
Write-Host ""
Write-Host "  APPSTORE_API_PRIVATE_KEY"
Write-Host "    conteudo inteiro do arquivo .p8 (com BEGIN/END PRIVATE KEY)"
Write-Host ""

Write-Host "Reaproveitar da Biblia DC (mesma conta Apple):" -ForegroundColor Yellow
Write-Host "  APPLE_TEAM_ID, APPSTORE_* e opcionalmente o mesmo certificado .p12"
Write-Host "  O perfil .mobileprovision DEVE ser novo para com.seuprojeto.primeprofast"
Write-Host ""

Write-Host "Checklist: .github\APPLE_SECRETS_CHECKLIST.md"
Write-Host "Depois: Actions > iOS App Store > Run workflow"
Write-Host ""
