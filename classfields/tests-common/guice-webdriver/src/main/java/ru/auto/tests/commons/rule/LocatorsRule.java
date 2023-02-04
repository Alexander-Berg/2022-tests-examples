package ru.auto.tests.commons.rule;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import io.qameta.allure.Attachment;
import io.qameta.allure.junit4.DisplayName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.auto.tests.commons.bean.LocatorBean;
import ru.auto.tests.commons.extension.context.StepContext;
import ru.auto.tests.commons.webdriver.WebDriverSteps;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.auto.tests.commons.extension.listener.StepTitleContainer.getStepTitle;
import static ru.auto.tests.commons.extension.listener.StepTitleContainer.stepArgs;

public class LocatorsRule extends TestWatcher {

    @Inject
    private WebDriverSteps webDriverSteps;

    @Override
    protected void finished(Description description) {
        locStat2(webDriverSteps.getLocatorStorage().getStepsList(), description);
    }

    @Attachment(value = "locators2", fileExtension = ".locators2")
    private String locStat2(List<StepContext> test, Description description) {



        List<LocatorBean> locators = new LinkedList<>();
        test.forEach(context -> processLocator(context, locators, description));

        return new GsonBuilder().setPrettyPrinting().create().toJson(locators);
    }

    private void processLocator(StepContext elementLocator, List<LocatorBean> allLocators, Description description) {

        LocatorBean node = new LocatorBean();

        if (allLocators.size()>0) {
            LocatorBean element = allLocators.get(allLocators.size() - 1);

            boolean isSameLocator = element.getFullPath().equals(elementLocator.locator());

            String prevAction = elementLocator.getAction();
            boolean isSameAction = element.getAction().equals(prevAction);

            if (isSameLocator && isSameAction) {
                return;
            }
        }

        node.setFullPath(elementLocator.locator());
        node.setPage(elementLocator.getUrl());
        node.setAction(elementLocator.getAction() + stepArgs(elementLocator.getAction(), elementLocator.getArgs()));
        if (!Optional.ofNullable(elementLocator.getDescription()).isPresent()) {
            getStepTitle(elementLocator.getAction()).ifPresent(title ->
                    node.setStepDescription(title.format(
                            elementLocator.name(),
                            elementLocator.getArgs())));
        } else {
            node.setStepDescription(elementLocator.getDescription());
        }

        node.setTest(testName(description));

        allLocators.add(node);
    }

    private String testName(Description description) {
        String param = "";
        String regex = ".*\\[(.*)\\].*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(description.getMethodName());
        if (matcher.find()) {
            param = String.format("[%s]", matcher.group(1));
        }

        if (description.getAnnotation(DisplayName.class) != null) {
            return Optional.of(description.getAnnotation(DisplayName.class).value() + param)
                    .orElse(description.getMethodName());
        }

        return description.getMethodName();
    }

}
