# Gera CSR para certificado Apple Distribution (PrimeProFast).
# Uso: powershell -ExecutionPolicy Bypass -File scripts/apple-gerar-csr.ps1

$ErrorActionPreference = 'Stop'
$openssl = 'C:\Program Files\Git\usr\bin\openssl.exe'
if (-not (Test-Path $openssl)) {
  Write-Host 'OpenSSL nao encontrado. Instale Git for Windows.' -ForegroundColor Red
  exit 1
}

$dir = Join-Path ([Environment]::GetFolderPath('Desktop')) 'PrimeProFast-certificado-Apple'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
Set-Location $dir

$email = Read-Host 'E-mail da conta Apple Developer'
if (-not $email) { $email = 'contato@exemplo.com' }

& $openssl genrsa -out apple_distribution.key 2048
& $openssl req -new -key apple_distribution.key -out CertificateSigningRequest.certSigningRequest `
  -subj "/email=$email/CN=Wilson Lucas Ferreira/C=BR"

Write-Host "Pronto! Pasta: $dir" -ForegroundColor Green
explorer $dir
