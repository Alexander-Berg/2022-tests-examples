# encoding: utf-8
import pytest

from jsonschema.exceptions import ValidationError

from infra.rtc_sla_tentacles.backend.lib.config.harvesters_config import HarvestersConfig


def test_harvesters_config_fails_to_create_on_invalid_config(raw_config_storage):
    full_config_invalid = raw_config_storage.to_dict()
    del full_config_invalid["harvesters"]["results_storage"]
    with pytest.raises(ValidationError):
        HarvestersConfig(full_config=full_config_invalid)
