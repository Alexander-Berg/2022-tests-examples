# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.config import Call
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import ResponseHeadersIf
from awacs.wrappers import main

from awtest.wrappers import get_validation_exception


def test_response_headers_if():
    pb = modules_pb2.ResponseHeadersIfModule()

    m = ResponseHeadersIf(pb)

    e = get_validation_exception(m.validate)
    e.match('at least one of the "create_header", "delete_header" must be specified')

    pb.create_header.add(key='header-name', value='header-value')
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('at least one of the "if_has_header", "matcher" must be specified')

    pb.if_has_header = 'a-.*'
    m.update_pb(pb)
    m.validate(chained_modules=True)

    pb.matcher.SetInParent()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('at most one of the "if_has_header", "matcher" must be specified')

    pb.if_has_header = ''
    pb.delete_header = 'b-.*'
    m.update_pb(pb)

    with mock.patch.object(main, 'validate_pire_regexp') as stub_1, \
            mock.patch.object(main, 'validate_header_name') as stub_2, \
            mock.patch.object(m.matcher, 'validate') as stub_3:
        m.validate(chained_modules=True)
    stub_1.assert_has_calls([
        mock.call('b-.*'),
    ], any_order=True)
    stub_2.assert_has_calls([
        mock.call('header-name'),
    ], any_order=True)
    assert stub_3.called


def test_response_headers_if_2():
    pb = modules_pb2.ResponseHeadersIfModule(if_has_header='a-.*')
    pb.create_header.add(key='My-Header', value='123')

    m = ResponseHeadersIf(pb)
    m.validate(chained_modules=True)

    assert m.to_config().table['create_header'].table == {'My-Header': '123'}

    entry_pb = pb.create_header.add(key='\\')
    entry_pb.f_value.type = entry_pb.f_value.GET_INT_VAR
    entry_pb.f_value.get_int_var_params.var = 'test'
    m.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        m.validate(chained_modules=True)
    e.match(r'create_header\[1\] -> key: invalid header name')

    entry_pb.key = 'Authorization'
    m.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        m.validate(chained_modules=True)
    e.match(r'create_header\[1\] -> value: only the following functions allowed here: "get_str_env_var"')

    entry_pb.f_value.type = entry_pb.f_value.GET_STR_ENV_VAR
    entry_pb.f_value.get_str_env_var_params.var.value = 'test'
    m.update_pb(pb)
    m.validate(chained_modules=True)
    t = m.to_config().table['create_header'].table
    assert t['My-Header'] == '123'
    assert t['Authorization'] == Call('get_str_env_var', ['test'])

    entry_pb.f_value.type = entry_pb.f_value.GET_STR_ENV_VAR
    entry_pb.f_value.get_str_env_var_params.ClearField('var')
    m.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        m.validate(chained_modules=True)
    e.match('get_str_env_var expects at least 1 arguments')
