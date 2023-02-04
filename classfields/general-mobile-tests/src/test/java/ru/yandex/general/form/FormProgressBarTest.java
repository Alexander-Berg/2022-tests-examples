package ru.yandex.general.form;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.FormConstants.Categories.REZUME_IN_SELLING;
import static ru.yandex.general.consts.FormConstants.Categories.UMNIE_KOLONKI;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(ADD_FORM_FEATURE)
@Feature("Прогресс-бар")
@DisplayName("Прогресс-бар")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class FormProgressBarTest {

    private static final float PHOTO_STEP_PROGRESSBAR_WIDTH = 16.6667F;
    private static final float NAME_STEP_PROGRESSBAR_WIDTH = 25.0F;
    private static final float CATEGORY_STEP_PROGRESSBAR_WIDTH = 33.3333F;
    private static final float DESCRIPTION_STEP_PROGRESSBAR_WIDTH = 41.6667F;
    private static final float DESCRIPTION_REZUME_STEP_PROGRESSBAR_WIDTH = 50.0F;
    private static final float VIDEO_STEP_PROGRESSBAR_WIDTH = 50.0F;
    private static final float VIDEO_REZUME_STEP_PROGRESSBAR_WIDTH = 60.0F;
    private static final float CONDITION_STEP_PROGRESSBAR_WIDTH = 58.3333F;
    private static final float ATTRIBUTES_STEP_PROGRESSBAR_WIDTH = 66.6667F;
    private static final float ATTRIBUTES_FIRST_REZUME_STEP_PROGRESSBAR_WIDTH = 70.0F;
    private static final float ATTRIBUTES_SECOND_REZUME_STEP_PROGRESSBAR_WIDTH = 81.8182F;
    private static final float PRICE_STEP_PROGRESSBAR_WIDTH = 75.0F;
    private static final float SALLARY_REZUME_STEP_PROGRESSBAR_WIDTH = 80.0F;
    private static final float CONTACTS_STEP_PROGRESSBAR_WIDTH = 83.3333F;
    private static final float CONTACTS_REZUME_STEP_PROGRESSBAR_WIDTH = 90.0F;
    private static final float ADDRESS_STEP_PROGRESSBAR_WIDTH = 91.6667F;
    private static final float ADDRESS_REZUME_STEP_PROGRESSBAR_WIDTH = 100.0F;
    private static final float DELIVERY_STEP_PROGRESSBAR_WIDTH = 100.0F;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(FORM).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления фото")
    public void shouldSeePhotoProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToPhotoStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(PHOTO_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления фото, с добавленным фото")
    public void shouldSeePhotoProgressBarWidthWithPhoto() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToPhotoStep();
        offerAddSteps.addPhoto();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(PHOTO_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления названия")
    public void shouldSeeNameProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToNameStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(NAME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления категории")
    public void shouldSeeCategoryProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToCategoryStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(CATEGORY_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления описания")
    public void shouldSeeDescriptionProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToDescriptionStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(DESCRIPTION_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления описания, резюме")
    public void shouldSeeDescriptionProgressBarWidthRezume() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToDescriptionStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(DESCRIPTION_REZUME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления видео")
    public void shouldSeeVideoProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToVideoStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(VIDEO_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления видео, резюме")
    public void shouldSeeVideoProgressBarWidthRezume() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToVideoStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(VIDEO_REZUME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления состояния")
    public void shouldSeeConditionProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToConditionStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(CONDITION_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления атрибутов")
    public void shouldSeeAttributesProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAttributesStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(ATTRIBUTES_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления атрибутов, первый экран, резюме")
    public void shouldSeeAttributesProgressBarWidthRezumeFirstPage() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToAttributesStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(ATTRIBUTES_FIRST_REZUME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления цены")
    public void shouldSeePriceProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToPriceStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(PRICE_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления зарплаты, резюме")
    public void shouldSeeSallaryProgressBarWidthRezume() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToPriceStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(SALLARY_REZUME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления контактов")
    public void shouldSeeContactsProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToContactsStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(CONTACTS_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления контактов, резюме")
    public void shouldSeeContactsProgressBarWidthRezume() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToContactsStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(CONTACTS_REZUME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления адреса")
    public void shouldSeePlaceOfDealProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToAddressStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(ADDRESS_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления адреса, резюме")
    public void shouldSeePlaceOfDealProgressBarWidthRezume() {
        offerAddSteps.withCategory(REZUME_IN_SELLING).fillToAddressStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(ADDRESS_REZUME_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ширина прогресс-бара на шаге добавления доставки")
    public void shouldSeeDeliveryProgressBarWidth() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToDeliveryStep();
        offerAddSteps.wait500MS();

        assertThat("Ширина прогресс-бара соответствует", offerAddSteps.onFormPage().getProgressBarWidth(),
                is(DELIVERY_STEP_PROGRESSBAR_WIDTH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет прогресс-бара на последнем шаге")
    public void shouldSeeNoProgressBarFinalStep() {
        offerAddSteps.withCategory(UMNIE_KOLONKI).fillToFinalStep();
        offerAddSteps.wait500MS();

        offerAddSteps.onFormPage().progressBar().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет прогресс-бара на первом шаге выбора типа товара")
    public void shouldSeeNoProgressBarOnSection() {
        offerAddSteps.resetIfDraft();
        offerAddSteps.onFormPage().progressBar().should(not(isDisplayed()));
    }

}
