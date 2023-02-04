package ru.yandex.general.myOffers;

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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mobile.element.ActualizationBlock.IS_ACTUAL;
import static ru.yandex.general.mobile.element.ActualizationBlock.NO;
import static ru.yandex.general.mobile.element.ActualizationBlock.OFFER_WILL_RAISE;
import static ru.yandex.general.mobile.element.ActualizationBlock.YES;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.ACTUALIZE;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(MY_OFFERS_FEATURE)
@Feature("Актуальность оффера")
@DisplayName("Актуальность оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SnippetActualizationBlockTest {

    private static final String REASON_INACTIVATE = "Укажите причину снятия";

    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse.setCabinetListing(cabinetListingResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).setAvaliableAction(ACTUALIZE, true))).build())
                .build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст «Ещё актуально?» в блоке актуализации")
    public void shouldSeeIsActualTextInActualizationBlock() {
        basePageSteps.onMyOffersPage().snippetFirst().actualizationBlock().text().should(hasText(IS_ACTUAL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик на «Да» в блоке актуализации")
    public void shouldSeeYesClickInActualizationBlock() {
        basePageSteps.onMyOffersPage().snippetFirst().actualizationBlock().buttons().spanLink(YES).click();

        basePageSteps.onMyOffersPage().snippetFirst().actualizationBlock().text().should(hasText(OFFER_WILL_RAISE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик на «Нет» в блоке актуализации")
    public void shouldSeeNoClickInActualizationBlock() {
        basePageSteps.onMyOffersPage().snippetFirst().actualizationBlock().buttons().spanLink(NO).click();

        basePageSteps.onMyOffersPage().modal().title().should(hasText(REASON_INACTIVATE));
    }

}
