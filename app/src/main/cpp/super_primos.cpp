#include <jni.h>

#include "ppf_engine.h"
#include "ppf_c_api.h"

#include <string>

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadePoints(
    JNIEnv*, jclass, jint number, jboolean prime, jboolean capture) {
    return ppf_arcade_points(number, prime, capture);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeAddScoreNativo(JNIEnv*, jclass, jint score, jint points) {
    return ppf_arcade_add_score(score, points);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeStage(JNIEnv*, jclass, jint score, jboolean flight) {
    return ppf_arcade_stage(score, flight);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadePhaseTarget(JNIEnv*, jclass, jint stage, jboolean flight) {
    return ppf_arcade_phase_target(stage, flight != JNI_FALSE);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadePhaseDuration(JNIEnv*, jclass, jboolean flight) {
    return ppf_arcade_phase_duration_ms(flight != JNI_FALSE);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_primeprofast_app_MainActivity_arcadeBossNativo(JNIEnv* env, jclass, jint stage) {
    auto b = ppf_arcade_boss(stage);
    const jint data[] = {b.exponent, b.prefix, b.health, b.pattern};
    auto result = env->NewIntArray(4);
    if (result) env->SetIntArrayRegion(result, 0, 4, data);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeEnemyRoleNativo(JNIEnv*, jclass, jboolean prime,
    jboolean flight, jint stage, jint seed) {
    return ppf_arcade_enemy_role(prime, flight, stage, static_cast<uint32_t>(seed));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeAddChargeNativo(JNIEnv*, jclass, jint charge, jint prime) {
    return ppf_arcade_add_charge(charge, prime);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeBossDamageNativo(JNIEnv*, jclass, jint charge) {
    return ppf_arcade_boss_damage(charge);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_primeprofast_app_MainActivity_arcadeFlightStageNativo(JNIEnv* env, jclass, jint stage) {
    const jint data[] = {ppf_arcade_flight_duration_ms(stage), ppf_arcade_boss_scale_percent(stage),
                         ppf_arcade_boss_escorts(stage), ppf_arcade_boss_guard_cycle_ms(stage),
                         ppf_arcade_boss_guard_window_ms(stage), ppf_arcade_flight_zone(stage, false),
                         ppf_arcade_flight_zone(stage, true)};
    jintArray result = env->NewIntArray(7);
    if (result != nullptr) env->SetIntArrayRegion(result, 0, 7, data);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeFlightLootNativo(JNIEnv*, jclass, jint stage, jint destroyed) {
    return ppf_arcade_flight_loot(stage, destroyed);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeBossDropsNativo(JNIEnv*, jclass, jint before, jint after, jint maximum) {
    return ppf_arcade_boss_drops(before, after, maximum);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_arcadeShieldInterceptsNativo(JNIEnv*, jclass, jdouble dx, jdouble dy) {
    return ppf_arcade_shield_intercepts(dx, dy) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeUpgradeDamageNativo(JNIEnv*, jclass, jint charge, jint weapon) {
    return ppf_arcade_upgrade_damage(charge, weapon);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeMaxLivesNativo(JNIEnv*, jclass, jint parts) {
    return ppf_arcade_max_lives(parts);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeFlightOutcomeNativo(JNIEnv*, jclass, jint lives, jint hp) {
    return ppf_arcade_flight_outcome(lives, hp);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadePhaseOutcome(JNIEnv*, jclass, jint stage, jboolean flight,
    jint elapsed, jint points, jboolean crashed) {
    return ppf_arcade_phase_outcome(stage, flight != JNI_FALSE, elapsed, points, crashed != JNI_FALSE);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeVehicle(JNIEnv*, jclass, jint stage) {
    return ppf_arcade_vehicle(stage);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_primeprofast_app_MainActivity_arcadeRulesNativo(JNIEnv* env, jclass) {
    const auto r = ppf_arcade_rules();
    const jint values[] = {
        r.max_frame_ms, r.spawn_start_ms, r.shot_cooldown_ms, r.shield_ms, r.hit_grace_ms,
        r.runner_x, r.floor_y, r.runner_center, r.ship_y, r.ship_margin, r.control_height,
        r.spawn_margin, r.shot_offset, r.shot_speed, r.lives, PPF_ARCADE_LANE_MAX,
        PPF_ARCADE_RUNNER, PPF_ARCADE_RUNNER_NUMBER, PPF_ARCADE_SHIP, PPF_ARCADE_SPACE_PRIME,
        PPF_ARCADE_SPACE_COMPOSITE, PPF_ARCADE_SHOT, PPF_ARCADE_CONTACT_RESCUE,
        PPF_ARCADE_CONTACT_SHIELD, PPF_ARCADE_CONTACT_DAMAGE
    };
    constexpr jsize count = sizeof(values) / sizeof(values[0]);
    jintArray result = env->NewIntArray(count);
    if (result != nullptr) env->SetIntArrayRegion(result, 0, count, values);
    return result;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_primeprofast_app_MainActivity_arcadeSpawnNativo(
    JNIEnv* env, jclass, jboolean flight, jint score, jint seed, jint previous) {
    const auto item = ppf_arcade_spawn(flight != JNI_FALSE, score, static_cast<uint32_t>(seed), previous);
    const jint values[] = {item.number, item.prime ? 1 : 0, item.speed, item.delay_ms, item.lane};
    jintArray result = env->NewIntArray(5);
    if (result != nullptr) env->SetIntArrayRegion(result, 0, 5, values);
    return result;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_primeprofast_app_MainActivity_arcadeRunnerSpeedNativo(JNIEnv*, jclass, jint score) {
    return ppf_arcade_runner_speed(score);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_primeprofast_app_MainActivity_arcadeRoadDriveSpeedNativo(JNIEnv*, jclass, jint score,
                                                                  jdouble throttle, jdouble speed, jdouble seconds) {
    return ppf_arcade_road_drive_speed(score, throttle, speed, seconds);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_arcadeRoadHitNativo(JNIEnv*, jclass, jdouble previousX,
    jdouble previousY, jdouble x, jdouble y, jdouble targetX, jdouble targetY, jdouble height) {
    return ppf_arcade_road_hit(previousX, previousY, x, y, targetX, targetY, height);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_primeprofast_app_MainActivity_arcadeJumpHeightNativo(JNIEnv*, jclass, jdouble seconds) {
    return ppf_arcade_jump_height(seconds);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_arcadeCollidesNativo(
    JNIEnv*, jclass, jint first, jdouble x1, jdouble y1, jint second, jdouble x2, jdouble y2) {
    return ppf_arcade_collides(first, x1, y1, second, x2, y2) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_arcadeRunnerPassedNativo(JNIEnv*, jclass, jdouble x) {
    return ppf_arcade_runner_passed(x) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_arcadeFlightContactNativo(
    JNIEnv*, jclass, jboolean prime, jboolean shield, jboolean invulnerable) {
    return ppf_arcade_flight_contact(prime != JNI_FALSE, shield != JNI_FALSE, invulnerable != JNI_FALSE);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_primeprofast_app_MainActivity_matchGroupsNativo(
    JNIEnv* env, jclass, jintArray colors, jlong occupied) {
    if (colors == nullptr || env->GetArrayLength(colors) != PPF_MATCH_CELLS) return nullptr;
    jint inputs[PPF_MATCH_CELLS];
    env->GetIntArrayRegion(colors, 0, PPF_MATCH_CELLS, inputs);
    if (env->ExceptionCheck()) return nullptr;
    uint64_t masks[PPF_MATCH_MAX_GROUPS];
    const int count = ppf_match_groups(inputs, occupied, masks);
    jlong output[PPF_MATCH_MAX_GROUPS];
    for (int i = 0; i < count; ++i) output[i] = static_cast<jlong>(masks[i]);
    jlongArray result = env->NewLongArray(count);
    if (result != nullptr && count > 0) env->SetLongArrayRegion(result, 0, count, output);
    return result;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_primeprofast_app_MainActivity_matchResolveNativo(
    JNIEnv* env, jclass, jintArray values, jlong black, jlong group, jlong matched, jlong credited, jlong sum) {
    if (values == nullptr || env->GetArrayLength(values) != PPF_MATCH_CELLS) return nullptr;
    jint inputs[PPF_MATCH_CELLS];
    env->GetIntArrayRegion(values, 0, PPF_MATCH_CELLS, inputs);
    if (env->ExceptionCheck()) return nullptr;
    const auto entry = ppf_match_resolve(inputs, black, group, matched, credited, sum);
    const jlong output[] = {static_cast<jlong>(entry.blast_mask), static_cast<jlong>(entry.credited_mask),
                            entry.points, entry.black_value, entry.blast_kind};
    jlongArray result = env->NewLongArray(5);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, 5, output);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchLevelTargetNativo(JNIEnv*, jclass, jint level) {
    return ppf_match_level_target(level);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchLevelMovesNativo(JNIEnv*, jclass, jint level) {
    return ppf_match_level_moves(level);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchPercentNativo(JNIEnv*, jclass, jint score, jint target) {
    return ppf_match_percent(score, target);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchStarsNativo(JNIEnv*, jclass, jint score, jint target) {
    return ppf_match_stars(score, target);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchLevelPowersNativo(JNIEnv*, jclass, jint level) {
    return ppf_match_level_powers(level);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchVictoryRewardsNativo(JNIEnv*, jclass, jint wins) {
    return ppf_match_victory_rewards(wins);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchDrawValueNativo(JNIEnv*, jclass, jint ticket) {
    return ppf_match_draw_value(ticket);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_matchLocalPatternNativo(
    JNIEnv* env, jclass, jintArray values, jintArray colors, jint index, jint value, jint color) {
    if (values == nullptr || colors == nullptr || env->GetArrayLength(values) != PPF_MATCH_CELLS
        || env->GetArrayLength(colors) != PPF_MATCH_CELLS) return JNI_FALSE;
    jint raw_values[PPF_MATCH_CELLS];
    jint raw_colors[PPF_MATCH_CELLS];
    env->GetIntArrayRegion(values, 0, PPF_MATCH_CELLS, raw_values);
    if (env->ExceptionCheck()) return JNI_FALSE;
    env->GetIntArrayRegion(colors, 0, PPF_MATCH_CELLS, raw_colors);
    if (env->ExceptionCheck()) return JNI_FALSE;
    return ppf_match_local_pattern(raw_values, raw_colors, index, value, color) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_matchPolishedValueNativo(
    JNIEnv*, jclass, jint value, jint color, jint delta) {
    return ppf_match_polished_value(value, color, delta);
}

namespace {
jstring copy_card_report(JNIEnv* env, const char* report) {
    if (report == nullptr) {
        jclass exception = env->FindClass("java/lang/IllegalStateException");
        if (exception != nullptr) env->ThrowNew(exception, "Falha ao preparar o resultado de primos");
        return nullptr;
    }
    jstring text = env->NewStringUTF(report);
    ppf_release_string(report);
    return text;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_specialPrimesNativo(
    JNIEnv* env, jclass, jint family, jlong limit, jint count) {
    if (!ppf_special_primes_supported(family, limit, count)) return nullptr;
    return copy_card_report(env, ppf_special_primes(family, limit, count));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_shortPrimesNativo(JNIEnv* env, jclass, jint bits, jint count) {
    return copy_card_report(env, ppf_short_primes(bits, count));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_primeprofast_app_MainActivity_shortPrimeCountNativo(JNIEnv*, jclass, jint bits) {
    return ppf_short_prime_count(bits);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_primeprofast_app_MainActivity_nextPrimeBetweenNativo(JNIEnv*, jclass, jlong lower, jlong upper) {
    return ppf_next_prime_between(lower, upper);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_ehPrimo64Nativo(JNIEnv*, jclass, jlong bits) {
    return ppf_is_prime_u64(static_cast<uint64_t>(bits)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_legendreReportNativo(JNIEnv* env, jclass, jstring first, jstring last) {
    if (!first || !last) return nullptr;
    const char* a = env->GetStringUTFChars(first, nullptr);
    if (!a) return nullptr;
    const char* b = env->GetStringUTFChars(last, nullptr);
    const char* report = b ? ppf_legendre_report_decimal(a, b) : nullptr;
    if (b) env->ReleaseStringUTFChars(last, b);
    env->ReleaseStringUTFChars(first, a);
    return copy_card_report(env, report);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_legendreWitnessNativo(JNIEnv* env, jclass, jstring input) {
    if (!input) return nullptr;
    const char* n = env->GetStringUTFChars(input, nullptr);
    if (!n) return nullptr;
    const char* witness = ppf_legendre_witness_decimal(n);
    env->ReleaseStringUTFChars(input, n);
    return copy_card_report(env, witness);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_primeStatsNativo(JNIEnv* env, jclass, jlong limit) {
    if (limit < 2 || limit > ppf_exact_stats_max()) return nullptr;
    return copy_card_report(env, ppf_prime_stats(limit));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_primeApproximationNativo(JNIEnv* env, jclass, jstring limit) {
    if (!limit || env->GetStringLength(limit) > PPF_APPROXIMATION_MAX_DIGITS) return nullptr;
    const char* decimal = env->GetStringUTFChars(limit, nullptr);
    if (!decimal) return nullptr;
    const char* report = ppf_prime_approximation(decimal);
    env->ReleaseStringUTFChars(limit, decimal);
    return copy_card_report(env, report);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_mersenneReportNativo(JNIEnv* env, jclass, jint p, jboolean list) {
    if (p < 2 || (list && p > PPF_MERSENNE_LIST_MAX)) return nullptr;
    return copy_card_report(env, ppf_mersenne_report(p, list != JNI_FALSE));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_evaluateIntegerNativo(JNIEnv* env, jclass, jstring expression) {
    if (!expression || env->GetStringLength(expression) > 200000) return nullptr;
    const char* input = env->GetStringUTFChars(expression, nullptr);
    if (!input) return nullptr;
    char* decimal = ppf_evaluate_integer(input);
    env->ReleaseStringUTFChars(expression, input);
    if (!decimal) return nullptr;
    jstring result = env->NewStringUTF(decimal);
    ppf_free_integer(decimal);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_factorReportNativo(JNIEnv* env, jclass, jstring number) {
    if (number == nullptr) return nullptr;
    const char* decimal = env->GetStringUTFChars(number, nullptr);
    if (decimal == nullptr) return nullptr;
    const char* report = ppf_factor_report(decimal);
    env->ReleaseStringUTFChars(number, decimal);
    return copy_card_report(env, report);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_perfectVerifyNativo(JNIEnv* env, jclass, jstring number) {
    if (!number || env->GetStringLength(number) > 10000) return nullptr;
    const char* decimal = env->GetStringUTFChars(number, nullptr);
    if (!decimal) return nullptr;
    const char* report = ppf_perfect_verify_decimal(decimal);
    env->ReleaseStringUTFChars(number, decimal);
    return copy_card_report(env, report);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_primeprofast_app_MainActivity_scanOddPerfectNativo(JNIEnv* env, jclass, jlong first, jlong last, jint attempts) {
    const auto batch = ppf_scan_odd_perfect(first, last, attempts);
    if (!batch.next) return nullptr;
    const jlong values[] = {batch.next, batch.found, batch.tested};
    auto result = env->NewLongArray(3);
    if (result) env->SetLongArrayRegion(result, 0, 3, values);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_perfectReportNativo(JNIEnv* env, jclass, jint mode, jlong value) {
    if (mode == PPF_PERFECT_VERIFY && (value < 1 || value > ppf_perfect_verify_max())) return nullptr;
    return copy_card_report(env, ppf_perfect_report(mode, value));
}

extern "C" JNIEXPORT void JNICALL
Java_com_primeprofast_app_MainActivity_iniciarGeracaoPrimosGigantesJob(
    JNIEnv* env, jobject /* this */, jint num_bits, jint num_primos, jint num_threads, jstring caminho_arquivo) {
    std::string caminho_out;
    if (caminho_arquivo != nullptr) {
        const char* p = env->GetStringUTFChars(caminho_arquivo, nullptr);
        if (p != nullptr) {
            caminho_out.assign(p);
            env->ReleaseStringUTFChars(caminho_arquivo, p);
        }
    }
    ppf::iniciar_geracao_primos_gigantes_job(num_bits, num_primos, num_threads, caminho_out);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_iniciarPrimosAleatoriosJob(
    JNIEnv* env, jobject, jint bits, jint quantidade, jstring caminho) {
    if (!caminho) return JNI_FALSE;
    const char* path = env->GetStringUTFChars(caminho, nullptr);
    if (!path) return JNI_FALSE;
    const bool started = ppf_iniciar_primos_aleatorios_job(bits, quantidade, path);
    env->ReleaseStringUTFChars(caminho, path);
    return started ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_obterStatusGeracaoPrimosGigantesJob(
    JNIEnv* env, jobject /* this */) {
  std::string status = ppf::obter_status_geracao_primos_gigantes_job();
  return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_geracaoPrimosGigantesJobConcluido(
    JNIEnv* /* env */, jobject /* this */) {
  return ppf::geracao_primos_gigantes_job_concluido() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_obterResultadoGeracaoPrimosGigantesJob(
    JNIEnv* env, jobject /* this */) {
  std::string resultado = ppf::obter_resultado_geracao_primos_gigantes_job();
  return env->NewStringUTF(resultado.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_primeprofast_app_MainActivity_cancelarGeracaoPrimosGigantesJob(
    JNIEnv* /* env */, jobject /* this */) {
  ppf::cancelar_geracao_primos_gigantes_job();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_primeprofast_app_MainActivity_testarPrimalidadeGiganteNativo(
    JNIEnv* env, jobject /* this */, jstring numero, jint repeticoes) {
  if (numero == nullptr) {
    return JNI_FALSE;
  }
  const char* numero_c = env->GetStringUTFChars(numero, 0);
  if (numero_c == nullptr) {
    return JNI_FALSE;
  }
  bool eh_primo = ppf::testar_primalidade_gigante(numero_c, repeticoes);
  env->ReleaseStringUTFChars(numero, numero_c);
  return eh_primo ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_gerarPrimosGrandes(
    JNIEnv* env, jobject /* this */, jint num_bits, jint num_primos, jstring nome_arquivo, jboolean salvar_arquivo) {
  std::string nome_arquivo_str;
  if (nome_arquivo != nullptr) {
    const char* nome_arquivo_c = env->GetStringUTFChars(nome_arquivo, 0);
    if (nome_arquivo_c != nullptr) {
      nome_arquivo_str.assign(nome_arquivo_c);
      env->ReleaseStringUTFChars(nome_arquivo, nome_arquivo_c);
    }
  }
  std::string resultado = ppf::gerar_primos_grandes(
      num_bits, num_primos, nome_arquivo_str, salvar_arquivo != JNI_FALSE);
  return env->NewStringUTF(resultado.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_primeprofast_app_MainActivity_calcularPrimos(JNIEnv* env, jobject /* this */, jlong n) {
  std::string resultado = ppf::calcular_primos(n);
  return env->NewStringUTF(resultado.c_str());
}
