package ru.yandex.arenda.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.constants.UriPath.LK_SDAT_KVARTIRY;
import static ru.yandex.arenda.constants.UriPath.LK_SELECTION;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Переходы по ссылкам ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class LkClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MainSteps mainSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
    }

    @Test
    @DisplayName("Клик на «Подать заявку»")
    public void shouldSeePassApplicationClick() {
        urlSteps.testing().path(LK_SELECTION).open();
        mainSteps.onBasePage().button("Подать заявку").click();
        urlSteps.testing().path(LK_SDAT_KVARTIRY).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Клик на «Заполнить анкету»")
    public void shouldSeeFillAnketaClick() {
        urlSteps.testing().path(LK_SELECTION).open();
        mainSteps.onBasePage().button("Заполнить анкету").click();
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).queryParam("flow", "TENANT")
                .queryParam("spaFromPage", "selection").shouldNotDiffWithWebDriverUrl();
    }
}
