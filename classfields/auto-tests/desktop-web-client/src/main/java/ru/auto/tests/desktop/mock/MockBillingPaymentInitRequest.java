package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.consts.SaleServices;
import ru.auto.tests.desktop.mock.beans.billing.Product;

import java.util.ArrayList;

import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.beans.billing.Product.product;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockBillingPaymentInitRequest {

    public static final String PAYMENT_INIT_VAS_TEMPLATE = "mocksConfigurable/billing/PaymentInitVasRequestTemplate.json";
    public static final String PAYMENT_INIT_WALLET = "mocksConfigurable/billing/PaymentInitWalletRequest.json";
    public static final String PAYMENT_INIT_HISTORY = "mocksConfigurable/billing/PaymentInitHistoryRequest.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockBillingPaymentInitRequest(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockBillingPaymentInitRequest paymentInitVasRequest() {
        return new MockBillingPaymentInitRequest(PAYMENT_INIT_VAS_TEMPLATE);
    }

    public static MockBillingPaymentInitRequest paymentInitWalletRequest() {
        return new MockBillingPaymentInitRequest(PAYMENT_INIT_WALLET);
    }

    public static MockBillingPaymentInitRequest paymentInitHistoryRequest() {
        return new MockBillingPaymentInitRequest(PAYMENT_INIT_HISTORY);
    }

    public MockBillingPaymentInitRequest setProducts(Product... products) {
        stream(products).forEach(product -> body.getAsJsonArray("product").add(getJsonObject(product)));
        return this;
    }

    public MockBillingPaymentInitRequest setProducts(SaleServices.VasProduct... vasProducts) {
        ArrayList<Product> products = new ArrayList<>();
        stream(vasProducts).forEach(vasProduct ->
                products.add(
                        product(vasProduct.getValue())));

        setProducts(products.toArray(new Product[0]));

        return this;
    }

    public MockBillingPaymentInitRequest setOfferId(String offerId) {
        body.getAsJsonObject("autoru_purchase").addProperty("offer_id", offerId);
        return this;
    }

    public MockBillingPaymentInitRequest setSubscribePurchaseCount(int count) {
        body.getAsJsonObject("subscribe_purchase").addProperty("count", count);
        return this;
    }

}
