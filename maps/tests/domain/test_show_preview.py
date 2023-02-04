import pytest

from maps_adv.geosmb.harmonist.server.lib.exceptions import InvalidSessionId

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_returns_preview(dm, domain):
    dm.show_preview.coro.return_value = dict(
        rows=[["Line", "One"], ["Second", "line"]], markup={"some": "params"}
    )

    got = await domain.show_preview(session_id="session_id", biz_id=123)

    assert got == dict(
        rows=[["Line", "One"], ["Second", "line"]], markup={"some": "params"}
    )


async def test_calls_dm_correctly(dm, domain):
    await domain.show_preview(session_id="session_id", biz_id=123)

    dm.show_preview.assert_called_with(session_id="session_id", biz_id=123)


async def test_raises_if_dm_raises(dm, domain):
    dm.show_preview.coro.side_effect = InvalidSessionId("boom!")

    with pytest.raises(InvalidSessionId, match="boom!"):
        await domain.show_preview(session_id="session_id", biz_id=123)
