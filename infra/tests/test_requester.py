import grpc
import pytest
from collections import defaultdict

from tornado import gen

from infra.yasm.gateway.lib.client import requester
from infra.yasm.gateway.lib.client.requester import ReadyHostAttemptSequence, MultiAttemptRpc, GrpcCallsFailed


class MockedHost(object):
    def __init__(self, fqdn, ready):
        self.fqdn = fqdn
        self.channel = fqdn
        self.ready = ready

    def check_connection(self):
        return self.ready


def test_random_sequence_is_random():
    hosts_per_cluster = [
        ("sas", [MockedHost("sas1", True), MockedHost("sas2", True), MockedHost("sas3", True)]),
        ("sas", [MockedHost("sas1", True), MockedHost("sas2", True), MockedHost("sas3", True)])
    ]
    host_at_pos_counters = [defaultdict(int) for _ in xrange(0, 4)]
    for i in xrange(0, 50):
        sequence = ReadyHostAttemptSequence.make_random(hosts_per_cluster, 2, 2)
        cur_host_sequence = []
        while not sequence.empty():
            cur_host_sequence.append(sequence.pop())

        for i, host in enumerate(cur_host_sequence):
            host_at_pos_counters[i][host] += 1

    for host_counters in host_at_pos_counters:
        assert len(host_counters) == 6


def test_random_sequence_prioritises_ready_channels():
    hosts_per_cluster = [
        ("sas", [MockedHost("sas1", True), MockedHost("sas2", True), MockedHost("sas3", False)]),
        ("sas", [MockedHost("sas1", True), MockedHost("sas2", False), MockedHost("sas3", True)])
    ]
    host_at_first_2_pos_counters = defaultdict(int)
    host_at_other_pos_counters = defaultdict(int)
    for i in xrange(0, 100):
        sequence = ReadyHostAttemptSequence.make_random(hosts_per_cluster, 2, 2)
        host_at_first_2_pos_counters[sequence.pop()] += 1
        host_at_first_2_pos_counters[sequence.pop()] += 1
        host_at_other_pos_counters[sequence.pop()] += 1
        host_at_other_pos_counters[sequence.pop()] += 1

    assert len(host_at_first_2_pos_counters) == 4
    for host in host_at_first_2_pos_counters.iterkeys():
        assert host.ready

    assert len(host_at_other_pos_counters) == 6


class FixedHostSequence(object):
    def __init__(self, sequence):
        self._sequence = sequence

    def pop(self, log):
        return self._sequence.pop()

    def empty(self):
        return not self._sequence


class RpcMock(object):
    def __init__(self, error_code):
        self._error_code = error_code

    def future(self, request, timeout=None, metadata=None):
        return self._error_code


class StubMock(object):
    RPC_NAME = "RpcMock"

    def __init__(self, error_code):
        setattr(self, self.RPC_NAME, RpcMock(error_code))


class StubMockFactory(object):
    def __init__(self, channel_errors):
        self._channel_errors = channel_errors
        self.channels_visited = set()

    def create_stub_mock(self, channel):
        self.channels_visited.add(channel)
        return StubMock(self._channel_errors.get(channel))


class ClockMock(object):
    def __init__(self, start_time):
        self.cur_time = start_time

    def time(self):
        return self.cur_time


class RpcExceptionMock(grpc.RpcError, grpc.Call):
    def __init__(self, code):
        self._code = code

    def code(self):
        return self._code


@pytest.fixture
def fwrap_mock(monkeypatch):
    def _fwrap_mock(error_code, ioloop=None):
        f = gen.Future()
        if error_code is None:
            f.set_result("OK")
        else:
            f.set_exception(RpcExceptionMock(error_code))
        return f
    monkeypatch.setattr(requester, "fwrap", _fwrap_mock)


@pytest.mark.gen_test
def test_requester_retries_calls(fwrap_mock):
    clock = ClockMock(1000)
    CALL_TIMEOUT = 10
    request = "some string instead of request should be fine"
    stub_factory = StubMockFactory(
        {
            "sas1": grpc.StatusCode.INTERNAL
        }
    )
    call = MultiAttemptRpc(
        FixedHostSequence(
            [
                MockedHost("sas2", True),
                MockedHost("sas1", True)
            ]
        ),
        stub_factory.create_stub_mock,
        StubMock.RPC_NAME,
        request,
        CALL_TIMEOUT,
        time_func=clock.time
    )
    response = yield call.future()
    assert "OK" == response
    assert stub_factory.channels_visited == {"sas1", "sas2"}


@pytest.mark.gen_test
def test_requester_does_not_retry_bad_request(fwrap_mock):
    clock = ClockMock(1000)
    request = "some string instead of request should be fine"
    stub_factory = StubMockFactory(
        {
            "sas1": grpc.StatusCode.INVALID_ARGUMENT
        }
    )
    call = MultiAttemptRpc(
        FixedHostSequence(
            [
                MockedHost("sas2", True),
                MockedHost("sas1", True)
            ]
        ),
        stub_factory.create_stub_mock,
        StubMock.RPC_NAME,
        request,
        10,
        time_func=clock.time
    )
    with pytest.raises(GrpcCallsFailed):
        yield call.future()
    assert stub_factory.channels_visited == {"sas1"}


@pytest.mark.gen_test
def test_requester_does_not_retry_after_deadline(fwrap_mock):
    clock = ClockMock(1000)
    CALL_TIMEOUT = 10
    request = "some string instead of request should be fine"
    stub_factory = StubMockFactory(
        {
            "sas1": grpc.StatusCode.INTERNAL
        }
    )

    def create_stub_mock_and_advance_time(channel):
        stub_mock = stub_factory.create_stub_mock(channel)
        clock.cur_time += CALL_TIMEOUT + 1
        return stub_mock

    call = MultiAttemptRpc(
        FixedHostSequence(
            [
                MockedHost("sas2", True),
                MockedHost("sas1", True)
            ]
        ),
        create_stub_mock_and_advance_time,
        StubMock.RPC_NAME,
        request,
        CALL_TIMEOUT,
        time_func=clock.time
    )
    with pytest.raises(GrpcCallsFailed):
        yield call.future()
    assert stub_factory.channels_visited == {"sas1"}
