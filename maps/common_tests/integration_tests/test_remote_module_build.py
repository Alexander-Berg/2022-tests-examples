import pytest

import common as cm


@pytest.mark.parametrize(
    "module_version",
    [cm.PREV_BINARY_MODULE_VERSION, cm.CURRENT_BINARY_MODULE_VERSION]
)
@pytest.mark.use_local_mongo
def test_build_remote_module(garden_client_helper, module_version):
    """
    Initial conditions:
    1. Two remote modules on YT ('source' and 'map' types), each built on the current trunk and in the past.
    2. The 'map' module is configured for autostart on completion of the 'source' one.
    3. A user contour is created.
    4. A custom version of the 'map' module is registered and activated for the user contour.

    Actions:
    1. Post a new build for the 'source' module.
    2. Wait until the 'source' module is completed.
    3. Wait until the 'map' module is completed for the system contour.
    4. TODO Ensure the 'map' module is not started in the user contour.
    5. Manually start the same 'map' module build in the user contour.
    6. Wait until the build in the user contour completes.
    7. Ensure the user contour build creates new resources.
    """
    garden_client_helper.init_module_version(module_version)

    src_build = garden_client_helper.build_src_module(cm.SYSTEM_CONTOUR_NAME)

    autostarted_build = garden_client_helper.wait_autostarted_build(cm.SYSTEM_CONTOUR_NAME)
    autostarted_build_resources = garden_client_helper.get_resources(
        module_name=cm.TEST_MODULE,
        build_id=autostarted_build['id'],
        contour_name=cm.SYSTEM_CONTOUR_NAME,
    )
    assert set(r["name"] for r in autostarted_build_resources) == {"output_resource"}

    # TODO: check that no build is autostarted in the user contour

    manual_build = garden_client_helper.build_module_from_source(src_build, cm.USER_CONTOUR_NAME)

    manual_build_resources = garden_client_helper.get_resources(
        module_name=cm.TEST_MODULE,
        build_id=manual_build['id'],
        contour_name=cm.USER_CONTOUR_NAME,
    )
    assert set(r["name"] for r in manual_build_resources) == {"output_resource"}

    assert set(r["key"] for r in autostarted_build_resources) !=\
        set(r["key"] for r in manual_build_resources)
