# coding: utf-8



import mock
import pytest

from idm.sync.staff.users import import_tvm_apps, import_tvm_responsibles
from idm.users.constants.group import GROUP_TYPES
from idm.users.constants.user import USER_TYPES
from idm.users.models import Group, GroupMembership, GroupResponsibility, User

pytestmark = [pytest.mark.django_db]


class MockedResponse(object):
    def __init__(self, json_data):
        self.json_data = json_data

    def json(self):
        return self.json_data


def create_tvm_json(external_id, name, service_id, service_slug, service_name, service_name_en):
    return {
        'resource': {
            'external_id': external_id,
            'name': name,
        },
        'service': {
            'id': service_id,
            'slug': service_slug,
            'name': {
                'ru': service_name,
                'en': service_name_en,
            }
        }
    }


def test_import_tvm_apps():
    abc_api_responses = MockedResponse({
        'next': None,
        'results': [
            create_tvm_json(1, 'tvm_name_1', 10, 'a', 'A', 'A'),
            create_tvm_json(2, 'tvm_name_2', 20, 'b', 'B', 'B'),
            create_tvm_json(3, 'tvm_name_3', 10, 'a', 'A', 'A'),
            create_tvm_json(5, 'tvm_name_1', 20, 'b', 'B', 'B')
        ]
    })

    app_1 = User.objects.create(type=USER_TYPES.TVM_APP, username=1)  # должно появиться имя
    app_4 = User.objects.create(type=USER_TYPES.TVM_APP, username=4)  # должно стать неактивным
    app_5 = User.objects.create(type=USER_TYPES.TVM_APP, username=5)
    service_2 = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, external_id=20)  # должно появиться имя
    service_4 = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, slug='d', name='D', name_en='D', external_id=40)
    membership_4 = GroupMembership.objects.create(user=app_4, group=service_4, state='active', is_direct=True)
    membership_5 = GroupMembership.objects.create(user=app_5, group=service_2, state='inactive', is_direct=True)

    with mock.patch('idm.sync.staff.users.http.get', return_value=abc_api_responses):
        import_tvm_apps()

    app_1.refresh_from_db()
    assert app_1.first_name == 'tvm_name_1'
    membership = GroupMembership.objects.select_related('group').get(user=app_1)
    assert membership.state == 'active'
    service_1 = membership.group
    assert service_1.type == GROUP_TYPES.TVM_SERVICE
    assert service_1.external_id == 10
    assert service_1.slug == 'a'
    assert service_1.name == 'A'
    assert service_1.name_en == 'A'

    app_2 = User.objects.get(type=USER_TYPES.TVM_APP, username=2)
    assert app_2.first_name == 'tvm_name_2'
    prev_service_2_id = service_2.id
    service_2 = GroupMembership.objects.select_related('group').get(user=app_2).group
    assert service_2.id == prev_service_2_id
    assert service_2.slug == 'b'
    assert service_2.name == 'B'
    assert service_2.name_en == 'B'

    app_3 = User.objects.get(type=USER_TYPES.TVM_APP, username=3)
    assert app_3.first_name == 'tvm_name_3'
    service_3 = GroupMembership.objects.select_related('group').get(user=app_3).group
    assert service_3.slug == 'a'
    assert service_3.name == 'A'
    assert service_3.name_en == 'A'

    app_4.refresh_from_db()
    assert app_4.is_active is False
    membership_4.refresh_from_db()
    assert membership_4.state == 'inactive'

    membership_5.refresh_from_db()
    assert membership_5.state == 'active'

    assert Group.objects.tvm_groups().count() == 3


def test_import_tvm_responsibles():
    tvm_service = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, external_id=100)
    odd_group = Group.objects.create(type=GROUP_TYPES.DEPARTMENT, external_id=666)
    user_a = User.objects.create(username='a')
    user_b = User.objects.create(username='b')
    user_c = User.objects.create(username='c')
    resp_a = GroupResponsibility.objects.create(group=tvm_service, user=user_a, is_active=True)
    resp_c = GroupResponsibility.objects.create(group=tvm_service, user=user_c, is_active=False)
    resp_d = GroupResponsibility.objects.create(group=odd_group, user=user_c, is_active=False)

    abc_api_responses = MockedResponse({
        'next': None,
        'results': [
            {
                'person': {
                    'login': 'b'
                },
            },
            {
                'person': {
                    'login': 'c'
                },
            },
        ]
    })
    with mock.patch('idm.sync.staff.users.http.get', return_value=abc_api_responses):
        import_tvm_responsibles()

    resp_a.refresh_from_db()
    assert not resp_a.is_active

    resp_b = GroupResponsibility.objects.get(group=tvm_service, user=user_b)
    assert resp_b.is_active

    resp_c.refresh_from_db()
    assert resp_c.is_active

    resp_d.refresh_from_db()
    assert not resp_d.is_active
