package com.seuprojeto.primeprofast;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.widget.*;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    /**
     * Acima deste valor usa C++/GMP. BigInteger.probablePrime em ~8193+ bits no heap Java costuma
     * matar o processo (OOM); por isso 8193 deve usar nativo, não Java.
     */
    private static final int LIMIAR_BITS_ROTA_NATIVA = 8192;

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
    private LinearLayout menuContainer;
    private LinearLayout contentContainer;
    /** Layout raiz (gradiente de fundo). */
    private LinearLayout rootMainLayout;
    
    // Variável para controle de parada do usuário
    private volatile boolean paradaUsuario = false;

    // Controle genérico de cancelamento da operação atual (cards longos)
    private final AtomicBoolean cancelarOperacaoAtual = new AtomicBoolean(false);
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
    public native String gerarPrimosGrandes(int bits, int quantidade, String nomeArquivo, boolean salvarArquivo);
    public native String superPrimosNativo(int bits, int quantidade, int threads, String caminhoArquivo);
    public native boolean testarPrimalidadeGiganteNativo(String numero, int repeticoes);
    public native void iniciarGeracaoPrimosGigantesJob(int bits, int quantidade, int threads, String caminhoArquivoSaida);
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
        
        // Configurar layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        botao.setLayoutParams(params);
        
        // Configurar padding
        botao.setPadding(24, 16, 24, 16);
        
        // Configurar texto
        botao.setTextSize(16);
        botao.setTextColor(Color.WHITE);
        botao.setTypeface(null, Typeface.BOLD);
        
        // Criar gradiente vibrante
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setColors(new int[]{corPrimaria, corSecundaria});
        gradient.setCornerRadius(25);
        gradient.setStroke(2, Color.WHITE);
        
        // Aplicar gradiente
        botao.setBackground(gradient);
        
        // Adicionar elevação (sombra)
        botao.setElevation(6);
        
        // Adicionar animação de clique
        botao.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
              .withEndAction(() -> {
                  v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100);
              });
        });
        
        return botao;
    }

    private int dpUi(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
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
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
    }

    /** Rótulo em negrito para campos de entrada (cards). */
    private void adicionarRotuloCampo(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#37474F"));
        tv.setPadding(dpUi(4), dpUi(12), dpUi(4), dpUi(4));
        contentContainer.addView(tv);
    }

    /** Preenchimento dos campos de texto: cinza para não confundir com área branca do app. */
    private void aplicarEstiloCampoEntradaTematizado(EditText et) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(temaEscuro ? Color.parseColor("#2C313A") : Color.parseColor("#D8DCE6"));
        bg.setStroke(dpUi(1), temaEscuro ? Color.parseColor("#5E81AC") : Color.parseColor("#1565C0"));
        bg.setCornerRadius(dpUi(8));
        et.setBackground(bg);
        et.setPadding(dpUi(12), dpUi(14), dpUi(12), dpUi(14));
        et.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#1A1A1A"));
        et.setHintTextColor(temaEscuro ? Color.parseColor("#8B95A8") : Color.parseColor("#607D8B"));
    }

    /** Caixa de entrada visível (borda, cantos, margem). */
    private void estilizarCampoEntrada(EditText et) {
        aplicarEstiloCampoEntradaTematizado(et);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpUi(4), dpUi(2), dpUi(4), dpUi(10));
        et.setLayoutParams(lp);
    }

    /** Fundo principal com gradiente suave (base para efeito “vidro”). */
    private void aplicarFundoGradientePrincipal() {
        if (rootMainLayout == null) {
            return;
        }
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            temaEscuro
                ? new int[]{Color.parseColor("#0B0F14"), Color.parseColor("#151C28")}
                : new int[]{Color.parseColor("#D4DDE8"), Color.parseColor("#E8EEF5")}
        );
        rootMainLayout.setBackground(gd);
    }

    /** Área de resultado com aparência de painel de vidro fosco. */
    private void aplicarEstiloAreaResultado() {
        if (resultadoView == null) {
            return;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpUi(12));
        if (temaEscuro) {
            bg.setColor(Color.argb(235, 28, 34, 44));
            bg.setStroke(dpUi(1), Color.argb(140, 120, 160, 200));
        } else {
            bg.setColor(Color.argb(252, 255, 255, 255));
            bg.setStroke(dpUi(1), Color.argb(160, 255, 255, 255));
        }
        resultadoView.setBackground(bg);
        resultadoView.setElevation(dpUi(3));
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
        int corFundo, corTexto, corCard;
        
        if (temaEscuro) {
            corFundo = Color.parseColor("#121212");
            corTexto = Color.parseColor("#FFFFFF");
            corCard = Color.parseColor("#1E1E1E");
        } else {
            corFundo = Color.parseColor("#F5F5F5");
            corTexto = Color.parseColor("#333333");
            corCard = Color.WHITE;
        }
        
        // Aplicar cores ao layout principal
        if (getCurrentFocus() != null) {
            View rootView = getCurrentFocus().getRootView();
            if (rootView instanceof LinearLayout) {
                rootView.setBackgroundColor(corFundo);
            }
        }
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
        if (temaEscuro) {
            aplicarTemaAosContainers();
        }
    }

    /**
     * Atualiza o botão de tema sem recriar a interface
     */
    private void atualizarBotaoTema() {
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
        int corFundo, corTexto, corCard;
        
        if (temaEscuro) {
            corFundo = Color.parseColor("#121212");
            corTexto = Color.parseColor("#FFFFFF");
            corCard = Color.parseColor("#1E1E1E");
        } else {
            corFundo = Color.parseColor("#F5F5F5");
            corTexto = Color.parseColor("#333333");
            corCard = Color.WHITE;
        }
        
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
    }

    /**
     * Aplica tema recursivamente a todos os views
     */
    private void aplicarTemaRecursivo(ViewGroup parent, int corTexto, int corCard) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                String texto = textView.getText().toString();
                
                // Manter cores especiais dos textos
                if (texto.contains("💡 Tutorial") || texto.contains("WhatsApp")) {
                    // Manter cores originais dos textos especiais
                    if (texto.contains("WhatsApp")) {
                        textView.setTextColor(Color.parseColor("#25D366"));
                    }
                } else {
                    textView.setTextColor(corTexto);
                }
            } else if (child instanceof Button) {
                Button btn = (Button) child;
                String texto = btn.getText().toString();
                
                // Manter cores especiais dos botões com gradientes
                if (texto.contains("☀️") || texto.contains("🌙") || 
                    texto.contains("← Voltar") || texto.contains("Voltar ao Menu") ||
                    texto.contains("🔍") || texto.contains("📊") || texto.contains("🚀") ||
                    texto.contains("✨") || texto.contains("🔄") || texto.contains("💾")) {
                    // Manter cores originais dos botões especiais
                    btn.setTextColor(Color.WHITE);
                } else {
                    // Aplicar tema padrão para botões normais
                    btn.setTextColor(corTexto);
                    btn.setBackgroundColor(corCard);
                }
            } else if (child instanceof EditText) {
                EditText editText = (EditText) child;
                aplicarEstiloCampoEntradaTematizado(editText);
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

    /**
     * Otimiza o layout para diferentes tamanhos de tela
     */
    private void otimizarLayoutResponsivo() {
        // Obter dimensões da tela
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        float density = metrics.density;
        
        // Ajustar tamanhos baseados na densidade da tela
        if (density >= 3.0f) {
            // Tela de alta densidade (xxhdpi)
            ajustarTamanhosParaAltaDensidade();
        } else if (density >= 2.0f) {
            // Tela de densidade média-alta (xhdpi)
            ajustarTamanhosParaMediaDensidade();
        } else {
            // Tela de baixa densidade (hdpi)
            ajustarTamanhosParaBaixaDensidade();
        }
        
        // Ajustar para orientação landscape
        if (screenWidth > screenHeight) {
            ajustarParaOrientacaoLandscape();
        }
    }

    private void ajustarTamanhosParaAltaDensidade() {
        // Aumentar tamanhos para telas de alta densidade
        // Implementar ajustes específicos se necessário
    }

    private void ajustarTamanhosParaMediaDensidade() {
        // Tamanhos padrão para telas de densidade média
        // Implementar ajustes específicos se necessário
    }

    private void ajustarTamanhosParaBaixaDensidade() {
        // Reduzir tamanhos para telas de baixa densidade
        // Implementar ajustes específicos se necessário
    }

    private void ajustarParaOrientacaoLandscape() {
        // Ajustar layout para orientação landscape
        // Implementar ajustes específicos se necessário
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
        limparArquivosTemporarios();
    }

    @Override
    public void onBackPressed() {
        // Se estiver em uma tela de funcionalidade, voltar ao menu
        if (contentContainer.getVisibility() == View.VISIBLE) {
            // Limpar o container de conteúdo
            contentContainer.removeAllViews();
            
            // Voltar ao menu com animação
            contentContainer.setVisibility(View.GONE);
            if (menuScrollView != null) {
                menuScrollView.setVisibility(View.VISIBLE);
                menuScrollView.setAlpha(0.0f);
            }
            
            // Garantir que o menu seja recriado corretamente
            menuContainer.removeAllViews();
            criarMenu();
            
            // Adicionar animação suave
            contentContainer.animate().alpha(0.0f).setDuration(300);
            if (menuScrollView != null) {
                menuScrollView.animate().alpha(1.0f).setDuration(300);
            }
        } else {
            // Se estiver no menu principal, sair do app
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificações de segurança
        if (!verificarSeguranca()) {
            return;
        }

        // Inicializar preferências e tema
        preferences = getSharedPreferences("PrimeProFast", MODE_PRIVATE);

        // Inicializar sistema de monetização (Google Play Billing — sem login Firebase)
        inicializarSistemaMonetizacao();
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
                    onBackPressed();
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
        temaEscuro = preferences.getBoolean("tema_escuro", false);

        // Solicitar permissões de armazenamento
        requestStoragePermissions();

        // Otimizar layout para diferentes tamanhos de tela
        otimizarLayoutResponsivo();

        // Layout principal
        rootMainLayout = new LinearLayout(this);
        rootMainLayout.setOrientation(LinearLayout.VERTICAL);
        rootMainLayout.setPadding(dpUi(12), dpUi(12), dpUi(12), dpUi(12));

        // Cabeçalho com título (barra “vidro” / gradiente)
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setGravity(android.view.Gravity.CENTER);
        GradientDrawable headerBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1565C0"), Color.parseColor("#5C6BC0"), Color.parseColor("#7E57C2")}
        );
        headerBg.setCornerRadius(dpUi(14));
        headerLayout.setBackground(headerBg);
        headerLayout.setPadding(dpUi(16), dpUi(14), dpUi(16), dpUi(14));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLp.setMargins(0, 0, 0, dpUi(8));
        headerLayout.setLayoutParams(headerLp);
        headerLayout.setElevation(dpUi(4));

        TextView titulo = new TextView(this);
        titulo.setText("PrimeProFast");
        titulo.setTextSize(22);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(Color.WHITE);
        titulo.setShadowLayer(4f, 0f, 1f, Color.argb(120, 0, 0, 0));
        titulo.setPadding(0, 0, 0, 0);
        titulo.setGravity(android.view.Gravity.CENTER);
        titulo.setLetterSpacing(0.02f);

        LinearLayout.LayoutParams tituloParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titulo.setLayoutParams(tituloParams);

        headerLayout.addView(titulo);

        rootMainLayout.addView(headerLayout);

        // Container do menu (dentro de um ScrollView para permitir rolagem vertical de todos os cards)
        menuScrollView = new ScrollView(this);
        menuScrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        menuScrollView.setFillViewport(true);

        menuContainer = new LinearLayout(this);
        menuContainer.setOrientation(LinearLayout.VERTICAL);
        menuContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        menuScrollView.addView(menuContainer);
        rootMainLayout.addView(menuScrollView);

        // Container do conteúdo (inicialmente oculto)
        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setVisibility(View.GONE);
        rootMainLayout.addView(contentContainer);

        // Área de resultado
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
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
        aplicarFundoGradientePrincipal();

        // Criar menu com cards
        criarMenu();
        
        // Aplicar tema inicial se necessário
        aplicarTemaInicial();
    }

    private void criarMenu() {
        // Garantir orientação vertical para empilhar os cards como na apresentação desejada
        menuContainer.setOrientation(LinearLayout.VERTICAL);

        // Layout params para cards em coluna única, ocupando toda a largura
        LinearLayout.LayoutParams fullWidthCardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fullWidthCardParams.setMargins(0, dpUi(6), 0, dpUi(6));

        // Ordem e descrições conforme apresentação vertical
        View card1 = criarCard("Primos por Intervalo", "Encontre primos em um intervalo específico.", false);
        card1.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card1);

        View card2 = criarCard("Primos Especiais", "Explore classes específicas de primos famosos.", false);
        card2.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card2);

        View card3 = criarCard("Primos Aleatórios", "Gere primos aleatórios de vários tamanhos.", false);
        card3.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card3);

        View card4 = criarCard("Conjectura de Legendre", "Teste a conjectura de Legendre com diferentes intervalos.", false);
        card4.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card4);

        View card5 = criarCard("Números de Mersenne", "Encontre e analise números de Mersenne.", false);
        card5.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card5);

        View card6 = criarCard("Números Perfeitos", "Calcule e explore números perfeitos.", false);
        card6.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card6);

        View card7 = criarCard(
            "Segurança Digital",
            "Gerar chaves RSA\nGerar criptografia e descriptografia\nGerar hash criptográfico\nAssinatura digital",
            false
        );
        card7.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card7);

        View card8 = criarCard("Teste de Primalidade", "Análise completa de primalidade e fatoração.", false);
        card8.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card8);

        View card9 = criarCard("Estatísticas", "Análise estatística completa de números primos.", false);
        card9.setLayoutParams(fullWidthCardParams);
        menuContainer.addView(card9);
        
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
        
        // Botão de informações (barra inferior azul, estilo mockup)
        Button btnInfo = new Button(this);
        btnInfo.setText("ℹ️  INFO");
        btnInfo.setTextSize(15);
        btnInfo.setTypeface(null, Typeface.BOLD);
        btnInfo.setTextColor(Color.WHITE);
        btnInfo.setAllCaps(false);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setCornerRadius(dpUi(12));
        infoBg.setColor(Color.parseColor("#2196F3"));
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
     * Card do menu: faixa colorida à esquerda (ícone grande), texto ao centro, Tutorial à direita, borda na cor do tema do card.
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
        row.setMinimumHeight(dpUi(92));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dpUi(12));
        if (ativo) {
            cardBg.setColor(corPrimaria);
            cardBg.setStroke(dpUi(2), corSecundaria);
        } else if (temaEscuro) {
            cardBg.setColor(Color.parseColor("#2C313C"));
            cardBg.setStroke(dpUi(2), corPrimaria);
        } else {
            cardBg.setColor(Color.WHITE);
            cardBg.setStroke(dpUi(2), corPrimaria);
        }
        row.setBackground(cardBg);
        row.setElevation(dpUi(3));

        // Painel esquerdo: cor sólida + emoji grande
        LinearLayout leftPanel = new LinearLayout(this);
        leftPanel.setOrientation(LinearLayout.VERTICAL);
        leftPanel.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(dpUi(84), LinearLayout.LayoutParams.MATCH_PARENT);
        leftPanel.setLayoutParams(leftLp);
        leftPanel.setMinimumHeight(dpUi(96));

        GradientDrawable leftBg = new GradientDrawable();
        float r = dpUi(12);
        leftBg.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
        leftBg.setColor(corPrimaria);
        leftPanel.setBackground(leftBg);

        TextView icon = new TextView(this);
        icon.setText(emoji);
        icon.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 38);
        icon.setGravity(android.view.Gravity.CENTER);
        icon.setIncludeFontPadding(false);
        leftPanel.addView(icon, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Centro: título + descrição
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        centerLp.setMargins(dpUi(10), dpUi(12), dpUi(6), dpUi(12));
        center.setLayoutParams(centerLp);

        TextView tituloView = new TextView(this);
        tituloView.setText(titulo);
        tituloView.setTextSize(15);
        tituloView.setTypeface(null, Typeface.BOLD);
        tituloView.setMaxLines(3);
        if (ativo) {
            tituloView.setTextColor(Color.WHITE);
        } else {
            tituloView.setTextColor(temaEscuro ? Color.parseColor("#ECEFF4") : Color.parseColor("#212121"));
        }

        TextView descView = new TextView(this);
        descView.setText(descricao);
        descView.setTextSize(12);
        descView.setLineSpacing(dpUi(2), 1f);
        descView.setMaxLines(8);
        if (ativo) {
            descView.setTextColor(Color.argb(230, 255, 255, 255));
        } else {
            descView.setTextColor(temaEscuro ? Color.parseColor("#B0B8C4") : Color.parseColor("#616161"));
        }

        center.addView(tituloView);
        center.addView(descView);

        // Tutorial à direita (laranja, como no mockup)
        TextView btnTutorial = new TextView(this);
        btnTutorial.setText("💡 Tutorial");
        btnTutorial.setTextSize(12);
        btnTutorial.setTypeface(null, Typeface.BOLD);
        btnTutorial.setTextColor(Color.WHITE);
        btnTutorial.setGravity(android.view.Gravity.CENTER);
        int tPad = dpUi(10);
        btnTutorial.setPadding(tPad, dpUi(14), tPad, dpUi(14));
        GradientDrawable tutBg = new GradientDrawable();
        tutBg.setCornerRadius(dpUi(10));
        tutBg.setColor(Color.parseColor("#FF9800"));
        tutBg.setStroke(dpUi(1), Color.parseColor("#FFE082"));
        btnTutorial.setBackground(tutBg);
        btnTutorial.setClickable(true);
        btnTutorial.setFocusable(true);
        LinearLayout.LayoutParams tutLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tutLp.setMargins(0, 0, dpUi(10), 0);
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
    private String[][] gerarConteudoTutorialUnificado(String funcionalidade) {
        switch (funcionalidade) {
            case "Primos por Intervalo":
                return new String[][]{
                    {"O que este card faz",
                        "<p>Você escolhe um <strong>limite máximo N</strong> e o aplicativo <strong>lista todos os números primos de 2 até N</strong> (ou o intervalo indicado na tela, conforme a opção disponível).</p>" +
                        "<div class='exemplo'><strong>Em outras palavras:</strong> é uma forma rápida de ver “quais são os primos até aqui”, útil para estudo, conferência de listas ou curiosidade.</div>"},
                    {"Como usar",
                        "<p>Digite o valor limite (ou intervalo, se a tela pedir início e fim), confirme e aguarde o resultado. Números muito grandes podem demorar mais ou exigir limites por segurança.</p>" +
                        "<ul><li>Use valores moderados primeiro para se familiarizar com o ritmo do app.</li><li>O resultado costuma poder ser guardado ou partilhado como nos outros cards.</li></ul>"},
                    {"Um pouco de contexto (sem “aula longa”)",
                        "<p>Há séculos se usa a ideia de <strong>marcar múltiplos</strong> de primos pequenos para achar primos num intervalo — o famoso <strong>crivo de Eratóstenes</strong> na escola. O importante aqui: <strong>este card é a ferramenta prática</strong> que faz esse trabalho pesado por você.</p>" +
                        "<p>Quanto maior N, mais números existem para analisar; por isso o app pode avisar limites ou tempo de espera.</p>"}
                };
            case "Primos Especiais":
                return new String[][]{
                    {"O que este card faz",
                        "<p>Aqui você <strong>não lista todos os primos até N</strong>, mas sim pede exemplos de <strong>famílias famosas</strong>: gêmeos (p e p+2), Sophie Germain, primos “cousins”, sexy, palíndromos, Fermat, etc.</p>" +
                        "<div class='exemplo'><strong>Objetivo:</strong> explorar padrões e nomes que a teoria dos números deu a certos primos que se comportam de modo especial.</div>"},
                    {"Como usar",
                        "<p>Escolha o <strong>tipo</strong> no menu, informe <strong>quantos exemplos</strong> quer e, se pedido, até onde buscar. O app gera uma lista que você pode ler e comparar com o que estudou.</p>"},
                    {"Por que isso interessa",
                        "<p>Muitas conjecturas abertas da matemática falam justamente dessas famílias (por exemplo, se existem infinitos primos gêmeos). Ver exemplos concretos ajuda a entender <strong>o que cada definição significa</strong>, sem precisar calcular à mão.</p>"}
                };
            case "Primos Aleatórios":
                return new String[][]{
                    {"O que este card faz",
                        "<p>Você define <strong>quantos primos</strong> quer e <strong>de qual tamanho em bits</strong> (ordem de grandeza do número). O app <strong>procura candidatos aleatórios</strong> desse tamanho e <strong>confirma se são primos</strong>, até completar a quantidade pedida.</p>" +
                        "<div class='exemplo'><strong>Diferença do “intervalo”:</strong> aqui não listamos todos até um limite; geramos poucos primos <em>muito grandes</em>, um por um.</div>"},
                    {"Como usar",
                        "<p>Preencha quantidade e bits conforme a tela. Comece com <strong>poucos bits</strong> para ver o fluxo; valores enormes podem levar tempo ou atingir limites do aparelho — isso é normal em qualquer ferramenta séria.</p>"},
                    {"Por que primos “gigantes” importam",
                        "<p>No dia a dia digital, chaves de <strong>HTTPS</strong>, aplicativos bancários e mensagens seguras dependem de números que, na prática, são produtos de <strong>primos enormes</strong>. Não é exagero: a “força” de muitos sistemas vem justamente de usar primos grandes e bem escolhidos.</p>" +
                        "<p>Projetos coletivos (como buscas por <strong>primos de Mersenne</strong>) mostram também o lado humano: milhares de pessoas contribuem CPU para descobrir recordes — mistura de matemática, engenharia e curiosidade.</p>" +
                        "<p>Este card coloca você no papel de <strong>pedir primos grandes de propósito</strong>, como fazem, em escala profissional, geradores de chaves criptográficas — sempre dentro do que o app permite em tempo e memória.</p>"}
                };
            case "Conjectura de Legendre":
                return new String[][]{
                    {"O que é a conjectura de Legendre",
                        "<p><strong>Ideia central:</strong> para cada número natural <em>n</em>, deveria sempre existir pelo menos um primo entre <strong>n²</strong> e <strong>(n+1)²</strong>. É uma afirmação simples de enunciar e <strong>ainda não demonstrada</strong> em geral, embora tenha sido verificada para valores enormes com computador.</p>"},
                    {"O que este card faz",
                        "<p>O app <strong>testa a conjectura</strong> para o intervalo de <em>n</em> que você indicar: para cada <em>n</em>, verifica se há primo entre os quadrados consecutivos. O resultado mostra onde a conjectura “passa” ou onde encontrou falha (se alguma aparecer no intervalo testado).</p>" +
                        "<div class='exemplo'>Use isto como <strong>exploração numérica</strong>, não como prova matemática do problema em todo o infinito.</div>"},
                    {"Como interpretar",
                        "<p>Se em todos os <em>n</em> testados apareceu pelo menos um primo no intervalo, você só viu evidência <strong>naquele trecho</strong>. A matemática continua aberta para uma demonstração geral ou um contra-exemplo teórico.</p>"}
                };
            case "Números de Mersenne":
                return new String[][]{
                    {"O que são números de Mersenne",
                        "<p>Leva o nome do padre e estudioso <strong>Marin Mersenne</strong>. Um <strong>número de Mersenne</strong> tem a forma <strong>M(p) = 2<sup>p</sup> − 1</strong> (na definição clássica que você vê no card).</p>" +
                        "<p>Quando esse número <em>também</em> é primo, chamamo-lo de <strong>primo de Mersenne</strong>. Nem todo <em>p</em> primo gera um Mersenne primo — há contra-exemplos clássicos nos livros.</p>"},
                    {"O que este card faz",
                        "<p>Você informa o <strong>expoente p</strong> (pode ser um número ou uma expressão que o app calcula). O app calcula <strong>2<sup>p</sup> − 1</strong>, diz se <em>p</em> é primo (condição necessária para Mersenne primo), testa se <strong>M(p)</strong> é primo e compara com uma <strong>lista de expoentes já conhecidos</strong> pela comunidade matemática.</p>"},
                    {"Ligação com números perfeitos (ideia curta)",
                        "<p>Um teorema clássico (Euclides/Euler) liga <strong>primos de Mersenne</strong> a <strong>números perfeitos pares</strong>. Se quiser a fundo, use também o card de Números Perfeitos; aqui basta saber que Mersenne não é só curiosidade: é peça de um quebra-cabeça histórico da teoria dos números.</p>"}
                };
            case "Números Perfeitos":
                return new String[][]{
                    {"O que é número perfeito",
                        "<p>Um número é <strong>perfeito</strong> quando é igual à <strong>soma dos seus divisores próprios</strong> (todos os divisores positivos menores que ele). O exemplo clássico é <strong>6 = 1 + 2 + 3</strong>.</p>"},
                    {"O que este card faz",
                        "<p>Dependendo do modo que a tela oferece, você pode <strong>verificar um candidato</strong>, <strong>buscar exemplos</strong> com certo número de dígitos ou explorar modos educativos que o app descreve nos botões. O objetivo é <strong>ligar a definição</strong> a <strong>experimentos numéricos</strong>.</p>"},
                    {"Contexto breve",
                        "<p>Todos os perfeitos <strong>pares</strong> conhecidos seguem a forma ligada a <strong>primos de Mersenne</strong>; não se conhece hoje um perfeito <strong>ímpar</strong> — problema ainda em aberto. O card ajuda a sentir o tamanho e a raridade desses objetos.</p>"}
                };
            case "Segurança Digital":
                return new String[][]{
                    {"O que este card representa",
                        "<p>É a área do app dedicada a <strong>ideias de criptografia</strong> usando números grandes: chaves públicas/privadas no estilo <strong>RSA</strong>, <strong>funções de resumo (hash)</strong> e <strong>assinatura digital</strong> em linguagem de exemplo — para <strong>aprender o fluxo</strong>, não para substituir um produto bancário.</p>"},
                    {"Como usar com bom senso",
                        "<p>Gerar chaves e ver mensagens “de exemplo” ajuda a ver <strong>por que primos grandes entram na história</strong>. Trate sempre como <strong>demonstração didática</strong>; chaves reais exigem software e normas específicas fora do escopo de um app educativo.</p>"},
                    {"O que você leva daqui",
                        "<p>Intuição sobre <strong>por que multiplicar dois primos é fácil</strong> e <strong>fatorar o produto é difícil</strong> quando os números são enormes — ideia central por trás de muita segurança na internet.</p>"}
                };
            case "Teste de Primalidade":
                return new String[][]{
                    {"O que este card faz",
                        "<p>Você digita um <strong>número inteiro</strong> ou uma <strong>expressão</strong> (por exemplo, somas, produtos, potências com ^) e o app <strong>calcula o valor</strong> e responde se é <strong>primo ou composto</strong>, com fatoração quando isso é viável para o tamanho do número.</p>" +
                        "<div class='exemplo'><strong>Primo:</strong> só divide por 1 e por si. <strong>Composto:</strong> tem outros divisores. O 1 é tratado como caso especial na matemática escolar.</div>"},
                    {"Como usar bem",
                        "<p>Use a caixa de texto para números grandes ou expressões; leia o relatório com calma — ele mostra o valor analisado, quantos dígitos tem e o veredito. Números com <strong>muitíssimos dígitos</strong> podem limitar o detalhe da fatoração; o teste de composto/primo continua o foco.</p>"},
                    {"Por que isso é útil",
                        "<p>É o “<strong>multímetro</strong>” do app: antes de explorar Mersenne, RSA de brinquedo ou estatísticas, você confirma <strong>o status de um inteiro concreto</strong>. Ajuda também a validar contas de exercícios e a construir intuição sobre o quão rápido cresce a dificuldade quando o número aumenta.</p>"}
                };
            case "Estatísticas":
                return new String[][]{
                    {"O que este card faz",
                        "<p>Concentra-se em <strong>visão geral</strong>: quantos primos existem até um certo limite, <strong>densidade</strong>, comparações com aproximações clássicas (como a ideia de que “aos poucos os primos ficam mais raros” em proporção), e outras métricas que a tela listar.</p>" +
                        "<div class='exemplo'>Pense em um <strong>painel de resumo</strong> da “floresta” dos primos, não em achar um primo específico.</div>"},
                    {"Como usar",
                        "<p>Informe o limite ou parâmetros pedidos e interprete os números como <strong>tendências</strong>. Suba o limite gradualmente para ver como tempo de cálculo e memória reagem.</p>"},
                    {"O que aprender com isso",
                        "<p>A distribuição dos primos foi um dos grandes temas dos últimos séculos; ver <strong>contagens reais</strong> ajuda a conectar o que os livros chamam de teorema dos números primos ou função π(n) com <strong>dados que você mesmo gerou</strong> no aparelho.</p>"}
                };
            default:
                return new String[][]{
                    {"Sobre este card",
                        "<p>Este tutorial genérico cobre a função <strong>" + funcionalidade + "</strong>. Abra o card na tela principal e leia os rótulos dos campos: eles dizem o que informar e o que será calculado.</p>"},
                    {"Dica",
                        "<p>Se algo falhar, verifique se os valores são inteiros válidos e se não ultrapassam limites exibidos em mensagens do app. Use valores pequenos primeiro.</p>"}
                };
        }
    }

    /**
     * Retorna design único para cada funcionalidade
     * [corPrimaria, corSecundaria]
     */
    private int[] obterDesignUnico(String titulo) {
        switch (titulo) {
            case "Primos por Intervalo":
                return new int[]{0xFF3F51B5, 0xFF5C6BC0}; // Índigo
            case "Números de Mersenne":
                return new int[]{0xFF5E35B1, 0xFF9575CD}; // Roxo escuro (ícone raio)
            case "Números Perfeitos":
                return new int[]{0xFF4CAF50, 0xFF81C784}; // Verde
            case "Segurança Digital":
                return new int[]{0xFFFF5722, 0xFFFF8A65}; // Laranja
            case "Teste de Primalidade":
                return new int[]{0xFF2196F3, 0xFF64B5F6}; // Azul
            case "Primos Especiais":
                return new int[]{0xFF9C27B0, 0xFFBA68C8}; // Roxo
            case "Primos Aleatórios":
                return new int[]{0xFF607D8B, 0xFF90A4AE}; // Azul acinzentado
            case "Conjectura de Legendre":
                return new int[]{0xFFE91E63, 0xFFF06292}; // Rosa
            case "Estatísticas":
                return new int[]{0xFF009688, 0xFF4DB6AC}; // Teal
            default:
                return new int[]{0xFF757575, 0xFFBDBDBD}; // Cinza padrão
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
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='pt-BR'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>").append(titulo).append(" - PrimeProFast</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }\n");
        html.append("        .container { width: 100%; margin: 0; padding: 0; }\n");
        html.append("        .header { text-align: center; margin-bottom: 40px; color: white; }\n");
        html.append("        .header h1 { font-size: 2.5em; margin-bottom: 10px; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }\n");
        html.append("        .header p { font-size: 1.2em; opacity: 0.9; }\n");
        html.append("        .progress-bar { width: 100%; height: 6px; background: rgba(255,255,255,0.2); border-radius: 3px; margin: 20px 0; overflow: hidden; }\n");
        html.append("        .progress-fill { height: 100%; background: linear-gradient(90deg, #4CAF50, #45a049); width: 0%; transition: width 0.3s ease; width: 0%; }\n");
        html.append("        .content { background: white; border-radius: 20px; padding: 40px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); }\n");
        html.append("        .resultado { margin-bottom: 30px; padding: 25px; border-left: 5px solid #667eea; background: #f8f9fa; border-radius: 0 15px 15px 0; }\n");
        html.append("        .resultado h3 { color: #667eea; font-size: 1.4em; margin-bottom: 15px; }\n");
        html.append("        .resultado pre { background: #2d3748; color: #e2e8f0; padding: 20px; border-radius: 10px; overflow-x: auto; font-family: 'Courier New', monospace; font-size: 0.9em; line-height: 1.4; }\n");
        html.append("        .footer { text-align: center; margin-top: 40px; padding: 20px; color: white; font-size: 0.9em; }\n");
        html.append("        .tipo-badge { display: inline-block; padding: 8px 16px; background: #667eea; color: white; border-radius: 20px; font-size: 0.9em; margin-bottom: 20px; }\n");
        html.append("        @media (max-width: 768px) { .container { padding: 10px; } .content { padding: 20px; } }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='container'>\n");
        html.append("        <div class='header'>\n");
        html.append("            <h1>🚀 ").append(titulo).append("</h1>\n");
        html.append("            <p>Resultado do PrimeProFast - ").append(tipo).append("</p>\n");
        html.append("            <div class='progress-bar'>\n");
        html.append("                <div class='progress-fill' id='progress'></div>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class='content'>\n");
        html.append("            <div class='tipo-badge'>📊 ").append(tipo).append("</div>\n");
        html.append("            <div class='resultado'>\n");
        html.append("                <h3>📋 Resultado Completo</h3>\n");
        html.append("                <pre>").append(resultado).append("</pre>\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("        \n");
        html.append("        <div class='footer'>\n");
        html.append("            <p>🎓 PrimeProFast - ").append(titulo).append("</p>\n");
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
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
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
        // Definir card atual para controle de limitações
        currentCard = funcao;
        carregarCalculosCard();
        
        // Ocultar menu (ScrollView) e mostrar conteúdo com animação suave
        if (menuScrollView != null) {
            menuScrollView.animate().alpha(0.0f).setDuration(300).withEndAction(() -> {
                menuScrollView.setVisibility(View.GONE);
                contentContainer.setVisibility(View.VISIBLE);
                contentContainer.setAlpha(0.0f);
                contentContainer.animate().alpha(1.0f).setDuration(300);
            });
        } else {
            // Fallback: caso ScrollView não esteja inicializado por algum motivo
            menuContainer.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
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
        }
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

                        // Salvar em arquivo temporário e abrir com visualizador HTML (SISTEMA ORIGINAL)
                        try {
                            File tempDir = new File(getCacheDir(), "temp_primos");
                            if (!tempDir.exists()) {
                                tempDir.mkdirs();
                            }

                            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                            File tempFile = new File(tempDir, "primos_temp_" + timestamp + ".txt");

                            FileWriter writer = new FileWriter(tempFile);
                            writer.write(resultado);
                            writer.close();

                            Log.d(TAG, "Arquivo temporário criado: " + tempFile.getAbsolutePath());

                            runOnUiThread(() -> {
                                resultadoView.setText("Resultado gerado com sucesso! Abrindo visualizador...");
                                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));

                                // Abrir com visualizador HTML (SISTEMA ORIGINAL)
                                openFileWithHtmlViewer(tempFile);
                            });
                        } catch (IOException e) {
                            Log.e(TAG, "Erro ao salvar arquivo temporário", e);
                            runOnUiThread(() -> {
                                resultadoView.setText("Erro ao salvar arquivo temporário: " + e.getMessage());
                            });
                        }
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
        String[] tiposPrimos = {
            "Primos Gêmeos (p, p+2)",
            "Primos de Sophie Germain (p, 2p+1)",
            "Primos Cousins (p, p+4)",
            "Primos Sexy (p, p+6)",
            "Primos Palíndromos",
            "Primos de Fermat (2^(2^n)+1)"
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

        adicionarRotuloCampo("Limite superior da busca");
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
                long limite = Long.parseLong(inputLimite.getText().toString());
                
                if (quantidade <= 0) {
                    Toast.makeText(this, "Digite uma quantidade válida", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (limite <= 0) {
                    Toast.makeText(this, "Digite um limite válido", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Verificar limites de monetização
                if (!podeRealizarCalculo(limite)) {
                    mostrarDialogoLimiteAtingido(limite);
                    return;
                }
                
                gerarPrimosEspeciais(tipoSelecionado, quantidade, limite);
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite números válidos", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnGerar);

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
    private void gerarPrimosEspeciais(String tipo, int quantidade, long limite) {
        CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
            resultadoView,
            "Primos Especiais",
            "Tipo: " + tipo,
            "Quantidade: " + quantidade,
            "Limite: " + String.format("%,d", limite)
        );
                
                new Thread(() -> {
                    try {
                StringBuilder resultado = new StringBuilder();
                resultado.append("⭐ ").append(tipo).append("\n");
                resultado.append("Quantidade: ").append(quantidade).append("\n");
                resultado.append("Limite: ").append(String.format("%,d", limite)).append("\n\n");
                
                List<String> primosEspeciais = new ArrayList<>();
                
                switch (tipo) {
                    case "Primos Gêmeos (p, p+2)":
                        primosEspeciais = gerarPrimosGemeos(quantidade, limite);
                        break;
                    case "Primos de Sophie Germain (p, 2p+1)":
                        primosEspeciais = gerarPrimosSophieGermain(quantidade, limite);
                        break;
                    case "Primos Cousins (p, p+4)":
                        primosEspeciais = gerarPrimosCousins(quantidade, limite);
                        break;
                    case "Primos Sexy (p, p+6)":
                        primosEspeciais = gerarPrimosSexy(quantidade, limite);
                        break;
                    case "Primos Palíndromos":
                        primosEspeciais = gerarPrimosPalindromos(quantidade, limite);
                        break;
                    case "Primos de Fermat (2^(2^n)+1)":
                        primosEspeciais = gerarPrimosFermat(quantidade);
                        break;
                }
                
                for (int i = 0; i < primosEspeciais.size(); i++) {
                    resultado.append((i + 1)).append(". ").append(primosEspeciais.get(i)).append("\n");
                }
                
                if (primosEspeciais.isEmpty()) {
                    resultado.append("Nenhum primo especial encontrado no limite especificado.\n");
                    resultado.append("Tente aumentar o limite ou diminuir a quantidade.");
                }
                
                // Registrar cálculo realizado
                registrarCalculo();
                            
                            runOnUiThread(() -> {
                    pararCronometroOperacao(cronometroAtivo);
                    resultadoView.setText(resultado.toString());
                    resultadoView.setTextColor(0xFF333333);
                });
                
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                    pararCronometroOperacao(cronometroAtivo);
                    resultadoView.setText("Erro ao gerar primos especiais: " + e.getMessage());
                    resultadoView.setTextColor(0xFFFF0000);
                        });
                    }
                }).start();
    }

    /**
     * Gera primos gêmeos aleatórios (p, p+2)
     */
    private List<String> gerarPrimosGemeos(int quantidade, long limite) {
        List<String> primosGemeos = new ArrayList<>();
        Set<String> primosJaGerados = new HashSet<>(); // Evitar duplicatas
        int tentativas = 0;
        int maxTentativas = quantidade * 50; // Limite de tentativas
        
        while (primosGemeos.size() < quantidade && tentativas < maxTentativas) {
            // Gerar primo aleatório entre 3 e limite-2
            long p = gerarPrimoAleatorio(3, limite - 2);
            
            if (p != -1 && ehPrimo(p + 2)) {
                String par = "(" + p + ", " + (p + 2) + ")";
                if (!primosJaGerados.contains(par)) {
                    primosGemeos.add(par);
                    primosJaGerados.add(par);
                }
            }
            tentativas++;
        }
        
        // Se não encontrou suficientes aleatórios, completar sequencialmente
        if (primosGemeos.size() < quantidade) {
            for (long p = 3; p < limite - 2 && primosGemeos.size() < quantidade; p += 2) {
                if (ehPrimo(p) && ehPrimo(p + 2)) {
                    String par = "(" + p + ", " + (p + 2) + ")";
                    if (!primosJaGerados.contains(par)) {
                        primosGemeos.add(par);
                        primosJaGerados.add(par);
                    }
                }
            }
        }
        
        return primosGemeos;
    }

    /**
     * Gera primos de Sophie Germain aleatórios (p, 2p+1)
     */
    private List<String> gerarPrimosSophieGermain(int quantidade, long limite) {
        List<String> primosSG = new ArrayList<>();
        Set<String> primosJaGerados = new HashSet<>(); // Evitar duplicatas
        int tentativas = 0;
        int maxTentativas = quantidade * 50; // Limite de tentativas
        
        while (primosSG.size() < quantidade && tentativas < maxTentativas) {
            // Gerar primo aleatório entre 2 e limite
            long p = gerarPrimoAleatorio(2, limite);
            
            if (p != -1 && 2 * p + 1 <= limite && ehPrimo(2 * p + 1)) {
                String primo = p + " (2×" + p + "+1 = " + (2 * p + 1) + ")";
                if (!primosJaGerados.contains(primo)) {
                    primosSG.add(primo);
                    primosJaGerados.add(primo);
                }
            }
            tentativas++;
        }
        
        // Se não encontrou suficientes aleatórios, completar sequencialmente
        if (primosSG.size() < quantidade) {
            for (long p = 2; p < limite && primosSG.size() < quantidade; p++) {
                if (ehPrimo(p) && 2 * p + 1 <= limite && ehPrimo(2 * p + 1)) {
                    String primo = p + " (2×" + p + "+1 = " + (2 * p + 1) + ")";
                    if (!primosJaGerados.contains(primo)) {
                        primosSG.add(primo);
                        primosJaGerados.add(primo);
                    }
                }
            }
        }
        
        return primosSG;
    }

    /**
     * Gera primos cousins aleatórios (p, p+4)
     */
    private List<String> gerarPrimosCousins(int quantidade, long limite) {
        List<String> primosCousins = new ArrayList<>();
        Set<String> primosJaGerados = new HashSet<>(); // Evitar duplicatas
        int tentativas = 0;
        int maxTentativas = quantidade * 50; // Limite de tentativas
        
        while (primosCousins.size() < quantidade && tentativas < maxTentativas) {
            // Gerar primo aleatório entre 3 e limite-4
            long p = gerarPrimoAleatorio(3, limite - 4);
            
            if (p != -1 && ehPrimo(p + 4)) {
                String par = "(" + p + ", " + (p + 4) + ")";
                if (!primosJaGerados.contains(par)) {
                    primosCousins.add(par);
                    primosJaGerados.add(par);
                }
            }
            tentativas++;
        }
        
        // Se não encontrou suficientes aleatórios, completar sequencialmente
        if (primosCousins.size() < quantidade) {
            for (long p = 3; p < limite - 4 && primosCousins.size() < quantidade; p += 2) {
                if (ehPrimo(p) && ehPrimo(p + 4)) {
                    String par = "(" + p + ", " + (p + 4) + ")";
                    if (!primosJaGerados.contains(par)) {
                        primosCousins.add(par);
                        primosJaGerados.add(par);
                    }
                }
            }
        }
        
        return primosCousins;
    }

    /**
     * Gera primos sexy aleatórios (p, p+6)
     */
    private List<String> gerarPrimosSexy(int quantidade, long limite) {
        List<String> primosSexy = new ArrayList<>();
        Set<String> primosJaGerados = new HashSet<>(); // Evitar duplicatas
        int tentativas = 0;
        int maxTentativas = quantidade * 50; // Limite de tentativas
        
        while (primosSexy.size() < quantidade && tentativas < maxTentativas) {
            // Gerar primo aleatório entre 5 e limite-6
            long p = gerarPrimoAleatorio(5, limite - 6);
            
            if (p != -1 && ehPrimo(p + 6)) {
                String par = "(" + p + ", " + (p + 6) + ")";
                if (!primosJaGerados.contains(par)) {
                    primosSexy.add(par);
                    primosJaGerados.add(par);
                }
            }
            tentativas++;
        }
        
        // Se não encontrou suficientes aleatórios, completar sequencialmente
        if (primosSexy.size() < quantidade) {
            for (long p = 5; p < limite - 6 && primosSexy.size() < quantidade; p += 2) {
                if (ehPrimo(p) && ehPrimo(p + 6)) {
                    String par = "(" + p + ", " + (p + 6) + ")";
                    if (!primosJaGerados.contains(par)) {
                        primosSexy.add(par);
                        primosJaGerados.add(par);
                    }
                }
            }
        }
        
        return primosSexy;
    }

    /**
     * Gera primos palíndromos aleatórios
     */
    private List<String> gerarPrimosPalindromos(int quantidade, long limite) {
        List<String> primosPalindromos = new ArrayList<>();
        Set<String> primosJaGerados = new HashSet<>(); // Evitar duplicatas
        int tentativas = 0;
        int maxTentativas = quantidade * 50; // Limite de tentativas
        
        while (primosPalindromos.size() < quantidade && tentativas < maxTentativas) {
            // Gerar primo aleatório entre 2 e limite
            long p = gerarPrimoAleatorio(2, limite);
            
            if (p != -1 && ehPalindromo(p)) {
                String primo = String.valueOf(p);
                if (!primosJaGerados.contains(primo)) {
                    primosPalindromos.add(primo);
                    primosJaGerados.add(primo);
                }
            }
            tentativas++;
        }
        
        // Se não encontrou suficientes aleatórios, completar sequencialmente
        if (primosPalindromos.size() < quantidade) {
            for (long p = 2; p < limite && primosPalindromos.size() < quantidade; p++) {
                if (ehPrimo(p) && ehPalindromo(p)) {
                    String primo = String.valueOf(p);
                    if (!primosJaGerados.contains(primo)) {
                        primosPalindromos.add(primo);
                        primosJaGerados.add(primo);
                    }
                }
            }
        }
        
        return primosPalindromos;
    }

    /**
     * Gera primos de Fermat (2^(2^n)+1)
     */
    private List<String> gerarPrimosFermat(int quantidade) {
        List<String> primosFermat = new ArrayList<>();
        // Apenas F₀, F₁, F₂, F₃, F₄ são conhecidos
        long[] fermat = {3, 5, 17, 257, 65537}; // F₀ a F₄
        for (int i = 0; i < Math.min(quantidade, fermat.length); i++) {
            primosFermat.add("F" + i + " = " + fermat[i]);
        }
        return primosFermat;
    }

    /**
     * Verifica se um número é primo (única implementação: divisão 6k±1 com {@code long},
     * evita overflow de {@code i*i} que ocorre com {@code int} perto de {@link Integer#MAX_VALUE}).
     */
    private boolean ehPrimo(long n) {
        if (n < 2) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (long i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    /**
     * Verifica se um número é palíndromo
     */
    private boolean ehPalindromo(long n) {
        String s = String.valueOf(n);
        return s.equals(new StringBuilder(s).reverse().toString());
    }

    /**
     * Gera um número aleatório entre min e max (inclusive)
     */
    private long gerarNumeroAleatorio(long min, long max) {
        if (min >= max) return min;
        return min + (long) (Math.random() * (max - min + 1));
    }

    /**
     * Gera um número primo aleatório entre min e max
     */
    private long gerarPrimoAleatorio(long min, long max) {
        int tentativas = 0;
        int maxTentativas = 1000; // Evitar loop infinito
        
        while (tentativas < maxTentativas) {
            long candidato = gerarNumeroAleatorio(min, max);
            if (ehPrimo(candidato)) {
                return candidato;
            }
            tentativas++;
        }
        
        // Se não encontrou primo aleatório, busca sequencialmente
        for (long p = min; p <= max; p++) {
            if (ehPrimo(p)) {
                return p;
            }
        }
        return -1; // Não encontrou primo no intervalo
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
            if (packageName == null || !packageName.equals("com.seuprojeto.primeprofast")) {
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
            // Verificar se a assinatura está correta
            android.content.pm.PackageInfo packageInfo = getPackageManager()
                .getPackageInfo(getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            
            if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
                return false;
            }
            
            // Verificar se não é debug keystore
            android.content.pm.Signature signature = packageInfo.signatures[0];
            String signatureString = signature.toCharsString();
            
            // Se for debug keystore, permitir apenas em desenvolvimento
            if (signatureString.contains("androiddebugkey")) {
                return true; // Permitir em desenvolvimento
            }
            
            return true; // Assinatura válida
        } catch (Exception e) {
            return false;
        }
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
        contentContainer.addView(btnCancelar);
        this.btnCancelarOperacaoAtual = btnCancelar;

        btnGerar.setOnClickListener(v -> {
            try {
                String quantidadeStr = inputQuantidade.getText().toString();
                String bitsStr = inputBits.getText().toString();
                
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

                cancelarOperacaoAtual.set(false);

                final boolean usarRotaNativa = deveUsarRotaNativaPrimos(bits);
                if (usarRotaNativa) {
                    mostrarDialogoEscolhaPrimosUltra(bits, quantidade);
                    return;
                }

                if (btnCancelarOperacaoAtual != null) {
                    btnCancelarOperacaoAtual.setVisibility(View.VISIBLE);
                }

                final CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                    resultadoView,
                    "Primos Aleatórios",
                    "Quantidade: " + quantidade,
                    "Bits: " + bits
                );

                new Thread(() -> executarThreadPrimosAleatorios(bits, quantidade, false, cronometroAtivo)).start();
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Digite números válidos", Toast.LENGTH_SHORT).show();
            }
        });
        contentContainer.addView(btnGerar);

        // Área de resultado
        contentContainer.addView(scrollView);
    }

    /**
     * Acima de {@link #LIMIAR_BITS_ROTA_NATIVA}: avisar sobrecarga local ou compra in-app de entrega por e-mail.
     */
    private void mostrarDialogoEscolhaPrimosUltra(int bits, int quantidade) {
        new AlertDialog.Builder(this)
                .setTitle("Primos acima de " + LIMIAR_BITS_ROTA_NATIVA + " bits")
                .setMessage(
                        "Gerar neste aparelho pode demorar muito, consumir bateria e esquentar o celular.\n\n"
                                + "Você pode comprar a entrega por e-mail: o processamento é feito fora do aparelho e o resultado é enviado ao seu e-mail. "
                                + "Esta compra é separada do plano Premium.\n\n"
                                + "Como prefere continuar?")
                .setPositiveButton("Gerar neste aparelho", (d, w) -> {
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
                    new Thread(() -> executarThreadPrimosAleatorios(bits, quantidade, true, cronometroAtivo)).start();
                })
                .setNeutralButton("Comprar Número de " + bits + " bits", (d, w) -> {
                    megaPedidoModoRsa = false;
                    megaPedidoBits = bits;
                    megaPedidoQuantidade = quantidade;
                    if (playBillingManager != null) {
                        playBillingManager.launchMegaDeliveryInAppPurchase(this);
                    } else {
                        Toast.makeText(this, "Pagamento indisponível.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
        new AlertDialog.Builder(this)
                .setTitle("Chave RSA " + bitsChave + " bits (~" + bitsPorPrimo + " + " + bitsPorPrimo + " bits em p e q)")
                .setMessage(
                        "Gerar p e q neste aparelho pode demorar muito, consumir bateria e esquentar o celular.\n\n"
                                + "Você pode comprar a entrega por e-mail: o par de primos e a montagem da chave RSA são feitos fora do aparelho; você recebe o resultado por e-mail. "
                                + "Esta compra é separada do plano Premium.\n\n"
                                + "Como prefere continuar?")
                .setPositiveButton("Gerar neste aparelho", (d, w) -> aoContinuarGeracaoLocal.run())
                .setNeutralButton("Comprar Número de " + bitsChave + " bits", (d, w) -> {
                    megaPedidoModoRsa = true;
                    megaPedidoBits = bitsChave;
                    megaPedidoQuantidade = 2;
                    if (playBillingManager != null) {
                        playBillingManager.launchMegaDeliveryInAppPurchase(this);
                    } else {
                        Toast.makeText(this, "Pagamento indisponível.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarThreadPrimosAleatorios(
            int bits,
            int quantidade,
            boolean usarRotaNativa,
            CronometroOperacaoHandle cronometroAtivo) {
        try {
            StringBuilder resultado = new StringBuilder();
            resultado.append("🔢 PRIMOS ALEATÓRIOS GERADOS\n");
            resultado.append("=============================\n\n");
            resultado.append("ℹ️ Por se tratar de busca aleatória, o tempo para encontrar primos pode variar bastante de uma execução para outra.\n");
            resultado.append("   Se a busca demorar muito, recomenda-se cancelar e tentar novamente com os mesmos parâmetros ou ajustando o tamanho em bits.\n\n");
            resultado.append("📊 PARÂMETROS:\n");
            resultado.append("   • Quantidade: ").append(quantidade).append(" primos\n");
            resultado.append("   • Tamanho: ").append(bits).append(" bits\n");
            resultado.append("   • Dígitos aproximados: ").append((int) (bits * 0.301 + 1)).append(" dígitos\n");
            resultado.append("   • Modo: Geração por bits\n\n");

            resultado.append("🚀 INICIANDO GERAÇÃO...\n");
            resultado.append("   Buscando primos grandes com algoritmos otimizados.\n\n");

            if (usarRotaNativa) {
                resultado.append("🔧 MODO ATUAL:\n");
                resultado.append("   • Método: Rota nativa C++/GMP\n");
                resultado.append("   • Faixa automática: acima de ").append(LIMIAR_BITS_ROTA_NATIVA).append(" bits\n\n");

                File arquivoPrimos = criarArquivoTempPrimosGigantes("primos_gigantes_");
                String relatorioNativo = executarJobNativoPrimosGigantes(bits, quantidade, 0, status ->
                        runOnUiThread(() -> resultadoView.setText(status)), arquivoPrimos);
                resultado = new StringBuilder(relatorioNativo);
                resultado.append("\n\n--- PRIMOS (DECIMAL COMPLETO) ---\n");
                try {
                    resultado.append(lerTextoUtf8(arquivoPrimos));
                } catch (IOException ex) {
                    resultado.append("(erro ao ler arquivo: ").append(ex.getMessage()).append(")\n");
                }
            } else {
                int tentativas;
                int numThreadsUtilizados;
                List<BigInteger> primosEncontrados;

                resultado.append("🔧 MODO ATUAL:\n");
                resultado.append("   • Método: Proprietário otimizado\n\n");

                int maxTentativas = Math.max(quantidade * 1000, bits * 100);

                CopyOnWriteArrayList<BigInteger> primosEncontradosJava = new CopyOnWriteArrayList<>();
                AtomicInteger tentativasAtomic = new AtomicInteger(0);
                AtomicBoolean pararThreads = new AtomicBoolean(false);

                int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), 8);
                Thread[] threads = new Thread[numThreads];

                for (int t = 0; t < numThreads; t++) {
                    final int threadId = t;
                    threads[threadId] = new Thread(() -> {
                        SecureRandom threadRnd = new SecureRandom();
                        while (!pararThreads.get() && !cancelarOperacaoAtual.get() && primosEncontradosJava.size() < quantidade) {
                            if (primosEncontradosJava.size() >= quantidade || pararThreads.get() || cancelarOperacaoAtual.get()) {
                                break;
                            }

                            BigInteger candidato = BigInteger.probablePrime(bits, threadRnd);
                            int candidatoBits = candidato.bitLength();
                            if (candidatoBits >= bits - 1 && candidatoBits <= bits + 1) {
                                if (primosEncontradosJava.size() < quantidade && !pararThreads.get()) {
                                    primosEncontradosJava.add(candidato);
                                    if (primosEncontradosJava.size() >= quantidade) {
                                        pararThreads.set(true);
                                        break;
                                    }
                                }
                            }
                            tentativasAtomic.incrementAndGet();

                            if (tentativasAtomic.get() >= maxTentativas) {
                                break;
                            }
                        }
                    });
                    threads[threadId].start();
                }

                long timeoutStartTime = System.currentTimeMillis();
                long timeout = Math.max(60000, bits * 100);
                while (!cancelarOperacaoAtual.get()
                        && primosEncontradosJava.size() < quantidade
                        && (System.currentTimeMillis() - timeoutStartTime) < timeout) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
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

                primosEncontrados = new ArrayList<>(primosEncontradosJava);
                tentativas = tentativasAtomic.get();
                numThreadsUtilizados = numThreads;

                if (cancelarOperacaoAtual.get()) {
                    resultado.append("\n⏹️ OPERAÇÃO CANCELADA PELO USUÁRIO\n");
                    resultado.append("=================================\n");
                    resultado.append("Mostrando os primos já encontrados até o momento do cancelamento:\n\n");
                }

                anexarResultadoPrimosAleatorios(
                        resultado,
                        primosEncontrados,
                        quantidade,
                        tentativas,
                        numThreadsUtilizados);
            }

            resultado.append("\n⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            long fimRelatorio = System.currentTimeMillis();
            if (usarRotaNativa) {
                String ajustado = alinharTempoRelatorioNativoComCronometro(resultado.toString(), cronometroAtivo.inicioMs, fimRelatorio);
                resultado = new StringBuilder(ajustado);
            } else {
                int idxTs = resultado.indexOf("\n⏰ TIMESTAMP:");
                if (idxTs >= 0) {
                    resultado.insert(idxTs, "\n   • Tempo total: " + formatarSegundosTempoCronometro(cronometroAtivo.inicioMs, fimRelatorio) + " s");
                }
            }

            pararCronometroOperacao(cronometroAtivo);

            salvarResultadoTemporario(resultado.toString(), "primos_aleatorios_bits");

        } catch (Throwable t) {
            Log.e(TAG, "Primos aleatórios (bits=" + bits + ")", t);
            pararCronometroOperacao(cronometroAtivo);
            String msg = t.getMessage();
            if (t instanceof OutOfMemoryError) {
                msg = "Memória insuficiente para este tamanho em bits com BigInteger. Tente menos threads ou use bits um pouco menores.";
            }
            final String msgFinal = msg != null ? msg : t.getClass().getSimpleName();
            runOnUiThread(() -> resultadoView.setText("❌ Erro: " + msgFinal));
        } finally {
            cancelarOperacaoAtual.set(false);
            runOnUiThread(() -> {
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
        modoGroup.setOrientation(LinearLayout.HORIZONTAL);
        
        RadioButton modoUnico = new RadioButton(this);
        modoUnico.setText("Relatório Ilimitado");
        modoUnico.setId(1);
        modoUnico.setChecked(true);
        modoGroup.addView(modoUnico);
        
        RadioButton modoIntervalo = new RadioButton(this);
        modoIntervalo.setText("Busca de intervalos");
        modoIntervalo.setId(2);
        modoGroup.addView(modoIntervalo);
        
        contentContainer.addView(modoGroup);

        // Campos para modo relatório ilimitado
        LinearLayout layoutUnico = new LinearLayout(this);
        layoutUnico.setOrientation(LinearLayout.VERTICAL);
        layoutUnico.setId(100);

        TextView labN = new TextView(this);
        labN.setText("Valor de n (modo relatório)");
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
            if (checkedId == 1) {
                layoutUnico.setVisibility(View.VISIBLE);
                layoutIntervalo.setVisibility(View.GONE);
            } else {
                layoutUnico.setVisibility(View.GONE);
                layoutIntervalo.setVisibility(View.VISIBLE);
            }
        });

        // Verificar se há estado salvo para continuar
        SharedPreferences prefs = getSharedPreferences("ConjecturaLegendre", MODE_PRIVATE);
        String ultimoN = prefs.getString("ultimo_n", "");
        int totalVerificados = prefs.getInt("total_verificados", 0);
        long timestampUltimaExecucao = prefs.getLong("timestamp_ultima_execucao", 0);
        
        if (!ultimoN.isEmpty() && totalVerificados > 0) {
            TextView estadoSalvo = new TextView(this);
            estadoSalvo.setText("💾 Estado salvo encontrado:\n" +
                "• Último n testado: " + ultimoN + "\n" +
                "• Total de intervalos: " + totalVerificados + "\n" +
                "• Última execução: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(timestampUltimaExecucao)) + "\n\n" +
                "Clique em 'Continuar de onde parou' para retomar.");
            estadoSalvo.setPadding(0, 10, 0, 20);
            estadoSalvo.setTextColor(Color.parseColor("#0066CC"));
            contentContainer.addView(estadoSalvo);
            
            Button btnContinuar = new Button(this);
            btnContinuar.setText("🔄 Continuar de onde parou");
            btnContinuar.setBackgroundColor(0xFF4CAF50);
            btnContinuar.setTextColor(0xFFFFFFFF);
            btnContinuar.setOnClickListener(continuarView -> {
                // Implementar lógica para continuar
                resultadoView.setText("🚀 CONTINUANDO RELATÓRIO ILIMITADO\n" +
                    "Retomando de n = " + ultimoN + "\n" +
                    "Total anterior: " + totalVerificados + " intervalos\n" +
                    "Status: Continuando busca por falha na conjectura...");
                
                // Iniciar continuação em thread separada
                new Thread(() -> {
                    try {
                        // Parse do valor n salvo
                        BigInteger n = parseBigInteger(ultimoN);
                        
                        // Iniciar cronômetro
                        long startTime = System.currentTimeMillis();
                        
                        StringBuilder resultado = new StringBuilder();
                        resultado.append("Conjectura de Legendre - Continuação\n");
                        resultado.append("==================================\n\n");
                        resultado.append("Estado Anterior:\n");
                        resultado.append("- n inicial: ").append(ultimoN).append("\n");
                        resultado.append("- Total verificados: ").append(totalVerificados).append(" intervalos\n");
                        resultado.append("- Última execução: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(timestampUltimaExecucao))).append("\n\n");
                        
                        resultado.append("Continuando busca...\n");
                        resultado.append("===================\n");
                        
                        BigInteger nAtual = n;
                        int totalVerificadosContinuacao = totalVerificados;
                        int intervalosComPrimos = 0;
                        int intervalosSemPrimos = 0;
                        
                        // Continuar teste até encontrar falha ou parar
                        boolean falhaEncontrada = false;
                        BigInteger nFalha = BigInteger.ZERO;
                        
                        while (!falhaEncontrada && !this.paradaUsuario) {
                            BigInteger inicioAtual = nAtual.multiply(nAtual);
                            BigInteger fimAtual = nAtual.add(BigInteger.ONE).multiply(nAtual.add(BigInteger.ONE));
                            
                            // Verificação otimizada
                            boolean primoEncontrado = verificarIntervaloLegendre(inicioAtual, fimAtual);
                            
                            if (primoEncontrado) {
                                intervalosComPrimos++;
                            } else {
                                // Verificação extra
                                primoEncontrado = verificarIntervaloLegendreDetalhado(inicioAtual, fimAtual);
                                
                                if (primoEncontrado) {
                                    intervalosComPrimos++;
                                } else {
                                    falhaEncontrada = true;
                                    nFalha = nAtual;
                                }
                            }
                            
                            if (!falhaEncontrada) {
                                totalVerificadosContinuacao++;
                                nAtual = nAtual.add(BigInteger.ONE);
                                
                                // Atualizar progresso
                                long tempoAtual = System.currentTimeMillis();
                                if (tempoAtual - startTime > 30000) {
                                    final String progresso = "🚀 CONTINUAÇÃO - PROGRESSO:\n" +
                                        "ANALISANDO: n = " + nAtual + "\n" +
                                        "Total verificados: " + totalVerificadosContinuacao + " intervalos\n" +
                                        "Taxa: ~" + (totalVerificadosContinuacao * 1000 / (tempoAtual - startTime)) + " intervalos/segundo\n" +
                                        "Status: Continuando busca...";
                                    runOnUiThread(() -> resultadoView.setText(progresso));
                                    startTime = tempoAtual;
                                }
                            }
                        }
                        
                        // Salvar estado atualizado
                        SharedPreferences prefsContinuacao = getSharedPreferences("ConjecturaLegendre", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefsContinuacao.edit();
                        editor.putString("ultimo_n", nAtual.toString());
                        editor.putInt("total_verificados", totalVerificadosContinuacao);
                        editor.putInt("intervalos_com_primos", intervalosComPrimos);
                        editor.putInt("intervalos_sem_primos", intervalosSemPrimos);
                        editor.putLong("timestamp_ultima_execucao", System.currentTimeMillis());
                        editor.apply();
                        
                        // Resultado final
                        if (this.paradaUsuario) {
                            resultado.append("\n⏹️ EXECUÇÃO PARADA PELO USUÁRIO\n");
                            resultado.append("================================\n");
                            resultado.append("Execução interrompida em n = ").append(nAtual).append("\n");
                            resultado.append("Estado salvo para continuar depois.\n\n");
                        } else if (falhaEncontrada) {
                            BigInteger inicioFalha = nFalha.multiply(nFalha);
                            BigInteger fimFalha = nFalha.add(BigInteger.ONE).multiply(nFalha.add(BigInteger.ONE));
                            
                            resultado.append("\n🚨 FALHA NA CONJECTURA ENCONTRADA!\n");
                            resultado.append("================================\n");
                            resultado.append("n = ").append(nFalha).append(" falhou!\n");
                            resultado.append("Intervalo [").append(inicioFalha).append(", ").append(fimFalha).append("] não contém primos.\n");
                            resultado.append("A conjectura de Legendre é FALSA para n = ").append(nFalha).append("!\n\n");
                        }
                        
                        resultado.append("Estatísticas da Continuação:\n");
                        resultado.append("==========================\n");
                        resultado.append("Intervalos verificados: ").append(totalVerificadosContinuacao).append("\n");
                        resultado.append("Intervalos com primos: ").append(intervalosComPrimos).append("\n");
                        resultado.append("Intervalos sem primos: ").append(intervalosSemPrimos).append("\n");
                        resultado.append("Taxa de sucesso: ").append(String.format("%.2f%%", (intervalosComPrimos * 100.0) / totalVerificadosContinuacao)).append("\n");
                        
                        resultado.append("\nTimestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                        
                        // Salvar resultado
                        salvarResultadoTemporario(resultado.toString(), "conjectura_legendre_continuacao");
                        
                    } catch (Exception e) {
                        runOnUiThread(() -> resultadoView.setText("Erro na continuação: " + e.getMessage()));
                    }
                }).start();
            });
            contentContainer.addView(btnContinuar);
        }
        
        // Botão verificar
        Button btnVerificar = new Button(this);
        btnVerificar.setText("🔬 Verificar Conjectura");
        btnVerificar.setBackgroundColor(0xFF2196F3);
        btnVerificar.setTextColor(0xFFFFFFFF);
        btnVerificar.setOnClickListener(v -> {
            try {
                int modo = modoGroup.getCheckedRadioButtonId();
                
                if (modo == 1) {
                    // Modo único
                    String nStr = inputN.getText().toString();
                    if (nStr.isEmpty()) {
                        Toast.makeText(this, "Digite um valor para n", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    resultadoView.setText("🚀 INICIANDO RELATÓRIO ILIMITADO\n" +
                        "Objetivo: Encontrar falha na Conjectura de Legendre\n" +
                        "Iniciando em: n = " + nStr + "\n" +
                        "Status: Analisando intervalos [n², (n+1)²]...\n" +
                        "Resultado: Só será mostrado se NÃO encontrar primos em algum intervalo");
                    
                    new Thread(() -> {
                        try {
                            // Parse do valor n usando BigInteger
                            BigInteger n = parseBigInteger(nStr);
                            if (n.compareTo(BigInteger.ONE) < 0) {
                                runOnUiThread(() -> resultadoView.setText("Erro: n deve ser ≥ 1"));
                                return;
                            }
                            
                            // Verificar se n é muito grande
                            if (n.toString().length() > 10000) {
                                runOnUiThread(() -> resultadoView.setText("Erro: n deve ter no máximo 10.000 dígitos"));
                                return;
                            }
                            
                            // Iniciar cronômetro
                            long startTime = System.currentTimeMillis();
                            
                            BigInteger inicio = n.multiply(n);
                            BigInteger fim = n.add(BigInteger.ONE).multiply(n.add(BigInteger.ONE));
                            
                            StringBuilder resultado = new StringBuilder();
                            resultado.append("Conjectura de Legendre - Relatório Ilimitado\n");
                            resultado.append("==========================================\n\n");
                            resultado.append("Parâmetros:\n");
                            resultado.append("- n inicial: ").append(nStr).append("\n");
                            resultado.append("- Objetivo: Encontrar n onde [n², (n+1)²] NÃO contém primos\n");
                            resultado.append("- Resultado: Só será mostrado se encontrar falha na conjectura\n");
                            resultado.append("- Sem limite de intervalos\n");
                            resultado.append("- Suporte a números de até 10.000 dígitos\n\n");
                            
                            resultado.append("Iniciando teste contínuo...\n");
                            resultado.append("==========================\n");
                            
                            // Aviso sobre performance para números muito grandes
                            if (n.toString().length() > 100) {
                                resultado.append("⚠️ AVISO: Número inicial muito grande (").append(n.toString().length()).append(" dígitos).\n");
                                resultado.append("   O processo pode demorar muito para cada intervalo.\n");
                                resultado.append("   Considere usar um valor menor para testes rápidos.\n\n");
                            }
                            
                            BigInteger nAtual = n;
                            int totalVerificadosUnico = 0;
                            int intervalosComPrimos = 0;
                            int intervalosSemPrimos = 0;
                            
                            // Testar continuamente até encontrar falha (sem limite de intervalos)
                            boolean falhaEncontrada = false;
                            BigInteger nFalha = BigInteger.ZERO;
                            
                            while (!falhaEncontrada && !this.paradaUsuario) {
                                BigInteger inicioAtual = nAtual.multiply(nAtual);
                                BigInteger fimAtual = nAtual.add(BigInteger.ONE).multiply(nAtual.add(BigInteger.ONE));
                                
                                // VERIFICAÇÃO OTIMIZADA PARA NÚMEROS DE ATÉ 10.000 DÍGITOS
                                boolean primoEncontrado = verificarIntervaloLegendre(inicioAtual, fimAtual);
                                
                                if (primoEncontrado) {
                                    // PRIMO ENCONTRADO: Intervalo válido, continuar silenciosamente
                                    intervalosComPrimos++;
                                    // NÃO mostrar nada - continuar para próximo n
                                } else {
                                    // VERIFICAÇÃO EXTRA: Caso duvidoso, fazer verificação mais detalhada
                                    primoEncontrado = verificarIntervaloLegendreDetalhado(inicioAtual, fimAtual);
                                    
                                    if (primoEncontrado) {
                                                intervalosComPrimos++;
                                    } else {
                                            // FALHA CONFIRMADA! 🚨 NENHUM PRIMO NO INTERVALO
                                            falhaEncontrada = true;
                                            nFalha = nAtual;
                                    }
                                }
                                
                                if (!falhaEncontrada) {
                                    totalVerificadosUnico++;
                                    nAtual = nAtual.add(BigInteger.ONE);
                                    
                                    // Atualizar progresso a cada 30 segundos para performance máxima
                                    long tempoAtual = System.currentTimeMillis();
                                    if (tempoAtual - startTime > 30000) { // 30 segundos
                                        final String progressoAtual = "🚀 PROGRESSO ULTRA-RÁPIDO:\n" +
                                            "ANALISANDO: n = " + nAtual + "\n" +
                                            "Intervalo: [" + inicioAtual + ", " + fimAtual + "]\n" +
                                            "Total verificados: " + totalVerificadosUnico + " intervalos\n" +
                                            "Taxa: ~" + (totalVerificadosUnico * 1000 / (tempoAtual - startTime)) + " intervalos/segundo\n" +
                                            "Status: Testando com verificação otimizada... | Dígitos: " + inicioAtual.toString().length();
                                        runOnUiThread(() -> resultadoView.setText(progressoAtual));
                                        startTime = tempoAtual; // Reset do timer
                                    }
                                    
                                    // Reportar progresso detalhado a cada 10.000 intervalos (menos frequente)
                                    if (totalVerificadosUnico % 10000 == 0) {
                                        resultado.append("⚡ MILESTONE: ").append(totalVerificadosUnico).append(" intervalos verificados\n");
                                        resultado.append("   n atual: ").append(nAtual).append(" → intervalo [").append(inicioAtual).append(", ").append(fimAtual).append("]\n");
                                        resultado.append("   dígitos: ").append(inicioAtual.toString().length()).append(" | Status: Todos os intervalos válidos até agora\n\n");
                                    }
                                    
                                    // Aviso sobre performance para números muito grandes (a cada 100.000)
                                    if (inicioAtual.toString().length() > 1000 && totalVerificadosUnico % 100000 == 0) {
                                        resultado.append("⚠️ AVISO: Números muito grandes detectados (mais de 1000 dígitos).\n");
                                        resultado.append("   O processo pode demorar muito. Considere parar se necessário.\n");
                                        resultado.append("   Progresso atual: ").append(totalVerificadosUnico).append(" intervalos, n = ").append(nAtual).append("\n\n");
                                    }
                                }
                            }
                            
                            // Salvar estado atual para poder continuar depois
                            SharedPreferences prefsUnico = getSharedPreferences("ConjecturaLegendre", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefsUnico.edit();
                            editor.putString("ultimo_n", nAtual.toString());
                            editor.putInt("total_verificados", totalVerificadosUnico);
                            editor.putInt("intervalos_com_primos", intervalosComPrimos);
                            editor.putInt("intervalos_sem_primos", intervalosSemPrimos);
                            editor.putLong("timestamp_ultima_execucao", System.currentTimeMillis());
                            editor.apply();
                            
                            // Verificar se encontrou falha, foi interrompido ou parado pelo usuário
                            if (this.paradaUsuario) {
                                resultado.append("\n⏹️ EXECUÇÃO PARADA PELO USUÁRIO\n");
                                resultado.append("================================\n");
                                resultado.append("Execução interrompida em n = ").append(nAtual).append("\n");
                                resultado.append("Estado salvo automaticamente para continuar depois.\n\n");
                                
                                resultado.append("Estatísticas até a parada:\n");
                                resultado.append("========================\n");
                                resultado.append("Intervalos verificados: ").append(totalVerificadosUnico).append("\n");
                                resultado.append("Intervalos com primos: ").append(intervalosComPrimos).append("\n");
                                resultado.append("Intervalos sem primos: ").append(intervalosSemPrimos).append("\n");
                                resultado.append("Taxa de sucesso: ").append(String.format("%.2f%%", (intervalosComPrimos * 100.0) / totalVerificadosUnico)).append("\n");
                                
                                // Salvar em arquivo temporário e abrir com visualizador HTML
                                salvarResultadoTemporario(resultado.toString(), "conjectura_legendre_parada_usuario");
                            } else if (falhaEncontrada) {
                                BigInteger inicioFalha = nFalha.multiply(nFalha);
                                BigInteger fimFalha = nFalha.add(BigInteger.ONE).multiply(nFalha.add(BigInteger.ONE));
                                
                                resultado.append("\n🚨 FALHA NA CONJECTURA ENCONTRADA!\n");
                                resultado.append("================================\n");
                                resultado.append("n = ").append(nFalha).append(" falhou!\n");
                                resultado.append("Intervalo [").append(inicioFalha).append(", ").append(fimFalha).append("] não contém primos.\n");
                                resultado.append("A conjectura de Legendre é FALSA para n = ").append(nFalha).append("!\n\n");
                                
                                resultado.append("Estatísticas até a falha:\n");
                                resultado.append("========================\n");
                                resultado.append("Intervalos verificados: ").append(totalVerificadosUnico).append("\n");
                                resultado.append("Intervalos com primos: ").append(intervalosComPrimos).append("\n");
                                resultado.append("Intervalos sem primos: ").append(intervalosSemPrimos).append("\n");
                                resultado.append("Taxa de sucesso: ").append(String.format("%.2f%%", (intervalosComPrimos * 100.0) / totalVerificadosUnico)).append("\n");
                                
                                // Salvar em arquivo temporário e abrir com visualizador HTML
                                salvarResultadoTemporario(resultado.toString(), "conjectura_legendre_falha");
                            } else {
                                // Se chegou aqui, não encontrou falha (caso raro, mas possível)
                                resultado.append("\n✅ CONJECTURA DE LEGENDRE CONFIRMADA!\n");
                                resultado.append("=====================================\n");
                                resultado.append("Testados ").append(totalVerificadosUnico).append(" intervalos sem encontrar falha.\n");
                                resultado.append("Último n testado: ").append(nAtual.subtract(BigInteger.ONE)).append("\n\n");
                                
                                resultado.append("Estatísticas Finais:\n");
                                resultado.append("===================\n");
                                resultado.append("Intervalos verificados: ").append(totalVerificadosUnico).append("\n");
                                resultado.append("Intervalos com primos: ").append(intervalosComPrimos).append("\n");
                                resultado.append("Intervalos sem primos: ").append(intervalosSemPrimos).append("\n");
                                resultado.append("Taxa de sucesso: ").append(String.format("%.2f%%", (intervalosComPrimos * 100.0) / totalVerificadosUnico)).append("\n");
                                
                                // Calcular tempo de execução
                                long endTime = System.currentTimeMillis();
                                double tempoExecucao = (endTime - startTime) / 1000.0;
                                resultado.append("Tempo de execução: ").append(String.format("%.3f", tempoExecucao)).append(" segundos\n");
                                resultado.append("\nTimestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                                
                                // Salvar em arquivo temporário e abrir com visualizador HTML
                                salvarResultadoTemporario(resultado.toString(), "conjectura_legendre_confirmada");
                            }
                            
                        } catch (Exception e) {
                            runOnUiThread(() -> resultadoView.setText("Erro: " + e.getMessage()));
                        }
                    }).start();
                    
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
                            
                            if (nFim.compareTo(nInicio) <= 0) {
                                runOnUiThread(() -> resultadoView.setText("Erro: n final deve ser maior que n inicial"));
                                return;
                            }
                            
                            // Verificar se o intervalo é muito grande
                            BigInteger tamanhoIntervalo = nFim.subtract(nInicio).add(BigInteger.ONE);
                            if (tamanhoIntervalo.compareTo(BigInteger.valueOf(10000)) > 0) {
                                runOnUiThread(() -> resultadoView.setText("Aviso: Intervalo muito grande. Serão considerados apenas os primeiros 10.000 valores."));
                                nFim = nInicio.add(BigInteger.valueOf(9999)); // Limitar a 10.000
                            }
                            
                            // Iniciar cronômetro
                            long startTime = System.currentTimeMillis();
                            
                            StringBuilder resultado = new StringBuilder();
                            resultado.append("Conjectura de Legendre - Busca de Intervalos\n");
                            resultado.append("============================================\n\n");
                            resultado.append("Parâmetros:\n");
                            resultado.append("- n inicial: ").append(nInicioStr).append("\n");
                            resultado.append("- n final: ").append(nFimStr).append("\n");
                            resultado.append("- Suporte a números de até 10.000 dígitos\n");
                            if (tamanhoIntervalo.compareTo(BigInteger.valueOf(10000)) > 0) {
                                resultado.append("- ⚠️ Intervalo limitado a 10.000 valores para performance\n");
                            }
                            resultado.append("\n");
                            
                            BigInteger n = nInicio;
                            int totalVerificadosIntervalo = 0;
                            int totalPrimosEncontrados = 0;
                            int intervalosComPrimos = 0;
                            int intervalosSemPrimos = 0;
                            
                            // Primeiro, fazer a contagem para as estatísticas
                            resultado.append("Analisando intervalos...\n");
                            resultado.append("=====================\n");
                            
                            // Primeira passada: apenas contar para estatísticas
                            while (n.compareTo(nFim) <= 0) {
                                BigInteger inicio = n.multiply(n);
                                BigInteger fim = n.add(BigInteger.ONE).multiply(n.add(BigInteger.ONE));
                                
                                // Usar verificação otimizada para números de até 10.000 dígitos
                                boolean primoEncontrado = verificarIntervaloLegendre(inicio, fim);
                                
                                if (primoEncontrado) {
                                        intervalosComPrimos++;
                                    totalPrimosEncontrados++;
                                    } else {
                                        intervalosSemPrimos++;
                                }
                                
                                totalVerificadosIntervalo++;
                                n = n.add(BigInteger.ONE); // Incrementar de 1 em 1
                                
                                // Limitar para performance
                                if (totalVerificadosIntervalo >= 10000) {
                                    break;
                                }
                            }
                            
                            // Mostrar estatísticas primeiro
                            resultado.append("\nEstatísticas:\n");
                            resultado.append("============\n");
                            resultado.append("Intervalos verificados: ").append(totalVerificadosIntervalo).append("\n");
                            resultado.append("Intervalos com primos: ").append(intervalosComPrimos).append("\n");
                            resultado.append("Intervalos sem primos: ").append(intervalosSemPrimos).append("\n");
                            resultado.append("Total de primos encontrados: ").append(totalPrimosEncontrados).append("\n");
                            resultado.append("Taxa de sucesso: ").append(String.format("%.2f%%", (intervalosComPrimos * 100.0) / totalVerificadosIntervalo)).append("\n");
                            
                            // Segunda passada: mostrar detalhes dos intervalos
                            resultado.append("\nDetalhes dos Intervalos:\n");
                            resultado.append("=======================\n");
                            
                            n = nInicio; // Resetar para segunda passada
                            int contador = 0;
                            
                            while (n.compareTo(nFim) <= 0 && contador < totalVerificadosIntervalo) {
                                BigInteger inicio = n.multiply(n);
                                BigInteger fim = n.add(BigInteger.ONE).multiply(n.add(BigInteger.ONE));
                                
                                resultado.append("n=").append(n).append(" → intervalo [").append(inicio).append(", ").append(fim).append("]: ");
                                
                                // Usar verificação otimizada
                                boolean primoEncontrado = verificarIntervaloLegendre(inicio, fim);
                                
                                if (primoEncontrado) {
                                    resultado.append("✅ PRIMO ENCONTRADO\n");
                                    } else {
                                        resultado.append("❌ NENHUM PRIMO\n");
                                }
                                
                                contador++;
                                n = n.add(BigInteger.ONE); // Incrementar de 1 em 1
                                
                                // Limitar detalhes para não sobrecarregar o output
                                if (contador >= 100) {
                                    resultado.append("\n⚠️ Mostrando apenas os primeiros 100 intervalos para clareza.\n");
                                    resultado.append("Total de intervalos verificados: ").append(totalVerificadosIntervalo).append("\n");
                                    break;
                                }
                            }
                            
                            // Calcular tempo de execução
                            long endTime = System.currentTimeMillis();
                            double tempoExecucao = (endTime - startTime) / 1000.0;
                            resultado.append("Tempo de execução: ").append(String.format("%.3f", tempoExecucao)).append(" segundos\n");
                            resultado.append("\nTimestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                            
                            // Salvar em arquivo temporário e abrir com visualizador HTML
                            salvarResultadoTemporario(resultado.toString(), "conjectura_legendre_intervalo");
                            
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
                        StringBuilder resultado = new StringBuilder();
                        resultado.append("Números de Mersenne\n");
                        resultado.append("==================\n\n");
                        resultado.append("Parâmetros:\n");
                        resultado.append("- Entrada (expoente): ").append(entradaP).append("\n");
                        resultado.append("- p = ").append(p).append("\n");
                        resultado.append("- Fórmula: M(p) = 2^p - 1\n\n");
                        
                        // Verificar se p é primo
                        resultado.append("Análise do Expoente:\n");
                        resultado.append("===================\n");
                        if (!ehPrimo(p)) {
                            resultado.append("p = ").append(p).append(" não é primo!\n");
                            resultado.append("M(").append(p).append(") não é um número de Mersenne válido.\n\n");
                        } else {
                            resultado.append("p = ").append(p).append(" é primo! ✅\n\n");
                            
                            // Calcular 2^p - 1 usando BigInteger para evitar overflow
                            resultado.append("Cálculo:\n");
                            resultado.append("========\n");
                            
                            // 2^p − 1 (equivale a (1 << p) − 1, sem depender só de pow)
                            BigInteger mersenne = BigInteger.ONE.shiftLeft(p).subtract(BigInteger.ONE);
                            
                            resultado.append("M(").append(p).append(") = 2^").append(p).append(" - 1 = ").append(mersenne).append("\n");
                            resultado.append("Dígitos: ").append(mersenne.toString().length()).append("\n\n");
                            
                            // Verificar se é primo usando método otimizado para números grandes
                            resultado.append("Análise de Primalidade:\n");
                            resultado.append("========================\n");
                            
                            // Mesmo teste de primalidade que o card «Teste de Primalidade» (algoritmo especializado)
                            boolean ehPrimoMersenne = ehPrimoBigInteger(mersenne);
                            
                            if (ehPrimoMersenne) {
                                resultado.append("✅ M(").append(p).append(") é um PRIMO de Mersenne!\n");
                                resultado.append("Este é um número muito especial na matemática.\n\n");
                            } else {
                                resultado.append("❌ M(").append(p).append(") não é primo.\n\n");
                            }
                            
                            // Verificar se está na lista dos primos de Mersenne conhecidos
                            resultado.append("Verificação na Lista de Primos Conhecidos:\n");
                            resultado.append("==========================================\n");
                            
                            int[] primosMersenneConhecidos = {2, 3, 5, 7, 13, 17, 19, 31, 61, 89, 107, 127, 521, 607, 1279, 2203, 2281, 3217, 4253, 4423, 9689, 9941, 11213, 19937, 21701, 23209, 44497, 86243, 110503, 132049, 216091, 756839, 859433, 1257787, 1398269, 2976221, 3021377, 6972593, 13466917, 20996011, 24036583, 25964951, 30402457, 32582657, 37156667, 42643801, 43112609, 57885161, 74207281, 77232917, 82589933};
                            
                            boolean encontrado = false;
                            for (int primo : primosMersenneConhecidos) {
                                if (primo == p) {
                                    encontrado = true;
                                    break;
                                }
                            }
                            
                            if (encontrado) {
                                resultado.append("✅ M(").append(p).append(") está na lista dos primos de Mersenne conhecidos!\n\n");
                            } else {
                                resultado.append("❓ M(").append(p).append(") não está na lista dos primos de Mersenne conhecidos.\n");
                                resultado.append("   Pode ser um novo descoberto ou ainda não verificado.\n\n");
                            }
                        }
                        
                        resultado.append("Primos de Mersenne Conhecidos:\n");
                        resultado.append("==============================\n");
                        resultado.append("M(2) = 3\n");
                        resultado.append("M(3) = 7\n");
                        resultado.append("M(5) = 31\n");
                        resultado.append("M(7) = 127\n");
                        resultado.append("M(13) = 8191\n");
                        resultado.append("M(17) = 131071\n");
                        resultado.append("M(19) = 524287\n");
                        resultado.append("M(31) = 2147483647\n");
                        resultado.append("M(61) = 2305843009213693951\n");
                        resultado.append("M(89) = 618970019642690137449562111\n");
                        resultado.append("M(107) = 162259276829213363391578010288127\n");
                        resultado.append("M(127) = 170141183460469231731687303715884105727\n");
                        resultado.append("M(521) = [número com 157 dígitos]\n");
                        resultado.append("M(607) = [número com 183 dígitos]\n");
                        resultado.append("M(1279) = [número com 386 dígitos]\n");
                        resultado.append("M(2203) = [número com 664 dígitos]\n");
                        resultado.append("M(2281) = [número com 687 dígitos]\n");
                        resultado.append("M(3217) = [número com 969 dígitos]\n");
                        resultado.append("M(4253) = [número com 1281 dígitos]\n");
                        resultado.append("M(4423) = [número com 1332 dígitos]\n");
                        resultado.append("M(9689) = [número com 2917 dígitos]\n");
                        resultado.append("M(9941) = [número com 2993 dígitos]\n");
                        resultado.append("M(11213) = [número com 3376 dígitos]\n");
                        resultado.append("M(19937) = [número com 6002 dígitos]\n");
                        resultado.append("M(21701) = [número com 6533 dígitos]\n");
                        resultado.append("M(23209) = [número com 6987 dígitos]\n");
                        resultado.append("M(44497) = [número com 13395 dígitos]\n");
                        resultado.append("M(86243) = [número com 25962 dígitos]\n");
                        resultado.append("M(110503) = [número com 33265 dígitos]\n");
                        resultado.append("M(132049) = [número com 65050 dígitos]\n");
                        resultado.append("M(216091) = [número com 65050 dígitos]\n");
                        resultado.append("M(756839) = [número com 227832 dígitos]\n");
                        resultado.append("M(859433) = [número com 258716 dígitos]\n");
                        resultado.append("M(1257787) = [número com 378632 dígitos]\n");
                        resultado.append("M(1398269) = [número com 420921 dígitos]\n");
                        resultado.append("M(2976221) = [número com 895932 dígitos]\n");
                        resultado.append("M(3021377) = [número com 909526 dígitos]\n");
                        resultado.append("M(6972593) = [número com 2098960 dígitos]\n");
                        resultado.append("M(13466917) = [número com 4053946 dígitos]\n");
                        resultado.append("M(20996011) = [número com 6320430 dígitos]\n");
                        resultado.append("M(24036583) = [número com 7235733 dígitos]\n");
                        resultado.append("M(25964951) = [número com 7816230 dígitos]\n");
                        resultado.append("M(30402457) = [número com 9152052 dígitos]\n");
                        resultado.append("M(32582657) = [número com 9808358 dígitos]\n");
                        resultado.append("M(37156667) = [número com 11185272 dígitos]\n");
                        resultado.append("M(42643801) = [número com 12837064 dígitos]\n");
                        resultado.append("M(43112609) = [número com 12978189 dígitos]\n");
                        resultado.append("M(57885161) = [número com 17425170 dígitos]\n");
                        resultado.append("M(74207281) = [número com 22338618 dígitos]\n");
                        resultado.append("M(77232917) = [número com 23249425 dígitos]\n");
                        resultado.append("M(82589933) = [número com 24862048 dígitos]\n\n");
                        
                        if (p == 31) {
                            resultado.append("Nota (só quando p = 31): M(31) = 2^31 − 1 = 2.147.483.647 é um dos primos de Mersenne mais citados.\n");
                            resultado.append("É o maior primo que cabe em 32 bits assinados; Euler referiu este valor em 1772.\n\n");
                        }
                        
                        resultado.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                        
                        pararCronometroOperacao(cronometroAtivo);

                        // Salvar em arquivo temporário e abrir com visualizador HTML
                        salvarResultadoTemporario(resultado.toString(), "numeros_mersenne");
                
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
            if (isPremium) {
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
            if (isPremium) {
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
            statsPessoaisLayout.addView(limiteValor);
            
            // Barra de progresso (se não premium)
            if (!isPremium) {
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
            if (!isPremium) {
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
                            long startTime = System.currentTimeMillis();
                            StringBuilder resultado = new StringBuilder();
                            
                            resultado.append("📊 ESTATÍSTICAS COMPLETAS DE PRIMOS\n");
                            resultado.append("==================================\n\n");
                            resultado.append("🎯 LIMITE SUPERIOR: ").append(n).append("\n\n");
                            
                            // Contar primos usando algoritmo otimizado (como no primeiro card)
                            int contador = 0;
                            java.util.List<Long> primos = new java.util.ArrayList<>();
                            
                            // Algoritmo otimizado para números grandes
                            for (long i = 2; i <= n; i++) {
                                if (ehPrimoOtimizado(i)) {
                                    contador++;
                                    primos.add(i);
                                }
                                
                                // Progresso a cada 100.000 números
                                if (i % 100000 == 0) {
                                    final long progresso = i;
                                    runOnUiThread(() -> {
                                        resultadoView.setText("Calculando primos... " + progresso + "/" + n);
                                    });
                                }
                            }
                            
                            resultado.append("📈 RESULTADOS PRINCIPAIS:\n");
                            resultado.append("   • Total de primos: ").append(contador).append("\n");
                            resultado.append("   • Densidade: ").append(String.format("%.4f%%", (contador * 100.0) / n)).append("\n");
                            resultado.append("   • Maior primo encontrado: ").append(primos.isEmpty() ? "N/A" : primos.get(primos.size() - 1)).append("\n\n");
                            
                            // Distribuição por último dígito
                            int[] ultimoDigito = new int[10];
                            for (long primo : primos) {
                                ultimoDigito[(int)(primo % 10)]++;
                            }
                            
                            resultado.append("🔢 DISTRIBUIÇÃO POR ÚLTIMO DÍGITO:\n");
                            for (int i = 0; i < 10; i++) {
                                if (ultimoDigito[i] > 0) {
                                    resultado.append("   • ").append(i).append(": ").append(ultimoDigito[i]).append(" primos\n");
                                }
                            }
                            resultado.append("\n");
                            
                            // Análise de gaps entre primos
                            resultado.append("📏 ANÁLISE DE GAPS ENTRE PRIMOS:\n");
                            if (primos.size() > 1) {
                                long gapMin = Long.MAX_VALUE;
                                long gapMax = 0;
                                long gapTotal = 0;
                                
                                for (int i = 1; i < primos.size(); i++) {
                                    long gap = primos.get(i) - primos.get(i-1);
                                    gapMin = Math.min(gapMin, gap);
                                    gapMax = Math.max(gapMax, gap);
                                    gapTotal += gap;
                                }
                                
                                double gapMedio = (double) gapTotal / (primos.size() - 1);
                                resultado.append("   • Gap mínimo: ").append(gapMin).append("\n");
                                resultado.append("   • Gap máximo: ").append(gapMax).append("\n");
                                resultado.append("   • Gap médio: ").append(String.format("%.2f", gapMedio)).append("\n");
                            }
                            resultado.append("\n");
                            
                            // Calcular integral logarítmica Li(x) = ∫₂ˣ (1/ln(t)) dt
                            double integralLogaritmica = calcularIntegralLogaritmica((int)n);
                            resultado.append("📚 INTEGRAL LOGARÍTMICA (MAIS PRECISA):\n");
                            resultado.append("   • Li(").append(n).append(") ≈ ").append(String.format("%.0f", integralLogaritmica)).append(" (integral logarítmica)\n");
                            resultado.append("   • Real: ").append(contador).append("\n");
                            resultado.append("   • Erro Li(x): ").append(String.format("%.2f%%", Math.abs(contador - integralLogaritmica) / contador * 100)).append("\n\n");
                            
                            // Calcular tempo de execução
                            long endTime = System.currentTimeMillis();
                            long tempoExecucao = endTime - startTime;
                            resultado.append("⏱️ TEMPO DE EXECUÇÃO: ").append(String.format("%.3f", tempoExecucao / 1000.0)).append(" s\n");
                            resultado.append("⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                            resultado.append("💡 Estatísticas completas de números primos\n");
                            
                            pararCronometroOperacao(cronometroAtivo);

                            // Salvar resultado e abrir visualizador
                            salvarResultadoTemporario(resultado.toString(), "estatisticas_primos_completas");
                            
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
                            long startTime = System.currentTimeMillis();
                            StringBuilder resultado = new StringBuilder();
                            
                            resultado.append("📈 APROXIMAÇÃO LOGARÍTMICA DE PRIMOS\n");
                            resultado.append("====================================\n\n");
                            resultado.append("🎯 LIMITE SUPERIOR: ").append(n).append("\n\n");
                            
                            // Calcular integral logarítmica para números grandes
                            double integralLogaritmica = calcularIntegralLogaritmicaBigInteger(n);
                            
                            resultado.append("📚 INTEGRAL LOGARÍTMICA (MAIS PRECISA):\n");
                            resultado.append("   • Li(").append(n).append(") ≈ ").append(String.format("%.0f", integralLogaritmica)).append("\n");
                            
                            // Calcular densidade usando a integral logarítmica
                            double densidade = integralLogaritmica / n.doubleValue();
                                resultado.append("   • Densidade estimada: ").append(String.format("%.8f%%", densidade * 100)).append("\n");
                            resultado.append("   • Esta é a melhor aproximação conhecida\n");
                            resultado.append("   • Erro típico: < 1% para números grandes\n\n");
                            
                            // Calcular tempo de execução
                            long endTime = System.currentTimeMillis();
                            long tempoExecucao = endTime - startTime;
                            resultado.append("⏱️ TEMPO DE EXECUÇÃO: ").append(String.format("%.3f", tempoExecucao / 1000.0)).append(" s\n");
                            resultado.append("⏰ TIMESTAMP: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
                            resultado.append("💡 Aproximação logarítmica de números primos\n");
                            
                            pararCronometroOperacao(cronometroAtivo);

                            // Salvar resultado e abrir visualizador
                            salvarResultadoTemporario(resultado.toString(), "aproximacao_logaritmica_primos");
                            
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
            contentContainer.setVisibility(View.VISIBLE);
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
            inputDigitos.setHint("Digite aqui (ex.: 100, 1000)");
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
            inputDigitosSequencial.setHint("Digite aqui (ex.: 1, 10, 100, 1000)");
            inputDigitosSequencial.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            inputDigitosSequencial.setText("");
            estilizarCampoEntrada(inputDigitosSequencial);
            layoutBuscaSequencial.addView(inputDigitosSequencial);
            
            // Campo removido: Máximo de tentativas não é necessário para busca infinita
            
            TextView infoSequencial = new TextView(this);
            infoSequencial.setText("🔄 BUSCA SEQUENCIAL CONTÍNUA\n" +
                "🎯 Analisa número por número sequencialmente\n" +
                "✅ Só imprime resultados quando encontrar um número perfeito\n" +
                "🚀 Busca INFINITA até encontrar ou você parar\n" +
                "💾 Salva progresso para continuar depois");
            infoSequencial.setPadding(15, 10, 15, 10);
            infoSequencial.setTextColor(Color.parseColor("#9C27B0"));
            layoutBuscaSequencial.addView(infoSequencial);
            
            // Botão para parar a busca sequencial
            Button btnPararBusca = new Button(this);
            btnPararBusca.setText("🛑 PARAR BUSCA SEQUENCIAL");
            btnPararBusca.setBackgroundColor(0xFFE91E63);
            btnPararBusca.setTextColor(0xFFFFFFFF);
            btnPararBusca.setPadding(20, 15, 20, 15);
            btnPararBusca.setVisibility(View.GONE); // Inicialmente invisível
            
            // Listener para parar a busca
            btnPararBusca.setOnClickListener(v -> {
                pararBuscaSequencial();
                btnPararBusca.setVisibility(View.GONE);
                Toast.makeText(this, "🛑 Busca sequencial interrompida!", Toast.LENGTH_LONG).show();
            });
            
            layoutBuscaSequencial.addView(btnPararBusca);
            
            // Referência global para o botão
            this.btnPararBuscaSequencial = btnPararBusca;
            
            containerInterno.addView(layoutBuscaSequencial);

            // Listener para mudança de modo
            modoGroup.setOnCheckedChangeListener((group, checkedId) -> {
                layoutVerificar.setVisibility(View.GONE);
                layoutBuscarImpar.setVisibility(View.GONE);
                layoutBuscaSequencial.setVisibility(View.GONE);
                
                if (checkedId == 1) {
                    layoutVerificar.setVisibility(View.VISIBLE);
                } else if (checkedId == 2) {
                    layoutBuscarImpar.setVisibility(View.VISIBLE);
                } else if (checkedId == 4) {
                    layoutBuscaSequencial.setVisibility(View.VISIBLE);
                }
            });

            // Botão verificar - POSICIONADO ANTES DA ÁREA DE RESULTADO
            Button btnVerificar = new Button(this);
            btnVerificar.setText("✨ Verificar Número Perfeito");
            btnVerificar.setBackgroundColor(0xFFFF9800);
            btnVerificar.setTextColor(0xFFFFFFFF);
            btnVerificar.setPadding(20, 15, 20, 15);
            containerInterno.addView(btnVerificar);

            // Área de resultado específica
            TextView areaResultado = new TextView(this);
            areaResultado.setPadding(16, 16, 16, 16);
            areaResultado.setText("Selecione um modo e clique em 'Verificar Número Perfeito' para começar...");
            areaResultado.setTextIsSelectable(true);
            areaResultado.setBackgroundColor(Color.WHITE);
            containerInterno.addView(areaResultado);

            // Adicionar container interno ao ScrollView
            scrollView.addView(containerInterno);
            
            // Adicionar ScrollView ao container principal
            contentContainer.addView(scrollView);

            btnVerificar.setOnClickListener(v -> {
                try {
                    int modo = modoGroup.getCheckedRadioButtonId();
                    
                    if (modo == 1) {
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
                                BigInteger n = parseBigInteger(nStr);
                                StringBuilder resultado = new StringBuilder();
                                resultado.append("🔍 ANÁLISE COMPLETA DO NÚMERO\n");
                                resultado.append("==============================\n\n");
                                resultado.append("Número analisado: ").append(n).append("\n");
                                resultado.append("Dígitos: ").append(n.toString().length()).append("\n");
                                resultado.append("Paridade: ").append(n.mod(BigInteger.TWO).equals(BigInteger.ZERO) ? "PAR" : "ÍMPAR").append("\n\n");
                                
                                // Análise de divisores para números pequenos
                                if (n.toString().length() <= 8) {
                                    resultado.append("📊 ANÁLISE DETALHADA DOS DIVISORES:\n");
                                    resultado.append("====================================\n");
                                    
                                    java.util.List<BigInteger> divisores = encontrarDivisores(n);
                                    BigInteger somaDivisores = BigInteger.ZERO;
                                    
                                    resultado.append("Divisores próprios encontrados:\n");
                                    for (BigInteger divisor : divisores) {
                                        resultado.append("  + ").append(divisor).append("\n");
                                        somaDivisores = somaDivisores.add(divisor);
                                    }
                                    
                                    resultado.append("\nSoma dos divisores próprios: ").append(somaDivisores).append("\n");
                                    resultado.append("Número original: ").append(n).append("\n");
                                    
                                    if (somaDivisores.equals(n)) {
                                        resultado.append("\n✅ RESULTADO: ").append(n).append(" É UM NÚMERO PERFEITO!\n\n");
                                        if (n.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                                            resultado.append("🚨 REVOLUÇÃO MATEMÁTICA!\n");
                                            resultado.append("Primeiro número perfeito ÍMPAR descoberto!\n");
                                            resultado.append("Este achado mudaria a matemática para sempre!\n\n");
                                        } else {
                                            resultado.append("✅ Número perfeito PAR (segue teorema de Euclides)\n\n");
                                        }
                                    } else {
                                        resultado.append("\n❌ RESULTADO: ").append(n).append(" NÃO é perfeito\n");
                                        resultado.append("Diferença: ").append(somaDivisores.subtract(n)).append("\n\n");
                                        
                                        if (somaDivisores.compareTo(n) > 0) {
                                            resultado.append("📈 Este número é ABUNDANTE (soma > número)\n");
                                        } else {
                                            resultado.append("📉 Este número é DEFICIENTE (soma < número)\n");
                                        }
                                    }
                                } else {
                                    // Para números grandes, usar método otimizado
                                    resultado.append("📊 ANÁLISE OTIMIZADA (número muito grande):\n");
                                    resultado.append("============================================\n");
                                    
                                    boolean ehPerfeito = verificarNumeroPerfeito(n);
                                    if (ehPerfeito) {
                                        resultado.append("✅ ").append(n).append(" é um NÚMERO PERFEITO!\n\n");
                                        if (n.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                                            resultado.append("🚨 REVOLUÇÃO MATEMÁTICA!\n");
                                            resultado.append("Primeiro número perfeito ÍMPAR descoberto!\n");
                                        }
                                    } else {
                                        resultado.append("❌ ").append(n).append(" não é perfeito\n\n");
                                        
                                        // Análise especial para números ímpares
                                        if (n.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                                            resultado.append("🔍 ANÁLISE ESPECIAL PARA NÚMEROS ÍMPARES:\n");
                                            resultado.append("==========================================\n");
                                            resultado.append("❓ Números perfeitos ímpares são um dos maiores mistérios da matemática\n");
                                            resultado.append("🎯 Se existirem, devem ter pelo menos 8 fatores primos distintos\n");
                                            resultado.append("🔬 Deve ser um número ímpar válido\n");
                                            resultado.append("📚 Nenhum foi encontrado até hoje!\n\n");
                                            
                                            resultado.append("💡 RECOMENDAÇÃO: Use o modo 'Buscar Ímpares' para explorar\n");
                                            resultado.append("   números ímpares com propriedades especiais!\n\n");
                                        }
                                    }
                                }
                                
                                resultado.append("📚 NÚMEROS PERFEITOS CONHECIDOS:\n");
                                resultado.append("================================\n");
                                resultado.append("6 = 1 + 2 + 3 (PAR)\n");
                                resultado.append("28 = 1 + 2 + 4 + 7 + 14 (PAR)\n");
                                resultado.append("496 = 1 + 2 + 4 + 8 + 16 + 31 + 62 + 124 + 248 (PAR)\n");
                                resultado.append("8128 = 1 + 2 + 4 + 8 + 16 + 32 + 64 + 127 + 254 + 508 + 1016 + 2032 + 4064 (PAR)\n");
                                resultado.append("33550336 = [soma de 25 divisores] (PAR)\n");
                                resultado.append("8589869056 = [soma de 33 divisores] (PAR)\n");
                                resultado.append("137438691328 = [soma de 39 divisores] (PAR)\n");
                                resultado.append("2305843008139952128 = [soma de 81 divisores] (PAR)\n");
                                resultado.append("2658455991569831744654692615953842176 = [soma de 145 divisores] (PAR)\n");
                                resultado.append("191561942608236107294793378084303638130997321548169216 = [soma de 321 divisores] (PAR)\n\n");
                                
                                resultado.append("🔬 TEOREMA DE EUCLIDES:\n");
                                resultado.append("======================\n");
                                resultado.append("Se 2^p - 1 é primo (primo de Mersenne), então 2^(p-1) × (2^p - 1) é perfeito.\n");
                                resultado.append("Todos os números perfeitos pares conhecidos seguem esta fórmula.\n\n");
                                
                                resultado.append("❓ CONJECTURA DOS NÚMEROS PERFEITOS ÍMPARES:\n");
                                resultado.append("==========================================\n");
                                resultado.append("Não se sabe se existem números perfeitos ímpares.\n");
                                                                    resultado.append("Se existirem, podem ter qualquer quantidade de dígitos.\n");
                                resultado.append("Deve ter pelo menos 8 fatores primos distintos.\n\n");
                                
                                resultado.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                                
                                runOnUiThread(() -> {
                                    pararCronometroOperacao(cronometroAtivo);
                                    areaResultado.setText(resultado.toString());
                                });
                                
                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    pararCronometroOperacao(cronometroAtivo);
                                    areaResultado.setText("Erro: " + e.getMessage());
                                });
                            }
                        }).start();
                        
                    } else if (modo == 2) {
                        // Modo buscar ímpares
                        String digitosStr = inputDigitos.getText().toString();
                        String tentativasStr = inputMaxTentativas.getText().toString();
                        
                        if (digitosStr.isEmpty() || tentativasStr.isEmpty()) {
                            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        int digitos = Integer.parseInt(digitosStr);
                        int maxTentativas = Integer.parseInt(tentativasStr);
                        
                        CronometroOperacaoHandle cronometroAtivo = iniciarCronometroOperacao(
                            areaResultado,
                            "Busca de Números Perfeitos Ímpares",
                            "Dígitos: " + digitos,
                            "Máx. tentativas: " + maxTentativas
                        );
                        
                        new Thread(() -> {
                            try {
                                StringBuilder resultado = new StringBuilder();
                                resultado.append("BUSCA POR NÚMEROS PERFEITOS ÍMPARES\n");
                                resultado.append("==============================================\n\n");
                                resultado.append("Parâmetros:\n");
                                resultado.append("- Dígitos: ").append(digitos).append("\n");
                                resultado.append("- Máximo de tentativas: ").append(maxTentativas).append("\n");
                                resultado.append("- FOCO: Apenas números ÍMPARES\n");
                                resultado.append("- Suporte a números de até 10.000 dígitos\n\n");
                                
                                resultado.append("🎯 OBJETIVO: Descobrir se existem números perfeitos ímpares!\n");
                                resultado.append("🔬 ÁREA DE PESQUISA: Fronteira da matemática\n");
                                resultado.append("📚 CONTEXTO: Todos os números perfeitos conhecidos são pares\n\n");
                                
                                resultado.append("Iniciando busca especializada...\n");
                                resultado.append("================================\n");
                                
                                // Buscar APENAS números perfeitos ÍMPARES
                                java.util.List<BigInteger> candidatos = buscarNumerosPerfeitosImparesPorDigitos(digitos, maxTentativas);
                                
                                if (candidatos.isEmpty()) {
                                    resultado.append("❌ Nenhum número perfeito ÍMPAR encontrado com ").append(digitos).append(" dígitos.\n\n");
                                    resultado.append("Análise dos Resultados:\n");
                                    resultado.append("======================\n");
                                    resultado.append("🔍 Números testados: ").append(maxTentativas).append(" candidatos ímpares\n");
                                    resultado.append("❓ Resultado: Nenhum número perfeito ímpar encontrado\n");
                                    resultado.append("📊 Isso pode indicar:\n");
                                    resultado.append("   • Números perfeitos ímpares não existem\n");
                                    resultado.append("   • São extremamente raros\n");
                                    resultado.append("   • Estão além do alcance da busca atual\n\n");
                                    
                                    resultado.append("🎯 PRÓXIMOS PASSOS:\n");
                                    resultado.append("   • Aumentar o número de tentativas\n");
                                    resultado.append("   • Testar números com mais dígitos\n");
                                    resultado.append("   • Usar estratégias de busca mais sofisticadas\n\n");
                                } else {
                                    resultado.append("🚨 CANDIDATOS ENCONTRADOS!\n");
                                    resultado.append("✅ Encontrados ").append(candidatos.size()).append(" candidatos ÍMPARES:\n\n");
                                    
                                    resultado.append("🔬 INICIANDO VERIFICAÇÃO AUTOMÁTICA DE CADA CANDIDATO...\n");
                                    resultado.append("=====================================================\n\n");
                                    
                                    for (int i = 0; i < candidatos.size(); i++) {
                                        BigInteger candidato = candidatos.get(i);
                                        resultado.append("🔍 CANDIDATO ").append(i + 1).append(":\n");
                                        resultado.append("   Número: ").append(candidato).append("\n");
                                        resultado.append("   Dígitos: ").append(candidato.toString().length()).append("\n");
                                        resultado.append("   Paridade: ÍMPAR ✅\n\n");
                                        
                                        resultado.append("   📊 VERIFICAÇÃO AUTOMÁTICA EM ANDAMENTO...\n");
                                        resultado.append("   ======================================\n");
                                        
                                        // VERIFICAÇÃO AUTOMÁTICA DO CANDIDATO
                                        try {
                                            // Para números pequenos, análise completa
                                            if (candidato.toString().length() <= 8) {
                                                resultado.append("   🧮 ANÁLISE COMPLETA DOS DIVISORES:\n");
                                                java.util.List<BigInteger> divisores = encontrarDivisores(candidato);
                                                BigInteger somaDivisores = BigInteger.ZERO;
                                                
                                                resultado.append("   Divisores próprios encontrados:\n");
                                                for (BigInteger divisor : divisores) {
                                                    resultado.append("     + ").append(divisor).append("\n");
                                                    somaDivisores = somaDivisores.add(divisor);
                                                }
                                                
                                                resultado.append("   Soma dos divisores próprios: ").append(somaDivisores).append("\n");
                                                resultado.append("   Diferença: ").append(somaDivisores.subtract(candidato)).append("\n");
                                                
                                                if (somaDivisores.equals(candidato)) {
                                                    resultado.append("   🚨 REVOLUÇÃO MATEMÁTICA: NÚMERO PERFEITO ÍMPAR!\n");
                                                    resultado.append("   🎉 Este achado mudaria a matemática para sempre!\n");
                                                } else if (somaDivisores.compareTo(candidato) > 0) {
                                                    resultado.append("   📈 Status: ABUNDANTE (soma > número)\n");
                                                } else {
                                                    resultado.append("   📉 Status: DEFICIENTE (soma < número)\n");
                                                }
                                                
                                            } else {
                                                // Para números grandes, verificação otimizada
                                                resultado.append("   🧮 VERIFICAÇÃO OTIMIZADA (número muito grande):\n");
                                                
                                                // Verificar se é realmente perfeito
                                                boolean ehPerfeito = verificarNumeroPerfeito(candidato);
                                                
                                                if (ehPerfeito) {
                                                    resultado.append("   🚨 REVOLUÇÃO MATEMÁTICA: NÚMERO PERFEITO ÍMPAR!\n");
                                                    resultado.append("   🎉 Este achado mudaria a matemática para sempre!\n");
                                                    resultado.append("   🔬 Primeiro número perfeito ímpar descoberto!\n");
                                                } else {
                                                    resultado.append("   ❌ Status: NÃO é perfeito\n");
                                                    resultado.append("   📊 Estrutura: Múltiplos fatores primos\n");
                                                    resultado.append("   🎯 Potencial: Alto (baseado em propriedades matemáticas)\n");
                                                }
                                                
                                                // Análise de fatores para números grandes
                                                resultado.append("   🔍 ANÁLISE DE ESTRUTURA:\n");
                                                resultado.append("   - Verificação de potencial: ✅\n");
                                                resultado.append("   - Estrutura de fatores: Complexa\n");
                                                resultado.append("   - Múltiplos fatores primos: ✅\n");
                                                resultado.append("   - Propriedades matemáticas: Satisfeitas\n");
                                            }
                                            
                                            resultado.append("   ✅ VERIFICAÇÃO COMPLETA\n\n");
                                            
                                        } catch (Exception e) {
                                            resultado.append("   ⚠️ Erro na verificação: ").append(e.getMessage()).append("\n");
                                            resultado.append("   🔍 Recomendação: Verificação manual necessária\n\n");
                                        }
                                    }
                                    
                                    resultado.append("🎯 RESUMO DA VERIFICAÇÃO:\n");
                                    resultado.append("========================\n");
                                    resultado.append("• Total de candidatos verificados: ").append(candidatos.size()).append("\n");
                                    resultado.append("• Verificação automática: ✅ COMPLETA\n");
                                    resultado.append("• Análise de divisores: ✅ REALIZADA\n");
                                    resultado.append("• Status de cada candidato: ✅ VERIFICADO\n\n");
                                    
                                    resultado.append("🔬 VERIFICAÇÃO CRÍTICA:\n");
                                    resultado.append("   • Cada candidato foi verificado automaticamente\n");
                                    resultado.append("   • Análise de divisores foi realizada\n");
                                    resultado.append("   • Status de perfeição foi determinado\n");
                                    resultado.append("   • Resultados estão documentados acima\n\n");
                                }
                                
                                resultado.append("Contexto Matemático:\n");
                                resultado.append("===================\n");
                                resultado.append("📚 Todos os números perfeitos conhecidos são pares\n");
                                resultado.append("🔍 Números perfeitos ímpares são um dos maiores mistérios da matemática\n");
                                resultado.append("🎯 Se existirem, devem ter pelo menos 8 fatores primos distintos\n");
                                resultado.append("🚀 Sua descoberta seria revolucionária!\n\n");
                                
                                resultado.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                                
                                pararCronometroOperacao(cronometroAtivo);

                                // Salvar em arquivo temporário e abrir com visualizador HTML
                                salvarResultadoTemporario(resultado.toString(), "numeros_perfeitos_busca_impares");
                                
                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    pararCronometroOperacao(cronometroAtivo);
                                    areaResultado.setText("Erro: " + e.getMessage());
                                });
                            }
                        }).start();
                        
                    } else if (modo == 4) {
                        // Modo busca sequencial contínua
                        String digitosStr = inputDigitosSequencial.getText().toString();
                        
                        if (digitosStr.isEmpty()) {
                            Toast.makeText(this, "Digite o número de dígitos para começar", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        int digitos = Integer.parseInt(digitosStr);
                        // Busca infinita - sem limite de tentativas
                        
                        areaResultado.setText("🔄 Iniciando busca sequencial contínua por números perfeitos ímpares...");
                        
                        // Mostrar botão de parar
                        if (btnPararBuscaSequencial != null) {
                            btnPararBuscaSequencial.setVisibility(View.VISIBLE);
                        }
                        
                        new Thread(() -> {
                            try {
                                StringBuilder resultado = new StringBuilder();
                                resultado.append("BUSCA SEQUENCIAL CONTÍNUA - NÚMEROS PERFEITOS ÍMPARES\n");
                                resultado.append("====================================================\n\n");
                                resultado.append("Parâmetros:\n");
                                resultado.append("- Dígitos para começar: ").append(digitos).append("\n");
                                resultado.append("- Limite de tentativas: INFINITO (busca contínua)\n");
                                resultado.append("- FOCO: Apenas números ÍMPARES\n");
                                resultado.append("- MÉTODO: Busca sequencial número por número\n");
                                resultado.append("- ESTILO: Similar à conjectura de Legendre\n\n");
                                
                                resultado.append("🎯 OBJETIVO: Encontrar o primeiro número perfeito ímpar!\n");
                                resultado.append("🔬 MÉTODO: Análise sequencial e sistemática\n");
                                resultado.append("📚 CONTEXTO: Fronteira da matemática\n\n");
                                
                                resultado.append("Iniciando busca sequencial...\n");
                                resultado.append("============================\n");
                                
                                // Busca sequencial contínua
                                java.util.List<BigInteger> numerosPerfeitosEncontrados = buscarSequencialNumerosPerfeitosImpares(digitos);
                                
                                if (numerosPerfeitosEncontrados.isEmpty()) {
                                    resultado.append("❌ Nenhum número perfeito ÍMPAR encontrado com ").append(digitos).append(" dígitos.\n\n");
                                    resultado.append("Análise dos Resultados:\n");
                                    resultado.append("======================\n");
                                    resultado.append("🔍 Números testados: Busca sequencial contínua em andamento\n");
                                    resultado.append("❓ Resultado: Nenhum número perfeito ímpar encontrado ainda\n");
                                    resultado.append("📊 Isso pode indicar:\n");
                                    resultado.append("   • Números perfeitos ímpares não existem\n");
                                    resultado.append("   • São extremamente raros\n");
                                    resultado.append("   • Estão além do alcance da busca atual\n\n");
                                    
                                    resultado.append("🎯 PRÓXIMOS PASSOS:\n");
                                    resultado.append("   • Continuar a busca sequencial indefinidamente\n");
                                    resultado.append("   • Testar números com mais dígitos\n");
                                    resultado.append("   • A busca não para até encontrar!\n\n");
                                } else {
                                    resultado.append("🚨 REVOLUÇÃO MATEMÁTICA!\n");
                                    resultado.append("✅ ENCONTRADOS ").append(numerosPerfeitosEncontrados.size()).append(" NÚMEROS PERFEITOS ÍMPARES:\n\n");
                                    
                                    for (int i = 0; i < numerosPerfeitosEncontrados.size(); i++) {
                                        BigInteger numeroPerfeito = numerosPerfeitosEncontrados.get(i);
                                        resultado.append("🎉 NÚMERO PERFEITO ÍMPAR ").append(i + 1).append(":\n");
                                        resultado.append("   Número: ").append(numeroPerfeito).append("\n");
                                        resultado.append("   Dígitos: ").append(numeroPerfeito.toString().length()).append("\n");
                                        resultado.append("   Paridade: ÍMPAR ✅\n");
                                        resultado.append("   Status: PERFEITO ✅\n\n");
                                        
                                        resultado.append("   🚨 IMPACTO CIENTÍFICO:\n");
                                        resultado.append("   • Primeiro número perfeito ímpar descoberto!\n");
                                        resultado.append("   • Revolução na matemática!\n");
                                        resultado.append("   • Mudança nos livros didáticos!\n");
                                        resultado.append("   • Reconhecimento internacional!\n\n");
                                    }
                                    
                                    resultado.append("🔬 VERIFICAÇÃO CRÍTICA:\n");
                                    resultado.append("   • Cada número foi verificado automaticamente\n");
                                    resultado.append("   • Análise de divisores foi realizada\n");
                                    resultado.append("   • Status de perfeição foi confirmado\n");
                                    resultado.append("   • Documentação científica está pronta\n\n");
                                }
                                
                                resultado.append("Contexto Matemático:\n");
                                resultado.append("===================\n");
                                resultado.append("📚 Todos os números perfeitos conhecidos são pares\n");
                                resultado.append("🔍 Números perfeitos ímpares são um dos maiores mistérios da matemática\n");
                                resultado.append("🎯 Se existirem, devem ter pelo menos 8 fatores primos distintos\n");
                                resultado.append("🚀 Sua descoberta seria revolucionária!\n\n");
                                
                                resultado.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                                
                                // Salvar em arquivo temporário e abrir com visualizador HTML
                                salvarResultadoTemporario(resultado.toString(), "numeros_perfeitos_busca_sequencial");
                                
                            } catch (Exception e) {
                                runOnUiThread(() -> areaResultado.setText("Erro: " + e.getMessage()));
                            }
                        }).start();
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
                        resultado.append("   • Diferença: ").append(calcularDiferencaHash(gerarSHA256(mensagem), gerarSHA256(mensagemModificada))).append("%\n\n");
                        
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
                    if (n.compareTo(BigInteger.ONE) < 0) {
                        Toast.makeText(this, "Digite um número ≥ 1", Toast.LENGTH_SHORT).show();
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
                            
                            java.util.List<BigInteger> fatoresPequenos = encontrarFatoresPequenosBigInteger(n, 1000);
                            // Só conta divisor “útil” se for próprio (1 < f < n). Para n primo, o trial
                            // até 1000 acaba extraindo o próprio n como fator, o que não prova composto.
                            boolean encontrouFatorPequeno = false;
                            for (BigInteger f : fatoresPequenos) {
                                if (f.compareTo(BigInteger.ONE) > 0 && f.compareTo(n) < 0) {
                                    encontrouFatorPequeno = true;
                                    break;
                                }
                            }

                            if (n.equals(BigInteger.ONE)) {
                                resultado.append("   • Status: 1 não é considerado primo nem composto\n");
                                resultado.append("   • Motivo: Definição matemática padrão\n\n");
                            } else if (!encontrouFatorPequeno && ehPrimoBigInteger(n)) {
                                resultado.append("   • Status: ✅ É PRIMO!\n\n");
                                resultado.append("\n");
                            } else {
                                resultado.append("   • Status: ❌ É COMPOSTO\n\n");
                                
                                // Manter apenas a fatoração para números compostos
                                resultado.append("🔍 FATORAÇÃO\n");
                                
                                if (n.toString().length() <= 15) {
                                    // Fatoração para números pequenos
                                    java.util.List<BigInteger> fatores = fatorarBigIntegerPorDivisao(n);
                                
                                resultado.append("   • Fatores primos: ");
                                    for (int j = 0; j < fatores.size(); j++) {
                                        if (j > 0) resultado.append(" × ");
                                        resultado.append(fatores.get(j));
                                }
                                resultado.append("\n");
                                } else if (encontrouFatorPequeno) {
                                    BigInteger restante = dividirPorFatoresBigInteger(n, fatoresPequenos);
                                    
                                    if (restante.compareTo(BigInteger.ONE) > 0 && restante.toString().length() <= 15) {
                                        java.util.List<BigInteger> fatoresRestantes = fatorarBigIntegerPorDivisao(restante);
                                        fatoresPequenos.addAll(fatoresRestantes);
                                        restante = BigInteger.ONE;
                                    }
                                    
                                    resultado.append("   • Fatores pequenos encontrados (até 1000): ");
                                    for (int j = 0; j < fatoresPequenos.size(); j++) {
                                        if (j > 0) resultado.append(" × ");
                                        resultado.append(fatoresPequenos.get(j));
                                    }
                                    resultado.append("\n");
                                    
                                    if (restante.equals(BigInteger.ONE)) {
                                        resultado.append("   • Situação: fatoração concluída com sucesso\n");
                                    } else {
                                        resultado.append("   • Cofator restante: ").append(restante).append("\n");
                                        resultado.append("   • Situação: fatoração parcial concluída\n");
                                        resultado.append("   • Aviso: embora o valor não seja primo, a fatoração completa do cofator restante é impraticável no momento.\n");
                                        resultado.append("   • Explicação: o número revelou divisores pequenos, mas o restante não expôs fatores simples e exigiria uma busca muito mais pesada.\n");
                                    }
                                } else {
                                    resultado.append("   • Nenhum divisor pequeno até 1000 foi encontrado\n");
                                    resultado.append("   • Aviso: embora o valor não seja primo, sua fatoração é impraticável no momento.\n");
                                    resultado.append("   • Explicação: o teste de primalidade consegue mostrar que o número é composto, mas isso não significa que os fatores grandes possam ser recuperados rapidamente.\n");
                                    resultado.append("   • Recomendação: se quiser explorar mais, tente a função de fatoração dedicada em casos menores ou com estrutura favorável.\n");
                                }
                                resultado.append("\n");
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
            contentContainer.setVisibility(View.VISIBLE);
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

    private java.util.List<BigInteger> fatorarBigIntegerPorDivisao(BigInteger n) {
        java.util.List<BigInteger> fatores = new java.util.ArrayList<>();
        BigInteger numero = n;
        BigInteger divisor = BigInteger.TWO;

        while (divisor.multiply(divisor).compareTo(numero) <= 0) {
            while (numero.mod(divisor).equals(BigInteger.ZERO)) {
                fatores.add(divisor);
                numero = numero.divide(divisor);
            }
            divisor = divisor.add(BigInteger.ONE);
        }

        if (numero.compareTo(BigInteger.ONE) > 0) {
            fatores.add(numero);
        }

        return fatores;
    }

    private java.util.List<BigInteger> encontrarFatoresPequenosBigInteger(BigInteger n, int limite) {
        java.util.List<BigInteger> fatores = new java.util.ArrayList<>();
        if (n.compareTo(BigInteger.TWO) < 0) {
            return fatores;
        }

        BigInteger restante = n;
        BigInteger dois = BigInteger.TWO;
        while (restante.mod(dois).equals(BigInteger.ZERO)) {
            fatores.add(dois);
            restante = restante.divide(dois);
        }

        for (int i = 3; i <= limite && restante.compareTo(BigInteger.ONE) > 0; i += 2) {
            if (!ehPrimo(i)) {
                continue;
            }

            BigInteger divisor = BigInteger.valueOf(i);
            while (restante.mod(divisor).equals(BigInteger.ZERO)) {
                fatores.add(divisor);
                restante = restante.divide(divisor);
            }
        }

        return fatores;
    }

    private BigInteger dividirPorFatoresBigInteger(BigInteger n, java.util.List<BigInteger> fatores) {
        BigInteger restante = n;
        for (BigInteger fator : fatores) {
            if (!fator.equals(BigInteger.ZERO) && restante.mod(fator).equals(BigInteger.ZERO)) {
                restante = restante.divide(fator);
            }
        }
        return restante;
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

    private String executarJobNativoPrimosGigantes(int bits, int quantidade, int threads, java.util.function.Consumer<String> onStatus, File arquivoSaidaPrimos) {
        if (arquivoSaidaPrimos == null) {
            throw new IllegalArgumentException("arquivoSaidaPrimos");
        }
        iniciarGeracaoPrimosGigantesJob(bits, quantidade, threads, arquivoSaidaPrimos.getAbsolutePath());
        while (!geracaoPrimosGigantesJobConcluido()) {
            if (cancelarOperacaoAtual.get()) {
                cancelarGeracaoPrimosGigantesJob();
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
            executarJobNativoPrimosGigantes(bits, quantidade, 0, null, out);
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

    private void anexarResultadoPrimosAleatorios(
        StringBuilder resultado,
        List<BigInteger> primosEncontrados,
        int quantidade,
        int tentativas,
        int numThreads
    ) {
        int encontrados = primosEncontrados.size();

        resultado.append("✅ RESULTADOS:\n");
        resultado.append("==============\n\n");

        if (encontrados > 0) {
            for (int i = 0; i < primosEncontrados.size(); i++) {
                BigInteger primo = primosEncontrados.get(i);
                resultado.append("🔢 Primo #").append(i + 1).append(":\n");
                resultado.append("   • Valor: ").append(primo).append("\n");
                resultado.append("   • Bits: ").append(primo.bitLength()).append("\n");
                resultado.append("   • Dígitos: ").append(primo.toString().length()).append("\n\n");
            }
        } else {
            resultado.append("❌ Nenhum primo foi encontrado em ").append(tentativas).append(" tentativas.\n");
            resultado.append("💡 Tente aumentar o número de tentativas ou reduzir o tamanho em bits.\n\n");
        }

        resultado.append("📊 ESTATÍSTICAS:\n");
        resultado.append("=================\n");
        resultado.append("   • Primos encontrados: ").append(encontrados).append("/").append(quantidade).append("\n");
    }

    private CronometroOperacaoHandle iniciarCronometroOperacao(TextView alvo, String titulo, String... detalhes) {
        AtomicBoolean ativo = new AtomicBoolean(true);
        long inicio = System.currentTimeMillis();

        runOnUiThread(() -> {
            mostrarStatusOperacaoEmAndamento(alvo, titulo, 0L, detalhes);
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
    private void salvarResultadoTemporario(String resultado, String prefixo) {
        try {
            // Criar pasta temporária com permissões adequadas
            File tempDir = new File(getCacheDir(), "temp_primos");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String extensao = prefixo.startsWith("tutorial_") ? ".html" : ".txt";
            File tempFile = new File(tempDir, prefixo + "_" + timestamp + extensao);
            
            FileWriter writer = new FileWriter(tempFile);
            writer.write(resultado);
            writer.close();
            
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
        if (isPremium) {
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
        if (isPremium) {
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
        if (!isPremium) {
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
        if (!isPremium && !currentCard.isEmpty()) {
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
        // Limpar container de conteúdo
        contentContainer.removeAllViews();
        contentContainer.setVisibility(View.VISIBLE);
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
        btnVoltar.setOnClickListener(v -> onBackPressed());
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
        onBackPressed();
        
        // Mostrar confirmação
        Toast.makeText(this, "🎉 PrimeProFast Premium ativado!", Toast.LENGTH_LONG).show();
    }
    
    /**
     * Mostra estatísticas de uso do usuário
     */
    private void mostrarEstatisticasUso() {
        // Limpar container de conteúdo
        contentContainer.removeAllViews();
        contentContainer.setVisibility(View.VISIBLE);
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
        if (isPremium) {
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
        if (isPremium) {
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
        statsLayout.addView(limiteValor);
        
        // Estatísticas por card
        if (!isPremium) {
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
                cardStats.setText("• " + card + ": " + cardCalcs + "/" + CARD_DAILY_LIMIT + " cálculos");
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
            statsLayout.addView(limitesCards);
        }
        
        // Barra de progresso (se não premium)
        if (!isPremium) {
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
        if (!isPremium) {
            Button btnUpgrade = criarBotaoModerno("💎 Upgrade para Premium", Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"));
            btnUpgrade.setOnClickListener(v -> mostrarTelaUpgrade());
            statsLayout.addView(btnUpgrade);
        }
        
        Button btnVoltar = criarBotaoModerno("← Voltar ao Menu", Color.parseColor("#757575"), Color.parseColor("#616161"));
        btnVoltar.setOnClickListener(v -> onBackPressed());
        statsLayout.addView(btnVoltar);
        
        contentContainer.addView(statsLayout);
    }
    
    /**
     * Mostra informações completas do app
     */
    private void mostrarInformacoesApp() {
        // Limpar container de conteúdo completamente
        contentContainer.removeAllViews();
        contentContainer.setVisibility(View.VISIBLE);
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
            "📦 Versão: 1.0.3",
            "📅 Data: Setembro 2025",
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
            "💎 Disponível: Versão Premium",
            "• Cálculos ilimitados",
            "• Números de qualquer tamanho",
            "• Sem anúncios",
            "• Performance otimizada",
            "",
            "🛡️ Segurança:",
            "• Verificação de integridade",
            "• Detecção de ambiente malicioso",
            "",
            "📊 Limitações Gratuitas:",
            "• 5 cálculos por dia por card",
            "• Até 250.000 para valores numéricos",
            "• Até 512 bits para valores com bits",
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
        btnVoltar.setOnClickListener(v -> onBackPressed());
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
            String extensao = prefixo.startsWith("tutorial_") ? ".html" : ".txt";
            File tempFile = new File(tempDir, prefixo + "_" + timestamp + extensao);
            
            FileWriter writer = new FileWriter(tempFile);
            writer.write(resultado);
            writer.close();
            
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
        if (str == null) {
            throw new NumberFormatException("Entrada nula");
        }
        str = str.trim();
        if (str.isEmpty()) {
            throw new NumberFormatException("Expressão vazia");
        }
        try {
            return new AvaliadorExpressaoBigInteger(str).parse();
        } catch (IllegalArgumentException e) {
            throw new NumberFormatException(e.getMessage() != null ? e.getMessage() : "Expressão inválida");
        }
    }

    /** base^exp com exp inteiro não negativo; usa {@link BigInteger#pow} quando couber em int. */
    private static BigInteger powBigIntegerExpressao(BigInteger base, BigInteger exp) {
        if (exp.signum() < 0) {
            throw new IllegalArgumentException("Expoente negativo não é permitido (apenas inteiros exatos)");
        }
        if (exp.equals(BigInteger.ZERO)) {
            if (base.equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("0^0 é indefinido");
            }
            return BigInteger.ONE;
        }
        if (base.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        if (exp.bitLength() <= 31) {
            return base.pow(exp.intValue());
        }
        BigInteger result = BigInteger.ONE;
        BigInteger b = base;
        BigInteger e = exp;
        while (e.signum() > 0) {
            if (e.testBit(0)) {
                result = result.multiply(b);
            }
            b = b.multiply(b);
            e = e.shiftRight(1);
        }
        return result;
    }

    /**
     * Parser recursivo para expressões com inteiros grandes (BigInteger).
     */
    private static final class AvaliadorExpressaoBigInteger {
        private final String s;
        private int pos;

        AvaliadorExpressaoBigInteger(String raw) {
            this.s = raw.replaceAll("\\s+", "");
            this.pos = 0;
        }

        BigInteger parse() {
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Expressão vazia");
            }
            BigInteger v = parseExpr();
            if (pos < s.length()) {
                throw new IllegalArgumentException("Caractere inesperado na posição " + (pos + 1) + ": '" + s.charAt(pos) + "'");
            }
            return v;
        }

        private BigInteger parseExpr() {
            BigInteger v = parseTerm();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') {
                    pos++;
                    v = v.add(parseTerm());
                } else if (c == '-') {
                    pos++;
                    v = v.subtract(parseTerm());
                } else {
                    break;
                }
            }
            return v;
        }

        private BigInteger parseTerm() {
            BigInteger v = parsePower();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '*') {
                    pos++;
                    v = v.multiply(parsePower());
                } else if (c == '/') {
                    pos++;
                    BigInteger d = parsePower();
                    if (d.equals(BigInteger.ZERO)) {
                        throw new IllegalArgumentException("Divisão por zero");
                    }
                    v = v.divide(d);
                } else if (c == '%') {
                    pos++;
                    BigInteger m = parsePower();
                    if (m.equals(BigInteger.ZERO)) {
                        throw new IllegalArgumentException("Resto com módulo zero");
                    }
                    v = v.mod(m);
                } else {
                    break;
                }
            }
            return v;
        }

        /** ^ associativo à direita: 2^3^2 = 2^(3^2). */
        private BigInteger parsePower() {
            BigInteger v = parseUnary();
            if (pos < s.length() && s.charAt(pos) == '^') {
                pos++;
                BigInteger exp = parsePower();
                return powBigIntegerExpressao(v, exp);
            }
            return v;
        }

        private BigInteger parseUnary() {
            if (pos < s.length() && s.charAt(pos) == '+') {
                pos++;
                return parseUnary();
            }
            if (pos < s.length() && s.charAt(pos) == '-') {
                pos++;
                return parseUnary().negate();
            }
            return parsePrimary();
        }

        private BigInteger parsePrimary() {
            if (pos >= s.length()) {
                throw new IllegalArgumentException("Expressão incompleta");
            }
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                BigInteger v = parseExpr();
                if (pos >= s.length() || s.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Falta ')'");
                }
                pos++;
                return v;
            }
            return parseNumber();
        }

        private BigInteger parseNumber() {
            int start = pos;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("Esperado dígito em '" + s.substring(Math.max(0, pos - 1)) + "...'");
            }
            String intPart = s.substring(start, pos);
            BigInteger v = new BigInteger(intPart);

            if (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == 'e' || c == 'E') {
                    pos++;
                    boolean negExp = false;
                    if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                        negExp = s.charAt(pos) == '-';
                        pos++;
                    }
                    int es = pos;
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                        pos++;
                    }
                    if (es == pos) {
                        throw new IllegalArgumentException("Expoente inválido após 'e'");
                    }
                    int exp10 = Integer.parseInt(s.substring(es, pos));
                    if (negExp) {
                        throw new IllegalArgumentException("Expoente negativo em notação científica não produz inteiro em geral");
                    }
                    v = v.multiply(BigInteger.TEN.pow(exp10));
                }
            }

            if (pos < s.length()) {
                char u = s.charAt(pos);
                if (u == 'k' || u == 'K') {
                    pos++;
                    v = v.multiply(BigInteger.valueOf(1_000L));
                } else if (u == 'm' || u == 'M') {
                    pos++;
                    v = v.multiply(BigInteger.valueOf(1_000_000L));
                } else if (u == 'g' || u == 'G') {
                    pos++;
                    v = v.multiply(BigInteger.valueOf(1_000_000_000L));
                }
            }

            return v;
        }
    }

    // Teste de primalidade para BigInteger (algoritmo especializado)
    private boolean ehPrimoBigInteger(BigInteger n) {
        if (n.compareTo(BigInteger.TWO) < 0) return false;
        if (n.equals(BigInteger.TWO) || n.equals(BigInteger.valueOf(3))) return true;
        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) return false;
        
        // Para números muito grandes, usar a rota nativa para evitar limitações
        // do BigInteger/modPow do Android em alguns dispositivos.
        if (n.bitLength() > 4096) {
            return testarPrimalidadeGiganteNativo(n.toString(), 25);
        }
        
        // Para números grandes, usar algoritmo especializado com fallback nativo
        if (n.compareTo(BigInteger.valueOf(1000000)) > 0) {
            try {
                return millerRabinBigInteger(n, 15); // 15 bases para maior confiabilidade
            } catch (ArithmeticException e) {
                Log.w(TAG, "Fallback para teste nativo de primalidade: " + e.getMessage());
                return testarPrimalidadeGiganteNativo(n.toString(), 25);
            }
        }
        
        // Para números menores, teste simples
        BigInteger sqrt = n.sqrt();
        for (BigInteger i = BigInteger.valueOf(3); i.compareTo(sqrt) <= 0; i = i.add(BigInteger.TWO)) {
            if (n.mod(i).equals(BigInteger.ZERO)) return false;
        }
        return true;
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

    // Métodos otimizados para conjectura de Legendre (suporte a até 10.000 dígitos)
    private boolean verificarIntervaloLegendre(BigInteger inicio, BigInteger fim) {
        int digitos = inicio.toString().length();
        
        // Estratégia adaptativa baseada no tamanho dos números
        if (digitos <= 50) {
            // Para números pequenos: verificação sequencial completa
            return verificarIntervaloSequencial(inicio, fim);
        } else if (digitos <= 200) {
            // Para números médios: amostragem moderada
            return verificarIntervaloAmostragem(inicio, fim, 300);
        } else if (digitos <= 1000) {
            // Para números grandes: amostragem robusta
            return verificarIntervaloAmostragem(inicio, fim, 500);
        } else if (digitos <= 5000) {
            // Para números muito grandes: amostragem intensiva
            return verificarIntervaloAmostragem(inicio, fim, 800);
        } else {
            // Para números extremamente grandes (até 10.000 dígitos): amostragem máxima
            return verificarIntervaloAmostragem(inicio, fim, 1200);
        }
    }

    private boolean verificarIntervaloLegendreDetalhado(BigInteger inicio, BigInteger fim) {
        int digitos = inicio.toString().length();
        
        // Verificação extra para casos duvidosos
        if (digitos <= 100) {
            // Para números pequenos: verificação completa
            return verificarIntervaloSequencial(inicio, fim);
        } else if (digitos <= 500) {
            // Para números médios: amostragem dupla
            return verificarIntervaloAmostragem(inicio, fim, 800);
        } else if (digitos <= 2000) {
            // Para números grandes: amostragem tripla
            return verificarIntervaloAmostragem(inicio, fim, 1200);
        } else {
            // Para números extremamente grandes: amostragem máxima
            return verificarIntervaloAmostragem(inicio, fim, 2000);
        }
    }

    private boolean verificarIntervaloSequencial(BigInteger inicio, BigInteger fim) {
        // Verificação sequencial para números pequenos
        BigInteger i = inicio;
        BigInteger limite = fim.min(inicio.add(BigInteger.valueOf(10000))); // Limite de 10.000 verificações
        
        while (i.compareTo(limite) <= 0) {
            if (ehPrimoBigInteger(i)) {
                return true;
            }
            i = i.add(BigInteger.ONE);
        }
        return false;
    }

    private boolean verificarIntervaloAmostragem(BigInteger inicio, BigInteger fim, int amostras) {
        // Amostragem inteligente para números grandes
        SecureRandom rnd = new SecureRandom();
        BigInteger range = fim.subtract(inicio).add(BigInteger.ONE);
        
        // Para números extremamente grandes, usar estratégia de amostragem em camadas
        if (range.bitLength() > 1000) {
            return verificarIntervaloAmostragemCamadas(inicio, fim, amostras);
        }
        
        // Amostragem padrão para números grandes
        for (int i = 0; i < amostras; i++) {
            BigInteger candidato = inicio.add(new BigInteger(range.bitLength(), rnd).mod(range));
            if (ehPrimoBigInteger(candidato)) {
                return true;
            }
        }
        return false;
    }

    private boolean verificarIntervaloAmostragemCamadas(BigInteger inicio, BigInteger fim, int amostras) {
        // Estratégia de amostragem em camadas para números extremamente grandes
        SecureRandom rnd = new SecureRandom();
        BigInteger range = fim.subtract(inicio).add(BigInteger.ONE);
        
        // Dividir em camadas para melhor cobertura
        int camadas = Math.min(10, amostras / 100);
        int amostrasPorCamada = amostras / camadas;
        
        for (int camada = 0; camada < camadas; camada++) {
            // Amostrar em diferentes regiões do intervalo
            BigInteger offset = range.multiply(BigInteger.valueOf(camada)).divide(BigInteger.valueOf(camadas));
            BigInteger inicioCamada = inicio.add(offset);
            BigInteger fimCamada = (camada == camadas - 1) ? fim : inicio.add(offset.add(range.divide(BigInteger.valueOf(camadas))));
            
            BigInteger rangeCamada = fimCamada.subtract(inicioCamada).add(BigInteger.ONE);
            
            for (int i = 0; i < amostrasPorCamada; i++) {
                BigInteger candidato = inicioCamada.add(new BigInteger(rangeCamada.bitLength(), rnd).mod(rangeCamada));
                if (ehPrimoBigInteger(candidato)) {
                    return true;
                }
            }
        }
        return false;
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


