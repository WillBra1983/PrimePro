package com.primeprofast.app;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.provider.MediaStore;
import android.widget.*;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.app.AlertDialog;
import android.text.InputType;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Set;
import java.util.HashSet;
import android.content.SharedPreferences;
import com.android.billingclient.api.Purchase;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String STATE_PDF_TO_SAVE = "ppf_pdf_to_save";
    private String pdfToSave;
    private final ActivityResultLauncher<String> savePdfDocument = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/pdf"), this::salvarPdfNoDestino);
    private static final int PERMISSION_REQUEST_CODE = 123;
    /** Acima deste valor o card pede confirmação e o RSA troca BigInteger por C++/GMP. */
    private static final int LIMIAR_BITS_ROTA_NATIVA = 8192;
    private static final String SPECIAL_FERMAT_LABEL = "Primos de Fermat (2^(2^n)+1)";
    private static final int LIGHT_BG_TOP = Color.rgb(246, 248, 251);
    private static final int LIGHT_BG_BOTTOM = Color.rgb(232, 239, 247);
    private static final int LIGHT_SURFACE = Color.rgb(255, 255, 255);
    private static final int LIGHT_SURFACE_RAISED = Color.rgb(247, 250, 252);
    private static final int LIGHT_TEXT = Color.rgb(20, 32, 51);
    private static final int LIGHT_TEXT_MUTED = Color.rgb(92, 105, 122);
    private static final int LIGHT_BORDER = Color.rgb(214, 225, 236);
    private static final int DARK_BG_TOP = Color.rgb(14, 19, 26);
    private static final int DARK_BG_BOTTOM = Color.rgb(22, 30, 41);
    private static final int DARK_SURFACE = Color.rgb(26, 34, 46);
    private static final int DARK_SURFACE_RAISED = Color.rgb(32, 42, 57);
    private static final int DARK_TEXT = Color.rgb(238, 243, 248);
    private static final int DARK_TEXT_MUTED = Color.rgb(170, 182, 197);
    private static final int DARK_BORDER = Color.rgb(54, 68, 88);
    private static final int BRAND_PRIMARY = Color.rgb(29, 78, 137);
    private static final int BRAND_SECONDARY = Color.rgb(46, 125, 111);
    private static final int PRIME_MATCH_ROWS = 6;
    private static final int PRIME_MATCH_COLS = 6;
    private static final int PRIME_MATCH_MIN_CHAIN = 3;
    private static final int PRIME_MATCH_MAX_CHAIN = 5;
    private static final int PRIME_MATCH_COR_PRETA = 99;
    private static final int PRIME_MATCH_POWER_NONE = 0;
    private static final int PRIME_MATCH_POWER_HAMMER = 1;
    private static final int PRIME_MATCH_POWER_FREE_SWAP = 2;
    private static final int PRIME_MATCH_POWER_COLOR_COPY = 3;
    private static final int PRIME_MATCH_POWER_POLISH_UP = 4;
    private static final int PRIME_MATCH_POWER_POLISH_DOWN = 5;
    private static final int PRIME_MATCH_POWER_ROW_RAY = 6;
    private static final int PRIME_MATCH_POWER_COL_RAY = 7;
    private static final int PRIME_MATCH_SPECIAL_NONE = 0;
    private static final int PRIME_MATCH_SPECIAL_AROUND_BLACK = 1;
    private static final int PRIME_MATCH_SPECIAL_BOARD_PRIMES = 2;
    private static final int PRIME_MATCH_SPECIAL_FULL_BOARD = 3;
    private static final int PRIME_MATCH_MAX_LEVELS = 24;
    private static final String PREFS_PRIME_MATCH = "prime_match_rewards";
    private static final String KEY_PM_HAMMER = "hammer";
    private static final String KEY_PM_PLUS_1 = "plus_1";
    private static final String KEY_PM_PLUS_2 = "plus_2";
    private static final String KEY_PM_PLUS_3 = "plus_3";
    private static final String KEY_PM_RESHUFFLE = "reshuffle";
    private static final String KEY_PM_FREE_SWAP = "free_swap";
    private static final String KEY_PM_COLOR_COPY = "color_copy";
    private static final String KEY_PM_POLISH_UP = "polish_up";
    private static final String KEY_PM_POLISH_DOWN = "polish_down";
    private static final String KEY_PM_ROW_RAY = "row_ray";
    private static final String KEY_PM_COL_RAY = "col_ray";
    private static final String KEY_PM_HINT = "hint";
    private static final String KEY_PM_SHIELD = "shield";
    private static final String KEY_PM_COMBO_MULT = "combo_mult";
    // Index maps to the PPF_MATCH_* power identifiers in the shared engine.
    private static final String[] PRIME_MATCH_POWER_KEYS = {
        KEY_PM_HAMMER, KEY_PM_PLUS_1, KEY_PM_PLUS_2, KEY_PM_PLUS_3, KEY_PM_RESHUFFLE,
        KEY_PM_FREE_SWAP, KEY_PM_COLOR_COPY, KEY_PM_POLISH_UP, KEY_PM_POLISH_DOWN,
        KEY_PM_ROW_RAY, KEY_PM_COL_RAY, KEY_PM_HINT, KEY_PM_COMBO_MULT
    };
    private static final String[] PRIME_MATCH_POWER_LABELS = {
        "Martelo", "+1", "+2", "+3", "Reorganizar", "Troca livre", "Coringa", "Lapidar +1",
        "Lapidar -1", "Raio linha", "Raio coluna", "Dica prima", "Combo x2"
    };
    private static final int PRIME_MATCH_HISTORY_MAX = 50;
    private static final long PRIME_MATCH_PREVIEW_MS = 2000L;
    private static final int PRIME_MATCH_VALUE_TICKETS = 10857600;
    private static final String KEY_PM_WINS = "wins";
    private static final String KEY_PM_BEST_SCORE = "best_score";
    private static final String KEY_PM_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_PM_UNLOCKED_LEVEL = "unlocked_level";
    private static final String KEY_PM_LEVEL_STAR_PREFIX = "level_star_";
    private static final String PREFS_PRIME_RUNNER = "prime_runner";
    private static final String KEY_PR_BEST_SCORE = "best_score";
    private static final String PREFS_PRIME_SHOOTER = "prime_shooter";
    private static final String KEY_PS_BEST_SCORE = "best_score";
    private static final String KEY_PS_LAST_SCORE = "last_score";
    private static final String KEY_PS_LIVES = "remaining_lives";
    private static final String KEY_PS_SHIELD_MS = "remaining_shield_ms";

    /**
     * Destino do pedido de “entrega por e-mail” de primos &gt;8192 bits (após compra in-app na Play).
     * Substitua pelo seu e-mail real antes de publicar.
     */
    private static final String EMAIL_PEDIDO_ENTREGA_PRIMO = "seuemail@exemplo.com";
    private int megaPedidoBits;
    private int megaPedidoQuantidade;
    /** Se true, {@link #megaPedidoBits} é o tamanho total da chave RSA (ex.: 16384); senão é bits por primo na geração aleatória. */
    private boolean megaPedidoModoRsa;

    // Sistema de Segurança
    private static final String SECURITY_KEY = "PrimeProFast2024";
    private boolean isSecurityValid = false;

    // Variáveis de instância para acesso global
    private TextView resultadoView;
    private ScrollView scrollView;
    private ScrollView menuScrollView;
    private ScrollView contentScrollView;
    private LinearLayout menuContainer;
    private LinearLayout contentContainer;
    /** Layout raiz (gradiente de fundo). */
    private LinearLayout rootMainLayout;
    private LinearLayout appHeader;
    private LinearLayout arcadeHost;
    private Button btnTema;
    private final SecureRandom primeMatchRandom = new SecureRandom();
    private int[][] primeMatchValues;
    private int[][] primeMatchColors;
    private TextView[][] primeMatchCells;
    private final ArrayList<Integer> primeMatchSelection = new ArrayList<>();
    private final ArrayList<String> primeMatchHistory = new ArrayList<>();
    private TextView primeMatchScoreView;
    private TextView primeMatchMovesView;
    private TextView primeMatchStatusView;
    private TextView primeMatchHistoryView;
    private TextView primeMatchPowersTitleView;
    private TextView primeMatchInventoryView;
    private FrameLayout primeMatchStage;
    private View primeMatchCelebrationOverlay;
    private GridLayout primeMatchGrid;
    private final ArrayList<View> primeMatchHighlightViews = new ArrayList<>();
    private LinearLayout primeMatchPowersRow;
    private Button primeMatchHammerButton;
    private Button primeMatchPlus1Button;
    private Button primeMatchPlus2Button;
    private Button primeMatchPlus3Button;
    private Button primeMatchReshuffleButton;
    private Button primeMatchFreeSwapButton;
    private Button primeMatchColorCopyButton;
    private Button primeMatchPolishUpButton;
    private Button primeMatchPolishDownButton;
    private Button primeMatchRowRayButton;
    private Button primeMatchColRayButton;
    private Button primeMatchHintButton;
    private Button primeMatchShieldButton;
    private Button primeMatchComboMultButton;
    private Button primeMatchSoundButton;
    private SoundPool primeMatchSoundPool;
    private boolean primeMatchSoundEnabled = true;
    private boolean primeMatchSoundReady;
    private int primeMatchSoundTap;
    private int primeMatchSoundPrime;
    private int primeMatchSoundComposite;
    private int primeMatchSoundCombo;
    private int primeMatchSoundPower;
    private int primeMatchSoundBlack;
    private int primeMatchSoundVictory;
    private int primeMatchScore;
    private int primeMatchMoves;
    private int primeMatchTarget;
    private int primeMatchCurrentLevel = 1;
    private int primeMatchHistoryCounter;
    private int primeMatchDragStart = -1;
    private int primeMatchDragTarget = -1;
    private int primeMatchMergeAnchor = -1;
    private int primeMatchMergeFallbackAnchor = -1;
    private int primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
    private int primeMatchPowerFirstSelection = -1;
    private int primeMatchPendingModifier;
    private int primeMatchHammerCount;
    private int primeMatchPlus1Count;
    private int primeMatchPlus2Count;
    private int primeMatchPlus3Count;
    private int primeMatchReshuffleCount;
    private int primeMatchFreeSwapCount;
    private int primeMatchColorCopyCount;
    private int primeMatchPolishUpCount;
    private int primeMatchPolishDownCount;
    private int primeMatchRowRayCount;
    private int primeMatchColRayCount;
    private int primeMatchHintCount;
    private int primeMatchShieldCount;
    private int primeMatchComboMultCount;
    private int primeMatchWins;
    private int primeMatchBestScore;
    private boolean primeMatchResolving;
    private boolean primeMatchCelebrationShown;
    private boolean primeMatchRewardGranted;
    private boolean primeMatchShieldArmed;
    private boolean primeMatchComboMultiplierArmed;
    private boolean primeMatchActive;
    private boolean primeMatchAutoShufflePending;
    private PrimeRunnerView primeRunnerView;
    private PrimeShooterView primeShooterView;

    private static class PrimeMatchGrupo {
        final long mascara;
        final boolean[][] marcadas;
        final int soma;
        final int removidas;
        final int pretas;

        PrimeMatchGrupo(long mascara, boolean[][] marcadas, int soma, int removidas, int pretas) {
            this.mascara = mascara;
            this.marcadas = marcadas;
            this.soma = soma;
            this.removidas = removidas;
            this.pretas = pretas;
        }
    }

    private static class PrimeMatchResultadoGrupo {
        final PrimeMatchGrupo grupo;
        final boolean[][] explosaoMarcada;
        final int somaCalculada;
        final int somaFinal;
        final int valorPreta;
        final int saldo;
        final int ancora;
        final boolean primo;
        final boolean ajusteUsado;
        final int ajusteValor;
        final boolean multiplicadorUsado;
        final int especialTipo;
        final int especialExtra;

        PrimeMatchResultadoGrupo(
                PrimeMatchGrupo grupo,
                boolean[][] explosaoMarcada,
                int somaCalculada,
                int somaFinal,
                int valorPreta,
                int saldo,
                int ancora,
                boolean primo,
                boolean ajusteUsado,
                int ajusteValor,
                boolean multiplicadorUsado,
                int especialTipo,
                int especialExtra
        ) {
            this.grupo = grupo;
            this.explosaoMarcada = explosaoMarcada;
            this.somaCalculada = somaCalculada;
            this.somaFinal = somaFinal;
            this.valorPreta = valorPreta;
            this.saldo = saldo;
            this.ancora = ancora;
            this.primo = primo;
            this.ajusteUsado = ajusteUsado;
            this.ajusteValor = ajusteValor;
            this.multiplicadorUsado = multiplicadorUsado;
            this.especialTipo = especialTipo;
            this.especialExtra = especialExtra;
        }
    }

    // Variável para controle de parada do usuário
    private volatile boolean paradaUsuario = false;
    private final AtomicBoolean legendreEmAndamento = new AtomicBoolean(false);

    // Controle genérico de cancelamento da operação atual (cards longos)
    private final AtomicBoolean cancelarOperacaoAtual = new AtomicBoolean(false);
    private final AtomicBoolean geracaoAleatoriaAtiva = new AtomicBoolean(false);
    private Button btnCancelarOperacaoAtual;

    // Botão para parar busca sequencial
    private Button btnPararBuscaSequencial;

    // Controle de tema
    private boolean temaEscuro = false;
    private SharedPreferences preferences;

    // Sistema de monetização
    private static final String PREFS_USAGE = "usage_prefs";
    private static final String KEY_DAILY_CALCULATIONS = "daily_calculations";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_IS_PREMIUM = "is_premium";
    private static final boolean MONETIZACAO_ATIVA = false;
    private static final String KEY_CARD_CALCULATIONS = "card_calculations_";
    private static final String KEY_CARD_RESET_DATE = "card_reset_date_";
    private static final int FREE_DAILY_LIMIT = 5; // Limite gratuito: 50 cálculos por dia
    private static final long FREE_MAX_VALUE = 2000000; // Limite gratuito / testes Play: até 2 milhões

    // Sistema de limitações por card
    private static final int CARD_DAILY_LIMIT = 5; // 5 cálculos por dia por card
    private static final long CARD_MAX_NUMERIC_VALUE = 2000000; // 2 milhões (alinhado a testadores Play / freemium amplo)
    private static final int CARD_MAX_BITS = 1024; // até 1024 bits na versão gratuita limitada

    private int dailyCalculations = 0;
    private boolean isPremium = false;
    private PlayBillingManager playBillingManager;

    // Contadores por card
    private int cardCalculations = 0;
    private String currentCard = "";

    static {
        System.loadLibrary("super_primos");
        System.loadLibrary("super_primos_exe");
    }

    public native String calcularPrimos(long n);
    private static native String specialPrimesNativo(int family, long limit, int count);
    private static native String shortPrimesNativo(int bits, int count);
    private static native int shortPrimeCountNativo(int bits);
    private static native long nextPrimeBetweenNativo(long lower, long upper);
    private static native boolean ehPrimo64Nativo(long bits);
    private static native String legendreReportNativo(String first, String last);
    private static native String legendreWitnessNativo(String n);
    private static native String primeStatsNativo(long limit);
    private static native String primeApproximationNativo(String limit);
    private static native String mersenneReportNativo(int exponent, boolean list);
    private static native String factorReportNativo(String number);
    private static native String evaluateIntegerNativo(String expression);
    private static native String perfectReportNativo(int mode, long value);
    private static native String perfectVerifyNativo(String number);
    private static native long[] scanOddPerfectNativo(long first, long last, int attempts);
    private final AtomicBoolean buscaImparEmAndamento = new AtomicBoolean(false);
    private final AtomicBoolean interromperBuscaImpar = new AtomicBoolean(false);
    private static native int arcadePoints(int number, boolean prime, boolean capture);
    private static native int arcadeAddScoreNativo(int score, int points);
    private static native int arcadeStage(int score, boolean flight);
    private static native int arcadePhaseTarget(int stage, boolean flight);
    private static native int arcadePhaseDuration(boolean flight);
    private static native int arcadePhaseOutcome(int stage, boolean flight, int elapsed, int points, boolean crashed);
    private static native int[] arcadeBossNativo(int stage);
    private static native int arcadeEnemyRoleNativo(boolean prime, boolean flight, int stage, int seed);
    private static native int arcadeAddChargeNativo(int charge, int prime);
    private static native int arcadeBossDamageNativo(int charge);
    private static native int[] arcadeFlightStageNativo(int stage);
    private static native int arcadeFlightLootNativo(int stage, int destroyed);
    private static native int arcadeBossDropsNativo(int before, int after, int maximum);
    private static native boolean arcadeShieldInterceptsNativo(double dx, double dy);
    private static native int arcadeUpgradeDamageNativo(int charge, int weapon);
    private static native int arcadeMaxLivesNativo(int parts);
    private static native int arcadeFlightOutcomeNativo(int lives, int bossHealth);
    private static final int ARCADE_ARMED = 1, ARCADE_REPAIR = 2;
    private static final int EARTH_STAGE = 50, BONUS_STAGE = 51;
    private static native int arcadeVehicle(int stage);
    private static native int[] arcadeRulesNativo();
    private static native int[] arcadeSpawnNativo(boolean flight, int score, int seed, int previous);
    private static native double arcadeRunnerSpeedNativo(int score);
    private static native boolean arcadeRoadHitNativo(double previousX, double previousY, double x, double y,
            double targetX, double targetY, double height);
    private static native double arcadeJumpHeightNativo(double seconds);
    private static native boolean arcadeCollidesNativo(int first, double x1, double y1, int second, double x2, double y2);
    private static native boolean arcadeRunnerPassedNativo(double x);
    private static native int arcadeFlightContactNativo(boolean prime, boolean shield, boolean invulnerable);
    private static native long[] matchGroupsNativo(int[] colors, long occupied);
    private static native long[] matchResolveNativo(int[] values, long black, long group, long matched, long credited, long sum);
    private static native int matchLevelTargetNativo(int level);
    private static native int matchLevelMovesNativo(int level);
    private static native int matchPercentNativo(int score, int target);
    private static native int matchStarsNativo(int score, int target);
    private static native int matchLevelPowersNativo(int level);
    private static native int matchVictoryRewardsNativo(int wins);
    private static native int matchDrawValueNativo(int ticket);
    private static native boolean matchLocalPatternNativo(int[] values, int[] colors, int index, int value, int color);
    private static native int matchPolishedValueNativo(int value, int color, int delta);
    public native String gerarPrimosGrandes(int bits, int quantidade, String nomeArquivo, boolean salvarArquivo);
    public native String superPrimosNativo(int bits, int quantidade, int threads, String caminhoArquivo);
    public native boolean testarPrimalidadeGiganteNativo(String numero, int repeticoes);
    public native void iniciarGeracaoPrimosGigantesJob(int bits, int quantidade, int threads, String caminhoArquivoSaida);
    private native boolean iniciarPrimosAleatoriosJob(int bits, int quantidade, String caminhoArquivoSaida);
    public native String obterStatusGeracaoPrimosGigantesJob();
    public native boolean geracaoPrimosGigantesJobConcluido();
    public native String obterResultadoGeracaoPrimosGigantesJob();
    public native void cancelarGeracaoPrimosGigantesJob();

    /**
     * Cronômetro de operação: o mesmo {@link #inicioMs} deve ser usado nos relatórios para o tempo coincidir com a UI.
     */
    private static final class CronometroOperacaoHandle {
        final AtomicBoolean ativo;
        final long inicioMs;

        CronometroOperacaoHandle(AtomicBoolean ativo, long inicioMs) {
            this.ativo = ativo;
            this.inicioMs = inicioMs;
        }
    }

    /** Mesma formatação que {@link #mostrarStatusOperacaoEmAndamento} (1 casa decimal). */
    private static String formatarSegundosTempoCronometro(long inicioMs, long fimMs) {
        return String.format(Locale.getDefault(), "%.1f", (fimMs - inicioMs) / 1000.0);
    }

    /** Substitui a linha "Tempo total:" do relatório nativo pelo intervalo medido com o mesmo início do cronômetro. */
    private static String alinharTempoRelatorioNativoComCronometro(String relatorio, long inicioMs, long fimMs) {
        if (relatorio == null || relatorio.isEmpty()) {
            return relatorio;
        }
        String tempo = formatarSegundosTempoCronometro(inicioMs, fimMs);
        return relatorio.replaceFirst("(?m)^Tempo total:.*$", "Tempo total: " + tempo + " segundos");
    }

    /** Substitui a linha "Tempo:" do retorno de {@link #calcularPrimos(long)} pelo intervalo do cronômetro (mesmo início da UI). */
    private static String alinharTempoCalcularPrimosComCronometro(String resultado, long inicioMs, long fimMs) {
        if (resultado == null || resultado.isEmpty()) {
            return resultado;
        }
        String tempo = formatarSegundosTempoCronometro(inicioMs, fimMs);
        return resultado.replaceFirst("(?m)^Tempo:.*$", "Tempo: " + tempo + " segundos");
    }

    /**
     * Cria um botão com design moderno e cores vibrantes
     */
    private Button criarBotaoModerno(String texto, int corPrimaria, int corSecundaria) {
        Button botao = new Button(this);
        botao.setText(texto);
        botao.setTag("ppf_action");
        botao.setAllCaps(false);

        // Configurar layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpUi(6), 0, dpUi(6));
        botao.setLayoutParams(params);

        // Configurar padding
        botao.setMinHeight(dpUi(50));
        botao.setPadding(dpUi(18), dpUi(11), dpUi(18), dpUi(11));

        // Configurar texto
        botao.setTextSize(15);
        botao.setTextColor(Color.WHITE);
        botao.setTypeface(null, Typeface.BOLD);

        // Criar gradiente vibrante
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setColors(new int[]{corPrimaria, corSecundaria});
        gradient.setCornerRadius(dpUi(8));

        // Aplicar gradiente
        botao.setBackground(gradient);

        // Adicionar elevação (sombra)
        botao.setElevation(dpUi(2));

        return botao;
    }

    private int dpUi(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }

    private int corFundoTopo() {
        return temaEscuro ? DARK_BG_TOP : LIGHT_BG_TOP;
    }

    private int corFundoBase() {
        return temaEscuro ? DARK_BG_BOTTOM : LIGHT_BG_BOTTOM;
    }

    private int corSuperficie() {
        return temaEscuro ? DARK_SURFACE : LIGHT_SURFACE;
    }

    private int corSuperficieElevada() {
        return temaEscuro ? DARK_SURFACE_RAISED : LIGHT_SURFACE_RAISED;
    }

    private int corTextoPrincipal() {
        return temaEscuro ? DARK_TEXT : LIGHT_TEXT;
    }

    private int corTextoSecundario() {
        return temaEscuro ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED;
    }

    private int corBorda() {
        return temaEscuro ? DARK_BORDER : LIGHT_BORDER;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private GradientDrawable fundoArredondado(int cor, int raioDp, int larguraBordaDp, int corBorda) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cor);
        bg.setCornerRadius(dpUi(raioDp));
        if (larguraBordaDp > 0) {
            bg.setStroke(dpUi(larguraBordaDp), corBorda);
        }
        return bg;
    }

    private void atualizarCoresSistema() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            aplicarCoresLegadasDasBarras();
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!temaEscuro);
        controller.setAppearanceLightNavigationBars(!temaEscuro);
    }

    @SuppressWarnings("deprecation")
    private void aplicarCoresLegadasDasBarras() {
        getWindow().setStatusBarColor(corFundoTopo());
        getWindow().setNavigationBarColor(corFundoBase());
    }

    private void estilizarBotaoSecundario(Button btn) {
        btn.setAllCaps(false);
        btn.setTextColor(corTextoPrincipal());
        btn.setTextSize(15);
        btn.setMinHeight(dpUi(48));
        btn.setPadding(dpUi(16), dpUi(10), dpUi(16), dpUi(10));
        btn.setBackground(fundoArredondado(corSuperficieElevada(), 8, 1, corBorda()));
        btn.setElevation(dpUi(1));
    }

    private LinearLayout.LayoutParams layoutCardMenuParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpUi(5), 0, dpUi(5));
        return params;
    }

    private boolean telaConteudoVisivel() {
        return contentScrollView != null
            ? contentScrollView.getVisibility() == View.VISIBLE
            : contentContainer != null && contentContainer.getVisibility() == View.VISIBLE;
    }

    private void mostrarTelaConteudo() {
        if (contentContainer != null) {
            contentContainer.setVisibility(View.VISIBLE);
        }
        if (contentScrollView != null) {
            contentScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void ocultarTelaConteudo() {
        if (contentContainer != null) {
            contentContainer.setVisibility(View.GONE);
        }
        if (contentScrollView != null) {
            contentScrollView.setVisibility(View.GONE);
        }
    }

    private void rolarConteudoParaInicio() {
        if (contentScrollView != null) {
            contentScrollView.post(() -> contentScrollView.fullScroll(ScrollView.FOCUS_UP));
        }
    }

    private void rolarConteudoParaView(View alvo) {
        if (contentScrollView == null || alvo == null) {
            return;
        }
        contentScrollView.post(() -> {
            int y = alvo.getTop();
            android.view.ViewParent parent = alvo.getParent();
            while (parent instanceof View && parent != contentScrollView) {
                View parentView = (View) parent;
                y += parentView.getTop();
                parent = parentView.getParent();
            }
            contentScrollView.smoothScrollTo(0, Math.max(0, y - dpUi(12)));
        });
    }

    /**
     * Garante que {@link #resultadoView} volte a ficar dentro do {@link #scrollView} global.
     * Em Segurança Digital ela é movida para o layout rolável do card; ao sair, isto evita telas sem status.
     */
    private void garantirResultadoViewNoScrollGlobal() {
        if (resultadoView == null || scrollView == null) {
            return;
        }
        if (resultadoView.getParent() == scrollView) {
            return;
        }
        ViewGroup oldParent = (ViewGroup) resultadoView.getParent();
        if (oldParent != null) {
            oldParent.removeView(resultadoView);
        }
        scrollView.removeAllViews();
        scrollView.addView(resultadoView);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    /** Rótulo em negrito para campos de entrada (cards). */
    private TextView adicionarRotuloCampo(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(corTextoPrincipal());
        tv.setPadding(dpUi(2), dpUi(12), dpUi(2), dpUi(5));
        contentContainer.addView(tv);
        return tv;
    }

    /** Preenchimento dos campos de texto: cinza para não confundir com área branca do app. */
    private void aplicarEstiloCampoEntradaTematizado(EditText et) {
        et.setBackground(fundoArredondado(corSuperficieElevada(), 8, 1, corBorda()));
        et.setMinHeight(dpUi(52));
        et.setPadding(dpUi(14), dpUi(12), dpUi(14), dpUi(12));
        et.setTextSize(15);
        et.setTextColor(corTextoPrincipal());
        et.setHintTextColor(corTextoSecundario());
    }

    /** Caixa de entrada visível (borda, cantos, margem). */
    private void estilizarCampoEntrada(EditText et) {
        aplicarEstiloCampoEntradaTematizado(et);
        configurarRolagemCampoEntrada(et);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpUi(2), 0, dpUi(10));
        et.setLayoutParams(lp);
    }

    private void configurarRolagemCampoEntrada(EditText et) {
        et.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.postDelayed(() -> rolarConteudoParaView(view), 250);
                view.postDelayed(() -> rolarConteudoParaView(view), 500);
            }
        });
        et.setOnClickListener(view -> {
            view.postDelayed(() -> rolarConteudoParaView(view), 250);
        });
    }

    private void aplicarMargensDoSistema(LinearLayout root) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return;
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int margem = dpUi(10);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int types = android.view.WindowInsets.Type.systemBars()
                    | android.view.WindowInsets.Type.displayCutout()
                    | android.view.WindowInsets.Type.ime();
            android.graphics.Insets area = insets.getInsets(types);
            view.setPadding(margem + area.left, margem + area.top,
                    margem + area.right, margem + area.bottom);
            if (insets.isVisible(android.view.WindowInsets.Type.ime())) {
                View focused = view.findFocus();
                if (focused instanceof EditText) {
                    focused.post(() -> {
                        if (focused.hasFocus()) {
                            focused.requestRectangleOnScreen(new android.graphics.Rect(
                                    0, 0, focused.getWidth(), focused.getHeight()), false);
                        }
                    });
                }
            }
            return insets;
        });
        root.requestApplyInsets();
    }

    /** Fundo principal com gradiente suave (base para efeito “vidro”). */
    private void aplicarFundoGradientePrincipal() {
        if (rootMainLayout == null) {
            return;
        }
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{corFundoTopo(), corFundoBase()}
        );
        rootMainLayout.setBackground(gd);
    }

    /** Área de resultado com aparência de painel de vidro fosco. */
    private void aplicarEstiloAreaResultado() {
        if (resultadoView == null) {
            return;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpUi(8));
        bg.setColor(corSuperficie());
        bg.setStroke(dpUi(1), corBorda());
        resultadoView.setBackground(bg);
        resultadoView.setTextColor(corTextoPrincipal());
        resultadoView.setTextSize(14);
        resultadoView.setLineSpacing(dpUi(2), 1.0f);
        resultadoView.setMinHeight(dpUi(168));
        resultadoView.setPadding(dpUi(14), dpUi(14), dpUi(14), dpUi(14));
        resultadoView.setElevation(dpUi(2));
    }

    /**
     * Gera primos p e q para chave RSA de {@code bitsChave} bits (cada primo ≈ bitsChave/2 bits).
     * Usado por "Gerar chaves" e demonstrações que devem usar os mesmos valores reais.
     */
    private BigInteger[] gerarPrimosPqRSA(int bitsChave) {
        int tamanhoPrimos = bitsChave / 2;
        if (deveUsarRotaNativaPrimos(tamanhoPrimos)) {
            List<BigInteger> primosNativos = gerarPrimosBigIntegerViaNativo(tamanhoPrimos, 2);
            if (primosNativos.size() >= 2) {
                return new BigInteger[]{primosNativos.get(0), primosNativos.get(1)};
            }
            throw new RuntimeException("Não foi possível gerar primos suficientes pela rota nativa");
        }
        CopyOnWriteArrayList<BigInteger> primosEncontrados = new CopyOnWriteArrayList<>();
        AtomicBoolean pararThreads = new AtomicBoolean(false);
        int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), bitsChave > 8192 ? 2 : 8);
        Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            threads[threadId] = new Thread(() -> {
                SecureRandom threadRnd = new SecureRandom();
                while (!pararThreads.get() && primosEncontrados.size() < 2) {
                    if (primosEncontrados.size() >= 2 || pararThreads.get()) {
                        break;
                    }
                    BigInteger candidato = BigInteger.probablePrime(tamanhoPrimos, threadRnd);
                    int candidatoBits = candidato.bitLength();
                    if (candidatoBits >= tamanhoPrimos - 1 && candidatoBits <= tamanhoPrimos + 1) {
                        if (primosEncontrados.size() < 2 && !pararThreads.get()) {
                            primosEncontrados.add(candidato);
                            if (primosEncontrados.size() >= 2) {
                                pararThreads.set(true);
                                break;
                            }
                        }
                    }
                }
            });
            threads[threadId].start();
        }
        long timeoutStartTime = System.currentTimeMillis();
        long timeout = Math.max(120000L, (long) tamanhoPrimos * 200L);
        while (primosEncontrados.size() < 2 && (System.currentTimeMillis() - timeoutStartTime) < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        pararThreads.set(true);
        for (Thread thread : threads) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                thread.interrupt();
            }
        }
        if (primosEncontrados.size() >= 2) {
            return new BigInteger[]{primosEncontrados.get(0), primosEncontrados.get(1)};
        }
        throw new RuntimeException("Timeout na geração de primos RSA");
    }

    /** Geração de chaves RSA (relatório com ou sem texto da mensagem). */
    private void iniciarGeracaoRSAChaves(final int bits, final boolean incluirMensagemNoRelatorio, final String textoMensagem) {
        Log.d(TAG, "Iniciando geração de chaves RSA de " + bits + " bits (incluir texto: " + incluirMensagemNoRelatorio + ")");
        CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
            resultadoView,
            "Gerando Chaves RSA",
            "Bits da chave: " + bits
        );
        new Thread(() -> {
            try {
                BigInteger[] pq = gerarPrimosPqRSA(bits);
                BigInteger p = pq[0];
                BigInteger q = pq[1];

                StringBuilder resultadoRSA = new StringBuilder();
                resultadoRSA.append("🔐 CRIPTOGRAFIA RSA — GERAÇÃO DE CHAVES\n");
                resultadoRSA.append("=====================================\n\n");

                if (incluirMensagemNoRelatorio) {
                    resultadoRSA.append("📝 TEXTO (contexto informado pelo usuário):\n");
                    resultadoRSA.append("   ").append(textoMensagem.isEmpty() ? "(vazio)" : textoMensagem).append("\n\n");
                } else {
                    resultadoRSA.append("📝 Texto da mensagem omitido do relatório (apenas parâmetros e chaves).\n\n");
                }

                resultadoRSA.append("📊 PARÂMETROS DA CHAVE:\n");
                resultadoRSA.append("   • Tamanho da chave: ").append(bits).append(" bits\n");
                resultadoRSA.append("   • Nível de segurança: ").append(getNivelSeguranca(bits)).append("\n");
                resultadoRSA.append("   • Tempo estimado para quebrar: ").append(getTempoQuebra(bits)).append("\n");

                BigInteger n = p.multiply(q);
                BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
                BigInteger e = BigInteger.valueOf(65537);
                BigInteger d = e.modInverse(phi);

                resultadoRSA.append("🔑 CHAVES RSA GERADAS:\n");
                resultadoRSA.append("   • Primo p: ").append(p).append(" (").append(p.toString().length()).append(" dígitos)\n");
                resultadoRSA.append("   • Primo q: ").append(q).append(" (").append(q.toString().length()).append(" dígitos)\n");
                resultadoRSA.append("   • n = p × q: ").append(n).append(" (").append(n.toString().length()).append(" dígitos)\n");
                resultadoRSA.append("   • φ(n) = (p-1)(q-1): ").append(phi).append(" (").append(phi.toString().length()).append(" dígitos)\n");
                resultadoRSA.append("   • Expoente público e: ").append(e).append("\n");
                resultadoRSA.append("   • Expoente privado d: ").append(d).append(" (").append(d.toString().length()).append(" dígitos)\n\n");

                resultadoRSA.append("🔐 CHAVES FINAIS:\n");
                resultadoRSA.append("   • Chave pública: (n=").append(n).append(", e=").append(e).append(")\n");
                resultadoRSA.append("   • Chave privada: (n=").append(n).append(", d=").append(d).append(")\n\n");

                resultadoRSA.append("✅ VERIFICAÇÃO MATEMÁTICA:\n");
                resultadoRSA.append("   • e × d mod φ(n) = ").append(e.multiply(d).mod(phi)).append(" ✓\n");
                resultadoRSA.append("   • p × q = ").append(p.multiply(q)).append(" ✓\n");
                resultadoRSA.append("   • n = ").append(n).append(" ✓\n\n");

                long fimMs = System.currentTimeMillis();
                resultadoRSA.append("⏱️ TEMPOS:\n");
                resultadoRSA.append("   • Tempo total do processo: ").append(formatarSegundosTempoCronometro(cronometroAtivo.inicioMs, fimMs)).append(" s\n\n");

                pararCronometroOperacao(cronometroAtivo);
                salvarResultadoTemporario(resultadoRSA.toString(), "seguranca_digital_rsa");
            } catch (Exception e) {
                pararCronometroOperacao(cronometroAtivo);
                Log.e(TAG, "Erro ao gerar chaves RSA", e);
                runOnUiThread(() -> resultadoView.setText("Erro ao gerar chaves RSA: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Aplica o tema atual (claro ou escuro)
     */
    private void aplicarTema() {
        aplicarFundoGradientePrincipal();
        aplicarEstiloAreaResultado();
        atualizarCoresSistema();
        atualizarBotaoTema();
    }

    /**
     * Alterna entre tema claro e escuro
     */
    private void alternarTema() {
        temaEscuro = !temaEscuro;
        preferences.edit().putBoolean("tema_escuro", temaEscuro).apply();
        aplicarTema();

        // Recriar menu para aplicar tema aos cards
        if (menuContainer != null) {
            menuContainer.removeAllViews();
            criarMenu();
        }

        // Aplicar tema aos containers existentes
        aplicarTemaAosContainers();
    }

    /**
     * Aplica o tema inicial quando a atividade é criada
     */
    private void aplicarTemaInicial() {
        aplicarTema();
        aplicarTemaAosContainers();
    }

    /**
     * Atualiza o botão de tema sem recriar a interface
     */
    private void atualizarBotaoTema() {
        if (btnTema != null) {
            btnTema.setText(temaEscuro ? "\u2600" : "\u263E");
            btnTema.setContentDescription(temaEscuro ? "Ativar tema claro" : "Ativar tema escuro");
            btnTema.setBackground(fundoArredondado(withAlpha(Color.WHITE, temaEscuro ? 24 : 46), 8, 1, withAlpha(Color.WHITE, 80)));
            return;
        }
        // Encontrar o botão de tema no header
        if (menuContainer != null && menuContainer.getParent() != null) {
            ViewGroup parent = (ViewGroup) menuContainer.getParent();
            if (parent instanceof LinearLayout) {
                LinearLayout mainLayout = (LinearLayout) parent;
                for (int i = 0; i < mainLayout.getChildCount(); i++) {
                    View child = mainLayout.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout headerLayout = (LinearLayout) child;
                        for (int j = 0; j < headerLayout.getChildCount(); j++) {
                            View headerChild = headerLayout.getChildAt(j);
                            if (headerChild instanceof Button) {
                                Button btn = (Button) headerChild;
                                if (btn.getText().toString().contains("☀️") || btn.getText().toString().contains("🌙")) {
                                    btn.setText(temaEscuro ? "☀️" : "🌙");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Aplica o tema aos containers existentes
     */
    private void aplicarTemaAosContainers() {
        int corTexto = corTextoPrincipal();
        int corCard = corSuperficie();

        // Aplicar tema ao menu container
        if (menuContainer != null) {
            menuContainer.setBackgroundColor(Color.TRANSPARENT);
            aplicarTemaRecursivo(menuContainer, corTexto, corCard);
        }

        // Aplicar tema ao content container
        if (contentContainer != null) {
            contentContainer.setBackgroundColor(Color.TRANSPARENT);
            aplicarTemaRecursivo(contentContainer, corTexto, corCard);
        }
        aplicarFundoGradientePrincipal();
        aplicarEstiloAreaResultado();
        atualizarCoresSistema();
        atualizarBotaoTema();
    }

    /**
     * Aplica tema recursivamente a todos os views
     */
    private void aplicarTemaRecursivo(ViewGroup parent, int corTexto, int corCard) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof Button) {
                Button btn = (Button) child;
                Object tag = btn.getTag();
                if ("ppf_action".equals(tag)
                        || "ppf_theme".equals(tag)
                        || "ppf_info".equals(tag)
                        || "ppf_prime_power".equals(tag)) {
                    btn.setTextColor(Color.WHITE);
                } else {
                    estilizarBotaoSecundario(btn);
                }
            } else if (child instanceof EditText) {
                EditText editText = (EditText) child;
                aplicarEstiloCampoEntradaTematizado(editText);
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                String texto = textView.getText().toString();
                Object tag = textView.getTag();

                // Manter cores especiais dos textos
                if ("ppf_tutorial".equals(tag) || texto.contains("WhatsApp")) {
                    // Manter cores originais dos textos especiais
                    if (texto.contains("WhatsApp")) {
                        textView.setTextColor(Color.parseColor("#25D366"));
                    }
                } else {
                    textView.setTextColor(corTexto);
                }
            } else if (child instanceof ScrollView) {
                ScrollView scrollView = (ScrollView) child;
                if (temaEscuro) {
                    scrollView.setBackgroundColor(Color.argb(40, 255, 255, 255));
                } else {
                    scrollView.setBackgroundColor(Color.argb(35, 255, 255, 255));
                }
                // Aplicar tema aos filhos do ScrollView
                for (int j = 0; j < scrollView.getChildCount(); j++) {
                    View scrollChild = scrollView.getChildAt(j);
                    if (scrollChild instanceof ViewGroup) {
                        aplicarTemaRecursivo((ViewGroup) scrollChild, corTexto, corCard);
                    }
                }
            } else if (child instanceof ViewGroup) {
                aplicarTemaRecursivo((ViewGroup) child, corTexto, corCard);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playBillingManager != null) {
            playBillingManager.refreshPurchases();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playBillingManager != null) {
            playBillingManager.endConnection();
            playBillingManager = null;
        }
        // Limpar arquivos temporários quando app for destruído
        liberarPrimeMatchAudio();
        limparArquivosTemporarios();
    }

    @Override
    protected void onPause() {
        paradaUsuario = true;
        interromperBuscaImpar.set(true);
        if (arcadeHost != null && primeShooterView != null) primeShooterView.pausar();
        if (arcadeHost != null && primeRunnerView != null) primeRunnerView.pausar();
        super.onPause();
    }

    private boolean voltarDentroDoApp() {
        paradaUsuario = true;
        if (arcadeHost != null) {
            rootMainLayout.removeView(arcadeHost);
            arcadeHost = null;
            primeRunnerView = null;
            primeShooterView = null;
            appHeader.setVisibility(View.VISIBLE);
            menuScrollView.setAlpha(1f);
            menuScrollView.setVisibility(View.VISIBLE);
            return true;
        }
        // Se estiver em uma tela de funcionalidade, voltar ao menu
        if (telaConteudoVisivel()) {
            appHeader.setVisibility(View.VISIBLE);
            // Limpar o container de conteúdo
            contentContainer.removeAllViews();

            // Voltar ao menu com animação
            ocultarTelaConteudo();
            if (menuScrollView != null) {
                menuScrollView.setVisibility(View.VISIBLE);
                menuScrollView.setAlpha(0.0f);
            }

            // Garantir que o menu seja recriado corretamente
            menuContainer.removeAllViews();
            criarMenu();

            // Adicionar animação suave
            if (contentScrollView != null) {
                contentScrollView.animate().alpha(0.0f).setDuration(300);
            } else {
                contentContainer.animate().alpha(0.0f).setDuration(300);
            }
            if (menuScrollView != null) {
                menuScrollView.animate().alpha(1.0f).setDuration(300);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pdfToSave = savedInstanceState.getString(STATE_PDF_TO_SAVE);
        }

        // Verificações de segurança
        if (!verificarSeguranca()) {
            return;
        }

        // Inicializar preferências e tema
        preferences = getSharedPreferences("PrimeProFast", MODE_PRIVATE);
        inicializarPrimeMatchAudio();

        inicializarSistemaMonetizacao();
        if (MONETIZACAO_ATIVA) {
            iniciarComprasPlay();
        }
        temaEscuro = preferences.getBoolean("tema_escuro", false);

        // Solicitar permissões de armazenamento
        requestStoragePermissions();

        inicializarLayoutPrincipal();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (voltarDentroDoApp()) return;
                setEnabled(false);
                try {
                    getOnBackPressedDispatcher().onBackPressed();
                } finally {
                    setEnabled(true);
                }
            }
        });
    }

    private void iniciarComprasPlay() {
        playBillingManager = new PlayBillingManager(this);
        playBillingManager.start(new PlayBillingManager.Listener() {
            @Override
            public void onPremiumStateChanged(boolean active) {
                runOnUiThread(() -> aplicarEstadoPremium(active));
            }

            @Override
            public void onPurchaseCompletedSuccessfully() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Compra concluída. Premium ativo.", Toast.LENGTH_LONG).show();
                    getOnBackPressedDispatcher().onBackPressed();
                    mostrarEstatisticasUso();
                });
            }

            @Override
            public void onPurchaseFlowError(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onMegaPrimeDeliveryPurchaseCompleted(Purchase purchase) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Compra registrada. Envie o pedido ao desenvolvedor pelo e-mail.", Toast.LENGTH_LONG).show();
                    abrirEmailPedidoEntregaMega(purchase);
                });
            }
        });
    }

    private void inicializarLayoutPrincipal() {
        // Layout principal
        rootMainLayout = new LinearLayout(this);
        rootMainLayout.setOrientation(LinearLayout.VERTICAL);
        rootMainLayout.setPadding(dpUi(10), dpUi(10), dpUi(10), dpUi(10));

        // Cabeçalho com título (barra “vidro” / gradiente)
        LinearLayout headerLayout = new LinearLayout(this);
        appHeader = headerLayout;
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable headerBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{BRAND_PRIMARY, Color.rgb(38, 98, 118), BRAND_SECONDARY}
        );
        headerBg.setCornerRadius(dpUi(8));
        headerLayout.setBackground(headerBg);
        headerLayout.setPadding(dpUi(14), dpUi(12), dpUi(10), dpUi(12));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLp.setMargins(0, 0, 0, dpUi(10));
        headerLayout.setLayoutParams(headerLp);
        headerLayout.setElevation(dpUi(3));

        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleStackLp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        titleStack.setLayoutParams(titleStackLp);

        TextView titulo = new TextView(this);
        titulo.setText("PrimeProFast");
        titulo.setTextSize(21);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.START);
        titulo.setLetterSpacing(0.01f);

        TextView subtitulo = new TextView(this);
        subtitulo.setText("Primos, RSA e análise matemática");
        subtitulo.setTextSize(12);
        subtitulo.setTextColor(withAlpha(Color.WHITE, 215));
        subtitulo.setPadding(0, dpUi(2), 0, 0);

        titleStack.addView(titulo);
        titleStack.addView(subtitulo);

        btnTema = new Button(this);
        btnTema.setTag("ppf_theme");
        btnTema.setAllCaps(false);
        btnTema.setTextColor(Color.WHITE);
        btnTema.setTextSize(19);
        btnTema.setMinWidth(dpUi(44));
        btnTema.setMinHeight(dpUi(44));
        btnTema.setPadding(0, 0, 0, 0);
        btnTema.setOnClickListener(v -> alternarTema());
        LinearLayout.LayoutParams temaLp = new LinearLayout.LayoutParams(dpUi(44), dpUi(44));
        temaLp.setMargins(dpUi(12), 0, 0, 0);
        btnTema.setLayoutParams(temaLp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnTema.setTooltipText("Alternar tema");
        }

        headerLayout.addView(titleStack);
        headerLayout.addView(btnTema);

        rootMainLayout.addView(headerLayout);

        // Container do menu (dentro de um ScrollView para permitir rolagem vertical de todos os cards)
        menuScrollView = new ScrollView(this);
        menuScrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));
        menuScrollView.setFillViewport(true);

        menuContainer = new LinearLayout(this);
        menuContainer.setOrientation(LinearLayout.VERTICAL);
        menuContainer.setPadding(0, dpUi(2), 0, dpUi(10));
        menuContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        menuScrollView.addView(menuContainer);
        rootMainLayout.addView(menuScrollView);

        // Container rolável do conteúdo (inicialmente oculto)
        contentScrollView = new ScrollView(this);
        contentScrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));
        contentScrollView.setFillViewport(false);
        contentScrollView.setVisibility(View.GONE);

        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setVisibility(View.GONE);
        contentContainer.setPadding(0, dpUi(2), 0, 0);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentScrollView.addView(contentContainer);
        rootMainLayout.addView(contentScrollView);

        // Área de resultado
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        resultadoView = new TextView(this);
        resultadoView.setPadding(16, 16, 16, 16);
        resultadoView.setText("Selecione uma função no menu acima...");
        resultadoView.setTextIsSelectable(true);
        resultadoView.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#333333"));
        aplicarEstiloAreaResultado();

        scrollView.addView(resultadoView);
        contentContainer.addView(scrollView);

        setContentView(rootMainLayout);
        aplicarMargensDoSistema(rootMainLayout);
        aplicarFundoGradientePrincipal();

        // Criar menu com cards
        criarMenu();

        // Aplicar tema inicial se necessário
        aplicarTemaInicial();
    }

    private void criarMenu() {
        // Garantir orientação vertical para empilhar os cards como na apresentação desejada
        menuContainer.setOrientation(LinearLayout.VERTICAL);

        // Ordem e descrições conforme apresentação vertical
        View card1 = criarCard("Primos por Intervalo", "Encontre primos em um intervalo específico.", false);
        card1.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card1);

        View card2 = criarCard("Primos Especiais", "Explore classes específicas de primos famosos.", false);
        card2.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card2);

        View card3 = criarCard("Primos Aleatórios", "Gere primos aleatórios de vários tamanhos.", false);
        card3.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card3);

        View card4 = criarCard("Conjectura de Legendre", "Teste a conjectura de Legendre com diferentes intervalos.", false);
        card4.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card4);

        View card5 = criarCard("Números de Mersenne", "Encontre e analise números de Mersenne.", false);
        card5.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card5);

        View card6 = criarCard("Números Perfeitos", "Calcule e explore números perfeitos.", false);
        card6.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card6);

        View card7 = criarCard(
            "Segurança Digital",
            "Gerar chaves RSA\nGerar criptografia e descriptografia\nGerar hash criptográfico\nAssinatura digital",
            false
        );
        card7.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card7);

        View card8 = criarCard("Teste de Primalidade", "Análise completa de primalidade e fatoração.", false);
        card8.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card8);

        View card9 = criarCard("Estatísticas", "Análise estatística completa de números primos.", false);
        card9.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card9);

        View card10 = criarCard("Jogo dos Primos", "Combine bolinhas e forme somas primas para avançar.", false);
        card10.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card10);

        View card11 = criarCard("Corrida Prima", "Desvie dos compostos e acerte os primos na estrada.", false);
        card11.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card11);

        View card12 = criarCard("Resgate Espacial", "Atire em compostos e resgate primos no espaco.", false);
        card12.setLayoutParams(layoutCardMenuParams());
        menuContainer.addView(card12);

        // Espaçador que empurra o rodapé para o fim
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        spacer.setLayoutParams(spacerParams);
        menuContainer.addView(spacer);   // insere o "vazio"

        // Rodapé com botão de informações - posicionado no final
        LinearLayout rodapeContainer = new LinearLayout(this);
        rodapeContainer.setOrientation(LinearLayout.VERTICAL);
        rodapeContainer.setGravity(android.view.Gravity.CENTER);
        rodapeContainer.setPadding(16, 20, 16, 16);

        // Botão de informações no rodapé.
        Button btnInfo = new Button(this);
        btnInfo.setText("Informações");
        btnInfo.setTag("ppf_info");
        btnInfo.setTextSize(15);
        btnInfo.setTypeface(null, Typeface.BOLD);
        btnInfo.setTextColor(Color.WHITE);
        btnInfo.setAllCaps(false);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setCornerRadius(dpUi(8));
        infoBg.setColor(BRAND_PRIMARY);
        btnInfo.setBackground(infoBg);
        btnInfo.setPadding(dpUi(20), dpUi(14), dpUi(20), dpUi(14));
        btnInfo.setElevation(dpUi(2));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoLp.setMargins(dpUi(8), dpUi(4), dpUi(8), 0);
        btnInfo.setLayoutParams(infoLp);
        btnInfo.setOnClickListener(v -> mostrarInformacoesApp());
        rodapeContainer.addView(btnInfo);

        menuContainer.addView(rodapeContainer);
    }

    /**
     * Card do menu: acento visual discreto, texto principal e atalho de tutorial.
     */
    private View criarCard(String titulo, String descricao, boolean ativo) {
        int[] design = obterDesignUnico(titulo);
        int corPrimaria = design[0];
        int corSecundaria = design[1];
        String emoji = obterEmojiUnico(titulo);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setMinimumHeight(dpUi(82));
        row.setPadding(dpUi(10), dpUi(8), dpUi(8), dpUi(8));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dpUi(8));
        if (ativo) {
            cardBg.setColor(corPrimaria);
            cardBg.setStroke(dpUi(1), corSecundaria);
        } else if (temaEscuro) {
            cardBg.setColor(corSuperficie());
            cardBg.setStroke(dpUi(1), withAlpha(corPrimaria, 150));
        } else {
            cardBg.setColor(corSuperficie());
            cardBg.setStroke(dpUi(1), corBorda());
        }
        row.setBackground(cardBg);
        row.setElevation(dpUi(2));

        // Chip esquerdo: cor de acento + ícone.
        LinearLayout leftPanel = new LinearLayout(this);
        leftPanel.setOrientation(LinearLayout.VERTICAL);
        leftPanel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(dpUi(54), dpUi(54));
        leftLp.setMargins(0, 0, dpUi(10), 0);
        leftPanel.setLayoutParams(leftLp);

        GradientDrawable leftBg = new GradientDrawable();
        leftBg.setCornerRadius(dpUi(8));
        leftBg.setColor(withAlpha(corPrimaria, temaEscuro ? 72 : 28));
        leftBg.setStroke(dpUi(1), withAlpha(corPrimaria, temaEscuro ? 130 : 86));
        leftPanel.setBackground(leftBg);

        TextView icon = new TextView(this);
        icon.setText(emoji);
        icon.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 27);
        icon.setGravity(Gravity.CENTER);
        icon.setIncludeFontPadding(false);
        leftPanel.addView(icon, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Centro: título + descrição
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        centerLp.setMargins(0, 0, dpUi(8), 0);
        center.setLayoutParams(centerLp);

        TextView tituloView = new TextView(this);
        tituloView.setText(titulo);
        tituloView.setTextSize(15);
        tituloView.setTypeface(null, Typeface.BOLD);
        tituloView.setMaxLines(3);
        if (ativo) {
            tituloView.setTextColor(Color.WHITE);
        } else {
            tituloView.setTextColor(corTextoPrincipal());
        }

        TextView descView = new TextView(this);
        descView.setText(descricao);
        descView.setTextSize(12);
        descView.setLineSpacing(dpUi(2), 1f);
        descView.setMaxLines(4);
        if (ativo) {
            descView.setTextColor(Color.argb(230, 255, 255, 255));
        } else {
            descView.setTextColor(corTextoSecundario());
        }

        center.addView(tituloView);
        center.addView(descView);

        // Tutorial à direita.
        TextView btnTutorial = new TextView(this);
        btnTutorial.setText("i");
        btnTutorial.setTag("ppf_tutorial");
        btnTutorial.setTextSize(17);
        btnTutorial.setTypeface(null, Typeface.BOLD);
        btnTutorial.setTextColor(temaEscuro ? Color.WHITE : corPrimaria);
        btnTutorial.setGravity(Gravity.CENTER);
        btnTutorial.setContentDescription("Abrir informações de " + titulo);
        GradientDrawable tutBg = new GradientDrawable();
        tutBg.setCornerRadius(dpUi(8));
        tutBg.setColor(withAlpha(corPrimaria, temaEscuro ? 68 : 18));
        tutBg.setStroke(dpUi(1), withAlpha(corPrimaria, temaEscuro ? 150 : 110));
        btnTutorial.setBackground(tutBg);
        btnTutorial.setClickable(true);
        btnTutorial.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnTutorial.setTooltipText("Informações");
        }
        LinearLayout.LayoutParams tutLp = new LinearLayout.LayoutParams(
            dpUi(40),
            dpUi(40)
        );
        tutLp.setMargins(0, 0, 0, 0);
        btnTutorial.setLayoutParams(tutLp);
        btnTutorial.setOnClickListener(v -> abrirTutorial(titulo));

        row.addView(leftPanel);
        row.addView(center);
        row.addView(btnTutorial);

        row.setOnClickListener(v -> abrirFuncao(titulo));

        return row;
    }

    /**
     * Abre um único guia por card (explicação do que faz, como usar e contexto pedagógico acessível).
     */
    private void abrirTutorial(String funcionalidade) {
        Log.d(TAG, "abrirTutorial: " + funcionalidade);
        runOnUiThread(() -> {
            try {
                String[][] secoes = gerarConteudoTutorialUnificado(funcionalidade);
                String tutorial = gerarTutorialHTMLNivel(funcionalidade, secoes);
                salvarResultadoTemporario(tutorial, "tutorial_" + funcionalidade.replace(" ", "_").toLowerCase(Locale.ROOT));
            } catch (Exception e) {
                Log.e(TAG, "Erro ao abrir tutorial", e);
                Toast.makeText(MainActivity.this, "Erro ao abrir tutorial: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Um único guia por card: o que a função faz no app, como usar, e contexto pedagógico acessível.
     * Evita jargão de implementação e opções de “nível”.
     */
    private String[][] gerarConteudoTutorialUnificado(String funcionalidade) throws IOException, JSONException {
        try (java.io.InputStream stream = getAssets().open("primeprofast-tutorials.json");
             java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int count; (count = stream.read(buffer)) != -1;) {
                data.write(buffer, 0, count);
            }
            JSONArray sections = new JSONObject(new String(data.toByteArray(), StandardCharsets.UTF_8))
                .getJSONArray(funcionalidade);
            String[][] result = new String[sections.length()][2];
            for (int index = 0; index < result.length; index++) {
                JSONArray section = sections.getJSONArray(index);
                result[index][0] = section.getString(0);
                result[index][1] = section.getString(1);
            }
            return result;
        }
    }

    /**
     * Retorna design único para cada funcionalidade
     * [corPrimaria, corSecundaria]
     */
    private int[] obterDesignUnico(String titulo) {
        switch (titulo) {
            case "Primos por Intervalo":
                return new int[]{0xFF1D4E89, 0xFF2F6FA3};
            case "Números de Mersenne":
                return new int[]{0xFF6A5A8C, 0xFF8B78AC};
            case "Números Perfeitos":
                return new int[]{0xFF2E7D6F, 0xFF4F9C8E};
            case "Segurança Digital":
                return new int[]{0xFFC45A3B, 0xFFD98262};
            case "Teste de Primalidade":
                return new int[]{0xFF2F6F9F, 0xFF5F94BD};
            case "Primos Especiais":
                return new int[]{0xFF8A5A83, 0xFFA979A1};
            case "Primos Aleatórios":
                return new int[]{0xFF5C677D, 0xFF7F8AA0};
            case "Conjectura de Legendre":
                return new int[]{0xFFB66B45, 0xFFD0996D};
            case "Estatísticas":
                return new int[]{0xFF357C73, 0xFF5AA69A};
            case "Jogo dos Primos":
                return new int[]{0xFF2F855A, 0xFF68A06D};
            case "Corrida Prima":
                return new int[]{0xFF256D85, 0xFF5EA7B8};
            case "Resgate Espacial":
                return new int[]{0xFF30336B, 0xFF7B61C8};
            default:
                return new int[]{0xFF6B7280, 0xFF9CA3AF};
        }
    }

    /**
     * Retorna emoji único para cada funcionalidade
     */
    private String obterEmojiUnico(String titulo) {
        switch (titulo) {
            case "Primos por Intervalo":
                return "🔢";
            case "Números de Mersenne":
                return "⚡";
            case "Números Perfeitos":
                return "✨";
            case "Segurança Digital":
                return "🔐";
            case "Teste de Primalidade":
                return "🔍";
            case "Primos Especiais":
                return "⭐";
            case "Primos Aleatórios":
                return "🎲";
            case "Conjectura de Legendre":
                return "🧮";
            case "Estatísticas":
                return "📊";
            case "Jogo dos Primos":
                return "🎯";
            default:
                return "📱";
        }
    }

    /**
     * Gera tutorial completo para Teste de Primalidade
     */
    private String gerarTutorialTestePrimalidade() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='pt-BR'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>🎓 Aula Completa: Teste de Primalidade</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #333; }\n");
        html.append("        .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 20px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); overflow: hidden; }\n");
        html.append("        .header { background: linear-gradient(135deg, #2196F3, #64B5F6); color: white; padding: 40px; text-align: center; }\n");
        html.append("        .header h1 { margin: 0; font-size: 2.5em; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }\n");
        html.append("        .header p { margin: 10px 0 0 0; font-size: 1.2em; opacity: 0.9; }\n");
        html.append("        .content { padding: 40px; }\n");
        html.append("        .chapter { margin-bottom: 50px; padding: 30px; border-radius: 15px; background: #f8f9fa; border-left: 5px solid #2196F3; }\n");
        html.append("        .chapter h2 { color: #2196F3; margin-top: 0; font-size: 1.8em; }\n");
        html.append("        .chapter h3 { color: #1976D2; margin-top: 25px; font-size: 1.4em; }\n");
        html.append("        .example { background: #e3f2fd; padding: 20px; border-radius: 10px; margin: 15px 0; border-left: 4px solid #2196F3; }\n");
        html.append("        .formula { background: #f5f5f5; padding: 15px; border-radius: 8px; font-family: 'Courier New', monospace; font-size: 1.1em; margin: 15px 0; text-align: center; }\n");
        html.append("        .highlight { background: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #ffc107; margin: 15px 0; }\n");
        html.append("        .interactive { background: #d4edda; padding: 20px; border-radius: 10px; border-left: 4px solid #28a745; margin: 15px 0; }\n");
        html.append("        .navigation { background: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0; text-align: center; }\n");
        html.append("        .nav-btn { display: inline-block; margin: 0 10px; padding: 10px 20px; background: #2196F3; color: white; text-decoration: none; border-radius: 25px; transition: all 0.3s; }\n");
        html.append("        .nav-btn:hover { background: #1976D2; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(33,150,243,0.4); }\n");
        html.append("        .progress { background: #e9ecef; height: 10px; border-radius: 5px; margin: 20px 0; overflow: hidden; }\n");
        html.append("        .progress-bar { background: linear-gradient(90deg, #2196F3, #64B5F6); height: 100%; width: 0%; transition: width 0.5s; }\n");
        html.append("        .emoji { font-size: 1.5em; margin-right: 10px; }\n");
        html.append("        .success { color: #28a745; font-weight: bold; }\n");
        html.append("        .error { color: #dc3545; font-weight: bold; }\n");
        html.append("        .info { color: #17a2b8; font-weight: bold; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='container'>\n");
        html.append("        <div class='header'>\n");
        html.append("            <h1>🔍 UNIVERSO DOS NÚMEROS PRIMOS</h1>\n");
        html.append("            <p>Aula Completa e Interativa sobre Teste de Primalidade</p>\n");
        html.append("            <div class='progress'>\n");
        html.append("                <div class='progress-bar' id='progressBar'></div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("        <div class='content'>\n");
        html.append("            <div class='navigation'>\n");
        html.append("                <a href='#cap1' class='nav-btn'>🌌 Capítulo 1</a>\n");
        html.append("                <a href='#cap2' class='nav-btn'>📊 Capítulo 2</a>\n");
        html.append("                <a href='#cap3' class='nav-btn'>🧮 Capítulo 3</a>\n");
        html.append("                <a href='#cap4' class='nav-btn'>💡 Capítulo 4</a>\n");
        html.append("                <a href='#cap5' class='nav-btn'>🎯 Capítulo 5</a>\n");
        html.append("            </div>\n");
        html.append("            <div id='cap1' class='chapter'>\n");
        html.append("                <h2>🌌 CAPÍTULO 1: O QUE SÃO OS NÚMEROS PRIMOS?</h2>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>📖 Definição Matemática</h3>\n");
        html.append("                    <p>Um <strong>número primo</strong> é um número natural maior que 1 que possui <strong>exatamente dois divisores</strong>: 1 e ele mesmo.</p>\n");
        html.append("                </div>\n");
        html.append("                <div class='example'>\n");
        html.append("                    <h3>🔬 Propriedades Fundamentais</h3>\n");
        html.append("                    <ul>\n");
        html.append("                        <li><strong>p > 1</strong> (não é 1 nem 0)</li>\n");
        html.append("                        <li><strong>Divisores</strong>: apenas 1 e p</li>\n");
        html.append("                        <li><strong>Fatoração única</strong>: todo número > 1 é primo ou produto de primos</li>\n");
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("                <div class='interactive'>\n");
        html.append("                    <h3> Exemplos Visuais Interativos</h3>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        <span class='success'>2: ✓ PRIMO</span> (divisores: 1, 2)<br>\n");
        html.append("                        <span class='success'>3: ✓ PRIMO</span> (divisores: 1, 3)<br>\n");
        html.append("                        <span class='error'>4: ✗ COMPOSTO</span> (divisores: 1, 2, 4)<br>\n");
        html.append("                        <span class='success'>5: ✓ PRIMO</span> (divisores: 1, 5)<br>\n");
        html.append("                        <span class='error'>6: ✗ COMPOSTO</span> (divisores: 1, 2, 3, 6)\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>🧮 Teorema Fundamental da Aritmética</h3>\n");
        html.append("                    <p>Todo número natural > 1 pode ser escrito como <strong>produto único</strong> de números primos (fatoração única).</p>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        <strong>Exemplo:</strong> 100 = 2² × 5² = 2 × 2 × 5 × 5\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div id='cap2' class='chapter'>\n");
        html.append("                <h2>📊 CAPÍTULO 2: COMO OS PRIMOS SE DISTRIBUEM?</h2>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>📈 Padrões Fascinantes</h3>\n");
        html.append("                    <ul>\n");
        html.append("                        <li><strong>Infinitos</strong> (Euclides, 300 a.C.)</li>\n");
        html.append("                        <li><strong>Distribuição irregular</strong> e imprevisível</li>\n");
        html.append("                        <li><strong>Tendência de rarefação</strong> conforme crescem</li>\n");
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("                <div class='example'>\n");
        html.append("                    <h3>🔍 Teorema dos Números Primos</h3>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        π(x) ≈ x/ln(x)\n");
        html.append("                    </div>\n");
        html.append("                    <p>Onde <strong>π(x)</strong> é o número de primos até x</p>\n");
        html.append("                </div>\n");
        html.append("                <div class='interactive'>\n");
        html.append("                    <h3>📊 Demonstração Visual</h3>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        π(10) = 4 primos: 2, 3, 5, 7<br>\n");
        html.append("                        π(100) = 25 primos<br>\n");
        html.append("                        π(1000) = 168 primos<br>\n");
        html.append("                        π(10000) = 1.229 primos\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>🧮 Integral Logarítmica (Li(x))</h3>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        π(x) ≈ Li(x) = ∫₂ˣ (1/ln(t)) dt\n");
        html.append("                    </div>\n");
        html.append("                    <p><strong>Por que Li(x) é melhor?</strong></p>\n");
        html.append("                    <ul>\n");
        html.append("                        <li><strong>x/ln(x)</strong>: aproximação de primeira ordem</li>\n");
        html.append("                        <li><strong>Li(x)</strong>: aproximação de ordem superior</li>\n");
        html.append("                        <li><strong>Muito mais precisa</strong> para números grandes</li>\n");
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("                <div class='example'>\n");
        html.append("                    <h3>📊 Comparação Real</h3>\n");
        html.append("                    <p>Para x = 1.000.000:</p>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        <span class='info'>Real:</span> 78.498 primos<br>\n");
        html.append("                        <span class='error'>x/ln(x):</span> 72.382 (erro: 7.8%)<br>\n");
        html.append("                        <span class='success'>Li(x):</span> 78.628 (erro: 0.2%)\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div id='cap3' class='chapter'>\n");
        html.append("                <h2>🧮 CAPÍTULO 3: NÚMEROS PRIMOS ESPECIAIS</h2>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>⚡ NÚMEROS DE MERSENNE</h3>\n");
        html.append("                    <div class='formula'>\n");
        html.append("                        M_p = 2^p - 1, onde p é primo\n");
        html.append("                    </div>\n");
        html.append("                    <div class='example'>\n");
        html.append("                        <h4>📖 História</h4>\n");
        html.append("                        <ul>\n");
        html.append("                            <li>Marin Mersenne (1588-1648) estudou esses números</li>\n");
        html.append("                            <li>Conectados aos números perfeitos de Euclides</li>\n");
        html.append("                            <li>Apenas 51 números de Mersenne são conhecidos</li>\n");
        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                    <div class='interactive'>\n");
        html.append("                        <h4> Exemplos</h4>\n");
        html.append("                        <div class='formula'>\n");
        html.append("                            M_2 = 2² - 1 = 4 - 1 = 3 <span class='success'>✓ (primo)</span><br>\n");
        html.append("                            M_3 = 2³ - 1 = 8 - 1 = 7 <span class='success'>✓ (primo)</span><br>\n");
        html.append("                            M_5 = 2⁵ - 1 = 32 - 1 = 31 <span class='success'>✓ (primo)</span><br>\n");
        html.append("                            M_7 = 2⁷ - 1 = 128 - 1 = 127 <span class='success'>✓ (primo)</span>\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>✨ NÚMEROS PERFEITOS</h3>\n");
        html.append("                    <p>Um número é <strong>perfeito</strong> se é igual à soma de seus divisores próprios.</p>\n");
        html.append("                    <div class='example'>\n");
        html.append("                        <h4>🧮 Teorema de Euclides</h4>\n");
        html.append("                        <p>Se 2^p - 1 é primo, então 2^(p-1) × (2^p - 1) é perfeito.</p>\n");
        html.append("                    </div>\n");
        html.append("                    <div class='interactive'>\n");
        html.append("                        <h4>📊 Exemplos</h4>\n");
        html.append("                        <div class='formula'>\n");
        html.append("                            6 = 1 + 2 + 3<br>\n");
        html.append("                            28 = 1 + 2 + 4 + 7 + 14<br>\n");
        html.append("                            496 = 1 + 2 + 4 + 8 + 16 + 31 + 62 + 124 + 248\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div id='cap4' class='chapter'>\n");
        html.append("                <h2>💡 CAPÍTULO 4: APLICAÇÕES PRÁTICAS</h2>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>🔐 CRIPTOGRAFIA RSA</h3>\n");
        html.append("                    <p>O algoritmo RSA é baseado na <strong>dificuldade de fatorar</strong> números grandes.</p>\n");
        html.append("                    <div class='example'>\n");
        html.append("                        <h4>🔑 Componentes</h4>\n");
        html.append("                        <ul>\n");
        html.append("                            <li><strong>Chave pública</strong>: (n, e)</li>\n");
        html.append("                            <li><strong>Chave privada</strong>: d</li>\n");
        html.append("                            <li><strong>n = p × q</strong> (produto de dois primos grandes)</li>\n");
                        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                    <div class='interactive'>\n");
        html.append("                        <h4>🧮 Exemplo Prático</h4>\n");
        html.append("                        <div class='formula'>\n");
        html.append("                            p = 61, q = 53<br>\n");
        html.append("                            n = 61 × 53 = 3.233<br>\n");
        html.append("                            e = 17<br>\n");
        html.append("                            d = 2.753\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div id='cap5' class='chapter'>\n");
        html.append("                <h2>🎯 CAPÍTULO 5: DESAFIOS E CONJECTURAS</h2>\n");
        html.append("                <div class='highlight'>\n");
        html.append("                    <h3>🧩 CONJECTURAS FAMOSAS</h3>\n");
        html.append("                    <div class='example'>\n");
        html.append("                        <h4>1. Conjectura de Goldbach</h4>\n");
        html.append("                        <p>Todo número par > 2 é soma de dois primos</p>\n");
        html.append("                        <div class='formula'>\n");
        html.append("                            4 = 2 + 2 <span class='success'>✓</span><br>\n");
        html.append("                            6 = 3 + 3 <span class='success'>✓</span><br>\n");
        html.append("                            8 = 3 + 5 <span class='success'>✓</span><br>\n");
        html.append("                            10 = 3 + 7 = 5 + 5 <span class='success'>✓</span>\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class='example'>\n");
        html.append("                        <h4>2. 🔴 Primos Gêmeos</h4>\n");
        html.append("                        <p>Infinitos pares de primos com diferença 2</p>\n");
        html.append("                        <div class='formula'>\n");
        html.append("                            (3, 5), (5, 7), (11, 13), (17, 19)...\n");
        html.append("                        </div>\n");
        html.append("                    </div>\n");
        html.append("                    <div class='highlight'>\n");
        html.append("                        <h4>3. 🔴 Hipótese de Riemann</h4>\n");
        html.append("                        <p>Conecta zeros da função zeta com distribuição dos primos</p>\n");
        html.append("                        <ul>\n");
        html.append("                            <li>Um dos 7 Problemas do Milênio</li>\n");
        html.append("                            <li>Prêmio: $1.000.000</li>\n");
        html.append("                            <li>Consequências profundas na teoria dos números</li>\n");
        html.append("                        </ul>\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("                <div class='interactive'>\n");
        html.append("                    <h3>🎯 DESAFIOS INTERATIVOS</h3>\n");
        html.append("                    <ul>\n");
        html.append("                        <li>Tente encontrar padrões nos primos</li>\n");
        html.append("                        <li>Teste conjecturas com números grandes</li>\n");
        html.append("                        <li>Explore a distribuição visualmente</li>\n");
        html.append("                    </ul>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("            <div class='navigation'>\n");
        html.append("                <h3>🚀 PRÓXIMOS PASSOS</h3>\n");
        html.append("                <p>Agora que você entendeu a teoria, experimente as funcionalidades práticas!</p>\n");
        html.append("                <a href='#cap1' class='nav-btn'>🔍 Voltar ao Início</a>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("    <script>\n");
        html.append("        // Barra de progresso\n");
        html.append("        window.addEventListener('scroll', function() {\n");
        html.append("            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;\n");
        html.append("            const docHeight = document.documentElement.scrollHeight - window.innerHeight;\n");
        html.append("            const scrollPercent = (scrollTop / docHeight) * 100;\n");
        html.append("            document.getElementById('progressBar').style.width = scrollPercent + '%';\n");
        html.append("        });\n");
        html.append("        \n");
        html.append("        // Navegação suave\n");
        html.append("        document.querySelectorAll('a[href^=\"#\"]').forEach(anchor => {\n");
        html.append("            anchor.addEventListener('click', function (e) {\n");
        html.append("                e.preventDefault();\n");
        html.append("                const target = document.querySelector(this.getAttribute('href'));\n");
        html.append("                target.scrollIntoView({ behavior: 'smooth' });\n");
        html.append("            });\n");
        html.append("        });\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Gera tutorial genérico para funcionalidades não implementadas
     */
    private String gerarTutorialGenerico(String funcionalidade) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='pt-BR'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <title>🎓 Tutorial: ").append(funcionalidade).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 20px; background: #f0f0f0; }\n");
        html.append("        .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 15px; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }\n");
        html.append("        h1 { color: #2196F3; text-align: center; }\n");
        html.append("        .info { background: #e3f2fd; padding: 20px; border-radius: 10px; border-left: 4px solid #2196F3; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='container'>\n");
        html.append("        <h1>🎓 ").append(funcionalidade).append("</h1>\n");
        html.append("        <div class='info'>\n");
        html.append("            <h2>📚 Tutorial em Desenvolvimento</h2>\n");
        html.append("            <p>O tutorial completo para <strong>").append(funcionalidade).append("</strong> está sendo desenvolvido.</p>\n");
        html.append("            <p>Em breve você terá acesso a uma aula completa e interativa sobre esta funcionalidade!</p>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    // Métodos stub para outros tutoriais (serão implementados posteriormente)
    private String gerarTutorialMersenne() {
        return gerarTutorialHTML("Números de Mersenne", new String[][]{
            {"🔍 Introdução", "Os números de Mersenne são da forma M(n) = 2ⁿ - 1, onde n é um número primo. Marin Mersenne (1588-1648) foi um padre francês que estudou esses números especiais."},
            {"📊 Propriedades", "• M(n) = 2ⁿ - 1, onde n é primo<br>• Se M(n) é primo, então n deve ser primo<br>• Nem todo primo n gera um M(n) primo<br>• Os maiores primos conhecidos são de Mersenne"},
            {"🔬 Teorema de método proprietário", "Para testar se M(p) é primo, onde p é um primo ímpar:<br>• S₁ = 4<br>• Sₖ₊₁ = (Sₖ² - 2) mod M(p)<br>• M(p) é primo se e somente se Sₚ₋₁ = 0"},
            {"🌍 História", "• 1588-1648: Marin Mersenne<br>• 1644: Primeira lista de primos de Mersenne<br>• 1876: Édouard Lucas descobre M(127)<br>• 1952: Primeiro computador testa M(521)<br>• 2018: M(82,589,933) - 24.862.048 dígitos!"},
            {"💻 Aplicações", "• Criptografia RSA<br>• Geração de números aleatórios<br>• Testes de hardware<br>• Busca por novos primos<br>• Desafios matemáticos"},
            {"🎯 Desafios", "• Encontrar novos primos de Mersenne<br>• Provar que há infinitos primos de Mersenne<br>• Melhorar algoritmos de teste<br>• Distribuição dos primos de Mersenne"}
        });
    }
    private String gerarTutorialNumerosPerfeitos() {
        return gerarTutorialHTML("Números Perfeitos", new String[][]{
            {"🔍 Definição", "Um número perfeito é um número natural igual à soma de seus divisores próprios (excluindo o próprio número). Exemplo: 6 = 1 + 2 + 3"},
            {"📊 Teorema de Euclides", "Se 2ᵖ - 1 é primo (primo de Mersenne), então 2ᵖ⁻¹(2ᵖ - 1) é um número perfeito par. Esta é a única forma conhecida de gerar números perfeitos pares."},
            {"🔬 Propriedades", "• Todos os números perfeitos conhecidos são pares<br>• Se existe um número perfeito ímpar, deve ser > 10¹⁵⁰⁰<br>• Números perfeitos são triangulares e hexagonais<br>• A soma dos dígitos de um perfeito par é sempre 1"},
            {"🌍 História", "• 300 a.C.: Euclides prova a fórmula<br>• 100 d.C.: Nicômaco lista os primeiros 4<br>• 1456: Descoberto o 5º (33.550.336)<br>• 1588: Pietro Cataldi encontra o 6º e 7º<br>• 1772: Euler prova a fórmula de Euclides"},
            {"💻 Números Conhecidos", "• 6 = 1 + 2 + 3<br>• 28 = 1 + 2 + 4 + 7 + 14<br>• 496 = 1 + 2 + 4 + 8 + 16 + 31 + 62 + 124 + 248<br>• 8128 = 1 + 2 + 4 + 8 + 16 + 32 + 64 + 127 + 254 + 508 + 1016 + 2032 + 4064"},
            {"🎯 Problemas em Aberto", "• Existem infinitos números perfeitos?<br>• Existe algum número perfeito ímpar?<br>• Há números perfeitos que não são de Euclides?<br>• Distribuição dos números perfeitos"}
        });
    }
    private String gerarTutorialSegurancaDigital() {
        return gerarTutorialHTML("Segurança Digital", new String[][]{
            {"🔐 Criptografia RSA", "RSA é um sistema de criptografia assimétrica baseado na dificuldade de fatorar o produto de dois primos grandes. A segurança depende da intratabilidade da fatoração de números compostos."},
            {"📊 Geração de Chaves", "• Escolher dois primos grandes p e q<br>• Calcular n = p × q<br>• Calcular φ(n) = (p-1)(q-1)<br>• Escolher e tal que mdc(e, φ(n)) = 1<br>• Calcular d = e⁻¹ mod φ(n)"},
            {"🔬 Criptografia/Descriptografia", "• Criptografia: c = mᵉ mod n<br>• Descriptografia: m = cᵈ mod n<br>• Onde m é a mensagem, c é o texto cifrado<br>• Chave pública: (e, n), Chave privada: (d, n)"},
            {"🌍 História", "• 1977: Rivest, Shamir e Adleman criam RSA<br>• 1978: Primeiro artigo publicado<br>• 1983: RSA Security fundada<br>• 2000: RSA se torna padrão internacional<br>• 2020: RSA-2048 ainda considerado seguro"},
            {"💻 Aplicações", "• HTTPS/SSL para web segura<br>• Assinatura digital de documentos<br>• Criptografia de emails<br>• VPNs e redes seguras<br>• Blockchain e criptomoedas"},
            {"🎯 Segurança", "• Tamanho mínimo recomendado: 2048 bits<br>• RSA-4096 para máxima segurança<br>• Ataques: fatoração, timing, side-channel<br>• Pós-quântico: vulnerável a algoritmos quânticos"}
        });
    }
    private String gerarTutorialPrimosIntervalo() {
        return gerarTutorialHTML("Primos por Intervalo", new String[][]{
            {"🔍 Conceito", "Encontrar todos os números primos em um intervalo específico [a, b] é um problema fundamental da teoria dos números. Permite análise de distribuição e padrões dos primos."},
            {"📊 Métodos de Busca", "• Crivo de Eratóstenes para intervalos pequenos<br>• Teste de primalidade para números grandes<br>• Otimizações: testar apenas até √n<br>• Paralelização para intervalos extensos"},
            {"🔬 Distribuição dos Primos", "• Teorema dos Números Primos: π(x) ≈ x/ln(x)<br>• Integral Logarítmica: Li(x) = ∫₂ˣ (1/ln(t)) dt<br>• Hipótese de Riemann: melhora a precisão<br>• Gaps entre primos: variam de 2 a valores muito grandes"},
            {"🌍 História", "• 300 a.C.: Crivo de Eratóstenes<br>• 1798: Legendre propõe π(x) ≈ x/(ln(x) - 1.08366)<br>• 1859: Riemann propõe a hipótese<br>• 1896: Hadamard e de la Vallée-Poussin provam PNT"},
            {"💻 Aplicações", "• Criptografia: geração de primos<br>• Estatística: análise de distribuição<br>• Matemática: testes de conjecturas<br>• Computação: benchmarks de performance<br>• Educação: demonstração de conceitos"},
            {"🎯 Desafios", "• Intervalos muito grandes (> 10¹⁰)<br>• Memória para armazenar resultados<br>• Tempo de processamento<br>• Verificação de primalidade eficiente<br>• Padrões na distribuição"}
        });
    }
    private String gerarTutorialPrimosAleatorios() {
        return gerarTutorialHTML("Primos Aleatórios", new String[][]{
            {"🔍 Conceito", "Primos aleatórios são números primos gerados de forma pseudo-aleatória, essenciais para criptografia. A qualidade da aleatoriedade é crucial para a segurança dos sistemas criptográficos."},
            {"📊 Geração de Primos", "• Escolher um número aleatório de n bits<br>• Tornar o número ímpar (último bit = 1)<br>• Aplicar teste de primalidade<br>• Se não for primo, incrementar e testar novamente<br>• Usar método proprietário otimizado para eficiência"},
            {"🔬 Testes de Primalidade", "• Teste de algoritmo especializado (probabilístico)<br>• Teste de Fermat (base 2)<br>• Teste de Solovay-Strassen<br>• Teste de Lucas<br>• Combinação de testes para maior confiabilidade"},
            {"🌍 História", "• 1976: Diffie-Hellman introduz criptografia de chave pública<br>• 1977: RSA requer primos aleatórios grandes<br>• 1980: Desenvolvimento de geradores pseudo-aleatórios<br>• 1990: Padrões para geração de primos<br>• 2000: Otimizações para criptografia moderna"},
            {"💻 Aplicações", "• Criptografia RSA e DSA<br>• Protocolos de troca de chaves<br>• Assinaturas digitais<br>• Criptografia de curva elíptica<br>• Sistemas de votação eletrônica"},
            {"🎯 Segurança", "• Tamanho mínimo: 2048 bits para RSA<br>• Qualidade da aleatoriedade<br>• Resistência a ataques de fatoração<br>• Distribuição uniforme dos primos<br>• Proteção contra ataques de timing"}
        });
    }
    private String gerarTutorialConjecturaLegendre() {
        return gerarTutorialHTML("Conjectura de Legendre", new String[][]{
            {"🔍 Enunciado", "A conjectura de Legendre afirma que para todo número natural n > 1, existe sempre pelo menos um número primo entre n² e (n+1)². Esta é uma das conjecturas mais famosas da teoria dos números."},
            {"📊 Formulação Matemática", "Para todo n ∈ ℕ, n > 1:<br>• Existe p primo tal que n² < p < (n+1)²<br>• Ou equivalentemente: π((n+1)²) - π(n²) ≥ 1<br>• Onde π(x) é a função contadora de primos"},
            {"🔬 Relação com Outras Conjecturas", "• Conjectura de Andrica: pₙ₊₁ - pₙ < 2√pₙ<br>• Conjectura de Brocard: π(pₙ²) - π(pₙ₋₁²) ≥ 4<br>• Hipótese de Riemann: melhora estimativas<br>• Teorema dos Números Primos: π(x) ≈ x/ln(x)"},
            {"🌍 História", "• 1798: Legendre propõe a conjectura<br>• 1850: Chebyshev prova π(x) ≈ x/ln(x)<br>• 1896: Hadamard e de la Vallée-Poussin provam PNT<br>• 1919: Ramanujan trabalha em estimativas<br>• 2024: Ainda não provada nem refutada"},
            {"💻 Verificação Computacional", "• Testada para n até 10¹⁰<br>• Sem contraexemplos encontrados<br>• Distribuição dos primos confirma<br>• Padrões interessantes observados<br>• Limites superiores estabelecidos"},
            {"🎯 Status Atual", "• Conjectura não provada<br>• Evidência computacional forte<br>• Relacionada a problemas em aberto<br>• Importante para teoria dos números<br>• Desafio para matemáticos"}
        });
    }

    /**
     * Gera HTML bonito para resultados dos botões (similar aos tutoriais)
     */
    private String gerarHTMLResultado(String titulo, String resultado, String tipo) {
        StringBuilder html = new StringBuilder();
        String tituloSeguro = escaparHtml(titulo);
        String tipoSeguro = escaparHtml(tipo);
        String resultadoSeguro = escaparHtml(resultado);
        String geradoEm = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String tamanhoRelatorio = String.format(Locale.getDefault(), "%,d", resultado != null ? resultado.length() : 0);

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='pt-BR'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>").append(tituloSeguro).append(" - PrimeProFast</title>\n");
        html.append("    <style>\n");
        html.append("        :root { color-scheme: light dark; --bg:#f6f8fb; --bg2:#e8eff7; --surface:#ffffff; --surface-2:#f7fafc; --text:#142033; --muted:#5c697a; --border:#d6e1ec; --brand:#1d4e89; --accent:#2e7d6f; --code:#0f1720; --code-text:#e8edf3; }\n");
        html.append("        @media (prefers-color-scheme: dark) { :root { --bg:#0e131a; --bg2:#161e29; --surface:#1a222e; --surface-2:#202a39; --text:#eef3f8; --muted:#aab6c5; --border:#364458; --code:#0b1118; --code-text:#edf4fb; } }\n");
        html.append("        * { box-sizing: border-box; }\n");
        html.append("        body { margin:0; min-height:100vh; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif; color:var(--text); background:linear-gradient(180deg,var(--bg),var(--bg2)); }\n");
        html.append("        .progress { position:fixed; top:0; left:0; right:0; height:4px; background:transparent; z-index:10; }\n");
        html.append("        .progress span { display:block; width:0%; height:100%; background:linear-gradient(90deg,var(--brand),var(--accent)); }\n");
        html.append("        .shell { width:min(1100px,100%); margin:0 auto; padding:18px; }\n");
        html.append("        .hero { background:linear-gradient(135deg,var(--brand),#266276,var(--accent)); color:white; border-radius:8px; padding:22px; box-shadow:0 12px 28px rgba(15,23,42,.18); }\n");
        html.append("        .brand { font-size:.78rem; font-weight:700; letter-spacing:.08em; text-transform:uppercase; opacity:.82; }\n");
        html.append("        h1 { margin:.35rem 0 .25rem; font-size:clamp(1.45rem,4vw,2.4rem); line-height:1.08; }\n");
        html.append("        .subtitle { margin:0; color:rgba(255,255,255,.86); }\n");
        html.append("        .meta { display:flex; flex-wrap:wrap; gap:8px; margin-top:16px; }\n");
        html.append("        .chip { border:1px solid rgba(255,255,255,.28); background:rgba(255,255,255,.12); color:white; border-radius:999px; padding:7px 10px; font-size:.86rem; }\n");
        html.append("        .panel { margin-top:14px; border:1px solid var(--border); background:var(--surface); border-radius:8px; overflow:hidden; box-shadow:0 8px 20px rgba(15,23,42,.08); }\n");
        html.append("        .panel-head { display:flex; gap:12px; align-items:center; justify-content:space-between; padding:14px 16px; border-bottom:1px solid var(--border); background:var(--surface-2); }\n");
        html.append("        .panel-title { font-weight:700; }\n");
        html.append("        button { border:1px solid var(--border); background:var(--surface); color:var(--text); border-radius:8px; padding:9px 12px; font-weight:700; }\n");
        html.append("        pre { margin:0; padding:16px; background:var(--code); color:var(--code-text); overflow:auto; white-space:pre-wrap; overflow-wrap:anywhere; word-break:break-word; font:13px/1.55 'SFMono-Regular',Consolas,'Liberation Mono',monospace; }\n");
        html.append("        .footer { color:var(--muted); text-align:center; padding:16px 8px 4px; font-size:.86rem; }\n");
        html.append("        @media (max-width:600px) { .shell { padding:10px; } .hero { padding:18px; } .panel-head { align-items:flex-start; flex-direction:column; } button { width:100%; } pre { font-size:12px; padding:13px; } }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='progress'><span id='progress'></span></div>\n");
        html.append("    <main class='shell'>\n");
        html.append("        <header class='hero'>\n");
        html.append("            <div class='brand'>PrimeProFast</div>\n");
        html.append("            <h1>").append(tituloSeguro).append("</h1>\n");
        html.append("            <p class='subtitle'>").append(tipoSeguro).append("</p>\n");
        html.append("            <div class='meta'>\n");
        html.append("                <span class='chip'>Gerado em ").append(escaparHtml(geradoEm)).append("</span>\n");
        html.append("                <span class='chip'>").append(escaparHtml(tamanhoRelatorio)).append(" caracteres</span>\n");
        html.append("            </div>\n");
        html.append("        </header>\n");
        html.append("        <section class='panel'>\n");
        html.append("            <div class='panel-head'>\n");
        html.append("                <div class='panel-title'>Resultado completo</div>\n");
        html.append("            </div>\n");
        html.append("            <pre id='raw'>").append(resultadoSeguro).append("</pre>\n");
        html.append("        </section>\n");
        html.append("        <div class='footer'>PrimeProFast - visualização otimizada para leitura, seleção e compartilhamento</div>\n");
        html.append("    </main>\n");
        html.append("    <script>\n");
        html.append("        window.addEventListener('scroll', () => {\n");
        html.append("            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;\n");
        html.append("            const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;\n");
        html.append("            const scrollPercent = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 100;\n");
        html.append("            document.getElementById('progress').style.width = scrollPercent + '%';\n");
        html.append("        });\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String tituloResultadoPorPrefixo(String prefixo) {
        if (prefixo == null) {
            return "Resultado";
        }
        if (prefixo.contains("primos_aleatorios")) return "Primos Aleatórios";
        if (prefixo.contains("primos_especiais")) return "Primos Especiais";
        if (prefixo.contains("conjectura_legendre")) return "Conjectura de Legendre";
        if (prefixo.contains("mersenne")) return "Números de Mersenne";
        if (prefixo.contains("perfeitos")) return "Números Perfeitos";
        if (prefixo.contains("seguranca") || prefixo.contains("criptografia") || prefixo.contains("hash") || prefixo.contains("assinatura")) return "Segurança Digital";
        if (prefixo.contains("estatisticas") || prefixo.contains("aproximacao")) return "Estatísticas";
        if (prefixo.contains("primalidade")) return "Teste de Primalidade";
        if (prefixo.contains("intervalo") || prefixo.contains("primos_temp")) return "Primos por Intervalo";
        return "Resultado";
    }

    private String tipoResultadoPorPrefixo(String prefixo) {
        if (prefixo == null || prefixo.isEmpty()) {
            return "Relatório gerado";
        }
        String normalizado = prefixo.replace('_', ' ').trim();
        return normalizado.isEmpty() ? "Relatório gerado" : normalizado;
    }

    /**
     * Gera tutorial HTML interativo e profissional com 3 níveis
     */
    private String gerarTutorialHTML(String titulo, String[][] secoes) {
        return gerarTutorialHTMLNivel(titulo, secoes);
    }

    /** Gera página HTML do guia do card (secções com navegação). */
    private String gerarTutorialHTMLNivel(String titulo, String[][] secoes) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='pt-BR'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>").append(titulo).append(" — Guia</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }\n");
        html.append("        .container { width: 100%; margin: 0; padding: 0; }\n");
        html.append("        .header { text-align: center; margin-bottom: 40px; color: white; }\n");
        html.append("        .header h1 { font-size: 2.5em; margin-bottom: 10px; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }\n");
        html.append("        .header p { font-size: 1.2em; opacity: 0.9; }\n");
        html.append("        .progress-bar { width: 100%; height: 6px; background: rgba(255,255,255,0.2); border-radius: 3px; margin: 20px 0; overflow: hidden; }\n");
        html.append("        .progress-fill { height: 100%; background: linear-gradient(90deg, #4CAF50, #45a049); width: 0%; transition: width 0.3s ease; }\n");
        html.append("        .content { background: white; border-radius: 0; padding: 20px; box-shadow: none; margin: 0; }\n");
        html.append("        .exemplo { background: #e8f4fd; padding: 15px; border-radius: 8px; margin: 10px 0; border-left: 4px solid #2196F3; font-size: 0.95em; }\n");
        html.append("        .exemplo strong { color: #1976D2; }\n");
        html.append("        h3 { color: #1976D2; margin: 20px 0 10px 0; font-size: 1.3em; }\n");
        html.append("        h4 { color: #424242; margin: 15px 0 8px 0; font-size: 1.1em; }\n");
        html.append("        ol, ul { margin: 10px 0; padding-left: 25px; }\n");
        html.append("        li { margin: 5px 0; }\n");
        html.append("        p { margin: 10px 0; text-align: justify; }\n");
        html.append("        .secao { margin-bottom: 30px; padding: 20px; border-left: 5px solid #667eea; background: #f8f9fa; border-radius: 0 10px 10px 0; transition: transform 0.3s ease, box-shadow 0.3s ease; }\n");
        html.append("        .secao:hover { transform: translateX(10px); box-shadow: 0 10px 25px rgba(0,0,0,0.15); }\n");
        html.append("        .secao h3 { color: #667eea; font-size: 1.4em; margin-bottom: 15px; display: flex; align-items: center; }\n");
        html.append("        .secao h3::before { content: ''; width: 8px; height: 8px; background: #667eea; border-radius: 50%; margin-right: 12px; }\n");
        html.append("        .secao p { font-size: 1.1em; line-height: 1.8; color: #555; }\n");
        html.append("        .navigation { position: fixed; top: 20px; right: 20px; background: white; border-radius: 15px; padding: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); z-index: 1000; }\n");
        html.append("        .nav-item { display: block; padding: 10px 15px; margin: 5px 0; text-decoration: none; color: #667eea; border-radius: 8px; transition: all 0.3s ease; }\n");
        html.append("        .nav-item:hover { background: #667eea; color: white; transform: translateX(5px); }\n");
        html.append("        .nav-item.active { background: #667eea; color: white; }\n");
        html.append("        .nivel-selector { text-align: center; margin-bottom: 20px; }\n");
        html.append("        .footer { text-align: center; margin-top: 40px; padding: 20px; color: white; font-size: 0.9em; }\n");
        html.append("        @media (max-width: 768px) { .navigation { position: static; margin-bottom: 20px; } .container { padding: 10px; } .content { padding: 20px; } }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='container'>\n");
        html.append("        <div class='header'>\n");
        html.append("            <h1>💡 ").append(titulo).append("</h1>\n");
        html.append("            <p>Guia do card — função, uso e contexto</p>\n");
        html.append("            <div class='progress-bar'>\n");
        html.append("                <div class='progress-fill' id='progress'></div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class='navigation'>\n");
        html.append("            <h4 style='margin-bottom: 15px; color: #333;'>📚 Navegação</h4>\n");

        // Adicionar itens de navegação
        for (int i = 0; i < secoes.length; i++) {
            String tituloSecao = secoes[i][0];
            html.append("            <a href='#secao").append(i).append("' class='nav-item' onclick='scrollToSection(").append(i).append(")'>").append(tituloSecao).append("</a>\n");
        }

        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class='content'>\n");

        // Adicionar seções
        for (int i = 0; i < secoes.length; i++) {
            String tituloSecao = secoes[i][0];
            String conteudoSecao = secoes[i][1];

            html.append("            <div class='secao' id='secao").append(i).append("'>\n");
            html.append("                <h3>").append(tituloSecao).append("</h3>\n");
            html.append("                <p>").append(conteudoSecao).append("</p>\n");
            html.append("            </div>\n");
        }

        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class='footer'>\n");
        html.append("            <p>🎓 PrimeProFast — ").append(titulo).append("</p>\n");
        html.append("            <p>📅 ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())).append("</p>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("    \n");
        html.append("    <script>\n");
        html.append("        // Atualizar barra de progresso baseada no scroll\n");
        html.append("        window.addEventListener('scroll', () => {\n");
        html.append("            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;\n");
        html.append("            const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;\n");
        html.append("            const scrollPercent = (scrollTop / scrollHeight) * 100;\n");
        html.append("            document.getElementById('progress').style.width = scrollPercent + '%';\n");
        html.append("        });\n");
        html.append("        \n");
        html.append("        // Navegação suave para seções\n");
        html.append("        function scrollToSection(index) {\n");
        html.append("            const section = document.getElementById('secao' + index);\n");
        html.append("            section.scrollIntoView({ behavior: 'smooth' });\n");
        html.append("            \n");
        html.append("            // Atualizar navegação ativa\n");
        html.append("            document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));\n");
        html.append("            event.target.classList.add('active');\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        // Destacar seção atual no scroll\n");
        html.append("        window.addEventListener('scroll', () => {\n");
        html.append("            const sections = document.querySelectorAll('.secao');\n");
        html.append("            const navItems = document.querySelectorAll('.nav-item');\n");
        html.append("            \n");
        html.append("            let current = '';\n");
        html.append("            sections.forEach((section, index) => {\n");
        html.append("                const sectionTop = section.offsetTop;\n");
        html.append("                if (pageYOffset >= sectionTop - 200) {\n");
        html.append("                    current = index;\n");
        html.append("                }\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            navItems.forEach((item, index) => {\n");
        html.append("                item.classList.remove('active');\n");
        html.append("                if (index === current) {\n");
        html.append("                    item.classList.add('active');\n");
        html.append("            });\n");
        html.append("        });\n");
        html.append("        });\n");
        html.append("        \n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }


    private void abrirFuncao(String funcao) {
        if ("Corrida Prima".equals(funcao) || "Resgate Espacial".equals(funcao)) {
            abrirArcade("Resgate Espacial".equals(funcao));
            return;
        }
        // Definir card atual para controle de limitações
        currentCard = funcao;
        carregarCalculosCard();

        // Ocultar menu (ScrollView) e mostrar conteúdo com animação suave
        if (menuScrollView != null) {
            menuScrollView.animate().alpha(0.0f).setDuration(300).withEndAction(() -> {
                menuScrollView.setVisibility(View.GONE);
                mostrarTelaConteudo();
                if (contentScrollView != null) {
                    contentScrollView.setAlpha(0.0f);
                    contentScrollView.animate().alpha(1.0f).setDuration(300);
                } else {
                    contentContainer.setAlpha(0.0f);
                    contentContainer.animate().alpha(1.0f).setDuration(300);
                }
            });
        } else {
            // Fallback: caso ScrollView não esteja inicializado por algum motivo
            menuContainer.setVisibility(View.GONE);
            mostrarTelaConteudo();
        }

        // Após Segurança Digital, resultadoView pode estar fora do scrollView global — restaurar antes dos outros cards
        if (!"Segurança Digital".equals(funcao)) {
            garantirResultadoViewNoScrollGlobal();
        }

        // Criar interface específica para cada função
        switch (funcao) {
            case "Primos por Intervalo":
                criarInterfacePrimosIntervalo();
                break;
            case "Primos Especiais":
                criarInterfacePrimosEspeciais();
                break;
            case "Primos Aleatórios":
                criarInterfacePrimosAleatorios();
                break;
            case "Conjectura de Legendre":
                criarInterfaceConjecturaLegendre();
                break;
            case "Números de Mersenne":
                criarInterfaceMersenne();
                break;
            case "Números Perfeitos":
                criarInterfaceNumerosPerfeitos();
                break;
            case "Segurança Digital":
                criarInterfaceSegurancaDigital();
                break;
            case "Teste de Primalidade":
                criarInterfaceTestePrimalidade();
                break;
            case "Estatísticas":
                criarInterfaceEstatisticas();
                break;
            case "Jogo dos Primos":
                criarInterfaceJogoPrimos();
                break;
        }
        rolarConteudoParaInicio();
    }

    private void criarInterfacePrimosIntervalo() {
        // Limpar container
        contentContainer.removeAllViews();

        // Título
        TextView titulo = new TextView(this);
        titulo.setText("Primos por Intervalo");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        contentContainer.addView(titulo);

        adicionarRotuloCampo("Valor máximo N (primos até este número)");
        EditText inputN = new EditText(this);
        inputN.setHint("Digite aqui (ex.: 1000000 — até 50 bilhões)");
        inputN.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        estilizarCampoEntrada(inputN);
        contentContainer.addView(inputN);

        // Botões com design moderno
        Button btnTela = criarBotaoModerno("📱 Calcular e Mostrar na Tela",
            Color.parseColor("#4CAF50"), Color.parseColor("#66BB6A"));
        contentContainer.addView(btnTela);

        Button btnTxt = criarBotaoModerno("💾 Calcular e Salvar em TXT",
            Color.parseColor("#2196F3"), Color.parseColor("#42A5F5"));
        contentContainer.addView(btnTxt);

        // Listener para cálculo até N (SISTEMA ORIGINAL RESTAURADO)
        btnTela.setOnClickListener(v -> {
            try {
                String inputText = inputN.getText().toString();
                if (inputText.isEmpty()) {
                    Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
                    return;
                }

                long n = Long.parseLong(inputText);
                if (n <= 0) {
                    Toast.makeText(this, "Digite um número maior que 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (n > 50000000000L) {
                    Toast.makeText(this, "Número muito grande. Use até 50.000.000.000", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verificar limites de monetização do card
                if (!podeRealizarCalculoCard(n, false)) {
                    mostrarDialogoLimiteCard(n, false);
                    return;
                }

                Log.d(TAG, "Iniciando cálculo de primos até " + n);
                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Primos por Intervalo",
                    "Valor máximo: " + String.format("%,d", n)
                );

                new Thread(() -> {
                    try {
                        Log.d(TAG, "Chamando função nativa calcularPrimos");
                        String resultado = calcularPrimos(n);
                        Log.d(TAG, "Resultado recebido: " + resultado.substring(0, Math.min(100, resultado.length())));

                        long fimMs = System.currentTimeMillis();
                        resultado = alinharTempoCalcularPrimosComCronometro(resultado, cronometroAtivo.inicioMs, fimMs);
                        pararCronometroOperacao(cronometroAtivo);

                        // Registrar cálculo realizado
                        registrarCalculo();
                        registrarCalculoCard();

                        runOnUiThread(() -> {
                            resultadoView.setText("Resultado gerado.");
                            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
                        });
                        salvarResultadoTemporario(resultado, "primos_intervalo");
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao calcular primos", e);
                        runOnUiThread(() -> {
                            pararCronometroOperacao(cronometroAtivo);
                            resultadoView.setText("Erro ao calcular: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para salvar em TXT (SISTEMA ORIGINAL)
        btnTxt.setOnClickListener(v -> {
            try {
                String inputText = inputN.getText().toString();
                if (inputText.isEmpty()) {
                    Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
                    return;
                }

                long n = Long.parseLong(inputText);
                if (n <= 0) {
                    Toast.makeText(this, "Digite um número maior que 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (n > 50000000000L) {
                    Toast.makeText(this, "Número muito grande. Use até 50.000.000.000", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verificar limites de monetização do card
                if (!podeRealizarCalculoCard(n, false)) {
                    mostrarDialogoLimiteCard(n, false);
                    return;
                }

                Log.d(TAG, "Iniciando cálculo de primos até " + n + " para salvar em TXT");
                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Primos por Intervalo",
                    "Valor máximo: " + String.format("%,d", n),
                    "Modo: salvar em TXT"
                );

                new Thread(() -> {
                    try {
                        Log.d(TAG, "Chamando função nativa calcularPrimos para TXT");
                        String resultado = calcularPrimos(n);
                        Log.d(TAG, "Resultado recebido para TXT: " + resultado.substring(0, Math.min(100, resultado.length())));

                        long fimMs = System.currentTimeMillis();
                        resultado = alinharTempoCalcularPrimosComCronometro(resultado, cronometroAtivo.inicioMs, fimMs);
                        pararCronometroOperacao(cronometroAtivo);

                        // Registrar cálculo realizado
                        registrarCalculo();
                        registrarCalculoCard();

                        // Salvar em arquivo TXT (Downloads/PrimeProFast via MediaStore em Android 10+)
                        try {
                            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                            String fileName = "primos_" + n + "_" + timestamp + ".txt";
                            String caminhoSalvo = salvarTxtDownloadsPrimeProFast(fileName, resultado);

                            Log.d(TAG, "Arquivo TXT criado: " + caminhoSalvo);

                            runOnUiThread(() -> {
                                resultadoView.setText("Arquivo salvo com sucesso em:\n" + caminhoSalvo);
                                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
                                Toast.makeText(this, "Arquivo salvo em: " + caminhoSalvo, Toast.LENGTH_LONG).show();
                            });
                        } catch (IOException e) {
                            Log.e(TAG, "Erro ao salvar arquivo TXT", e);
                            runOnUiThread(() -> {
                                resultadoView.setText("Erro ao salvar arquivo TXT: " + e.getMessage());
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao calcular primos para TXT", e);
                        runOnUiThread(() -> {
                            pararCronometroOperacao(cronometroAtivo);
                            resultadoView.setText("Erro ao calcular: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
            }
        });

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    private void criarInterfacePrimosEspeciais() {
        contentContainer.removeAllViews();

        // Título
        TextView titulo = new TextView(this);
        titulo.setText("⭐ Primos Especiais");
        titulo.setTextSize(20);
        titulo.setTextColor(0xFF9C27B0);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setPadding(16, 20, 16, 20);
        titulo.setGravity(android.view.Gravity.CENTER);
        contentContainer.addView(titulo);

        // Descrição
        TextView descricao = new TextView(this);
        descricao.setText("Explore classes específicas de números primos famosos\nDescubra padrões matemáticos fascinantes e suas aplicações");
        descricao.setTextSize(14);
        descricao.setTextColor(0xFF666666);
        descricao.setPadding(16, 0, 16, 20);
        descricao.setGravity(android.view.Gravity.CENTER);
        contentContainer.addView(descricao);

        // Seção de seleção de tipo
        TextView tituloTipo = new TextView(this);
        tituloTipo.setText("🔬 Escolha o tipo de primo especial:");
        tituloTipo.setTextSize(16);
        tituloTipo.setTextColor(0xFF333333);
        tituloTipo.setTypeface(null, Typeface.BOLD);
        tituloTipo.setPadding(16, 16, 16, 8);
        contentContainer.addView(tituloTipo);

        // Spinner para seleção do tipo
        Spinner spinnerTipo = new Spinner(this);
        // Indices follow the PPF_SPECIAL_* family identifiers in ppf_c_api.h.
        String[] tiposPrimos = {
            "Primos Gêmeos (p, p+2)",
            "Primos de Sophie Germain (p, 2p+1)",
            "Primos Cousins (p, p+4)",
            "Primos Sexy (p, p+6)",
            "Primos Palíndromos",
            SPECIAL_FERMAT_LABEL
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tiposPrimos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);
        spinnerTipo.setPadding(16, 8, 16, 8);
        contentContainer.addView(spinnerTipo);

        adicionarRotuloCampo("Quantidade de primos a listar");
        EditText inputQuantidade = new EditText(this);
        inputQuantidade.setHint("Digite aqui (ex.: 10)");
        inputQuantidade.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputQuantidade.setText("10");
        estilizarCampoEntrada(inputQuantidade);
        contentContainer.addView(inputQuantidade);

        TextView rotuloLimite = adicionarRotuloCampo("Limite superior da busca");
        EditText inputLimite = new EditText(this);
        inputLimite.setHint("Digite aqui (ex.: 10000)");
        inputLimite.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputLimite.setText("10000");
        estilizarCampoEntrada(inputLimite);
        contentContainer.addView(inputLimite);

        // Botão para gerar
        Button btnGerar = new Button(this);
        btnGerar.setText("⭐ Gerar Primos Especiais");
        btnGerar.setBackgroundColor(0xFF9C27B0);
        btnGerar.setTextColor(0xFFFFFFFF);
        btnGerar.setPadding(16, 12, 16, 12);
        btnGerar.setOnClickListener(v -> {
            try {
                String tipoSelecionado = (String) spinnerTipo.getSelectedItem();
                int quantidade = Integer.parseInt(inputQuantidade.getText().toString());
                boolean fermat = SPECIAL_FERMAT_LABEL.equals(tipoSelecionado);
                long limite = fermat ? 0 : Long.parseLong(inputLimite.getText().toString());

                if (quantidade <= 0) {
                    Toast.makeText(this, "Digite uma quantidade válida", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!fermat && limite < 2) {
                    Toast.makeText(this, "Digite um limite válido", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!fermat && quantidade > 10000) {
                    Toast.makeText(this, "Informe até 10.000 resultados.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verificar limites de monetização
                if (!fermat && !podeRealizarCalculo(limite)) {
                    mostrarDialogoLimiteAtingido(limite);
                    return;
                }

                gerarPrimosEspeciais(spinnerTipo.getSelectedItemPosition(), tipoSelecionado, quantidade, limite);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite números válidos", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnGerar);

        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean fermat = SPECIAL_FERMAT_LABEL.equals(parent.getItemAtPosition(position));
                rotuloLimite.setVisibility(fermat ? View.GONE : View.VISIBLE);
                inputLimite.setVisibility(fermat ? View.GONE : View.VISIBLE);
                btnGerar.setText(fermat ? "Listar primos conhecidos" : "Sortear primos especiais");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Área de resultado
        resultadoView = new TextView(this);
        resultadoView.setText("Clique em 'Gerar Primos Especiais' para começar");
        resultadoView.setTextSize(14);
        resultadoView.setPadding(16, 16, 16, 16);
        resultadoView.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#333333"));
        aplicarEstiloAreaResultado();
        contentContainer.addView(resultadoView);
    }

    /**
     * Gera primos especiais baseado no tipo selecionado
     */
    private void gerarPrimosEspeciais(int familia, String tipo, int quantidade, long limite) {
        boolean fermat = SPECIAL_FERMAT_LABEL.equals(tipo);
        CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
            resultadoView,
            "Primos Especiais",
            "Tipo: " + tipo,
            "Quantidade: " + quantidade,
            fermat ? "Cinco primos de Fermat conhecidos" : "Limite: " + String.format("%,d", limite)
        );

                new Thread(() -> {
                    try {
                String relatorioComum = specialPrimesNativo(familia, limite, quantidade);
                if (relatorioComum == null || relatorioComum.isEmpty()) {
                    throw new IllegalStateException("Não foi possível preparar a busca.");
                }
                publicarPrimosEspeciais(relatorioComum, cronometroAtivo);

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                    pararCronometroOperacao(cronometroAtivo);
                    resultadoView.setText("Erro ao gerar primos especiais: " + e.getMessage());
                    resultadoView.setTextColor(0xFFFF0000);
                        });
                    }
                }).start();
    }

    private void publicarPrimosEspeciais(String resultado, CronometroOperacaoHandle cronometro) {
        registrarCalculo();
        runOnUiThread(() -> {
            pararCronometroOperacao(cronometro);
            resultadoView.setText(resultado);
            resultadoView.setTextColor(corTextoPrincipal());
            salvarResultadoTemporario(resultado, "primos_especiais");
        });
    }

    private boolean ehPrimo(long n) {
        return n >= 2 && ehPrimo64Nativo(n);
    }

    // ========================================
    // SISTEMA DE SEGURANÇA E ANTI-PIRATARIA
    // ========================================

    /**
     * Verifica a segurança do app e detecta tentativas de engenharia reversa
     */
    private boolean verificarSeguranca() {
        try {
            // Em builds debuggable (debug), não bloquear a execução.
            // As validações abaixo são voltadas para release/produção.
            if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                isSecurityValid = true;
                return true;
            }

            // 1. Verificar integridade do app
            if (!verificarIntegridadeApp()) {
                mostrarErroSeguranca("App corrompido detectado");
                return false;
            }

            // 2. Detectar ambiente malicioso
            if (detectarAmbienteMalicioso()) {
                mostrarErroSeguranca("Ambiente não seguro detectado");
                return false;
            }

            // 3. Verificar assinatura digital
            if (!verificarAssinaturaDigital()) {
                mostrarErroSeguranca("Assinatura inválida");
                return false;
            }

            // 4. Detectar debugging
            if (detectarDebugging()) {
                mostrarErroSeguranca("Debugging detectado");
                return false;
            }

            isSecurityValid = true;
            return true;

        } catch (Exception e) {
            mostrarErroSeguranca("Erro de segurança");
            return false;
        }
    }

    /**
     * Verifica a integridade do app
     */
    private boolean verificarIntegridadeApp() {
        try {
            // Verificar se o app não foi modificado
            String packageName = getPackageName();
            if (packageName == null || !packageName.equals("com.primeprofast.app")) {
                return false;
            }

            // Verificar se os recursos essenciais existem
            if (getResources().getIdentifier("app_name", "string", getPackageName()) == 0) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detecta ambiente malicioso (root, emulador, etc.)
     */
    private boolean detectarAmbienteMalicioso() {
        try {
            // Detectar root
            if (verificarRoot()) {
                return true;
            }

            // Detectar emulador
            if (verificarEmulador()) {
                return true;
            }

            // Detectar ferramentas de análise
            if (verificarFerramentasAnalise()) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return true; // Em caso de erro, considerar inseguro
        }
    }

    /**
     * Verifica se o dispositivo está com root
     */
    private boolean verificarRoot() {
        try {
            // Verificar arquivos comuns de root
            String[] rootPaths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            };

            for (String path : rootPaths) {
                if (new java.io.File(path).exists()) {
                    return true;
                }
            }

            // Verificar se su está disponível
            try {
                Process process = Runtime.getRuntime().exec("su");
                process.destroy();
                return true;
            } catch (Exception e) {
                // Su não disponível, ok
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se está rodando em emulador
     */
    private boolean verificarEmulador() {
        try {
            String buildModel = android.os.Build.MODEL.toLowerCase();
            String buildManufacturer = android.os.Build.MANUFACTURER.toLowerCase();
            String buildProduct = android.os.Build.PRODUCT.toLowerCase();

            // Verificar padrões de emulador
            if (buildModel.contains("google_sdk") ||
                buildModel.contains("emulator") ||
                buildModel.contains("android sdk") ||
                buildManufacturer.contains("genymotion") ||
                buildProduct.contains("sdk") ||
                buildProduct.contains("emulator")) {
                return true;
            }

            // Verificar propriedades específicas de emulador
            try {
                String roKernelQemu = System.getProperty("ro.kernel.qemu");
                if ("1".equals(roKernelQemu)) {
                    return true;
                }
            } catch (Exception e) {
                // Ignorar
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica ferramentas de análise
     */
    private boolean verificarFerramentasAnalise() {
        try {
            // Verificar se há apps de análise instalados
            String[] analysisApps = {
                "com.keniuwa.spy",
                "com.saurik.substrate",
                "de.robv.android.xposed.installer",
                "com.topjohnwu.magisk",
                "com.noshufou.android.su",
                "com.thirdparty.superuser",
                "eu.chainfire.supersu",
                "com.koushikdutta.superuser"
            };

            for (String app : analysisApps) {
                try {
                    getPackageManager().getPackageInfo(app, 0);
                    return true; // App encontrado
                } catch (Exception e) {
                    // App não encontrado, continuar
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica assinatura digital do app
     */
    private boolean verificarAssinaturaDigital() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return possuiAssinaturaLegada();
            android.content.pm.PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info = getPackageManager().getPackageInfo(getPackageName(),
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES));
            } else {
                info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            }
            android.content.pm.Signature[] assinantes = info.signingInfo == null
                    ? null : info.signingInfo.getApkContentsSigners();
            return assinantes != null && assinantes.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean possuiAssinaturaLegada() throws PackageManager.NameNotFoundException {
        android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(
                getPackageName(), PackageManager.GET_SIGNATURES);
        return info.signatures != null && info.signatures.length > 0;
    }

    /**
     * Detecta se o app está sendo debugado
     */
    private boolean detectarDebugging() {
        try {
            // Verificar se está em modo debug
            if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return false;
            }

            // Verificar se há debugger conectado
            if (android.os.Debug.isDebuggerConnected()) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mostra erro de segurança e fecha o app
     */
    private void mostrarErroSeguranca(String mensagem) {
        try {
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Erro de Segurança")
                    .setMessage("Este app não pode ser executado neste ambiente.\n\n" + mensagem)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setOnDismissListener(d -> {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
            });
        } catch (Exception e) {
            finish();
        }
    }

    // Interfaces para as outras funcionalidades (serão implementadas)
    private void criarInterfacePrimosAleatorios() {
        contentContainer.removeAllViews();


        // Título
        TextView titulo = new TextView(this);
        titulo.setText("🔢 Primos Aleatórios");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        contentContainer.addView(titulo);

        // Descrição
        TextView descricao = new TextView(this);
        descricao.setText("Gere números primos aleatórios de tamanho criptográfico (até 10.000 dígitos).");
        descricao.setPadding(0, 0, 0, 16);
        contentContainer.addView(descricao);

        // Aviso sobre natureza aleatória da busca e recomendação de cancelamento
        TextView avisoAleatorio = new TextView(this);
        avisoAleatorio.setText(
            "ℹ️ Por se tratar de busca aleatória, o tempo para encontrar primos pode variar bastante de uma execução para outra.\n" +
            "Se a busca demorar muito, recomenda-se cancelar e tentar novamente com os mesmos parâmetros ou ajustando o tamanho em bits."
        );
        avisoAleatorio.setTextSize(12);
        avisoAleatorio.setTextColor(Color.parseColor("#555555"));
        avisoAleatorio.setPadding(0, 0, 0, 16);
        contentContainer.addView(avisoAleatorio);

        adicionarRotuloCampo("Quantidade de primos");
        EditText inputQuantidade = new EditText(this);
        inputQuantidade.setHint("Digite aqui (1 a 50)");
        inputQuantidade.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputQuantidade.setText("5");
        estilizarCampoEntrada(inputQuantidade);
        contentContainer.addView(inputQuantidade);

        adicionarRotuloCampo("Tamanho de cada primo (bits)");
        EditText inputBits = new EditText(this);
        inputBits.setHint("Digite aqui (ex.: 512, 1024… até ~33.000)");
        inputBits.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputBits.setText("1024");
        estilizarCampoEntrada(inputBits);
        contentContainer.addView(inputBits);

        // Informações sobre bits vs dígitos
        TextView infoBits = new TextView(this);
        infoBits.setText("💡 Informações sobre tamanhos:\n" +
                        "• 512 bits → números de ~155 dígitos\n" +
                        "• 1024 bits → números de ~309 dígitos\n" +
                        "• 2048 bits → números de ~617 dígitos\n" +
                        "• 4096 bits → números de ~1233 dígitos\n" +
                        "• 8192 bits → números de ~2465 dígitos");
        infoBits.setPadding(0, 10, 0, 20);
        infoBits.setTextColor(Color.parseColor("#666666"));
        contentContainer.addView(infoBits);



        // Botão gerar com design moderno
        Button btnGerar = criarBotaoModerno("Gerar primos aleatórios",
            Color.parseColor("#FF6B6B"), Color.parseColor("#FF8A80"));

        // Botão genérico de cancelar operação longa deste card
        Button btnCancelar = criarBotaoModerno("Cancelar",
            Color.parseColor("#757575"), Color.parseColor("#9E9E9E"));
        btnCancelar.setVisibility(View.GONE);
        btnCancelar.setOnClickListener(c -> {
            cancelarOperacaoAtual.set(true);
            Toast.makeText(this, "Operação atual será cancelada...", Toast.LENGTH_SHORT).show();
        });
        this.btnCancelarOperacaoAtual = btnCancelar;

        btnGerar.setOnClickListener(v -> {
            try {
                String quantidadeStr = inputQuantidade.getText().toString();
                String bitsStr = inputBits.getText().toString();
                if (geracaoAleatoriaAtiva.get()) return;

                if (quantidadeStr.isEmpty() || bitsStr.isEmpty()) {
                    Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                int quantidade = Integer.parseInt(quantidadeStr);
                int bits = Integer.parseInt(bitsStr);

                if (quantidade <= 0 || quantidade > 50) {
                    Toast.makeText(this, "Quantidade deve ser entre 1 e 50", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (bits <= 0 || bits > 33000) {
                    Toast.makeText(this, "Bits deve ser entre 1 e 33.000", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (bits == 1) {
                    Toast.makeText(this, "Não existe número primo de 1 bit.", Toast.LENGTH_LONG).show();
                    return;
                }

                int disponiveis = shortPrimeCountNativo(bits);
                if (disponiveis < 0) {
                    Toast.makeText(this, "Não foi possível preparar o sorteio.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (disponiveis > 0) {
                    if (quantidade > disponiveis) {
                        Toast.makeText(this, "Há apenas " + disponiveis + " primos distintos de "
                            + bits + " bits. Reduza a quantidade.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    mostrarPrimosCurtos(bits, quantidade);
                    return;
                }

                if (bits > LIMIAR_BITS_ROTA_NATIVA) {
                    mostrarDialogoEscolhaPrimosUltra(bits, quantidade);
                    return;
                }

                if (!geracaoAleatoriaAtiva.compareAndSet(false, true)) return;
                cancelarOperacaoAtual.set(false);
                if (btnCancelarOperacaoAtual != null) {
                    btnCancelarOperacaoAtual.setVisibility(View.VISIBLE);
                }

                final CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Primos Aleatórios",
                    "Quantidade: " + quantidade,
                    "Bits: " + bits
                );

                new Thread(() -> executarThreadPrimosAleatorios(bits, quantidade, cronometroAtivo)).start();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite números válidos", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnGerar);
        contentContainer.addView(btnCancelar);

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    private void criarInterfaceJogoPrimos() {
        criarInterfacePrimeMatchMapa();
    }

    private void criarInterfacePrimeMatchMapa() {
        appHeader.setVisibility(View.VISIBLE);
        contentContainer.removeAllViews();
        carregarPrimeMatchPoderes();

        int faseLiberada = obterPrimeMatchFaseLiberada();
        TextView titulo = new TextView(this);
        titulo.setText("Prime Match Saga");
        titulo.setTextSize(24);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(corTextoPrincipal());
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(0, dpUi(10), 0, dpUi(4));
        contentContainer.addView(titulo);

        TextView subtitulo = new TextView(this);
        subtitulo.setText("Escolha uma fase, rejogue para melhorar estrelas e avance pela trilha dos primos.");
        subtitulo.setTextSize(13);
        subtitulo.setTextColor(corTextoSecundario());
        subtitulo.setGravity(Gravity.CENTER);
        subtitulo.setPadding(dpUi(6), 0, dpUi(6), dpUi(12));
        contentContainer.addView(subtitulo);

        LinearLayout painel = new LinearLayout(this);
        painel.setOrientation(LinearLayout.VERTICAL);
        painel.setPadding(dpUi(14), dpUi(12), dpUi(14), dpUi(12));
        painel.setBackground(criarPrimeMatchMapaPainelDrawable());
        LinearLayout.LayoutParams painelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        painelLp.setMargins(0, 0, 0, dpUi(12));
        painel.setLayoutParams(painelLp);

        TextView progresso = new TextView(this);
        progresso.setText("Fase liberada: " + faseLiberada
                + "   Melhor valor: " + primeMatchBestScore
                + "\nPoderes guardados: " + totalPrimeMatchPoderesGuardados());
        progresso.setTextSize(13);
        progresso.setTypeface(null, Typeface.BOLD);
        progresso.setTextColor(Color.WHITE);
        progresso.setGravity(Gravity.CENTER);
        painel.addView(progresso);
        contentContainer.addView(painel);

        GridLayout mapa = new GridLayout(this);
        mapa.setColumnCount(4);
        mapa.setUseDefaultMargins(false);
        LinearLayout.LayoutParams mapaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        mapaLp.setMargins(0, 0, 0, dpUi(12));
        mapa.setLayoutParams(mapaLp);
        for (int fase = 1; fase <= PRIME_MATCH_MAX_LEVELS; fase++) {
            mapa.addView(criarPrimeMatchBotaoFase(fase, faseLiberada));
        }
        contentContainer.addView(mapa);

        Button continuar = criarBotaoModerno(
                "Continuar na fase " + faseLiberada,
                Color.parseColor("#2D9CDB"),
                Color.parseColor("#2FBA7C")
        );
        continuar.setOnClickListener(v -> iniciarPrimeMatchFase(faseLiberada));
        contentContainer.addView(continuar);
    }

    private Drawable criarPrimeMatchMapaPainelDrawable() {
        GradientDrawable fundo = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#256D85"),
                        Color.parseColor("#2FBA7C"),
                        Color.parseColor("#F2A541")
                }
        );
        fundo.setCornerRadius(dpUi(10));
        fundo.setStroke(dpUi(1), withAlpha(Color.WHITE, 132));
        return fundo;
    }

    private View criarPrimeMatchBotaoFase(int fase, int faseLiberada) {
        TextView btn = new TextView(this);
        int estrelas = obterPrimeMatchEstrelasFase(fase);
        boolean liberada = fase <= faseLiberada;
        btn.setGravity(Gravity.CENTER);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextSize(13);
        btn.setText(liberada
                ? "Fase " + fase + "\n" + formatarPrimeMatchEstrelas(estrelas)
                : "Fase " + fase + "\n---");
        btn.setTextColor(liberada ? Color.WHITE : corTextoSecundario());
        btn.setAlpha(liberada ? 1f : 0.48f);
        int corBase = liberada
                ? primeMatchCorFase(fase)
                : (temaEscuro ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1"));
        btn.setBackground(criarPrimeMatchBadgePontuacao(corBase));
        btn.setOnClickListener(v -> {
            if (liberada) {
                iniciarPrimeMatchFase(fase);
            } else {
                Toast.makeText(this, "Conclua fases anteriores para liberar esta.", Toast.LENGTH_SHORT).show();
            }
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dpUi(78);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dpUi(4), dpUi(4), dpUi(4), dpUi(4));
        btn.setLayoutParams(lp);
        return btn;
    }

    private int primeMatchCorFase(int fase) {
        int[] cores = new int[]{
                Color.parseColor("#2D9CDB"),
                Color.parseColor("#2FBA7C"),
                Color.parseColor("#F2A541"),
                Color.parseColor("#D65087"),
                Color.parseColor("#8D6AD9")
        };
        return cores[Math.abs(fase - 1) % cores.length];
    }

    private int calcularPrimeMatchMetaFase(int fase) {
        return matchLevelTargetNativo(fase);
    }

    private int obterPrimeMatchFaseLiberada() {
        return Math.max(1, Math.min(
                PRIME_MATCH_MAX_LEVELS,
                getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE)
                        .getInt(KEY_PM_UNLOCKED_LEVEL, 1)
        ));
    }

    private int obterPrimeMatchEstrelasFase(int fase) {
        return Math.max(0, Math.min(3,
                getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE)
                        .getInt(KEY_PM_LEVEL_STAR_PREFIX + fase, 0)
        ));
    }

    private void salvarPrimeMatchResultadoFase(int estrelas) {
        int estrelasFinais = Math.max(0, Math.min(3, estrelas));
        SharedPreferences prefs = getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE);
        int melhorAnterior = prefs.getInt(KEY_PM_LEVEL_STAR_PREFIX + primeMatchCurrentLevel, 0);
        int faseLiberada = obterPrimeMatchFaseLiberada();
        SharedPreferences.Editor editor = prefs.edit();
        if (estrelasFinais > melhorAnterior) {
            editor.putInt(KEY_PM_LEVEL_STAR_PREFIX + primeMatchCurrentLevel, estrelasFinais);
        }
        if (estrelasFinais > 0 && primeMatchCurrentLevel >= faseLiberada && primeMatchCurrentLevel < PRIME_MATCH_MAX_LEVELS) {
            editor.putInt(KEY_PM_UNLOCKED_LEVEL, primeMatchCurrentLevel + 1);
        }
        editor.apply();
    }

    private void iniciarPrimeMatchFase(int fase) {
        appHeader.setVisibility(View.GONE);
        contentContainer.removeAllViews();

        primeMatchValues = new int[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        primeMatchColors = new int[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        primeMatchCells = new TextView[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        primeMatchSelection.clear();
        limparPrimeMatchMarcaTexto();
        primeMatchHistory.clear();
        primeMatchHistoryCounter = 0;
        primeMatchScore = 0;
        primeMatchCurrentLevel = Math.max(1, Math.min(PRIME_MATCH_MAX_LEVELS, fase));
        primeMatchMoves = matchLevelMovesNativo(primeMatchCurrentLevel);
        primeMatchTarget = calcularPrimeMatchMetaFase(primeMatchCurrentLevel);
        primeMatchDragStart = -1;
        primeMatchDragTarget = -1;
        primeMatchMergeAnchor = -1;
        primeMatchMergeFallbackAnchor = -1;
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchPendingModifier = 0;
        primeMatchShieldArmed = false;
        primeMatchComboMultiplierArmed = false;
        primeMatchResolving = false;
        primeMatchCelebrationShown = false;
        primeMatchRewardGranted = false;
        primeMatchCelebrationOverlay = null;
        primeMatchActive = true;
        primeMatchAutoShufflePending = false;
        carregarPrimeMatchPoderes();
        preencherPrimeMatchTabuleiro();

        TextView titulo = new TextView(this);
        titulo.setText("Prime Match  -  Fase " + primeMatchCurrentLevel);
        titulo.setTextSize(22);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(corTextoPrincipal());
        titulo.setPadding(0, dpUi(10), 0, dpUi(4));
        contentContainer.addView(titulo);

        LinearLayout placar = new LinearLayout(this);
        placar.setOrientation(LinearLayout.HORIZONTAL);
        placar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams placarLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        placarLp.setMargins(0, 0, 0, dpUi(10));
        placar.setLayoutParams(placarLp);

        primeMatchScoreView = criarPrimeMatchMetrica();
        primeMatchMovesView = criarPrimeMatchMetrica();
        placar.addView(primeMatchScoreView);
        placar.addView(primeMatchMovesView);
        contentContainer.addView(placar);

        primeMatchStatusView = new TextView(this);
        primeMatchStatusView.setTextSize(14);
        primeMatchStatusView.setTextColor(corTextoPrincipal());
        primeMatchStatusView.setPadding(dpUi(12), dpUi(10), dpUi(12), dpUi(10));
        primeMatchStatusView.setBackground(fundoArredondado(corSuperficie(), 8, 1, corBorda()));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusLp.setMargins(0, 0, 0, dpUi(10));
        primeMatchStatusView.setLayoutParams(statusLp);
        contentContainer.addView(primeMatchStatusView);

        primeMatchHistoryView = new TextView(this);
        primeMatchHistoryView.setTextSize(12);
        primeMatchHistoryView.setTextColor(corTextoSecundario());
        primeMatchHistoryView.setLineSpacing(dpUi(2), 1.0f);
        primeMatchHistoryView.setPadding(dpUi(12), dpUi(9), dpUi(12), dpUi(9));
        primeMatchHistoryView.setBackground(fundoArredondado(corSuperficie(), 8, 1, corBorda()));
        LinearLayout.LayoutParams historyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        historyLp.setMargins(0, 0, 0, dpUi(10));
        primeMatchHistoryView.setLayoutParams(historyLp);
        atualizarPrimeMatchHistoricoView();

        Button btnHistorico = criarPrimeMatchBotaoPoder("Historico", Color.parseColor("#52637A"));
        btnHistorico.setText("Historico da fase");
        btnHistorico.setOnClickListener(v -> mostrarPrimeMatchHistoricoDialog());
        contentContainer.addView(btnHistorico);

        primeMatchStage = new FrameLayout(this);
        primeMatchStage.setClipChildren(false);
        primeMatchStage.setClipToPadding(false);
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stageLp.setMargins(0, 0, 0, dpUi(10));
        primeMatchStage.setLayoutParams(stageLp);

        primeMatchGrid = new GridLayout(this);
        primeMatchGrid.setColumnCount(PRIME_MATCH_COLS);
        primeMatchGrid.setRowCount(PRIME_MATCH_ROWS);
        primeMatchGrid.setUseDefaultMargins(false);
        primeMatchGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        primeMatchGrid.setPadding(dpUi(6), dpUi(6), dpUi(6), dpUi(6));
        primeMatchGrid.setBackground(criarPrimeMatchFundoTabuleiro());
        primeMatchGrid.setClickable(true);
        FrameLayout.LayoutParams gridLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        primeMatchGrid.setLayoutParams(gridLp);

        View.OnTouchListener primeMatchTouchListener = (view, event) -> {
            if (!primeMatchActive && !primeMatchResolving) {
                bloquearPrimeMatchRolagem(false);
                return false;
            }
            if (primeMatchResolving) {
                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    bloquearPrimeMatchRolagem(false);
                } else {
                    bloquearPrimeMatchRolagem(true);
                }
                return true;
            }
            int idx = obterPrimeMatchIndicePorEvento(view, event);
            if (primeMatchPowerMode != PRIME_MATCH_POWER_NONE) {
                return processarPrimeMatchToquePoder(idx, event.getActionMasked());
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    bloquearPrimeMatchRolagem(true);
                    iniciarPrimeMatchArrasto(idx);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    atualizarPrimeMatchArrasto(idx);
                    return true;
                case MotionEvent.ACTION_UP:
                    finalizarPrimeMatchArrasto(idx);
                    bloquearPrimeMatchRolagem(false);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    cancelarPrimeMatchArrasto();
                    bloquearPrimeMatchRolagem(false);
                    return true;
                default:
                    return true;
            }
        };

        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(17);
                cell.setTypeface(null, Typeface.BOLD);
                cell.setTextColor(Color.WHITE);
                cell.setShadowLayer(dpUi(2), 0, dpUi(1), Color.argb(185, 0, 0, 0));
                cell.setIncludeFontPadding(false);
                cell.setFocusable(false);
                cell.setClickable(true);
                cell.setOnTouchListener(primeMatchTouchListener);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(r),
                    GridLayout.spec(c, 1f)
                );
                lp.width = 0;
                lp.height = dpUi(54);
                lp.setMargins(dpUi(4), dpUi(4), dpUi(4), dpUi(4));
                cell.setLayoutParams(lp);
                primeMatchCells[r][c] = cell;
                primeMatchGrid.addView(cell);
            }
        }

        primeMatchGrid.setOnTouchListener(primeMatchTouchListener);
        primeMatchGrid.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                  oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            if (width <= 0 || width == oldRight - oldLeft) return;
            int size = Math.max(dpUi(28),
                (width - view.getPaddingLeft() - view.getPaddingRight()) / PRIME_MATCH_COLS - dpUi(8));
            GridLayout grid = (GridLayout) view;
            for (int i = 0; i < grid.getChildCount(); i++) {
                View cell = grid.getChildAt(i);
                ViewGroup.LayoutParams params = cell.getLayoutParams();
                params.height = size;
                cell.setLayoutParams(params);
            }
        });
        primeMatchStage.addView(primeMatchGrid);
        contentContainer.addView(primeMatchStage);

        criarPrimeMatchBarraPoderes();

        Button btnReiniciar = criarBotaoModerno(
            "Rejogar fase " + primeMatchCurrentLevel,
            Color.parseColor("#2F855A"),
            Color.parseColor("#68A06D")
        );
        btnReiniciar.setOnClickListener(v -> iniciarPrimeMatchFase(primeMatchCurrentLevel));
        contentContainer.addView(btnReiniciar);

        Button btnMapa = criarBotaoModerno(
                "Mapa de fases",
                Color.parseColor("#256D85"),
                Color.parseColor("#5EA7B8")
        );
        btnMapa.setOnClickListener(v -> criarInterfacePrimeMatchMapa());
        contentContainer.addView(btnMapa);

        atualizarPrimeMatchPlacar();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText("Meta: acumule " + primeMatchTarget + " pontos. Tente criar primos cada vez maiores.");
        verificarPrimeMatchJogadasPossiveis();
    }

    private void abrirArcade(boolean voo) {
        menuScrollView.setVisibility(View.GONE);
        appHeader.setVisibility(View.GONE);

        LinearLayout host = new LinearLayout(this);
        host.setOrientation(LinearLayout.VERTICAL);
        host.setBackgroundColor(Color.parseColor("#101C28"));
        host.setPadding(0, dpUi(4), 0, 0);
        arcadeHost = host;

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        Button voltar = arcadeButton("\u2190", "Voltar ao menu");
        voltar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.addView(voltar, new LinearLayout.LayoutParams(dpUi(48), dpUi(48)));
        TextView title = new TextView(this);
        title.setText(voo ? "Resgate Espacial" : "Corrida Prima");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dpUi(48), 1f));
        Button fases = arcadeButton("\u2637", "Fases");
        toolbar.addView(fases, new LinearLayout.LayoutParams(dpUi(44), dpUi(48)));
        Button reiniciar = arcadeButton("\u21bb", "Reiniciar");
        toolbar.addView(reiniciar, new LinearLayout.LayoutParams(dpUi(48), dpUi(48)));
        host.addView(toolbar);

        FrameLayout board = new FrameLayout(this);
        board.setClipChildren(true);
        board.setClipToPadding(true);
        View jogo;
        if (voo) {
            PrimeShooterView view = new PrimeShooterView(this);
            primeShooterView = view;
            jogo = view;
            view.phaseReady = () -> mostrarResultadoArcade(board, view, view.phase, view.pontuacao,
                    view.lives, view::avancarFase, view::repetirFase);
            reiniciar.setOnClickListener(v -> {
                limparResultadoArcade(board, view);
                view.repetirFase();
            });
            fases.setOnClickListener(v -> {
                view.pausar();
                mostrarFasesArcade(true, index -> {
                    limparResultadoArcade(board, view);
                    view.iniciarFaseEscolhida(index);
                });
            });
        } else {
            PrimeRunnerView view = new PrimeRunnerView(this);
            primeRunnerView = view;
            jogo = view;
            view.phaseReady = () -> mostrarResultadoArcade(board, view, view.phase, view.pontuacao,
                    -1, view::avancarFase, view::repetirFase);
            reiniciar.setOnClickListener(v -> {
                limparResultadoArcade(board, view);
                view.repetirFase();
            });
            fases.setOnClickListener(v -> {
                view.pausar();
                mostrarFasesArcade(false, index -> {
                    limparResultadoArcade(board, view);
                    view.iniciarFaseEscolhida(index);
                });
            });
        }
        board.addView(jogo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        host.addView(board, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        rootMainLayout.addView(host, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private static final class ArcadePhase {
        static final int PASSED = 1, CRASHED = 3;
        final boolean flight;
        int duration;
        int[] flightStage;
        int index, startingScore, elapsed, outcome;
        long exitStart = -1;
        boolean shown;

        ArcadePhase(boolean flight) {
            this.flight = flight;
            duration = arcadePhaseDuration(flight);
        }

        void reset(int index, int startingScore) {
            this.index = index;
            this.startingScore = startingScore;
            if (flight) {
                flightStage = arcadeFlightStageNativo(index);
                duration = flightStage[0];
            }
            exitStart = -1;
            shown = false;
            elapsed = 0;
            outcome = 0;
        }

        int points(int total) { return Math.max(0, total - startingScore); }
        int pace() { return Math.min(25, index) * (flight ? 600 : 40); }
        int remainingSeconds() { return (Math.max(0, duration - elapsed) + 999) / 1000; }

        boolean conclude(long now, int total, boolean crashed, SharedPreferences prefs) {
            return concludeWith(now, total, arcadePhaseOutcome(index, flight, elapsed, points(total), crashed), prefs);
        }

        boolean concludeWith(long now, int total, int result, SharedPreferences prefs) {
            if (outcome != 0) return false;
            outcome = result;
            if (outcome == 0) return false;
            exitStart = now;
            SharedPreferences.Editor edit = prefs.edit();
            if ((flight || outcome != CRASHED) && points(total) > prefs.getInt("timed_best_" + index, -1)) {
                edit.putInt("timed_best_" + index, points(total));
            }
            if (outcome == PASSED) {
                int next = flight ? Math.min(BONUS_STAGE, index + 1) : index + 1;
                edit.putInt(flight ? "campaign_unlocked" : "timed_unlocked", Math.max(next, arcadeUnlocked(prefs, flight)));
            }
            edit.apply();
            return true;
        }

        float exitProgress(long now) {
            if (exitStart < 0) return 0;
            float t = Math.max(0, Math.min(1, (now - exitStart) / 850f));
            return t * t * (3 - 2 * t);
        }
    }

    private SharedPreferences arcadePrefs(boolean flight) {
        return getSharedPreferences(flight ? PREFS_PRIME_SHOOTER : PREFS_PRIME_RUNNER, MODE_PRIVATE);
    }

    private static int arcadeUnlocked(SharedPreferences prefs, boolean flight) {
        int unlocked = Math.max(0, prefs.getInt(flight ? "campaign_unlocked" : "timed_unlocked", 0));
        return flight ? Math.min(BONUS_STAGE, unlocked) : unlocked;
    }

    private void mostrarFasesArcade(boolean flight, java.util.function.IntConsumer select) {
        SharedPreferences prefs = arcadePrefs(flight);
        String[] phases = new String[arcadeUnlocked(prefs, flight) + 1];
        for (int i = 0; i < phases.length; i++) {
            int best = prefs.getInt("timed_best_" + i, -1);
            String title = flight && i == EARTH_STAGE ? "M51 · Defesa da Terra"
                : flight && i == BONUS_STAGE ? "M52 · Missão bônus" : "Fase " + (i + 1);
            phases[i] = title + (best < 0 ? "" : "  •  Recorde " + formatarPontuacaoArcade(best));
        }
        new AlertDialog.Builder(this).setTitle("Fases")
                .setItems(phases, (dialog, index) -> select.accept(index))
                .setNegativeButton("Fechar", null).show();
    }

    private void limparResultadoArcade(FrameLayout board, View game) {
        if (board.getChildCount() > 1) board.removeViewAt(1);
        game.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    private void mostrarResultadoArcade(FrameLayout board, View game, ArcadePhase phase, int score,
                                       int lives, Runnable continuar, Runnable repetir) {
        if (!board.isAttachedToWindow() || board.getChildCount() != 1) return;
        game.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(0xD009131D);
        dim.setClickable(true);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dpUi(20), dpUi(20), dpUi(20), dpUi(16));
        panel.setBackground(fundoArredondado(0xFF182731, 8, 1, 0xFF42615F));
        FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        centered.setMargins(dpUi(20), dpUi(12), dpUi(20), dpUi(12));
        dim.addView(panel, centered);
        TextView heading = new TextView(this);
        boolean passed = phase.outcome == ArcadePhase.PASSED;
        heading.setText("Fase " + (phase.index + 1) + (passed ? " concluída" : " não superada"));
        heading.setTextColor(passed ? 0xFFFFDB91 : 0xFFFFB9A5);
        heading.setTextSize(22);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) heading.setAccessibilityHeading(true);
        panel.addView(heading);
        TextView stats = new TextView(this);
        int record = arcadePrefs(phase.flight).getInt("timed_best_" + phase.index, -1);
        stats.setText((phase.outcome == ArcadePhase.CRASHED ? "Percurso interrompido\n" : "")
                + "Pontos na fase: " + formatarPontuacaoArcade(phase.points(score))
                + (phase.flight ? "" : "\nMínimo: " + formatarPontuacaoArcade(arcadePhaseTarget(phase.index, false)))
                + "\nRecorde da fase: " + (record >= 0 ? formatarPontuacaoArcade(record) : "—")
                + (lives >= 0 ? "\nVidas: " + lives : ""));
        stats.setTextColor(Color.WHITE);
        stats.setTextSize(16);
        stats.setPadding(0, dpUi(12), 0, dpUi(14));
        panel.addView(stats);
        if (passed && (!phase.flight || phase.index < BONUS_STAGE)) {
            Button next = criarBotaoModerno(phase.flight && phase.index == EARTH_STAGE ? "Missão bônus" : "Próxima fase", 0xFF137769, 0xFF285B76);
            panel.addView(next, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpUi(48)));
            next.setOnClickListener(v -> {
                limparResultadoArcade(board, game);
                continuar.run();
            });
        }
        Button retry = criarBotaoModerno("Rejogar fase", 0xFF2C4553, 0xFF263A48);
        panel.addView(retry, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpUi(48)));
        retry.setOnClickListener(v -> {
            limparResultadoArcade(board, game);
            repetir.run();
        });
        board.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dim.setAlpha(0f);
        dim.animate().alpha(1f).setDuration(200).start();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private Button arcadeButton(String label, String description) {
        Button button = new Button(this, null, android.R.attr.borderlessButtonStyle);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(19);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setContentDescription(description);
        return button;
    }

    private android.graphics.Bitmap[] carregarVeiculos(String... nomes) {
        android.graphics.Bitmap[] imagens = new android.graphics.Bitmap[nomes.length];
        for (int i = 0; i < nomes.length; i++) {
            imagens[i] = carregarImagemArcade(nomes[i] + ".png");
        }
        return imagens;
    }

    private android.graphics.Bitmap carregarImagemArcade(String nome) {
        try (java.io.InputStream stream = getAssets().open("arcade/" + nome)) {
            return android.graphics.BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            Log.e(TAG, "Imagem do jogo indisponivel: " + nome, e);
            return null;
        }
    }

    private boolean desenharFotoArcade(Canvas canvas, android.graphics.Bitmap foto, int w, int h,
                                      int tier, Paint paint) {
        if (foto == null) return false;
        float scale = Math.max((float) w / foto.getWidth(), (float) h / foto.getHeight());
        float halfW = foto.getWidth() * scale / 2f, halfH = foto.getHeight() * scale / 2f;
        paint.setColor(Color.WHITE);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(foto, null, new RectF(w / 2f - halfW, h / 2f - halfH,
            w / 2f + halfW, h / 2f + halfH), paint);
        paint.setColor(tier == 0 ? 0x5910202C : tier == 1 ? 0x60332135 : 0x6011293D);
        canvas.drawRect(0, 0, w, h, paint);
        return true;
    }

    private boolean desenharVeiculo(Canvas canvas, android.graphics.Bitmap imagem, RectF area, Paint paint) {
        if (imagem == null) return false;
        float scale = Math.min(area.width() / imagem.getWidth(), area.height() / imagem.getHeight());
        float w = imagem.getWidth() * scale / 2f;
        float h = imagem.getHeight() * scale;
        float cx = area.centerX();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(imagem, null, new RectF(cx - w, area.bottom - h, cx + w, area.bottom), paint);
        return true;
    }

    private void desenharEscapeArcade(Canvas canvas, float x, float y, long now, boolean voo, Paint paint) {
        for (int i = 0; i < 6; i++) {
            float fase = ((now % 500L) / 500f + i / 6f) % 1f;
            paint.setColor(withAlpha(voo ? 0xFF8FEDFF : 0xFFD6DDE0, (int) ((1f - fase) * 85f)));
            float px = voo ? x + (i % 3 - 1) * fase * dpUi(12) : x - fase * dpUi(40);
            float py = voo ? y + fase * dpUi(31) : y - fase * dpUi(14);
            canvas.drawCircle(px, py, dpUi(3) + fase * dpUi(6), paint);
        }
    }

    private static final class ArcadeRules {
        private static final int[] DATA = arcadeRulesNativo();
        static final int MAX_FRAME_MS = DATA[0], START_MS = DATA[1], SHOT_MS = DATA[2];
        static final int SHIELD_MS = DATA[3], HIT_GRACE_MS = DATA[4], RUNNER_X = DATA[5];
        static final int FLOOR_Y = DATA[6], RUNNER_CENTER = DATA[7], SHIP_Y = DATA[8];
        static final int SHIP_MARGIN = DATA[9], CONTROL_HEIGHT = DATA[10], SPAWN_MARGIN = DATA[11];
        static final int SHOT_OFFSET = DATA[12], SHOT_SPEED = DATA[13], LIVES = DATA[14], LANE_MAX = DATA[15];
        static final int RUNNER_BOX = DATA[16], RUNNER_NUMBER = DATA[17], SHIP_BOX = DATA[18];
        static final int SPACE_PRIME = DATA[19], SPACE_COMPOSITE = DATA[20], SHOT_BOX = DATA[21];
        static final int RESCUE = DATA[22], SHIELD = DATA[23], DAMAGE = DATA[24];
    }

    private void desenharPausaArcade(Canvas canvas, int w, int h, boolean retomar, Paint paint) {
        paint.setColor(0x800B1824);
        canvas.drawRect(0, h / 2f - dpUi(25), w, h / 2f + dpUi(25), paint);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dpUi(18));
        paint.setColor(Color.WHITE);
        canvas.drawText(retomar ? "Toque para continuar" : "Toque para começar", w / 2f, h / 2f + dpUi(6), paint);
    }

    private String formatarPontuacaoArcade(int valor) {
        if (valor >= 1000000) {
            return String.format(Locale.getDefault(), "%.1fM", valor / 1000000f);
        }
        if (valor >= 10000) {
            return String.format(Locale.getDefault(), "%.1fk", valor / 1000f);
        }
        return String.valueOf(valor);
    }

    private void criarPrimeMatchBarraPoderes() {
        primeMatchPowersTitleView = new TextView(this);
        primeMatchPowersTitleView.setText("Poderes desta fase");
        primeMatchPowersTitleView.setTextSize(12);
        primeMatchPowersTitleView.setTypeface(null, Typeface.BOLD);
        primeMatchPowersTitleView.setTextColor(corTextoSecundario());
        primeMatchPowersTitleView.setPadding(0, 0, 0, dpUi(4));
        contentContainer.addView(primeMatchPowersTitleView);

        primeMatchInventoryView = new TextView(this);
        primeMatchInventoryView.setTextSize(11);
        primeMatchInventoryView.setTextColor(corTextoSecundario());
        primeMatchInventoryView.setLineSpacing(dpUi(2), 1.0f);
        primeMatchInventoryView.setPadding(dpUi(10), dpUi(7), dpUi(10), dpUi(7));
        primeMatchInventoryView.setBackground(fundoArredondado(corSuperficieElevada(), 8, 1, corBorda()));
        LinearLayout.LayoutParams inventoryLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inventoryLp.setMargins(0, 0, 0, dpUi(7));
        primeMatchInventoryView.setLayoutParams(inventoryLp);
        contentContainer.addView(primeMatchInventoryView);

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollLp.setMargins(0, 0, 0, dpUi(10));
        scroller.setLayoutParams(scrollLp);

        primeMatchPowersRow = new LinearLayout(this);
        primeMatchPowersRow.setOrientation(LinearLayout.HORIZONTAL);
        primeMatchPowersRow.setGravity(Gravity.CENTER_VERTICAL);
        primeMatchPowersRow.setPadding(0, 0, dpUi(4), 0);

        primeMatchHammerButton = null;
        primeMatchPlus1Button = null;
        primeMatchPlus2Button = null;
        primeMatchPlus3Button = null;
        primeMatchReshuffleButton = null;
        primeMatchFreeSwapButton = null;
        primeMatchColorCopyButton = null;
        primeMatchPolishUpButton = null;
        primeMatchPolishDownButton = null;
        primeMatchRowRayButton = null;
        primeMatchColRayButton = null;
        primeMatchHintButton = null;
        primeMatchShieldButton = null;
        primeMatchComboMultButton = null;
        primeMatchSoundButton = null;

        primeMatchSoundButton = adicionarPrimeMatchBotaoPoder("Som", Color.parseColor("#64748B"), v -> alternarPrimeMatchSom());
        adicionarPrimeMatchBotoesAtivosDaFase();
        scroller.addView(primeMatchPowersRow);
        contentContainer.addView(scroller);
        atualizarPrimeMatchInventarioView();
        atualizarPrimeMatchBotoesPoder();
    }

    private void adicionarPrimeMatchBotoesAtivosDaFase() {
        if (primeMatchPoderAtivoNaFase(KEY_PM_HAMMER)) {
            primeMatchHammerButton = adicionarPrimeMatchBotaoPoder("Martelo", Color.parseColor("#B96E31"), v -> alternarPrimeMatchMartelo());
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_PLUS_1)) {
            primeMatchPlus1Button = adicionarPrimeMatchBotaoPoder("+1", Color.parseColor("#2F9A70"), v -> alternarPrimeMatchAjuste(1));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_PLUS_2)) {
            primeMatchPlus2Button = adicionarPrimeMatchBotaoPoder("+2", Color.parseColor("#2586C8"), v -> alternarPrimeMatchAjuste(2));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_PLUS_3)) {
            primeMatchPlus3Button = adicionarPrimeMatchBotaoPoder("+3", Color.parseColor("#8A67D6"), v -> alternarPrimeMatchAjuste(3));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_RESHUFFLE)) {
            primeMatchReshuffleButton = adicionarPrimeMatchBotaoPoder("Reorganizar", Color.parseColor("#D65087"), v -> usarPrimeMatchReorganizar());
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_FREE_SWAP)) {
            primeMatchFreeSwapButton = adicionarPrimeMatchBotaoPoder("Troca livre", Color.parseColor("#2B6CB0"), v -> alternarPrimeMatchPoder(PRIME_MATCH_POWER_FREE_SWAP));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_COLOR_COPY)) {
            primeMatchColorCopyButton = adicionarPrimeMatchBotaoPoder("Coringa", Color.parseColor("#0F9F8F"), v -> alternarPrimeMatchPoder(PRIME_MATCH_POWER_COLOR_COPY));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_POLISH_UP)) {
            primeMatchPolishUpButton = adicionarPrimeMatchBotaoPoder("Lapidar +1", Color.parseColor("#6B8E23"), v -> alternarPrimeMatchPoder(PRIME_MATCH_POWER_POLISH_UP));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_POLISH_DOWN)) {
            primeMatchPolishDownButton = adicionarPrimeMatchBotaoPoder("Lapidar -1", Color.parseColor("#805AD5"), v -> alternarPrimeMatchPoder(PRIME_MATCH_POWER_POLISH_DOWN));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_ROW_RAY)) {
            primeMatchRowRayButton = adicionarPrimeMatchBotaoPoder("Raio linha", Color.parseColor("#DD6B20"), v -> alternarPrimeMatchPoder(PRIME_MATCH_POWER_ROW_RAY));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_COL_RAY)) {
            primeMatchColRayButton = adicionarPrimeMatchBotaoPoder("Raio coluna", Color.parseColor("#C53030"), v -> alternarPrimeMatchPoder(PRIME_MATCH_POWER_COL_RAY));
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_HINT)) {
            primeMatchHintButton = adicionarPrimeMatchBotaoPoder("Dica prima", Color.parseColor("#319795"), v -> usarPrimeMatchDicaPrima());
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_SHIELD)) {
            primeMatchShieldButton = adicionarPrimeMatchBotaoPoder("Escudo", Color.parseColor("#4A5568"), v -> alternarPrimeMatchEscudo());
        }
        if (primeMatchPoderAtivoNaFase(KEY_PM_COMBO_MULT)) {
            primeMatchComboMultButton = adicionarPrimeMatchBotaoPoder("Combo x2", Color.parseColor("#B83280"), v -> alternarPrimeMatchMultiplicador());
        }
    }

    private Button adicionarPrimeMatchBotaoPoder(String texto, int corBase, View.OnClickListener listener) {
        Button btn = criarPrimeMatchBotaoPoder(texto, corBase);
        btn.setOnClickListener(listener);
        primeMatchPowersRow.addView(btn);
        return btn;
    }

    private boolean primeMatchPoderAtivoNaFase(String poder) {
        int disponiveis = matchLevelPowersNativo(primeMatchCurrentLevel);
        for (int i = 0; i < PRIME_MATCH_POWER_KEYS.length; i++) {
            if (PRIME_MATCH_POWER_KEYS[i].equals(poder)) return (disponiveis & (1 << i)) != 0;
        }
        return false;
    }

    private Button criarPrimeMatchBotaoPoder(String texto, int corBase) {
        Button btn = new Button(this);
        btn.setTag("ppf_prime_power");
        btn.setAllCaps(false);
        btn.setText(texto);
        btn.setTextSize(12);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setMinHeight(dpUi(42));
        btn.setMinWidth(dpUi(76));
        btn.setPadding(dpUi(10), dpUi(7), dpUi(10), dpUi(7));
        btn.setBackground(criarPrimeMatchBadgePontuacao(corBase));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, dpUi(7), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private TextView criarPrimeMatchMetrica() {
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(corTextoPrincipal());
        tv.setPadding(dpUi(10), dpUi(10), dpUi(10), dpUi(10));
        tv.setBackground(fundoArredondado(corSuperficie(), 8, 1, corBorda()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        lp.setMargins(dpUi(3), 0, dpUi(3), 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int[] primeMatchPaleta() {
        return new int[]{
            Color.parseColor("#2D9CDB"),
            Color.parseColor("#2FBA7C"),
            Color.parseColor("#F2A541"),
            Color.parseColor("#D65087"),
            Color.parseColor("#8D6AD9")
        };
    }

    private boolean primeMatchEhCorPreta(int cor) {
        return cor == PRIME_MATCH_COR_PRETA;
    }

    private boolean primeMatchCoresConectam(int corA, int corB) {
        return corA == corB || primeMatchEhCorPreta(corA) || primeMatchEhCorPreta(corB);
    }

    private void carregarPrimeMatchPoderes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE);
        primeMatchHammerCount = prefs.getInt(KEY_PM_HAMMER, 0);
        primeMatchPlus1Count = prefs.getInt(KEY_PM_PLUS_1, 0);
        primeMatchPlus2Count = prefs.getInt(KEY_PM_PLUS_2, 0);
        primeMatchPlus3Count = prefs.getInt(KEY_PM_PLUS_3, 0);
        primeMatchReshuffleCount = prefs.getInt(KEY_PM_RESHUFFLE, 0);
        primeMatchFreeSwapCount = prefs.getInt(KEY_PM_FREE_SWAP, 0);
        primeMatchColorCopyCount = prefs.getInt(KEY_PM_COLOR_COPY, 0);
        primeMatchPolishUpCount = prefs.getInt(KEY_PM_POLISH_UP, 0);
        primeMatchPolishDownCount = prefs.getInt(KEY_PM_POLISH_DOWN, 0);
        primeMatchRowRayCount = prefs.getInt(KEY_PM_ROW_RAY, 0);
        primeMatchColRayCount = prefs.getInt(KEY_PM_COL_RAY, 0);
        primeMatchHintCount = prefs.getInt(KEY_PM_HINT, 0);
        primeMatchShieldCount = prefs.getInt(KEY_PM_SHIELD, 0);
        primeMatchComboMultCount = prefs.getInt(KEY_PM_COMBO_MULT, 0);
        primeMatchWins = prefs.getInt(KEY_PM_WINS, 0);
        primeMatchBestScore = prefs.getInt(KEY_PM_BEST_SCORE, 0);
        primeMatchSoundEnabled = prefs.getBoolean(KEY_PM_SOUND_ENABLED, true);
    }

    private void salvarPrimeMatchPoderes() {
        getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE)
                .edit()
                .putInt(KEY_PM_HAMMER, primeMatchHammerCount)
                .putInt(KEY_PM_PLUS_1, primeMatchPlus1Count)
                .putInt(KEY_PM_PLUS_2, primeMatchPlus2Count)
                .putInt(KEY_PM_PLUS_3, primeMatchPlus3Count)
                .putInt(KEY_PM_RESHUFFLE, primeMatchReshuffleCount)
                .putInt(KEY_PM_FREE_SWAP, primeMatchFreeSwapCount)
                .putInt(KEY_PM_COLOR_COPY, primeMatchColorCopyCount)
                .putInt(KEY_PM_POLISH_UP, primeMatchPolishUpCount)
                .putInt(KEY_PM_POLISH_DOWN, primeMatchPolishDownCount)
                .putInt(KEY_PM_ROW_RAY, primeMatchRowRayCount)
                .putInt(KEY_PM_COL_RAY, primeMatchColRayCount)
                .putInt(KEY_PM_HINT, primeMatchHintCount)
                .putInt(KEY_PM_SHIELD, primeMatchShieldCount)
                .putInt(KEY_PM_COMBO_MULT, primeMatchComboMultCount)
                .putInt(KEY_PM_WINS, primeMatchWins)
                .putInt(KEY_PM_BEST_SCORE, primeMatchBestScore)
                .apply();
    }

    private void inicializarPrimeMatchAudio() {
        primeMatchSoundEnabled = getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE)
                .getBoolean(KEY_PM_SOUND_ENABLED, true);
        if (primeMatchSoundPool != null) {
            return;
        }

        AudioAttributes atributos = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        primeMatchSoundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(atributos)
                .build();
        primeMatchSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                primeMatchSoundReady = true;
            }
        });

        primeMatchSoundTap = primeMatchSoundPool.load(this, R.raw.ppf_pm_tap, 1);
        primeMatchSoundPrime = primeMatchSoundPool.load(this, R.raw.ppf_pm_prime, 1);
        primeMatchSoundComposite = primeMatchSoundPool.load(this, R.raw.ppf_pm_composite, 1);
        primeMatchSoundCombo = primeMatchSoundPool.load(this, R.raw.ppf_pm_combo, 1);
        primeMatchSoundPower = primeMatchSoundPool.load(this, R.raw.ppf_pm_power, 1);
        primeMatchSoundBlack = primeMatchSoundPool.load(this, R.raw.ppf_pm_black, 1);
        primeMatchSoundVictory = primeMatchSoundPool.load(this, R.raw.ppf_pm_victory, 1);
    }

    private void liberarPrimeMatchAudio() {
        if (primeMatchSoundPool != null) {
            primeMatchSoundPool.release();
            primeMatchSoundPool = null;
        }
        primeMatchSoundReady = false;
    }

    private void tocarPrimeMatchSom(int soundId) {
        if (!primeMatchSoundEnabled
                || !primeMatchSoundReady
                || primeMatchSoundPool == null
                || soundId == 0) {
            return;
        }
        primeMatchSoundPool.play(soundId, 0.58f, 0.58f, 1, 0, 1.0f);
    }

    private void tocarPrimeMatchSomResultados(List<PrimeMatchResultadoGrupo> resultados, int combo) {
        boolean temPrimo = false;
        boolean temPreta = false;
        boolean temEspecial = false;
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            if (resultado.primo) {
                temPrimo = true;
            }
            if (resultado.grupo.pretas > 0) {
                temPreta = true;
            }
            if (resultado.especialTipo != PRIME_MATCH_SPECIAL_NONE) {
                temEspecial = true;
            }
        }

        if (temEspecial || (temPrimo && temPreta)) {
            tocarPrimeMatchSom(primeMatchSoundBlack);
        } else if (temPrimo && (combo > 1 || resultados.size() > 1)) {
            tocarPrimeMatchSom(primeMatchSoundCombo);
        } else if (temPrimo) {
            tocarPrimeMatchSom(primeMatchSoundPrime);
        } else {
            tocarPrimeMatchSom(primeMatchSoundComposite);
        }
    }

    private void alternarPrimeMatchSom() {
        primeMatchSoundEnabled = !primeMatchSoundEnabled;
        getSharedPreferences(PREFS_PRIME_MATCH, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PM_SOUND_ENABLED, primeMatchSoundEnabled)
                .apply();
        atualizarPrimeMatchBotoesPoder();
        if (primeMatchSoundEnabled) {
            tocarPrimeMatchSom(primeMatchSoundTap);
        }
    }

    private int totalPrimeMatchPoderesGuardados() {
        return primeMatchHammerCount
                + primeMatchPlus1Count
                + primeMatchPlus2Count
                + primeMatchPlus3Count
                + primeMatchReshuffleCount
                + primeMatchFreeSwapCount
                + primeMatchColorCopyCount
                + primeMatchPolishUpCount
                + primeMatchPolishDownCount
                + primeMatchRowRayCount
                + primeMatchColRayCount
                + primeMatchHintCount
                + primeMatchShieldCount
                + primeMatchComboMultCount;
    }

    private void atualizarPrimeMatchInventarioView() {
        int total = totalPrimeMatchPoderesGuardados();
        if (primeMatchPowersTitleView != null) {
            primeMatchPowersTitleView.setText(total > 0
                    ? "Poderes desta fase • " + total + " guardado(s) na carteira"
                    : "Poderes desta fase");
        }
        if (primeMatchInventoryView != null) {
            primeMatchInventoryView.setText(formatarPrimeMatchInventarioResumo());
            primeMatchInventoryView.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private String formatarPrimeMatchInventarioResumo() {
        StringBuilder sb = new StringBuilder();
        adicionarPrimeMatchItemInventario(sb, "Martelo", primeMatchHammerCount);
        adicionarPrimeMatchItemInventario(sb, "+1", primeMatchPlus1Count);
        adicionarPrimeMatchItemInventario(sb, "+2", primeMatchPlus2Count);
        adicionarPrimeMatchItemInventario(sb, "+3", primeMatchPlus3Count);
        adicionarPrimeMatchItemInventario(sb, "Reorganizar", primeMatchReshuffleCount);
        adicionarPrimeMatchItemInventario(sb, "Troca livre", primeMatchFreeSwapCount);
        adicionarPrimeMatchItemInventario(sb, "Coringa", primeMatchColorCopyCount);
        adicionarPrimeMatchItemInventario(sb, "Lapidar +1", primeMatchPolishUpCount);
        adicionarPrimeMatchItemInventario(sb, "Lapidar -1", primeMatchPolishDownCount);
        adicionarPrimeMatchItemInventario(sb, "Raio linha", primeMatchRowRayCount);
        adicionarPrimeMatchItemInventario(sb, "Raio coluna", primeMatchColRayCount);
        adicionarPrimeMatchItemInventario(sb, "Dica", primeMatchHintCount);
        adicionarPrimeMatchItemInventario(sb, "Escudo", primeMatchShieldCount);
        adicionarPrimeMatchItemInventario(sb, "Combo x2", primeMatchComboMultCount);
        if (sb.length() == 0) {
            return "Carteira permanente: ganhe fases para acumular facilidades.";
        }
        return "Carteira permanente: " + sb;
    }

    private void adicionarPrimeMatchItemInventario(StringBuilder sb, String nome, int quantidade) {
        if (quantidade <= 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" • ");
        }
        sb.append(nome).append(" x").append(quantidade);
    }

    private void alternarPrimeMatchMartelo() {
        if (!primeMatchActive || primeMatchResolving || primeMatchHammerCount <= 0) {
            return;
        }
        primeMatchPendingModifier = 0;
        primeMatchPowerMode = primeMatchPowerMode == PRIME_MATCH_POWER_HAMMER
                ? PRIME_MATCH_POWER_NONE
                : PRIME_MATCH_POWER_HAMMER;
        primeMatchSelection.clear();
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText(primeMatchPowerMode == PRIME_MATCH_POWER_HAMMER
                ? "Martelo armado. Toque em uma bolinha para quebrar."
                : "Martelo cancelado.");
    }

    private void alternarPrimeMatchAjuste(int ajuste) {
        if (!primeMatchActive || primeMatchResolving || obterPrimeMatchAjusteCount(ajuste) <= 0) {
            return;
        }
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPendingModifier = primeMatchPendingModifier == ajuste ? 0 : ajuste;
        primeMatchSelection.clear();
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText(primeMatchPendingModifier > 0
                ? "Ajuste +" + ajuste + " armado para a próxima jogada válida."
                : "Ajuste cancelado.");
    }

    private int obterPrimeMatchAjusteCount(int ajuste) {
        if (ajuste == 1) {
            return primeMatchPlus1Count;
        }
        if (ajuste == 2) {
            return primeMatchPlus2Count;
        }
        if (ajuste == 3) {
            return primeMatchPlus3Count;
        }
        return 0;
    }

    private void consumirPrimeMatchAjustePendente() {
        if (primeMatchPendingModifier == 1 && primeMatchPlus1Count > 0) {
            primeMatchPlus1Count--;
        } else if (primeMatchPendingModifier == 2 && primeMatchPlus2Count > 0) {
            primeMatchPlus2Count--;
        } else if (primeMatchPendingModifier == 3 && primeMatchPlus3Count > 0) {
            primeMatchPlus3Count--;
        }
        primeMatchPendingModifier = 0;
        tocarPrimeMatchSom(primeMatchSoundPower);
        salvarPrimeMatchPoderes();
        atualizarPrimeMatchBotoesPoder();
    }

    private void usarPrimeMatchReorganizar() {
        if (!primeMatchActive || primeMatchResolving || primeMatchReshuffleCount <= 0) {
            return;
        }
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPendingModifier = 0;
        primeMatchMergeAnchor = -1;
        primeMatchMergeFallbackAnchor = -1;
        primeMatchSelection.clear();
        primeMatchReshuffleCount--;
        salvarPrimeMatchPoderes();
        atualizarPrimeMatchBotoesPoder();
        reorganizarPrimeMatchTabuleiroInteiro();
        atualizarPrimeMatchCelulas();
        animarPrimeMatchReorganizacao();
        primeMatchStatusView.setText("Tabuleiro inteiro reorganizado.");
    }

    private void alternarPrimeMatchPoder(int poder) {
        if (!primeMatchActive || primeMatchResolving || obterPrimeMatchPoderCount(poder) <= 0) {
            return;
        }
        primeMatchPendingModifier = 0;
        primeMatchPowerFirstSelection = -1;
        primeMatchSelection.clear();
        primeMatchPowerMode = primeMatchPowerMode == poder ? PRIME_MATCH_POWER_NONE : poder;
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText(primeMatchPowerMode == PRIME_MATCH_POWER_NONE
                ? "Poder cancelado."
                : primeMatchMensagemPoderArmado(poder));
    }

    private void alternarPrimeMatchEscudo() {
        if (!primeMatchActive || primeMatchResolving || primeMatchShieldCount <= 0) {
            return;
        }
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchPendingModifier = 0;
        primeMatchShieldArmed = false;
        primeMatchSelection.clear();
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText("Nesta regra, somas compostas não descontam. O escudo fica guardado.");
    }

    private void alternarPrimeMatchMultiplicador() {
        if (!primeMatchActive || primeMatchResolving || primeMatchComboMultCount <= 0) {
            return;
        }
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchPendingModifier = 0;
        primeMatchComboMultiplierArmed = !primeMatchComboMultiplierArmed;
        primeMatchSelection.clear();
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText(primeMatchComboMultiplierArmed
                ? "Combo x2 armado. O próximo combo primo em cadeia dobra."
                : "Multiplicador cancelado.");
    }

    private void usarPrimeMatchDicaPrima() {
        if (!primeMatchActive || primeMatchResolving || primeMatchHintCount <= 0) {
            return;
        }
        int[] dica = encontrarPrimeMatchDicaPrima();
        if (dica == null) {
            primeMatchStatusView.setText("Nenhuma troca prima clara foi encontrada nesta fase.");
            return;
        }

        primeMatchHintCount--;
        salvarPrimeMatchPoderes();
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchSelection.clear();
        primeMatchSelection.add(dica[0]);
        primeMatchSelection.add(dica[1]);
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText("Dica prima: troque as bolinhas destacadas. Soma provável: "
                + dica[2] + " com " + dica[3] + " bolinhas.");
    }

    private int[] encontrarPrimeMatchDicaPrima() {
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                int idx = r * PRIME_MATCH_COLS + c;
                int direita = c + 1 < PRIME_MATCH_COLS ? idx + 1 : -1;
                int abaixo = r + 1 < PRIME_MATCH_ROWS ? idx + PRIME_MATCH_COLS : -1;
                int[] dicaDireita = testarPrimeMatchDicaTroca(idx, direita);
                if (dicaDireita != null) {
                    return dicaDireita;
                }
                int[] dicaAbaixo = testarPrimeMatchDicaTroca(idx, abaixo);
                if (dicaAbaixo != null) {
                    return dicaAbaixo;
                }
            }
        }
        return null;
    }

    private int[] testarPrimeMatchDicaTroca(int primeiro, int segundo) {
        if (primeiro < 0 || segundo < 0) {
            return null;
        }
        trocarPrimeMatchPecas(primeiro, segundo);
        List<PrimeMatchGrupo> grupos = encontrarPrimeMatchGruposCombinacoes();
        trocarPrimeMatchPecas(primeiro, segundo);
        for (PrimeMatchGrupo grupo : grupos) {
            if (grupo.removidas >= PRIME_MATCH_MIN_CHAIN && ehPrimo(grupo.soma)) {
                return new int[]{primeiro, segundo, grupo.soma, grupo.removidas};
            }
        }
        return null;
    }

    private int obterPrimeMatchPoderCount(int poder) {
        switch (poder) {
            case PRIME_MATCH_POWER_HAMMER:
                return primeMatchHammerCount;
            case PRIME_MATCH_POWER_FREE_SWAP:
                return primeMatchFreeSwapCount;
            case PRIME_MATCH_POWER_COLOR_COPY:
                return primeMatchColorCopyCount;
            case PRIME_MATCH_POWER_POLISH_UP:
                return primeMatchPolishUpCount;
            case PRIME_MATCH_POWER_POLISH_DOWN:
                return primeMatchPolishDownCount;
            case PRIME_MATCH_POWER_ROW_RAY:
                return primeMatchRowRayCount;
            case PRIME_MATCH_POWER_COL_RAY:
                return primeMatchColRayCount;
            default:
                return 0;
        }
    }

    private String primeMatchMensagemPoderArmado(int poder) {
        switch (poder) {
            case PRIME_MATCH_POWER_HAMMER:
                return "Martelo armado. Toque em uma bolinha para quebrar.";
            case PRIME_MATCH_POWER_FREE_SWAP:
                return "Troca livre armada. Toque em duas bolinhas quaisquer.";
            case PRIME_MATCH_POWER_COLOR_COPY:
                return "Coringa armado. Toque na bolinha e depois na cor que deseja copiar.";
            case PRIME_MATCH_POWER_POLISH_UP:
                return "Lapidador +1 armado. Toque em uma bolinha para aumentar o valor.";
            case PRIME_MATCH_POWER_POLISH_DOWN:
                return "Lapidador -1 armado. Toque em uma bolinha para reduzir o valor.";
            case PRIME_MATCH_POWER_ROW_RAY:
                return "Raio de linha armado. Toque em uma bolinha da linha que será removida.";
            case PRIME_MATCH_POWER_COL_RAY:
                return "Raio de coluna armado. Toque em uma bolinha da coluna que será removida.";
            default:
                return "Poder armado.";
        }
    }

    private void atualizarPrimeMatchBotoesPoder() {
        if (primeMatchHammerButton == null) {
            // A fase pode não mostrar martelo, mas outros botões ainda podem estar visíveis.
        }
        configurarPrimeMatchBotaoPoder(primeMatchHammerButton, "Martelo", primeMatchHammerCount, primeMatchPowerMode == PRIME_MATCH_POWER_HAMMER, Color.parseColor("#B96E31"));
        configurarPrimeMatchBotaoPoder(primeMatchPlus1Button, "+1", primeMatchPlus1Count, primeMatchPendingModifier == 1, Color.parseColor("#2F9A70"));
        configurarPrimeMatchBotaoPoder(primeMatchPlus2Button, "+2", primeMatchPlus2Count, primeMatchPendingModifier == 2, Color.parseColor("#2586C8"));
        configurarPrimeMatchBotaoPoder(primeMatchPlus3Button, "+3", primeMatchPlus3Count, primeMatchPendingModifier == 3, Color.parseColor("#8A67D6"));
        configurarPrimeMatchBotaoPoder(primeMatchReshuffleButton, "Reorganizar", primeMatchReshuffleCount, false, Color.parseColor("#D65087"));
        configurarPrimeMatchBotaoPoder(primeMatchFreeSwapButton, "Troca livre", primeMatchFreeSwapCount, primeMatchPowerMode == PRIME_MATCH_POWER_FREE_SWAP, Color.parseColor("#2B6CB0"));
        configurarPrimeMatchBotaoPoder(primeMatchColorCopyButton, "Coringa", primeMatchColorCopyCount, primeMatchPowerMode == PRIME_MATCH_POWER_COLOR_COPY, Color.parseColor("#0F9F8F"));
        configurarPrimeMatchBotaoPoder(primeMatchPolishUpButton, "Lapidar +1", primeMatchPolishUpCount, primeMatchPowerMode == PRIME_MATCH_POWER_POLISH_UP, Color.parseColor("#6B8E23"));
        configurarPrimeMatchBotaoPoder(primeMatchPolishDownButton, "Lapidar -1", primeMatchPolishDownCount, primeMatchPowerMode == PRIME_MATCH_POWER_POLISH_DOWN, Color.parseColor("#805AD5"));
        configurarPrimeMatchBotaoPoder(primeMatchRowRayButton, "Raio linha", primeMatchRowRayCount, primeMatchPowerMode == PRIME_MATCH_POWER_ROW_RAY, Color.parseColor("#DD6B20"));
        configurarPrimeMatchBotaoPoder(primeMatchColRayButton, "Raio coluna", primeMatchColRayCount, primeMatchPowerMode == PRIME_MATCH_POWER_COL_RAY, Color.parseColor("#C53030"));
        configurarPrimeMatchBotaoPoder(primeMatchHintButton, "Dica prima", primeMatchHintCount, false, Color.parseColor("#319795"));
        configurarPrimeMatchBotaoPoder(primeMatchShieldButton, "Escudo", primeMatchShieldCount, primeMatchShieldArmed, Color.parseColor("#4A5568"));
        configurarPrimeMatchBotaoPoder(primeMatchComboMultButton, "Combo x2", primeMatchComboMultCount, primeMatchComboMultiplierArmed, Color.parseColor("#B83280"));
        configurarPrimeMatchBotaoSom();
        atualizarPrimeMatchInventarioView();
    }

    private void configurarPrimeMatchBotaoPoder(Button btn, String nome, int quantidade, boolean ativo, int corBase) {
        if (btn == null) {
            return;
        }
        btn.setText((ativo ? "✓ " : "") + nome + " x" + quantidade);
        btn.setEnabled(primeMatchActive && !primeMatchResolving && quantidade > 0);
        btn.setAlpha(quantidade > 0 ? 1f : 0.42f);
        btn.setBackground(criarPrimeMatchBadgePontuacao(ativo
                ? misturarPrimeMatchCor(corBase, Color.WHITE, 0.18f)
                : corBase));
    }

    private void configurarPrimeMatchBotaoSom() {
        if (primeMatchSoundButton == null) {
            return;
        }
        primeMatchSoundButton.setText(primeMatchSoundEnabled ? "Som ligado" : "Som desligado");
        primeMatchSoundButton.setEnabled(true);
        primeMatchSoundButton.setAlpha(primeMatchSoundEnabled ? 1f : 0.62f);
        int corBase = primeMatchSoundEnabled ? Color.parseColor("#256D85") : Color.parseColor("#64748B");
        primeMatchSoundButton.setBackground(criarPrimeMatchBadgePontuacao(corBase));
    }

    private Drawable criarPrimeMatchFundoTabuleiro() {
        int topo = temaEscuro ? Color.rgb(28, 38, 52) : Color.rgb(243, 247, 251);
        int base = temaEscuro ? Color.rgb(16, 24, 36) : Color.rgb(226, 235, 244);
        GradientDrawable fundo = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topo, base}
        );
        fundo.setCornerRadius(dpUi(8));
        fundo.setStroke(dpUi(1), corBorda());

        GradientDrawable brilho = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(Color.WHITE, temaEscuro ? 18 : 128), Color.TRANSPARENT}
        );
        brilho.setCornerRadius(dpUi(8));

        LayerDrawable camadas = new LayerDrawable(new Drawable[]{fundo, brilho});
        camadas.setLayerInset(1, dpUi(1), dpUi(1), dpUi(1), dpUi(16));
        return camadas;
    }

    private Drawable criarPrimeMatchBolinhaVidro(int cor, boolean selecionada) {
        int luz = misturarPrimeMatchCor(cor, Color.WHITE, 0.76f);
        int alto = misturarPrimeMatchCor(cor, Color.WHITE, 0.42f);
        int profundo = misturarPrimeMatchCor(cor, Color.BLACK, 0.40f);
        int borda = selecionada ? Color.WHITE : withAlpha(misturarPrimeMatchCor(cor, Color.BLACK, 0.22f), 210);

        GradientDrawable sombra = new GradientDrawable();
        sombra.setShape(GradientDrawable.OVAL);
        sombra.setColor(Color.argb(82, 0, 0, 0));

        GradientDrawable aro = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(Color.WHITE, 105), profundo}
        );
        aro.setShape(GradientDrawable.OVAL);
        aro.setStroke(dpUi(selecionada ? 2 : 1), borda);

        GradientDrawable corpo = new GradientDrawable();
        corpo.setShape(GradientDrawable.OVAL);
        corpo.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        corpo.setGradientCenter(0.34f, 0.24f);
        corpo.setGradientRadius(dpUi(46));
        corpo.setColors(new int[]{luz, alto, cor, profundo});
        corpo.setStroke(dpUi(1), withAlpha(Color.WHITE, selecionada ? 190 : 72));

        GradientDrawable reflexoPrincipal = new GradientDrawable();
        reflexoPrincipal.setShape(GradientDrawable.OVAL);
        reflexoPrincipal.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        reflexoPrincipal.setGradientCenter(0.35f, 0.28f);
        reflexoPrincipal.setGradientRadius(dpUi(22));
        reflexoPrincipal.setColors(new int[]{withAlpha(Color.WHITE, 220), withAlpha(Color.WHITE, 78), Color.TRANSPARENT});

        GradientDrawable reflexoFino = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(Color.WHITE, 145), withAlpha(Color.WHITE, 22)}
        );
        reflexoFino.setShape(GradientDrawable.OVAL);

        GradientDrawable sombraInterna = new GradientDrawable();
        sombraInterna.setShape(GradientDrawable.OVAL);
        sombraInterna.setColor(Color.TRANSPARENT);
        sombraInterna.setStroke(dpUi(2), withAlpha(Color.BLACK, 34));

        LayerDrawable camadas = new LayerDrawable(new Drawable[]{
                sombra,
                aro,
                corpo,
                sombraInterna,
                reflexoPrincipal,
                reflexoFino
        });
        camadas.setLayerInset(0, dpUi(3), dpUi(6), dpUi(3), 0);
        camadas.setLayerInset(1, 0, 0, 0, dpUi(1));
        camadas.setLayerInset(2, dpUi(2), dpUi(2), dpUi(2), dpUi(3));
        camadas.setLayerInset(3, dpUi(3), dpUi(3), dpUi(3), dpUi(4));
        camadas.setLayerInset(4, dpUi(8), dpUi(7), dpUi(28), dpUi(30));
        camadas.setLayerInset(5, dpUi(22), dpUi(8), dpUi(15), dpUi(37));
        return camadas;
    }

    private int misturarPrimeMatchCor(int corA, int corB, float pesoB) {
        float peso = Math.max(0f, Math.min(1f, pesoB));
        int r = Math.round(Color.red(corA) + (Color.red(corB) - Color.red(corA)) * peso);
        int g = Math.round(Color.green(corA) + (Color.green(corB) - Color.green(corA)) * peso);
        int b = Math.round(Color.blue(corA) + (Color.blue(corB) - Color.blue(corA)) * peso);
        return Color.rgb(r, g, b);
    }

    private void preencherPrimeMatchTabuleiro() {
        preencherPrimeMatchTabuleiroBasico();
        for (int tentativa = 0; tentativa < 12 && !primeMatchTemJogadaPossivel(); tentativa++) {
            reorganizarPrimeMatchTabuleiroInteiro();
        }
        if (!primeMatchTemJogadaPossivel()) {
            forcarPrimeMatchJogadaPossivel();
        }
    }

    private void preencherPrimeMatchTabuleiroBasico() {
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                gerarPrimeMatchPecaEvitandoTrinca(r, c);
            }
        }
    }

    private void gerarPrimeMatchPeca(int row, int col) {
        int totalCores = primeMatchPaleta().length;
        int melhorValor = 1;
        int melhorCor = primeMatchRandom.nextInt(totalCores);
        for (int tentativa = 0; tentativa < 28; tentativa++) {
            int valor = gerarPrimeMatchValorVariado();
            int cor = primeMatchRandom.nextInt(totalCores);
            melhorValor = valor;
            melhorCor = cor;
            if (!primeMatchValorCriaPadraoLocal(row, col, valor, cor)) {
                break;
            }
        }
        primeMatchValues[row][col] = melhorValor;
        primeMatchColors[row][col] = melhorCor;
    }

    private int gerarPrimeMatchValorVariado() {
        return matchDrawValueNativo(primeMatchRandom.nextInt(PRIME_MATCH_VALUE_TICKETS));
    }

    private boolean primeMatchValorCriaPadraoLocal(int row, int col, int valor, int cor) {
        int[] valores = new int[PRIME_MATCH_ROWS * PRIME_MATCH_COLS];
        int[] cores = new int[valores.length];
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            System.arraycopy(primeMatchValues[r], 0, valores, r * PRIME_MATCH_COLS, PRIME_MATCH_COLS);
            System.arraycopy(primeMatchColors[r], 0, cores, r * PRIME_MATCH_COLS, PRIME_MATCH_COLS);
        }
        return matchLocalPatternNativo(valores, cores, row * PRIME_MATCH_COLS + col, valor, cor);
    }

    private void gerarPrimeMatchPecaEvitandoTrinca(int row, int col) {
        for (int tentativa = 0; tentativa < 40; tentativa++) {
            gerarPrimeMatchPeca(row, col);
            if (!primeMatchFormaTrincaEm(row, col)) {
                return;
            }
        }
    }

    private boolean primeMatchFormaTrincaEm(int row, int col) {
        if (row < 0 || row >= PRIME_MATCH_ROWS || col < 0 || col >= PRIME_MATCH_COLS) {
            return false;
        }
        int cor = primeMatchColors[row][col];
        if (cor < 0 || primeMatchValues[row][col] == 0) {
            return false;
        }
        int horizontal = 1
                + contarPrimeMatchCor(row, col, 0, -1, cor)
                + contarPrimeMatchCor(row, col, 0, 1, cor);
        int vertical = 1
                + contarPrimeMatchCor(row, col, -1, 0, cor)
                + contarPrimeMatchCor(row, col, 1, 0, cor);
        return horizontal >= PRIME_MATCH_MIN_CHAIN || vertical >= PRIME_MATCH_MIN_CHAIN;
    }

    private int contarPrimeMatchCor(int row, int col, int dr, int dc, int cor) {
        int total = 0;
        int r = row + dr;
        int c = col + dc;
        while (r >= 0
                && r < PRIME_MATCH_ROWS
                && c >= 0
                && c < PRIME_MATCH_COLS
                && primeMatchColors[r][c] == cor
                && primeMatchValues[r][c] != 0) {
            total++;
            r += dr;
            c += dc;
        }
        return total;
    }

    private int obterPrimeMatchIndice(float x, float y) {
        if (primeMatchCells == null) {
            return -1;
        }
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                TextView cell = primeMatchCells[r][c];
                if (cell != null
                        && x >= cell.getLeft()
                        && x <= cell.getRight()
                        && y >= cell.getTop()
                        && y <= cell.getBottom()) {
                    return r * PRIME_MATCH_COLS + c;
                }
            }
        }
        return -1;
    }

    private int obterPrimeMatchIndicePorEvento(View origem, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (origem != null && origem != primeMatchGrid) {
            x += origem.getLeft();
            y += origem.getTop();
        }
        return obterPrimeMatchIndice(x, y);
    }

    private void bloquearPrimeMatchRolagem(boolean bloquear) {
        if (primeMatchGrid != null && primeMatchGrid.getParent() != null) {
            primeMatchGrid.getParent().requestDisallowInterceptTouchEvent(bloquear);
        }
        if (contentScrollView != null) {
            contentScrollView.requestDisallowInterceptTouchEvent(bloquear);
        }
    }

    private void iniciarPrimeMatchArrasto(int idx) {
        if (!primeMatchActive || primeMatchResolving || idx < 0) {
            return;
        }
        primeMatchDragStart = idx;
        primeMatchDragTarget = -1;
        primeMatchSelection.clear();
        primeMatchSelection.add(idx);
        tocarPrimeMatchSom(primeMatchSoundTap);
        primeMatchStatusView.setText("Arraste para uma bolinha vizinha.");
        atualizarPrimeMatchCelulas();
    }

    private void atualizarPrimeMatchArrasto(int idx) {
        if (!primeMatchActive || primeMatchResolving || primeMatchDragStart < 0 || idx < 0) {
            return;
        }
        if (idx == primeMatchDragStart) {
            primeMatchDragTarget = -1;
            primeMatchSelection.clear();
            primeMatchSelection.add(primeMatchDragStart);
            atualizarPrimeMatchCelulas();
            return;
        }
        if (!primeMatchSaoVizinhas(primeMatchDragStart, idx)) {
            return;
        }
        if (primeMatchDragTarget != idx) {
            primeMatchDragTarget = idx;
            primeMatchSelection.clear();
            primeMatchSelection.add(primeMatchDragStart);
            primeMatchSelection.add(idx);
            primeMatchStatusView.setText("Solte para trocar as bolinhas.");
            atualizarPrimeMatchCelulas();
        }
    }

    private void finalizarPrimeMatchArrasto(int idx) {
        if (!primeMatchActive || primeMatchResolving || primeMatchDragStart < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }
        int destino = primeMatchSaoVizinhas(primeMatchDragStart, idx) ? idx : primeMatchDragTarget;
        if (!primeMatchSaoVizinhas(primeMatchDragStart, destino)) {
            cancelarPrimeMatchArrasto();
            primeMatchStatusView.setText("Arraste para uma bolinha vizinha para fazer a troca.");
            return;
        }

        primeMatchResolving = true;
        atualizarPrimeMatchBotoesPoder();
        final int origem = primeMatchDragStart;
        final int alvo = destino;
        primeMatchMergeAnchor = alvo;
        primeMatchMergeFallbackAnchor = origem;
        trocarPrimeMatchPecas(origem, alvo);
        primeMatchSelection.clear();
        primeMatchSelection.add(origem);
        primeMatchSelection.add(alvo);
        atualizarPrimeMatchCelulas();

        primeMatchGrid.postDelayed(() -> {
            if (!primeMatchResolving) {
                return;
            }
            boolean[][] primeiraCombinacao = encontrarPrimeMatchCombinacoes();
            if (contarPrimeMatchMarcadas(primeiraCombinacao) == 0) {
                primeMatchSelection.clear();
                primeMatchSelection.add(origem);
                primeMatchSelection.add(alvo);
                animarPrimeMatchTrocaInvalida(() -> {
                    if (!primeMatchResolving) {
                        return;
                    }
                    trocarPrimeMatchPecas(origem, alvo);
                    primeMatchSelection.clear();
                    primeMatchDragStart = -1;
                    primeMatchDragTarget = -1;
                    primeMatchMergeAnchor = -1;
                    primeMatchMergeFallbackAnchor = -1;
                    primeMatchResolving = false;
                    atualizarPrimeMatchBotoesPoder();
                    primeMatchGrid.postDelayed(() -> verificarPrimeMatchJogadasPossiveis(), 60);
                    primeMatchStatusView.setText("Essa troca não formou trio. Tente outra combinação.");
                    atualizarPrimeMatchCelulas();
                });
                return;
            }

            primeMatchMoves--;
            primeMatchDragStart = -1;
            primeMatchDragTarget = -1;
            primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
            primeMatchSelection.clear();
            atualizarPrimeMatchPlacar();
            resolverPrimeMatchCombinacoesAnimadas(0, 0, 0);
        }, 90);
    }

    private void cancelarPrimeMatchArrasto() {
        primeMatchDragStart = -1;
        primeMatchDragTarget = -1;
        primeMatchMergeAnchor = -1;
        primeMatchMergeFallbackAnchor = -1;
        primeMatchPowerFirstSelection = -1;
        primeMatchSelection.clear();
        atualizarPrimeMatchCelulas();
    }

    private boolean processarPrimeMatchToquePoder(int idx, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                bloquearPrimeMatchRolagem(true);
                primeMatchDragStart = idx;
                if (idx >= 0) {
                    destacarPrimeMatchIndicePoder(idx);
                }
                return true;
            case MotionEvent.ACTION_UP:
                executarPrimeMatchPoderEm(idx >= 0 ? idx : primeMatchDragStart);
                bloquearPrimeMatchRolagem(false);
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelarPrimeMatchArrasto();
                bloquearPrimeMatchRolagem(false);
                return true;
            default:
                return true;
        }
    }

    private void destacarPrimeMatchIndicePoder(int idx) {
        primeMatchSelection.clear();
        if (primeMatchPowerFirstSelection >= 0) {
            primeMatchSelection.add(primeMatchPowerFirstSelection);
        }
        if (idx >= 0 && !primeMatchSelection.contains(idx)) {
            primeMatchSelection.add(idx);
        }
        atualizarPrimeMatchCelulas();
    }

    private void executarPrimeMatchPoderEm(int idx) {
        if (!primeMatchActive || primeMatchResolving || idx < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }

        switch (primeMatchPowerMode) {
            case PRIME_MATCH_POWER_HAMMER:
                usarPrimeMatchMarteloEm(idx);
                break;
            case PRIME_MATCH_POWER_FREE_SWAP:
                usarPrimeMatchTrocaLivreEm(idx);
                break;
            case PRIME_MATCH_POWER_COLOR_COPY:
                usarPrimeMatchCoringaCorEm(idx);
                break;
            case PRIME_MATCH_POWER_POLISH_UP:
                usarPrimeMatchLapidadorEm(idx, 1);
                break;
            case PRIME_MATCH_POWER_POLISH_DOWN:
                usarPrimeMatchLapidadorEm(idx, -1);
                break;
            case PRIME_MATCH_POWER_ROW_RAY:
                usarPrimeMatchRaioEm(idx, true);
                break;
            case PRIME_MATCH_POWER_COL_RAY:
                usarPrimeMatchRaioEm(idx, false);
                break;
            default:
                cancelarPrimeMatchArrasto();
                break;
        }
    }

    private void usarPrimeMatchMarteloEm(int idx) {
        if (!primeMatchActive || primeMatchResolving || primeMatchHammerCount <= 0 || idx < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }

        consumirPrimeMatchPoder(PRIME_MATCH_POWER_HAMMER);
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchPendingModifier = 0;
        primeMatchMergeAnchor = -1;
        primeMatchMergeFallbackAnchor = -1;
        primeMatchResolving = true;
        atualizarPrimeMatchBotoesPoder();

        primeMatchSelection.clear();
        primeMatchSelection.add(idx);
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText("Martelo usado. Quebrando a bolinha escolhida.");

        boolean[][] marcada = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        marcada[idx / PRIME_MATCH_COLS][idx % PRIME_MATCH_COLS] = true;
        animarPrimeMatchExplosao(marcada, () -> {
            if (!primeMatchResolving) {
                return;
            }
            primeMatchSelection.clear();
            limparPrimeMatchMarcadas(marcada);
            compactarPrimeMatchTabuleiro();
            atualizarPrimeMatchCelulas();
            animarPrimeMatchEntradaTabuleiro();
            primeMatchGrid.postDelayed(() -> resolverPrimeMatchCombinacoesAnimadas(1, 0, 0), 260);
        });
    }

    private void usarPrimeMatchTrocaLivreEm(int idx) {
        if (primeMatchFreeSwapCount <= 0 || idx < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }
        if (primeMatchPowerFirstSelection < 0) {
            primeMatchPowerFirstSelection = idx;
            destacarPrimeMatchIndicePoder(idx);
            primeMatchStatusView.setText("Troca livre: escolha a segunda bolinha.");
            return;
        }
        if (primeMatchPowerFirstSelection == idx) {
            primeMatchStatusView.setText("Escolha uma bolinha diferente para a troca livre.");
            return;
        }

        int origem = primeMatchPowerFirstSelection;
        primeMatchMergeAnchor = idx;
        primeMatchMergeFallbackAnchor = origem;
        consumirPrimeMatchPoder(PRIME_MATCH_POWER_FREE_SWAP);
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchSelection.clear();
        trocarPrimeMatchPecas(origem, idx);
        primeMatchStatusView.setText("Troca livre concluída.");
        atualizarPrimeMatchCelulas();
        animarPrimeMatchReorganizacao();
        resolverPrimeMatchAposPoder("Troca livre concluída.");
    }

    private void usarPrimeMatchCoringaCorEm(int idx) {
        if (primeMatchColorCopyCount <= 0 || idx < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }
        int corEscolhida = primeMatchColors[idx / PRIME_MATCH_COLS][idx % PRIME_MATCH_COLS];
        if (primeMatchEhCorPreta(corEscolhida)) {
            primeMatchStatusView.setText("Peças pretas não podem copiar nem emprestar a cor.");
            return;
        }
        if (primeMatchPowerFirstSelection < 0) {
            primeMatchPowerFirstSelection = idx;
            destacarPrimeMatchIndicePoder(idx);
            primeMatchStatusView.setText("Coringa: agora toque na bolinha com a cor desejada.");
            return;
        }
        if (primeMatchPowerFirstSelection == idx) {
            primeMatchStatusView.setText("Escolha outra bolinha para copiar a cor.");
            return;
        }

        int destino = primeMatchPowerFirstSelection;
        if (primeMatchColors[destino / PRIME_MATCH_COLS][destino % PRIME_MATCH_COLS] == corEscolhida) {
            primeMatchStatusView.setText("Escolha uma cor diferente.");
            return;
        }
        primeMatchMergeAnchor = destino;
        primeMatchMergeFallbackAnchor = idx;
        primeMatchColors[destino / PRIME_MATCH_COLS][destino % PRIME_MATCH_COLS] =
                primeMatchColors[idx / PRIME_MATCH_COLS][idx % PRIME_MATCH_COLS];
        consumirPrimeMatchPoder(PRIME_MATCH_POWER_COLOR_COPY);
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchSelection.clear();
        primeMatchStatusView.setText("Coringa aplicado. A bolinha copiou a cor escolhida.");
        atualizarPrimeMatchCelulas();
        resolverPrimeMatchAposPoder("Coringa aplicado.");
    }

    private void usarPrimeMatchLapidadorEm(int idx, int delta) {
        int poder = delta > 0 ? PRIME_MATCH_POWER_POLISH_UP : PRIME_MATCH_POWER_POLISH_DOWN;
        if (obterPrimeMatchPoderCount(poder) <= 0 || idx < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }
        int row = idx / PRIME_MATCH_COLS;
        int col = idx % PRIME_MATCH_COLS;
        int novoValor = matchPolishedValueNativo(primeMatchValues[row][col], primeMatchColors[row][col], delta);
        if (novoValor == 0) {
            primeMatchStatusView.setText("Escolha uma peça colorida cujo valor possa ficar entre 1 e 149.");
            return;
        }

        primeMatchValues[row][col] = novoValor;
        primeMatchMergeAnchor = idx;
        primeMatchMergeFallbackAnchor = -1;
        consumirPrimeMatchPoder(poder);
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchSelection.clear();
        primeMatchStatusView.setText("Lapidador aplicado. Valor ajustado para " + novoValor + ".");
        atualizarPrimeMatchCelulas();
    }

    private void usarPrimeMatchRaioEm(int idx, boolean linha) {
        int poder = linha ? PRIME_MATCH_POWER_ROW_RAY : PRIME_MATCH_POWER_COL_RAY;
        if (obterPrimeMatchPoderCount(poder) <= 0 || idx < 0) {
            cancelarPrimeMatchArrasto();
            return;
        }
        boolean[][] marcadas = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        int row = idx / PRIME_MATCH_COLS;
        int col = idx % PRIME_MATCH_COLS;
        if (linha) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                marcadas[row][c] = true;
            }
        } else {
            for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
                marcadas[r][col] = true;
            }
        }

        consumirPrimeMatchPoder(poder);
        primeMatchPowerMode = PRIME_MATCH_POWER_NONE;
        primeMatchPowerFirstSelection = -1;
        primeMatchMergeAnchor = -1;
        primeMatchMergeFallbackAnchor = -1;
        primeMatchResolving = true;
        primeMatchSelection.clear();
        marcarPrimeMatchSelecao(marcadas);
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText(linha ? "Raio de linha ativado." : "Raio de coluna ativado.");
        animarPrimeMatchExplosao(marcadas, () -> {
            if (!primeMatchResolving) {
                return;
            }
            primeMatchSelection.clear();
            limparPrimeMatchMarcadas(marcadas);
            compactarPrimeMatchTabuleiro();
            atualizarPrimeMatchCelulas();
            animarPrimeMatchEntradaTabuleiro();
            primeMatchGrid.postDelayed(() -> resolverPrimeMatchCombinacoesAnimadas(1, 0, 0), 260);
        });
    }

    private void resolverPrimeMatchAposPoder(String statusSemCombo) {
        primeMatchResolving = true;
        atualizarPrimeMatchBotoesPoder();
        primeMatchGrid.postDelayed(() -> {
            if (contarPrimeMatchMarcadas(encontrarPrimeMatchCombinacoes()) == 0) {
                primeMatchResolving = false;
                primeMatchStatusView.setText(statusSemCombo);
                atualizarPrimeMatchBotoesPoder();
                atualizarPrimeMatchCelulas();
                return;
            }
            resolverPrimeMatchCombinacoesAnimadas(1, 0, 0);
        }, 220);
    }

    private void consumirPrimeMatchPoder(int poder) {
        switch (poder) {
            case PRIME_MATCH_POWER_HAMMER:
                primeMatchHammerCount = Math.max(0, primeMatchHammerCount - 1);
                break;
            case PRIME_MATCH_POWER_FREE_SWAP:
                primeMatchFreeSwapCount = Math.max(0, primeMatchFreeSwapCount - 1);
                break;
            case PRIME_MATCH_POWER_COLOR_COPY:
                primeMatchColorCopyCount = Math.max(0, primeMatchColorCopyCount - 1);
                break;
            case PRIME_MATCH_POWER_POLISH_UP:
                primeMatchPolishUpCount = Math.max(0, primeMatchPolishUpCount - 1);
                break;
            case PRIME_MATCH_POWER_POLISH_DOWN:
                primeMatchPolishDownCount = Math.max(0, primeMatchPolishDownCount - 1);
                break;
            case PRIME_MATCH_POWER_ROW_RAY:
                primeMatchRowRayCount = Math.max(0, primeMatchRowRayCount - 1);
                break;
            case PRIME_MATCH_POWER_COL_RAY:
                primeMatchColRayCount = Math.max(0, primeMatchColRayCount - 1);
                break;
            default:
                break;
        }
        tocarPrimeMatchSom(primeMatchSoundPower);
        salvarPrimeMatchPoderes();
        atualizarPrimeMatchBotoesPoder();
    }

    private boolean primeMatchSaoVizinhas(int primeiro, int segundo) {
        if (primeiro < 0 || segundo < 0) {
            return false;
        }
        int row1 = primeiro / PRIME_MATCH_COLS;
        int col1 = primeiro % PRIME_MATCH_COLS;
        int row2 = segundo / PRIME_MATCH_COLS;
        int col2 = segundo % PRIME_MATCH_COLS;
        return Math.abs(row1 - row2) + Math.abs(col1 - col2) == 1;
    }

    private void trocarPrimeMatchPecas(int primeiro, int segundo) {
        int row1 = primeiro / PRIME_MATCH_COLS;
        int col1 = primeiro % PRIME_MATCH_COLS;
        int row2 = segundo / PRIME_MATCH_COLS;
        int col2 = segundo % PRIME_MATCH_COLS;

        int valor = primeMatchValues[row1][col1];
        int cor = primeMatchColors[row1][col1];
        primeMatchValues[row1][col1] = primeMatchValues[row2][col2];
        primeMatchColors[row1][col1] = primeMatchColors[row2][col2];
        primeMatchValues[row2][col2] = valor;
        primeMatchColors[row2][col2] = cor;
    }

    private void selecionarPrimeMatchIndice(int idx) {
        if (!primeMatchActive || idx < 0 || primeMatchSelection.contains(idx)) {
            return;
        }
        int row = idx / PRIME_MATCH_COLS;
        int col = idx % PRIME_MATCH_COLS;
        if (primeMatchSelection.isEmpty()) {
            primeMatchSelection.add(idx);
            atualizarPrimeMatchSelecao();
            return;
        }
        if (primeMatchSelection.size() >= PRIME_MATCH_MAX_CHAIN) {
            return;
        }
        int primeiro = primeMatchSelection.get(0);
        int primeiraCor = primeMatchColors[primeiro / PRIME_MATCH_COLS][primeiro % PRIME_MATCH_COLS];
        if (primeMatchColors[row][col] != primeiraCor) {
            return;
        }
        int ultimo = primeMatchSelection.get(primeMatchSelection.size() - 1);
        int ultimoRow = ultimo / PRIME_MATCH_COLS;
        int ultimoCol = ultimo % PRIME_MATCH_COLS;
        if (Math.abs(row - ultimoRow) > 1 || Math.abs(col - ultimoCol) > 1) {
            return;
        }
        primeMatchSelection.add(idx);
        atualizarPrimeMatchSelecao();
    }

    private void atualizarPrimeMatchSelecao() {
        atualizarPrimeMatchCelulas();
        int soma = somaPrimeMatchSelecao();
        if (primeMatchSelection.isEmpty()) {
            return;
        }
        String status = "Soma atual: " + soma;
        if (primeMatchSelection.size() >= PRIME_MATCH_MIN_CHAIN) {
            status += ehPrimo(soma) ? " (prima)" : " (composta)";
        }
        primeMatchStatusView.setText(status);
    }

    private int somaPrimeMatchSelecao() {
        int soma = 0;
        for (int idx : primeMatchSelection) {
            soma += primeMatchValues[idx / PRIME_MATCH_COLS][idx % PRIME_MATCH_COLS];
        }
        return soma;
    }

    private void finalizarPrimeMatchSelecao() {
        if (!primeMatchActive) {
            primeMatchSelection.clear();
            atualizarPrimeMatchCelulas();
            return;
        }
        if (primeMatchSelection.size() < PRIME_MATCH_MIN_CHAIN) {
            primeMatchSelection.clear();
            atualizarPrimeMatchCelulas();
            primeMatchStatusView.setText("Selecione 3, 4 ou 5 bolinhas vizinhas da mesma cor.");
            return;
        }

        int tamanho = primeMatchSelection.size();
        int soma = somaPrimeMatchSelecao();
        boolean primo = ehPrimo(soma);
        primeMatchMoves--;

        if (primo) {
            int pontos = soma;
            primeMatchScore += pontos;
            removerPrimeMatchSelecao();
            primeMatchStatusView.setText("Soma " + soma + " é prima. +" + pontos + " pontos.");
        } else {
            removerPrimeMatchSelecao();
            primeMatchStatusView.setText("Soma " + soma + " é composta. As bolinhas abriram espaço, sem desconto.");
        }

        primeMatchSelection.clear();
        atualizarPrimeMatchPlacar();
        atualizarPrimeMatchCelulas();
        verificarPrimeMatchFimFase();
    }

    private void removerPrimeMatchSelecao() {
        for (int idx : primeMatchSelection) {
            int row = idx / PRIME_MATCH_COLS;
            int col = idx % PRIME_MATCH_COLS;
            primeMatchValues[row][col] = 0;
            primeMatchColors[row][col] = -1;
        }

        compactarPrimeMatchTabuleiro();
        resolverPrimeMatchCombinacoesAutomaticas(1);
    }

    private void compactarPrimeMatchTabuleiro() {
        compactarPrimeMatchTabuleiro(null);
    }

    private void compactarPrimeMatchTabuleiro(boolean[][] fixas) {
        for (int col = 0; col < PRIME_MATCH_COLS; col++) {
            int bottom = PRIME_MATCH_ROWS - 1;
            while (bottom >= 0) {
                while (bottom >= 0 && fixas != null && fixas[bottom][col]) {
                    bottom--;
                }
                if (bottom < 0) {
                    break;
                }

                int top = bottom;
                while (top >= 0 && (fixas == null || !fixas[top][col])) {
                    top--;
                }

                compactarPrimeMatchSegmento(col, top + 1, bottom);
                bottom = top - 1;
            }
        }
    }

    private void compactarPrimeMatchSegmento(int col, int top, int bottom) {
        int writeRow = bottom;
        for (int row = bottom; row >= top; row--) {
            if (primeMatchValues[row][col] != 0) {
                if (writeRow != row) {
                    primeMatchValues[writeRow][col] = primeMatchValues[row][col];
                    primeMatchColors[writeRow][col] = primeMatchColors[row][col];
                    primeMatchValues[row][col] = 0;
                    primeMatchColors[row][col] = -1;
                }
                writeRow--;
            }
        }
        for (int row = top; row <= writeRow; row++) {
            gerarPrimeMatchPecaEvitandoTrinca(row, col);
        }
    }

    private void reorganizarPrimeMatchTabuleiroInteiro() {
        int total = PRIME_MATCH_ROWS * PRIME_MATCH_COLS;
        int[] valores = new int[total];
        int[] cores = new int[total];
        int pos = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                valores[pos] = primeMatchValues[r][c];
                cores[pos] = primeMatchColors[r][c];
                pos++;
            }
        }

        for (int tentativa = 0; tentativa < 80; tentativa++) {
            for (int i = total - 1; i > 0; i--) {
                int j = primeMatchRandom.nextInt(i + 1);
                int valorTemp = valores[i];
                int corTemp = cores[i];
                valores[i] = valores[j];
                cores[i] = cores[j];
                valores[j] = valorTemp;
                cores[j] = corTemp;
            }
            aplicarPrimeMatchOrdem(valores, cores);
            if (contarPrimeMatchMarcadas(encontrarPrimeMatchCombinacoes()) == 0
                    && primeMatchTemJogadaPossivel()) {
                return;
            }
        }

        preencherPrimeMatchTabuleiroBasico();
        if (!primeMatchTemJogadaPossivel()) {
            forcarPrimeMatchJogadaPossivel();
        }
    }

    private void aplicarPrimeMatchOrdem(int[] valores, int[] cores) {
        int pos = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                primeMatchValues[r][c] = valores[pos];
                primeMatchColors[r][c] = cores[pos];
                pos++;
            }
        }
    }

    private boolean primeMatchTemJogadaPossivel() {
        return encontrarPrimeMatchPrimeiraJogadaPossivel() != null;
    }

    private int[] encontrarPrimeMatchPrimeiraJogadaPossivel() {
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                int idx = r * PRIME_MATCH_COLS + c;
                if (!primeMatchCelulaOcupada(r, c)) {
                    continue;
                }
                if (c + 1 < PRIME_MATCH_COLS
                        && primeMatchTrocaCriaCombinacao(idx, idx + 1)) {
                    return new int[]{idx, idx + 1};
                }
                if (r + 1 < PRIME_MATCH_ROWS
                        && primeMatchTrocaCriaCombinacao(idx, idx + PRIME_MATCH_COLS)) {
                    return new int[]{idx, idx + PRIME_MATCH_COLS};
                }
            }
        }
        return null;
    }

    private boolean primeMatchTrocaCriaCombinacao(int primeiro, int segundo) {
        if (!primeMatchSaoVizinhas(primeiro, segundo)) {
            return false;
        }
        if (!primeMatchCelulaOcupada(primeiro / PRIME_MATCH_COLS, primeiro % PRIME_MATCH_COLS)
                || !primeMatchCelulaOcupada(segundo / PRIME_MATCH_COLS, segundo % PRIME_MATCH_COLS)) {
            return false;
        }
        trocarPrimeMatchPecas(primeiro, segundo);
        boolean temCombinacao = !encontrarPrimeMatchGruposCombinacoes().isEmpty();
        trocarPrimeMatchPecas(primeiro, segundo);
        return temCombinacao;
    }

    private boolean forcarPrimeMatchJogadaPossivel() {
        int totalCores = primeMatchPaleta().length;
        for (int tentativa = 0; tentativa < 80; tentativa++) {
            int row = primeMatchRandom.nextInt(PRIME_MATCH_ROWS - 1);
            int col = primeMatchRandom.nextInt(PRIME_MATCH_COLS - 2);
            int[][] pontos = new int[][]{
                    {row, col},
                    {row, col + 1},
                    {row, col + 2},
                    {row + 1, col + 1}
            };
            if (algumPrimeMatchPontoPreto(pontos)) {
                continue;
            }

            int[] valoresAntigos = new int[pontos.length];
            int[] coresAntigas = new int[pontos.length];
            for (int i = 0; i < pontos.length; i++) {
                valoresAntigos[i] = primeMatchValues[pontos[i][0]][pontos[i][1]];
                coresAntigas[i] = primeMatchColors[pontos[i][0]][pontos[i][1]];
            }

            int corA = primeMatchRandom.nextInt(totalCores);
            int corB = (corA + 1 + primeMatchRandom.nextInt(totalCores - 1)) % totalCores;
            aplicarPrimeMatchPontoForcado(pontos[0][0], pontos[0][1], corA);
            aplicarPrimeMatchPontoForcado(pontos[1][0], pontos[1][1], corB);
            aplicarPrimeMatchPontoForcado(pontos[2][0], pontos[2][1], corA);
            aplicarPrimeMatchPontoForcado(pontos[3][0], pontos[3][1], corA);

            if (contarPrimeMatchMarcadas(encontrarPrimeMatchCombinacoes()) == 0
                    && primeMatchTemJogadaPossivel()) {
                return true;
            }

            for (int i = 0; i < pontos.length; i++) {
                primeMatchValues[pontos[i][0]][pontos[i][1]] = valoresAntigos[i];
                primeMatchColors[pontos[i][0]][pontos[i][1]] = coresAntigas[i];
            }
        }
        return false;
    }

    private boolean algumPrimeMatchPontoPreto(int[][] pontos) {
        for (int[] ponto : pontos) {
            if (primeMatchEhCorPreta(primeMatchColors[ponto[0]][ponto[1]])) {
                return true;
            }
        }
        return false;
    }

    private void aplicarPrimeMatchPontoForcado(int row, int col, int cor) {
        primeMatchValues[row][col] = gerarPrimeMatchValorVariado();
        primeMatchColors[row][col] = cor;
    }

    private int[] resolverPrimeMatchCombinacoesAutomaticas(int combosJaResolvidos) {
        int combos = 0;
        int removidasTotal = 0;
        int saldoTotal = 0;

        while (combos < 12) {
            List<PrimeMatchGrupo> grupos = encontrarPrimeMatchGruposCombinacoes();
            if (grupos.isEmpty()) {
                break;
            }

            List<PrimeMatchResultadoGrupo> resultados = prepararPrimeMatchResultados(
                    grupos,
                    combosJaResolvidos + combos,
                    0,
                    false
            );
            int saldoRodada = somarPrimeMatchSaldoResultados(resultados);
            int removidasRodada = contarPrimeMatchCelulasResultados(resultados);
            primeMatchScore += saldoRodada;
            saldoTotal += saldoRodada;
            removidasTotal += removidasRodada;
            combos++;
            aplicarPrimeMatchResultados(resultados);
            primeMatchMergeAnchor = -1;
            primeMatchMergeFallbackAnchor = -1;
        }

        return new int[]{combos, removidasTotal, saldoTotal};
    }

    private void resolverPrimeMatchCombinacoesAnimadas(int combos, int removidasTotal, int saldoTotal) {
        if (!primeMatchResolving || primeMatchGrid == null) {
            return;
        }
        if (combos >= 12) {
            finalizarPrimeMatchResolucaoAnimada(combos, removidasTotal, saldoTotal);
            return;
        }

        List<PrimeMatchGrupo> grupos = encontrarPrimeMatchGruposCombinacoes();
        boolean[][] calculoMarcadas = juntarPrimeMatchGrupos(grupos);
        if (contarPrimeMatchMarcadas(calculoMarcadas) == 0) {
            finalizarPrimeMatchResolucaoAnimada(combos, removidasTotal, saldoTotal);
            return;
        }

        int ajusteUsado = combos == 0 ? primeMatchPendingModifier : 0;
        List<PrimeMatchResultadoGrupo> resultados = prepararPrimeMatchResultados(
                grupos,
                combos,
                ajusteUsado,
                true
        );
        boolean[][] explosaoMarcadas = juntarPrimeMatchExplosoes(resultados);
        int removidas = contarPrimeMatchMarcadas(explosaoMarcadas);
        int saldo = somarPrimeMatchSaldoResultados(resultados);
        if (algumPrimeMatchResultadoUsouAjuste(resultados)) {
            consumirPrimeMatchAjustePendente();
        }
        atualizarPrimeMatchBotoesPoder();

        final int saldoResolvido = saldo;
        final GridLayout tabuleiro = primeMatchGrid;

        apresentarPrimeMatchCalculos(tabuleiro, resultados, combos + 1, 0, () -> {
            if (!primeMatchResolving || primeMatchGrid != tabuleiro || !tabuleiro.isAttachedToWindow()) {
                return;
            }
            animarPrimeMatchExplosao(explosaoMarcadas, () -> {
                if (!primeMatchResolving || primeMatchGrid != tabuleiro || !tabuleiro.isAttachedToWindow()) {
                    return;
                }
                primeMatchSelection.clear();
                registrarPrimeMatchHistorico(resultados, combos + 1);
                aplicarPrimeMatchResultados(resultados);
                primeMatchScore = (int) Math.min(Integer.MAX_VALUE, (long) primeMatchScore + saldoResolvido);
                atualizarPrimeMatchPlacar();
                atualizarPrimeMatchCelulas();
                animarPrimeMatchFusoes(resultados);
                primeMatchMergeAnchor = -1;
                primeMatchMergeFallbackAnchor = -1;
                primeMatchGrid.postDelayed(() -> resolverPrimeMatchCombinacoesAnimadas(
                        combos + 1,
                        removidasTotal + removidas,
                        saldoTotal + saldoResolvido
                ), 260);
            });
        });
    }

    private void apresentarPrimeMatchCalculos(GridLayout tabuleiro, List<PrimeMatchResultadoGrupo> resultados,
                                             int combo, int indice, Runnable aoTerminar) {
        if (!primeMatchResolving || primeMatchGrid != tabuleiro || !tabuleiro.isAttachedToWindow()) return;
        if (indice >= resultados.size()) {
            aoTerminar.run();
            return;
        }
        PrimeMatchResultadoGrupo atual = resultados.get(indice);
        List<PrimeMatchResultadoGrupo> resultado = java.util.Collections.singletonList(atual);
        primeMatchSelection.clear();
        marcarPrimeMatchSelecao(atual.grupo.marcadas);
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText((combo == 1 ? "Jogada" : "Combo " + combo) + " em cena no tabuleiro.");
        mostrarPrimeMatchResultadoFlutuante(resultado, atual.saldo, indice + 1, resultados.size());
        tocarPrimeMatchSomResultados(resultado, combo);
        animarPrimeMatchDestaqueCalculo(atual.grupo.marcadas, atual.explosaoMarcada,
            () -> apresentarPrimeMatchCalculos(tabuleiro, resultados, combo, indice + 1, aoTerminar));
    }

    private void finalizarPrimeMatchResolucaoAnimada(int combos, int removidasTotal, int saldoTotal) {
        primeMatchResolving = false;
        if (removidasTotal > 0) {
            if (saldoTotal > 0) {
                primeMatchStatusView.setText("Resolvido: " + removidasTotal + " bolinhas em " + combos
                        + " combo(s). +" + saldoTotal + " pontos acumulados.");
            } else {
                primeMatchStatusView.setText("Resolvido: soma composta explodiu tudo, sem desconto.");
            }
        }
        atualizarPrimeMatchPlacar();
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        verificarPrimeMatchFimFase();
        if (primeMatchActive) {
            verificarPrimeMatchJogadasPossiveis();
        }
    }

    private void verificarPrimeMatchJogadasPossiveis() {
        if (!primeMatchActive
                || primeMatchResolving
                || primeMatchAutoShufflePending
                || primeMatchGrid == null
                || primeMatchValues == null
                || primeMatchColors == null) {
            return;
        }
        if (primeMatchTemJogadaPossivel()) {
            return;
        }

        primeMatchAutoShufflePending = true;
        primeMatchResolving = true;
        primeMatchSelection.clear();
        bloquearPrimeMatchRolagem(false);
        atualizarPrimeMatchBotoesPoder();
        atualizarPrimeMatchCelulas();
        primeMatchStatusView.setText("Sem jogadas possíveis. Embaralhando o tabuleiro automaticamente...");
        tocarPrimeMatchSom(primeMatchSoundPower);

        primeMatchGrid.postDelayed(() -> {
            if (!primeMatchActive || !primeMatchAutoShufflePending) {
                return;
            }
            reorganizarPrimeMatchTabuleiroInteiro();
            primeMatchAutoShufflePending = false;
            primeMatchResolving = false;
            primeMatchSelection.clear();
            atualizarPrimeMatchBotoesPoder();
            atualizarPrimeMatchCelulas();
            animarPrimeMatchReorganizacao();
            primeMatchStatusView.setText("Tabuleiro embaralhado. Nova jogada disponível, sem perder jogada.");
        }, 780);
    }

    private String concederPrimeMatchRecompensasVitoria() {
        if (primeMatchRewardGranted) return "";
        primeMatchRewardGranted = true;
        int mask = matchVictoryRewardsNativo(++primeMatchWins);
        List<String> recompensas = new ArrayList<>();
        for (int i = 0; i < PRIME_MATCH_POWER_KEYS.length; i++) {
            if ((mask & (1 << i)) == 0) continue;
            switch (PRIME_MATCH_POWER_KEYS[i]) {
                case KEY_PM_HAMMER: primeMatchHammerCount++; break;
                case KEY_PM_PLUS_1: primeMatchPlus1Count++; break;
                case KEY_PM_PLUS_2: primeMatchPlus2Count++; break;
                case KEY_PM_PLUS_3: primeMatchPlus3Count++; break;
                case KEY_PM_RESHUFFLE: primeMatchReshuffleCount++; break;
                case KEY_PM_FREE_SWAP: primeMatchFreeSwapCount++; break;
                case KEY_PM_COLOR_COPY: primeMatchColorCopyCount++; break;
                case KEY_PM_POLISH_UP: primeMatchPolishUpCount++; break;
                case KEY_PM_POLISH_DOWN: primeMatchPolishDownCount++; break;
                case KEY_PM_ROW_RAY: primeMatchRowRayCount++; break;
                case KEY_PM_COL_RAY: primeMatchColRayCount++; break;
                case KEY_PM_HINT: primeMatchHintCount++; break;
                case KEY_PM_COMBO_MULT: primeMatchComboMultCount++; break;
                default: continue;
            }
            recompensas.add(PRIME_MATCH_POWER_LABELS[i] + " +1");
        }
        salvarPrimeMatchPoderes();
        atualizarPrimeMatchBotoesPoder();
        return recompensas.isEmpty() ? "" : "Recompensas: " + android.text.TextUtils.join(", ", recompensas);
    }

    private List<PrimeMatchResultadoGrupo> prepararPrimeMatchResultados(
            List<PrimeMatchGrupo> grupos,
            int combosJaResolvidos,
            int ajusteUsado,
            boolean aplicarPoderes
    ) {
        ArrayList<PrimeMatchResultadoGrupo> resultados = new ArrayList<>();
        int indiceAjuste = escolherPrimeMatchIndiceAjuste(grupos);
        boolean multiplicadorConsumido = false;
        long todasGruposMarcadas = 0;
        for (PrimeMatchGrupo grupo : grupos) todasGruposMarcadas |= grupo.mascara;
        long extrasPontuadas = 0;
        long pretas = 0;
        int[] valores = new int[PRIME_MATCH_ROWS * PRIME_MATCH_COLS];
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (!primeMatchCelulaOcupada(r, c)) continue;
                int index = r * PRIME_MATCH_COLS + c;
                valores[index] = primeMatchValues[r][c];
                if (primeMatchEhCorPreta(primeMatchColors[r][c])) pretas |= 1L << index;
            }
        }

        for (int i = 0; i < grupos.size(); i++) {
            PrimeMatchGrupo grupo = grupos.get(i);
            boolean usaAjuste = ajusteUsado > 0 && i == indiceAjuste;
            int somaCalculada = (int) Math.min(Integer.MAX_VALUE, (long) grupo.soma + (usaAjuste ? ajusteUsado : 0));
            long[] evento = matchResolveNativo(valores, pretas, grupo.mascara, todasGruposMarcadas,
                extrasPontuadas, somaCalculada);
            boolean[][] explosaoMarcada = decodificarPrimeMatchMascara(evento[0]);
            extrasPontuadas |= evento[1];
            int saldo = (int) Math.min(Integer.MAX_VALUE, evento[2]);
            int valorPreta = (int) Math.min(Integer.MAX_VALUE, evento[3]);
            boolean primo = valorPreta > 0;
            int somaFinal = primo ? saldo : somaCalculada;
            int especialExtra = Math.max(0, somaFinal - somaCalculada);
            int especialTipo = (int) evento[4];
            boolean multiplicadorUsado = false;

            if (aplicarPoderes
                    && saldo > 0
                    && combosJaResolvidos > 0
                    && !multiplicadorConsumido
                    && primeMatchComboMultiplierArmed
                    && primeMatchComboMultCount > 0) {
                primeMatchComboMultCount--;
                primeMatchComboMultiplierArmed = false;
                salvarPrimeMatchPoderes();
                saldo = (int) Math.min(Integer.MAX_VALUE, 2L * saldo);
                multiplicadorUsado = true;
                multiplicadorConsumido = true;
            }

            resultados.add(new PrimeMatchResultadoGrupo(
                    grupo,
                    explosaoMarcada,
                    somaCalculada,
                    somaFinal,
                    valorPreta,
                    saldo,
                    escolherPrimeMatchAncoraGrupo(grupo),
                    primo,
                    usaAjuste,
                    usaAjuste ? ajusteUsado : 0,
                    multiplicadorUsado,
                    especialTipo,
                    especialExtra
            ));
        }

        return resultados;
    }

    private boolean primeMatchCelulaOcupada(int row, int col) {
        return row >= 0
                && row < PRIME_MATCH_ROWS
                && col >= 0
                && col < PRIME_MATCH_COLS
                && primeMatchValues[row][col] != 0
                && primeMatchColors[row][col] >= 0;
    }

    private int escolherPrimeMatchIndiceAjuste(List<PrimeMatchGrupo> grupos) {
        for (int i = 0; i < grupos.size(); i++) {
            PrimeMatchGrupo grupo = grupos.get(i);
            if (primeMatchGrupoContem(grupo, primeMatchMergeAnchor)
                    || primeMatchGrupoContem(grupo, primeMatchMergeFallbackAnchor)) {
                return i;
            }
        }
        return grupos.isEmpty() ? -1 : 0;
    }

    private int somarPrimeMatchSaldoResultados(List<PrimeMatchResultadoGrupo> resultados) {
        int total = 0;
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            total += resultado.saldo;
        }
        return total;
    }

    private int contarPrimeMatchCelulasResultados(List<PrimeMatchResultadoGrupo> resultados) {
        return contarPrimeMatchMarcadas(juntarPrimeMatchExplosoes(resultados));
    }

    private boolean algumPrimeMatchResultadoUsouAjuste(List<PrimeMatchResultadoGrupo> resultados) {
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            if (resultado.ajusteUsado) {
                return true;
            }
        }
        return false;
    }

    private boolean[][] juntarPrimeMatchGrupos(List<PrimeMatchGrupo> grupos) {
        boolean[][] marcadas = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        for (PrimeMatchGrupo grupo : grupos) {
            for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
                for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                    marcadas[r][c] = marcadas[r][c] || grupo.marcadas[r][c];
                }
            }
        }
        return marcadas;
    }

    private boolean[][] juntarPrimeMatchExplosoes(List<PrimeMatchResultadoGrupo> resultados) {
        boolean[][] marcadas = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
                for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                    marcadas[r][c] = marcadas[r][c] || resultado.explosaoMarcada[r][c];
                }
            }
        }
        return marcadas;
    }

    private boolean primeMatchGrupoContem(PrimeMatchGrupo grupo, int idx) {
        if (grupo == null || idx < 0) {
            return false;
        }
        int row = idx / PRIME_MATCH_COLS;
        int col = idx % PRIME_MATCH_COLS;
        return row >= 0
                && row < PRIME_MATCH_ROWS
                && col >= 0
                && col < PRIME_MATCH_COLS
                && grupo.marcadas[row][col];
    }

    private int escolherPrimeMatchAncoraGrupo(PrimeMatchGrupo grupo) {
        if (primeMatchGrupoContem(grupo, primeMatchMergeAnchor)) {
            return primeMatchMergeAnchor;
        }
        if (primeMatchGrupoContem(grupo, primeMatchMergeFallbackAnchor)) {
            return primeMatchMergeFallbackAnchor;
        }

        int melhor = -1;
        int melhorRow = -1;
        int melhorCol = Integer.MAX_VALUE;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (!grupo.marcadas[r][c]) {
                    continue;
                }
                if (r > melhorRow || (r == melhorRow && c < melhorCol)) {
                    melhor = r * PRIME_MATCH_COLS + c;
                    melhorRow = r;
                    melhorCol = c;
                }
            }
        }
        return melhor;
    }

    private void aplicarPrimeMatchResultados(List<PrimeMatchResultadoGrupo> resultados) {
        boolean[][] limpar = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        boolean[][] fixas = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];

        for (PrimeMatchResultadoGrupo resultado : resultados) {
            for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
                for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                    limpar[r][c] = limpar[r][c] || resultado.explosaoMarcada[r][c];
                }
            }
        }

        limparPrimeMatchMarcadas(limpar);

        for (PrimeMatchResultadoGrupo resultado : resultados) {
            if (!resultado.primo || resultado.ancora < 0) {
                continue;
            }
            int row = resultado.ancora / PRIME_MATCH_COLS;
            int col = resultado.ancora % PRIME_MATCH_COLS;
            primeMatchValues[row][col] = Math.max(primeMatchValues[row][col], resultado.valorPreta);
            primeMatchColors[row][col] = PRIME_MATCH_COR_PRETA;
            fixas[row][col] = true;
        }

        compactarPrimeMatchTabuleiro(fixas);
    }

    private void marcarPrimeMatchSelecao(boolean[][] marcadas) {
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (marcadas[r][c]) {
                    primeMatchSelection.add(r * PRIME_MATCH_COLS + c);
                }
            }
        }
    }

    private String formatarPrimeMatchDetalheGrupos(
            List<PrimeMatchResultadoGrupo> resultados,
            int combo,
            int saldoTotal
    ) {
        if (resultados.size() == 1) {
            return formatarPrimeMatchDetalheCombo(resultados.get(0), combo);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(combo == 1 ? "Jogada" : "Combo " + combo)
                .append(": ")
                .append(resultados.size())
                .append(" grupos separados. ");

        int limite = Math.min(resultados.size(), 4);
        for (int i = 0; i < limite; i++) {
            PrimeMatchResultadoGrupo resultado = resultados.get(i);
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append("G")
                    .append(i + 1)
                    .append(": ")
                    .append(formatarPrimeMatchResumoResultado(resultado));
        }

        if (resultados.size() > limite) {
            sb.append(" | +").append(resultados.size() - limite).append(" grupo(s)");
        }
        if (saldoTotal > 0) {
            sb.append(". Total +").append(saldoTotal).append(" pontos.");
        } else {
            sb.append(". Total: sem pontos adicionados.");
        }
        return sb.toString();
    }

    private String formatarPrimeMatchDetalheCombo(PrimeMatchResultadoGrupo resultado, int combo) {
        String prefixo = combo == 1 ? "Jogada: " : "Combo " + combo + ": ";
        return prefixo + formatarPrimeMatchResumoResultado(resultado) + ".";
    }

    private String formatarPrimeMatchResumoResultado(PrimeMatchResultadoGrupo resultado) {
        PrimeMatchGrupo grupo = resultado.grupo;
        String calculo = listarPrimeMatchNumerosMarcados(grupo.marcadas) + " = " + grupo.soma;
        if (resultado.ajusteUsado) {
            calculo += "; ajuste +" + resultado.ajusteValor + " => " + resultado.somaCalculada;
        }
        if (!resultado.primo) {
            return calculo + ". Soma composta: explodiu tudo, sem desconto";
        }

        if (resultado.especialExtra > 0) {
            calculo += "; efeito preto +" + resultado.especialExtra + " => " + resultado.somaFinal;
        }
        String texto = calculo + ". Soma prima: vira peça preta " + resultado.valorPreta
                + " e soma +" + resultado.saldo;
        if (resultado.especialTipo != PRIME_MATCH_SPECIAL_NONE) {
            texto += " (" + nomePrimeMatchEspecialPreto(resultado.especialTipo) + ")";
        }
        if (resultado.multiplicadorUsado) {
            texto += " com x2";
        }
        return texto + " pontos";
    }

    private String nomePrimeMatchEspecialPreto(int especialTipo) {
        if (especialTipo == PRIME_MATCH_SPECIAL_AROUND_BLACK) {
            return "preta explode entorno";
        }
        if (especialTipo == PRIME_MATCH_SPECIAL_BOARD_PRIMES) {
            return "duas pretas explodem primos";
        }
        if (especialTipo == PRIME_MATCH_SPECIAL_FULL_BOARD) {
            return "tres pretas explodem tabuleiro";
        }
        return "sem efeito preto";
    }

    private void registrarPrimeMatchHistorico(List<PrimeMatchResultadoGrupo> resultados, int combo) {
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            primeMatchHistoryCounter++;
            String tipo = combo == 1 ? "Jogada" : "Combo " + combo;
            StringBuilder linha = new StringBuilder();
            linha.append(primeMatchHistoryCounter)
                    .append(". ")
                    .append(tipo)
                    .append(": ")
                    .append(listarPrimeMatchNumerosMarcados(resultado.grupo.marcadas))
                    .append(" = ")
                    .append(resultado.grupo.soma);
            if (resultado.ajusteUsado) {
                linha.append(" +").append(resultado.ajusteValor)
                        .append(" => ").append(resultado.somaCalculada);
            }
            if (resultado.primo) {
                if (resultado.especialExtra > 0) {
                    linha.append(" | ")
                            .append(nomePrimeMatchEspecialPreto(resultado.especialTipo))
                            .append(" +").append(resultado.especialExtra)
                            .append(" => ").append(resultado.somaFinal);
                }
                linha.append(" | primo, +").append(resultado.saldo)
                        .append(", virou preta ").append(resultado.valorPreta);
            } else {
                linha.append(" | composto, explodiu tudo, sem desconto");
            }
            primeMatchHistory.add(0, linha.toString());
        }
        while (primeMatchHistory.size() > PRIME_MATCH_HISTORY_MAX) {
            primeMatchHistory.remove(primeMatchHistory.size() - 1);
        }
        atualizarPrimeMatchHistoricoView();
    }

    private void atualizarPrimeMatchHistoricoView() {
        if (primeMatchHistoryView == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("Histórico da fase");
        if (primeMatchHistory.isEmpty()) {
            sb.append("\nAinda sem cálculos.");
        } else {
            int limite = primeMatchHistory.size();
            for (int i = 0; i < limite; i++) {
                sb.append("\n").append(primeMatchHistory.get(i));
            }
        }
        primeMatchHistoryView.setText(sb.toString());
    }

    private void mostrarPrimeMatchHistoricoDialog() {
        String texto = primeMatchHistoryView != null
                ? primeMatchHistoryView.getText().toString()
                : "Historico da fase\nAinda sem calculos.";
        new AlertDialog.Builder(this)
                .setTitle("Historico da fase")
                .setMessage(texto)
                .setPositiveButton("Fechar", null)
                .show();
    }

    private String listarPrimeMatchNumerosMarcados(boolean[][] marcadas) {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (marcadas[r][c]) {
                    if (total > 0) {
                        sb.append(" + ");
                    }
                    sb.append(primeMatchValues[r][c]);
                    total++;
                }
            }
        }
        return sb.toString();
    }

    private void mostrarPrimeMatchResultadoFlutuante(List<PrimeMatchResultadoGrupo> resultados, int saldo,
                                                      int indice, int total) {
        if (primeMatchStage == null) {
            return;
        }
        TextView painel = new TextView(this);
        painel.setText((total > 1 ? indice + "/" + total + "\n" : "")
            + formatarPrimeMatchCenaResultado(resultados, saldo));
        painel.setTextSize(resultados.size() == 1 ? 16 : 14);
        painel.setTypeface(null, Typeface.BOLD);
        painel.setGravity(Gravity.CENTER);
        painel.setTextColor(Color.WHITE);
        painel.setLineSpacing(dpUi(2), 1.0f);
        painel.setShadowLayer(dpUi(3), 0, dpUi(1), Color.argb(178, 0, 0, 0));
        painel.setPadding(dpUi(16), dpUi(11), dpUi(16), dpUi(11));
        int corBase = saldo == 0
                ? Color.parseColor("#475569")
                : Color.parseColor("#15865C");
        painel.setBackground(criarPrimeMatchBadgePontuacao(corBase));
        painel.setAlpha(0f);
        painel.setScaleX(0.78f);
        painel.setScaleY(0.78f);
        painel.setMaxLines(total > 1 ? 6 : 5);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        lp.leftMargin = dpUi(20);
        lp.rightMargin = dpUi(20);
        primeMatchStage.addView(painel, lp);

        painel.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(-dpUi(4))
                .setDuration(220)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.9f))
                .withEndAction(() -> painel.animate()
                        .alpha(0f)
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .translationY(-dpUi(28))
                        .setStartDelay(1420)
                        .setDuration(300)
                        .withEndAction(() -> {
                            if (painel.getParent() instanceof ViewGroup) {
                                ((ViewGroup) painel.getParent()).removeView(painel);
                            }
                        })
                        .start())
                .start();
    }

    private String formatarPrimeMatchCenaResultado(List<PrimeMatchResultadoGrupo> resultados, int saldo) {
        if (resultados.size() == 1) {
            return formatarPrimeMatchCenaLinha(resultados.get(0), true);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(rotuloPrimeMatchCombo(resultados));
        sb.append(" • ").append(resultados.size()).append(" grupos");
        int limite = Math.min(4, resultados.size());
        for (int i = 0; i < limite; i++) {
            sb.append("\nG").append(i + 1).append(": ")
                    .append(formatarPrimeMatchCenaLinha(resultados.get(i), false));
        }
        if (resultados.size() > limite) {
            sb.append("\n+").append(resultados.size() - limite).append(" grupo(s) na cadeia");
        }
        sb.append(saldo > 0 ? "\nTOTAL + " + saldo + " pontos" : "\nSEM PONTOS • explodiu sem desconto");
        return sb.toString();
    }

    private String rotuloPrimeMatchCombo(List<PrimeMatchResultadoGrupo> resultados) {
        boolean temPrimo = false;
        boolean temComposto = false;
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            if (resultado.primo) {
                temPrimo = true;
            } else {
                temComposto = true;
            }
        }
        if (temPrimo && temComposto) {
            return "COMBO MISTO";
        }
        return temPrimo ? "COMBO PRIMO" : "COMBO COMPOSTO";
    }

    private String formatarPrimeMatchCenaLinha(PrimeMatchResultadoGrupo resultado, boolean destaque) {
        String calculo = listarPrimeMatchNumerosMarcados(resultado.grupo.marcadas);
        if (resultado.ajusteUsado) {
            calculo += " + ajuste " + resultado.ajusteValor;
        }
        calculo += " = " + resultado.somaCalculada;
        if (!resultado.primo) {
            return (destaque ? "COMPOSTO\n" : "")
                    + calculo
                    + (destaque ? "\n0 pontos • as peças explodem" : " • composto");
        }

        if (resultado.especialExtra > 0) {
            calculo += "\n" + nomePrimeMatchEspecialPreto(resultado.especialTipo)
                    + ": +" + resultado.especialExtra + " => " + resultado.somaFinal;
        }

        String saldo = "+" + resultado.saldo;
        if (resultado.multiplicadorUsado) {
            saldo += " com x2";
        }
        return (destaque ? "PRIMO\n" : "")
                + calculo
                + (destaque
                ? "\n" + saldo + " pontos • vira peça preta " + resultado.valorPreta
                : " • " + saldo);
    }

    private Drawable criarPrimeMatchBadgePontuacao(int corBase) {
        GradientDrawable corpo = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        misturarPrimeMatchCor(corBase, Color.WHITE, 0.34f),
                        corBase,
                        misturarPrimeMatchCor(corBase, Color.BLACK, 0.24f)
                }
        );
        corpo.setCornerRadius(dpUi(24));
        corpo.setStroke(dpUi(1), withAlpha(Color.WHITE, 160));

        GradientDrawable brilho = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(Color.WHITE, 150), Color.TRANSPARENT}
        );
        brilho.setCornerRadius(dpUi(24));

        LayerDrawable camadas = new LayerDrawable(new Drawable[]{corpo, brilho});
        camadas.setLayerInset(1, dpUi(2), dpUi(2), dpUi(2), dpUi(17));
        return camadas;
    }

    private int contarPrimeMatchMarcadas(boolean[][] marcadas) {
        int total = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (marcadas[r][c]) {
                    total++;
                }
            }
        }
        return total;
    }

    private int somarPrimeMatchMarcadas(boolean[][] marcadas) {
        int soma = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (marcadas[r][c]) {
                    soma += primeMatchValues[r][c];
                }
            }
        }
        return soma;
    }

    private void limparPrimeMatchMarcadas(boolean[][] marcadas) {
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (marcadas[r][c]) {
                    primeMatchValues[r][c] = 0;
                    primeMatchColors[r][c] = -1;
                }
            }
        }
    }

    private void mostrarPrimeMatchMarcaTexto(boolean[][] marcadas) {
        if (primeMatchStage == null || primeMatchCells == null) {
            return;
        }
        limparPrimeMatchMarcaTexto();
        primeMatchStage.post(() -> {
            if (primeMatchStage == null || primeMatchCells == null) {
                return;
            }
            for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
                int inicio = 0;
                while (inicio < PRIME_MATCH_COLS) {
                    while (inicio < PRIME_MATCH_COLS && !marcadas[r][inicio]) {
                        inicio++;
                    }
                    int fim = inicio;
                    while (fim < PRIME_MATCH_COLS && marcadas[r][fim]) {
                        fim++;
                    }
                    if (fim - inicio >= PRIME_MATCH_MIN_CHAIN) {
                        adicionarPrimeMatchMarcaTexto(r, inicio, r, fim - 1, true);
                    }
                    inicio = Math.max(fim, inicio + 1);
                }
            }

            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                int inicio = 0;
                while (inicio < PRIME_MATCH_ROWS) {
                    while (inicio < PRIME_MATCH_ROWS && !marcadas[inicio][c]) {
                        inicio++;
                    }
                    int fim = inicio;
                    while (fim < PRIME_MATCH_ROWS && marcadas[fim][c]) {
                        fim++;
                    }
                    if (fim - inicio >= PRIME_MATCH_MIN_CHAIN) {
                        adicionarPrimeMatchMarcaTexto(inicio, c, fim - 1, c, false);
                    }
                    inicio = Math.max(fim, inicio + 1);
                }
            }
        });
    }

    private void adicionarPrimeMatchMarcaTexto(int rowInicio, int colInicio, int rowFim, int colFim, boolean horizontal) {
        TextView primeira = primeMatchCells[rowInicio][colInicio];
        TextView ultima = primeMatchCells[rowFim][colFim];
        if (primeira.getWidth() == 0 || primeira.getHeight() == 0 || ultima.getWidth() == 0 || ultima.getHeight() == 0) {
            return;
        }

        int[] stageLoc = new int[2];
        int[] firstLoc = new int[2];
        int[] lastLoc = new int[2];
        primeMatchStage.getLocationOnScreen(stageLoc);
        primeira.getLocationOnScreen(firstLoc);
        ultima.getLocationOnScreen(lastLoc);

        int left = Math.min(firstLoc[0], lastLoc[0]) - stageLoc[0] - dpUi(3);
        int top = Math.min(firstLoc[1], lastLoc[1]) - stageLoc[1] - dpUi(2);
        int right = Math.max(firstLoc[0] + primeira.getWidth(), lastLoc[0] + ultima.getWidth()) - stageLoc[0] + dpUi(3);
        int bottom = Math.max(firstLoc[1] + primeira.getHeight(), lastLoc[1] + ultima.getHeight()) - stageLoc[1] + dpUi(2);

        View marca = new View(this);
        marca.setClickable(false);
        marca.setFocusable(false);
        marca.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        marca.setBackground(criarPrimeMatchMarcaTextoDrawable(horizontal));
        marca.setAlpha(0f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Math.max(dpUi(24), right - left),
                Math.max(dpUi(24), bottom - top)
        );
        lp.leftMargin = left;
        lp.topMargin = top;
        primeMatchStage.addView(marca, lp);
        primeMatchHighlightViews.add(marca);
        marca.animate()
                .alpha(0.52f)
                .setDuration(180)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private Drawable criarPrimeMatchMarcaTextoDrawable(boolean horizontal) {
        GradientDrawable corpo = new GradientDrawable(
                horizontal ? GradientDrawable.Orientation.LEFT_RIGHT : GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        withAlpha(Color.parseColor("#F7D84D"), 38),
                        withAlpha(Color.parseColor("#FFE66D"), 176),
                        withAlpha(Color.parseColor("#F59E0B"), 126),
                        withAlpha(Color.parseColor("#F7D84D"), 42)
                }
        );
        corpo.setCornerRadius(dpUi(18));
        corpo.setStroke(dpUi(1), withAlpha(Color.WHITE, 150));
        return corpo;
    }

    private void mostrarPrimeMatchBombas(boolean[][] marcadas) {
        if (primeMatchStage == null || primeMatchCells == null) {
            return;
        }
        primeMatchStage.post(() -> {
            if (primeMatchStage == null || primeMatchCells == null) {
                return;
            }
            for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
                for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                    if (marcadas[r][c]) {
                        adicionarPrimeMatchBomba(r, c);
                    }
                }
            }
        });
    }

    private void adicionarPrimeMatchBomba(int row, int col) {
        TextView cell = primeMatchCells[row][col];
        if (cell.getWidth() == 0 || cell.getHeight() == 0) {
            return;
        }

        int[] stageLoc = new int[2];
        int[] cellLoc = new int[2];
        primeMatchStage.getLocationOnScreen(stageLoc);
        cell.getLocationOnScreen(cellLoc);

        int tamanho = dpUi(24);
        FrameLayout bomba = criarPrimeMatchBombaView(tamanho);
        bomba.setAlpha(0f);
        bomba.setScaleX(0.62f);
        bomba.setScaleY(0.62f);
        bomba.setRotation(-9f);
        bomba.setClickable(false);
        bomba.setFocusable(false);
        bomba.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tamanho, tamanho);
        lp.leftMargin = cellLoc[0] - stageLoc[0] + cell.getWidth() - tamanho + dpUi(2);
        lp.topMargin = cellLoc[1] - stageLoc[1] - dpUi(2);
        primeMatchStage.addView(bomba, lp);
        primeMatchHighlightViews.add(bomba);

        int atraso = (row + col) * 30;
        bomba.animate()
                .alpha(1f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .rotation(0f)
                .setStartDelay(atraso)
                .setDuration(220)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .withEndAction(() -> bomba.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .rotation(5f)
                        .setDuration(360)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start())
                .start();
    }

    private FrameLayout criarPrimeMatchBombaView(int tamanho) {
        FrameLayout bomba = new FrameLayout(this);
        bomba.setClipChildren(false);
        bomba.setClipToPadding(false);

        View corpo = new View(this);
        corpo.setBackground(criarPrimeMatchBombaCorpoDrawable());
        FrameLayout.LayoutParams corpoLp = new FrameLayout.LayoutParams(
                Math.max(1, tamanho - dpUi(5)),
                Math.max(1, tamanho - dpUi(5)),
                Gravity.BOTTOM | Gravity.LEFT
        );
        corpoLp.leftMargin = dpUi(1);
        corpoLp.bottomMargin = 0;
        bomba.addView(corpo, corpoLp);

        View brilho = new View(this);
        GradientDrawable brilhoDrawable = new GradientDrawable();
        brilhoDrawable.setShape(GradientDrawable.OVAL);
        brilhoDrawable.setColor(withAlpha(Color.WHITE, 158));
        brilho.setBackground(brilhoDrawable);
        FrameLayout.LayoutParams brilhoLp = new FrameLayout.LayoutParams(dpUi(6), dpUi(4));
        brilhoLp.leftMargin = dpUi(6);
        brilhoLp.topMargin = dpUi(8);
        bomba.addView(brilho, brilhoLp);

        View pavio = new View(this);
        GradientDrawable pavioDrawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#6B3A16"),
                        Color.parseColor("#F4B23D"),
                        Color.parseColor("#FFF1A8")
                }
        );
        pavioDrawable.setCornerRadius(dpUi(3));
        pavio.setBackground(pavioDrawable);
        pavio.setRotation(-35f);
        FrameLayout.LayoutParams pavioLp = new FrameLayout.LayoutParams(dpUi(10), dpUi(4));
        pavioLp.leftMargin = tamanho - dpUi(11);
        pavioLp.topMargin = dpUi(4);
        bomba.addView(pavio, pavioLp);

        View faisca = new View(this);
        GradientDrawable faiscaDrawable = new GradientDrawable();
        faiscaDrawable.setShape(GradientDrawable.OVAL);
        faiscaDrawable.setColor(Color.parseColor("#FFE66D"));
        faiscaDrawable.setStroke(dpUi(1), Color.parseColor("#F97316"));
        faisca.setBackground(faiscaDrawable);
        FrameLayout.LayoutParams faiscaLp = new FrameLayout.LayoutParams(dpUi(7), dpUi(7));
        faiscaLp.leftMargin = tamanho - dpUi(5);
        faiscaLp.topMargin = dpUi(1);
        bomba.addView(faisca, faiscaLp);

        return bomba;
    }

    private Drawable criarPrimeMatchBombaCorpoDrawable() {
        GradientDrawable sombra = new GradientDrawable();
        sombra.setShape(GradientDrawable.OVAL);
        sombra.setColor(Color.argb(118, 0, 0, 0));

        GradientDrawable corpo = new GradientDrawable();
        corpo.setShape(GradientDrawable.OVAL);
        corpo.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        corpo.setGradientCenter(0.34f, 0.26f);
        corpo.setGradientRadius(dpUi(18));
        corpo.setColors(new int[]{
                Color.parseColor("#4B5563"),
                Color.parseColor("#111827"),
                Color.parseColor("#020617")
        });
        corpo.setStroke(dpUi(1), withAlpha(Color.WHITE, 92));

        LayerDrawable camadas = new LayerDrawable(new Drawable[]{sombra, corpo});
        camadas.setLayerInset(0, dpUi(2), dpUi(4), 0, 0);
        camadas.setLayerInset(1, 0, 0, dpUi(2), dpUi(2));
        return camadas;
    }

    private void desvanecerPrimeMatchMarcaTexto() {
        if (primeMatchHighlightViews.isEmpty()) {
            return;
        }
        ArrayList<View> atuais = new ArrayList<>(primeMatchHighlightViews);
        primeMatchHighlightViews.clear();
        for (View marca : atuais) {
            marca.animate().cancel();
            marca.animate()
                    .alpha(0f)
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(240)
                    .withEndAction(() -> {
                        if (marca.getParent() instanceof ViewGroup) {
                            ((ViewGroup) marca.getParent()).removeView(marca);
                        }
                    })
                    .start();
        }
    }

    private void limparPrimeMatchMarcaTexto() {
        if (primeMatchHighlightViews.isEmpty()) {
            return;
        }
        ArrayList<View> atuais = new ArrayList<>(primeMatchHighlightViews);
        primeMatchHighlightViews.clear();
        for (View marca : atuais) {
            marca.animate().cancel();
            if (marca.getParent() instanceof ViewGroup) {
                ((ViewGroup) marca.getParent()).removeView(marca);
            }
        }
    }

    private void animarPrimeMatchDestaqueCalculo(boolean[][] calculoMarcadas, boolean[][] explosaoMarcadas, Runnable aoTerminar) {
        mostrarPrimeMatchMarcaTexto(calculoMarcadas);
        mostrarPrimeMatchBombas(explosaoMarcadas);
        int[] paleta = primeMatchPaleta();
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (!calculoMarcadas[r][c]) {
                    continue;
                }
                TextView cell = primeMatchCells[r][c];
                boolean preta = primeMatchEhCorPreta(primeMatchColors[r][c]);
                int cor = preta
                        ? Color.parseColor("#101217")
                        : paleta[Math.max(0, primeMatchColors[r][c]) % paleta.length];
                int atraso = (r + c) * 28;

                cell.animate().cancel();
                cell.bringToFront();
                cell.setTextColor(Color.parseColor("#FFF2B8"));
                cell.setBackground(criarPrimeMatchBolinhaVidro(cor, true));
                cell.setElevation(dpUi(11));
                cell.animate()
                        .scaleX(preta ? 1.22f : 1.16f)
                        .scaleY(preta ? 1.22f : 1.16f)
                        .setStartDelay(atraso)
                        .setDuration(360)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.65f))
                        .withEndAction(() -> cell.animate()
                                .scaleX(preta ? 1.12f : 1.08f)
                                .scaleY(preta ? 1.12f : 1.08f)
                                .setDuration(420)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start())
                        .start();
            }
        }

        primeMatchGrid.postDelayed(aoTerminar, PRIME_MATCH_PREVIEW_MS);
    }

    private void animarPrimeMatchExplosao(boolean[][] marcadas, Runnable aoTerminar) {
        desvanecerPrimeMatchMarcaTexto();
        int[] paleta = primeMatchPaleta();
        int totalMarcadas = contarPrimeMatchMarcadas(marcadas);
        int fragmentosPorPeca = totalMarcadas > 18 ? 3 : (totalMarcadas > 9 ? 4 : 7);
        int maiorAtraso = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                if (!marcadas[r][c]) {
                    continue;
                }
                TextView cell = primeMatchCells[r][c];
                int cor = primeMatchEhCorPreta(primeMatchColors[r][c])
                        ? Color.parseColor("#101217")
                        : paleta[Math.max(0, primeMatchColors[r][c]) % paleta.length];
                int corExplosao = misturarPrimeMatchCor(cor, Color.WHITE, 0.28f);
                int atraso = (r + c) * 18;
                maiorAtraso = Math.max(maiorAtraso, atraso);

                cell.animate().cancel();
                cell.bringToFront();
                adicionarPrimeMatchClaraoExplosao(r, c, corExplosao, atraso);
                adicionarPrimeMatchFragmentosExplosao(r, c, cor, atraso, fragmentosPorPeca);
                cell.setBackground(criarPrimeMatchBolinhaVidro(corExplosao, true));
                cell.setElevation(dpUi(9));
                cell.animate()
                        .scaleX(1.52f)
                        .scaleY(1.52f)
                        .alpha(0f)
                        .rotationBy((r + c) % 2 == 0 ? 18f : -18f)
                        .setStartDelay(atraso)
                        .setDuration(270)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .start();
            }
        }

        primeMatchGrid.postDelayed(aoTerminar, maiorAtraso + 310L);
    }

    private void adicionarPrimeMatchClaraoExplosao(int row, int col, int cor, int atraso) {
        if (primeMatchStage == null || primeMatchCells == null) {
            return;
        }
        TextView cell = primeMatchCells[row][col];
        if (cell.getWidth() == 0 || cell.getHeight() == 0) {
            return;
        }

        int[] stageLoc = new int[2];
        int[] cellLoc = new int[2];
        primeMatchStage.getLocationOnScreen(stageLoc);
        cell.getLocationOnScreen(cellLoc);

        int tamanho = Math.max(cell.getWidth(), cell.getHeight()) + dpUi(14);
        View clarao = new View(this);
        clarao.setClickable(false);
        clarao.setFocusable(false);
        clarao.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        clarao.setBackground(criarPrimeMatchClaraoDrawable(cor));
        clarao.setAlpha(0.72f);
        clarao.setScaleX(0.36f);
        clarao.setScaleY(0.36f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tamanho, tamanho);
        lp.leftMargin = cellLoc[0] - stageLoc[0] + (cell.getWidth() - tamanho) / 2;
        lp.topMargin = cellLoc[1] - stageLoc[1] + (cell.getHeight() - tamanho) / 2;
        primeMatchStage.addView(clarao, lp);

        clarao.animate()
                .alpha(0f)
                .scaleX(1.58f)
                .scaleY(1.58f)
                .setStartDelay(atraso)
                .setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    if (clarao.getParent() instanceof ViewGroup) {
                        ((ViewGroup) clarao.getParent()).removeView(clarao);
                    }
                })
                .start();
    }

    private Drawable criarPrimeMatchClaraoDrawable(int cor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setGradientCenter(0.5f, 0.5f);
        drawable.setGradientRadius(dpUi(42));
        drawable.setColors(new int[]{
                withAlpha(Color.WHITE, 230),
                withAlpha(misturarPrimeMatchCor(cor, Color.WHITE, 0.45f), 158),
                Color.TRANSPARENT
        });
        return drawable;
    }

    private void adicionarPrimeMatchFragmentosExplosao(int row, int col, int cor, int atraso, int quantidade) {
        if (primeMatchStage == null || primeMatchCells == null) {
            return;
        }
        TextView cell = primeMatchCells[row][col];
        if (cell.getWidth() == 0 || cell.getHeight() == 0) {
            return;
        }

        int[] stageLoc = new int[2];
        int[] cellLoc = new int[2];
        primeMatchStage.getLocationOnScreen(stageLoc);
        cell.getLocationOnScreen(cellLoc);

        int centroX = cellLoc[0] - stageLoc[0] + cell.getWidth() / 2;
        int centroY = cellLoc[1] - stageLoc[1] + cell.getHeight() / 2;
        int base = misturarPrimeMatchCor(cor, Color.WHITE, 0.24f);
        for (int i = 0; i < quantidade; i++) {
            int tamanho = dpUi(4 + (i % 3));
            View fragmento = new View(this);
            fragmento.setClickable(false);
            fragmento.setFocusable(false);
            fragmento.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            fragmento.setBackground(criarPrimeMatchFragmentoDrawable(i % 2 == 0
                    ? base
                    : misturarPrimeMatchCor(cor, Color.WHITE, 0.55f)));
            fragmento.setAlpha(0.95f);
            fragmento.setScaleX(0.72f);
            fragmento.setScaleY(0.72f);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tamanho, tamanho);
            lp.leftMargin = centroX - tamanho / 2;
            lp.topMargin = centroY - tamanho / 2;
            primeMatchStage.addView(fragmento, lp);

            double angulo = (Math.PI * 2d * i / Math.max(1, quantidade)) + ((row + col) * 0.17d);
            float distancia = dpUi(20 + (i % 4) * 6);
            float destinoX = (float) Math.cos(angulo) * distancia;
            float destinoY = (float) Math.sin(angulo) * distancia + dpUi(8);
            fragmento.animate()
                    .translationX(destinoX)
                    .translationY(destinoY)
                    .alpha(0f)
                    .scaleX(0.28f)
                    .scaleY(0.28f)
                    .rotationBy((i % 2 == 0 ? 1 : -1) * 110f)
                    .setStartDelay(atraso + 30L)
                    .setDuration(430)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> {
                        if (fragmento.getParent() instanceof ViewGroup) {
                            ((ViewGroup) fragmento.getParent()).removeView(fragmento);
                        }
                    })
                    .start();
        }
    }

    private Drawable criarPrimeMatchFragmentoDrawable(int cor) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        misturarPrimeMatchCor(cor, Color.WHITE, 0.66f),
                        cor,
                        misturarPrimeMatchCor(cor, Color.BLACK, 0.24f)
                }
        );
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setStroke(dpUi(1), withAlpha(Color.WHITE, 118));
        return drawable;
    }

    private void animarPrimeMatchEntradaTabuleiro() {
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                TextView cell = primeMatchCells[r][c];
                int atraso = (PRIME_MATCH_ROWS - 1 - r) * 22 + c * 7;
                cell.animate().cancel();
                cell.setAlpha(0.28f);
                cell.setScaleX(0.82f);
                cell.setScaleY(0.82f);
                cell.setTranslationY(-dpUi(18));
                cell.animate()
                        .alpha(primeMatchActive ? 1f : 0.62f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setStartDelay(atraso)
                        .setDuration(240)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.85f))
                        .start();
            }
        }
    }

    private void animarPrimeMatchFusoes(List<PrimeMatchResultadoGrupo> resultados) {
        if (primeMatchCells == null || primeMatchGrid == null) {
            return;
        }
        for (PrimeMatchResultadoGrupo resultado : resultados) {
            if (!resultado.primo || resultado.ancora < 0) {
                continue;
            }
            int row = resultado.ancora / PRIME_MATCH_COLS;
            int col = resultado.ancora % PRIME_MATCH_COLS;
            TextView cell = primeMatchCells[row][col];
            cell.animate().cancel();
            cell.setAlpha(primeMatchActive ? 1f : 0.62f);
            cell.setScaleX(0.68f);
            cell.setScaleY(0.68f);
            cell.setElevation(dpUi(10));
            cell.bringToFront();
            cell.animate()
                    .scaleX(1.18f)
                    .scaleY(1.18f)
                    .setStartDelay(80)
                    .setDuration(190)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.72f))
                    .withEndAction(() -> cell.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start())
                    .start();
        }
    }

    private void animarPrimeMatchReorganizacao() {
        if (primeMatchGrid == null || primeMatchCells == null) {
            return;
        }
        primeMatchGrid.animate().cancel();
        primeMatchGrid.setScaleX(0.98f);
        primeMatchGrid.setScaleY(0.98f);
        primeMatchGrid.setRotation(-1.5f);
        primeMatchGrid.animate()
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.65f))
                .start();

        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                TextView cell = primeMatchCells[r][c];
                int atraso = (r * PRIME_MATCH_COLS + c) * 7;
                cell.animate().cancel();
                cell.setAlpha(0.44f);
                cell.setScaleX(0.86f);
                cell.setScaleY(0.86f);
                cell.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setStartDelay(atraso)
                        .setDuration(210)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
        }
    }

    private void animarPrimeMatchTrocaInvalida(Runnable aoTerminar) {
        for (int idx : primeMatchSelection) {
            int row = idx / PRIME_MATCH_COLS;
            int col = idx % PRIME_MATCH_COLS;
            TextView cell = primeMatchCells[row][col];
            cell.animate().cancel();
            cell.animate()
                    .translationX(dpUi(7))
                    .setDuration(55)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> cell.animate()
                            .translationX(-dpUi(7))
                            .setDuration(70)
                            .withEndAction(() -> cell.animate()
                                    .translationX(0f)
                                    .setDuration(55)
                                    .start())
                            .start())
                    .start();
        }
        primeMatchGrid.postDelayed(aoTerminar, 215L);
    }

    private void mostrarPrimeMatchCelebracao() {
        if (primeMatchStage == null || primeMatchCelebrationShown) {
            return;
        }
        primeMatchCelebrationShown = true;
        tocarPrimeMatchSom(primeMatchSoundVictory);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setClickable(false);
        overlay.setFocusable(false);
        overlay.setClipChildren(false);
        overlay.setClipToPadding(false);
        overlay.setAlpha(0f);
        overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        primeMatchCelebrationOverlay = overlay;

        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        for (int i = 0; i < 16; i++) {
            View raio = new View(this);
            raio.setBackground(criarPrimeMatchRaio());
            raio.setAlpha(0f);
            raio.setScaleX(0.24f);
            raio.setScaleY(0.82f);
            raio.setRotation(i * 11.25f);
            FrameLayout.LayoutParams raioLp = new FrameLayout.LayoutParams(
                    dpUi(178),
                    dpUi(5),
                    Gravity.CENTER
            );
            overlay.addView(raio, raioLp);
            final int atraso = i * 24;
            raio.animate()
                    .alpha(i % 2 == 0 ? 0.88f : 0.58f)
                    .scaleX(1.35f)
                    .setStartDelay(atraso)
                    .setDuration(620)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> raio.animate()
                            .alpha(0.22f)
                            .scaleX(0.96f)
                            .setDuration(520)
                            .start())
                    .start();
        }

        TextView selo = new TextView(this);
        selo.setText("FASE CONCLUÍDA\n"
                + formatarPrimeMatchEstrelas(3)
                + "\n" + primeMatchScore + " pontos");
        selo.setGravity(Gravity.CENTER);
        selo.setTextSize(18);
        selo.setTypeface(null, Typeface.BOLD);
        selo.setTextColor(Color.WHITE);
        selo.setShadowLayer(dpUi(3), 0, dpUi(1), Color.argb(170, 0, 0, 0));
        selo.setPadding(dpUi(18), dpUi(12), dpUi(18), dpUi(12));
        selo.setBackground(criarPrimeMatchBadgePontuacao(Color.parseColor("#E5A72E")));
        selo.setAlpha(0f);
        selo.setScaleX(0.72f);
        selo.setScaleY(0.72f);
        FrameLayout.LayoutParams seloLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        overlay.addView(selo, seloLp);

        primeMatchStage.addView(overlay, overlayLp);
        overlay.animate()
                .alpha(1f)
                .setDuration(220)
                .start();
        selo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(160)
                .setDuration(360)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.85f))
                .start();
    }

    private Drawable criarPrimeMatchRaio() {
        GradientDrawable raio = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.TRANSPARENT,
                        withAlpha(Color.WHITE, 210),
                        Color.parseColor("#F6C34F"),
                        withAlpha(Color.WHITE, 175),
                        Color.TRANSPARENT
                }
        );
        raio.setCornerRadius(dpUi(4));
        return raio;
    }

    private List<PrimeMatchGrupo> encontrarPrimeMatchGruposCombinacoes() {
        ArrayList<PrimeMatchGrupo> grupos = new ArrayList<>();
        for (long mascara : mascarasPrimeMatchCombinacoes()) {
            boolean[][] marcadas = decodificarPrimeMatchMascara(mascara);
            int soma = 0;
            int pretas = 0;
            for (long bits = mascara; bits != 0; bits &= bits - 1) {
                int cell = Long.numberOfTrailingZeros(bits);
                int row = cell / PRIME_MATCH_COLS;
                int col = cell % PRIME_MATCH_COLS;
                soma = (int) Math.min(Integer.MAX_VALUE, (long) soma + primeMatchValues[row][col]);
                if (primeMatchEhCorPreta(primeMatchColors[row][col])) pretas++;
            }
            grupos.add(new PrimeMatchGrupo(mascara, marcadas, soma, Long.bitCount(mascara), pretas));
        }
        return grupos;
    }

    private long[] mascarasPrimeMatchCombinacoes() {
        int[] cores = new int[PRIME_MATCH_ROWS * PRIME_MATCH_COLS];
        long ocupadas = 0;
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                int index = r * PRIME_MATCH_COLS + c;
                cores[index] = primeMatchColors[r][c];
                if (primeMatchCelulaOcupada(r, c)) ocupadas |= 1L << index;
            }
        }
        return matchGroupsNativo(cores, ocupadas);
    }

    private boolean[][] decodificarPrimeMatchMascara(long mascara) {
        boolean[][] marcadas = new boolean[PRIME_MATCH_ROWS][PRIME_MATCH_COLS];
        for (long bits = mascara; bits != 0; bits &= bits - 1) {
            int cell = Long.numberOfTrailingZeros(bits);
            marcadas[cell / PRIME_MATCH_COLS][cell % PRIME_MATCH_COLS] = true;
        }
        return marcadas;
    }

    private boolean[][] encontrarPrimeMatchCombinacoes() {
        long marcadas = 0;
        for (long grupo : mascarasPrimeMatchCombinacoes()) marcadas |= grupo;
        return decodificarPrimeMatchMascara(marcadas);
    }

    private void atualizarPrimeMatchPlacar() {
        if (primeMatchScoreView != null) {
            primeMatchScoreView.setText("Valor\n" + primeMatchScore + " / " + primeMatchTarget);
        }
        if (primeMatchMovesView != null) {
            primeMatchMovesView.setText("Jogadas\n" + Math.max(0, primeMatchMoves));
        }
    }

    private void atualizarPrimeMatchCelulas() {
        if (primeMatchCells == null) {
            return;
        }
        boolean podeTocar = primeMatchActive || primeMatchResolving;
        if (primeMatchGrid != null) {
            primeMatchGrid.setClickable(podeTocar);
            primeMatchGrid.setEnabled(podeTocar);
        }
        int[] paleta = primeMatchPaleta();
        for (int r = 0; r < PRIME_MATCH_ROWS; r++) {
            for (int c = 0; c < PRIME_MATCH_COLS; c++) {
                TextView cell = primeMatchCells[r][c];
                int idx = r * PRIME_MATCH_COLS + c;
                boolean selecionada = primeMatchSelection.contains(idx);
                boolean preta = primeMatchEhCorPreta(primeMatchColors[r][c]);
                int cor = preta
                        ? Color.parseColor("#101217")
                        : paleta[Math.max(0, primeMatchColors[r][c]) % paleta.length];
                int valor = primeMatchValues[r][c];
                cell.animate().cancel();
                cell.setBackground(criarPrimeMatchBolinhaVidro(cor, selecionada || preta));
                cell.setText(valor == 0 ? "" : String.valueOf(valor));
                cell.setTextColor(preta ? Color.parseColor("#F8E5A0") : Color.WHITE);
                cell.setTextSize(valor >= 1000 ? 11 : (valor >= 100 ? 13 : 17));
                cell.setAlpha(primeMatchActive ? 1.0f : 0.62f);
                float escala = selecionada ? 1.12f : (preta ? 1.08f : 1.0f);
                cell.setScaleX(escala);
                cell.setScaleY(escala);
                cell.setRotation(0f);
                cell.setTranslationX(0f);
                cell.setTranslationY(0f);
                cell.setElevation(selecionada ? dpUi(8) : (preta ? dpUi(5) : dpUi(1)));
                cell.setClickable(podeTocar);
                cell.setEnabled(podeTocar);
            }
        }
    }

    private int calcularPrimeMatchPercentual(int valor) {
        return matchPercentNativo(valor, primeMatchTarget);
    }

    private int calcularPrimeMatchEstrelas(int valor) {
        return matchStarsNativo(valor, primeMatchTarget);
    }

    private String formatarPrimeMatchEstrelas(int estrelas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(i < estrelas ? "★" : "☆");
        }
        return sb.toString();
    }

    private void registrarPrimeMatchMelhorValor() {
        if (primeMatchScore > primeMatchBestScore) {
            primeMatchBestScore = primeMatchScore;
            salvarPrimeMatchPoderes();
        }
    }

    private void verificarPrimeMatchFimFase() {
        if (primeMatchScore >= primeMatchTarget) {
            primeMatchActive = false;
            primeMatchResolving = false;
            primeMatchSelection.clear();
            bloquearPrimeMatchRolagem(false);
            registrarPrimeMatchMelhorValor();
            salvarPrimeMatchResultadoFase(3);
            String recompensas = concederPrimeMatchRecompensasVitoria();
            primeMatchStatusView.setText("Fase concluída: "
                    + formatarPrimeMatchEstrelas(3)
                    + " 100% da meta com " + primeMatchScore + " pontos."
                    + (recompensas.isEmpty() ? "" : "\n" + recompensas + "."));
            atualizarPrimeMatchCelulas();
            mostrarPrimeMatchCelebracao();
        } else if (primeMatchMoves <= 0) {
            primeMatchActive = false;
            primeMatchResolving = false;
            primeMatchSelection.clear();
            bloquearPrimeMatchRolagem(false);
            registrarPrimeMatchMelhorValor();
            int estrelas = calcularPrimeMatchEstrelas(primeMatchScore);
            int percentual = calcularPrimeMatchPercentual(primeMatchScore);
            salvarPrimeMatchResultadoFase(estrelas);
            primeMatchStatusView.setText("Fim da fase: "
                    + formatarPrimeMatchEstrelas(estrelas)
                    + " " + percentual + "% da meta. Valor: " + primeMatchScore
                    + ". Melhor tentativa: " + primeMatchBestScore + ".");
            atualizarPrimeMatchBotoesPoder();
            atualizarPrimeMatchCelulas();
        }
    }

    private static class PrimeRunnerNumero {
        float x;
        float y;
        long nextAttack;
        int role;
        final int lane;
        final int value;
        final boolean prime;

        PrimeRunnerNumero(float x, int lane, int value, boolean prime) {
            this.x = x;
            this.lane = lane;
            this.value = value;
            this.prime = prime;
        }
    }

    private class PrimeRunnerView extends View {
        private final ArcadePhase phase = new ArcadePhase(false);
        private Runnable phaseReady;
        private final android.graphics.Bitmap[] veiculos = carregarVeiculos("motorcycle", "car", "car");
        private final android.graphics.Bitmap estrada = carregarImagemArcade("road.jpg");
        private final android.graphics.Bitmap arvore = carregarImagemArcade("road-tree.png");
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF chipRect = new RectF();
        private final ArrayList<PrimeRunnerNumero> numeros = new ArrayList<>();
        private final ArrayList<PrimeShooterShot> disparos = new ArrayList<>();
        private boolean roadFire;
        private float fireX, fireY;
        private long roadShotAt;
        private long lastFrameMs, playTimeMs, nextSpawnMs, pulseUntilMs, burstAt;
        private int previousNumber, pontuacao, bestScore;
        private float carX, carY, touchX, touchY, burstX, burstY;
        private float previousCarX, previousCarY;
        private int pointer = -1;
        private double travel;
        private boolean gameOver, running;
        private String pulseText = "";
        private int pulseColor = Color.WHITE;

        PrimeRunnerView(android.content.Context context) {
            super(context);
            setFocusable(true);
            setClickable(true);
            setContentDescription("Corrida Prima");
            bestScore = getSharedPreferences(PREFS_PRIME_RUNNER, MODE_PRIVATE).getInt(KEY_PR_BEST_SCORE, 0);
            reiniciarPartida();
        }

        private float laneY(int lane) { return getHeight() * (0.59f + 0.12f * lane); }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            carX = w * 0.30f;
            carY = laneY(1);
            previousCarX = carX;
            previousCarY = carY;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int center = (int) laneY(1);
                setSystemGestureExclusionRects(java.util.Collections.singletonList(new android.graphics.Rect(
                    0, Math.max(0, center - dpUi(100)), w, Math.min(h, center + dpUi(100)))));
            }
        }

        void reiniciarPartida() {
            numeros.clear();
            disparos.clear();
            roadFire = false;
            roadShotAt = 0;
            lastFrameMs = 0;
            playTimeMs = 0;
            nextSpawnMs = ArcadeRules.START_MS;
            previousNumber = 0;
            pulseUntilMs = 0;
            burstAt = -1000;
            pontuacao = 0;
            phase.reset(0, 0);
            travel = 0;
            pointer = -1;
            carX = getWidth() * 0.30f;
            carY = laneY(1);
            previousCarX = carX;
            previousCarY = carY;
            gameOver = false;
            running = false;
            pulseText = "";
            invalidate();
        }

        void pausar() {
            running = false;
            roadFire = false;
            pointer = -1;
            lastFrameMs = 0;
            salvarRecorde();
            invalidate();
        }

        @Override
        protected void onDetachedFromWindow() {
            salvarRecorde();
            super.onDetachedFromWindow();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (phase.exitStart >= 0) return true;
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_MOVE) roadFire = false;
            if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2) {
                fireX = (event.getX(0) + event.getX(1)) / 2;
                fireY = (event.getY(0) + event.getY(1)) / 2;
            } else if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 2) {
                float cx = (event.getX(0) + event.getX(1)) / 2;
                float cy = (event.getY(0) + event.getY(1)) / 2;
                roadFire |= Math.hypot(cx - fireX, cy - fireY) >= dpUi(4);
            }
            if (action == MotionEvent.ACTION_DOWN) {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                performClick();
                pointer = event.getPointerId(0);
                touchX = event.getX();
                touchY = event.getY();
            } else if (action == MotionEvent.ACTION_MOVE) {
                int index = event.findPointerIndex(pointer);
                if (index >= 0 && running && !gameOver) {
                    float x = event.getX(index), y = event.getY(index);
                    float left = Math.min(getWidth() * 0.30f, dpUi(44));
                    carX = Math.max(left, Math.min(getWidth() * 0.64f, carX + x - touchX));
                    carY = Math.max(laneY(0), Math.min(laneY(2), carY + y - touchY));
                    touchX = x;
                    touchY = y;
                    invalidate();
                }
            } else if (action == MotionEvent.ACTION_POINTER_UP
                    && event.getPointerId(event.getActionIndex()) == pointer) {
                int next = event.getActionIndex() == 0 ? 1 : 0;
                pointer = event.getPointerId(next);
                touchX = event.getX(next);
                touchY = event.getY(next);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                pointer = -1;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            }
            return true;
        }

        @Override
        public boolean performClick() {
            if (phase.exitStart >= 0) return false;
            super.performClick();
            if (gameOver) reiniciarPartida();
            if (!running) {
                running = true;
                lastFrameMs = 0;
            }
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            long now = android.os.SystemClock.uptimeMillis();
            if (lastFrameMs == 0) lastFrameMs = now;
            long delta = Math.min(ArcadeRules.MAX_FRAME_MS, Math.max(0, now - lastFrameMs));
            lastFrameMs = now;
            if (running && !gameOver) {
                delta = Math.min(delta, phase.duration - phase.elapsed);
                phase.elapsed += (int) delta;
                playTimeMs += delta;
                atualizar(delta / 1000f, now, w);
                completarFase(now, false);
            }

            desenharPista(canvas, w, h);
            for (PrimeShooterShot shot : disparos) {
                float sy = roadY(shot.x, shot.y);
                paint.setColor(shot.hostile ? 0xFFFF806F : 0xFFFFE49A);
                if (shot.hostile) {
                    paint.setStrokeWidth(dpUi(5));
                    canvas.drawLine(shot.x, sy, shot.x + dpUi(34), roadY(shot.x + dpUi(34), shot.y), paint);
                    canvas.drawCircle(shot.x, sy, dpUi(13), paint);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(dpUi(11));
                    paint.setColor(0xFF230914);
                    canvas.drawText(String.valueOf(shot.number), shot.x, sy + dpUi(4), paint);
                } else {
                    canvas.drawRoundRect(shot.x - dpUi(12), sy - dpUi(3), shot.x + dpUi(12), sy + dpUi(3), dpUi(3), dpUi(3), paint);
                }
            }
            desenharNumeros(canvas);
            float departure = phase.exitProgress(now);
            if (departure < 1 || phase.outcome == ArcadePhase.CRASHED) {
                canvas.save();
                canvas.translate(0, roadY(carX, carY) - carY);
                if (phase.outcome != ArcadePhase.CRASHED) canvas.translate((w - carX + dpUi(94)) * departure, 0);
                desenharCorredor(canvas);
                canvas.restore();
            }
            desenharExplosao(canvas, now);
            desenharPlacar(canvas);
            desenharPulso(canvas, w, now);
            if (phase.exitStart >= 0) {
                if (departure < 1) postInvalidateOnAnimation();
                else if (!phase.shown && phaseReady != null) {
                    phase.shown = true;
                    post(() -> { if (phase.shown) phaseReady.run(); });
                }
            } else if (gameOver && now - burstAt >= 420) desenharFim(canvas, w, h);
            else if (gameOver) postInvalidateOnAnimation();
            else if (running) postInvalidateOnAnimation();
            else desenharPausaArcade(canvas, w, h, playTimeMs > 0, paint);
        }

        private void atualizar(float dt, long now, int w) {
            float density = getResources().getDisplayMetrics().density;
            float distance = (float) arcadeRunnerSpeedNativo(phase.pace()) * density * dt;
            travel += distance;
            if (roadFire && playTimeMs >= roadShotAt) {
                disparos.add(new PrimeShooterShot(carX + dpUi(33), carY, 490 * density, 0, false));
                roadShotAt = playTimeMs + 240;
            }
            if (playTimeMs >= nextSpawnMs) {
                int[] spawn = arcadeSpawnNativo(false, phase.pace(), primeMatchRandom.nextInt(), previousNumber);
                previousNumber = spawn[0];
                PrimeRunnerNumero item = new PrimeRunnerNumero(w + dpUi(34), spawn[4], previousNumber, spawn[1] != 0);
                item.y = laneY(item.lane);
                item.role = arcadeEnemyRoleNativo(item.prime, false, phase.index, primeMatchRandom.nextInt());
                item.nextAttack = playTimeMs + 300;
                numeros.add(item);
                nextSpawnMs = playTimeMs + spawn[3];
            }
            for (int i = disparos.size() - 1; i >= 0; i--) {
                PrimeShooterShot shot = disparos.get(i);
                shot.x += shot.vx * dt;
                if (shot.x < -dpUi(22) || shot.x > w + dpUi(22)) { disparos.remove(i); continue; }
                if (shot.hostile) {
                    if (Math.abs(shot.x - carX) < dpUi(25) && Math.abs(shot.y - carY) < dpUi(12)) {
                        disparos.remove(i);
                        gameOver = true;
                        burstAt = now; burstX = carX; burstY = carY;
                        mostrarPulso("Impacto: projétil composto", 0xFFFBA890, now);
                        tocarPrimeMatchSom(primeMatchSoundComposite);
                        completarFase(now, true);
                        return;
                    }
                    continue;
                }
                for (int j = numeros.size() - 1; j >= 0; j--) {
                    PrimeRunnerNumero item = numeros.get(j);
                    if (Math.abs(shot.x - item.x) >= dpUi(25) || Math.abs(shot.y - item.y) >= dpUi(15)) continue;
                    disparos.remove(i);
                    numeros.remove(j);
                    if (!item.prime) {
                        int points = arcadePoints(item.value, false, false);
                        pontuacao = arcadeAddScoreNativo(pontuacao, points);
                        mostrarPulso("Composto " + item.value + "  +" + points, 0xFF93C5FD, now);
                        tocarPrimeMatchSom(primeMatchSoundCombo);
                    } else {
                        mostrarPulso("Primo perdido", 0xFFFCA5A5, now);
                    }
                    burstAt = now; burstX = item.x; burstY = item.y;
                    break;
                }
            }

            for (int i = numeros.size() - 1; i >= 0; i--) {
                PrimeRunnerNumero numero = numeros.get(i);
                float enemyTravel = distance * (numero.role == ARCADE_ARMED ? 1.23f : 1f);
                numero.x -= enemyTravel;
                if (numero.role == ARCADE_ARMED) {
                    float wanted = carY + (float) Math.sin(playTimeMs / 460.0) * dpUi(17);
                    float shift = Math.max(-dpUi(44) * dt, Math.min(dpUi(44) * dt, wanted - numero.y));
                    numero.y = Math.max(laneY(0), Math.min(laneY(2), numero.y + shift));
                    if (playTimeMs >= numero.nextAttack && numero.x < w - dpUi(8)
                            && numero.x > carX + Math.max(dpUi(108), w * .27f)) {
                        disparos.add(new PrimeShooterShot(numero.x - dpUi(23), numero.y, -310 * density, 0, true, numero.value));
                        numero.nextAttack = playTimeMs + 1650;
                    }
                }
                if (arcadeRoadHitNativo((previousCarX - enemyTravel) / density, previousCarY / density,
                        carX / density, carY / density, numero.x / density, numero.y / density,
                        getHeight() / density)) {
                    numeros.remove(i);
                    burstAt = now;
                    burstX = numero.x;
                    burstY = numero.y;
                    if (!numero.prime) {
                        gameOver = true;
                        mostrarPulso("Composto " + numero.value, 0xFFFFB4A2, now);
                        tocarPrimeMatchSom(primeMatchSoundComposite);
                        completarFase(now, true);
                        return;
                    }
                    int pontos = arcadePoints(numero.value, true, true);
                    pontuacao = arcadeAddScoreNativo(pontuacao, pontos);
                    mostrarPulso("Primo " + numero.value + "  +" + pontos, 0xFFFFE08A, now);
                    tocarPrimeMatchSom(primeMatchSoundPrime);
                } else if (numero.x < -dpUi(34)) {
                    numeros.remove(i);
                    if (!numero.prime) {
                        int pontos = arcadePoints(numero.value, false, false);
                        pontuacao = arcadeAddScoreNativo(pontuacao, pontos);
                        mostrarPulso("Desviou " + numero.value + "  +" + pontos, 0xFFA7F3D0, now);
                    }
                }
            }
            previousCarX = carX;
            previousCarY = carY;
        }

        private void completarFase(long now, boolean crashed) {
            if (!phase.conclude(now, pontuacao, crashed, arcadePrefs(false))) return;
            running = false;
            pointer = -1;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            salvarRecorde();
            if (phase.outcome == ArcadePhase.PASSED) tocarPrimeMatchSom(primeMatchSoundVictory);
        }

        private void avancarFase() {
            if (phase.outcome != ArcadePhase.PASSED) return;
            iniciarFase(phase.index + 1, pontuacao);
        }

        private void repetirFase() {
            iniciarFase(phase.index, phase.startingScore);
        }

        private void iniciarFaseEscolhida(int stage) {
            iniciarFase(stage, 0);
            running = false;
        }

        private void iniciarFase(int stage, int baseScore) {
            reiniciarPartida();
            phase.reset(stage, baseScore);
            pontuacao = baseScore;
            playTimeMs = (stage % 2) * 28000L;
            nextSpawnMs = playTimeMs + ArcadeRules.START_MS;
            running = true;
            invalidate();
        }

        private void salvarRecorde() {
            if (pontuacao > bestScore) {
                bestScore = pontuacao;
                getSharedPreferences(PREFS_PRIME_RUNNER, MODE_PRIVATE).edit().putInt(KEY_PR_BEST_SCORE, bestScore).apply();
            }
        }

        private void mostrarPulso(String texto, int cor, long now) {
            pulseText = texto;
            pulseColor = cor;
            pulseUntilMs = now + 850;
        }

        private float noite() {
            double phase = (playTimeMs % 56000L) / 56000.0;
            return (float) Math.max(0, Math.min(1, (0.5 - 0.5 * Math.cos(2 * Math.PI * phase) - 0.20) / 0.60));
        }

        private float roadY(float x, float y) {
            float along = Math.max(-.5f, Math.min(.5f, x / Math.max(1f, getWidth()) - .5f));
            float center = getHeight() * .70f;
            return center + (y - center) * (1 - .38f * along) - getHeight() * .028f * along;
        }

        private void desenharPista(Canvas c, int w, int h) {
            c.drawColor(0xFF55717E);
            paint.setAlpha(255);
            if (estrada != null) {
                int end = (int) (estrada.getHeight() * 0.79f);
                float sceneHeight = h * .68f;
                float scale = Math.max(w / (float) estrada.getWidth(), sceneHeight / end);
                float sw = estrada.getWidth() * scale;
                float shift = (float) (0.5 + 0.42 * Math.sin(travel / 10500));
                float left = -(sw - w) * shift;
                c.save();
                c.clipRect(0, 0, w, sceneHeight);
                c.drawBitmap(estrada, new android.graphics.Rect(0, 0, estrada.getWidth(), end),
                    new RectF(left, sceneHeight - end * scale, left + sw, sceneHeight), paint);
                c.restore();
            }

            paint.setColor(0xFF566345);
            Path ground = roadPolygon(w, h * .47f, h * 1.06f);
            c.drawPath(ground, paint);
            if (arvore != null) {
                float gap = Math.max(dpUi(138), w / 3f);
                float cycle = gap * 6;
                for (int depth = 0; depth < 3; depth++) {
                    for (int i = 0; i < 6; i++) {
                        float x = (float) (((i * gap + depth * gap / 3 - travel * (.12 + depth * .21))
                            % cycle + cycle) % cycle) - gap;
                        float size = dpUi(56 + depth * 37 + (i * 31) % 36);
                        float y = roadY(x + size / 2, h * (.43f + depth * .025f));
                        paint.setAlpha(155 + 50 * depth);
                        c.drawBitmap(arvore, null, new RectF(x, y - size, x + size, y), paint);
                    }
                }
                paint.setAlpha(255);
            }
            float far = h * .515f, near = h * .965f;
            Path asphalt = roadPolygon(w, far, near);
            paint.setShader(new LinearGradient(0, far, 0, h, 0xFF484D48, 0xFF222827, Shader.TileMode.CLAMP));
            c.drawPath(asphalt, paint);
            paint.setShader(null);
            if (estrada != null) {
                c.save();
                c.clipPath(asphalt);
                paint.setAlpha(65);
                c.drawBitmap(estrada, new android.graphics.Rect(0, (int) (estrada.getHeight() * 0.84f),
                    estrada.getWidth(), estrada.getHeight()), new RectF(0, h * .46f, w, h * 1.02f), paint);
                paint.setAlpha(255);
                c.restore();
            }
            paint.setColor(0xFFE4D3A3);
            paint.setStrokeWidth(dpUi(2));
            c.drawLine(0, roadY(0, far), w, roadY(w, far), paint);
            c.drawLine(0, roadY(0, near), w, roadY(w, near), paint);
            paint.setColor(0xB8E9E7DB);
            for (int lane = 0; lane < 2; lane++) {
                float y = (laneY(lane) + laneY(lane + 1)) * 0.5f;
                float stride = dpUi(60 + lane * 22);
                float phase = (float) (travel * (.75 + lane * .24) % stride);
                for (float x = -phase; x < w; x += stride) {
                    float end = x + stride * (.48f - .14f * x / w);
                    c.drawLine(x, roadY(x, y), end, roadY(end, y), paint);
                }
            }
            paint.setColor(withAlpha(0xFF071229, (int) (194 * noite())));
            c.drawRect(0, 0, w, h, paint);
        }

        private Path roadPolygon(int width, float far, float near) {
            Path path = new Path();
            path.moveTo(0, roadY(0, far));
            path.lineTo(width, roadY(width, far));
            path.lineTo(width, roadY(width, near));
            path.lineTo(0, roadY(0, near));
            path.close();
            return path;
        }

        private void desenharNumeros(Canvas c) {
            float radius = Math.min(dpUi(21), getHeight() * 0.049f);
            for (PrimeRunnerNumero n : numeros) {
                float y = roadY(n.x, n.y);
                if (n.role == ARCADE_ARMED) {
                    c.save();
                    c.scale(-1, 1, n.x, y);
                    paint.setColor(Color.WHITE);
                    desenharVeiculo(c, veiculos[2], new RectF(n.x - dpUi(39), y - dpUi(21), n.x + dpUi(39), y + dpUi(21)), paint);
                    c.restore();
                    paint.setColor(0xFFEC784F);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dpUi(2));
                    c.drawRoundRect(n.x - radius - dpUi(8), y - radius - dpUi(4), n.x + radius + dpUi(8), y + radius + dpUi(4),
                        dpUi(8), dpUi(8), paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(n.prime ? 0xFF137769 : 0xFFA54233);
                c.drawCircle(n.x, y, radius, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dpUi(2));
                paint.setColor(n.prime ? 0xFF9EEAD7 : 0xFFFFCFB3);
                c.drawCircle(n.x, y, radius - dpUi(1), paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(dpUi(16));
                c.drawText(String.valueOf(n.value), n.x, y - (paint.ascent() + paint.descent()) / 2, paint);
            }
            paint.setTypeface(Typeface.DEFAULT);
        }

        private void desenharCorredor(Canvas c) {
            int tier = arcadeVehicle(phase.index);
            float body = Math.min(dpUi(24), getHeight() * 0.053f);
            if (noite() > 0.01f) {
                Path luz = new Path();
                luz.moveTo(carX + dpUi(30), carY);
                luz.lineTo(carX + dpUi(155), carY - body);
                luz.lineTo(carX + dpUi(155), carY + body);
                luz.close();
                paint.setShader(new LinearGradient(carX + dpUi(30), carY, carX + dpUi(155), carY,
                    withAlpha(0xFFFFEBAC, (int) (128 * noite())), 0x00FFEBAC, Shader.TileMode.CLAMP));
                c.drawPath(luz, paint);
                paint.setShader(null);
            }
            paint.setColor(0x80000000);
            c.drawOval(carX - dpUi(33), carY + body - dpUi(7), carX + dpUi(33), carY + body + dpUi(3), paint);
            if (playTimeMs > 0) desenharEscapeArcade(c, carX - dpUi(37), carY + body * 0.45f, playTimeMs, false, paint);
            paint.setColor(Color.WHITE);
            RectF area = new RectF(carX - dpUi(41), carY - body, carX + dpUi(41), carY + body);
            if (desenharVeiculo(c, veiculos[tier], area, paint)) {
                desenharRodas(c, veiculos[tier], area, tier);
            } else {
                paint.setColor(0xFF578DBC);
                c.drawRoundRect(area, dpUi(8), dpUi(8), paint);
            }
        }

        private void desenharExplosao(Canvas c, long now) {
            float t = (now - burstAt) / 420f;
            if (t < 0 || t >= 1) return;
            c.save();
            c.translate(0, roadY(burstX, burstY) - burstY);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpUi(3) * (1 - t) + 1);
            paint.setColor(withAlpha(gameOver ? 0xFFFF8066 : 0xFFFFDA81, (int) (230 * (1 - t))));
            c.drawCircle(burstX, burstY, dpUi(8) + dpUi(53) * t, paint);
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI / 7;
                float distance = dpUi(16 + (i * 29) % 53) * t;
                float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
                c.drawLine(burstX + distance * dx, burstY + distance * dy,
                    burstX + (distance + dpUi(10) * (1 - t)) * dx,
                    burstY + (distance + dpUi(10) * (1 - t)) * dy, paint);
            }
            paint.setStyle(Paint.Style.FILL);
            c.restore();
        }

        private void desenharRodas(Canvas canvas, android.graphics.Bitmap sprite, RectF area, int tier) {
            float scale = Math.min(area.width() / sprite.getWidth(), area.height() / sprite.getHeight());
            float width = sprite.getWidth() * scale;
            float height = sprite.getHeight() * scale;
            float left = area.centerX() - width / 2f;
            float y = area.bottom - height * (tier == 0 ? 0.33f : 0.30f);
            paint.setColor(0x4DFFFFFF);
            paint.setStrokeWidth(dpUi(1));
            for (int i = 0; i < 2; i++) {
                float hub = i == 0 ? (tier == 0 ? 0.23f : 0.18f) : (tier == 0 ? 0.81f : 0.77f);
                float x = left + width * hub;
                canvas.save();
                canvas.rotate((float) (Math.toDegrees(travel / dpUi(9)) % 360), x, y);
                float raio = width * 0.085f;
                canvas.drawLine(x - raio, y, x + raio, y, paint);
                canvas.drawLine(x, y - raio, x, y + raio, paint);
                canvas.restore();
            }
        }

        private void desenharPlacar(Canvas canvas) {
            desenharChip(canvas, "Pontos " + formatarPontuacaoArcade(phase.points(pontuacao)), dpUi(12), dpUi(12), Color.parseColor("#0F766E"));
            desenharChip(canvas, "Tempo " + phase.remainingSeconds() + " s", dpUi(12), dpUi(54), Color.parseColor("#1D4ED8"));
            int target = arcadePhaseTarget(phase.index, false);
            String label = "Fase " + (phase.index + 1) + (target > 0 ? "  •  Meta " + formatarPontuacaoArcade(target) : "");
            desenharChip(canvas, label, dpUi(12), dpUi(96), 0xFF293D49);
        }

        private void desenharChip(Canvas canvas, String texto, float left, float top, int cor) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(cor, 218));
            chipRect.set(left, top, left + dpUi(156), top + dpUi(34));
            canvas.drawRoundRect(chipRect, dpUi(10), dpUi(10), paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(dpUi(13));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText(texto, chipRect.centerX(), chipRect.centerY() - (fm.ascent + fm.descent) / 2f, paint);
            paint.setTypeface(Typeface.DEFAULT);
        }

        private void desenharPulso(Canvas canvas, int w, long now) {
            if (pulseText == null || pulseText.isEmpty() || now > pulseUntilMs) {
                return;
            }
            float alpha = Math.max(0f, (pulseUntilMs - now) / 850f);
            paint.setColor(withAlpha(pulseColor, (int) (210 * alpha)));
            paint.setTextSize(dpUi(24));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setShadowLayer(dpUi(8), 0, dpUi(2), withAlpha(Color.BLACK, 120));
            canvas.drawText(pulseText, w / 2f, dpUi(166) - dpUi(20) * (1f - alpha), paint);
            paint.clearShadowLayer();
            paint.setTypeface(Typeface.DEFAULT);
        }

        private void desenharFim(Canvas canvas, int w, int h) {
            paint.setColor(withAlpha(Color.BLACK, 150));
            canvas.drawRoundRect(new RectF(dpUi(24), h / 2f - dpUi(58), w - dpUi(24), h / 2f + dpUi(58)), dpUi(14), dpUi(14), paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dpUi(21));
            canvas.drawText("Fim da corrida", w / 2f, h / 2f - dpUi(12), paint);
            paint.setTextSize(dpUi(13));
            canvas.drawText("Toque para tentar de novo", w / 2f, h / 2f + dpUi(20), paint);
            paint.setTypeface(Typeface.DEFAULT);
        }
    }

    private static class PrimeShooterItem {
        float x;
        float y;
        final float speed;
        final int value;
        final boolean prime;
        int role;
        int loot;
        float escortOffset = Float.NaN;
        long nextAttack;

        PrimeShooterItem(float x, float y, float speed, int value, boolean prime) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.value = value;
            this.prime = prime;
        }
    }

    private static class PrimeShooterShot {
        float x;
        float y;
        final float vx, vy;
        final boolean hostile;
        final int number;

        PrimeShooterShot(float x, float y) {
            this(x, y, 0, 0, false);
        }

        PrimeShooterShot(float x, float y, float vx, float vy, boolean hostile) {
            this(x, y, vx, vy, hostile, 4);
        }

        PrimeShooterShot(float x, float y, float vx, float vy, boolean hostile, int number) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.hostile = hostile;
            this.number = number;
        }
    }

    private class PrimeShooterView extends View {
        private static final int LOOT_SHIELD = 4, PICKUP_SHIELD_MS = 4500;
        private final String[] zoneNames = {"Órbita azul", "Nebulosa", "Cinturão rochoso", "Gigante anelado", "Terra"};
        private final float[] zoneAnchors = {.78f, .82f, .16f, .035f, .50f};
        private final ArcadePhase phase = new ArcadePhase(true);
        private Runnable phaseReady;
        private final android.graphics.Bitmap[] veiculos = carregarVeiculos("airplane", "rocket", "spaceship");
        private final android.graphics.Bitmap espaco = carregarImagemArcade("space.jpg");
        private final android.graphics.Bitmap[] zonas = {
            carregarImagemArcade("flight-zone-0.jpg"), carregarImagemArcade("flight-zone-1.jpg"),
            carregarImagemArcade("flight-zone-2.jpg"), carregarImagemArcade("flight-zone-3.jpg"), espaco
        };
        private final android.graphics.Bitmap terra = carregarImagemArcade("earth.png");
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shipPath = new Path();
        private final RectF panelRect = new RectF();
        private final ArrayList<PrimeShooterItem> itens = new ArrayList<>();
        private final ArrayList<PrimeShooterShot> tiros = new ArrayList<>();
        private long lastFrameMs;
        private long playTimeMs;
        private long nextSpawnMs;
        private int previousNumber;
        private long nextShotMs;
        private long pulseUntilMs;
        private long boostUntilMs;
        private long shieldFlashUntilMs;
        private long hitGraceUntilMs;
        private long lastSaveMs;
        private float shipX;
        private float shipY;
        private float lastTouchX;
        private float lastTouchY;
        private int pontuacao;
        private int bestScore;
        private int lives = ArcadeRules.LIVES;
        private int bossHp = -1, charge, wave;
        private int parts, weapon, destroyed;
        private boolean escortsReady;
        private int[] boss;
        private boolean autoAdvance;
        private float bossX, bossY;
        private long bossStart, bossNextWave, bossBurst = -1;
        private boolean gameOver;
        private boolean running;
        private boolean fireHeld;
        private boolean pendingShot;
        private int pilotPointer = -1;
        private String pulseText = "";
        private int pulseColor = Color.WHITE;

        PrimeShooterView(android.content.Context context) {
            super(context);
            setFocusable(true);
            setClickable(true);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setOverScrollMode(OVER_SCROLL_NEVER);
            SharedPreferences prefs = getSharedPreferences(PREFS_PRIME_SHOOTER, MODE_PRIVATE);
            bestScore = prefs.getInt(KEY_PS_BEST_SCORE, 0);
            boolean hasTimedSave = prefs.contains("campaign_boss_hp");
            reiniciarComPontuacao(hasTimedSave ? prefs.getInt(KEY_PS_LAST_SCORE, 0) : 0);
            int savedStage = hasTimedSave ? Math.max(0, Math.min(arcadeUnlocked(prefs, true), prefs.getInt("timed_phase_index", 0))) : 0;
            phase.reset(savedStage, Math.max(0, Math.min(pontuacao, prefs.getInt("timed_phase_base", 0))));
            phase.elapsed = hasTimedSave ? Math.max(0, Math.min(phase.duration, prefs.getInt("timed_elapsed", 0))) : 0;
            if (hasTimedSave) {
                parts = Math.max(0, Math.min(12, prefs.getInt("campaign_parts", 0)));
                weapon = Math.max(0, Math.min(12, prefs.getInt("campaign_weapon", 0)));
                destroyed = Math.max(0, prefs.getInt("campaign_destroyed", 0));
                lives = Math.max(0, Math.min(arcadeMaxLivesNativo(parts), prefs.getInt(KEY_PS_LIVES, ArcadeRules.LIVES)));
                boostUntilMs = Math.max(0, Math.min(PICKUP_SHIELD_MS, prefs.getInt(KEY_PS_SHIELD_MS, 0)));
                charge = Math.max(0, Math.min(100, prefs.getInt("campaign_charge", 0)));
                boss = arcadeBossNativo(phase.index);
                bossHp = Math.max(-1, Math.min(boss[2], prefs.getInt("campaign_boss_hp", -1)));
                bossNextWave = 1400;
                phase.outcome = arcadeFlightOutcomeNativo(lives, bossHp);
                if (phase.outcome != 0) phase.exitStart = Math.max(0, android.os.SystemClock.uptimeMillis() - 850);
            }
        }

        void salvarProgresso() {
            int atual = Math.max(0, pontuacao);
            if (atual > bestScore) {
                bestScore = atual;
            }
            getSharedPreferences(PREFS_PRIME_SHOOTER, MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_PS_BEST_SCORE, bestScore)
                    .putInt(KEY_PS_LAST_SCORE, gameOver ? 0 : atual)
                    .putInt(KEY_PS_LIVES, gameOver ? ArcadeRules.LIVES : lives)
                    .putInt(KEY_PS_SHIELD_MS, gameOver ? 0 : (int) Math.max(0, boostUntilMs - playTimeMs))
                    .putInt("timed_phase_index", phase.index)
                    .putInt("timed_phase_base", phase.startingScore)
                    .putInt("timed_elapsed", phase.elapsed)
                    .putInt("campaign_charge", charge)
                    .putInt("campaign_parts", parts)
                    .putInt("campaign_weapon", weapon)
                    .putInt("campaign_destroyed", destroyed)
                    .putInt("campaign_boss_hp", bossHp)
                    .apply();
            lastSaveMs = android.os.SystemClock.uptimeMillis();
        }

        void pausar() {
            running = false;
            autoAdvance = false;
            fireHeld = false;
            pendingShot = false;
            lastFrameMs = 0;
            salvarProgresso();
            invalidate();
        }

        private void reiniciarComPontuacao(int pontuacaoInicial) {
            itens.clear();
            tiros.clear();
            bossHp = -1;
            boss = null;
            autoAdvance = false;
            charge = wave = 0;
            parts = weapon = destroyed = 0;
            escortsReady = false;
            bossNextWave = 0;
            bossStart = 0;
            bossBurst = -1;
            lastFrameMs = 0;
            playTimeMs = 0;
            previousNumber = 0;
            nextSpawnMs = ArcadeRules.START_MS;
            nextShotMs = 0;
            pulseUntilMs = 0;
            boostUntilMs = 0;
            shieldFlashUntilMs = 0;
            hitGraceUntilMs = 0;
            pontuacao = Math.max(0, pontuacaoInicial);
            phase.reset(0, 0);
            lives = ArcadeRules.LIVES;
            gameOver = false;
            running = false;
            fireHeld = false;
            pendingShot = false;
            pulseText = "";
            if (getWidth() > 0) moverNave(getWidth() / 2f, getHeight() - dpUi(ArcadeRules.SHIP_Y));
            invalidate();
        }

        @Override
        protected void onDetachedFromWindow() {
            salvarProgresso();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (bossHp > 0) {
                bossX = w / 2f;
                bossY = Math.min(h * .30f, dpUi(145));
            }
            moverNave(oldw > 0 ? shipX * w / oldw : w / 2f,
                oldh > 0 ? shipY * h / oldh : h - dpUi(ArcadeRules.SHIP_Y));
            protegerFaixaDeVoo(h - dpUi(100));
        }

        private void protegerFaixaDeVoo(int touchY) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
            int height = Math.min(getHeight(), dpUi(200));
            int top = Math.max(0, Math.min(getHeight() - height, touchY - height / 2));
            setSystemGestureExclusionRects(java.util.Collections.singletonList(
                new android.graphics.Rect(0, top, getWidth(), top + height)));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (phase.exitStart >= 0) return true;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (gameOver) {
                    repetirFase();
                }
                if (!running) {
                    running = true;
                    lastFrameMs = 0;
                    if (phase.elapsed == 0) narrarInicioVoo();
                }
                pilotPointer = event.getPointerId(0);
                protegerFaixaDeVoo(Math.round(event.getY(0)));
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                fireHeld = false;
                pilotPointer = -1;
                protegerFaixaDeVoo(getHeight() - dpUi(100));
                if (action == MotionEvent.ACTION_CANCEL) pendingShot = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE
                    || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
                int lifting = action == MotionEvent.ACTION_POINTER_UP ? event.getActionIndex() : -1;
                int count = event.getPointerCount() - (lifting >= 0 ? 1 : 0);
                int pointer = event.findPointerIndex(pilotPointer);
                if (pointer < 0 || pointer == lifting) {
                    pointer = lifting == 0 ? 1 : 0;
                    if (pointer >= event.getPointerCount()) return true;
                    pilotPointer = event.getPointerId(pointer);
                }
                float x = event.getX(pointer);
                float y = event.getY(pointer);
                if (action != MotionEvent.ACTION_MOVE) {
                    fireHeld = count == 2;
                    if (fireHeld && action == MotionEvent.ACTION_POINTER_DOWN) {
                        pendingShot = true;
                        atirar(true);
                    }
                    lastTouchX = x;
                    lastTouchY = y;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    moverNave(shipX + x - lastTouchX, shipY + y - lastTouchY);
                    lastTouchX = x;
                    lastTouchY = y;
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            long now = android.os.SystemClock.uptimeMillis();
            if (lastFrameMs == 0) {
                lastFrameMs = now;
            }
            long deltaMs = Math.min(ArcadeRules.MAX_FRAME_MS, Math.max(0, now - lastFrameMs));
            lastFrameMs = now;

            if (running && !gameOver) {
                if (bossHp < 0) {
                    deltaMs = Math.min(deltaMs, phase.duration - phase.elapsed);
                    phase.elapsed += (int) deltaMs;
                }
                playTimeMs += deltaMs;
                atualizar(deltaMs / 1000f, now, w, h);
                if (phase.outcome == 0 && bossHp < 0 && phase.elapsed >= phase.duration) iniciarChefe(now);
                completarFase(now, false);
            }

            desenharEspaco(canvas, w, h);
            desenharTiros(canvas);
            desenharItens(canvas);
            desenharChefe(canvas, now);
            float departure = phase.exitProgress(now);
            if (departure < 1 || phase.outcome == ArcadePhase.CRASHED) {
                canvas.save();
                if (phase.outcome != ArcadePhase.CRASHED) canvas.translate(0, -(shipY + dpUi(120)) * departure);
                desenharNave(canvas, w, h, now);
                canvas.restore();
            }
            desenharFaixaControle(canvas, w, h);
            desenharPlacar(canvas, w, now);
            desenharPulso(canvas, w, now);

            if (phase.exitStart >= 0) {
                if (departure < 1) postInvalidateOnAnimation();
                else if (phase.outcome == ArcadePhase.PASSED && phase.index < EARTH_STAGE && autoAdvance) {
                    if (now - phase.exitStart < 1450) postInvalidateOnAnimation();
                    else if (!phase.shown) {
                        phase.shown = true;
                        post(() -> {
                            if (!phase.shown || phase.outcome != ArcadePhase.PASSED) return;
                            if (autoAdvance) avancarFase();
                            else if (phaseReady != null) phaseReady.run();
                        });
                    }
                }
                else if (bossBurst >= 0 && now - bossBurst < 1050) postInvalidateOnAnimation();
                else if (!phase.shown && phaseReady != null) {
                    phase.shown = true;
                    post(() -> { if (phase.shown) phaseReady.run(); });
                }
            } else if (gameOver) {
                desenharFim(canvas, w, h);
            } else if (running) {
                postInvalidateOnAnimation();
            } else {
                desenharPausaArcade(canvas, w, h,
                    playTimeMs > 0 || pontuacao > 0 || lives < ArcadeRules.LIVES, paint);
            }
        }

        private void atualizar(float dt, long now, int w, int h) {
            float density = getResources().getDisplayMetrics().density;

            if (fireHeld || pendingShot) {
                atirar(pendingShot);
            }

            if (bossHp > 0) atualizarChefe(w, h);
            if (bossHp < 0 && playTimeMs >= nextSpawnMs) {
                int[] spawn = arcadeSpawnNativo(true, phase.pace(), primeMatchRandom.nextInt(), previousNumber);
                previousNumber = spawn[0];
                float margem = Math.min(w / 2f, ArcadeRules.SHIP_MARGIN * density);
                float x = margem + (w - 2 * margem) * spawn[4] / ArcadeRules.LANE_MAX;
                PrimeShooterItem item = new PrimeShooterItem(x, -ArcadeRules.SPAWN_MARGIN * density,
                    spawn[2] * density, previousNumber, spawn[1] != 0);
                item.role = arcadeEnemyRoleNativo(item.prime, true, phase.index, primeMatchRandom.nextInt());
                item.nextAttack = playTimeMs + 1200;
                itens.add(item);
                nextSpawnMs = playTimeMs + spawn[3];
            }

            resgatarAntesDosTiros(dt, now, h, density);
            for (int i = tiros.size() - 1; i >= 0; i--) {
                PrimeShooterShot tiro = tiros.get(i);
                tiro.x += tiro.vx * dt;
                tiro.y += (tiro.hostile ? tiro.vy : -ArcadeRules.SHOT_SPEED * density) * dt;
                if (tiro.y < -dpUi(36) || tiro.y > h + dpUi(36) || tiro.x < -dpUi(36) || tiro.x > w + dpUi(36)) {
                    tiros.remove(i);
                    continue;
                }
                if (tiro.hostile) {
                    if (playTimeMs < boostUntilMs && arcadeShieldInterceptsNativo(
                            (tiro.x - shipX) / density, (tiro.y - shipY) / density)) {
                        tiros.remove(i);
                        shieldFlashUntilMs = playTimeMs + 200;
                        mostrarPulso("Tiro bloqueado", 0xFFBAE6FD, now);
                        tocarPrimeMatchSom(primeMatchSoundPower);
                    } else if (Math.abs(tiro.x - shipX) < dpUi(24) && Math.abs(tiro.y - shipY) < dpUi(26)) {
                        tiros.remove(i);
                        sofrerImpacto(now);
                        if (lives <= 0) return;
                    }
                    continue;
                }
                boolean hitEscort = false;
                for (int j = itens.size() - 1; j >= 0; j--) {
                    PrimeShooterItem item = itens.get(j);
                    if (!Float.isNaN(item.escortOffset) && Math.abs(tiro.x - item.x) < dpUi(25)
                            && Math.abs(tiro.y - item.y) < dpUi(25)) {
                        itens.remove(j);
                        tiros.remove(i);
                        destruirInimigo(item, now);
                        hitEscort = true;
                        break;
                    }
                }
                if (hitEscort) continue;
                if (bossHp > 0 && Math.abs(tiro.x - bossX) < dpUi(53) * bossScale()
                        && Math.abs(tiro.y - bossY) < dpUi(34) * bossScale()) {
                    tiros.remove(i);
                    if (bossGuarded() || Math.abs(tiro.x - bossX) >= dpUi(22) * bossScale()) continue;
                    int previousHp = bossHp;
                    bossHp = Math.max(0, bossHp - arcadeUpgradeDamageNativo(charge, weapon));
                    soltarBonusDoChefe(previousHp, bossHp);
                    if (bossHp == 0) {
                        bossBurst = now;
                        tocarPrimeMatchSom(primeMatchSoundComposite);
                        completarFase(now, false);
                        return;
                    }
                    continue;
                }

                for (int j = itens.size() - 1; j >= 0; j--) {
                    PrimeShooterItem item = itens.get(j);
                    if (item.loot != 0 || !Float.isNaN(item.escortOffset)) continue;
                    int box = item.prime ? ArcadeRules.SPACE_PRIME : ArcadeRules.SPACE_COMPOSITE;
                    if (arcadeCollidesNativo(ArcadeRules.SHOT_BOX, tiro.x / density, (h - tiro.y) / density,
                        box, item.x / density, (h - item.y) / density)) {
                        itens.remove(j);
                        tiros.remove(i);
                        destruirInimigo(item, now);
                        break;
                    }
                }
            }

            for (int i = itens.size() - 1; i >= 0; i--) {
                PrimeShooterItem item = itens.get(i);
                if (!Float.isNaN(item.escortOffset)) continue;
                item.y += item.speed * dt;
                if (item.role == ARCADE_ARMED && playTimeMs >= item.nextAttack && item.y > dpUi(55)
                        && item.y < shipY - dpUi(84)) {
                    tiros.add(new PrimeShooterShot(item.x, item.y + dpUi(21), 0, 260 * density, true, item.value));
                    item.nextAttack = playTimeMs + 1600;
                }
                if (item.y > h + ArcadeRules.SPAWN_MARGIN * density) {
                    itens.remove(i);
                    continue;
                }

                if (item.prime || item.loot != 0) continue;
                if (arcadeCollidesNativo(ArcadeRules.SHIP_BOX, shipX / density, (h - shipY) / density,
                    ArcadeRules.SPACE_COMPOSITE, item.x / density, (h - item.y) / density)) {
                    itens.remove(i);
                    sofrerImpacto(now);
                    if (lives <= 0) return;
                }
            }

            if (now - lastSaveMs > 1800) {
                salvarProgresso();
            }
        }

        private void resgatarAntesDosTiros(float dt, long now, int h, float density) {
            for (int i = itens.size() - 1; i >= 0; i--) {
                PrimeShooterItem item = itens.get(i);
                if (!item.prime && item.loot == 0) continue;
                int box = item.prime ? ArcadeRules.SPACE_PRIME : ArcadeRules.SPACE_COMPOSITE;
                float nextY = item.y + item.speed * dt;
                if (!arcadeCollidesNativo(ArcadeRules.SHIP_BOX, shipX / density, (h - shipY) / density,
                    box, item.x / density, (h - nextY) / density)) continue;
                itens.remove(i);
                if (item.loot != 0) {
                    recolherBonus(item.loot, now);
                } else {
                    int bonus = arcadePoints(item.value, true, true);
                    pontuacao = arcadeAddScoreNativo(pontuacao, bonus);
                    boostUntilMs = Math.max(boostUntilMs, playTimeMs + ArcadeRules.SHIELD_MS);
                    charge = arcadeAddChargeNativo(charge, item.value);
                    mostrarPulso("Resgate " + item.value + "  +" + bonus, 0xFFA7F3D0, now);
                    tocarPrimeMatchSom(primeMatchSoundPrime);
                }
            }
        }

        private void completarFase(long now, boolean crashed) {
            if (!phase.concludeWith(now, pontuacao, arcadeFlightOutcomeNativo(crashed ? 0 : lives, bossHp), arcadePrefs(true))) return;
            running = false;
            fireHeld = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            salvarProgresso();
            if (phase.outcome == ArcadePhase.PASSED) {
                autoAdvance = true;
                if (phase.index < BONUS_STAGE) guardarEquipamento(phase.index + 1);
                tocarPrimeMatchSom(primeMatchSoundVictory);
                mostrarPulso("Fase " + (phase.index + 1) + " concluída", 0xFFFFDB91, now);
            }
        }

        private void avancarFase() {
            if (phase.outcome != ArcadePhase.PASSED || phase.index >= BONUS_STAGE) return;
            iniciarFase(phase.index + 1, pontuacao);
        }

        private void repetirFase() {
            iniciarFase(phase.index, phase.startingScore);
        }

        private void iniciarFaseEscolhida(int stage) {
            iniciarFase(stage, 0);
            running = false;
        }

        private void iniciarFase(int stage, int baseScore) {
            reiniciarComPontuacao(baseScore);
            phase.reset(stage, baseScore);
            SharedPreferences prefs = arcadePrefs(true);
            parts = prefs.getInt("kit_parts_" + stage, 0);
            weapon = prefs.getInt("kit_weapon_" + stage, 0);
            lives = prefs.getInt("kit_lives_" + stage, ArcadeRules.LIVES);
            running = true;
            narrarInicioVoo();
            salvarProgresso();
            invalidate();
        }

        private void narrarInicioVoo() {
            long now = android.os.SystemClock.uptimeMillis();
            if (phase.index == 0) mostrarPulso("Defesa digital da Terra", 0xFF93C5FD, now);
            if (phase.index == EARTH_STAGE) mostrarPulso("Terra  ·  Defesa final", 0xFF93C5FD, now);
        }

        private void moverNave(float x, float y) {
            float margem = Math.min(getWidth() / 2f, dpUi(ArcadeRules.SHIP_MARGIN));
            shipX = Math.max(margem, Math.min(getWidth() - margem, x));
            float top = Math.min(getHeight() / 2f, dpUi(92));
            if (bossHp > 0) top = Math.max(top, Math.min(getHeight() * .58f, bossY + dpUi(55)));
            float bottom = Math.max(top, getHeight() - dpUi(ArcadeRules.CONTROL_HEIGHT + 36));
            shipY = Math.max(top, Math.min(bottom, y));
            invalidate();
        }

        private void atirar(boolean tocarSom) {
            if (playTimeMs < nextShotMs || gameOver || !running) {
                return;
            }
            tiros.add(new PrimeShooterShot(shipX, shipY - dpUi(ArcadeRules.SHOT_OFFSET)));
            pendingShot = false;
            nextShotMs = playTimeMs + Math.max(105, ArcadeRules.SHOT_MS - 6 * weapon);
            if (tocarSom) {
                tocarPrimeMatchSom(primeMatchSoundTap);
            }
            invalidate();
        }

        private void encerrarVoo(long now) {
            gameOver = true;
            mostrarPulso("Fim do voo", Color.parseColor("#FCA5A5"), now);
            salvarProgresso();
        }

        private void mostrarPulso(String texto, int cor, long now) {
            pulseText = texto;
            pulseColor = cor;
            pulseUntilMs = now + 950;
        }

        private String nomeChefe() {
            return "M" + (phase.index + 1) + " · " + boss[1] + (phase.index >= 4 ? "∞" : "");
        }

        private float bossScale() { return phase.flightStage[1] / 100f; }

        private boolean bossGuarded() {
            if (bossHp <= 0) return false;
            for (PrimeShooterItem item : itens) {
                if (!Float.isNaN(item.escortOffset)) return true;
            }
            return Math.max(0, playTimeMs - bossStart) % phase.flightStage[3] < phase.flightStage[4];
        }

        private float zoneBlend() {
            return Math.max(0, Math.min(1, (phase.elapsed / (float) phase.duration - .44f) / .17f));
        }

        private void guardarEquipamento(int stage) {
            SharedPreferences prefs = arcadePrefs(true);
            int bestParts = Math.max(parts, prefs.getInt("kit_parts_" + stage, 0));
            prefs.edit().putInt("kit_parts_" + stage, bestParts)
                .putInt("kit_weapon_" + stage, Math.max(weapon, prefs.getInt("kit_weapon_" + stage, 0)))
                .putInt("kit_lives_" + stage, Math.min(arcadeMaxLivesNativo(bestParts),
                    Math.max(lives, prefs.getInt("kit_lives_" + stage, 0))))
                .apply();
        }

        private void destruirInimigo(PrimeShooterItem item, long now) {
            if (item.prime) {
                mostrarPulso("Primo perdido", 0xFFFCA5A5, now);
                tocarPrimeMatchSom(primeMatchSoundComposite);
                return;
            }
            int points = arcadePoints(item.value, false, false);
            pontuacao = arcadeAddScoreNativo(pontuacao, points);
            mostrarPulso("Composto " + item.value + "  +" + points, 0xFF93C5FD, now);
            tocarPrimeMatchSom(primeMatchSoundCombo);
            if (destroyed < Integer.MAX_VALUE) destroyed++;
            int kind = arcadeFlightLootNativo(phase.index, destroyed);
            if (kind != 0) {
                PrimeShooterItem pickup = new PrimeShooterItem(item.x, item.y, dpUi(82), 0, false);
                pickup.loot = kind;
                itens.add(pickup);
            }
        }

        private void soltarBonusDoChefe(int before, int after) {
            int drops = arcadeBossDropsNativo(before, after, boss[2]);
            for (int mark = 0; mark < 3; mark++) {
                if ((drops & (1 << mark)) == 0) continue;
                float x = Math.max(dpUi(22), Math.min(getWidth() - dpUi(22), bossX + dpUi(32) * (mark - 1)));
                PrimeShooterItem pickup = new PrimeShooterItem(x, bossY + dpUi(52) * bossScale(), dpUi(112), 0, false);
                pickup.loot = mark == 1 ? 1 : LOOT_SHIELD;
                itens.add(pickup);
            }
        }

        private void recolherBonus(int kind, long now) {
            String label;
            if (kind == LOOT_SHIELD) {
                boostUntilMs = Math.max(boostUntilMs, playTimeMs + PICKUP_SHIELD_MS);
                label = "Escudo reforçado";
            } else if (kind == 1) {
                lives = Math.min(arcadeMaxLivesNativo(parts), lives + 1);
                label = "Vidas: " + lives;
            } else if (kind == 2) {
                parts = Math.min(12, parts + 1);
                label = "Peças: " + parts;
            } else {
                weapon = Math.min(12, weapon + 1);
                label = "Arma: nível " + weapon;
            }
            mostrarPulso(label, 0xFFA7F3D0, now);
            tocarPrimeMatchSom(primeMatchSoundPower);
            salvarProgresso();
        }

        private void iniciarChefe(long now) {
            boss = arcadeBossNativo(phase.index);
            bossHp = boss[2];
            bossStart = playTimeMs;
            bossNextWave = playTimeMs + 1450;
            bossX = getWidth() / 2f;
            bossY = Math.min(getHeight() * .30f, dpUi(145));
            itens.clear();
            tiros.clear();
            moverNave(shipX, shipY);
            mostrarPulso(nomeChefe(), 0xFFFF9C86, now);
            salvarProgresso();
        }

        private void atualizarChefe(int w, int h) {
            float edge = Math.min(w / 2f, dpUi(57) * bossScale());
            float orbit = (float) (w * (.50 + .27 * Math.sin(playTimeMs / 1320.0)));
            bossX = Math.max(edge, Math.min(w - edge, orbit));
            bossY = Math.min(h * .30f, dpUi(145));
            if (!escortsReady) {
                escortsReady = true;
                int count = phase.flightStage[2];
                for (int i = 0; i < count; i++) {
                    PrimeShooterItem escort = new PrimeShooterItem(bossX, bossY, 0, 4 + 2 * i, false);
                    escort.escortOffset = i - (count - 1) / 2f;
                    escort.role = ARCADE_ARMED;
                    escort.nextAttack = playTimeMs + 1600 + 380 * i;
                    itens.add(escort);
                }
            }
            for (PrimeShooterItem escort : itens) {
                if (Float.isNaN(escort.escortOffset)) continue;
                escort.x = Math.max(dpUi(19), Math.min(w - dpUi(19), bossX + escort.escortOffset * dpUi(54)
                    + (float) Math.sin(playTimeMs / 435.0 + escort.escortOffset) * dpUi(22)));
                escort.y = bossY + dpUi(52) * bossScale() + dpUi(15) + Math.abs(escort.escortOffset) * dpUi(8);
                if (playTimeMs >= escort.nextAttack) {
                    float aim = Math.max(-dpUi(110), Math.min(dpUi(110), (shipX - escort.x) * .7f));
                    tiros.add(new PrimeShooterShot(escort.x, escort.y, aim, dpUi(245), true, escort.value));
                    escort.nextAttack = playTimeMs + Math.max(1150, 2250 - phase.index * 18L);
                }
            }
            if (playTimeMs < bossNextWave) return;
            float speed = dpUi(230 + Math.min(phase.index, 45) * 2);
            int compound = new int[]{4, 6, 8, 9, 10, 12}[wave % 6];
            if (boss[3] == 1) {
                for (int lane = 0; lane < 4; lane++) {
                    if (lane != wave % 4) tiros.add(new PrimeShooterShot(w * (lane + .5f) / 4f, bossY, 0, speed, true, compound));
                }
            } else {
                float aim = boss[3] == 2 || (wave & 1) == 1
                    ? Math.max(-dpUi(135), Math.min(dpUi(135), (shipX - bossX) * 1.2f)) : 0;
                for (int i = -1; i <= 1; i++) {
                    tiros.add(new PrimeShooterShot(bossX, bossY + dpUi(22), aim + dpUi(88) * i, speed, true, compound));
                }
            }
            wave++;
            bossNextWave = playTimeMs + Math.max(800, 1550 - phase.index * 13L);
        }

        private void sofrerImpacto(long now) {
            int evento = arcadeFlightContactNativo(false, playTimeMs < boostUntilMs, playTimeMs < hitGraceUntilMs);
            if (evento == ArcadeRules.SHIELD) {
                mostrarPulso("Escudo ativo", 0xFFBAE6FD, now);
                tocarPrimeMatchSom(primeMatchSoundPower);
            } else if (evento == ArcadeRules.DAMAGE) {
                lives--;
                hitGraceUntilMs = playTimeMs + ArcadeRules.HIT_GRACE_MS;
                mostrarPulso("Dano", 0xFFFDBA74, now);
                tocarPrimeMatchSom(primeMatchSoundComposite);
                if (lives <= 0) completarFase(now, true);
            }
        }

        private void desenharChefe(Canvas canvas, long now) {
            if (bossHp > 0) {
                if (bossY <= 0) return;
                canvas.save();
                canvas.scale(bossScale(), bossScale(), bossX, bossY);
                boolean guarded = bossGuarded();
                paint.setColor(guarded ? 0x5066DBFF : bossNextWave - playTimeMs < 320 ? 0xAAFF724E : 0x50FB573D);
                canvas.drawOval(bossX - dpUi(57), bossY - dpUi(39), bossX + dpUi(57), bossY + dpUi(39), paint);
                if (guarded) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dpUi(2));
                    paint.setColor(0xFF78DFFF);
                    canvas.drawOval(bossX - dpUi(57), bossY - dpUi(39), bossX + dpUi(57), bossY + dpUi(39), paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(0xFF78DFFF);
                }
                canvas.save();
                canvas.rotate(180, bossX, bossY);
                paint.setColor(Color.WHITE);
                desenharVeiculo(canvas, veiculos[veiculos.length - 1],
                    new RectF(bossX - dpUi(47), bossY - dpUi(40), bossX + dpUi(47), bossY + dpUi(40)), paint);
                canvas.restore();
                if (!guarded) {
                    paint.setColor(0x5CFFB15E);
                    canvas.drawCircle(bossX, bossY, dpUi(19), paint);
                    paint.setColor(0xFFFFD384);
                    canvas.drawCircle(bossX, bossY, dpUi(6), paint);
                }
                paint.setColor(0xFFA52F46);
                canvas.drawRoundRect(bossX - dpUi(43), bossY + dpUi(45), bossX + dpUi(43), bossY + dpUi(51), dpUi(3), dpUi(3), paint);
                paint.setColor(guarded ? 0xFF78DFFF : 0xFFFFCE78);
                canvas.drawRoundRect(bossX - dpUi(43), bossY + dpUi(45), bossX - dpUi(43) + dpUi(86) * bossHp / (float) boss[2],
                    bossY + dpUi(51), dpUi(3), dpUi(3), paint);
                canvas.restore();
            } else if (bossBurst >= 0 && now - bossBurst < 1050) {
                float p = (now - bossBurst) / 1050f;
                paint.setColor(withAlpha(0xFFFFAD4D, (int) ((1 - p) * 220)));
                paint.setStrokeWidth(dpUi(3));
                for (int i = 0; i < 22; i++) {
                    double angle = i * Math.PI * 2 / 22;
                    float dx = (float) Math.cos(angle), dy = (float) Math.sin(angle);
                    float span = dpUi(35 + i % 5 * 12) * p;
                    canvas.drawLine(bossX + dx * span, bossY + dy * span,
                        bossX + dx * (span + dpUi(26)), bossY + dy * (span + dpUi(26)), paint);
                }
                paint.setColor(withAlpha(0xFFFFE0AA, (int) ((1 - p) * 190)));
                canvas.drawCircle(bossX, bossY, dpUi(14) * (1 + p * 3), paint);
            }
        }

        private void desenharEspaco(Canvas canvas, int w, int h) {
            canvas.drawColor(0xFF090F18);
            float blend = zoneBlend();
            desenharZona(canvas, w, h, phase.flightStage[5], 255);
            if (phase.flightStage[6] != phase.flightStage[5] && blend > 0) {
                desenharZona(canvas, w, h, phase.flightStage[6], (int) (255 * blend));
            }
            if (terra != null && phase.index == EARTH_STAGE) {
                float r = w * .65f;
                float centerX = w * .67f;
                float centerY = h * .58f;
                paint.setAlpha(215);
                canvas.drawBitmap(terra, null, new RectF(centerX - r, centerY - r, centerX + r, centerY + r), paint);
                paint.setAlpha(255);
            }
            for (int i = 0; i < 34; i++) {
                float x = (i * 73) % Math.max(1, w);
                float y = ((i * 127) + (playTimeMs / (7 + i % 13))) % Math.max(1, h);
                paint.setColor(withAlpha(Color.WHITE, 70 + i % 3 * 38));
                canvas.drawCircle(x, y, dpUi(1 + i % 2), paint);
            }
        }

        private void desenharZona(Canvas canvas, int w, int h, int zone, int alpha) {
            android.graphics.Bitmap image = zonas[zone] == null ? espaco : zonas[zone];
            if (image == null) return;
            float scale = 1.12f * Math.max(w / (float) image.getWidth(), h / (float) image.getHeight());
            float width = image.getWidth() * scale, height = image.getHeight() * scale;
            float anchor = zoneAnchors[zone];
            float drift = .42f * Math.min(anchor, 1 - anchor);
            float x = (w - width) * anchor + (float) Math.sin(playTimeMs / 18000.0) * (width - w) * drift;
            float y = (h - height) * .5f + (float) Math.sin(playTimeMs / 23000.0) * (height - h) * .24f;
            paint.setColor(Color.WHITE);
            paint.setAlpha(alpha);
            canvas.drawBitmap(image, null, new RectF(x, y, x + width, y + height), paint);
            paint.setAlpha(255);
        }

        private void desenharFaixaControle(Canvas canvas, int w, int h) {
            float top = h - dpUi(ArcadeRules.CONTROL_HEIGHT);
            paint.setColor(0xB0101C28);
            canvas.drawRect(0, top, w, h, paint);
            paint.setColor(0x3867E8F9);
            canvas.drawRect(0, top, w, top + dpUi(1), paint);
            paint.setColor(0x88FFFFFF);
            canvas.drawRoundRect(w / 2f - dpUi(20), h - dpUi(24),
                w / 2f + dpUi(20), h - dpUi(21), dpUi(2), dpUi(2), paint);
        }

        private void desenharTiros(Canvas canvas) {
            paint.setShader(null);
            paint.setColor(Color.parseColor("#FDE68A"));
            paint.setShadowLayer(dpUi(14), 0, 0, Color.parseColor("#FACC15"));
            for (PrimeShooterShot tiro : tiros) {
                if (tiro.hostile) {
                    paint.clearShadowLayer();
                    paint.setColor(0xFFFF724E);
                    canvas.drawCircle(tiro.x, tiro.y, dpUi(10), paint);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(dpUi(11));
                    paint.setColor(0xFF230914);
                    canvas.drawText(String.valueOf(tiro.number), tiro.x, tiro.y + dpUi(4), paint);
                    paint.setColor(0xFFFDE68A);
                    continue;
                }
                canvas.drawRoundRect(new RectF(tiro.x - dpUi(6), tiro.y - dpUi(30), tiro.x + dpUi(6), tiro.y + dpUi(10)), dpUi(6), dpUi(6), paint);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(new RectF(tiro.x - dpUi(2), tiro.y - dpUi(27), tiro.x + dpUi(2), tiro.y + dpUi(7)), dpUi(3), dpUi(3), paint);
                paint.setColor(Color.parseColor("#FDE68A"));
            }
            paint.clearShadowLayer();
        }

        private void desenharItens(Canvas canvas) {
            for (PrimeShooterItem item : itens) {
                int cor = item.loot == LOOT_SHIELD ? 0xFF147C9E : item.loot == 1 ? 0xFF137A62 :
                    item.loot != 0 ? 0xFF2765A1 : item.prime ? 0xFF137A62 : 0xFF9D334D;
                float raio = !Float.isNaN(item.escortOffset) ? dpUi(16) : item.prime ? dpUi(22) : dpUi(20);
                if (!Float.isNaN(item.escortOffset)) {
                    canvas.save();
                    canvas.rotate(180, item.x, item.y);
                    paint.setColor(Color.WHITE);
                    desenharVeiculo(canvas, veiculos[2],
                        new RectF(item.x - dpUi(27), item.y - dpUi(23), item.x + dpUi(27), item.y + dpUi(23)), paint);
                    canvas.restore();
                }
                if (item.role == ARCADE_ARMED) {
                    paint.setColor(0xFFFFAD62);
                    paint.setStrokeWidth(dpUi(2));
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(item.x, item.y, raio + dpUi(5), paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(withAlpha(Color.BLACK, 62));
                canvas.drawCircle(item.x + dpUi(3), item.y + dpUi(4), raio, paint);
                paint.setColor(cor);
                paint.setShadowLayer(dpUi(12), 0, 0, withAlpha(cor, 180));
                canvas.drawCircle(item.x, item.y, raio, paint);
                paint.clearShadowLayer();
                paint.setColor(withAlpha(Color.WHITE, item.prime ? 150 : 95));
                canvas.drawCircle(item.x - dpUi(7), item.y - dpUi(8), dpUi(6), paint);
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(dpUi(item.loot == 0 ? 15 : item.loot == LOOT_SHIELD ? 9 : 11));
                Paint.FontMetrics fm = paint.getFontMetrics();
                canvas.drawText(item.loot == LOOT_SHIELD ? "ESCUDO" : item.loot == 1 ? "+1" :
                    item.loot == 2 ? "PEÇA" : item.loot == 3 ? "ARMA" : String.valueOf(item.value),
                    item.x, item.y - (fm.ascent + fm.descent) / 2f, paint);
                paint.setTypeface(Typeface.DEFAULT);
            }
        }

        private void desenharNave(Canvas canvas, int w, int h, long now) {
            float baseY = shipY;
            float size = dpUi(34);
            boolean boost = playTimeMs < boostUntilMs;
            paint.setColor(withAlpha(Color.BLACK, 75));
            canvas.drawOval(new RectF(shipX - dpUi(30), baseY + dpUi(24), shipX + dpUi(30), baseY + dpUi(34)), paint);
            if (!gameOver) desenharEscapeArcade(canvas, shipX, baseY + dpUi(30), now, true, paint);

            paint.setColor(Color.WHITE);
            int tier = arcadeVehicle(phase.index);
            float hull = 1 + parts * .012f;
            if (!desenharVeiculo(canvas, veiculos[tier],
                new RectF(shipX - dpUi(36) * hull, baseY - dpUi(43) * hull, shipX + dpUi(36) * hull, baseY + dpUi(29) * hull), paint)) {
                shipPath.reset();
                shipPath.moveTo(shipX, baseY - size);
                shipPath.lineTo(shipX - size * 0.82f, baseY + size * 0.72f);
                shipPath.lineTo(shipX, baseY + size * 0.35f);
                shipPath.lineTo(shipX + size * 0.82f, baseY + size * 0.72f);
                shipPath.close();
                paint.setColor(boost ? Color.parseColor("#67E8F9") : Color.parseColor("#60A5FA"));
                canvas.drawPath(shipPath, paint);
            }

            if (boost) {
                boolean hit = playTimeMs < shieldFlashUntilMs;
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dpUi(hit ? 4 : 2));
                paint.setColor(hit ? 0xFFE1FAFF : 0xBEA7F3D0);
                canvas.drawCircle(shipX, baseY, dpUi(44), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        private void desenharPlacar(Canvas canvas, int w, long now) {
            int setor = 1 + phase.index;
            panelRect.set(dpUi(10), dpUi(10), w - dpUi(10), dpUi(54));
            paint.setColor(withAlpha(Color.BLACK, 118));
            canvas.drawRoundRect(panelRect, dpUi(12), dpUi(12), paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dpUi(13));
            paint.setColor(Color.WHITE);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float textY = panelRect.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText("Pontos " + formatarPontuacaoArcade(phase.points(pontuacao)), dpUi(22), textY, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Setor " + setor, w / 2f, textY, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Vidas " + lives, w - dpUi(22), textY, paint);
            paint.setTypeface(Typeface.DEFAULT);

            {
                paint.setColor(Color.parseColor("#A7F3D0"));
                paint.setTextSize(dpUi(11));
                paint.setTextAlign(Paint.Align.CENTER);
                String sector = bossHp >= 0 && boss != null ? nomeChefe() + (bossGuarded() ? " · Blindado" : "")
                    : zoneNames[phase.flightStage[zoneBlend() >= .5f ? 6 : 5]];
                String meta = sector + " · Carga " + charge + "% · Arma " + weapon;
                String escudo = playTimeMs < boostUntilMs ? (meta.isEmpty() ? "Escudo" : "  •  Escudo") : "";
                canvas.drawText(meta + escudo, w / 2f, dpUi(76), paint);
            }
        }

        private void desenharPulso(Canvas canvas, int w, long now) {
            if (pulseText == null || pulseText.isEmpty() || now > pulseUntilMs) {
                return;
            }
            float alpha = Math.max(0f, (pulseUntilMs - now) / 950f);
            paint.setColor(withAlpha(pulseColor, (int) (230 * alpha)));
            paint.setTextSize(dpUi(22));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setShadowLayer(dpUi(10), 0, dpUi(2), withAlpha(Color.BLACK, 150));
            canvas.drawText(pulseText, w / 2f, dpUi(116) - dpUi(18) * (1f - alpha), paint);
            paint.clearShadowLayer();
            paint.setTypeface(Typeface.DEFAULT);
        }

        private void desenharFim(Canvas canvas, int w, int h) {
            paint.setColor(withAlpha(Color.BLACK, 170));
            canvas.drawRoundRect(new RectF(dpUi(24), h / 2f - dpUi(64), w - dpUi(24), h / 2f + dpUi(64)), dpUi(14), dpUi(14), paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dpUi(21));
            canvas.drawText("Fim do voo", w / 2f, h / 2f - dpUi(18), paint);
            paint.setTextSize(dpUi(13));
            canvas.drawText("Recorde " + formatarPontuacaoArcade(bestScore), w / 2f, h / 2f + dpUi(12), paint);
            canvas.drawText("Toque para reiniciar", w / 2f, h / 2f + dpUi(38), paint);
            paint.setTypeface(Typeface.DEFAULT);
        }
    }

    private void mostrarPrimosCurtos(int bits, int quantidade) {
        try {
            String report = shortPrimesNativo(bits, quantidade);
            if (report == null || report.isEmpty()) {
                throw new IllegalStateException("Resultado nativo indisponivel");
            }
            registrarCalculo();
            salvarResultadoTemporario(report, "primos_aleatorios_bits");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Falha ao sortear primos curtos", e);
            Toast.makeText(this, "Não foi possível preparar o resultado.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Acima de {@link #LIMIAR_BITS_ROTA_NATIVA}: avisar sobrecarga local ou compra in-app de entrega por e-mail.
     */
    private void mostrarDialogoEscolhaPrimosUltra(int bits, int quantidade) {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(this)
                .setTitle("Primos acima de " + LIMIAR_BITS_ROTA_NATIVA + " bits")
                .setMessage("Gerar neste aparelho pode demorar muito, consumir bateria e esquentar o celular."
                        + (MONETIZACAO_ATIVA ? "\n\nTambém é possível comprar a entrega por e-mail." : ""))
                .setPositiveButton("Gerar neste aparelho", (d, w) -> {
                    if (!geracaoAleatoriaAtiva.compareAndSet(false, true)) return;
                    cancelarOperacaoAtual.set(false);
                    if (btnCancelarOperacaoAtual != null) {
                        btnCancelarOperacaoAtual.setVisibility(View.VISIBLE);
                    }
                    CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                            resultadoView,
                            "Primos Aleatórios",
                            "Quantidade: " + quantidade,
                            "Bits: " + bits);
                    if (resultadoView != null) {
                        resultadoView.setText("⏳ Preparando busca nativa...");
                    }
                    new Thread(() -> executarThreadPrimosAleatorios(bits, quantidade, cronometroAtivo)).start();
                });
        if (MONETIZACAO_ATIVA) {
            dialogo.setNeutralButton("Comprar Número de " + bits + " bits", (d, w) -> {
                    megaPedidoModoRsa = false;
                    megaPedidoBits = bits;
                    megaPedidoQuantidade = quantidade;
                    if (playBillingManager != null) {
                        playBillingManager.launchMegaDeliveryInAppPurchase(this);
                    } else {
                        Toast.makeText(this, "Pagamento indisponível.", Toast.LENGTH_SHORT).show();
                    }
                });
        }
        dialogo.setNegativeButton("Cancelar", null).show();
    }

    /**
     * Chave RSA com tamanho ≥ 2×8192 (ex.: 16384): cada primo p,q tem ~8192 bits — mesma lógica de aviso / entrega por e-mail.
     */
    private boolean rsaChaveImplicaPrimos8192BitsOuMais(int bitsChave) {
        return bitsChave >= 2 * LIMIAR_BITS_ROTA_NATIVA;
    }

    /**
     * Mesmo fluxo que primos ultra: gerar no aparelho (com aviso) ou comprar entrega in-app (separado do Premium).
     *
     * @param aoContinuarGeracaoLocal após “Gerar neste aparelho”: diálogo relatório RSA (mensagem ou só números).
     */
    private void mostrarDialogoEscolhaRsaUltra(int bitsChave, Runnable aoContinuarGeracaoLocal) {
        int bitsPorPrimo = bitsChave / 2;
        AlertDialog.Builder dialogo = new AlertDialog.Builder(this)
                .setTitle("Chave RSA " + bitsChave + " bits (~" + bitsPorPrimo + " + " + bitsPorPrimo + " bits em p e q)")
                .setMessage("Gerar p e q neste aparelho pode demorar muito, consumir bateria e esquentar o celular."
                        + (MONETIZACAO_ATIVA ? "\n\nTambém é possível comprar a entrega por e-mail." : ""))
                .setPositiveButton("Gerar neste aparelho", (d, w) -> aoContinuarGeracaoLocal.run());
        if (MONETIZACAO_ATIVA) {
            dialogo.setNeutralButton("Comprar Número de " + bitsChave + " bits", (d, w) -> {
                    megaPedidoModoRsa = true;
                    megaPedidoBits = bitsChave;
                    megaPedidoQuantidade = 2;
                    if (playBillingManager != null) {
                        playBillingManager.launchMegaDeliveryInAppPurchase(this);
                    } else {
                        Toast.makeText(this, "Pagamento indisponível.", Toast.LENGTH_SHORT).show();
                    }
                });
        }
        dialogo.setNegativeButton("Cancelar", null).show();
    }

    private void executarThreadPrimosAleatorios(
            int bits, int quantidade, CronometroOperacaoHandle cronometroAtivo) {
        try {
            File arquivoPrimos = criarArquivoTempPrimosGigantes("primos_gigantes_");
            String resultado;
            try {
                String relatorio = executarJobNativoPrimosGigantes(bits, quantidade, 0, status -> {
                    pararCronometroOperacao(cronometroAtivo);
                    runOnUiThread(() -> resultadoView.setText(status));
                }, arquivoPrimos, true);
                resultado = relatorio + "\n\n--- PRIMOS (DECIMAL COMPLETO) ---\n"
                        + lerTextoUtf8(arquivoPrimos);
            } finally {
                arquivoPrimos.delete();
            }
            resultado = alinharTempoRelatorioNativoComCronometro(
                    resultado, cronometroAtivo.inicioMs, System.currentTimeMillis());
            pararCronometroOperacao(cronometroAtivo);
            salvarResultadoTemporario(resultado, "primos_aleatorios_bits");

        } catch (Throwable t) {
            Log.e(TAG, "Primos aleatórios (bits=" + bits + ")", t);
            pararCronometroOperacao(cronometroAtivo);
            String msg = t.getMessage();
            if (t instanceof OutOfMemoryError) {
                msg = "Memória insuficiente para esta busca. Tente uma quantidade ou um tamanho em bits menor.";
            }
            final String msgFinal = msg != null ? msg : t.getClass().getSimpleName();
            runOnUiThread(() -> resultadoView.setText("❌ Erro: " + msgFinal));
        } finally {
            cancelarOperacaoAtual.set(false);
            runOnUiThread(() -> {
                geracaoAleatoriaAtiva.set(false);
                if (btnCancelarOperacaoAtual != null) {
                    btnCancelarOperacaoAtual.setVisibility(View.GONE);
                }
            });
        }
    }

    private void abrirEmailPedidoEntregaMega(Purchase purchase) {
        String token = purchase.getPurchaseToken();
        String tokenCurto = token != null && token.length() > 0
                ? token.substring(0, Math.min(48, token.length()))
                : "(n/d)";
        String corpo;
        String assunto;
        if (megaPedidoModoRsa) {
            int bpq = megaPedidoBits > 0 ? megaPedidoBits / 2 : 0;
            corpo =
                    "Pedido PrimeProFast — entrega de chave RSA por e-mail\n\n"
                            + "Tamanho da chave RSA: " + megaPedidoBits + " bits (dois primos p e q de ~" + bpq + " bits cada).\n"
                            + "Order ID (Play): " + (purchase.getOrderId() != null ? purchase.getOrderId() : "(n/d)") + "\n"
                            + "Referência compra (token parcial): " + tokenCurto + "\n\n"
                            + "Coloque abaixo o e-mail para envio do material:\n\n";
            assunto = "[PrimeProFast] Pedido RSA " + megaPedidoBits + " bits";
        } else {
            corpo =
                    "Pedido PrimeProFast — entrega de primos por e-mail\n\n"
                            + "Bits solicitados: " + megaPedidoBits + "\n"
                            + "Quantidade: " + megaPedidoQuantidade + "\n"
                            + "Order ID (Play): " + (purchase.getOrderId() != null ? purchase.getOrderId() : "(n/d)") + "\n"
                            + "Referência compra (token parcial): " + tokenCurto + "\n\n"
                            + "Coloque abaixo o e-mail para envio do arquivo:\n\n";
            assunto = "[PrimeProFast] Pedido primos " + megaPedidoBits + " bits ×" + megaPedidoQuantidade;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{EMAIL_PEDIDO_ENTREGA_PRIMO});
        intent.putExtra(Intent.EXTRA_SUBJECT, assunto);
        intent.putExtra(Intent.EXTRA_TEXT, corpo);
        try {
            startActivity(Intent.createChooser(intent, "Enviar pedido ao desenvolvedor"));
        } catch (Exception e) {
            Toast.makeText(this, "Instale um app de e-mail para enviar o pedido.", Toast.LENGTH_LONG).show();
        }
    }

    private void criarInterfaceConjecturaLegendre() {
        contentContainer.removeAllViews();


        // Título
        TextView titulo = new TextView(this);
        titulo.setText("Conjectura de Legendre");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        contentContainer.addView(titulo);

        // Descrição
        TextView descricao = new TextView(this);
        descricao.setText("A conjectura de Legendre afirma que sempre existe um primo entre n² e (n+1)² para qualquer n ≥ 1.\n\nSuporte a números de até 10.000 dígitos com busca de intervalos otimizada.");
        descricao.setPadding(0, 0, 0, 16);
        contentContainer.addView(descricao);

        // Seleção de modo
        TextView modoLabel = new TextView(this);
        modoLabel.setText("Modo de verificação:");
        modoLabel.setPadding(0, 10, 0, 5);
        contentContainer.addView(modoLabel);

        RadioGroup modoGroup = new RadioGroup(this);
        modoGroup.setOrientation(LinearLayout.VERTICAL);

        RadioButton modoUnico = new RadioButton(this);
        modoUnico.setText("Busca contínua");
        modoUnico.setId(1);
        modoUnico.setChecked(true);
        modoGroup.addView(modoUnico);

        RadioButton modoIntervalo = new RadioButton(this);
        modoIntervalo.setText("Faixa");
        modoIntervalo.setId(2);
        modoGroup.addView(modoIntervalo);

        RadioButton modoUmValor = new RadioButton(this);
        modoUmValor.setText("Um valor");
        modoUmValor.setId(3);
        modoGroup.addView(modoUmValor);

        contentContainer.addView(modoGroup);

        // Campos para modo relatório ilimitado
        LinearLayout layoutUnico = new LinearLayout(this);
        layoutUnico.setOrientation(LinearLayout.VERTICAL);
        layoutUnico.setId(100);

        TextView labN = new TextView(this);
        labN.setText("n inicial");
        labN.setTypeface(null, Typeface.BOLD);
        labN.setPadding(dpUi(4), dpUi(8), dpUi(4), dpUi(4));
        layoutUnico.addView(labN);
        EditText inputN = new EditText(this);
        inputN.setHint("Digite aqui (ex.: 10^100, 2^1000 — até 10.000 dígitos)");
        inputN.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        estilizarCampoEntrada(inputN);
        layoutUnico.addView(inputN);

        contentContainer.addView(layoutUnico);

        // Campos para modo intervalo
        LinearLayout layoutIntervalo = new LinearLayout(this);
        layoutIntervalo.setOrientation(LinearLayout.VERTICAL);
        layoutIntervalo.setId(200);
        layoutIntervalo.setVisibility(View.GONE);

        TextView labNi = new TextView(this);
        labNi.setText("n inicial (intervalo)");
        labNi.setTypeface(null, Typeface.BOLD);
        labNi.setPadding(dpUi(4), dpUi(8), dpUi(4), dpUi(4));
        layoutIntervalo.addView(labNi);
        EditText inputNInicio = new EditText(this);
        inputNInicio.setHint("Digite aqui (ex.: 1, 10^50)");
        inputNInicio.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        estilizarCampoEntrada(inputNInicio);
        layoutIntervalo.addView(inputNInicio);

        TextView labNf = new TextView(this);
        labNf.setText("n final (intervalo)");
        labNf.setTypeface(null, Typeface.BOLD);
        labNf.setPadding(dpUi(4), dpUi(8), dpUi(4), dpUi(4));
        layoutIntervalo.addView(labNf);
        EditText inputNFim = new EditText(this);
        inputNFim.setHint("Digite aqui (ex.: 100, 10^100)");
        inputNFim.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        estilizarCampoEntrada(inputNFim);
        layoutIntervalo.addView(inputNFim);

        contentContainer.addView(layoutIntervalo);

        // Listener para mudança de modo
        modoGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != 2) {
                labN.setText(checkedId == 3 ? "n" : "n inicial");
                layoutUnico.setVisibility(View.VISIBLE);
                layoutIntervalo.setVisibility(View.GONE);
            } else {
                layoutUnico.setVisibility(View.GONE);
                layoutIntervalo.setVisibility(View.VISIBLE);
            }
        });

        SharedPreferences prefs = getSharedPreferences("ConjecturaLegendre", MODE_PRIVATE);
        Button btnContinuar = new Button(this);
        btnContinuar.setText("Continuar busca salva");
        btnContinuar.setVisibility(prefs.getString("ultimo_n", "").isEmpty() ? View.GONE : View.VISIBLE);
        btnContinuar.setOnClickListener(v -> {
            String salvo = prefs.getString("ultimo_n", "");
            if (!salvo.isEmpty()) iniciarLegendreContinuo(salvo, resultadoView, btnContinuar);
        });
        contentContainer.addView(btnContinuar);

        // Botão verificar
        Button btnVerificar = new Button(this);
        btnVerificar.setText("🔬 Verificar Conjectura");
        btnVerificar.setBackgroundColor(0xFF2196F3);
        btnVerificar.setTextColor(0xFFFFFFFF);
        btnVerificar.setOnClickListener(v -> {
            try {
                int modo = modoGroup.getCheckedRadioButtonId();

                if (modo == 3) {
                    String entrada = inputN.getText().toString();
                    resultadoView.setText("Verificando intervalo...");
                    new Thread(() -> {
                        try {
                            BigInteger n = parseBigInteger(entrada);
                            if (n.signum() <= 0) throw new IllegalArgumentException("n deve ser maior ou igual a 1.");
                            String relatorio = relatorioLegendreComum(n, n);
                            salvarResultadoTemporario(relatorio, "conjectura_legendre_intervalo");
                        } catch (Exception e) {
                            runOnUiThread(() -> resultadoView.setText("Erro: " + e.getMessage()));
                        }
                    }).start();
                } else if (modo == 1) {
                    iniciarLegendreContinuo(inputN.getText().toString(), resultadoView, btnContinuar);

                } else {
                    // Modo intervalo
                    String nInicioStr = inputNInicio.getText().toString();
                    String nFimStr = inputNFim.getText().toString();

                    if (nInicioStr.isEmpty() || nFimStr.isEmpty()) {
                        Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    resultadoView.setText("Verificando conjectura em intervalo...");

                    new Thread(() -> {
                        try {
                            // Parse dos valores usando BigInteger
                            BigInteger nInicio = parseBigInteger(nInicioStr);
                            BigInteger nFim = parseBigInteger(nFimStr);

                            if (nInicio.compareTo(BigInteger.ONE) < 0) {
                                runOnUiThread(() -> resultadoView.setText("Erro: n inicial deve ser ≥ 1"));
                                return;
                            }

                            if (nFim.compareTo(nInicio) < 0) {
                                runOnUiThread(() -> resultadoView.setText("Erro: n final deve ser maior ou igual a n inicial"));
                                return;
                            }

                            String relatorio = relatorioLegendreComum(nInicio, nFim);
                            salvarResultadoTemporario(relatorio, "conjectura_legendre_intervalo");

                        } catch (Exception e) {
                            runOnUiThread(() -> resultadoView.setText("Erro: " + e.getMessage()));
                        }
                    }).start();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnVerificar);

        // Botão para parar e salvar estado
        Button btnParar = new Button(this);
        btnParar.setText("⏹️ Parar e Salvar Estado");
        btnParar.setBackgroundColor(0xFFFF5722);
        btnParar.setTextColor(0xFFFFFFFF);
        btnParar.setOnClickListener(pararView -> {
            // Parada imediata do usuário
            this.paradaUsuario = true;
            Toast.makeText(this, "Parando execução...", Toast.LENGTH_SHORT).show();
        });
        contentContainer.addView(btnParar);

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    private void iniciarLegendreContinuo(String entrada, TextView alvo, Button retomar) {
        if (!legendreEmAndamento.compareAndSet(false, true)) return;
        paradaUsuario = false;
        retomar.setEnabled(false);
        alvo.setText("Verificando quadrados consecutivos…");
        rolarConteudoParaView(alvo);
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("ConjecturaLegendre", MODE_PRIVATE);
            String relatorio;
            try {
                BigInteger n = parseBigInteger(entrada);
                String inicio = n.toString();
                if (n.signum() <= 0 || inicio.length() > 10000) {
                    throw new IllegalArgumentException("Informe um n positivo com até 10.000 dígitos.");
                }
                String proximo = inicio;
                String ultimo = "";
                long testados = 0;
                boolean inconclusivo = false;
                long atualizacao = android.os.SystemClock.elapsedRealtime();
                while (!paradaUsuario && proximo.length() <= 10000) {
                    String candidato = legendreWitnessNativo(proximo);
                    if (candidato == null || candidato.isEmpty()) {
                        inconclusivo = !paradaUsuario;
                        break;
                    }
                    ultimo = candidato;
                    ++testados;
                    n = n.add(BigInteger.ONE);
                    proximo = n.toString();
                    long agora = android.os.SystemClock.elapsedRealtime();
                    if (agora - atualizacao >= 1000) {
                        atualizacao = agora;
                        prefs.edit().putString("ultimo_n", proximo).apply();
                        long v = testados;
                        runOnUiThread(() -> alvo.setText("Intervalos com candidato: " + v));
                    }
                }
                prefs.edit().putString("ultimo_n", proximo.length() <= 10000 ? proximo : "").apply();
                String estado = inconclusivo ? "Busca inconclusiva para n = " + proximo
                        + ". A ausência de candidato não refuta a conjectura."
                        : paradaUsuario ? "Busca interrompida." : "Limite de n atingido.";
                relatorio = "CONJECTURA DE LEGENDRE - BUSCA CONTÍNUA\n\nn inicial: " + inicio
                        + "\nIntervalos com primo ou provável primo: " + testados
                        + "\nÚltimo candidato: " + (ultimo.isEmpty() ? "nenhum" : ultimo) + "\n" + estado;
            } catch (Exception e) {
                relatorio = "Erro: " + e.getMessage();
            }
            String resultado = relatorio;
            runOnUiThread(() -> {
                legendreEmAndamento.set(false);
                retomar.setEnabled(true);
                retomar.setVisibility(prefs.getString("ultimo_n", "").isEmpty() ? View.GONE : View.VISIBLE);
                alvo.setText(resultado);
                if (resultado.startsWith("CONJECTURA") && alvo.isShown() && hasWindowFocus()) {
                    salvarResultadoTemporario(resultado, "conjectura_legendre_continua");
                }
            });
        }).start();
    }

    private void criarInterfaceMersenne() {
        contentContainer.removeAllViews();


        // Título
        TextView titulo = new TextView(this);
        titulo.setText("Números de Mersenne");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        contentContainer.addView(titulo);

        // Descrição
        TextView descricao = new TextView(this);
        descricao.setText("Números de Mersenne: M(p) = 2^p − 1, com p primo.\n" +
            "O expoente p pode ser um inteiro ou uma expressão cujo resultado seja o expoente (ex.: 31, 5+2*3, (40-9)).");
        descricao.setPadding(0, 0, 0, 16);
        contentContainer.addView(descricao);

        adicionarRotuloCampo("Expoente p (M(p) = 2^p − 1) — inteiro ou expressão");
        EditText inputP = new EditText(this);
        inputP.setHint("Ex.: 31 | 7 | (50-19) | 2^4-1");
        inputP.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        estilizarCampoEntrada(inputP);
        contentContainer.addView(inputP);

        // Botão calcular
        Button btnCalcular = new Button(this);
        btnCalcular.setText("⚡ Calcular Número de Mersenne");
        btnCalcular.setBackgroundColor(0xFF9C27B0);
        btnCalcular.setTextColor(0xFFFFFFFF);
        btnCalcular.setOnClickListener(v -> {
            try {
                final String entradaP = inputP.getText().toString().trim();
                if (entradaP.isEmpty()) {
                    Toast.makeText(this, "Digite o expoente p", Toast.LENGTH_SHORT).show();
                    return;
                }
                BigInteger pBig = parseBigIntegerOuExpressao(entradaP);
                if (pBig.compareTo(BigInteger.valueOf(2)) < 0) {
                    Toast.makeText(this, "O expoente p deve ser ≥ 2", Toast.LENGTH_SHORT).show();
                    return;
                }
                final int p;
                try {
                    p = pBig.intValueExact();
                } catch (ArithmeticException ex) {
                    Toast.makeText(this, "Expoente p fora do intervalo de int. Use um valor que caiba em 32 bits assinados.", Toast.LENGTH_LONG).show();
                    return;
                }

                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Números de Mersenne",
                    "p = " + p + " (entrada: " + entradaP + ")"
                );

                new Thread(() -> {
                    try {
                        String relatorio = mersenneReportNativo(p, false);
                        if (relatorio == null || relatorio.isEmpty()) {
                            throw new IllegalStateException("Não foi possível calcular o número de Mersenne.");
                        }
                        pararCronometroOperacao(cronometroAtivo);
                        salvarResultadoTemporario(relatorio, "numeros_mersenne");

            } catch (Exception e) {
                        pararCronometroOperacao(cronometroAtivo);
                        runOnUiThread(() -> resultadoView.setText("Erro: " + e.getMessage()));
                    }
                }).start();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnCalcular);

        Button btnListar = new Button(this);
        btnListar.setAllCaps(false);
        btnListar.setText("Listar Mersenne primos até o expoente");
        btnListar.setOnClickListener(v -> {
            int p;
            try {
                p = parseBigIntegerOuExpressao(inputP.getText().toString()).intValueExact();
                if (p < 2 || p > 10000) throw new IllegalArgumentException();
            } catch (IllegalArgumentException | ArithmeticException e) {
                Toast.makeText(this, "Use um expoente máximo entre 2 e 10.000.", Toast.LENGTH_SHORT).show();
                return;
            }
            CronometroOperacaoHandle cronometro = iniciarCronometroOperacao(resultadoView,
                "Listagem de Mersenne", "Expoente máximo: " + p);
            new Thread(() -> {
                try {
                    String report = mersenneReportNativo(p, true);
                    if (report == null) throw new IllegalStateException("Faixa indisponível.");
                    salvarResultadoTemporario(report, "numeros_mersenne_listagem");
                } catch (Exception e) {
                    runOnUiThread(() -> resultadoView.setText("Erro: " + e.getMessage()));
                } finally {
                    pararCronometroOperacao(cronometro);
                }
            }).start();
        });
        contentContainer.addView(btnListar);

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    private void criarInterfaceEstatisticas() {
        try {
            // Limpar interface anterior
            contentContainer.removeAllViews();

            // Título principal
            TextView titulo = new TextView(this);
            titulo.setText("📊 Estatísticas de Números Primos");
            titulo.setTextSize(20);
            titulo.setTextColor(0xFF2E7D32);
            titulo.setPadding(16, 20, 16, 20);
            titulo.setGravity(android.view.Gravity.CENTER);
            contentContainer.addView(titulo);

            // Seção de estatísticas pessoais do usuário
            LinearLayout statsPessoaisLayout = new LinearLayout(this);
            statsPessoaisLayout.setOrientation(LinearLayout.VERTICAL);
            statsPessoaisLayout.setPadding(16, 16, 16, 16);
            statsPessoaisLayout.setBackgroundColor(0xFFF5F5F5);

            TextView tituloPessoal = new TextView(this);
            tituloPessoal.setText("👤 Suas Estatísticas de Uso");
            tituloPessoal.setTextSize(16);
            tituloPessoal.setTextColor(0xFF1976D2);
            tituloPessoal.setTypeface(null, Typeface.BOLD);
            tituloPessoal.setPadding(0, 0, 0, 8);
            statsPessoaisLayout.addView(tituloPessoal);

            // Status premium
            TextView statusPremium = new TextView(this);
            if (!MONETIZACAO_ATIVA) {
                statusPremium.setText("Acesso gratuito");
                statusPremium.setTextColor(0xFF4CAF50);
            } else if (isPremium) {
                statusPremium.setText("💎 Status: PREMIUM ATIVO");
                statusPremium.setTextColor(0xFF4CAF50);
            } else {
                statusPremium.setText("🆓 Status: VERSÃO GRATUITA");
                statusPremium.setTextColor(0xFFFF9800);
            }
            statusPremium.setTextSize(14);
            statusPremium.setTypeface(null, Typeface.BOLD);
            statusPremium.setPadding(0, 4, 0, 4);
            statsPessoaisLayout.addView(statusPremium);

            // Cálculos do dia
            TextView statsDia = new TextView(this);
            if (!MONETIZACAO_ATIVA) {
                statsDia.setText("Cálculos hoje: " + dailyCalculations);
            } else if (isPremium) {
                statsDia.setText("✅ Cálculos hoje: Ilimitados");
            } else {
                statsDia.setText("📊 Cálculos hoje: " + dailyCalculations + "/" + FREE_DAILY_LIMIT);
            }
            statsDia.setTextSize(14);
            statsDia.setTextColor(0xFF333333);
            statsDia.setPadding(0, 4, 0, 4);
            statsPessoaisLayout.addView(statsDia);

            // Limite de valor
            TextView limiteValor = new TextView(this);
            if (isPremium) {
                limiteValor.setText("✅ Limite de valor: Ilimitado");
            } else {
                limiteValor.setText("🔢 Limite de valor: Até " + String.format("%,d", FREE_MAX_VALUE));
            }
            limiteValor.setTextSize(14);
            limiteValor.setTextColor(0xFF333333);
            limiteValor.setPadding(0, 4, 0, 4);
            if (MONETIZACAO_ATIVA) {
                statsPessoaisLayout.addView(limiteValor);
            }

            // Barra de progresso (se não premium)
            if (MONETIZACAO_ATIVA && !isPremium) {
                ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                progressBar.setMax(FREE_DAILY_LIMIT);
                progressBar.setProgress(dailyCalculations);
                progressBar.setPadding(0, 8, 0, 8);
                statsPessoaisLayout.addView(progressBar);

                TextView progressText = new TextView(this);
                progressText.setText("Progresso do dia: " + (dailyCalculations * 100 / FREE_DAILY_LIMIT) + "%");
                progressText.setTextSize(12);
                progressText.setTextColor(0xFF666666);
                progressText.setGravity(android.view.Gravity.CENTER);
                statsPessoaisLayout.addView(progressText);
            }

            // Botão upgrade (se não premium)
            if (MONETIZACAO_ATIVA && !isPremium) {
                Button btnUpgrade = new Button(this);
                btnUpgrade.setText("💎 Upgrade para Premium");
                btnUpgrade.setBackgroundColor(0xFF4CAF50);
                btnUpgrade.setTextColor(0xFFFFFFFF);
                btnUpgrade.setPadding(16, 8, 16, 8);
                btnUpgrade.setOnClickListener(v -> mostrarTelaUpgrade());
                statsPessoaisLayout.addView(btnUpgrade);
            }

            contentContainer.addView(statsPessoaisLayout);

            // Separador
            View separador = new View(this);
            separador.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
            separador.setBackgroundColor(0xFFE0E0E0);
            contentContainer.addView(separador);

            // Descrição das estatísticas matemáticas
            TextView descricao = new TextView(this);
            descricao.setText("Análise estatística completa da distribuição de números primos\nEscolha entre estatística completa ou aproximação logarítmica");
            descricao.setTextSize(14);
            descricao.setTextColor(0xFF666666);
            descricao.setPadding(16, 20, 16, 20);
            descricao.setGravity(android.view.Gravity.CENTER);
            contentContainer.addView(descricao);

            adicionarRotuloCampo("Limite superior N (estatísticas até N)");
            EditText inputLimite = new EditText(this);
            inputLimite.setHint("Digite aqui (ex.: 1000)");
            inputLimite.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            inputLimite.setText("1000");
            estilizarCampoEntrada(inputLimite);
            contentContainer.addView(inputLimite);

            // Botões para estatísticas gerais
            Button btnEstatisticasCompleta = new Button(this);
            btnEstatisticasCompleta.setText("📊 Estatística Completa");
            btnEstatisticasCompleta.setBackgroundColor(0xFF4CAF50);
            btnEstatisticasCompleta.setTextColor(0xFFFFFFFF);
            btnEstatisticasCompleta.setPadding(16, 12, 16, 12);
            btnEstatisticasCompleta.setOnClickListener(v -> {
                try {
                    String inputText = inputLimite.getText().toString();
                    if (inputText.isEmpty()) {
                        Toast.makeText(this, "Digite um limite para as estatísticas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long n = Long.parseLong(inputText);
                    if (n < 2) {
                        Toast.makeText(this, "Digite um número ≥ 2", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Verificar limite máximo para estatística completa
                    if (n > 10000000000L) {
                        Toast.makeText(this, "Para limites > 10 bilhões, use 'Aproximação Logarítmica'", Toast.LENGTH_LONG).show();
                        return;
                    }

                    CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                        resultadoView,
                        "Estatísticas Completas",
                        "Limite: " + String.format("%,d", n)
                    );

                    // Executar estatísticas em thread separada
                    new Thread(() -> {
                        try {
                            String relatorioComum = primeStatsNativo(n);
                            if (relatorioComum == null || relatorioComum.isEmpty()) {
                                throw new IllegalStateException("Não foi possível calcular as estatísticas.");
                            }
                            pararCronometroOperacao(cronometroAtivo);
                            salvarResultadoTemporario(relatorioComum, "estatisticas_primos_completas");

                        } catch (Exception e) {
                            Log.e(TAG, "Erro nas estatísticas", e);
                            runOnUiThread(() -> {
                                pararCronometroOperacao(cronometroAtivo);
                                Toast.makeText(this, "Erro nas estatísticas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar estatísticas", e);
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            contentContainer.addView(btnEstatisticasCompleta);

            // Botão para aproximação logarítmica
            Button btnAproximacaoLogaritmica = new Button(this);
            btnAproximacaoLogaritmica.setText("📈 Aproximação Logarítmica");
            btnAproximacaoLogaritmica.setBackgroundColor(0xFF2196F3);
            btnAproximacaoLogaritmica.setTextColor(0xFFFFFFFF);
            btnAproximacaoLogaritmica.setPadding(16, 12, 16, 12);
            btnAproximacaoLogaritmica.setOnClickListener(v -> {
                try {
                    String inputText = inputLimite.getText().toString();
                    if (inputText.isEmpty()) {
                        Toast.makeText(this, "Digite um limite para as estatísticas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Suporte para números muito grandes usando BigInteger
                    BigInteger n;
                    try {
                        n = parseBigInteger(inputText);
                        if (n.compareTo(BigInteger.valueOf(2)) < 0) {
                            Toast.makeText(this, "Digite um número ≥ 2", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Número muito grande ou inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                        resultadoView,
                        "Aproximação Logarítmica",
                        "Dígitos: " + n.toString().length(),
                        "Bits: " + n.bitLength()
                    );

                    // Executar aproximação logarítmica em thread separada
                    new Thread(() -> {
                        try {
                            String relatorioComum = primeApproximationNativo(n.toString());
                            if (relatorioComum == null || relatorioComum.isEmpty()) {
                                throw new IllegalArgumentException("Use um inteiro a partir de 2, com até 100.000 dígitos.");
                            }
                            pararCronometroOperacao(cronometroAtivo);
                            salvarResultadoTemporario(relatorioComum, "aproximacao_logaritmica_primos");

                        } catch (Exception e) {
                            Log.e(TAG, "Erro na aproximação logarítmica", e);
                            runOnUiThread(() -> {
                                pararCronometroOperacao(cronometroAtivo);
                                Toast.makeText(this, "Erro na aproximação: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar aproximação", e);
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            contentContainer.addView(btnAproximacaoLogaritmica);

            // Espaçador final
            TextView espacador2 = new TextView(this);
            espacador2.setText("");
            espacador2.setPadding(0, 30, 0, 30);
            contentContainer.addView(espacador2);

            // Mostrar interface
            mostrarTelaConteudo();
            if (menuScrollView != null) {
                menuScrollView.setVisibility(View.GONE);
            } else {
                menuContainer.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar interface de estatísticas", e);
            Toast.makeText(this, "Erro ao criar interface: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void criarInterfaceNumerosPerfeitos() {
        try {
            contentContainer.removeAllViews();

            // Criar ScrollView para permitir rolar
            ScrollView scrollView = new ScrollView(this);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ));

            // Container interno para os elementos
            LinearLayout containerInterno = new LinearLayout(this);
            containerInterno.setOrientation(LinearLayout.VERTICAL);
            containerInterno.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            containerInterno.setPadding(20, 20, 20, 40);


            // Título
            TextView titulo = new TextView(this);
            titulo.setText("Números Perfeitos - Busca por Ímpares (10.000+ Dígitos)");
            titulo.setTextSize(18);
            titulo.setPadding(0, 16, 0, 16);
            containerInterno.addView(titulo);

            // Descrição
            TextView descricao = new TextView(this);
            descricao.setText("Um número perfeito é igual à soma de seus divisores próprios.\n\n" +
                "🔍 FOCO: Busca por NÚMEROS PERFEITOS ÍMPARES (mistério matemático)\n" +
                "✅ Números perfeitos pares já são conhecidos (teorema de Euclides)\n" +
                "❓ Números perfeitos ímpares: existem? Ninguém sabe!\n\n" +
                "Suporte a números de até 10.000 dígitos com busca otimizada.");
            descricao.setPadding(0, 0, 0, 16);
            containerInterno.addView(descricao);

            // Seleção de modo
            TextView modoLabel = new TextView(this);
            modoLabel.setText("Modo de busca:");
            modoLabel.setPadding(0, 10, 0, 5);
            containerInterno.addView(modoLabel);

            // RadioGroup
            RadioGroup modoGroup = new RadioGroup(this);
            modoGroup.setOrientation(LinearLayout.VERTICAL);
            modoGroup.setPadding(0, 10, 0, 20);

            RadioButton modoVerificar = new RadioButton(this);
            modoVerificar.setText("Verificar Número");
            modoVerificar.setId(1);
            modoVerificar.setChecked(true);
            modoVerificar.setPadding(0, 8, 0, 8);
            modoVerificar.setTextSize(16);
            modoGroup.addView(modoVerificar);

            RadioButton modoBuscarImpar = new RadioButton(this);
            modoBuscarImpar.setText("🔍 Buscar Ímpares");
            modoBuscarImpar.setId(2);
            modoBuscarImpar.setPadding(0, 8, 0, 8);
            modoBuscarImpar.setTextSize(16);
            modoGroup.addView(modoBuscarImpar);


            RadioButton modoBuscaSequencial = new RadioButton(this);
            modoBuscaSequencial.setText("🔄 Busca Sequencial Contínua");
            modoBuscaSequencial.setId(4);
            modoBuscaSequencial.setPadding(0, 8, 0, 8);
            modoBuscaSequencial.setTextSize(16);
            modoGroup.addView(modoBuscaSequencial);

            RadioButton modoParesLimite = new RadioButton(this);
            modoParesLimite.setText("Perfeitos pares até um limite");
            modoParesLimite.setId(5);
            modoGroup.addView(modoParesLimite);

            RadioButton modoParesDigitos = new RadioButton(this);
            modoParesDigitos.setText("Perfeitos pares por quantidade de dígitos");
            modoParesDigitos.setId(6);
            modoGroup.addView(modoParesDigitos);

            containerInterno.addView(modoGroup);

            // Campos para modo verificar
            LinearLayout layoutVerificar = new LinearLayout(this);
            layoutVerificar.setOrientation(LinearLayout.VERTICAL);
            layoutVerificar.setId(100);
            layoutVerificar.setPadding(0, 10, 0, 20);

            TextView lvN = new TextView(this);
            lvN.setText("Número a verificar");
            lvN.setTypeface(null, Typeface.BOLD);
            lvN.setPadding(dpUi(4), dpUi(4), dpUi(4), dpUi(4));
            layoutVerificar.addView(lvN);
            EditText inputN = new EditText(this);
            inputN.setHint("Digite aqui (ex.: 28, 10^100 — até 10.000 dígitos)");
            inputN.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            estilizarCampoEntrada(inputN);
            layoutVerificar.addView(inputN);

            containerInterno.addView(layoutVerificar);

            // Campos para modo buscar ímpares
            LinearLayout layoutBuscarImpar = new LinearLayout(this);
            layoutBuscarImpar.setOrientation(LinearLayout.VERTICAL);
            layoutBuscarImpar.setId(200);
            layoutBuscarImpar.setVisibility(View.GONE);
            layoutBuscarImpar.setPadding(0, 10, 0, 20);

            TextView ld = new TextView(this);
            ld.setText("Número de dígitos");
            ld.setTypeface(null, Typeface.BOLD);
            ld.setPadding(dpUi(4), dpUi(4), dpUi(4), dpUi(4));
            layoutBuscarImpar.addView(ld);
            EditText inputDigitos = new EditText(this);
            inputDigitos.setHint("Dígitos de 1 a 12");
            inputDigitos.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            inputDigitos.setText("");
            estilizarCampoEntrada(inputDigitos);
            layoutBuscarImpar.addView(inputDigitos);

            TextView lt = new TextView(this);
            lt.setText("Máximo de tentativas");
            lt.setTypeface(null, Typeface.BOLD);
            lt.setPadding(dpUi(4), dpUi(8), dpUi(4), dpUi(4));
            layoutBuscarImpar.addView(lt);
            EditText inputMaxTentativas = new EditText(this);
            inputMaxTentativas.setHint("Digite aqui (ex.: 100000)");
            inputMaxTentativas.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            inputMaxTentativas.setText("100000");
            estilizarCampoEntrada(inputMaxTentativas);
            layoutBuscarImpar.addView(inputMaxTentativas);

            TextView infoBusca = new TextView(this);
            infoBusca.setText("🔍 Busca FOCADA em números ÍMPARES apenas\n" +
                "⚠️ Números pares seguem teorema de Euclides (já conhecidos)\n" +
                "🎯 Objetivo: Descobrir se existem números perfeitos ímpares!");
            infoBusca.setPadding(15, 10, 15, 10);
            infoBusca.setTextColor(Color.parseColor("#FF6B35"));
            layoutBuscarImpar.addView(infoBusca);

            containerInterno.addView(layoutBuscarImpar);


            // Campos para modo busca sequencial contínua
            LinearLayout layoutBuscaSequencial = new LinearLayout(this);
            layoutBuscaSequencial.setOrientation(LinearLayout.VERTICAL);
            layoutBuscaSequencial.setId(400);
            layoutBuscaSequencial.setVisibility(View.GONE);
            layoutBuscaSequencial.setPadding(0, 10, 0, 20);

            TextView lds = new TextView(this);
            lds.setText("Dígitos iniciais (busca sequencial)");
            lds.setTypeface(null, Typeface.BOLD);
            lds.setPadding(dpUi(4), dpUi(4), dpUi(4), dpUi(4));
            layoutBuscaSequencial.addView(lds);
            EditText inputDigitosSequencial = new EditText(this);
            inputDigitosSequencial.setHint("Dígitos iniciais de 1 a 12");
            inputDigitosSequencial.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            inputDigitosSequencial.setText("");
            estilizarCampoEntrada(inputDigitosSequencial);
            layoutBuscaSequencial.addView(inputDigitosSequencial);

            // Campo removido: Máximo de tentativas não é necessário para busca infinita

            TextView infoSequencial = new TextView(this);
            infoSequencial.setText("Busca exata de ímpares até 12 dígitos. O progresso fica salvo no aparelho.");
            infoSequencial.setPadding(15, 10, 15, 10);
            infoSequencial.setTextColor(Color.parseColor("#9C27B0"));
            layoutBuscaSequencial.addView(infoSequencial);

            Button btnRetomar = new Button(this);
            btnRetomar.setText("Continuar busca salva");
            btnRetomar.setVisibility(proximoImparSalvo() > 0 ? View.VISIBLE : View.GONE);
            layoutBuscaSequencial.addView(btnRetomar);

            containerInterno.addView(layoutBuscaSequencial);

            // Listener para mudança de modo
            modoGroup.setOnCheckedChangeListener((group, checkedId) -> {
                layoutVerificar.setVisibility(View.GONE);
                layoutBuscarImpar.setVisibility(View.GONE);
                layoutBuscaSequencial.setVisibility(View.GONE);

                if (checkedId == 1 || checkedId == 5 || checkedId == 6) {
                    lvN.setText(checkedId == 5 ? "Limite superior"
                        : checkedId == 6 ? "Quantidade de dígitos" : "Número a verificar");
                    layoutVerificar.setVisibility(View.VISIBLE);
                } else if (checkedId == 2) {
                    layoutBuscarImpar.setVisibility(View.VISIBLE);
                } else if (checkedId == 4) {
                    layoutBuscaSequencial.setVisibility(View.VISIBLE);
                }
            });

            // Botão verificar - POSICIONADO ANTES DA ÁREA DE RESULTADO
            Button btnVerificar = new Button(this);
            btnVerificar.setText("Calcular");
            btnVerificar.setBackgroundColor(0xFFFF9800);
            btnVerificar.setTextColor(0xFFFFFFFF);
            btnVerificar.setPadding(20, 15, 20, 15);
            containerInterno.addView(btnVerificar);
            Button btnParar = new Button(this);
            btnParar.setText("Parar e ver resultado");
            btnParar.setVisibility(View.GONE);
            btnParar.setOnClickListener(v -> interromperBuscaImpar.set(true));
            containerInterno.addView(btnParar);

            // Área de resultado específica
            TextView areaResultado = new TextView(this);
            areaResultado.setPadding(16, 16, 16, 16);
            areaResultado.setText("Selecione um modo e clique em 'Verificar Número Perfeito' para começar...");
            areaResultado.setTextIsSelectable(true);
            areaResultado.setBackgroundColor(Color.WHITE);
            containerInterno.addView(areaResultado);
            areaResultado.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View view) {}
                @Override public void onViewDetachedFromWindow(View view) {
                    interromperBuscaImpar.set(true);
                }
            });
            btnRetomar.setOnClickListener(v -> {
                long proximo = proximoImparSalvo();
                if (proximo > 0) iniciarBuscaImpares(1, 0, true, proximo, areaResultado, btnParar, btnRetomar);
            });

            // Adicionar container interno ao ScrollView
            scrollView.addView(containerInterno);

            // Adicionar ScrollView ao container principal
            contentContainer.addView(scrollView);

            btnVerificar.setOnClickListener(v -> {
                try {
                    int modo = modoGroup.getCheckedRadioButtonId();

                    if (modo == 5 || modo == 6) {
                        long valor = parseBigInteger(inputN.getText().toString()).longValueExact();
                        if (valor < 1 || (modo == 6 && valor > 10000)) {
                            Toast.makeText(this, modo == 6 ? "Informe de 1 a 10.000 dígitos."
                                : "Informe um limite positivo.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int operacao = modo == 5 ? 1 : 2; // PPF_PERFECT_BY_LIMIT / PPF_PERFECT_BY_DIGITS
                        CronometroOperacaoHandle cronometro = iniciarCronometroOperacao(
                            areaResultado, "Números Perfeitos", "Valor: " + valor);
                        new Thread(() -> {
                            try {
                                String relatorio = perfectReportNativo(operacao, valor);
                                if (relatorio == null || relatorio.isEmpty()) {
                                    throw new IllegalStateException("Resultado indisponível.");
                                }
                                salvarResultadoTemporario(relatorio, "numeros_perfeitos_pares");
                            } catch (Exception e) {
                                runOnUiThread(() -> areaResultado.setText("Erro: " + e.getMessage()));
                            } finally {
                                pararCronometroOperacao(cronometro);
                            }
                        }).start();
                    } else if (modo == 1) {
                        // Modo verificar
                        String nStr = inputN.getText().toString();
                        if (nStr.isEmpty()) {
                            Toast.makeText(this, "Digite um número para verificar", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                            areaResultado,
                            "Verificação de Número Perfeito",
                            "Entrada: " + nStr
                        );

                        new Thread(() -> {
                            try {
                                String decimal = parseBigInteger(nStr).toString();
                                String relatorio = perfectVerifyNativo(decimal);
                                if (relatorio == null || relatorio.isEmpty()) {
                                    throw new IllegalArgumentException("Informe um inteiro positivo de até 10.000 dígitos.");
                                }
                                runOnUiThread(() -> {
                                    pararCronometroOperacao(cronometroAtivo);
                                    areaResultado.setText(relatorio);
                                    salvarResultadoTemporario(relatorio, "numeros_perfeitos_verificacao");
                                });

                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    pararCronometroOperacao(cronometroAtivo);
                                    areaResultado.setText("Erro: " + e.getMessage());
                                });
                            }
                        }).start();

                    } else if (modo == 2 || modo == 4) {
                        boolean sequencial = modo == 4;
                        int digitos = Integer.parseInt((sequencial ? inputDigitosSequencial : inputDigitos).getText().toString());
                        int tentativas = sequencial ? 0 : Integer.parseInt(inputMaxTentativas.getText().toString());
                        if (digitos < 1 || digitos > 12 || (!sequencial && (tentativas < 1 || tentativas > 100000))) {
                            Toast.makeText(this, "Use de 1 a 12 dígitos e até 100.000 tentativas.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        iniciarBuscaImpares(digitos, tentativas, sequencial, 0, areaResultado, btnParar, btnRetomar);
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Digite números válidos", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao criar interface: " + e.getMessage(), Toast.LENGTH_LONG).show();
            contentContainer.removeAllViews();
            TextView erro = new TextView(this);
            erro.setText("❌ Erro ao carregar interface dos Números Perfeitos");
            erro.setPadding(20, 20, 20, 20);
            contentContainer.addView(erro);
        }
    }

    private long proximoImparSalvo() {
        return getSharedPreferences("numeros_perfeitos_sequencial", MODE_PRIVATE).getLong("proximo_exato", 0);
    }

    private void iniciarBuscaImpares(int digitos, int tentativas, boolean sequencial, long retomar,
                                     TextView areaResultado, Button btnParar, Button btnRetomar) {
        if (!buscaImparEmAndamento.compareAndSet(false, true)) return;
        interromperBuscaImpar.set(false);
        btnParar.setVisibility(View.VISIBLE);
        btnRetomar.setEnabled(false);
        areaResultado.setText("Testando candidatos ímpares…");
        rolarConteudoParaView(areaResultado);

        new Thread(() -> {
            long testados = 0;
            long encontrado = 0;
            long limite = 999_999_999_999L;
            long proximo = retomar;
            String estado;
            SharedPreferences prefs = getSharedPreferences("numeros_perfeitos_sequencial", MODE_PRIVATE);
            try {
                long base = 1;
                for (int i = 1; i < digitos; i++) base *= 10;
                long primeiro = base | 1L;
                if (!sequencial) limite = base * 10 - 1;
                long candidatos = (limite - primeiro) / 2 + 1;
                long total = Math.min(tentativas, candidatos);
                if (proximo <= 0) {
                    proximo = sequencial ? primeiro
                            : primeiro + 2 * java.util.concurrent.ThreadLocalRandom.current().nextLong(candidatos);
                }
                long ultimaAtualizacao = android.os.SystemClock.elapsedRealtime();
                while (!interromperBuscaImpar.get() && encontrado == 0
                        && (sequencial ? proximo <= limite : testados < total)) {
                    if (proximo > limite) proximo = primeiro;
                    int lote = sequencial ? 64 : (int) Math.min(64, total - testados);
                    long[] resultado = scanOddPerfectNativo(proximo, limite, lote);
                    if (resultado == null) throw new IllegalStateException("Não foi possível verificar o próximo lote.");
                    proximo = resultado[0];
                    encontrado = resultado[1];
                    testados += resultado[2];
                    long agora = android.os.SystemClock.elapsedRealtime();
                    if (agora - ultimaAtualizacao >= 250) {
                        ultimaAtualizacao = agora;
                        if (sequencial) prefs.edit().putLong("proximo_exato", proximo <= limite && encontrado == 0 ? proximo : 0).apply();
                        long progresso = testados;
                        runOnUiThread(() -> areaResultado.setText("Candidatos testados: " + progresso));
                    }
                }
                if (encontrado > 0) {
                    estado = "Candidato: " + encontrado + ". Confirme o resultado por um método independente.";
                } else {
                    String prefixo = interromperBuscaImpar.get() ? "Busca interrompida. " : "";
                    estado = prefixo + "Nenhum número perfeito ímpar entre os candidatos testados.";
                }
            } catch (Exception e) {
                estado = "Não foi possível terminar a busca: " + e.getMessage();
            }
            if (sequencial) prefs.edit().putLong("proximo_exato", proximo <= limite && encontrado == 0 ? proximo : 0).apply();
            String relatorio = "BUSCA POR PERFEITOS ÍMPARES\n\nCandidatos testados: " + testados + "\n\n" + estado;
            runOnUiThread(() -> {
                buscaImparEmAndamento.set(false);
                btnParar.setVisibility(View.GONE);
                btnRetomar.setVisibility(proximoImparSalvo() > 0 ? View.VISIBLE : View.GONE);
                btnRetomar.setEnabled(true);
                areaResultado.setText(relatorio);
                if (areaResultado.isShown() && hasWindowFocus()) {
                    salvarResultadoTemporario(relatorio, "numeros_perfeitos_impares");
                }
            });
        }).start();
    }

    private void criarInterfaceSegurancaDigital() {
        contentContainer.removeAllViews();

        // Um único ScrollView em altura total: formulário + botões + área de status (cronômetro) na mesma rolagem
        if (resultadoView != null) {
            ViewGroup rp = (ViewGroup) resultadoView.getParent();
            if (rp != null) {
                rp.removeView(resultadoView);
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        scroll.setFillViewport(true);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(inner);
        contentContainer.addView(scroll);

        LinearLayout container = inner;

        // Título
        TextView titulo = new TextView(this);
        titulo.setText("🔐 Segurança Digital - Criptografia Educacional");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        container.addView(titulo);

        // Descrição educacional
        TextView descricao = new TextView(this);
        descricao.setText("Este módulo demonstra como a matemática dos primos protege suas comunicações digitais.");
        descricao.setPadding(0, 0, 0, 16);
        container.addView(descricao);

        // Aviso sobre natureza aleatória da geração de chaves RSA
        TextView avisoRSA = new TextView(this);
        avisoRSA.setText(
            "ℹ️ A geração de chaves RSA envolve buscas aleatórias por grandes números primos, " +
            "por isso o tempo pode variar bastante de uma execução para outra. " +
            "Se a geração demorar muito, é recomendável cancelar e tentar novamente com os mesmos parâmetros ou com um tamanho de chave diferente."
        );
        avisoRSA.setTextSize(12);
        avisoRSA.setTextColor(Color.parseColor("#555555"));
        avisoRSA.setPadding(0, 0, 0, 20);
        container.addView(avisoRSA);

        // Rótulo local para o tamanho da chave (usa o container interno deste card)
        TextView rotuloBits = new TextView(this);
        rotuloBits.setText("Tamanho da chave RSA (bits)");
        rotuloBits.setTextSize(14);
        rotuloBits.setTypeface(null, Typeface.BOLD);
        rotuloBits.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#37474F"));
        rotuloBits.setPadding(dpUi(4), dpUi(12), dpUi(4), dpUi(4));
        container.addView(rotuloBits);

        EditText inputBitsRSA = new EditText(this);
        inputBitsRSA.setHint("Ex.: 1024, 2048, 4096… (512 a 33000)");
        inputBitsRSA.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputBitsRSA.setText("1024");
        estilizarCampoEntrada(inputBitsRSA);
        container.addView(inputBitsRSA);

        // Rótulo local para a mensagem de exemplo
        TextView rotuloMsg = new TextView(this);
        rotuloMsg.setText("Mensagem de exemplo (demonstrações de cifra / hash / assinatura)");
        rotuloMsg.setTextSize(14);
        rotuloMsg.setTypeface(null, Typeface.BOLD);
        rotuloMsg.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#37474F"));
        rotuloMsg.setPadding(dpUi(4), dpUi(12), dpUi(4), dpUi(4));
        container.addView(rotuloMsg);

        EditText inputMensagem = new EditText(this);
        inputMensagem.setHint("Digite aqui o texto para os demonstrativos abaixo");
        inputMensagem.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputMensagem.setMinLines(2);
        inputMensagem.setText("Hello World!");
        estilizarCampoEntrada(inputMensagem);
        container.addView(inputMensagem);

        // Botão para gerar chaves RSA
        Button btnGerarChavesRSA = new Button(this);
        btnGerarChavesRSA.setText("🔑 Gerar Chaves RSA");
        btnGerarChavesRSA.setBackgroundColor(0xFF2196F3); // Azul
        btnGerarChavesRSA.setTextColor(0xFFFFFFFF);
        btnGerarChavesRSA.setOnClickListener(v -> {
            try {
                int bits = Integer.parseInt(inputBitsRSA.getText().toString().trim());

                if (bits < 512) {
                    Toast.makeText(this, "Use pelo menos 512 bits para segurança", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (bits > 33000) {
                    Toast.makeText(this, "Use no máximo 33000 bits", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String msg = inputMensagem.getText() != null ? inputMensagem.getText().toString() : "";
                final int bitsFinal = bits;

                Runnable abrirDialogoRelatorioRsa = () -> new AlertDialog.Builder(this)
                        .setTitle("Relatório de chaves RSA")
                        .setMessage("Incluir o texto da mensagem no relatório ou calcular e listar apenas os números (parâmetros e chaves)?")
                        .setPositiveButton("Incluir mensagem", (d, w) -> iniciarGeracaoRSAChaves(bitsFinal, true, msg))
                        .setNegativeButton("Apenas números", (d, w) -> iniciarGeracaoRSAChaves(bitsFinal, false, msg))
                        .setNeutralButton(android.R.string.cancel, null)
                        .show();

                if (rsaChaveImplicaPrimos8192BitsOuMais(bitsFinal)) {
                    mostrarDialogoEscolhaRsaUltra(bitsFinal, abrirDialogoRelatorioRsa);
                } else {
                    abrirDialogoRelatorioRsa.run();
                }

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite números válidos", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnGerarChavesRSA);

        // Botão para demonstração de criptografia
        Button btnDemonstrarCriptografia = new Button(this);
        btnDemonstrarCriptografia.setText("🔒 Demonstrar Criptografia/Descriptografia");
        btnDemonstrarCriptografia.setBackgroundColor(0xFF4CAF50); // Verde
        btnDemonstrarCriptografia.setTextColor(0xFFFFFFFF);
        btnDemonstrarCriptografia.setOnClickListener(v -> {
            try {
                String mensagem = inputMensagem.getText().toString();
                if (mensagem.isEmpty()) {
                    Toast.makeText(this, "Digite uma mensagem para criptografar", Toast.LENGTH_SHORT).show();
                    return;
                }

                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Demonstração de Criptografia",
                    "Caracteres: " + mensagem.length()
                );
                // Iniciando demonstração de criptografia RSA (sem mostrar na tela do app)

                new Thread(() -> {
                    try {
                        // Iniciar cronômetro
                        long startTime = System.currentTimeMillis();

                        StringBuilder resultado = new StringBuilder();
                        resultado.append("🔒 DEMONSTRAÇÃO DE CRIPTOGRAFIA RSA\n");
                        resultado.append("====================================\n\n");

                        resultado.append("📝 MENSAGEM ORIGINAL:\n");
                        resultado.append("   ").append(mensagem).append("\n\n");

                        resultado.append("🔢 CONVERSÃO PARA NÚMEROS:\n");
                        String mensagemNumerica = converterParaNumeros(mensagem);
                        resultado.append("   ").append(mensagemNumerica).append("\n\n");

                        resultado.append("🔑 PROCESSO DE CRIPTOGRAFIA:\n");
                        resultado.append("   • C = M^e mod n\n");
                        resultado.append("   • M = mensagem em números\n");
                        resultado.append("   • e = expoente público (geralmente 65537)\n");
                        resultado.append("   • n = produto de dois primos grandes\n\n");

                        resultado.append("🔓 PROCESSO DE DESCRIPTOGRAFIA:\n");
                        resultado.append("   • M = C^d mod n\n");
                        resultado.append("   • C = mensagem criptografada\n");
                        resultado.append("   • d = expoente privado (calculado com p e q)\n");
                        resultado.append("   • n = mesmo produto de primos\n\n");

                        resultado.append("🎯 DEMONSTRAÇÃO REAL COM SUA MENSAGEM:\n");
                        resultado.append("   • Mensagem: '").append(mensagem).append("'\n");
                        resultado.append("   • Conversão para números: ").append(mensagemNumerica).append("\n");
                        resultado.append("   • Tamanho da mensagem: ").append(mensagem.length()).append(" caracteres\n");
                        resultado.append("   • Valores numéricos: ");

                        // Mostrar valores numéricos reais
                        String[] numeros = mensagemNumerica.split(" ");
                        for (int i = 0; i < numeros.length; i++) {
                            if (i > 0) resultado.append(", ");
                            resultado.append(numeros[i]);
                        }
                        resultado.append("\n\n");

                        int bitsChave = 1024;
                        try {
                            bitsChave = Integer.parseInt(inputBitsRSA.getText().toString().trim());
                        } catch (NumberFormatException ignored) {
                            bitsChave = 1024;
                        }
                        if (bitsChave < 512) {
                            bitsChave = 512;
                        }
                        if (bitsChave > 33000) {
                            bitsChave = 33000;
                        }
                        resultado.append("🚀 CHAVES RSA (mesmo tamanho informado acima: ").append(bitsChave).append(" bits)\n");
                        resultado.append("   • Gerando primos p e q com o mesmo motor de \"Gerar Chaves RSA\"…\n");

                        BigInteger[] pq = gerarPrimosPqRSA(bitsChave);
                        BigInteger p = pq[0];
                        BigInteger q = pq[1];
                        BigInteger n = p.multiply(q);
                        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
                        BigInteger e = BigInteger.valueOf(65537);
                        BigInteger d = e.modInverse(phi);

                        resultado.append("   • Primo p: ").append(p).append(" (").append(p.bitLength()).append(" bits)\n");
                        resultado.append("   • Primo q: ").append(q).append(" (").append(q.bitLength()).append(" bits)\n");
                        resultado.append("   • n = p × q: ").append(n).append(" (").append(n.bitLength()).append(" bits)\n");
                        resultado.append("   • e = ").append(e).append("\n");
                        resultado.append("   • d = ").append(d).append(" (").append(d.bitLength()).append(" bits)\n\n");

                        resultado.append("🔐 CRIPTOGRAFIA REAL PASSO A PASSO:\n");
                        StringBuilder mensagemCriptografada = new StringBuilder();
                        StringBuilder mensagemDescriptografada = new StringBuilder();

                        for (int i = 0; i < numeros.length; i++) {
                            BigInteger M = new BigInteger(numeros[i]);
                            BigInteger C = M.modPow(e, n);
                            BigInteger M_decifrado = C.modPow(d, n);

                            resultado.append("   • M").append(i+1).append(" = ").append(M).append("\n");
                            resultado.append("     C").append(i+1).append(" = ").append(M).append("^").append(e).append(" mod ").append(n).append(" = ").append(C).append("\n");
                            resultado.append("     M").append(i+1).append("' = ").append(C).append("^").append(d).append(" mod ").append(n).append(" = ").append(M_decifrado).append(" ✓\n\n");

                            // Armazenar códigos ASCII para descriptografia
                            if (i > 0) {
                                mensagemCriptografada.append(" ");
                                mensagemDescriptografada.append(" ");
                            }
                            mensagemCriptografada.append(C);
                            mensagemDescriptografada.append(M_decifrado);
                        }

                        resultado.append("🔒 MENSAGEM CRIPTOGRAFADA COMPLETA:\n");
                        resultado.append("   ").append(mensagemCriptografada.toString()).append("\n\n");

                        resultado.append("🔓 VERIFICAÇÃO DE DESCRIPTOGRAFIA:\n");
                        resultado.append("   • Mensagem original: '").append(mensagem).append("'\n");
                        resultado.append("   • Mensagem descriptografada: '").append(converterDeNumeros(mensagemDescriptografada.toString())).append("'\n");
                        resultado.append("   • Verificação: ").append(mensagem.equals(converterDeNumeros(mensagemDescriptografada.toString())) ? "✅ CORRETO" : "❌ ERRO").append("\n\n");

                        resultado.append("🔐 SEGURANÇA DA CRIPTOGRAFIA:\n");
                        resultado.append("   • Sem a chave privada, é computacionalmente impossível\n");
                        resultado.append("   • Mesmo com n e e, não é possível calcular d\n");
                        resultado.append("   • A segurança depende da dificuldade de fatorar n\n");
                        resultado.append("   • Chaves maiores = mais tempo para quebrar\n\n");

                        resultado.append("⚡ LIMITAÇÕES DO RSA:\n");
                        resultado.append("   • Lento para mensagens grandes\n");
                        resultado.append("   • Usado principalmente para chaves simétricas\n");
                        resultado.append("   • Vulnerável a ataques de timing\n");
                        resultado.append("   • Requer padding seguro (PKCS#1, OAEP)\n\n");

                        resultado.append("🚀 ALTERNATIVAS MODERNAS:\n");
                        resultado.append("   • Criptografia híbrida (RSA + AES)\n");
                        resultado.append("   • Curvas elípticas (ECC)\n");
                        resultado.append("   • Criptografia pós-quântica\n");
                        resultado.append("   • Lattices e reticulados\n\n");

                        // Calcular tempo de execução
                        long endTime = System.currentTimeMillis();
                        long tempoExecucao = endTime - startTime;
                        resultado.append("⏱️ TEMPO DE EXECUÇÃO: ").append(String.format("%.3f", tempoExecucao / 1000.0)).append(" s\n");
                        resultado.append("⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                        resultado.append("💡 Demonstração educacional para compreensão da criptografia RSA\n");

                        pararCronometroOperacao(cronometroAtivo);

                        // Usar o método padronizado salvarResultadoTemporario
                        salvarResultadoTemporario(resultado.toString(), "demonstracao_criptografia");

                    } catch (Exception e) {
                        pararCronometroOperacao(cronometroAtivo);
                        Log.e(TAG, "Erro na demonstração de criptografia", e);
                        runOnUiThread(() -> {
                            resultadoView.setText("Erro na demonstração: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (Exception e) {
                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnDemonstrarCriptografia);

        // Botão para hash criptográfico
        Button btnHashCriptografico = new Button(this);
        btnHashCriptografico.setText("🔍 Demonstrar Hash Criptográfico");
        btnHashCriptografico.setBackgroundColor(0xFFFF9800); // Laranja
        btnHashCriptografico.setTextColor(0xFFFFFFFF);
        btnHashCriptografico.setOnClickListener(v -> {
            try {
                String mensagem = inputMensagem.getText().toString();
                if (mensagem.isEmpty()) {
                    Toast.makeText(this, "Digite uma mensagem para gerar hash", Toast.LENGTH_SHORT).show();
                    return;
                }

                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Hashes Criptográficos",
                    "Caracteres: " + mensagem.length()
                );
                // Iniciando geração de hashes criptográficos (sem mostrar na tela do app)

                new Thread(() -> {
                    try {
                        // Iniciar cronômetro
                        long startTime = System.currentTimeMillis();

                        StringBuilder resultado = new StringBuilder();
                        resultado.append("🔍 HASHES CRIPTOGRÁFICOS - DEMONSTRAÇÃO EDUCACIONAL\n");
                        resultado.append("==================================================\n\n");

                        resultado.append("📝 MENSAGEM ORIGINAL:\n");
                        resultado.append("   ").append(mensagem).append("\n\n");
                        byte[] utf8Msg = mensagem.getBytes(StandardCharsets.UTF_8);
                        resultado.append("📏 ENTRADA REAL PARA OS HASHES (UTF-8):\n");
                        resultado.append("   • Caracteres (String.length): ").append(mensagem.length()).append("\n");
                        resultado.append("   • Bytes UTF-8: ").append(utf8Msg.length).append("\n");
                        resultado.append("   • Bits processados (8 × bytes): ").append(utf8Msg.length * 8).append("\n\n");

                        resultado.append("🔢 HASHES GERADOS (sobre os bytes UTF-8 acima):\n");
                        resultado.append("   • MD5 (128 bits): ").append(gerarMD5(mensagem)).append("\n");
                        resultado.append("   • SHA-1 (160 bits): ").append(gerarSHA1(mensagem)).append("\n");
                        resultado.append("   • SHA-256 (256 bits): ").append(gerarSHA256(mensagem)).append("\n");
                        resultado.append("   • SHA-512 (512 bits): ").append(gerarSHA512(mensagem)).append("\n\n");

                        resultado.append("📚 O QUE SÃO HASHES:\n");
                        resultado.append("   • Funções matemáticas unidirecionais\n");
                        resultado.append("   • Convertem qualquer entrada em saída de tamanho fixo\n");
                        resultado.append("   • Impossível reverter hash para mensagem original\n");
                        resultado.append("   • Pequena mudança na entrada = hash completamente diferente\n\n");

                        resultado.append("🎯 PROPRIEDADES DOS HASHES:\n");
                        resultado.append("   • Determinístico: mesma entrada = mesmo hash\n");
                        resultado.append("   • Resistente a colisões: difícil encontrar duas entradas com mesmo hash\n");
                        resultado.append("   • Resistente a pré-imagem: difícil encontrar entrada para um hash\n");
                        resultado.append("   • Efeito avalanche: pequena mudança = hash muito diferente\n\n");

                        resultado.append("🚀 APLICAÇÕES PRÁTICAS:\n");
                        resultado.append("   • Verificação de integridade de arquivos\n");
                        resultado.append("   • Armazenamento seguro de senhas\n");
                        resultado.append("   • Assinatura digital de documentos\n");
                        resultado.append("   • Blockchain e criptomoedas\n");
                        resultado.append("   • VPNs e comunicações seguras\n\n");

                        resultado.append("⚡ COMPARAÇÃO DE ALGORITMOS:\n");
                        resultado.append("   • MD5: 128 bits - DESCONTINUADO (colisões encontradas)\n");
                        resultado.append("   • SHA-1: 160 bits - DESCONTINUADO (ataques práticos)\n");
                        resultado.append("   • SHA-256: 256 bits - RECOMENDADO (padrão atual)\n");
                        resultado.append("   • SHA-512: 512 bits - ALTO NÍVEL (segurança máxima)\n\n");

                        resultado.append("🔬 DEMONSTRAÇÃO DE AVALANCHE:\n");
                        String mensagemModificada = mensagem + "!";
                        resultado.append("   • Mensagem original: ").append(mensagem).append("\n");
                        resultado.append("   • Hash original: ").append(gerarSHA256(mensagem)).append("\n");
                        resultado.append("   • Mensagem modificada: ").append(mensagemModificada).append("\n");
                        resultado.append("   • Hash modificado: ").append(gerarSHA256(mensagemModificada)).append("\n");
                        resultado.append("   • Diferença nos caracteres hexadecimais: ").append(calcularDiferencaHash(gerarSHA256(mensagem), gerarSHA256(mensagemModificada))).append("%\n\n");

                        // Calcular tempo de execução
                        long endTime = System.currentTimeMillis();
                        long tempoExecucao = endTime - startTime;
                        resultado.append("⏱️ TEMPO DE EXECUÇÃO: ").append(String.format("%.3f", tempoExecucao / 1000.0)).append(" s\n");
                        resultado.append("⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                        resultado.append("💡 Os hashes abaixo são calculados sobre a mensagem real (UTF-8).\n");

                        resultado.append("🔍 VERIFICAÇÃO DE DETERMINISMO (mesma entrada → mesmo digest):\n");
                        resultado.append("   • MD5 (1ª/2ª/3ª): ").append(gerarMD5(mensagem)).append(" | ").append(gerarMD5(mensagem)).append(" | ").append(gerarMD5(mensagem)).append("\n");
                        resultado.append("   • SHA-256 (1ª/2ª/3ª): ").append(gerarSHA256(mensagem)).append(" | ").append(gerarSHA256(mensagem)).append(" | ").append(gerarSHA256(mensagem)).append("\n");
                        resultado.append("   • ✅ Determinísticos para a mesma string de entrada\n\n");

                        pararCronometroOperacao(cronometroAtivo);
                        salvarResultadoTemporario(resultado.toString(), "hashes_criptograficos");
                    } catch (Exception e) {
                        pararCronometroOperacao(cronometroAtivo);
                        Log.e(TAG, "Erro na geração de hashes", e);
                        runOnUiThread(() -> {
                            resultadoView.setText("Erro na geração de hashes: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (Exception e) {
                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnHashCriptografico);

        // Botão para assinatura digital
        Button btnAssinaturaDigital = new Button(this);
        btnAssinaturaDigital.setText("✍️ Demonstrar Assinatura Digital");
        btnAssinaturaDigital.setBackgroundColor(0xFFE91E63); // Rosa
        btnAssinaturaDigital.setTextColor(0xFFFFFFFF);
        btnAssinaturaDigital.setOnClickListener(v -> {
            try {
                String mensagem = inputMensagem.getText().toString();
                if (mensagem.isEmpty()) {
                    Toast.makeText(this, "Digite uma mensagem para assinar", Toast.LENGTH_SHORT).show();
                    return;
                }

                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Assinatura Digital",
                    "Caracteres: " + mensagem.length()
                );
                // Iniciando assinatura digital (sem mostrar na tela do app)

                new Thread(() -> {
                    try {
                        // Iniciar cronômetro
                        long startTime = System.currentTimeMillis();

                        StringBuilder resultado = new StringBuilder();
                        resultado.append("✍️ ASSINATURA DIGITAL - DEMONSTRAÇÃO REAL\n");
                        resultado.append("=========================================\n\n");

                        resultado.append("📝 MENSAGEM PARA ASSINAR:\n");
                        resultado.append("   ").append(mensagem).append("\n\n");

                        // PASSO 1: Gerar chaves RSA para assinatura (tamanho = campo "bits" acima)
                        int bitsAss = 1024;
                        try {
                            bitsAss = Integer.parseInt(inputBitsRSA.getText().toString().trim());
                        } catch (NumberFormatException ignored) {
                            bitsAss = 1024;
                        }
                        if (bitsAss < 512) {
                            bitsAss = 512;
                        }
                        if (bitsAss > 33000) {
                            bitsAss = 33000;
                        }
                        resultado.append("🔑 PASSO 1: GERANDO CHAVES RSA PARA ASSINATURA (").append(bitsAss).append(" bits)\n");
                        BigInteger[] pqAss = gerarPrimosPqRSA(bitsAss);
                        BigInteger p = pqAss[0];
                        BigInteger q = pqAss[1];
                        BigInteger n = p.multiply(q);
                        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
                        BigInteger e = BigInteger.valueOf(65537);
                        BigInteger d = e.modInverse(phi);

                        resultado.append("   • Chave pública: (n=").append(n).append(", e=").append(e).append(")\n");
                        resultado.append("   • Chave privada: (n=").append(n).append(", d=").append(d).append(")\n\n");

                        // PASSO 2: Calcular hash da mensagem
                        resultado.append("🔍 PASSO 2: CALCULANDO HASH DA MENSAGEM\n");
                        String hashMensagem = gerarSHA256(mensagem);
                        resultado.append("   • Hash SHA-256: ").append(hashMensagem).append("\n");
                        resultado.append("   • Tamanho do hash: ").append(hashMensagem.length()).append(" caracteres (256 bits)\n\n");

                        // PASSO 3: Assinar o hash (criptografar com chave privada)
                        resultado.append("✍️ PASSO 3: CRIANDO ASSINATURA DIGITAL\n");
                        BigInteger hashBigInt = new BigInteger(hashMensagem, 16); // Converter hex para BigInteger
                        BigInteger assinatura = hashBigInt.modPow(d, n); // Assinar = Criptografar com chave privada

                        resultado.append("   • Hash como número: ").append(hashBigInt).append("\n");
                        resultado.append("   • Assinatura digital: ").append(assinatura).append("\n");
                        resultado.append("   • Tamanho da assinatura: ").append(assinatura.toString().length()).append(" dígitos\n\n");

                        // PASSO 4: Verificar assinatura (descriptografar com chave pública)
                        resultado.append("✅ PASSO 4: VERIFICANDO ASSINATURA DIGITAL\n");
                        BigInteger hashVerificado = assinatura.modPow(e, n); // Verificar = Descriptografar com chave pública
                        String hashVerificadoHex = hashVerificado.toString(16);

                        resultado.append("   • Hash verificado: ").append(hashVerificadoHex).append("\n");
                        resultado.append("   • Hash original: ").append(hashMensagem).append("\n");
                        resultado.append("   • Assinatura válida: ").append(hashMensagem.equals(hashVerificadoHex) ? "✅ SIM" : "❌ NÃO").append("\n\n");

                        // DEMONSTRAÇÃO: Diferentes pessoas, mesma mensagem
                        resultado.append("👥 DEMONSTRAÇÃO: DIFERENTES PESSOAS, MESMA MENSAGEM\n");
                        resultado.append("   • João diz: \"").append(mensagem).append("\"\n");
                        resultado.append("   • Hash do João: ").append(gerarSHA256(mensagem)).append("\n");
                        resultado.append("   • Maria diz: \"").append(mensagem).append("\"\n");
                        resultado.append("   • Hash da Maria: ").append(gerarSHA256(mensagem)).append("\n");
                        resultado.append("   • Pedro diz: \"").append(mensagem).append("\"\n");
                        resultado.append("   • Hash do Pedro: ").append(gerarSHA256(mensagem)).append("\n");
                        resultado.append("   • ✅ Todos os hashes são IGUAIS (correto!)\n\n");

                        // DEMONSTRAÇÃO: Como a assinatura garante autenticidade
                        resultado.append("🔐 COMO A ASSINATURA GARANTE AUTENTICIDADE:\n");
                        resultado.append("   • Hash é público (qualquer um pode calcular)\n");
                        resultado.append("   • MAS só quem tem a chave privada pode ASSINAR o hash\n");
                        resultado.append("   • Verificação usa chave pública (todos podem verificar)\n");
                        resultado.append("   • Se alguém alterar a mensagem, o hash muda\n");
                        resultado.append("   • Se o hash muda, a assinatura fica inválida\n\n");

                        // DEMONSTRAÇÃO: Tentativa de falsificação
                        resultado.append("🚨 DEMONSTRAÇÃO: TENTATIVA DE FALSIFICAÇÃO\n");
                        String mensagemFalsa = mensagem + " ALTERADA";
                        String hashFalso = gerarSHA256(mensagemFalsa);
                        resultado.append("   • Mensagem alterada: \"").append(mensagemFalsa).append("\"\n");
                        resultado.append("   • Hash da mensagem alterada: ").append(hashFalso).append("\n");
                        resultado.append("   • Hash original: ").append(hashMensagem).append("\n");
                        resultado.append("   • Hashes são iguais: ").append(hashMensagem.equals(hashFalso) ? "SIM" : "❌ NÃO").append("\n");
                        resultado.append("   • Resultado: ").append(hashMensagem.equals(hashFalso) ? "Falsificação bem-sucedida" : "✅ Falsificação DETECTADA!").append("\n\n");

                        resultado.append("📚 CONCLUSÃO EDUCACIONAL:\n");
                        resultado.append("   • Hash SEMPRE igual para mesma mensagem = ✅ CORRETO\n");
                        resultado.append("   • Bits RSA NÃO afetam o hash = ✅ CORRETO\n");
                        resultado.append("   • Privacidade vem da CHAVE PRIVADA, não do hash\n");
                        resultado.append("   • Assinatura digital = Hash + Criptografia RSA\n");
                        resultado.append("   • Integridade + Autenticidade + Não-repúdio\n\n");

                        // Calcular tempo de execução
                        long endTime = System.currentTimeMillis();
                        long tempoExecucao = endTime - startTime;
                        resultado.append("⏱️ TEMPO DE EXECUÇÃO: ").append(String.format("%.3f", tempoExecucao / 1000.0)).append(" s\n");
                        resultado.append("⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                        resultado.append("💡 Demonstração educacional de assinatura digital\n");

                        pararCronometroOperacao(cronometroAtivo);

                        // Usar o método padronizado salvarResultadoTemporario
                        salvarResultadoTemporario(resultado.toString(), "assinatura_digital");

                    } catch (Exception e) {
                        pararCronometroOperacao(cronometroAtivo);
                        Log.e(TAG, "Erro na assinatura digital", e);
                        runOnUiThread(() -> {
                            resultadoView.setText("Erro na assinatura digital: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (Exception e) {
                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnAssinaturaDigital);

        // Status em tempo real (cronômetro, bits, etc.) — mesmo fluxo de rolagem, tela inteira
        if (resultadoView != null) {
            resultadoView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            container.addView(resultadoView);
        }
    }

    private void criarInterfaceFatoracao() {
        contentContainer.removeAllViews();


        // Título
        TextView titulo = new TextView(this);
        titulo.setText("Fatoração de Números");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        contentContainer.addView(titulo);

        adicionarRotuloCampo("Número para fatorar");
        EditText inputN = new EditText(this);
        inputN.setHint("Digite aqui (ex.: 100)");
        inputN.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        estilizarCampoEntrada(inputN);
        contentContainer.addView(inputN);

        // Botão fatorar
        Button btnFatorar = new Button(this);
        btnFatorar.setText("Fatorar Número");
        btnFatorar.setOnClickListener(v -> {
            try {
                int n = Integer.parseInt(inputN.getText().toString());
                if (n < 2) {
                    Toast.makeText(this, "Digite um número ≥ 2", Toast.LENGTH_SHORT).show();
                    return;
                }

                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Fatoração",
                    "Número: " + n
                );

                new Thread(() -> {
                    try {
                        StringBuilder resultado = new StringBuilder();
                        resultado.append("Fatoração de ").append(n).append("\n\n");

                        if (ehPrimo(n)) {
                            resultado.append(n).append(" é primo!\n");
                            resultado.append("Fatoração: ").append(n).append(" = ").append(n);
                        } else {
                            java.util.List<Integer> fatores = new java.util.ArrayList<>();
                            int numero = n;

                            // Fatorar por divisão ((long) evita overflow em i*i para n grande)
                            for (int i = 2; (long) i * i <= numero; i++) {
                                while (numero % i == 0) {
                                    fatores.add(i);
                                    numero = numero / i;
                                }
                            }

                            if (numero > 1) {
                                fatores.add(numero);
                            }

                            resultado.append("Fatores primos: ");
                            for (int i = 0; i < fatores.size(); i++) {
                                resultado.append(fatores.get(i));
                                if (i < fatores.size() - 1) resultado.append(" × ");
                            }

                            resultado.append("\n\nFatoração única: ").append(n).append(" = ");
                            java.util.Map<Integer, Integer> expoentes = new java.util.HashMap<>();
                            for (int fator : fatores) {
                                expoentes.put(fator, expoentes.getOrDefault(fator, 0) + 1);
                            }

                            boolean primeiro = true;
                            for (java.util.Map.Entry<Integer, Integer> entry : expoentes.entrySet()) {
                                if (!primeiro) resultado.append(" × ");
                                resultado.append(entry.getKey());
                                if (entry.getValue() > 1) {
                                    resultado.append("^").append(entry.getValue());
                                }
                                primeiro = false;
                            }
                        }

                        runOnUiThread(() -> {
                            pararCronometroOperacao(cronometroAtivo);
                            resultadoView.setText(resultado.toString());
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            pararCronometroOperacao(cronometroAtivo);
                            resultadoView.setText("Erro: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnFatorar);

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    private void criarInterfaceAnalisePrimalidade() {
        contentContainer.removeAllViews();


        // Título
        TextView titulo = new TextView(this);
        titulo.setText("Análise de Primalidade");
        titulo.setTextSize(18);
        titulo.setPadding(0, 16, 0, 16);
        contentContainer.addView(titulo);

        adicionarRotuloCampo("Número para analisar");
        EditText inputN = new EditText(this);
        inputN.setHint("Digite aqui (ex.: 97)");
        inputN.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        estilizarCampoEntrada(inputN);
        contentContainer.addView(inputN);

        // Botão analisar
        Button btnAnalisar = new Button(this);
        btnAnalisar.setText("Analisar Primalidade");
        btnAnalisar.setOnClickListener(v -> {
            try {
                int n = Integer.parseInt(inputN.getText().toString());
                if (n < 1) {
                    Toast.makeText(this, "Digite um número ≥ 1", Toast.LENGTH_SHORT).show();
                    return;
                }

                CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Análise de Primalidade",
                    "Número: " + n
                );

                new Thread(() -> {
                    try {
                        StringBuilder resultado = new StringBuilder();
                        resultado.append("Análise de Primalidade: ").append(n).append("\n\n");

                        if (n == 1) {
                            resultado.append("1 não é considerado primo nem composto.");
                        } else if (ehPrimo(n)) {
                            resultado.append("✓ ").append(n).append(" é PRIMO!\n\n");

                            // Análise adicional
                            resultado.append("Propriedades:\n");
                            if (n == 2) {
                                resultado.append("• Único primo par\n");
                            } else {
                                resultado.append("• Primo ímpar\n");
                            }

                            // Verificar se é primo especial
                            if (ehPrimoMersenne(n)) {
                                resultado.append("• Primo de Mersenne\n");
                            }
                            if (ehPrimoSophieGermain(n)) {
                                resultado.append("• Primo de Sophie Germain\n");
                            }
                            if (ehPrimoGemeo(n)) {
                                resultado.append("• Parte de um par de primos gêmeos\n");
                            }
                        } else {
                            resultado.append("✗ ").append(n).append(" é COMPOSTO\n\n");

                            // Encontrar um divisor
                            int divisor = encontrarDivisor(n);
                            resultado.append("Divisor encontrado: ").append(divisor).append("\n");
                            resultado.append("Fatoração: ").append(n).append(" = ").append(divisor).append(" × ").append(n / divisor);
                        }

                        runOnUiThread(() -> {
                            pararCronometroOperacao(cronometroAtivo);
                            resultadoView.setText(resultado.toString());
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            pararCronometroOperacao(cronometroAtivo);
                            resultadoView.setText("Erro: " + e.getMessage());
                        });
                    }
                }).start();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnAnalisar);

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    private void criarInterfaceTestePrimalidade() {
        try {
            // Limpar interface anterior
            contentContainer.removeAllViews();


            // Título principal
            TextView titulo = new TextView(this);
            titulo.setText("🔍 Teste de Primalidade - Análise Completa");
            titulo.setTextSize(20);
            titulo.setTextColor(0xFF2E7D32);
            titulo.setPadding(0, 20, 0, 20);
            titulo.setGravity(android.view.Gravity.CENTER);
            titulo.setTypeface(null, android.graphics.Typeface.BOLD);
            contentContainer.addView(titulo);

            // Descrição
            TextView descricao = new TextView(this);
            descricao.setText("Análise completa de números: primalidade, fatoração e estatísticas.\n" +
                             "Pode digitar um inteiro ou uma expressão (ex.: 2^31-1, (10+5)*2, 10^100+3).\n" +
                             "Números muito grandes: até 10.000 dígitos no resultado.");
            descricao.setTextSize(14);
            descricao.setTextColor(0xFF666666);
            descricao.setPadding(0, 0, 0, 30);
            descricao.setGravity(android.view.Gravity.CENTER);
            contentContainer.addView(descricao);

            // ===== SEÇÃO 1: ANÁLISE INDIVIDUAL =====
            TextView secao1 = new TextView(this);
            secao1.setText("📊 ANÁLISE INDIVIDUAL");
            secao1.setTextSize(16);
            secao1.setTextColor(0xFF1976D2);
            secao1.setTypeface(null, android.graphics.Typeface.BOLD);
            secao1.setPadding(0, 0, 0, 10);
            contentContainer.addView(secao1);

            TextView labNum = new TextView(this);
            labNum.setText("Número ou expressão a analisar");
            labNum.setTypeface(null, Typeface.BOLD);
            labNum.setPadding(dpUi(4), dpUi(8), dpUi(4), dpUi(4));
            contentContainer.addView(labNum);
            EditText inputNumero = new EditText(this);
            inputNumero.setHint("Inteiro ou expressão: 97 | 2^31-1 | 10^50+1 | (2+3)*11");
            inputNumero.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            inputNumero.setMaxLines(3);
            estilizarCampoEntrada(inputNumero);
            contentContainer.addView(inputNumero);

            TextView statusPrimalidadeView = new TextView(this);
            statusPrimalidadeView.setText("Pronto para iniciar o teste.");
            statusPrimalidadeView.setTextSize(14);
            statusPrimalidadeView.setPadding(16, 16, 16, 16);
            statusPrimalidadeView.setBackgroundColor(0xFFF5F5F5);
            statusPrimalidadeView.setTextColor(0xFF333333);
            statusPrimalidadeView.setVisibility(View.GONE);

            // Botão para análise individual
            Button btnAnalisarNumero = new Button(this);
            btnAnalisarNumero.setText("🔍 Analisar Número Individual");
            btnAnalisarNumero.setBackgroundColor(0xFF2196F3);
            btnAnalisarNumero.setTextColor(0xFFFFFFFF);
            btnAnalisarNumero.setPadding(16, 12, 16, 12);
            btnAnalisarNumero.setOnClickListener(v -> {
                try {
                    String inputText = inputNumero.getText().toString();
                    if (inputText.isEmpty()) {
                        Toast.makeText(this, "Digite um número para analisar", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String entradaOriginal = inputText.trim();
                    // Mesmo fluxo numérico: literal ou expressão (+, -, *, /, %, ^, parênteses)
                    BigInteger n = parseBigIntegerOuExpressao(entradaOriginal);
                    if (n.signum() < 0) {
                        Toast.makeText(this, "Digite um inteiro não negativo.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Verificar limite de dígitos
                    if (n.toString().length() > 10000) {
                        Toast.makeText(this, "Número muito grande! Máximo: 10.000 dígitos", Toast.LENGTH_LONG).show();
                        return;
                    }

                    final int digitosNumero = n.toString().length();
                    final int bitsNumero = n.bitLength();
                    final AtomicBoolean analiseAtiva = new AtomicBoolean(true);
                    final long inicioAnalise = System.currentTimeMillis();
                    final String[] detalhesAnalise = new String[] {
                        "Dígitos: " + digitosNumero,
                        "Bits: " + bitsNumero
                    };

                    statusPrimalidadeView.setVisibility(View.VISIBLE);
                    mostrarStatusOperacaoEmAndamento(statusPrimalidadeView, "Teste de Primalidade", 0L, detalhesAnalise);

                    Thread cronometroThread = new Thread(() -> {
                        while (analiseAtiva.get()) {
                            long decorridoMs = System.currentTimeMillis() - inicioAnalise;
                            runOnUiThread(() -> mostrarStatusOperacaoEmAndamento(
                                statusPrimalidadeView,
                                "Teste de Primalidade",
                                decorridoMs,
                                detalhesAnalise
                            ));

                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    });
                    cronometroThread.start();

                    // Executar análise em thread separada
                    new Thread(() -> {
                        try {
                            long startTime = System.currentTimeMillis();
                            StringBuilder resultado = new StringBuilder();

                            resultado.append("🔍 TESTE DE PRIMALIDADE\n");
                            resultado.append("=======================\n\n");
                            resultado.append("📝 ENTRADA: ").append(entradaOriginal).append("\n");
                            resultado.append("📌 NÚMERO ANALISADO: ").append(n).append("\n");
                            resultado.append("📏 DÍGITOS: ").append(digitosNumero).append("\n");
                            resultado.append("🔢 BITS: ").append(bitsNumero).append("\n\n");

                            // Resultado principal: apenas primalidade
                            resultado.append("✅ RESULTADO DO TESTE\n");

                            if (n.compareTo(BigInteger.TWO) < 0) {
                                resultado.append("   • Status: ").append(n)
                                    .append(" não é considerado primo nem composto\n\n");
                            } else if (bitsNumero <= 64 ? ehPrimo64Nativo(n.longValue())
                                    : testarPrimalidadeGiganteNativo(n.toString(), 25)) {
                                resultado.append(bitsNumero <= 64
                                    ? "   • Status: ✅ É PRIMO (teste determinístico)\n\n"
                                    : "   • Status: PROVAVELMENTE PRIMO\n\n");
                            } else {
                                resultado.append("   • Status: ❌ É COMPOSTO\n\n");
                                resultado.append("🔍 FATORAÇÃO\n");
                                String fatores = factorReportNativo(n.toString());
                                resultado.append(fatores == null || fatores.isEmpty()
                                    ? "Não foi possível preparar a fatoração."
                                    : fatores).append("\n\n");
                            }

                            // Calcular tempo de execução
                            long endTime = System.currentTimeMillis();
                            long tempoExecucao = endTime - startTime;
                            resultado.append("⏱️ TEMPO DE EXECUÇÃO: ").append(String.format("%.3f", tempoExecucao / 1000.0)).append(" s\n");
                            resultado.append("⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                            resultado.append("💡 Se quiser informações estatísticas mais detalhadas, use a função `Estatísticas`.\n");

                            analiseAtiva.set(false);
                            runOnUiThread(() -> mostrarStatusOperacaoConcluida(
                                statusPrimalidadeView,
                                "Teste de Primalidade",
                                "Dígitos: " + digitosNumero,
                                "Bits: " + bitsNumero,
                                "Tempo total: " + String.format(Locale.getDefault(), "%.3f", tempoExecucao / 1000.0) + " s"
                            ));

                            // Salvar resultado e abrir visualizador
                            salvarResultadoTemporario(resultado.toString(), "teste_primalidade");

                        } catch (Exception e) {
                            analiseAtiva.set(false);
                            Log.e(TAG, "Erro na análise de primalidade", e);
                            runOnUiThread(() -> {
                                mostrarStatusOperacaoErro(
                                    statusPrimalidadeView,
                                    "Teste de Primalidade",
                                    "Dígitos: " + digitosNumero,
                                    "Bits: " + bitsNumero,
                                    "Mensagem: " + e.getMessage()
                                );
                                Toast.makeText(this, "Erro na análise: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Digite um número válido", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar análise", e);
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            contentContainer.addView(btnAnalisarNumero);
            contentContainer.addView(statusPrimalidadeView);

            // Espaçador
            TextView espacador1 = new TextView(this);
            espacador1.setText("");
            espacador1.setPadding(0, 30, 0, 30);
            contentContainer.addView(espacador1);


            // Mostrar interface
            mostrarTelaConteudo();
            if (menuScrollView != null) {
                menuScrollView.setVisibility(View.GONE);
            } else {
                menuContainer.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar interface de teste de primalidade", e);
            Toast.makeText(this, "Erro ao criar interface: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    // Métodos auxiliares
    /**
     * Verifica se um número é primo usando algoritmo otimizado
     * Versão otimizada para números grandes (como no primeiro card)
     */
    private boolean ehPrimoOtimizado(long n) {
        if (n < 2) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        // Verificar divisores até √n
        for (long i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula a integral logarítmica Li(x) = ∫₂ˣ (1/ln(t)) dt para números gigantes
     * Versão otimizada para BigInteger (suporta milhares de dígitos)
     * Usa regra de Simpson adaptativa para alta precisão
     */
    private double calcularIntegralLogaritmicaBigInteger(BigInteger x) {
        if (x.compareTo(BigInteger.valueOf(2)) < 0) return 0.0;

        // Para números muito grandes, usar BigDecimal para evitar overflow
        if (x.bitLength() > 50) { // Para números com mais de 50 bits
            return calcularIntegralLogaritmicaBigDecimal(x);
        }

        // Para números menores, usar método original otimizado
        int n;
        long xLong = x.longValue();

        if (xLong <= 1000) {
            n = 50000;  // 50k pontos para números pequenos
        } else if (xLong <= 100000) {
            n = 100000; // 100k pontos para números médios
        } else if (xLong <= 1000000) {
            n = 500000; // 500k pontos para números grandes
        } else if (xLong <= 1000000000L) {
            n = 1000000; // 1M pontos para números muito grandes
        } else {
            n = 2000000; // 2M pontos para números gigantes
        }

        double a = 2.0;  // Limite inferior
        double b = x.doubleValue();  // Limite superior
        double h = (b - a) / n;  // Tamanho do passo

        // Usar regra de Simpson para maior precisão
        double soma = 0.0;

        // Primeiro ponto
        soma += 1.0 / Math.log(a);

        // Pontos intermediários com pesos alternados (Simpson)
        for (int i = 1; i < n; i++) {
            double t = a + i * h;
            double peso = (i % 2 == 0) ? 2.0 : 4.0;  // Simpson: 1,4,2,4,2,4,...,1
            soma += peso * (1.0 / Math.log(t));
        }

        // Último ponto
        soma += 1.0 / Math.log(b);

        return (h / 3.0) * soma;  // Fator 1/3 da regra de Simpson
    }

    /**
     * Calcula a integral logarítmica Li(x) = ∫₂ˣ (1/ln(t)) dt para números gigantes
     * Usa BigDecimal e MathContext para evitar overflow e NaN
     */
    private double calcularIntegralLogaritmicaBigDecimal(BigInteger x) {
        if (x.compareTo(BigInteger.valueOf(2)) < 0) return 0.0;

        // Configuração de precisão para cálculos
        MathContext mc = new MathContext(50, RoundingMode.HALF_UP);

            // Converter para BigDecimal
            BigDecimal xBig = new BigDecimal(x, mc);
        BigDecimal dois = new BigDecimal("2", mc);

        // Para números muito grandes, usar aproximação assintótica
        if (x.bitLength() > 100) {
                // Li(x) ≈ x/ln(x) + x/(ln(x))² + 2x/(ln(x))³ + ...
            // Para números gigantes, usar apenas os primeiros termos
            BigDecimal logX = logBigDecimal(xBig, mc);
                BigDecimal logX2 = logX.multiply(logX, mc);
            BigDecimal logX3 = logX2.multiply(logX, mc);

                // Li(x) ≈ x/ln(x) + x/(ln(x))²
                BigDecimal termo1 = xBig.divide(logX, mc);
                BigDecimal termo2 = xBig.divide(logX2, mc);

                BigDecimal resultado = termo1.add(termo2, mc);

            // Verificar se o resultado é válido
            if (resultado.compareTo(BigDecimal.ZERO) <= 0 ||
                resultado.toString().contains("Infinity") ||
                resultado.toString().contains("NaN")) {
                // Fallback para aproximação simples
                return xBig.divide(logX, mc).doubleValue();
            }

                return resultado.doubleValue();
            }

        // Para números grandes mas não gigantes, usar integração numérica
        int n = Math.min(100000, Math.max(1000, x.bitLength() * 100));

            BigDecimal a = dois;
            BigDecimal b = xBig;
            BigDecimal h = b.subtract(a, mc).divide(new BigDecimal(n), mc);

            BigDecimal soma = BigDecimal.ZERO;

            // Primeiro ponto
        BigDecimal logA = logBigDecimal(a, mc);
            soma = soma.add(BigDecimal.ONE.divide(logA, mc), mc);

            // Pontos intermediários com regra de Simpson
            for (int i = 1; i < n; i++) {
                BigDecimal t = a.add(h.multiply(new BigDecimal(i), mc), mc);
            BigDecimal logT = logBigDecimal(t, mc);
                BigDecimal peso = (i % 2 == 0) ? new BigDecimal("2") : new BigDecimal("4");

                BigDecimal termo = BigDecimal.ONE.divide(logT, mc).multiply(peso, mc);
                soma = soma.add(termo, mc);
            }

            // Último ponto
        BigDecimal logB = logBigDecimal(b, mc);
            soma = soma.add(BigDecimal.ONE.divide(logB, mc), mc);

            // Aplicar fator 1/3 da regra de Simpson
            BigDecimal resultado = h.multiply(soma, mc).divide(new BigDecimal("3"), mc);

        // Verificar se o resultado é válido
        if (resultado.compareTo(BigDecimal.ZERO) <= 0 ||
            resultado.toString().contains("Infinity") ||
            resultado.toString().contains("NaN")) {
            // Fallback para aproximação simples
            BigDecimal logX = logBigDecimal(xBig, mc);
            return xBig.divide(logX, mc).doubleValue();
        }

        return resultado.doubleValue();
    }

    /**
     * Calcula o logaritmo natural usando BigDecimal
     * Implementação usando série de Taylor para ln(x)
     */
    private BigDecimal logBigDecimal(BigDecimal x, MathContext mc) {
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Logaritmo de número não positivo");
        }

        // Para x > 2, usar ln(x) = ln(2) + ln(x/2)
        if (x.compareTo(new BigDecimal("2")) > 0) {
            BigDecimal dois = new BigDecimal("2", mc);
            BigDecimal log2 = logBigDecimal(dois, mc);
            BigDecimal xDiv2 = x.divide(dois, mc);
            return log2.add(logBigDecimal(xDiv2, mc), mc);
        }

        // Para x < 1, usar ln(x) = -ln(1/x)
        if (x.compareTo(BigDecimal.ONE) < 0) {
            BigDecimal um = BigDecimal.ONE;
            BigDecimal umDivX = um.divide(x, mc);
            return logBigDecimal(umDivX, mc).negate(mc);
        }

        // Para 1 <= x <= 2, usar série de Taylor
        // ln(x) = 2 * ((x-1)/(x+1) + (x-1)³/(3(x+1)³) + (x-1)⁵/(5(x+1)⁵) + ...)
        BigDecimal um = BigDecimal.ONE;
        BigDecimal xMenos1 = x.subtract(um, mc);
        BigDecimal xMais1 = x.add(um, mc);
        BigDecimal razao = xMenos1.divide(xMais1, mc);

        BigDecimal resultado = BigDecimal.ZERO;
        BigDecimal razaoElevada = razao;

        for (int n = 1; n <= 100; n += 2) { // Apenas potências ímpares
            BigDecimal termo = razaoElevada.divide(new BigDecimal(n), mc);
            resultado = resultado.add(termo, mc);
            razaoElevada = razaoElevada.multiply(razao, mc).multiply(razao, mc);

            // Verificar convergência
            if (termo.abs(mc).compareTo(new BigDecimal("1E-50")) < 0) {
                break;
            }
        }

        return resultado.multiply(new BigDecimal("2"), mc);
    }

    /**
     * Calcula a integral logarítmica Li(x) = ∫₂ˣ (1/ln(t)) dt para números grandes
     * Versão otimizada para Long (suporta até 15 dígitos)
     * Usa regra de Simpson adaptativa para alta precisão
     */
    private double calcularIntegralLogaritmicaLong(long x) {
        if (x < 2) return 0.0;

        // Para números muito grandes, usar método adaptativo
        int n;
        if (x <= 1000) {
            n = 50000;  // 50k pontos para números pequenos
        } else if (x <= 100000) {
            n = 100000; // 100k pontos para números médios
        } else if (x <= 1000000) {
            n = 500000; // 500k pontos para números grandes
        } else if (x <= 1000000000L) {
            n = 1000000; // 1M pontos para números muito grandes
        } else {
            n = 2000000; // 2M pontos para números gigantes (15 dígitos)
        }

        double a = 2.0;  // Limite inferior
        double b = (double) x;  // Limite superior
        double h = (b - a) / n;  // Tamanho do passo

        // Usar regra de Simpson para maior precisão
        double soma = 0.0;

        // Primeiro ponto
        soma += 1.0 / Math.log(a);

        // Pontos intermediários com pesos alternados (Simpson)
        for (int i = 1; i < n; i++) {
            double t = a + i * h;
            double peso = (i % 2 == 0) ? 2.0 : 4.0;  // Simpson: 1,4,2,4,2,4,...,1
            soma += peso * (1.0 / Math.log(t));
        }

        // Último ponto
        soma += 1.0 / Math.log(b);

        return (h / 3.0) * soma;  // Fator 1/3 da regra de Simpson
    }

    /**
     * Calcula a integral logarítmica Li(x) = ∫₂ˣ (1/ln(t)) dt
     * Usa regra de Simpson adaptativa para alta precisão
     * Esta é uma das melhores aproximações para π(x)
     */
    private double calcularIntegralLogaritmica(int x) {
        if (x < 2) return 0.0;

        // Para números grandes, usar mais pontos e método adaptativo
        int n;
        if (x <= 1000) {
            n = 50000;  // 50k pontos para números pequenos
        } else if (x <= 100000) {
            n = 100000; // 100k pontos para números médios
        } else {
            n = 500000; // 500k pontos para números grandes (1M+)
        }

        double a = 2.0;  // Limite inferior
        double b = (double) x;  // Limite superior
        double h = (b - a) / n;  // Tamanho do passo

        // Usar regra de Simpson para maior precisão
        double soma = 0.0;

        // Primeiro ponto
        soma += 1.0 / Math.log(a);

        // Pontos intermediários com pesos alternados (Simpson)
        for (int i = 1; i < n; i++) {
            double t = a + i * h;
            double peso = (i % 2 == 0) ? 2.0 : 4.0;  // Simpson: 1,4,2,4,2,4,...,1
            soma += peso * (1.0 / Math.log(t));
        }

        // Último ponto
        soma += 1.0 / Math.log(b);

        return (h / 3.0) * soma;  // Fator 1/3 da regra de Simpson
    }

    /**
     * Método de debug para validar a precisão da integral logarítmica
     * Compara com valores conhecidos para verificar a implementação
     */
    private void validarIntegralLogaritmica() {
        StringBuilder debug = new StringBuilder();
        debug.append("🔍 VALIDAÇÃO DA INTEGRAL LOGARÍTMICA\n");
        debug.append("====================================\n\n");

        // Valores conhecidos para validação
        int[] valoresTeste = {100, 1000, 10000, 100000, 1000000};
        int[] valoresReais = {25, 168, 1229, 9592, 78498};

        for (int i = 0; i < valoresTeste.length; i++) {
            int x = valoresTeste[i];
            int real = valoresReais[i];
            double li = calcularIntegralLogaritmica(x);
            double aproximacao = x / Math.log(x);

            double erroLi = Math.abs(real - li);
            double erroAprox = Math.abs(real - aproximacao);
            double melhoria = ((erroAprox - erroLi) / erroAprox) * 100;

            debug.append("📊 x = ").append(String.format("%,d", x)).append(":\n");
            debug.append("   • Real: ").append(String.format("%,d", real)).append("\n");
            debug.append("   • Li(x): ").append(String.format("%.0f", li)).append("\n");
            debug.append("   • x/ln(x): ").append(String.format("%.0f", aproximacao)).append("\n");
            debug.append("   • Erro Li(x): ").append(String.format("%.0f", erroLi)).append(" (").append(String.format("%.2f%%", (erroLi/real)*100)).append(")\n");
            debug.append("   • Erro x/ln(x): ").append(String.format("%.0f", erroAprox)).append(" (").append(String.format("%.2f%%", (erroAprox/real)*100)).append(")\n");
            debug.append("   • Melhoria Li(x): ").append(String.format("%.1f", melhoria)).append("%\n\n");
        }

        // Salvar resultado de debug
        salvarResultadoTemporario(debug.toString(), "validacao_integral_logaritmica");
    }

    /** Delega para {@link #ehPrimo(long)} — não duplicar laços com {@code int} ({@code i*i} pode estourar). */
    private boolean ehPrimo(int n) {
        return ehPrimo((long) n);
    }

    private boolean ehPrimoMersenne(int n) {
        int m = n + 1;
        if (m <= 1 || (m & (m - 1)) != 0) return false;

        int p = (int)Math.round(Math.log(m) / Math.log(2));
        return ehPrimo(p);
    }

    private boolean ehPrimoSophieGermain(int n) {
        return ehPrimo(2 * n + 1);
    }

    private boolean ehPrimoGemeo(int n) {
        return ehPrimo(n + 2) || (n > 2 && ehPrimo(n - 2));
    }

    private int encontrarDivisor(int n) {
        for (int i = 2; (long) i * i <= n; i++) {
            if (n % i == 0) return i;
        }
        return n;
    }

    private boolean deveUsarRotaNativaPrimos(int bits) {
        return bits > LIMIAR_BITS_ROTA_NATIVA;
    }

    /** Em cache/temp_primos (compatível com FileProvider em file_paths.xml). */
    private File criarArquivoTempPrimosGigantes(String prefixo) throws IOException {
        File dir = new File(getCacheDir(), "temp_primos");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Não foi possível criar o diretório temp_primos");
        }
        return File.createTempFile(prefixo, ".txt", dir);
    }

    private synchronized String executarJobNativoPrimosGigantes(int bits, int quantidade, int threads, java.util.function.Consumer<String> onStatus, File arquivoSaidaPrimos, boolean modoAleatorio) {
        if (arquivoSaidaPrimos == null) {
            throw new IllegalArgumentException("arquivoSaidaPrimos");
        }
        if (modoAleatorio) {
            if (!iniciarPrimosAleatoriosJob(bits, quantidade, arquivoSaidaPrimos.getAbsolutePath())) {
                throw new IllegalStateException("Não foi possível iniciar a geração.");
            }
        } else {
            iniciarGeracaoPrimosGigantesJob(bits, quantidade, threads, arquivoSaidaPrimos.getAbsolutePath());
        }
        while (!geracaoPrimosGigantesJobConcluido()) {
            if (cancelarOperacaoAtual.get()) {
                cancelarGeracaoPrimosGigantesJob();
                if (modoAleatorio) return obterResultadoGeracaoPrimosGigantesJob();
                throw new RuntimeException("Busca nativa cancelada pelo usuário");
            }
            if (onStatus != null) {
                onStatus.accept(obterStatusGeracaoPrimosGigantesJob());
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                cancelarGeracaoPrimosGigantesJob();
                Thread.currentThread().interrupt();
                throw new RuntimeException("Busca nativa interrompida", e);
            }
        }
        return obterResultadoGeracaoPrimosGigantesJob();
    }

    private static String lerTextoUtf8(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private List<BigInteger> lerPrimosDecimaisDoArquivo(File arquivo, int quantidadeEsperada) throws IOException {
        List<BigInteger> primos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null && primos.size() < quantidadeEsperada) {
                String valor = linha == null ? "" : linha.trim();
                if (ehLinhaNumeroDecimal(valor)) {
                    primos.add(new BigInteger(valor));
                }
            }
        }
        return primos;
    }

    private List<BigInteger> gerarPrimosBigIntegerViaNativo(int bits, int quantidade) {
        File out;
        try {
            out = criarArquivoTempPrimosGigantes("primos_nativo_rsa_");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            executarJobNativoPrimosGigantes(bits, quantidade, 0, null, out, false);
            List<BigInteger> primos = lerPrimosDecimaisDoArquivo(out, quantidade);
            if (primos.size() < quantidade) {
                throw new RuntimeException("Rota nativa retornou apenas " + primos.size() + " de " + quantidade + " primos");
            }
            return primos;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<BigInteger> extrairPrimosDoRelatorioNativo(String relatorio, int quantidadeEsperada) {
        List<BigInteger> primos = new ArrayList<>();
        if (relatorio == null || relatorio.isEmpty()) {
            return primos;
        }

        String[] linhas = relatorio.split("\\R");
        for (String linha : linhas) {
            String valor = linha == null ? "" : linha.trim();
            if (ehLinhaNumeroDecimal(valor)) {
                primos.add(new BigInteger(valor));
                if (primos.size() >= quantidadeEsperada) {
                    break;
                }
            }
        }
        return primos;
    }

    private boolean ehLinhaNumeroDecimal(String valor) {
        if (valor == null || valor.isEmpty()) {
            return false;
        }
        for (int i = 0; i < valor.length(); i++) {
            if (!Character.isDigit(valor.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private CronometroOperacaoHandle iniciarCronometroOperacao(TextView alvo, String titulo, String... detalhes) {
        AtomicBoolean ativo = new AtomicBoolean(true);
        long inicio = System.currentTimeMillis();

        runOnUiThread(() -> {
            mostrarStatusOperacaoEmAndamento(alvo, titulo, 0L, detalhes);
            rolarConteudoParaView(alvo);
        });

        Thread cronometroThread = new Thread(() -> {
            while (ativo.get()) {
                long decorridoMs = System.currentTimeMillis() - inicio;
                runOnUiThread(() -> {
                    mostrarStatusOperacaoEmAndamento(alvo, titulo, decorridoMs, detalhes);
                });

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cronometroThread.start();

        return new CronometroOperacaoHandle(ativo, inicio);
    }

    private void pararCronometroOperacao(CronometroOperacaoHandle handle) {
        if (handle != null) {
            handle.ativo.set(false);
        }
    }

    private void mostrarStatusOperacaoEmAndamento(TextView alvo, String titulo, long decorridoMs, String... detalhes) {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        if (detalhes != null) {
            for (String detalhe : detalhes) {
                if (detalhe != null && !detalhe.isEmpty()) {
                    linhas.add(detalhe);
                }
            }
        }
        linhas.add("Tempo decorrido: " + String.format(Locale.getDefault(), "%.1f", decorridoMs / 1000.0) + " s");
        aplicarTextoStatusOperacao(alvo, "⏳", titulo, 0xFF333333, linhas.toArray(new String[0]));
    }

    private void mostrarStatusOperacaoConcluida(TextView alvo, String titulo, String... detalhes) {
        aplicarTextoStatusOperacao(alvo, "✅", titulo, 0xFF2E7D32, detalhes);
    }

    private void mostrarStatusOperacaoErro(TextView alvo, String titulo, String... detalhes) {
        aplicarTextoStatusOperacao(alvo, "❌", titulo, 0xFFC62828, detalhes);
    }

    private void aplicarTextoStatusOperacao(TextView alvo, String icone, String titulo, int cor, String... linhas) {
        alvo.setText(montarTextoStatusOperacao(icone, titulo, linhas));
        alvo.setTextColor(cor);
    }

    private String montarTextoStatusOperacao(String icone, String titulo, String... linhas) {
        StringBuilder texto = new StringBuilder();
        texto.append(icone).append(" ").append(titulo).append("\n");
        if (linhas != null) {
            for (String linha : linhas) {
                if (linha != null && !linha.isEmpty()) {
                    texto.append("   • ").append(linha).append("\n");
                }
            }
        }
        return texto.toString().trim();
    }

    private String montarTextoCronometroOperacao(String titulo, long decorridoMs, String... detalhes) {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        if (detalhes != null) {
            for (String detalhe : detalhes) {
                if (detalhe != null && !detalhe.isEmpty()) {
                    linhas.add(detalhe);
                }
            }
        }
        linhas.add("Tempo decorrido: " + String.format(Locale.getDefault(), "%.1f", decorridoMs / 1000.0) + " s");
        return montarTextoStatusOperacao("⏳", titulo, linhas.toArray(new String[0]));
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissão de armazenamento concedida.");
            } else {
                Log.e(TAG, "Permissão de armazenamento negada.");
                Toast.makeText(this, "Permissão de armazenamento negada. A aplicação não poderá salvar arquivos.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openFileWithHtmlViewer(File file) {
        try {
            // Criar URI usando FileProvider para compartilhar o arquivo
            Uri fileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider", file);

            // Determinar o tipo MIME baseado na extensão do arquivo
            String mimeType = "text/plain";
            if (file.getName().endsWith(".html")) {
                mimeType = "text/html";
            }

            // Intent para abrir com visualizador apropriado
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Verificar se há app disponível para abrir (somente por MIME de texto).
            // Não usar fallback genérico sem type, pois isso faz o Android oferecer apps irrelevantes (ex.: bancos).
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Log.d(TAG, "Arquivo aberto com visualizador " + mimeType + ": " + file.getAbsolutePath());
                return;
            }

            // Se for HTML e não houver visualizador HTML, tentar como texto.
            if ("text/html".equals(mimeType)) {
                Intent textIntent = new Intent(Intent.ACTION_VIEW);
                textIntent.setDataAndType(fileUri, "text/plain");
                textIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (textIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(textIntent);
                    Log.d(TAG, "Arquivo HTML aberto como texto: " + file.getAbsolutePath());
                    return;
                }
            }

            // Se não houver app de texto/HTML instalado, perguntar o que fazer.
            mostrarOpcoesArquivo(file, fileUri, mimeType);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir arquivo com visualizador", e);
            Toast.makeText(this, "Erro ao abrir arquivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarOpcoesArquivo(File file, Uri fileUri, String mimeTypeTentado) {
        try {
            // Verificar se é arquivo temporário (na pasta temp_primos)
            boolean isTemporario = file.getAbsolutePath().contains("temp_primos");
            String tipoArquivo = isTemporario ? "temporário" : "salvo";
            String mensagemPasta = isTemporario ?
                "Arquivo temporário (será removido automaticamente)" :
                "Arquivo salvo permanentemente";
            boolean isHtml = file.getName().endsWith(".html") || "text/html".equals(mimeTypeTentado);
            String tipoTexto = isHtml ? "HTML" : "texto";

            // Mostrar diálogo com opções
            new AlertDialog.Builder(this)
                .setTitle("📄 Arquivo " + tipoArquivo + " Gerado")
                .setMessage("Arquivo " + tipoArquivo + " em:\n" + file.getAbsolutePath()
                        + "\n\n" + mensagemPasta
                        + "\n\nNenhum app para abrir este arquivo como " + tipoTexto + " foi encontrado. Escolha uma opção:")
                .setPositiveButton("📤 Compartilhar", (dialog, which) -> {
                    // Compartilhar arquivo
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(isHtml ? "text/html" : "text/plain");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Compartilhar arquivo"));
                })
                .setNeutralButton("📁 Localização", (dialog, which) -> {
                    // Mostrar localização da pasta do arquivo
                    String pastaArquivo = file.getParent();
                    if (pastaArquivo != null) {
                        String mensagem = isTemporario ?
                            "Arquivo temporário em: " + pastaArquivo + "\n\n⚠️ Este arquivo será removido automaticamente pelo sistema." :
                            "Arquivo salvo em: " + pastaArquivo + "\n\n✅ Este arquivo permanece salvo permanentemente.";

                        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();

                        // Para arquivos salvos, tentar abrir pasta
                        if (!isTemporario) {
                            try {
                                Intent folderIntent = new Intent(Intent.ACTION_VIEW);
                                Uri folderUri = Uri.parse("content://com.android.externalstorage.documents/root/primary/Download/PrimeProFast");
                                folderIntent.setDataAndType(folderUri, "resource/folder");
                                startActivity(folderIntent);
                            } catch (Exception e) {
                                Log.d(TAG, "Não foi possível abrir pasta: " + e.getMessage());
                            }
                        }
                    } else {
                        Toast.makeText(this, "Localização do arquivo não disponível", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("❌ Fechar", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mostrar opções de arquivo", e);
            Toast.makeText(this, "Arquivo salvo em: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Salva TXT em Downloads/PrimeProFast sem MANAGE_EXTERNAL_STORAGE.
     * Android 10+ (API 29+): MediaStore. Android 9 e anteriores: pasta pública com permissão legada.
     */
    private String salvarTxtDownloadsPrimeProFast(String fileName, String conteudo) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PrimeProFast");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Não foi possível criar o arquivo em Downloads/PrimeProFast");
            }
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    throw new IOException("Não foi possível abrir o arquivo para escrita");
                }
                os.write(conteudo.getBytes(StandardCharsets.UTF_8));
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(uri, values, null, null);
            return "Downloads/PrimeProFast/" + fileName;
        }

        File file = new File(getPrimeProFastDirectory(), fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(conteudo);
        }
        return file.getAbsolutePath();
    }

    /**
     * Cria a pasta PrimeProFast no Downloads (Android 9 e anteriores).
     */
    private File getPrimeProFastDirectory() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File primeProFastDir = new File(downloadsDir, "PrimeProFast");

        if (!primeProFastDir.exists()) {
            boolean created = primeProFastDir.mkdirs();
            if (created) {
                Log.d(TAG, "Pasta PrimeProFast criada: " + primeProFastDir.getAbsolutePath());
            } else {
                Log.e(TAG, "Erro ao criar pasta PrimeProFast");
            }
        }

        return primeProFastDir;
    }

    // Métodos auxiliares para números grandes
    private long parseNumber(String str) {
        str = str.trim().toLowerCase();

        try {
            if (str.contains("^")) {
                // Notação exponencial: 2^1024, 10^100, etc.
                String[] parts = str.split("\\^");
                if (parts.length == 2) {
                    long base = Long.parseLong(parts[0]);
                    int exp = Integer.parseInt(parts[1]);

                    // Para bases pequenas e expoentes razoáveis
                    if (base <= 10 && exp <= 20) {
                        return (long)Math.pow(base, exp);
                    } else if (base == 2 && exp <= 63) { // 2^63 é o máximo para long
                        return 1L << exp;
                    } else if (base == 10 && exp <= 18) { // 10^18 é o máximo para long
                        return (long)Math.pow(10, exp);
                    } else if (base == 2 && exp <= 1024) {
                        // Para 2^1024 e similares, usar BigInteger e converter se possível
                        try {
                            BigInteger result = BigInteger.valueOf(2).pow(exp);
                            if (result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
                                return result.longValue();
                            }
                        } catch (Exception ex) {
                            // Ignorar e continuar
                        }
                    }
                }
            } else if (str.contains("e")) {
                // Notação exponencial: 1e100
                double value = Double.parseDouble(str);
                if (value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
                    return (long)value;
                }
            } else if (str.contains("k") || str.contains("m") || str.contains("g")) {
                // Notação abreviada: 1k = 1000, 1m = 1000000, 1g = 1000000000
                str = str.replaceAll("k", "000").replaceAll("m", "000000").replaceAll("g", "000000000");
                return Long.parseLong(str);
            }

            // Tentar parse direto
            return Long.parseLong(str);

        } catch (NumberFormatException e) {
            // Se falhar, tentar interpretar como notação científica
            if (str.contains("10^")) {
                str = str.replace("10^", "");
                try {
                    int exp = Integer.parseInt(str);
                    if (exp <= 18) {
                        return (long)Math.pow(10, exp);
                    }
                } catch (NumberFormatException ex) {
                    // Ignorar e continuar
                }
            }

            // Se tudo falhar, retornar valor padrão ou lançar exceção
            throw new NumberFormatException("Não foi possível interpretar: " + str +
                "\nUse formatos como: 1000, 2^10, 10^15, 1k, 1m, 1g\n" +
                "Para números muito grandes (>2^63), use o modo 'Dígitos específicos'");
        }
    }

    private String gerarPrimoSimulado(int digitos) {
        // Simular um número primo grande
        StringBuilder primo = new StringBuilder();

        // Primeiro dígito (1-9)
        primo.append(1 + (int)(Math.random() * 9));

        // Dígitos do meio
        for (int i = 1; i < digitos - 1; i++) {
            primo.append((int)(Math.random() * 10));
        }

        // Último dígito (1,3,7,9 para ser ímpar)
        int[] ultimosDigitos = {1, 3, 7, 9};
        primo.append(ultimosDigitos[(int)(Math.random() * 4)]);

        return primo.toString();
    }

    // Lista para rastrear arquivos temporários
    private static final List<File> arquivosTemporarios = new ArrayList<>();

    // Método auxiliar para salvar resultado temporário (com permissões corretas)
    private void abrirRelatorio(String titulo, String resultado, String tipo) {
        if (isFinishing() || isDestroyed()) return;
        WebView preview = new WebView(this);
        preview.getSettings().setAllowFileAccess(false);
        preview.getSettings().setAllowContentAccess(false);
        preview.getSettings().setBlockNetworkLoads(true);
        preview.getSettings().setJavaScriptEnabled(true);
        preview.loadDataWithBaseURL(null, gerarHTMLResultado(titulo, resultado, tipo),
            "text/html", "UTF-8", null);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(preview)
            .setNegativeButton("Fechar", null)
            .setNeutralButton("Copiar", null)
            .setPositiveButton("PDF", null)
            .create();
        File[] pdf = {null};
        dialog.setOnDismissListener(ignored -> {
            if (preview.getParent() instanceof ViewGroup) {
                ((ViewGroup) preview.getParent()).removeView(preview);
            }
            preview.destroy();
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (getWindow().getDecorView().getHeight() * 0.92f));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText(titulo, resultado));
                Toast.makeText(this, "Resultado copiado", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Resultado muito grande para copiar. Use o PDF.",
                    Toast.LENGTH_LONG).show();
            }
        });
        Button pdfButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        pdfButton.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(this, view);
            menu.getMenu().add(0, 1, 0, "Salvar PDF");
            menu.getMenu().add(0, 2, 1, "Compartilhar PDF");
            menu.setOnMenuItemClickListener(item -> {
                boolean compartilhar = item.getItemId() == 2;
                if (pdf[0] != null && pdf[0].isFile()) {
                    abrirAcaoPdf(pdf[0], compartilhar);
                    return true;
                }
                pdfButton.setEnabled(false);
                pdfButton.setText("Gerando...");
                new Thread(() -> {
                    try {
                        File pasta = new File(getCacheDir(), "temp_primos/pdf/" + java.util.UUID.randomUUID());
                        File arquivo = PPFResultPdf.write(getApplicationContext(), pasta, titulo, resultado);
                        runOnUiThread(() -> {
                            if (!dialog.isShowing() || isFinishing() || isDestroyed()) return;
                            pdf[0] = arquivo;
                            pdfButton.setText("PDF");
                            pdfButton.setEnabled(true);
                            abrirAcaoPdf(arquivo, compartilhar);
                        });
                    } catch (IOException | RuntimeException e) {
                        Log.e(TAG, "Falha ao gerar PDF", e);
                        runOnUiThread(() -> {
                            if (!dialog.isShowing() || isFinishing() || isDestroyed()) return;
                            pdfButton.setText("PDF");
                            pdfButton.setEnabled(true);
                            Toast.makeText(this, "Não foi possível gerar o PDF.", Toast.LENGTH_LONG).show();
                        });
                    }
                }, "PPF-PDF").start();
                return true;
            });
            menu.show();
        });
    }

    private void abrirAcaoPdf(File arquivo, boolean compartilhar) {
        try {
            if (!compartilhar) {
                pdfToSave = arquivo.getAbsolutePath();
                savePdfDocument.launch(arquivo.getName());
                return;
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivo);
            Intent send = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(ClipData.newRawUri(arquivo.getName(), uri));
            startActivity(Intent.createChooser(send, "Compartilhar PDF"));
        } catch (RuntimeException e) {
            if (!compartilhar) pdfToSave = null;
            Log.e(TAG, "Falha ao abrir ação de PDF", e);
            Toast.makeText(this, "Não foi possível abrir a opção de PDF.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(STATE_PDF_TO_SAVE, pdfToSave);
    }

    private void salvarPdfNoDestino(Uri destino) {
        String caminho = pdfToSave;
        pdfToSave = null;
        if (destino == null) return;
        new Thread(() -> {
            String mensagem;
            try {
                if (caminho == null) throw new IOException("Origem do PDF indisponível");
                try (FileInputStream entrada = new FileInputStream(caminho);
                     OutputStream saida = getContentResolver().openOutputStream(destino, "w")) {
                    if (saida == null) throw new IOException("Destino do PDF indisponível");
                    byte[] buffer = new byte[32768];
                    for (int tamanho; (tamanho = entrada.read(buffer)) != -1;) {
                        saida.write(buffer, 0, tamanho);
                    }
                }
                mensagem = "PDF salvo.";
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Falha ao salvar PDF", e);
                mensagem = "Não foi possível salvar o PDF.";
            }
            String aviso = mensagem;
            runOnUiThread(() -> Toast.makeText(this, aviso, Toast.LENGTH_LONG).show());
        }, "PPF-Save-PDF").start();
    }

    private void salvarResultadoTemporario(String resultado, String prefixo) {
        if (!prefixo.startsWith("tutorial_")) {
            runOnUiThread(() -> abrirRelatorio(tituloResultadoPorPrefixo(prefixo), resultado,
                tipoResultadoPorPrefixo(prefixo)));
            return;
        }
        try {
            // Criar pasta temporária com permissões adequadas
            File tempDir = new File(getCacheDir(), "temp_primos");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String extensao = ".html";
            File tempFile = new File(tempDir, prefixo + "_" + timestamp + extensao);

            try (OutputStream os = new java.io.FileOutputStream(tempFile)) {
                os.write(resultado.getBytes(StandardCharsets.UTF_8));
            }

            // Adicionar à lista de arquivos temporários
            arquivosTemporarios.add(tempFile);

            Log.d(TAG, "Arquivo temporário criado: " + tempFile.getAbsolutePath());

            runOnUiThread(() -> {
                // Abrir com visualizador HTML diretamente
                openFileWithHtmlViewer(tempFile);
            });
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar arquivo temporário", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Erro ao salvar arquivo temporário: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    // Método para limpar arquivos temporários
    private void limparArquivosTemporarios() {
        for (File arquivo : arquivosTemporarios) {
            if (arquivo.exists()) {
                boolean deletado = arquivo.delete();
                Log.d(TAG, "Arquivo temporário " + (deletado ? "removido" : "não removido") + ": " + arquivo.getName());
            }
        }
        arquivosTemporarios.clear();
    }

    // ==================== SISTEMA DE MONETIZAÇÃO ====================

    /**
     * Inicializa o sistema de monetização e carrega dados de uso
     */
    private void inicializarSistemaMonetizacao() {
        SharedPreferences usagePrefs = getSharedPreferences(PREFS_USAGE, MODE_PRIVATE);

        // Verificar se é um novo dia para resetar contador
        String hoje = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String ultimaData = usagePrefs.getString(KEY_LAST_RESET_DATE, "");

        if (!hoje.equals(ultimaData)) {
            // Novo dia - resetar contador
            dailyCalculations = 0;
            usagePrefs.edit()
                .putInt(KEY_DAILY_CALCULATIONS, 0)
                .putString(KEY_LAST_RESET_DATE, hoje)
                .apply();
        } else {
            // Mesmo dia - carregar contador atual
            dailyCalculations = usagePrefs.getInt(KEY_DAILY_CALCULATIONS, 0);
        }

        // Carregar status premium
        isPremium = usagePrefs.getBoolean(KEY_IS_PREMIUM, false);

        Log.d(TAG, "Sistema monetização inicializado - Cálculos hoje: " + dailyCalculations +
              ", Premium: " + isPremium);
    }

    /**
     * Verifica se o usuário pode realizar um cálculo
     */
    private boolean podeRealizarCalculo(long valorEntrada) {
        // Usuário premium tem acesso ilimitado
        if (!MONETIZACAO_ATIVA || isPremium) {
            return true;
        }

        // Verificar limite de cálculos diários
        if (dailyCalculations >= FREE_DAILY_LIMIT) {
            return false;
        }

        // Verificar limite de valor de entrada
        if (valorEntrada > FREE_MAX_VALUE) {
            return false;
        }

        return true;
    }

    /**
     * Verifica se o usuário pode realizar um cálculo no card atual
     */
    private boolean podeRealizarCalculoCard(long valorEntrada, boolean isBitValue) {
        // Usuário premium tem acesso ilimitado
        if (!MONETIZACAO_ATIVA || isPremium) {
            return true;
        }

        // Verificar limite de cálculos diários do card
        if (cardCalculations >= CARD_DAILY_LIMIT) {
            return false;
        }

        // Verificar limite de valor baseado no tipo
        if (isBitValue) {
            // Valores em bits: respeita CARD_MAX_BITS (ex.: 1024 na faixa gratuita limitada)
            if (valorEntrada > CARD_MAX_BITS) {
                return false;
            }
        } else {
            // Valores numéricos: respeita CARD_MAX_NUMERIC_VALUE
            if (valorEntrada > CARD_MAX_NUMERIC_VALUE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Registra um cálculo realizado
     */
    private void registrarCalculo() {
        if (!MONETIZACAO_ATIVA || !isPremium) {
            dailyCalculations++;
            getSharedPreferences(PREFS_USAGE, MODE_PRIVATE)
                .edit()
                .putInt(KEY_DAILY_CALCULATIONS, dailyCalculations)
                .apply();
        }
    }

    /**
     * Carrega cálculos do card atual
     */
    private void carregarCalculosCard() {
        if (currentCard.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_USAGE, MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastReset = prefs.getString(KEY_CARD_RESET_DATE + currentCard, "");

        // Reset se for um novo dia
        if (!today.equals(lastReset)) {
            cardCalculations = 0;
            prefs.edit()
                .putInt(KEY_CARD_CALCULATIONS + currentCard, 0)
                .putString(KEY_CARD_RESET_DATE + currentCard, today)
                .apply();
        } else {
            cardCalculations = prefs.getInt(KEY_CARD_CALCULATIONS + currentCard, 0);
        }
    }

    /**
     * Registra um cálculo no card atual
     */
    private void registrarCalculoCard() {
        if ((!MONETIZACAO_ATIVA || !isPremium) && !currentCard.isEmpty()) {
            cardCalculations++;
            getSharedPreferences(PREFS_USAGE, MODE_PRIVATE)
                .edit()
                .putInt(KEY_CARD_CALCULATIONS + currentCard, cardCalculations)
                .apply();
        }
    }

    /**
     * Mostra diálogo de limite atingido
     */
    private void mostrarDialogoLimiteAtingido(long valorEntrada) {
        String titulo, mensagem;

        if (dailyCalculations >= FREE_DAILY_LIMIT) {
            titulo = "🚫 Limite Diário Atingido";
            mensagem = "Você atingiu o limite de " + FREE_DAILY_LIMIT + " cálculos gratuitos por dia.\n\n" +
                      "💎 Upgrade para PrimeProFast Premium e tenha:\n" +
                      "• Cálculos ilimitados\n" +
                      "• Números de qualquer tamanho\n" +
                      "• Sem anúncios\n" +
                      "• Suporte prioritário\n\n" +
                      "Apenas R$ 9,90/mês!";
        } else if (valorEntrada > FREE_MAX_VALUE) {
            titulo = "🔒 Valor Muito Alto";
            mensagem = "A versão gratuita suporta números até " + String.format("%,d", FREE_MAX_VALUE) + ".\n\n" +
                      "💎 Upgrade para PrimeProFast Premium e calcule:\n" +
                      "• Números de qualquer tamanho (bilhões, trilhões...)\n" +
                      "• Cálculos ilimitados\n" +
                      "• Sem anúncios\n" +
                      "• Suporte prioritário\n\n" +
                      "Apenas R$ 9,90/mês!";
        } else {
            return; // Não deveria chegar aqui
        }

        new AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setPositiveButton("💎 Upgrade Agora", (dialog, which) -> mostrarTelaUpgrade())
            .setNegativeButton("❌ Cancelar", null)
            .setNeutralButton("📊 Ver Estatísticas", (dialog, which) -> mostrarEstatisticasUso())
            .show();
    }

    /**
     * Mostra diálogo de limite do card atingido
     */
    private void mostrarDialogoLimiteCard(long valorEntrada, boolean isBitValue) {
        String titulo, mensagem;

        if (cardCalculations >= CARD_DAILY_LIMIT) {
            titulo = "🚫 Limite do Card Atingido";
            mensagem = "Você atingiu o limite de " + CARD_DAILY_LIMIT + " cálculos por dia no card '" + currentCard + "'.\n\n" +
                      "💎 Upgrade para PrimeProFast Premium e tenha:\n" +
                      "• Cálculos ilimitados em todos os cards\n" +
                      "• Números de qualquer tamanho\n" +
                      "• Sem anúncios\n" +
                      "• Suporte prioritário\n\n" +
                      "Apenas R$ 9,90/mês!";
        } else if (isBitValue && valorEntrada > CARD_MAX_BITS) {
            titulo = "🔒 Limite de Bits Excedido";
            mensagem = "A versão gratuita suporta até " + CARD_MAX_BITS + " bits no card '" + currentCard + "'.\n\n" +
                      "💎 Upgrade para PrimeProFast Premium e calcule:\n" +
                      "• Qualquer quantidade de bits\n" +
                      "• Cálculos ilimitados\n" +
                      "• Sem anúncios\n" +
                      "• Suporte prioritário\n\n" +
                      "Apenas R$ 9,90/mês!";
        } else if (!isBitValue && valorEntrada > CARD_MAX_NUMERIC_VALUE) {
            titulo = "🔒 Limite de Valor Excedido";
            mensagem = "A versão gratuita suporta números até " + String.format("%,d", CARD_MAX_NUMERIC_VALUE) +
                      " no card '" + currentCard + "'.\n\n" +
                      "💎 Upgrade para PrimeProFast Premium e calcule:\n" +
                      "• Números de qualquer tamanho\n" +
                      "• Cálculos ilimitados\n" +
                      "• Sem anúncios\n" +
                      "• Suporte prioritário\n\n" +
                      "Apenas R$ 9,90/mês!";
        } else {
            return; // Não deveria chegar aqui
        }

        new AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setPositiveButton("💎 Upgrade Agora", (dialog, which) -> mostrarTelaUpgrade())
            .setNegativeButton("❌ Cancelar", null)
            .setNeutralButton("📊 Ver Estatísticas", (dialog, which) -> mostrarEstatisticasUso())
            .show();
    }

    /**
     * Mostra tela de upgrade para versão premium
     */
    private void mostrarTelaUpgrade() {
        if (!MONETIZACAO_ATIVA) {
            return;
        }
        // Limpar container de conteúdo
        contentContainer.removeAllViews();
        mostrarTelaConteudo();
        if (menuScrollView != null) {
            menuScrollView.setVisibility(View.GONE);
        } else {
            menuContainer.setVisibility(View.GONE);
        }

        // Criar layout da tela de upgrade
        LinearLayout upgradeLayout = new LinearLayout(this);
        upgradeLayout.setOrientation(LinearLayout.VERTICAL);
        upgradeLayout.setPadding(32, 32, 32, 32);
        upgradeLayout.setBackgroundColor(temaEscuro ? Color.parseColor("#1a1a1a") : Color.WHITE);

        // Título
        TextView titulo = new TextView(this);
        titulo.setText("💎 PrimeProFast Premium");
        titulo.setTextSize(28);
        titulo.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setGravity(android.view.Gravity.CENTER);
        titulo.setPadding(0, 0, 0, 24);
        upgradeLayout.addView(titulo);

        // Subtítulo
        TextView subtitulo = new TextView(this);
        subtitulo.setText("Desbloqueie todo o potencial da calculadora de primos!");
        subtitulo.setTextSize(16);
        subtitulo.setTextColor(temaEscuro ? Color.LTGRAY : Color.DKGRAY);
        subtitulo.setGravity(android.view.Gravity.CENTER);
        subtitulo.setPadding(0, 0, 0, 32);
        upgradeLayout.addView(subtitulo);

        // Lista de benefícios
        String[] beneficios = {
            "🚀 Cálculos ilimitados por dia",
            "📊 Estatísticas avançadas e relatórios",
            "🎨 Limites exclusivos premium",
            " 📱 Sem anúncios",
            "⚡ Performance otimizada",
            "🔢 Números de qualquer tamanho (Primos no limite até 10 bilhões, geração de primos e pesquisa até 10 mil dígitos...)"
        };

        for (String beneficio : beneficios) {
            TextView item = new TextView(this);
            item.setText(beneficio);
            item.setTextSize(16);
            item.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
            item.setPadding(0, 8, 0, 8);
            upgradeLayout.addView(item);
        }

        // Preço
        TextView preco = new TextView(this);
        preco.setText("\n💎 Apenas R$ 9,90/mês\nou R$ 99,90/ano (2 meses grátis!)");
        preco.setTextSize(20);
        preco.setTextColor(Color.parseColor("#4CAF50"));
        preco.setTypeface(null, Typeface.BOLD);
        preco.setGravity(android.view.Gravity.CENTER);
        preco.setPadding(0, 24, 0, 24);
        upgradeLayout.addView(preco);

        // Botões
        LinearLayout botoesLayout = new LinearLayout(this);
        botoesLayout.setOrientation(LinearLayout.HORIZONTAL);
        botoesLayout.setGravity(android.view.Gravity.CENTER);

        Button btnMensal = criarBotaoModerno("R$ 9,90/mês", Color.parseColor("#2196F3"), Color.parseColor("#1976D2"));
        btnMensal.setOnClickListener(v -> processarCompra("mensal"));

        Button btnAnual = criarBotaoModerno("R$ 99,90/ano", Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"));
        btnAnual.setOnClickListener(v -> processarCompra("anual"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(8, 0, 8, 0);
        btnMensal.setLayoutParams(params);
        btnAnual.setLayoutParams(params);

        botoesLayout.addView(btnMensal);
        botoesLayout.addView(btnAnual);
        upgradeLayout.addView(botoesLayout);

        // Botão voltar - usar a mesma função do botão físico de voltar
        Button btnVoltar = criarBotaoModerno("← Voltar ao Menu", Color.parseColor("#757575"), Color.parseColor("#616161"));
        btnVoltar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        upgradeLayout.addView(btnVoltar);

        contentContainer.addView(upgradeLayout);
    }

    /**
     * Inicia o fluxo real de assinatura na Google Play (mesmos IDs criados no Play Console).
     */
    private void processarCompra(String tipo) {
        if (playBillingManager == null) {
            Toast.makeText(this, "Serviço de pagamento indisponível. Abra o app pela Play Store.", Toast.LENGTH_LONG).show();
            return;
        }
        playBillingManager.launchSubscriptionPurchase(this, tipo);
    }

    /** Aplica estado premium local e em {@link #PREFS_USAGE} (sincronizado com compras na Play). */
    private void aplicarEstadoPremium(boolean active) {
        isPremium = active;
        getSharedPreferences(PREFS_USAGE, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_PREMIUM, active)
            .apply();
    }

    /**
     * Ativa versão premium (uso legado / fallback; preferir compra pela Play)
     */
    private void ativarPremium() {
        aplicarEstadoPremium(true);

        // Voltar ao menu usando a mesma função do botão físico de voltar
        getOnBackPressedDispatcher().onBackPressed();

        // Mostrar confirmação
        Toast.makeText(this, "🎉 PrimeProFast Premium ativado!", Toast.LENGTH_LONG).show();
    }

    /**
     * Mostra estatísticas de uso do usuário
     */
    private void mostrarEstatisticasUso() {
        // Limpar container de conteúdo
        contentContainer.removeAllViews();
        mostrarTelaConteudo();
        if (menuScrollView != null) {
            menuScrollView.setVisibility(View.GONE);
        } else {
            menuContainer.setVisibility(View.GONE);
        }

        // Criar layout das estatísticas
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.VERTICAL);
        statsLayout.setPadding(32, 32, 32, 32);
        statsLayout.setBackgroundColor(temaEscuro ? Color.parseColor("#1a1a1a") : Color.WHITE);

        // Título
        TextView titulo = new TextView(this);
        titulo.setText("📊 Suas Estatísticas");
        titulo.setTextSize(24);
        titulo.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setGravity(android.view.Gravity.CENTER);
        titulo.setPadding(0, 0, 0, 24);
        statsLayout.addView(titulo);

        // Status premium
        TextView statusPremium = new TextView(this);
        if (!MONETIZACAO_ATIVA) {
            statusPremium.setText("Acesso gratuito");
            statusPremium.setTextColor(Color.parseColor("#4CAF50"));
        } else if (isPremium) {
            statusPremium.setText("💎 Status: PREMIUM ATIVO");
            statusPremium.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            statusPremium.setText("🆓 Status: VERSÃO GRATUITA");
            statusPremium.setTextColor(Color.parseColor("#FF9800"));
        }
        statusPremium.setTextSize(18);
        statusPremium.setTypeface(null, Typeface.BOLD);
        statusPremium.setGravity(android.view.Gravity.CENTER);
        statusPremium.setPadding(0, 0, 0, 16);
        statsLayout.addView(statusPremium);

        // Estatísticas do dia
        TextView statsDia = new TextView(this);
        if (!MONETIZACAO_ATIVA) {
            statsDia.setText("Cálculos hoje: " + dailyCalculations);
        } else if (isPremium) {
            statsDia.setText("✅ Cálculos hoje: Ilimitados");
        } else {
            statsDia.setText("📊 Cálculos hoje: " + dailyCalculations + "/" + FREE_DAILY_LIMIT);
        }
        statsDia.setTextSize(16);
        statsDia.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
        statsDia.setPadding(0, 8, 0, 8);
        statsLayout.addView(statsDia);

        // Limite de valor
        TextView limiteValor = new TextView(this);
        if (isPremium) {
            limiteValor.setText("✅ Limite de valor: Ilimitado");
        } else {
            limiteValor.setText("🔢 Limite de valor: Até " + String.format("%,d", FREE_MAX_VALUE));
        }
        limiteValor.setTextSize(16);
        limiteValor.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
        limiteValor.setPadding(0, 8, 0, 8);
        if (MONETIZACAO_ATIVA) {
            statsLayout.addView(limiteValor);
        }

        // Estatísticas por card
        if (!MONETIZACAO_ATIVA || !isPremium) {
            TextView tituloCards = new TextView(this);
            tituloCards.setText("📋 Estatísticas por Card");
            tituloCards.setTextSize(18);
            tituloCards.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
            tituloCards.setTypeface(null, Typeface.BOLD);
            tituloCards.setPadding(0, 16, 0, 8);
            statsLayout.addView(tituloCards);

            // Lista de cards com suas estatísticas
            String[] cards = {
                "Primos por Intervalo", "Primos Especiais", "Primos Aleatórios",
                "Conjectura de Legendre", "Números de Mersenne", "Números Perfeitos",
                "Segurança Digital", "Teste de Primalidade", "Estatísticas"
            };

            for (String card : cards) {
                SharedPreferences prefs = getSharedPreferences(PREFS_USAGE, MODE_PRIVATE);
                int cardCalcs = prefs.getInt(KEY_CARD_CALCULATIONS + card, 0);

                TextView cardStats = new TextView(this);
                cardStats.setText("• " + card + ": " + cardCalcs
                        + (MONETIZACAO_ATIVA ? "/" + CARD_DAILY_LIMIT : "") + " cálculos");
                cardStats.setTextSize(14);
                cardStats.setTextColor(temaEscuro ? Color.LTGRAY : Color.DKGRAY);
                cardStats.setPadding(16, 4, 0, 4);
                statsLayout.addView(cardStats);
            }

            // Limites por card
            TextView limitesCards = new TextView(this);
            limitesCards.setText("\n🔒 Limites por Card:\n" +
                               "• Valores numéricos: Até " + String.format("%,d", CARD_MAX_NUMERIC_VALUE) + "\n" +
                               "• Valores em bits: Até " + CARD_MAX_BITS + " bits\n" +
                               "• Cálculos por card: " + CARD_DAILY_LIMIT + " por dia");
            limitesCards.setTextSize(14);
            limitesCards.setTextColor(temaEscuro ? Color.LTGRAY : Color.DKGRAY);
            limitesCards.setPadding(0, 8, 0, 8);
            if (MONETIZACAO_ATIVA) {
                statsLayout.addView(limitesCards);
            }
        }

        // Barra de progresso (se não premium)
        if (MONETIZACAO_ATIVA && !isPremium) {
            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(FREE_DAILY_LIMIT);
            progressBar.setProgress(dailyCalculations);
            progressBar.setPadding(0, 16, 0, 16);
            statsLayout.addView(progressBar);

            TextView progressText = new TextView(this);
            progressText.setText("Progresso do dia: " + (dailyCalculations * 100 / FREE_DAILY_LIMIT) + "%");
            progressText.setTextSize(14);
            progressText.setTextColor(temaEscuro ? Color.LTGRAY : Color.DKGRAY);
            progressText.setGravity(android.view.Gravity.CENTER);
            statsLayout.addView(progressText);
        }

        // Botões
        if (MONETIZACAO_ATIVA && !isPremium) {
            Button btnUpgrade = criarBotaoModerno("💎 Upgrade para Premium", Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"));
            btnUpgrade.setOnClickListener(v -> mostrarTelaUpgrade());
            statsLayout.addView(btnUpgrade);
        }

        Button btnVoltar = criarBotaoModerno("← Voltar ao Menu", Color.parseColor("#757575"), Color.parseColor("#616161"));
        btnVoltar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        statsLayout.addView(btnVoltar);

        contentContainer.addView(statsLayout);
    }

    /**
     * Mostra informações completas do app
     */
    private void mostrarInformacoesApp() {
        // Limpar container de conteúdo completamente
        contentContainer.removeAllViews();
        mostrarTelaConteudo();
        if (menuScrollView != null) {
            menuScrollView.setVisibility(View.GONE);
        } else {
            menuContainer.setVisibility(View.GONE);
        }

        // Resetar completamente o estado do contentContainer
        contentContainer.setBackgroundColor(Color.TRANSPARENT);
        contentContainer.setAlpha(1.0f);

        // Limpar também o scrollView global se existir
        if (this.scrollView != null) {
            this.scrollView.removeAllViews();
            this.scrollView = null;
        }

        // Criar ScrollView para permitir rolagem
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));

        // Container interno para os elementos
        LinearLayout containerInterno = new LinearLayout(this);
        containerInterno.setOrientation(LinearLayout.VERTICAL);
        containerInterno.setPadding(32, 32, 32, 40);
        containerInterno.setBackgroundColor(temaEscuro ? Color.parseColor("#1a1a1a") : Color.WHITE);

        // Título
        TextView titulo = new TextView(this);
        titulo.setText("ℹ️ PrimeProFast - Informações");
        titulo.setTextSize(24);
        titulo.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setGravity(android.view.Gravity.CENTER);
        titulo.setPadding(0, 0, 0, 24);
        containerInterno.addView(titulo);

        // Informações do app
        String[] informacoes = {
            "📱 Nome: PrimeProFast",
            "📦 Versão: " + BuildConfig.VERSION_NAME,
            "",
            "👨‍💻 Desenvolvido por:",
            "Wilson Lucas Ferreira",
            "",
            "📞 Contato:",
            "WhatsApp: +55 (69) 9 99104826",
            "",
            "🔧 Funcionalidades:",
            "• Encontre primos em intervalos (até 10 bilhões)",
            "• Gere primos gigantes (4096+ bits)",
            "• Primos especiais (gêmeos, sexy, etc.)",
            "• Teste de primalidade avançado",
            "• Estatísticas de primos (densidade, gaps, distribuição, etc.)",
            "• Aplicações criptográficas",
            "• Números de Mersenne",
            "• Números perfeitos",
            "• Conjectura de Legendre",
            "",
            MONETIZACAO_ATIVA ? "💎 Disponível: Versão Premium" : "Acesso gratuito",
            "",
            "🛡️ Segurança:",
            "• Verificação de integridade",
            "• Detecção de ambiente malicioso",
            "",
            "🌐 Tecnologias:",
            "• Android Native (Java)",
            "• Algoritmos matemáticos avançados",
            "• Interface moderna e responsiva"
        };

        for (String info : informacoes) {
            TextView item = new TextView(this);
            item.setText(info);
            item.setTextSize(14);

            if (info.isEmpty()) {
                item.setPadding(0, 8, 0, 8);
            } else if (info.startsWith("📱") || info.startsWith("👨") || info.startsWith("📞") ||
                      info.startsWith("🔧") || info.startsWith("💎") || info.startsWith("🛡️") ||
                      info.startsWith("📊") || info.startsWith("🌐")) {
                // Cabeçalhos das seções
                item.setTextColor(Color.parseColor("#2196F3"));
                item.setTypeface(null, Typeface.BOLD);
                item.setPadding(0, 16, 0, 8);
            } else if (info.startsWith("•")) {
                // Itens de lista
                item.setTextColor(temaEscuro ? Color.LTGRAY : Color.DKGRAY);
                item.setPadding(16, 4, 0, 4);
            } else if (info.contains("WhatsApp")) {
                // Número do WhatsApp - clicável
                item.setTextColor(Color.parseColor("#25D366"));
                item.setTypeface(null, Typeface.BOLD);
                item.setPadding(0, 4, 0, 4);
                item.setClickable(true);
                item.setFocusable(true);
                item.setOnClickListener(v -> {
                    try {
                        String phoneNumber = "5569999104826";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://wa.me/" + phoneNumber));
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao abrir WhatsApp: " + e.getMessage());
                        Toast.makeText(this, "Erro ao abrir WhatsApp", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Texto normal
                item.setTextColor(temaEscuro ? Color.WHITE : Color.BLACK);
                item.setPadding(0, 4, 0, 4);
            }
            containerInterno.addView(item);
        }

        // Botão voltar
        Button btnVoltar = criarBotaoModerno("← Voltar ao Menu", Color.parseColor("#757575"), Color.parseColor("#616161"));
        btnVoltar.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        containerInterno.addView(btnVoltar);

        // Adicionar container interno ao ScrollView
        scrollView.addView(containerInterno);

        // Adicionar ScrollView ao container principal
        contentContainer.addView(scrollView);
    }

    // Método para salvar resultado permanente (para funcionalidades que precisam persistir)
    private void salvarResultadoPermanente(String resultado, String prefixo) {
        try {
            File tempDir = new File(getCacheDir(), "temp_primos");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            boolean isTutorial = prefixo.startsWith("tutorial_");
            String conteudoFinal = isTutorial
                ? resultado
                : gerarHTMLResultado(tituloResultadoPorPrefixo(prefixo), resultado, tipoResultadoPorPrefixo(prefixo));
            String extensao = ".html";
            File tempFile = new File(tempDir, prefixo + "_" + timestamp + extensao);

            try (OutputStream os = new java.io.FileOutputStream(tempFile)) {
                os.write(conteudoFinal.getBytes(StandardCharsets.UTF_8));
            }

            Log.d(TAG, "Arquivo persistente criado: " + tempFile.getAbsolutePath());

            runOnUiThread(() -> {
                // Abrir com visualizador HTML diretamente
                openFileWithHtmlViewer(tempFile);
            });
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar arquivo persistente", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Erro ao salvar arquivo persistente: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    // Métodos auxiliares para geração robusta de primos
    private String gerarNumeroComDigitos(int digitos) {
        StringBuilder numero = new StringBuilder();

        // Primeiro dígito (1-9)
        numero.append(1 + (int)(Math.random() * 9));

        // Dígitos do meio
        for (int i = 1; i < digitos - 1; i++) {
            numero.append((int)(Math.random() * 10));
        }

        // Último dígito (1,3,7,9 para ser ímpar)
        int[] ultimosDigitos = {1, 3, 7, 9};
        numero.append(ultimosDigitos[(int)(Math.random() * 4)]);

        return numero.toString();
    }

    private boolean ehPrimoRobusto(int n) {
        if (n < 2) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        // Para números grandes, usar teste probabilístico
        if (n > 1000000) {
            return millerRabinRobusto(n, 10);
        }

        // Para números menores, usar divisão por tentativa
        for (int i = 5; (long) i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    private boolean ehPrimoRobusto(long n) {
        if (n < 2) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        // Para números muito grandes, usar teste probabilístico
        if (n > 1000000) {
            return millerRabinRobustoLong(n, 10);
        }

        // Para números menores, usar divisão por tentativa
        for (long i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    private boolean millerRabinRobusto(int n, int k) {
        if (n <= 3) return n > 1;
        if (n % 2 == 0) return false;

        // Escrever n-1 como 2^r * d
        int r = 0;
        int d = n - 1;
        while (d % 2 == 0) {
            r++;
            d = d / 2;
        }

        // Testar k bases
        for (int i = 0; i < k; i++) {
            int a = 2 + (int)(Math.random() * (n - 3));
            int x = modularExponentiation(a, d, n);

            if (x == 1 || x == n - 1) continue;

            boolean ehProvavelmentePrimo = false;
            for (int j = 0; j < r - 1; j++) {
                x = (x * x) % n;
                if (x == n - 1) {
                    ehProvavelmentePrimo = true;
                    break;
                }
            }

            if (!ehProvavelmentePrimo) return false;
        }

        return true;
    }

    private boolean millerRabinRobustoLong(long n, int k) {
        if (n <= 3) return n > 1;
        if (n % 2 == 0) return false;

        // Escrever n-1 como 2^r * d
        int r = 0;
        long d = n - 1;
        while (d % 2 == 0) {
            r++;
            d = d / 2;
        }

        // Testar k bases
        for (int i = 0; i < k; i++) {
            long a = 2 + (long)(Math.random() * (n - 3));
            long x = modularExponentiationLong(a, d, n);

            if (x == 1 || x == n - 1) continue;

            boolean ehProvavelmentePrimo = false;
            for (int j = 0; j < r - 1; j++) {
                x = (x * x) % n;
                if (x == n - 1) {
                    ehProvavelmentePrimo = true;
                    break;
                }
            }

            if (!ehProvavelmentePrimo) return false;
        }

        return true;
    }

    private int modularExponentiation(int base, int expoente, int modulo) {
        if (modulo == 1) return 0;

        long resultado = 1;
        base = base % modulo;

        while (expoente > 0) {
            if (expoente % 2 == 1) {
                resultado = (resultado * base) % modulo;
            }
            expoente = expoente / 2;
            base = (base * base) % modulo;
        }

        return (int)resultado;
    }

    private long modularExponentiationLong(long base, long expoente, long modulo) {
        if (modulo == 1) return 0;

        long resultado = 1;
        base = base % modulo;

        while (expoente > 0) {
            if (expoente % 2 == 1) {
                resultado = (resultado * base) % modulo;
            }
            expoente = expoente / 2;
            base = (base * base) % modulo;
        }

        return resultado;
    }

    /**
     * Interpreta inteiros ou expressões com + − * / % ^ e parênteses (resultado inteiro).
     * Suporta ainda sufixos k/m/g e notação científica em inteiro (ex.: 1e30 = 1×10³⁰).
     */
    private BigInteger parseBigInteger(String str) {
        return parseBigIntegerOuExpressao(str);
    }

    private BigInteger parseBigIntegerOuExpressao(String str) {
        if (str == null || str.length() > 200_000) {
            throw new NumberFormatException("Entrada inválida ou muito longa");
        }
        str = str.trim();
        if (str.isEmpty()) {
            throw new NumberFormatException("Expressão vazia");
        }
        StringBuilder normalized = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = c > 127 ? Character.digit(c, 10) : -1;
            normalized.append(digit < 0 ? c : (char) ('0' + digit));
        }
        String decimal = evaluateIntegerNativo(normalized.toString());
        if (decimal == null) {
            throw new NumberFormatException("Expressão inválida ou acima do limite de cálculo");
        }
        return new BigInteger(decimal);
    }

    // Teste de primalidade para BigInteger (algoritmo especializado)
    private boolean ehPrimoBigInteger(BigInteger n) {
        if (n.compareTo(BigInteger.TWO) < 0) return false;
        if (n.bitLength() <= 64) return ehPrimo64Nativo(n.longValue());
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) return false;

        // Para números muito grandes, usar a rota nativa para evitar limitações
        // do BigInteger/modPow do Android em alguns dispositivos.
        if (n.bitLength() > 4096) {
            return testarPrimalidadeGiganteNativo(n.toString(), 25);
        }

        try {
            return millerRabinBigInteger(n, 15);
        } catch (ArithmeticException e) {
            Log.w(TAG, "Fallback para teste nativo de primalidade: " + e.getMessage());
            return testarPrimalidadeGiganteNativo(n.toString(), 25);
        }
    }

    // algoritmo especializado para BigInteger
    private boolean millerRabinBigInteger(BigInteger n, int k) {
        if (n.compareTo(BigInteger.TWO) < 0) return false;
        if (n.equals(BigInteger.TWO)) return true;
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) return false;

        // Escrever n-1 como 2^r * d
        BigInteger nMinus1 = n.subtract(BigInteger.ONE);
        int r = 0;
        BigInteger d = nMinus1;
        while (d.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            r++;
            d = d.divide(BigInteger.TWO);
        }

        // Testar k bases
        for (int i = 0; i < k; i++) {
            BigInteger a = BigInteger.valueOf(2).add(
                new BigInteger(n.bitLength(), new SecureRandom()).mod(n.subtract(BigInteger.valueOf(3)))
            );

            BigInteger x = a.modPow(d, n);
            if (x.equals(BigInteger.ONE) || x.equals(nMinus1)) continue;

            boolean ehProvavelmentePrimo = false;
            for (int j = 0; j < r - 1; j++) {
                x = x.modPow(BigInteger.TWO, n);
                if (x.equals(nMinus1)) {
                    ehProvavelmentePrimo = true;
                    break;
                }
            }

            if (!ehProvavelmentePrimo) return false;
        }

        return true;
    }

    private String relatorioLegendreComum(BigInteger inicio, BigInteger fim) {
        String relatorio = legendreReportNativo(inicio.toString(), fim.toString());
        if (relatorio == null || relatorio.isEmpty()) {
            throw new IllegalArgumentException("Informe valores positivos de n com até 10.000 dígitos.");
        }
        return relatorio;
    }

    private void incluirLegendreSemResultado(StringBuilder report, BigInteger n) {
        BigInteger baixo = n.pow(2);
        BigInteger alto = n.add(BigInteger.ONE).pow(2);
        report.append("\nBUSCA SEM PRIMO ENCONTRADO\n")
            .append("n = ").append(n).append('\n')
            .append("Intervalo (").append(baixo).append(", ").append(alto).append(")\n");
        if (alto.bitLength() > 63) {
            report.append("A verificação foi parcial. O resultado é inconclusivo.\n\n");
        } else {
            report.append("A busca exata não encontrou primo. Confirme por um método independente antes de tirar conclusões.\n\n");
        }
    }

    private String taxaLegendre(int acertos, int total) {
        return total > 0 ? String.format("%.2f%%", acertos * 100.0 / total) : "Sem intervalos concluídos";
    }

    // Teste de primalidade otimizado para números muito grandes (até 10.000 dígitos)
    private boolean ehPrimoBigIntegerOtimizado(BigInteger n) {
        if (n.compareTo(BigInteger.TWO) < 0) return false;
        if (n.equals(BigInteger.TWO) || n.equals(BigInteger.valueOf(3))) return true;
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) return false;

        int digitos = n.toString().length();

        // Estratégia adaptativa baseada no tamanho
        if (digitos <= 100) {
            // Para números pequenos: teste determinístico
            return millerRabinBigInteger(n, 10);
        } else if (digitos <= 500) {
            // Para números médios: teste probabilístico moderado
            return millerRabinBigInteger(n, 15);
        } else if (digitos <= 2000) {
            // Para números grandes: teste probabilístico robusto
            return millerRabinBigInteger(n, 20);
        } else if (digitos <= 5000) {
            // Para números muito grandes: teste probabilístico intensivo
            return millerRabinBigInteger(n, 25);
        } else {
            // Para números extremamente grandes (até 10.000 dígitos): teste máximo
            return millerRabinBigInteger(n, 30);
        }
    }

    // algoritmo especializado otimizado para números extremamente grandes
    private boolean millerRabinBigIntegerOtimizado(BigInteger n, int k) {
        if (n.compareTo(BigInteger.TWO) < 0) return false;
        if (n.equals(BigInteger.TWO)) return true;
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) return false;

        // Escrever n-1 como 2^r * d
        BigInteger nMinus1 = n.subtract(BigInteger.ONE);
        int r = 0;
        BigInteger d = nMinus1;
        while (d.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            r++;
            d = d.divide(BigInteger.TWO);
        }

        // Usar bases fixas conhecidas para maior confiabilidade
        int[] bases = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113};
        int maxBases = Math.min(k, bases.length);

        // Testar com bases fixas primeiro
        for (int i = 0; i < maxBases; i++) {
            if (bases[i] >= n.intValue()) break;

            BigInteger a = BigInteger.valueOf(bases[i]);
            BigInteger x = a.modPow(d, n);

            if (x.equals(BigInteger.ONE) || x.equals(nMinus1)) continue;

            boolean ehProvavelmentePrimo = false;
            for (int j = 0; j < r - 1; j++) {
                x = x.modPow(BigInteger.TWO, n);
                if (x.equals(nMinus1)) {
                    ehProvavelmentePrimo = true;
                    break;
                }
            }

            if (!ehProvavelmentePrimo) return false;
        }

        // Se ainda precisar de mais bases, usar aleatórias
        if (k > maxBases) {
            SecureRandom rnd = new SecureRandom();
            for (int i = maxBases; i < k; i++) {
                BigInteger a = BigInteger.valueOf(2).add(
                    new BigInteger(n.bitLength(), rnd).mod(n.subtract(BigInteger.valueOf(3)))
                );

                BigInteger x = a.modPow(d, n);
                if (x.equals(BigInteger.ONE) || x.equals(nMinus1)) continue;

                boolean ehProvavelmentePrimo = false;
                for (int j = 0; j < r - 1; j++) {
                    x = x.modPow(BigInteger.TWO, n);
                    if (x.equals(nMinus1)) {
                        ehProvavelmentePrimo = true;
                        break;
                    }
                }

                if (!ehProvavelmentePrimo) return false;
            }
        }

        return true;
    }

    // Métodos auxiliares para números perfeitos com suporte a dígitos extremamente grandes
    private boolean verificarNumeroPerfeito(BigInteger n) {
        try {
            if (n.compareTo(BigInteger.ONE) <= 0) return false;

            int digitos = n.toString().length();

            // Limite de segurança para evitar crash
            if (digitos > 1000) {
                Log.w(TAG, "Número muito grande para verificação: " + digitos + " dígitos");
                return false;
            }

            // Para números muito grandes, usar estratégia otimizada
            if (digitos > 100) {
                return verificarNumeroPerfeitoOtimizado(n);
            }

            // Para números pequenos, verificação completa
            return verificarNumeroPerfeitoCompleto(n);

        } catch (OutOfMemoryError | StackOverflowError e) {
            Log.e(TAG, "Erro de memória em verificarNumeroPerfeito: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Erro em verificarNumeroPerfeito: " + e.getMessage());
            return false;
        }
    }

    private boolean verificarNumeroPerfeitoCompleto(BigInteger n) {
        try {
            // Verificação de segurança para evitar loops muito longos
            if (n.toString().length() > 50) {
                Log.w(TAG, "Número muito grande para verificação completa, usando método otimizado");
                return verificarNumeroPerfeitoOtimizado(n);
            }

            // Verificação completa para números pequenos
            BigInteger soma = BigInteger.ZERO;
            BigInteger sqrt = n.sqrt();

            // Limite adicional de segurança
            if (sqrt.compareTo(BigInteger.valueOf(1000000)) > 0) {
                Log.w(TAG, "Raiz quadrada muito grande, abortando verificação completa");
                return false;
            }

            // Encontrar divisores até a raiz quadrada
            for (BigInteger i = BigInteger.ONE; i.compareTo(sqrt) <= 0; i = i.add(BigInteger.ONE)) {
                try {
                    if (n.mod(i).equals(BigInteger.ZERO)) {
                        if (i.compareTo(n) < 0) {
                            soma = soma.add(i);
                        }

                        BigInteger outroDivisor = n.divide(i);
                        if (outroDivisor.compareTo(i) > 0 && outroDivisor.compareTo(n) < 0) {
                            soma = soma.add(outroDivisor);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro no loop de divisores: " + e.getMessage());
                    break;
                }
            }

            return soma.equals(n);

        } catch (OutOfMemoryError | StackOverflowError e) {
            Log.e(TAG, "Erro de memória em verificarNumeroPerfeitoCompleto: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Erro em verificarNumeroPerfeitoCompleto: " + e.getMessage());
            return false;
        }
    }

    private boolean verificarNumeroPerfeitoOtimizado(BigInteger n) {
        // Para números muito grandes, usar propriedades conhecidas
        int digitos = n.toString().length();

        // Verificar se é par (todos os números perfeitos conhecidos são pares)
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            // Verificar se segue o teorema de Euclides
            return verificarTeoremaEuclides(n);
        }

        // Para números ímpares, verificação limitada (conjectura: não existem)
        // Removida limitação artificial de 1500 dígitos

        // Verificação parcial para números ímpares pequenos
        return verificarNumeroPerfeitoImpar(n);
    }

    private boolean verificarTeoremaEuclides(BigInteger n) {
        // Verificar se n = 2^(p-1) × (2^p - 1) onde 2^p - 1 é primo
        try {
            // Tentar fatorar n como 2^(p-1) × primo
            BigInteger temp = n;
            int potencia = 0;

            // Dividir por 2 até não ser mais possível
            while (temp.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                temp = temp.divide(BigInteger.TWO);
                potencia++;
            }

            if (potencia > 0 && temp.compareTo(BigInteger.ONE) > 0) {
                // Verificar se temp é primo (primo de Mersenne)
                if (ehPrimoBigIntegerOtimizado(temp)) {
                    // Verificar se temp + 1 é uma potência de 2
                    BigInteger tempMais1 = temp.add(BigInteger.ONE);
                    if (tempMais1.bitCount() == 1) {
                        // Encontrar o expoente
                        int expoente = tempMais1.bitLength() - 1;
                        if (expoente == potencia + 1) {
                            return true; // Segue o teorema de Euclides
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Em caso de erro, continuar com verificação alternativa
        }

        return false;
    }

    private boolean verificarNumeroPerfeitoImpar(BigInteger n) {
        // Verificação limitada para números ímpares
        // Baseada na conjectura de que não existem números perfeitos ímpares

        // Verificar divisibilidade por números pequenos
        for (int i = 3; i <= 1000; i += 2) {
            if (n.mod(BigInteger.valueOf(i)).equals(BigInteger.ZERO)) {
                BigInteger divisor = BigInteger.valueOf(i);
                BigInteger outroDivisor = n.divide(divisor);

                // Verificar se a soma dos divisores é maior que n
                if (divisor.add(outroDivisor).compareTo(n) > 0) {
                    return false; // Definitivamente não é perfeito
                }
            }
        }

        // Para números ímpares, não podemos determinar com certeza
        // Retornar false baseado na conjectura
        return false;
    }

    private boolean ehNumeroPerfeitoConhecido(BigInteger n) {
        // Lista dos números perfeitos conhecidos
        String[] perfeitosConhecidos = {
            "6", "28", "496", "8128", "33550336", "8589869056",
            "137438691328", "2305843008139952128",
            "2658455991569831744654692615953842176",
            "191561942608236107294793378084303638130997321548169216"
        };

        String nStr = n.toString();
        for (String perfeito : perfeitosConhecidos) {
            if (nStr.equals(perfeito)) {
                return true;
            }
        }

        return false;
    }

    private java.util.List<BigInteger> buscarNumerosPerfeitosPorDigitos(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Para números muito grandes, usar estratégia baseada no teorema de Euclides
        if (digitos > 100) {
            return buscarNumerosPerfeitosPorDigitosOtimizado(digitos, maxTentativas);
        }

        // Para números pequenos, busca sequencial
        BigInteger inicio = BigInteger.valueOf(10).pow(digitos - 1);
        BigInteger fim = BigInteger.valueOf(10).pow(digitos).subtract(BigInteger.ONE);

        int tentativas = 0;
        BigInteger atual = inicio;

        while (atual.compareTo(fim) <= 0 && tentativas < maxTentativas) {
            if (verificarNumeroPerfeito(atual)) {
                candidatos.add(atual);
                if (candidatos.size() >= 10) break; // Limitar resultados
            }

            atual = atual.add(BigInteger.ONE);
            tentativas++;

            // Progresso a cada 1000 tentativas
            if (tentativas % 1000 == 0) {
                Log.d(TAG, "Buscando números perfeitos: " + tentativas + "/" + maxTentativas);
            }
        }

        return candidatos;
    }

    private java.util.List<BigInteger> buscarNumerosPerfeitosPorDigitosOtimizado(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Usar o teorema de Euclides para gerar candidatos
        // Buscar primos de Mersenne que gerem números com o número de dígitos desejado

        int tentativas = 0;
        int p = 2; // Começar com o menor primo

        while (tentativas < maxTentativas) {
            try {
                // Verificar se p é primo
                if (ehPrimo(p)) {
                    // Calcular M(p) = 2^p - 1
                    BigInteger mersenne = BigInteger.valueOf(2).pow(p).subtract(BigInteger.ONE);

                    // Verificar se M(p) é primo
                    if (ehPrimoBigIntegerOtimizado(mersenne)) {
                        // Calcular 2^(p-1) × M(p)
                        BigInteger candidato = BigInteger.valueOf(2).pow(p - 1).multiply(mersenne);
                        int digitosCandidato = candidato.toString().length();

                        if (digitosCandidato == digitos) {
                            candidatos.add(candidato);
                            if (candidatos.size() >= 5) break; // Limitar resultados
                        }
                    }
                }

                p++;
                tentativas++;

                // Progresso a cada 100 tentativas
                if (tentativas % 100 == 0) {
                    Log.d(TAG, "Buscando números perfeitos otimizado: " + tentativas + "/" + maxTentativas + ", p=" + p);
                }

            } catch (Exception e) {
                p++;
                tentativas++;
                continue;
            }
        }

        return candidatos;
    }

    private java.util.List<BigInteger> gerarCandidatosNumerosPerfeitos(int digitos, int quantidade) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Gerar candidatos usando o teorema de Euclides
        int p = 2;
        int encontrados = 0;

        while (encontrados < quantidade && p < 1000000) { // Limite de segurança
            try {
                // Verificar se p é primo
                if (ehPrimo(p)) {
                    // Calcular M(p) = 2^p - 1
                    BigInteger mersenne = BigInteger.valueOf(2).pow(p).subtract(BigInteger.ONE);

                    // Verificar se M(p) é primo
                    if (ehPrimoBigIntegerOtimizado(mersenne)) {
                        // Calcular 2^(p-1) × M(p)
                        BigInteger candidato = BigInteger.valueOf(2).pow(p - 1).multiply(mersenne);
                        int digitosCandidato = candidato.toString().length();

                        if (digitosCandidato == digitos) {
                            candidatos.add(candidato);
                            encontrados++;

                            if (encontrados >= quantidade) break;
                        }
                    }
                }

                p++;

            } catch (Exception e) {
                p++;
                continue;
            }
        }

        return candidatos;
    }



    // Métodos especializados para busca de números perfeitos ÍMPARES
    private java.util.List<BigInteger> buscarNumerosPerfeitosImparesPorDigitos(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        resultadoView.setText("🔍 Buscando números perfeitos ÍMPARES com " + digitos + " dígitos...\n" +
            "Esta é uma busca revolucionária na matemática!");

        // Para números muito grandes, usar estratégias especializadas
        if (digitos > 100) {
            return buscarNumerosPerfeitosImparesOtimizado(digitos, maxTentativas);
        }

        // Para números pequenos, busca sequencial focada em ímpares
        BigInteger inicio = BigInteger.valueOf(10).pow(digitos - 1);
        if (inicio.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            inicio = inicio.add(BigInteger.ONE); // Garantir que começa com ímpar
        }

        BigInteger fim = BigInteger.valueOf(10).pow(digitos).subtract(BigInteger.ONE);
        if (fim.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            fim = fim.subtract(BigInteger.ONE); // Garantir que termina com ímpar
        }

        int tentativas = 0;
        BigInteger atual = inicio;

        while (atual.compareTo(fim) <= 0 && tentativas < maxTentativas) {
            // Verificar apenas números ímpares
            if (atual.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                if (verificarNumeroPerfeito(atual)) {
                    candidatos.add(atual);
                    if (candidatos.size() >= 5) break; // Limitar resultados
                }
            }

            atual = atual.add(BigInteger.TWO); // Pular para o próximo ímpar
            tentativas++;

            // Progresso a cada 1000 tentativas
            if (tentativas % 1000 == 0) {
                final String progresso = "🔍 Buscando números perfeitos ÍMPARES: " + tentativas + "/" + maxTentativas +
                    "\nTestando: " + atual + " (" + atual.toString().length() + " dígitos)";
                runOnUiThread(() -> resultadoView.setText(progresso));
            }
        }

        return candidatos;
    }

    private java.util.List<BigInteger> buscarNumerosPerfeitosImparesOtimizado(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        resultadoView.setText("🚀 Busca OTIMIZADA para números perfeitos ÍMPARES com " + digitos + " dígitos...\n" +
            "Usando estratégias matemáticas avançadas!");

        // Estratégias especializadas para números muito grandes
        int tentativas = 0;

        // Estratégia 1: Números com propriedades especiais de divisibilidade
        candidatos.addAll(buscarImparesComPropriedadesEspeciais(digitos, maxTentativas / 3));
        tentativas += maxTentativas / 3;

        if (candidatos.size() >= 5) return candidatos;

        // Estratégia 2: Candidatos baseados em conjecturas matemáticas
        candidatos.addAll(buscarImparesBaseadosEmConjecturas(digitos, maxTentativas / 3));
        tentativas += maxTentativas / 3;

        if (candidatos.size() >= 5) return candidatos;

        // Estratégia 3: Estratégias probabilísticas para números extremamente grandes
        candidatos.addAll(buscarImparesProbabilistico(digitos, maxTentativas - tentativas));

        return candidatos;
    }

    private java.util.List<BigInteger> buscarImparesComPropriedadesEspeciais(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Propriedade: Números perfeitos ímpares devem ter pelo menos 8 fatores primos distintos
        // E devem ser da forma p^a × q^b × r^c × ... onde p, q, r são primos ímpares

        try {
            // Gerar candidatos com estrutura específica
            SecureRandom rnd = new SecureRandom();
            int tentativas = 0;

            while (tentativas < maxTentativas) {
                // Gerar número com estrutura de fatores primos
                BigInteger candidato = gerarCandidatoImparEstruturado(digitos, rnd);

                if (candidato != null && candidato.toString().length() == digitos) {
                    // Verificar se tem potencial (propriedades parciais)
                    if (temPotencialNumeroPerfeitoImpar(candidato)) {
                        candidatos.add(candidato);
                        if (candidatos.size() >= 3) break;
                    }
                }

                tentativas++;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro na busca com propriedades especiais", e);
        }

        return candidatos;
    }

    private java.util.List<BigInteger> buscarImparesBaseadosEmConjecturas(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Conjectura: Números perfeitos ímpares devem ser da forma p^a × q^b × r^c × ...
        // onde p, q, r são primos ímpares e a, b, c são expoentes específicos

        try {
            int tentativas = 0;

            // Testar diferentes combinações de fatores primos
            for (int fatores = 8; fatores <= 12 && tentativas < maxTentativas; fatores++) {
                for (int tentativa = 0; tentativa < maxTentativas / 5 && tentativas < maxTentativas; tentativa++) {

                    BigInteger candidato = gerarCandidatoImparConjectura(digitos, fatores);

                    if (candidato != null && candidato.toString().length() == digitos) {
                        if (temPotencialNumeroPerfeitoImpar(candidato)) {
                            candidatos.add(candidato);
                            if (candidatos.size() >= 3) break;
                        }
                    }

                    tentativas++;
                }

                if (candidatos.size() >= 3) break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro na busca baseada em conjecturas", e);
        }

        return candidatos;
    }

    private java.util.List<BigInteger> buscarImparesProbabilistico(int digitos, int maxTentativas) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Estratégia probabilística para números extremamente grandes
        // Usar propriedades estatísticas e amostragem inteligente

        try {
            SecureRandom rnd = new SecureRandom();
            int tentativas = 0;

            while (tentativas < maxTentativas) {
                // Gerar candidato usando estratégia probabilística
                BigInteger candidato = gerarCandidatoImparProbabilistico(digitos, rnd);

                if (candidato != null && candidato.toString().length() == digitos) {
                    // Verificação rápida de potencial
                    if (verificacaoRapidaPotencialImpar(candidato)) {
                        candidatos.add(candidato);
                        if (candidatos.size() >= 2) break;
                    }
                }

                tentativas++;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro na busca probabilística", e);
        }

        return candidatos;
    }

    private java.util.List<BigInteger> gerarCandidatosNumerosPerfeitosImpares(int digitos, int quantidade) {
        java.util.List<BigInteger> candidatos = new java.util.ArrayList<>();

        // Inicialização da geração de candidatos

        try {
            SecureRandom rnd = new SecureRandom();
            int tentativas = 0;
            int maxTentativas = quantidade * 1000; // 1000 tentativas por candidato

            while (candidatos.size() < quantidade && tentativas < maxTentativas) {

                // Estratégia 1: Candidatos estruturados
                if (tentativas % 3 == 0) {
                    BigInteger candidato = gerarCandidatoImparEstruturado(digitos, rnd);
                    if (candidato != null && !candidatos.contains(candidato)) {
                        candidatos.add(candidato);
                    }
                }
                // Estratégia 2: Baseados em conjecturas
                else if (tentativas % 3 == 1) {
                    BigInteger candidato = gerarCandidatoImparConjectura(digitos, 8 + rnd.nextInt(5));
                    if (candidato != null && !candidatos.contains(candidato)) {
                        candidatos.add(candidato);
                    }
                }
                // Estratégia 3: Probabilístico
                else {
                    BigInteger candidato = gerarCandidatoImparProbabilistico(digitos, rnd);
                    if (candidato != null && !candidatos.contains(candidato)) {
                        candidatos.add(candidato);
                    }
                }

                tentativas++;

                // Progresso a cada 100 tentativas
                if (tentativas % 100 == 0) {
                    final String progresso = "🚀 Gerando candidatos ÍMPARES: " + candidatos.size() + "/" + quantidade +
                        " (tentativas: " + tentativas + ")";
                    runOnUiThread(() -> resultadoView.setText(progresso));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro na geração de candidatos ímpares", e);
        }

        return candidatos;
    }

    // Métodos auxiliares para geração de candidatos ímpares
    private BigInteger gerarCandidatoImparEstruturado(int digitos, SecureRandom rnd) {
        try {
            // Gerar número com estrutura específica de fatores primos
            // Propriedade: deve ter pelo menos 8 fatores primos distintos

            BigInteger candidato = BigInteger.ONE;
            int fatoresAdicionados = 0;

            // Adicionar fatores primos ímpares
            for (int i = 0; i < 8 && fatoresAdicionados < 8; i++) {
                BigInteger primo = BigInteger.probablePrime(10 + rnd.nextInt(20), rnd);
                int expoente = 1 + rnd.nextInt(3);

                candidato = candidato.multiply(primo.pow(expoente));
                fatoresAdicionados++;

                // Verificar se já atingiu o tamanho desejado
                if (candidato.toString().length() >= digitos) {
                    break;
                }
            }

            // Ajustar para o número exato de dígitos
            while (candidato.toString().length() < digitos) {
                BigInteger primo = BigInteger.probablePrime(5 + rnd.nextInt(10), rnd);
                candidato = candidato.multiply(primo);
            }

            // Garantir que é ímpar
            if (candidato.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                candidato = candidato.add(BigInteger.ONE);
            }

            return candidato;

        } catch (Exception e) {
            return null;
        }
    }

    private BigInteger gerarCandidatoImparConjectura(int digitos, int numFatores) {
        try {
            // Gerar candidato baseado em conjecturas matemáticas
            // Estrutura: p^a × q^b × r^c × ... com expoentes específicos

            SecureRandom rnd = new SecureRandom();
            BigInteger candidato = BigInteger.ONE;

            // Usar primos conhecidos para maior confiabilidade
            int[] primosConhecidos = {3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};

            for (int i = 0; i < numFatores && i < primosConhecidos.length; i++) {
                BigInteger primo = BigInteger.valueOf(primosConhecidos[i]);
                int expoente = 1 + rnd.nextInt(4); // Expoente entre 1 e 4

                candidato = candidato.multiply(primo.pow(expoente));

                if (candidato.toString().length() >= digitos) {
                    break;
                }
            }

            // Ajustar para o número exato de dígitos
            while (candidato.toString().length() < digitos) {
                BigInteger primo = BigInteger.probablePrime(5 + rnd.nextInt(15), rnd);
                candidato = candidato.multiply(primo);
            }

            // Garantir que é ímpar
            if (candidato.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                candidato = candidato.add(BigInteger.ONE);
            }

            return candidato;

        } catch (Exception e) {
            return null;
        }
    }

    private BigInteger gerarCandidatoImparProbabilistico(int digitos, SecureRandom rnd) {
        try {
            // Estratégia probabilística para números muito grandes
            // Usar propriedades estatísticas e amostragem inteligente

            // Gerar número base com estrutura aleatória
            BigInteger candidato = BigInteger.probablePrime(digitos * 3 / 4, rnd);

            // Adicionar fatores para atingir o tamanho desejado
            while (candidato.toString().length() < digitos) {
                BigInteger fator = BigInteger.probablePrime(5 + rnd.nextInt(20), rnd);
                candidato = candidato.multiply(fator);
            }

            // Ajustar para o número exato de dígitos
            while (candidato.toString().length() > digitos) {
                candidato = candidato.divide(BigInteger.valueOf(2));
            }

            // Garantir que é ímpar
            if (candidato.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                candidato = candidato.add(BigInteger.ONE);
            }

            return candidato;

        } catch (Exception e) {
            return null;
        }
    }

    private boolean temPotencialNumeroPerfeitoImpar(BigInteger n) {
        // Verificação SIMPLIFICADA de potencial para números ímpares
        // Evitar cálculos pesados que podem causar crash

        try {
            // Propriedade 1: Deve ser ímpar
            if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                return false;
            }

            // Propriedade 2: Verificação SIMPLIFICADA de fatores (evitar loop pesado)
            // Em vez de contar todos os fatores, fazer verificação básica
            if (n.compareTo(BigInteger.valueOf(1000)) <= 0) {
                // Para números pequenos, verificação rápida
                return n.mod(BigInteger.valueOf(3)).equals(BigInteger.ZERO) ||
                       n.mod(BigInteger.valueOf(5)).equals(BigInteger.ZERO) ||
                       n.mod(BigInteger.valueOf(7)).equals(BigInteger.ZERO);
            }

            // Propriedade 3: Para números grandes, verificação muito básica
            if (n.toString().length() < 10) {
                return false; // Números perfeitos ímpares devem ser muito grandes
            }

            // Propriedade 4: Verificação rápida de divisibilidade (apenas alguns primos)
            int[] primosBasicos = {3, 5, 7, 11, 13, 17, 19, 23, 29, 31};
            for (int primo : primosBasicos) {
                if (n.mod(BigInteger.valueOf(primo)).equals(BigInteger.ZERO)) {
                    return true; // Tem pelo menos um fator primo
                }
            }

            return false; // Se não é divisível por primos básicos, provavelmente é primo

        } catch (Exception e) {
            Log.e(TAG, "Erro em temPotencialNumeroPerfeitoImpar: " + e.getMessage());
            return false;
        }
    }

    private boolean verificacaoRapidaPotencialImpar(BigInteger n) {
        // Verificação rápida de potencial para números ímpares
        try {
            // Testar divisibilidade por pequenos primos ímpares
            int[] primosTeste = {3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};

            for (int primo : primosTeste) {
                if (n.mod(BigInteger.valueOf(primo)).equals(BigInteger.ZERO)) {
                    // Se é divisível por um primo pequeno, tem potencial
                    return true;
                }
            }

            // Se não é divisível por primos pequenos, pode ser primo
            // Números primos não podem ser perfeitos (exceto 2, que é par)
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private int contarFatoresPrimosDistintos(BigInteger n) {
        // Contagem SIMPLIFICADA de fatores primos para evitar crash
        try {
            if (n.compareTo(BigInteger.valueOf(1000)) <= 0) {
                // Para números pequenos, contagem completa
                java.util.Set<BigInteger> fatores = new java.util.HashSet<>();
                BigInteger temp = n;

                // Testar apenas até 100 para evitar loops longos
                for (int i = 2; i <= 100 && temp.compareTo(BigInteger.ONE) > 0; i++) {
                    while (temp.mod(BigInteger.valueOf(i)).equals(BigInteger.ZERO)) {
                        fatores.add(BigInteger.valueOf(i));
                        temp = temp.divide(BigInteger.valueOf(i));
                    }
                }

                // Se ainda há fatores restantes, contar como 1 fator adicional
                if (temp.compareTo(BigInteger.ONE) > 0) {
                    fatores.add(temp);
                }

                return fatores.size();
            } else {
                // Para números grandes, estimativa baseada em propriedades
                int estimativa = 0;

                // Verificar divisibilidade por primos pequenos
                int[] primosComuns = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47};
                for (int primo : primosComuns) {
                    if (n.mod(BigInteger.valueOf(primo)).equals(BigInteger.ZERO)) {
                        estimativa++;
                    }
                }

                // Para números muito grandes, assumir estrutura complexa
                if (n.toString().length() > 100) {
                    estimativa += 3; // Estimativa conservadora
                }

                return estimativa;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro em contarFatoresPrimosDistintos: " + e.getMessage());
            return 0;
        }
    }

    private java.util.List<BigInteger> encontrarDivisores(BigInteger n) {
        java.util.List<BigInteger> divisores = new java.util.ArrayList<>();

        try {
            if (n.compareTo(BigInteger.ONE) <= 0) {
                return divisores;
            }

            BigInteger sqrt = n.sqrt();

            // Encontrar divisores até a raiz quadrada
            for (BigInteger i = BigInteger.ONE; i.compareTo(sqrt) <= 0; i = i.add(BigInteger.ONE)) {
                if (n.mod(i).equals(BigInteger.ZERO)) {
                    if (i.compareTo(n) < 0) {
                        divisores.add(i);
                    }

                    BigInteger outroDivisor = n.divide(i);
                    if (outroDivisor.compareTo(i) > 0 && outroDivisor.compareTo(n) < 0) {
                        divisores.add(outroDivisor);
                    }
                }
            }

            // Ordenar divisores
            java.util.Collections.sort(divisores);

        } catch (Exception e) {
            // Em caso de erro, retornar lista vazia
        }

        return divisores;
    }

    private java.util.List<BigInteger> buscarSequencialNumerosPerfeitosImpares(int digitos) {
        java.util.List<BigInteger> numerosPerfeitosEncontrados = new java.util.ArrayList<>();

        try {
            // Inicializar progresso
            SharedPreferences prefs = getSharedPreferences("numeros_perfeitos_sequencial", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Carregar progresso salvo ou iniciar novo
            long ultimaExecucao = prefs.getLong("ultima_execucao", 0);
            int tentativasExecutadas = prefs.getInt("tentativas_executadas", 0);
            BigInteger ultimoNumero = new BigInteger(prefs.getString("ultimo_numero", "0"));

            // SEMPRE iniciar do início com os dígitos especificados pelo usuário
            ultimoNumero = BigInteger.valueOf(10).pow(digitos - 1);
            if (ultimoNumero.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                ultimoNumero = ultimoNumero.add(BigInteger.ONE); // Garantir que é ímpar
            }
            tentativasExecutadas = 0;

            // Resetar progresso anterior para sempre começar do início
            Log.d(TAG, "Iniciando busca sequencial do início com " + digitos + " dígitos: " + ultimoNumero);

            long startTime = System.currentTimeMillis();
            long lastProgressUpdate = startTime;
            int tentativas = 0;

            // Criar cópias finais das variáveis para o lambda ANTES do loop
            final BigInteger[] ultimoNumeroRef = {ultimoNumero};
            final int[] tentativasRef = {tentativas};
            final int[] tentativasExecutadasRef = {tentativasExecutadas};
            final long startTimeRef = startTime;

            // Ativar flag de busca
            buscaSequencialAtiva = true;

            // Callback para atualizar progresso na UI
            Runnable updateProgress = () -> {
                runOnUiThread(() -> {
                    try {
                        // Usar as referências finais
                        final BigInteger numeroAtual = ultimoNumeroRef[0];
                        final int tentativasAtuais = tentativasRef[0];
                        final int tentativasTotais = tentativasExecutadasRef[0];
                        final long tempoInicio = startTimeRef;

                        // Encontrar a área de resultado
                        View contentView = findViewById(android.R.id.content);
                        if (contentView != null) {
                            // Buscar por TextView que contenha o resultado
                            java.util.List<TextView> textViews = new java.util.ArrayList<>();
                            findTextViews(contentView, textViews);

                            for (TextView tv : textViews) {
                                if (tv.getText().toString().contains("BUSCA SEQUENCIAL CONTÍNUA")) {
                                    StringBuilder progressText = new StringBuilder();
                                    progressText.append("BUSCA SEQUENCIAL CONTÍNUA - NÚMEROS PERFEITOS ÍMPARES\n");
                                    progressText.append("====================================================\n\n");
                                    progressText.append("🔄 BUSCA EM ANDAMENTO...\n");
                                    progressText.append("========================\n\n");
                                    progressText.append("📊 PROGRESSO ATUAL:\n");
                                    progressText.append("   • Número sendo analisado: ").append(numeroAtual).append("\n");
                                    progressText.append("   • Dígitos: ").append(numeroAtual.toString().length()).append("\n");
                                    progressText.append("   • Tentativas realizadas: ").append(tentativasAtuais).append(" (CONTÍNUAS)\n");
                                    progressText.append("   • Tentativas totais: ").append(tentativasTotais).append("\n");
                                    progressText.append("   • Tempo decorrido: ")
                                        .append(String.format(Locale.getDefault(), "%.1f", (System.currentTimeMillis() - tempoInicio) / 1000.0))
                                        .append(" s\n");
                                    progressText.append("   • Velocidade: ").append(tentativasAtuais > 0 ? String.format("%.2f", (double)tentativasAtuais / ((System.currentTimeMillis() - tempoInicio) / 1000.0)) : "0").append(" números/s\n\n");
                                    progressText.append("🔍 STATUS DA BUSCA:\n");
                                    progressText.append("   • Modo: Busca sequencial CONTÍNUA E INFINITA\n");
                                    progressText.append("   • Foco: Apenas números ÍMPARES\n");
                                    progressText.append("   • Estratégia: Análise número por número\n");
                                    progressText.append("   • Objetivo: Encontrar número perfeito ímpar\n");
                                    progressText.append("   • Duração: ATÉ ENCONTRAR OU VOCÊ PARAR\n\n");
                                    progressText.append("⏰ Última atualização: ").append(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                                    progressText.append("💾 Progresso salvo automaticamente\n");
                                    progressText.append("🛑 Para parar: Use o botão 'PARAR BUSCA SEQUENCIAL' acima\n");

                                    tv.setText(progressText.toString());
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao atualizar progresso: " + e.getMessage());
                    }
                });
            };

            // Busca contínua infinita até encontrar ou ser interrompida
            while (numerosPerfeitosEncontrados.isEmpty() && buscaSequencialAtiva) {
                try {
                    // Verificar se o número atual é ímpar
                    if (ultimoNumero.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                        // Verificar se tem potencial (evitar números muito simples)
                        if (temPotencialNumeroPerfeitoImpar(ultimoNumero)) {
                            // VERIFICAÇÃO COMPLETA - é aqui que a mágica acontece!
                            boolean ehPerfeito = verificarNumeroPerfeito(ultimoNumero);

                            if (ehPerfeito) {
                                // 🚨 REVOLUÇÃO MATEMÁTICA!
                                numerosPerfeitosEncontrados.add(ultimoNumero);
                                Log.d(TAG, "NÚMERO PERFEITO ÍMPAR ENCONTRADO: " + ultimoNumero);

                                // Salvar descoberta imediatamente
                                editor.putString("numero_perfeito_encontrado", ultimoNumero.toString());
                                editor.putLong("timestamp_descoberta", System.currentTimeMillis());
                                editor.apply();

                                break; // Parar a busca - encontramos um!
                            }
                        }
                    }

                    // Próximo número ímpar
                    ultimoNumero = ultimoNumero.add(BigInteger.TWO);
                    tentativas++;
                    tentativasExecutadas++;

                    // Atualizar as referências para o lambda
                    ultimoNumeroRef[0] = ultimoNumero;
                    tentativasRef[0] = tentativas;
                    tentativasExecutadasRef[0] = tentativasExecutadas;

                    // Atualizar progresso a cada 30 segundos
                    if (System.currentTimeMillis() - lastProgressUpdate > 30000) { // 30 segundos
                        updateProgress.run();
                        lastProgressUpdate = System.currentTimeMillis();
                    }

                    // Salvar progresso a cada 1000 tentativas
                    if (tentativas % 1000 == 0) {
                        editor.putString("ultimo_numero", ultimoNumero.toString());
                        editor.putInt("tentativas_executadas", tentativasExecutadas);
                        editor.putLong("ultima_execucao", System.currentTimeMillis());
                        editor.apply();

                        Log.d(TAG, "Progresso busca sequencial: " + tentativas +
                              " tentativas - Último número: " + ultimoNumero.toString().length() + " dígitos");
                    }

                    // Busca contínua infinita - sem timeout
                    // Continua até encontrar um número perfeito ou ser interrompida

                } catch (Exception e) {
                    Log.e(TAG, "Erro na verificação do número: " + e.getMessage());
                    ultimoNumero = ultimoNumero.add(BigInteger.TWO);
                    tentativas++;

                    // Atualizar as referências também no catch
                    ultimoNumeroRef[0] = ultimoNumero;
                    tentativasRef[0] = tentativas;
                }
            }

            // Salvar progresso final
            editor.putString("ultimo_numero", ultimoNumero.toString());
            editor.putInt("tentativas_executadas", tentativasExecutadas);
            editor.putLong("ultima_execucao", System.currentTimeMillis());
            editor.apply();

            // Esconder botão de parar
            runOnUiThread(() -> {
                if (btnPararBuscaSequencial != null) {
                    btnPararBuscaSequencial.setVisibility(View.GONE);
                }
            });

            Log.d(TAG, "Busca sequencial concluída: " + tentativas + " tentativas, " +
                  numerosPerfeitosEncontrados.size() + " números perfeitos encontrados");

        } catch (Exception e) {
            Log.e(TAG, "Erro na busca sequencial: " + e.getMessage());
        }

        return numerosPerfeitosEncontrados;
    }

    // Método auxiliar para encontrar TextViews
    private void findTextViews(View view, java.util.List<TextView> textViews) {
        if (view instanceof TextView) {
            textViews.add((TextView) view);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                findTextViews(viewGroup.getChildAt(i), textViews);
            }
        }
    }

    // Método para parar a busca sequencial
    private volatile boolean buscaSequencialAtiva = false;

    public void pararBuscaSequencial() {
        buscaSequencialAtiva = false;
        Log.d(TAG, "Busca sequencial interrompida pelo usuário");
    }

    // Método para verificar se a busca está ativa
    public boolean isBuscaSequencialAtiva() {
        return buscaSequencialAtiva;
    }

    // Métodos auxiliares para Segurança Digital

    private String getNivelSeguranca(int bits) {
        if (bits < 512) return "Muito baixo (inseguro)";
        if (bits < 1024) return "Baixo (descontinuado)";
        if (bits < 2048) return "Médio (mínimo recomendado)";
        if (bits < 3072) return "Alto (recomendado)";
        if (bits < 4096) return "Muito alto (segurança máxima)";
        return "Extremo (longo prazo)";
    }

    private String getTempoQuebra(int bits) {
        if (bits < 512) return "Minutos (inseguro)";
        if (bits < 1024) return "Dias a semanas";
        if (bits < 2048) return "Anos a décadas";
        if (bits < 3072) return "Séculos";
        if (bits < 4096) return "Milênios";
        return "Tempo geológico";
    }

    private String converterParaNumeros(String mensagem) {
        StringBuilder resultado = new StringBuilder();
        for (char c : mensagem.toCharArray()) {
            resultado.append((int) c).append(" ");
        }
        return resultado.toString().trim();
    }

    private String gerarMD5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    private String gerarSHA1(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    private String gerarSHA256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    private String gerarSHA512(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = md.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }

    private int calcularDiferencaHash(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) return 100;

        int diferencas = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                diferencas++;
            }
        }

        return (diferencas * 100) / hash1.length();
    }

    private String converterDeNumeros(String numeros) {
        StringBuilder resultado = new StringBuilder();
        String[] valores = numeros.split(" ");
        for (String valor : valores) {
            try {
                int codigo = Integer.parseInt(valor.trim());
                resultado.append((char) codigo);
            } catch (NumberFormatException e) {
                resultado.append("?");
            }
        }
        return resultado.toString();
    }
}
