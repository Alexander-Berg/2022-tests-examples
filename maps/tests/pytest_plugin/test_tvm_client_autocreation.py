import aiohttp
import pytest

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]


class SampleApp(Lasagna):

    SWIM_ENGINE_CLS = None

    async def _setup_layers(self, db):
        return aiohttp.web.Application()


@pytest.fixture
def app():
    return SampleApp({"TVM_DAEMON_URL": "http://tvm.daemon", "TVM_TOKEN": "token"})


@pytest.mark.parametrize(
    "kw", ({"source": "src1", "destination": "dst1"}, {"destination": "dst1"})
)
async def test_tvm_is_mocked_if_exist(kw, app, api):
    assert await app.tvm.retrieve_ticket(**kw) == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
