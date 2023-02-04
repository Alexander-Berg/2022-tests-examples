from typing import List

import pytest
from asyncpg import Connection

from maps_adv.geosmb.marksman.server.lib.enums import SegmentType


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def list_businesses(self) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM businesses
            ORDER BY created_at, id
            """
        )

        return list(map(dict, rows))

    async def fetch_business(self, biz_id: int) -> dict:
        row = await self._con.fetchrow(
            """
            SELECT *
            FROM businesses
            WHERE biz_id = $1
            """,
            biz_id,
        )

        return dict(row)

    async def create_business(
        self, biz_id: int = 123, permalink: int = 56789, counter_id: int = 3333
    ) -> dict:
        row = await self._con.fetchrow(
            """
            INSERT INTO businesses (biz_id, permalink, counter_id)
            VALUES ($1, $2, $3)
            RETURNING *
            """,
            biz_id,
            permalink,
            counter_id,
        )

        return dict(row)

    async def create_biz_segment(
        self,
        business_id: int,
        name: str = "ACTIVE",
        cdp_id: int = 55,
        cdp_size: int = 1001,
        type_: SegmentType = SegmentType.SEGMENT,
    ) -> dict:
        row = await self._con.fetchrow(
            """
            INSERT INTO segments (business_id, name, cdp_id, cdp_size, type)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
            """,
            business_id,
            name,
            cdp_id,
            cdp_size,
            type_.value,
        )

        return dict(row)

    async def list_business_segments(self, business_id: int) -> List[dict]:
        rows = await self._con.fetch(
            """
            SELECT *
            FROM segments
            WHERE business_id = $1
            ORDER BY id
            """,
            business_id,
        )

        return list(map(dict, rows))


@pytest.fixture
def factory(con):
    return Factory(con)
