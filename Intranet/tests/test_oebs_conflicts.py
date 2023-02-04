# -*- coding: utf-8 -*-


import pytest
from ast import literal_eval
from django.conf import settings
from os import path

from idm.tests.utils import add_perms_by_role
from idm.utils import reverse


pytestmark = [pytest.mark.django_db]


def test_normal_user(client, arda_users):
    client.login('frodo')
    url = reverse('admin:oebs-conflicts')

    response = client.json.get(url)
    assert response.status_code == 302

    with open(path.join(settings.SETTINGS_ROOT_PATH, 'tests', 'static', 'OEBS_test.xlsx'), 'rb') as fd:
        response = client.post(url, {'file': fd})

    assert response.status_code == 302


@pytest.mark.parametrize('role', ['superuser', 'matrix_viewer'])
def test_build_conflicts_from_excel_file(client, arda_users, role):
    frodo = arda_users.frodo
    add_perms_by_role(role, frodo)
    if role in settings.IDM_NO_STAFF_INTERNAL_ROLES:
        assert frodo.is_staff is False
    client.login('frodo')
    url = reverse('admin:oebs-conflicts')

    response = client.json.get(url)
    assert response.status_code == 200

    with open(path.join(settings.SETTINGS_ROOT_PATH, 'tests', 'static', 'OEBS_test.xlsx'), 'rb') as fd:
        response = client.post(url, {'file': fd})
    assert response.status_code == 200
    workflow = response.context['workflow']
    assert workflow.startswith('CONFLICT_DATA = ')
    padding = len('CONFLICT_DATA = ')
    conflicts = literal_eval(workflow[padding:])

    expected = [
        ('UMX|XXYA_%_OEBS_MANAGER', 'UMX|XXYA_%_MASTER_DATA'),
        ('UMX|XXYA_%_GL_CHIEF_ACCOUNTANT_GAAP', 'UMX|XXYA_%_GL_ACCOUNTANT_GAAP'),
        ('UMX|XXYA_%_GL_CHIEF_ACCOUNTANT_GAAP', 'UMX|XXYA_%_GL_MACCOUNTANT_GAAP'),
        ('UMX|XXYA_%_OEBS_ROLES_ADMIN', 'UMX|XXYA_%_OEBS_MANAGER_ADMIN'),
        ('UMX|XXYA_%_OEBS_SYSADMIN', 'UMX|XXYA_%_OEBS_HRMS_MANAGER'),
        ('UMX|XXYA_%_LOGISTIC_SUPPORT', 'UMX|XXYA_%_TAX'),
        ('UMX|XXYA_%_LOGISTIC_SUPPORT', 'UMX|XXYA_%_MANAGER_SUPPORT_SK'),
        ('UMX|XXYA_%_USER_FORMS_MENU', 'UMX|XXYA_%_MASTER_DATA'),
        ('UMX|XXYA_%_USER_FORMS_MENU', 'UMX|XXYA_%_OEBS_MANAGER_ADMIN'),
    ]
    assert sorted(conflicts) == sorted(expected)
