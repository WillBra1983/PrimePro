# Converte distribution.cer + apple_distribution.key em distribution.p12
$ErrorActionPreference = 'Stop'
$openssl = 'C:\Program Files\Git\usr\bin\openssl.exe'
if (-not (Test-Path $openssl)) { throw 'OpenSSL nao encontrado (Git for Windows).' }

$dir = Read-Host 'Pasta com apple_distribution.key e distribution.cer [Desktop\PrimeProFast-certificado-Apple]'
if (-not $dir) {
  $dir = Join-Path ([Environment]::GetFolderPath('Desktop')) 'PrimeProFast-certificado-Apple'
}
Set-Location $dir

& $openssl x509 -in distribution.cer -inform DER -out distribution.pem -outform PEM
& $openssl pkcs12 -export -out distribution.p12 -inkey apple_distribution.key -in distribution.pem

Write-Host 'Gerado: distribution.p12' -ForegroundColor Green
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes('distribution.p12'))
$b64 | Set-Clipboard
Write-Host 'Base64 copiado para a area de transferencia (secret APPLE_CERTIFICATE_BASE64).' -ForegroundColor Cyan
