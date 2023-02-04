import pytest

from infra.rtc_sla_tentacles.backend.lib.tests import conftest  # noqa

from infra.rtc_sla_tentacles.backend.lib.config.clickhouse_config import ClickhouseConfig
from infra.rtc_sla_tentacles.backend.lib.config.harvesters_config import HarvestersConfig
from infra.rtc_sla_tentacles.backend.lib.config.logging_config import LoggingConfig
from infra.rtc_sla_tentacles.backend.lib.config.misc_config import MiscConfig
from infra.rtc_sla_tentacles.backend.lib.config.mongo_config import MongoConfig
from infra.rtc_sla_tentacles.backend.lib.config.nanny_config import NannyConfig
from infra.rtc_sla_tentacles.backend.lib.config.secrets_config import SecretsConfig
from infra.rtc_sla_tentacles.backend.lib.config.yp_config import YpConfig
from infra.rtc_sla_tentacles.backend.lib.config.yp_lite_pods_manager_config import YpLitePodsManagerConfig


@pytest.fixture
def secrets_config(raw_config_storage) -> SecretsConfig:
    """
        Creates and returns an instance of 'SecretsConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return SecretsConfig(full_config=full_valid_config)


@pytest.fixture
def mongo_config(raw_config_storage, secrets_config) -> MongoConfig:
    """
        Creates and returns an instance of 'MongoConfig'
        based on valid config from 'raw_config_storage' and
        'secrets_config' fixtures.
    """
    full_valid_config = raw_config_storage.to_dict()
    return MongoConfig(full_config=full_valid_config, secrets_config=secrets_config)


@pytest.fixture
def clickhouse_config(raw_config_storage, secrets_config) -> ClickhouseConfig:
    """
        Creates and returns an instance of 'ClickhouseConfig'
        based on valid config from 'raw_config_storage' and
        'secrets_config' fixtures.
    """
    full_valid_config = raw_config_storage.to_dict()
    return ClickhouseConfig(full_config=full_valid_config, secrets_config=secrets_config)


@pytest.fixture
def misc_config(raw_config_storage) -> MiscConfig:
    """
        Creates and returns an instance of 'MiscConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return MiscConfig(full_config=full_valid_config)


@pytest.fixture
def harvesters_config(raw_config_storage) -> HarvestersConfig:
    """
        Creates and returns an instance of 'HarvestersConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return HarvestersConfig(full_config=full_valid_config)


@pytest.fixture
def nanny_config(raw_config_storage, secrets_config) -> NannyConfig:
    """
        Creates and returns an instance of 'NannyConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return NannyConfig(full_config=full_valid_config, secrets_config=secrets_config)


@pytest.fixture
def yp_config(raw_config_storage, secrets_config) -> YpConfig:
    """
        Creates and returns an instance of 'YpConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return YpConfig(full_config=full_valid_config, secrets_config=secrets_config)


@pytest.fixture
def yp_lite_pods_manager_config(raw_config_storage) -> YpLitePodsManagerConfig:
    """
        Creates and returns an instance of 'YpLitePodsManagerConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return YpLitePodsManagerConfig(full_config=full_valid_config)


@pytest.fixture
def logging_config(raw_config_storage) -> LoggingConfig:
    """
        Creates and returns an instance of 'LoggingConfig'
        based on valid config from 'raw_config_storage' fixture.
    """
    full_valid_config = raw_config_storage.to_dict()
    return LoggingConfig(full_config=full_valid_config)
