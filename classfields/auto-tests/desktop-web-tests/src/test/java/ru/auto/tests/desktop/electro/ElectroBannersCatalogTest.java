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
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.ELECTRO;
import static ru.auto.tests.desktop.consts.QueryParams.ENGINE_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.GASOLINE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.ELECTRO)
@Feature("Баннеры в каталоге")
@DisplayName("Баннеры в каталоге")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ElectroBannersCatalogTest {

    private static final String CATALOG_ELECTRO_BANNER = "Электромобили\nРассказываем — как выбрать, где заряжать и " +
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
    @DisplayName("Баннер «Электромобили» в каталоге, при выбранном электро двигателе")
    public void shouldSeeElectroBannerWithElectroEngine() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(ENGINE_TYPE, QueryParams.ELECTRO).open();
        basePageSteps.onCatalogPage().electroBanner().waitUntil(hasText(CATALOG_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в каталоге, при выбранном гибридном двигателе")
    public void shouldSeeElectroBannerWithHybridEngine() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(ENGINE_TYPE, QueryParams.HYBRID).open();
        basePageSteps.onCatalogPage().electroBanner().waitUntil(hasText(CATALOG_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Баннер «Электромобили» в каталоге, при выбранном электро и бензиновом двигателях")
    public void shouldSeeElectroBannerWithElectroAndBenzinEngine() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL)
                .addParam(ENGINE_TYPE, QueryParams.ELECTRO)
                .addParam(ENGINE_TYPE, GASOLINE).open();
        basePageSteps.onCatalogPage().electroBanner().waitUntil(hasText(CATALOG_ELECTRO_BANNER)).click();

        urlSteps.testing().path(ELECTRO).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет баннера «Электромобили» в каталоге, при выбранном бензиновом двигателе")
    public void shouldNotSeeElectroBannerWithGazolineEngine() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(ENGINE_TYPE, GASOLINE).open();

        basePageSteps.onCatalogPage().electroBanner().should(not(isDisplayed()));
    }

}
