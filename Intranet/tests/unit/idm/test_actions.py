import pytest
from mock import patch

from plan import settings
from plan.api.idm import actions
from plan.services.models import Service
from plan.roles.models import Role
from common import factories

pytestmark = pytest.mark.django_db


@patch('plan.idm.adapters.rolenode.RoleNodeManager.create')
def test_add_role(create_node):
    service = factories.ServiceFactory()
    role = factories.RoleFactory(service=service)

    actions.add_role(role)

    assert create_node.call_args[1]['slug'] == str(role.pk)
    assert create_node.call_args[1]['name'] == {'ru': role.name, 'en': role.name_en}
    assert create_node.call_args[1]['parent'] == \
        '/type/services/services_key/%s/%s_key/*/role/' % (service.slug, service.slug)


@patch('plan.idm.adapters.rolenode.RoleNodeManager.delete')
def test_delete_role(delete_node):
    service = factories.ServiceFactory()
    role = factories.RoleFactory(service=service)

    actions.delete_role(role)

    role_node_path = '/type/services/services_key/%s/%s_key/*/role/%s/' % (service.slug, service.slug, role.pk)
    assert delete_node.call_args[1]['node_path'] == role_node_path


@pytest.mark.parametrize('review_required', [True, False])
def test_set_review_policy_to_service(review_required):
    role_count = 3
    service: Service = factories.ServiceFactory()
    [factories.RoleFactory(service=service) for _ in range(role_count)]

    with patch('plan.idm.adapters.rolenode.RoleNodeManager.update') as manager_update_mock:
        actions.set_review_policy_to_service(service, review_required)

    assert manager_update_mock.call_count == service.role_set.count() + Role.objects.filter(service=None).count()
    for call in manager_update_mock.call_args_list:
        assert call.kwargs['system'] == settings.ABC_IDM_SYSTEM_SLUG
        assert call.kwargs['review_required'] == review_required
