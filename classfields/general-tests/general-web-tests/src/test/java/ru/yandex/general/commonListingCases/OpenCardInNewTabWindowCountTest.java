package ru.yandex.general.commonListingCases;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.Pages.SLASH;

@Epic("Общие кейсы для страниц с листингами")
@Feature(NAVIGATION_FEATURE)
@DisplayName("Кол-во окон после открытия оффера = «2»")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OpenCardInNewTabWindowCountTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"С главной", SLASH},
                {"С листинга категории", ELEKTRONIKA},
                {"С профиля", format("%s%s", PROFILE, SELLER_PATH).replace("//", "/")}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во окон после открытия оффера = «2»")
    public void shouldSeeWindowCountAfterOpenOfferCard() {
        basePageSteps.onListingPage().snippetFirst().click();

        assertThat("Кол-во окон после открытия оффера = «2»",
                basePageSteps.getWindowCount(), is(2));
    }

}
