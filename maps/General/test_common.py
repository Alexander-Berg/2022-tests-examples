import datetime

from maps.routing.matrix_router.common.pylib.common import calculate_day_type


def test_validate_russian_holiday():
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=3, hour=12).timestamp()) == "fri"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=4, hour=12).timestamp()) == "sat"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=5, hour=12).timestamp()) == "mon"


def test_validate_ordinary_days():
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=9, hour=12).timestamp()) == "mon"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=10, hour=12).timestamp()) == "tue"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=11, hour=12).timestamp()) == "wed"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=12, hour=12).timestamp()) == "thu"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=13, hour=12).timestamp()) == "fri"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=14, hour=12).timestamp()) == "sat"
    assert calculate_day_type(datetime.datetime(year=2020, month=11, day=15, hour=12).timestamp()) == "sun"
