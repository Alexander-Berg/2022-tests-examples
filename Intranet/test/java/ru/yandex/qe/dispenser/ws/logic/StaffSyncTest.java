package ru.yandex.qe.dispenser.ws.logic;

import java.util.ArrayList;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.domain.dao.group.GroupDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonGroupMembershipDao;
import ru.yandex.qe.dispenser.domain.staff.StaffDepartmentGroup;
import ru.yandex.qe.dispenser.domain.staff.StaffGroup;
import ru.yandex.qe.dispenser.domain.staff.StaffGroupType;
import ru.yandex.qe.dispenser.domain.staff.StaffPerson;
import ru.yandex.qe.dispenser.domain.staff.StaffPersonAffiliation;
import ru.yandex.qe.dispenser.standalone.MockStaff;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.staff.StaffSyncManager;

public class StaffSyncTest extends BusinessLogicTestBase {

    @Autowired
    private MockStaff mockStaff;

    @Autowired
    private StaffSyncManager staffSyncManager;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private PersonGroupMembershipDao groupMembershipDao;

    @AfterAll
    public static void afterClass(@Autowired final MockStaff mockStaff) {
        mockStaff.setGroups(new ArrayList<>());
        mockStaff.setPersons(new ArrayList<>());
    }

    @BeforeEach
    public void beforeMethods() {
        mockStaff.setGroups(new ArrayList<>());
        mockStaff.setPersons(new ArrayList<>());
        TransactionWrapper.INSTANCE.execute(() -> {
            personDao.clear();
            groupDao.clear();
            groupMembershipDao.clear();
        });
    }

    @Test
    public void testSync() {
        mockStaff.setGroups(ImmutableList.of(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false),
                new StaffGroup(4L, "test_4", StaffGroupType.SERVICE, false),
                new StaffGroup(5L, "test_5", StaffGroupType.SERVICE, true),
                new StaffGroup(6L, "test_6", StaffGroupType.SERVICE, true),
                new StaffGroup(7L, "test_7", StaffGroupType.DEPARTMENT, false)));
        mockStaff.setPersons(ImmutableList.of(new StaffPerson(1L, "1", "test_1", false,
                new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                        new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.SERVICE, false)),
                        new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                        ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                                new StaffGroup(7L, "test_7", StaffGroupType.DEPARTMENT, false)))),
                new StaffPerson(2L, "2", "test_2", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                                        new StaffGroup(7L, "test_7", StaffGroupType.DEPARTMENT, false))))));
        staffSyncManager.syncAll();
        Assertions.assertEquals(2, personDao.tryReadPersonsByUids(ImmutableList.of(1L, 2L)).size());
        Assertions.assertEquals(7, groupDao.tryReadYaGroupsByStaffIds(ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
        Assertions.assertEquals(12, groupMembershipDao.getAll().size());
        mockStaff.setGroups(ImmutableList.of(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false),
                new StaffGroup(4L, "test_4", StaffGroupType.SERVICE, false),
                new StaffGroup(5L, "test_5", StaffGroupType.SERVICE, true),
                new StaffGroup(6L, "test_6", StaffGroupType.SERVICE, true),
                new StaffGroup(7L, "test_7", StaffGroupType.DEPARTMENT, true)));
        mockStaff.setPersons(ImmutableList.of(new StaffPerson(1L, "1", "test_1", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false)))),
                new StaffPerson(2L, "2", "test_2", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false))))));
        staffSyncManager.syncAll();
        Assertions.assertEquals(2, personDao.tryReadPersonsByUids(ImmutableList.of(1L, 2L)).size());
        Assertions.assertEquals(7, groupDao.tryReadYaGroupsByStaffIds(ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
        Assertions.assertEquals(10, groupMembershipDao.getAll().size());
    }

    @Test
    public void testSyncWithNullStaffGroupType() {
        mockStaff.setGroups(ImmutableList.of(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false),
                new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false),
                new StaffGroup(5L, "test_5", StaffGroupType.SERVICE, true),
                new StaffGroup(6L, "test_6", StaffGroupType.SERVICE, true),
                new StaffGroup(7L, "test_7", null, false)));
        mockStaff.setPersons(ImmutableList.of(new StaffPerson(1L, "1", "test_1", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                                        new StaffGroup(7L, "test_7", StaffGroupType.DEPARTMENT, false)))),
                new StaffPerson(2L, "2", "test_2", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                                        new StaffGroup(7L, "test_7", null, false))))));
        staffSyncManager.syncAll();
        Assertions.assertEquals(2, personDao.tryReadPersonsByUids(ImmutableList.of(1L, 2L)).size());
        Assertions.assertEquals(5, groupDao.tryReadYaGroupsByStaffIds(ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
        Assertions.assertEquals(8, groupMembershipDao.getAll().size());
        mockStaff.setGroups(ImmutableList.of(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false),
                new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false),
                new StaffGroup(5L, "test_5", StaffGroupType.SERVICE, true),
                new StaffGroup(6L, "test_6", StaffGroupType.SERVICE, true),
                new StaffGroup(7L, "test_7", null, true)));
        mockStaff.setPersons(ImmutableList.of(new StaffPerson(1L, "1", "test_1", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false)))),
                new StaffPerson(2L, "2", "test_2", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false))))));
        staffSyncManager.syncAll();
        Assertions.assertEquals(2, personDao.tryReadPersonsByUids(ImmutableList.of(1L, 2L)).size());
        Assertions.assertEquals(5, groupDao.tryReadYaGroupsByStaffIds(ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
        Assertions.assertEquals(8, groupMembershipDao.getAll().size());
    }

    @Test
    public void testSyncWithNullStaffPersonGroup() {
        mockStaff.setGroups(ImmutableList.of(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false),
                new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false),
                new StaffGroup(5L, "test_5", StaffGroupType.SERVICE, true),
                new StaffGroup(6L, "test_6", StaffGroupType.SERVICE, true),
                new StaffGroup(7L, "test_7", null, false)));
        mockStaff.setPersons(ImmutableList.of(new StaffPerson(1L, "1", "test_1", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        null,
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                                        new StaffGroup(7L, "test_7", StaffGroupType.DEPARTMENT, false)))),
                new StaffPerson(2L, "2", "test_2", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        null)));
        staffSyncManager.syncAll();
        Assertions.assertEquals(2, personDao.tryReadPersonsByUids(ImmutableList.of(1L, 2L)).size());
        Assertions.assertEquals(6, groupDao.tryReadYaGroupsByStaffIds(ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
        Assertions.assertEquals(5, groupMembershipDao.getAll().size());
        mockStaff.setGroups(ImmutableList.of(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false),
                new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false),
                new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false),
                new StaffGroup(5L, "test_5", StaffGroupType.SERVICE, true),
                new StaffGroup(6L, "test_6", StaffGroupType.SERVICE, true),
                new StaffGroup(7L, "test_7", null, true)));
        mockStaff.setPersons(ImmutableList.of(new StaffPerson(1L, "1", "test_1", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false)))),
                new StaffPerson(2L, "2", "test_2", false,
                        new StaffPerson.Official(StaffPersonAffiliation.YANDEX, false, false),
                        ImmutableList.of(new StaffPerson.PersonGroup(new StaffGroup(3L, "test_3", StaffGroupType.SERVICE, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(4L, "test_4", StaffGroupType.UNKNOWN, false)),
                                new StaffPerson.PersonGroup(new StaffGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false))),
                        new StaffDepartmentGroup(1L, "test_1", StaffGroupType.DEPARTMENT, false,
                                ImmutableList.of(new StaffGroup(2L, "test_2", StaffGroupType.DEPARTMENT, false))))));
        staffSyncManager.syncAll();
        Assertions.assertEquals(2, personDao.tryReadPersonsByUids(ImmutableList.of(1L, 2L)).size());
        Assertions.assertEquals(6, groupDao.tryReadYaGroupsByStaffIds(ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L)).size());
        Assertions.assertEquals(8, groupMembershipDao.getAll().size());
    }
}
