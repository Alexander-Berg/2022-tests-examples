import asyncio
import pytest
import torch.nn
from ads_pytorch.core import BaseParameterServerModule, ParameterServerOptimizer
from ads_pytorch.core.periodic_callback import PeriodicCallback


class CallCountSnapshotter(PeriodicCallback):
    def __init__(self, *args, **kwargs):
        super(CallCountSnapshotter, self).__init__(*args, **kwargs)
        self.call_count = 0

    async def _call_impl(
            self,
            model: BaseParameterServerModule,
            optimizer: ParameterServerOptimizer,
            loss: torch.nn.Module,
            uri
    ):
        self.call_count += 1


@pytest.mark.asyncio
async def test_uris_snapshotter():
    async with CallCountSnapshotter(uris=frozenset(["x1", "x2", "x3"])) as snapshotter:
        await snapshotter(1, 2, 3, 4)  # some random args
        assert snapshotter.call_count == 0
        await snapshotter(1, 3, 4, "x3")
        assert snapshotter.call_count == 1


@pytest.mark.asyncio
async def test_frequency_snapshotter():
    async with CallCountSnapshotter(frequency=0.2) as snapshotter:
        for _ in range(3):
            await snapshotter(1, 2, 3, 4)  # some random args
            assert snapshotter.call_count == 0
        await asyncio.sleep(0.4)
        await snapshotter(1, 2, 3, 4)
        assert snapshotter.call_count == 1

        # Check that after timeout snapshotter is valled only once
        for _ in range(3):
            await snapshotter(1, 2, 3, 4)  # some random args
            assert snapshotter.call_count == 1

        await asyncio.sleep(0.4)
        await snapshotter(1, 2, 3, 4)
        assert snapshotter.call_count == 2
