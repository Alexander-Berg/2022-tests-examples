import pytest
from django.core.urlresolvers import reverse

from plan.roles.models import Role
from plan.services.models import ServiceCreateRequest
from common import factories

pytestmark = pytest.mark.django_db


def get_counts(client, **params):
    return client.json.get(reverse('cabinet-api:cabinet_request_counts'), params)


def test_count_200(client, mock_idm, service, role):
    response = get_counts(client)

    assert response.status_code == 200


def test_count_values(client, django_assert_num_queries, mock_idm, service, role, move_request, resources):
    factories.ServiceMemberFactory(
        staff=service.owner,
        service=resources.type.supplier,
        role=Role.get_responsible(),
    )
    factories.ServiceMemberFactory(
        staff=service.owner,
        service=move_request.service,
        role=Role.get_responsible(),
    )
    issue = factories.ServiceIssueFactory(
        service=service
    )
    factories.ServiceAppealIssueFactory(
        service_issue=issue
    )
    factories.ServiceAppealIssueFactory(
        service_issue=issue,
        state='rejected'
    )
    factories.ServiceAppealIssueFactory(
        service_issue=issue,
        state='approved'
    )
    factories.ServiceCreateRequestFactory(move_to=service, state=ServiceCreateRequest.REQUESTED)

    client.login(service.owner.login)
    with django_assert_num_queries(12):
        # 1 select intranet_staff join auth_user and join intranet_department
        # 1 select django_content_type join auth_user_user_permissions
        # 1 select services_service join service_member and join roles_role
        # 1 select intranet_staff
        # 1 select intranet_department
        # 1 select roles_role join roles_rolescope

        # 4 select count:
        #   - servicemoverequest
        #   - servicecreaterequest
        #   - serviceresource
        #   - serviceappealissue
        # 1 select pg_is_in_recovery
        # 1 select waffle_switch
        response = get_counts(client).json()

    assert {'role', 'service_move', 'service_create', 'resources', 'appeals'} == set(response['request_counts'].keys())
    assert response['request_counts']['role'] == 3
    assert response['request_counts']['resources'] == 1
    assert response['request_counts']['service_move'] == 1
    assert response['request_counts']['service_create'] == 1
    assert response['request_counts']['appeals'] == 1


def test_count_only_mine_requests(client, django_assert_num_queries, mock_idm, service):
    client.login(service.owner.login)
    with django_assert_num_queries(12):
        # 1 select intranet_staff join auth_user and join intranet_department
        # 1 select django_content_type join auth_user_user_permissions
        # 1 select services_service join service_member and join roles_role
        # 1 select intranet_staff
        # 1 select intranet_department
        # 1 select roles_role join roles_rolescope
        # 4 select count
        # 1 select pg_is_in_recovery
        # 1 select waffle_switch
        response = get_counts(client, only_mine=True).json()
    assert response['request_counts']['role'] == 2
