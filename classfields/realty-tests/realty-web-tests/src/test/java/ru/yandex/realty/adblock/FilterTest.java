package ru.yandex.realty.adblock;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Adblock;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModuleWithAdBlock;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница с AdBlock'ом")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithAdBlock.class)
public class FilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Adblock.class, Production.class})
    @DisplayName("Проверяем фильтр типа жилья")
    public void shouldSeeRentPageUrl() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.findAdblockCookie();
        basePageSteps.onOffersSearchPage().filters().checkButton("2");
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.path("dvuhkomnatnaya/").shouldNotDiffWithWebDriverUrl();
    }
}
