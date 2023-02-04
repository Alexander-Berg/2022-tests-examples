import json
import logging
import os

import pytest

import aiohttp.pytest_plugin
from aioresponses import aioresponses

from library.python import resource

from logos.libs.logging import configure_logger

from ads.emily.viewer.backend.app import create_app
from ads.emily.viewer.backend.app.config import APP_PATH
from ads.emily.viewer.backend.app.db.data import YtStorage

pytest_plugins = ["aiohttp.pytest_plugin"]
del aiohttp.pytest_plugin.loop

logger = logging.getLogger(__name__)
configure_logger(logger, logger_level=logging.DEBUG)
logger = logging.getLogger("ads.emily.viewer.backend.app")
configure_logger(logger, logger_level=logging.DEBUG)


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def app():
    return create_app()


@pytest.fixture
async def client(loop, app, aiohttp_client, data_to_disk):
    return await aiohttp_client(app)


@pytest.fixture(scope="module")
def stable_json():
    logger.debug("Load stable.json from Sandbox")
    stable_json = [os.path.join("app/data", x) for x in os.listdir("app/data") if x.endswith("json")]
    assert stable_json, "Sandbox error: No stable.json was found"
    assert len(stable_json) == 1, "Sandbox: more than one json found {}".format(stable_json)
    with open(stable_json[0], encoding="utf-8") as f:
        return f.read()


@pytest.fixture(scope="module")
def swagger_json_disk():
    logger.debug("Load swagger.json from client disk")
    r = resource.find("ads/emily/viewer/frontend/src/services/swagger.json")
    return json.loads(r.decode("utf-8"))


@pytest.fixture(scope="module")
def ml_backend_pkg():
    logger.debug("Load ml-backend-pkg.json from disk")
    r = resource.find("ads/emily/viewer/backend/ml-backend-pkg.json")
    return json.loads(r.decode("utf-8"))


@pytest.fixture(scope="module")
def test_ya_make():
    logger.debug("Load tests/ya.make from disk")
    r = resource.find("ads/emily/viewer/backend/tests/ya.make")
    return r.decode("utf-8")


@pytest.fixture(scope="module")
def data_to_disk(stable_json):
    os.makedirs(f"{APP_PATH}/data", exist_ok=True)
    with open(f"{APP_PATH}/data/stable.json", "w") as f:
        logger.debug("Dump stable.json to {}".format(os.path.abspath(f"{APP_PATH}/data/stable.json")))
        f.write(stable_json)


@pytest.fixture(autouse=True)
def mock_responses():
    with aioresponses(passthrough=["http://127.0.0.1:"]) as m:
        yield m


@pytest.fixture(scope="session", autouse=True)
def yt_client():
    r = resource.find("ads/emily/viewer/backend/tests/resources/lm_configs.json")
    lm_configs = json.loads(r.decode("utf-8"))

    r = resource.find("ads/emily/viewer/backend/tests/resources/tracking_states.json")
    tracking_states = json.loads(r.decode("utf-8"))

    class LocalYTClient:
        lm_config_path = "//home/bs/emily/prod/LinearModelConfig"
        tracking_states_path = "//home/bs/emily/dev/dt/model-states"

        def read_table(self, path: str):
            if path == self.lm_config_path:
                return lm_configs
            if path == self.tracking_states_path:
                return tracking_states
            raise ValueError("Unknown path")

        def get(self, path: str):
            if path.endswith("/@modification_time"):
                return "2022-01-01T00:00:00.000000Z"
            raise ValueError("Unknown path")

    yt_client = LocalYTClient()
    YtStorage._yt_client = yt_client
    return yt_client
