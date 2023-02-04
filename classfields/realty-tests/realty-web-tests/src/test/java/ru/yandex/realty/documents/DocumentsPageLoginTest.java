package ru.yandex.realty.documents;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;
import static ru.yandex.realty.page.DocumentsPage.ENTER_YOUR_EMAIL;
import static ru.yandex.realty.page.DocumentsPage.SEND_DOCUMENTS;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.getRandomUserRequestBody;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница документов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DocumentsPageLoginTest {

    String expectedEmail;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Before
    public void before() {
        expectedEmail = getRandomEmail();
        apiSteps.createVos2Account(account, getRandomUserRequestBody(account.getId(),
                OWNER.getValue()).withEmail(expectedEmail));
        urlSteps.testing().path(DOKUMENTY).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вход под залогином > почта в инпуте")
    public void shouldSeeAccountEmail() {
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).should(hasValue(expectedEmail));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вход под залогином > почта в инпуте > отправить на почту")
    public void shouldSeeSuccessSend() {
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).click();
        basePageSteps.onDocumentsPage().message().should(hasText(containsString(expectedEmail)));
    }
}
