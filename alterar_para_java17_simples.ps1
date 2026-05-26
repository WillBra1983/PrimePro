# Script para alterar projeto de Java 21 para Java 17

Write-Host "Alterando projeto para Java 17..." -ForegroundColor Cyan
Write-Host ""

$changed = $false

# 1. Alterar capacitor.build.gradle
Write-Host "1. Alterando android\app\capacitor.build.gradle..." -ForegroundColor Yellow
$capacitorBuild = "android\app\capacitor.build.gradle"
if (Test-Path $capacitorBuild) {
    try {
        $content = Get-Content $capacitorBuild -Raw
        $originalContent = $content
        
        # Substituir VERSION_21 por VERSION_17
        $content = $content -replace "VERSION_21", "VERSION_17"
        
        if ($content -ne $originalContent) {
            # Remover BOM UTF-8 se houver
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText((Resolve-Path $capacitorBuild), $content, $utf8NoBom)
            Write-Host "   OK: Arquivo atualizado: VERSION_21 -> VERSION_17" -ForegroundColor Green
            $changed = $true
        } else {
            Write-Host "   INFO: Arquivo ja esta configurado para Java 17" -ForegroundColor Cyan
        }
    } catch {
        Write-Host "   ERRO ao alterar arquivo: $_" -ForegroundColor Red
    }
} else {
    Write-Host "   AVISO: Arquivo nao encontrado: $capacitorBuild" -ForegroundColor Yellow
}

Write-Host ""

# 2. Alterar capacitor-cordova-android-plugins/build.gradle
Write-Host "2. Alterando android\capacitor-cordova-android-plugins\build.gradle..." -ForegroundColor Yellow
$cordovaBuild = "android\capacitor-cordova-android-plugins\build.gradle"
if (Test-Path $cordovaBuild) {
    try {
        $content = Get-Content $cordovaBuild -Raw
        $originalContent = $content
        
        # Substituir VERSION_21 por VERSION_17
        $content = $content -replace "VERSION_21", "VERSION_17"
        
        if ($content -ne $originalContent) {
            # Remover BOM UTF-8 se houver
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText((Resolve-Path $cordovaBuild), $content, $utf8NoBom)
            Write-Host "   OK: Arquivo atualizado: VERSION_21 -> VERSION_17" -ForegroundColor Green
            $changed = $true
        } else {
            Write-Host "   INFO: Arquivo ja esta configurado para Java 17" -ForegroundColor Cyan
        }
    } catch {
        Write-Host "   ERRO ao alterar arquivo: $_" -ForegroundColor Red
    }
} else {
    Write-Host "   AVISO: Arquivo nao encontrado: $cordovaBuild" -ForegroundColor Yellow
}

Write-Host ""

if ($changed) {
    Write-Host "OK: Projeto alterado para Java 17!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Proximos passos:" -ForegroundColor Cyan
    Write-Host "   1. Execute: cd android" -ForegroundColor White
    Write-Host "   2. Execute: .\gradlew clean" -ForegroundColor White
    Write-Host "   3. Execute: .\gradlew assembleDebug --no-daemon" -ForegroundColor White
    Write-Host ""
    
    $tryBuild = Read-Host "Deseja tentar fazer build agora? (S/N)"
    if ($tryBuild -eq "S" -or $tryBuild -eq "s") {
        Write-Host ""
        Write-Host "Iniciando build..." -ForegroundColor Cyan
        
        if (Test-Path "android") {
            cd android
            Write-Host ""
            Write-Host "Limpando projeto..." -ForegroundColor Yellow
            .\gradlew clean --no-daemon
            
            Write-Host ""
            Write-Host "Fazendo build..." -ForegroundColor Yellow
            .\gradlew assembleDebug --no-daemon
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host ""
                Write-Host "BUILD CONCLUIDO COM SUCESSO!" -ForegroundColor Green
            } else {
                Write-Host ""
                Write-Host "Build falhou. Verifique os erros acima." -ForegroundColor Red
            }
        }
    }
} else {
    Write-Host "INFO: Nenhuma alteracao necessaria. Projeto ja esta configurado para Java 17." -ForegroundColor Cyan
}

