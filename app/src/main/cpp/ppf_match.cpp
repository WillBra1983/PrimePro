#include "ppf_c_api.h"

#include <algorithm>
#include <cstdlib>
#include <limits>

namespace {
uint64_t bit(int index) { return UINT64_C(1) << index; }

int level_in_range(int level) {
    return std::max(1, std::min(static_cast<int>(PPF_MATCH_MAX_LEVELS), level));
}

int cell_id(int axis, int line, int position) {
    return axis == 0 ? line * PPF_MATCH_SIDE + position : position * PPF_MATCH_SIDE + line;
}
}

int ppf_match_groups(const int32_t* colors, uint64_t occupied, uint64_t* masks) {
    if (colors == nullptr || masks == nullptr) return 0;
    int count = 0;
    for (int axis = 0; axis < 2; ++axis) {
        for (int line = 0; line < PPF_MATCH_SIDE; ++line) {
            for (int start = 0; start < PPF_MATCH_SIDE;) {
                uint64_t group = 0;
                int base = -1;
                int end = start;
                for (; end < PPF_MATCH_SIDE; ++end) {
                    const int cell = cell_id(axis, line, end);
                    if ((occupied & bit(cell)) == 0 || colors[cell] < 0) break;
                    if (colors[cell] != PPF_MATCH_BLACK) {
                        if (base >= 0 && base != colors[cell]) break;
                        base = colors[cell];
                    }
                    group |= bit(cell);
                }
                if (end - start >= 3) {
                    masks[count++] = group;
                    start = end;
                } else {
                    // A wild tile at the end of a short run can start the next colour.
                    ++start;
                }
            }
        }
    }
    return count;
}

PPFMatchResult ppf_match_resolve(const int32_t* values, uint64_t black,
                                uint64_t group, uint64_t matched,
                                uint64_t already_credited, int64_t sum) {
    const uint64_t board_mask = bit(PPF_MATCH_CELLS) - 1;
    group &= board_mask;
    PPFMatchResult result{group, 0, 0, 0, 0};
    if (values == nullptr || group == 0 || sum < 2 || !ppf_is_prime_u64(sum)) return result;

    result.points = result.black_value = sum;
    const uint64_t match_black = black & group;
    const int black_count = __builtin_popcountll(match_black);
    result.blast_kind = std::min(3, black_count);
    if (black_count == 0) return result;

    for (int i = 0; i < PPF_MATCH_CELLS; ++i) {
        if (values[i] <= 0) continue;
        if (black_count >= 3 || (black_count == 2 && ppf_is_prime_u64(values[i]))) {
            result.blast_mask |= bit(i);
        } else if (black_count == 1) {
            for (int b = 0; b < PPF_MATCH_CELLS; ++b) {
                if ((match_black & bit(b)) != 0
                    && std::abs(b / PPF_MATCH_SIDE - i / PPF_MATCH_SIDE) <= 1
                    && std::abs(b % PPF_MATCH_SIDE - i % PPF_MATCH_SIDE) <= 1) {
                    result.blast_mask |= bit(i);
                    break;
                }
            }
        }
    }

    const uint64_t extra_mask = result.blast_mask & ~(matched | group | already_credited);
    int64_t extra = 0;
    for (int i = 0; i < PPF_MATCH_CELLS; ++i) {
        if ((extra_mask & bit(i)) != 0) extra += values[i];
    }
    if (extra == 0 || sum > std::numeric_limits<int64_t>::max() - extra) return result;
    const int64_t combined = sum + extra;
    const bool combined_prime = ppf_is_prime_u64(combined);
    if (black_count == 2 || combined_prime) {
        result.points = combined;
        result.credited_mask = extra_mask;
    }
    // Even when two black pieces award every extra, the new black tile stays prime.
    if (combined_prime) result.black_value = combined;
    return result;
}

int ppf_match_level_target(int level) {
    return 760 + 140 * level_in_range(level);
}

int ppf_match_level_moves(int level) {
    return std::max(16, 22 - std::min(6, level_in_range(level) / 4));
}

int ppf_match_percent(int score, int target) {
    if (score <= 0 || target <= 0) return 0;
    if (score >= target) return 100;
    return static_cast<int>(static_cast<int64_t>(score) * 100 / target);
}

int ppf_match_stars(int score, int target) {
    const int percent = ppf_match_percent(score, target);
    return percent >= 100 ? 3 : percent >= 66 ? 2 : percent >= 33 ? 1 : 0;
}

uint32_t ppf_match_level_powers(int level) {
    switch ((level_in_range(level) - 1) % 4) {
        case 0: return bit(PPF_MATCH_HAMMER) | bit(PPF_MATCH_PLUS_1) |
                       bit(PPF_MATCH_SHUFFLE) | bit(PPF_MATCH_HINT);
        case 1: return bit(PPF_MATCH_PLUS_2) | bit(PPF_MATCH_SWAP) |
                       bit(PPF_MATCH_HINT) | bit(PPF_MATCH_ROW_RAY);
        case 2: return bit(PPF_MATCH_PLUS_1) | bit(PPF_MATCH_PLUS_3) |
                       bit(PPF_MATCH_COPY_COLOR) | bit(PPF_MATCH_MULTIPLIER) | bit(PPF_MATCH_COLUMN_RAY);
        default: return bit(PPF_MATCH_HAMMER) | bit(PPF_MATCH_POLISH_UP) |
                        bit(PPF_MATCH_POLISH_DOWN) | bit(PPF_MATCH_SHUFFLE) | bit(PPF_MATCH_HINT);
    }
}

uint32_t ppf_match_victory_rewards(int wins) {
    if (wins <= 0) return 0;
    switch (wins % 6) {
        case 1: return bit(PPF_MATCH_HAMMER) | bit(PPF_MATCH_PLUS_1);
        case 2: return bit(PPF_MATCH_SHUFFLE) | bit(PPF_MATCH_PLUS_2) | bit(PPF_MATCH_HINT);
        case 3: return bit(PPF_MATCH_PLUS_3) | bit(PPF_MATCH_SWAP);
        case 4: return bit(PPF_MATCH_COPY_COLOR) | bit(PPF_MATCH_POLISH_UP) | bit(PPF_MATCH_POLISH_DOWN);
        case 5: return bit(PPF_MATCH_ROW_RAY) | bit(PPF_MATCH_COLUMN_RAY);
        default: return bit(PPF_MATCH_HINT) | bit(PPF_MATCH_MULTIPLIER) | bit(PPF_MATCH_HAMMER);
    }
}

int ppf_match_draw_value(int ticket) {
    if (ticket < 0 || ticket >= PPF_MATCH_VALUE_TICKETS) return 1;
    // Each tier has the same number of tickets per value, including the boundaries.
    const int tickets_per_roll = PPF_MATCH_VALUE_TICKETS / 100;
    const int roll = ticket / tickets_per_roll;
    const int value_roll = ticket % tickets_per_roll;
    if (roll < 48) return 1 + value_roll % 29;
    if (roll < 78) return 30 + value_roll % 32;
    if (roll < 94) return 62 + value_roll % 36;
    return 98 + value_roll % 52;
}

bool ppf_match_local_pattern(const int32_t* values, const int32_t* colors, int index, int value, int color) {
    if (values == nullptr || colors == nullptr || index < 0 || index >= PPF_MATCH_CELLS) return false;
    const int row = index / PPF_MATCH_SIDE;
    const int col = index % PPF_MATCH_SIDE;
    int repeats = 0;
    for (int dr = -1; dr <= 1; ++dr) {
        for (int dc = -1; dc <= 1; ++dc) {
            if (dr == 0 && dc == 0) continue;
            const int r = row + dr;
            const int c = col + dc;
            if (r < 0 || r >= PPF_MATCH_SIDE || c < 0 || c >= PPF_MATCH_SIDE) continue;
            const int near = r * PPF_MATCH_SIDE + c;
            if (values[near] == value && (colors[near] == color || ++repeats >= 2)) return true;
        }
    }
    auto arithmetic = [&](int step) {
        const auto a = values[index - step * 2];
        const auto b = values[index - step];
        return a > 0 && b > 0 && static_cast<int64_t>(a) + value == 2LL * b;
    };
    return (col >= 2 && arithmetic(1)) || (row >= 2 && arithmetic(PPF_MATCH_SIDE));
}

int ppf_match_polished_value(int value, int color, int delta) {
    if (color < 0 || color == PPF_MATCH_BLACK || (delta != 1 && delta != -1)) return 0;
    if (value < 1 || value > PPF_MATCH_TILE_VALUE_MAX) return 0;
    const int polished = value + delta;
    return polished >= 1 && polished <= PPF_MATCH_TILE_VALUE_MAX ? polished : 0;
}
