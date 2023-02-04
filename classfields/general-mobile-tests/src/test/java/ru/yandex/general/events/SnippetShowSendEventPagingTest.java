package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.TOVARI_DLYA_ZHIVOTNIH;
import static ru.yandex.general.consts.QueryParams.PAGE_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.LIST;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SNIPPET_SHOW)
@DisplayName("Отправка поля page в событии «snippetShow» со второй страницы листинга категории, списком/плиткой")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetShowSendEventPagingTest {

    private static final int PAGE_NUMBER = 2;
    private static final int FIVE_SNIPPET_ROWS_WIDTH = 1200;
    private static final int STEP_PXLS = 15;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String listingType;

    @Parameterized.Parameters(name = "Тип листинга «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {LIST},
                {GRID}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, listingType);
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка поля page в «snippetShow» со второй страницы листинга категории, списком/плиткой")
    public void shouldSeeSnippetShowSendEventSecondPage() {
        urlSteps.testing().path(TOVARI_DLYA_ZHIVOTNIH).queryParam(PAGE_PARAM, String.valueOf(PAGE_NUMBER)).open();
        basePageSteps.scrolling(FIVE_SNIPPET_ROWS_WIDTH, STEP_PXLS);

        eventSteps.withEventType(SNIPPET_SHOW).allEventsWithPageNumber(PAGE_NUMBER).shouldExist();
    }

}
