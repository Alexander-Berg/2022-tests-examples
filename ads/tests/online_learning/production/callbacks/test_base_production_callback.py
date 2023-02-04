from ads_pytorch.online_learning.production.callbacks.base_production_callback import (
    ProductionCallback,
    ProdURI
)
from ads_pytorch.online_learning.production.dataset import DatetimeURI
import pytest
import datetime
import time


class ForceSkipCallback(ProductionCallback):
    def __init__(self, *args, **kwargs):
        super(ForceSkipCallback, self).__init__(*args, **kwargs)
        self.call_impl_called = False
        self.call_count = 0

    async def _call_impl(
            self,
            model,
            optimizer,
            loss,
            uri: ProdURI
    ):
        self.call_impl_called = True
        self.call_count += 1


@pytest.mark.parametrize('force_skip', [True, False])
@pytest.mark.asyncio
async def test_force_skip(force_skip):
    cb = ForceSkipCallback()
    uri = ProdURI(DatetimeURI("ahaha", date=datetime.datetime.now()), force_skip=force_skip)
    await cb(None, None, None, uri)
    assert cb.call_impl_called == (not force_skip)


@pytest.mark.parametrize('frequency', [0, 0.4])
@pytest.mark.asyncio
async def test_frequency(frequency):
    cb = ForceSkipCallback(min_frequency=frequency, force_uri_frequency=1e10)
    uri = ProdURI(DatetimeURI("ahaha", date=datetime.datetime.now()), force_skip=False)

    wait_time = 1
    start_time = time.time()
    call_counter = 0
    while time.time() - start_time < wait_time:
        call_counter += 1
        await cb(None, None, None, uri)
    if frequency == 0:
        assert call_counter == cb.call_count
    else:
        assert cb.call_count == int(wait_time / frequency)
