package ru.yandex.solomon.alert.dao;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Vladimir Gordiychuk
 */
public class ProjectsHolderStub implements ProjectsHolder {

    private ConcurrentMap<String, ProjectHolderImpl.ProjectView> projectsById = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> reload() {
        return CompletableFuture.runAsync(() -> {});
    }

    @Override
    public CompletableFuture<Boolean> hasProject(String projectId) {
        return CompletableFuture.completedFuture(projectsById.containsKey(projectId));
    }

    @Override
    public Set<String> getProjects() {
        return projectsById.keySet();
    }

    @Override
    public Optional<ProjectHolderImpl.ProjectView> getProjectView(String id) {
        return Optional.ofNullable(projectsById.get(id));
    }

    public void addProject(String projectId) {
        projectsById.putIfAbsent(projectId, new ProjectHolderImpl.ProjectView(projectId, projectId));
    }

    public void removeProject(String projectId) {
        projectsById.remove(projectId);
    }
}
