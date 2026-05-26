# Script para verificar compatibilidade do JDK

Write-Host "🔍 Verificando compatibilidade do JDK..." -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Java instalado
Write-Host "1️⃣ Java Instalado no Sistema:" -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version"
    Write-Host "   $javaVersion" -ForegroundColor Cyan
    
    if ($javaVersion -match "version ""(\d+)") {
        $installedVersion = [int]$matches[1]
        Write-Host "   Versão: Java $installedVersion" -ForegroundColor Cyan
        
        if ($installedVersion -lt 17) {
            Write-Host "   ❌ ERRO: Java 17 ou superior é necessário!" -ForegroundColor Red
        } elseif ($installedVersion -eq 17) {
            Write-Host "   ⚠️  Java 17 instalado (projeto exige Java 21)" -ForegroundColor Yellow
        } elseif ($installedVersion -ge 21) {
            Write-Host "   ✅ Java 21 ou superior instalado (compatível)" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "   ❌ Java não encontrado!" -ForegroundColor Red
}

Write-Host ""

# 2. Verificar Java usado pelo Gradle
Write-Host "2️⃣ Java Usado pelo Gradle:" -ForegroundColor Yellow
if (Test-Path "android\gradlew.bat") {
    try {
        cd android
        $gradleVersion = .\gradlew -version 2>&1 | Select-String -Pattern "(Gradle|JVM)" -Context 0,0
        
        Write-Host "   Detalhes do Gradle:" -ForegroundColor Cyan
        $gradleVersion | ForEach-Object {
            Write-Host "   $_" -ForegroundColor Cyan
        }
        
        $jvmInfo = .\gradlew -version 2>&1 | Select-String -Pattern "JVM:" | Out-String
        if ($jvmInfo -match "JVM:\s+(\d+)\.\d+") {
            $gradleJavaVersion = [int]$matches[1]
            
            if ($gradleJavaVersion -lt 21) {
                Write-Host ""
                Write-Host "   ⚠️  INCOMPATIBILIDADE DETECTADA!" -ForegroundColor Red
                Write-Host "   Gradle está usando Java $gradleJavaVersion" -ForegroundColor Yellow
                Write-Host "   Projeto exige Java 21" -ForegroundColor Yellow
            } else {
                Write-Host "   ✅ Gradle usando Java $gradleJavaVersion (compatível)" -ForegroundColor Green
            }
        }
        
        cd ..
    } catch {
        Write-Host "   ⚠️  Erro ao verificar versão do Gradle" -ForegroundColor Yellow
        cd ..
    }
} else {
    Write-Host "   ⚠️  Gradle não encontrado (pasta android não existe)" -ForegroundColor Yellow
}

Write-Host ""

# 3. Verificar configuração do projeto
Write-Host "3️⃣ Configuração do Projeto:" -ForegroundColor Yellow
$capacitorBuild = "android\app\capacitor.build.gradle"
$cordovaBuild = "android\capacitor-cordova-android-plugins\build.gradle"

if (Test-Path $capacitorBuild) {
    $javaVersion = Select-String -Path $capacitorBuild -Pattern "VERSION_(\d+)" | ForEach-Object {
        if ($_.Line -match "VERSION_(\d+)") {
            [int]$matches[1]
        }
    } | Select-Object -First 1
    
    if ($javaVersion) {
        Write-Host "   Projeto configurado para: Java $javaVersion" -ForegroundColor Cyan
        
        if ($javaVersion -gt 17) {
            Write-Host "   ⚠️  Projeto exige Java $javaVersion (você tem Java 17)" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ Configuração compatível com Java instalado" -ForegroundColor Green
        }
    }
}

Write-Host ""

# 4. Recomendações
Write-Host "4️⃣ Recomendações:" -ForegroundColor Yellow
Write-Host ""

$installedVersion = if ($javaVersion -match "version ""(\d+)") { [int]$matches[1] } else { 17 }

if ($installedVersion -lt 21) {
    Write-Host "   📥 OPÇÃO 1: Atualizar para Java 21 (RECOMENDADO)" -ForegroundColor Cyan
    Write-Host "      ✅ Melhor compatibilidade" -ForegroundColor Green
    Write-Host "      ✅ Evita problemas futuros" -ForegroundColor Green
    Write-Host "      ✅ Acesso a recursos modernos" -ForegroundColor Green
    Write-Host ""
    Write-Host "      Download: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor White
    Write-Host ""
    Write-Host "   🔧 OPÇÃO 2: Alterar projeto para Java 17" -ForegroundColor Cyan
    Write-Host "      ⚠️  Pode requerer ajustes no código" -ForegroundColor Yellow
    Write-Host "      ⚠️  Menos recursos disponíveis" -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "   Qual opção você prefere?" -ForegroundColor Cyan
    $opcao = Read-Host "   Digite 1 (Atualizar Java) ou 2 (Alterar projeto)"
    
    if ($opcao -eq "1") {
        Write-Host ""
        Write-Host "   📋 Passos para atualizar:" -ForegroundColor Yellow
        Write-Host "      1. Baixe Java 21: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor White
        Write-Host "      2. Instale o JDK" -ForegroundColor White
        Write-Host "      3. Execute: java -version (deve mostrar 21)" -ForegroundColor White
        Write-Host "      4. Execute: .\fix_gradle_cache.ps1" -ForegroundColor White
        Write-Host "      5. Execute: cd android && .\gradlew assembleDebug" -ForegroundColor White
    } elseif ($opcao -eq "2") {
        Write-Host ""
        Write-Host "   🔧 Alterando projeto para Java 17..." -ForegroundColor Cyan
        Write-Host "   (Aguarde enquanto modifico os arquivos)" -ForegroundColor Yellow
        
        # Alterar capacitor.build.gradle
        if (Test-Path $capacitorBuild) {
            $content = Get-Content $capacitorBuild -Raw
            $content = $content -replace "VERSION_21", "VERSION_17"
            Set-Content $capacitorBuild -Value $content -NoNewline
            Write-Host "   ✅ $capacitorBuild atualizado" -ForegroundColor Green
        }
        
        # Alterar cordova build.gradle
        if (Test-Path $cordovaBuild) {
            $content = Get-Content $cordovaBuild -Raw
            $content = $content -replace "VERSION_21", "VERSION_17"
            Set-Content $cordovaBuild -Value $content -NoNewline
            Write-Host "   ✅ $cordovaBuild atualizado" -ForegroundColor Green
        }
        
        Write-Host ""
        Write-Host "   ✅ Projeto alterado para Java 17!" -ForegroundColor Green
        Write-Host "   📋 Próximos passos:" -ForegroundColor Yellow
        Write-Host "      1. Execute: cd android" -ForegroundColor White
        Write-Host "      2. Execute: .\gradlew clean" -ForegroundColor White
        Write-Host "      3. Execute: .\gradlew assembleDebug" -ForegroundColor White
    }
} else {
    Write-Host "   ✅ Tudo OK! Java instalado é compatível com o projeto." -ForegroundColor Green
}

Write-Host ""

