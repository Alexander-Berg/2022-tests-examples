"""Tests gevent utilities."""

import json
import textwrap
import timeit

import gevent
import pytest
import simplejson

from infra.walle.server.tests.lib.util import patch
from walle.util.gevent_tools import monkeypatch_json, demonkeypatch_json, gevent_idle_iter, gevent_idle_generator


@pytest.mark.parametrize("json", (json, simplejson))
@patch("gevent.idle")
def test_json_monkeypatching(gevent_idle, json):
    demonkeypatch_json()
    monkeypatch_json(idle_every=1)

    simple_obj = {"a": 1, "b": 2}
    simple_obj_string = json.dumps(simple_obj)

    complex_obj = {"a": 1, "b": 2, "c": {"c1": 1, "c2": 2, "c3": [1, 2, 3], "c4": [{}, {}, {}, {}, {}]}}
    complex_obj_string = json.dumps(complex_obj)

    assert gevent_idle.call_count == 0

    assert json.loads(simple_obj_string) == simple_obj
    assert gevent_idle.call_count == 1
    gevent_idle.reset_mock()

    assert json.loads(complex_obj_string) == complex_obj
    assert gevent_idle.call_count == 7
    gevent_idle.reset_mock()


@pytest.mark.slow
@pytest.mark.skip_on_cov
@pytest.mark.parametrize("module", ("json", "simplejson"))
def test_json_monkeypatching_performance(module):
    setup = "import {} as json".format(module)
    setup += textwrap.dedent(
        """
        from walle.util.gevent_tools import monkeypatch_json, demonkeypatch_json

        test_data = json.dumps([{
            "a": 1,
            "b": 2,
            "c": 3,
            "d": [{
                "a": 1,
                "b": 2,
                "c": 3,
                "d": 4,
                "e": 5,
                "f": {
                    "g": 6,
                    "h": 7,
                    "i": 8,
                    "j": 9,
                }
            } for j in range(10)]
        } for i in range(1000)])
    """
    )

    number = 10
    orig_time = timeit.timeit("json.loads(test_data)", setup + "\ndemonkeypatch_json()", number=number)
    patched_time = timeit.timeit("json.loads(test_data)", setup + "\nmonkeypatch_json()", number=number)

    assert patched_time // orig_time < 3


class TestGeventIdleIterator:
    def test_passes_items_through(self, mp):
        mp.function(gevent.idle, module=gevent)
        items = list(gevent_idle_iter(range(5)))
        expected_items = list(range(5))

        assert items == expected_items

    def test_idles_every_given_amount_of_items(self, mp):
        mock_idle = mp.function(gevent.idle, module=gevent)
        list(gevent_idle_iter(range(210)))

        assert mock_idle.call_count == 2

    def test_idles_on_start(self, mp):
        mock_idle = mp.function(gevent.idle, module=gevent)
        list(gevent_idle_iter(range(210), idle_on_start=True))

        assert mock_idle.call_count == 3

    def test_idles_more_frequently_for_cpu_greedy_iterators(self, mp):
        mock_idle = mp.function(gevent.idle, module=gevent)
        list(gevent_idle_iter(range(210), cpu_intensive=True))

        assert mock_idle.call_count == 21


class TestGeventIdleGenerator:
    def test_passes_items_through(self, mp):
        mp.function(gevent.idle, module=gevent)

        @gevent_idle_generator
        def _generator(limit):
            yield from range(limit)

        items = list(_generator(5))
        expected_items = list(range(5))

        assert items == expected_items

    def test_idles_every_given_amount_of_items(self, mp):
        mock_idle = mp.function(gevent.idle, module=gevent)

        @gevent_idle_generator()
        def _generator(limit):
            yield from range(limit)

        items = list(_generator(210))
        expected_items = list(range(210))

        assert items == expected_items
        assert mock_idle.call_count == 2

    def test_idles_on_start(self, mp):
        mock_idle = mp.function(gevent.idle, module=gevent)

        @gevent_idle_generator(idle_on_start=True)
        def _generator(limit):
            yield from range(limit)

        items = list(_generator(210))
        expected_items = list(range(210))

        assert items == expected_items
        assert mock_idle.call_count == 3

    def test_idles_more_frequently_for_cpu_greedy_iterators(self, mp):
        mock_idle = mp.function(gevent.idle, module=gevent)

        @gevent_idle_generator(cpu_intensive=True)
        def _generator(limit):
            yield from range(limit)

        items = list(_generator(210))
        expected_items = list(range(210))

        assert items == expected_items
        assert mock_idle.call_count == 21
