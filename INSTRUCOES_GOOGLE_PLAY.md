# 📱 Instruções para Publicação no Google Play

## 🔐 Passo 1: Gerar Keystore de Produção

1. **Execute o script de geração do keystore:**
   ```bash
   gerar_keystore_producao.bat
   ```

2. **Configure as informações solicitadas:**
   - Nome e sobrenome
   - Nome da unidade organizacional
   - Nome da cidade
   - Nome do estado/província
   - Código do país (ex: BR)
   - **SENHA DO KEYSTORE** (guarde em local seguro!)
   - **SENHA DA CHAVE** (pode ser a mesma do keystore)

## 🔧 Passo 2: Configurar Credenciais

1. **Edite o arquivo `keystore.properties`:**
   ```properties
   storeFile=keystore/primeprofast-release.keystore
   storePassword=SUA_SENHA_DO_KEYSTORE
   keyAlias=primeprofast
   keyPassword=SUA_SENHA_DA_CHAVE
   ```

2. **Substitua as senhas** pelas que você definiu no Passo 1

## 🚀 Passo 3: Gerar AAB

1. **Execute o script de geração do AAB:**
   ```bash
   gerar_aab_producao.bat
   ```

2. **O AAB será gerado em:**
   ```
   app\build\outputs\bundle\release\app-release.aab
   ```

## 📋 Passo 4: Upload no Google Play Console

1. **Acesse:** [Google Play Console](https://play.google.com/console)

2. **Crie um novo app** ou selecione um existente

3. **Vá para:** Produção → Versões do app

4. **Clique em:** Criar nova versão

5. **Faça upload do arquivo:** `app-release.aab`

6. **Preencha as informações:**
   - Nome da versão: 1.0 (1)
   - Notas da versão: Descrição das funcionalidades

7. **Revise e publique**

## ⚠️ IMPORTANTE - Segurança

- **GUARDE O KEYSTORE EM LOCAL SEGURO!**
- **FAÇA BACKUP** do arquivo `keystore/primeprofast-release.keystore`
- **NÃO COMMITE** o arquivo `keystore.properties` no Git
- **ANOTE AS SENHAS** em local seguro

## 🔄 Atualizações Futuras

Para futuras atualizações, use sempre o **mesmo keystore**:
- Mantenha o arquivo `keystore/primeprofast-release.keystore`
- Use as mesmas senhas no `keystore.properties`
- Execute `gerar_aab_producao.bat` novamente

## 📊 Informações do App

- **Package Name:** com.seuprojeto.primeprofast
- **Version Code:** 1
- **Version Name:** 1.0
- **Target SDK:** 34
- **Min SDK:** 21
- **Architecture:** arm64-v8a

## 🛠️ Solução de Problemas

### Erro de Keystore
- Verifique se o arquivo `keystore.properties` existe
- Confirme se as senhas estão corretas
- Execute `gerar_keystore_producao.bat` novamente

### Erro de Compilação
- Execute `.\gradlew clean` antes de gerar o AAB
- Verifique se todas as dependências estão instaladas

### AAB Muito Grande
- O AAB já está otimizado com:
  - Minificação habilitada
  - Shrink resources habilitado
  - Apenas arquitetura arm64-v8a
