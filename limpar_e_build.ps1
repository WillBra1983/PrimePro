# Script para limpar cache e fazer build do Android

Write-Host "🧹 Limpando caches do Gradle..." -ForegroundColor Cyan

# Parar todos os processos Java/Gradle
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Limpar cache do usuário
Write-Host "Limpando cache do Gradle em ~\.gradle..." -ForegroundColor Yellow
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\daemon" -ErrorAction SilentlyContinue

# Limpar cache local do projeto
Write-Host "Limpando cache local do projeto..." -ForegroundColor Yellow
Remove-Item -Recurse -Force "android\.gradle" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "android\app\build" -ErrorAction SilentlyContinue

Write-Host "✅ Limpeza concluída!" -ForegroundColor Green
Write-Host ""
Write-Host "📱 Agora você tem duas opções:" -ForegroundColor Cyan
Write-Host ""
Write-Host "OPÇÃO 1 (Recomendada): Usar Android Studio" -ForegroundColor Yellow
Write-Host "  1. Abra Android Studio"
Write-Host "  2. File → Open → Selecione a pasta 'android'"
Write-Host "  3. Aguarde sincronização do Gradle"
Write-Host "  4. Build → Rebuild Project"
Write-Host "  5. Run (▶️) para instalar no celular"
Write-Host ""
Write-Host "OPÇÃO 2: Tentar build via linha de comando" -ForegroundColor Yellow
Write-Host "  cd android"
Write-Host "  .\gradlew assembleDebug"
Write-Host ""

$opcao = Read-Host "Deseja tentar build via linha de comando agora? (S/N)"

if ($opcao -eq "S" -or $opcao -eq "s") {
    Write-Host ""
    Write-Host "🔨 Iniciando build..." -ForegroundColor Cyan
    cd android
    .\gradlew assembleDebug --no-daemon
} else {
    Write-Host ""
    Write-Host "✅ Use o Android Studio então! É mais confiável. 😊" -ForegroundColor Green
}

