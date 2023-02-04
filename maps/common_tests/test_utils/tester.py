import copy
import http.client
import json
import logging

from yandex.maps.test_utils.common import wait_until as wait_until_common

from maps.garden.libs_server.build.build_defs import BuildOperationString
from maps.garden.common_tests.test_utils.constants import RESOURCE_TEMPLATE

logger = logging.getLogger("garden.tester")


RESOURCES = [
    json.loads(RESOURCE_TEMPLATE %
               ("europe_src", "europe", "20201011", "navteq")),
    json.loads(RESOURCE_TEMPLATE %
               ("australia_src", "australia", "20201011", "navteq"))
]

RESOURCES_OLD = [
    json.loads(RESOURCE_TEMPLATE %
               ("europe_src", "europe", "20201010", "navteq")),
    json.loads(RESOURCE_TEMPLATE %
               ("australia_src", "australia", "20201010", "navteq"))
]

SRC_RESOURCES = [
    {
        "name": "europe_src",
        "version": {
            "properties": {
                "region": "europe",
                "shipping_date": "20201011",
                "vendor": "navteq",
            },
        },
    },
    {
        "name": "europe_src",
        "version": {
            "properties": {
                "region": "europe",
                "shipping_date": "20201012",
                "vendor": "navteq",
            },
        },
    },
    {
        "name": "australia_src",
        "version": {
            "properties": {
                "region": "australia",
                "shipping_date": "20201011",
                "vendor": "navteq",
            },
        },
    },
]

RASP_RESOURCES = [
    {
        "name": "rasp_export_src",
        "version": {
            "properties": {
                "release_name": "A",
                "shipping_date": "2016",
            },
        },
    },
    {
        "name": "rasp_export_src",
        "version": {
            "properties": {
                "release_name": "B",
                "shipping_date": "2017",
            },
        },
    },
]


# Syncronized with the return value of `lib.utils.default_contour_name`
DEFAULT_SYSTEM_CONTOUR = "unittest"

BUILD_URL_TEMPLATE = "/modules/{module_name}/builds/{build_id}/"


def update_shipping_date(input_res):
    res = copy.deepcopy(input_res)
    res["version"]["properties"]["shipping_date"] = str(
        int(input_res["version"]["properties"]["shipping_date"]) + 1
    )

    return res


DEFAULT_WAITUNTIL_TIMEOUT = 30  # seconds
DEFAULT_WAITUNTIL_CHECK_INTERVAL = 0.3  # seconds


def wait_until(*args, **kwargs):
    kwargs.setdefault("timeout", DEFAULT_WAITUNTIL_TIMEOUT)
    kwargs.setdefault("check_interval", DEFAULT_WAITUNTIL_CHECK_INTERVAL)
    return wait_until_common(*args, **kwargs)


MODULES_WITH_AUTOSTARTERS = (
    "ymapsdf",
    "denormalization",
    "world_creator_deployment",
    "world_with_params_creator",
)


class BuildsHelper:
    def __init__(self, garden_client):
        self.garden_client = garden_client

        for module in MODULES_WITH_AUTOSTARTERS:
            self.disable_module_autostart(module)

    def build_src(self, module_name, resource_info, contour_name=DEFAULT_SYSTEM_CONTOUR):
        """
        Create a new source build and wait for its completion
        """
        properties = resource_info["version"]["properties"]
        foreign_key = {"shipping_date": properties["shipping_date"]}
        region = properties.get("region")
        if region:
            foreign_key["region"] = region

        data = {
            "contour": contour_name,
            "resources": [resource_info],
            "foreign_key": foreign_key,
        }
        response = self.garden_client.post(f"/modules/{module_name}/builds/", json=data)
        assert response.status_code == http.client.CREATED
        result = response.get_json()
        build = {
            "module_name": result["name"],
            "build_id": result["id"],
            "version": result["version"],
        }
        self.wait_for_build(build)
        return build

    def get_build_info(self, module_name, build_id):
        return self.garden_client.get(f"/modules/{module_name}/builds/{build_id}/").get_json()

    def build_ymapsdf_src(self, resource_info, contour_name=DEFAULT_SYSTEM_CONTOUR):
        return self.build_src("ymapsdf_src", resource_info, contour_name)

    def start_module(
            self,
            module_name,
            sources,
            contour_name=DEFAULT_SYSTEM_CONTOUR,
            expected_code=http.client.CREATED,
            release_name=None,
            ignore_warnings=None):
        """
        Create a new build
        """
        data = {
            "action": {
                "name":  BuildOperationString.CREATE,
                "params": [
                    {"name": "buildName", "value": module_name},
                    {"name": "contourName", "value": contour_name},
                    {
                        "name": "sources",
                        "value": [
                            "{module_name}:{build_id}".format(**source) for source in sources
                        ]
                    }
                ]
            }
        }
        if release_name:
            data["action"]["params"].append({"name": "releaseName", "value": release_name})
        if ignore_warnings is not None:
            data["action"]["params"].append({"name": "ignoreWarnings", "value": ignore_warnings})
        response = self.garden_client.post("/build/", json=data)
        assert response.status_code == expected_code,\
            f"Wrong status_code {response.status_code}. Expected {expected_code}"
        result = response.get_json()
        if expected_code == http.client.CREATED:
            module_name, build_id = result["id"].split(":")
            return {
                "module_name": module_name,
                "build_id": build_id,
                "version": f"{module_name}:{build_id}",
                "url": result["url"],
            }
        else:
            return result

    def start_ymapsdf(
            self,
            ymapsdf_src,
            contour_name=DEFAULT_SYSTEM_CONTOUR,
            expected_code=http.client.CREATED):
        return self.start_module(
            "ymapsdf", [ymapsdf_src],
            contour_name=contour_name, expected_code=expected_code)

    def build_module(
            self,
            module_name,
            sources,
            contour_name=DEFAULT_SYSTEM_CONTOUR,
            expected_status="completed",
            release_name=None,
            ignore_warnings=None):
        """
        Create a new build and wait for its completion
        """
        build = self.start_module(
            module_name, sources,
            contour_name=contour_name, release_name=release_name, ignore_warnings=ignore_warnings)
        self.wait_for_build(build, expected_status)
        return build

    def build_ymapsdf(
            self,
            ymapsdf_src,
            contour_name=DEFAULT_SYSTEM_CONTOUR,
            expected_status="completed"):
        return self.build_module(
            "ymapsdf", [ymapsdf_src],
            contour_name=contour_name, expected_status=expected_status)

    def wait_for_build(self, build, expected_status="completed"):
        terminal_statuses = ["failed", "completed", "cancelled"]

        build_url = BUILD_URL_TEMPLATE.format(**build)

        def get_status():
            response = self.garden_client.get(build_url)
            assert response.status_code == http.client.OK
            return response.get_json()["progress"]["status"]

        wait_until(lambda: get_status() in terminal_statuses)

        status = get_status()
        assert status in expected_status, f"Got unexpected status '{status}'"

    def build_exists(self, build):
        build_url = BUILD_URL_TEMPLATE.format(**build)
        response = self.garden_client.get(build_url)
        if response.status_code == http.client.OK:
            return True
        elif response.status_code == http.client.NOT_FOUND:
            return False
        else:
            assert False, f"Unexpected response code {response.status_code}"

    def delete_build(self, build, expected_code=http.client.OK):
        """
        Start build deletion and wait until it is deleted
        """
        data = {
            "action": {"name":  BuildOperationString.REMOVE},
            "ids": ["{module_name}:{build_id}".format(**build)],
        }
        response = self.garden_client.post("/build/", json=data)
        assert response.status_code == expected_code
        if expected_code == http.client.OK:
            assert wait_until(lambda: not self.build_exists(build))
        return response.get_json()

    def get_scheduled_tasks_count(self):
        data = self.garden_client.get("unistat/").json
        return next((value for key, value in data if key == "scheduled_tasks"), -1)

    def check_resources_count(self, name, count):
        assert wait_until(
            lambda: len(self.garden_client.get(f"storage/?name={name}").json) == count
        )

    def module_builds(self, module_name):
        response = self.garden_client.get(f"modules/{module_name}/builds/")
        assert response.status_code == http.client.OK
        return response.get_json()

    def enable_module_autostart(self, module_name):
        response = self.garden_client.post(
            f"modules/{module_name}/enable-autostart/?contour={DEFAULT_SYSTEM_CONTOUR}")
        assert response.status_code == http.client.OK

    def disable_module_autostart(self, module_name):
        response = self.garden_client.post(
            f"modules/{module_name}/disable-autostart/?contour={DEFAULT_SYSTEM_CONTOUR}")
        assert response.status_code == http.client.OK


def expected_error_msg(message, type, **kwargs):
    result = copy.deepcopy(kwargs)
    result["message"] = message
    result["type"] = type
    return result
