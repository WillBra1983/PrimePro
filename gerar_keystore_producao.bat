@echo off
echo Gerando keystore de producao para Google Play...
echo.
echo IMPORTANTE: As senhas nao aparecerao na tela por seguranca!
echo Digite normalmente mesmo que nao veja os caracteres.
echo.

REM Criar diretorio keystore se nao existir
if not exist "keystore" mkdir keystore

echo Digite as informacoes solicitadas:
echo.

REM Gerar keystore de producao com informacoes predefinidas
keytool -genkey -v -keystore keystore/primeprofast-release.keystore -alias primeprofast -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Wilson Lucas Ferreira, OU=Desenvolvimento, O=PrimeProFast, L=Porto Velho, ST=Rondonia, C=BR"

echo.
echo Keystore gerado com sucesso!
echo Localizacao: keystore/primeprofast-release.keystore
echo.
echo IMPORTANTE: Guarde as senhas e o arquivo keystore em local seguro!
echo Este arquivo sera necessario para futuras atualizacoes no Google Play.
echo.
echo Agora configure as senhas no arquivo keystore.properties
echo.
pause
