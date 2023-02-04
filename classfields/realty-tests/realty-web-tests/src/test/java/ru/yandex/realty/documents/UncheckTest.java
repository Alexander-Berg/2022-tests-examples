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
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.DOKUMENTY;
import static ru.yandex.realty.consts.Pages.KUPLIA_PRODAZHA;
import static ru.yandex.realty.consts.RealtyFeatures.DOCUMENTS_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.page.DocumentsPage.DOCUMENTS_DOWNLOAD;
import static ru.yandex.realty.page.DocumentsPage.ENTER_YOUR_EMAIL;
import static ru.yandex.realty.page.DocumentsPage.SEND_DOCUMENTS;

@Feature(DOCUMENTS_FEATURE)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница документов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class UncheckTest {

    private static final String TEST_EMAIL = getRandomEmail();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.resize(1400, 1600);
        urlSteps.testing().path(DOKUMENTY);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Аренда» Деселект чекбоксов документов -> кнопка «Скачать шаблоны» дизейблится")
    public void shouldSeeUnselectCheckboxesOnRent() {
        urlSteps.open();
        clickCheckboxesCheckButton();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Купля-продажа» Деселект чекбоксов документов -> кнопка «Скачать шаблоны» дизейблится")
    public void shouldSeeUnselectCheckboxesOnBuy() {
        urlSteps.path(KUPLIA_PRODAZHA).open();
        clickCheckboxesCheckButton();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вкладка «Аренда». Селект всех чекбоксов документов -> кнопка «Скачать шаблоны» активируется")
    public void shouldSeeSelectCheckboxesOnRent() {
        urlSteps.open();
        clickCheckboxesCheckButton();
        basePageSteps.onDocumentsPage().checkBoxList().forEach(c -> c.clickWhile(isChecked()));
        basePageSteps.onDocumentsPage().link(DOCUMENTS_DOWNLOAD).should(not(isDisabled()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Отправить» активируется при вводе в инпут")
    public void shouldSeeEnabledInputOnBuy() {
        urlSteps.path(KUPLIA_PRODAZHA).open();
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).waitUntil(isDisabled());
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).sendKeys(TEST_EMAIL);
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).should(not(isDisabled()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Отправить» дизейблится при деселекте чекбоксов при вводе в инпут")
    public void shouldSeeDisabledInputOnBuy() {
        urlSteps.path(KUPLIA_PRODAZHA).open();
        basePageSteps.onDocumentsPage().input(ENTER_YOUR_EMAIL).sendKeys(TEST_EMAIL);
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).waitUntil(not(isDisabled()));
        clickCheckboxesCheckButton();
        basePageSteps.onDocumentsPage().button(SEND_DOCUMENTS).should(isDisabled());
    }

    private void clickCheckboxesCheckButton() {
        basePageSteps.onDocumentsPage().link(DOCUMENTS_DOWNLOAD).waitUntil(not(isDisabled()));
        basePageSteps.onDocumentsPage().checkBoxList().forEach(c -> c.waitUntil(isChecked()));
        basePageSteps.onDocumentsPage().checkBoxList().forEach(c -> c.clickWhile(not(isChecked())));
        basePageSteps.onDocumentsPage().link(DOCUMENTS_DOWNLOAD).waitUntil(isDisabled());
    }
}
