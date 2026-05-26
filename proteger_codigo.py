#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para proteger implementaÃ§Ãµes tÃ©cnicas no Prime Pro Fast
Remove/oculta informaÃ§Ãµes que podem revelar algoritmos proprietÃ¡rios
"""

import re
import os

def proteger_implementacoes():
    """Protege implementaÃ§Ãµes tÃ©cnicas no cÃ³digo"""
    
    # PadrÃµes a serem substituÃ­dos
    substituicoes = [
        # Algoritmos especÃ­ficos
        (r'BigInteger\.probablePrime\(\)', 'mÃ©todo proprietÃ¡rio otimizado'),
        (r'Miller-Rabin', 'algoritmo especializado'),
        (r'Lucas-Lehmer', 'mÃ©todo proprietÃ¡rio'),
        (r'Pollard.*rho', 'algoritmo especializado'),
        
        # InformaÃ§Ãµes de threads
        (r'Threads: \d+', 'Processamento paralelo otimizado'),
        (r'threads: \d+', 'processamento paralelo'),
        (r'Runtime\.getRuntime\(\)\.availableProcessors\(\)', 'processadores disponÃ­veis'),
        
        # ConfiguraÃ§Ãµes tÃ©cnicas
        (r'MÃ¡ximo tentativas: \d+', 'Limite otimizado configurado'),
        (r'maxTentativas', 'limiteOtimizado'),
        (r'ESTRATÃ‰GIA DE GERAÃ‡ÃƒO', 'PROCESSAMENTO AVANÃ‡ADO'),
        (r'EstratÃ©gia de geraÃ§Ã£o', 'Processamento avanÃ§ado'),
        
        # Detalhes de implementaÃ§Ã£o
        (r'Algoritmo: [^\\n]+', 'MÃ©todo: ProprietÃ¡rio otimizado'),
        (r'algoritmo: [^\\n]+', 'mÃ©todo: proprietÃ¡rio'),
        (r'ConfiguraÃ§Ã£o: [^\\n]+', 'ConfiguraÃ§Ã£o: Otimizada'),
        
        # MÃ©tricas internas
        (r'Taxa de sucesso: [^\\n]+', 'EficiÃªncia: Otimizada'),
        (r'Velocidade: [^\\n]+', 'Performance: AvanÃ§ada'),
        (r'Tempo de execuÃ§Ã£o: [^\\n]+', 'Processamento: Otimizado'),
        
        # Bibliotecas especÃ­ficas
        (r'GMP', 'Biblioteca especializada'),
        (r'gmp', 'biblioteca especializada'),
        (r'Capacitor', 'Framework hÃ­brido'),
        
        # Detalhes de memÃ³ria
        (r'MemÃ³ria utilizada: [^\\n]+', 'Recursos: Otimizados'),
        (r'memÃ³ria: [^\\n]+', 'recursos: otimizados'),
    ]
    
    arquivo = 'app/src/main/java/com/primeproject/primeprofast/MainActivity.java'
    
    if not os.path.exists(arquivo):
        print(f"Arquivo nÃ£o encontrado: {arquivo}")
        return
    
    # Ler arquivo
    with open(arquivo, 'r', encoding='utf-8') as f:
        conteudo = f.read()
    
    # Aplicar substituiÃ§Ãµes
    conteudo_original = conteudo
    for padrao, substituicao in substituicoes:
        conteudo = re.sub(padrao, substituicao, conteudo, flags=re.IGNORECASE)
    
    # Verificar se houve mudanÃ§as
    if conteudo != conteudo_original:
        # Fazer backup
        with open(arquivo + '.backup', 'w', encoding='utf-8') as f:
            f.write(conteudo_original)
        
        # Salvar arquivo protegido
        with open(arquivo, 'w', encoding='utf-8') as f:
            f.write(conteudo)
        
        print("âœ… ImplementaÃ§Ãµes protegidas com sucesso!")
        print("ðŸ“ Backup criado: MainActivity.java.backup")
        print("ðŸ”’ InformaÃ§Ãµes tÃ©cnicas ocultadas")
    else:
        print("â„¹ï¸ Nenhuma informaÃ§Ã£o tÃ©cnica encontrada para proteger")

if __name__ == "__main__":
    proteger_implementacoes()
