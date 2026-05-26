# Testa se o .p12 e a senha estao corretos (antes de atualizar o secret no GitHub).
# Uso: powershell -ExecutionPolicy Bypass -File scripts/apple-testar-p12.ps1

$ErrorActionPreference = 'Stop'
$openssl = 'C:\Program Files\Git\usr\bin\openssl.exe'
if (-not (Test-Path $openssl)) {
  Write-Host 'OpenSSL nao encontrado (Git for Windows).' -ForegroundColor Red
  exit 1
}

$p12 = Read-Host 'Caminho do distribution.p12'
if (-not (Test-Path $p12)) {
  Write-Host "Arquivo nao encontrado: $p12" -ForegroundColor Red
  exit 1
}

$senha = Read-Host 'Senha do .p12' -AsSecureString
$BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($senha)
$senhaPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)

$env:OPENSSL_PW = $senhaPlain
& $openssl pkcs12 -in $p12 -noout -passin env:OPENSSL_PW
$code = $LASTEXITCODE
Remove-Item Env:OPENSSL_PW -ErrorAction SilentlyContinue

if ($code -eq 0) {
  Write-Host ''
  Write-Host 'OK: senha correta. Use esta senha em APPLE_CERTIFICATE_PASSWORD no GitHub.' -ForegroundColor Green
  Write-Host 'Sem espacos no inicio/fim. Cole de novo o secret se tinha erro.' -ForegroundColor Green
} else {
  Write-Host ''
  Write-Host 'FALHOU: senha incorreta ou .p12 corrompido.' -ForegroundColor Red
  Write-Host 'Se usou o .p12 da Biblia DC, a senha e a que voce definiu ao rodar apple-cer-para-p12.ps1' -ForegroundColor Yellow
  exit 1
}
