# 📱 Como Fazer Build para Testar no Celular

## 🚀 Método Rápido (Recomendado)

### ⭐ Opção 1: Via Android Studio (MAIS RECOMENDADO - Mais Fácil)

Esta é a forma mais confiável e evita problemas com cache do Gradle:

1. **Abrir Android Studio**
2. **File → Open** → Selecionar a pasta `android`
3. **Aguardar sincronização** do Gradle (pode demorar alguns minutos na primeira vez)
4. **Build → Clean Project** (opcional, mas recomendado)
5. **Build → Rebuild Project**
6. **Conectar celular via USB** (com Depuração USB ativada)
7. **Clicar em Run (▶️)** ou pressionar `Shift + F10`

**Se houver problemas de cache:**
- **File → Invalidate Caches... → Invalidate and Restart**
- Aguardar o Android Studio reiniciar e sincronizar novamente

### Opção 2: Via Linha de Comando

```powershell
# Navegar para a pasta android
cd android

# Build de debug (mais rápido, para testes)
.\gradlew assembleDebug

# O APK será gerado em:
# android\app\build\outputs\apk\debug\app-debug.apk
```

## 📲 Instalar no Celular

### Via ADB (Android Debug Bridge)

```powershell
# Verificar se o celular está conectado
adb devices

# DESINSTALAR versão anterior (resolve erro INSTALL_FAILED_VERSION_DOWNGRADE)
adb uninstall com.primeproject.primeprofast

# Instalar o APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**⚠️ Erro INSTALL_FAILED_VERSION_DOWNGRADE:**
Isso acontece quando você tenta instalar uma versão mais antiga. Solução:
```powershell
# Desinstalar primeiro
adb uninstall com.primeproject.primeprofast

# Depois instalar
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Manualmente

1. Copiar o arquivo `app-debug.apk` para o celular
2. No celular, navegar até o arquivo
3. Tocar para instalar (permitir fontes desconhecidas se solicitado)

## ⚙️ Pré-requisitos

### No Celular

1. **Ativar Modo Desenvolvedor:**
   - Configurações → Sobre o telefone
   - Tocar 7x em "Número da versão"

2. **Ativar Depuração USB:**
   - Configurações → Opções do desenvolvedor
   - Ativar "Depuração USB"

### No Computador

1. **Android Studio** instalado
2. **JDK 11+** instalado
3. **Android SDK** configurado
4. **Driver USB** do celular (se Windows)

## 🔧 Comandos Úteis

### Build Completo

```powershell
cd android

# Limpar build anterior
.\gradlew clean

# Build de debug
.\gradlew assembleDebug

# Build de release (assinado)
.\gradlew assembleRelease
```

### Verificar Dispositivos

```powershell
# Listar dispositivos conectados
adb devices

# Ver logs do app
adb logcat | findstr "PrimeProFast"
```

### Instalar e Executar

```powershell
# Instalar APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Abrir o app
adb shell am start -n com.seuprojeto.primeprofast/.MainActivity
```

## 📁 Localização dos Arquivos Gerados

| Tipo | Caminho |
|------|---------|
| APK Debug | `android\app\build\outputs\apk\debug\app-debug.apk` |
| APK Release | `android\app\build\outputs\apk\release\app-release.apk` |
| AAB (Play Store) | `android\app\build\outputs\bundle\release\app-release.aab` |

## ⚠️ Problemas Comuns e Soluções

### Cache do Gradle Corrompido

Se você encontrar erros como `Failed to create Jar file`, use o script de limpeza:

```powershell
# Executar script de limpeza
.\limpar_e_build.ps1
```

**Ou manualmente:**

```powershell
# Parar processos Java
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force

# Limpar caches
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "android\.gradle" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "android\app\build" -ErrorAction SilentlyContinue
```

**💡 DICA:** Se problemas de cache persistirem, **use o Android Studio diretamente**. Ele gerencia melhor os caches e é mais confiável:

1. Android Studio → File → Invalidate Caches... → Invalidate and Restart
2. Aguardar reinicialização
3. Build → Rebuild Project

### "Device not found"

```powershell
# Reiniciar ADB
adb kill-server
adb start-server
adb devices
```

### "Build failed"

```powershell
# Limpar e reconstruir
cd android
.\gradlew clean
.\gradlew assembleDebug --stacktrace
```

### "CMakeLists.txt not found" ou caminho errado

**Erro:** `expected buildFiles file 'C:\PrimePro\...' to exist`

**Causa:** Cache do Gradle com caminho antigo ou executando na pasta errada.

**Solução:**
```powershell
# 1. Use o script automático (recomendado)
.\build_android.ps1

# 2. Ou manualmente:
cd android  # ← IMPORTANTE: Execute na pasta android!
.\gradlew clean
.\gradlew assembleDebug
```

### "SDK not found"

1. Abrir Android Studio
2. File → Project Structure → SDK Location
3. Verificar se o caminho do Android SDK está correto

### "Gradle sync failed"

```powershell
# Atualizar dependências
cd android
.\gradlew --refresh-dependencies
```

## 🎯 Testar o Card de Transformação

Após instalar o app:

1. Abrir o app **PrimeProFast**
2. No menu principal, tocar em **"Teste: Transformação vs Aleatório"**
3. Configurar:
   - Número de testes: **10**
   - Tamanho dos primos: **5 dígitos**
4. Tocar em **"🚀 Executar Teste Comparativo"**
5. Aguardar os resultados

## 📊 O Que Esperar

O teste comparará:

- ⚡ **Método de Transformação** (baseado no diálogo)
- 🎲 **Método Aleatório** (tradicional)

E mostrará:
- Tempo de execução de cada método
- Número de tentativas necessárias
- Qual método foi mais rápido
- Percentual de diferença

## 🔄 Atualizações Rápidas

Para testar mudanças no código web (`www/`):

```powershell
# Sincronizar assets web com Android
npx cap sync android

# Rebuild e instalar
cd android
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 📝 Notas

- **Debug build** é mais rápido para compilar, ideal para testes
- **Release build** é otimizado, mas precisa de assinatura
- O card de teste funciona tanto no navegador quanto no app Android
- Para testes rápidos, use o navegador: `http://localhost:8080`

---

**Bons testes! 🚀**

