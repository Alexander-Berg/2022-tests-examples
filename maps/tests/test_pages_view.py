import datetime
import http.client
import pytest
import pytz

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType, FillMissingPolicy, SortOption
from maps.garden.libs_server.build.build_defs import Build, BuildStatus, Dataset, BuildExternalResource, Source
from maps.garden.libs_server.build.datasets_registrar import DatasetsRegistrar
from maps.garden.libs_server.common.contour_manager import ContourManager

from maps.garden.server.lib.formats.api_objects import ModuleVersionDescriptor, PageInfo

from . import common

NOW = datetime.datetime(2016, 11, 23, 12, 25, 22, tzinfo=pytz.utc)

USER_CONTOUR = "apollo_test_contour"

SYSTEM_CONTOUR_TRAITS = [
    ModuleTraits(
        name="source_module",
        displayed_name="Source Module",
        groups=["group2", "group1"],
        type=ModuleType.SOURCE,
        sort_options=[SortOption(key_pattern="test_pattern")]),
    ModuleTraits(
        name="source_config_module",
        displayed_name="Source Config Module",
        groups=["group1"],
        type=ModuleType.SOURCE,
        sort_options=[SortOption(key_pattern="test_pattern")]),
    ModuleTraits(
        name="map_module",
        displayed_name="Map Module",
        type=ModuleType.MAP,
        sources=["source_module"],
        configs=["source_config_module"]),
    ModuleTraits(
        name="reduce_module",
        displayed_name="Reduce Module",
        type=ModuleType.REDUCE,
        sources=["map_module"]),
    ModuleTraits(
        name="deployment_module",
        displayed_name="Deployment Module",
        groups=["group1"],
        type=ModuleType.DEPLOYMENT,
        sources=["reduce_module"]),
]

USER_CONTOUR_ADDITIONAL_TRAITS = [
    ModuleTraits(
        name="my_module",
        displayed_name="My Module",
        type=ModuleType.MAP)
]


def _create_build(id, module_name, contour_name, source_builds=None, extras=None, foreign_key=None):
    if source_builds:
        sources = [Source.generate_from(b) for b in source_builds]
    else:
        sources = []

    return Build(
        id=id,
        name=module_name,
        contour_name=contour_name,
        sources=sources,
        request_id=1,
        extras=extras or {},
        output_resources_keys=set(),
        created_at=NOW,
        status=BuildStatus.create_completed(),
        fill_missing_policy=FillMissingPolicy(),
        foreign_key=foreign_key
    )


@pytest.fixture
def prepare_contours(db):
    contour_manager = ContourManager(db)
    contour_manager.create(USER_CONTOUR, "apollo")


@pytest.fixture
def prepare_traits(module_helper):
    for traits in SYSTEM_CONTOUR_TRAITS:
        module_helper.add_module_to_system_contour(traits)

    for traits in USER_CONTOUR_ADDITIONAL_TRAITS:
        module_helper.add_module_to_user_contour(traits, user_contour=USER_CONTOUR)


@pytest.mark.parametrize(
    "contour_name",
    [common.DEFAULT_SYSTEM_CONTOUR, USER_CONTOUR],
)
def test_all_pages(garden_client, prepare_contours, prepare_traits, contour_name):
    response = garden_client.get(f"/pages/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


def test_nonexistent_module(garden_client, prepare_contours, prepare_traits):
    response = garden_client.get("/pages/nonexistent/")
    assert response.status_code == http.client.NOT_FOUND


def test_nonexistent_contour(garden_client, prepare_contours, prepare_traits):
    response = garden_client.get("/pages/?contour=nonexistent")
    assert response.status_code == http.client.NOT_FOUND

    response = garden_client.get("/pages/source_module/?contour=nonexistent")
    assert response.status_code == http.client.NOT_FOUND


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "contour_name",
    [common.DEFAULT_SYSTEM_CONTOUR, USER_CONTOUR],
)
def test_source_module_page(garden_client, db, prepare_contours, prepare_traits, contour_name):
    module_name = "source_module"
    db.builds.insert_one(
        _create_build(1, module_name, common.DEFAULT_SYSTEM_CONTOUR, foreign_key={"some_key": "some_value"}).dict())
    db.builds.insert_one(
        _create_build(2, module_name, USER_CONTOUR, foreign_key={"some_key": "another_value"}).dict())

    datasets_registrar = DatasetsRegistrar(db)
    datasets_registrar.actualize_datasets(
        module_name=module_name,
        contour_name=common.DEFAULT_SYSTEM_CONTOUR,
        datasets=[
            Dataset(
                module_name=module_name,
                contour_name=common.DEFAULT_SYSTEM_CONTOUR,
                foreign_key={"some_key": "some_value"},
                properties={"release_name": "123123"},
                resources=[
                    BuildExternalResource(
                        resource_name="some_resource",
                        properties={}
                    )
                ]
            ),
            Dataset(
                module_name=module_name,
                contour_name=common.DEFAULT_SYSTEM_CONTOUR,
                foreign_key={"some_key": "third_value"},
                properties={"release_name": "7777"},
                resources=[
                    BuildExternalResource(
                        resource_name="some_resource",
                        properties={}
                    )
                ]
            )
        ]
    )

    response = garden_client.get(f"/pages/source_module/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "contour_name",
    [common.DEFAULT_SYSTEM_CONTOUR, USER_CONTOUR],
)
def test_map_module_page(garden_client, db, prepare_contours, prepare_traits, contour_name):
    system_source_build = _create_build(1, "source_module", common.DEFAULT_SYSTEM_CONTOUR)
    db.builds.insert_one(system_source_build.dict())

    user_source_build = _create_build(2, "source_module", USER_CONTOUR)
    db.builds.insert_one(user_source_build.dict())

    system_config_build = _create_build(3, "source_config_module", common.DEFAULT_SYSTEM_CONTOUR)
    db.builds.insert_one(system_config_build.dict())

    user_config_build = _create_build(4, "source_config_module", USER_CONTOUR)
    db.builds.insert_one(user_config_build.dict())

    db.builds.insert_one(
        _create_build(5, "map_module", common.DEFAULT_SYSTEM_CONTOUR, [system_source_build, system_config_build]).dict())
    db.builds.insert_one(
        _create_build(6, "map_module", USER_CONTOUR, [user_source_build, user_config_build]).dict())

    response = garden_client.get(f"/pages/map_module/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "contour_name",
    [common.DEFAULT_SYSTEM_CONTOUR, USER_CONTOUR],
)
def test_reduce_module_page(garden_client, db, prepare_contours, prepare_traits, contour_name):
    system_build = _create_build(1, "map_module", common.DEFAULT_SYSTEM_CONTOUR)
    db.builds.insert_one(system_build.dict())

    user_build = _create_build(2, "map_module", USER_CONTOUR)
    db.builds.insert_one(user_build.dict())

    db.builds.insert_one(
        _create_build(3, "reduce_module", common.DEFAULT_SYSTEM_CONTOUR, [system_build]).dict())
    db.builds.insert_one(
        _create_build(4, "reduce_module", USER_CONTOUR, [user_build]).dict())

    response = garden_client.get(f"/pages/reduce_module/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "contour_name",
    [common.DEFAULT_SYSTEM_CONTOUR, USER_CONTOUR],
)
def test_deployment_module_page(garden_client, db, prepare_contours, prepare_traits, contour_name):
    system_build = _create_build(1, "reduce_module", common.DEFAULT_SYSTEM_CONTOUR)
    db.builds.insert_one(system_build.dict())

    user_build = _create_build(2, "reduce_module", USER_CONTOUR)
    db.builds.insert_one(user_build.dict())

    db.builds.insert_one(
        _create_build(3, "deployment_module", common.DEFAULT_SYSTEM_CONTOUR, [system_build]).dict())
    db.builds.insert_one(
        _create_build(4, "deployment_module", USER_CONTOUR, [user_build]).dict())

    response = garden_client.get(f"/pages/deployment_module/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "contour_name",
    [common.DEFAULT_SYSTEM_CONTOUR, USER_CONTOUR],
)
def test_deployment_module_page_with_steps(garden_client, db, module_helper, prepare_contours, contour_name):
    traits = ModuleTraits(
        name="reduce_module",
        type=ModuleType.REDUCE)
    module_helper.add_module_to_system_contour(traits)

    traits = ModuleTraits(
        name="deployment_module",
        displayed_name="Deployment Module",
        type=ModuleType.DEPLOYMENT,
        sources=["reduce_module"],
        deploy_steps=["prestable", "stable"])
    module_helper.add_module_to_system_contour(traits)

    system_build = _create_build(1, "reduce_module", common.DEFAULT_SYSTEM_CONTOUR)
    db.builds.insert_one(system_build.dict())

    user_build = _create_build(2, "reduce_module", USER_CONTOUR)
    db.builds.insert_one(user_build.dict())

    build = Build(
        id=3,
        name="deployment_module",
        contour_name=common.DEFAULT_SYSTEM_CONTOUR,
        extras={"deploy_step": "prestable"},
        sources=[Source.generate_from(system_build)],
        status=BuildStatus.create_completed(),
    )
    db.builds.insert_one(build.dict())

    build = Build(
        id=4,
        name="deployment_module",
        contour_name=USER_CONTOUR,
        extras={"deploy_step": "stable"},
        sources=[Source.generate_from(user_build)],
        status=BuildStatus.create_completed(),
    )
    db.builds.insert_one(build.dict())

    build = Build(
        id=5,
        name="deployment_module",
        contour_name=USER_CONTOUR,
        extras={"deploy_step": "outdated"},
        sources=[Source.generate_from(user_build)],
        status=BuildStatus.create_completed(),
    )
    db.builds.insert_one(build.dict())

    response = garden_client.get(f"/pages/deployment_module/?contour={contour_name}")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.freeze_time(NOW)
def test_module_versions_in_contour(garden_client, module_helper):
    traits = ModuleTraits(
        name="ymapsdf",
        type=ModuleType.MAP,
    )
    module_helper.add_module_to_system_contour(traits)

    response = garden_client.get(f"/pages/ymapsdf/?contour={common.DEFAULT_SYSTEM_CONTOUR}")
    assert response.status_code == http.client.OK
    result = PageInfo.parse_obj(response.get_json())
    assert result.moduleVersions == [
        ModuleVersionDescriptor(
            displayedName="1 (released by @tester at 2016-11-23 15:25:22 MSK)",
            version="1",
            isActive=True,
            url="https://a.yandex-team.ru/arc/commit/1"
        )
    ]
