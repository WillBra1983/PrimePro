# Solução agressiva para erro de criação de JAR no Gradle

Write-Host "🔧 Solução Agressiva para Erro de JAR no Gradle" -ForegroundColor Cyan
Write-Host ""

# Verificar se está executando como Administrador
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "⚠️  ATENÇÃO: Não está executando como Administrador!" -ForegroundColor Yellow
    Write-Host "   Recomenda-se executar como Administrador para garantir permissões" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Deseja continuar mesmo assim? (S/N)"
    if ($continue -ne "S" -and $continue -ne "s") {
        Write-Host ""
        Write-Host "💡 Para executar como Administrador:" -ForegroundColor Cyan
        Write-Host "   1. Clique com botão direito no PowerShell" -ForegroundColor White
        Write-Host "   2. Selecione 'Executar como administrador'" -ForegroundColor White
        Write-Host "   3. Execute este script novamente" -ForegroundColor White
        exit
    }
    Write-Host ""
}

# 1. Parar TODOS os processos relacionados
Write-Host "1️⃣ Parando TODOS os processos relacionados..." -ForegroundColor Yellow

$processes = @("java", "javaw", "gradle", "gradlew")
foreach ($proc in $processes) {
    $procs = Get-Process -Name $proc -ErrorAction SilentlyContinue
    if ($procs) {
        Write-Host "   Parando processos $proc..." -ForegroundColor Cyan
        $procs | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
}

# Fechar Android Studio se estiver aberto
$studio = Get-Process -Name "studio64" -ErrorAction SilentlyContinue
if ($studio) {
    Write-Host "   ⚠️  Android Studio detectado! Fechando..." -ForegroundColor Yellow
    $studio | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}

Start-Sleep -Seconds 2
Write-Host "   ✅ Processos parados" -ForegroundColor Green
Write-Host ""

# 2. Limpar daemons do Gradle
Write-Host "2️⃣ Parando daemons do Gradle..." -ForegroundColor Yellow
if (Test-Path "android\gradlew.bat") {
    try {
        cd android
        .\gradlew --stop --no-daemon 2>&1 | Out-Null
        cd ..
        Write-Host "   ✅ Daemons parados" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Não foi possível parar daemons (pode estar tudo bem)" -ForegroundColor Yellow
        cd ..
    }
} else {
    Write-Host "   ⚠️  Gradlew não encontrado" -ForegroundColor Yellow
}
Write-Host ""

Start-Sleep -Seconds 3

# 3. Remover cache específico do arquivo problemático
Write-Host "3️⃣ Removendo cache específico do arquivo problemático..." -ForegroundColor Yellow

$problematicPath = "$env:USERPROFILE\.gradle\caches\jars-9\d78d5ca2aa14c70895820c0e5bba6a9b"
$problematicFile = "$problematicPath\gradle-8.2.1.jar"

if (Test-Path $problematicFile) {
    Write-Host "   Arquivo problemático encontrado: $problematicFile" -ForegroundColor Cyan
    
    # Tentar remover arquivo específico
    try {
        Remove-Item -Force $problematicFile -ErrorAction Stop
        Write-Host "   ✅ Arquivo problemático removido" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Não foi possível remover o arquivo: $_" -ForegroundColor Yellow
        
        # Tentar remover pasta inteira
        try {
            Remove-Item -Recurse -Force $problematicPath -ErrorAction Stop
            Write-Host "   ✅ Pasta problemática removida" -ForegroundColor Green
        } catch {
            Write-Host "   ❌ Não foi possível remover a pasta" -ForegroundColor Red
            Write-Host "   💡 Tente executar como Administrador" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ℹ️  Arquivo problemático não existe (já foi removido)" -ForegroundColor Cyan
}

# Remover cache completo de JARs
Write-Host "   Removendo cache completo de JARs..." -ForegroundColor Cyan
$jarCachePath = "$env:USERPROFILE\.gradle\caches\jars-9"
if (Test-Path $jarCachePath) {
    try {
        Remove-Item -Recurse -Force $jarCachePath -ErrorAction Stop
        Write-Host "   ✅ Cache de JARs removido completamente" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Erro ao remover cache completo: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ℹ️  Cache de JARs não existe" -ForegroundColor Cyan
}

Write-Host ""

# 4. Limpar todo cache do Gradle
Write-Host "4️⃣ Limpando cache completo do Gradle..." -ForegroundColor Yellow
$gradleCachePath = "$env:USERPROFILE\.gradle\caches"
$gradleDaemonPath = "$env:USERPROFILE\.gradle\daemon"

Write-Host "   Removendo cache geral..." -ForegroundColor Cyan
if (Test-Path $gradleCachePath) {
    try {
        Remove-Item -Recurse -Force $gradleCachePath -ErrorAction Stop
        Write-Host "   ✅ Cache completo removido" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Erro: $_" -ForegroundColor Yellow
        Write-Host "   💡 Pode ser necessário executar como Administrador" -ForegroundColor Yellow
    }
}

Write-Host "   Removendo daemons..." -ForegroundColor Cyan
if (Test-Path $gradleDaemonPath) {
    try {
        Remove-Item -Recurse -Force $gradleDaemonPath -ErrorAction Stop
        Write-Host "   ✅ Daemons removidos" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Erro: $_" -ForegroundColor Yellow
    }
}

Write-Host ""

# 5. Verificar permissões
Write-Host "5️⃣ Verificando permissões..." -ForegroundColor Yellow
$testPath = "$env:USERPROFILE\.gradle"
if (-not (Test-Path $testPath)) {
    try {
        New-Item -ItemType Directory -Path $testPath -Force | Out-Null
        Write-Host "   ✅ Diretório criado com sucesso" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ Erro ao criar diretório: $_" -ForegroundColor Red
        Write-Host "   💡 Execute como Administrador" -ForegroundColor Yellow
    }
} else {
    try {
        $testFile = "$testPath\test-write-$(Get-Date -Format 'yyyyMMddHHmmss').txt"
        "test" | Out-File -FilePath $testFile -ErrorAction Stop
        Remove-Item $testFile -ErrorAction Stop
        Write-Host "   ✅ Permissões OK" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ Problema de permissões: $_" -ForegroundColor Red
        Write-Host "   💡 Execute como Administrador" -ForegroundColor Yellow
    }
}

Write-Host ""

# 6. Verificar versão do Java
Write-Host "6️⃣ Verificando versão do Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version"
    Write-Host "   $javaVersion" -ForegroundColor Cyan
    
    if ($javaVersion -match "version ""(\d+)") {
        $version = [int]$matches[1]
        if ($version -lt 21) {
            Write-Host ""
            Write-Host "   ⚠️  PROBLEMA IDENTIFICADO!" -ForegroundColor Red
            Write-Host "   Você tem Java $version, mas o projeto exige Java 21" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "   💡 OPÇÃO A: Atualizar para Java 21 (RECOMENDADO)" -ForegroundColor Cyan
            Write-Host "      Download: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor White
            Write-Host ""
            Write-Host "   💡 OPÇÃO B: Alterar projeto para Java 17" -ForegroundColor Cyan
            $opcao = Read-Host "   Deseja alterar o projeto para Java 17 agora? (S/N)"
            
            if ($opcao -eq "S" -or $opcao -eq "s") {
                Write-Host ""
                Write-Host "   🔧 Alterando projeto para Java 17..." -ForegroundColor Cyan
                
                # Alterar capacitor.build.gradle
                $capacitorBuild = "android\app\capacitor.build.gradle"
                if (Test-Path $capacitorBuild) {
                    $content = Get-Content $capacitorBuild -Raw
                    $content = $content -replace "VERSION_21", "VERSION_17"
                    Set-Content $capacitorBuild -Value $content -NoNewline
                    Write-Host "   ✅ $capacitorBuild atualizado" -ForegroundColor Green
                }
                
                # Alterar cordova build.gradle
                $cordovaBuild = "android\capacitor-cordova-android-plugins\build.gradle"
                if (Test-Path $cordovaBuild) {
                    $content = Get-Content $cordovaBuild -Raw
                    $content = $content -replace "VERSION_21", "VERSION_17"
                    Set-Content $cordovaBuild -Value $content -NoNewline
                    Write-Host "   ✅ $cordovaBuild atualizado" -ForegroundColor Green
                }
                
                Write-Host "   ✅ Projeto alterado para Java 17!" -ForegroundColor Green
            }
        } else {
            Write-Host "   ✅ Java $version instalado (compatível)" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "   ❌ Java não encontrado!" -ForegroundColor Red
}

Write-Host ""

# 7. Tentar build
Write-Host "7️⃣ Limpeza concluída!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Próximos passos:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   1. Aguarde 5 segundos para garantir que tudo parou" -ForegroundColor Yellow
Start-Sleep -Seconds 5
Write-Host ""
Write-Host "   2. Tente fazer build novamente:" -ForegroundColor Yellow
Write-Host "      cd android" -ForegroundColor White
Write-Host "      .\gradlew clean" -ForegroundColor White
Write-Host "      .\gradlew assembleDebug --no-daemon" -ForegroundColor White
Write-Host ""

$tryBuild = Read-Host "Deseja tentar fazer build agora? (S/N)"
if ($tryBuild -eq "S" -or $tryBuild -eq "s") {
    Write-Host ""
    Write-Host "🔨 Iniciando build..." -ForegroundColor Cyan
    
    if (Test-Path "android") {
        cd android
        Write-Host ""
        Write-Host "Executando: .\gradlew clean" -ForegroundColor Yellow
        .\gradlew clean --no-daemon
        
        Write-Host ""
        Write-Host "Executando: .\gradlew assembleDebug" -ForegroundColor Yellow
        .\gradlew assembleDebug --no-daemon
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ BUILD CONCLUÍDO COM SUCESSO!" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "❌ BUILD FALHOU" -ForegroundColor Red
            Write-Host ""
            Write-Host "💡 Se o erro persistir, considere:" -ForegroundColor Yellow
            Write-Host "   1. Atualizar para Java 21" -ForegroundColor White
            Write-Host "   2. Usar Android Studio (gerencia melhor os caches)" -ForegroundColor White
            Write-Host "   3. Executar este script como Administrador" -ForegroundColor White
        }
    } else {
        Write-Host "❌ Pasta android não encontrada!" -ForegroundColor Red
    }
} else {
    Write-Host ""
    Write-Host "✅ Limpeza concluída. Execute o build manualmente quando estiver pronto." -ForegroundColor Green
}

