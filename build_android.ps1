# Script para build do Android - Corrige problemas de caminho e cache

Write-Host "🔍 Verificando estrutura do projeto..." -ForegroundColor Cyan

# Verificar se estamos na pasta correta
$currentDir = Get-Location
Write-Host "Diretório atual: $currentDir" -ForegroundColor Yellow

# Navegar para a pasta android
if (Test-Path "android") {
    Set-Location "android"
    Write-Host "✅ Pasta android encontrada!" -ForegroundColor Green
} elseif (Test-Path "..\android") {
    Set-Location "..\android"
    Write-Host "✅ Pasta android encontrada (um nível acima)!" -ForegroundColor Green
} else {
    Write-Host "❌ ERRO: Pasta 'android' não encontrada!" -ForegroundColor Red
    Write-Host "Execute este script na raiz do projeto (onde está a pasta 'android')" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "🧹 Limpando caches..." -ForegroundColor Cyan

# Parar processos Java
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Limpar cache do Gradle
Write-Host "Limpando cache do Gradle..." -ForegroundColor Yellow
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\daemon" -ErrorAction SilentlyContinue

# Limpar builds locais
Write-Host "Limpando builds locais..." -ForegroundColor Yellow
Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "app\.cxx" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "app\build" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "build" -ErrorAction SilentlyContinue

# Verificar estrutura necessária
Write-Host ""
Write-Host "🔍 Verificando arquivos necessários..." -ForegroundColor Cyan

$cmakeLists = "app\CMakeLists.txt"
if (-not (Test-Path $cmakeLists)) {
    Write-Host "❌ ERRO: $cmakeLists não encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ $cmakeLists encontrado" -ForegroundColor Green

$cppDir = "app\src\main\cpp"
if (-not (Test-Path $cppDir)) {
    Write-Host "❌ ERRO: $cppDir não encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ $cppDir encontrado" -ForegroundColor Green

$gradlew = "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "❌ ERRO: $gradlew não encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ $gradlew encontrado" -ForegroundColor Green

Write-Host ""
Write-Host "🔨 Iniciando build..." -ForegroundColor Cyan
Write-Host ""

# Limpar primeiro
Write-Host "Executando: .\gradlew clean" -ForegroundColor Yellow
.\gradlew clean --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "⚠️  Limpeza falhou, mas continuando..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Executando: .\gradlew assembleDebug" -ForegroundColor Yellow
.\gradlew assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ BUILD CONCLUÍDO COM SUCESSO!" -ForegroundColor Green
    Write-Host ""
    
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "📦 APK gerado: $apkPath" -ForegroundColor Green
        Write-Host "📊 Tamanho: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Green
        Write-Host ""
        
        # Verificar se adb está disponível
        $adbCheck = Get-Command adb -ErrorAction SilentlyContinue
        if ($adbCheck) {
            Write-Host "📱 Deseja instalar no dispositivo conectado?" -ForegroundColor Cyan
            Write-Host "Execute: adb install -r $apkPath" -ForegroundColor Yellow
            
            $install = Read-Host "Instalar agora? (S/N)"
            if ($install -eq "S" -or $install -eq "s") {
                Write-Host ""
                Write-Host "📲 Instalando no dispositivo..." -ForegroundColor Cyan
                adb uninstall com.primeproject.primeprofast -ErrorAction SilentlyContinue
                adb install -r $apkPath
                
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "✅ APK instalado com sucesso!" -ForegroundColor Green
                    Write-Host ""
                    Write-Host "🚀 Para abrir o app:" -ForegroundColor Cyan
                    Write-Host "   adb shell am start -n com.primeproject.primeprofast/.MainActivity" -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "⚠️  ADB não encontrado. Instale manualmente o APK no dispositivo." -ForegroundColor Yellow
        }
    }
} else {
    Write-Host ""
    Write-Host "❌ BUILD FALHOU!" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Dicas:" -ForegroundColor Yellow
    Write-Host "   1. Verifique se o Android SDK está configurado corretamente"
    Write-Host "   2. Tente usar o Android Studio: File → Open → Selecione a pasta 'android'"
    Write-Host "   3. File → Invalidate Caches... → Invalidate and Restart"
    exit 1
}


