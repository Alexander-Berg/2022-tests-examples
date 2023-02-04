# coding: utf-8


import pytest

from django.utils import timezone

from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import RoleNode, System
from idm.tests.utils import create_system

pytestmark = [pytest.mark.django_db]


def _refresh_timestamps(system, hours_offset=0):
    now = timezone.now()
    for field in system.metainfo._meta.fields:
        if field.name.startswith('last_'):
            setattr(system.metainfo, field.name, now - timezone.timedelta(hours=hours_offset))
    system.metainfo.save()


def test_monitoring(client):
    """Проверяем работу мониторинга давно не выполнявшихся посистемных тасок"""

    system_1 = create_system('system_1', sync_role_tree=False)
    system_2 = create_system('system_2', sync_role_tree=False)

    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 200  # таймстемпов нет, но системы новые
    assert response.content == b'ok'

    System.objects.update(added=timezone.now() - timezone.timedelta(days=10))
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 400  # если для старой системы таймстемпа не было – это тоже плохо
    assert response.content == (
        b'"check_inconsistencies" task has not been recently run for systems system_1,system_2 | '
        b'"deprive_nodes" task has not been recently run for systems system_1,system_2 | '
        b'"recalc_pipeline" task has not been recently run for systems system_1,system_2 | '
        b'"report_inconsistencies" task has not been recently run for systems system_1,system_2 | '
        b'"resolve_inconsistencies" task has not been recently run for systems system_1,system_2'
    )

    _refresh_timestamps(system_1)
    _refresh_timestamps(system_2)

    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 200
    assert response.content == b'ok'

    system_1.metainfo.last_sync_nodes_finish = timezone.now() - timezone.timedelta(days=4)
    system_1.metainfo.save()
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 200  # для системы без автоапдейта мониторинг не загорается

    system_1.root_role_node.is_auto_updated = True
    system_1.root_role_node.save()
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 400
    assert response.content == b'"sync_nodes" task has not been recently run for systems system_1'
    RoleNode.objects.all().delete()

    system_1.metainfo.last_activate_memberships_finish = timezone.now() - timezone.timedelta(hours=5)
    system_1.metainfo.last_deprive_memberships_finish = timezone.now() - timezone.timedelta(days=10)
    system_1.metainfo.save()
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 200  # мониторинг на членства не загорается для систем, не поддерживающих их

    system_1.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS
    system_1.save(update_fields=['group_policy'])
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 400
    assert response.content == (
        b'"activate_memberships" task has not been recently run for systems system_1 | '
        b'"deprive_memberships" task has not been recently run for systems system_1'
    )

    system_1.metainfo.monitor_deprive_memberships = False
    system_1.metainfo.save()
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 400
    # monitor_{task}=False позволяет выкинуть какую-то таску какой-то системы из мониторинга
    assert response.content == b'"activate_memberships" task has not been recently run for systems system_1'

    system_2.group_policy = SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS
    system_2.is_broken = True
    system_2.save(update_fields=['group_policy', 'is_broken'])
    _refresh_timestamps(system_2, 5*24)  # всё должно протухнуть
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 400
    # система сломана, поэтому мониторинги на загораются
    assert response.content == b'"activate_memberships" task has not been recently run for systems system_1'

    system_2.is_broken = False
    system_2.save(update_fields=['is_broken'])
    response = client.get('/monitorings/unsynchronized-systems/')
    assert response.status_code == 400
    assert response.content == (
        b'"activate_memberships" task has not been recently run for systems system_1,system_2 | '
        b'"check_inconsistencies" task has not been recently run for systems system_2 | '
        b'"check_memberships" task has not been recently run for systems system_2 | '
        b'"deprive_memberships" task has not been recently run for systems system_2 | '
        b'"deprive_nodes" task has not been recently run for systems system_2 | '
        b'"recalc_pipeline" task has not been recently run for systems system_2 | '
        b'"report_inconsistencies" task has not been recently run for systems system_2 | '
        b'"resolve_inconsistencies" task has not been recently run for systems system_2 | '
        b'"resolve_memberships" task has not been recently run for systems system_2 | '
        b'"update_memberships" task has not been recently run for systems system_2'
    )
