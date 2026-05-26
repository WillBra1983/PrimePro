#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para proteger APENAS os resultados exibidos ao usuÃ¡rio
Sem quebrar o cÃ³digo funcional
"""

import re
import os

def proteger_apenas_resultados():
    """Protege apenas os resultados exibidos, sem quebrar o cÃ³digo"""
    
    # PadrÃµes SEGUROS - apenas em strings de resultado
    substituicoes_seguras = [
        # Proteger apenas em append() de resultados
        (r'resultado\.append\("ðŸ”§ ESTRATÃ‰GIA DE GERAÃ‡ÃƒO:"\)', 'resultado.append("ðŸš€ PROCESSAMENTO AVANÃ‡ADO:")'),
        (r'resultado\.append\("   â€¢ Algoritmo: [^"]*"\)', 'resultado.append("   â€¢ MÃ©todo: ProprietÃ¡rio otimizado")'),
        (r'resultado\.append\("   â€¢ Threads: [^"]*"\)', 'resultado.append("   â€¢ Processamento: Paralelo especializado")'),
        (r'resultado\.append\("   â€¢ MÃ¡ximo tentativas: [^"]*"\)', 'resultado.append("   â€¢ ConfiguraÃ§Ã£o: Limite otimizado")'),
        
        # Proteger em logs de debug (nÃ£o crÃ­ticos)
        (r'Log\.d\(TAG, "[^"]*BigInteger[^"]*"\)', 'Log.d(TAG, "Processamento otimizado")'),
        (r'Log\.d\(TAG, "[^"]*thread[^"]*"\)', 'Log.d(TAG, "Processamento paralelo")'),
        
        # Proteger em descriÃ§Ãµes de tutorial
        (r'BigInteger\.probablePrime\(\)', 'mÃ©todo proprietÃ¡rio otimizado'),
        (r'Miller-Rabin', 'algoritmo especializado'),
        (r'Lucas-Lehmer', 'mÃ©todo proprietÃ¡rio'),
    ]
    
    arquivo = 'app/src/main/java/com/primeproject/primeprofast/MainActivity.java'
    
    if not os.path.exists(arquivo):
        print(f"Arquivo nÃ£o encontrado: {arquivo}")
        return
    
    # Ler arquivo
    with open(arquivo, 'r', encoding='utf-8') as f:
        conteudo = f.read()
    
    # Aplicar substituiÃ§Ãµes SEGURAS
    conteudo_original = conteudo
    mudancas = 0
    
    for padrao, substituicao in substituicoes_seguras:
        novo_conteudo = re.sub(padrao, substituicao, conteudo)
        if novo_conteudo != conteudo:
            mudancas += 1
            conteudo = novo_conteudo
    
    # Verificar se houve mudanÃ§as
    if conteudo != conteudo_original:
        # Fazer backup
        with open(arquivo + '.backup2', 'w', encoding='utf-8') as f:
            f.write(conteudo_original)
        
        # Salvar arquivo protegido
        with open(arquivo, 'w', encoding='utf-8') as f:
            f.write(conteudo)
        
        print("âœ… ProteÃ§Ã£o SEGURA aplicada!")
        print(f"ðŸ”’ {mudancas} seÃ§Ãµes de resultado protegidas")
        print("ðŸ“ Backup criado: MainActivity.java.backup2")
        print("ðŸ›¡ï¸ CÃ³digo funcional preservado")
    else:
        print("â„¹ï¸ Nenhuma seÃ§Ã£o de resultado encontrada para proteger")

if __name__ == "__main__":
    proteger_apenas_resultados()
