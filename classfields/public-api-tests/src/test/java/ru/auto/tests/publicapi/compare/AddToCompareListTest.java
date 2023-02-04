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
import ru.auto.tests.publicapi.model.AutoApiCarInfo;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.USER_ALREADY_HAS_THIS_CATALOG_CARD;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400InvalidIdFormatError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("POST /user/compare/{category}/{catalogCardId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class AddToCompareListTest {

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
    public void shouldSee400WithIncorrectCatalogId() {
        String catalogId = Utils.getRandomString();
        api.userCompare().addCard()
                .categoryPath(CARS.name())
                .catalogCardIdPath(catalogId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400InvalidIdFormatError(catalogId)));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        String catalogId = Utils.getRandomString();
        String incorrectCategory = Utils.getRandomString();
        api.userCompare().addCard()
                .categoryPath(incorrectCategory)
                .catalogCardIdPath(catalogId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    public void shouldAddToCompare() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiCarInfo carInfo = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer().getCarInfo();
        api.userCompare().addCard()
                .categoryPath(CARS.name())
                .catalogCardIdPath(getCatalogId(carInfo))
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldNotAddTwiceToCompare() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiCarInfo carInfo = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer().getCarInfo();
        String catalogId = getCatalogId(carInfo);

        api.userCompare().addCard()
                .categoryPath(CARS.name())
                .catalogCardIdPath(catalogId)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));

        AutoApiErrorResponse response = api.userCompare().addCard()
                .categoryPath(CARS.name())
                .catalogCardIdPath(catalogId)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_CONFLICT)))
                .as(AutoApiErrorResponse.class);

        assertThat(response)
                .hasStatus(ERROR)
                .hasError(USER_ALREADY_HAS_THIS_CATALOG_CARD)
                .hasDetailedError(USER_ALREADY_HAS_THIS_CATALOG_CARD.name());
    }

    /**
     * catalog_id формируется следующем образом: {configuration_id}_{complectation_id}_{tech_param_id}
     */
    private String getCatalogId(AutoApiCarInfo carInfo) {
        return String.format("%s__%s", carInfo.getConfigurationId(), carInfo.getTechParamId());
    }
}
