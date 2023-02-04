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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.consts.Pages.TRANSPORTIROVKA_PERENOSKI;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с карточки оффера. Открытие ссылок по CMD + Click в новом окне")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class CmdClickLinksTest {

    private static final String TRANSPORTIROVKA_PERENOSKI_TEXT = "Транспортировка, переноски";

    private String offerCardPath;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        basePageSteps.resize(1920, 1080);
        offerCardPath = jSoupSteps.getActualOfferCardUrl();
        urlSteps.testing().path(offerCardPath).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на карточку в блоке похожих снизу")
    public void shouldSeeGoToBottomSimilarCardCmdClick() {
        String cardUrl = basePageSteps.onOfferCardPage().similarSnippetFirst().getUrl();
        basePageSteps.cmdClick(basePageSteps.onOfferCardPage().similarSnippetFirst().hover());
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(cardUrl).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на категорию в ХК с карточки")
    public void shouldSeeGoToBreadcrumbCategoryCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onOfferCardPage().breadcrumbsItem(TRANSPORTIROVKA_PERENOSKI_TEXT));
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText("Транспортировка, переноски в Новосибирске"));
        urlSteps.testing().path(NOVOSIBIRSK).path(TOVARI_DLYA_ZHIVOTNIH).path(TRANSPORTIROVKA_PERENOSKI)
                .shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на главную в сайдбаре с карточки оффера")
    public void shouldSeeGoToHomepageFromSidebarCmdClick() {
        basePageSteps.cmdClick(basePageSteps.onOfferCardPage().sidebarCategories().link("Все объявления"));
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().h1().should(hasText("Объявления в Новосибирске"));
        urlSteps.testing().path(NOVOSIBIRSK).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Cmd+Click переход на профиль продавца с карточки")
    public void shouldSeeGoToSellerProfileCmdClick() {
        String sellerUrl = basePageSteps.onOfferCardPage().sidebar().sellerInfo().link().getAttribute(HREF);
        basePageSteps.cmdClick(basePageSteps.onOfferCardPage().sidebar().sellerInfo());
        basePageSteps.switchToNextTab();

        basePageSteps.onProfilePage().sidebar().followersCount().should(isDisplayed());
        urlSteps.fromUri(sellerUrl).shouldNotDiffWithWebDriverUrl();
        assertThat("Кол-во окон = «2»", basePageSteps.getWindowCount(), is(2));
    }

}
