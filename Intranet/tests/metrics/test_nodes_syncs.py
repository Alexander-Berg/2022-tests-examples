# coding: utf-8


import pytest
from django.utils import timezone

from idm.core.models import Action, System
from idm.metrics.nodes_syncs import count_successful_nodes_syncs, count_successful_nodes_syncs_time
from idm.core.constants.action import ACTION


pytestmark = pytest.mark.django_db


def test_count_successful_nodes_syncs():
    system_slug = 'abc'
    system = System.objects.create(slug=system_slug)

    for _ in range(3):
        Action.objects.create(action=ACTION.ROLE_TREE_STARTED_SYNC)
    Action.objects.create(action=ACTION.ROLE_TREE_SYNCED, data={'status': 1})
    Action.objects.create(action=ACTION.ROLE_TREE_SYNCED, data={'status': 0})
    Action.objects.update(system=system)

    success = count_successful_nodes_syncs()[0]
    assert success == {
        'slug': system_slug + '_success',
        'value': 1,
    }
    fail = count_successful_nodes_syncs()[1]
    assert fail == {
        'slug': system_slug + '_fail',
        'value': 2,
    }


def test_count_successful_nodes_syncs_time():
    system_slug = 'abc'
    system = System.objects.create(slug=system_slug)

    started = timezone.now() - timezone.timedelta(weeks=1, hours=1)
    finished = started
    for _ in range(11):
        start = Action.objects.create(action=ACTION.ROLE_TREE_STARTED_SYNC)
        finish = Action.objects.create(
            action=ACTION.ROLE_TREE_SYNCED,
            data={'status': 0},
            parent=start,
        )
        Action.objects.filter(id=start.id).update(added=started)
        Action.objects.filter(id=finish.id).update(added=finished)
        finished += timezone.timedelta(hours=1)
    Action.objects.create(action=ACTION.ROLE_TREE_SYNCED, data={'status': 1})
    Action.objects.update(system=system)

    result = count_successful_nodes_syncs_time()[0]
    assert result == {
        'slug': system_slug,
        'value': 32400.0,
    }
