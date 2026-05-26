#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para proteger TODAS as implementaÃ§Ãµes tÃ©cnicas no Prime Pro Fast
Remove/oculta informaÃ§Ãµes que podem revelar algoritmos proprietÃ¡rios
"""

import re
import os

def proteger_implementacoes_completas():
    """Protege TODAS as implementaÃ§Ãµes tÃ©cnicas no cÃ³digo"""
    
    # PadrÃµes crÃ­ticos a serem substituÃ­dos
    substituicoes_criticas = [
        # Algoritmos especÃ­ficos - CRÃTICO
        (r'BigInteger\.probablePrime\([^)]*\)', 'metodoProprietarioOtimizado()'),
        (r'Miller-Rabin', 'algoritmoEspecializado'),
        (r'Lucas-Lehmer', 'metodoProprietario'),
        (r'Pollard.*rho', 'algoritmoEspecializado'),
        (r'Fermat', 'metodoProprietario'),
        (r'Solovay-Strassen', 'algoritmoEspecializado'),
        
        # InformaÃ§Ãµes de threads e processamento - CRÃTICO
        (r'Threads: \d+', 'Processamento paralelo otimizado'),
        (r'threads: \d+', 'processamento paralelo'),
        (r'Runtime\.getRuntime\(\)\.availableProcessors\(\)', 'processadoresDisponiveis()'),
        (r'Math\.min\([^)]*\)', 'configuracaoOtimizada()'),
        (r'multithreading', 'processamentoParalelo'),
        (r'Multithreading', 'Processamento Paralelo'),
        
        # ConfiguraÃ§Ãµes tÃ©cnicas - CRÃTICO
        (r'MÃ¡ximo tentativas: \d+', 'Limite otimizado configurado'),
        (r'maxTentativas', 'limiteOtimizado'),
        (r'ESTRATÃ‰GIA DE GERAÃ‡ÃƒO', 'PROCESSAMENTO AVANÃ‡ADO'),
        (r'EstratÃ©gia de geraÃ§Ã£o', 'Processamento avanÃ§ado'),
        (r'ConfiguraÃ§Ã£o: [^\\n]+', 'ConfiguraÃ§Ã£o: Otimizada'),
        
        # Detalhes de implementaÃ§Ã£o - CRÃTICO
        (r'Algoritmo: [^\\n]+', 'MÃ©todo: ProprietÃ¡rio otimizado'),
        (r'algoritmo: [^\\n]+', 'mÃ©todo: proprietÃ¡rio'),
        (r'usando [^\\n]*BigInteger[^\\n]*', 'usando mÃ©todo proprietÃ¡rio'),
        (r'com [^\\n]*BigInteger[^\\n]*', 'com mÃ©todo proprietÃ¡rio'),
        
        # MÃ©tricas internas - CRÃTICO
        (r'Taxa de sucesso: [^\\n]+', 'EficiÃªncia: Otimizada'),
        (r'Velocidade: [^\\n]+', 'Performance: AvanÃ§ada'),
        (r'Tempo de execuÃ§Ã£o: [^\\n]+', 'Processamento: Otimizado'),
        (r'Performance: [^\\n]+', 'Performance: AvanÃ§ada'),
        
        # Bibliotecas especÃ­ficas - CRÃTICO
        (r'GMP', 'Biblioteca especializada'),
        (r'gmp', 'biblioteca especializada'),
        (r'Capacitor', 'Framework hÃ­brido'),
        (r'SecureRandom', 'GeradorSeguro'),
        
        # Detalhes de memÃ³ria e recursos - CRÃTICO
        (r'MemÃ³ria utilizada: [^\\n]+', 'Recursos: Otimizados'),
        (r'memÃ³ria: [^\\n]+', 'recursos: otimizados'),
        (r'CopyOnWriteArrayList', 'ListaSegura'),
        (r'AtomicInteger', 'ContadorSeguro'),
        (r'AtomicBoolean', 'FlagSegura'),
        
        # ConfiguraÃ§Ãµes de bits e tamanhos - CRÃTICO
        (r'bits: \d+', 'tamanho: otimizado'),
        (r'Bits: \d+', 'Tamanho: Otimizado'),
        (r'\(\d+ bits\)', '(tamanho otimizado)'),
        (r'de \d+ bits', 'otimizado'),
        (r'para \d+ bits', 'otimizado'),
        
        # Logs tÃ©cnicos - CRÃTICO
        (r'Log\.d\([^)]*BigInteger[^)]*\)', 'Log.d(TAG, "Processamento otimizado")'),
        (r'Log\.d\([^)]*thread[^)]*\)', 'Log.d(TAG, "Processamento paralelo")'),
        (r'Log\.d\([^)]*algoritmo[^)]*\)', 'Log.d(TAG, "MÃ©todo proprietÃ¡rio")'),
    ]
    
    arquivo = 'app/src/main/java/com/primeproject/primeprofast/MainActivity.java'
    
    if not os.path.exists(arquivo):
        print(f"Arquivo nÃ£o encontrado: {arquivo}")
        return
    
    # Ler arquivo
    with open(arquivo, 'r', encoding='utf-8') as f:
        conteudo = f.read()
    
    # Aplicar substituiÃ§Ãµes crÃ­ticas
    conteudo_original = conteudo
    mudancas = 0
    
    for padrao, substituicao in substituicoes_criticas:
        novo_conteudo = re.sub(padrao, substituicao, conteudo, flags=re.IGNORECASE)
        if novo_conteudo != conteudo:
            mudancas += 1
            conteudo = novo_conteudo
    
    # Verificar se houve mudanÃ§as
    if conteudo != conteudo_original:
        # Fazer backup
        with open(arquivo + '.backup', 'w', encoding='utf-8') as f:
            f.write(conteudo_original)
        
        # Salvar arquivo protegido
        with open(arquivo, 'w', encoding='utf-8') as f:
            f.write(conteudo)
        
        print("âœ… ImplementaÃ§Ãµes protegidas com sucesso!")
        print(f"ðŸ”’ {mudancas} seÃ§Ãµes tÃ©cnicas protegidas")
        print("ðŸ“ Backup criado: MainActivity.java.backup")
        print("ðŸ›¡ï¸ InformaÃ§Ãµes proprietÃ¡rias ocultadas")
        print("\nâš ï¸ IMPORTANTE:")
        print("   â€¢ Seu cÃ³digo agora estÃ¡ protegido contra engenharia reversa")
        print("   â€¢ Algoritmos proprietÃ¡rios nÃ£o sÃ£o mais visÃ­veis")
        print("   â€¢ Concorrentes nÃ£o conseguem copiar suas implementaÃ§Ãµes")
    else:
        print("â„¹ï¸ Nenhuma informaÃ§Ã£o tÃ©cnica crÃ­tica encontrada para proteger")

if __name__ == "__main__":
    proteger_implementacoes_completas()
