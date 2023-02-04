import logging
from unittest import mock
import os
import time
import shutil

import pytest

import yatest.common

from maps.pylibs.utils.lib import process

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import PythonResource

from maps.garden.libs_server.graph.task_queue import TaskQueue
from maps.garden.libs_server.module import communicate
from maps.garden.libs_server.module.module import Module
from maps.garden.libs_server.graph.request_handler import RequestHandler
from maps.garden.libs_server.graph.request_storage import RAMRequestStorage
from maps.garden.libs_server.test_utils.task_handler_stubs import ModuleGraphManager
from maps.garden.libs_server.test_utils.task_handler import UnittestTaskHandler

from maps.garden.scheduler.lib.graph_builder import get_module_graph

logger = logging.getLogger("garden.server")

REGION = "australia"
VENDOR = "navteq"
SHIPPING_DATE = "201903022"

TEST_REGIONS = [(REGION, VENDOR)]

PROPERTIES = {
    "region": REGION,
    "vendor": VENDOR,
    "shipping_date": SHIPPING_DATE
}

MODULE_TO_TEST = 'maps/garden/libs_server/test_utils/test_module/test_module'

CONTOUR_NAME = "contour_name"


@pytest.fixture
def tempdir_patch(request):
    # There is a limit 107 symbols for unix socked path length
    # Create a temporary subdirectory with not too long name
    random_dir = str(hash(request.function.__name__))[:5]
    new_tmp_dir = os.path.join(os.environ["TMPDIR"], random_dir)

    with mock.patch.dict(os.environ, {"TMPDIR": new_tmp_dir}):
        with mock.patch("tempfile.tempdir", new_tmp_dir):
            os.mkdir(new_tmp_dir)
            yield
            shutil.rmtree(new_tmp_dir, ignore_errors=True)


@pytest.fixture
def yt_client():
    yt_client = mock.Mock()
    with mock.patch("yt.wrapper.YtClient", return_value=yt_client):
        yt_client.exists.return_value = True
        yield yt_client


@pytest.mark.usefixtures("tempdir_patch")
@pytest.mark.usefixtures("yt_client")
@mock.patch("maps.pylibs.utils.lib.process.check_call_logged", wraps=process.check_call_logged)
def test_isolated_module_scheduling(check_call_mock, metrics, resource_storage):
    module_path = yatest.common.binary_path(MODULE_TO_TEST)
    module_info = communicate.get_module_info(module_path)

    module = Module(
        module_name=module_info.name,
        module_version=module_info.version,
        yt_client_config=None,
        local_path=module_path,
        remote_path="//tmp/stub"
    )
    module.evaluate_module_info()

    check_call_mock.reset_mock()

    with module.get_runner(CONTOUR_NAME):
        graph_builder = get_module_graph(module, TEST_REGIONS, CONTOUR_NAME)

        check_call_mock.assert_called_once()
        assert check_call_mock.call_args.kwargs["env"] == {"ENVIRONMENT_NAME": "unstable"}

        for resource_name in graph_builder.input_resources():
            graph_builder.add_resource(PythonResource(resource_name))

        input_name_to_version = {}
        for resource_name in graph_builder.input_resources():
            input_name_to_version[resource_name] = Version(properties=PROPERTIES)

        target_names = graph_builder.output_resources()
        assert "output_resource" in target_names

        start_time = time.time()

        with RequestHandler(
            UnittestTaskHandler.create_from_graph_builder(graph_builder, resource_storage),
            TaskQueue(),
            ModuleGraphManager(graph_builder, CONTOUR_NAME),
            RAMRequestStorage(),
            mock.Mock(),
        ) as request_handler:
            request_handler.enable_logging = True
            request_handler.handle(input_name_to_version, target_names)

        metrics.set("scheduling_time_ms", (time.time() - start_time) * 1000)
