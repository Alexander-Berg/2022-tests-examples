# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers import funcs
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import Call
from awacs.wrappers.luaparser import parse_call, dump_call, RawCall


def test_func():
    raw_call = parse_call('get_ip_by_iproute(123)')
    assert raw_call == RawCall('get_ip_by_iproute', [123])

    raw_call = parse_call('get_ip_by_iproute("v4")')
    assert raw_call == RawCall('get_ip_by_iproute', ['v4'])

    call_pb = modules_pb2.Call()
    funcs.raw_call_to_call_pb(raw_call, call_pb)

    assert call_pb.type == call_pb.GET_IP_BY_IPROUTE
    assert call_pb.get_ip_by_iproute_params.family == 'v4'
    call = Call(call_pb)
    call.validate()

    raw_call = parse_call('get_log_path("name", get_port_var("test_var", 1))')
    assert raw_call == RawCall('get_log_path', ['name', RawCall('get_port_var', ['test_var', 1])])

    call_pb = modules_pb2.Call()
    funcs.raw_call_to_call_pb(raw_call, call_pb)

    assert call_pb.type == call_pb.GET_LOG_PATH
    assert call_pb.get_log_path_params.name == 'name'
    assert call_pb.get_log_path_params.HasField('f_port')
    assert call_pb.get_log_path_params.f_port.type == call_pb.GET_PORT_VAR
    assert call_pb.get_log_path_params.f_port.get_port_var_params.var == 'test_var'
    assert call_pb.get_log_path_params.f_port.get_port_var_params.offset.value == 1
    assert not call_pb.get_log_path_params.f_port.get_port_var_params.HasField('default')
    call = Call(call_pb)
    call.validate()

    raw_call = funcs.call_pb_to_raw_call(call_pb)
    assert raw_call == RawCall(func='get_log_path', args=['name', RawCall(func='get_port_var', args=['test_var', 1])])
    assert raw_call == parse_call(dump_call(raw_call))

    raw_call = parse_call('prefix_with_dc("bnp", "", "")')
    assert raw_call == RawCall('prefix_with_dc', ['bnp', '', ''])
    call_pb = modules_pb2.Call()
    funcs.raw_call_to_call_pb(raw_call, call_pb)
    assert raw_call == parse_call(dump_call(funcs.call_pb_to_raw_call(call_pb)))


def test_get_random_timedelta():
    call_pb = modules_pb2.Call()
    call_pb.type = modules_pb2.Call.GET_RANDOM_TIMEDELTA
    call_pb.get_random_timedelta_params.start = 1
    call_pb.get_random_timedelta_params.end = 2

    call = Call(call_pb)
    with pytest.raises(ValidationError) as e:
        call.validate()
    assert e.match(r"get_random_timedelta's 3rd argument \(unit\) must be one of the following: 'ms', 's'")

    call_pb.get_random_timedelta_params.unit = 's'
    call.update_pb(call_pb)
    call.validate()

    call_pb.get_random_timedelta_params.start = 200
    call_pb.get_random_timedelta_params.end = 100
    call.update_pb(call_pb)
    with pytest.raises(ValidationError) as e:
        call.validate()
    assert e.match("invalid arguments for get_random_timedelta's call: \"start\" must be less or equal to \"end\"")
