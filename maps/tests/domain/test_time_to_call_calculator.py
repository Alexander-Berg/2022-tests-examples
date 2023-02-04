import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.server.lib.domains.time_to_call_calcultator import (
    TimeToCallCalculator,
)

pytestmark = [
    # Wednesday, 14:00 Europe/Moscow
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00"))
]


@pytest.mark.parametrize(
    "open_hours",
    [
        None,
        [
            # Wednesday, 8-18
            (201600, 237600)
        ],
        [
            # Wednesday, all day
            (201600, 259200)
        ],
        [
            # Monday, Wednesday, Friday 8-18
            (28800, 64800),
            (201600, 237600),
            (374400, 410400),
        ],
        [
            # Wednesday, 9-11, 13-15, 17-19
            (205200, 212400),
            (219600, 226800),
            (234000, 241200),
        ],
        [
            # Monday 8-11, Wednesday 8-18
            (28800, 39600),
            (201600, 237600),
        ],
        [
            # Tuesday 22 - Wednesday 18
            (165600, 237600)
        ],
    ],
)
def test_calculates_as_now_if_call_center_and_org_are_open_and_reservation_available(
    open_hours,
):
    time_to_call = TimeToCallCalculator(
        org_open_hours=open_hours,
        reservation_datetime=dt("2019-12-25 14:00:00"),  # 17:00 MSK
        reservation_timezone="Europe/Moscow",
    ).calculate()

    assert time_to_call == dt("2019-12-25 11:00:00")


@pytest.mark.parametrize(
    "reservation_datetime, open_hours, expected_time_to_call",
    [
        (
            dt("2019-12-30 03:00:00"),  # 06:00 MSK
            [
                # Mon 0-9, no crossing, should try to call now
                (0, 32400)
            ],
            dt("2019-12-25 11:00:00"),
        ),
        (
            dt("2019-12-25 13:00:00"),  # 16:00 MSK
            [
                # Wednesday 8-11, 15-17
                (201600, 212400),
                (226800, 234000),
            ],
            dt("2019-12-25 12:00:00"),
        ),
        (
            dt("2020-01-08 05:00:00"),  # 08:00 MSK
            [
                # Wednesday 8-10
                (201600, 218800)
            ],
            dt("2020-01-01 06:00:00"),
        ),
        (
            dt("2019-12-31 12:00:00"),  # 15:00 MSK
            [
                # Tuesday 8-18
                (115200, 151200)
            ],
            dt("2019-12-31 06:00:00"),
        ),
        (
            dt("2020-01-02 12:00:00"),  # 15:00 MSK
            [
                # Tuesday, Thursday 8-18, Wednesday 8-11, 15-17
                (115200, 151200),
                (201600, 212400),
                (226800, 234000),
                (288000, 324000),
            ],
            dt("2019-12-25 12:00:00"),
        ),
        (
            dt("2019-12-30 13:00:00"),  # 16:00 MSK
            [
                # Monday 15-17
                (54305, 61200)
            ],
            dt("2019-12-30 12:05:00"),
        ),
    ],
)
def test_calculates_correctly_if_org_is_not_open_and_reservation_available(
    reservation_datetime, open_hours, expected_time_to_call
):
    time_to_call = TimeToCallCalculator(
        org_open_hours=open_hours,
        reservation_datetime=reservation_datetime,
        reservation_timezone="Europe/Moscow",
    ).calculate()

    assert time_to_call == expected_time_to_call


@pytest.mark.parametrize(
    "reservation_datetime, open_hours, reservation_timezone, expected_time_to_call",
    [
        (
            dt("2020-01-05 17:00:00"),  # 00:00 NSK
            [
                # Mon 0-9 (Sun 20 - Mon 5 in Moscow)
                (0, 32400)
            ],
            "Asia/Novosibirsk",
            dt("2019-12-29 17:00:00"),
        ),
        (
            dt("2020-01-01 04:00:00"),  # 16:00 Kamchatka
            [
                # Wednesday 7-11, 15-17
                # Tue 22 - Wed 2, Wed 4 - 8 in moscow time
                (198000, 212400),
                (226800, 234000),
            ],
            "Asia/Kamchatka",
            dt("2019-12-31 19:00:00"),
        ),
        (
            dt("2019-12-31 12:00:00"),  # 14:00 KGD
            [
                # Tuesday, Thursday 8-18, Wednesday 8-11, 15-17
                # Tue, Thu 9-19, Wed 9-12, 16-18 moscow time
                (115200, 151200),
                (201600, 212400),
                (226800, 234000),
                (288000, 324000),
            ],
            "Europe/Kaliningrad",
            dt("2019-12-25 13:00:00"),
        ),
    ],
)
def test_calculates_correctly_if_org_is_not_open_and_reservation_available_different_tz(
    reservation_datetime, reservation_timezone, open_hours, expected_time_to_call
):
    time_to_call = TimeToCallCalculator(
        org_open_hours=open_hours,
        reservation_datetime=reservation_datetime,
        reservation_timezone=reservation_timezone,
    ).calculate()

    assert time_to_call == expected_time_to_call


@pytest.mark.parametrize(
    "reservation_datetime, open_hours, expected_time_to_call",
    [
        (dt("2019-12-25 12:00:00"), None, dt("2019-12-25 06:00:00")),
        # Org open, Wed all day
        (dt("2019-12-25 12:00:00"), [(172800, 259200)], dt("2019-12-25 06:00:00")),
        # Org closed, Tue 15-18
        (dt("2020-01-07 14:00:00"), [(140400, 151200)], dt("2019-12-31 12:00:00")),
    ],
)
@pytest.mark.freeze_time(dt("2019-12-25 03:00:00"))
def test_calculates_correctly_if_call_center_off_and_reservation_available(
    reservation_datetime, open_hours, expected_time_to_call
):
    time_to_call = TimeToCallCalculator(
        org_open_hours=open_hours,
        reservation_datetime=reservation_datetime,
        reservation_timezone="Europe/Moscow",
    ).calculate()

    assert time_to_call == expected_time_to_call


@pytest.mark.parametrize(
    "reservation_datetime, open_hours, reservation_timezone",
    [
        (
            dt("2020-01-05 12:00:00"),  # Sun
            [
                # Org open, Mon, Wed, Fri 8-18
                (28800, 64800),
                (201600, 237600),
                (374400, 410400),
            ],
            "Europe/Moscow",
        ),
        # Org closed, Tue 15-18
        (
            dt("2020-01-01 05:00:00"),  # Wed 17:00
            [
                # Wednesday 7-11, 15-17
                # Tue 22 - Wed 2, Wed 4 - 8 in moscow time
                (198000, 212400),
                (226800, 234000),
            ],
            "Asia/Kamchatka",
        ),
        (
            dt("2019-12-26 05:00:00"),  # Thu 07:00 KGD
            [
                # Tuesday, Thursday 8-18, Wednesday 8-11, 15-17
                # Tue, Thu 9-19, Wed 9-12, 16-18 moscow time
                (115200, 151200),
                (201600, 212400),
                (226800, 234000),
                (288000, 324000),
            ],
            "Europe/Kaliningrad",
        ),
    ],
)
def test_returns_none_if_reservation_time_not_in_org_working_hours(
    reservation_datetime, open_hours, reservation_timezone
):
    time_to_call = TimeToCallCalculator(
        org_open_hours=open_hours,
        reservation_datetime=reservation_datetime,
        reservation_timezone=reservation_timezone,
    ).calculate()

    assert time_to_call is None


def test_returns_none_if_reservation_time_too_close_to_org_closing():
    time_to_call = TimeToCallCalculator(
        org_open_hours=[
            # Wednesday 7-11, 15-17
            (198000, 212400),
            (226800, 234000),
        ],
        reservation_datetime=dt("2020-01-01 04:31:00"),  # lt 30 minutes before closing
        reservation_timezone="Asia/Kamchatka",
    ).calculate()

    assert time_to_call is None


def test_returns_none_if_calculated_time_too_close_to_reservation():
    time_to_call = TimeToCallCalculator(
        org_open_hours=[
            # Tuesday 8-18
            (115200, 151200)
        ],
        reservation_datetime=dt(
            "2019-12-31 06:45:00"
        ),  # calculated time "2019-12-31 06:00:00"
        reservation_timezone="Europe/Moscow",
    ).calculate()

    assert time_to_call is None
