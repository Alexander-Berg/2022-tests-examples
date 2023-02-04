package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.ProjectRole;
import ru.yandex.qe.dispenser.domain.YaGroup;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.role.ProjectRoleDao;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectRoleTest extends AcceptanceTestBase {

    @Autowired
    private ProjectRoleDao projectRoleDao;
    @Autowired
    private PersonDao personDao;

    @Test
    public void personAndGroupCanBeAttachedToRoleAndDeattached() {
        final Person person = personDao.readPersonByLogin(QDEEE.getLogin());
        final Project project = projectDao.read(YANDEX);
        final YaGroup group = groupDao.read(YANDEX);

        final ProjectRole role = projectRoleDao.create(new ProjectRole("test", ProjectRole.AbcRoleSyncType.NONE, null));

        projectDao.attachAll(Collections.singleton(person), Collections.singleton(group), project, role.getId());

        assertEquals(Collections.singleton(person), projectDao.getLinkedPersons(project, role.getKey()));
        assertEquals(Collections.singleton(group), projectDao.getLinkedGroups(project, role.getKey()));

        projectDao.detachAll(Collections.singleton(person), Collections.emptySet(), project, role.getId());
        assertEquals(Collections.emptySet(), projectDao.getLinkedPersons(project, role.getKey()));

        projectDao.detachAll(Collections.emptySet(), Collections.singleton(group), project, role.getId());
        assertEquals(Collections.emptySet(), projectDao.getLinkedGroups(project, role.getKey()));
    }

}
