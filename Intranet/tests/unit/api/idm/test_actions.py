import itertools

import pytest
from mock import patch, Mock

from common import factories
from plan.api.idm import actions
import pretend

pytestmark = pytest.mark.django_db


@pytest.fixture
def data():
    metaservice = factories.ServiceFactory()
    service1 = factories.ServiceFactory(parent=metaservice)
    service2 = factories.ServiceFactory(parent=metaservice)

    return pretend.stub(
        metaservice=metaservice,
        service1=service1,
        service2=service2,
    )


@patch('plan.idm.nodes.Node.exists')
@pytest.mark.parametrize('service_node', [True, False])
def test_move_service(exists, service_node, data):
    exists.return_value = node_mock = Mock()
    # сначала проверяем существование service_node, затем new_parent_node, затем new_node
    node_mock.exists.side_effect = [service_node, True, True]

    with patch('plan.idm.adapters.RoleNodeManager.update') as update:
        actions.move_service(data.service1, data.service2)

    params = update.call_args_list[0][1]
    assert params['node_path'] == \
        '/type/services/services_key/{0}/{0}_key/{1}/'.format(data.metaservice.slug, data.service1.slug)
    assert params['parent'] == \
        '/type/services/services_key/{0}/{0}_key/{1}/{1}_key/'.format(data.metaservice.slug, data.service2.slug)


@patch('plan.idm.nodes.Node.exists')
def test_move_service_to_root(exists, data):
    exists.return_value = node_mock = Mock()
    node_mock.exists.return_value = True

    with patch('plan.idm.adapters.RoleNodeManager.update') as update:
        actions.move_service(data.service1, None)

    params = update.call_args_list[0][1]
    assert params['node_path'] == \
        '/type/services/services_key/{0}/{0}_key/{1}/'.format(data.metaservice.slug, data.service1.slug)
    assert params['parent'] == '/type/services/services_key/'


@pytest.mark.parametrize('crowdtest_environment', [
    ['plan.api.idm.actions'],
], indirect=['crowdtest_environment'])
def test_get_roles_crowdtest_disabled(crowdtest_environment):
    """actions.get_roles переопределены в crowdtest окружении"""
    assert actions.get_roles(None) == []
    assert actions.chain is itertools.chain
