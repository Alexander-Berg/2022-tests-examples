package ru.yandex.realty.journal.touch;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AsideCategoriesTest {

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
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> placementPeriod() {
        return asList(new Object[][]{
                {"Все статьи", "/journal/"},
                {"Аналитика", "/journal/category/analitika/"},
                {"Аренда", "/journal/category/arenda/"},
                {"Дизайн", "/journal/category/dizayn/"},
                {"Ипотека", "/journal/category/ipoteka/"},
                {"Новости", "/journal/category/novosti-nedvizhimosti/"},
                {"Новостройки", "/journal/category/novostroyki/"},
                {"Ремонт и дизайн", "/journal/category/remont-i-dizayn/"},
                {"Спецпроект", "/journal/category/specproekt/"},
                {"Вторичное жильё", "/journal/category/vtorichnoe-zhilyo/"},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылки слева")
    public void shouldSeeAsideLinks() {
        urlSteps.testing().path(JOURNAL).open();
        basePageSteps.onJournalPage().asideLink(title)
                .should(hasHref(equalTo(urlSteps.testing().path(expected).toString())));
    }
}
