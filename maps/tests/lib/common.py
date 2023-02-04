from unittest import mock
import pytest

from maps.garden.modules.renderer_denormalization.lib import const, tool


def used_ymapsdf_tables():
    tables = set([const.FAKE_INPUT_TABLE, "model3d"])
    for traits in tool.tasks_info().values():
        tables |= set(traits["demandsInput"])
    tables -= set([const.MASSTRANSIT_DATA_RESOURCE])
    return list(tables)


# Setup decorator for tests executing denormalization graph.
# Decorated function`s parameter list is expected to begin with "sandbox_upload_mock, geobase_mock" parameters.
def setup_workflow(executor):
    executor = pytest.mark.use_local_yt_yql()(executor)
    executor = mock.patch("maps.garden.sdk.yt.geobase.get_geobase5", return_value="geodata5.bin")(executor)
    executor = mock.patch("maps.garden.sdk.sandbox.UploadToSandboxTask._upload")(executor)
    return executor
