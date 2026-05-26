# 🔧 Compilação da Implementação Híbrida

## 📋 Pré-requisitos

### **Android NDK**
- **Versão**: r21 ou superior
- **Download**: [Android NDK](https://developer.android.com/ndk/downloads)
- **Arquiteturas**: arm64-v8a, armeabi-v7a, x86, x86_64

### **CMake**
- **Versão**: 3.4.1 ou superior
- **Incluído**: Android Studio 4.0+

### **Biblioteca GMP**
- **Versão**: 6.2.1 ou superior
- **Compilada**: Para cada arquitetura Android

## 🚀 Passos de Compilação

### **1. Configurar Android Studio**

```bash
# Abrir o projeto no Android Studio
# Verificar se o NDK está configurado corretamente
# File -> Project Structure -> SDK Location -> Android NDK location
```

### **2. Verificar Estrutura de Arquivos**

```
app/src/main/cpp/
├── CMakeLists.txt
├── primos_aleatorios_otimizado.cpp
├── primos_aleatorios_hibrido.cpp  # NOVO ARQUIVO
├── super_primos.cpp
├── super_primos_standalone.cpp
└── gmp/
    ├── include/
    │   ├── gmp.h
    │   └── gmpxx.h
    └── lib/
        ├── arm64-v8a/
        │   ├── libgmp.a
        │   └── libgmpxx.a
        ├── armeabi-v7a/
        │   ├── libgmp.a
        │   └── libgmpxx.a
        ├── x86/
        │   ├── libgmp.a
        │   └── libgmpxx.a
        └── x86_64/
            ├── libgmp.a
            └── libgmpxx.a
```

### **3. Verificar CMakeLists.txt**

```cmake
cmake_minimum_required(VERSION 3.4.1)
project(PrimeProFast)

# Otimizações compatíveis com Android NDK
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -funroll-loops -DNDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -funroll-loops -DNDEBUG")

# Caminhos locais da GMP
set(GMP_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/gmp/include")
set(GMP_LIBRARY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/gmp/lib")
include_directories(${GMP_INCLUDE_DIR})

# Detectar arquitetura do Android
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/armeabi-v7a")
elseif(ANDROID_ABI STREQUAL "x86")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/x86")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/x86_64")
else()
    # Fallback para arm64-v8a
    set(GMP_LIB_PATH "${GMP_LIBRARY_DIR}/arm64-v8a")
endif()

# Bibliotecas existentes
add_library(super_primos SHARED super_primos.cpp)
add_library(super_primos_exe SHARED super_primos_standalone.cpp)
add_library(primos_aleatorios_otimizado SHARED primos_aleatorios_otimizado.cpp)

# NOVA BIBLIOTECA HÍBRIDA
add_library(primos_aleatorios_hibrido SHARED primos_aleatorios_hibrido.cpp)

find_library(log-lib log)

# Linkar com GMP e outras bibliotecas necessárias
target_link_libraries(super_primos ${log-lib} ${GMP_LIB_PATH}/libgmp.a ${GMP_LIB_PATH}/libgmpxx.a)
target_link_libraries(super_primos_exe ${log-lib} ${GMP_LIB_PATH}/libgmp.a ${GMP_LIB_PATH}/libgmpxx.a)
target_link_libraries(primos_aleatorios_otimizado ${log-lib} ${GMP_LIB_PATH}/libgmp.a ${GMP_LIB_PATH}/libgmpxx.a)

# NOVA BIBLIOTECA HÍBRIDA
target_link_libraries(primos_aleatorios_hibrido ${log-lib} ${GMP_LIB_PATH}/libgmp.a ${GMP_LIB_PATH}/libgmpxx.a)
```

### **4. Compilar no Android Studio**

```bash
# Build -> Make Project
# Build -> Clean Project (se necessário)
# Build -> Rebuild Project
```

### **5. Verificar Build**

```bash
# Verificar se os arquivos .so foram gerados
app/build/intermediates/cmake/debug/obj/
├── arm64-v8a/
│   ├── libprimos_aleatorios_hibrido.so
│   ├── libprimos_aleatorios_otimizado.so
│   └── libsuper_primos.so
├── armeabi-v7a/
├── x86/
└── x86_64/
```

## 🧪 Teste de Compilação

### **Teste Standalone (Linux/macOS)**

```bash
# Compilar o arquivo de teste
g++ -o teste_hibrido teste_hibrido.cpp -lgmp -lgmpxx -std=c++17 -O3

# Executar o teste
./teste_hibrido
```

### **Teste no Android**

```bash
# Instalar o app no dispositivo/emulador
# Navegar para "Primos Aleatórios"
# Testar os botões híbridos:
# - 🔥 Híbrido: Java + C++ + mpz_nextprime (Dígitos)
# - 🔥 Híbrido: Java + C++ + mpz_nextprime (Bits)
```

## ❌ Solução de Problemas

### **Erro: "undefined reference to mpz_nextprime"**

```bash
# Verificar se a biblioteca GMP está linkada corretamente
# Verificar se as versões são compatíveis
# Verificar se o CMakeLists.txt está correto
```

### **Erro: "library not found"**

```bash
# Verificar se os arquivos .a da GMP existem
# Verificar se os caminhos no CMakeLists.txt estão corretos
# Verificar se a arquitetura está sendo detectada corretamente
```

### **Erro: "JNI function not found"**

```bash
# Verificar se as funções nativas estão declaradas no MainActivity.java
# Verificar se os nomes das funções estão corretos
# Verificar se o app foi reinstalado após mudanças no código nativo
```

## 📱 Configuração do App

### **MainActivity.java**

```java
// Declarações das funções nativas
public native String gerarPrimosAleatoriosHibrido(int quantidade, int digitos, int maxTentativas);
public native String gerarPrimosAleatoriosHibridoPorBits(int quantidade, int bits, int maxTentativas);

// Carregamento da biblioteca
static {
    System.loadLibrary("primos_aleatorios_hibrido");
}
```

### **build.gradle (app)**

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'
        }
    }
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
            version "3.4.1"
        }
    }
}
```

## 🔍 Verificação de Funcionamento

### **Logs de Debug**

```bash
# Verificar logs do Android
adb logcat | grep "PrimosHibrido"

# Logs esperados:
# 🔄 Thread 1 (mpz_nextprime): Iniciada
# 🔄 Thread 2 (sequencial): Iniciada
# 🔄 Thread 3 (incremento): Iniciada
# ✅ Thread X encontrou primo #Y (Z dígitos)
```

### **Performance**

```bash
# Comparar tempos de execução:
# - Java puro: ~X segundos
# - C++ + GMP: ~Y segundos
# - Híbrido: ~Z segundos (esperado: mais rápido)
```

## 🚀 Otimizações de Compilação

### **Flags de Otimização**

```cmake
# Otimizações agressivas para release
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -ffast-math -funroll-loops -DNDEBUG")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3 -ffast-math -funroll-loops -DNDEBUG")

# Otimizações específicas para ARM
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=native -mtune=native")
endif()
```

### **LTO (Link Time Optimization)**

```cmake
# Habilitar LTO para otimizações entre arquivos
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION TRUE)
```

## 📊 Benchmarking

### **Teste de Performance**

```bash
# Executar testes com diferentes tamanhos:
# - 100 dígitos: X segundos
# - 500 dígitos: Y segundos
# - 1000 dígitos: Z segundos
# - 5000 dígitos: W segundos
```

### **Comparação de Métodos**

```bash
# Criar tabela comparativa:
# | Dígitos | Java | C++ | Híbrido | Melhoria |
# |---------|------|-----|---------|----------|
# | 100     | Xs   | Ys  | Zs      | +N%      |
# | 500     | Xs   | Ys  | Zs      | +N%      |
# | 1000    | Xs   | Ys  | Zs      | +N%      |
```

---

**Nota**: Esta implementação híbrida combina as melhores características do Java e C++ nativo para maximizar a performance na geração de números primos aleatórios. 