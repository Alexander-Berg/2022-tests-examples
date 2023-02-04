# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.config import Call
from awacs.wrappers.main import Headers, ResponseHeaders, OrderedDict
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.util import is_valid_func


@pytest.mark.parametrize(('pb_cls', 'wrapper_cls'), [
    (modules_pb2.HeadersModule, Headers),
    (modules_pb2.ResponseHeadersModule, ResponseHeaders),
])
def test_headers_1(pb_cls, wrapper_cls):
    pb = pb_cls()

    headers = wrapper_cls(pb)

    with pytest.raises(ValidationError) as e:
        headers.validate(chained_modules=True)
    e.match('at least one of the "append", "append_func", "append_func_weak", "append_weak", '
            '"copy", "copy_weak", "create", "create_func", "create_func_weak", '
            '"create_weak", "delete" must be specified')

    pb.create_func['test'] = 'unknown_func'
    headers.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        headers.validate(chained_modules=True)
    e.match('invalid func "unknown_func" for header "test"')

    pb.create_func['test'] = 'realip'
    headers.update_pb(pb)
    headers.validate(chained_modules=True)

    pb.create_func['c'] = 'ssl_handshake_info'
    pb.create_func['b'] = 'reqid'
    pb.create_func['a'] = 'realport'
    pb.create_func['d'] = 'ja3'
    headers.update_pb(pb)
    headers.validate(chained_modules=True)

    headers.update_pb(pb)
    config = headers.to_config()
    assert isinstance(config.table, OrderedDict)
    assert isinstance(config.table['create_func'].table, OrderedDict)
    assert list(config.table['create_func'].table.keys()) == ['a', 'b', 'c', 'd', 'test']


@pytest.mark.parametrize(('pb_cls', 'wrapper_cls'), [
    (modules_pb2.HeadersModule, Headers),
    (modules_pb2.ResponseHeadersModule, ResponseHeaders),
])
def test_headers_2(pb_cls, wrapper_cls):
    pb = pb_cls()
    pb.create.add(key='My-Header', value='123')

    headers = wrapper_cls(pb)
    headers.validate(chained_modules=True)

    assert headers.to_config().table['create'].table == {'My-Header': '123'}

    entry_pb = pb.create.add(key='\\')
    entry_pb.f_value.type = entry_pb.f_value.GET_INT_VAR
    entry_pb.f_value.get_int_var_params.var = 'test'
    headers.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        headers.validate(chained_modules=True)
    e.match(r'create\[1\] -> key: invalid header name')

    entry_pb.key = 'Authorization'
    headers.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        headers.validate(chained_modules=True)
    e.match(r'create\[1\] -> value: only the following functions allowed here: "get_str_env_var"')

    entry_pb.f_value.type = entry_pb.f_value.GET_STR_ENV_VAR
    entry_pb.f_value.get_str_env_var_params.var.value = 'test'
    headers.update_pb(pb)
    headers.validate(chained_modules=True)
    t = headers.to_config().table['create'].table
    assert t['My-Header'] == '123'
    assert t['Authorization'] == Call('get_str_env_var', ['test'])

    entry_pb.f_value.type = entry_pb.f_value.GET_STR_ENV_VAR
    entry_pb.f_value.get_str_env_var_params.ClearField('var')
    headers.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        headers.validate(chained_modules=True)
    e.match('get_str_env_var expects at least 1 arguments')


def test_is_valid_func():
    assert not is_valid_func('xxx')
    assert not is_valid_func('time')
    assert not is_valid_func('time:')
    assert not is_valid_func('time:0')
    assert not is_valid_func('time:1')
    assert not is_valid_func('time:1.1s')
    assert not is_valid_func('time:1h23s')

    assert is_valid_func('time:1h')
    assert is_valid_func('time:1ms')
    assert is_valid_func('reqid')
    assert is_valid_func('realip')
