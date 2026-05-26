from __future__ import annotations

import argparse
import json
import math
import os
import time
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple


# ============================================================
# PRIMALIDADE
# ============================================================

_SMALL_PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47]


def is_prime(n: int) -> bool:
    if n < 2:
        return False

    for p in _SMALL_PRIMES:
        if n == p:
            return True
        if n % p == 0:
            return False

    d = n - 1
    s = 0
    while d % 2 == 0:
        s += 1
        d //= 2

    def check(a: int) -> bool:
        x = pow(a, d, n)
        if x == 1 or x == n - 1:
            return True
        for _ in range(s - 1):
            x = pow(x, 2, n)
            if x == n - 1:
                return True
        return False

    for a in [2, 3, 5, 7, 11, 13, 17]:
        if a >= n:
            continue
        if not check(a):
            return False
    return True


def smallest_factor(n: int, max_check: int = 200_000) -> Optional[int]:
    if n < 2:
        return None
    if n % 2 == 0:
        return 2
    if n % 3 == 0:
        return 3
    if n % 5 == 0:
        return 5

    limit = min(math.isqrt(n), max_check)
    f = 7
    step_pattern = [4, 2, 4, 2, 4, 6, 2, 6]
    idx = 0

    while f <= limit:
        if n % f == 0:
            return f
        f += step_pattern[idx]
        idx = (idx + 1) % len(step_pattern)

    return None


# ============================================================
# ÍMPARES TERMINADOS EM 1
# ============================================================

def next_odd_ending_in_1_after(n: int) -> int:
    """
    Próximo número ímpar terminado em 1, sem exigir primalidade.
    Exemplos: 11, 21, 31, 41, ...
    """
    candidate = max(11, n + 1)
    while candidate % 10 != 1:
        candidate += 1
    return candidate


# ============================================================
# MODELOS
# ============================================================

def build_models(p: int, q: int) -> Dict[str, int]:
    pq = p * q
    return {
        "S1 p+q-1": p + q - 1,
        "S2 p+q+1": p + q + 1,
        "D1 q-p-1": q - p - 1,
        "D2 q-p+1": q - p + 1,
        "L1 2p-1": 2 * p - 1,
        "L2 2p+1": 2 * p + 1,
        "L3 2q-1": 2 * q - 1,
        "L4 2q+1": 2 * q + 1,
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
# FILTROS
# ============================================================

GOOD_RESIDUES_MOD30 = {1, 7, 11, 13, 17, 19, 23, 29}


def passes_fast_mod_filters(n: int) -> bool:
    if n < 2:
        return False
    if n in (2, 3, 5):
        return True
    if n % 2 == 0 or n % 3 == 0 or n % 5 == 0:
        return False
    return (n % 30) in GOOD_RESIDUES_MOD30


def blocked_pair_mod120(p: int, q: int, blocked_residues: set[int]) -> bool:
    return ((q - p) % 120) in blocked_residues


def blocked_pair_mod3(p: int, q: int, use_mod3_filter: bool) -> bool:
    if not use_mod3_filter:
        return False
    return (p % 3) == (q % 3)


# ============================================================
# DENSIDADE + ARREDONDAMENTOS
# ============================================================

@dataclass
class DensityInfo:
    raw_ln: float
    floor_ln: int
    round_ln: int
    ceil_ln: int
    shell_radius: int


def density_info(n: int) -> DensityInfo:
    if n < 3:
        raw = 2.0
    else:
        raw = math.log(n)

    fl = max(1, math.floor(raw))
    rd = max(1, round(raw))
    cl = max(1, math.ceil(raw))
    shell = cl

    return DensityInfo(
        raw_ln=raw,
        floor_ln=fl,
        round_ln=rd,
        ceil_ln=cl,
        shell_radius=shell,
    )


def valid_offset_for_prime(base: int, offset: int) -> bool:
    """
    Todo primo maior que 2 é ímpar.
    Portanto base+offset precisa ser ímpar.
    """
    return ((base + offset) % 2) == 1


def unique_preserve_order(values: List[int]) -> List[int]:
    seen = set()
    out = []
    for v in values:
        if v not in seen:
            seen.add(v)
            out.append(v)
    return out


def generate_density_offsets(base: int, info: DensityInfo) -> List[int]:
    candidates: List[int] = []

    # camada 1: curtos
    for k in [1, 2]:
        if valid_offset_for_prime(base, +k):
            candidates.append(+k)
        if valid_offset_for_prime(base, -k):
            candidates.append(-k)

    anchor_values = [info.floor_ln, info.round_ln, info.ceil_ln]
    anchor_values = unique_preserve_order(anchor_values)

    # camada 2: saltos exatos da densidade arredondada
    for a in anchor_values:
        if valid_offset_for_prime(base, +a):
            candidates.append(+a)
        if valid_offset_for_prime(base, -a):
            candidates.append(-a)

    # camada 3: vizinhança ao redor dos saltos arredondados
    for a in anchor_values:
        for delta in [1, 2]:
            kp = a + delta
            km = a - delta
            if kp > 0:
                if valid_offset_for_prime(base, +kp):
                    candidates.append(+kp)
                if valid_offset_for_prime(base, -kp):
                    candidates.append(-kp)
            if km > 0:
                if valid_offset_for_prime(base, +km):
                    candidates.append(+km)
                if valid_offset_for_prime(base, -km):
                    candidates.append(-km)

    # camada 4: preenchimento completo até a casca ceil(ln n)
    for k in range(1, info.shell_radius + 1):
        if valid_offset_for_prime(base, +k):
            candidates.append(+k)
        if valid_offset_for_prime(base, -k):
            candidates.append(-k)

    candidates = unique_preserve_order(candidates)
    candidates.sort(key=lambda x: (abs(x), 0 if x > 0 else 1))
    return candidates


def search_nearby_prime(base: int) -> Tuple[Optional[int], Optional[int], DensityInfo]:
    info = density_info(base)
    offsets = generate_density_offsets(base, info)

    for offset in offsets:
        n = base + offset
        if not passes_fast_mod_filters(n):
            continue
        if is_prime(n):
            return n, offset, info

    return None, None, info


# ============================================================
# RELATÓRIOS
# ============================================================

@dataclass
class CandidateResult:
    label: str
    base_value: int
    exact_prime: bool
    exact_factor: Optional[int]
    nearby_prime: Optional[int]
    nearby_offset: Optional[int]
    ln_raw: float
    ln_floor: int
    ln_round: int
    ln_ceil: int

    def text(self) -> str:
        density_text = (
            f"ln={self.ln_raw:.6f} | floor={self.ln_floor} | "
            f"round={self.ln_round} | ceil={self.ln_ceil}"
        )

        if self.exact_prime:
            return (
                f"{self.label:<18} = {self.base_value} -> PRIMO\n"
                f"{'':<18}   densidade: {density_text}"
            )

        fator_str = "composto"
        if self.exact_factor is not None:
            outro = self.base_value // self.exact_factor
            fator_str = f"composto = {self.exact_factor} x {outro}"

        if self.nearby_prime is not None:
            sign = "+" if self.nearby_offset >= 0 else ""
            return (
                f"{self.label:<18} = {self.base_value} -> {fator_str}\n"
                f"{'':<18}   primo_proximo = {self.nearby_prime} ({sign}{self.nearby_offset})\n"
                f"{'':<18}   densidade: {density_text}"
            )

        return (
            f"{self.label:<18} = {self.base_value} -> {fator_str}\n"
            f"{'':<18}   sem_primo_proximo\n"
            f"{'':<18}   densidade: {density_text}"
        )


@dataclass
class PairAnalysis:
    p: int
    q: int
    diff: int
    digit_count: int
    any_exact_prime: bool
    any_nearby_prime: bool
    results: List[CandidateResult]

    def text_block(self) -> str:
        out = []
        out.append("\n" + "=" * 120)
        out.append(
            f"PAR ANALISADO: p={self.p}, q={self.q}, diferença={self.diff}, dígitos={self.digit_count}"
        )
        out.append("=" * 120)
        for r in self.results:
            out.append(r.text())
        out.append("")
        out.append(f"Tem primo exato em algum modelo? {'SIM' if self.any_exact_prime else 'NÃO'}")
        out.append(f"Tem primo próximo em algum modelo? {'SIM' if self.any_nearby_prime else 'NÃO'}")
        out.append("")
        return "\n".join(out)


# ============================================================
# ANÁLISE DE PAR
# ============================================================

def analyze_pair(p: int, q: int, factor_limit: int) -> PairAnalysis:
    models = build_models(p, q)
    results: List[CandidateResult] = []
    any_exact = False
    any_nearby = False

    for label, value in models.items():
        exact = is_prime(value)
        factor = None if exact else smallest_factor(value, max_check=factor_limit)

        nearby_prime = None
        nearby_offset = None
        info = density_info(value)

        if exact:
            any_exact = True
        else:
            near_prime, near_offset, info = search_nearby_prime(value)
            if near_prime is not None:
                nearby_prime = near_prime
                nearby_offset = near_offset
                any_nearby = True

        results.append(
            CandidateResult(
                label=label,
                base_value=value,
                exact_prime=exact,
                exact_factor=factor,
                nearby_prime=nearby_prime,
                nearby_offset=nearby_offset,
                ln_raw=info.raw_ln,
                ln_floor=info.floor_ln,
                ln_round=info.round_ln,
                ln_ceil=info.ceil_ln,
            )
        )

    return PairAnalysis(
        p=p,
        q=q,
        diff=q - p,
        digit_count=len(str(p)),
        any_exact_prime=any_exact,
        any_nearby_prime=any_nearby,
        results=results,
    )


# ============================================================
# CHECKPOINT / LOG
# ============================================================

def load_checkpoint(path: str) -> Optional[dict]:
    if not os.path.exists(path):
        return None
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_checkpoint(path: str, data: dict) -> None:
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp, path)


def append_log(path: str, text: str) -> None:
    with open(path, "a", encoding="utf-8") as f:
        f.write(text)
        if not text.endswith("\n"):
            f.write("\n")


# ============================================================
# LOOP PRINCIPAL
# ============================================================

def search_loop(
    checkpoint_file: str,
    report_file: str,
    summary_file: str,
    start_p: int,
    start_q: Optional[int],
    screen_every: int,
    save_every: int,
    factor_limit: int,
    blocked_mod120_residues: set[int],
    use_mod3_filter: bool,
    max_pairs: int,
) -> None:
    started_at = time.time()

    tested_pairs = 0
    skipped_mod120 = 0
    skipped_mod3 = 0
    pairs_with_exact_prime = 0
    pairs_without_exact_prime = 0
    pairs_with_nearby_prime = 0
    pairs_without_nearby_prime = 0

    p = next_odd_ending_in_1_after(start_p - 1)
    q_resume_min = start_q

    while True:
        if tested_pairs >= max_pairs:
            break

        p_digits = len(str(p))

        if q_resume_min is not None and len(str(q_resume_min)) == p_digits and q_resume_min > p:
            q = next_odd_ending_in_1_after(q_resume_min - 1)
        else:
            q = next_odd_ending_in_1_after(p)

        while len(str(q)) == p_digits:
            if tested_pairs >= max_pairs:
                break

            tested_pairs += 1
            diff = q - p

            if tested_pairs % screen_every == 0:
                elapsed = time.time() - started_at
                print(
                    f"Buscando... p={p} | q={q} | diff={diff} | dígitos={p_digits} | "
                    f"pares={tested_pairs}/{max_pairs} | skip120={skipped_mod120} | "
                    f"skip3={skipped_mod3} | exatos={pairs_with_exact_prime} | "
                    f"sem_exatos={pairs_without_exact_prime} | prox={pairs_with_nearby_prime} | "
                    f"sem_prox={pairs_without_nearby_prime} | tempo={elapsed:.1f}s",
                    flush=True,
                )

            if blocked_pair_mod120(p, q, blocked_mod120_residues):
                skipped_mod120 += 1
                q = next_odd_ending_in_1_after(q)
                continue

            if blocked_pair_mod3(p, q, use_mod3_filter):
                skipped_mod3 += 1
                q = next_odd_ending_in_1_after(q)
                continue

            analysis = analyze_pair(p=p, q=q, factor_limit=factor_limit)

            if analysis.any_exact_prime:
                pairs_with_exact_prime += 1
            else:
                pairs_without_exact_prime += 1

            if analysis.any_nearby_prime:
                pairs_with_nearby_prime += 1
            else:
                pairs_without_nearby_prime += 1

            append_log(report_file, analysis.text_block())

            summary_line = (
                f"p={analysis.p}, q={analysis.q}, diff={analysis.diff}, digitos={analysis.digit_count}, "
                f"exato={'SIM' if analysis.any_exact_prime else 'NAO'}, "
                f"proximo={'SIM' if analysis.any_nearby_prime else 'NAO'}"
            )
            append_log(summary_file, summary_line)

            if tested_pairs % save_every == 0:
                save_checkpoint(
                    checkpoint_file,
                    {
                        "last_p": p,
                        "last_q": q,
                        "tested_pairs": tested_pairs,
                        "skipped_mod120": skipped_mod120,
                        "skipped_mod3": skipped_mod3,
                        "pairs_with_exact_prime": pairs_with_exact_prime,
                        "pairs_without_exact_prime": pairs_without_exact_prime,
                        "pairs_with_nearby_prime": pairs_with_nearby_prime,
                        "pairs_without_nearby_prime": pairs_without_nearby_prime,
                        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
                    },
                )

            q = next_odd_ending_in_1_after(q)

        q_resume_min = None
        p = next_odd_ending_in_1_after(p)

    elapsed = time.time() - started_at
    final_msg = (
        "\nBusca concluída pelo limite definido.\n"
        f"Ponto final: p={p}, q={q if 'q' in locals() else 'N/A'}\n"
        f"Pares testados: {tested_pairs}\n"
        f"Pares pulados mod120: {skipped_mod120}\n"
        f"Pares pulados mod3: {skipped_mod3}\n"
        f"Pares com primo exato: {pairs_with_exact_prime}\n"
        f"Pares sem primo exato: {pairs_without_exact_prime}\n"
        f"Pares com primo próximo: {pairs_with_nearby_prime}\n"
        f"Pares sem primo próximo: {pairs_without_nearby_prime}\n"
        f"Tempo: {elapsed:.1f}s\n"
    )

    print(final_msg, flush=True)
    append_log(report_file, final_msg)

    save_checkpoint(
        checkpoint_file,
        {
            "last_p": p,
            "last_q": q if 'q' in locals() else None,
            "tested_pairs": tested_pairs,
            "skipped_mod120": skipped_mod120,
            "skipped_mod3": skipped_mod3,
            "pairs_with_exact_prime": pairs_with_exact_prime,
            "pairs_without_exact_prime": pairs_without_exact_prime,
            "pairs_with_nearby_prime": pairs_with_nearby_prime,
            "pairs_without_nearby_prime": pairs_without_nearby_prime,
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "finished_by_limit": True,
        },
    )


# ============================================================
# MAIN
# ============================================================

def parse_mod120_residues(text: str) -> set[int]:
    text = text.strip()
    if not text:
        return {0}
    return {int(x.strip()) % 120 for x in text.split(",") if x.strip()}


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Busca com ímpares terminados em 1, densidade automática, arredondamentos floor/round/ceil e paridade correta."
    )

    parser.add_argument("--checkpoint", type=str, default="checkpoint_busca.json")
    parser.add_argument("--report-file", type=str, default="relatorio_completo.txt")
    parser.add_argument("--summary-file", type=str, default="resumo_pares.txt")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--start-p", type=int, default=11)
    parser.add_argument("--start-q", type=int, default=None)

    parser.add_argument("--screen-every", type=int, default=50)
    parser.add_argument("--save-every", type=int, default=200)
    parser.add_argument("--factor-limit", type=int, default=200000)

    parser.add_argument("--mod120-residues", type=str, default="0")
    parser.add_argument("--use-mod3-filter", action="store_true")

    parser.add_argument("--max-pairs", type=int, default=5000)

    args = parser.parse_args()

    start_p = args.start_p
    start_q = args.start_q

    if args.resume:
        cp = load_checkpoint(args.checkpoint)
        if cp:
            start_p = cp.get("last_p", start_p)
            start_q = cp.get("last_q", start_q)
            print(
                f"Retomando: p={start_p}, q={start_q}, tested_pairs={cp.get('tested_pairs', 0)}",
                flush=True,
            )
        else:
            print("Checkpoint não encontrado. Iniciando do começo.", flush=True)

    blocked_mod120_residues = parse_mod120_residues(args.mod120_residues)

    header = (
        "\n" + "=" * 120 + "\n"
        f"Início/retomada: {time.strftime('%Y-%m-%d %H:%M:%S')}\n"
        f"start_p={start_p}, start_q={start_q}\n"
        f"screen_every={args.screen_every}, save_every={args.save_every}\n"
        f"factor_limit={args.factor_limit}\n"
        f"mod120={sorted(blocked_mod120_residues)}, mod3={args.use_mod3_filter}\n"
        f"max_pairs={args.max_pairs}\n"
        f"Base: qualquer ímpar terminado em 1\n"
        f"Busca local pela densidade: ln(N), com floor/round/ceil e ajuste de paridade\n"
        + "=" * 120 + "\n"
    )

    append_log(args.report_file, header)
    append_log(args.summary_file, header)

    search_loop(
        checkpoint_file=args.checkpoint,
        report_file=args.report_file,
        summary_file=args.summary_file,
        start_p=start_p,
        start_q=start_q,
        screen_every=args.screen_every,
        save_every=args.save_every,
        factor_limit=args.factor_limit,
        blocked_mod120_residues=blocked_mod120_residues,
        use_mod3_filter=args.use_mod3_filter,
        max_pairs=args.max_pairs,
    )


if __name__ == "__main__":
    main()