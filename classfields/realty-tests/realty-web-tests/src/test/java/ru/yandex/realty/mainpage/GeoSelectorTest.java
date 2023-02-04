package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.consts.Location.MOSCOW;
import static ru.yandex.realty.consts.Location.MOSCOW_AND_MO;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.element.base.GeoSelectorPopup.RegionSelectorPopup.SAVE;

@DisplayName("Главная. Геоселектор")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class GeoSelectorTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(VICDEV)
    @Description("Россия > Москва и МО > Москва. Проверяем, что работает ссылка: Москва и МО")
    public void shouldSeeBreadcrumbs() {
        urlSteps.testing().path(MOSCOW.getPath()).open();
        user.onBasePage().headerMain().regionSelector().click();
        user.onBasePage().regionSelectorPopup().breadCrumbs()
                .should("Ожидалось другое количество хлебных крошек для региона <Москва>", hasSize(2));
        user.onBasePage().regionSelectorPopup().breadCrumb(MOSCOW_AND_MO.getName()).click();
        user.onBasePage().regionSelectorPopup().breadCrumbs()
                .should("Ожидалось другое количество хлебных крошек для региона <Москва и МО>", hasSize(1));
        user.onBasePage().regionSelectorPopup().button(SAVE).click();
        urlSteps.testing().path(MOSCOW_AND_MO.getPath()).shouldNotDiffWithWebDriverUrl();
    }
}
