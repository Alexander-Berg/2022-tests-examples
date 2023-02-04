# coding: utf-8

from future import standard_library
import http.client as http
import xmlrpclib
import mock
import pytest
import hamcrest as hm
import xml.etree.ElementTree as et
from werkzeug import exceptions as we

from butils.metrics import SolomonStatisticsKeeperMixin, STATUS_CODE_TIMEOUT
from balance import exc

from balance.core import Core


@pytest.fixture
def stat_method_mock():
    with mock.patch.object(SolomonStatisticsKeeperMixin,
                           SolomonStatisticsKeeperMixin.save_request_stats.__name__,
                           autospec=True) as method_mock:
        yield method_mock


def _check_stat_call(method_mock, method_name, status_code):
    method_mock.assert_called_once_with(
        self=mock.ANY,
        method_name=method_name,
        start_ts=mock.ANY,
        end_ts=mock.ANY,
        status_code=status_code,
        response=mock.ANY,
        environ=mock.ANY
    )


@pytest.mark.parametrize(
    "callback",
    [
        pytest.param(
            lambda x, s: x.CreateClient(s.oper_id),
            id="TypeError-400"
        ),
        pytest.param(
            lambda x, s: x.CreateClient("not_number", dict(NAME="Metrics unittester")),
            id="INVALID_PARAM-400"
        ),
    ])
def test_status_code_invalid_params(medium_xmlrpc, session, stat_method_mock, callback):
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        callback(medium_xmlrpc, session)

    _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=http.BAD_REQUEST)


@pytest.mark.parametrize(
    "exception_type,status_code",
    [
        pytest.param(
            exception_type, status_code, id="%s-%d" % (exception_type.__name__, status_code)
        ) for exception_type, status_code in [
        (exc.NOT_FOUND, http.NOT_FOUND),  # есть в маппинге ошибка - статус код
        (exc.PASSPORT_NOT_FOUND, http.NOT_FOUND),  # парент есть в маппинге
        (exc.INVALID_PARAM, http.BAD_REQUEST),  # есть в маппинге
        (exc.NO_PAYMENTS_FOR_REQUEST, http.BAD_REQUEST),  # парент есть в маппинге
        (exc.ORDERS_NOT_SYNCHRONIZED, http.BAD_REQUEST),  # есть в маппинге
        (exc.CLIENTS_NOT_MATCH, http.BAD_REQUEST),  # парент не выводящийся в сообщении есть в маппинге
        (exc.PASSPORT_API_FORBIDDEN, http.FORBIDDEN), # парент не выводящийся в сообщении есть в маппинге
        (exc.CLIENT_NOT_FOUND, http.NOT_FOUND),  # парент есть в маппинге
        (we.NotFound, http.NOT_FOUND),
        (we.Forbidden, http.FORBIDDEN),
        (exc.EXCEPTION, http.INTERNAL_SERVER_ERROR),  # нет в маппинге
        (RuntimeError, http.INTERNAL_SERVER_ERROR),
        (ImportError, http.INTERNAL_SERVER_ERROR),
        (ValueError, http.INTERNAL_SERVER_ERROR),
        (AttributeError, http.INTERNAL_SERVER_ERROR),
        (Exception, http.INTERNAL_SERVER_ERROR),
    ]
    ])
def test_status_code_error_type(medium_xmlrpc, session, stat_method_mock, exception_type, status_code):
    with mock.patch.object(Core, Core.create_or_update_client.__name__, autospec=True) as core_method_mock:
        core_method_mock.side_effect = exception_type("Dummy exception")

        with pytest.raises(xmlrpclib.Fault) as excinfo:
            medium_xmlrpc.CreateClient(session.oper_id, dict(NAME="Metrics unittester"))

        _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=status_code)


@pytest.mark.parametrize(
    "exception,status_code",
    [
        pytest.param(TypeError("CreateClient() takes exactly 1 argument (2 given)"), http.BAD_REQUEST,
            id="400_on_missed_api_param"
        ),
        pytest.param(TypeError("not_logic_method() takes exactly 1 argument (2 given)"), http.INTERNAL_SERVER_ERROR,
            id="500_on_missed_internal_call_param"
        ),
        pytest.param(exc.TRUST_API_EXCEPTION("Can't bind more than 5 cards to one user."), http.BAD_REQUEST,
                     id="400_on_binding_6th_card"),
        pytest.param(exc.TRUST_API_EXCEPTION("User 000000 has too many active bindings (5)"), http.BAD_REQUEST,
                     id="400_having_too_many_bindings"),
        pytest.param(NotImplementedError("Person filtering allowed only by ID field"), http.BAD_REQUEST,
                     id="400_on_wrong_person_filtering_field"
        ),
    ])
def test_status_code_error_message(medium_xmlrpc, session, stat_method_mock, exception, status_code):
    with mock.patch.object(Core, Core.create_or_update_client.__name__, autospec=True) as core_method_mock:
        core_method_mock.side_effect = exception

        with pytest.raises(xmlrpclib.Fault) as excinfo:
            medium_xmlrpc.CreateClient(session.oper_id, dict(NAME="Metrics unittester"))

        _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=status_code)


def test_status_code_invalid_method(medium_xmlrpc, session, stat_method_mock):
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        res = medium_xmlrpc.NoSuchMethodInLogick()

    # igogor: все попытки вызвать отсутствующий метод кладем в одну кучу в соломоне
    _check_stat_call(stat_method_mock, method_name="Balance.InvalidMethod", status_code=http.NOT_FOUND)


def test_status_code_valid_call(medium_xmlrpc, session, stat_method_mock):
    res = medium_xmlrpc.CreateClient(session.oper_id, dict(NAME="Metrics Tester"))

    _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=http.OK)


def test_tvm_no_ticket(medium_xmlrpc_tvm_server, session, stat_method_mock):
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        res = medium_xmlrpc_tvm_server.CreateClient(session.oper_id, dict(NAME="Metrics Tester"))

    _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=http.BAD_REQUEST)


def test_tvm_invalid_ticket(medium_xmlrpc_tvm_server, session, tvm_invalid_ticket_mock, stat_method_mock):
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        res = medium_xmlrpc_tvm_server.CreateClient(session.oper_id, dict(NAME="Metrics Tester"))

    _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=http.UNAUTHORIZED)


def test_tvm_no_permission(medium_xmlrpc_tvm_server, session, tvm_no_permission_mock, stat_method_mock):
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        res = medium_xmlrpc_tvm_server.CreateClient(session.oper_id, dict(NAME="Metrics Tester"))

    _check_stat_call(stat_method_mock, method_name="Balance.CreateClient", status_code=http.UNAUTHORIZED)


def test_metrics_error_dont_break_application(medium_xmlrpc_tvm_server, session, tvm_valid_ticket_mock,
                                              stat_method_mock):
    stat_method_mock.side_effect = RuntimeError("Something went very wrong")
    res = medium_xmlrpc_tvm_server.CreateClient(session.oper_id, dict(NAME="Metrics Tester"))

    hm.assert_that(res, hm.has_item("SUCCESS"))


def test_timeout(medium_xmlrpc, session, stat_method_mock):
    with mock.patch.object(Core, Core.create_or_update_client.__name__, autospec=True,
                           side_effect=exc.TIMEOUT_ERROR(method_name="Balance.CreateClient",
                                                         timeout=600),
                           ):
        with pytest.raises(xmlrpclib.Fault) as excinfo:
            medium_xmlrpc.CreateClient(session.oper_id, dict(NAME="Metrics unittester"))

        _check_stat_call(stat_method_mock, method_name="Balance.CreateClient",
                         # igogor: для таймаута особый статус
                         status_code=STATUS_CODE_TIMEOUT)
