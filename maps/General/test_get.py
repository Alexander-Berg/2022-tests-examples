import maps.garden.sandbox.server_autotests.lib.constants as const
from maps.garden.sandbox.server_autotests.lib.decorator import test_case
import maps.garden.sandbox.server_autotests.lib.helper_functions as hf
import maps.garden.sandbox.server_autotests.lib.server_tester as st


HANDLERS = [
    # autostarters
    const.AUTOSTARTERS,
    # idm
    const.IDM_GET_ALL_ROLES,
    const.IDM_INFO,
    # module versions
    const.MODULES_MODULE_NAME_RELEASE_INFO.format(module_name=const.EXAMPLE_MAP),
    # modules
    const.MODULE_EVENT_TYPES,
    const.MODULE_LOG_TYPES,
    # module statistics
    const.BUILD_HIERARCHY,
    # ui
    const.DEV_LINKS,
    # contours
    const.CONTOURS,
    # tasks
    const.TASKS
]

HANDLERS_WITH_CONTOUR = [
    # module versions
    const.MODULES_MODULE_NAME_VERSIONS.format(module_name=const.EXAMPLE_MAP),
    # modules
    const.MODULES_MODULE_NAME_EVENTS.format(module_name=const.EXAMPLE_MAP),
    const.MODULES_MODULE_NAME_TRAITS.format(module_name=const.EXAMPLE_MAP),
    const.MODULES_MODULE_NAME_LOGS.format(module_name=const.EXAMPLE_MAP),
    const.MODULES_MODULE_NAME.format(module_name=const.EXAMPLE_MAP),
    # module statistics
    const.MODULE_STATISTICS,
    const.PAGES,
    # builds
    const.MODULES_MODULE_NAME_BUILDS.format(module_name=const.EXAMPLE_MAP),
]


@test_case
def test_get(tester: st.ServerTester):
    _test_parameterless_get_handlers(tester)
    _test_param_contour_handlers(tester)
    _test_parameterized_get_handlers(tester)


def _test_parameterless_get_handlers(tester: st.ServerTester):
    for handler in HANDLERS:
        tester.get(handler)


def _test_param_contour_handlers(tester: st.ServerTester):
    for handler in HANDLERS_WITH_CONTOUR:
        tester.get(handler, params={"contour": tester.system_contour_name})


def _test_parameterized_get_handlers(tester: st.ServerTester):
    # ui
    hf.check_pages(tester)
    full_build_id = hf.get_example_map_pages(tester)["buildDescriptors"][0]["target"]["id"]
    # full_build_id : "<module_name>:<build_id>"
    build_id = full_build_id.split(":")[1]
    build_full_info_url = hf.get_module_build_full_info_url(const.EXAMPLE_MAP, build_id)
    tester.get(build_full_info_url)

    build_statistics_url = hf.get_module_build_statistic_url(const.EXAMPLE_MAP, build_id)
    tester.get(build_statistics_url)

    # error logs
    tester.get(const.ERROR_LOG_MODULE_NAME_BUILD_ID.format(module_name=const.EXAMPLE_MAP, build_id=build_id))

    # resources
    response = tester.get(const.STORAGE).json()
    key = response[0]["key"]
    tester.get(const.STORAGE_KEY.format(key=key))

    # builds
    response = tester.get(const.BUILDS, params={"module": const.RASP_EXPORT}).json()
    full_build_id = hf.get_completed_build_id(const.RASP_EXPORT, response["builds"])

    response = tester.get(const.STORAGE, params={"build": full_build_id}).json()
    key = hf.get_dir_resource_key(full_build_id, response)

    # resources
    tester.get(const.DIR_KEY.format(key=key))

    response = tester.get(const.BUILDS, params={"module": const.EXAMPLE_MAP}).json()
    full_build_id = hf.get_completed_build_id(const.EXAMPLE_MAP, response["builds"])

    # contours
    tester.get(const.CONTOURS_CONTOUR_NAME.format(contour_name=tester.system_contour_name))

    # builds
    module_build_url = hf.get_module_build_url(const.EXAMPLE_MAP, build_id)
    tester.get(module_build_url)
    tester.get(const.BUILD_FULL_BUILD_ID.format(full_build_id=full_build_id))
