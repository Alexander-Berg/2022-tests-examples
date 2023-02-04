import aiohttp
import pytest

from smb.common.aiotvm import TvmClient
from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]


class SampleApp(Lasagna):
    async def _setup_layers(self, db):
        return aiohttp.web.Application()


async def test_creates_tvm_client_if_config_specified(run):
    app = SampleApp({"TVM_DAEMON_URL": "http://tvm.daemon", "TVM_TOKEN": "token"})

    await run(app)

    assert isinstance(app.tvm, TvmClient)


@pytest.mark.parametrize(
    "config", ({"TVM_DAEMON_URL": "http://tvm.daemon"}, {"TVM_TOKEN": "token"}, {})
)
async def test_not_creates_tvm_client_if_config_not_specified(config, run):
    app = SampleApp(config)

    await run(app)

    assert app.tvm is None
