$path = Read-Host 'Caminho do .mobileprovision App Store (com.seuprojeto.primeprofast)'
if (-not (Test-Path $path)) { throw "Arquivo nao encontrado: $path" }
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($path))
$b64 | Set-Clipboard
Write-Host 'Base64 copiado (secret APPLE_PROVISION_PROFILE_BASE64).' -ForegroundColor Green
