package ru.auto.tests.salesman.user.adaptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import retrofit2.Response;
import ru.auto.test.salesman.user.ApiClient;
import ru.auto.test.salesman.user.api.PaymentApi;
import ru.auto.test.salesman.user.api.TransactionApi;
import ru.auto.test.salesman.user.model.AutoSalesmanUserCreateTransactionResult;
import ru.auto.test.salesman.user.model.AutoSalesmanUserPrice;
import ru.auto.test.salesman.user.model.AutoSalesmanUserProductContext;
import ru.auto.test.salesman.user.model.AutoSalesmanUserProductContextBundleContext;
import ru.auto.test.salesman.user.model.AutoSalesmanUserProductContextGoodsContext;
import ru.auto.test.salesman.user.model.AutoSalesmanUserProductContextSubscriptionContext;
import ru.auto.test.salesman.user.model.AutoSalesmanUserProductPrice;
import ru.auto.test.salesman.user.model.AutoSalesmanUserProductRequest;
import ru.auto.test.salesman.user.model.AutoSalesmanUserTransactionRequest;
import ru.auto.test.salesman.user.model.Payload;
import ru.auto.test.salesman.user.model.PaymentNotification;
import ru.auto.tests.commons.util.Utils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static ru.auto.test.salesman.user.model.PaymentNotification.ActionEnum.ACTIVATE;

public class SalesmanUserApiAdaptor extends AbstractModule {

    private static final String AUTORU_DOMAIN = "autoru";

    @Inject
    private ApiClient salesmanUser;

    @Step("Создаем транзакцию пользователю {userId} для продукта {product} в оффере {offerId}")
    public Response<AutoSalesmanUserCreateTransactionResult> createTransactionForOffer(String userId, String product, String offerId) {
        try {
            String user = format("user:%s", userId);
            long price = 1L;
            // Раньше проставляли 3 дня.
            // Потом в пакет VIP добавили условие, что он должен действовать не меньше 14 дней.
            // Проставляем 60 дней, как в VIP и Placement для частников.
            String duration = "5184000.0s";
            AutoSalesmanUserProductContext context = productContext(product, price, duration);
            AutoSalesmanUserProductRequest productRequest = new AutoSalesmanUserProductRequest().product(product)
                    .offer(offerId).amount(price).context(context).prolongable(false);
            AutoSalesmanUserTransactionRequest transactionRequest = new AutoSalesmanUserTransactionRequest()
                    .user(user).amount(price).addPayloadItem(productRequest);

            return salesmanUser.createService(TransactionApi.class)
                    .createTransaction(AUTORU_DOMAIN, user, format("autoru-api-tests-%s", Utils.getRandomShortInt()), transactionRequest)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Создаем транзакцию для подписки на услугу {product} пользователю {userId}")
    public Response<AutoSalesmanUserCreateTransactionResult> createSubscriptionTransaction(String userId, String product, Long price) {
        try {
            String user = format("user:%s", userId);
            String duration = "259200.0s"; // 3 days
            AutoSalesmanUserPrice fullPrice = new AutoSalesmanUserPrice().basePrice(price).effectivePrice(price);
            AutoSalesmanUserProductPrice productPrice = new AutoSalesmanUserProductPrice().product(product)
                    .duration(duration).price(fullPrice);
            AutoSalesmanUserProductContextSubscriptionContext subscriptionContext =
                    new AutoSalesmanUserProductContextSubscriptionContext().productPrice(productPrice);
            AutoSalesmanUserProductContext context = new AutoSalesmanUserProductContext()
                    .subscription(subscriptionContext);
            AutoSalesmanUserProductRequest productRequest = new AutoSalesmanUserProductRequest()
                    .product(format("%s-1", product)).amount(price).context(context).prolongable(false);
            AutoSalesmanUserTransactionRequest transactionRequest = new AutoSalesmanUserTransactionRequest()
                    .user(user).amount(price).addPayloadItem(productRequest);

            return salesmanUser.createService(TransactionApi.class)
                    .createTransaction(AUTORU_DOMAIN, user, format("autoru-api-tests-%s", Utils.getRandomShortInt()), transactionRequest)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Проводим успешно платеж {transactionId}")
    public void activatePayment(String transactionId) {
        try {
            Map<String, String> map = newHashMap();
            map.put("domain", AUTORU_DOMAIN);
            map.put("transaction", transactionId);

            PaymentNotification paymentNotification = new PaymentNotification().id("autoru-api-tests")
                    .timestamp(OffsetDateTime.now())
                    .action(ACTIVATE)
                    .payload(new Payload().struct(map));

            salesmanUser.createService(PaymentApi.class)
                    .paymentReceive(newArrayList(paymentNotification))
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configure() {
    }

    private AutoSalesmanUserProductContext productContext(String product, long price, String duration) {
        AutoSalesmanUserPrice fullPrice = new AutoSalesmanUserPrice().basePrice(price).effectivePrice(price);
        AutoSalesmanUserProductPrice productPrice = new AutoSalesmanUserProductPrice().product(product)
                .duration(duration).price(fullPrice);
        AutoSalesmanUserProductContext base = new AutoSalesmanUserProductContext();

        if (isBundle(product)) {
            AutoSalesmanUserProductContextBundleContext context = new AutoSalesmanUserProductContextBundleContext()
                    .productPrice(productPrice);
            return base.bundle(context);
        } else {
            AutoSalesmanUserProductContextGoodsContext context = new AutoSalesmanUserProductContextGoodsContext()
                    .productPrice(productPrice);
            return base.goods(context);
        }
    }

    // Какие есть бандлы: vip-package, turbo-package, express-package
    private boolean isBundle(String product) {
        return product.endsWith("package");
    }
}
