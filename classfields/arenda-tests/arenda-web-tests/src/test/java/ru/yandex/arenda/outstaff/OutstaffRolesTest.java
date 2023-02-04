package ru.yandex.arenda.outstaff;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
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

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.COPYWRITER;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.OUTSTAFF;
import static ru.yandex.arenda.constants.UriPath.PHOTOGRAPHER;
import static ru.yandex.arenda.constants.UriPath.RETOUCHER;
import static ru.yandex.arenda.element.lk.outstaff.OutstaffFlatFilters.SEARCH_BUTTON;
import static ru.yandex.arenda.matcher.AttributeMatcher.hasHref;
import static ru.yandex.arenda.matcher.AttributeMatcher.isDisabled;
import static ru.yandex.arenda.pages.OutstaffFlatCopywriterPage.FLAT_PHOTO_URL;
import static ru.yandex.arenda.pages.OutstaffFlatCopywriterPage.TOUR_3D_URL;
import static ru.yandex.arenda.pages.OutstaffFlatPhotographerPage.SAVE_BUTTON;
import static ru.yandex.arenda.pages.OutstaffFlatRetoucherPage.INITIAL_PHOTOS;
import static ru.yandex.arenda.steps.LkSteps.getNumberedImagePath;
import static ru.yandex.arenda.steps.MainSteps.FIRST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1834")
@DisplayName("[ARENDA] Формы копирайтера/ретушера/фотографа")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class OutstaffRolesTest {

    private static final String TEST_STREET = "Большеохтинский";
    private static final String TEST_URL = "https://arenda.test.vertis.yandex.ru/%s";
    private static final String HAS_NO_LINK = "Ссылка не указана";
    private static final String BLOCKED_TEXT = "Интерфейс заблокирован";
    private static final String ERROR_TOAST_TEXT = "Должно быть минимум 4 фотографии";

    private String createdFlatId;

    String photoUrl;
    String tour3dUrl;

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

    @Before
    public void before() {
        passportSteps.outstaffLogin();
    }

    @Test
    @DisplayName("В поле с поиском вводим улицу -> url поменялся.")
    public void shouldSeeOutstaffStreetFilterUrl() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(PHOTOGRAPHER).path(FLATS).open();
        lkSteps.onOutstaffPage().outstaffFlatFilters().addressFilter().sendKeys(TEST_STREET);
        lkSteps.onOutstaffPage().outstaffFlatFilters().button(SEARCH_BUTTON).click();
        urlSteps.queryParam("query", TEST_STREET).ignoreParam("page").ignoreParam("preset")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("В поле с поиском вводим улицу -> видим квартиры только на этой улице")
    public void shouldSeeOutstaffStreetFilter() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(PHOTOGRAPHER).path(FLATS).open();
        lkSteps.onOutstaffPage().outstaffFlatFilters().addressFilter().sendKeys(TEST_STREET);
        lkSteps.onOutstaffPage().outstaffFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onOutstaffPage().outstaffFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        lkSteps.onOutstaffPage().outstaffFlatsItem().forEach(flat ->
                flat.link().should(hasText(containsString(TEST_STREET))));
    }

    @Test
    @DisplayName("Фотограф. Изменить ссылки и сохранить. Ссылки поменялись в интерфейсе анкеты и копирайтера")
    public void shouldSeeChangesOnCopywriter() {
        createFlatForOutstaff();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(COPYWRITER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatCopywriterPage().field(FLAT_PHOTO_URL).should(hasText(containsString(HAS_NO_LINK)));
        lkSteps.onOutstaffFlatCopywriterPage().field(TOUR_3D_URL).should(hasText(containsString(HAS_NO_LINK)));

        fillUrlsOnPhotographerPage();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(COPYWRITER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatCopywriterPage().field(TOUR_3D_URL).link().should(hasHref(equalTo(tour3dUrl)));
    }

    @Test
    @DisplayName("Ретушер. Фотограф еще не загружал фотки -> интерфейс недоступен.")
    public void shouldSeeChangesOnRetoucher() {
        createFlatForOutstaff();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(RETOUCHER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatRetoucherPage().root().should(hasText(containsString(BLOCKED_TEXT)));
        lkSteps.onOutstaffFlatRetoucherPage().inputPhoto().should(not(exists()));
    }

    @Test
    @DisplayName("Ретушер. Если фотограф загрузил фотки. " +
            "Отображается на чтение ссылка на фотки от фотографа и есть возможность загрузить новые фото")
    public void shouldSeeUpload() {
        createFlatForOutstaff();
        fillUrlsOnPhotographerPage();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(RETOUCHER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatRetoucherPage().field(INITIAL_PHOTOS).link().should(hasHref(equalTo(photoUrl)));
    }

    @Test
    @DisplayName("Ретушер. Если фотограф загрузил фотки. 3 недостаточно.")
    public void shouldSeeUploadThreePhotos() {
        createFlatForOutstaff();
        fillUrlsOnPhotographerPage();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(RETOUCHER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatRetoucherPage().field(INITIAL_PHOTOS).link().should(hasHref(equalTo(photoUrl)));
        addPhotoCount(3);
        lkSteps.onOutstaffFlatRetoucherPage().button(SAVE_BUTTON).waitUntil(not(isDisabled())).click();
        lkSteps.onOutstaffFlatPhotographerPage().errorToast();
        lkSteps.onOutstaffFlatPhotographerPage().toast().should(hasText(containsString(ERROR_TOAST_TEXT)));
    }

    @Test
    @DisplayName("Ретушер. Если фотограф загрузил фотки. 4 достаточно.")
    public void shouldSeeUploadFourPhotos() {
        createFlatForOutstaff();
        fillUrlsOnPhotographerPage();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(RETOUCHER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatRetoucherPage().field(INITIAL_PHOTOS).link().should(hasHref(equalTo(photoUrl)));
        addPhotoCount(4);
        lkSteps.onOutstaffFlatRetoucherPage().button(SAVE_BUTTON).waitUntil(not(isDisabled())).click();
        lkSteps.onOutstaffFlatPhotographerPage().successToast();
    }

    @Test
    @DisplayName("Копирайтер. Видим фото на странице добавленные у ретушера")
    public void shouldSeeUploadFourOnCopywriter() {
        createFlatForOutstaff();
        fillUrlsOnPhotographerPage();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(RETOUCHER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatRetoucherPage().field(INITIAL_PHOTOS).link().should(hasHref(equalTo(photoUrl)));
        addPhotoCount(4);
        lkSteps.onOutstaffFlatRetoucherPage().button(SAVE_BUTTON).waitUntil(not(isDisabled())).click();
        lkSteps.onOutstaffFlatPhotographerPage().successToast();

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(COPYWRITER).path(FLAT).path(createdFlatId).open();
        lkSteps.onOutstaffFlatCopywriterPage().previews().should(hasSize(4));
    }

    @Ignore("Комиссия пока не зашита")
    @Test
    @DisplayName("Копирайтер. Нет анкеты, заполнить текст, добавить 4 фотографии и сохранить. Отображается модалка")
    public void shouldSeeFillCopywriter() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);

        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(COPYWRITER).path(FLAT).path(createdFlatId).open();
        addPhotoCount(4);
        lkSteps.onOutstaffFlatCopywriterPage().textarea().sendKeys(getRandomString());
        lkSteps.onOutstaffFlatCopywriterPage().button(SAVE_BUTTON).click();
        lkSteps.onOutstaffFlatCopywriterPage().modal()
                .should(hasText(containsString("В анкете не заполнены обязательные поля")));
    }

    private void fillUrlsOnPhotographerPage() {
        urlSteps.testing().path(MANAGEMENT).path(OUTSTAFF).path(PHOTOGRAPHER).path(FLAT).path(createdFlatId).open();
        photoUrl = format(TEST_URL, getRandomString());
        tour3dUrl = format(TEST_URL, getRandomString());
        lkSteps.onOutstaffFlatPhotographerPage().photoUrl().sendKeys(photoUrl);
        lkSteps.onOutstaffFlatPhotographerPage().tour3dUrl().sendKeys(tour3dUrl);
        lkSteps.onOutstaffFlatPhotographerPage().button(SAVE_BUTTON).click();
        lkSteps.onOutstaffFlatPhotographerPage().successToast();
    }

    private void createFlatForOutstaff() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        retrofitApiSteps.postModerationFlatsQuestionnaire(createdFlatId);
    }

    @Step("Добавляем {number}")
    public void addPhotoCount(int number) {
        lkSteps.setFileDetector();
        for (int i = 0; i < number; i++) {
            // TODO: 31.01.2021 шаг нужен потому что в инпут почему-то загружается еще и предыдущий файл
            lkSteps.onOutstaffFlatRetoucherPage().inputPhoto().executeScript("arguments[0].value = '';");
            lkSteps.onOutstaffFlatRetoucherPage().inputPhoto().sendKeys(getNumberedImagePath((i % 4) + 1));
            lkSteps.onOutstaffFlatRetoucherPage().previews().waitUntil(Matchers.hasSize(i + 1));
        }
    }
}
