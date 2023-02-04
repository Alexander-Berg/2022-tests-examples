package ru.auto.tests.desktop.step.cabinet;

import io.qameta.allure.Step;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.calculator.Balance;
import ru.auto.tests.desktop.element.cabinet.calculator.Services;
import ru.auto.tests.desktop.step.BasePageSteps;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 22.05.18
 */
public class CalculatorPageSteps extends BasePageSteps {

    @Step("Сумма в блоке {curSum} должна увеличиться")
    public void shouldSeeAmountIncrease(int prevInCategorySum, VertisElement curSum) {
        assertThat(extractSum(curSum), greaterThan(prevInCategorySum));
    }

    @Step("Извлекаем сумму из блока {amount}")
    public int extractSum(VertisElement amount) {
        Matcher matcher = Pattern.compile("(\\d+\\s*)+").matcher(amount.getText());
        String sum = "0";
        if (matcher.find()) {
            sum = matcher.group();
        }

        return Integer.parseInt(sum.replaceAll("\\s", ""));
    }

    @Step("Добавляем услугу {service} в блоке {blockWithService}")
    public void addServiceInBlock(String service, Services blockWithService) {
        blockWithService.service(service).input().click();
        blockWithService.service(service).input().clear();
        blockWithService.service(service).input().sendKeys("1");
    }

    @Step("Разворачиваем блок {block} и блок Баланса")
    public void expandCalculatorAndBalanceBlocks(VertisElement block) {
        block.click();
        onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        if (!onNewCalculatorPage().balance().toggleIcon(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).getAttribute
                ("class").contains("rotated")) {
            onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        }

        onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        if (!onNewCalculatorPage().balance().toggleIcon(Balance.SERVICES_IN_CATEGORIES_SUMMARY).getAttribute
                ("class").contains("rotated")) {
            onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        }
    }

    @Step("Разворачиваем только сводку «Размещение в категория» на блоке Баланса")
    public void expandOnlyAccommodationInCategoriesSummaryBlock() {
        onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        if (onNewCalculatorPage().balance().toggleIcon(Balance.SERVICES_IN_CATEGORIES_SUMMARY).getAttribute
                ("class").contains("rotated")) {
            onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        }

        onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        if (!onNewCalculatorPage().balance().toggleIcon(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).getAttribute
                ("class").contains("rotated")) {
            onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        }
    }

    @Step("Разворачиваем только сводку «Услуги в категориях» на блоке Баланса")
    public void expandOnlyServicesInCategoriesSummaryBlock() {
        onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        if (onNewCalculatorPage().balance().toggleIcon(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).getAttribute
                ("class").contains("rotated")) {
            onNewCalculatorPage().balance().summary(Balance.ACCOMMODATION_IN_CATEGORIES_SUMMARY).click();
        }

        onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        if (!onNewCalculatorPage().balance().toggleIcon(Balance.SERVICES_IN_CATEGORIES_SUMMARY).getAttribute
                ("class").contains("rotated")) {
            onNewCalculatorPage().balance().summary(Balance.SERVICES_IN_CATEGORIES_SUMMARY).click();
        }
    }
}
