# -*- encoding: utf-8 -*-
import json
from datetime import timedelta
from contextlib2 import nullcontext as does_not_raise

from yb_darkspirit import scheme
from utils import insert_into_oebs_data
from yb_darkspirit.interface import CashRegister, get_whitespirit_api, get_whitespirit_upload_api
from hamcrest import assert_that, equal_to
from json import dumps
import pytest

from tests import ws_responses
from yb_darkspirit.rest_client import Files
from yb_darkspirit.task.cron_scripts.exceptions import TimeoutException


def test_fs_model_change(cr_wrapper, session):
    """
    Проверяет, что модель ФН изменяется после того, как
    мы начали получать её из OEBS, а не заполнять по умолчанию
    """
    code = scheme.FISCAL_STORAGE_TYPE_CODE_FN_1_1_PR_15_2
    assert cr_wrapper.fiscal_storage.type.code != code
    cr_wrapper.metadata.fs_sn_to_type_code[ws_responses.FS_SN] = code
    assert cr_wrapper.fiscal_storage.type.code == code


def test_get_cr_inventary_number(session):
    serial_number = '54321'
    expected_inventary_number = 12345
    cash_register = scheme.CashRegister(serial_number=serial_number)

    insert_into_oebs_data(session, expected_inventary_number, serial_number)
    session.add(cash_register)
    session.flush()

    real_inventary_number = CashRegister.from_mapper(cash_register).cr_inventary_number
    assert_that(real_inventary_number, equal_to(expected_inventary_number))


@pytest.mark.parametrize('use_password,status,expected_response', [
    (True, 200, True),
    (False, 200, True),
    (True, 500, False),
    (False, 500, False),
])
def test_ssh_ping(cr_wrapper, ws_mocks, use_password, status, expected_response):
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        body=dumps(ws_responses.CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD)
    )
    ws_mocks.cashmachines_ssh_ping(
        cr_wrapper.long_serial_number,
        use_password=use_password,
        body='{"response": "pong"}' if status == 200 else 'An error',
        status=status
    )

    response = cr_wrapper.is_ssh_enabled(use_password=use_password)

    assert_that(response, equal_to(expected_response))


def test_load_password_from_cashregister(cr_wrapper, ws_mocks):
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        body=dumps(ws_responses.CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD)
    )
    ws_mocks.cashmachines_get_password(
        cr_wrapper.long_serial_number,
        use_password=False,
        json={"Password": 666666}
    )

    response = cr_wrapper.load_password_from_cashregister()

    assert_that(response, equal_to(666666))


def test_setup_ssh_connection(cr_wrapper, ws_mocks):
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        body=dumps(ws_responses.CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD)
    )
    ws_mocks.cashmachines_setup_ssh_connection(
        cr_wrapper.long_serial_number,
    )

    cr_wrapper.setup_ssh_connection()


def test_get_whitespirit_api_with_non_dict_response(ws_mocks):
    ws_mocks.uploads(body=dumps(ws_responses.WS_UPLOADS))
    resp = get_whitespirit_api(ws_mocks.url, empty_response_to_dict=False).uploads.get()
    assert sorted(ws_responses.WS_UPLOADS) == sorted(resp)


def test_get_whitespirit_api_with_non_dict_empty_response(ws_mocks):
    ws_mocks.uploads(body=dumps([]))
    assert [] == get_whitespirit_api(ws_mocks.url, empty_response_to_dict=False).uploads.get()


def test_get_whitespirit_upload_api(ws_mocks):
    ws_mocks.post_upload(body=dumps(ws_responses.WS_POST_UPLOAD))
    files = Files(file=(ws_responses.WS_POST_UPLOAD_FILE_NAME, ws_responses.WS_POST_UPLOAD_FILE_CONTENT))
    resp = get_whitespirit_upload_api(ws_mocks.url).upload.post(files)
    for k, v in ws_responses.WS_POST_UPLOAD.items():
        assert k in resp and resp[k] == v


def test_api_uses_tvm_header(rsps, ws_url):
    def request_callback(request):
        ret = 'ok' if 'X-Ya-Service-Ticket' in request.headers else 'fail'
        return 200, {}, json.dumps({'ticket': ret})

    rsps.add_callback(rsps.GET, ws_url + '/v1/test', callback=request_callback, content_type='application/json')
    resp = get_whitespirit_api(ws_url).test.get()
    assert resp['ticket'] == 'ok'


@pytest.mark.parametrize(
    'timeout, raises',
    [
        (None, does_not_raise()), (timedelta(hours=1), does_not_raise()),
        (timedelta(hours=-1), pytest.raises(TimeoutException)),
    ]
)
def test_pull_missing_documents_timeout_raise(
        session, cr_wrapper, pull_document, get_missing_document_numbers, timeout, raises
):
    timeout_deadline = session.now() + timeout if timeout else None
    get_missing_document_numbers.return_value = [10, 22]
    cr_wrapper.cash_register.whitespirit
    with raises:
        cr_wrapper.pull_missing_documents(timeout_deadline=timeout_deadline)
