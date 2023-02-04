package ru.yandex.realty.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.FAVORITES;


@Issue("VERTISTEST-1355")
@Epic(RealtyFeatures.FAVORITES)
@DisplayName("Пустое избранное")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class EmptyFavoritesScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим скриншот пустого избранного")
    public void shouldSeeEmptyFavoritesScreenshot() {
        urlSteps.testing().path(FAVORITES).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFavoritesPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
