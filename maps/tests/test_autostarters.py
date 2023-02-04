import datetime as dt
import http.client
import pytest
import pytz

from maps.garden.sdk.module_traits import module_traits as mt
from maps.garden.libs_server.build.build_defs import Build
from maps.garden.libs_server.common.contour_manager import ContourManager

from . import common

TEST_TARGET_MODULE_NAME = "target_module_name"
TEST_CONTOUR_NAME = "contour_name"
TEST_NOT_EXISTING_MODULE_NAME = "not_existing_module_name"
TEST_NOT_EXISTING_CONTOUR_NAME = "not_existing_contour_name"
TEST_TRIGGER_MODULE_NAME = "trigger_module_name"
TEST_TRIGGER_BUILD_ID = 1
TEST_OWNER = "vasya"


def test_success(garden_client, module_helper):
    module_helper.add_module_to_system_contour(
        mt.ModuleTraits(
            name="ymapsdf",
            type=mt.ModuleType.MAP,
        )
    )
    module_helper.add_module_to_system_contour(
        mt.ModuleTraits(
            name="graph_build",
            type=mt.ModuleType.REDUCE,
            autostarter=mt.ModuleAutostarter(
                trigger_by=["ymapsdf"]
            )
        )
    )

    response = garden_client.get("/autostarters/")
    assert response.status_code == http.client.OK
    assert response.data == b"ymapsdf                             -> graph_build"


@pytest.mark.parametrize(
    "contour_name,target_module_name,trigger_module_name,trigger_build_id,user_name,expected_code",
    [
        (
            TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, TEST_TRIGGER_MODULE_NAME, TEST_TRIGGER_BUILD_ID,
            TEST_OWNER, http.client.OK
        ),
        (
            TEST_NOT_EXISTING_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, TEST_TRIGGER_MODULE_NAME, TEST_TRIGGER_BUILD_ID,
            TEST_OWNER, http.client.NOT_FOUND
        ),
        (
            TEST_CONTOUR_NAME, TEST_NOT_EXISTING_MODULE_NAME, TEST_TRIGGER_MODULE_NAME, TEST_TRIGGER_BUILD_ID,
            TEST_OWNER, http.client.NOT_FOUND
        ),
        (
            TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, TEST_NOT_EXISTING_MODULE_NAME, TEST_TRIGGER_BUILD_ID,
            TEST_OWNER, http.client.NOT_FOUND
        ),
        (
            TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, TEST_TRIGGER_MODULE_NAME, TEST_TRIGGER_BUILD_ID + 1,
            TEST_OWNER, http.client.NOT_FOUND
        ),
        (
            TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, TEST_TRIGGER_MODULE_NAME, TEST_TRIGGER_BUILD_ID,
            "forbidden_person", http.client.FORBIDDEN
        )
    ]
)
def test_trigger_autostart(
    # parameters:
    contour_name, target_module_name, trigger_module_name, trigger_build_id, user_name, expected_code,
    # fixtures:
    garden_client, db, module_helper, mocker
):
    # Prepare data
    contour_manager = ContourManager(db)
    contour_manager.create(TEST_CONTOUR_NAME, owner=TEST_OWNER)

    module_helper.add_module_to_user_contour(
        mt.ModuleTraits(
            name=TEST_TRIGGER_MODULE_NAME,
            type=mt.ModuleType.MAP,
        ),
        user_contour=TEST_CONTOUR_NAME
    )
    module_helper.add_module_to_user_contour(
        mt.ModuleTraits(
            name=TEST_TARGET_MODULE_NAME,
            type=mt.ModuleType.REDUCE,
        ),
        user_contour=TEST_CONTOUR_NAME
    )

    db.builds.insert_one(
        Build(
            id=TEST_TRIGGER_BUILD_ID,
            name=TEST_TRIGGER_MODULE_NAME,
            contour_name=common.DEFAULT_SYSTEM_CONTOUR,
            module_version="3",
        ).dict()
    )

    # Parameterized test
    common.mock_auth(mocker, user_name)
    response = garden_client.post(
        f"/modules/{target_module_name}/trigger_autostart/?"
        f"contour={contour_name}&trigger_module={trigger_module_name}&"
        f"trigger_build_id={trigger_build_id}"
    )
    assert response.status_code == expected_code


@pytest.mark.parametrize(
    "contour_name,target_module_name,insert_autostart_request,user_name,expected_code",
    [
        (TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, True, TEST_OWNER, http.client.OK),
        (TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, False, TEST_OWNER, http.client.NOT_FOUND),
        (TEST_CONTOUR_NAME, TEST_NOT_EXISTING_MODULE_NAME, True, TEST_OWNER, http.client.NOT_FOUND),
        (TEST_NOT_EXISTING_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, True, TEST_OWNER, http.client.NOT_FOUND),
        (TEST_CONTOUR_NAME, TEST_TARGET_MODULE_NAME, True, "forbidden_person", http.client.FORBIDDEN)
    ]
)
def test_cancel_autostart(
    # parameters:
    contour_name, target_module_name, insert_autostart_request, user_name, expected_code,
    # fixtures:
    garden_client, db, module_helper, mocker
):
    # Prepare data
    contour_manager = ContourManager(db)
    contour_manager.create(TEST_CONTOUR_NAME, owner=TEST_OWNER)

    module_helper.add_module_to_user_contour(
        mt.ModuleTraits(
            name=TEST_TARGET_MODULE_NAME,
            type=mt.ModuleType.REDUCE,
        ),
        user_contour=TEST_CONTOUR_NAME
    )

    # Parameterized test
    if insert_autostart_request:
        db.autostart_requests.insert_one(
            {
                "trigger_module_name": TEST_TRIGGER_MODULE_NAME,
                "trigger_build_id": TEST_TRIGGER_BUILD_ID,
                "target_contour_name": TEST_CONTOUR_NAME,
                "target_module_name": TEST_TARGET_MODULE_NAME,
                "delayed_run_at": dt.datetime(2020, 12, 25, 18, 45, tzinfo=pytz.utc)
            }
        )

    common.mock_auth(mocker, user_name)
    response = garden_client.post(
        f"/modules/{target_module_name}/cancel_autostart/?contour={contour_name}"
    )
    assert response.status_code == expected_code
