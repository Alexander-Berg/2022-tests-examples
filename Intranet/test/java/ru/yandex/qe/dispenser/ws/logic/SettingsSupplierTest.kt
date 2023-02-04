package ru.yandex.qe.dispenser.ws.logic

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao
import ru.yandex.qe.dispenser.domain.hierarchy.PermissionsCache
import ru.yandex.qe.dispenser.domain.hierarchy.Role
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase

class SettingsSupplierTest(
    @Autowired private val projectDaoImpl: ProjectDao,
    @Autowired private val personDaoImpl: PersonDao,
    @Autowired private val permissionsCache: PermissionsCache,
    @Value("\${dispenser.owning.cost.access.project.key}") private val owningCostAccessProjectKey: String
): AcceptanceTestBase() {

    @Test
    fun testIsInOwningCostAccessProjectForMember() {
        val personInProject = personDaoImpl.readPersonByLogin(VEGED.login)
        val personInParentProject = personDaoImpl.readPersonByLogin(WELVET.login)
        val project = projectDaoImpl.read(owningCostAccessProjectKey)
        val parentProject = projectDaoImpl.read(YANDEX)
        try {
            projectDaoImpl.attach(personInProject, project, Role.MEMBER)
            projectDaoImpl.attach(personInParentProject, parentProject, Role.MEMBER)
            updateHierarchy()
            Assertions.assertTrue(permissionsCache.canUserViewMoney(personInProject))
            Assertions.assertFalse(permissionsCache.canUserViewMoney(personInParentProject))
        } finally {
            projectDaoImpl.detach(personInProject, project, Role.MEMBER)
            projectDaoImpl.detach(personInParentProject, parentProject, Role.MEMBER)
            updateHierarchy()
        }
    }

    @Test
    fun testIsInOwningCostAccessProjectForSteward() {
        val personInProject = personDaoImpl.readPersonByLogin(VEGED.login)
        val personInParentProject = personDaoImpl.readPersonByLogin(WELVET.login)
        val project = projectDaoImpl.read(owningCostAccessProjectKey)
        val parentProject = projectDaoImpl.read(YANDEX)
        try {
            projectDaoImpl.attach(personInProject, project, Role.STEWARD)
            projectDaoImpl.attach(personInParentProject, parentProject, Role.STEWARD)
            updateHierarchy()
            Assertions.assertTrue(permissionsCache.canUserViewMoney(personInProject))
            Assertions.assertFalse(permissionsCache.canUserViewMoney(personInParentProject))
        } finally {
            projectDaoImpl.detach(personInProject, project, Role.STEWARD)
            projectDaoImpl.detach(personInParentProject, parentProject, Role.STEWARD)
            updateHierarchy()
        }
    }

    @Test
    fun testIsInOwningCostAccessProjectForLeader() {
        val personInProject = personDaoImpl.readPersonByLogin(VEGED.login)
        val personInParentProject = personDaoImpl.readPersonByLogin(WELVET.login)
        val project = projectDaoImpl.read(owningCostAccessProjectKey)
        val parentProject = projectDaoImpl.read(YANDEX)
        try {
            projectDaoImpl.attach(personInProject, project, Role.VS_LEADER)
            projectDaoImpl.attach(personInParentProject, parentProject, Role.VS_LEADER)
            updateHierarchy()
            Assertions.assertTrue(permissionsCache.canUserViewMoney(personInProject))
            Assertions.assertFalse(permissionsCache.canUserViewMoney(personInParentProject))
        } finally {
            projectDaoImpl.detach(personInProject, project, Role.VS_LEADER)
            projectDaoImpl.detach(personInParentProject, parentProject, Role.VS_LEADER)
            updateHierarchy()
        }
    }

}
