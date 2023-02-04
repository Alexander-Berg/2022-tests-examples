package ru.auto.tests.publicapi.compare;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("DELETE /user/compare/{category}/{catalogCardId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteFromCompareListTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee200WithIncorrectCatalogId() {
        String catalogId = Utils.getRandomString();
        api.userCompare().deleteCardCompare().categoryPath(CARS.name()).catalogCardIdPath(catalogId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        String catalogId = Utils.getRandomString();
        String incorrectCategory = Utils.getRandomString();
        api.userCompare().deleteCardCompare()
                .categoryPath(incorrectCategory)
                .catalogCardIdPath(catalogId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    public void shouldDeleteFromCompareList() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String catalogCardId = adaptor.addToCompare(account.getLogin(), sessionId);

        api.userCompare().deleteCardCompare()
                .categoryPath(CARS.name())
                .catalogCardIdPath(catalogCardId)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSee200AfterDeleteTwiceFromCompare() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String catalogCardId = adaptor.addToCompare(account.getLogin(), sessionId);

        api.userCompare().deleteCardCompare().categoryPath(CARS.name()).catalogCardIdPath(catalogCardId).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));

        api.userCompare().deleteCardCompare().categoryPath(CARS.name()).catalogCardIdPath(catalogCardId).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }


}
