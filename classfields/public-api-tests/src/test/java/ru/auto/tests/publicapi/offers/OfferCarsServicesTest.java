package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiPaidService;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /user/offers/{category}/{offerID}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCarsServicesTest {

    private static final int INTERVAL = 3;
    private static final int DELAY = 5;
    private static final int TIMEOUT = 60;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Parameterized.Parameter
    public String productName;

    @Parameterized.Parameter(1)
    public List<String> servicesList;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"boost", newArrayList("all_sale_fresh")},
                {"highlighting", newArrayList("all_sale_color")},
                {"special-offer", newArrayList("all_sale_special")},
                {"top", newArrayList("all_sale_toplist")},
                {"vip-package", newArrayList("package_vip", "all_sale_color", "all_sale_special", "all_sale_toplist")},
                {"turbo-package", newArrayList("all_sale_color", "all_sale_toplist", "all_sale_special", "package_turbo")},
                {"express-package", newArrayList("package_express", "all_sale_color", "all_sale_special")}
        });
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeActiveServicesAfterPayment() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        adaptor.buyOfferService(account.getId(), offerId, productName);

        List<AutoApiPaidService> offerServices = given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(DELAY, SECONDS).pollInterval(INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                .until(() -> api.userOffers().getMyOffer().reqSpec(defaultSpec())
                        .categoryPath(CARS)
                        .offerIDPath(offerId)
                        .xSessionIdHeader(sessionId)
                        .executeAs(validatedWith(shouldBe200OkJSON()))
                        .getOffer()
                        .getServices(), notNullValue());

        List<String> offerServicesNames = offerServices.stream().map(AutoApiPaidService::getService).collect(Collectors.toList());

        Assertions.assertThat(offerServicesNames).hasSize(servicesList.size()).containsOnlyElementsOf(servicesList);
        offerServices.forEach(service -> assertThat(service).hasIsActive(true));
    }
}
