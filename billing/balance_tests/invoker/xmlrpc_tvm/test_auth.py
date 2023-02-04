# coding=utf-8

import xmlrpclib
from collections import namedtuple

import pytest

try:
    from tvmauth.exceptions import TicketParsingException
except ImportError:
    from ticket_parser2.api.v1.exceptions import TicketParsingException

from balance.constants import TVM2_SERVICE_TICKET_HEADER
from balance.exc import XMLRPCInvalidParam
from medium.medium_servant import MediumXmlRpcTVMInvoker

from tests import object_builder as ob
from tests.base import ServiceTicket


@pytest.fixture()
def invoker():
    return MediumXmlRpcTVMInvoker({})


def test_no_ticket(medium_xmlrpc_tvm_server):
    with pytest.raises(xmlrpclib.Fault) as exception_info:
        medium_xmlrpc_tvm_server.SomeMethod()
    assert 'Service ticket is not specified' in exception_info.value.faultString


def test_ticket_parsing_exception(medium_xmlrpc_tvm_server, tvm_check_service_ticket_mock):
    ticket_parsing_exception = TicketParsingException(
        message='Bad ticket',
        status=1,
        debug_info='debug info'
    )
    expected_message = (
        'Service ticket validation has failed: {}. '
        'You should request proper ticket from TVM service.'
    ).format(ticket_parsing_exception.message)
    tvm_check_service_ticket_mock.side_effect = ticket_parsing_exception
    medium_xmlrpc_tvm_server.yb_extra_headers[TVM2_SERVICE_TICKET_HEADER] = 'xxx'
    with pytest.raises(xmlrpclib.Fault) as exception_info:
        medium_xmlrpc_tvm_server.SomeMethod()
    assert expected_message in exception_info.value.faultString


@pytest.mark.parametrize(
    ['create_acl_objects', 'tvm_app_params', 'accessible_method_name', 'query_method_name'],
    [
        pytest.param(False, {}, 'SomeMethod', 'SomeMethod', id='missing from t_tvm_acl_app'),
        pytest.param(True, {}, 'SomeMethod', 'ForbiddenMethod', id='forbidden method name'),
        pytest.param(True, {'env': 'prod'}, 'SomeMethod', 'SomeMethod', id='wrong app env'),
        pytest.param(True, {'hidden': 1}, 'SomeMethod', 'SomeMethod', id='hidden app'),
    ]
)
def test_method_is_not_allowed(
        session, invoker, tvm_check_service_ticket_mock,
        create_acl_objects, tvm_app_params,
        accessible_method_name, query_method_name
):
    tvm_id = ob.generate_int(5)
    ticket = ServiceTicket(tvm_id)
    tvm_check_service_ticket_mock.return_value = ticket
    environ = {'HTTP_X_YA_SERVICE_TICKET': 'xxx'}
    method_ns = 'Balance'
    if create_acl_objects:
        ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id, **tvm_app_params)
        ob.TVMACLPermissionBuilder.construct(
            session, tvm_id=tvm_id,
            endpoint=MediumXmlRpcTVMInvoker.endpoint,
            method_name=accessible_method_name
        )
    with pytest.raises(XMLRPCInvalidParam) as exception_info:
        invoker.check_auth(method_ns, query_method_name, environ)

    expected_message = (
        ': Method {}.{} call is forbidden for {} TVM application. '
        'You should request access from Yandex.Balance team.'
    ).format(method_ns, query_method_name, tvm_id)
    assert exception_info.value.msg == expected_message


def test_happy_path(
        session, invoker, tvm_check_service_ticket_mock
):
    tvm_id = ob.generate_int(5)
    service_id = ob.ServiceBuilder.construct(session).id
    ticket = ServiceTicket(tvm_id)
    tvm_check_service_ticket_mock.return_value = ticket
    environ = {'HTTP_X_YA_SERVICE_TICKET': 'xxx'}
    method_ns = 'Balance'
    method_name = 'SomeMethod'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id,
        endpoint=MediumXmlRpcTVMInvoker.endpoint,
        method_name=method_name
    )
    ob.TVMACLAllowedServiceBuilder.construct(session, tvm_id=tvm_id, service_id=service_id)
    assert [service_id] == invoker.check_auth(method_ns, method_name, environ)


def test_happy_path_with_real_server(
        session, medium_xmlrpc_tvm_server, tvm_check_service_ticket_mock
):
    tvm_id = ob.generate_int(5)
    service_id = ob.ServiceBuilder.construct(session).id
    ticket = ServiceTicket(tvm_id)
    tvm_check_service_ticket_mock.return_value = ticket
    # Метод здесь выбран случайно, можно использовать любой.
    method_name = 'GetCurrencyProducts'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id,
        endpoint=MediumXmlRpcTVMInvoker.endpoint,
        method_name=method_name
    )
    medium_xmlrpc_tvm_server.yb_extra_headers[TVM2_SERVICE_TICKET_HEADER] = 'xxx'

    assert getattr(medium_xmlrpc_tvm_server, method_name)(service_id) == {}


def test_no_need_ticket_for_ping(medium_xmlrpc_tvm_server):
    assert medium_xmlrpc_tvm_server.Ping()
