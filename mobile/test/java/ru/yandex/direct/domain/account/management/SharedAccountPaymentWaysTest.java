package ru.yandex.direct.domain.account.management;

import org.junit.runners.Parameterized;

import java.util.List;

import ru.yandex.direct.domain.PaymentWayTest;
import ru.yandex.direct.domain.PaymentWayTestData;
import ru.yandex.direct.domain.enums.PaymentWay;

public class SharedAccountPaymentWaysTest extends PaymentWayTest {
    @Parameterized.Parameters(name = "{index}: {7}")
    public static Object[][] getParameters() {
        return PaymentWayTestData.getSharedAccountTestData();
    }

    @Override
    public List<PaymentWay> getPossiblePaymentWays() {
        return sharedAccount.getPossiblePaymentWays(
                client,
                configuration.isAgency);
    }
}
