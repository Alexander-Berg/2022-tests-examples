package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.page.FormPage.SAVE;
import static ru.yandex.general.page.OfferCardPage.EDIT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;

@Epic(NAVIGATION_FEATURE)
@Feature("Навигация с карточки оффера")
@DisplayName("Отображается карточка оффера после редактирования")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class BackToOfferCardFromEditTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается карточка оффера после редактирования")
    public void shouldSeeCardAfterEdit() {
        String offerUrl = basePageSteps.onMyOffersPage().snippetFirst().getUrl();
        basePageSteps.onMyOffersPage().snippetFirst().click();
        basePageSteps.onOfferCardPage().link(EDIT).click();
        basePageSteps.onFormPage().button(SAVE).click();

        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

}
