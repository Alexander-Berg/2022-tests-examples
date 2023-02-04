import datetime
import pprint
import pytest
from typing import List, Union, Tuple, Any
from ads_pytorch.tools.date_range import (
    slice_date_range,
    match_date_ranges_with_shift,
    DateRange,
    validate_disjoint
)


_DD = datetime.datetime
_DELTA = datetime.timedelta


##################################################################
#                        slice_date_range                        #
##################################################################


def test_start_greater_end_slice():
    with pytest.raises(ValueError):
        slice_date_range(
            start_date=_DD(2021, 3, 17),
            end_date=_DD(2021, 3, 16),
            timedelta=_DELTA(seconds=3600)
        )


def test_start_equal_end_slice():
    with pytest.raises(ValueError):
        slice_date_range(
            start_date=_DD(2021, 3, 17),
            end_date=_DD(2021, 3, 17),
            timedelta=_DELTA(seconds=3600)
        )


def test_slice_timedelta_not_divisor_of_range():
    with pytest.raises(ValueError):
        slice_date_range(
            start_date=_DD(2021, 3, 17),
            end_date=_DD(2021, 3, 18),
            timedelta=_DELTA(seconds=3603)
        )


def test_proper_slice():
    slices = slice_date_range(
        start_date=_DD(2021, 3, 17, 1),
        end_date=_DD(2021, 3, 17, 1, 40),
        timedelta=_DELTA(seconds=1200)
    )

    assert len(set(slices)) == len(slices)
    assert set(slices) == {
        DateRange(start=_DD(2021, 3, 17, 1), end=_DD(2021, 3, 17, 1, 20)),
        DateRange(start=_DD(2021, 3, 17, 1, 20), end=_DD(2021, 3, 17, 1, 40))
    }


def test_single_slice():
    slices = slice_date_range(
        start_date=_DD(2021, 3, 17, 1),
        end_date=_DD(2021, 3, 17, 1, 40),
        timedelta=_DELTA(seconds=2400)
    )

    assert len(set(slices)) == len(slices)
    assert set(slices) == {
        DateRange(start=_DD(2021, 3, 17, 1), end=_DD(2021, 3, 17, 1, 40))
    }


def test_timedelta_greater_than_delta_slice():
    with pytest.raises(ValueError):
        slice_date_range(
            start_date=_DD(2021, 3, 17, 1),
            end_date=_DD(2021, 3, 17, 1, 40),
            timedelta=_DELTA(seconds=5000000)
        )


##################################################################
#                merge_train_and_eval_sliced_uris                #
##################################################################


def _gen_hour_range(start: datetime.datetime, count: int) -> List[DateRange]:
    return [
        DateRange(
            start=start + datetime.timedelta(hours=i),
            end=start + datetime.timedelta(hours=i + 1)
        )
        for i in range(count)
    ]


def test_disjoint():
    validate_disjoint(date_ranges=_gen_hour_range(datetime.datetime(2021, 3, 17), 3))


def test_disjoint_hole_fail():
    uris = _gen_hour_range(datetime.datetime(2021, 3, 17), 3)
    uris += _gen_hour_range(datetime.datetime(2021, 3, 18), 3)
    with pytest.raises(ValueError):
        validate_disjoint(date_ranges=uris)


def test_disjoint_hole_ok():
    uris = _gen_hour_range(datetime.datetime(2021, 3, 17), 3)
    uris += _gen_hour_range(datetime.datetime(2021, 3, 18), 3)
    validate_disjoint(date_ranges=uris, allow_holes=True)


@pytest.mark.parametrize('allow_holes', [True, False])
def test_non_disjoint(allow_holes):
    uris = _gen_hour_range(datetime.datetime(2021, 3, 17, 5), 10)
    uris += _gen_hour_range(datetime.datetime(2021, 3, 17), 10)
    with pytest.raises(ValueError):
        validate_disjoint(date_ranges=uris, allow_holes=allow_holes)


def test_match_start_tail():
    train_ranges = _gen_hour_range(_DD(2021, 3, 17), 10)
    match_ranges = _gen_hour_range(_DD(2021, 3, 16, 22), 10)

    with pytest.raises(ValueError):
        match_date_ranges_with_shift(
            train_ranges=train_ranges,
            match_ranges=match_ranges,
            shift=datetime.timedelta(hours=1)
        )


def test_match_finish_tail():
    train_ranges = _gen_hour_range(_DD(2021, 3, 17), 10)
    match_ranges = _gen_hour_range(_DD(2021, 3, 17, 3), 10)

    with pytest.raises(ValueError):
        match_date_ranges_with_shift(
            train_ranges=train_ranges,
            match_ranges=match_ranges,
            shift=datetime.timedelta(hours=1)
        )


def test_match_ok_interval():
    train_ranges = _gen_hour_range(_DD(2021, 3, 17), 10)
    match_ranges = _gen_hour_range(_DD(2021, 3, 17, 3), 7)

    result = match_date_ranges_with_shift(
        train_ranges=train_ranges,
        match_ranges=match_ranges,
        shift=datetime.timedelta(hours=3)
    )

    assert set(result) == {
        (
            DateRange(start=_DD(2021, 3, 17) + datetime.timedelta(hours=i), end=_DD(2021, 3, 17) + datetime.timedelta(hours=i + 1)),
            DateRange(start=_DD(2021, 3, 17, 3) + datetime.timedelta(hours=i), end=_DD(2021, 3, 17, 3) + datetime.timedelta(hours=i + 1))
        )
        for i in range(7)
    }


def test_intersecting_intervals():
    # Start with minutes
    train_ranges = _gen_hour_range(_DD(2021, 3, 16, 0, 15), 150)
    match_ranges = _gen_hour_range(_DD(2021, 3, 17, 3), 7)

    with pytest.raises(ValueError):
        match_date_ranges_with_shift(
            train_ranges=train_ranges,
            match_ranges=match_ranges,
            shift=datetime.timedelta(hours=3)
        )


def test_match_large_interval():
    train_ranges = _gen_hour_range(_DD(2021, 3, 16), 150)
    match_ranges = _gen_hour_range(_DD(2021, 3, 17, 3), 7)

    results = match_date_ranges_with_shift(
        train_ranges=train_ranges,
        match_ranges=match_ranges,
        shift=datetime.timedelta(hours=3)
    )

    assert set(results) == {
        (
            DateRange(
                start=_DD(2021, 3, 17) + datetime.timedelta(hours=i),
                end=_DD(2021, 3, 17) + datetime.timedelta(hours=i + 1)
            ),
            DateRange(
                start=_DD(2021, 3, 17, 3) + datetime.timedelta(hours=i),
                end=_DD(2021, 3, 17, 3) + datetime.timedelta(hours=i + 1)
            )
        )
        for i in range(7)
    }
