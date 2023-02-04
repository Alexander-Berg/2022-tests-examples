import itertools
from datetime import datetime
from typing import Collection

from google.protobuf import timestamp_pb2

from maps_adv.common.helpers import Any, coro_mock, dt

__all__ = [
    "Any",
    "coro_mock",
    "dt",
    "all_combinations",
    "dt_to_proto",
    "all_creative_type_combinations",
]


def all_combinations(it: Collection):
    return list(
        itertools.chain.from_iterable(
            itertools.combinations(it, r) for r in range(1, len(it) + 1)
        )
    )


def dt_to_proto(py_dt: datetime):
    seconds, micros = map(int, "{:.6f}".format(py_dt.timestamp()).split("."))
    return timestamp_pb2.Timestamp(seconds=seconds, nanos=micros * 1000)


all_creative_type_combinations = set(
    all_combinations(
        [
            "pin",
            "billboard",
            "icon",
            "pin_search",
            "logo_and_text",
            "text",
            "via_point",
            "banner",
            "audio_banner",
        ]
    )
)
