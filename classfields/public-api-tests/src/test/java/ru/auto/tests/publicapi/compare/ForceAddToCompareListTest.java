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
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400InvalidIdFormatError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 10.10.17.
 */

@DisplayName("PUT /user/compare/{category}/{catalogCardId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ForceAddToCompareListTest {

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
        api.userCompare().upsertCard().categoryPath(CARS.name()).catalogCardIdPath(catalogId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400InvalidIdFormatError(catalogId)));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        String catalogId = Utils.getRandomString();
        String incorrectCategory = Utils.getRandomString();
        api.userCompare().upsertCard().categoryPath(incorrectCategory)
                .catalogCardIdPath(catalogId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    public void shouldForceAddToCompare() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiCarInfo carInfo = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer().getCarInfo();
        api.userCompare().upsertCard().categoryPath(CARS.name()).catalogCardIdPath(getCatalogId(carInfo)).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldForceAddTwiceToCompare() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiCarInfo carInfo = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOffer().getCarInfo();
        api.userCompare().upsertCard().categoryPath(CARS.name()).catalogCardIdPath(getCatalogId(carInfo)).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        api.userCompare().upsertCard().categoryPath(CARS.name()).catalogCardIdPath(getCatalogId(carInfo)).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    /**
     * catalog_id формируется следующем образом: {configuration_id}_{complectation_id}_{tech_param_id}
     *
     * @param carInfo
     * @return
     */
    private String getCatalogId(AutoApiCarInfo carInfo) {
        return String.format("%s__%s", carInfo.getConfigurationId(), carInfo.getTechParamId());
    }
}
