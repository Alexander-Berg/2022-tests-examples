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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FORMS;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы - удаление заполненных полей")
@Feature(FORMS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CarsDeletionFieldsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FormsSteps formsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        formsSteps.createReviewsCarsForm();
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).open();
        formsSteps.fillForm("Ваш отзыв");
        formsSteps.onFormsPage().input("Текст отзыва", "Это надо удалить");
        formsSteps.addReviewPhoto();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Удаление заполненных полей")
    public void shouldSeeEmptyReview() {
        formsSteps.onFormsPage().clearInput("Заголовок");
        formsSteps.onFormsPage().input("Текст отзыва").hover();
        formsSteps.onFormsPage().removeTextButton().click();
        formsSteps.onFormsPage().removePhotoButton().click();
        formsSteps.onFormsPage().input("Заголовок").should(hasText(""));
        formsSteps.onFormsPage().input("Текст отзыва").should(hasText(""));
        formsSteps.onFormsPage().reviewPhoto().should(not(isDisplayed()));
    }
}