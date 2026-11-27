#ifndef PPF_C_API_H
#define PPF_C_API_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

const char* ppf_calcular_primos(int64_t n);
const char* ppf_gerar_primos_grandes(int bits, int quantidade, const char* nome_arquivo, bool salvar_arquivo);
const char* ppf_super_primos_nativo(int bits, int quantidade, int threads, const char* caminho_arquivo);
bool ppf_testar_primalidade_gigante(const char* numero, int repeticoes);
enum {
    PPF_SPECIAL_TWINS,
    PPF_SPECIAL_SOPHIE,
    PPF_SPECIAL_COUSINS,
    PPF_SPECIAL_SEXY,
    PPF_SPECIAL_PALINDROMES,
    PPF_SPECIAL_FERMAT,
    PPF_SPECIAL_SIEVE_LIMIT = 10000000,
    PPF_SPECIAL_MAX_COUNT = 10000
};
bool ppf_special_primes_supported(int family, int64_t limit, int count);
const char* ppf_special_primes(int family, int64_t limit, int count);
const char* ppf_short_primes(int bits, int count);
enum { PPF_SHORT_PRIME_MAX_BITS = 12 };
int ppf_short_prime_count(int bits);

bool ppf_legendre_report_supported(int64_t first, int64_t last);
const char* ppf_legendre_report(int64_t first, int64_t last);
int64_t ppf_legendre_max_n(void);
enum { PPF_LEGENDRE_MAX_DIGITS = 10000 };
const char* ppf_legendre_report_decimal(const char* first, const char* last);
const char* ppf_legendre_witness_decimal(const char* n);
// Returns the first prime strictly between the bounds, 0 if absent, or -1 for invalid bounds.
int64_t ppf_next_prime_between(int64_t lower, int64_t upper);
bool ppf_is_prime_u64(uint64_t number);
int64_t ppf_exact_stats_max(void);
const char* ppf_prime_stats(int64_t limit);
enum { PPF_APPROXIMATION_MAX_DIGITS = 100000 };
const char* ppf_prime_approximation(const char* limit);
enum { PPF_MERSENNE_LIST_MAX = 10000 };
const char* ppf_mersenne_report(int exponent, bool list);
enum { PPF_TRIAL_FACTOR_MAX = 10000 };
const char* ppf_factor_report(const char* number);
char* ppf_evaluate_integer(const char* input);
void ppf_free_integer(char* value);
enum { PPF_PERFECT_VERIFY, PPF_PERFECT_BY_LIMIT, PPF_PERFECT_BY_DIGITS };
int64_t ppf_perfect_verify_max(void);
const char* ppf_perfect_report(int mode, int64_t value);
const char* ppf_perfect_verify_decimal(const char* number);
enum { PPF_ODD_PERFECT_MAX_DIGITS = 12, PPF_ODD_PERFECT_MAX_ATTEMPTS = 100000 };
typedef struct {
    int64_t next;
    int64_t found;
    int tested;
} PPFOddPerfectBatch;
PPFOddPerfectBatch ppf_scan_odd_perfect(int64_t first, int64_t last, int attempts);
int ppf_arcade_points(int number, bool prime, bool capture);
int ppf_arcade_add_score(int score, int points);
int ppf_arcade_stage(int score, bool flight);
int ppf_arcade_phase_target(int stage, bool flight);
int ppf_arcade_phase_duration_ms(bool flight);
enum {
    PPF_ARCADE_PHASE_RUNNING,
    PPF_ARCADE_PHASE_PASSED,
    PPF_ARCADE_PHASE_BELOW_TARGET,
    PPF_ARCADE_PHASE_CRASHED
};
int ppf_arcade_phase_outcome(int stage, bool flight, int elapsed_ms, int points, bool crashed);
enum {
    PPF_ARCADE_CAMPAIGN_STAGES = 52,
    PPF_ARCADE_REGULAR = 0, PPF_ARCADE_ARMED, PPF_ARCADE_REPAIR
};
typedef struct { int exponent, prefix, health, pattern; } PPFArcadeBoss;
PPFArcadeBoss ppf_arcade_boss(int stage);
int ppf_arcade_enemy_role(bool prime, bool flight, int stage, uint32_t seed);
int ppf_arcade_add_charge(int charge, int prime);
int ppf_arcade_boss_damage(int charge);
int ppf_arcade_flight_outcome(int lives, int boss_health);
enum { PPF_ARCADE_LOOT_NONE, PPF_ARCADE_LOOT_LIFE, PPF_ARCADE_LOOT_PART, PPF_ARCADE_LOOT_WEAPON, PPF_ARCADE_LOOT_SHIELD };
enum { PPF_ARCADE_PICKUP_SHIELD_MS = 4500, PPF_ARCADE_SHIELD_INTERCEPT = 54 };
int ppf_arcade_boss_drops(int before, int after, int maximum);
bool ppf_arcade_shield_intercepts(double dx, double dy);
int ppf_arcade_flight_duration_ms(int stage);
int ppf_arcade_boss_scale_percent(int stage);
int ppf_arcade_boss_escorts(int stage);
int ppf_arcade_flight_loot(int stage, int destroyed);
int ppf_arcade_upgrade_damage(int charge, int weapon_level);
int ppf_arcade_max_lives(int parts);
int ppf_arcade_boss_guard_cycle_ms(int stage);
int ppf_arcade_boss_guard_window_ms(int stage);
bool ppf_arcade_boss_guarded(int stage, int boss_elapsed_ms, bool has_escort);
int ppf_arcade_flight_zone(int stage, bool approach);
int ppf_arcade_vehicle(int stage);
typedef struct {
    int max_frame_ms;
    int spawn_start_ms;
    int shot_cooldown_ms;
    int shield_ms;
    int hit_grace_ms;
    int runner_x;
    int floor_y;
    int runner_center;
    int ship_y;
    int ship_margin;
    int control_height;
    int spawn_margin;
    int shot_offset;
    int shot_speed;
    int lives;
} PPFArcadeRules;

typedef struct {
    int number;
    bool prime;
    int speed;
    int delay_ms;
    int lane;
} PPFArcadeSpawn;

enum {
    PPF_ARCADE_RUNNER,
    PPF_ARCADE_RUNNER_NUMBER,
    PPF_ARCADE_SHIP,
    PPF_ARCADE_SPACE_PRIME,
    PPF_ARCADE_SPACE_COMPOSITE,
    PPF_ARCADE_SHOT,
    PPF_ARCADE_LANE_MAX = 10000
};

PPFArcadeRules ppf_arcade_rules(void);
PPFArcadeSpawn ppf_arcade_spawn(bool flight, int score, uint32_t seed, int previous_number);
double ppf_arcade_runner_speed(int score);
bool ppf_arcade_road_hit(double previous_x, double previous_y, double x, double y,
                         double target_x, double target_y, double height);
double ppf_arcade_jump_height(double seconds);
bool ppf_arcade_collides(int first, double x1, double y1, int second, double x2, double y2);
bool ppf_arcade_runner_passed(double obstacle_x);
enum {
    PPF_ARCADE_CONTACT_NONE,
    PPF_ARCADE_CONTACT_RESCUE,
    PPF_ARCADE_CONTACT_SHIELD,
    PPF_ARCADE_CONTACT_DAMAGE
};
int ppf_arcade_flight_contact(bool prime, bool shield, bool invulnerable);

enum {
    PPF_MATCH_SIDE = 6,
    PPF_MATCH_CELLS = 36,
    PPF_MATCH_MAX_GROUPS = 24,
    PPF_MATCH_MAX_LEVELS = 24,
    PPF_MATCH_HISTORY_MAX = 50,
    PPF_MATCH_PREVIEW_MS = 2000,
    PPF_MATCH_VALUE_TICKETS = 10857600,
    PPF_MATCH_TILE_VALUE_MAX = 149,
    PPF_MATCH_BLACK = 99
};

enum {
    PPF_MATCH_HAMMER,
    PPF_MATCH_PLUS_1,
    PPF_MATCH_PLUS_2,
    PPF_MATCH_PLUS_3,
    PPF_MATCH_SHUFFLE,
    PPF_MATCH_SWAP,
    PPF_MATCH_COPY_COLOR,
    PPF_MATCH_POLISH_UP,
    PPF_MATCH_POLISH_DOWN,
    PPF_MATCH_ROW_RAY,
    PPF_MATCH_COLUMN_RAY,
    PPF_MATCH_HINT,
    PPF_MATCH_MULTIPLIER
};

typedef struct {
    uint64_t blast_mask;
    uint64_t credited_mask;
    int64_t points;
    int64_t black_value;
    int blast_kind; /* 0: none, 1: neighbours, 2: board primes, 3: full board */
} PPFMatchResult;

int ppf_match_groups(const int32_t* colors, uint64_t occupied, uint64_t* masks);
PPFMatchResult ppf_match_resolve(const int32_t* values, uint64_t black,
                                 uint64_t group, uint64_t matched,
                                 uint64_t already_credited, int64_t sum);
int ppf_match_level_target(int level);
int ppf_match_level_moves(int level);
int ppf_match_percent(int score, int target);
int ppf_match_stars(int score, int target);
uint32_t ppf_match_level_powers(int level);
uint32_t ppf_match_victory_rewards(int wins);
int ppf_match_draw_value(int ticket);
bool ppf_match_local_pattern(const int32_t* values, const int32_t* colors, int index, int value, int color);
int ppf_match_polished_value(int value, int color, int delta);

void ppf_iniciar_geracao_job(int bits, int quantidade, int threads, const char* caminho_arquivo);
bool ppf_iniciar_primos_aleatorios_job(int bits, int quantidade, const char* caminho_arquivo);
const char* ppf_obter_status_job(void);
bool ppf_geracao_job_concluido(void);
const char* ppf_obter_resultado_job(void);
void ppf_cancelar_job(void);

void ppf_release_string(const char* ptr);

#ifdef __cplusplus
}
#endif

#endif
