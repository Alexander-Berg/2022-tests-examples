import pytest
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


async def test_creates_biz_state(factory, dm):
    await dm.create_biz_state(biz_id=15, slug="cafe", permalink="5679")

    assert await factory.list_all_biz_states() == [
        {
            "id": Any(int),
            "biz_id": 15,
            "permalink": "5679",
            "slug": "cafe",
            "stable_version": None,
            "unstable_version": None,
            "published": False,
            "blocked": False,
            "blocking_data": None,
        }
    ]


async def test_returns_none(factory, dm):
    result = await dm.create_biz_state(biz_id=15, slug="cafe", permalink="5679")

    assert result is None
