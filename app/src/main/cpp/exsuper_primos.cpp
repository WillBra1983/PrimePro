#include <jni.h>
#include <string>
#include <vector>
#include <cmath>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_primeproject_primeprofast_MainActivity_calcularPrimos(JNIEnv* env, jobject, jint fim) {
    std::vector<int> primos;
    if (fim >= 2) primos.push_back(2);
    if (fim >= 3) primos.push_back(3);
    if (fim >= 5) primos.push_back(5);

    for (int x = 7; x <= fim; x += 2) {
        int mod10 = x % 10;
        if (mod10 != 1 && mod10 != 3 && mod10 != 7 && mod10 != 9) continue;
        bool ehPrimo = true;
        int limite = sqrt(x);
        for (int p : primos) {
            if (p > limite) break;
            if (x % p == 0) {
                ehPrimo = false;
                break;
            }
        }
        if (ehPrimo) primos.push_back(x);
    }

    std::string resultado = "Total de primos: " + std::to_string(primos.size()) + "\n";
    int mostrar = std::min(50, (int)primos.size());
    resultado += "Primeiros primos: ";
    for (int i = 0; i < mostrar; ++i) {
        resultado += std::to_string(primos[i]) + " ";
    }
    return env->NewStringUTF(resultado.c_str());
} 