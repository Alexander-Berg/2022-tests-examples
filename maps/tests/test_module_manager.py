import datetime as dt
import os
import shutil
from unittest import mock
import pytest
import pytz

import yatest.common

from maps.pylibs.yandex_environment import environment as yenv

from maps.garden.sdk.utils.contour import default_contour_name
from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType
from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as proto_command

from maps.garden.libs_server.common.contour_manager import ContourManager, Contour, ContourStatus
from maps.garden.libs_server.common.errors import ModuleNotFoundException
from maps.garden.libs_server.module import communicate
from maps.garden.libs_server.module import module_manager
from maps.garden.libs_server.module.module import Module
from maps.garden.libs_server.module.storage_interface import ModuleVersionInfo, ModuleReleaseInfo

DATATESTING = "datatesting"
TESTING = str(yenv.Environment.TESTING)
STABLE = str(yenv.Environment.STABLE)
DEVELOPMENT = str(yenv.Environment.DEVELOPMENT)
ALL_ENVIRONMENTS = [
    yenv.Environment.DEVELOPMENT,
    yenv.Environment.TESTING,
    yenv.Environment.STABLE
]

NOW = dt.datetime(2020, 9, 25, 14, 25, 22, tzinfo=pytz.utc)
MODULE_NAME = "test_module"
USER_NAME = "test_user"
MODULE_TO_TEST = 'maps/garden/libs_server/test_utils/test_module/test_module'


@pytest.fixture
def yt_client():
    yt_client = mock.Mock()
    with mock.patch("yt.wrapper.YtClient", return_value=yt_client):
        transaction = mock.Mock()
        transaction.__enter__ = mock.Mock(return_value=None)
        transaction.__exit__ = mock.Mock(return_value=None)
        yt_client.Transaction.return_value = transaction
        yield yt_client


@pytest.fixture
def contour_manager(db):
    for contour_name in (DEVELOPMENT, TESTING, DATATESTING, STABLE):
        contour = Contour(
            _id=contour_name,
            is_system=True,
            status=ContourStatus.ACTIVE,
        )
        db.contours.insert_one(contour.dict())
    return ContourManager(db)


@pytest.fixture
def storage(db, contour_manager):
    # Override stable settings to simplify tests
    module_manager._SERVER_ENVIRONMENT_CLEANUP_POLICY[yenv.Environment.STABLE] = {
        yenv.Environment.STABLE: 3,
        yenv.Environment.TESTING: 3,
        yenv.Environment.DEVELOPMENT: 3,
    }

    yield module_manager.ModuleManager(
        config={
            "yt": {
                "config": None,  # real yt client is not constructed
                "prefix": "//home/garden"
            },
            "sandbox": {"token": "fake"},
            "isolated_modules_cache_dir": "./cache"
        },
        db=db,
    )


@pytest.mark.freeze_time(NOW)
def test_register_module_version(yt_client, storage, db):
    module_traits = ModuleTraits(name=MODULE_NAME, type=ModuleType.MAP)
    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="111",
            remote_path="//tmp/some_path",
            description="some description",
            module_traits=module_traits,
            sandbox_task_id="1234",
        ),
        USER_NAME)
    _release_module_version(storage, MODULE_NAME, "111", yenv.Environment.STABLE)

    # Check filled module data for the stored contour_name="stable"

    module = storage.get_module(MODULE_NAME, "111", STABLE)
    assert module
    assert isinstance(module, Module)
    assert module.remote_path == "//home/garden/modules/{}/111".format(MODULE_NAME)
    assert module.version == "111"
    assert module.name == MODULE_NAME
    assert module.sandbox_task_id == "1234"
    assert module.traits == module_traits
    yt_client.exists.called_with_with("//home/garden/modules/{}/111".format(MODULE_NAME))
    yt_client.copy.called_with_with("//tmp/some_path", "//home/garden/modules/{}/111".format(MODULE_NAME))

    assert storage.get_modules_versions(contour_name=STABLE) == {MODULE_NAME: ["111"]}

    expected_release_info = ModuleReleaseInfo(
        module_name=MODULE_NAME,
        released_at=NOW.isoformat(),
        module_version="111",
        sandbox_task_id="1234",
    )
    release_info = storage.get_module_release_info(MODULE_NAME)
    assert release_info.get(STABLE) == expected_release_info

    release_infos = storage.get_all_modules_release_info()
    assert release_infos.get(MODULE_NAME) == {
        DEVELOPMENT: expected_release_info,
        STABLE: expected_release_info,
    }

    assert storage.get_module_traits(MODULE_NAME, contour_name=STABLE) == module_traits

    assert storage.get_all_modules_traits(contour_name=STABLE) == [module_traits]

    assert db.module_versions.find_one({"version": "111"})["traits"] == {
        "name": MODULE_NAME,
        "type": "map",
    }

    assert storage.get_version_registration_infos(MODULE_NAME) == [
        ModuleVersionInfo(
            module_version="111",
            remote_path="//home/garden/modules/test_module/111",
            description='some description',
            module_traits=module_traits,
            sandbox_task_id="1234")
    ]

    assert set(storage.get_all_module_names()) == {MODULE_NAME}
    assert set(storage.get_all_module_names("unknown_contour")) == set()

    # Check empty module data for any other contour_name (default and "datatesting")

    with pytest.raises(ModuleNotFoundException):
        storage.get_module(MODULE_NAME, "222", default_contour_name())
    with pytest.raises(ModuleNotFoundException):
        storage.get_module(MODULE_NAME, "111", contour_name=DATATESTING)
    with pytest.raises(ModuleNotFoundException):
        storage.get_module(MODULE_NAME, "111", default_contour_name())
    assert storage.get_modules_versions(default_contour_name()) == {}
    assert storage.get_modules_versions(contour_name=DATATESTING) == {}
    with pytest.raises(ModuleNotFoundException):
        storage.get_module_traits(MODULE_NAME, contour_name=default_contour_name())
    assert storage.get_all_modules_traits(default_contour_name()) == []


@pytest.mark.usefixtures("yt_client")
def test_module_release(storage):
    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="111",
            remote_path="//tmp/some_path",
            module_traits=ModuleTraits(name=MODULE_NAME, type=ModuleType.MAP),
        ),
        USER_NAME)
    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="222",
            remote_path="//tmp/some_path",
            module_traits=ModuleTraits(name=MODULE_NAME, type=ModuleType.MAP),
        ),
        USER_NAME)
    _release_module_version(storage, MODULE_NAME, "222", yenv.Environment.STABLE)
    _release_module_version(storage, MODULE_NAME, "111", yenv.Environment.STABLE)

    assert storage.get_module(MODULE_NAME, "111", STABLE)
    assert storage.get_module(MODULE_NAME, module_version=None, contour_name=STABLE).version == "111"

    _release_module_version(storage, MODULE_NAME, "222", yenv.Environment.STABLE)
    module = storage.get_module(MODULE_NAME, module_version=None, contour_name=STABLE)
    assert module.version == "222"

    _release_module_version(storage, MODULE_NAME, "111", yenv.Environment.TESTING)
    assert storage.get_module(MODULE_NAME, module_version=None, contour_name=STABLE).version == "222"
    assert storage.get_module(MODULE_NAME, module_version=None, contour_name=DATATESTING).version == "111"

    release_infos = storage.get_contour_release_infos(MODULE_NAME, contour_name=STABLE)
    assert len(release_infos) == 2
    assert release_infos[0].version == "222"
    assert release_infos[0].description == "v222"


@pytest.mark.usefixtures("yt_client")
def test_get_module_version(storage):
    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="111",
            remote_path="//tmp/some_path",
            module_traits=ModuleTraits(name=MODULE_NAME, type=ModuleType.MAP),
        ),
        USER_NAME)
    _release_module_version(storage, MODULE_NAME, "111", yenv.Environment.TESTING)

    module_path = yatest.common.binary_path(MODULE_TO_TEST)
    isolated_modules_dir = storage._cache_dir
    module_dir = os.path.join(isolated_modules_dir, MODULE_NAME)
    if not os.path.exists(module_dir):
        os.makedirs(module_dir)
    shutil.copy(module_path, os.path.join(module_dir, "111"))

    module = storage.get_module(MODULE_NAME, "111", contour_name=DATATESTING)
    module_info = module.get_runner("contour_name").execute_proto(
        input_message=communicate.make_module_info_command(),
        output_message_type=proto_command.ModuleInfoOutput,
        operation_name="module_info"
    )
    assert module_info.name == MODULE_NAME


@pytest.mark.parametrize(
    (
        "server_environment",
        "expected_versions",
        "expected_deleted_versions",
    ),
    [
        # All versions registered during the test: ["1".. "11"].
        (
            yenv.Environment.STABLE,
            # Max 3 versions are kept for stable by the policy.
            # "1" is kept as active in some contour
            ["11", "10", "9", "1"],
            # "0" is not deleted as required for development
            # "1" is not deleted as active in some contour
            ["2", "3", "4", "5", "6", "7", "8"],
        ),
        (
            yenv.Environment.TESTING,
            # Max 2 versions are kept for stable by the policy.
            # "1" is kept as active in some contour
            ["11", "10", "1"],
            ["2", "3", "4", "5", "6", "7", "8", "9"],
        ),
        (
            yenv.Environment.DEVELOPMENT,
            ["11", "10", "1"],
            ["2", "3", "4", "5", "6", "7", "8", "9"],
        ),
    ],
)
@mock.patch("maps.pylibs.yandex_environment.environment.get_yandex_environment",
            return_vallue=yenv.Environment.STABLE)
def test_module_cleanup(get_yandex_environment,
                        server_environment,
                        expected_versions,
                        expected_deleted_versions,
                        storage,
                        yt_client,
                        contour_manager):
    """
    For each server environment verify the result of the module cleanup procedure
    after a successive release of several module versions to development, testing and stable.
    Set the very first version as active in a user contour to ensure that is it skipped for deletion.
    """
    get_yandex_environment.return_value = server_environment

    def prepare_module_for_env(name, version, env, contour=None):
        if env == yenv.Environment.DEVELOPMENT:
            storage.register_module_version(
                name,
                ModuleVersionInfo(
                    module_version=version,
                    remote_path=f"//tmp/some_path_{name}_{version}",
                    module_traits=ModuleTraits(name=MODULE_NAME, type=ModuleType.MAP),
                    user_contour=contour
                ),
                USER_NAME)
        else:
            _release_module_version(storage, name, version, env)

    # Setup a module that must not be touched by the test
    for module_environment in ALL_ENVIRONMENTS:
        prepare_module_for_env("name2", "0", module_environment)

    prepare_module_for_env("name1", "0", yenv.Environment.DEVELOPMENT)
    prepare_module_for_env("name1", "1", yenv.Environment.DEVELOPMENT)

    # Activate version "1" in a contour
    contour_manager.create("vasya_test", "user")
    contour_manager.activate_module_version("vasya_test", "name1", "1")

    # Setup a testable module released to each environment multiple times
    for index in range(1, 12):
        for module_environment in ALL_ENVIRONMENTS:
            prepare_module_for_env("name1", str(index), module_environment)

    # Create an extra user version to check that is delete by cleanup procedure
    for i in range(module_manager._SERVER_ENVIRONMENT_CLEANUP_POLICY[module_environment][yenv.Environment.DEVELOPMENT]):
        prepare_module_for_env("name1", f"user_version_{i}", yenv.Environment.DEVELOPMENT, contour="vasya_test")

    # Create an extra user version to check that is not delete by cleanup procedure
    prepare_module_for_env("name1", "user_version", yenv.Environment.DEVELOPMENT, contour="vasya_test")

    expected_modules_versions = {
        "name1": expected_versions,
        "name2": ["0"]  # Adjacent module is not unintentionally touched by the cleanup
    }

    for module_environment in ALL_ENVIRONMENTS:
        assert storage.get_modules_versions(contour_name=str(module_environment)) == expected_modules_versions,\
            f"Wrong versions for {module_environment}"

    yt_client.remove.assert_has_calls([
        mock.call(f"//home/garden/modules/name1/{version}", force=True)
        for version in expected_deleted_versions
    ])

    # Ensure the previos user version was deleted
    with pytest.raises(ModuleNotFoundException):
        storage.get_module("name1", "user_version_0", contour_name="vasya_test")

    # Ensure the latest user version is left in place
    assert storage.get_module("name1", "user_version", contour_name="vasya_test")


@pytest.mark.usefixtures("yt_client")
def test_active_contour_versions(storage, contour_manager):
    initial_traits = ModuleTraits(
        name=MODULE_NAME,
        type=ModuleType.MAP,
        displayed_name="some module",
    )

    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="system_version1",
            remote_path="//tmp/some_path",
            module_traits=initial_traits,
        ),
        USER_NAME)
    _release_module_version(storage, MODULE_NAME, "system_version1", yenv.Environment.TESTING)

    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="system_version2",
            remote_path="//tmp/some_path",
            module_traits=initial_traits,
        ),
        USER_NAME)
    _release_module_version(storage, MODULE_NAME, "system_version2", yenv.Environment.TESTING)
    _release_module_version(storage, MODULE_NAME, "system_version2", yenv.Environment.STABLE)

    modified_traits = initial_traits.copy()
    modified_traits.displayed_name = "some vasya module"

    contour_manager.create("vasya_test", "user")
    storage.register_module_version(
        MODULE_NAME,
        ModuleVersionInfo(
            module_version="user_version",
            remote_path="//tmp/some_other_path",
            module_traits=modified_traits,
            user_contour="vasya_test",
            description="User version description"
        ),
        USER_NAME)

    release_infos = storage.get_contour_release_infos(MODULE_NAME, "vasya_test")
    assert [m.version for m in release_infos] == ["system_version2", "system_version1", "user_version"]
    assert [m.description for m in release_infos] == ["vsystem_version2", "vsystem_version1", "User version description"]
    assert storage.get_all_modules_traits(contour_name="vasya_test") == [initial_traits]

    contour_manager.activate_module_version("vasya_test", MODULE_NAME, "user_version")
    release_infos = storage.get_contour_release_infos(MODULE_NAME, "vasya_test")
    assert [r.version for r in release_infos] == ["user_version", "system_version2", "system_version1"]


def _release_module_version(storage, name, version, environment):
    storage.release_module_version(
        module_name=name,
        module_version=version,
        environment=environment,
        released_at=dt.datetime.now(pytz.utc),
        released_by=USER_NAME,
        description=f"v{version}"
    )
