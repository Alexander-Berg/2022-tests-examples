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
import ru.yandex.general.beans.ajaxRequests.RotateImage;
import ru.yandex.general.mobile.step.OfferAddSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.beans.ajaxRequests.RotateImage.rotateImage;
import static ru.yandex.general.consts.GeneralFeatures.ADD_FORM_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.step.AjaxProxySteps.ROTATE_IMAGE;

@Epic(ADD_FORM_FEATURE)
@Feature("Экран «Фотографии»")
@DisplayName("Проверка запроса при повороте фото")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FormPhotoStepRotateTest {

    private static final String[] JSONPATHS_TO_IGNORE = {"groupId", "queryId", "url", "name"};

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(FORM).open();
        offerAddSteps.fillSection();
        offerAddSteps.addPhoto();
        offerAddSteps.onFormPage().photoList().waitUntil(hasSize(1));
        offerAddSteps.waitSomething(2, TimeUnit.SECONDS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется запрос «rotateImage» с angle = «-90» при повороте фото")
    public void shouldSeeRotatePhotoRequest() {
        offerAddSteps.onFormPage().photoList().get(0).rotate().click();

        ajaxProxySteps.setAjaxHandler(ROTATE_IMAGE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getRotateImage().setAngle(-90)).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправляется запрос «rotateImage» с angle = «-180» при двойном повороте фото")
    public void shouldSeeDoubleRotatePhotoRequest() {
        offerAddSteps.onFormPage().photoList().get(0).rotate().click();
        offerAddSteps.wait500MS();
        offerAddSteps.onFormPage().photoList().get(0).rotate().click();

        ajaxProxySteps.setAjaxHandler(ROTATE_IMAGE).withPathsToBeIgnored(JSONPATHS_TO_IGNORE)
                .withRequestText(getRotateImage().setAngle(-180)).shouldExist();
    }

    private RotateImage getRotateImage() {
        return rotateImage().setNamespace("o-yandex").setRatio(1);
    }

}
