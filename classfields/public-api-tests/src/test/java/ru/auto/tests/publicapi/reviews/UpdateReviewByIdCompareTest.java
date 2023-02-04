package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiReview;
import ru.auto.tests.publicapi.model.AutoApiReviewSaveResponse;
import ru.auto.tests.publicapi.model.AutoApiReviewSaveResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.model.AutoApiReviewSaveResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultReviewsByCategories;


@DisplayName("PUT /user/reviews/{reviewId}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdateReviewByIdCompareTest {

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

    @Parameter("Ревью")
    @Parameterized.Parameter
    public String review;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(defaultReviewsByCategories());
    }

    @Test
    public void shouldUpdateReview() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, review).getReviewId();

        AutoApiReviewSaveResponse response = api.reviews().updateReview().reviewIdPath(reviewId).subjectPath(AUTO)
                .xSessionIdHeader(sessionId)
                .body(getReview(review).id(reviewId))
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
        AutoApiReviewSaveResponseAssert.assertThat(response).hasReviewId(reviewId).hasStatus(SUCCESS);
    }

    private AutoApiReview getReview(String path) {
        JSON json = new JSON();
        return json.deserialize(getResourceAsString(path), AutoApiReview.class);
    }
}
