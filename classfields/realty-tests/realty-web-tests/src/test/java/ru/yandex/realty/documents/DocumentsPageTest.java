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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.page.DocumentsPage.ENTER_YOUR_EMAIL;
import static ru.yandex.realty.page.DocumentsPage.SEND_DOCUMENTS;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница документов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DocumentsPageTest {

    private static final String ARENDA = "/arenda/";
    private static final String DOCS = "docs";
    private static final String BAD_EMAIL = "123";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DOKUMENTY);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Купля-продажа»")
    public void shouldSeeDocsBuyPage() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().button("Купля-продажа").click();
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).ignoreParam(DOCS)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Аренда»")
    public void shouldSeeDocsRentPage() {
        urlSteps.path(KUPLIA_PRODAZHA).open();
        basePageSteps.onDocumentsPage().button("Аренда").click();
        urlSteps.testing().path(DOKUMENTY).path(ARENDA).ignoreParam(DOCS)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Отправить на почту -> невалидная почта -> кнопка задизейблена")
    public void shouldBadEmailError() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).should(isDisabled());
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).sendKeys(BAD_EMAIL);
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).should(isDisabled());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Отправить на почту > валидная почта появляется «Отправить ещё» > кликаем > возврат инпута и кнопки")
    public void shouldGoodEmailSuccess() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).sendKeys(getRandomEmail());
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).click();
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).should(not(isDisplayed()));
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).should(not(isDisplayed()));
        basePageSteps.onDocumentsPage().spanLink("Отправить ещё").should(isDisplayed()).click();
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).should(isDisplayed());
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на вопросик возле чекбокса ведет на страницу документа")
    public void shouldSeeDocPageClick() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().labelLink("Договор найма жилого помещения").click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DOKUMENTY).path("/dogovor-nayma-zhilogo-pomeshcheniya/")
                .shouldNotDiffWithWebDriverUrl();
    }
}
