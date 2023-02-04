import json

import pytest
from django.core.urlresolvers import reverse

pytestmark = pytest.mark.django_db


def test_roles(client, django_assert_num_queries, mock_idm, service, role):
    url = reverse('cabinet-api:cabinet_requested_roles')
    with django_assert_num_queries(8):
        # 1 select intranet_staff join auth_user and join intranet_department
        # 1 select django_content_type join auth_user_user_permissions
        # 1 select services_service join service_member and join roles_role
        # 1 select intranet_staff
        # 1 select intranet_department
        # 1 select roles_role join roles_rolescope

        # 1 select pg_is_in_recovery
        # 1 select waffle_switch
        response = client.get(url)
    assert response.status_code == 200
    assert mock_idm.called is True


def test_roles_meta(client, mock_idm, service, role):
    url = reverse('cabinet-api:cabinet_requested_roles')
    client.login(service.owner.login)
    response = client.get(url, {'only_mine': True})
    response_json = json.loads(response.content)

    assert response_json['meta']['count'] == len(response_json['objects']) == 2


def test_roles_serialization(client, mock_idm, service, role):
    url = reverse('cabinet-api:cabinet_requested_roles')
    client.login(service.owner.login)
    response = client.get(url, {'only_mine': True})
    response_json = json.loads(response.content)

    assert response_json['meta']['count'] == len(response_json['objects']) == 2
    assert response_json['objects'][0]['role']['id'] == role.id
    assert len(response_json['objects'][0]['service_parents']) == service.get_ancestors(include_self=True).count()

    service_parent_ids = [parent['id'] for parent in response_json['objects'][0]['service_parents']]
    assert service_parent_ids == sorted(service_parent_ids)


@pytest.mark.parametrize('only_mine', [True, False])
def test_roles_only_mine(client, mock_idm, service, role, only_mine):
    url = reverse('cabinet-api:cabinet_requested_roles')
    client.login(service.owner.login)
    response = client.get(url, {'only_mine': only_mine})
    response_json = json.loads(response.content)

    assert len(response_json['objects']) == (2 if only_mine else 3)
