package ru.yandex.realty.step;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.realty.page.BasePage;
import ru.yandex.realty.page.NewBuildingSitePage;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.element.newbuildingsite.SiteReview.DELETE;

public class NewBuildingSteps extends CommonSteps {

    public NewBuildingSitePage onNewBuildingSitePage() {
        return on(NewBuildingSitePage.class);
    }

    public BasePage onBasePage() {
        return on(BasePage.class);
    }

    @Step("Удаляем отзывы если есть")
    public void deleteReview() {
        refresh();
        onNewBuildingSitePage().reviewBlock().siteReviewList().filter(s -> exists().matches(s.spanLink(DELETE)))
                .forEach(s -> s.spanLink(DELETE).click());
    }

    @Override
    public WebDriver getDriver() {
        return super.getDriver();
    }
}
