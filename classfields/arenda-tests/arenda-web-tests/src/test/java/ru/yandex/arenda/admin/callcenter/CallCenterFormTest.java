package ru.yandex.arenda.admin.callcenter;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.rule.MockRuleConfigurable;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.OUTSTAFF_CALL_CENTER_FORM;
import static ru.yandex.arenda.mock.ModerationFlatsSearchByAddress.searchByAddressTemplate;
import static ru.yandex.arenda.pages.AdminCallCenterPage.ADDRESS_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.ESTIMATED_RENT_DURATION_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.FIND_BUTTON;
import static ru.yandex.arenda.pages.AdminCallCenterPage.NUMBER_OF_ADULTS_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.NUMBER_OF_CHILDREN_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.ONLINE_SHOWING_DATE_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.ONLINE_SHOWING_SLOT_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.AdminCallCenterPage.SHOWING_TYPE_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.TENANT_NAME_ID;
import static ru.yandex.arenda.pages.AdminCallCenterPage.TENANT_PHONE_ID;
import static ru.yandex.arenda.pages.BasePage.CALL_CENTER_LINK;
import static ru.yandex.arenda.steps.MainSteps.FIRST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1830")
@DisplayName("[ARENDA] Форма создания показа КЦ")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class CallCenterFormTest {

    private static final String TEST_ADDRESS = "Санкт‑Петербург, Пискарёвский проспект";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.callCenterLogin();
    }

    @Test
    @DisplayName("Видим строчку «Call-центр» в выпадающем меню")
    public void shouldSeeCallCenterRow() {
        urlSteps.testing().open();
        lkSteps.onBasePage().myCabinet().click();
        lkSteps.onBasePage().myCabinetPopupDesktop().link(CALL_CENTER_LINK).click();
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Видим форму колл-центра")
    public void shouldSeeCallCenterForm() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        lkSteps.onAdminCallCenterPage().callCenterForm().should(isDisplayed());
    }

    @Test
    @DisplayName("Видим квартиру выбранную в саджесте с нужным адресом")
    public void shouldSeeFlatFromSuggest() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        lkSteps.onAdminCallCenterPage().inputId(ADDRESS_ID).sendKeys(TEST_ADDRESS);
        lkSteps.onAdminCallCenterPage().suggestItems().waitUntil(hasSize(greaterThan(0))).get(FIRST).click();
        lkSteps.onAdminCallCenterPage().button(FIND_BUTTON).click();
        lkSteps.onAdminCallCenterPage().firstSnippet().snippetAddress().should(hasText(containsString(TEST_ADDRESS)));
    }

    @Test
    @DisplayName("Видим «Информацию о жильце» по клику на сниппет")
    public void shouldSeeTenantInfoForm() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().tenantInfo().should(not(isDisplayed()));
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        lkSteps.onAdminCallCenterPage().tenantInfo().should(isDisplayed());
    }

    @Test
    @DisplayName("Видим ошибки полей, кол-во детей - необязательно")
    public void shouldSeeErrorTenantInfoForm() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().tenantInfo().should(not(isDisplayed()));
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        lkSteps.onAdminCallCenterPage().tenantInfo().should(isDisplayed());
        lkSteps.onAdminCallCenterPage().button(SEND_BUTTON).click();
        lkSteps.onAdminCallCenterPage().invalidInputCallCenter(TENANT_NAME_ID).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().invalidInputCallCenter(TENANT_PHONE_ID).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().invalidInputCallCenter(ESTIMATED_RENT_DURATION_ID).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().invalidInputCallCenter(NUMBER_OF_ADULTS_ID).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().invalidInputCallCenter(NUMBER_OF_CHILDREN_ID).should(not(isDisplayed()));
        lkSteps.onAdminCallCenterPage().invalidInputCallCenter(TENANT_NAME_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("При нажатии на «Обновить свободные слоты» появляется success toast")
    public void shouldSeeSuccessToastFromRefresh() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        lkSteps.onAdminCallCenterPage().option("Онлайн").click();
        lkSteps.onAdminCallCenterPage().button("Обновить свободные слоты").click();
        lkSteps.onAdminCallCenterPage().successToast();
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMMM, yyyy").withLocale(Locale.forLanguageTag("ru")));
    }

    @Test
    @DisplayName("Онлайн -> появляются поля с датой показа (тек.дата+2 дня) и время показа")
    public void shouldSeeExtraFieldsForm() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        lkSteps.onAdminCallCenterPage().selector(ONLINE_SHOWING_DATE_ID).should(not(isDisplayed()));
        lkSteps.onAdminCallCenterPage().selector(ONLINE_SHOWING_SLOT_ID).should(not(isDisplayed()));
        lkSteps.onAdminCallCenterPage().selector(SHOWING_TYPE_ID).click();
        lkSteps.onAdminCallCenterPage().option("Онлайн").click();
        lkSteps.onAdminCallCenterPage().selector(ONLINE_SHOWING_DATE_ID).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().selector(ONLINE_SHOWING_SLOT_ID).should(isDisplayed());

        final LocalDate now = LocalDate.now();
        String currentDate = formatDate(now);
        String currentDatePlusOne = formatDate(now.plusDays(1));
        String currentDatePlusTwo = formatDate(now.plusDays(2));
        lkSteps.onAdminCallCenterPage().option(currentDate).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().option(currentDatePlusOne).should(isDisplayed());
        lkSteps.onAdminCallCenterPage().option(currentDatePlusTwo).should(isDisplayed());
    }

    @Test
    @DisplayName("Видим скрин «Информацию о жильце»")
    public void shouldSeeCallCenterFormScreenshot() {
        compareSteps.resize(1600, 3000);
        mockRuleConfigurable.byAddressStub(searchByAddressTemplate().addSecondFlat().build())
                .createWithDefaults();
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onAdminCallCenterPage().tenantInfo());

        urlSteps.setProductionHost().open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onAdminCallCenterPage().tenantInfo());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Вводим адрес -> видим сниппет")
    private void findSnippet() {
        lkSteps.onAdminCallCenterPage().inputId(ADDRESS_ID).sendKeys(TEST_ADDRESS);
        lkSteps.onAdminCallCenterPage().suggestItems().waitUntil(hasSize(greaterThan(0))).get(FIRST).click();
        lkSteps.onAdminCallCenterPage().button(FIND_BUTTON).click();
    }

    @Test
    @DisplayName("После заполнения полей и нажатия на «Отправить» ")
    public void shouldSeeFilledForm() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().tenantInfo().should(not(isDisplayed()));
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        lkSteps.onAdminCallCenterPage().tenantInfo().should(isDisplayed());
        lkSteps.onAdminCallCenterPage().inputId(TENANT_NAME_ID).sendKeys(getRandomString());
        lkSteps.onAdminCallCenterPage().inputId(TENANT_PHONE_ID).sendKeys("7963" + randomNumeric(7));
        lkSteps.onAdminCallCenterPage().inputId(ESTIMATED_RENT_DURATION_ID).sendKeys("23");
        lkSteps.onAdminCallCenterPage().inputId(NUMBER_OF_ADULTS_ID).sendKeys("2");
        lkSteps.onAdminCallCenterPage().selector(SHOWING_TYPE_ID).click();
        lkSteps.onAdminCallCenterPage().option("Без показа").click();
        lkSteps.onAdminCallCenterPage().button(SEND_BUTTON).click();
        lkSteps.onAdminCallCenterPage().successModal().button("Хорошо").isDisplayed();
    }

    @Ignore("МОКИ РАБОТАЮТ. НЕ РАБОТАЕТ ЧТО-ТО ДРУГОЕ")
    @Test
    @DisplayName("Если есть три оффлайн показа по квартире в поле «Тип просмотра» можно выбрать только онлайн, " +
            "остальные поля доступны.")
    public void shouldSeeThreeOfflineShowings() {
        mockRuleConfigurable.byAddressStub(searchByAddressTemplate().add3OnlineShowings().build())
                .createWithDefaults();
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF_CALL_CENTER_FORM).open();
        findSnippet();
        lkSteps.onAdminCallCenterPage().firstSnippet().click();
        lkSteps.onAdminCallCenterPage().tenantInfo().showingTypeSelector().click();
        lkSteps.onAdminCallCenterPage().tenantInfo().showingTypeSelector().click();
    }
}
