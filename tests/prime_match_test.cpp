#include "../app/src/main/cpp/ppf_c_api.h"

#include <array>
#include <cassert>

namespace {
uint64_t bit(int position) { return UINT64_C(1) << position; }
}

int main() {
    std::array<int32_t, PPF_MATCH_CELLS> colors{};
    std::array<uint64_t, PPF_MATCH_MAX_GROUPS> masks{};
    colors[0] = 0;
    colors[1] = PPF_MATCH_BLACK;
    colors[2] = colors[3] = 1;
    assert(ppf_match_groups(colors.data(), 15, masks.data()) == 1);
    assert(masks[0] == (bit(1) | bit(2) | bit(3)));
    assert(ppf_match_groups(colors.data(), 0, masks.data()) == 0);
    for (int i = 0; i < PPF_MATCH_CELLS; ++i) {
        colors[i] = (i / PPF_MATCH_SIDE / 3 + i % PPF_MATCH_SIDE / 3) % 2;
    }
    assert(ppf_match_groups(colors.data(), bit(PPF_MATCH_CELLS) - 1, masks.data()) == PPF_MATCH_MAX_GROUPS);

    std::array<int32_t, PPF_MATCH_CELLS> values{};
    const auto group = bit(0) | bit(1) | bit(2);
    const auto next_group = bit(6) | bit(7) | bit(8);
    values[0] = 5;
    values[1] = 7;
    values[2] = 11;
    values[7] = 5;
    auto event = ppf_match_resolve(values.data(), bit(1), group, group, 0, 23);
    assert(event.points == 23 && event.black_value == 23);
    assert(event.blast_kind == 1 && event.blast_mask == (group | bit(7)) && event.credited_mask == 0);

    values[7] = 6;
    event = ppf_match_resolve(values.data(), bit(1), group, group, 0, 23);
    assert(event.points == 29 && event.black_value == 29 && event.credited_mask == bit(7));
    event = ppf_match_resolve(values.data(), bit(1), group, group | next_group, 0, 23);
    assert(event.points == 23 && event.credited_mask == 0);

    values[2] = 19;
    values[35] = 11;
    const auto two_black = bit(0) | bit(1);
    event = ppf_match_resolve(values.data(), two_black, group, group, 0, 31);
    assert(event.blast_kind == 2 && event.points == 42 && event.black_value == 31);
    assert(event.credited_mask == bit(35) && (event.blast_mask & bit(7)) == 0);
    event = ppf_match_resolve(values.data(), two_black, group, group, bit(35), 31);
    assert(event.points == 31 && event.credited_mask == 0);

    values[35] = 0;
    values[2] = 11;
    event = ppf_match_resolve(values.data(), group, group, group, 0, 23);
    assert(event.blast_kind == 3 && event.points == 29 && event.black_value == 29);
    event = ppf_match_resolve(values.data(), group, group, group, 0, 58);
    assert(event.blast_mask == group && event.blast_kind == 0 && event.points == 0 && event.black_value == 0);

    const auto separate_a = ppf_match_resolve(values.data(), 0, group, group | next_group, 0, 31);
    const auto separate_b = ppf_match_resolve(values.data(), 0, next_group, group | next_group, 0, 31);
    assert(separate_a.points + separate_b.points == 62);

    assert(ppf_match_level_target(1) == 900 && ppf_match_level_target(24) == 4120);
    assert(ppf_match_level_moves(1) == 22 && ppf_match_level_moves(24) == 16);
    assert(ppf_match_stars(296, 900) == 0 && ppf_match_stars(297, 900) == 1);
    assert(ppf_match_stars(594, 900) == 2 && ppf_match_stars(899, 900) == 2);
    assert(ppf_match_stars(900, 900) == 3);
    assert(ppf_match_level_powers(1) == ppf_match_level_powers(5));
    assert((ppf_match_level_powers(2) & bit(PPF_MATCH_ROW_RAY)) != 0);
    assert((ppf_match_level_powers(1) & bit(PPF_MATCH_ROW_RAY)) == 0);
    assert(ppf_match_victory_rewards(1) == (bit(PPF_MATCH_HAMMER) | bit(PPF_MATCH_PLUS_1)));

    const int stride = PPF_MATCH_VALUE_TICKETS / 100;
    assert(ppf_match_draw_value(0) == 1 && ppf_match_draw_value(stride * 48 - 1) == 29);
    assert(ppf_match_draw_value(stride * 48) == 30 && ppf_match_draw_value(stride * 78) == 62);
    assert(ppf_match_draw_value(stride * 94) == 98 && ppf_match_draw_value(PPF_MATCH_VALUE_TICKETS - 1) == 149);
    values.fill(0);
    colors.fill(0);
    values[0] = 5;
    values[1] = 7;
    assert(ppf_match_local_pattern(values.data(), colors.data(), 2, 9, 2));
    assert(ppf_match_local_pattern(values.data(), colors.data(), 1, 5, 0));
    assert(!ppf_match_local_pattern(values.data(), colors.data(), 2, 10, 2));
    assert(ppf_match_polished_value(20, 1, 1) == 21);
    assert(ppf_match_polished_value(149, 1, 1) == 0);
    assert(ppf_match_polished_value(13, PPF_MATCH_BLACK, 1) == 0);
}
