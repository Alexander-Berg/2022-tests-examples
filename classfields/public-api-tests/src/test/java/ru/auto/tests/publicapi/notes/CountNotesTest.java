package ru.auto.tests.publicapi.notes;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOfferCountResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by scrooge on 27.12.17.
 */


@DisplayName("GET /user/notes/{category}/count")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CountNotesTest {

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
    @Owner(SCROOGE)
    public void shouldSee403WhenNoAuth() {
        api.userNotes().countNotes().categoryPath(CARS.name()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeSuccessWithoutSessionId() {
        AutoApiOfferCountResponse response = api.userNotes().countNotes().categoryPath(CARS.name())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()))
                .as(AutoApiOfferCountResponse.class);
        assertThat(response).hasCount(0);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithIncorrectCategory() {
        String incorrectCategory = getRandomString();
        api.userNotes().countNotes().categoryPath(incorrectCategory)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }
}
