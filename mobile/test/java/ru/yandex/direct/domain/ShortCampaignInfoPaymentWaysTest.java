package ru.yandex.direct.domain;

import org.junit.runners.Parameterized;

import java.util.List;

import ru.yandex.direct.domain.enums.PaymentWay;

public class ShortCampaignInfoPaymentWaysTest extends PaymentWayTest {
    @Parameterized.Parameters(name = "{index}: {7}")
    public static Object[][] getParameters() {
        return PaymentWayTestData.getShortCampaignInfoTestData();
    }

    @Override
    public List<PaymentWay> getPossiblePaymentWays() {
        return campaign.getPossiblePaymentWays(
                allCampaigns,
                client,
                configuration.isSharedAccountEnabled,
                configuration.isAgency);
    }
}
