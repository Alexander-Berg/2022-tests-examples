# coding: utf-8
from idm.users.utils import get_group_url
from idm.users.models import Group
from idm.tests.utils import ctt_data_is_consistent, refresh
import pytest

pytestmark = pytest.mark.django_db


def test_ctt_hook(department_structure):
    test_group = Group.objects.get(external_id=105)
    assert ctt_data_is_consistent(test_group)


def test_external_link(department_structure):
    """Проверим генерацию внешних ссылок"""

    fellowship = department_structure.fellowship
    assert fellowship.get_external_url() == 'https://staff.test.yandex-team.ru/departments/fellowship-of-the-ring/'

    Group.objects.update(type='wiki')
    fellowship = refresh(fellowship)
    assert fellowship.get_external_url() == 'https://staff.test.yandex-team.ru/groups/fellowship-of-the-ring/'

    Group.objects.update(type='service')
    earth = refresh(department_structure.earth)
    fellowship = refresh(fellowship)
    fellowship.level = 2
    fellowship.parent.level = 1
    # если урл сервисной группы не начинается с svc_, то оставляем как есть
    assert fellowship.get_external_url() == 'https://abc.test.yandex-team.ru/services/associations/'

    # а если начинается, то отсекаем первый svc_.
    department_structure.associations.slug = 'svc_earth_svc_hello'
    department_structure.associations.save()
    fellowship = refresh(fellowship)
    fellowship.level = 2
    fellowship.parent.level = 1
    assert fellowship.get_external_url() == 'https://abc.test.yandex-team.ru/services/earth_svc_hello/'


def test_external_link_from_group_dict():
    group = {
        'slug': 'svc_grandchild',
        'type': 'service',
        'level': 3,
        'parent': {
            'slug': 'svc_child',
            'type': 'service',
            'level': 2,
            'parent': {
                'slug': 'svc_parent',
                'type': 'service',
                'level': 1,
                'parent': {
                    'slug': 'service',
                    'type': 'service',
                    'level': 0,
                }
            }
        }
    }
    group_without_parent = {
        'slug': 'svc_invalid',
        'type': 'service',
        'level': 2,
        'parent': None
    }
    assert get_group_url(group) == 'https://abc.test.yandex-team.ru/services/parent/'
    assert get_group_url(group['parent']) == 'https://abc.test.yandex-team.ru/services/parent/'
    assert get_group_url(group['parent']['parent']) == 'https://abc.test.yandex-team.ru/services/parent/'
    assert get_group_url(group['parent']['parent']['parent']) is None
    assert get_group_url(group_without_parent) is None

    group = {
        'slug': 'svc_group_scope',
        'type': 'service',
        'level': 2,
        'parent': {
            'slug': 'svc_group'
        },
    }
    assert get_group_url(group) == 'https://abc.test.yandex-team.ru/services/group/'
