# coding: utf-8


import pytest
from django.utils import timezone

from idm.core.constants.rolenode import ROLENODE_STATE
from idm.core.models import RoleNode
from idm.metrics.deprived_nodes import depriving_nodes_number

pytestmark = pytest.mark.django_db


def test_deprived_nodes(complex_system):
    node1, node2 = RoleNode.objects.all()[:2]
    RoleNode.objects.all().update(depriving_at=timezone.now())
    node1.state = ROLENODE_STATE.DEPRIVING
    node1.depriving_at = timezone.now() - timezone.timedelta(hours=2)
    node1.save(update_fields=['state', 'depriving_at'])
    node2.depriving_at = timezone.now() - timezone.timedelta(hours=2)
    node2.save(update_fields=['depriving_at'])
    assert depriving_nodes_number() == [{
        'slug': 'depriving_nodes_number',
        'value': 1,
    }]
