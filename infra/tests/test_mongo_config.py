# encoding: utf-8
import pytest

from jsonschema.exceptions import ValidationError

from infra.rtc_sla_tentacles.backend.lib.config.exceptions import ConfigGetOptionError
from infra.rtc_sla_tentacles.backend.lib.config.mongo_config import MongoConfig


def test_mongo_config_fails_to_create_on_invalid_config(raw_config_storage, secrets_config):
    """
        Check that 'MongoConfig' fails to create with 'ValidationError'
        on a invalid config schema.
    """
    full_config_invalid = raw_config_storage.to_dict()
    del full_config_invalid["storage"]["mongodb"]["credentials"]["default"]
    with pytest.raises(ValidationError):
        MongoConfig(full_config=full_config_invalid, secrets_config=secrets_config)


def test_mongo_config_requesting_existing_connection(mongo_config, raw_config_storage):
    """
        Check that existing 'connection' is accessible with
        '.get_option()' method and is not changed.
    """
    connection = mongo_config.get_option("connection", "default")
    full_config_valid = raw_config_storage.to_dict()
    assert connection == (full_config_valid["storage"]
                                           ["mongodb"]
                                           ["connection"]
                                           ["default"])


def test_mongo_config_requesting_nonexisting_connection(mongo_config):
    """
        Check that non-existing 'connection' raises exception when accessed.
    """
    with pytest.raises(ConfigGetOptionError):
        mongo_config.get_option("connection", "UNEXISTENT_CONNECTION")


def test_mongo_config_requesting_existing_credentials(mongo_config, raw_config_storage):
    """
        Check that existing 'credentials' is accessible with
        '.get_option()' method, and only 'password' field added to it.
    """
    credentials = mongo_config.get_option("credentials", "default")
    full_config_valid = raw_config_storage.to_dict()
    default_mongo_credentials = full_config_valid["storage"]["mongodb"]["credentials"]["default"]
    assert "password" not in default_mongo_credentials
    # Add 'password' field - it is added when secrets are resolved.
    default_mongo_credentials["password"] = 'mongodb_qwerty_custom'
    assert credentials == default_mongo_credentials


def test_mongo_config_requesting_nonexisting_credentials(mongo_config):
    """
        Check that non-existing 'credentials' raises exception when accessed.
    """
    with pytest.raises(ConfigGetOptionError):
        mongo_config.get_option("credentials", "UNEXISTENT_CREDENTIALS")
