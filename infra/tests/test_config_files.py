import os.path
import random
from unittest.mock import Mock

import pytest

import sepelib.core.config as config
from sepelib.core.exceptions import Error
from walle.main import load_config
from walle.util.cloud_tools import get_config_path

_CONFIG_NAME_TEMPLATE = "walle.{name}.yaml"

_REQUIRED_CONFIG_ENVIRONMENT = list(
    map(
        "WALLE_{}".format,
        [
            # If you are adding new item into this list, then
            # don't forget to add new environment variable (in the form on WALLE_<name>)
            # to both production and prestable environments
            "main_mongodb_password",
            "health_mongodb_password",
            "oauth_client_secret",
            "dns_api_token",
            "staff_token",
            "abc_token",
            "startrek_token",
            "ipmiproxy_token",
            "lui_token",
            "eine_token",
            "racktables_token",
            "bot_token",
            "juggler_token",
            "cms_token",
            "tvm_secret",
            "authorization_csrf_key",
            "certificator_token",
            "ok_token",
            "qloud_token",
            "idm_token",
            "yp_token",
            "vlan_toggler_token",
            "box_il_password",
        ],
    )
)


def mk_random_string(length):
    alphabet = "abcdefghijklmnopqrstuvwxyz"
    if length > len(alphabet):
        alphabet *= (length // len(alphabet)) + 1

    return "".join(random.sample(alphabet, length))


@pytest.fixture
def mock_config_environment(monkeypatch):
    for param in _REQUIRED_CONFIG_ENVIRONMENT:
        monkeypatch.setitem(os.environ, param, mk_random_string(10))


def default_config_context():
    return {
        "root": "/",
        "log_dir": "/logs",
        "port": "8080",
    }


@pytest.fixture()
def monkeypatch_config(monkeypatch):
    monkeypatch.setattr(config, "_CONFIG", None)


def from_root_dir(*path_parts):
    root_dir = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))
    return os.path.join(root_dir, *path_parts)


def config_path(ctype):
    return "../conf/{}".format(_CONFIG_NAME_TEMPLATE.format(name=ctype))


def load_config_for_ctype(ctype, context=None):
    config_file = config_path(ctype)

    mock_args = Mock()
    mock_args.config = config_file
    mock_args.config_context = context if context is not None else default_config_context()

    load_config(mock_args)  # shall not rise


@pytest.mark.parametrize("ctype", ["prod", "test"])
def test_have_required_params(monkeypatch_config, ctype, mock_config_environment):
    load_config_for_ctype(ctype)  # shall not rise
    required_keys_list = [
        "mongodb",
        "health-mongodb",
        "oauth",
        "juggler.client_kwargs",
        "juggler.notifications",
        "juggler.checks",
        "juggler.checks._generic_passive",
        "tvm.app_id",
    ]
    for key in required_keys_list:
        assert config.get_value(key), "Key {} must be present in config file for {} ctype".format(key, ctype)


@pytest.mark.parametrize("ctype", ["prod", "test"])
@pytest.mark.parametrize("missing_environment", _REQUIRED_CONFIG_ENVIRONMENT)
def test_loading_fails_on_missing_environment(monkeypatch_config, ctype, mock_config_environment, missing_environment):
    del os.environ[missing_environment]

    if ctype == "prod" or missing_environment != "WALLE_health_mongodb_password":
        with pytest.raises(Error) as exc:
            load_config_for_ctype(ctype)

        missing_context_name = missing_environment.replace("WALLE_", "")
        assert "'{}' is undefined".format(missing_context_name) in str(exc.value)


def test_config_path_detection(monkeypatch):
    monkeypatch.setitem(os.environ, "BSCONFIG_IDIR", "/mock-idir-path")
    monkeypatch.setitem(os.environ, "BSCONFIG_ITAGS", "a_ctype_ctypemock")

    # path should be absolute
    assert get_config_path() == "/mock-idir-path/conf/" + _CONFIG_NAME_TEMPLATE.format(name="ctypemock")


def test_config_path_detection_without_idir(monkeypatch):
    if "BSCONFIG_IDIR" in os.environ:
        monkeypatch.delitem(os.environ, "BSCONFIG_IDIR")
    monkeypatch.setitem(os.environ, "BSCONFIG_ITAGS", "a_ctype_ctypemock")

    # path should be relative to CWD
    assert get_config_path() == "conf/" + _CONFIG_NAME_TEMPLATE.format(name="ctypemock")


def test_config_path_detection_without_any_tags(monkeypatch):
    if "BSCONFIG_IDIR" in os.environ:
        monkeypatch.delitem(os.environ, "BSCONFIG_IDIR")
    if "BSCONFIG_ITAGS" in os.environ:
        monkeypatch.delitem(os.environ, "BSCONFIG_ITAGS")

    # path should be relative to CWD
    assert get_config_path() == "conf/" + _CONFIG_NAME_TEMPLATE.format(name="conf")
