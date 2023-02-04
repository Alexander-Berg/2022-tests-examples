# coding: utf-8

import pytest

from infra.yasm.yasmapi import RtGolovanRequest, TsPoints, RtError


def prepare_timeline(signals, timeline=None):
    values_nums = set(map(len, signals.values()))
    assert len(values_nums) == 1, "signal values must have same length"
    values_num = values_nums.pop()
    timeline = timeline or range(values_num)
    return timeline


class MockedRtGolovanRequest(RtGolovanRequest):
    REQUEST_INTERVAL = 0

    def set_response(self, response):
        self._response = response

    def _request_signals(self, *args, **kwargs):
        return self._response


@pytest.fixture
def rt():
    return MockedRtGolovanRequest({"YASM_TEST": {"common_self": ["signal"]}})


class CommonApiFormat(object):

    @staticmethod
    def _set_response(rt, signals, timeline=None, messages=None):
        pass  # redefine me!

    def test_simple(self, rt):
        self._set_response(rt, {"host:tags:signal1": [1, 2],
                                "host:tags:signal2": [9, 8]})
        points = list(rt.points_from_request())
        assert points == [TsPoints(ts=0,
                                   values={"host": {"tags": {"signal1": 1,
                                                             "signal2": 9}}},
                                   errors={}),
                          TsPoints(ts=5,
                                   values={"host": {"tags": {"signal1": 2,
                                                             "signal2": 8}}},
                                   errors={})]

    def test_each_point_one_time(self, rt):
        self._set_response(rt, {"host:tags:signal": [1, 2]},
                           timeline=[0, 5]
                           )
        list(rt.points_from_request())
        self._set_response(rt,
                           {"host:tags:signal": [2, 3]},
                           timeline=[5, 10])
        result = list(rt.points_from_request())
        assert result == [
            TsPoints(ts=10, values={"host": {"tags": {"signal": 3}}}, errors={})]


def _make_new_values(timeline, values, all_errors):
    return [{'timestamp': ts,
             'value': value,
             'errors': errors} for ts, value, errors in zip(timeline, values, all_errors)]


class TestNewApiFormat(CommonApiFormat):

    @staticmethod
    def _set_response(rt, signals, timeline=None, messages=None):
        timeline = prepare_timeline(signals, timeline)
        errors = messages or {}
        rt.set_response({
            'status': 'ok',
            'response': {
                signal: _make_new_values(
                    timeline,
                    values,
                    errors.get(signal, [[]] * len(timeline)))
                for signal, values in signals.items()
            },
            'fqdn': 'yasmsrv-fol09.search.yandex.net'})

    def test_error_new_format(self, rt):
        err = "some funny error"
        signals = {"host:tags:signal1": [1, 2, 3],
                   "host:tags:signal2": [9, None, 7]}
        messages = {"host:tags:signal1": [[], [], []],
                    "host:tags:signal2": [[], [err], []]}
        self._set_response(
            rt, signals, messages=messages,
            timeline=[0, 5, 10]
        )
        result = list(rt.points_from_request())
        assert result == [
            TsPoints(ts=0,
                     values={"host": {"tags": {"signal1": 1,
                                               "signal2": 9}}},
                     errors={}),
            TsPoints(ts=5,
                     values={"host": {"tags": {"signal1": 2}}},
                     errors={"host": {"tags": {'signal2': ['some funny error']}}}),
            TsPoints(ts=10,
                     values={"host": {"tags": {"signal1": 3, "signal2": 7}}},
                     errors={})]


def test_wrong_request(rt):
    rt.set_response({
        'status': 'error',
        'response': {},
        'fqdn': 'yasmsrv-fol09.search.yandex.net'})
    with pytest.raises(RtError):
        list(rt.points_from_request())
