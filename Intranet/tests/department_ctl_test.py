from datetime import date

import pytest

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import (
    StaffFactory,
    DepartmentKindFactory,
    DepartmentFactory,
    GroupFactory,
)
from staff.departments.controllers.department import DepartmentCtl
from staff.departments.controllers.exceptions import DepartmentCtlError
from staff.departments.models import Department, DepartmentStaff, DepartmentRoles


def reinit(ctl):
    return DepartmentCtl(
        Department.objects.get(id=ctl.id),
        author_user=ctl._author,
    )


@pytest.fixture
def department(kinds):
    department = DepartmentFactory(
        name='dep_name',
        name_en='dep_name_en',
        parent=None,
        kind=kinds[1],
    )

    root_group = GroupFactory(
        name='__departments__',
        department=None,
        url='__departments__',
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )

    GroupFactory(
        name='dep_name',
        url=department.url,
        department=department,
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
        parent=root_group,
    )

    return department


@pytest.fixture
def author(department):
    return StaffFactory(
        login='author_login',
        department=department
    )


@pytest.fixture
def person1(department):
    return StaffFactory(
        login='person1_login',
        department=department
    )


@pytest.fixture
def person2(department):
    return StaffFactory(
        login='person2_login',
        department=department
    )


# todo: тесты на отправку писем и создание записей в audit.models.Log

def test_create_orphan(author):

    kind_id = DepartmentKindFactory().id
    department_code = 'department_code'
    DepartmentCtl.create(
        code=department_code,
        kind_id=kind_id,
        author_user=author.user,
        parent=None,
        oebs_structure_date=date.today(),
    )
    assert Department.objects.exclude(id=author.department.id).count() == 1
    db_obj = Department.objects.get(code=department_code)
    assert db_obj.parent is None
    assert db_obj.group is not None
    assert db_obj.url == db_obj.code == department_code
    assert db_obj.intranet_status == 1
    # check group
    assert db_obj.group.type == GROUP_TYPE_CHOICES.DEPARTMENT
    assert db_obj.group.parent is None
    equal_fields = (
        'name', 'parent', 'created_at', 'modified_at', 'intranet_status',
        'url', 'code', 'position', 'native_lang',
    )
    for field in equal_fields:
        assert getattr(db_obj, field) == getattr(db_obj.group, field)


def test_create_nested(author, kinds):

    root_kind_id = DepartmentKindFactory().id
    department_code = 'department_code'
    DepartmentCtl.create(
        code=department_code,
        kind_id=root_kind_id,
        author_user=author.user,
        parent=author.department,
        oebs_structure_date=date.today(),
    )
    assert Department.objects.exclude(id=author.department.id).count() == 1
    db_obj = Department.objects.get(code=department_code)
    assert db_obj.kind_id == root_kind_id
    assert db_obj.parent == author.department
    assert db_obj.group is not None
    assert db_obj.code == department_code
    assert db_obj.url == db_obj.parent.url + '_' + department_code

    assert db_obj.intranet_status == 1
    # check group
    assert db_obj.group.type == GROUP_TYPE_CHOICES.DEPARTMENT
    assert db_obj.group.parent == db_obj.parent.group
    equal_fields = (
        'name', 'created_at', 'modified_at', 'intranet_status',
        'url', 'code', 'position', 'native_lang',
    )
    for field in equal_fields:
        assert getattr(db_obj, field) == getattr(db_obj.group, field)


TEST_DEPARTMENT_DATA = [
    # attr, ctl_value, db_value, error_key if not correct else: None
    ('name', 'test_name', 'test_name', None),
    ('name_en', 'test_name_en', 'test_name_en', None),
    ('short_name', 'test_short_name', 'test_short_name', None),
    ('short_name_en', 'test_short_name_en', 'test_short_name_en', None),

    ('bg_color', '#FF0077', '#FF0077', None),
    ('fg_color', '#334455', '#334455', None),

    ('wiki_page', 'test_wiki_page', 'test_wiki_page', None),
    ('clubs', ['test_club1', 'test_club2'], 'test_club1,test_club2', None),
    ('maillists', ['test_ml1', 'test_ml2', 'ml3'], 'test_ml1,test_ml2,ml3', None),

    ('code', 'test_code', 'test_code', None),
    ('position', 112, 112, None),
    ('description', 'Test description', 'Test description', None),
    ('description_en', 'Test English description', 'Test English description', None),
    ('native_lang', 'en', 'en', None),
    ('url', 'test_url', 'test_url', None),
]


@pytest.mark.parametrize('attr,ctl_value,db_value,error_key', TEST_DEPARTMENT_DATA)
def test_simple_setattr(department, author, attr, ctl_value, db_value, error_key):
    # todo: test illegal values using error_key param
    dep_ctl = DepartmentCtl(department, author_user=author.user)
    assert dep_ctl.id == department.id
    setattr(dep_ctl, attr, ctl_value)

    db_instance = Department.objects.get(id=department.id)
    assert getattr(db_instance, attr) != db_value
    assert getattr(DepartmentCtl(db_instance), attr) != ctl_value
    assert getattr(dep_ctl, attr) == ctl_value

    dep_ctl.save()

    db_instance = Department.objects.get(id=department.id)
    assert getattr(db_instance, attr) == db_value
    assert getattr(DepartmentCtl(db_instance), attr) == ctl_value


def test_set_chief(department, person1, author, settings):
    settings.DEBUG = True
    ctl = DepartmentCtl(department, author_user=author.user)
    assert ctl.chief is None

    ctl.chief = person1
    assert ctl.chief == person1
    assert DepartmentStaff.objects.filter(
        department=department,
        staff=person1,
        role_id=DepartmentRoles.CHIEF.value,
    ).count() == 0

    ctl.save()
    assert DepartmentStaff.objects.filter(
        department=department,
        staff=person1,
        role_id=DepartmentRoles.CHIEF.value,
    ).count() == 1
    ctl = reinit(ctl)
    assert ctl.chief == person1

    ctl.chief = None
    ctl.save()
    assert DepartmentStaff.objects.filter(
        department=department,
        staff=person1,
        role_id=DepartmentRoles.CHIEF.value,
    ).count() == 0

    ctl = reinit(ctl)
    assert ctl.chief is None


def test_set_deputies(department, person1, person2, author):
    ctl = DepartmentCtl(department, author_user=author.user)
    assert ctl.deputies == []

    ctl.deputies = [person1, person2]
    assert ctl.deputies == [person1, person2]
    assert DepartmentStaff.objects.filter(
        department=department,
        role_id=DepartmentRoles.DEPUTY.value,
    ).count() == 0

    ctl.save()
    assert DepartmentStaff.objects.filter(
        department=department,
        role_id=DepartmentRoles.DEPUTY.value,
    ).count() == 2

    ctl = reinit(ctl)
    assert set(ctl.deputies) == {person1, person2}

    ctl.deputies = [person2, author]
    ctl.save()
    assert DepartmentStaff.objects.filter(
        department=department,
        role_id=DepartmentRoles.DEPUTY.value,
    ).count() == 2

    ctl = reinit(ctl)
    assert set(ctl.deputies) == {person2, author}

    ctl.deputies = []
    ctl.save()
    assert DepartmentStaff.objects.filter(
        department=department,
        role_id=DepartmentRoles.DEPUTY.value,
    ).count() == 0

    assert reinit(ctl).deputies == []


def test_set_parent(department, author, kinds):
    child_ctl = DepartmentCtl.create(
        code='child',
        kind_id=kinds[3].id,
        author_user=author.user,
        parent=department,
        oebs_structure_date=date.today(),
    ).save()
    sub_child_ctl = DepartmentCtl.create(
        code='sub_child',
        kind_id=kinds[5].id,
        author_user=author.user,
        parent=child_ctl.instance,
        oebs_structure_date=date.today(),
    ).save()

    assert sub_child_ctl.parent == child_ctl.instance
    with pytest.raises(DepartmentCtlError):
        child_ctl.parent = sub_child_ctl.instance
        child_ctl.save()

    child_ctl = reinit(child_ctl)
    assert child_ctl.parent == department

    sub_child_ctl.parent = department
    sub_child_ctl.save()

    sub_child_ctl = reinit(sub_child_ctl)
    assert sub_child_ctl.parent == department
    assert sub_child_ctl.group.parent == department.group


def test_set_name(department, author):
    assert department.name == department.group.name
    ctl = DepartmentCtl(department, author_user=author.user)
    ctl.name = 'new name'
    ctl.save()

    assert Department.objects.get(url=department.url).group.name == 'new name'


def test_delete(department, author):
    ctl = DepartmentCtl(department, author_user=author.user)

    with pytest.raises(DepartmentCtlError):
        ctl.delete()

    author.department = DepartmentFactory(
        name='another dep',
        code='other',
        tree_id=ctl.tree_id
    )
    author.save()

    ctl.delete()
    dep = Department.objects.get(id=ctl.id)

    assert dep.intranet_status == 0
    assert dep.group.intranet_status == 0


def test_delete_nested_will_not_remove_chief_role(author, kinds):
    parent_dep_ctl = DepartmentCtl.create(
        code='parent_dep',
        kind_id=kinds[2].id,
        author_user=author.user,
        oebs_structure_date=date.today(),
    )
    parent_dep_ctl.chief = author
    parent_dep_ctl.save()

    child_dep_ctl = DepartmentCtl.create(
        code='child_dep',
        kind_id=kinds[3].id,
        author_user=author.user,
        parent=parent_dep_ctl.instance,
        oebs_structure_date=date.today(),
    )
    child_dep_ctl.chief = author
    child_dep_ctl.save()

    child_dep_ctl.delete()

    parent_dep_ctl = DepartmentCtl(parent_dep_ctl.instance)
    assert parent_dep_ctl.chief


@pytest.mark.django_db
def test_department_version(department):
    department.version = 1
    department.save()
    assert department.version == 2
    department.save()
    department.save()
    assert department.version == 4
