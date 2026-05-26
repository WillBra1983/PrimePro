from __future__ import annotations

import argparse
import math
import os
import secrets
import time
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple


# ============================================================
# CONFIGURAÇÃO
# ============================================================

SMALL_PRIMES = [
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29,
    31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
    73, 79, 83, 89, 97
]

GOOD_RESIDUES_MOD30 = {1, 7, 11, 13, 17, 19, 23, 29}


# ============================================================
# PRIMALIDADE
# ============================================================

def is_probable_prime(n: int, rounds: int = 16) -> bool:
    """
    Miller-Rabin probabilístico.
    """
    if n < 2:
        return False

    for p in SMALL_PRIMES:
        if n == p:
            return True
        if n % p == 0:
            return False

    d = n - 1
    s = 0
    while d % 2 == 0:
        d //= 2
        s += 1

    for _ in range(rounds):
        a = secrets.randbelow(n - 3) + 2  # 2 <= a <= n-2
        x = pow(a, d, n)

        if x == 1 or x == n - 1:
            continue

        skip_to_next_round = False
        for _ in range(s - 1):
            x = pow(x, 2, n)
            if x == n - 1:
                skip_to_next_round = True
                break

        if skip_to_next_round:
            continue

        return False

    return True


def smallest_factor(n: int, max_check: int = 100_000) -> Optional[int]:
    """
    Só para relatório em números menores/depuração.
    """
    if n < 2:
        return None

    for p in SMALL_PRIMES:
        if n == p:
            return p
        if n % p == 0:
            return p

    limit = min(math.isqrt(n), max_check)
    k = 101
    while k <= limit:
        if n % k == 0:
            return k
        k += 2

    return None


# ============================================================
# FILTROS RÁPIDOS
# ============================================================

def passes_fast_filters(n: int) -> bool:
    if n < 2:
        return False
    if n in (2, 3, 5):
        return True
    if n % 2 == 0:
        return False
    if n % 3 == 0:
        return False
    if n % 5 == 0:
        return False
    return (n % 30) in GOOD_RESIDUES_MOD30


# ============================================================
# DENSIDADE
# ============================================================

@dataclass
class DensityInfo:
    raw_ln: float
    floor_ln: int
    round_ln: int
    ceil_ln: int


def density_info(n: int) -> DensityInfo:
    if n < 3:
        raw = 2.0
    else:
        raw = math.log(n)

    return DensityInfo(
        raw_ln=raw,
        floor_ln=max(1, math.floor(raw)),
        round_ln=max(1, round(raw)),
        ceil_ln=max(1, math.ceil(raw)),
    )


def valid_offset_for_prime(base: int, offset: int) -> bool:
    """
    Todo primo maior que 2 é ímpar.
    """
    return ((base + offset) % 2) == 1


def unique_preserve_order(values: List[int]) -> List[int]:
    seen = set()
    result: List[int] = []
    for v in values:
        if v not in seen:
            seen.add(v)
            result.append(v)
    return result


def build_density_offsets(base: int, info: DensityInfo, shell_multiplier: int = 1) -> List[int]:
    offsets: List[int] = []

    # camada 1: curtos
    for k in [1, 2]:
        if valid_offset_for_prime(base, +k):
            offsets.append(+k)
        if valid_offset_for_prime(base, -k):
            offsets.append(-k)

    anchors = unique_preserve_order([info.floor_ln, info.round_ln, info.ceil_ln])

    # camada 2: âncoras
    for a in anchors:
        if valid_offset_for_prime(base, +a):
            offsets.append(+a)
        if valid_offset_for_prime(base, -a):
            offsets.append(-a)

    # camada 3: vizinhança
    for a in anchors:
        for delta in [1, 2]:
            kp = a + delta
            km = a - delta
            if km > 0:
                if valid_offset_for_prime(base, +km):
                    offsets.append(+km)
                if valid_offset_for_prime(base, -km):
                    offsets.append(-km)
            if valid_offset_for_prime(base, +kp):
                offsets.append(+kp)
            if valid_offset_for_prime(base, -kp):
                offsets.append(-kp)

    # camada 4: casca completa até ceil(ln(n)) * multiplicador
    max_radius = max(1, info.ceil_ln * shell_multiplier)
    for k in range(1, max_radius + 1):
        if valid_offset_for_prime(base, +k):
            offsets.append(+k)
        if valid_offset_for_prime(base, -k):
            offsets.append(-k)

    offsets = unique_preserve_order(offsets)
    offsets.sort(key=lambda x: (abs(x), 0 if x > 0 else 1))
    return offsets


# ============================================================
# GERAÇÃO DAS BASES ÍMPARES TERMINADAS EM 1
# ============================================================

def random_odd_ending_1_exact_bits(bits: int) -> int:
    """
    Gera um número ímpar terminado em 1 com número exato de bits.
    Não exige primalidade.
    """
    if bits < 4:
        raise ValueError("bits deve ser >= 4")

    while True:
        n = secrets.randbits(bits)
        n |= (1 << (bits - 1))   # garante tamanho exato
        n |= 1                   # garante ímpar

        # ajusta para terminar em 1
        # anda de 2 em 2 até chegar em final 1
        while n % 10 != 1:
            n += 2
            if n.bit_length() != bits:
                break

        if n.bit_length() == bits and n % 10 == 1:
            return n


# ============================================================
# MODELOS DO SEU MÉTODO
# ============================================================

def build_product_models(p: int, q: int) -> Dict[str, int]:
    pq = p * q
    return {
        "P1 pq-2": pq - 2,
        "P2 pq+2": pq + 2,
        "M1 pq+p+q-4": pq + p + q - 4,
        "M2 pq+p+q": pq + p + q,
        "M3 pq+p+q+4": pq + p + q + 4,
        "F1 pq+p+3q-4": pq + p + 3 * q - 4,
        "F2 pq+p+3q": pq + p + 3 * q,
        "F3 pq+p+3q+4": pq + p + 3 * q + 4,
    }


# ============================================================
# BUSCA DE PRIMO A PARTIR DE UM CANDIDATO
# ============================================================

@dataclass
class CandidateSearchResult:
    model_name: str
    base_value: int
    base_bits: int
    exact_prime: bool
    found_prime: Optional[int]
    found_offset: Optional[int]
    attempts: int
    density: DensityInfo
    elapsed: float


def search_prime_from_candidate(
    model_name: str,
    base_value: int,
    rounds: int,
    shell_multiplier: int,
    max_candidate_tests: int
) -> CandidateSearchResult:
    started = time.time()
    info = density_info(base_value)
    attempts = 0

    # teste exato
    attempts += 1
    exact = False
    if passes_fast_filters(base_value) and is_probable_prime(base_value, rounds=rounds):
        exact = True
        return CandidateSearchResult(
            model_name=model_name,
            base_value=base_value,
            base_bits=base_value.bit_length(),
            exact_prime=True,
            found_prime=base_value,
            found_offset=0,
            attempts=attempts,
            density=info,
            elapsed=time.time() - started,
        )

    offsets = build_density_offsets(base_value, info, shell_multiplier=shell_multiplier)

    for offset in offsets:
        if attempts >= max_candidate_tests:
            break

        candidate = base_value + offset
        attempts += 1

        if not passes_fast_filters(candidate):
            continue

        if is_probable_prime(candidate, rounds=rounds):
            return CandidateSearchResult(
                model_name=model_name,
                base_value=base_value,
                base_bits=base_value.bit_length(),
                exact_prime=False,
                found_prime=candidate,
                found_offset=offset,
                attempts=attempts,
                density=info,
                elapsed=time.time() - started,
            )

    return CandidateSearchResult(
        model_name=model_name,
        base_value=base_value,
        base_bits=base_value.bit_length(),
        exact_prime=False,
        found_prime=None,
        found_offset=None,
        attempts=attempts,
        density=info,
        elapsed=time.time() - started,
    )


# ============================================================
# UMA TENTATIVA COMPLETA PARA UM TAMANHO EM BITS
# ============================================================

@dataclass
class BitRunResult:
    target_bits: int
    p: int
    q: int
    p_bits: int
    q_bits: int
    searched_models: List[CandidateSearchResult]
    winner: Optional[CandidateSearchResult]
    total_elapsed: float


def run_one_bit_target(
    target_bits: int,
    rounds: int,
    shell_multiplier: int,
    max_candidate_tests: int
) -> BitRunResult:
    started = time.time()

    p_bits = target_bits // 2
    q_bits = target_bits - p_bits

    p = random_odd_ending_1_exact_bits(p_bits)
    q = random_odd_ending_1_exact_bits(q_bits)

    models = build_product_models(p, q)

    searched: List[CandidateSearchResult] = []
    winner: Optional[CandidateSearchResult] = None

    # procura modelos com bit length mais próximo do alvo primeiro
    items = list(models.items())
    items.sort(key=lambda kv: (abs(kv[1].bit_length() - target_bits), kv[0]))

    for name, value in items:
        result = search_prime_from_candidate(
            model_name=name,
            base_value=value,
            rounds=rounds,
            shell_multiplier=shell_multiplier,
            max_candidate_tests=max_candidate_tests,
        )
        searched.append(result)

        if result.found_prime is not None and result.found_prime.bit_length() == target_bits:
            winner = result
            break

    return BitRunResult(
        target_bits=target_bits,
        p=p,
        q=q,
        p_bits=p_bits,
        q_bits=q_bits,
        searched_models=searched,
        winner=winner,
        total_elapsed=time.time() - started,
    )


# ============================================================
# RELATÓRIO
# ============================================================

def format_candidate_result(r: CandidateSearchResult) -> str:
    lines: List[str] = []
    lines.append(f"Modelo: {r.model_name}")
    lines.append(f"Base bits: {r.base_bits}")
    lines.append(f"Base valor: {r.base_value}")
    lines.append(
        f"Densidade: ln={r.density.raw_ln:.6f} | floor={r.density.floor_ln} | "
        f"round={r.density.round_ln} | ceil={r.density.ceil_ln}"
    )
    lines.append(f"Tentativas no modelo: {r.attempts}")
    lines.append(f"Tempo no modelo: {r.elapsed:.6f} s")

    if r.found_prime is not None:
        sign = "+" if (r.found_offset or 0) >= 0 else ""
        kind = "PRIMO EXATO" if r.exact_prime else "PRIMO PRÓXIMO"
        lines.append(f"Resultado: {kind}")
        lines.append(f"Primo encontrado bits: {r.found_prime.bit_length()}")
        lines.append(f"Primo encontrado offset: {sign}{r.found_offset}")
        lines.append(f"Primo encontrado valor: {r.found_prime}")
    else:
        lines.append("Resultado: NÃO ENCONTROU PRIMO NESTE MODELO")

    return "\n".join(lines)


def format_run_result(run: BitRunResult) -> str:
    lines: List[str] = []
    lines.append("=" * 100)
    lines.append(f"ALVO: {run.target_bits} bits")
    lines.append("=" * 100)
    lines.append(f"p bits: {run.p_bits}")
    lines.append(f"q bits: {run.q_bits}")
    lines.append(f"p (terminado em 1): {run.p}")
    lines.append(f"q (terminado em 1): {run.q}")
    lines.append("-" * 100)

    for res in run.searched_models:
        lines.append(format_candidate_result(res))
        lines.append("-" * 100)

    if run.winner is not None:
        lines.append("VENCEDOR:")
        lines.append(f"Modelo: {run.winner.model_name}")
        lines.append(f"Bits do primo: {run.winner.found_prime.bit_length() if run.winner.found_prime else 'N/A'}")
        lines.append(f"Offset: {run.winner.found_offset}")
    else:
        lines.append("VENCEDOR: NENHUM MODELO ENTREGOU PRIMO NO TAMANHO EXATO")

    lines.append(f"Tempo total da rodada: {run.total_elapsed:.6f} s")
    lines.append("")

    return "\n".join(lines)


def save_text(path: str, text: str) -> None:
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


# ============================================================
# BENCHMARK EM LOTE
# ============================================================

def parse_bit_list(text: str) -> List[int]:
    return [int(x.strip()) for x in text.split(",") if x.strip()]


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Benchmark do seu modelo para encontrar primos grandes até 32768 bits."
    )
    parser.add_argument(
        "--bit-list",
        type=str,
        default="1024,2048,4096,8192,16384,32768",
        help="Lista de tamanhos em bits separados por vírgula."
    )
    parser.add_argument("--rounds", type=int, default=16, help="Rodadas do Miller-Rabin.")
    parser.add_argument(
        "--shell-multiplier",
        type=int,
        default=1,
        help="Multiplicador da casca derivada de ceil(ln(n))."
    )
    parser.add_argument(
        "--max-candidate-tests",
        type=int,
        default=5000,
        help="Máximo de candidatos testados por modelo."
    )
    parser.add_argument(
        "--output",
        type=str,
        default="benchmark_modelo_primos_bits.txt",
        help="Arquivo txt de saída."
    )
    args = parser.parse_args()

    bit_list = parse_bit_list(args.bit_list)
    started_all = time.time()

    full_report: List[str] = []
    full_report.append("BENCHMARK DO MODELO PARA PRIMOS GRANDES")
    full_report.append("=" * 100)
    full_report.append(f"Bit list: {bit_list}")
    full_report.append(f"Rounds MR: {args.rounds}")
    full_report.append(f"Shell multiplier: {args.shell_multiplier}")
    full_report.append(f"Max candidate tests per model: {args.max_candidate_tests}")
    full_report.append("")

    summary_lines: List[str] = []
    summary_lines.append("RESUMO")
    summary_lines.append("-" * 100)

    for bits in bit_list:
        print(f"[INÍCIO] {bits} bits...", flush=True)

        run = run_one_bit_target(
            target_bits=bits,
            rounds=args.rounds,
            shell_multiplier=args.shell_multiplier,
            max_candidate_tests=args.max_candidate_tests,
        )

        full_report.append(format_run_result(run))

        if run.winner is not None and run.winner.found_prime is not None:
            summary_lines.append(
                f"{bits} bits | OK | modelo={run.winner.model_name} | "
                f"offset={run.winner.found_offset} | tempo={run.total_elapsed:.6f}s"
            )
        else:
            summary_lines.append(
                f"{bits} bits | FALHOU NO TAMANHO EXATO | tempo={run.total_elapsed:.6f}s"
            )

        print(f"[FIM] {bits} bits em {run.total_elapsed:.6f}s", flush=True)

    total_elapsed = time.time() - started_all
    summary_lines.append("-" * 100)
    summary_lines.append(f"Tempo total do benchmark: {total_elapsed:.6f}s")

    full_report.append("\n".join(summary_lines))
    report_text = "\n".join(full_report)

    save_text(args.output, report_text)

    print("\n" + "\n".join(summary_lines))
    print(f"\nRelatório salvo em: {args.output}")


if __name__ == "__main__":
    main()