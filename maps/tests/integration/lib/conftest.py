import pytest
import maps.infopoint.points_for_period.lib.common as common
from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
from yatest.common import source_path


@pytest.fixture(scope='module')
def yt_config():
    return YtConfig(local_cypress_dir=source_path(
        'maps/infopoint/points_for_period/tests/integration/cypress'))


@pytest.fixture(scope='module')
def yt_stuff(yt_config):
    yt = YtStuff(yt_config)
    yt.start_local_yt()
    yield yt
    yt.stop_local_yt()


@pytest.fixture(scope='module')
def ytc(yt_stuff):
    yield common.yt_client(proxy=yt_stuff.get_server())
