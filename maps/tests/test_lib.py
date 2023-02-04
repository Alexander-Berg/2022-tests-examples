from unittest.mock import Mock

from maps.geoq.hypotheses.lib.lib import (
    window, time_difference, yt_move
)


def test_window():
    window_size = 8
    for i, w in enumerate(window(range(128), window_size)):
        assert w == tuple(range(i, i + window_size))


def test_empty_window():
    assert list(window([])) == []


def test_time_difference():
    assert time_difference('2020-01-01', '2020-01-02') == 86400
    assert time_difference('2020-01-02', '2020-01-01') == 86400
    assert time_difference('2020-01-01', '2020-01-01') == 0


def test_yt_move():
    ytc_mock = Mock()

    path_from = '//some/path_from'
    path_to = '//some/path_to'
    yt_move(ytc_mock, path_from, path_to, days=7)

    ytc_mock.move.assert_called_once()
    ytc_mock.set.assert_called_once()
