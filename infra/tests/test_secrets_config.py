# encoding: utf-8
import pytest

from jsonschema.exceptions import ValidationError

from infra.rtc_sla_tentacles.backend.lib.config.exceptions import ConfigValidationError
from infra.rtc_sla_tentacles.backend.lib.config.secrets_config import SecretsConfig


def test_secrets_config_fails_to_create_on_invalid_config(raw_config_storage):
    """
        Check that 'SecretsConfig' fails to create with 'ValidationError'
        on a invalid config schema.
    """
    full_config_invalid = raw_config_storage.to_dict()
    del full_config_invalid["secrets"]["secrets_directory"]
    with pytest.raises(ValidationError):
        SecretsConfig(full_config=full_config_invalid)


def test_secrets_config_reads_secret_from_env(secrets_config):
    """
         Check that reading secret values from ENV variables works.
         'ENV_SECRET_NAME' is in 'rcs' fixture.
    """
    secret_value_from_env = secrets_config.get_secret("ENV_SECRET_NAME")
    assert secret_value_from_env == "ENV_SECRET_VALUE"


def test_secrets_config_reads_secret_from_file(monkeypatch, secrets_config):
    """
         Check that 'SecretsConfig' reads file named as secret name
         when there is no such ENV variable.
    """
    # Mock '_read_secret_file(filename)' method.
    def mock_read_secret_file(filename):
        if filename.endswith("FILE_SECRET_NAME"):
            return "FILE_SECRET_VALUE"

    monkeypatch.setattr(secrets_config, "_read_secret_file", mock_read_secret_file)
    secret_value_from_file = secrets_config.get_secret("FILE_SECRET_NAME")
    assert secret_value_from_file == "FILE_SECRET_VALUE"


def test_secrets_config_raises_exception_when_no_secret_value_found(secrets_config):
    """
        Check that 'SecretsConfig' raises 'ConfigValidationError'
        when there is no filename or ENV variable with secret name found.
    """
    with pytest.raises(ConfigValidationError):
        secrets_config.get_secret("UNEXISTENT_SECRET_NAME")
