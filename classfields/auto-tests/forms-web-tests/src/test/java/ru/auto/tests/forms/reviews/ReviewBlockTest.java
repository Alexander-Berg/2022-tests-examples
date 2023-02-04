package ru.auto.tests.forms.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.forms.FormsSteps;

import javax.inject.Inject;
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - блок отзыва")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewBlockTest {

    private static final String VIDEO_LINK = "https://www.youtube.com/watch?v=WXuMoKwOTgg";
    private static final String iframeUrl = "https://www.youtube.com/embed/WXuMoKwOTgg";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        formsSteps.createReviewsMotorcyclesForm();
        urlSteps.testing().path(MOTO).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm(formsSteps.getReviewTitle().getBlock());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Подсказка на кнопке «Текст»")
    public void shouldHoverTextButton() {
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        formsSteps.onFormsPage().textButton().hover();
        formsSteps.onFormsPage().tooltipText().should(isDisplayed()).should(hasText("Текст"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Подсказка на кнопке «Подзаголовок»")
    public void shouldHoverParagraphButton() {
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        formsSteps.onFormsPage().paragraphButton().hover();
        formsSteps.onFormsPage().tooltipText().should(isDisplayed()).should(hasText("Превратить в подзаголовок"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Подсказка на кнопке добавления фото")
    public void shouldHoverAddPhotoButton() {
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        formsSteps.onFormsPage().addPhotoButton().hover();
        formsSteps.onFormsPage().tooltipText().should(isDisplayed()).should(hasText("Вставить изображение"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Подсказка на кнопке добавления видео")
    public void shouldHoverAddVideoButton() {
        formsSteps.onFormsPage().saveStatus().waitUntil(hasText("Сохранён"));
        formsSteps.onFormsPage().addVideoButton().hover();
        formsSteps.onFormsPage().tooltipText().should(isDisplayed()).should(hasText("Вставить видео"));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Добавление видео")
    public void shouldAddVideo() {
        formsSteps.onFormsPage().addVideoButton().click();
        formsSteps.onFormsPage().input("Ссылка на YouTube", VIDEO_LINK);
        formsSteps.onFormsPage().button("Добавить").click();
        formsSteps.onFormsPage().addedVideo().waitUntil(isDisplayed());
        formsSteps.onFormsPage().videoFrame().should(isDisplayed()).should(hasAttribute("src", iframeUrl));

    }
}