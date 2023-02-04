from asyncpg import Connection

from maps_adv.points.proto.primitives_pb2 import Point
from maps_adv.points.server.lib.enums import PointType


class PointsGenerator:
    step = 0.1
    start = {"lon": 33.145, "lat": 58.607}
    longitude_limit = 140.524
    latitude_limit = 43.360
    chunk_size = 15000

    def __iter__(self):
        point = self.start.copy()
        chunk = [point]

        while point["lon"] < self.longitude_limit:

            while point["lat"] > self.latitude_limit:

                point = point.copy()
                point["lat"] -= self.step
                chunk.append(point)

                if len(chunk) >= self.chunk_size:
                    yield chunk
                    chunk = []

            point = {"lon": point["lon"] + self.step, "lat": self.start["lat"]}

        yield chunk

    async def __call__(self, point_type: PointType, version: int, con: Connection):
        collection_id_sql = """
        INSERT INTO collections (type, version)
        VALUES ($1, $2)
        RETURNING id
        """
        collection_id = await con.fetchval(collection_id_sql, point_type, version)

        chunk_sql = """
            INSERT INTO points (collection_id, geometry)
            VALUES ($1, $2::geometry)
            """

        for chunk in self:
            await con.executemany(
                chunk_sql,
                [
                    (collection_id, f'SRID=4326;POINT({el["lon"]} {el["lat"]})')
                    for el in chunk
                ],
            )

        await con.execute("REFRESH MATERIALIZED VIEW points_view")


class PolygonsGenerator:
    step = 0.5
    size = 0.01
    start = {"longitude": 33.145, "latitude": 58.607}
    longitude_limit = 140.524
    latitude_limit = 43.360

    def __iter__(self):
        point = self.start.copy()

        while point["longitude"] < self.longitude_limit:

            while point["latitude"] > self.latitude_limit:

                point = point.copy()
                point["latitude"] -= self.step

                point_2 = point.copy()
                point_2["longitude"] += self.size

                point_3 = point.copy()
                point_3["latitude"] -= self.size

                yield [
                    Point(
                        longitude=str(point["longitude"]),
                        latitude=str(point["latitude"]),
                    ),
                    Point(
                        longitude=str(point_2["longitude"]),
                        latitude=str(point_2["latitude"]),
                    ),
                    Point(
                        longitude=str(point_3["longitude"]),
                        latitude=str(point_3["latitude"]),
                    ),
                    Point(
                        longitude=str(point["longitude"]),
                        latitude=str(point["latitude"]),
                    ),
                ]

            point = {
                "longitude": point["longitude"] + self.step,
                "latitude": self.start["latitude"],
            }
