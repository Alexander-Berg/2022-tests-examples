# coding: utf-8

import pytest
from infra.yasm.yasmapi import GolovanRequest, RetryLimitExceeded


class MockedTransport:
    def __init__(self, host, period, st, et, signals, read_from_stockpile, fail=False):
        self.host = host
        self.period = period
        self.st = st
        self.et = et
        self.signals = signals
        self.read_from_stockpile = read_from_stockpile
        self.fail = fail
        pass

    def request(self, request, path):
        assert not self.read_from_stockpile and path == GolovanRequest.GOLOVAN_HISTDB_PATH \
            or self.read_from_stockpile and path == GolovanRequest.GOLOVAN_STOCKPILE_PATH
        self._check_request(request)
        return self._response()

    def _check_request(self, request):
        assert "ctxList" in request and len(request["ctxList"]) == 1

        req = request["ctxList"][0]
        assert "name" in req and req["name"] == "hist"
        assert "host" in req and req["host"] == self.host
        assert "period" in req and req["period"] == self.period
        assert "st" in req and req["st"] == self.st
        assert "et" in req and req["et"] == self.et
        assert "signals" in req and req["signals"] == self.signals
        assert "id" in req and req["id"] == "{}:{}_{}_{}".format(self.host, self.st, self.et, self.period)

    def _response(self):
        errors = ["Expected test error"] if self.fail else []
        resp = {
            'status': 'ok',
            'response': {
                '{}:{}_{}_{}'.format(self.host, self.st, self.et, self.period): {
                    'content': {
                        'timeline': [1568706300, 1568706600, 1568706900, 1568707200],
                        'values': {
                            signal: [1, 2, 3, 4] for signal
                            in self.signals
                        }
                    }
                },
                'errors': errors,
                'request_id': 'test'
            },
            'fqdn': 'test'
        }
        return resp


def test_request(read_from_stockpile=False, fail=False):
    host = "ASEARCH"
    period = 300
    et = 1568707200
    st = et - period * 3
    signals = ["itype=common:havg(cpu-us_hgram)", "itype=common:havg(mem-us_hgram)"]

    timestamps = []
    values = {}
    transport = MockedTransport(host, period, st, et, signals, read_from_stockpile, fail=fail)
    for timestamp, value in GolovanRequest(host, period, st, et, signals, explicit_fail=fail, transport=transport,
                                           read_from_stockpile=read_from_stockpile):
        for k in value:
            values.setdefault(k, []).append(value[k])
        timestamps.append(timestamp)

    assert timestamps == [1568706300, 1568706600, 1568706900, 1568707200]
    assert values == {signal: [1, 2, 3, 4] for signal in signals}


class TestGolovanRequest(object):
    def test_request_and_response_format(self):
        test_request()

    def test_stockpile_request(self):
        test_request(read_from_stockpile=True)

    def test_error_handling(self):
        with pytest.raises(RetryLimitExceeded):
            test_request(fail=True)
