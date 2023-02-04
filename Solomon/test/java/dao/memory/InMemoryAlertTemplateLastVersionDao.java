package ru.yandex.solomon.alert.dao.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import ru.yandex.solomon.alert.dao.AlertTemplateLastVersionDao;
import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;
import ru.yandex.solomon.core.exceptions.ConflictException;
import ru.yandex.solomon.ydb.page.TokenBasePage;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * @author Alexey Trushkin
 */
public class InMemoryAlertTemplateLastVersionDao implements AlertTemplateLastVersionDao {

    private final ConcurrentMap<String, AlertTemplateLastVersion> templateById = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> createSchemaForTests() {
        return runAsync(() -> {
        });
    }

    @Override
    public CompletableFuture<Void> dropSchemaForTests() {
        return runAsync(templateById::clear);
    }

    @Override
    public CompletableFuture<TokenBasePage<AlertTemplateLastVersion>> find(String serviceProviderId, String name, int pageSize, String pageToken) {
        return CompletableFuture.supplyAsync(() -> {
            var list = templateById.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(entry -> (serviceProviderId.isEmpty() || entry.getValue().serviceProviderId().equals(serviceProviderId)) &&
                            (name.isEmpty() || entry.getValue().name().equals(name)))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            return toPageResult(list, pageSize, pageToken);
        });
    }

    @Override
    public CompletableFuture<Optional<AlertTemplateLastVersion>> findById(String id) {
        return supplyAsync(() -> Optional.ofNullable(templateById.get(id)));
    }

    @Override
    public CompletableFuture<Boolean> delete(String id, int version) {
        return supplyAsync(() -> templateById.remove(id))
                .thenApply(alertTemplateLastVersion -> true);
    }

    @Override
    public CompletableFuture<Boolean> create(AlertTemplateLastVersion alertTemplate) {
        return supplyAsync(() -> templateById.putIfAbsent(alertTemplate.id(), alertTemplate) == null);
    }

    @Override
    public CompletableFuture<Boolean> updateDeployTask(String templateId, int version, String taskId) {
        return supplyAsync(() -> {
            AlertTemplateLastVersion previous = templateById.get(templateId);
            if (previous.version() != version) {
                throw new ConflictException("Conflict");
            }
            var result = new AlertTemplateLastVersion
                    (
                            previous.id(),
                            previous.templateVersionTag(),
                            previous.serviceProviderId(),
                            previous.name(),
                            previous.version() + 1,
                            taskId
                    );
            boolean replace = templateById.replace(templateId, previous, result);
            if (!replace) {
                throw new ConflictException("Conflict");
            }
            return replace;
        });
    }

    @Override
    public CompletableFuture<List<AlertTemplateLastVersion>> getAll() {
        return supplyAsync(() -> new ArrayList<>(templateById.values()));
    }

    private TokenBasePage<AlertTemplateLastVersion> toPageResult(List<AlertTemplateLastVersion> matched, int pageSize, String pageToken) {
        if (pageSize <= 0) {
            pageSize = 100;
        } else {
            pageSize = Math.min(pageSize, 1000);
        }

        int offset = pageToken.isEmpty() ? 0 : Integer.parseInt(pageToken);
        int nextOffset = Math.min(offset + pageSize, matched.size());
        var list = matched.subList(offset, nextOffset);
        if (nextOffset >= matched.size()) {
            return new TokenBasePage<>(list, "");
        }
        return new TokenBasePage<>(list, Integer.toString(nextOffset));
    }
}
