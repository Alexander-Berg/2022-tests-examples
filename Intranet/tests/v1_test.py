from datetime import datetime
from json import loads

import pytest

from staff.person.models import Staff
from staff.groups.models import Group

from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    GroupFactory,
    DepartmentStaffFactory,
)


def parse_datetime(dt_str):
    return datetime.strptime(dt_str, "%Y-%m-%dT%H:%M:%S")


def create_root_groups():
    r = []
    kw = {'parent': None, 'service_id': None, 'department': None}
    kw.update(Group.objects.intranet_root_kwargs)
    r.append(GroupFactory(**kw))
    kw.update(Group.objects.departments_root_kwargs)
    r.append(GroupFactory(**kw))
    kw.update(Group.objects.services_root_kwargs)
    r.append(GroupFactory(**kw))
    return r


def prepare_departments():
    d1 = DepartmentFactory(name='Яндекс', parent=None)
    d2 = DepartmentFactory(name='Департамент разработки', parent=d1)
    d3 = DepartmentFactory(name='Отдел убийств', parent=d2)
    DepartmentFactory(name='Служба не дружба', parent=d3)
    DepartmentFactory(name='Департамент переработки', parent=d1)
    thasonic = StaffFactory(login='thasonic', department=d3)
    StaffFactory(login='kolomeetz', department=d2)
    DepartmentStaffFactory(department=d3, staff=thasonic, role_id='C')


@pytest.fixture
def group_deps_data():
    create_root_groups()
    prepare_departments()


@pytest.mark.django_db
def test_all_users(group_deps_data, client):
    # test getting all
    data = loads(client.get('/center/api/v1/users.json').content.decode('utf-8'))
    assert len(data) == Staff.objects.count()
    # test getting by last_modified
    last_modified: datetime = Staff.objects.order_by('-modified_at').first().modified_at
    last_modified = last_modified.replace(hour=0, minute=0, microsecond=0)
    data = loads(client.get(
        '/center/api/v1/users.json?last_modified=%s&fields=modified_at' %
        last_modified.strftime('%Y-%m-%d')
    ).content.decode('utf-8'))
    for item in data:
        assert parse_datetime(item['modified_at']) >= last_modified
    # test is_dismissed=0
    data = loads(client.get(
        '/center/api/v1/users.json?last_modified=%s&include_dismissed=0&fields=is_dismissed' %
        last_modified.strftime('%Y-%m-%d')
    ).content.decode('utf-8'))
    for item in data:
        assert not item['is_dismissed']


@pytest.mark.django_db
def test_departments(group_deps_data, client):
    # test getting all
    data = loads(client.get('/center/api/v1/departments.json').content.decode('utf-8'))
    assert data[0]['name'] == 'Яндекс'
    assert data[0]['_departments'][0]['name'] == 'Департамент разработки'
    # test getting chiefs
    data = loads(client.get('/center/api/v1/departments.json?fields=id|chief').content.decode('utf-8'))
    assert data[0]['_departments'][0]['_departments'][0]['chief'] == Staff.objects.get(login='thasonic').id
