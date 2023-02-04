import dataclasses

import mongomock
import networkx

from maps.garden.sdk.module_traits import module_traits as module_traits_lib
from maps.garden.libs_server.build import build_defs
from maps.garden.libs_server.build import build_statistics
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage
from maps.garden.libs_server.module import module_settings_storage

from maps.garden.tools.releases_monitoring.lib import path_inspection


def test(requests_mock, mocker):
    server_hostname = "fake.nostname.net"
    server_config = {
        "garden": {
            "server_hostname": server_hostname,
            "ui_hostname": "fake.ui.hostname.net",
        },
        "tvm_client": {
            "client_id": 1,
            "token": "test_token",
        },
        "sedem": {
            "token": "test_token"
        }
    }

    contour_name = "some_contour_name"
    monitoring_config = {
        "contour": contour_name,
        "startrek_queue": "TEST"
    }

    modules_traits_list = [
        {
            "name": "ymapsdf_src",
            "type": module_traits_lib.ModuleType.SOURCE,
            "sort_options": [{"key_pattern": "test_pattern"}],
        },
        {
            "name": "rasp_export",
            "type": module_traits_lib.ModuleType.SOURCE,
            "sort_options": [{"key_pattern": "test_pattern"}],
        },
        {
            "name": "yet_another_source",
            "type": module_traits_lib.ModuleType.SOURCE,
            "sort_options": [{"key_pattern": "test_pattern"}],
        },
        {
            "name": "ymapsdf",
            "type": module_traits_lib.ModuleType.MAP,
            "sources": ["ymapsdf_src", "yet_another_source"],
            "configs": ["rasp_export"],
            "autostarter": {
                "trigger_by": ["ymapsdf_src"],
            },
            "freshness_check": {
                "sources": ["ymapsdf_src"]
            }
        },
        {
            "name": "road_graph_build",
            "type": module_traits_lib.ModuleType.MAP,
            "sources": ["ymapsdf"],
            "autostarter": {"trigger_by": ["ymapsdf"]},
            "freshness_check": {
                "sources": ["ymapsdf"]
            }
        }
    ]

    class FakeABCClient:
        def __init__(self, server_settings) -> None:
            pass

        def get_module_followers(self, module_name) -> list[str]:
            return ['vasya', 'petya', 'kolya']

    class FakeModuleManager:
        def __init__(self, server_config, database) -> None:
            self.settings = None

        def get_all_modules_traits(self, contour):
            return list(parsed_modules_traits.values())

    parsed_modules_traits = module_traits_lib.parse_traits(modules_traits_list)
    mocker.patch("maps.garden.libs_server.infrastructure_clients.abc.Client", FakeABCClient)

    database = mongomock.MongoClient(tz_aware=True).db
    build_statistics_storage = build_statistics.BuildStatisticsStorage(database)
    module_manager_instance = FakeModuleManager(server_config, database)
    module_event_storage = ModuleEventStorage(database)

    @dataclasses.dataclass
    class FakeModuleInspector:
        source: str
        target: str
        contour_name: str
        ui_hostname: str
        fresh_source_builds: dict[int, build_defs.Build]
        flow_graphs: list[networkx.DiGraph]
        module_settings: module_settings_storage.ModuleSettingsStorage
        module_event_storage: ModuleEventStorage
        build_statistics_storage: build_statistics.BuildStatisticsStorage

        def check(self):
            result = [f"{self.source} -> {self.target} in contour {self.contour_name}"]
            for graph in self.flow_graphs:
                path = networkx.algorithms.shortest_paths.generic.shortest_path(graph, self.source, self.target)
                result.append(graph.name)
                result.append("->".join(path))
            return "\n".join(result)

    mocker.patch.object(path_inspection, "ModuleInspector", FakeModuleInspector)

    inspector = path_inspection.ContourInspector(
        server_config=server_config,
        monitoring_config=monitoring_config,
        build_statistics_storage=build_statistics_storage,
        module_manager=module_manager_instance,
        module_event_storage=module_event_storage,
    )
    # modules that has freshness_check config in traits
    assert inspector.modules_monitoring_configs == {
        'ymapsdf': {
            'name': 'ymapsdf',
            'sources': ['ymapsdf_src'],
            'followers': ['vasya', 'petya', 'kolya']
        },
        'road_graph_build': {
            'name': 'road_graph_build',
            'sources': ['ymapsdf'],
            'followers': ['vasya', 'petya', 'kolya']
        }
    }

    scan_results = [dataclasses.asdict(res) for res in inspector.scan_unreleased_modules()]
    # we do not want to see this source in result graph because it is not on the path
    # from source (defined in freshness_check field)
    # module to the module being checked
    assert not any("yet_another_source" in res["message"] for res in scan_results)

    # this module is also not on the path from source module
    # to the module being checked
    assert not any("rasp_export" in res["message"] for res in scan_results)

    assert not any(res["exception"] for res in scan_results)
    return scan_results
