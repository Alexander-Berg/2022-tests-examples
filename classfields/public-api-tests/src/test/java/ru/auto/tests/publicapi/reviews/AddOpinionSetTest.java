package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiReview;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultOpinions;

@DisplayName("POST /reviews/{subject}/{reviewId}/opinion/{opinion}")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddOpinionSetTest {

    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter
    @Parameterized.Parameter(0)
    public AutoApiReview.UserOpinionEnum opinion;

    @Parameterized.Parameters(name = "opinion={0}")
    public static Object[] provideOpinions() {
        return defaultOpinions();
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSetOpinion() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        api.reviews().setOpinion().subjectPath(AUTO).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .reviewIdPath(reviewId).opinionPath(opinion).execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));
    }
}
