package ru.yandex.qe.dispenser.domain;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ProjectTest {
    private List<Project> tree;

    @BeforeEach
    public void setUp() {
        final Project p0 = new Project("0", "0", null);
        final Project p1 = new Project("1", "1", p0);
        final Project p2 = new Project("2", "2", p1);
        final Project p3 = new Project("3", "3", p1);
        final Project p4 = new Project("4", "4", p1);
        final Project p5 = new Project("5", "5", p4);
        final Project p6 = new Project("6", "6", p4);
        final Project p7 = new Project("7", "7", p0);
        tree = Arrays.asList(p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Test
    public void testLCP() {
        assertEquals(tree.get(1), tree.get(2).getLeastCommonAncestor(tree.get(5)));
    }

    @Test
    public void testEqulasOrSon() {
        assertTrue(tree.get(4).equalsOrSon(tree.get(4)));
        assertTrue(tree.get(6).equalsOrSon(tree.get(1)));
    }

    @Test
    public void equalsProjectsMustBeEqualsOrSon() {
        final Project p1 = tree.get(1);
        final Project p1Copy = Project.withKey(p1.getPublicKey()).name(p1.getName()).description(p1.getDescription()).parent(p1.getParent()).build();
        assertTrue(p1Copy.equalsOrSon(p1));
    }
}