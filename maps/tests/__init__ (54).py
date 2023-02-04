from asyncio import coroutine
from typing import List
from unittest.mock import Mock


class Any:
    def __init__(self, instance_of: type):
        self.instance_of = instance_of

    def __eq__(self, value) -> bool:
        return isinstance(value, self.instance_of)


def coro_mock():
    coro = Mock(name="CoroutineResult")
    corofunc = Mock(name="CoroutineFunction", side_effect=coroutine(coro))
    corofunc.coro = coro
    return corofunc


def make_points(raw_points: List[str], with_id: bool = False) -> List[dict]:
    result = []

    for raw_point in raw_points:
        lon, lat = raw_point.strip().split(" ")
        point = {"longitude": lon, "latitude": lat}

        if with_id:
            point["id"] = Any(int)

        result.append(point)

    return result


def make_polygons(raw_polygons: List[str]) -> List[List[dict]]:
    result = []
    for raw_polygon in raw_polygons:
        raw_points = raw_polygon.split(",")

        result.append(make_points(raw_points))

    return result
