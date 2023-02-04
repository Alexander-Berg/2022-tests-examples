import datetime as dt
import http.client
import pytest
import pytz

from maps.garden.sdk.module_traits import module_traits
from maps.garden.libs_server.build.build_defs import Build
from maps.garden.libs_server.module.storage_interface import ModuleVersionInfo

from maps.garden.libs_server.common.contour_manager import ContourManager

from . import common

NOW = dt.datetime(2016, 11, 23, 12, 25, 22, tzinfo=pytz.utc)


@pytest.mark.parametrize(
    "user_contour",
    [
        None,
        "user_contour",
    ]
)
@pytest.mark.parametrize(
    "test_args",
    [
        {
            "module_type": module_traits.ModuleType.SOURCE,
            "sort_options": [{"key_pattern": "test_pattern"}],
            "tracked_ancestor": True,
            "is_good": True,
        },
        {
            "module_type": module_traits.ModuleType.SOURCE,
            "sources": ["existing_module"],  # not empty source for source module
            "sort_options": [{"key_pattern": "test_pattern"}],
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.SOURCE,
            "triggered_by": ["existing_module"],  # not empty autostarter for source module
            "sort_options": [{"key_pattern": "test_pattern"}],
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.MAP,
            "sources": ["existing_module"],
            "triggered_by": ["existing_module"],
            "track_ancestors_from": ["existing_module"],
            "is_good": True,
        },
        {
            "module_type": module_traits.ModuleType.MAP,
            "sources": ["not_existing_module"],
            "triggered_by": ["existing_module"],
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.MAP,
            "sources": ["existing_module"],
            "triggered_by": ["not_existing_module"],
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.MAP,
            "sources": [],  # empty source
            "triggered_by": ["existing_module"],
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.MAP,
            "sources": ["existing_module"],
            "triggered_by": ["existing_module"],
            "track_ancestors_from": ["not_existing_module"],
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.MAP,
            "sources": ["existing_module"],
            "triggered_by": ["existing_module"],
            "tracked_ancestor": True,  # Can not be tracked
            "is_good": False,
        },
        {
            "module_type": module_traits.ModuleType.SOURCE,
            "track_ancestors_from": ["existing_module"],  # Sorce module can not have tracked ancestors
            "is_good": False,
            "sort_options": [{"key_pattern": "test_pattern"}],
        },
    ]
)
def test_release_module_version(garden_client, module_helper, db, test_args, user_contour):
    traits = module_traits.ModuleTraits(
        name="existing_module",
        type=module_traits.ModuleType.SOURCE,
        sort_options=[module_traits.SortOption(key_pattern="test_pattern")]
    )
    module_helper.add_module_to_system_contour(traits)

    if user_contour:
        contour_manager = ContourManager(db)
        contour_manager.create("user_contour", owner="vasya")
        module_helper.add_module_to_user_contour(traits, user_contour=user_contour)

    request_body = {
        "module_version": "1234",
        "remote_path": "//tmp/path",
        "description": "test description",
        "user_contour": user_contour,
        "module_traits": {
            "name": "module_name",
            "displayed_name": "Some Module",
            "type": test_args["module_type"],
            "sort_options": test_args.get("sort_options", []),
            "sources": test_args.get("sources", []),
            "autostarter": {
                "trigger_by": test_args.get("triggered_by", []),
            },
            "tracked_ancestor": test_args.get("tracked_ancestor", False),
            "track_ancestors_from": test_args.get("track_ancestors_from")
        },
    }

    response = garden_client.post(
        "modules/some_module/versions/",
        json=request_body)

    if user_contour:
        assert (response.status_code == http.client.CREATED) == test_args["is_good"]
        return response.get_json()

    # Below is system contour
    assert response.status_code == http.client.CREATED

    # Try to register the same version again
    response = garden_client.post(
        "modules/some_module/versions/",
        json=request_body)
    assert response.status_code == http.client.CONFLICT

    # Release to stable
    response = garden_client.put(
        "modules/some_module/versions/1234/",
        json={
            "released_to": "stable",
            "released_at": "2019-11-27T08:12:29.187000+00:00",
            "released_by": "test_user"
        })

    assert (response.status_code == http.client.OK) == test_args["is_good"]
    return response.get_json()


def test_get_module_versions(garden_client, module_helper):
    traits = module_traits.ModuleTraits(
        name="some_module",
        type=module_traits.ModuleType.MAP,
    )
    module_helper.add_module_to_system_contour(traits)
    module_helper.add_module_to_system_contour(traits, description="description1")
    module_helper.add_module_to_user_contour(traits, user_contour="contour3")
    module_helper.add_module_to_user_contour(traits, user_contour="contour4", description="description1")
    module_helper.add_module_to_user_contour(traits, user_contour="contour4", description="description2")

    version1 = ModuleVersionInfo(
        module_version="1",
        sandbox_task_id=1001,
        remote_path="//home/garden/prod/modules/some_module/1",
        module_traits=traits)
    version2 = ModuleVersionInfo(
        module_version="2",
        sandbox_task_id=1002,
        remote_path="//home/garden/prod/modules/some_module/2",
        module_traits=traits,
        description="description1")
    version3 = ModuleVersionInfo(
        module_version="3",
        sandbox_task_id=1003,
        remote_path="//home/garden/prod/modules/some_module/3",
        module_traits=traits,
        user_contour="contour3")
    version4 = ModuleVersionInfo(
        module_version="4",
        sandbox_task_id=1004,
        remote_path="//home/garden/prod/modules/some_module/4",
        module_traits=traits,
        user_contour="contour4",
        description="description1")
    version5 = ModuleVersionInfo(
        module_version="5",
        sandbox_task_id=1005,
        remote_path="//home/garden/prod/modules/some_module/5",
        module_traits=traits,
        user_contour="contour4",
        description="description2")

    response = garden_client.get("modules/some_module/versions/")
    assert response.status_code == http.client.OK

    assert response.get_json() == [
        v.dict(exclude_none=True)
        for v in (version1, version2, version3, version4, version5)]

    response = garden_client.get("modules/some_module/versions/?contour=unknown_contour")
    assert response.status_code == http.client.OK  # FIXME? Should return BAD_REQUEST or NOT_FOUND?

    response = garden_client.get("modules/some_module/versions/?contour=contour4")
    assert response.status_code == http.client.OK
    assert response.get_json() == [version4.dict(exclude_none=True), version5.dict(exclude_none=True)]

    response = garden_client.get("modules/some_module/versions/?module_version=1")
    assert response.status_code == http.client.OK
    assert response.get_json() == [version1.dict(exclude_none=True)]

    response = garden_client.get("modules/some_module/versions/?description=description1")
    assert response.status_code == http.client.OK
    assert response.get_json() == [version2.dict(exclude_none=True), version4.dict(exclude_none=True)]


def test_remove_module_version(garden_client, db, module_helper, mocker):
    # Prepare data
    contour_manager = ContourManager(db)
    contour_manager.create("contour1", owner="vasya")

    traits = module_traits.ModuleTraits(
        name="some_module",
        type=module_traits.ModuleType.MAP,
    )
    version1 = module_helper.add_module_to_system_contour(traits)
    version2 = module_helper.add_module_to_user_contour(traits, user_contour="contour1")
    version3 = module_helper.add_module_to_user_contour(traits, user_contour="contour1")

    source_build = Build(
        id=1,
        name="some_module",
        contour_name=common.DEFAULT_SYSTEM_CONTOUR,
        module_version="3",
    )
    db.builds.insert_one(source_build.dict())

    # Do requests

    response = garden_client.delete(f"modules/some_module/versions/{version1}/")
    assert response.status_code == http.client.FORBIDDEN  # failed to remove system contour version

    common.mock_auth(mocker, "notvasya")

    response = garden_client.delete(f"modules/some_module/versions/{version2}/")
    assert response.status_code == http.client.FORBIDDEN  # failed to remove non-user contour version

    common.mock_auth(mocker, "vasya")

    response = garden_client.delete(f"modules/some_module/versions/{version2}/")
    assert response.status_code == http.client.OK

    response = garden_client.delete(f"modules/some_module/versions/{version3}/")
    assert response.status_code == http.client.CONFLICT  # failed to remove due to existing builds


@pytest.mark.freeze_time(NOW)
def test_module_release_info(garden_client, module_helper):
    some_module_traits = module_traits.ModuleTraits(
        name="some_module",
        type=module_traits.ModuleType.MAP,
    )
    module_helper.add_module_to_system_contour(some_module_traits)  # version 1
    module_helper.add_module_to_user_contour(some_module_traits, user_contour="contour")

    response = garden_client.get("/modules/some_module/release_info/")
    assert response.status_code == http.client.OK
    return response.get_json()
