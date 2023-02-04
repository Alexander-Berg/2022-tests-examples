package ru.yandex.solomon.alert.cluster.broker.alert;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;

/**
 * @author Alexey Trushkin
 */
public class AlertPostInitializerStub implements AlertPostInitializer {
    @Override
    public CompletableFuture<Alert> initializeCreate(Alert alert) {
        return CompletableFuture.completedFuture(alert);
    }

    @Override
    public CompletableFuture<Alert> initializeVersionUpdate(Alert alert) {
        return CompletableFuture.completedFuture(alert);
    }

    @Override
    public CompletableFuture<List<AlertFromTemplatePersistent>> initializeTemplateAlertsFromPublishedTemplates(List<AlertFromTemplatePersistent> alerts, String serviceProviderId) {
        return CompletableFuture.completedFuture(alerts);
    }
}
