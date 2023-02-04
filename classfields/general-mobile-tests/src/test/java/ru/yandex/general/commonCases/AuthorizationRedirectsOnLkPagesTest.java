package ru.yandex.general.commonCases;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;

@Epic("Открытие ЛК страниц незалогином")
@DisplayName("Открытие ЛК страниц незалогином")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AuthorizationRedirectsOnLkPagesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Страница «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Статистика", STATS},
                {"Мои объявления", OFFERS},
                {"Избранное", FAVORITES},
                {"Автозагрузка", FEED},
                {"Настройки", CONTACTS}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MY).path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открываем страницы ЛК незалогином - проверяем редирект на паспорт")
    public void shouldSeeRedirectToPassport() {
        urlSteps.passport().path("auth").queryParam("mode", "auth")
                .queryParam("retpath", urlSteps.testing().path(MY).path(path).toString())
                .queryParam("backpath", urlSteps.testing().toString());
    }

}
