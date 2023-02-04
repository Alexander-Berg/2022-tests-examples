package ru.yandex.qe.dispenser.ws.owning_cost;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.bot.Provider;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.pricing.PricingModel;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.pricing.QuotaChangeOwningCostTariffManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;


/**
 * Calculating relative cost of request.
 *
 * @author Marat Mustakayev <aqru@yandex-team.ru>
 */
public class RelativeCostTest extends BaseQuotaRequestTest {

    private static final String PROVIDER_QUOTA_PRICE_SKU = "yp.cpu.quota";

    @Autowired
    private QuotaChangeOwningCostTariffManager quotaChangeOwningCostTariffManager;

    @Autowired
    private ResourceDao resourceDao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        LocalDate date100 = LocalDate.of(2020, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderThree)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date100)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderThree.getId(),
                        date100)))
                .build());

        LocalDate date142 = LocalDate.of(2021, Month.FEBRUARY, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderTwo)
                .setKey("feb2021")
                .setName("feb2021")
                .setId(142L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(),
                        date142)))
                .build());

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());
    }

    @Test
    public void relativeCostReturnedByAPITest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);

        resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        updateHierarchy();
        prepareCampaignResources();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        long id = quotaRequests.getFirst().getId();
        DiQuotaChangeRequest firstRequest = dispenser().quotaChangeRequests().byId(id).get().perform();

        PricingModel providerQuotaPrice = quotaChangeOwningCostTariffManager.getByProvider(Provider.YP).stream()
                .filter(p -> p.getSKU().equals(PROVIDER_QUOTA_PRICE_SKU))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(providerQuotaPrice);

        String expectedRelativeCost = new BigDecimal(
                        String.valueOf(firstRequest.getRequestOwningCost()), new MathContext(18, RoundingMode.HALF_UP)
                ).divide(providerQuotaPrice.getPrice(), new MathContext(18, RoundingMode.HALF_UP))
                .setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        Assertions.assertEquals(expectedRelativeCost, firstRequest.getRelativeCost());
    }

}
