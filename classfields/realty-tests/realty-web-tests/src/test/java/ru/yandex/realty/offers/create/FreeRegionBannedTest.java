package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.realty.consts.OfferByRegion.Region.REGION;
import static ru.yandex.realty.consts.OfferByRegion.getLocationForRegion;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.page.OfferAddPage.SAVE_AND_CONTINUE;
import static ru.yandex.realty.step.UrlSteps.EDIT_VALUE;
import static ru.yandex.realty.step.UrlSteps.FALSE_VALUE;
import static ru.yandex.realty.step.UrlSteps.FROM_FORM_VALUE;
import static ru.yandex.realty.step.UrlSteps.PAYMENT_ID_VALUE;
import static ru.yandex.realty.step.UrlSteps.TRAP_VALUE;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.getRandomApartment;

@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class FreeRegionBannedTest {

    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createVos2Account(account, OWNER);
        offerId = offerBuildingSteps.addNewOffer(account)
                .withBody(OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL)
                        .withLocation(getLocationForRegion(REGION).withApartment(getRandomApartment())))
                .withBanned().create().getId();
        compareSteps.resize(1280, 10000);
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).open();

    }

    @Ignore("Запилить рабочий")
    @Test
    @DisplayName("Редактирование не активного оффера в регионе. Редирект в лк. Оффер остается забаненным")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeFreeRegionInactiveWithoutPhotoEditOfferPublishBlockToLk() {
        offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_AND_CONTINUE).click();
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(FROM_FORM_VALUE, EDIT_VALUE).queryParam(TRAP_VALUE, FALSE_VALUE)
                .ignoreParam(PAYMENT_ID_VALUE).shouldNotDiffWithWebDriverUrl();
        apiSteps.waitSearcherOfferStatusInactive(offerId);
    }
}