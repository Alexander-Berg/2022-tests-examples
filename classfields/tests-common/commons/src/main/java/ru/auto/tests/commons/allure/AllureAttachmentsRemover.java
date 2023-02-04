package ru.auto.tests.commons.allure;

import io.qameta.allure.AllureResultsWriteException;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.aeonbits.owner.ConfigFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author kurau (Yuri Kalinin)
 * https://github.com/allure-framework/allure2/issues/569
 */
public class AllureAttachmentsRemover implements TestLifecycleListener {

    private static final AllureProperties allure = ConfigFactory
            .create(AllureProperties.class, System.getProperties(), System.getenv());

    @Override
    public void beforeTestWrite(TestResult testResult) {
        if (testResult.getStatus().equals(Status.PASSED)) {
            removeMatchingAttachments(testResult.getAttachments());
            testResult.getSteps().stream()
                    .flatMap(this::getSubsteps)
                    .map(StepResult::getAttachments)
                    .forEach(this::removeMatchingAttachments);
        }
    }

    private Stream<StepResult> getSubsteps(StepResult step) {
        if (step.getSteps().isEmpty()) {
            return Stream.of(step);
        }
        return step.getSteps().stream().flatMap(this::getSubsteps);
    }

    private void removeMatchingAttachments(Collection<Attachment> attachments) {
        List<Attachment> attachmentsToRemove = attachments.stream()
                .filter(isTrash())
                .collect(toList());
        attachmentsToRemove.forEach(this::deleteAttachmentFile);
        attachments.removeAll(attachmentsToRemove);
    }

    private void deleteAttachmentFile(Attachment attachment) {
        Path filePath = Paths.get(allure.allureResultsPath()).resolve(attachment.getSource());
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new AllureResultsWriteException("Could not remove Allure attachment", e);
        }
    }

    private Predicate<Attachment> isTrash() {
        return attachment -> attachment.getName().matches(allure.removeAttachmentsPattern())
                && !attachment.getName().matches(allure.excludeAttachmentsPattern());
    }
}
