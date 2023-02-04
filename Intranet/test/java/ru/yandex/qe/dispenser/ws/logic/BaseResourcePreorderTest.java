package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.BotCampaignGroup;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.role.ProjectRoleDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;

public class BaseResourcePreorderTest extends BaseQuotaRequestTest {
    public static final String SQS = "sqs";

    @Autowired
    protected QuotaChangeRequestDao requestDao;

    @Autowired
    protected MockTrackerManager trackerManager;

    @Autowired
    protected ProjectRoleDao roleDao;

    @Autowired
    protected BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    protected PersonDao personDao;
    @Autowired
    protected ResourceDao resourceDao;
    @Autowired
    protected ServiceDao serviceDao;

    protected Service nirvana;
    protected Resource ytCpu;
    protected Resource sqsYtCpu;

    protected Campaign campaign;
    protected BotCampaignGroup botCampaignGroup;

    protected QuotaChangeRequest.Builder defaultBuilder() {
        return defaultBuilder(campaign);
    }

    protected QuotaChangeRequest.Builder defaultBuilder(Campaign campaign) {
        final Campaign.BigOrder bigOrder = campaign.getBigOrders().iterator().next();

        final Person person = hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());
        return new QuotaChangeRequest.Builder()
                .summary("test")
                .project(hierarchy.get().getProjectReader().read(YANDEX))
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .campaignType(campaign.getType())
                .author(person)
                .description("Default")
                .comment("Default")
                .calculations("Default")
                .created(0)
                .updated(0)
                .changes(Collections.singletonList(QuotaChangeRequest.Change.newChangeBuilder().order(bigOrder).resource(ytCpu).segments(Collections.emptySet()).amount(10_000).build()))
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .chartLinks(Collections.emptyList())
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .cost(0)
                .requestOwningCost(0L);
    }

    protected long createRequest(final QuotaChangeRequest.Builder builder) {
        return requestDao.create(builder.build()).getId();
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
        botCampaignGroup = botCampaignGroupDao.create(defaultCampaignGroupBuilder()
                .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                .build());

        nirvana = hierarchy.get().getServiceReader().read(NIRVANA);
        ytCpu = hierarchy.get().getResourceReader().read(new Resource.Key(YT_CPU, nirvana));

        final Service sqs = serviceDao.create(Service.withKey(SQS)
                .withName("Sqs")
                .withAbcServiceId(2489)
                .withSettings(Service.Settings.builder()
                        .accountActualValuesInQuotaDistribution(false)
                        .usesProjectHierarchy(true)
                        .manualQuotaAllocation(false)
                        .build())
                .build());

        serviceDao.attachAdmin(sqs, personDao.readPersonByLogin(SANCHO.getLogin()));

        updateHierarchy();

        sqsYtCpu = resourceDao.create(new Resource.Builder(YT_CPU, sqs)
                .name("YT CPU")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();
    }
}
