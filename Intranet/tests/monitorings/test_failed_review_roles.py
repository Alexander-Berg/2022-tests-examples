# coding: utf-8

import pytest

from idm.tests.utils import assert_contains

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('status', ['warn', 'ok'])
def test_failed_review_roles(arda_users, simple_system, other_system, client, status):
    """Проверяем работу мониторинга ролей с неудачным пересмотром"""

    expected_status = 200
    expected_content = b'ok'

    if status == 'warn':
        expected_status = 412
        expected_content = b'IDM has 3 failed roles on last review'
        simple_system.metainfo.roles_failed_on_last_review = [100500, 1005001]
        simple_system.metainfo.save(update_fields=['roles_failed_on_last_review'])

        other_system.metainfo.roles_failed_on_last_review = [100502]
        other_system.metainfo.save(update_fields=['roles_failed_on_last_review'])

    client.login(arda_users.frodo)
    response = client.get('/monitorings/failed-review-roles/')

    assert response.status_code == expected_status
    assert_contains([expected_content], response.content)
