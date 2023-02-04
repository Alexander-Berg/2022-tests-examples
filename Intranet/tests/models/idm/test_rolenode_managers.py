# coding: utf-8


import pytest

from django.db import IntegrityError
from django.utils import timezone

from idm.core.models import RoleNode
from idm.framework.backend.upsert import ConflictAction
from idm.tests.utils import days_from_now, compare_time

pytestmark = pytest.mark.django_db


def test_insert_object(simple_system):
    parent = RoleNode.objects.get(slug_path='/role/')
    count = RoleNode.objects.count()

    node = RoleNode(system_id=simple_system.pk, parent_id=parent.pk, state='active', slug='1', name='Hi')
    node.calc_fields()
    node = RoleNode.objects.insert_object(node, send_save_signals=True)

    assert compare_time(node.created_at, timezone.now())
    assert compare_time(node.updated_at, timezone.now())
    assert RoleNode.objects.count() == count + 1
    assert node.name == 'Hi'


def test_upsert_node_with_default_queryset(simple_system):
    """Тест, важный не только для RoleNode, а вообще для всех объектов, которые хотят делать UPSERT'ы"""
    parent = RoleNode.objects.get(slug_path='/role/')
    count = RoleNode.objects.count()

    node = RoleNode(system_id=simple_system.pk, parent_id=parent.pk, state='active', slug='1', name='Hi')
    node.calc_fields()
    node = RoleNode.objects.insert_object(node, send_save_signals=True)
    assert compare_time(node.created_at, timezone.now())
    assert compare_time(node.updated_at, timezone.now())
    assert RoleNode.objects.count() == count + 1
    assert node.name == 'Hi'

    with pytest.raises(IntegrityError):
        new_node = RoleNode(system_id=simple_system.pk, parent_id=parent.pk, state='active', slug='1', name='Mark')
        new_node.calc_fields()
        new_node = RoleNode.objects.insert_object(node, send_save_signals=True)


def test_upsert_node_with_upgraded_queryset(simple_system):
    """Тест, важный не только для RoleNode, а вообще для всех объектов, которые хотят делать UPSERT'ы"""
    parent = RoleNode.objects.get(slug_path='/role/')
    count = RoleNode.objects.count()

    queryset = RoleNode.objects.on_conflict(['system_id', 'slug_path'],
        ConflictAction.UPDATE,
        'state=\'active\'',
    )

    node = RoleNode(system_id=simple_system.pk, parent_id=parent.pk, state='active', slug='1', name='Hi')
    node.calc_fields()
    node = queryset.insert_object(node, send_save_signals=True)
    assert RoleNode.objects.count() == count+1
    assert node.name == 'Hi'

    with days_from_now(1):
        new_node = RoleNode(system_id=simple_system.pk, parent_id=parent.pk, state='active', slug='1', name='Mark')
        new_node.calc_fields()
        new_node = queryset.insert_object(new_node, send_save_signals=True, updated_fields=['name'])
        assert compare_time(new_node.created_at, node.created_at)
        assert compare_time(new_node.updated_at, timezone.now())
        assert RoleNode.objects.count() == count+1
        assert new_node.name == 'Mark'
        assert new_node.pk == node.pk


def test_get_node_by_unique_id(simple_system, complex_system):
    node = simple_system.nodes.get(slug='poweruser')
    node.unique_id = 'wow'
    node.save(update_fields=['unique_id'])

    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wat') == None
    assert RoleNode.objects.get_node_by_unique_id(system=complex_system, unique_id='wow') == None
    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow') == node


def test_get_node_by_unique_id_multiple_active(simple_system):
    node = simple_system.nodes.get(slug='poweruser')
    node.unique_id = 'wow'
    node.save(update_fields=['unique_id'])
    new_node = RoleNode.objects.create(
        system=simple_system,
        unique_id='wow',
        state='active',
        parent=node.parent,
        slug='newuser',
    )

    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow') == None


def test_get_node_by_unique_id_depriving(simple_system):
    node = simple_system.nodes.get(slug='poweruser')
    node.unique_id = 'wow'
    node.state = 'depriving'
    node.save(update_fields=['unique_id', 'state'])

    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow') == None
    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow', include_depriving=True) == node


def test_get_node_by_unique_id_multiple_depriving(simple_system):
    node = simple_system.nodes.get(slug='poweruser')
    node.unique_id = 'wow'
    node.state = 'depriving'
    node.save(update_fields=['unique_id', 'state'])
    with days_from_now(1):
        new_node = RoleNode.objects.create(
            system=simple_system,
            unique_id='wow',
            state='depriving',
        )
    with days_from_now(-1):
        old_node = RoleNode.objects.create(
            system=simple_system,
            unique_id='wow',
            state='depriving',
        )

    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow') == None
    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow', include_depriving=True) == new_node


def test_get_node_by_unique_id_multiple_active_and_depriving(simple_system):
    node = simple_system.nodes.get(slug='poweruser')
    node.unique_id = 'wow'
    node.save(update_fields=['unique_id'])
    with days_from_now(1):
        new_node = RoleNode.objects.create(
            system=simple_system,
            unique_id='wow',
            state='depriving',
        )
    with days_from_now(-1):
        old_node = RoleNode.objects.create(
            system=simple_system,
            unique_id='wow',
            state='depriving',
        )

    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow') == node
    assert RoleNode.objects.get_node_by_unique_id(system=simple_system, unique_id='wow', include_depriving=True) == node

