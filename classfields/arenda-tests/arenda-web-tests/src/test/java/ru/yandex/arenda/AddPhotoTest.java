package ru.yandex.arenda;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
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
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.arenda.constants.UriPath.LK_SDAM;
import static ru.yandex.arenda.element.lk.ownerlk.ToDoRow.ADD_LINK;
import static ru.yandex.arenda.element.lk.ownerlk.ToDoRow.ADD_PHOTO;
import static ru.yandex.arenda.pages.LkOwnerFlatPhotoPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SAVE_BUTTON;
import static ru.yandex.arenda.steps.RetrofitApiSteps.PATH_TO_POST_FLAT_DRAFT;
import static ru.yandex.arenda.utils.UtilsWeb.getObjectFromJson;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1831")
@DisplayName("[ARENDA] Загрузка фоток собственником")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class AddPhotoTest {

    private String uid;

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
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.login(account);
        uid = account.getId();
        retrofitApiSteps.createUser(uid);
    }

    @Test
    @DisplayName("Кнопка  «Добавить фотографии» есть для подтвержденной квартиры")
    public void shouldSeeLoadButton() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).should(isDisplayed());
    }

    @Test
    @DisplayName("Кнопка  «Добавить фотографии» нет для квартиры в статусе черновика")
    public void shouldNotSeeLoadButtonOnDraft() {
        retrofitApiSteps.postFlatDraft(uid, getObjectFromJson(JsonObject.class, PATH_TO_POST_FLAT_DRAFT));
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).should(not(isDisplayed()));
    }

    @Test
    @DisplayName("Кнопка  «Добавить фотографии» нет для квартиры в статусе ожидания")
    public void shouldNotSeeLoadButton() {
        String flatId = retrofitApiSteps.postFlatDraft(uid, getObjectFromJson(JsonObject.class, PATH_TO_POST_FLAT_DRAFT));
        retrofitApiSteps.sendSms(uid, flatId);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).should(not(isDisplayed()));
    }


    @Test
    @DisplayName("Нет фото - скриншотный тест")
    public void shouldSeeNoPhotoScreenshot() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onLkOwnerFlatListingPage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onLkOwnerFlatListingPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Добавляем 3 фото, видим алерт что нужно 4")
    public void shouldAddThreePhoto() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();
        lkSteps.addPhotoCount(3);
        lkSteps.onLkOwnerFlatListingPage().button(SEND_BUTTON).click();
        lkSteps.onLkOwnerFlatListingPage().errorToast();
        lkSteps.onLkOwnerFlatListingPage().toast().should(hasText(containsString("Должно быть минимум 4 фотографии")));
    }

    @Test
    @DisplayName("Добавляем 4 фото - скриншотный тест")
    public void shouldAddFourPhotoScreenshot() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();
        lkSteps.addPhotoCount(4);
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onLkOwnerFlatListingPage().root());

        urlSteps.setProductionHost().open();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();
        lkSteps.addPhotoCount(4);
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onLkOwnerFlatListingPage().root());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Добавляем 4 фото и сохраняем")
    public void shouldAddFourPhoto() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();

        lkSteps.addPhotoCount(4);
        lkSteps.onLkOwnerFlatPhotoPage().button(SEND_BUTTON).click();
        lkSteps.onLkOwnerFlatPhotoPage().successToast();

        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).checkMark().waitUntil(isDisplayed());
    }

    @Test
    @DisplayName("Добавляем 30 фото и сохраняем")
    public void shouldAddThirtyPhoto() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();


        lkSteps.onLkOwnerFlatPhotoPage().photoInput().waitUntil(exists());
        lkSteps.addPhotoCount(30);
        lkSteps.onLkOwnerFlatPhotoPage().addPhotoButton().waitUntil(not(isDisplayed()));
        lkSteps.onLkOwnerFlatPhotoPage().button(SEND_BUTTON).click();
        lkSteps.onLkOwnerFlatPhotoPage().successToast();
    }

    @Test
    @DisplayName("Добавляем 4 фото не сохраняем и удаляем")
    public void shouldAddDeletePhoto() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();


        lkSteps.onLkOwnerFlatPhotoPage().photoInput().waitUntil(exists());
        lkSteps.addPhotoCount(4);
        lkSteps.onLkOwnerFlatPhotoPage().button(SEND_BUTTON).waitUntil(isDisplayed());
        lkSteps.onLkOwnerFlatPhotoPage().photosPreviews().forEach(p -> {
            lkSteps.moveCursor(p);
            p.deleteButton().click();
        });
        lkSteps.onLkOwnerFlatPhotoPage().photoInput().waitUntil(exists());

        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).waitUntil(isDisplayed());
    }

    @Test
    @DisplayName("Добавляем 4 фото сохраняем, потом удаляем")
    public void shouldAddSaveDeletePhoto() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().firstNavFlatsButton();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).click();

        lkSteps.onLkOwnerFlatPhotoPage().photoInput().waitUntil(exists());
        lkSteps.addPhotoCount(4);
        lkSteps.onLkOwnerFlatPhotoPage().button(SEND_BUTTON).click();
        lkSteps.onLkOwnerFlatPhotoPage().successToast();
        lkSteps.onLkOwnerFlatListingPage().flatPhotoLink().click();
        lkSteps.onLkOwnerFlatPhotoPage().photosPreviews().waitUntil(hasSize(greaterThan(0)));
        lkSteps.onLkOwnerFlatPhotoPage().deleteAllPhotos();
        lkSteps.onLkOwnerFlatPhotoPage().button(SAVE_BUTTON).click();

        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().toDoRow(ADD_PHOTO).link(ADD_LINK).waitUntil(isDisplayed());
    }
}

