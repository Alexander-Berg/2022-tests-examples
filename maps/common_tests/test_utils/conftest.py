import os
from unittest import mock

import pytest

import yatest.common
import yt.wrapper as yt

from maps.pylibs.utils.lib import json_utils

from yandex.maps.test_utils.smtp import bring_up_smtp_server
from maps.garden.libs_server.config.settings_provider import EnvironmentSettingsProvider
from maps.garden.sdk.utils import GB

from maps.garden.libs.auth.auth_server import UserInfo, GardenClients, AuthMethod

from maps.garden.libs_server.acl_manager import acl_manager

from . import tester


TEST_USER = "someuser"


@pytest.fixture(scope="module")
def patched_regions():
    regions_filepath = yatest.common.source_path("maps/garden/common_tests/configs/regions.json")
    with mock.patch("maps.garden.libs_server.config.config.REGIONS_CONFIG_PATH", regions_filepath):
        yield


@pytest.fixture(scope="module")
def smtp_server():
    # TODO: do not bring up smtp server in tests.
    # Try mocking smtp calls.
    with bring_up_smtp_server() as smtp_config:
        yield smtp_config


@pytest.fixture
def patched_server_config(smtp_server, request):
    server_config = json_utils.load_json(
        yatest.common.source_path("maps/garden/common_tests/configs/server.json"))

    server_config["smtp"] = {
        "host": smtp_server["host"],
        "port": smtp_server["port"]
    }

    server_config["isolated_modules_cache_dir"] = os.path.abspath("./cache/")

    try:
        _update_server_yt_config(server_config, request)
        server_config["sandbox"] = {"token": "stub"}
    except:
        # Ignore. Recipe yt_stuff is not included for all the test suites that use server's test_lib.
        pass

    with mock.patch("maps.garden.libs_server.config.config.server_settings", return_value=server_config):
        yield server_config


def _update_server_yt_config(server_config, request):
    yt_fixture = request.getfixturevalue("yt_stuff")
    yt_proxy = "localhost:{}".format(yt_fixture.yt_proxy_port)
    yt_config = {
        "proxy": {
            "url": yt_proxy,
            "enable_proxy_discovery": False
        },
        "token": "token",
        "spec_overrides": {
            "max_failed_job_count": 1,
            "fail_on_job_restart": True,
            "acl": [],
        }
    }
    server_config["yt"] = {
        "config": yt_config,
        "configs_ypath": "//home/garden/configs",
        "task_settings": {
            "environment": {
                "YT_ALLOW_HTTP_REQUESTS_TO_YT_FROM_JOB": "1"
            }
        },
        "error_retry_policy": {
            "try_number": 8,
            "start_delay": 120,
            "backoff_multiplier": 2
        },
        "prefix": "//home/garden/unittests",
    }
    client = yt.YtClient(config=yt_config)
    client.mkdir("//home/garden/configs", recursive=True)
    client.mkdir("//home/garden/bin", recursive=True)
    server_config["resources"] = {
        "cpu": 10,
        "ram": GB,
    }


@pytest.fixture
def server_hostname():
    return "localhost.unittest"


@pytest.fixture
def patched_environment_settings(patched_server_config, s3stub_environment_settings, server_hostname, mocker):
    environment_settings = json_utils.load_json(
        yatest.common.source_path("maps/garden/common_tests/configs/environment_settings.json"))
    environment_settings.update(s3stub_environment_settings)

    environment_settings["garden"]["server_hostname"] = server_hostname

    if "yt" in patched_server_config:
        environment_settings["yt_servers"] = {
            "hahn": {
                "yt_config": {
                    "proxy": {
                        "url": patched_server_config["yt"]["config"]["proxy"]["url"],
                        "enable_proxy_discovery": False
                    },
                    "token": "token"
                },
                "prefix": "//home/garden/unittests",
                "tmp_dir": "//home/garden/unittests/tmp"
            }
        }

        environment_settings["yt"] = {
            # TODO: Network_project does not work in tests. Uncomment after YT-13268
            # "network_project": "maps_core_garden_stable"
        }

    mocker.patch.object(EnvironmentSettingsProvider, "get_settings", return_value=environment_settings)


@pytest.fixture
def prepare_server(db, mocker):
    am = acl_manager.AclManager(db)
    role = acl_manager.UserRole(
        username=TEST_USER,
        role=acl_manager.ADMIN_ROLE
    )
    am.add_role(role)

    mocker.patch(
        "maps.garden.libs_server.application.flask_utils.user_from_request",
        return_value=UserInfo(
            username=TEST_USER,
            servicename=GardenClients.GARDEN_UI_STABLE,
            method=AuthMethod.TVM
        ),
    )


@pytest.fixture
def builds_helper(garden_client):
    return tester.BuildsHelper(garden_client)
