package ru.auto.tests.commons.allure;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

/**
 * @author kurau (Yuri Kalinin)
 */
@Sources("classpath:allure.properties")
public interface AllureProperties extends Config {

    @Key("allure.results.directory")
    @DefaultValue("allure-results")
    String allureResultsPath();

    @Key("allure.report.remove.attachments")
    @DefaultValue("")
    String removeAttachmentsPattern();

    @Key("allure.report.exclude.attachments")
    @DefaultValue("(.*locators.*)|(.*Session id.*)")
    String excludeAttachmentsPattern();

    @Key("allure.step.logger.enabled")
    @DefaultValue("true")
    boolean allureStepLoggerEnabled();
}
