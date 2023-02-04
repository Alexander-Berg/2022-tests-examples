from pathlib import PurePath
from unittest.mock import create_autospec

import pytest

from maps.infra.sedem.lib.config import (
    BadConfigError,
    LocalServiceConfigBuilder,
)
from maps.infra.sedem.sandbox.config_applier.lib.config_factory import ServiceConfigFactory
from maps.infra.sedem.sandbox.config_applier.tests.mock_service_config import mock_config


MOCK_SERVICE_CONFIGS = {
    config.service_path(): config
    for config in [
        mock_config('maps/common_mock'),
        mock_config('maps/agent_mock', virtual=True),
        mock_config('maps/infra_mock', dependencies=['maps/agent_mock']),
        mock_config('maps/virtual_mock', virtual=True),
        mock_config('maps/with_depency_mock', dependencies=['maps/virtual_mock']),
    ]
}


def load_mock_config(path: PurePath) -> ServiceConfigFactory:
    if path in MOCK_SERVICE_CONFIGS:
        return MOCK_SERVICE_CONFIGS[path]
    raise BadConfigError(f'Missing config at {path}')


def test_iter_all_configs() -> None:
    mock_builder = create_autospec(LocalServiceConfigBuilder, instance=True)
    mock_builder.iter_all_service_paths.return_value = iter(MOCK_SERVICE_CONFIGS)
    mock_builder.load_config.side_effect = load_mock_config

    factory = ServiceConfigFactory(
        path_prefix='maps',
        config_builder=mock_builder,
    )

    result = sorted(
        str(config.service_path())
        for config in factory.iter_all_service_configs()
    )
    assert result == [
        'maps/common_mock',
        'maps/infra_mock',
        'maps/with_depency_mock',
    ]


def test_iter_all_configs_broken_config() -> None:
    mock_builder = create_autospec(LocalServiceConfigBuilder, instance=True)
    mock_builder.iter_all_service_paths.return_value = iter((
        PurePath('maps/agent_mock'),
        PurePath('maps/broken_mock'),
        PurePath('maps/common_mock'),
    ))
    mock_builder.load_config.side_effect = load_mock_config

    factory = ServiceConfigFactory(
        path_prefix='maps',
        config_builder=mock_builder,
    )

    with pytest.raises(BadConfigError):
        list(factory.iter_all_service_configs())


def test_iter_all_configs_duplicated_name() -> None:
    mock_builder = create_autospec(LocalServiceConfigBuilder, instance=True)
    mock_builder.iter_all_service_paths.return_value = iter((
        PurePath('maps/common_mock'),
        PurePath('maps/infra/common_mock'),
    ))
    mock_builder.load_config.side_effect = mock_config

    factory = ServiceConfigFactory(
        path_prefix='maps',
        config_builder=mock_builder,
    )

    with pytest.raises(BadConfigError, match='Service "maps-core-common-mock" found on multiple paths'):
        list(factory.iter_all_service_configs())


def test_iter_selected_configs() -> None:
    mock_builder = create_autospec(LocalServiceConfigBuilder, instance=True)
    mock_builder.iter_all_service_paths.return_value = iter(MOCK_SERVICE_CONFIGS)
    mock_builder.load_config.side_effect = load_mock_config

    factory = ServiceConfigFactory(
        path_prefix='maps',
        config_builder=mock_builder,
    )

    result = sorted(
        str(config.service_path())
        for config in factory.iter_selected_service_configs(service_names=[
            'maps-core-agent-mock',
            'maps-core-common-mock',
            'maps-core-with-depency-mock',
        ])
    )
    assert result == [
        'maps/common_mock',
        'maps/with_depency_mock',
    ]


def test_iter_affected_configs() -> None:
    mock_builder = create_autospec(LocalServiceConfigBuilder, instance=True)
    mock_builder.iter_all_service_paths.return_value = iter(MOCK_SERVICE_CONFIGS)
    mock_builder.iter_affected_service_paths.return_value = iter((
        PurePath('maps/virtual_mock'),
        PurePath('maps/common_mock'),
    ))
    mock_builder.load_config.side_effect = load_mock_config

    factory = ServiceConfigFactory(
        path_prefix='maps',
        config_builder=mock_builder,
    )

    result = sorted(
        str(config.service_path())
        for config in factory.iter_affected_service_configs(changed_paths=[])
    )
    assert result == [
        'maps/common_mock',
        'maps/with_depency_mock',
    ]
