# encoding: utf-8
import pytest

from jsonschema.exceptions import ValidationError

from infra.rtc_sla_tentacles.backend.lib.config.misc_config import MiscConfig


def test_misc_config_fails_to_create_on_invalid_config_wrong_dc_name(raw_config_storage):
    """
        Check that 'MiscConfig' fails to create with 'ValidationError'
        on a invalid config schema - unsupported `my_datacenter_name`
        value.
    """
    full_config_invalid_my_datacenter_name = raw_config_storage.to_dict()
    full_config_invalid_my_datacenter_name["misc"]["my_datacenter_name"] = "nonexistent"
    with pytest.raises(ValidationError):
        MiscConfig(full_config=full_config_invalid_my_datacenter_name)


def test_misc_config_fails_to_create_on_invalid_config_wrong_env_name(raw_config_storage):
    """
        Check that 'MiscConfig' fails to create with 'ValidationError'
        on a invalid config schema - unsupported `env_name` value.
    """
    full_config_invalid_my_datacenter_name = raw_config_storage.to_dict()
    full_config_invalid_my_datacenter_name["misc"]["env_name"] = "nonexistent"
    with pytest.raises(ValidationError):
        MiscConfig(full_config=full_config_invalid_my_datacenter_name)
