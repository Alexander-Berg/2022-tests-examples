package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.consts.SaleServices;
import ru.auto.tests.desktop.mock.beans.billing.DetailedProductInfo;

import java.util.ArrayList;

import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.beans.billing.DetailedProductInfo.detailedProductInfo;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;

public class MockBillingPaymentInitResponse {

    public static final String PAYMENT_INIT_VAS = "mocksConfigurable/billing/PaymentInitVasResponse.json";
    public static final String TIED_CARD = "mocksConfigurable/billing/TiedCard.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockBillingPaymentInitResponse(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockBillingPaymentInitResponse paymentInitVasResponse() {
        return new MockBillingPaymentInitResponse(PAYMENT_INIT_VAS);
    }

    public static MockBillingPaymentInitResponse paymentInitWalletResponse() {
        return new MockBillingPaymentInitResponse(PAYMENT_INIT_VAS)
                .removeDetailedProductInfos()
                .setCost(0)
                .setBaseCost(0);
    }

    @Step("Добавляем привязанную карту")
    public MockBillingPaymentInitResponse tieCard() {
        body.getAsJsonArray("payment_methods").add(
                new GsonBuilder().create().fromJson(getResourceAsString(TIED_CARD), JsonObject.class));
        return this;
    }

    @Step("Добавляем стоимость услуги = «{cost}»")
    public MockBillingPaymentInitResponse setCost(int cost) {
        body.addProperty("cost", cost * 100);
        return this;
    }

    public MockBillingPaymentInitResponse setBaseCost(int cost) {
        body.addProperty("base_cost", cost * 100);
        return this;
    }

    public MockBillingPaymentInitResponse setDetailedProductInfos(DetailedProductInfo... productInfos) {
        JsonArray detailedProductInfos = new JsonArray();
        stream(productInfos).forEach(productInfo -> detailedProductInfos.add(getJsonObject(productInfo)));

        body.add("detailed_product_infos", detailedProductInfos);
        return this;
    }

    public MockBillingPaymentInitResponse setDetailedProductInfos(SaleServices.VasProduct... vasProducts) {
        ArrayList<DetailedProductInfo> detailedProductInfos = new ArrayList<>();

        stream(vasProducts).forEach(product ->
                detailedProductInfos.add(
                        getProductInfoTemplate()
                                .setService(product.getValue())
                                .setName(product.getName()))
        );

        setDetailedProductInfos(detailedProductInfos.toArray(new DetailedProductInfo[0]));

        return this;
    }

    public MockBillingPaymentInitResponse removeDetailedProductInfos() {
        body.remove("detailed_product_infos");
        return this;
    }

    @Step("Добавляем баланс аккаунта = «{balance}»")
    public MockBillingPaymentInitResponse setAccountBalance(int balance) {
        body.addProperty("account_balance", balance * 100);
        return this;
    }

    public static DetailedProductInfo getProductInfoTemplate() {
        return detailedProductInfo()
                .setBasePrice(getRandomBetween(500, 1000) * 100)
                .setDuration("259200s")
                .setDays(3)
                .setProlongationAllowed(false)
                .setProlongationForced(false);
    }

    public static DetailedProductInfo getHistoryReportProductInfo() {
        return detailedProductInfo()
                .setBasePrice(12900)
                .setDuration("31536000s")
                .setService("offers-history-reports")
                .setName("Отчёт о проверке по VIN")
                .setDays(365)
                .setEffectivePrice(12900)
                .setProlongationAllowed(false)
                .setProlongationForced(false)
                .setProlongationForcedNotTogglable(false);
    }

}
