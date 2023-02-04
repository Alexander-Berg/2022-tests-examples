from unittest.mock import patch

from intranet.femida.src.staff.sync import DepartmentUserSyncronizer
from intranet.femida.src.staff.models import DepartmentUser
from intranet.femida.src.staff.choices import DEPARTMENT_ROLES
from intranet.femida.src.vacancies.choices import VACANCY_ROLES, VACANCY_STATUSES
from intranet.femida.src.vacancies.models import Vacancy, VacancyMembership

from .utils import api, db


DR = DEPARTMENT_ROLES
VR = VACANCY_ROLES


def test_sync_empty_db():
    departments = db.create_departments()
    db.create_users()
    db.create_vacancies(departments)

    syncronizer = DepartmentUserSyncronizer()
    with patch.object(syncronizer.repo, 'getiter', new=api.response_without_last_head):
        syncronizer.sync()

        data = list(
            DepartmentUser.objects
            .values_list('department_id', 'user__username', 'role', 'is_direct', 'is_closest')
        )

        assert len(data) == 9
        assert (1001, 'chief1', DR.chief, True, True) in data
        assert (1002, 'chief1', DR.chief, False, False) in data
        assert (1002, 'chief2', DR.chief, True, True) in data
        assert (1002, 'hr1', DR.hr_partner, True, True) in data
        assert (1002, 'hr2', DR.hr_partner, True, True) in data
        assert (1003, 'chief1', DR.chief, False, False) in data
        assert (1003, 'chief2', DR.chief, False, True) in data
        assert (1003, 'hr1', DR.hr_partner, False, True) in data
        assert (1003, 'hr2', DR.hr_partner, False, True) in data

        data = list(
            VacancyMembership.unsafe
            .filter(role__in=(VR.head, VR.auto_observer))
            .values_list('vacancy_id', 'member__username', 'role')
        )
        assert len(data) == 12
        assert (1, 'chief1', VR.head) in data
        assert (1, 'chief1', VR.auto_observer) in data
        assert (2, 'chief1', VR.auto_observer) in data
        assert (2, 'chief2', VR.auto_observer) in data
        assert (2, 'chief2', VR.head) in data
        assert (2, 'hr1', VR.auto_observer) in data
        assert (2, 'hr2', VR.auto_observer) in data
        assert (3, 'chief1', VR.auto_observer) in data
        assert (3, 'chief2', VR.auto_observer) in data
        assert (3, 'chief2', VR.head) in data
        assert (3, 'hr1', VR.auto_observer) in data
        assert (3, 'hr2', VR.auto_observer) in data


def test_sync_add_last_head():
    db.state_without_last_head()
    syncronizer = DepartmentUserSyncronizer()
    with patch.object(syncronizer.repo, 'getiter', new=api.response_with_last_head):
        syncronizer.sync()

        data = list(
            DepartmentUser.objects.values_list(
                'department_id', 'role', 'user__username', 'is_direct', 'is_closest',
            )
        )
        assert len(data) == 10
        assert (1003, DR.chief, 'chief3', True, True) in data
        assert (1003, DR.chief, 'chief2', False, False) in data
        assert (1003, DR.chief, 'chief2', False, True) not in data

        data = list(
            VacancyMembership.unsafe
            .filter(role__in=(VR.head, VR.auto_observer))
            .values_list('vacancy_id', 'member__username', 'role')
        )
        assert len(data) == 13
        assert (3, 'chief2', VR.head) not in data
        assert (3, 'chief3', VR.head) in data
        assert (3, 'chief3', VR.auto_observer) in data


def test_sync_remove_last_head():
    db.state_with_last_head()
    syncronizer = DepartmentUserSyncronizer()
    with patch.object(syncronizer.repo, 'getiter', new=api.response_without_last_head):
        syncronizer.sync()

        data = list(
            DepartmentUser.objects.values_list(
                'department_id', 'role', 'user__username', 'is_direct', 'is_closest',
            )
        )
        assert len(data) == 9
        assert (1003, DR.chief, 'chief3', True, True) not in data
        assert (1003, DR.chief, 'chief2', False, False) not in data
        assert (1003, DR.chief, 'chief2', False, True) in data

        data = list(
            VacancyMembership.unsafe
            .filter(role__in=(VR.head, VR.auto_observer))
            .values_list('vacancy_id', 'member__username', 'role')
        )
        assert len(data) == 12
        assert (3, 'chief2', VR.head) in data
        assert (3, 'chief3', VR.head) not in data
        assert (3, 'chief3', VR.auto_observer) not in data


def test_sync_head_on_closed_vacancy():
    db.state_with_last_head()
    vacancy3 = Vacancy.unsafe.get(id=3)
    vacancy3.status = VACANCY_STATUSES.closed
    vacancy3.save()

    syncronizer = DepartmentUserSyncronizer()
    with patch.object(syncronizer.repo, 'getiter', new=api.response_without_last_head):
        syncronizer.sync()

        data = list(
            VacancyMembership.unsafe
            .filter(role__in=(VR.head, VR.auto_observer))
            .values_list('vacancy_id', 'member__username', 'role')
        )
        assert len(data) == 12
        assert (3, 'chief2', VR.head) not in data
        assert (3, 'chief3', VR.head) in data
