package ru.yandex.general.robot;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.ROBOT_ACCESSIBILITY_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;

@Epic(ROBOT_ACCESSIBILITY_FEATURE)
@DisplayName("Карточка товара доступна роботу, есть цена")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class RobotOfferCardAccessibilityTest {

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Карточка товара доступна роботу, есть цена")
    public void shouldSeeOfferCardByRobot() {
        jSoupSteps.testing().path(ELEKTRONIKA).get();
        String cardPath = jSoupSteps.select("a[href*='/offer/']").attr("href");
        jSoupSteps.testing().path(cardPath).setDesktopRobotUserAgent().get();
        String price = jSoupSteps.select("span[class*='_price']").text();

        assertThat("На карточке есть цена", price, notNullValue());
    }

}

