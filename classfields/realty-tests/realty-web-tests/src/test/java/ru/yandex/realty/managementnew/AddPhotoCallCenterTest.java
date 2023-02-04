package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.element.management.Preview.DELETE_ICON;
import static ru.yandex.realty.element.management.Preview.TURN_ICON;
import static ru.yandex.realty.matchers.OfferInfoMatchers.hasPhoto;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@Link("https://st.yandex-team.ru/VERTISTEST-1490")
@DisplayName("Редактирование оффера со страницы колл центра")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AddPhotoCallCenterTest {

    private static final String MDS_PATH_TO_PHOTO = "https://avatars.mdst.yandex.net/get-realty";
    private static final String SAVE_CHANGES = "Сохранить изменения";
    private static final String CANCEL_BUTTON = "Отменить";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Before
    public void openManagementPage() {
        apiSteps.createVos2Account(account, OWNER);
        offerBuildingSteps.addNewOffer(account).withBody(getDefaultOffer(APARTMENT_SELL).withCallCenter(true)).create()
                .getId();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).path("cc").path("007").queryParam("cc_current_user", "1").open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрывется галерея после сохранения")
    public void shouldNotSeeGalleryAfterSave() {
        managementSteps.addPhoto();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES).click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().should("Не должно быть галереи",
                not(isDisplayed()), 10);
    }

    @Ignore("не работает перетаскивание")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Устанавливаем превью оффера")
    public void shouldSetOfferPreview() {
        managementSteps.addPhoto();
        basePageSteps.moveToElement(managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery()
                .preview(DEFAULT_PHOTO_COUNT_FOR_DWELLING),
                managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery()
                .preview(FIRST));
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(FIRST).click();
        String photoUrl = apiSteps.getOfferInfo(account).getPhoto().get(1).getUrl();

        managementSteps.moveCursor(managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(1));
        managementSteps.moveElementTo(
                () -> managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(1),
                () -> managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(FIRST));
        String previewPhotoLink = managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery()
                .preview(FIRST).previewImg().getAttribute("style");
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES)
                .clickWhile(not(isDisplayed()));
        apiSteps.waitFirstOffer(account, hasPhoto(hasSize(DEFAULT_PHOTO_COUNT_FOR_DWELLING + 1)));

        assertThat("Сравниваем URL в preview и в базе",
                previewPhotoLink, containsString(photoUrl.replace("orig", "main")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Не меняется превью оффера после добавления еще одного фото")
    public void shouldNotChangeOfferPreviewAfterAddSecondPhoto() {
        managementSteps.addPhoto();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES).click();
        apiSteps.waitFirstOffer(account, hasPhoto(hasSize(DEFAULT_PHOTO_COUNT_FOR_DWELLING + 1)));
        String previewPhotoLink = managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo()
                .offerPreview().getAttribute("style");
        managementSteps.addPhoto();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES).click();
        apiSteps.waitFirstOffer(account, hasPhoto(hasSize(DEFAULT_PHOTO_COUNT_FOR_DWELLING + 2)));

        assertThat("Сравниваем URL в preview и в базе",
                previewPhotoLink, containsString(managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo()
                        .offerPreview().getAttribute("style")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняется превью если удалить первое фото")
    public void shouldChangeOfferPreviewAfterDeleteFirstPhoto() {
        managementSteps.addPhoto();
        String secondPreviewPhotoLink = managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery()
                .previews().get(1).previewImg().getAttribute("style");

        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery()
                .previews().get(FIRST).icon(DELETE_ICON).click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES).click();
        apiSteps.waitFirstOffer(account, hasPhoto(hasSize(DEFAULT_PHOTO_COUNT_FOR_DWELLING)));

        assertThat("Сравниваем URL в preview и в базе",
                secondPreviewPhotoLink, containsString(managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo()
                        .offerPreview().getAttribute("style")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Удаляем все фото.")
    public void shouldDeletePhoto() {
        managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo().addPhotoButton().click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().previews()
                .forEach(preview -> {
                    basePageSteps.moveCursor(preview);
                    preview.icon(DELETE_ICON).click();
                });
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().previews().waitUntil(hasSize(0));
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES).click();
        managementSteps.refresh();
        managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo().addPhotoButton()
                .should(hasClass(containsString("photo_empty")));

        apiSteps.waitFirstOffer(account, hasPhoto(hasSize(0)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поворачиваем картинку и сохраняем")
    public void shouldTurnAndSave() {
        String defaultPhoto = apiSteps.getOfferInfo(account).getPhoto().get(0).getUrl();

        managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo().offerPreview().click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(FIRST)
                .icon(TURN_ICON).click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(FIRST)
                .previewImg().waitUntil(isDisplayed());
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(SAVE_CHANGES).click();

        apiSteps.waitFirstOffer(account, hasPhoto(hasItem(containsString(MDS_PATH_TO_PHOTO))));
        apiSteps.waitFirstOffer(account, hasPhoto(hasItem(not(defaultPhoto))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поворачиваем картинку и НЕ сохраняем")
    public void shouldTurnAndCancel() {
        String defaultPhoto = apiSteps.getOfferInfo(account).getPhoto().get(0).getUrl();

        managementSteps.onManagementNewPage().ccOffer(FIRST).offerInfo().offerPreview().click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(FIRST)
                .icon(TURN_ICON).click();
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().preview(FIRST)
                .previewImg().waitUntil(isDisplayed());
        managementSteps.onManagementNewPage().ccOffer(FIRST).photoGallery().button(CANCEL_BUTTON).click();

        apiSteps.waitFirstOffer(account, hasPhoto(hasItem(containsString(MDS_PATH_TO_PHOTO))));
        apiSteps.waitFirstOffer(account, hasPhoto(hasItem(defaultPhoto)));
    }
}
