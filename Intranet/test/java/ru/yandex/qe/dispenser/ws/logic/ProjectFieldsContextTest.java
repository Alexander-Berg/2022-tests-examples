package ru.yandex.qe.dispenser.ws.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.google.common.collect.HashMultimap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.dao.person.PersonProjectRelations;
import ru.yandex.qe.dispenser.domain.dao.person.StaffCache;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDaoImpl;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectUtils;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.ProxyingHierarchy;
import ru.yandex.qe.dispenser.domain.hierarchy.Session;
import ru.yandex.qe.dispenser.ws.api.ApiTestBase;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * User: farruxkarimov
 * Date: 26.08.2019
 * Time: 16:47:xx
 * Тестируем ускоренное получение данных по ручке "/v1/projects", с параметром "fields=allMembers".
 */
public class ProjectFieldsContextTest extends ApiTestBase {

    @Inject
    private QuotaDao quotaDao;
    @Autowired
    private ProjectDao projectDao;
    @Autowired
    private StaffCache staffCache;


    /**
     * Тестируем, что запрашиваем, у Hierarchy, членов всех проекта - только 1 раз.
     */
    @Test
    public void noExtraFunctionCalls() {

        final AtomicInteger singleProjectParamMethodCallsNum = new AtomicInteger(0);
        final AtomicInteger multiProjectParamMethodCallsNum = new AtomicInteger(0);
        final ProjectDaoImpl localProjectDaoImpl = new ProjectDaoImpl() {
            @Override
            public @NotNull Set<String> getAllMembers(final @NotNull Project project) {
                singleProjectParamMethodCallsNum.incrementAndGet();
                return super.getAllMembers(project);
            }

            @NotNull
            @Override
            public HashMultimap<Project, String> getAllMembers(@NotNull final Collection<Project> projects) {
                multiProjectParamMethodCallsNum.incrementAndGet();
                return super.getAllMembers(projects);
            }
        };
        TransactionWrapper.INSTANCE.execute(() -> {
            quotaDao.clear();
            localProjectDaoImpl.setQuotaDao(quotaDao);
            localProjectDaoImpl.setStaffCache(staffCache);
            localProjectDaoImpl.setUserProjects(new PersonProjectRelations());
            localProjectDaoImpl.createAll(ProjectUtils.topologicalOrder(projectDao.getRoot()));
        });

        final ProxyingHierarchy proxyedHierarchy = (ProxyingHierarchy) Hierarchy.get();
        final ProjectDao oldReader = (ProjectDao) proxyedHierarchy.getProjectReader();
        proxyedHierarchy.setProjectDao(localProjectDaoImpl);
        Session.HIERARCHY.set(proxyedHierarchy);

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/projects/" + YANDEX + "/")
                .query("fields", "allMembers")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(0, singleProjectParamMethodCallsNum.get());
        assertEquals(1, multiProjectParamMethodCallsNum.get());

        singleProjectParamMethodCallsNum.set(0);
        multiProjectParamMethodCallsNum.set(0);
        response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/projects/")
                .query("fields", "allMembers")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals(0, singleProjectParamMethodCallsNum.get()); // no Hierarchy extra calling for initializing root members in MultiProjectFieldsContext
        assertEquals(1, multiProjectParamMethodCallsNum.get());

        proxyedHierarchy.setProjectDao(oldReader);
    }


    @Test
    public void methodCallParameters() {
        final List<Project> methodCalledArguments = new ArrayList<>();
        final ProjectDaoImpl localProjectDaoImpl = new ProjectDaoImpl() {
            @NotNull
            @Override
            public HashMultimap<Project, String> getAllMembers(@NotNull final Collection<Project> projects) {
                methodCalledArguments.addAll(projects);
                return super.getAllMembers(projects);
            }
        };
        TransactionWrapper.INSTANCE.execute(() -> {
            quotaDao.clear();
            localProjectDaoImpl.setQuotaDao(quotaDao);
            localProjectDaoImpl.setStaffCache(staffCache);
            localProjectDaoImpl.setUserProjects(new PersonProjectRelations());
            localProjectDaoImpl.createAll(ProjectUtils.topologicalOrder(projectDao.getRoot()));
        });

        final ProxyingHierarchy proxyedHierarchy = (ProxyingHierarchy) Hierarchy.get();
        final @NotNull ProjectDao oldReader = (ProjectDao) proxyedHierarchy.getProjectReader();
        proxyedHierarchy.setProjectDao(localProjectDaoImpl);
        Session.HIERARCHY.set(proxyedHierarchy);

        Response response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/projects/" + YANDEX + "/")
                .query("fields", "allMembers")
                .get();
        assertEquals(200, response.getStatus());
        final List<Project> pathToYandex = Hierarchy.get()
                .getProjectReader()
                .getRoot()
                .getPathToRoot();
        assertEquals(pathToYandex, methodCalledArguments);

        methodCalledArguments.clear();
        response = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/projects/")
                .query("fields", "allMembers")
                .get();
        assertEquals(200, response.getStatus());
        final Set<Project> allProjectsSet = Hierarchy.get()
                .getProjectReader()
                .getAll();
        assertEquals(new HashSet<>(methodCalledArguments), allProjectsSet);

        proxyedHierarchy.setProjectDao(oldReader);
    }
}
