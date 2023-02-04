package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.InsuranceForm.NAME_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.PHONE_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.SERIAL_AND_NUMBER_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.VALID_FROM_FIELD;
import static ru.auto.tests.desktop.consts.InsuranceForm.VALID_TO_FIELD;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление страховки «ОСАГО» с фото страховки")
@Story("Страхование")
@Feature(AutoruFeatures.GARAGE)
@RunWith(Parameterized.class)
@GuiceModules(MobileDevToolsTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GarageAddInsurancePhotoOsagoTest {

    private static final String CARD_ID = "/1146321503/";
    private final static String CARD_ADD_INSURANCE_WITH_IMAGE = "request/GarageUserCardAddInsuranceOsagoWithImage.json";
    private final static String CARD_ADD_INSURANCE_WITH_FILE = "request/GarageUserCardAddInsuranceOsagoWithFile.json";

    private final static String JPG_FILE = "src/test/resources/files/insuranceJPG.jpg";
    private final static String JPEG_FILE = "src/test/resources/files/insuranceJPEG.jpeg";
    private final static String PNG_FILE = "src/test/resources/files/insurancePNG.png";
    private final static String GIF_FILE = "src/test/resources/files/insuranceGIF.gif";
    private final static String PDF_FILE = "src/test/resources/files/insurancePDF.pdf";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String filePath;

    @Parameterized.Parameter(2)
    public String request;

    @Parameterized.Parameters(name = "name = {index}: добавляем фото в формате {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {"JPG", JPG_FILE, CARD_ADD_INSURANCE_WITH_IMAGE},
                {"JPEG", JPEG_FILE, CARD_ADD_INSURANCE_WITH_IMAGE},
                {"PNG", PNG_FILE, CARD_ADD_INSURANCE_WITH_IMAGE},
                {"GIF", GIF_FILE, CARD_ADD_INSURANCE_WITH_IMAGE},
                {"PDF", PDF_FILE, CARD_ADD_INSURANCE_WITH_FILE},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/GarageUserCardVin",
                "desktop/GarageUserMediaUploadUrlProxy").post();

        urlSteps.testing().path(GARAGE).path(CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос добавления страховки «ОСАГО» с фото страховки")
    public void shouldAddInsuranceWithPhoto() {
        mockRule.with("desktop/GarageUserCardAddInsuranceOsago").update();

        basePageSteps.onGarageCardPage().button("Добавить полис").waitUntil(isDisplayed()).click();
        basePageSteps.onGarageCardPage().popup().waitUntil(isDisplayed());
        basePageSteps.onGarageCardPage().popup().item("ОСАГО").click();
        basePageSteps.addFileInPopup(filePath);
        basePageSteps.onGarageCardPage().popup().spinner().waitUntil(not(isDisplayed()));
        basePageSteps.onGarageCardPage().popup().input(NAME_FIELD, "Рога и копыта");
        basePageSteps.onGarageCardPage().popup().input(SERIAL_AND_NUMBER_FIELD, "XXX111");
        basePageSteps.onGarageCardPage().popup().input(PHONE_FIELD, "+71112221111");
        basePageSteps.onGarageCardPage().popup().input(VALID_FROM_FIELD, "01.01.2020");
        basePageSteps.onGarageCardPage().popup().input(VALID_TO_FIELD, "31.12.2020");
        basePageSteps.onGarageCardPage().popup().button("Сохранить").click();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/mobile/garageUpdateCard/",
                hasJsonBody(request)
        ));
    }

}
