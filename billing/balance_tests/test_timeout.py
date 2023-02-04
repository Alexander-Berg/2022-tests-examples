# coding: utf-8
from future import standard_library

from io import BytesIO
import sys
import xmlrpc.client as xmlrpclib
import http
import time
import contextlib

from werkzeug.test import EnvironBuilder, Client
import pytest
from pytest import param as pp
import mock
import hamcrest as hm

from medium import medium_logic
from medium.medium_http import TVMMediumHttpLogic, MediumHttpLogic
from medium.medium_nirvana import NirvanaLogic
from medium.upravlyator.logic import Upravlyator
from butils.invoker import BaseInvoker, TimeoutMixin
from butils.exc import TIMEOUT_ERROR

"""
В юниттестах нельзя проверить работу timeout_decorator на моке сокета, т.к. при моке сокета код инвокера запускается
не в основном потоке. timeout_decorator на сигналах работает через обработчик системного сигнала, который можно
задать только в основном потоке. Поэтому эти тесты делаем без мока сокета.
"""

TIMEOUT_VALUE = 0.1
TIMEOUT_ERROR_NAME = TIMEOUT_ERROR.__name__
TIMEOUT_REASON = "500 %s" % TimeoutMixin.HTTP_REASON


@pytest.fixture
def medium_client(medium_instance):
    port, dispatcher = medium_instance
    with mock.patch.object(
            BaseInvoker,
            BaseInvoker.get_timeout.__name__,
            autospec=True,
    ) as get_timeout_mock:
        get_timeout_mock.return_value = TIMEOUT_VALUE
        yield Client(dispatcher)


def long_call_xmlrpc(*args, **kwargs):
    time.sleep(30)
    return (0, "SUCCESS", "Long method finished. It shouldn't have.")


def long_call_http(*args, **kwargs):
    time.sleep(30)
    return "Long method finished. It shouldn't have."


@contextlib.contextmanager
def check_call_span():
    start_ts = time.time()
    yield
    call_span = time.time() - start_ts
    # igogor: тут добавляем небольшую константу, т.к. таймаут накладывается на вызов метода логики,
    # а время выполнения мы собираем от вызова всего диспатчера.
    hm.assert_that(call_span, hm.less_than_or_equal_to(TIMEOUT_VALUE + 3),
                   "Call took more than timeout"
                   )


def test_medium_timeout_xmlrpc(medium_client, session):
    with mock.patch.object(
            medium_logic.Logic, medium_logic.Logic.CreateClient.__name__, autospec=True,
            side_effect=long_call_xmlrpc,
    ):
        body = xmlrpclib.dumps((session.oper_id, dict(NAME="Some Name")),
                               methodname="Balance.CreateClient")
        with check_call_span():
            res = medium_client.post(path="/xmlrpc", input_stream=BytesIO(body))
        with pytest.raises(xmlrpclib.Fault) as excinfo:
            xmlrpclib.loads("".join(res[0]))

        hm.assert_that(excinfo.value.faultString, hm.contains_string(TIMEOUT_ERROR_NAME))


def test_medium_timeout_xmlrpctvm(medium_client, session, tvm_valid_ticket_mock):
    with mock.patch.object(
            medium_logic.Logic, medium_logic.Logic.CreateClient.__name__, autospec=True,
            side_effect=long_call_xmlrpc,
    ):
        ticket, service_ticket, headers = tvm_valid_ticket_mock

        body = xmlrpclib.dumps((session.oper_id, dict(NAME="Some Name")),
                               methodname="Balance.CreateClient")
        with check_call_span():
            res = medium_client.post(path="/xmlrpctv", input_stream=BytesIO(body), headers=headers)
        with pytest.raises(xmlrpclib.Fault) as excinfo:
            xmlrpclib.loads("".join(res[0]))
        hm.assert_that(excinfo.value.faultString, hm.contains_string(TIMEOUT_ERROR_NAME))


def test_medium_timeout_httpapi(medium_client):
    with mock.patch.object(
            MediumHttpLogic, MediumHttpLogic.get_payouts_by_purchase_token.__name__, autospec=True,
            side_effect=long_call_http,
    ):
        with check_call_span():
            res = medium_client.get(path="/httpapi/get_payouts_by_purchase_token",
                                    query_string="purchase_token=some_token",
                                    )

        body_iter, reason, headers = res
        hm.assert_that(reason, hm.equal_to(TIMEOUT_REASON))


@pytest.mark.parametrize(
    "builder, logic_mock",
    [
        pp(
            EnvironBuilder(path="/httpapitvm/get_invoice_from_mds",
                           query_string="invoice_id=12345667",
                           ),
            mock.patch.object(TVMMediumHttpLogic, TVMMediumHttpLogic.get_invoice_from_mds.__name__,
                              autospec=True, side_effect=long_call_http),
            id="httpapitvm",
        ),
        pp(
            EnvironBuilder(path="/mdsproxy/get-invoice-from-mds",
                           query_string="invoice_id=1234567",
                           ),
            mock.patch.object(TVMMediumHttpLogic, TVMMediumHttpLogic.get_invoice_from_mds.__name__,
                              autospec=True, side_effect=long_call_http),
            id="mdsproxy",
        ),
        pp(
            EnvironBuilder(path="/nirvana/v2/call/some_operation/some_instance_id",
                           query_string="ticket=1234567",
                           ),
            mock.patch.object(NirvanaLogic, NirvanaLogic.get_operation.__name__,
                              autospec=True, side_effect=long_call_http),
            id="nirvana",
        ),
        pp(
            EnvironBuilder(path="/httpapitvm/get_act_from_mds",
                           query_string="act_id=12345667",
                           ),
            mock.patch.object(TVMMediumHttpLogic, TVMMediumHttpLogic.get_act_from_mds.__name__,
                              autospec=True, side_effect=long_call_http),
            id="httpapitvm",
        ),
    ]
)
def test_medium_timeout(medium_client, tvm_valid_ticket_mock, builder, logic_mock):
    with logic_mock:
        ticket, service_ticket, headers = tvm_valid_ticket_mock
        env = EnvironBuilder(environ_overrides=builder.get_environ(), headers=headers)
        with check_call_span():
            res = medium_client.open(env)

        body_iter, reason, headers = res
        hm.assert_that(reason, hm.equal_to(TIMEOUT_REASON))


def test_medium_timeout_upravlyator(medium_client, tvm_valid_ticket_mock):
    with mock.patch.object(
            Upravlyator, Upravlyator.get_roles.__name__, autospec=True,
            side_effect=long_call_http,
    ):
        ticket, service_ticket, headers = tvm_valid_ticket_mock
        with check_call_span():
            res = medium_client.get(path="/idm/get-roles", headers=headers, )

        body_iter, reason, headers = res
        hm.assert_that(reason, hm.equal_to("200 OK"))
        hm.assert_that("".join(body_iter), hm.contains_string(TIMEOUT_ERROR_NAME))


def test_medium_timeout_valid_call(medium_client, session):
    return_val = (0, 'SUCCESS', 12345656)
    with mock.patch.object(
            medium_logic.Logic, medium_logic.Logic.CreateClient.__name__, autospec=True,
            return_value=return_val
    ):
        body = xmlrpclib.dumps((session.oper_id, dict(NAME="Some Name")),
                               methodname="Balance.CreateClient")
        with check_call_span():
            res = medium_client.post(path="/xmlrpc", input_stream=BytesIO(body))

        response = xmlrpclib.loads("".join(res[0]))
        hm.assert_that(response, hm.has_item(return_val))
