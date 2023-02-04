package ru.yandex.solomon.alert.cluster.broker.alert;

import java.util.concurrent.CompletableFuture;

import ru.yandex.solomon.alert.cluster.broker.alert.activity.AlertActivity;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.protobuf.CreateAlertsFromTemplateRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;

/**
 * @author Alexey Trushkin
 */
public class ProjectAlertServiceValidatorStub implements ProjectAlertServiceValidator {
    @Override
    public CompletableFuture<String> validateCreate(Alert alert) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> validateUpdate(Alert alert, AlertActivity prevActivity, boolean isUserRequest) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> validateAlertVersionUpdate(Alert alert) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> validateCreateAlertsFromTemplate(CreateAlertsFromTemplateRequest alert) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean validateDelete(TDeleteAlertRequest request, AlertActivity alertActivity) {
        return true;
    }
}
