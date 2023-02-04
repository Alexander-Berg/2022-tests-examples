"""Tests limit handling utils."""

from walle.util.limits import parse_timed_limit, TimedLimit


def test_timed_limit_parsing():
    assert parse_timed_limit({"period": "3s", "limit": 1}) == TimedLimit(period=3, limit=1)
    assert parse_timed_limit({"period": "2m", "limit": 2}) == TimedLimit(period=120, limit=2)
    assert parse_timed_limit({"period": "2h", "limit": 3}) == TimedLimit(period=2 * 60 * 60, limit=3)
    assert parse_timed_limit({"period": "5d", "limit": 4}) == TimedLimit(period=5 * 24 * 60 * 60, limit=4)
