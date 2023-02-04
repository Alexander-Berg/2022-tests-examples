package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки в подхедере")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HeaderMainLinksTestWhithoutParams {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;



    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        urlSteps.setMoscowCookie();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка хедера. Аренда")
    public void shouldSeeMainLinksArenda() {
        user.onBasePage().headerUnder().mainMenuItem("YANDEX_ARENDA")
                .should(hasAttribute(
                        "href", equalTo("https://arenda.test.vertis.yandex.ru/?from=main_menu&utm_source=header_yarealty")));
    }


}
