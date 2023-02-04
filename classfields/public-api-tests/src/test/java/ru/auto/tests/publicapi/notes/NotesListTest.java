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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by scrooge on 27.12.17.
 */

@DisplayName("GET /user/notes/{category}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class NotesListTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(SCROOGE)
    public void shouldSee403WhenNoAuth() {
        api.userNotes().notes().categoryPath(CARS.name()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeSuccessWithoutSessionId() {
        api.userNotes().notes().categoryPath(CARS.name())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithIncorrectCategory() {
        String incorrectCategory = Utils.getRandomString();
        api.userNotes().notes().categoryPath(incorrectCategory)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }
}
