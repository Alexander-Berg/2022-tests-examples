import json
import yandex.maps.proto.bizdir.common.hours_pb2 as hr
from google.protobuf import text_format
from maps.bizdir.sps.yang import hours
from . import testdata

Days = list["hr.DayOfWeek.V"]
Hours = list[tuple[int, int]]

dow = hr.DayOfWeek
WEEKEND = [dow.SATURDAY, dow.SUNDAY]
WEEKDAYS = [dow.MONDAY, dow.TUESDAY, dow.WEDNESDAY, dow.THURSDAY, dow.FRIDAY]
EVERYDAY = WEEKDAYS + WEEKEND
ODD = [dow.MONDAY, dow.WEDNESDAY, dow.FRIDAY]
EVEN = [dow.TUESDAY, dow.THURSDAY, dow.SATURDAY]


def _timerange(f: int, t: int) -> hr.TimeRange:
    tr = hr.TimeRange()
    setattr(tr, 'from', f * 3600)
    setattr(tr, 'to', t * 3600)
    return tr


def _hours(days: Days, hrs: Hours) -> hr.Hours:
    timeranges = [_timerange(f, t) for f, t in hrs]
    return hr.Hours(day=days, time_range=timeranges)


def _openhours(days: Days, hrs: Hours) -> hr.OpenHours:
    return hr.OpenHours(hours=[_hours(days, hrs)])


def _multiopenhours(args: list[tuple[Days, tuple[int, int]]]) -> hr.OpenHours:
    return hr.OpenHours(hours=[_hours(d, [h]) for d, h in args])


HOURS = [
    (_openhours(EVERYDAY, [(10, 22)]), testdata.EVERYDAY_10_22),
    (_openhours(WEEKDAYS, [(10, 13), (14, 22)]), testdata.WEEKDAYS_10_22_BREAK_13_14),
    (_openhours(WEEKDAYS, [(8, 10), (17, 19)]), testdata.WEEKDAYS_8_10_AND_17_19),
    (_openhours(WEEKDAYS[1:], [(8, 12), (13, 16), (17, 24)]), testdata.WEEKDAYS_EXCEPT_MON_8_24_TWO_BREAKS),
    (_multiopenhours([(ODD, (10, 15)), (EVEN, (15, 19))]), testdata.ODD_10_15_EVEN_15_19),
]


def test_hours_pb_to_cc() -> None:
    pretty_print = json.dumps
    for pb, cc in HOURS:
        assert pretty_print(cc) == pretty_print(hours.pb_to_cc(pb))
        assert cc == hours.pb_to_cc(pb)
        assert cc == hours.pb_to_cc(hours.cc_to_pb(cc))


def test_hours_cc_to_pb() -> None:
    pretty_print = text_format.MessageToString
    for pb, cc in HOURS:
        assert pretty_print(pb) == pretty_print(hours.cc_to_pb(cc))
        assert pb == hours.cc_to_pb(cc)
        assert pb == hours.cc_to_pb(hours.pb_to_cc(pb))


def test_isomorphism() -> None:
    data = [
        _openhours(WEEKDAYS, [(10, 17)]),
        _openhours(WEEKEND, [(22, 3)]),
        _openhours(WEEKDAYS, [(15, 18), (19, 3)])
    ] + [x[0] for x in HOURS]

    for pb in data:
        assert pb == hours.cc_to_pb(hours.pb_to_cc(pb))


def test_normalization_replaces_all_days_with_single_EVERYDAY() -> None:
    make_hours = lambda days: hr.OpenHours(hours=[_hours(days, [(10, 23)])])

    norm = make_hours([dow.EVERYDAY])
    assert hours.normalize(norm) == norm

    h = make_hours(WEEKDAYS + WEEKEND)
    assert hours.normalize(h) == norm


def test_normalization_standardizes_24h_timeranges() -> None:
    make_hours = lambda f, t: hr.OpenHours(hours=[_hours(WEEKEND, [(f, t)])])

    norm = make_hours(0, 0)
    norm.hours[0].time_range[0].all_day = True
    assert hours.normalize(norm) == norm

    hs = [make_hours(0, 24), make_hours(0, 0), make_hours(2, 2), make_hours(2, 26)]
    for h in hs:
        assert hours.normalize(h) == norm


def test_normalization_joins_equal_days() -> None:
    norm = _multiopenhours([(WEEKDAYS, (10, 17)), (WEEKEND, (15, 18))])
    assert hours.normalize(norm) == norm

    weekdays = [([day], (10, 17)) for day in WEEKDAYS]
    weekend = [([day], (15, 18)) for day in WEEKEND]
    h = _multiopenhours(weekdays + weekend)
    assert hours.normalize(h) == norm
