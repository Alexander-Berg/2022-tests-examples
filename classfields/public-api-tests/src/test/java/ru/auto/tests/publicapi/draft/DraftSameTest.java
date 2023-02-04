package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOfferResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.*;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /user/draft/{category}/{offerId}/same")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DraftSameTest {
    private static final int POLL_INTERVAL = 3;
    private static final int TIMEOUT = 60;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @Parameter("Оффер")
    @Parameterized.Parameter(1)
    public String offer;


    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(offersByCategories());
    }

    public static Object[][] offersByCategories() {
        return new Object[][]{
                {CARS, "offers/cars.ftl"},
                {MOTO, "offers/moto.ftl"},
                {TRUCKS, "offers/trucks.ftl"}
        };
    }

    @Test
    public void shouldHasSameOffer() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerTemplate = new OfferTemplate().process(offer, account.getLogin());
        String offerId = adaptor.createDraft(sessionId, category, offerTemplate).getOfferId();
        adaptor.publishDraft(sessionId, category, offerId);

        String draftId = adaptor.createDraft(sessionId, category, offerTemplate).getOfferId();

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() ->
                        api.draft().sameOffer()
                                .categoryPath(category.name())
                                .offerIdPath(draftId)
                                .xSessionIdHeader(sessionId)
                                .reqSpec(defaultSpec())
                                .executeAs(validatedWith(shouldBeSuccess())).getStatus(),
                        equalTo(AutoApiOfferResponse.StatusEnum.SUCCESS)
                );
    }

    @Test
    public void shouldHasNotSameOffer() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerTemplate = new OfferTemplate().process(offer, account.getLogin());
        String draftTemplate = new OfferTemplate().process(offer, account.getLogin());

        String offerId = adaptor.createDraft(sessionId, category, offerTemplate).getOfferId();
        adaptor.publishDraft(sessionId, category, offerId);

        String draftId = adaptor.createDraft(sessionId, category, draftTemplate).getOfferId();
        api.draft().sameOffer()
                .categoryPath(category.name())
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(204)));

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() ->
                                api.draft().sameOffer()
                                        .categoryPath(category.name())
                                        .offerIdPath(draftId)
                                        .xSessionIdHeader(sessionId)
                                        .reqSpec(defaultSpec())
                                        .execute(validatedWith(shouldBeCode(204))).getStatusCode(),
                        equalTo(204)
                );
    }
}
