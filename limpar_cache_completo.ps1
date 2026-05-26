# Limpeza completa e agressiva do cache do Gradle

Write-Host "Limpando cache completo do Gradle..." -ForegroundColor Cyan
Write-Host ""

# Parar TODOS os processos
Write-Host "1. Parando processos..." -ForegroundColor Yellow
Get-Process -Name "java","javaw","gradle" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

# Parar daemons do Gradle
Write-Host "2. Parando daemons do Gradle..." -ForegroundColor Yellow
if (Test-Path "android\gradlew.bat") {
    cd android
    .\gradlew --stop --no-daemon 2>&1 | Out-Null
    cd ..
}
Start-Sleep -Seconds 2

# Remover cache completo de JARs
Write-Host "3. Removendo cache completo de JARs..." -ForegroundColor Yellow
$jarCachePath = "$env:USERPROFILE\.gradle\caches\jars-9"
if (Test-Path $jarCachePath) {
    try {
        Remove-Item -Recurse -Force $jarCachePath -ErrorAction Stop
        Write-Host "   OK: Cache de JARs removido" -ForegroundColor Green
    } catch {
        Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "   Tentando remover arquivos individuais..." -ForegroundColor Yellow
        
        # Tentar remover arquivos específicos mencionados nos erros
        $problematicFiles = @(
            "$jarCachePath\d78d5ca2aa14c70895820c0e5bba6a9b\gradle-8.2.1.jar",
            "$jarCachePath\18366b31678c0171857be093a3b8ec22\bcprov-jdk18on-1.79.jar"
        )
        
        foreach ($file in $problematicFiles) {
            if (Test-Path $file) {
                try {
                    Remove-Item -Force $file -ErrorAction Stop
                    Write-Host "   OK: $($file.Split('\')[-1]) removido" -ForegroundColor Green
                } catch {
                    Write-Host "   ERRO ao remover $($file.Split('\')[-1])" -ForegroundColor Yellow
                }
            }
        }
    }
} else {
    Write-Host "   INFO: Cache de JARs nao existe" -ForegroundColor Cyan
}

# Remover todo cache do Gradle
Write-Host "4. Removendo cache completo do Gradle..." -ForegroundColor Yellow
$gradleCachePath = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCachePath) {
    try {
        Remove-Item -Recurse -Force $gradleCachePath -ErrorAction Stop
        Write-Host "   OK: Cache completo removido" -ForegroundColor Green
    } catch {
        Write-Host "   ERRO: Nao foi possivel remover todo o cache" -ForegroundColor Yellow
        Write-Host "   Tentando remover subpastas..." -ForegroundColor Yellow
        
        # Remover subpastas problemáticas
        $subPaths = @("modules-2", "jars-9", "transforms-3")
        foreach ($subPath in $subPaths) {
            $fullPath = "$gradleCachePath\$subPath"
            if (Test-Path $fullPath) {
                try {
                    Remove-Item -Recurse -Force $fullPath -ErrorAction Stop
                    Write-Host "   OK: $subPath removido" -ForegroundColor Green
                } catch {
                    Write-Host "   ERRO ao remover $subPath" -ForegroundColor Yellow
                }
            }
        }
    }
}

# Remover daemons
Write-Host "5. Removendo daemons..." -ForegroundColor Yellow
$daemonPath = "$env:USERPROFILE\.gradle\daemon"
if (Test-Path $daemonPath) {
    try {
        Remove-Item -Recurse -Force $daemonPath -ErrorAction Stop
        Write-Host "   OK: Daemons removidos" -ForegroundColor Green
    } catch {
        Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Limpeza concluida!" -ForegroundColor Green
Write-Host ""
Write-Host "Agora tente fazer o build:" -ForegroundColor Cyan
Write-Host "  cd android" -ForegroundColor White
Write-Host "  .\gradlew clean --no-daemon" -ForegroundColor White
Write-Host "  .\gradlew assembleDebug --no-daemon" -ForegroundColor White

