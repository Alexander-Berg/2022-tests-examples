from typing import Optional

import pytest
from asyncpg import Connection

from maps_adv.points.server.lib.enums import PointType


class Factory:
    __all__ = ("_con",)

    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_collection(
        self,
        point_type: Optional[PointType] = PointType.billboard,
        version: Optional[int] = 1,
    ) -> int:
        sql = """
            INSERT INTO collections (type, version)
            VALUES ($1, $2)
            RETURNING id
        """
        return await self._con.fetchval(sql, point_type, version)

    async def create_points(
        self,
        point_type: Optional[PointType] = PointType.billboard,
        version: Optional[int] = 1,
    ):
        points_data = (
            # POLYGON ((0 40, 80 40, 80 -40, 0 -40, 0 40))
            "25 25",
            "55 20",
            "30 -15",
            # POLYGON ((130 40, -150 40, -150 -40, 130 -40, 130 40))
            "155 25",
            "-175 20",
            "160 -15",
            # outside
            "20 55",
            "115 0",
            "60 -45",
        )

        collection_id = await self.create_collection(point_type, version)
        sql = """
            INSERT INTO points (collection_id, geometry)
            VALUES ($1, $2)
        """
        await self._con.executemany(
            sql, [(collection_id, f"SRID=4326;POINT({point})") for point in points_data]
        )
        await self._con.execute("REFRESH MATERIALIZED VIEW points_view")

    async def fill_shows_forecasts_table(self):
        shows_data = [
            # P1 ((140 -20, 140 30, -160 30, -160 -20, 140 -20))
            # "170 -20"
            dict(geohash="ru2yhr", bb=5, zsb=20000, pin=300, ovw=20000),
            # "150 -10"
            dict(geohash="rq4xj7", bb=1, zsb=200000, pin=3000, ovw=20000),
            # "180 20"
            dict(geohash="xgxczb", bb=10, zsb=2000000, pin=30000, ovw=20000),
            # "-170 -5"
            dict(geohash="2pp5e9", bb=100, zsb=20000000, pin=300000, ovw=20000),
            # P2 ((120 10, 120 60, 170 60, 170 10, 120 10))
            # "130 40"
            dict(geohash="wzh7w1", bb=1000, zsb=5, pin=3000000, ovw=5),
            # "140 60"
            dict(geohash="z4et3f", bb=10000, zsb=2, pin=30000000, ovw=2),
            # CROSS P1, P2
            # "160 25"
            dict(geohash="xs3y0z", bb=100000, zsb=20, pin=5, ovw=20),
            # "150 20"
            dict(geohash="x7d9v2", bb=1000000, zsb=200, pin=3, ovw=200),
            # "170 25"
            dict(geohash="xu2yhr", bb=10000000, zsb=2000, pin=30, ovw=2000),
            # P3 ((50 50, 80 50, 80 20, 50 20, 50 50))
            # "70 30"
            dict(geohash="tt3f8v", bb=30, zsb=20, pin=50, ovw=20),
            # "60 40"
            dict(geohash="tr4et3", bb=40, zsb=30, pin=20, ovw=30),
            # OUTSIDE
            # "90 40"
            dict(
                geohash="tzpgxc",
                bb=100000000,
                zsb=100000000,
                pin=100000000,
                ovw=100000000,
            ),
            # "-130 -40"
            dict(
                geohash="30gs3y",
                bb=100000000,
                zsb=100000000,
                pin=100000000,
                ovw=100000000,
            ),
        ]

        sql = """
            INSERT INTO shows_forecasts (
                geohash,
                billboard_shows,
                zsb_shows,
                pin_shows,
                overview_shows
            )
            VALUES ($1, $2, $3, $4, $5)
        """

        await self._con.executemany(
            sql,
            [
                (
                    shows["geohash"],
                    shows["bb"],
                    shows["zsb"],
                    shows["pin"],
                    shows["ovw"],
                )
                for shows in shows_data
            ],
        )


@pytest.fixture
def factory(con):
    return Factory(con)


@pytest.fixture
async def fill_shows_forecasts(factory):
    await factory.fill_shows_forecasts_table()
