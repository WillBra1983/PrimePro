# PrimeProFast — Publicação na Apple App Store

## O que foi implementado

- App **iOS nativo** em `ios/PrimeProFast/` (SwiftUI).
- **Mesmo núcleo C++/GMP** do Android (`ppf_engine.cpp`, algoritmo especializado).
- CI **GitHub Actions** sem Mac local: [docs/GITHUB_IOS_APP_STORE.md](docs/GITHUB_IOS_APP_STORE.md).

## Requisitos Apple

| Item | Valor |
|------|--------|
| Bundle ID | `com.seuprojeto.primeprofast` |
| Versão | 1.0 (build 3, alinhado ao Android) |
| iOS mínimo | 16.0 |
| Conta | Apple Developer Program |

## Passos resumidos

1. Registrar **Bundle ID** no Apple Developer.
2. Criar app no **App Store Connect** com esse bundle.
3. Configurar **secrets** no GitHub (certificado, perfil, API).
4. Executar workflow **iOS App Store**.
5. TestFlight → revisão → loja.

## Metadados sugeridos

- **Nome:** PrimeProFast
- **Subtítulo:** Calculadora de números primos
- **Categoria:** Educação ou Utilitários
- **Política de privacidade:** URL pública (pode usar texto de [PRIVACY_POLICY.md](PRIVACY_POLICY.md))
- **Screenshots:** iPhone 6,7" e 6,5" (obrigatórios)

## StoreKit (Premium)

SKUs Android existentes (referência para criar no App Store Connect):

- `primeprofast_premium_mensal`
- `primeprofast_premium_anual`
- `primeprofast_entrega_primo_email`

A integração StoreKit 2 no iOS pode ser adicionada na fase seguinte; o app funciona sem compras para cálculos principais.

## Diferença em relação ao Salvation

| Salvation (Bíblia DC) | PrimeProFast |
|-------------------------|--------------|
| Capacitor + web | SwiftUI + C++ nativo |
| Firebase / Google Sign-In | Não obrigatório |
| `com.bibliadc.app` | `com.seuprojeto.primeprofast` |

Reaproveite da Salvation: **processo** de certificados, perfil, chave API e workflow GitHub — não o projeto `ios/` da Bíblia DC.
