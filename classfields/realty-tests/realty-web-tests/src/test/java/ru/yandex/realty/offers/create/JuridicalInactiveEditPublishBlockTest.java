package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.page.OfferAddPage.SAVE_CHANGES;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.EDIT_VALUE;
import static ru.yandex.realty.step.UrlSteps.FALSE_VALUE;
import static ru.yandex.realty.step.UrlSteps.PAYMENT_ID_VALUE;
import static ru.yandex.realty.step.UrlSteps.TRAP_VALUE;

@Tag(JURICS)
@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class JuridicalInactiveEditPublishBlockTest {

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
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createRealty3JuridicalAccount(account);
        urlSteps.setSpbCookie();
    }

    @Test
    @DisplayName("Не активный оффер. Блок публикации для юр. лица c фото")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeJuridicalWithPhotoInactive() {
        String offerId = offerBuildingSteps.addNewOffer(account).create().withInactive().getId();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).open();
        offerAddSteps.setFlat("1");
        offerAddSteps.onOfferAddPage().publishBlock().payButton().should(hasText(SAVE_CHANGES)).click();
        managementSteps.onManagementNewPage().agencyOffer(FIRST);
        urlSteps.testing().path(MANAGEMENT_NEW).queryParam(UrlSteps.FROM_FORM_VALUE, EDIT_VALUE)
                .queryParam(TRAP_VALUE, FALSE_VALUE).ignoreParam(PAYMENT_ID_VALUE).shouldNotDiffWithWebDriverUrl();
    }
}