import random

import pytest
import mock
from functools import partial

from waffle.models import Switch

from staff.departments.controllers.value_streams_rollup import ValueStreamsRollupController
from staff.departments.models import Department, DepartmentStaff, ValuestreamRoles
from staff.departments.tests.factories import HRProductFactory

from staff.lib.testing import (
    ServiceGroupFactory,
    ValueStreamFactory,
    StaffFactory,
    DepartmentStaffFactory,
)


@pytest.fixture
def vs_services(db):
    """Дерево value stream`ов, хранящихся в сервисных группах"""
    # Value Stream`ное дерево (не mptt, а по смыслу vs через parent_service_id):
    #  VS1          VS2
    #  |    \        |
    # VS11  VS12    VS21
    mptt_root = ServiceGroupFactory(url='__services__')

    service = partial(ServiceGroupFactory, parent=mptt_root)

    service(url='svc_s1', service_id=12345)
    service(url='svc_s2', service_id=54321)

    vs_services = {}
    vs_services['vs1'] = service(url='vs1', service_id=1, parent_service_id=None)
    vs_services['vs2'] = service(url='vs2', service_id=2, parent_service_id=None)
    vs_services['vs1_vs11'] = service(
        url='vs1_vs11',
        service_id=11,
        parent_service_id=vs_services['vs1'].service_id,
    )
    vs_services['vs1_vs12'] = service(
        url='vs1_vs12',
        service_id=12,
        parent_service_id=vs_services['vs1'].service_id,
    )
    vs_services['vs2_vs21'] = service(
        url='vs2_vs21',
        service_id=21,
        parent_service_id=vs_services['vs2'].service_id,
    )

    service(
        url='vs2_vs21_someservice',
        service_id=98765,
        parent_service_id=vs_services['vs2_vs21'].service_id,
    )

    return vs_services


@pytest.fixture
def abc_members_empty_mock():
    return []


@pytest.fixture
def abc_members_mock(vs_services):
    chiefs = {
        'vs1-1': StaffFactory(login='vs1-1'),
        'vs1-2': StaffFactory(login='vs1-2'),
        'vs11-1': StaffFactory(login='vs11-1'),
        'vs21-1': StaffFactory(login='vs21-1'),
    }
    abc_answer = [
        {
            'person': {'login': chiefs['vs1-1'].login},
            'service': {'id': vs_services['vs1'].service_id},
            'role': {'code': ValuestreamRoles.HEAD.value}
        },
        {
            'person': {'login': chiefs['vs1-2'].login},
            'service': {'id': vs_services['vs1'].service_id},
            'role': {'code': ValuestreamRoles.HEAD.value}
        },
        {
            'person': {'login': chiefs['vs11-1'].login},
            'service': {'id': vs_services['vs1_vs11'].service_id},
            'role': {'code': ValuestreamRoles.HEAD.value}
        },
        {
            'person': {'login': chiefs['vs21-1'].login},
            'service': {'id': vs_services['vs2_vs21'].service_id},
            'role': {'code': ValuestreamRoles.HEAD.value}
        },
    ]
    return {
        'chiefs': chiefs,
        'mock': [
            (record['person']['login'], record['service']['id'])
            for record in abc_answer
        ]
    }


@mock.patch.object(ValueStreamsRollupController, '_get_services_roles', lambda *args, **kwargs: [])
def test_creating_new_value_streams_with_parents(vs_services, abc_members_empty_mock):
    # arrange
    Switch.objects.get_or_create(name='enable_vs_rollup_from_groups', active=True)
    vs_in_hr_products = ['vs1_vs11', 'vs1_vs12', 'vs2_vs21']
    for vs_url in vs_in_hr_products:
        HRProductFactory(service_id=vs_services[vs_url].service_id)

    # act:
    ValueStreamsRollupController().rollup()

    # assert:
    assert Department.valuestreams.count() == 5
    assert DepartmentStaff.objects.count() == 0

    for vs_url, vs_group in vs_services.items():
        vs = Department.valuestreams.get(url=vs_url)
        assert vs.url == vs_group.url
        assert vs.code == vs_group.code
        assert vs.name == vs_group.name
        assert vs.name_en == vs_group.name
        assert vs.description == vs_group.description
        assert vs.description_en == vs_group.description
        assert vs.intranet_status == vs_group.intranet_status

        assert not ValueStreamsRollupController().need_update(vs_group, vs)


@mock.patch.object(ValueStreamsRollupController, '_get_services_roles', lambda *args, **kwargs: [])
def test_deleting_old_value_streams(vs_services, abc_members_empty_mock):
    # arrange
    Switch.objects.get_or_create(name='enable_vs_rollup_from_groups', active=True)
    _create_hr_products(vs_services)

    # arrange:
    old_vs_1 = ValueStreamFactory(intranet_status=1, parent=None, code='oldvs1', url='oldvs1')
    old_vs_2 = ValueStreamFactory(intranet_status=1, parent=old_vs_1, code='oldvs2', url='oldvs2')

    # act:
    ValueStreamsRollupController().rollup()

    # assert:
    assert Department.valuestreams.filter(intranet_status=1).count() == 5
    assert Department.valuestreams.filter(intranet_status=0).count() == 2

    assert Department.valuestreams.get(url=old_vs_1.url).intranet_status == 0
    assert Department.valuestreams.get(url=old_vs_2.url).intranet_status == 0


@mock.patch.object(ValueStreamsRollupController, '_get_services_roles', lambda *args, **kwargs: [])
def test_updating_old_value_streams(vs_services, abc_members_empty_mock):
    # arrange
    Switch.objects.get_or_create(name='enable_vs_rollup_from_groups', active=True)
    _create_hr_products(vs_services)

    to_update_vs_1 = ValueStreamFactory(intranet_status=1, parent=None, code='vs1', url='vs1')
    to_update_vs_2 = ValueStreamFactory(intranet_status=1, parent=to_update_vs_1, code='vs2', url='vs2')
    ValueStreamFactory(intranet_status=1, parent=to_update_vs_1, code='oldvs3', url='oldvs3')

    # act:
    ValueStreamsRollupController().rollup()

    # assert:
    assert Department.valuestreams.filter(intranet_status=1).count() == 5
    assert Department.valuestreams.filter(intranet_status=0).count() == 1  # oldvs3

    for vs in [to_update_vs_1, to_update_vs_2]:
        vs = Department.valuestreams.get(id=vs.id)
        vs_group = vs_services[vs.url]

        assert vs.url == vs_group.url
        assert vs.code == vs_group.code
        assert vs.name == vs_group.name
        assert vs.name_en == vs_group.name
        assert vs.description == vs_group.description
        assert vs.description_en == vs_group.description
        assert vs.intranet_status == vs_group.intranet_status

        assert not ValueStreamsRollupController().need_update(vs_group, vs)


def test_creating_value_stream_chiefs(vs_services, abc_members_mock):
    # arrange
    Switch.objects.get_or_create(name='enable_vs_rollup_from_groups', active=True)
    _create_hr_products(vs_services)

    # act:
    with mock.patch.object(
        ValueStreamsRollupController,
        '_get_services_roles',
        lambda *args, **kwargs: abc_members_mock['mock'],
    ):
        ValueStreamsRollupController().rollup()

    # assert:
    assert DepartmentStaff.objects.filter(role_id=ValuestreamRoles.HEAD.value).count() == 4
    assert DepartmentStaff.objects.filter(
        staff_id=abc_members_mock['chiefs']['vs1-1'],
        department__url=vs_services['vs1'].url,
        role_id=ValuestreamRoles.HEAD.value,
    ).exists()
    assert DepartmentStaff.objects.filter(
        staff_id=abc_members_mock['chiefs']['vs1-2'],
        department__url=vs_services['vs1'].url,
        role_id=ValuestreamRoles.HEAD.value,
    ).exists()
    assert DepartmentStaff.objects.filter(
        staff_id=abc_members_mock['chiefs']['vs11-1'],
        department__url=vs_services['vs1_vs11'].url,
        role_id=ValuestreamRoles.HEAD.value,
    ).exists()
    assert DepartmentStaff.objects.filter(
        staff_id=abc_members_mock['chiefs']['vs21-1'],
        department__url=vs_services['vs2_vs21'].url,
        role_id=ValuestreamRoles.HEAD.value,
    ).exists()


def test_deleting_value_stream_chiefs(vs_services, abc_members_mock):
    # arrange:
    Switch.objects.get_or_create(name='enable_vs_rollup_from_groups', active=True)
    _create_hr_products(vs_services)

    with mock.patch.object(
        ValueStreamsRollupController,
        '_get_services_roles',
        lambda *args, **kwargs: abc_members_mock['mock'],
    ):
        ValueStreamsRollupController().rollup()

    ds1 = DepartmentStaffFactory(
        department=Department.valuestreams.get(url='vs1_vs11'),
        staff=StaffFactory(login='stranger1'),
        role_id=ValuestreamRoles.HEAD.value,
    )
    ds2 = DepartmentStaffFactory(
        department=Department.valuestreams.get(url='vs2'),
        staff=StaffFactory(login='stranger2'),
        role_id=ValuestreamRoles.HEAD.value,
    )

    # act:
    with mock.patch.object(
        ValueStreamsRollupController,
        '_get_services_roles',
        lambda *args, **kwargs: abc_members_mock['mock'],
    ):
        ValueStreamsRollupController().rollup()

    # assert:
    assert DepartmentStaff.objects.filter(role_id=ValuestreamRoles.HEAD.value).count() == 4
    assert not DepartmentStaff.objects.filter(id=ds1.id).exists()
    assert not DepartmentStaff.objects.filter(id=ds2.id).exists()


def _create_hr_products(vs_services):
    vs_in_hr_products = ['vs1', 'vs1_vs11', 'vs1_vs12', 'vs2_vs21', 'vs2']
    for vs_url in vs_in_hr_products:
        HRProductFactory(service_id=vs_services[vs_url].service_id)


@pytest.mark.django_db
@pytest.mark.parametrize(
    'name, name_en, expected_name_en',
    [
        ('name', 'name_en', 'name_en'),
        ('name', None, 'name'),
    ],
)
@mock.patch.object(ValueStreamsRollupController, '_get_services_roles', lambda *args, **kwargs: [])
def test_rollup_value_streams_names(name, name_en, expected_name_en, abc_members_empty_mock):
    Switch.objects.get_or_create(name='enable_vs_rollup_from_groups', active=True)
    mptt_root = ServiceGroupFactory(url='__services__')
    service_group = ServiceGroupFactory(
        service_id=random.randint(10, 432213),
        name=name,
        name_en=name_en,
        parent=mptt_root,
    )
    HRProductFactory(service_id=service_group.service_id)

    ValueStreamsRollupController().rollup()

    vs = Department.valuestreams.get(url=service_group.url)
    assert vs.url == service_group.url
    assert vs.code == service_group.code
    assert vs.name == service_group.name
    assert vs.name_en == expected_name_en
    assert vs.description == service_group.description
    assert vs.description_en == service_group.description
    assert vs.intranet_status == service_group.intranet_status

    assert not ValueStreamsRollupController().need_update(service_group, vs)
