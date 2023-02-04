# coding: utf-8


import pytest

from idm.core.workflow.common.subject import subjectify
from idm.tests.utils import setify_changes, create_user, create_group_structure, ensure_group_roots
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group
from idm.services.models import Service

pytestmark = [pytest.mark.django_db]


def test_responsibles_for_service_scope():
    _, service_root, _ = ensure_group_roots()

    alice = create_user('alice')
    create_group_structure(
        {
            'external_id': 1,
            'slug': 'svc_1',
            'name': 'service',
            'members': ['alice', 'bob'],
            'responsible': [('alice', 'head')],
            'children': [
                {
                    'external_id': 2,
                    'slug': 'svc_2',
                    'name': 'service_scope',
                    'members': ['bob']
                }
            ]
        },
        service_root,
        GROUP_TYPES.SERVICE,
    )

    subject = subjectify(Group.objects.get(slug='svc_2'))
    assert set(subject.get_responsibles()) == {alice}  # Возвращаются ответственные за сервис


def test_group_get_changes_for_new_root_nothing_happens(services, groups):
    (group1, group2, group6, group8, group9, group10,
     group1m, group2s, group6m, group6s, group8s, group9a, group10m, group10s) = groups
    changes = group2.get_changes_for_new_root(group10)
    setify_changes(changes)
    assert changes == {}


def test_group_get_changes_for_new_root(services, groups):
    (group1, group2, group6, group8, group9, group10,
     group1m, group2s, group6m, group6s, group8s, group9a, group10m, group10s) = groups
    changes = group6.get_changes_for_new_root(group10)
    setify_changes(changes)
    assert changes == {
        group6: {
            'obtained': {group10, group8, group1},
            'lost': {group2},
            'ancestors': {group6},
        },
        group6m: {
            'obtained': {group10m, group1m},
            'lost': set(),
            'ancestors': {group6, group6m},
        },
        group6s: {
            'obtained': {group10s, group8s},
            'lost': {group2s},
            'ancestors': {group6, group6s},
        },
    }


@pytest.fixture
def services():
    top_service = Service.objects.create(
        slug='',
        external_id=0,
        membership_inheritance=True,
        parent=None,
    )
    service1 = Service.objects.create(
        slug='1',
        external_id=1,
        membership_inheritance=True,
        parent=top_service,
    )
    service2 = Service.objects.create(
        slug='2',
        external_id=2,
        membership_inheritance=False,
        parent=service1,
    )
    service6 = Service.objects.create(
        slug='6',
        external_id=6,
        membership_inheritance=True,
        parent=service2,
    )
    service8 = Service.objects.create(
        slug='8',
        external_id=8,
        membership_inheritance=True,
        parent=service1,
    )
    service9 = Service.objects.create(
        slug='9',
        external_id=9,
        membership_inheritance=False,
        parent=service8,
    )
    service10 = Service.objects.create(
        slug='10',
        external_id=10,
        membership_inheritance=True,
        parent=service8,
    )
    return [service1, service2, service6, service8, service9, service10]


@pytest.fixture
def groups():
    group0 = Group.objects.create(
        type='service',
        slug='',
        name='',
        external_id=0,
        parent=None
    )
    group1 = Group.objects.create(
        slug='svc_1',
        type='service',
        name='group1',
        external_id=1,
        parent=group0,
    )
    group2 = Group.objects.create(
        slug='svc_2',
        type='service',
        external_id=2,
        name='group2',
        parent=group0,
    )
    group6 = Group.objects.create(
        slug='svc_6',
        external_id=6,
        type='service',
        parent=group0,
        name='group6',
    )
    group8 = Group.objects.create(
        slug='svc_8',
        name='group8',
        type='service',
        parent=group0,
        external_id=8,
    )
    group9 = Group.objects.create(
        parent=group0,
        type='service',
        name='group9',
        slug='svc_9',
        external_id=9,
    )
    group10 = Group.objects.create(
        name='group10',
        parent=group0,
        type='service',
        external_id=10,
        slug='svc_10',
    )
    group1m = Group.objects.create(
        slug='svc_1_manage',
        name='group1m',
        external_id=11,
        parent=group1,
    )
    group2s = Group.objects.create(
        slug='svc_2_support',
        external_id=22,
        name='group2s',
        parent=group2,
    )
    group6m = Group.objects.create(
        external_id=16,
        slug='svc_6_manage',
        name='group6m',
        parent=group6,
    )
    group6s = Group.objects.create(
        slug='svc_6_support',
        external_id=26,
        name='group6s',
        parent=group6,
    )
    group8s = Group.objects.create(
        slug='svc_8_support',
        name='group8s',
        external_id=28,
        parent=group8,
    )
    group9a = Group.objects.create(
        slug='svc_9_admin',
        name='group9a',
        external_id=39,
        parent=group9,
    )
    group10m = Group.objects.create(
        slug='svc_10_manage',
        external_id=110,
        name='group10m',
        parent=group10,
    )
    group10s = Group.objects.create(
        slug='svc_10_support',
        external_id=210,
        name='group10s',
        parent=group10,
    )
    return [group1, group2, group6, group8, group9, group10,
            group1m, group2s, group6m, group6s, group8s, group9a, group10m, group10s]
