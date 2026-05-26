# Gera base64 e checklist para GitHub Actions (iOS App Store — PrimeProFast).
# Uso: powershell -ExecutionPolicy Bypass -File scripts/ios-prepare-github-secrets.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

function Show-B64File($label, $path) {
  if (-not (Test-Path $path)) {
    Write-Host "○ $label — arquivo não encontrado: $path" -ForegroundColor Yellow
    return $false
  }
  $bytes = [IO.File]::ReadAllBytes($path)
  $b64 = [Convert]::ToBase64String($bytes)
  Write-Host "`n=== $label ===" -ForegroundColor Cyan
  Write-Host "Arquivo: $path"
  Write-Host "Secret no GitHub → Settings → Secrets → Actions"
  Write-Host "Cole o valor em UMA linha (sem quebras):`n"
  Write-Host $b64
  $clip = Read-Host 'Copiar base64 para a área de transferência? [S/n]'
  if ($clip -ne 'n' -and $clip -ne 'N') {
    $b64 | Set-Clipboard
    Write-Host 'Copiado.' -ForegroundColor Green
  }
  return $true
}

Write-Host "PrimeProFast — secrets GitHub (iOS App Store)`n" -ForegroundColor Green
Write-Host "Bundle: com.seuprojeto.primeprofast"
Write-Host "Guia: docs\GITHUB_IOS_APP_STORE.md`n"

$p12 = Read-Host 'Caminho do certificado .p12 (Distribution) [Enter pula]'
if ($p12) { Show-B64File 'APPLE_CERTIFICATE_BASE64' $p12 | Out-Null }

$pp = Read-Host 'Caminho do .mobileprovision App Store (com.seuprojeto.primeprofast) [Enter pula]'
if ($pp) { Show-B64File 'APPLE_PROVISION_PROFILE_BASE64' $pp | Out-Null }

Write-Host "`n=== Secrets de texto (criar manualmente no GitHub) ===" -ForegroundColor Cyan
$textSecrets = @(
  @{ Name = 'APPLE_TEAM_ID'; Hint = 'developer.apple.com → Membership' },
  @{ Name = 'APPLE_CERTIFICATE_PASSWORD'; Hint = 'senha do .p12' },
  @{ Name = 'KEYCHAIN_PASSWORD'; Hint = 'invente uma senha (ex. Ci-local-2026!) — só no runner CI' },
  @{ Name = 'APPSTORE_ISSUER_ID'; Hint = 'App Store Connect → Chaves de API' },
  @{ Name = 'APPSTORE_API_KEY_ID'; Hint = 'Key ID da chave .p8' },
  @{ Name = 'APPSTORE_API_PRIVATE_KEY'; Hint = 'conteúdo inteiro do arquivo .p8' }
)
foreach ($s in $textSecrets) {
  Write-Host ("  • {0}" -f $s.Name) -ForegroundColor White
  Write-Host ("    {0}" -f $s.Hint) -ForegroundColor DarkGray
}

Write-Host "`nReaproveitar da Bíblia DC (mesma conta Apple):" -ForegroundColor Yellow
Write-Host '  APPLE_TEAM_ID, APPSTORE_* e opcionalmente o mesmo certificado .p12'
Write-Host '  O perfil .mobileprovision DEVE ser novo para com.seuprojeto.primeprofast'
Write-Host ''

Write-Host 'Checklist: .github\APPLE_SECRETS_CHECKLIST.md'
Write-Host 'Depois: git push → Actions → iOS App Store → Run workflow'
Write-Host ''
