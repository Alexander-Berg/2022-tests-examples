import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.telegraphist.server.lib.domain import ru_format_date


@pytest.mark.parametrize(
    "input_, expected",
    (
        ["2020-01-01", "1 января"],
        ["2020-02-03", "3 февраля"],
        ["2020-03-07", "7 марта"],
        ["2020-04-10", "10 апреля"],
        ["2020-05-13", "13 мая"],
        ["2020-06-16", "16 июня"],
        ["2020-07-19", "19 июля"],
        ["2020-08-20", "20 августа"],
        ["2020-09-23", "23 сентября"],
        ["2020-10-25", "25 октября"],
        ["2020-11-27", "27 ноября"],
        ["2020-12-31", "31 декабря"],
    ),
)
def test_format_as_expected(input_, expected):
    got = ru_format_date(dt(input_))

    assert got == expected


@pytest.mark.parametrize(
    "input_, expected",
    (
        ["2020-01-01", "1 января 2020"],
        ["2021-02-03", "3 февраля 2021"],
        ["2022-03-07", "7 марта 2022"],
        ["2023-04-10", "10 апреля 2023"],
        ["2024-05-13", "13 мая 2024"],
        ["2025-06-16", "16 июня 2025"],
        ["2026-07-19", "19 июля 2026"],
        ["2027-08-20", "20 августа 2027"],
        ["2028-09-23", "23 сентября 2028"],
        ["2029-10-25", "25 октября 2029"],
        ["2030-11-27", "27 ноября 2030"],
        ["2031-12-31", "31 декабря 2031"],
    ),
)
def test_format_with_year_as_expected(input_, expected):
    got = ru_format_date(dt(input_), with_year=True)

    assert got == expected
