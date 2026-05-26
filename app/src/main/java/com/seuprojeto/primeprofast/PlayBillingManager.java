package com.seuprojeto.primeprofast;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import java.util.ArrayList;
import java.util.List;

/**
 * Integração com Google Play Billing (assinaturas + compra in-app consumível separada).
 */
public class PlayBillingManager implements PurchasesUpdatedListener {

    /**
     * IDs de assinatura na Play Console (um por base plan / produto).
     * Altere apenas se criar produtos com outros identificadores.
     */
    public static final String SKU_MENSAL = "primeprofast_premium_mensal";
    public static final String SKU_ANUAL = "primeprofast_premium_anual";

    /**
     * Produto in-app consumível: entrega de primos &gt; 8192 bits por e-mail (independente do Premium).
     * Play Console → Monetização → Produtos in-app → Comprável (consumível).
     */
    public static final String SKU_ENTREGA_EMAIL_PRIMO_ULTRA = "primeprofast_entrega_primo_email";

    public interface Listener {
        void onPremiumStateChanged(boolean active);

        /** Chamado após compra de assinatura concluída e reconhecida (não em cada sincronização ao abrir o app). */
        void onPurchaseCompletedSuccessfully();

        /**
         * Compra consumível de “entrega por e-mail” concluída; o app deve abrir e-mail para o desenvolvedor.
         */
        void onMegaPrimeDeliveryPurchaseCompleted(Purchase purchase);

        void onPurchaseFlowError(String message);
    }

    private final BillingClient billingClient;
    private final Context appContext;
    private Listener listener;
    private boolean serviceConnected;

    public PlayBillingManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
    }

    public void start(Listener listener) {
        this.listener = listener;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    serviceConnected = true;
                    queryExistingPurchasesAndSync();
                } else {
                    serviceConnected = false;
                    if (PlayBillingManager.this.listener != null) {
                        PlayBillingManager.this.listener.onPurchaseFlowError(
                                "Billing indisponível: " + billingResult.getDebugMessage());
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                serviceConnected = false;
            }
        });
    }

    public void refreshPurchases() {
        if (serviceConnected) {
            queryExistingPurchasesAndSync();
        }
    }

    private void queryExistingPurchasesAndSync() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        return;
                    }
                    boolean active = hasActiveSubscription(purchases);
                    if (listener != null) {
                        listener.onPremiumStateChanged(active);
                    }
                });
    }

    private static boolean hasActiveSubscription(@Nullable List<Purchase> purchases) {
        if (purchases == null) {
            return false;
        }
        for (Purchase p : purchases) {
            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                return true;
            }
        }
        return false;
    }

    public void launchSubscriptionPurchase(Activity activity, String tipo) {
        final String sku = "mensal".equals(tipo) ? SKU_MENSAL : SKU_ANUAL;
        if (!serviceConnected) {
            if (listener != null) {
                listener.onPurchaseFlowError("Loja não conectada. Tente novamente.");
            }
            return;
        }
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build());
        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder().setProductList(products).build();

        billingClient.queryProductDetailsAsync(params, (br, detailsResult) -> {
            List<ProductDetails> detailsList =
                    detailsResult != null ? detailsResult.getProductDetailsList() : null;
            if (br.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || detailsList == null
                    || detailsList.isEmpty()) {
                if (listener != null) {
                    listener.onPurchaseFlowError(
                            "Produto não encontrado na Play Store. Confira o ID da assinatura: " + sku);
                }
                return;
            }
            ProductDetails pd = detailsList.get(0);
            List<ProductDetails.SubscriptionOfferDetails> offers = pd.getSubscriptionOfferDetails();
            if (offers == null || offers.isEmpty()) {
                if (listener != null) {
                    listener.onPurchaseFlowError(
                            "Nenhuma oferta ativa para " + sku + ". Verifique planos no Play Console.");
                }
                return;
            }
            String offerToken = offers.get(0).getOfferToken();
            List<BillingFlowParams.ProductDetailsParams> pdpList = new ArrayList<>();
            pdpList.add(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(pd)
                            .setOfferToken(offerToken)
                            .build());
            BillingFlowParams flowParams =
                    BillingFlowParams.newBuilder().setProductDetailsParamsList(pdpList).build();
            BillingResult launchResult = billingClient.launchBillingFlow(activity, flowParams);
            if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                    && listener != null) {
                listener.onPurchaseFlowError(
                        "Não foi possível abrir o pagamento: " + launchResult.getDebugMessage());
            }
        });
    }

    @Override
    public void onPurchasesUpdated(
            @NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int code = billingResult.getResponseCode();
        if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            return;
        }
        if (code != BillingClient.BillingResponseCode.OK || purchases == null) {
            if (listener != null) {
                listener.onPurchaseFlowError(
                        billingResult.getDebugMessage() != null
                                ? billingResult.getDebugMessage()
                                : "Erro na compra.");
            }
            return;
        }
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                continue;
            }
            List<String> productIds = purchase.getProducts();
            if (productIds == null || productIds.isEmpty()) {
                continue;
            }
            String sku = productIds.get(0);
            if (SKU_ENTREGA_EMAIL_PRIMO_ULTRA.equals(sku)) {
                consumirEntregaMegaEPNotificar(purchase);
                return;
            }
            if (SKU_MENSAL.equals(sku) || SKU_ANUAL.equals(sku)) {
                acknowledgeIfNeeded(purchase);
                if (listener != null) {
                    listener.onPremiumStateChanged(true);
                    listener.onPurchaseCompletedSuccessfully();
                }
                return;
            }
        }
    }

    private void consumirEntregaMegaEPNotificar(@NonNull Purchase purchase) {
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
        billingClient.consumeAsync(
                consumeParams,
                (billingResult, purchaseToken) -> {
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        if (listener != null) {
                            listener.onPurchaseFlowError(
                                    "Não foi possível finalizar o pedido: " + billingResult.getDebugMessage());
                        }
                        return;
                    }
                    if (listener != null) {
                        listener.onMegaPrimeDeliveryPurchaseCompleted(purchase);
                    }
                });
    }

    /**
     * Inicia compra de entrega por e-mail (consumível). Independente do Premium.
     */
    public void launchMegaDeliveryInAppPurchase(Activity activity) {
        if (!serviceConnected) {
            if (listener != null) {
                listener.onPurchaseFlowError("Loja não conectada. Tente novamente.");
            }
            return;
        }
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_ENTREGA_EMAIL_PRIMO_ULTRA)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build());
        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder().setProductList(products).build();

        billingClient.queryProductDetailsAsync(params, (br, detailsResult) -> {
            List<ProductDetails> detailsList =
                    detailsResult != null ? detailsResult.getProductDetailsList() : null;
            if (br.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || detailsList == null
                    || detailsList.isEmpty()) {
                if (listener != null) {
                    listener.onPurchaseFlowError(
                            "Produto não encontrado na Play Store. Crie o in-app consumível: "
                                    + SKU_ENTREGA_EMAIL_PRIMO_ULTRA);
                }
                return;
            }
            ProductDetails pd = detailsList.get(0);
            ProductDetails.OneTimePurchaseOfferDetails otp = pd.getOneTimePurchaseOfferDetails();
            if (otp == null) {
                if (listener != null) {
                    listener.onPurchaseFlowError("Oferta de preço indisponível para este produto.");
                }
                return;
            }
            String offerToken = otp.getOfferToken();
            List<BillingFlowParams.ProductDetailsParams> pdpList = new ArrayList<>();
            pdpList.add(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(pd)
                            .setOfferToken(offerToken)
                            .build());
            BillingFlowParams flowParams =
                    BillingFlowParams.newBuilder().setProductDetailsParamsList(pdpList).build();
            BillingResult launchResult = billingClient.launchBillingFlow(activity, flowParams);
            if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                    && listener != null) {
                listener.onPurchaseFlowError(
                        "Não foi possível abrir o pagamento: " + launchResult.getDebugMessage());
            }
        });
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (purchase.isAcknowledged()) {
            return;
        }
        AcknowledgePurchaseParams ackParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        billingClient.acknowledgePurchase(ackParams, billingResult -> { });
    }

    public void endConnection() {
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}
