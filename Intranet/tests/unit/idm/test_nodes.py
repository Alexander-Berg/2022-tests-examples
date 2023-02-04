import pytest

from common import factories

from plan.idm import nodes


pytestmark = pytest.mark.django_db


def test_get_service_node():
    parent = factories.ServiceFactory(slug='parent')

    node = nodes.get_service_node(parent)
    assert node.slug_path == '/type/services/services_key/parent/'
    assert node.key_path == '/type/services/services_key/parent/parent_key/'
    assert node.value_path == '/services/parent/'

    child = factories.ServiceFactory(slug='child', parent=parent)

    node = nodes.get_service_node(child)
    assert node.slug_path == '/type/services/services_key/parent/parent_key/child/'
    assert node.key_path == '/type/services/services_key/parent/parent_key/child/child_key/'
    assert node.value_path == '/services/parent/child/'


def test_get_service_roles_node():
    service = factories.ServiceFactory(slug='node_test')

    roles_node = nodes.get_service_roles_node(service)
    assert roles_node.slug_path == '/type/services/services_key/node_test/node_test_key/*/'
    assert roles_node.key_path == '/type/services/services_key/node_test/node_test_key/*/role/'
    assert roles_node.value_path == '/services/node_test/*/'


def test_get_role_node():
    service = factories.ServiceFactory(slug='node_test')
    role = factories.RoleFactory()

    role_node = nodes.get_role_node(service, role)
    assert role_node.slug_path == (
        '/type/services/services_key/node_test/node_test_key/*/role/{}/'.format(role.id)
    )
    assert role_node.value_path == '/services/node_test/*/{}/'.format(role.id)


def test_get_service_from_value_path():
    service = factories.ServiceFactory(slug='node_test')
    node = nodes.get_service_node(service)
    assert nodes.get_service_from_value_path(node.value_path) == service


def test_get_service_from_value_path_raises_exception():
    for invalid_path in ('/services/', '/services/*/123/'):
        with pytest.raises(ValueError):
            nodes.get_service_from_value_path(invalid_path)
