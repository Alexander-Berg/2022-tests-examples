import pytest
import mongomock
import yaml

from infra.rtc_sla_tentacles.backend.lib.tests.config_example1 import (
    env_example1,
    cli_args_example1,
    config_file_example1,
)

from infra.rtc_sla_tentacles.backend.lib.config.base_config import BaseConfig
from infra.rtc_sla_tentacles.backend.lib.config.interface import ConfigInterface
from infra.rtc_sla_tentacles.backend.lib.config.raw_config_storage import RawConfigStorage


@pytest.fixture
def patched_base_config(monkeypatch) -> None:
    """
        Mock 'BaseConfig._check_file_exists_and_readable(path_to_file)'
        method so not to check if example 'ssl_ca_cert_path' file exists
        and is readable.
    """
    # noinspection PyUnusedLocal
    def mock_check_file_exists_and_readable(self, path_to_file):
        return True

    monkeypatch.setattr(BaseConfig, "_check_file_exists_and_readable", mock_check_file_exists_and_readable)


@pytest.fixture
def raw_config_storage(monkeypatch, patched_base_config) -> RawConfigStorage:
    """
        Creates and returns an instance of 'RawConfigStorage'.
        Configs are minimal and sane.
    """
    for env_name, env_value in env_example1.items():
        monkeypatch.setenv(env_name, env_value)
    file_cfg = yaml.safe_load(config_file_example1)
    return RawConfigStorage(cli_args=cli_args_example1, file_cfg=file_cfg)


@pytest.fixture
def config_interface(raw_config_storage) -> ConfigInterface:
    """
        Creates and returns an instance of 'ConfigInterface'
        based on valid config from 'raw_config_storage' and
        'secrets_config' fixtures.
    """
    return ConfigInterface(raw_config_storage=raw_config_storage, configure_logging=False)


@pytest.fixture
def mongomock_client(config_interface):
    results_storage_config = config_interface.get_harvesters_results_storage_config()
    mongodb_parameters = results_storage_config["db_url"]
    return mongomock.MongoClient(**mongodb_parameters)


class FakeSnapshotManager:
    class _FakeConfigInterface:
        def register_broadcast_harvester(self, class_):
            pass

        def get_env_name(self):
            return "some"

    def get_last_snapshot_labels_from_harvester(self, harvester_type):
        return []

    _config_interface = _FakeConfigInterface()


@pytest.fixture
def fake_snapshot_manager():
    return FakeSnapshotManager()
