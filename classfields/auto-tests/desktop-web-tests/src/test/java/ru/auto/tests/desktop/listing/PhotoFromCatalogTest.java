package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.element.listing.SalesListItem.PHOTO_FROM_CATALOG;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - фото из каталога")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PhotoFromCatalogTest {

    private static final String PHOTO_FROM_CATALOG_TOOLTIP = "Продавец не загрузил фото автомобиля, " +
            "вы видите изображение из каталога";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsWithPhotoFromCatalog")
        ).create();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение предупреждения, что фото из каталога")
    public void shouldSeePhotoFromCatalogWarning() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();

        basePageSteps.onListingPage().getSale(0).photoFromCatalogWarning().should(hasText(PHOTO_FROM_CATALOG));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение тултипа бейджа «Фото из каталога» по ховеру")
    public void shouldSeePhotoFromCatalogWarningTooltip() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).photoFromCatalogWarning().hover();

        basePageSteps.onListingPage().getSale(0).photoFromCatalogWarningTooltip().waitUntil(isDisplayed())
                .should(hasText(PHOTO_FROM_CATALOG_TOOLTIP));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение бейджа «Фото из каталога», тип листинга «Карусель»")
    public void shouldSeePhotoFromCatalogBadgeCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).badge(PHOTO_FROM_CATALOG).should(isDisplayed());
    }

    @Test
    @Ignore
    @Owner(ALEKS_IVANOV)
    @Issue("AUTORUFRONT-21850")
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение тултипа бейджа «Фото из каталога» по ховеру, тип листинга «Карусель»")
    public void shouldSeePhotoFromCatalogWarningTooltipCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).badge(PHOTO_FROM_CATALOG).hover();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(PHOTO_FROM_CATALOG_TOOLTIP));
    }

}
