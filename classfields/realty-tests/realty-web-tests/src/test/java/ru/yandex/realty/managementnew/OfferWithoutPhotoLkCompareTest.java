package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL_WITHOUT_PHOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Частные лица. Отображение оффера без фото")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class OfferWithoutPhotoLkCompareTest {

    private static final String NOT_READY_MESSAGE = "Оплата публикации в процессе";
    private static final String ADD_FLAT_NUMBER = "Добавьте номер квартиры.";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 5000);
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение оффера без фото в ЛК")
    public void shouldSeeManagementNewOffer() {
        api.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL_WITHOUT_PHOTO).create();
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.refreshUntil(() -> managementSteps.onManagementNewPage().offer(FIRST).offerMessage(),
                hasText(containsString(ADD_FLAT_NUMBER)), 60);
        Screenshot testing = compareSteps.takeScreenshot(managementSteps.onManagementNewPage().offer(FIRST));
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(managementSteps.onManagementNewPage().offer(FIRST));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Tag(JURICS)
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение оффера без фото в ЛК")
    public void shouldSeeManagementNewJuridicalOffer() {
        api.createRealty3JuridicalAccount(account);
        offerBuildingSteps.addNewOffer(account).withType(APARTMENT_SELL_WITHOUT_PHOTO).create();
        urlSteps.testing().path(MANAGEMENT_NEW).open();
        managementSteps.refreshUntil(() -> managementSteps.onManagementNewPage().agencyOffer(FIRST).offerMessage(),
                hasText(containsString(NOT_READY_MESSAGE)), 60);
        Screenshot testing = compareSteps.takeScreenshot(managementSteps.onManagementNewPage().agencyOffer(FIRST));
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(managementSteps.onManagementNewPage().agencyOffer(FIRST));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
