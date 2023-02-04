# coding: utf-8

import pytest

from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import mock_roles, raw_make_role, assert_inconsistency

pytestmark = [pytest.mark.django_db, pytest.mark.robot, pytest.mark.streaming_roles]
TYPE_THEIR, TYPE_OUR = Inconsistency.TYPE_THEIR, Inconsistency.TYPE_OUR


def test_no_our_nor_their(simple_system, arda_users):
    with mock_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 0


def test_has_our_not_their(simple_system, arda_users):
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'})
    with mock_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert_inconsistency(
        Inconsistency.objects.get(),
        system=simple_system,
        user=arda_users.frodo,
        type=TYPE_OUR,
        path='/role/admin/',
        remote_fields=None,
        state='active'
    )


def test_no_our_has_their(simple_system, arda_users):
    roles = [
        {
            'login': 'frodo',
            'path': '/role/manager/'
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert_inconsistency(
        Inconsistency.objects.get(),
        system=simple_system,
        user=arda_users.frodo,
        type=TYPE_THEIR,
        path='/role/manager/',
        remote_fields=None,
        ident_type='path'
    )


def test_no_our_has_their_with_fields(simple_system, arda_users):
    roles = [
        {
            'login': 'frodo',
            'path': '/role/manager/',
            'fields': {'login': 'b@gg1n$'}
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert_inconsistency(
        Inconsistency.objects.get(),
        system=simple_system,
        user=arda_users.frodo,
        type=TYPE_THEIR,
        path='/role/manager/',
        remote_fields={'login': 'b@gg1n$'}
    )


def test_no_our_has_their_group_but_is_unaware(simple_system, arda_users, department_structure):
    roles = [
        {
            'group': department_structure.fellowship.external_id,
            'path': '/role/manager/',
            'fields': {'login': 'b@gg1n$'}
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    # неконсистентностей нет, так как система не group-aware
    assert Inconsistency.objects.count() == 0


def test_no_our_has_their_group_but_is_unaware_still_w_users(simple_system, arda_users, department_structure):
    roles = [
        {
            'group': department_structure.fellowship.external_id,
            'path': '/role/manager/',
            'fields': {'login': 'b@gg1n$'}
        },
        {
            'login': 'frodo',
            'path': '/role/admin/',
            'fields': {'login': 'BGNS'}
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    # неконсистентность только одна, про пользователя
    assert Inconsistency.objects.count() == 1
    assert_inconsistency(
        Inconsistency.objects.get(),
        system=simple_system,
        user=arda_users.frodo,
        type=TYPE_THEIR,
        path='/role/admin/',
        remote_fields={'login': 'BGNS'}
    )


def test_no_our_has_their_group_and_is_aware(aware_simple_system, arda_users, department_structure):
    roles = [
        {
            'group': department_structure.fellowship.external_id,
            'path': '/role/manager/',
            'fields': {'login': 'b@gg1n$'}
        }
    ]
    with mock_roles(aware_simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert_inconsistency(
        Inconsistency.objects.get(),
        system=aware_simple_system,
        group=department_structure.fellowship,
        type=TYPE_THEIR,
        path='/role/manager/',
        remote_fields={'login': 'b@gg1n$'}
    )


def test_duplicate_roles(simple_system, arda_users):
    """Проверим случай, когда система отдаёт нам абсолютно одинаковые роли"""
    roles = [
        {
            'login': 'frodo',
            'path': '/role/admin/',
            'fields': {'login': 'BGNS'}
        },
        {
            'login': 'frodo',
            'path': '/role/admin/',
            'fields': {'login': 'BGNS'}
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    ic = Inconsistency.objects.get()
    assert_inconsistency(ic, type=Inconsistency.TYPE_THEIR,
                         system=simple_system, path='/role/admin/', remote_fields={'login': 'BGNS'})


@pytest.mark.parametrize('inconsistency_policy', ['donottrust', 'donottrust_strict'])
def test_duplicate_roles_fields_does_not_matter(simple_system, arda_users, inconsistency_policy):
    """Проверим случай, когда система отдаёт роли, отличающиеся только fields_data,
    при этом роль – unresolvable для не-sox систем, например, неконсистентность логина
    Для sox-систем это будут разные роли, а для не-sox – одна и та же.
    """
    simple_system.inconsistency_policy = inconsistency_policy
    simple_system.save()

    roles = [
        {
            'login': 'tylerdurden',
            'path': '/role/admin/',
            'fields': {'login': 'tyler'},
        },
        {
            'login': 'tylerdurden',
            'path': '/role/admin/',
            'fields': {'login': 'joe'},
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    if inconsistency_policy == 'donottrust':
        assert Inconsistency.objects.count() == 1
        ic = Inconsistency.objects.get()
        assert_inconsistency(ic, type=Inconsistency.TYPE_UNKNOWN_USER,
                             system=simple_system, path='/role/admin/', remote_fields={'login': 'tyler'})
    else:
        assert Inconsistency.objects.count() == 2
        ic1, ic2 = Inconsistency.objects.order_by('pk')
        assert_inconsistency(ic1, type=Inconsistency.TYPE_UNKNOWN_USER,
                             system=simple_system, path='/role/admin/', remote_fields={'login': 'tyler'})
        assert_inconsistency(ic2, type=Inconsistency.TYPE_UNKNOWN_USER,
                             system=simple_system, path='/role/admin/', remote_fields={'login': 'joe'})


def test_several_unknown_roles(simple_system, arda_users):
    """Проверим случай, когда система отдаёт разные роли, но мы ничего не знаем про пользователя"""

    roles = [
        {
            'login': 'johnsnow',
            'path': '/role/admin/',
            'fields': {'login': 'BGNS'}
        },
        {
            'login': 'johnsnow',
            'path': '/role/admin/',
            'fields': {'login': 'BGNS'}
        }
    ]
    with mock_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    ic = Inconsistency.objects.get()
    assert_inconsistency(ic, type=Inconsistency.TYPE_UNKNOWN_USER,
                         system=simple_system, path='/role/admin/', remote_username='johnsnow')
