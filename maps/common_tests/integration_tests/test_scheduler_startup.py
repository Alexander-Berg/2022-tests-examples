import copy
from unittest import mock
import pytest
import flask

from maps.garden.scheduler.lib import run_scheduler


@pytest.mark.use_local_mongo
def test_scheduler_startup_without_yt(
    patched_server_config,
    patched_regions,
    patched_environment_settings,
    module_executor,
    db
):
    server_config = copy.copy(patched_server_config)
    server_config["yt"]["config"]["proxy"]["url"] = "localhost:1"  # with some fake port

    with mock.patch("maps.garden.libs_server.config.config.server_settings", return_value=server_config):
        app_scheduler = flask.Flask("scheduler")
        app_scheduler.testing = True
        with run_scheduler.prepare_app(server_config, app_scheduler):
            pass
