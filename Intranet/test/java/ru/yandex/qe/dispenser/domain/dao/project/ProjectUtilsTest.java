package ru.yandex.qe.dispenser.domain.dao.project;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.domain.Project;

public final class ProjectUtilsTest {
    @Test
    public void topologicalOrderMustBeValidAtAllInputProjectSequenses() {
        final Project p1 = new Project("1", "", null);
        final Project p2 = new Project("2", "", p1);
        final Project p3 = new Project("3", "", p1);
        final Project p4 = new Project("4", "", p3);
        final Project p5 = new Project("5", "", p3);
        final Project p6 = new Project("6", "", p1);
        final Project p7 = new Project("7", "", p1);
        final Project p8 = new Project("8", "", p7);
        final List<Project> projects = Arrays.asList(p1, p3, p4, p8, p6, p2, p7, p5);
        for (int t = 0; t < 1000; t++) {
            Collections.shuffle(projects);
            final List<Project> sortedProjects = ProjectUtils.topologicalOrder(ProjectUtils.root(projects));
            for (int i = 0; i < sortedProjects.size(); i++) {
                final Project project = sortedProjects.get(i);
                if (!project.isRoot()) {
                    if (sortedProjects.subList(0, i).stream().noneMatch(project.getParent()::equals)) {
                        Assertions.fail(Arrays.toString(sortedProjects.stream().map(Project::getPublicKey).map(Integer::parseInt).toArray()));
                    }
                }
            }
        }
    }
}