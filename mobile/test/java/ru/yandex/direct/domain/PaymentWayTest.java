package ru.yandex.direct.domain;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import ru.yandex.direct.domain.account.management.SharedAccount;
import ru.yandex.direct.domain.clients.ClientInfo;
import ru.yandex.direct.domain.enums.PaymentWay;
import ru.yandex.direct.utils.CurrencyInitializer;

import static org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public abstract class PaymentWayTest {
    @Parameter(0)
    public SharedAccount sharedAccount;

    @Parameter(1)
    public ShortCampaignInfo campaign;

    @Parameter(2)
    public Float[] sumsAvailableForTransferArray;

    @Parameter(3)
    public ShortCampaignInfo[] allCampaignsArray;

    @Parameter(4)
    public ClientInfo client;

    @Parameter(5)
    public Configuration configuration;

    @Parameter(6)
    public PaymentWay[] expectedResult;

    @Parameter(7)
    public String testName;

    public List<Float> sumsAvailableForTransfer;

    public List<ShortCampaignInfo> allCampaigns;

    @Test
    public void runPaymentMethodsTest() {
        CurrencyInitializer.injectTestDataInStaticFields();
        sumsAvailableForTransfer = Arrays.asList(sumsAvailableForTransferArray);
        allCampaigns = Arrays.asList(allCampaignsArray);

        List<PaymentWay> actualResult = getPossiblePaymentWays();

        Assert.assertEquals(new HashSet<>(Arrays.asList(expectedResult)), new HashSet<>(actualResult));
    }

    public abstract List<PaymentWay> getPossiblePaymentWays();
}
