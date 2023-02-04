
from httplib import UNAUTHORIZED, FORBIDDEN
from collections import namedtuple

import pytest

try:
    from tvmauth.exceptions import TicketParsingException
except ImportError:
    from ticket_parser2.api.v1.exceptions import TicketParsingException

from medium.medium_http import NewMediumHttpInvoker, TVMMediumHttpLogic
from balance.constants import TVMToolAliases

from tests import object_builder as ob

ServiceTicket = namedtuple('ServiceTicket', ['src'])


def create_invoker(tvm_alias=None, endpoint=None):
    return NewMediumHttpInvoker(TVMMediumHttpLogic, tvm_alias=tvm_alias, endpoint=endpoint)


def create_environ_with_ticket():
    return {'HTTP_X_YA_SERVICE_TICKET': ob.generate_character_string()}


def test_allowed_without_tvm_alias():
    invoker = create_invoker()
    assert invoker.check_auth(environ={}, method_name='test_method') is None


def test_no_ticket():
    invoker = create_invoker(tvm_alias=TVMToolAliases.YB_MEDIUM)
    response = invoker.check_auth(environ={}, method_name='test_method')
    assert response.reason == 'You must provide TVM2 service ticket'
    assert response.status_code == UNAUTHORIZED


def test_bad_ticket(tvm_check_service_ticket_mock):
    invoker = create_invoker(tvm_alias=TVMToolAliases.YB_MEDIUM)

    exception = TicketParsingException(
        message='Bad ticket',
        status=1,
        debug_info='debug info'
    )
    expected_message = 'Bad TVM2 service ticket: %s' % exception.message
    tvm_check_service_ticket_mock.side_effect = exception

    response = invoker.check_auth(
        environ=create_environ_with_ticket(),
        method_name='test_method'
    )

    assert response.reason == expected_message
    assert response.status_code == UNAUTHORIZED


def test_no_need_permissions_without_endpoint(tvm_check_service_ticket_mock):
    invoker = create_invoker(tvm_alias=TVMToolAliases.YB_MEDIUM)
    tvm_check_service_ticket_mock.return_value = ServiceTicket(src=ob.generate_int(5))
    assert invoker.check_auth(
        environ=create_environ_with_ticket(),
        method_name='test_method'
    ) is None


def test_has_no_permission(tvm_check_service_ticket_mock):
    endpoint = 'httpapi'
    invoker = create_invoker(tvm_alias=TVMToolAliases.YB_MEDIUM, endpoint=endpoint)
    ticket = ServiceTicket(src=ob.generate_int(5))
    tvm_check_service_ticket_mock.return_value = ticket
    method_name = 'test_method'
    response = invoker.check_auth(
        environ=create_environ_with_ticket(),
        method_name=method_name
    )
    expected_message = (
        'Service %s is not allowed to call method %s on endpoint %s'
        % (ticket.src, method_name, endpoint)
    )
    assert response.reason == expected_message
    assert response.status_code == FORBIDDEN


def test_has_permission(session, tvm_check_service_ticket_mock):
    endpoint = 'httpapi'
    tvm_id = ob.generate_int(5)
    invoker = create_invoker(tvm_alias=TVMToolAliases.YB_MEDIUM, endpoint=endpoint)
    ticket = ServiceTicket(src=tvm_id)
    tvm_check_service_ticket_mock.return_value = ticket
    method_name = 'test_method'
    ob.TVMACLAppBuidler.construct(session, tvm_id=tvm_id)
    ob.TVMACLPermissionBuilder.construct(
        session, tvm_id=tvm_id,
        endpoint=endpoint,
        method_name=method_name
    )
    assert invoker.check_auth(
        environ=create_environ_with_ticket(),
        method_name=method_name
    ) is None
