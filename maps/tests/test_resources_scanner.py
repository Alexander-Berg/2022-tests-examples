from collections import defaultdict
from unittest import mock

from maps.garden.sdk.module_rpc.common import Capabilities
from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType, ScanResourcesConfig, SortOption

from maps.garden.libs_server.common.contour_manager import ContourManager
from maps.garden.libs_server.test_utils.thread_mocks import SchedulerMock, ThreadPoolExecutorMock
from maps.garden.libs_server.build.build_defs import Build, Dataset, BuildExternalResource, SourceDataset
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLogStorage
from maps.garden.libs_server.module.scan_resources_common import ScanResourcesRequestStorage, ScanRequestRecord
from maps.garden.libs_server.module.scan_resources_common import SCAN_REQUEST_COLLECTION_NAME
from maps.garden.libs_server.module import storage_interface

from maps.garden.scheduler.lib.scan_resources_scheduler import ResourceScanner

BASE_PROPERTIES={
    "first_property": "other_first_value",
    "second_property": 2,
    "list_property": [1, 2, 3],
}
UNKNOWN_KEY = {"foreign_key": "unknown_value"}
UNKNOWN_RESOURCE = BuildExternalResource(
    resource_name="unknown_name",
    properties={
        **BASE_PROPERTIES,
        "grouping_property": ["first_group"],
        "different_property": 0,
        "different_list": [0],
    }
)

MODULE_NAME = "test_module"
CONTOUR_NAME = "contour"

KNOWN_KEY = {"foreign_key": "known_value"}
KNOWN_RESOURCE = BuildExternalResource(
    resource_name="known_name",
    properties={
        **BASE_PROPERTIES,
        "grouping_property": ["second_group"],
        "different_property": 1,
        "different_list": [1],
    }
)
UNKNOWN_RESOURCE_2 = BuildExternalResource(
    resource_name="unknown_name_2",
    properties={
        **BASE_PROPERTIES,
        "grouping_property": ["first_group"],
        "different_property": 2,
        "different_list": [2],
        "unique_property": None,
    }
)

KNOWN_DATASET = Dataset(
    module_name=MODULE_NAME,
    contour_name=CONTOUR_NAME,
    foreign_key=KNOWN_KEY,
    properties=KNOWN_RESOURCE.properties,
    resources=[
        BuildExternalResource(
            resource_name=KNOWN_RESOURCE.resource_name,
            properties=KNOWN_RESOURCE.properties
        )
    ]
)

EXPECTED_BUILD = Build(
    _id=0,
    id=0,
    name=MODULE_NAME,
    contour_name=CONTOUR_NAME,
    sources=[],
    extras={},
    request_id=0,
    output_resources_keys=set(),
    created_at=0,
    key="",
    fill_missing_policy={},
)

SCAN_RESOURCES_SCHEDULER_CONFIG = {
    "poll_period": 360,
    "thread_count": 1
}

SCAN_RESOURCES_SCHEDULER_SETTINGS = {
    "resource_scanner": {
        "enable_scan": False,
        "contours": []
    }
}

ALL_DATASETS = [
    SourceDataset(
        foreign_key=UNKNOWN_KEY,
        resources=[UNKNOWN_RESOURCE, UNKNOWN_RESOURCE_2]
    ),
    SourceDataset(
        foreign_key=KNOWN_KEY,
        resources=[KNOWN_RESOURCE]
    )
]


class StubModule(storage_interface.ModuleInterface):
    def __init__(self, raise_exception=None, resources=ALL_DATASETS):
        super().__init__()
        self._raise_exception = raise_exception
        self._resources = resources

    @property
    def name(self):
        return MODULE_NAME

    @property
    def version(self):
        return "module_version"

    def is_capable_of(self, capability):
        return True

    def scan_resources(self, environment_settings, contour_name):
        if self._raise_exception is not None:
            raise self._raise_exception

        return self._resources


class StubRegistrar:
    def __init__(self):
        self.registered = defaultdict(list)

    def get_datasets(
        self,
        module_name: str,
        contour_name: str,
    ) -> list[Dataset]:
        return self.registered[f"{module_name}:{contour_name}"]

    def actualize_datasets(
        self,
        module_name: str,
        contour_name: str,
        datasets: list[Dataset],
    ) -> None:
        self.registered[f"{module_name}:{contour_name}"] = datasets.copy()


def test_resource_scanner_once(db, mocker):
    module_manager = mock.Mock()
    module_manager.get_module.return_value = StubModule()
    module_manager.get_module_traits.return_value = ModuleTraits(
        name=MODULE_NAME,
        type=ModuleType.SOURCE,
        sort_options=[
            SortOption(
                key_pattern="{0.properties[second_property]}"
            )
        ]
    )

    datasets_registrar = StubRegistrar()
    datasets_registrar.actualize_datasets(MODULE_NAME, CONTOUR_NAME, [KNOWN_DATASET])
    mocker.patch(
        "maps.garden.scheduler.lib.scan_resources_scheduler.DatasetsRegistrar",
        return_value=datasets_registrar,
    )

    scanner = ResourceScanner(db, settings=SCAN_RESOURCES_SCHEDULER_SETTINGS, config=SCAN_RESOURCES_SCHEDULER_CONFIG)

    build_manager = mock.Mock()
    build_manager.create.return_value = EXPECTED_BUILD

    with mock.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager), \
         mock.patch("maps.garden.libs_server.application.state.settings_provider"), \
         mock.patch("maps.garden.libs_server.application.state.module_builds_manager", return_value=build_manager):
        scanner.scan_once(author="user", contour_name=CONTOUR_NAME, module_name=MODULE_NAME)

    assert build_manager.create.call_count == 1
    logs = [doc for doc in db.module_logs.find()]
    assert len(logs) == 1
    assert not logs[0]["exceptions"]

    result_datasets = datasets_registrar.get_datasets(MODULE_NAME, CONTOUR_NAME)
    checked_properties = False
    for dataset in result_datasets:
        if len(dataset.resources) > 1:
            assert dataset.properties == {**BASE_PROPERTIES, "grouping_property": ["first_group"]}
            checked_properties = True

    assert checked_properties


def test_resource_scanner_once_with_grouping(db, mocker):
    module_manager = mock.Mock()

    # no need to pay attention to "known" and "unknown" here, it is just two different resources
    group1 = [SourceDataset(foreign_key=KNOWN_KEY | {"number": str(i)}, resources=[KNOWN_RESOURCE]) for i in range(2)]
    group2 = [SourceDataset(foreign_key=UNKNOWN_KEY | {"number": str(i)}, resources=[UNKNOWN_RESOURCE]) for i in range(2)]
    module_manager.get_module.return_value = StubModule(
        resources=group1+group2
    )

    module_manager.get_module_traits.return_value = ModuleTraits(
        name=MODULE_NAME,
        type=ModuleType.SOURCE,
        builds_grouping=["grouping_property"],
        sort_options=[SortOption(key_pattern="test_pattern")],
    )

    datasets_registrar = StubRegistrar()
    mocker.patch(
        "maps.garden.scheduler.lib.scan_resources_scheduler.DatasetsRegistrar",
        return_value=datasets_registrar,
    )

    scanner = ResourceScanner(db, settings=SCAN_RESOURCES_SCHEDULER_SETTINGS, config=SCAN_RESOURCES_SCHEDULER_CONFIG)

    build_manager = mock.Mock()
    build_manager.create.return_value = EXPECTED_BUILD

    with mock.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager), \
         mock.patch("maps.garden.libs_server.application.state.settings_provider"), \
         mock.patch("maps.garden.libs_server.application.state.module_builds_manager", return_value=build_manager):
        scanner.scan_once(author="user", contour_name=CONTOUR_NAME, module_name=MODULE_NAME)

    assert build_manager.create.call_count == 2
    logs = [doc for doc in db.module_logs.find()]
    assert len(logs) == 1
    assert not logs[0]["exceptions"]


def test_resource_scanner_async(db, mocker):
    request_collection = db[SCAN_REQUEST_COLLECTION_NAME]

    module_manager = mock.Mock()
    module_manager.get_module.return_value = StubModule()
    module_manager.get_module_traits.return_value = ModuleTraits(name=MODULE_NAME, type=ModuleType.SOURCE, sort_options=[SortOption(key_pattern="test_pattern")])

    mocker.patch(
        "concurrent.futures.ThreadPoolExecutor",
        new=ThreadPoolExecutorMock
    )
    scheduler_mock = SchedulerMock()
    mocker.patch(
        "maps.garden.scheduler.lib.scan_resources_scheduler.Scheduler",
        return_value=scheduler_mock
    )

    datasets_registrar = StubRegistrar()
    datasets_registrar.actualize_datasets(MODULE_NAME, CONTOUR_NAME, [KNOWN_DATASET])
    mocker.patch(
        "maps.garden.scheduler.lib.scan_resources_scheduler.DatasetsRegistrar",
        return_value=datasets_registrar,
    )

    scanner = ResourceScanner(db, settings=SCAN_RESOURCES_SCHEDULER_SETTINGS, config=SCAN_RESOURCES_SCHEDULER_CONFIG)
    scan_resource_request_storage = ScanResourcesRequestStorage(db)

    build_manager = mock.Mock()
    build_manager.create.return_value = EXPECTED_BUILD

    with mock.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager), \
         mock.patch("maps.garden.libs_server.application.state.settings_provider"), \
         mock.patch("maps.garden.libs_server.application.state.module_builds_manager", return_value=build_manager), \
         scanner:
        scan_resource_request_storage.create_request(ScanRequestRecord(author="user", contour_name=CONTOUR_NAME, module_name=MODULE_NAME))
        while db.module_logs.count() == 0:
            scheduler_mock.execute_background_task()

    assert request_collection.find().count() == 0
    assert build_manager.create.call_count == 1
    logs = [doc for doc in db.module_logs.find()]
    assert len(logs) == 1
    assert not logs[0]["exceptions"]


def _run_worker(db, module, registrar, except_exception, mocker):
    settings = {
        "resource_scanner": {
            "enable_scan": True,
            "contours": ["contour_one"]
        }
    }

    module_traits = ModuleTraits(
        name=MODULE_NAME,
        type=ModuleType.SOURCE,
        scan_resources=ScanResourcesConfig(period_sec=1),
        capabilities=[Capabilities.SCAN_RESOURCES],
        sort_options=[SortOption(key_pattern="test_pattern")]
    )

    module_manager = mock.Mock()
    module_manager.get_module.return_value = module
    module_manager.get_module_traits.return_value = module_traits
    module_manager.get_all_modules_traits.return_value = [module_traits]

    module_log_storage = ModuleLogStorage(db)
    module_log_collection = db.module_logs

    mocker.patch(
        "concurrent.futures.ThreadPoolExecutor",
        new=ThreadPoolExecutorMock
    )
    scheduler_mock = SchedulerMock()
    mocker.patch(
        "maps.garden.scheduler.lib.scan_resources_scheduler.Scheduler",
        return_value=scheduler_mock
    )

    datasets_registrar = registrar
    datasets_registrar.actualize_datasets(MODULE_NAME, CONTOUR_NAME, [KNOWN_DATASET])
    mocker.patch(
        "maps.garden.scheduler.lib.scan_resources_scheduler.DatasetsRegistrar",
        return_value=datasets_registrar,
    )

    scanner = ResourceScanner(db, settings=settings, config=SCAN_RESOURCES_SCHEDULER_CONFIG)

    build_manager = mock.Mock()
    build_manager.create.return_value = EXPECTED_BUILD

    contour_manager = ContourManager(db)
    contour_manager.create("contour_one", "admin", is_system=True)

    with mock.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager), \
         mock.patch("maps.garden.libs_server.application.state.settings_provider"), \
         mock.patch("maps.garden.libs_server.application.state.module_builds_manager", return_value=build_manager), \
         scanner:
        while module_log_collection.count() == 0:
            scheduler_mock.execute_background_task()

    call_count = 1 if except_exception is None else 0
    assert build_manager.create.call_count == call_count
    logs = module_log_storage.find_log_records(contour_name="contour_one", module_name=MODULE_NAME, limit=100)
    assert len(logs) == 1

    if except_exception:
        assert logs[0].exceptions
        assert logs[0].exceptions[0].message == except_exception
    else:
        assert not logs[0].exceptions


def test_worker_with_exception_in_module(db, mocker):
    _run_worker(
        db, StubModule(raise_exception=Exception("some_exception")),
        StubRegistrar(), "some_exception", mocker
    )


def test_worker(db, mocker):
    _run_worker(db, StubModule(), StubRegistrar(), None, mocker)
