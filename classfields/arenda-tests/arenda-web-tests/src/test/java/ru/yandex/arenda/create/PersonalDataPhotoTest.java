package ru.yandex.arenda.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.element.lk.PhotoPassportItem.LOAD_BUTTON;
import static ru.yandex.arenda.element.lk.PhotoPassportItem.PERSONAL_DATA_INPUT;
import static ru.yandex.arenda.element.lk.PhotoPassportItem.REGISTRATION_INPUT;
import static ru.yandex.arenda.element.lk.PhotoPassportItem.WITH_SELFIE_INPUT;
import static ru.yandex.arenda.pages.LkPage.SEND_TO_CHECK_BUTTON;
import static ru.yandex.arenda.steps.LkSteps.getNumberedImagePath;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2139")
@DisplayName("Личные данные")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PersonalDataPhotoTest {

    private static final String PASSPORT_TEXT = "9999999999";
    private static final String PASSPORT_ISSUE_BY = getRandomString();
    private static final String ISSUE_DATE_TEXT = "01012000";
    private static final String DEPARTMENT_TEXT = "123 - 456";
    private static final String BIRTHDAY_TEXT = "01011990";
    private static final String BIRTHPLACE_TEXT = getRandomString();
    private static final String NAME = "Валерий";
    private static final String SURNAME = "Валеридзе";
    private static final String PATRONYMIC = "Валерыч";
    private static final String HEADER_TEXT_PERSONAL_DATA = "Личные данные";

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
    private LkSteps lkSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
    }

    @Test
    @DisplayName("Отправляем заявку с фотографиями -> в персональных данных появляется возможность редактирования")
    public void shouldSeeSuccessDataWithPhoto() {
        lkSteps.fillFormPersonalData(NAME, SURNAME, PATRONYMIC);
        lkSteps.fillPhonePersonalData();
        lkSteps.fillFormPassportData(PASSPORT_TEXT, PASSPORT_ISSUE_BY, ISSUE_DATE_TEXT, DEPARTMENT_TEXT, BIRTHDAY_TEXT,
                BIRTHPLACE_TEXT);
        addPhoto(0, PERSONAL_DATA_INPUT);
        addPhoto(1, REGISTRATION_INPUT);
        addPhoto(2, WITH_SELFIE_INPUT);
        lkSteps.onLkPage().editPhoneButton().waitUntil(isDisplayed());
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().successToast();
        lkSteps.onLkPage().photoPassportItems().should(hasSize(0));
        lkSteps.onLkPage().headerText(HEADER_TEXT_PERSONAL_DATA).waitUntil(isDisplayed());
        lkSteps.onLkPage().link("Редактировать").should(isDisplayed());
    }

    private void addPhoto(int i, String text) {
        lkSteps.setFileDetector();
        lkSteps.onLkPage().photoPassportItems().get(i).waitUntil(hasText(containsString(text)));
        lkSteps.onLkPage().photoPassportItems().get(i).button(LOAD_BUTTON).waitUntil(isDisplayed());
        lkSteps.onLkPage().photoPassportItems().get(i).photoInput().sendKeys(getNumberedImagePath(1));
        lkSteps.onLkPage().successPhoto().should(hasSize(i + 1)).get(i)
                .should(allOf(hasText(Matchers.containsString("Нажмите,")),
                        hasText(Matchers.containsString("чтобы посмотреть"))));
    }
}
