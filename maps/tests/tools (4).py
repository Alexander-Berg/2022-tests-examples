from typing import Union

from maps_adv.common.helpers import dt


def dt_timestamp(value: Union[str, int]) -> int:
    return int(dt(value).timestamp())
