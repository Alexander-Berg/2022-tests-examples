import json
import yatest
import yandex.maps.proto.bizdir.callcenter.callcenter_pb2 as cc
from maps.bizdir.sps.yang import typing as ty
from google.protobuf import text_format
from typing import Final, TypeVar

# hours #
# ежедневно, с 10 до 22:00
EVERYDAY_10_22 = json.loads('''[{
    "day": "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY",
    "timeRange": [ "10:00-22:00" ],
    "breakRange": []
}]''')

# по будням, с 10 до 22:00, перерыв с часу до двух
WEEKDAYS_10_22_BREAK_13_14 = json.loads('''[{
    "day": "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
    "timeRange": [ "10:00-22:00" ],
    "breakRange": [ "13:00-14:00" ]
}]''')

# по будням, кроме понедельника, с 8 до 24:00, перерыв с 12 до 13 и с 16 до 17
WEEKDAYS_EXCEPT_MON_8_24_TWO_BREAKS = json.loads('''[{
    "day": "TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
    "timeRange": [ "08:00-24:00" ],
    "breakRange": [ "12:00-13:00", "16:00-17:00" ]
}]''')

# по будням, с 8:00 до 10:00 и с 17:00 до 19:00
WEEKDAYS_8_10_AND_17_19 = json.loads('''[{
    "day": "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
    "timeRange": [ "08:00-10:00", "17:00-19:00" ],
    "breakRange": []
}]''')

# пн-ср-пт, с 10:00 до 15:00, вт-чт-сб с 15:00 до 19:00
ODD_10_15_EVEN_15_19 = json.loads('''[
{
    "day": "MONDAY,WEDNESDAY,FRIDAY",
    "timeRange": [ "10:00-15:00" ],
    "breakRange": []
}, {
    "day": "TUESDAY,THURSDAY,SATURDAY",
    "timeRange": [ "15:00-19:00" ],
    "breakRange": []
}]''')


# callcenter #
DATA_PATH: Final = yatest.common.source_path("maps/bizdir/sps/yang/tests/data")
T = TypeVar('T')


def parse_pbtext(path: str, msg: T) -> T:
    return text_format.Parse(open(f'{DATA_PATH}/{path}').read(), msg)  # type:ignore


def expected_solution(fn: str) -> tuple[ty.CC, cc.TaskResult]:
    return (json.loads(open(f'{DATA_PATH}/{fn}-yang-solution.json').read()),
            parse_pbtext(f'{fn}-taskresult.pbtext', cc.TaskResult()))


def expected_input(fn: str) -> tuple[cc.Task, ty.CC]:
    return (parse_pbtext(f'{fn}.pbtext', cc.Task()),
            json.loads(open(f'{DATA_PATH}/{fn}-yang-input.json').read()))


YANG_INPUTS = [expected_input('pushkin'), expected_input('kazan')]
YANG_SOLUTIONS = [expected_solution('pushkin'), expected_solution('refusal')]
