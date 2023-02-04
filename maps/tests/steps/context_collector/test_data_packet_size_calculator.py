import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.steps.context_collector import NoPacket

pytestmark = [pytest.mark.asyncio]


async def test_uses_packet_size_calculator(
    context_collector_step, packet_size_calculator_mock
):
    await context_collector_step.run({})

    packet_size_calculator_mock.assert_called_with()


async def test_saves_packet_bounds(context_collector_step):
    result = await context_collector_step.run()

    assert result["packet_start"] == dt("2000-02-02 01:10:00")
    assert result["packet_end"] == dt("2000-02-02 01:20:00")


async def test_raises_if_packet_size_calculator_returns_none(
    context_collector_step, packet_size_calculator_mock
):
    packet_size_calculator_mock.coro.return_value = None

    with pytest.raises(NoPacket):
        await context_collector_step.run()
