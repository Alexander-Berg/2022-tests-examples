package ru.yandex.arenda.admin.users;

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
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.PETS_INFO;
import static ru.yandex.arenda.pages.LkPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.WITH_PETS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1734")
@DisplayName("[Админка] Редактирование анкеты пользователя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserAnketaPetsTest {

    private static final String EMPTY_STRING = "";
    private static final String DISABLED_ATTRIBUTE = "disabled";
    private static final String TRUE_VALUE = "true";
    private static final String ANKETA = "Анкета";
    private static final String NEW_TEXT = "New_text";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Проверяем валидацию на поле «С животными» и «Описание питомца». Заполнена галочка и описание, " +
            "убрать галочку при убирании галочки поле описание недоступно -> после обновления пустое")
    public void shouldNotSeePetsInfo() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());

        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(isDisplayed());
        lkSteps.onLkPage().label(WITH_PETS).click();
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.refresh();

        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));
    }

    @Test
    @DisplayName("Проверяем валидацию на поле «С животными» и «Описание питомца». Выставляем галку и описание, " +
            "после обновления видимо новое описание")
    public void shouldSeeWithPets() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());

        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(isDisplayed());
        lkSteps.onLkPage().label(WITH_PETS).click();
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.refresh();

        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().label(WITH_PETS).click();
        lkSteps.onLkPage().textAreaId(PETS_INFO).sendKeys(NEW_TEXT);
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.refresh();

        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());
        lkSteps.onLkPage().label(WITH_PETS).should(isChecked());
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(isDisplayed());
        lkSteps.onLkPage().textAreaId(PETS_INFO).should(hasText(NEW_TEXT));
    }
}
