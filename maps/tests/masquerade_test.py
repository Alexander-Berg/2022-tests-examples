import dataclasses
import typing as tp
from dataclasses import dataclass
from unittest.mock import create_autospec

import pytest

from maps.infra.sedem.lib.config import BadConfigError, LocalServiceConfigBuilder
from maps.infra.sedem.lib.config.test_utils import config_factory
from maps.infra.masquerade.sandbox.lib.config import (
    MASQUERADE_FQDN,
    MasqueradeClient,
    masquerade_client,
    masquerade_clients,
)


TEAPOT_CLIENT = MasqueradeClient(
    hosts=['core-teapot.common.testing.maps.yandex.net'],
    testing_tvm_dst_id=2008809,
)
TEACUP_CLIENT = MasqueradeClient(
    hosts=['core-teacup.common.testing.maps.yandex.net'],
    testing_tvm_dst_id=2008809,
)
TEAPOT_TWO_HOSTS_CLIENT = MasqueradeClient(
    hosts=['core-teapot-1.common.testing.maps.yandex.net', 'core-teapot-2.common.testing.maps.yandex.net'],
    testing_tvm_dst_id=2008809,
)
TEASPOON_CLIENT = MasqueradeClient(
    hosts=['core-teaspoon.common.testing.maps.yandex.net'],
    testing_tvm_dst_id=2008809,
)
MASQUERADE_CLIENT_HAS_A_NON_MASQUERADE_MIRRORING = MasqueradeClient(
    hosts=['core-teaspoon.common.testing.maps.yandex.net'],
    testing_tvm_dst_id=2008809,
)
NON_MASQUERADE_MIRRORING = {
    'fqdn': 'core-teaspoon.testing.maps.yandex.net',
    'rate': 0.777,
}
SOME_SECRET = {
    'secret_id': 'sec-01dkvdrcatst3qfm8d4e82mh8h',
    'key': 'MAPS_ST_MONITOR_TELEGRAM_TOKEN',
    'env': 'SOME_SECRET',
    'version': 'ver-01dkvdrcb718me5km8hmjn9x3z',
}
TESTING_BALANCER = {
    'prj': 'core-teaspoon',
    'instances_count': 2,
    'mirroring': {
        'fqdn': MASQUERADE_FQDN,
        'host': TEASPOON_CLIENT,
        'rate': 0.12345,
        'remove_credentials': False,
    },
}


@dataclass
class MasqueradeCase:
    name: str
    expected_masquerade_client: tp.Optional[MasqueradeClient]
    expected_exception: tp.Optional[BadConfigError]
    has_balancers: bool = False
    has_stable_balancer: bool = False
    mirroring_hosts: list[tp.Optional[str]] = dataclasses.field(default_factory=list)
    has_testing_mirroring: bool = False
    has_a_non_masquerade_mirroring: bool = False
    has_stable_secrets: bool = False
    has_testing_secrets: bool = False
    stable_tvm_ids: list[int] = dataclasses.field(default_factory=list)
    testing_tvm_ids: list[int] = dataclasses.field(default_factory=list)


def service_config(case: MasqueradeCase) -> dict[str, tp.Any]:
    if case.stable_tvm_ids:
        assert case.has_stable_secrets, _no_secrets_error('stable')
    if case.testing_tvm_ids:
        assert case.has_testing_secrets, _no_secrets_error('testing')
    if case.mirroring_hosts:
        assert case.has_balancers and case.has_stable_balancer, (
            'Test case is invalid: please state explicitly that the service'
            ' "has_balancers" and "has_stable_balancer" in all cases with mirroring hosts.'
        )
    if case.has_stable_balancer or case.has_testing_mirroring or case.has_a_non_masquerade_mirroring:
        assert (
            case.has_balancers
        ), 'Test case is invalid: please state explicitly that the service "has_balancers" in this case.'

    stable_secrets = [SOME_SECRET] + _make_tvm_secrets(case.stable_tvm_ids) if case.has_stable_secrets else None
    testing_secrets = [SOME_SECRET] + _make_tvm_secrets(case.testing_tvm_ids) if case.has_testing_secrets else None
    secrets = _remove_none_values_shallowly(
        {
            'testing': testing_secrets,
            'stable': stable_secrets,
        }
    )

    balancer = _make_balancer(case)

    return {
        'main': {
            'name': 'masquerade-fake',
            'balancer': balancer,
        },
        'resources': {'unstable': {}},
        'secrets': secrets,
    }


def _remove_none_values_shallowly(d: dict) -> dict:
    return {k: v for k, v in d.items() if v is not None}


def _no_secrets_error(env: str):
    return (
        f'Test case is invalid: please state explicitly that test has_{env}_secrets'
        f' for all test cases with {env}_tvm_ids'
    )


def _make_tvm_secrets(tvm_ids: list[int]):
    return [
        {
            'secret_id': 'sec-01d16mfhkr10hzwdnme39cwxqx',
            'key': 'tvm_secret',
            'self_tvm_id': tvm_id,
            'version': 'ver-01d16mfhm766cxjj7z0hr66gyd',
        }
        for tvm_id in tvm_ids
    ]


def _make_balancer(case: MasqueradeCase):
    mirrorings = _make_mirrorings(case.mirroring_hosts, case.has_a_non_masquerade_mirroring)
    stable_balancer = _make_stable_balancer(mirrorings)
    return (
        _remove_none_values_shallowly(
            {
                'stable': [stable_balancer] if case.has_stable_balancer else None,
                'testing': [TESTING_BALANCER] if case.has_testing_mirroring else None,
            }
        )
        if case.has_balancers
        else None
    )


Mirrorings = tp.Optional[list[dict[str, tp.Any]]]


def _make_mirrorings(
    mirroring_hosts: list[str],
    non_masquerade_mirroring: bool,
) -> Mirrorings:
    mirrorings = []
    if mirroring_hosts:
        mirrorings += [_make_mirroring(host) for host in mirroring_hosts]
    if non_masquerade_mirroring:
        mirrorings.append(NON_MASQUERADE_MIRRORING)
    return mirrorings or None


def _make_mirroring(host: tp.Optional[str]) -> dict[str, ...]:
    mirroring = {
        'fqdn': MASQUERADE_FQDN,
        'rate': 0.12345,
        'remove_credentials': False,
        'host': host,
    }
    return _remove_none_values_shallowly(mirroring)


def _make_stable_balancer(mirrorings: Mirrorings):
    upstreams = {
        'default': _make_upstream('2s', mirrorings),
        'another': _make_upstream('40s', mirrorings),
    }

    return {
        'prj': 'core-teaspoon',
        'instances_count': 2,
        'upstreams': upstreams,
    }


def _make_upstream(timeout: str, mirrorings: Mirrorings) -> dict[str, ...]:
    return _remove_none_values_shallowly(
        {
            'backend_timeout': timeout,
            'recv_timeout': timeout,
            'mirroring': mirrorings,
        }
    )


def _make_bad_config_error(*args) -> BadConfigError:
    errors = '\n'.join(args)
    return BadConfigError(
        f'Invalid config at fake-path: '
        f'{len(args)} validation error{"s" if len(args) > 1 else ""} for ServiceConfig\n'
        f'{errors}'
    )


def _make_bad_config_error_line(reason: str, where: str = '__root__') -> str:
    return f'{where}\n  {reason}'


MASQUERADE_CASES = [
    MasqueradeCase(
        name='baseline',
        expected_masquerade_client=TEACUP_CLIENT,
        expected_exception=None,
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEACUP_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[TEACUP_CLIENT.testing_tvm_dst_id],
    ),
    MasqueradeCase(
        name='has_another_non_masquerade_mirroring',
        expected_masquerade_client=TEAPOT_CLIENT,
        expected_exception=None,
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
        has_a_non_masquerade_mirroring=True,
    ),
    MasqueradeCase(
        name='has_testing_tvm_and_a_stable_tvm',
        expected_masquerade_client=TEAPOT_CLIENT,
        expected_exception=None,
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
        has_stable_secrets=True,
        stable_tvm_ids=[2000000],
    ),
    MasqueradeCase(
        name='several_hosts',
        expected_masquerade_client=TEAPOT_TWO_HOSTS_CLIENT,
        expected_exception=None,
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_TWO_HOSTS_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
    ),
    MasqueradeCase(
        name='no_masquerade_mirroring',
        expected_masquerade_client=None,
        expected_exception=None,
        has_balancers=True,
        has_stable_balancer=True,
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
    ),
    MasqueradeCase(
        name='no_masquerade_mirroring__has_another_non_masquerade_mirroring',
        expected_masquerade_client=None,
        expected_exception=None,
        has_balancers=True,
        has_stable_balancer=True,
        has_a_non_masquerade_mirroring=True,
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
    ),
    MasqueradeCase(
        name='no_secrets',
        expected_masquerade_client=None,
        expected_exception=_make_bad_config_error(
            _make_bad_config_error_line(
                'mirroring through Masquerade requires providing "self_tvm_id" in secrets for testing (type=value_error)'
            )
        ),
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_CLIENT.hosts,
        has_testing_secrets=False,
    ),
    MasqueradeCase(
        name='no_tvm',
        expected_masquerade_client=None,
        expected_exception=_make_bad_config_error(
            _make_bad_config_error_line(
                'mirroring through Masquerade requires providing "self_tvm_id" in secrets for testing (type=value_error)'
            )
        ),
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[],
    ),
    MasqueradeCase(
        name='no_tvm__has_stable_tvm',
        expected_masquerade_client=None,
        expected_exception=_make_bad_config_error(
            _make_bad_config_error_line(
                'mirroring through Masquerade requires providing "self_tvm_id" in secrets for testing (type=value_error)'
            )
        ),
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[],
        has_stable_secrets=True,
        stable_tvm_ids=[2000000],
    ),
    MasqueradeCase(
        name='conflicting_tvm',
        expected_masquerade_client=None,
        expected_exception=_make_bad_config_error(
            _make_bad_config_error_line(
                'more than one "self_tvm_id" in testing (type=value_error)',
                where='secrets',
            )
        ),
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=TEAPOT_CLIENT.hosts,
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id, 2000000],
    ),
    MasqueradeCase(
        name='wrong_host_in_masquerade_mirroring__no_host',
        expected_masquerade_client=None,
        expected_exception=_make_bad_config_error(
            _make_bad_config_error_line(
                '"host" must be different from Masquerade FQDN to deduce backend to mirror to (type=value_error)',
                where='balancers -> stable -> 0 -> upstreams -> default -> mirroring -> 0 -> __root__',
            ),
            _make_bad_config_error_line(
                '"host" must be different from Masquerade FQDN to deduce backend to mirror to (type=value_error)',
                where='balancers -> stable -> 0 -> upstreams -> another -> mirroring -> 0 -> __root__',
            ),
        ),
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=[None],
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
    ),
    MasqueradeCase(
        name='wrong_host_in_masquerade_mirroring__masquerade_host',
        expected_masquerade_client=None,
        expected_exception=_make_bad_config_error(
            _make_bad_config_error_line(
                '"host" must be different from Masquerade FQDN to deduce backend to mirror to (type=value_error)',
                where='balancers -> stable -> 0 -> upstreams -> default -> mirroring -> 0 -> __root__',
            ),
            _make_bad_config_error_line(
                '"host" must be different from Masquerade FQDN to deduce backend to mirror to (type=value_error)',
                where='balancers -> stable -> 0 -> upstreams -> another -> mirroring -> 0 -> __root__',
            ),
        ),
        has_balancers=True,
        has_stable_balancer=True,
        mirroring_hosts=[MASQUERADE_FQDN],
        has_testing_secrets=True,
        testing_tvm_ids=[TEAPOT_CLIENT.testing_tvm_dst_id],
    ),
]


@pytest.mark.parametrize(
    'case',
    [case for case in MASQUERADE_CASES if not case.expected_exception],
    ids=lambda case: case.name,
)
def test_masquerade_client(case: MasqueradeCase) -> None:
    config = config_factory(config=service_config(case)).as_config()

    client = masquerade_client(config)

    assert client == case.expected_masquerade_client


def test_masquerade_clients() -> None:
    configs = {f'path/{i}': service_config(case) for i, case in enumerate(MASQUERADE_CASES)}

    mock_builder = create_autospec(LocalServiceConfigBuilder, instance=True)
    mock_builder.iter_all_service_paths.return_value = iter(configs)
    mock_builder.load_config.side_effect = lambda path: config_factory(config=configs[path])

    clients, errors = masquerade_clients(mock_builder)

    assert clients == [
        case.expected_masquerade_client
        for case in MASQUERADE_CASES
        if case.expected_masquerade_client
    ]
    assert list(map(str, errors)) == [
        str(case.expected_exception)
        for case in MASQUERADE_CASES
        if case.expected_exception
    ]
