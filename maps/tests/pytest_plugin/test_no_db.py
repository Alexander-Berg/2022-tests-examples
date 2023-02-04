import aiohttp
import pytest

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def inner_mock(mocker):
    return mocker.Mock()


@pytest.fixture
def app(inner_mock):
    class SampleApp(Lasagna):

        SWIM_ENGINE_CLS = None

        async def _setup_layers(self, db):
            inner_mock(db)
            return aiohttp.web.Application()

    return SampleApp({"TVM_DAEMON_URL": "http://tvm.daemon", "TVM_TOKEN": "token"})


async def test_works_well_without_db(inner_mock, api):
    inner_mock.assert_called_with(None)
