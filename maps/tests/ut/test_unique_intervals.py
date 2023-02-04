from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote

from ya_courier_backend.util.unique_intervals import UniqueIntervals, Interval


@skip_if_remote
def test_gap_in_empty_container_is_given_interval():
    used_intervals = UniqueIntervals()
    assert list(used_intervals.gaps_in(Interval(1, 2))) == [Interval(1, 2)]
    assert list(used_intervals.gaps_in(Interval(-10000, 10000))) == [Interval(-10000, 10000)]


@skip_if_remote
def test_gap_in_used_interval_is_empty():
    used_intervals = UniqueIntervals()
    used_intervals.add(Interval(1, 100))
    used_intervals.add(Interval(-100, -1))

    assert list(used_intervals.gaps_in(Interval(1, 1))) == []
    assert list(used_intervals.gaps_in(Interval(-1, -1))) == []

    assert list(used_intervals.gaps_in(Interval(1, 2))) == []
    assert list(used_intervals.gaps_in(Interval(-2, -1))) == []

    assert list(used_intervals.gaps_in(Interval(99, 100))) == []
    assert list(used_intervals.gaps_in(Interval(-100, -99))) == []

    assert list(used_intervals.gaps_in(Interval(-50, -49))) == []
    assert list(used_intervals.gaps_in(Interval(49, 50))) == []


@skip_if_remote
def test_gaps_between_used_ones_are_returned():
    used_intervals = UniqueIntervals()
    used_intervals.add(Interval(1, 2))
    used_intervals.add(Interval(3, 4))
    used_intervals.add(Interval(5, 6))

    assert list(used_intervals.gaps_in(Interval(2, 3))) == [Interval(2, 3)]
    assert list(used_intervals.gaps_in(Interval(2, 4))) == [Interval(2, 3)]
    assert list(used_intervals.gaps_in(Interval(2, 4.5))) == [Interval(2, 3), Interval(4, 4.5)]
    assert list(used_intervals.gaps_in(Interval(0, 5))) == [Interval(0, 1), Interval(2, 3), Interval(4, 5)]
    assert list(used_intervals.gaps_in(Interval(3.5, 7))) == [Interval(4, 5), Interval(6, 7)]


@skip_if_remote
def test_adding_overlaping_intervals_merges_them():
    used_intervals = UniqueIntervals()
    assert used_intervals.get() == []

    used_intervals.add(Interval(5, 10))
    assert used_intervals.get() == [Interval(5, 10)]

    used_intervals.add(Interval(4, 5))
    assert used_intervals.get() == [Interval(4, 10)]

    used_intervals.add(Interval(3, 5))
    assert used_intervals.get() == [Interval(3, 10)]

    used_intervals.add(Interval(10, 11))
    assert used_intervals.get() == [Interval(3, 11)]

    used_intervals.add(Interval(10, 12))
    assert used_intervals.get() == [Interval(3, 12)]

    used_intervals.add(Interval(15, 20))
    assert used_intervals.get() == [Interval(3, 12), Interval(15, 20)]

    used_intervals.add(Interval(10, 16))
    assert used_intervals.get() == [Interval(3, 20)]

    used_intervals.add(Interval(30, 40))
    used_intervals.add(Interval(50, 60))
    used_intervals.add(Interval(2, 55))
    assert used_intervals.get() == [Interval(2, 60)]
