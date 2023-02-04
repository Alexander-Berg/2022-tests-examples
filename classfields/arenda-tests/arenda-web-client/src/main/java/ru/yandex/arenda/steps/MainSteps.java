package ru.yandex.arenda.steps;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.arenda.config.ArendaWebConfig;
import ru.yandex.arenda.pages.AdminAssignedUserPage;
import ru.yandex.arenda.pages.AdminCallCenterPage;
import ru.yandex.arenda.pages.AdminFlatPage;
import ru.yandex.arenda.pages.AdminListingPage;
import ru.yandex.arenda.pages.AdminUserPage;
import ru.yandex.arenda.pages.BasePage;
import ru.yandex.arenda.pages.CalculatorCostPage;
import ru.yandex.arenda.pages.CalculatorCostTouchPage;
import ru.yandex.arenda.pages.ContractPage;
import ru.yandex.arenda.pages.LkFeedBackPage;
import ru.yandex.arenda.pages.LkOwnerFlatListingPage;
import ru.yandex.arenda.pages.LkOwnerFlatPhotoPage;
import ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage;
import ru.yandex.arenda.pages.LkOwnerHouseServiceMetersPage;
import ru.yandex.arenda.pages.LkPage;
import ru.yandex.arenda.pages.LkPaymentDataPage;
import ru.yandex.arenda.pages.LkPaymentMethodsPage;
import ru.yandex.arenda.pages.LkTenantFlatListingPage;
import ru.yandex.arenda.pages.MainLandingPage;
import ru.yandex.arenda.pages.NpsPage;
import ru.yandex.arenda.pages.OutstaffFlatCopywriterPage;
import ru.yandex.arenda.pages.OutstaffFlatPhotographerPage;
import ru.yandex.arenda.pages.OutstaffFlatRetoucherPage;
import ru.yandex.arenda.pages.OutstaffPage;
import ru.yandex.arenda.pages.RealtyPage;
import ru.yandex.arenda.pages.TinkoffPayPage;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class MainSteps extends WebDriverSteps {

    @Inject
    protected ArendaWebConfig config;

    @Inject
    private WebDriverManager driverManager;

    public static final int FIRST = 0;
    public static final String RGID = "rgid";
    public static final String MOSCOW_RGID = "587795";
    public static final String SPB_RGID = "417899";
    public static final String COOKIE_DOMAIN = ".yandex.";

    public LkPage onLkPage() {
        return on(LkPage.class);
    }

    public AdminListingPage onAdminListingPage() {
        return on(AdminListingPage.class);
    }

    public OutstaffPage onOutstaffPage() {
        return on(OutstaffPage.class);
    }

    public OutstaffFlatPhotographerPage onOutstaffFlatPhotographerPage() {
        return on(OutstaffFlatPhotographerPage.class);
    }

    public OutstaffFlatCopywriterPage onOutstaffFlatCopywriterPage() {
        return on(OutstaffFlatCopywriterPage.class);
    }

    public OutstaffFlatRetoucherPage onOutstaffFlatRetoucherPage() {
        return on(OutstaffFlatRetoucherPage.class);
    }

    public AdminUserPage onAdminUserPage() {
        return on(AdminUserPage.class);
    }

    public ContractPage onContractPage() {
        return on(ContractPage.class);
    }

    public AdminAssignedUserPage onAdminAssignedUserPage() {
        return on(AdminAssignedUserPage.class);
    }

    public AdminCallCenterPage onAdminCallCenterPage() {
        return on(AdminCallCenterPage.class);
    }

    public LkPaymentDataPage onLkPaymentDataPage() {
        return on(LkPaymentDataPage.class);
    }

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    public WebDriver getDriver() {
        return super.getDriver();
    }

    public LkOwnerFlatListingPage onLkOwnerFlatListingPage() {
        return on(LkOwnerFlatListingPage.class);
    }

    public LkOwnerFlatPhotoPage onLkOwnerFlatPhotoPage() {
        return on(LkOwnerFlatPhotoPage.class);
    }

    public LkTenantFlatListingPage onLkTenantFlatListingPage() {
        return on(LkTenantFlatListingPage.class);
    }

    public LkFeedBackPage onLkFeedBackPage() {
        return on(LkFeedBackPage.class);
    }

    public LkPaymentMethodsPage onLkPaymentMethodsPage() {
        return on(LkPaymentMethodsPage.class);
    }

    public TinkoffPayPage onTinkoffPayPage() {
        return on(TinkoffPayPage.class);
    }

    public MainLandingPage onMainLandingPage() {
        return on(MainLandingPage.class);
    }

    public RealtyPage onRealtyPage() {
        return on(RealtyPage.class);
    }

    public CalculatorCostPage onCalculatorCostPage() {
        return on(CalculatorCostPage.class);
    }

    public CalculatorCostTouchPage onCalculatorCostTouchPage() {
        return on(CalculatorCostTouchPage.class);
    }

    public LkOwnerHouseServiceAdjustPage onLkOwnerHouseServiceAdjustPage() {
        return on(LkOwnerHouseServiceAdjustPage.class);
    }

    public LkOwnerHouseServiceMetersPage onLkOwnerHouseServiceMetersPage() {
        return on(LkOwnerHouseServiceMetersPage.class);
    }

    public AdminFlatPage onAdminFlatPage() {
        return on(AdminFlatPage.class);
    }

    public NpsPage onNpsPage() {
        return on(NpsPage.class);
    }

    @Step("Обновляем {timeout} секунд пока нужный объект не будет {matcher}")
    public <T> void refreshUntil(Supplier<T> object, Matcher<T> matcher, int timeout) {
        T obj = object.get();
        if (!matcher.matches(obj)) {
            given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                    .pollInterval(1, SECONDS).atMost(timeout, SECONDS).ignoreExceptions().pollInSameThread()
                    .until(() -> {
                        refresh();
                        T obj1 = object.get();
                        return matcher.matches(obj1);
                    });
        }
    }

    @Step("Скролл к элементу {element} в центр экрана")
    public void scrollElementToCenter(AtlasWebElement element) {
        element.waitUntil(isDisplayed());
        int i = element.getLocation().getY();
        int cur = getCurrentYPosition();
        int screen = getDriver().manage().window().getSize().getHeight();
        scroll(i - cur - screen / 2);
    }

    public void clearInputByBackSpace(Supplier<? extends AtlasWebElement> element) {
        clearInputByBackSpace(element, equalTo(""), () -> element.get().getAttribute("value"));
    }

    public void clearInputByBackSpace(Supplier<? extends AtlasWebElement> element, Matcher matcher,
                                      Supplier<? extends Object> object) {
        await().conditionEvaluationListener(new AllureConditionEvaluationLogger()).ignoreExceptions().pollInSameThread()
                .pollInterval(500, MILLISECONDS).atMost(15, TimeUnit.SECONDS)
                .until(() -> {
                    element.get().sendKeys(Keys.BACK_SPACE);
                    return matcher.matches(object.get());
                });
        element.get().sendKeys(Keys.BACK_SPACE);
    }

    @Step("Ждем пока количество табов будет {count} и переключаемся на вкладку")
    public void waitUntilSeeTabsCountAndSwitch(int count) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(1000, MILLISECONDS).atMost(20000, MILLISECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> equalTo(count).matches(getDriver().getWindowHandles().size()));
        switchToNextTab();
    }

    public void setSpbCookie() {
        setCookie(RGID, SPB_RGID, COOKIE_DOMAIN + "ru");
    }

    public void setMoscowCookie() {
        setCookie(RGID, MOSCOW_RGID, COOKIE_DOMAIN + "ru");
    }

    public void performInNewSession(Runnable code) {
        String proxy = "proxy-ext.test.vertis.yandex.net:3128";
        WebDriver oldDriver = driverManager.getDriver();
        driverManager.updateChromeOptions(chromeOptions -> chromeOptions.addArguments("--proxy-server=http://" + proxy));
        try {
            driverManager.startDriver();
            code.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            driverManager.stopDriver();
            driverManager.setDriver(oldDriver);
        }
    }

}
