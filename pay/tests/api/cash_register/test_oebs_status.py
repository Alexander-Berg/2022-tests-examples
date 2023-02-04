import json

import requests.exceptions
from hamcrest import assert_that, is_, has_entries

from tests import ws_responses
from yb_darkspirit.interface import WhitespiritError, WhitespiritClientError


def _assert_resp_has_json_part(resp, js):
    assert_that(resp.status_code, is_(200))
    assert_that(json.loads(resp.data), has_entries(js))


def _get_oebs_status_serial_link(serial_number):
    return '/v1/cash-registers/oebs-cash-info/by_serial/{}'.format(serial_number)


def _get_oebs_status_inventory_link(inventory_number):
    return '/v1/cash-registers/oebs-cash-info/by_inventory/{}'.format(inventory_number)


def test_cash_register_oebs_status_no_serial(test_client):
    resp = test_client.get(_get_oebs_status_serial_link('643455434534543'))
    _assert_resp_has_json_part(resp, {'darkspirit': 'serial_not_found'})


def test_cash_register_oebs_status_ws_offline(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    cr_wrapper = cr_wrapper_with_completed_registration
    ws_mocks.cashmachines_ssh_ping(
        cr_wrapper.long_serial_number,
        use_password=True,
        body=requests.exceptions.ConnectionError('some text'),
    )
    resp = test_client.get(_get_oebs_status_serial_link(cr_wrapper.serial_number))
    _assert_resp_has_json_part(resp, {'whitespirit': 'offline'})


def test_cash_register_oebs_status_cash_ping_fail(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    cr_wrapper = cr_wrapper_with_completed_registration
    ws_mocks.cashmachines_ssh_ping(
        cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        use_password=True,
        body=WhitespiritClientError(),
    )
    resp = test_client.get(_get_oebs_status_serial_link(cr_wrapper.serial_number))
    _assert_resp_has_json_part(resp, {'cash_ping': 'fail'})


def test_cash_register_oebs_status_ws_no_cash(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    cr_wrapper = cr_wrapper_with_completed_registration
    ws_mocks.cashmachines_ssh_ping(
        cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        use_password=True,
        body='{"response": "pong"}',
    )
    ws_mocks.cashmachines_status(
        cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        body=WhitespiritClientError(),
    )
    resp = test_client.get(_get_oebs_status_serial_link(cr_wrapper.serial_number))
    _assert_resp_has_json_part(resp, {'cash_by_whitespirit': 'cash_not_found'})


def test_cash_register_oebs_status_cash_offline(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    cr_wrapper = cr_wrapper_with_completed_registration
    ws_mocks.cashmachines_ssh_ping(
        cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        use_password=True,
        body='{"response": "pong"}',
    )
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        json=ws_responses.CASHMACHINES_STATUS_CR_OFFLINE,
        content_type="application/json"
    )
    resp = test_client.get(_get_oebs_status_serial_link(cr_wrapper.serial_number))
    _assert_resp_has_json_part(resp, {'cash_state': 'OFFLINE'})


def test_cash_register_oebs_status_cash_nonconfigured(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    cr_wrapper = cr_wrapper_with_completed_registration
    ws_mocks.cashmachines_ssh_ping(
        cr_long_sn=cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        use_password=True,
        body='{"response": "pong"}',
    )
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.cash_register.get_whitespirit_key(use_serial=False),
        json=ws_responses.CASHMACHINES_STATUS_CR_NONCONFIGURED_FS_READY_FISCAL_GOOD,
        content_type="application/json"
    )
    resp = test_client.get(_get_oebs_status_serial_link(cr_wrapper.serial_number))
    _assert_resp_has_json_part(resp, {'cash_state': 'NONCONFIGURED'})


def test_cash_register_oebs_status_no_inventory_number(test_client):
    resp = test_client.get(_get_oebs_status_inventory_link('4325345234324'))
    _assert_resp_has_json_part(resp, {'darkspirit': 'inventory_not_found'})
