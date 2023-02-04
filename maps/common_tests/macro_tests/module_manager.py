import datetime as dt
import importlib
import pytz

import yatest.common

from maps.garden.sdk.core import GardenError, TaskGraphBuilder, ProxyGraphBuilder
from maps.garden.sdk.module_traits import module_traits
from maps.garden.libs_server.module import storage_interface
from maps.garden.libs_server.module.module_settings_storage import ModuleSettingsStorage
from maps.garden.libs_server.common.errors import ModuleNotFoundException
from maps.garden.sdk.utils import add_insert_traceback
from maps.pylibs.utils.lib.json_utils import load_json as load_json_file

VERSION = "test"

_PYTHON_MODULES = [
    "create_world",
    "denormalization",
    "dummy_source",
    "extra_resources",
    "failing_module",
    "geocoder_indexer",
    "graph",
    "offline_cache",
    "rasp_export_src",
    "road_graph",
    "signals_process",
    "signals_produce",
    "ymapsdf",
    "ymapsdf_src"
]


class _ModuleGraphBuilder(ProxyGraphBuilder):
    """
    A proxy graph builder setting module name to every task added.
    """
    def __init__(self, graph_builder, module_name):
        """
        Args:
            graph_builder (TaskGraphBuilder): The graph builder to proxy.
            module_name (str): The name of the module to set to every task.
        """
        super(_ModuleGraphBuilder, self).__init__(graph_builder)
        self._module_name = module_name

    def add_task(self, demands, creates, task):
        task.module_name = self._module_name
        add_insert_traceback(task, skip_frames=1)
        super(_ModuleGraphBuilder, self).add_task(demands, creates, task)


class _TestModule(storage_interface.ModuleInterface):
    def __init__(self, module_name, fill_graph, traits):
        self._module_name = module_name
        self.fill_graph = fill_graph
        self.traits = traits

    @property
    def name(self):
        return self._module_name

    @property
    def version(self):
        return VERSION

    @property
    def registration_datetime(self):
        return dt.datetime.now(pytz.utc)


def get_module_graph(module, regions, contour_name):
    module_graph_builder = _ModuleGraphBuilder(
        TaskGraphBuilder(),
        module.name)
    module.fill_graph(module_graph_builder)
    return module_graph_builder


class TestModuleManager(storage_interface.ModuleManagerInterface):
    def __init__(self, db):
        self._module_settings = ModuleSettingsStorage(db)
        self._modules = {}

        traits_filename = yatest.common.source_path("maps/garden/common_tests/configs/modules_traits.json")
        self._module_traits = module_traits.parse_traits(load_json_file(traits_filename))

        for python_module_name in _PYTHON_MODULES:
            python_module = importlib.import_module(
                "maps.garden.common_tests.macro_tests.test_modules." + python_module_name)

            for module_description in getattr(python_module, "modules"):
                module_name = module_description["name"]
                fill_graph = module_description["fill_graph"]
                self._modules[module_name] = _TestModule(module_name, fill_graph, self._module_traits[module_name])

    def get_module(self, module_name, module_version=None, contour_name=None):
        return self._modules[module_name]

    def get_modules_versions(self, contour_name):
        return {
            name: [VERSION]
            for name, module in self._modules.items()
        }

    def get_contour_release_infos(self, module_name, contour_name):
        module = self._modules.get(module_name)
        if not module:
            return []
        return [
            storage_interface.ContourVersionReleaseInfo(
                is_system=True,
                released_at=module.registration_datetime,
                released_by="tester",
                version=module.version
            )]

    def get_contour_release_info(
        self,
        module_name: str,
        module_version: str,
        contour_name: str,
    ):
        if module := self._modules.get(module_name):
            return storage_interface.ContourVersionReleaseInfo(
                is_system=True,
                released_at=module.registration_datetime,
                released_by="tester",
                version=module_version
            )
        else:
            raise GardenError(f"Failed to find module {module_name}")

    def get_module_traits(self, module_name, contour_name, module_version=None):
        if module_name not in self._module_traits:
            raise ModuleNotFoundException(module_name, contour_name, module_version)
        return self._module_traits[module_name]

    def get_all_modules_traits(self, contour_name):
        return list(self._module_traits.values())

    @property
    def settings(self):
        return self._module_settings

    def register_module_version(self, module_name, module_info, user):
        """
        This function is only used to update module traits
        """
        self._module_traits[module_name] = module_info.module_traits

    def release_module_version(
            self, *, module_name, module_version, environment, released_at, released_by, description):
        pass

    def get_version_registration_infos(self, module_name, module_version=None):
        return [
            storage_interface.ModuleVersionInfo(module_version=module.version)
            for name, module in self._modules.items()
            if not module_version or module.version == module_version
        ]

    def remove_module_version(self, module_name, module_version):
        pass
