# encoding: utf-8

from cppzoom import ZWindowRecord, ZAccumulator


def test_summ_enough_space():
    window_record = ZWindowRecord(3)
    accumulator = ZAccumulator('summ')
    assert window_record.size == 0

    window_record.push(5, 1)
    assert window_record.size == 1
    window_record.push(10, 2)
    assert window_record.size == 2
    window_record.rollup(accumulator)

    assert accumulator.get_value() == 3


def test_summ_not_enough_space():
    window_record = ZWindowRecord(3)
    accumulator = ZAccumulator('summ')
    assert window_record.size == 0

    window_record.push(5, 1)
    assert window_record.size == 1
    window_record.push(10, 2)
    assert window_record.size == 2
    window_record.push(15, 3)
    assert window_record.size == 3
    window_record.push(20, 4)
    assert window_record.size == 3
    window_record.rollup(accumulator)

    assert accumulator.get_value() == 9


def test_min():
    window_record = ZWindowRecord(3)
    accumulator = ZAccumulator('min')

    window_record.push(5, 1)
    window_record.push(10, 2)
    window_record.rollup(accumulator)

    assert accumulator.get_value() == 1
    assert window_record.size == 2


def test_hgram():
    window_record = ZWindowRecord(3)
    accumulator = ZAccumulator('hgram')

    window_record.push(5, 1)
    window_record.push(10, 2)
    window_record.rollup(accumulator)

    assert accumulator.get_value() == [[1, 2], 0, None]
    assert window_record.size == 2


def test_ugram():
    window_record = ZWindowRecord(3)
    accumulator = ZAccumulator('hgram')

    window_record.push(5, ['ugram', [(1, 0), (2, 1), (3, 1), (4, 0)]])
    window_record.push(10, ['ugram', [(2, 1), (3, 0)]])
    window_record.rollup(accumulator)

    assert accumulator.get_value() == ['ugram', [(2, 2), (3, 1), (4, 0)]]
    assert window_record.size == 2


def test_reordering():
    window_record = ZWindowRecord(3)
    accumulator = ZAccumulator('hgram')

    window_record.push(25, 5)
    window_record.push(10, 2)
    window_record.push(15, 3)
    window_record.push(20, 4)
    window_record.push(5, 1)
    window_record.rollup(accumulator)

    assert accumulator.get_value() == [[3, 4, 5], 0, None]
    assert window_record.size == 3
