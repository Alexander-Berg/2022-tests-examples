package ru.yandex.qe.dispenser.testing;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Triple;

import ru.yandex.qe.dispenser.api.v1.request.DiEntity;

public class Context {
    private final Random rand = new Random();
    private final int usersCount;
    private final int projectsCount;
    private final List<Triple<String, String, DiEntity>> usages = new ArrayList<>();

    public Context(final int usersCount, final int projectsCount) {
        this.usersCount = usersCount;
        this.projectsCount = projectsCount;
    }

    public String getAnyUser() {
        return "user-" + (rand.nextInt(usersCount) + 1);
    }

    public String getAnyProject() {
        return "project-" + (rand.nextInt(projectsCount) + 1);
    }

    public void registerUsage(final String project, final String user, final DiEntity entity) {
        usages.add(Triple.of(project, user, entity));
    }

    public int getAnyUsageId() {
        return usages.isEmpty() ? -1 : rand.nextInt(usages.size());
    }

    public void removeUsage(final int i) {
        if (i >= 0) {
            usages.remove(i);
        }
    }

    public void applyToUsageAndRemove(final Consumer<Triple<String, String, DiEntity>> consumer) {
        if (!usages.isEmpty()) {
            final int i = getAnyUsageId();
            final Triple<String, String, DiEntity> usage = usages.get(i);
            removeUsage(i);
            consumer.accept(usage);
        }
    }

    public void applyToUsageAndAdd(final Consumer<Triple<String, String, DiEntity>> consumer) {
        if (!usages.isEmpty()) {
            final int i = getAnyUsageId();
            usages.add(usages.get(i));
            consumer.accept(usages.get(i));
        }
    }

    public int getRandomInt(final int bound) {
        return rand.nextInt(bound);
    }
}
