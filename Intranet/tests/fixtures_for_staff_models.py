# coding: utf-8
import itertools
import uuid
import random
import datetime
import string
from functools import partial
from copy import deepcopy

import pytest

from django.conf import settings

from review.staff import models as staff_models
from review.staff import const as staff_const


@pytest.fixture
def fetch_root_departments_path(db):
    """
    Фикстура нужна для того, чтобы  выполнить запрос за Lazy-объёктом
    с путями для подразделений и не ходить за ним повторно в тестах
    и при этом иметь актуальные значения в каждом тесте
    """
    def fetcher():
        staff_const.ROOT_DEPARTMENT_PATHS = staff_const.get_root_department_paths()
        return staff_const.ROOT_DEPARTMENT_PATHS
    return fetcher


@pytest.fixture
def department_root_builder(db):
    def builder(**kwargs):
        suffix = str(uuid.uuid4())[:10]
        params = {
            'slug': 'slug' + suffix,
        }
        params['name_ru'] = params['name_en'] = params['slug']
        params.update(kwargs)
        return staff_models.Department.add_root(**params)
    return builder


@pytest.fixture
def department_child_builder(db):
    def builder(parent, **kwargs):
        suffix = str(uuid.uuid4())[:10]
        params = {
            'slug': 'slug' + suffix,
        }
        params.update(kwargs)
        if 'name' not in params:
            params['name_ru'] = params['name_en'] = params['slug']
        return parent.add_child(**params)
    return builder


@pytest.fixture
def department_root(department_root_builder):
    return department_root_builder()


@pytest.fixture
def person_builder(db, department_root_builder):
    def builder(**kwargs):
        params = get_person_default_params()
        params.update(kwargs)
        if 'department' not in params:
            params['department'] = department_root_builder()
        person = staff_models.Person.objects.create(**params)
        sync_subordination_with_structure()
        return person
    return builder


@pytest.fixture
def person_builder_bulk(db, department_root_builder):
    def builder(**kwargs):
        count = kwargs.pop('_count', 1)
        models = []
        for _ in range(count):
            params = get_person_default_params()
            params.update(kwargs)
            if 'department' not in params:
                params['department'] = department_root_builder()
            models.append(staff_models.Person(**params))
        persons = staff_models.Person.objects.bulk_create(models)
        sync_subordination_with_structure()
        return persons
    return builder


person_uid_gen = itertools.count()


def get_person_default_params():
    person_id = next(person_uid_gen)
    login = 'employee' + str(person_id)
    return {
        'id': person_id,
        'login': login,
        'first_name_ru': 'Иван' + str(uuid.uuid4())[0],
        'last_name_ru': 'Петров' + str(uuid.uuid4())[0],
        'first_name_en': 'Ivan' + str(uuid.uuid4())[0],
        'last_name_en': 'Petrov' + str(uuid.uuid4())[0],
        'uid': random.randint(10000000, 20000000),
        'join_at': '2015-01-01',
        'work_email': login + '@yandex-team.ru',
    }


@pytest.fixture
def person(person_builder):
    return person_builder()


@pytest.fixture(autouse=True)
def test_person(person_builder):
    return person_builder(login=settings.YAUTH_DEV_USER_LOGIN)


@pytest.fixture
def department_role_builder(db, person_builder, department_root_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'person' not in params:
            params['person'] = person_builder()
        if 'department' not in params:
            params['department'] = params['person'].department
        role = staff_models.DepartmentRole.objects.create(**params)
        sync_subordination_with_structure()
        return role
    return builder


@pytest.fixture
def department_role_head(department_role_builder):
    return department_role_builder(
        type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD,
    )


@pytest.fixture
def department_head_role_builder(department_role_builder):
    return partial(department_role_builder, type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD)


def sync_subordination_with_structure():
    """
    Расплата за денормалзицию.
    """
    staff_models.Subordination.objects.all().delete()
    from review.staff import logic
    for person in staff_models.Person.objects.all():
        for position, head in enumerate(logic.get_person_heads(person)):
            if head == person:
                continue
            if position == 0:
                type = staff_const.SUBORDINATION.DIRECT
            else:
                type = staff_const.SUBORDINATION.INDIRECT
            staff_models.Subordination.objects.create(
                subject=head,
                object=person,
                position=position,
                type=type,
            )
        staff_models.HR.objects.bulk_create(
            staff_models.HR(
                hr_person=hr,
                cared_person=person,
                type=type,
            )
            for hr, type in logic.get_person_hrs(person)
        )


def rand_string(length=8):
    return ''.join(random.choice(string.ascii_lowercase) for _ in range(length))


@pytest.fixture
def staff_structure_change_builder(db):
    def builder(**kwargs):
        kwargs.setdefault('date', datetime.date.today())
        kwargs.setdefault('staff_id', rand_string())
        return staff_models.StaffStructureChange.objects.create(**kwargs)
    return builder


@pytest.fixture
def staff_structure_builder(person_builder, department_root_builder):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'department' not in params:
            params['department'] = department_root_builder()
        if 'persons' not in params:
            params['persons'] = [person_builder(department=params['department']) for _ in range(20)]
        if 'date' not in params:
            params['date'] = datetime.date(
                year=random.randint(1997, 2017),
                month=random.randint(1, 12),
                day=random.randint(1, 28),
            )
        if 'staff_id' not in params:
            params['staff_id'] = str(uuid.uuid4())[:10]
        if 'heads_chain' not in params:
            params['heads_chain'] = [person_builder() for _ in range(5)]
        params['structure_change'] = _create_staff_structure_change(
            date=params['date'],
            staff_id=params['staff_id'],
            persons=params['persons'],
            heads_chain=params['heads_chain'],
        )
        return params
    return builder


@pytest.fixture
def hr_builder(person_builder):
    def builder(**kwargs):
        params = deepcopy(kwargs)
        if 'hr_person' not in kwargs:
            kwargs['hr_person'] = person_builder()
        if 'cared_person' not in kwargs:
            kwargs['cared_person'] = person_builder()
        kwargs.setdefault('type', staff_const.STAFF_ROLE.HR.HR_ANALYST)
        return staff_models.HR.objects.create(**params)
    return builder


def _create_staff_structure_change(date, staff_id, persons, heads_chain):
    heads = ','.join(str(h.id) for h in heads_chain)

    structure_change = staff_models.StaffStructureChange.objects.create(
        date=date,
        staff_id=staff_id,
    )
    if heads:
        staff_models.PersonHeads.objects.bulk_create(
            staff_models.PersonHeads(
                structure_change=structure_change,
                person_id=person.id,
                heads=heads
            )
            for person in persons
        )
    return structure_change
