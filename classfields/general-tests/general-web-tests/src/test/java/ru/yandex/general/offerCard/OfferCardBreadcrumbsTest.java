package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOBILNIE_TELEFONI;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.TELEFONY_I_UMNYE_CHASY;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@DisplayName("Текст и ссылки в хлебных крошках")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardBreadcrumbsTest {

    private static final String ID = "12345";

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

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .setCurrentUserExample()
                .build()).withDefaults().create();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст и ссылки в хлебных крошках")
    public void shouldSeeOfferCardBreadcrumbs() {
        basePageSteps.onOfferCardPage().breadcrumbsList().should(hasItems(
                allOf(hasAttribute(HREF, urlSteps.testing().path(ROSSIYA).toString()), hasText("Все объявления")),
                allOf(hasAttribute(HREF, urlSteps.testing().path(MOSKVA).toString()), hasText("Москва")),
                allOf(hasAttribute(HREF, urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).toString()), hasText("Электроника")),
                allOf(hasAttribute(HREF, urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).path(TELEFONY_I_UMNYE_CHASY).toString()), hasText("Телефоны и умные часы")),
                allOf(hasAttribute(HREF, urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).path(MOBILNIE_TELEFONI).toString()), hasText("Мобильные телефоны"))
        ));
    }

}
