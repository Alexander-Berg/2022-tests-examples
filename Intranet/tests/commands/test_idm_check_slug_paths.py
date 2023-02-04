# coding: utf-8



import mock
import pytest
import waffle.testutils

from django.core.management import call_command
from django.utils import timezone

from idm.core.models import RoleNode, System

pytestmark = [pytest.mark.django_db]


@waffle.testutils.override_switch('check_slug_paths', active=True)
def test_check_slug_paths():
    system = System.objects.create(slug='system')
    root_node = system.nodes.get()
    pushed_node = RoleNode.objects.create(parent=root_node, pushed_at=timezone.now(), system=system, slug='x')
    moved_node = RoleNode.objects.create(parent=root_node, moved_at=timezone.now(), system=system, slug='y')
    failed_moved_node = RoleNode.objects.create(parent=root_node, moved_at=timezone.now(), system=system, slug='a')
    failed_moved_node.slug_path = 'cfgvubhijo'
    failed_moved_node.save(update_fields=['slug_path'])
    node = RoleNode.objects.create(parent=root_node, system=system, slug='z')

    with mock.patch(
            'idm.core.management.commands.idm_check_slug_paths.IntrasearchFetcher.search', return_value=True
    ):
        call_command('idm_check_slug_paths')

    node.refresh_from_db()
    moved_node.refresh_from_db()
    pushed_node.refresh_from_db()
    failed_moved_node.refresh_from_db()

    assert moved_node.slug_path_ok_after_push_at is None
    assert moved_node.slug_path_ok_after_move_at is not None
    assert moved_node.suggest_ok_after_move_at is not None
    assert moved_node.suggest_ok_after_push_at is None

    assert pushed_node.slug_path_ok_after_push_at is not None
    assert pushed_node.slug_path_ok_after_move_at is None
    assert pushed_node.suggest_ok_after_move_at is None
    assert pushed_node.suggest_ok_after_push_at is not None

    assert node.slug_path_ok_after_push_at is None
    assert node.slug_path_ok_after_move_at is None
    assert node.suggest_ok_after_move_at is None
    assert node.suggest_ok_after_push_at is None

    assert failed_moved_node.slug_path_ok_after_push_at is None
    assert failed_moved_node.slug_path_ok_after_move_at is None
    assert failed_moved_node.suggest_ok_after_move_at is None
    assert failed_moved_node.suggest_ok_after_push_at is None
