package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /reviews/{subject}/summary")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetSummaryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.reviews().summaryAggregators().subjectPath(AUTO)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithInvalidSubject() {
        String invalidSubject = Utils.getRandomString();
        api.reviews().summaryAggregators().subjectPath(invalidSubject).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
