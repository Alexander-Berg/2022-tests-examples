from unittest.mock import create_autospec, call, ANY, Mock

import pytest

from maps.infra.sedem.lib.config.test_utils import config_factory
from maps.infra.sedem.lib.roles import RolesProvider, ServiceRoles, AbcRole
from maps.infra.sedem.sandbox.config_applier.lib.appliers.manage_nanny import (
    Context,
    NannyManager,
    ManageNannyAuth,
)


MOCK_APPLIER_PARAMS = {
    'oauth_token': 'fake-secret',
    'arcadia_root': '/fake-arcadia',
    'arcadia_revision': 42,
    'work_dirs_root': '/fake-cwd',
}


def service_roles_mock() -> ServiceRoles:
    return ServiceRoles(
        responsible=AbcRole(abc_id=0, staff_id=10, abc_slug='responsible-slug'),
        devops=[
            AbcRole(abc_id=1, staff_id=11, abc_slug='devops-slug', role_scope='devops-scope')
        ],
        duty=[
            AbcRole(abc_id=2, staff_id=12, abc_slug='duty-slug', role_scope='duty-scope')
        ],
        developers=[
            AbcRole(abc_id=3, staff_id=13, abc_slug='developer-slug', role_scope='developer-scope')
        ],
    )


@pytest.fixture
def roles_provider_mock() -> Mock:
    roles_provider = create_autospec(RolesProvider, instance=True)
    roles_provider.resolve_service_roles.return_value = service_roles_mock()
    return roles_provider


@pytest.fixture
def nanny_client_mock() -> Mock:
    return create_autospec(NannyManager, instance=True)


@pytest.mark.parametrize('deploy_type', ('rtc', 'garden'))
def test_applicable_for_nanny_only(deploy_type: str) -> None:
    service_config = config_factory(config={
        'main': {'name': 'mock-service'},
        'deploy': {'type': deploy_type},
    })

    manage_nanny_auth = ManageNannyAuth(
        **MOCK_APPLIER_PARAMS,
    )

    is_nanny_service = bool(deploy_type == 'rtc')
    assert manage_nanny_auth.is_applicable_for(service_config) == is_nanny_service


@pytest.mark.parametrize('restricted_environment', (True, False), ids=('restricted', 'relaxed'))
def test_format_nanny_auth(restricted_environment: bool) -> None:
    manage_nanny_auth = ManageNannyAuth(
        **MOCK_APPLIER_PARAMS,
    )

    service_auth = manage_nanny_auth.format_nanny_auth(
        {
            'owners': {'logins': ['user'], 'groups': ['10']},
            'ops_managers': {'logins': ['user'], 'groups': ['10']},
            'conf_managers': {'logins': ['user'], 'groups': ['10']},
            'developers': {'logins': ['user'], 'groups': ['10']},
            'observers': {'logins': ['user'], 'groups': ['10']},
        },
        roles=service_roles_mock(),
        deploy_unit='stable' if restricted_environment else 'testing',
    )

    if restricted_environment:
        assert service_auth == {
            'owners': {'logins': ['robot-maps-sandbox'], 'groups': ['11']},
            'ops_managers': {'logins': [], 'groups': ['12']},
            'conf_managers': {'logins': [], 'groups': ['12']},
            'developers': {'logins': [], 'groups': ['13']},
            'observers': {'logins': ['user'], 'groups': ['10']},
        }
    else:
        assert service_auth == {
            'owners': {'logins': ['robot-maps-sandbox'], 'groups': ['11', '13']},
            'ops_managers': {'logins': [], 'groups': ['12']},
            'conf_managers': {'logins': [], 'groups': ['12']},
            'developers': {'logins': [], 'groups': []},
            'observers': {'logins': ['user'], 'groups': ['10']},
        }


def test_format_nanny_auth_for_load_testing() -> None:
    manage_nanny_auth = ManageNannyAuth(
        **MOCK_APPLIER_PARAMS,
    )

    service_auth = manage_nanny_auth.format_nanny_auth(
        {},
        roles=service_roles_mock(),
        deploy_unit='load',
    )

    assert 'lunapark' in service_auth['ops_managers']['logins']


def test_update_nanny_auth(roles_provider_mock: Mock, nanny_client_mock: Mock) -> None:
    service_config = config_factory(config={
        'main': {'name': 'mock-service'},
        'resources': {'stable': {}, 'prestable': {}, 'testing': {}},
    })

    manage_nanny_auth = ManageNannyAuth(
        roles_provider=roles_provider_mock,
        nanny_client=nanny_client_mock,
        **MOCK_APPLIER_PARAMS,
    )

    manage_nanny_auth(
        config=service_config,
        context=Context(),
    )

    assert roles_provider_mock.mock_calls == [
        call.resolve_service_roles(service_config),
    ]
    assert nanny_client_mock.mock_calls == [
        call.modify_service_attrs(
            service_id=f'maps_core_mock_service_{deploy_unit}',
            attrs='auth_attrs',
            transform=ANY,  # FIXME
            modify_only_if_differ=True,
            comment='Updated by Sedem (r42)',
        )
        for deploy_unit in sorted(('stable', 'testing', 'prestable'))
    ]
