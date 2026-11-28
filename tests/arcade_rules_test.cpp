#include "../app/src/main/cpp/ppf_c_api.h"
#include <cassert>
#include <initializer_list>
#include <limits>

int main() {
    assert(ppf_arcade_points(12, false, false) + ppf_arcade_points(7, true, true) == 19);
    assert(ppf_arcade_points(13, true, false) == 0);
    assert(ppf_arcade_points(24, false, true) == 0);
    assert(ppf_arcade_points(1, false, false) == 0);
    assert(ppf_arcade_stage(449, false) == 0);
    assert(ppf_arcade_stage(450, false) == 1);
    assert(ppf_arcade_stage(900, false) == 2);
    assert(ppf_arcade_stage(599, true) == 0);
    assert(ppf_arcade_stage(600, true) == 1);
    assert(ppf_arcade_stage(1200, true) == 2);
    assert(ppf_arcade_phase_target(0, false) == 450);
    assert(ppf_arcade_phase_target(1, false) == 485);
    assert(ppf_arcade_phase_target(0, true) == 600);
    assert(ppf_arcade_phase_target(1, true) == 645);
    assert(ppf_arcade_phase_target(-1, true) == 0);
    assert(ppf_arcade_phase_target(std::numeric_limits<int>::max(), false) == 870);
    assert(ppf_arcade_phase_duration_ms(false) == 40000);
    assert(ppf_arcade_phase_duration_ms(true) == 50000);
    const int total = ppf_arcade_add_score(449, 17);
    assert(total == 466 && total >= ppf_arcade_phase_target(0, false));
    assert(ppf_arcade_phase_outcome(0, false, 15000, total, false) == PPF_ARCADE_PHASE_RUNNING);
    assert(ppf_arcade_phase_outcome(0, false, 39999, 1200, false) == PPF_ARCADE_PHASE_RUNNING);
    assert(ppf_arcade_phase_outcome(0, false, 40000, 1200, false) == PPF_ARCADE_PHASE_PASSED);
    assert(ppf_arcade_phase_outcome(0, false, 40000, 449, false) == PPF_ARCADE_PHASE_BELOW_TARGET);
    assert(ppf_arcade_phase_outcome(0, false, 40000, 450, false) == PPF_ARCADE_PHASE_PASSED);
    assert(ppf_arcade_phase_outcome(0, true, 49999, 1800, false) == PPF_ARCADE_PHASE_RUNNING);
    assert(ppf_arcade_phase_outcome(0, true, 50000, 600, false) == PPF_ARCADE_PHASE_PASSED);
    assert(ppf_arcade_phase_outcome(0, true, 30000, 700, true) == PPF_ARCADE_PHASE_CRASHED);
    assert(ppf_arcade_vehicle(0) == 0);
    assert(ppf_arcade_vehicle(1) == 1);
    assert(ppf_arcade_vehicle(ppf_arcade_stage(6000, true)) == 2);

    const auto rules = ppf_arcade_rules();
    assert(rules.spawn_start_ms == 1200 && rules.max_frame_ms == 33);
    assert(rules.lives == 3 && rules.shield_ms == 2500 && rules.hit_grace_ms == 800);
    assert(rules.ship_y - rules.control_height > 40);
    assert(ppf_arcade_add_score(500, -20) == 500);
    assert(ppf_arcade_add_score(std::numeric_limits<int>::max() - 10, 149)
        == std::numeric_limits<int>::max());
    assert(ppf_arcade_runner_speed(0) == 220 && ppf_arcade_runner_speed(10000) == 355);
    const double cruising = ppf_arcade_runner_speed(0);
    const double gaining = ppf_arcade_road_drive_speed(0, 1, cruising, 0.03);
    const double slowing = ppf_arcade_road_drive_speed(0, 0, cruising, 0.03);
    assert(gaining > cruising && gaining < cruising * 1.23);
    assert(slowing < cruising && slowing > cruising * 0.74);
    assert(ppf_arcade_road_drive_speed(0, 1, cruising, 0) == cruising);
    assert(ppf_arcade_road_drive_speed(0, 1, 0, 0) == cruising * 0.58);
    assert(ppf_arcade_road_hit(20, 120, 180, 120, 100, 120, 500));
    assert(!ppf_arcade_road_hit(20, 180, 180, 180, 100, 120, 500));
    assert(ppf_arcade_road_hit(100, 180, 100, 60, 100, 120, 500));
    assert(!ppf_arcade_road_hit(100, 180, 100, 150, 100, 120, 500));
    assert(ppf_arcade_jump_height(0) == 0);
    assert(ppf_arcade_jump_height(0.46) > 195);
    assert(ppf_arcade_jump_height(0.95) == 0);

    const double ground_y = rules.floor_y + rules.runner_center;
    assert(ppf_arcade_collides(PPF_ARCADE_RUNNER, rules.runner_x, ground_y,
        PPF_ARCADE_RUNNER_NUMBER, rules.runner_x, ground_y));
    assert(!ppf_arcade_collides(PPF_ARCADE_RUNNER, rules.runner_x, ground_y + 100,
        PPF_ARCADE_RUNNER_NUMBER, rules.runner_x, ground_y));
    assert(ppf_arcade_runner_passed(33) && !ppf_arcade_runner_passed(33.01));
    assert(ppf_arcade_collides(PPF_ARCADE_SHIP, 100, rules.ship_y,
        PPF_ARCADE_SPACE_PRIME, 100, rules.ship_y));
    assert(!ppf_arcade_collides(PPF_ARCADE_SHIP, 100, rules.ship_y,
        PPF_ARCADE_SPACE_COMPOSITE, 160, rules.ship_y));
    assert(ppf_arcade_collides(PPF_ARCADE_SHOT, 100, 140,
        PPF_ARCADE_SPACE_COMPOSITE, 100, 171));
    assert(ppf_arcade_flight_contact(true, true, true) == PPF_ARCADE_CONTACT_RESCUE);
    assert(ppf_arcade_flight_contact(false, true, false) == PPF_ARCADE_CONTACT_SHIELD);
    assert(ppf_arcade_flight_contact(false, false, true) == PPF_ARCADE_CONTACT_NONE);
    assert(ppf_arcade_flight_contact(false, false, false) == PPF_ARCADE_CONTACT_DAMAGE);

    assert(PPF_ARCADE_CAMPAIGN_STAGES == 52);
    assert(ppf_arcade_boss(-1).health == 0);
    assert(ppf_arcade_boss(PPF_ARCADE_CAMPAIGN_STAGES).health == 0);
    assert(ppf_arcade_boss(0).exponent == 2 && ppf_arcade_boss(0).prefix == 3);
    assert(ppf_arcade_boss(4).exponent == 13 && ppf_arcade_boss(4).prefix == 819);
    assert(ppf_arcade_boss(50).exponent == 82589933);
    assert(ppf_arcade_boss(51).exponent == 136279841);
    for (int stage = 0; stage < PPF_ARCADE_CAMPAIGN_STAGES; ++stage) {
        const auto boss = ppf_arcade_boss(stage);
        assert(boss.health >= 112 && boss.pattern >= 0 && boss.pattern < 3);
        assert(stage < 4 || (boss.prefix >= 100 && boss.prefix <= 999));
        if (stage > 0) assert(boss.health > ppf_arcade_boss(stage - 1).health);
        for (uint32_t seed = 0; seed < 300; ++seed) {
            assert(ppf_arcade_enemy_role(true, true, stage, seed) == PPF_ARCADE_REGULAR);
            assert(ppf_arcade_enemy_role(true, false, stage, seed) == PPF_ARCADE_REGULAR);
            assert(ppf_arcade_enemy_role(false, false, stage, seed) != PPF_ARCADE_REPAIR);
        }
    }
    assert(ppf_arcade_enemy_role(false, true, 0, 0) == PPF_ARCADE_ARMED);
    assert(ppf_arcade_enemy_role(false, true, 0, 1) == PPF_ARCADE_ARMED);
    assert(ppf_arcade_enemy_role(false, false, 0, 99) == PPF_ARCADE_REGULAR);
    assert(ppf_arcade_add_charge(0, 2) == 1 && ppf_arcade_add_charge(0, 67) == 16);
    assert(ppf_arcade_add_charge(98, 19) == 100 && ppf_arcade_add_charge(100, 19) == 100);
    assert(ppf_arcade_boss_damage(0) == 8 && ppf_arcade_boss_damage(100) == 16);
    assert(ppf_arcade_flight_outcome(3, -1) == PPF_ARCADE_PHASE_RUNNING);
    assert(ppf_arcade_flight_outcome(1, 1) == PPF_ARCADE_PHASE_RUNNING);
    assert(ppf_arcade_flight_outcome(1, 0) == PPF_ARCADE_PHASE_PASSED);
    assert(ppf_arcade_flight_outcome(0, 0) == PPF_ARCADE_PHASE_CRASHED);
    assert(ppf_arcade_flight_duration_ms(0) == 50000);
    assert(ppf_arcade_flight_duration_ms(51) == 131600);
    assert(ppf_arcade_boss_scale_percent(0) == 100);
    assert(ppf_arcade_boss_scale_percent(50) == 165);
    assert(ppf_arcade_boss_escorts(0) == 0 && ppf_arcade_boss_escorts(1) == 1);
    assert(ppf_arcade_boss_escorts(50) == 3);
    assert(ppf_arcade_flight_loot(0, 0) == PPF_ARCADE_LOOT_NONE);
    assert(ppf_arcade_flight_loot(0, 2) == PPF_ARCADE_LOOT_LIFE);
    assert(ppf_arcade_flight_loot(0, 4) == PPF_ARCADE_LOOT_PART);
    assert(ppf_arcade_flight_loot(0, 5) == PPF_ARCADE_LOOT_WEAPON);
    assert(ppf_arcade_flight_loot(12, 9) == PPF_ARCADE_LOOT_LIFE);
    assert(ppf_arcade_upgrade_damage(100, 12) == 40);
    assert(ppf_arcade_max_lives(0) == 5 && ppf_arcade_max_lives(12) == 9);
    assert(ppf_arcade_boss(0).health == 175);
    const int window = ppf_arcade_boss_guard_window_ms(0);
    const int cycle = ppf_arcade_boss_guard_cycle_ms(0);
    assert(ppf_arcade_boss_guarded(0, 0, false));
    assert(!ppf_arcade_boss_guarded(0, window, false));
    assert(ppf_arcade_boss_guarded(0, window, true));
    assert(ppf_arcade_boss_guarded(0, cycle, false));
    assert(ppf_arcade_flight_zone(0, false) != ppf_arcade_flight_zone(0, true));
    assert(ppf_arcade_flight_zone(50, false) == 4 && ppf_arcade_flight_zone(50, true) == 4);
    assert(ppf_arcade_flight_zone(51, false) != ppf_arcade_flight_zone(51, true));
    assert(ppf_arcade_shield_intercepts(45, 0));
    assert(ppf_arcade_shield_intercepts(0, PPF_ARCADE_SHIELD_INTERCEPT));
    assert(!ppf_arcade_shield_intercepts(45, 45));
    assert(!ppf_arcade_shield_intercepts(0, 55));
    assert(ppf_arcade_boss_drops(76, 75, 100) == 1);
    assert(ppf_arcade_boss_drops(75, 51, 100) == 0);
    assert(ppf_arcade_boss_drops(52, 48, 100) == 2);
    assert(ppf_arcade_boss_drops(28, 20, 100) == 4);
    assert(ppf_arcade_boss_drops(78, 24, 100) == 7);
    assert(ppf_arcade_boss_drops(26, 0, 100) == 0);

    for (const bool flight : {false, true}) {
        int previous = 0;
        bool saw_prime = false, saw_composite = false;
        unsigned lanes = 0;
        for (uint32_t seed = 0; seed < 256; ++seed) {
            const auto spawn = ppf_arcade_spawn(flight, 0, seed, previous);
            assert(spawn.number >= 2 && spawn.number <= 150 && spawn.number != previous);
            assert(spawn.prime == ppf_is_prime_u64(spawn.number));
            assert(spawn.lane >= 0 && spawn.lane <= PPF_ARCADE_LANE_MAX);
            if (flight) {
                assert(spawn.speed >= 90 && spawn.speed <= 123);
                assert(spawn.delay_ms >= 786 && spawn.delay_ms <= 1045);
                const auto fast = ppf_arcade_spawn(true, std::numeric_limits<int>::max(), seed, spawn.number);
                assert(fast.number != spawn.number && fast.speed <= 333);
                assert(fast.delay_ms >= 360 && fast.delay_ms <= 619);
            } else {
                assert(spawn.lane >= 0 && spawn.lane < 3);
                lanes |= 1u << spawn.lane;
                assert(spawn.delay_ms >= 1380 && spawn.delay_ms <= 2029);
            }
            saw_prime |= spawn.prime;
            saw_composite |= !spawn.prime;
            previous = spawn.number;
        }
        assert(saw_prime && saw_composite);
        if (!flight) assert(lanes == 7);
    }
}
