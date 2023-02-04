import threading

import pytest
import flask

import yt.wrapper as yt
import yatest.common
from yatest.common.network import PortManager

from maps.garden.libs_server.test_utils.common import server_root_path

from maps.garden.server.lib import run_garden_server

import common as cm


@pytest.fixture
def yt_client(patched_server_config):
    return yt.YtClient(config=patched_server_config["yt"]["config"])


@pytest.fixture
def module_executor(mocker):
    path = yatest.common.binary_path("maps/garden/tools/yt_module_executor/bin/garden-yt-module-executor")
    mocker.patch("maps.garden.libs_server.yt_task_handler.pymod.utils.MODULE_EXECUTOR_LOCAL_PATH", path)


@pytest.fixture
def server_hostname(
    db,
    prepare_server,
    patched_server_config,
):
    app_client = flask.Flask("server", root_path=server_root_path())
    run_garden_server.prepare_app(patched_server_config, app_client)
    with PortManager() as pm:
        port = pm.get_port()
        thread = threading.Thread(
            target=app_client.run,
            daemon=True,
            kwargs=dict(host="localhost", port=port)
        )
        thread.start()
        yield f"localhost:{port}"


@pytest.fixture
def scheduler(
    db,
    server_hostname,
    patched_server_config,
    patched_environment_settings,
    patched_regions,
    module_executor
):
    with cm.RestartableScheduler(patched_server_config) as sch:
        yield sch


@pytest.fixture
def garden_client_helper(patched_server_config, server_hostname, scheduler):
    return cm.GardenClientHelper(patched_server_config, server_hostname)
