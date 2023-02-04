package ru.yandex.realty.documents;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.RealtyFeatures.MOBILE;
import static ru.yandex.realty.mobile.page.DocumentsPage.BUY_AKT;
import static ru.yandex.realty.mobile.page.DocumentsPage.BUY_DOGOVOR;
import static ru.yandex.realty.mobile.page.DocumentsPage.BUY_RASPISKA;
import static ru.yandex.realty.mobile.page.DocumentsPage.RENT_AKT;
import static ru.yandex.realty.mobile.page.DocumentsPage.RENT_AKT_VOZVRAT;
import static ru.yandex.realty.mobile.page.DocumentsPage.RENT_DOGOVOR;
import static ru.yandex.realty.mobile.page.DocumentsPage.RENT_RASPISKA;
import static ru.yandex.realty.mobile.page.DocumentsPage.RENT_RASTORZHENIE;
import static ru.yandex.realty.mobile.page.DocumentsPage.RENT_SPISOK;

@DisplayName("Страница документов")
@Feature(MOBILE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DokumentyPageTest {

    public static final String SEND = "Отправить";
    public static final String SEND_MORE = "Отправить ещё";
    public static final String ENTER_EMAIL = "Введите свою электронную почту";
    public static final String ARENDA = "/arenda/";
    public static final String DOCS = "docs";
    public static final String SELL_CONTRACT = "SELL_CONTRACT";
    public static final String DISABLED = "_disabled";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Купля-продажа»")
    public void shouldSeeDocsBuyPage() {
        urlSteps.testing().path(DOKUMENTY).open();
        basePageSteps.onDocumentsPage().button("Купля-продажа").click();
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).ignoreParam(DOCS)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Аренда»")
    public void shouldSeeDocsRentPage() {
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).open();
        basePageSteps.onDocumentsPage().button("Аренда").click();
        urlSteps.testing().path(DOKUMENTY).path(ARENDA).ignoreParam(DOCS)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Отправить» отключается при деселекте чекбоксов")
    public void shouldSeeUncheckedBuyButton() {
        urlSteps.testing().path(DOKUMENTY).path(KUPLIA_PRODAZHA).open();
        basePageSteps.onDocumentsPage().input(ENTER_EMAIL, getRandomEmail());
        basePageSteps.onDocumentsPage().deselectCheckBox(BUY_DOGOVOR);
        basePageSteps.onDocumentsPage().deselectCheckBox(BUY_AKT);
        basePageSteps.onDocumentsPage().deselectCheckBox(BUY_RASPISKA);
        basePageSteps.onDocumentsPage().button(SEND).should(hasClass(containsString(DISABLED)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Отправить» включается при отсутствующей почте")
    public void shouldSeeUncheckedRentButton() {
        urlSteps.testing().path(DOKUMENTY).path(ARENDA).open();
        basePageSteps.onDocumentsPage().button(SEND).waitUntil(hasClass(containsString(DISABLED)));
        basePageSteps.onDocumentsPage().input(ENTER_EMAIL, getRandomEmail());
        basePageSteps.onDocumentsPage().button(SEND).should(hasClass(not(containsString(DISABLED))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Заходим на страницу без чекнутых чекбоксов и выбираем все и отправляем на почту")
    public void shouldSeeSentLetter() {
        urlSteps.testing().path(DOKUMENTY).path(ARENDA).queryParam(DOCS, SELL_CONTRACT).open();
        basePageSteps.onDocumentsPage().input(ENTER_EMAIL, getRandomEmail());
        basePageSteps.onDocumentsPage().selectCheckBox(RENT_DOGOVOR);
        basePageSteps.onDocumentsPage().selectCheckBox(RENT_AKT);
        basePageSteps.onDocumentsPage().selectCheckBox(RENT_SPISOK);
        basePageSteps.onDocumentsPage().selectCheckBox(RENT_RASPISKA);
        basePageSteps.onDocumentsPage().selectCheckBox(RENT_RASTORZHENIE);
        basePageSteps.onDocumentsPage().selectCheckBox(RENT_AKT_VOZVRAT);
        basePageSteps.onDocumentsPage().button(SEND).click();
        basePageSteps.onDocumentsPage().spanLink(SEND_MORE).should(isDisplayed());
    }
}
