

class MockedMultiAttemptRpc(object):
    def __init__(self, host_sequence, stub_cls, rpc_name, request, timeout,
                 client_id=None, unistat=None, time_func=None,
                 log=None, signals_prefix=None):
        self.host_sequence = host_sequence
        self.stub_cls = stub_cls
        self.rpc_name = rpc_name
        self.request = request
        self.client_id = client_id
        self.timeout = timeout
        self.time_func = time_func
        self.log = log
        self.unistat = unistat
        self.signals_prefix = signals_prefix


def fill_timeseries(proto_ts, ts_value_pairs, errors=None):
    for ts, value in ts_value_pairs:
        proto_ts.values.append(value)
        proto_ts.timestamps_millis.append(ts * 1000)
    if errors:
        proto_ts.errors.extend(errors)
