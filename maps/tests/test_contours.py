import http.client

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType

from maps.garden.server.lib.formats.api_objects import (
    ContourCreationInfo,
    FullContourInfo,
    ContoursInfo
)

from maps.garden.libs_server.common.contour_manager import ContourStatus

from . import common


def test_get_permanent_contours(garden_client):
    response = garden_client.get("/contours/")
    result = ContoursInfo.parse_obj(response.get_json()).items
    assert len(result) == 1
    assert result[0].name == "unittest"
    assert result[0].is_system
    assert result[0].status == ContourStatus.ACTIVE

    response = garden_client.get("/contours/unittest")
    result = FullContourInfo.parse_obj(response.get_json())
    assert result.name == "unittest"
    assert result.is_system
    assert result.status == ContourStatus.ACTIVE


def test_get_nonexistent_contour(garden_client):
    response = garden_client.get("/contours/vasya_nonexistent")
    assert response.status_code == http.client.NOT_FOUND


def test_delete_nonexistent_contour(garden_client):
    response = garden_client.delete("/contours/vasya_nonexistent")
    assert response.status_code == http.client.NOT_FOUND


def test_delete_contour_of_other_user(garden_client):
    response = garden_client.delete("/contours/petya_nonexistent")
    assert response.status_code == http.client.NOT_FOUND


def test_create_contour_without_user(garden_client, mocker):
    common.mock_auth(mocker, None)
    creation_info = ContourCreationInfo(name="vasya_test")

    response = garden_client.post("/contours/", json=creation_info.dict())
    assert response.status_code == http.client.FORBIDDEN


def test_create_contour_of_other_user(garden_client):
    creation_info = ContourCreationInfo(name="petya_test")
    response = garden_client.post("/contours/", json=creation_info.dict())
    assert response.status_code == http.client.BAD_REQUEST


def test_create_contour(garden_client, mocker):
    common.mock_auth(mocker, "vasya")

    creation_info = ContourCreationInfo(name="vasya_test")

    response = garden_client.post("/contours/", json=creation_info.dict())
    result = FullContourInfo.parse_obj(response.get_json())
    assert result.name == "vasya_test"
    assert result.status == ContourStatus.ACTIVE

    # Try to create a new contour with the same name
    response = garden_client.post("/contours/", json=creation_info.dict())
    assert response.status_code == http.client.CONFLICT

    response = garden_client.get("/contours/")
    result = ContoursInfo.parse_obj(response.get_json()).items
    assert len(result) == 2
    assert result[1].name == "vasya_test"
    assert not result[1].is_system
    assert result[1].status == ContourStatus.ACTIVE

    response = garden_client.get("/contours/vasya_test")
    result = FullContourInfo.parse_obj(response.get_json())
    assert result.name == "vasya_test"
    assert not result.is_system
    assert result.status == ContourStatus.ACTIVE

    response = garden_client.delete("/contours/vasya_test")
    assert response.status_code == http.client.OK

    response = garden_client.get("/contours/vasya_test")
    result = FullContourInfo.parse_obj(response.get_json())
    assert result.name == "vasya_test"
    assert result.status == ContourStatus.DELETING


def test_activate_module_version(garden_client, module_helper):
    creation_info = ContourCreationInfo(name="someuser_test")
    garden_client.post("/contours/", json=creation_info.dict())

    traits = ModuleTraits(
        name="ymapsdf",
        type=ModuleType.MAP,
    )
    module_helper.add_module_to_system_contour(traits)

    response = garden_client.post(
        "/contours/someuser_test/modules/ymapsdf/activate_version/?module_version=123")
    assert response.status_code == http.client.NOT_FOUND

    response = garden_client.post(
        "/contours/someuser_test/modules/ymapsdf/activate_version/?module_version=1")
    assert response.status_code == http.client.OK
