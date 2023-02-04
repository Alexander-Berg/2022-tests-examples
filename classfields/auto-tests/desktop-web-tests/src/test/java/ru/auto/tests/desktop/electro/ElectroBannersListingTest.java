package ru.auto.tests.desktop.electro;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.ENGINE_BENZIN;
import static ru.auto.tests.desktop.consts.Pages.ENGINE_ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.ENGINE_GIBRID;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.ENGINE_GROUP;
import static ru.auto.tests.desktop.consts.QueryParams.GASOLINE;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.TABLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.ELECTRO)
@Feature("Баннеры в листинге")
@DisplayName("Баннеры в листинге")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ElectroBannersListingTest {

    private static final String LISTING_HEAD_ELECTRO_TOOLTIP = "Электромобили\nКак и зачем их покупать";
    private static final String LISTING_ELECTRO_BANNER = "Электромобили\nРассказываем — как выбрать, где заряжать и " +
            "на что обращать внимание при выборе электроавтомобиля\nДавайте посмотрим";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Тултип по ховеру на «Электромобили» в хэдере")
    public void shouldSeeHeaderElectroTooltip() {
        urlSteps.testing().open();
        basePageSteps.onMainPage().header().line2().button("Электромобили").hover();

        basePageSteps.onMainPage().electroTooltip().should(hasText("Электромобили\n" +
                "Рассказываем всё о тонкостях выбора электромобиля, в новом разделе на Авто.ру"));
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в фильтрах, при выбранном электро двигателе")
    public void shouldSeeElectroBannerInFiltersWithElectroEngine() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_ELECTRO).open();
        basePageSteps.onListingPage().listingHeadBanner().waitUntil(
                hasText(LISTING_HEAD_ELECTRO_TOOLTIP)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в фильтрах, при выбранном гибридном двигателе")
    public void shouldSeeElectroBannerInFiltersWithGibridEngine() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_GIBRID).open();
        basePageSteps.onListingPage().listingHeadBanner().waitUntil(
                hasText(LISTING_HEAD_ELECTRO_TOOLTIP)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в фильтрах, при выбранном электро и бензиновом двигателях")
    public void shouldSeeElectroBannerInFiltersWithElectroAndBenzinEngine() {
        urlSteps.testing().path(CARS).path(ALL)
                .addParam(ENGINE_GROUP, QueryParams.ELECTRO)
                .addParam(ENGINE_GROUP, GASOLINE).open();
        basePageSteps.onListingPage().listingHeadBanner().waitUntil(
                hasText(LISTING_HEAD_ELECTRO_TOOLTIP)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет баннера «Электромобили» в фильтрах, при выбранном бензиновом двигателе")
    public void shouldNotSeeElectroBannerInFiltersWithGazolineEngine() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_BENZIN).open();

        basePageSteps.onListingPage().listingHeadBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в листинге, при выбранном электро двигателе")
    public void shouldSeeElectroBannerInListingWithElectroEngine() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_ELECTRO).open();
        basePageSteps.onListingPage().electroBanner().waitUntil(
                hasText(LISTING_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в листинге, при выбранном гибридном двигателе")
    public void shouldSeeElectroBannerInListingWithGibridEngine() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_GIBRID).open();
        basePageSteps.onListingPage().electroBanner().waitUntil(
                hasText(LISTING_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в листинге, при выбранном электро и бензиновом двигателях")
    public void shouldSeeElectroBannerInListingWithElectroAndBenzinEngine() {
        urlSteps.testing().path(CARS).path(ALL)
                .addParam(ENGINE_GROUP, QueryParams.ELECTRO)
                .addParam(ENGINE_GROUP, GASOLINE).open();
        basePageSteps.onListingPage().electroBanner().waitUntil(
                hasText(LISTING_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет баннера «Электромобили» в листинге, при выбранном бензиновом двигателе")
    public void shouldNotSeeElectroBannerInListingWithGazolineEngine() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_BENZIN).open();

        basePageSteps.onListingPage().electroBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет баннера «Электромобили» в листинге, при типе листинга - карусель")
    public void shouldNotSeeElectroBannerInListingWithCarouselListingType() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_ELECTRO).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().electroBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет баннера «Электромобили» в листинге, при типе листинга - таблица")
    public void shouldNotSeeElectroBannerInListingWithTableListingType() {
        urlSteps.testing().path(CARS).path(ALL).path(ENGINE_ELECTRO).addParam(OUTPUT_TYPE, TABLE).open();

        basePageSteps.onListingPage().electroBanner().should(not(isDisplayed()));
    }

}
