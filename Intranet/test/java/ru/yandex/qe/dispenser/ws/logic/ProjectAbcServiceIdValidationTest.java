package ru.yandex.qe.dispenser.ws.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceReference;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;

import static ru.yandex.qe.dispenser.ws.abc.AbcApiHelper.INVALID_ABC_SERVICE_ID_ERROR_MESSAGE;
import static ru.yandex.qe.dispenser.ws.abc.AbcApiHelper.INVALID_ANCESTOR_ERROR_MESSAGE;
import static ru.yandex.qe.dispenser.ws.abc.AbcApiHelper.INVALID_DESCENDANTS_ABC_SERVICES_ERROR_MESSAGE;
import static ru.yandex.qe.dispenser.ws.abc.AbcApiHelper.MISSING_NESTED_ABC_SERVICES_ERROR_MESSAGE;
import static ru.yandex.qe.dispenser.ws.abc.AbcApiHelper.NON_SPECIFIC_PARENT_ABC_SERVICE_ERROR_MESSAGE;

public class ProjectAbcServiceIdValidationTest extends BusinessLogicTestBase {

    @Autowired
    private MockAbcApi mockAbcApi;

    @BeforeAll
    public void initAbcTree() {
        constructAbcBinaryTree(10);
    }

    @AfterAll
    public void resetAbcTree() {
        mockAbcApi.reset();
    }

    private void constructAbcBinaryTree(final int depth) {
        final int count = (int) Math.pow(2, depth);
        final AbcService[] services = new AbcService[count];
        for (int i = 1; i < count; i++) {

            final List<AbcServiceReference> ancestors;

            if (i > 1) {
                final int parent = i / 2;
                ancestors = new ArrayList<>(services[parent].getAncestors());
                ancestors.add(new AbcServiceReference(parent));
            } else {
                ancestors = Collections.emptyList();
            }
            services[i] = mockAbcApi.addService(i, ancestors);
        }
    }

    public static Object[][] validChildServiceIds() {
        return new Object[][]{{14}, {30}, {124}};
    }

    @MethodSource("validChildServiceIds")
    @ParameterizedTest
    private void insertedProjectMustHaveNestedAbcService(final Integer validChildAbcServiceId) {
        initProject("abc-7", 7, YANDEX);

        insertProject("abc-valid", validChildAbcServiceId, "abc-7", Collections.emptySet());
    }

    public static Object[][] invalidChildServiceIds() {
        return new Object[][]{{1}, {3}, {12}, {16}, {21}, {129}};
    }

    @MethodSource("invalidChildServiceIds")
    @ParameterizedTest
    private void insertedProjectMustHaveOnlyNestedAbcService(final Integer invalidChildAbcServiceId) {
        initProject("abc-7", 7, YANDEX);

        assertThrowsWithMessage(() -> {
            insertProject("abc-invalid", invalidChildAbcServiceId, "abc-7", Collections.emptySet());
        }, INVALID_ANCESTOR_ERROR_MESSAGE, "abc-7");
    }

    @Test
    private void insertedProjectMustHaveMostSpecifyingAbcService() {
        initProject("abc-7", 7, YANDEX);
        initProject("abc-30", 30, "abc-7");

        assertThrowsWithMessage(() -> {
            insertProject("abc-122", 122, "abc-7", Collections.emptySet());
        }, NON_SPECIFIC_PARENT_ABC_SERVICE_ERROR_MESSAGE, "abc-30");

        insertProject("abc-122", 124, "abc-7", Collections.emptySet());
    }

    @Test
    private void nonLeafProjectInsertionShouldRetainTreeConsistent() {
        initProject("abc-9", 9, YANDEX);
        initProject("abc-19", 19, "abc-9");
        initProject("abc-36", 36, "abc-9");
        initProject("abc-37", 37, "abc-9");
        initProject("abc-73", 73, "abc-36");
        initProject("abc-39", 39, "abc-19");

        assertThrowsWithMessage(() -> {
            insertProject("abc-18", 18, "abc-9", Collections.emptySet());
        }, MISSING_NESTED_ABC_SERVICES_ERROR_MESSAGE, "abc-36", "abc-37");

        assertThrowsWithMessage(() -> {
            insertProject("abc-18", 18, "abc-9", ImmutableSet.of("abc-73"));
        }, MISSING_NESTED_ABC_SERVICES_ERROR_MESSAGE, "abc-36");

        assertThrowsWithMessage(() -> {
            insertProject("abc-18", 18, "abc-9", ImmutableSet.of("abc-19"));
        }, INVALID_DESCENDANTS_ABC_SERVICES_ERROR_MESSAGE, "abc-19");

        insertProject("abc-18", 18, "abc-9", ImmutableSet.of("abc-36", "abc-37"));
    }

    @Test
    public void insertionProjectsWithSameAbcServiceIdMustWork() {
        initProject("no-abc-1", null, YANDEX);
        initProject("abc-1", 1, "no-abc-1");
        initProject("no-abc-19", null, "abc-1");
        initProject("abc-19", 19, "no-abc-19");
        initProject("abc-100", 100, "abc-1");
        initProject("abc-400", 400, "abc-100");

        insertProject("abc-400-2", 400, "abc-400", Collections.emptySet());
        insertProject("abc-400-3", 400, "abc-100", ImmutableSet.of("abc-400"));
        insertProject("abc-1-2", 1, "abc-1", ImmutableSet.of("no-abc-19"));
    }

    @Test
    public void insertInValidTreeShouldCheckMostSpecificParent() {
        initProject("no-abc-4", null, YANDEX);
        initProject("abc-4", 4, "no-abc-4");
        initProject("no-abc-8", null, YANDEX);
        assertThrowsWithMessage(() -> {
            insertProject("abc-8", 8, "no-abc-8", Collections.emptySet());
        }, NON_SPECIFIC_PARENT_ABC_SERVICE_ERROR_MESSAGE, "abc-4");
    }

    @Test
    public void updatingProjectShouldSaveCorrectHierarcy() {
        initProject("abc-16", 16, YANDEX);

        insertProject("abc-64-257", 64, "abc-16", Collections.emptySet());

        updateProject("abc-64-257", 257, "abc-16", Collections.emptySet());
        updateProject("abc-64-257", 64, "abc-16", Collections.emptySet());
    }

    private void initProject(final String key, final Integer abcServiceId, final String parentKey) {

        projectDao.create(Project.withKey(key)
                .name(key)
                .description(key)
                .abcServiceId(abcServiceId)
                .parent(projectDao.read(parentKey))
                .build());

        updateHierarchy();
    }

    private void insertProject(final String key, final int abcServiceId, final String parentKey,
                               final Collection<String> subProjectKeys) {
        dispenser()
                .projects()
                .create(DiProject.withKey(key)
                        .withName(key)
                        .withDescription(key)
                        .withParentProject(parentKey)
                        .withAbcServiceId(abcServiceId)
                        .withSubprojects(subProjectKeys)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();
    }

    @Test
    public void insertProjectWithLeafAbcServiceIdShouldWork() {
        initProject("abc-1", 1, YANDEX);

        insertProject("abc-1000", 1000, "abc-1", Collections.emptySet());
    }

    @Test
    public void insertProjectWithInvalidAbcServiceIdShouldReturnCorrectMessage() {
        assertThrowsWithMessage(() -> {
            insertProject("invalid-abc", 9000, YANDEX, Collections.emptySet());
        }, INVALID_ABC_SERVICE_ID_ERROR_MESSAGE);

        mockAbcApi.addService(9000, Collections.emptyList());

        insertProject("valid-abc", 9000, YANDEX, Collections.emptySet());
    }

    private void updateProject(final String key, final int abcServiceId, final String parentKey,
                               final Collection<String> subProjectKeys) {
        final DiProject project = dispenser()
                .projects()
                .get()
                .withKey(key)
                .perform();

        dispenser()
                .projects()
                .update(DiProject.copyOf(project)
                        .withParentProject(parentKey)
                        .withAbcServiceId(abcServiceId)
                        .withSubprojects(subProjectKeys)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();
    }
}
