# Script para corrigir erro de criação de JAR no cache do Gradle

Write-Host "🔧 Corrigindo problema de cache do Gradle..." -ForegroundColor Cyan
Write-Host ""

# 1. Parar todos os processos Java/Gradle
Write-Host "1️⃣ Parando processos Java/Gradle..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "   Encontrados $($javaProcesses.Count) processo(s) Java" -ForegroundColor Yellow
    $javaProcesses | Stop-Process -Force
    Write-Host "   ✅ Processos parados" -ForegroundColor Green
    Start-Sleep -Seconds 3
} else {
    Write-Host "   ✅ Nenhum processo Java encontrado" -ForegroundColor Green
}

# 2. Verificar espaço em disco
Write-Host ""
Write-Host "2️⃣ Verificando espaço em disco..." -ForegroundColor Yellow
$drive = (Get-Location).Drive
$freeSpace = (Get-PSDrive $drive.Name).Free / 1GB
Write-Host "   Espaço livre: $([math]::Round($freeSpace, 2)) GB" -ForegroundColor Cyan
if ($freeSpace -lt 1) {
    Write-Host "   ⚠️  AVISO: Pouco espaço em disco! Recomenda-se pelo menos 2GB livre." -ForegroundColor Red
} else {
    Write-Host "   ✅ Espaço suficiente" -ForegroundColor Green
}

# 3. Limpar cache do Gradle
Write-Host ""
Write-Host "3️⃣ Limpando cache do Gradle..." -ForegroundColor Yellow

$gradleCachePath = "$env:USERPROFILE\.gradle\caches"
$gradleDaemonPath = "$env:USERPROFILE\.gradle\daemon"

# Tentar fechar daemon do Gradle primeiro
Write-Host "   Parando daemons do Gradle..." -ForegroundColor Cyan
try {
    cd android -ErrorAction SilentlyContinue
    .\gradlew --stop --no-daemon 2>&1 | Out-Null
    cd .. -ErrorAction SilentlyContinue
} catch {
    # Ignorar erros
}

Start-Sleep -Seconds 2

# Remover cache de JARs especificamente
Write-Host "   Removendo cache de JARs..." -ForegroundColor Cyan
$jarCachePath = "$gradleCachePath\jars-9"
if (Test-Path $jarCachePath) {
    try {
        Remove-Item -Recurse -Force $jarCachePath -ErrorAction Stop
        Write-Host "   ✅ Cache de JARs removido" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Erro ao remover cache de JARs: $_" -ForegroundColor Yellow
        Write-Host "   Tentando remover arquivos individuais..." -ForegroundColor Yellow
        
        # Tentar remover arquivos específicos mencionados no erro
        $problematicJar = "$jarCachePath\d78d5ca2aa14c70895820c0e5bba6a9b\gradle-8.2.1.jar"
        if (Test-Path $problematicJar) {
            try {
                Remove-Item -Force $problematicJar -ErrorAction Stop
                Write-Host "   ✅ Arquivo problemático removido" -ForegroundColor Green
            } catch {
                Write-Host "   ❌ Não foi possível remover o arquivo. Pode estar em uso." -ForegroundColor Red
                Write-Host "   💡 Tente fechar o Android Studio e executar novamente." -ForegroundColor Yellow
            }
        }
    }
} else {
    Write-Host "   ℹ️  Cache de JARs não existe (já foi limpo)" -ForegroundColor Cyan
}

# Remover daemon cache
Write-Host "   Removendo cache de daemons..." -ForegroundColor Cyan
if (Test-Path $gradleDaemonPath) {
    try {
        Remove-Item -Recurse -Force $gradleDaemonPath -ErrorAction Stop
        Write-Host "   ✅ Cache de daemons removido" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Erro ao remover cache de daemons: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ℹ️  Cache de daemons não existe" -ForegroundColor Cyan
}

# Limpar cache completo se o problema persistir
Write-Host ""
Write-Host "4️⃣ Opção: Limpar cache completo do Gradle" -ForegroundColor Yellow
Write-Host "   Isso vai fazer o Gradle baixar tudo novamente na próxima build" -ForegroundColor Cyan
$fullClean = Read-Host "   Deseja limpar TODO o cache do Gradle? (S/N)"

if ($fullClean -eq "S" -or $fullClean -eq "s") {
    Write-Host "   Limpando cache completo..." -ForegroundColor Cyan
    if (Test-Path $gradleCachePath) {
        try {
            Remove-Item -Recurse -Force $gradleCachePath -ErrorAction Stop
            Write-Host "   ✅ Cache completo removido" -ForegroundColor Green
        } catch {
            Write-Host "   ⚠️  Erro: $_" -ForegroundColor Yellow
            Write-Host "   💡 Tente executar como Administrador" -ForegroundColor Yellow
        }
    }
}

# 5. Verificar permissões
Write-Host ""
Write-Host "5️⃣ Verificando permissões..." -ForegroundColor Yellow
$testPath = "$env:USERPROFILE\.gradle"
if (Test-Path $testPath) {
    try {
        $testFile = "$testPath\test-write.txt"
        "test" | Out-File -FilePath $testFile -ErrorAction Stop
        Remove-Item $testFile -ErrorAction Stop
        Write-Host "   ✅ Permissões OK" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Problema de permissões detectado" -ForegroundColor Red
        Write-Host "   💡 Tente executar como Administrador" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✅ Diretório não existe (será criado na primeira build)" -ForegroundColor Green
}

Write-Host ""
Write-Host "✅ Limpeza concluída!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Próximos passos:" -ForegroundColor Cyan
Write-Host "   1. Se o Android Studio estiver aberto, feche-o" -ForegroundColor Yellow
Write-Host "   2. Aguarde 5 segundos" -ForegroundColor Yellow
Write-Host "   3. Execute o build novamente:" -ForegroundColor Yellow
Write-Host "      cd android" -ForegroundColor White
Write-Host "      .\gradlew assembleDebug" -ForegroundColor White
Write-Host ""

$tryBuild = Read-Host "Deseja tentar fazer build agora? (S/N)"
if ($tryBuild -eq "S" -or $tryBuild -eq "s") {
    Write-Host ""
    Write-Host "🔨 Iniciando build..." -ForegroundColor Cyan
    if (Test-Path "android") {
        cd android
        .\gradlew assembleDebug --no-daemon
    } else {
        Write-Host "❌ Pasta android não encontrada!" -ForegroundColor Red
    }
}


