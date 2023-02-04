package ru.yandex.arenda.outstaff;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.arenda.constants.UriPath.COPYWRITER;
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.OUTSTAFF;
import static ru.yandex.arenda.constants.UriPath.PHOTOGRAPHER;
import static ru.yandex.arenda.constants.UriPath.RETOUCHER;
import static ru.yandex.arenda.matcher.AttributeMatcher.hasHref;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Link("https://st.yandex-team.ru/VERTISTEST-1834")
@DisplayName("[ARENDA] Формы копирайтера/ретушера/фотографа")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OutstaffRolesPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String user;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Страница «{0}»")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"фотографа", PHOTOGRAPHER},
                {"копирайтера", COPYWRITER},
                {"ретушёра", RETOUCHER}
        });
    }

    @Before
    public void before() {
        passportSteps.outstaffLogin();
    }

    @Test
    @DisplayName("Видим квартиру для ")
    public void shouldSeeOutstaffPage() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(path).path(FLATS).open();
        lkSteps.onOutstaffPage().h1().should(hasText(format("Поиск квартир для %s", user)));
        lkSteps.onOutstaffPage().myCabinet().click();
        lkSteps.onOutstaffPage().myCabinetPopupDesktop().link(format("Для %s", user))
                .should(hasHref(equalTo(urlSteps.toString())));
    }
}
