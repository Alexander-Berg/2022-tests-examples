from datetime import timedelta

import pytest
from jinja2 import Undefined

from maps_adv.common.helpers import dt
from maps_adv.geosmb.telegraphist.server.lib.templates import env


@pytest.mark.parametrize(
    "order, expected",
    (
        [
            dict(
                items=[
                    dict(key=1, booking_timestamp=dt("2020-01-10 13:00:00")),
                    dict(key=2, booking_timestamp=dt("2020-01-01 10:00:00")),
                    dict(key=3, booking_timestamp=dt("2020-01-20 21:00:00")),
                ]
            ),
            "2020-01-01 10:00:00+00:00",
        ],
        [dict(items=[]), "None"],
    ),
)
def test_earliest_order_booking(order, expected):
    got = env.from_string("{{ order | earliest_order_booking }}").render(order=order)

    assert got == expected


@pytest.mark.parametrize(
    ("tz_offset", "expected"),
    [
        (None, "2020-01-10 10:00:00+00:00"),
        (Undefined(), "2020-01-10 10:00:00+00:00"),
        (timedelta(hours=0), "2020-01-10 10:00:00+00:00"),
        (timedelta(hours=3), "2020-01-10 13:00:00+03:00"),
    ],
)
def test_earliest_order_booking_tz_offset(tz_offset, expected):
    got = env.from_string("{{ order | earliest_order_booking(tz_offset) }}").render(
        order=dict(items=[dict(key=1, booking_timestamp=dt("2020-01-10 10:00:00"))]),
        tz_offset=tz_offset,
    )

    assert got == expected


@pytest.mark.parametrize(
    "value, expected",
    (
        [dt("2020-11-27 13:00:49"), "13:00, пт, 27 нояб."],
        [dt("2020-01-01 04:37:00"), "04:37, ср, 1 янв."],
    ),
)
def test_babel_datetime_defaults(value, expected):
    got = env.from_string("{{ value | babel_datetime }}").render(value=value)

    assert got == expected


@pytest.mark.parametrize(
    "value, format, locale, expected",
    (
        [dt("2020-11-27 13:00:49"), "HH:mm, E, d MMM", "ru_RU", "13:00, пт, 27 нояб."],
        [dt("2020-11-25 13:00:49"), "EEEE, d MMMM", "de_DE", "Mittwoch, 25 November"],
    ),
)
def test_babel_datetime(value, format, locale, expected):
    got = env.from_string(
        "{{" + f" value | babel_datetime(format='{format}', locale='{locale}') " + "}}"
    ).render(value=value)

    assert got == expected
