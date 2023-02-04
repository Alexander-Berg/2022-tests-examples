import pytest

from skybone.rbtorrent.skbn.dfs import get_range_for_request, _parse_http_range, adjust_range


def test_get_range_for_request():
    cnt = 20
    piecelen = 4 * 1024 * 1024
    total_length = cnt * piecelen
    idxs = [(i, piecelen) for i in xrange(cnt)]

    needed = set(xrange(20))
    assert get_range_for_request(needed, idxs, total_length) == ((0, total_length - 1), (0, 19))

    needed = set(xrange(5))
    assert get_range_for_request(needed, idxs, total_length) == ((0, 5 * piecelen - 1), (0, 4))

    needed = set(xrange(5, 20))
    assert get_range_for_request(needed, idxs, total_length) == ((5 * piecelen, total_length - 1), (5, 19))

    needed = set([5, 11, 15])
    assert get_range_for_request(needed, idxs, total_length) == ((5 * piecelen, 16 * piecelen - 1), (5, 15))

    needed = set()
    assert get_range_for_request(needed, idxs, total_length) == (None, None)

    needed = set([21, 22, 23])
    assert get_range_for_request(needed, idxs, total_length) == (None, None)

def test_parse_http_range():
    assert _parse_http_range('bytes=0-1023') == (0, 1023)

    with pytest.raises(ValueError):
        _parse_http_range('bytes=0-1023, 2048-4095')

    with pytest.raises(ValueError):
        _parse_http_range('bytes=0-')

    with pytest.raises(ValueError):
        _parse_http_range('bytes=-1023')

    with pytest.raises(ValueError):
        _parse_http_range('invalid range spec')

def test_adjust_range():
    assert adjust_range((1024, 4095), (1048576, 5242879)) == (1024 + 1048576, 4095 + 1048576)

    with pytest.raises(AssertionError):
        adjust_range((1024, 2047), (0, 1023))
