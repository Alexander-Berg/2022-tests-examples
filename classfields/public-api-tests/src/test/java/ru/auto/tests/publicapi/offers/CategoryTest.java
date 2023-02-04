package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /user/offers/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CategoryTest {
    private static final int POLL_INTERVAL = 2;
    private static final int TIMEOUT = 30;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userOffers().offers().categoryPath(CARS.name()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        adaptor.createOffer(account.getLogin(), sessionId, CARS);
        api.userOffers().offers().categoryPath(CARS.name())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        String incorrectCategory = Utils.getRandomString();
        api.userOffers().offers().categoryPath(incorrectCategory)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Issue("AUTORUAPI-4315")
    public void shouldSeeMarkInfoWhenSeveralOffers() {
        Account account = am.create();
        String path = "offers[0,1].truck_info.mark_info.name";
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createOffer(account.getLogin(), sessionId, TRUCKS, "offers/trucks.ftl");
        adaptor.createOffer(account.getLogin(), sessionId, TRUCKS, "offers/swap_body_offer.ftl");

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> api.userOffers().offers().categoryPath(TRUCKS)
                        .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                        .executeAs(validatedWith(shouldBeSuccess())).getOffers().size(), equalTo(2));

        List<String> response = api.userOffers().offers().categoryPath(TRUCKS).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess())).jsonPath().get(path);

        MatcherAssert.assertThat(String.format("в ответе отсутствует %s [AUTORUAPI-4315]", path), response, hasSize(2));
    }
}
