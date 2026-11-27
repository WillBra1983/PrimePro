#include "ppf_c_api.h"

#include <algorithm>
#include <array>
#include <limits>
#include <random>

namespace {
constexpr PPFArcadeRules rules = {
    33, 1200, 185, 2500, 800,
    72, 58, 26, 104, 36, 56, 30, 36, 420, 3
};

// Bounds relative to the logical centre; the vertical axis points up.
struct Bounds { int left; int right; int bottom; int top; };
constexpr Bounds collision_boxes[] = {
    {-19, 19, -21, 22},   // runner
    {-20, 20, -19, 22},   // runner number
    {-27, 27, -24, 30},   // ship
    {-22, 22, -22, 22},   // prime in flight
    {-20, 20, -20, 20},   // composite in flight
    {-7, 7, -12, 32}      // shot
};
}

int ppf_arcade_points(int number, bool prime, bool capture) {
    // Pass and destroy award composites; capture awards primes.
    return number >= 2 && prime == capture ? number : 0;
}

int ppf_arcade_stage(int score, bool flight) {
    return score > 0 ? score / (flight ? 600 : 450) : 0;
}

int ppf_arcade_phase_target(int stage, bool flight) {
    if (stage < 0) return 0;
    return (flight ? 600 : 450) + std::min(stage, 12) * (flight ? 45 : 35);
}

int ppf_arcade_phase_duration_ms(bool flight) {
    return flight ? 50000 : 40000;
}

int ppf_arcade_phase_outcome(int stage, bool flight, int elapsed_ms, int points, bool crashed) {
    if (crashed) return PPF_ARCADE_PHASE_CRASHED;
    if (elapsed_ms < ppf_arcade_phase_duration_ms(flight)) return PPF_ARCADE_PHASE_RUNNING;
    return points >= ppf_arcade_phase_target(stage, flight) ? PPF_ARCADE_PHASE_PASSED : PPF_ARCADE_PHASE_BELOW_TARGET;
}

PPFArcadeBoss ppf_arcade_boss(int stage) {
    constexpr int exponents[] = {
        2,3,5,7,13,17,19,31,61,89,107,127,521,607,1279,2203,2281,3217,4253,4423,9689,9941,
        11213,19937,21701,23209,44497,86243,110503,132049,216091,756839,859433,1257787,1398269,
        2976221,3021377,6972593,13466917,20996011,24036583,25964951,30402457,32582657,37156667,
        42643801,43112609,57885161,74207281,77232917,82589933,136279841
    };
    // Leading decimal digits of 2^p - 1; the large integers need not be constructed in the game.
    constexpr int prefixes[] = {
        3,7,31,127,819,131,524,214,230,618,162,170,686,531,104,147,446,259,190,285,478,346,
        281,431,448,402,854,536,521,512,746,174,129,412,814,623,127,437,924,125,299,122,315,
        124,202,169,316,581,300,467,148,881
    };
    static_assert(sizeof(exponents) / sizeof(int) == PPF_ARCADE_CAMPAIGN_STAGES, "Boss exponent count");
    static_assert(sizeof(prefixes) / sizeof(int) == PPF_ARCADE_CAMPAIGN_STAGES, "Boss prefix count");
    if (stage < 0 || stage >= PPF_ARCADE_CAMPAIGN_STAGES) return {};
    return {exponents[stage], prefixes[stage], 175 + stage * 11 + stage * stage / 3, stage % 3};
}

int ppf_arcade_enemy_role(bool prime, bool flight, int stage, uint32_t seed) {
    if (prime) return PPF_ARCADE_REGULAR;
    if (flight && seed % 13 == 0) return PPF_ARCADE_ARMED;
    return seed % 100 < 24u + static_cast<unsigned>(std::min(12, std::max(0, stage)))
        ? PPF_ARCADE_ARMED : PPF_ARCADE_REGULAR;
}

int ppf_arcade_add_charge(int charge, int prime) {
    const int current = std::min(100, std::max(0, charge));
    return current + std::min(100 - current, std::min(16, std::max(1, prime / 4)));
}

int ppf_arcade_boss_damage(int charge) {
    return 8 + std::min(100, std::max(0, charge)) / 12;
}

int ppf_arcade_flight_outcome(int lives, int boss_health) {
    if (lives <= 0) return PPF_ARCADE_PHASE_CRASHED;
    return boss_health == 0 ? PPF_ARCADE_PHASE_PASSED : PPF_ARCADE_PHASE_RUNNING;
}

int ppf_arcade_flight_duration_ms(int stage) {
    stage = std::min(PPF_ARCADE_CAMPAIGN_STAGES - 1, std::max(0, stage));
    return ppf_arcade_phase_duration_ms(true) + stage * 1600;
}

int ppf_arcade_boss_scale_percent(int stage) {
    return 100 + std::min(65, std::min(33, std::max(0, stage)) * 2);
}

int ppf_arcade_boss_escorts(int stage) {
    if (stage < 1) return 0;
    return 1 + std::min(2, stage / 10);
}

int ppf_arcade_flight_loot(int stage, int destroyed) {
    if (destroyed <= 0) return PPF_ARCADE_LOOT_NONE;
    const int slot = destroyed % (stage >= 12 ? 7 : 6);
    if (slot == 2) return PPF_ARCADE_LOOT_LIFE;
    if (slot == 4) return PPF_ARCADE_LOOT_PART;
    if (slot == 5) return PPF_ARCADE_LOOT_WEAPON;
    return PPF_ARCADE_LOOT_NONE;
}

int ppf_arcade_boss_drops(int before, int after, int maximum) {
    if (maximum <= 0 || after <= 0 || before <= after) return 0;
    const int marks[] = {75, 50, 25};
    int drops = 0;
    for (int i = 0; i < 3; i++) {
        const auto threshold = static_cast<int64_t>(maximum) * marks[i];
        if (static_cast<int64_t>(before) * 100 > threshold && static_cast<int64_t>(after) * 100 <= threshold) {
            drops |= 1 << i;
        }
    }
    return drops;
}

bool ppf_arcade_shield_intercepts(double dx, double dy) {
    return dx * dx + dy * dy <= PPF_ARCADE_SHIELD_INTERCEPT * PPF_ARCADE_SHIELD_INTERCEPT;
}

int ppf_arcade_upgrade_damage(int charge, int weapon_level) {
    return ppf_arcade_boss_damage(charge) + std::min(12, std::max(0, weapon_level)) * 2;
}

int ppf_arcade_max_lives(int parts) {
    return 5 + std::min(12, std::max(0, parts)) / 3;
}

int ppf_arcade_boss_guard_cycle_ms(int stage) {
    return 4600 - std::min(30, std::max(0, stage)) * 32;
}

int ppf_arcade_boss_guard_window_ms(int stage) {
    return 1050 + std::min(30, std::max(0, stage)) * 14;
}

bool ppf_arcade_boss_guarded(int stage, int boss_elapsed_ms, bool has_escort) {
    return has_escort || std::max(0, boss_elapsed_ms) % ppf_arcade_boss_guard_cycle_ms(stage)
        < ppf_arcade_boss_guard_window_ms(stage);
}

int ppf_arcade_flight_zone(int stage, bool approach) {
    if (stage == PPF_ARCADE_CAMPAIGN_STAGES - 2) return 4;
    if (stage >= PPF_ARCADE_CAMPAIGN_STAGES - 1) return approach ? 1 : 3;
    stage = std::max(0, stage);
    const int base = (stage / 4 + stage / 17) % 4;
    return approach ? (base + 1 + stage % 3) % 4 : base;
}

int ppf_arcade_add_score(int score, int points) {
    score = std::max(0, score);
    if (points <= 0) return score;
    const int ceiling = std::numeric_limits<int>::max();
    return score > ceiling - points ? ceiling : score + points;
}

int ppf_arcade_vehicle(int stage) {
    return stage < 0 ? 0 : (stage > 2 ? 2 : stage);
}

PPFArcadeRules ppf_arcade_rules(void) {
    return rules;
}

PPFArcadeSpawn ppf_arcade_spawn(bool flight, int score, uint32_t seed, int previous_number) {
    std::mt19937 rng(seed);
    auto draw = [&rng](int low, int high) {
        return std::uniform_int_distribution<int>(low, high)(rng);
    };
    const bool prime = draw(0, 99) < (flight ? 38 : 42);
    const int ceiling = draw(97, 150);
    std::array<int, 149> candidates{};
    int size = 0;
    for (int n = 2; n <= ceiling; ++n) {
        if (n != previous_number && ppf_is_prime_u64(n) == prime) candidates[size++] = n;
    }
    const int number = candidates[draw(0, size - 1)];
    if (flight) {
        const int sector = 1 + ppf_arcade_stage(score, true);
        const int speed = std::min(300, 82 + std::min(28, sector) * 8) + draw(0, 33);
        const int delay = std::max(360, 820 - sector * 34) + draw(0, 259);
        return {number, prime, speed, delay, draw(0, PPF_ARCADE_LANE_MAX)};
    }
    const int pace = std::max(0, std::min(430, static_cast<int>(std::max(0, score) * 0.28)));
    const int delay = std::max(920, 1380 - pace) + draw(0, 649);
    return {number, prime, 0, delay, draw(0, 2)};
}

double ppf_arcade_runner_speed(int score) {
    return 220 + std::min(135.0, std::max(0, score) * 0.46);
}

bool ppf_arcade_road_hit(double previous_x, double previous_y, double x, double y,
                         double target_x, double target_y, double height) {
    if (height <= 0) return false;
    double enter = 0, leave = 1;
    // Intersect the relative movement during this frame with the obstacle box.
    const auto clip = [&](double start, double end, double radius) {
        const double delta = end - start;
        if (delta == 0) return start > -radius && start < radius;
        const double a = (-radius - start) / delta;
        const double b = (radius - start) / delta;
        enter = std::max(enter, std::min(a, b));
        leave = std::min(leave, std::max(a, b));
        return enter < leave;
    };
    return clip(previous_x - target_x, x - target_x, 38)
        && clip(previous_y - target_y, y - target_y, std::min(26.0, height * 0.05));
}

double ppf_arcade_jump_height(double seconds) {
    if (!(seconds >= 0 && seconds < 2.0 * 850.0 / 1850.0)) return 0;
    return seconds * (850.0 - 925.0 * seconds);
}

bool ppf_arcade_collides(int first, double x1, double y1, int second, double x2, double y2) {
    if (first < PPF_ARCADE_RUNNER || first > PPF_ARCADE_SHOT ||
        second < PPF_ARCADE_RUNNER || second > PPF_ARCADE_SHOT) return false;
    const auto a = collision_boxes[first];
    const auto b = collision_boxes[second];
    return x1 + a.left < x2 + b.right && x2 + b.left < x1 + a.right
        && y1 + a.bottom < y2 + b.top && y2 + b.bottom < y1 + a.top;
}

bool ppf_arcade_runner_passed(double obstacle_x) {
    return obstacle_x + collision_boxes[PPF_ARCADE_RUNNER_NUMBER].right
        <= rules.runner_x + collision_boxes[PPF_ARCADE_RUNNER].left;
}

int ppf_arcade_flight_contact(bool prime, bool shield, bool invulnerable) {
    if (prime) return PPF_ARCADE_CONTACT_RESCUE;
    if (shield) return PPF_ARCADE_CONTACT_SHIELD;
    return invulnerable ? PPF_ARCADE_CONTACT_NONE : PPF_ARCADE_CONTACT_DAMAGE;
}
