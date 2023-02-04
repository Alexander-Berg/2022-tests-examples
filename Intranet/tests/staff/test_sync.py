# coding=utf-8
from collections import namedtuple
import mock
import uuid

from review.shortcuts import models
from review.staff import const as staff_const
from review.staff.sync.store import sync_staff_full


def test_staff_sync(
        department_child_builder,
        department_root_builder,
        department_role_builder,
        person_builder,
        hr_builder,
):
    models.Person.objects.all().delete()  # в начале теста создается ненужный пользователь
    to_del_by_sync = person_builder(login='will_be_deleted')

    # root department
    dep_root = department_root_builder(slug='dep_root')
    chief_root = person_builder(login='chief_root', department=dep_root)

    # department 0
    # департамент не меняется в ходе теста
    dep_0 = department_child_builder(slug='dep_0', parent=dep_root)
    chief_0 = person_builder(login='chief_0', department=dep_0)
    empl_0 = person_builder(login='empl_0', department=dep_0)
    dismissed_0 = person_builder(login='dismissed_0', department=dep_0, is_dismissed=True, quit_at='2008-02-04')
    hr_partner_0 = person_builder(login='hr_partner_0', department=dep_0)

    # department 1
    # chief_1 - уволится
    # dismissed_1 - возвратится в компанию
    # empl_1 - займет метсто chief_1
    dep_1 = department_child_builder(slug='dep_1', parent=dep_root)
    chief_1 = person_builder(login='chief_1', department=dep_1)
    empl_1 = person_builder(login='empl_1', department=dep_1)
    dismissed_1 = person_builder(login='dismissed_1', department=dep_1, is_dismissed=True, quit_at='2008-02-04')
    hr_partner_1 = person_builder(login='hr_partner_1', department=dep_1)

    # department 2
    # департамент будет расформирован
    # hr_partner_2 - уволится
    # chief_2 и empl_2 перейдут в dep_1
    dep_2 = department_child_builder(slug='dep_2', parent=dep_root)
    chief_2 = person_builder(login='chief_2', department=dep_2)
    empl_2 = person_builder(login='empl_2', department=dep_2)
    hr_partner_2 = person_builder(login='hr_partner_2', department=dep_2)

    # у уволенных данные для Subordination не обновляются со стаффа как у остальных,
    # они берутся из моделей StaffStructureChange и PersonHeads
    create_structure_change_and_person_heads(dismissed_0, [empl_0, chief_root], '2008-02-01')
    create_structure_change_and_person_heads(dismissed_1, [chief_2, chief_root], '2008-02-01')

    # add roles
    for dep, chief in [
        (dep_root, chief_root),
        (dep_0, chief_0),
        (dep_1, chief_1),
        (dep_2, chief_2),
    ]:
        department_role_builder(department=dep, person=chief, type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD)

    for cared, hr in [
        (chief_0, hr_partner_0),
        (empl_0, hr_partner_0),
        (dismissed_0, hr_partner_0),
        (chief_1, hr_partner_1),
        (empl_1, hr_partner_1),
        (dismissed_1, hr_partner_1),
        (chief_2, hr_partner_2),
        (empl_2, hr_partner_2),
    ]:
        hr_builder(cared_person=cared, hr_person=hr, type=staff_const.STAFF_ROLE.HR.HR_PARTNER)

    # new objects from staff
    NewDepartment = namedtuple('NewDepartment', 'id, slug')
    NewPerson = namedtuple('NewPerson', 'id, login, uid, quit_at')

    hr_partner_root = NewPerson(1000, 'hr_partner_root', 1000, None)
    empl_root = NewPerson(1001, 'empl_root', 1001, None)
    empl_new_1 = NewPerson(1002, 'empl_new_1', 1002, None)

    # department 3
    dep_3 = NewDepartment(1000, 'dep_3')
    chief_3 = NewPerson(1004, 'chief_3', 1004, None)
    empl_3 = NewPerson(1005, 'empl_3', 1005, None)
    hr_partner_3 = NewPerson(1003, 'hr_partner_3', 1003, None)

    # create staff mocked data
    dep_to_persons = {
        dep_root: {chief_root, hr_partner_root, empl_root},
        dep_0: {chief_0, empl_0, dismissed_0, hr_partner_0},
        dep_1: {empl_1, empl_new_1, dismissed_1, chief_2, empl_2, hr_partner_1, chief_1},
        dep_2: {hr_partner_2},
        dep_3: {chief_3, empl_3, hr_partner_3},
    }
    persons_to_department_id = get_persons_to_department_id(dep_to_persons)

    department_to_parent_id = {
        dep_root: None,
        dep_0: dep_root.id,
        dep_1: dep_root.id,
        dep_2: dep_root.id,
        dep_3: dep_root.id,
    }
    raw_deps_data = [
        {
            'id': dep.id,
            'parent_id': parent_id,
            'slug': dep.slug,
            'name_ru': dep.slug,  # для простоты используем slug
            'name_en': dep.slug,
        } for dep, parent_id in department_to_parent_id.items()
    ]

    raw_roles_data = [
        {
            'department_group': {'department': {'id': dep.id}},
            'id': id_,
            'person': {'login': person.login},
            'role': role_type,
        } for id_, (dep, person, role_type) in enumerate([
            (dep_root, chief_root, 'chief'),
            (dep_root, hr_partner_root, 'hr_partner'),
            (dep_0, chief_0, 'chief'),
            (dep_0, hr_partner_0, 'hr_partner'),
            (dep_1, empl_1, 'chief'),
            (dep_1, hr_partner_1, 'hr_partner'),
            (dep_3, chief_3, 'chief'),
            (dep_3, hr_partner_3, 'hr_partner'),
        ])
    ]

    persons = [chief_root, chief_0, hr_partner_0, empl_0, hr_partner_1, empl_1, chief_2, empl_2]
    returned_persons = [dismissed_1]
    new_persons = [empl_new_1, empl_3, chief_3, hr_partner_3, hr_partner_root, empl_root]
    chief_1.quit_at = '2008-02-14'
    hr_partner_2.quit_at = '2008-02-14'
    dismissed_persons = [dismissed_0, chief_1, hr_partner_2]
    persons_pages = [get_persons_data(persons + returned_persons + new_persons, dismissed_persons)]

    # sync
    # предполагается, что новые данные уже есть в StaffStructureChange и PersonHeads
    create_structure_change_and_person_heads(chief_1, [chief_root], '2008-02-13')
    create_structure_change_and_person_heads(hr_partner_2, [chief_2, chief_root], '2008-02-13')

    with mock.patch('review.staff.sync.fetch.get_persons_departments_belonging',
                    return_value=persons_to_department_id) as persons_deps_mock, \
            mock.patch('review.staff.sync.fetch.get_departments', return_value=raw_deps_data) as deps_mock, \
            mock.patch('review.staff.sync.fetch.get_roles_result_set', return_value=raw_roles_data) as roles_mock, \
            mock.patch('review.staff.sync.fetch.get_persons_from_staff_paged',
                       return_value=persons_pages) as persons_mock:
        sync_staff_full()
        sync_staff_full()

    # check Person model
    all_persons = persons + returned_persons + dismissed_persons + new_persons
    assert models.Person.objects.filter(id__in=[p.id for p in all_persons]).count() == len(all_persons)
    not_dismissed = persons + returned_persons + new_persons
    assert models.Person.objects.filter(
        id__in=[it.id for it in not_dismissed], is_dismissed=False
    ).count() == len(not_dismissed)
    assert models.Person.objects.filter(
        id__in=[it.pk for it in dismissed_persons], is_dismissed=True
    ).count() == len(dismissed_persons)
    # new persons check
    for person in new_persons:
        assert models.Person.objects.filter(is_dismissed=False, is_robot=False, **person._asdict()).exists()

    # check Department model
    deps = [dep_root, dep_0, dep_1, dep_2, dep_3]
    assert models.Department.objects.filter(id__in=[d.id for d in deps]).count() == len(deps)
    # new departments check
    assert models.Department.objects.filter(**dep_3._asdict()).exists()

    # check DepartmentRole model
    chiefs_to_deps = {
        chief_root: dep_root,
        chief_0: dep_0,
        empl_1: dep_1,
        chief_3: dep_3,
    }
    for chief, dep in chiefs_to_deps.items():
        assert check_dep_role(chief, dep, staff_const.STAFF_ROLE.DEPARTMENT.HEAD)
    hrs_to_deps = {
        hr_partner_root: dep_root,
        hr_partner_0: dep_0,
        hr_partner_1: dep_1,
        hr_partner_3: dep_3,
    }
    for hr, dep in hrs_to_deps.items():
        assert check_dep_role(hr, dep, staff_const.STAFF_ROLE.HR.HR_PARTNER)

    # check Subordination model
    chief_to_employees_direct = {
        chief_root: {hr_partner_root, empl_root, chief_0, empl_1, chief_3, chief_1},
        chief_0: {hr_partner_0, empl_0},
        empl_0: {dismissed_0},
        empl_1: {hr_partner_1, empl_new_1, dismissed_1, chief_2, empl_2},
        chief_2: {hr_partner_2},
        chief_3: {hr_partner_3, empl_3},
    }

    for chief, employees in chief_to_employees_direct.items():
        assert check_subordination_for_all(chief, employees, staff_const.SUBORDINATION.DIRECT)

    chief_to_employees_indirect = {
        chief_root: {hr_partner_0, empl_0, dismissed_0, hr_partner_1, empl_new_1, dismissed_1,
                     chief_2, empl_2, hr_partner_3, empl_3, hr_partner_2},
    }
    for chief, employees in chief_to_employees_indirect.items():
        assert check_subordination_for_all(chief, employees, staff_const.SUBORDINATION.INDIRECT)

    all_subordinations_count = (sum(map(len, chief_to_employees_direct.values())) +
                                sum(map(len, chief_to_employees_indirect.values())))
    assert models.Subordination.objects.all().count() == all_subordinations_count

    # check HR model
    # Здесь предполагается, что hr-партнеры у уволенных сотрудников определяются
    # также как и у работающих, так как текущий синк работает так
    hr_partners_to_cared_persons = {
        hr_partner_root: {chief_root, empl_root, chief_0, hr_partner_0, empl_0, empl_1, hr_partner_1,
                          empl_new_1, dismissed_1, chief_2, empl_2, chief_3, hr_partner_3, empl_3,
                          dismissed_0, chief_1, hr_partner_2},
        hr_partner_0: {chief_0, empl_0, dismissed_0},
        hr_partner_1: {empl_1, empl_new_1, dismissed_1, chief_2, empl_2, chief_1},
        hr_partner_3: {chief_3, empl_3},
    }

    for hr, cared in hr_partners_to_cared_persons.items():
        assert check_hr_for_all(hr, cared, staff_const.STAFF_ROLE.HR.HR_PARTNER)

    all_hrs_connections_count = sum(map(len, hr_partners_to_cared_persons.values()))
    assert models.HR.objects.all().count() == all_hrs_connections_count

    assert not models.Person.objects.filter(login=to_del_by_sync.login).exists()


def create_structure_change_and_person_heads(dismissed_person, chiefs, change_date):
    structure_change = models.StaffStructureChange(date=change_date, staff_id=str(uuid.uuid4()))
    structure_change.save()
    person_heads = models.PersonHeads(
        structure_change=structure_change,
        person_id=dismissed_person.id,
        heads=','.join([str(c.id) for c in chiefs])
    )
    person_heads.save()


def check_hr_for_all(hr_person, cared_persons, hr_type):
    connections_number = models.HR.objects.filter(
        hr_person=hr_person,
        cared_person__in=cared_persons,
        type=hr_type,
    ).count()
    return len(cared_persons) == connections_number


def check_subordination_for_all(subject_person, object_persons, subordination_type):
    connections_number = models.Subordination.objects.filter(
        subject=subject_person,
        object__in=object_persons,
        type=subordination_type,
    ).count()
    return len(object_persons) == connections_number


def check_dep_role(person, department, role):
    roles = models.DepartmentRole.objects.filter(
        person=person,
        department=department,
        type=role,
    )
    return len(roles) == 1


def get_persons_to_department_id(dep_to_persons):
    persons_to_department_id = {}
    for dep, persons in dep_to_persons.items():
        for person in persons:
            persons_to_department_id[person.login] = dep.id
    return persons_to_department_id


def get_persons_data(persons, dismissed_persons):
    persons_data = []

    def create_person_dict(person, is_dismissed):
        return {
            'id': person.id,
            'login': person.login,
            'name': {'first': {'en': 'Firtsname', 'ru': 'Имя'},
                     'last': {'en': 'Lastname', 'ru': 'Фамилия'}},
            'location': {'office': {'city': {'name': {'en': 'Moscow', 'ru': 'Москва'}}}},
            'uid': person.uid,
            'official': {
                'is_dismissed': is_dismissed,
                'is_robot': False,
                'position': {'en': 'Software developer', 'ru': 'Разработчик'},
                'quit_at': person.quit_at
            },
            'work_email': 'work_email@yandex-team.ru',
            'language': {'ui': 'ru'},
            'environment': {'timezone': 'Europe/Moscow'},
        }

    for person in persons:
        persons_data.append(create_person_dict(person, False))

    for person in dismissed_persons:
        persons_data.append(create_person_dict(person, True))

    return persons_data
