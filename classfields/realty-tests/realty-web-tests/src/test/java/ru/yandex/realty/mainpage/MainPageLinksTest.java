package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Сслыки")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainPageLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String link;

    @Parameterized.Parameter(2)
    public String path;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", "Студии", "/kupit/kvartira/studiya/"},
                {"Купить квартиру", "1-комнатные", "/kupit/kvartira/odnokomnatnaya/"},
                {"Купить квартиру", "2-комнатные", "/kupit/kvartira/dvuhkomnatnaya/"},
                {"Купить квартиру", "3-комнатные", "/kupit/kvartira/tryohkomnatnaya/"},
                {"Купить квартиру", "Комнаты в квартире", "/kupit/komnata/"},
                {"Снять квартиру", "Без посредников", "/snyat/kvartira/bez-posrednikov/"},
                {"Снять квартиру", "1-комнатные", "/snyat/kvartira/odnokomnatnaya/"},
                {"Снять квартиру", "2-комнатные", "/snyat/kvartira/dvuhkomnatnaya/"},
                {"Снять квартиру", "На длительный срок", "/snyat/kvartira/"},
                {"Снять квартиру", "Комнаты на длительный срок", "/snyat/komnata/"},
                {"Коммерческая недвижимость", "Купить офис", "/kupit/kommercheskaya-nedvizhimost/ofis/"},
                {"Коммерческая недвижимость", "Аренда офиса", "/snyat/kommercheskaya-nedvizhimost/ofis/"},
                {"Загородная недвижимость", "Купить участок", "/kupit/uchastok/"},
                {"Гаражи", "Купить гараж", "/kupit/garazh/"},
                {"Гаражи", "Снять гараж", "/snyat/garazh/"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Smoke.class, Production.class})
    @DisplayName("Проверяем урл при переходе по ссылке")
    public void shouldSeeTypeInUrl() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMainPage().mainBlock(name).link(link).waitUntil(isDisplayed()).click();
        urlSteps.path(path).shouldNotDiffWithWebDriverUrl();
    }
}
