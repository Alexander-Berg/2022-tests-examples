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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOGOVOR_KUPLI_PRODAZHI;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.page.DocumentsPage.ENTER_YOUR_EMAIL;
import static ru.yandex.realty.page.DocumentsPage.SEND_DOCUMENTS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница шаблона")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DocTemplateTest {

    public static final String CAROUSEL_ITEM_TEXT = "Образец передаточного акта к договору купли-продажи квартиры";
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
        urlSteps.testing().path(DOKUMENTY).path(DOGOVOR_KUPLI_PRODAZHI);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Отправить на почту > невалидная почта появляется «Попробуйте отправить ещё раз» > кликаем > " +
            "возврат инпута и кнопки")
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
    @DisplayName("Ссылка на основную страницу документов")
    public void shouldSeeLinkToDocuments() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().link("Шаблоны документов").click();
        urlSteps.testing().path(DOKUMENTY).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("В карусели документов нет ссылки на текущий")
    public void shouldNotSeeCarouselLinkCurrent() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().docCarousel().forEach(e ->
                e.should(not(hasHref(containsString("/dogovor-kupli-prodazhi-nedvizhimogo-imushchestva/")))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на ссылку в карусели документов")
    public void shouldSeeClickOnCarouselLink() {
        urlSteps.open();
        basePageSteps.onDocumentsPage().docCarousel().waitUntil(hasSize(greaterThan(0)))
                .filter(e -> e.getText().contains(CAROUSEL_ITEM_TEXT)).get(FIRST).click();
        urlSteps.testing().path(DOKUMENTY).path("/peredatochnyy-akt-k-dogovoru-kupli-prodazhi/")
                .shouldNotDiffWithWebDriverUrl();
    }
}
