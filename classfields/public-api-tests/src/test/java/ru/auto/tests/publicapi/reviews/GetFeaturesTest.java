package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.*;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;


@DisplayName("GET /reviews/{subject}/features/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetFeaturesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.reviews().features().subjectPath(AUTO).categoryPath(CARS)
                .execute((validatedWith(shouldBeCode(SC_FORBIDDEN))));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectSubject() {
        api.reviews().features().subjectPath(Utils.getRandomString()).categoryPath(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectCategory() {
        String incorrectCategory = Utils.getRandomString();
        api.reviews().features().subjectPath(AUTO).categoryPath(incorrectCategory)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee200WithoutModel() {
        api.reviews().features().subjectPath(AUTO).categoryPath(CARS)
                .markQuery(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutMark() {
        api.reviews().features().subjectPath(AUTO).categoryPath(CARS)
                .modelQuery(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee200WithoutGeneration() {
        api.reviews().features().subjectPath(AUTO).categoryPath(CARS)
                .markQuery(Utils.getRandomString())
                .modelQuery(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_OK)));
    }
}
