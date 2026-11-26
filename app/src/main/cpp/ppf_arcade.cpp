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
    return {number, prime, 0, delay, 0};
}

double ppf_arcade_runner_speed(int score) {
    return 220 + std::min(135.0, std::max(0, score) * 0.46);
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
