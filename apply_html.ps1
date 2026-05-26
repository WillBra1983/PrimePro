# Script para aplicar HTML bonito a todos os módulos do PrimeProFast
Write-Host "Aplicando HTML bonito a todos os módulos..."

# Ler o arquivo
 = Get-Content "app/src/main/java/com/primeproject/primeprofast/MainActivity.java" -Raw

# Aplicar HTML bonito a todos os módulos
 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "conjectura_legendre_continuacao"\);', 'String htmlBonito = gerarHTMLResultado("Conjectura de Legendre", resultado.toString(), "Verificação Contínua"); salvarResultadoTemporario(htmlBonito, "conjectura_legendre_continuacao");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "conjectura_legendre_parada_usuario"\);', 'String htmlBonito = gerarHTMLResultado("Conjectura de Legendre", resultado.toString(), "Parada pelo Usuário"); salvarResultadoTemporario(htmlBonito, "conjectura_legendre_parada_usuario");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "conjectura_legendre_falha"\);', 'String htmlBonito = gerarHTMLResultado("Conjectura de Legendre", resultado.toString(), "Falha na Verificação"); salvarResultadoTemporario(htmlBonito, "conjectura_legendre_falha");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "conjectura_legendre_confirmada"\);', 'String htmlBonito = gerarHTMLResultado("Conjectura de Legendre", resultado.toString(), "Conjectura Confirmada"); salvarResultadoTemporario(htmlBonito, "conjectura_legendre_confirmada");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "conjectura_legendre_intervalo"\);', 'String htmlBonito = gerarHTMLResultado("Conjectura de Legendre", resultado.toString(), "Verificação por Intervalo"); salvarResultadoTemporario(htmlBonito, "conjectura_legendre_intervalo");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "numeros_mersenne"\);', 'String htmlBonito = gerarHTMLResultado("Números de Mersenne", resultado.toString(), "Busca de Primos de Mersenne"); salvarResultadoTemporario(htmlBonito, "numeros_mersenne");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "numeros_perfeitos_busca_sequencial"\);', 'String htmlBonito = gerarHTMLResultado("Números Perfeitos", resultado.toString(), "Busca Sequencial"); salvarResultadoTemporario(htmlBonito, "numeros_perfeitos_busca_sequencial");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "assinatura_digital"\);', 'String htmlBonito = gerarHTMLResultado("Segurança Digital", resultado.toString(), "Assinatura Digital"); salvarResultadoTemporario(htmlBonito, "assinatura_digital");'

 =  -replace 'salvarResultadoTemporario\(resultado\.toString\(\), "estatisticas_primos_completas"\);', 'String htmlBonito = gerarHTMLResultado("Teste de Primalidade", resultado.toString(), "Estatísticas Completas"); salvarResultadoTemporario(htmlBonito, "estatisticas_primos_completas");'

# Salvar o arquivo modificado
Set-Content "app/src/main/java/com/primeproject/primeprofast/MainActivity.java" -Value 

Write-Host "HTML bonito aplicado a todos os módulos com sucesso!"
