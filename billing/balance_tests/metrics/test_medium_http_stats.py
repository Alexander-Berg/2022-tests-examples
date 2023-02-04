# coding: utf-8

from butils.metrics import SolomonStatisticsKeeperMixin
import pytest
import mock
from httplib import BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, INTERNAL_SERVER_ERROR, OK

from medium.medium_http import MediumHttpLogic
from balance import exc
from butils.metrics import STATUS_CODE_TIMEOUT

# igogor: тесты этого модуля намеренно написаны так чтобы падать на этапе парсинга параметров, чтобы быть герметичными


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

def test_httpapi_missing_param(medium_http, stat_method_mock):
    res = medium_http.request("/httpapi/get_payouts_by_purchase_token")
    _check_stat_call(
        stat_method_mock,
        method_name="/httpapi/get_payouts_by_purchase_token",
        # igogor: кидаем пятисотку даже если нет обязательного параметра в вызове
        status_code=INTERNAL_SERVER_ERROR,
    )


def test_httpapitvm_wrong_param_type(medium_http, stat_method_mock, tvm_valid_ticket_mock):
    _, _, headers = tvm_valid_ticket_mock
    res = medium_http.request("/httpapitvm/get_invoice_from_mds?invoice_id=ololo",
                              headers=headers)
    _check_stat_call(
        stat_method_mock,
        method_name="/httpapi/get_invoice_from_mds",
        # igogor: кидаем пятисотку даже если тип параметра неверный.
        # Ошибки декоратора параметров никак не обрабатываются
        status_code=INTERNAL_SERVER_ERROR,
    )


def test_mdsproxy_dash_to_underscore(medium_http, stat_method_mock, tvm_invalid_ticket_mock):
    _, _, headers = tvm_invalid_ticket_mock
    res = medium_http.request("/mdsproxy/get-invoice-from-mds?invoice_id=1234567",
                              headers=headers)
    _check_stat_call(
        stat_method_mock,
        method_name="/mdsproxy/get_invoice_from_mds",
        status_code=UNAUTHORIZED,
    )


def test_nirvana_not_path_info(medium_http, stat_method_mock, tvm_valid_ticket_mock):
    ticket, service_ticket, headers = tvm_valid_ticket_mock
    res = medium_http.request("/nirvana/v2/call/some_operation/some_instance_id", http_method="GET", headers=headers)

    _check_stat_call(
        stat_method_mock,
        method_name="nirvana.get_operation",
        status_code=BAD_REQUEST
    )


def test_idm(medium_http, stat_method_mock, tvm_valid_ticket_mock):
    _, _, headers = tvm_valid_ticket_mock
    res = medium_http.request("/idm/add-role", http_method="POST", headers=headers)

    _check_stat_call(
        stat_method_mock,
        method_name="/idm/add_role",
        status_code=OK,  # igogor: управлятор всегда возвращает 200 с ошибкой в теле ответа.
    )


def test_takeout(takeout_http, stat_method_mock, tvm_valid_ticket_mock):
    _, _, headers = tvm_valid_ticket_mock
    res = takeout_http.request("/takeout", headers=headers)

    _check_stat_call(
        stat_method_mock,
        method_name="/takeout",
        # igogor: кидаем пятисотку даже если нет обязательного параметра в вызове
        status_code=INTERNAL_SERVER_ERROR,
    )


def test_timeout(medium_http, stat_method_mock):
    with mock.patch.object(
            MediumHttpLogic, MediumHttpLogic.get_payouts_by_purchase_token.__name__, autospec=True,
            side_effect=exc.TIMEOUT_ERROR(method_name="/httpapi/get_payouts_by_purchase_token",
                                              timeout=600),
    ):

        res = medium_http.request("/httpapi/get_payouts_by_purchase_token?purchase_token=some_token")

        _check_stat_call(
            stat_method_mock,
            method_name="/httpapi/get_payouts_by_purchase_token",
            # igogor: для таймаута особый статус
            status_code=STATUS_CODE_TIMEOUT,
        )
