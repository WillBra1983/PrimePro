# 🔧 Solução para Erros de Build

## ❌ Erros Encontrados e Soluções

### 1. Erro: CMakeLists.txt não encontrado / Caminho errado

**Erro:**
```
expected buildFiles file 'C:\PrimePro\PrimeProFastNative\app\src\main\cpp\CMakeLists.txt' to exist
```

**Causa:**
- Cache do Gradle com caminho antigo
- Executando comando na pasta errada

**✅ Solução:**

```powershell
# MÉTODO 1: Script Automático (RECOMENDADO)
.\build_android.ps1

# MÉTODO 2: Manual
# 1. IMPORTANTE: Entre na pasta android primeiro!
cd android

# 2. Limpar caches
.\gradlew clean

# 3. Build
.\gradlew assembleDebug
```

---

### 2. Erro: INSTALL_FAILED_VERSION_DOWNGRADE

**Erro:**
```
adb: failed to install app-debug.apk: Failure [INSTALL_FAILED_VERSION_DOWNGRADE]
```

**Causa:**
- Tentando instalar versão mais antiga que a já instalada

**✅ Solução:**

```powershell
# Desinstalar versão anterior primeiro
adb uninstall com.primeproject.primeprofast

# Depois instalar nova versão
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

### 3. Erro: Failed to create Jar file no cache

**Erro:**
```
Failed to create Jar file C:\Users\...\.gradle\caches\jars-9\...\gradle-8.2.1.jar
```

**O que significa:**
- O Gradle não conseguiu criar/salvar arquivos JAR no cache
- Geralmente causado por: arquivos travados, cache corrompido, permissões ou antivírus

**✅ Solução:**

**Método 1: Script Automático (Recomendado)**
```powershell
.\fix_gradle_cache.ps1
```

**Método 2: Manual**
```powershell
# 1. Parar processos Java
Get-Process -Name "java" | Stop-Process -Force

# 2. Fechar Android Studio (se estiver aberto)

# 3. Limpar cache específico de JARs
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\jars-9" -ErrorAction SilentlyContinue

# 4. Limpar daemons
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\daemon" -ErrorAction SilentlyContinue

# 5. Tentar build novamente
cd android
.\gradlew assembleDebug --no-daemon
```

**Se persistir:**
- Executar PowerShell como **Administrador**
- Verificar se antivírus não está bloqueando
- Limpar cache completo: `Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches"`

---

### 4. Erro: Gradle daemon desapareceu / JVM crash

**Erro:**
```
Gradle build daemon disappeared unexpectedly (it may have been killed or may have crashed)
```

**Causa:**
- Processo Java travado
- Cache corrompido
- Memória insuficiente

**✅ Solução:**

```powershell
# Parar todos os processos Java
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force

# Limpar caches
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\daemon" -ErrorAction SilentlyContinue

# Tentar novamente
cd android
.\gradlew assembleDebug --no-daemon
```

---

## 🚀 Solução Completa (Recomendada)

### Use o Script Automático:

```powershell
# Na raiz do projeto (onde está a pasta android)
.\build_android.ps1
```

**O script faz automaticamente:**
1. ✅ Verifica estrutura do projeto
2. ✅ Limpa todos os caches
3. ✅ Para processos travados
4. ✅ Executa na pasta correta
5. ✅ Faz build e instala (opcional)

---

## 📱 Método Alternativo: Android Studio

Se os problemas persistirem, use o Android Studio:

1. **Abrir Android Studio**
2. **File → Open** → Selecione a pasta `android`
3. **Aguardar sincronização do Gradle**
4. **File → Invalidate Caches...** → Invalidate and Restart
5. **Build → Clean Project**
6. **Build → Rebuild Project**
7. **Run (▶️)** para instalar no dispositivo

---

## ✅ Checklist Rápido

Antes de fazer build, verifique:

- [ ] Está na pasta **`android`** (não na raiz)
- [ ] Arquivo `CMakeLists.txt` existe em `app/`
- [ ] Arquivo `build.gradle` existe em `app/`
- [ ] Android SDK configurado
- [ ] Java JDK instalado
- [ ] Cache do Gradle limpo (se houver erros)

---

## 🎯 Comandos Corretos

```powershell
# SEMPRE execute da pasta android:
cd android

# Limpar
.\gradlew clean

# Build
.\gradlew assembleDebug

# Instalar (após desinstalar versão anterior)
adb uninstall com.primeproject.primeprofast
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

**💡 Dica:** Se tiver muitos problemas, o Android Studio é mais confiável e gerencia melhor os caches automaticamente.

