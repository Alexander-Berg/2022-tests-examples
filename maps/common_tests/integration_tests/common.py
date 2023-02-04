from datetime import datetime
import pytz
import flask

import yatest.common
import yt.wrapper as yt
from yt.wrapper.ypath import ypath_join

from maps.pylibs.utils.lib import common
from maps.pylibs.yandex_environment import environment as yenv

from maps.garden.libs.garden_client.garden_client import GardenClient
from maps.garden.libs_server.module.module import Module

from maps.garden.scheduler.lib import run_scheduler


# module binaries just built from arcadia
CURRENT_BINARY_MODULE_VERSION = "current_binary"

# previous module binaries
PREV_BINARY_MODULE_VERSION = "prev_binary"

SYSTEM_CONTOUR_NAME = "unittest"
USER_CONTOUR_NAME = "someuser_contour"

TEST_MODULE = "test_module"
TEST_SRC_MODULE = "test_src_module"
MODULES = [
    {
        "name": TEST_SRC_MODULE,
        "paths": {
            CURRENT_BINARY_MODULE_VERSION:
                "maps/garden/libs_server/test_utils/test_src_module/test_src_module",
            PREV_BINARY_MODULE_VERSION:
                "maps/garden/common_tests/integration_tests/prev_binaries/prev_test_src_module"
        },
        "traits": {
            "type": "source",
            "name": TEST_SRC_MODULE,
            "sort_options": [{"key_pattern": "test_pattern"}],
        },
        "versions": {
            CURRENT_BINARY_MODULE_VERSION: "12",
            PREV_BINARY_MODULE_VERSION: "11",
        },
        "released_to_stable": True,
        "contour_name": None,
    },
    {
        "name": TEST_SRC_MODULE,
        "paths": {
            CURRENT_BINARY_MODULE_VERSION:
                "maps/garden/libs_server/test_utils/test_src_module/test_src_module",
            PREV_BINARY_MODULE_VERSION:
                "maps/garden/common_tests/integration_tests/prev_binaries/prev_test_module"
        },
        "traits": {
            "type": "source",
            "name": TEST_SRC_MODULE,
            "sort_options": [{"key_pattern": "test_pattern"}],
        },
        "versions": {
            CURRENT_BINARY_MODULE_VERSION: "22",
            PREV_BINARY_MODULE_VERSION: "21",
        },
        "released_to_stable": False,
        "contour_name": USER_CONTOUR_NAME,
    },
    {
        "name": TEST_MODULE,
        "paths": {
            CURRENT_BINARY_MODULE_VERSION:
                "maps/garden/libs_server/test_utils/test_module/test_module",
            PREV_BINARY_MODULE_VERSION:
                "maps/garden/common_tests/integration_tests/prev_binaries/prev_test_module"
        },
        "traits": {
            "type": "map",
            "name": TEST_MODULE,
            "sources": [TEST_SRC_MODULE],
            "autostarter": {
                "trigger_by": [TEST_SRC_MODULE],
            },
            "capabilities": [
                "handle_build_status"
            ]
        },
        "versions": {
            CURRENT_BINARY_MODULE_VERSION: "2",
            PREV_BINARY_MODULE_VERSION: "1",
        },
        "released_to_stable": True,
        "contour_name": None,
    },
]


def _upload_remote_module(
    *,
    yt_client_config,
    module_name,
    module_version,
    module_traits,
    modules_remote_dir,
    module_binary_local_path,
    sandbox_task_id
):
    remote_module_dir = ypath_join(modules_remote_dir, module_name)
    remote_module_path = ypath_join(remote_module_dir, module_version)

    yt_client = yt.YtClient(config=yt_client_config)
    yt_client.mkdir(remote_module_dir, recursive=True)

    local_module_path = yatest.common.binary_path(module_binary_local_path)
    module = Module(
        module_name=module_name,
        module_version=module_version,
        module_traits=module_traits,
        sandbox_task_id=sandbox_task_id,
        local_path=local_module_path,
        yt_client_config=yt_client_config,
    )
    module.upload_to(remote_module_path)

    return remote_module_path


class GardenClientHelper:
    def __init__(self, patched_server_config, server_hostname):
        self._patched_server_config = patched_server_config
        self._server_hostname = server_hostname

    def start_build(self, *, module_name, sources, contour_name):
        return self._garden_client_by_contour(contour_name).start_build(
            module_name=module_name,
            build_data={
                "action": {
                    "name":  "create",
                    "params": [
                        {"name": "buildName", "value": module_name},
                        {"name": "contourName", "value": contour_name},
                        {
                            "name": "sources",
                            "value": [
                                "{}:{}".format(source["name"], source["id"]) for source in sources
                            ]
                        }
                    ]
                }
            },
        )

    def wait_build(self, module_name, build_id, status="completed"):
        get_build_kwargs = {
            "module_name": module_name,
            "build_id": build_id,
        }
        garden_client = self._system_contour_garden_client()
        assert common.wait_until(
            lambda: garden_client.get_build(**get_build_kwargs)["progress"]["status"] == status,
            timeout=120, check_interval=1)
        assert not garden_client.get_build(**get_build_kwargs)["progress"].get("error_log_url")

    def build_src_module(self, contour_name):
        src_build_data = {
            "contour": contour_name,
            "resources": [
                {
                    "version": {
                        "properties": {
                            "shipping_date": "20200423"
                        }
                    },
                    "name": "input_resource"
                }
            ],
            "foreign_key": {"shipping_date": "20200423"}
        }
        src_build = self._garden_client_by_contour(contour_name).start_module_build(
            module_name=TEST_SRC_MODULE,
            build_data=src_build_data,
        )
        self.wait_build(TEST_SRC_MODULE, src_build["id"])
        return src_build

    def build_module_from_source(self, src_build, contour_name):
        build = self.start_build(
            module_name=TEST_MODULE,
            sources=[src_build],
            contour_name=contour_name
        )
        build["id"] = build["id"].split(":")[-1]
        self.wait_build(TEST_MODULE, build["id"])
        return build

    def wait_autostarted_build(self, contour_name):
        garden_client = self._garden_client_by_contour(contour_name)

        def check_builds():
            builds = garden_client.get_builds(TEST_MODULE)
            return len(builds) and builds[0]["progress"]["status"] == "completed"

        assert common.wait_until(check_builds, timeout=120, check_interval=1)
        return garden_client.get_builds(TEST_MODULE)[0]

    def init_module_version(self, module_version):
        for module in MODULES:
            if module_version not in module["paths"]:
                continue
            name = module["name"]
            module_path = module["paths"][module_version]
            version = module["versions"][module_version]
            contour_name = module["contour_name"]

            if contour_name:
                garden_client = self._user_contour_garden_client()
                garden_client.create_contour()
            else:
                garden_client = self._system_contour_garden_client()

            remote_ypath = _upload_remote_module(
                yt_client_config=self._patched_server_config["yt"]["config"],
                module_name=name,
                module_version=version,
                module_traits=module["traits"],
                modules_remote_dir="//home/garden/modules",
                module_binary_local_path=module_path,
                sandbox_task_id="1000",
            )
            garden_client.register_module_version(
                module_name=name,
                module_version=version,
                remote_path=remote_ypath,
                module_traits=module["traits"],
                description="test description",
                contour=contour_name,
                sandbox_task_id="1000"
            )

            if module["released_to_stable"]:
                garden_client.release_module_version(
                    module_name=name,
                    module_version=version,
                    environment=yenv.Environment.STABLE,
                    released_at=datetime.now(pytz.utc),
                    released_by="test_user",
                    description="",
                )
            else:
                garden_client.activate_module_version(
                    module_name=name,
                    module_version=version,
                )

    def get_yt_client(self):
        yt_client_config = self._patched_server_config["yt"]["config"]
        return yt.YtClient(config=yt_client_config)

    def start_long_build(self, src_build, contour_name):
        yt_client = self.get_yt_client()
        flag_file = ypath_join(self._patched_server_config["yt"]["prefix"], "flag_file")
        yt_client.create("map_node", self._patched_server_config["yt"]["prefix"], recursive=True, ignore_existing=True)
        yt_client.write_file(flag_file, b"wait")

        build = self.start_build(
            module_name=TEST_MODULE,
            sources=[src_build],
            contour_name=contour_name
        )
        build["id"] = build["id"].split(":")[-1]

        self.wait_build(
            module_name=TEST_MODULE,
            build_id=build["id"],
            status="in_progress",
        )
        return build

    def complete_long_build(self):
        yt_client = self.get_yt_client()
        flag_file = ypath_join(self._patched_server_config["yt"]["prefix"], "flag_file")
        yt_client.write_file(flag_file, b"complete")

    def fail_long_build(self, error="Expected error"):
        yt_client = self.get_yt_client()
        flag_file = ypath_join(self._patched_server_config["yt"]["prefix"], "flag_file")
        yt_client.write_file(flag_file, error.encode())

    def cancel_build(self, build_id):
        self._system_contour_garden_client().cancel_build(TEST_MODULE, build_id)

    def delete_build(self, build_id):
        self._system_contour_garden_client().delete_build(TEST_MODULE, build_id)

    def restart_build(self, build_id):
        self._system_contour_garden_client().restart_build(TEST_MODULE, build_id)

    def get_resources(self, *, module_name, build_id, contour_name):
        return self._garden_client_by_contour(contour_name).get_resources(module_name, build_id)

    def get_builds(self, *, module_name, contour_name):
        return self._garden_client_by_contour(contour_name).get_builds(module_name)

    def scan_resources(self, *, module_name, contour_name):
        return self._garden_client_by_contour(contour_name).scan_resources(module_name)

    def get_build_full_info(self, *, module_name, build_id, contour_name):
        return self._garden_client_by_contour(contour_name).get_build_full_info(module_name, build_id)

    def _garden_client_by_contour(self, contour_name):
        if contour_name == USER_CONTOUR_NAME:
            return self._user_contour_garden_client()
        else:
            return self._system_contour_garden_client()

    def _system_contour_garden_client(self):
        return GardenClient(server_hostname=self._server_hostname, contour_name=SYSTEM_CONTOUR_NAME)

    def _user_contour_garden_client(self):
        return GardenClient(server_hostname=self._server_hostname, contour_name=USER_CONTOUR_NAME)

    def ignore_task(self, *, module_name: str, build_id: str, task_id: str):
        self._system_contour_garden_client().ignore_task(module_name, build_id, task_id)


class RestartableScheduler:
    def __init__(self, patched_server_config):
        self._scheduler = None
        self._scheduler_config = patched_server_config

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args, **kwargs):
        if self._scheduler:
            self.stop()

    def start(self):
        assert not self._scheduler, "Scheduler already started"
        app_scheduler = flask.Flask("scheduler")
        app_scheduler.testing = True
        self._scheduler = run_scheduler.prepare_app(self._scheduler_config, app_scheduler)
        self._scheduler.__enter__()

    def stop(self):
        assert self._scheduler, "Scheduler not started yet"
        self._scheduler.__exit__(None, None, None)
        self._scheduler = None

    def restart(self):
        if self._scheduler:
            self.stop()

        self.start()
