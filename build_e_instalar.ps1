# Script para build e instalação completa

Write-Host "=== Build e Instalacao do PrimeProFast ===" -ForegroundColor Cyan
Write-Host ""

# 1. Parar processos
Write-Host "1. Parando processos Java..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Write-Host "   OK" -ForegroundColor Green

# 2. Sincronizar Capacitor
Write-Host ""
Write-Host "2. Sincronizando Capacitor..." -ForegroundColor Yellow
cd C:\PrimeProFastNative
npx cap sync android
if ($LASTEXITCODE -ne 0) {
    Write-Host "   ERRO na sincronizacao!" -ForegroundColor Red
    exit 1
}
Write-Host "   OK" -ForegroundColor Green

# 3. Limpar build
Write-Host ""
Write-Host "3. Limpando build anterior..." -ForegroundColor Yellow
cd android
Remove-Item -Recurse -Force "app\build\intermediates" -ErrorAction SilentlyContinue
.\gradlew clean --no-daemon | Out-Null
Write-Host "   OK" -ForegroundColor Green

# 4. Build
Write-Host ""
Write-Host "4. Fazendo build (pode demorar alguns minutos)..." -ForegroundColor Yellow
.\gradlew assembleDebug --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "   ERRO no build!" -ForegroundColor Red
    Write-Host "   Verifique os erros acima" -ForegroundColor Yellow
    exit 1
}

Write-Host "   OK: Build concluido!" -ForegroundColor Green

# 5. Verificar APK
Write-Host ""
Write-Host "5. Verificando APK..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length / 1MB
    Write-Host "   APK encontrado: $apkPath" -ForegroundColor Green
    Write-Host "   Tamanho: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Cyan
} else {
    Write-Host "   ERRO: APK nao encontrado!" -ForegroundColor Red
    exit 1
}

# 6. Instalar
Write-Host ""
Write-Host "6. Instalando no dispositivo..." -ForegroundColor Yellow

# Verificar se dispositivo está conectado
$devices = adb devices 2>&1 | Select-String -Pattern "device$"
if (-not $devices) {
    Write-Host "   AVISO: Nenhum dispositivo encontrado!" -ForegroundColor Yellow
    Write-Host "   Conecte o celular via USB e ative depuracao USB" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   Para instalar depois, execute:" -ForegroundColor Cyan
    Write-Host "   adb uninstall com.primeproject.primeprofast" -ForegroundColor White
    Write-Host "   adb install -r $apkPath" -ForegroundColor White
    exit 0
}

# Desinstalar versão anterior
Write-Host "   Desinstalando versao anterior..." -ForegroundColor Cyan
adb uninstall com.primeproject.primeprofast 2>&1 | Out-Null

# Instalar nova versão
Write-Host "   Instalando nova versao..." -ForegroundColor Cyan
adb install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=== SUCESSO! ===" -ForegroundColor Green
    Write-Host "   App instalado com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "   Para abrir o app:" -ForegroundColor Cyan
    Write-Host "   adb shell am start -n com.primeproject.primeprofast/.MainActivity" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "   ERRO na instalacao!" -ForegroundColor Red
    Write-Host "   Tente manualmente:" -ForegroundColor Yellow
    Write-Host "   adb install -r $apkPath" -ForegroundColor White
}

