package ru.yandex.navi.tf;

import io.qameta.allure.Allure;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import org.openqa.selenium.OutputType;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static io.qameta.allure.AllureConstants.ATTACHMENT_FILE_SUFFIX;

public class AllureStepListener implements StepLifecycleListener {
    @Override
    public void afterStepStop(StepResult result) {
        final String source = UUID.randomUUID().toString() + ATTACHMENT_FILE_SUFFIX + ".png";
        result.withAttachments(
            new Attachment().withName("screenshot").withType("image/png").withSource(source));
        Allure.getLifecycle().writeAttachment(source,
            new ByteArrayInputStream(
                MobileUser.getUser().getDriver().getScreenshotAs(OutputType.BYTES)));
    }
}
