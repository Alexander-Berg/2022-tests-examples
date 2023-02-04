package ru.yandex.arenda.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.arenda.constants.AttributeLocator.VALUE_ATTRIBUTE;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.LK_SDAM;
import static ru.yandex.arenda.constants.UriPath.LK_SDAT_KVARTIRY;
import static ru.yandex.arenda.element.lk.OwnerFlatSnippet.CONFIRM_APPLICATION;
import static ru.yandex.arenda.matcher.AttributeMatcher.isDisabled;
import static ru.yandex.arenda.pages.LkPage.CONFIRMATION_CODE_ID;
import static ru.yandex.arenda.pages.LkPage.CONFIRMATION_CODE_NUMBER;
import static ru.yandex.arenda.pages.LkPage.CONFIRM_BUTTON;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.PASS_TO_LK_BUTTON;
import static ru.yandex.arenda.pages.LkPage.PHONE_ID;
import static ru.yandex.arenda.pages.LkPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SURNAME_ID;
import static ru.yandex.arenda.pages.LkPage.TEST_ADDRESS;
import static ru.yandex.arenda.pages.LkPage.TEST_NAME;
import static ru.yandex.arenda.pages.LkPage.TEST_PHONE;
import static ru.yandex.arenda.pages.LkPage.TEST_SURNAME;
import static ru.yandex.arenda.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.arenda.utils.UtilsWeb.makePhoneFormatted;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Link("https://st.yandex-team.ru/VERTISTEST-1662")
@DisplayName("Создание заявки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class CreateApplicationTest {

    public static final String NAME_FIELD = "name";
    public static final String SURNAME_FIELD = "surname";
    public static final String PERSON_FIELD = "person";
    private String phone;
    private String name;
    private String surname;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_SDAT_KVARTIRY).open();
        phone = TEST_PHONE;
        name = TEST_NAME;
        surname = TEST_SURNAME;
        lkSteps.fillFormApplication(TEST_ADDRESS, name, surname, phone);
        lkSteps.onLkPage().button(SEND_BUTTON).waitUntil(isEnabled()).click();
        lkSteps.onLkPage().h1().waitUntil("Ждем перехода в «Подтверждение заявки»", hasText("Подтверждение заявки"));

    }

    @Test
    @DisplayName("Создаем новую заявку, кликаем на «Оправить» и не подтверждаем, данные юзера не поменялись")
    public void shouldSeeUserData() {
        JsonObject userInfo = retrofitApiSteps.getUserByUid(account.getId());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(userInfo.getAsJsonObject(PERSON_FIELD).entrySet().isEmpty())
                    .as("Проверяем что поле «person» пусто").isTrue();
            softly.assertThat(userInfo.has("phone"))
                    .as("Проверяем что поле «phone» отсутствует").isFalse();
        });
    }

    @Test
    @DisplayName("Создаем новую заявку, не подтверждаем -> видим кнопку «Подтвердить заявку»")
    public void shouldSeeConfirmButton() {
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkPage().buttonRequest(CONFIRM_APPLICATION).should(isDisplayed());
    }

    @Test
    @DisplayName("Создаем новую квартиру, кликаем на «Отправить повторно -> видим минуту»")
    public void shouldSeeSendSms1Minute() {
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkPage().buttonRequest(CONFIRM_APPLICATION).click();
        urlSteps.shouldUrl("Должны быть на странице заявок",
                containsString(urlSteps.testing().path(LK_SDAT_KVARTIRY).toString()));
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().exactButton(SEND_BUTTON).should(not(isDisplayed()));
        lkSteps.onLkPage().button("Отправить через").should(isDisplayed()).should(isDisabled());
    }

    @Test
    @DisplayName("Создаем новую квартиру, кликаем на «Отправить», видим сохраненные данные в беке")
    public void shouldSeeUserDraftBack() {
        lkSteps.onLkPage().inputId(CONFIRMATION_CODE_ID).sendKeys(CONFIRMATION_CODE_NUMBER);
        lkSteps.onLkPage().button(CONFIRM_BUTTON).click();
        lkSteps.onLkPage().successModal().button(PASS_TO_LK_BUTTON).click();
        urlSteps.shouldUrl("Должны быть на странице заявок",
                containsString(urlSteps.testing().path(LK).path(FLAT).toString()));
        String uid = account.getId();
        JsonObject userInfo = retrofitApiSteps.getUserByUid(uid);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(userInfo.getAsJsonObject(PERSON_FIELD).getAsJsonPrimitive(NAME_FIELD).getAsString())
                    .as("Проверяем что поле «name»").isEqualTo(name);
            softly.assertThat(userInfo.getAsJsonObject(PERSON_FIELD).getAsJsonPrimitive(SURNAME_FIELD).getAsString())
                    .as("Проверяем что поле «surname»").isEqualTo(surname);
            softly.assertThat(userInfo.getAsJsonPrimitive("phone").getAsString())
                    .as("Проверяем что поле «phone»").isEqualTo(format("+%s", phone));
        });
    }

    @Test
    @DisplayName("Создаем новую квартиру, кликаем на «Отправить», видим сохрененные данные при подаче новой")
    public void shouldSeeUserDraftFront() {
        lkSteps.onLkPage().inputId(CONFIRMATION_CODE_ID).sendKeys(CONFIRMATION_CODE_NUMBER);
        lkSteps.onLkPage().button(CONFIRM_BUTTON).click();
        lkSteps.onLkPage().successModal().button(PASS_TO_LK_BUTTON).click();
        urlSteps.testing().path(LK_SDAT_KVARTIRY).open();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(lkSteps.onLkPage().inputId(NAME_ID).getAttribute(VALUE_ATTRIBUTE))
                    .isEqualTo(name);
            softly.assertThat(lkSteps.onLkPage().inputId(SURNAME_ID).getAttribute(VALUE_ATTRIBUTE))
                    .isEqualTo(surname);
            softly.assertThat(lkSteps.onLkPage().inputId(PHONE_ID).getAttribute(VALUE_ATTRIBUTE))
                    .isEqualTo(makePhoneFormatted(phone, PHONE_PATTERN_BRACKETS));
        });
    }
}
