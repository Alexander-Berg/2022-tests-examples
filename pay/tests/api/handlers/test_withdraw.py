import json
from collections import namedtuple

from hamcrest import assert_that, is_, has_entries

from yb_darkspirit.interactions.fnsreg import WithdrawReason

ResponseClass = namedtuple('Response', ('status_code', 'content',))


class WithdrawStatusResponse(object):
    def __init__(self, status_code, body):
        self.status_code = status_code
        self.body = body

    def json(self):
        return self.body


def test_create_application(
        cr_wrapper_ready_for_reregistration,
        test_client,
        fnsreg_client
):
    cr_wrapper = cr_wrapper_ready_for_reregistration
    cr_sn = cr_wrapper.serial_number

    fnsreg_client.generate_withdraw_application.return_value = ResponseClass(
        status_code=200,
        content='It looks like a valid application XML'
    )

    data = {
        'cr_serial_number': cr_sn,
        'reason': WithdrawReason.KKT_STOLEN.name,
        'use_source_kkt_sn': True
    }
    response = test_client.post(
        '/v1/withdraw/create-application',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'application': 'It looks like a valid application XML',
            'cr_id': cr_wrapper.cash_register_id,
            'cr_serial_number': cr_sn,
            'fn_serial_number': cr_wrapper.fiscal_storage.serial_number
        })
    )


def test_upload_application(
        test_client,
        fnsreg_client,
        documents_client
):
    application_xml = 'It looks like a valid application XML'
    withdraw_uuid = '4242-test'

    fnsreg_client.upload_withdraw_application.return_value = ResponseClass(status_code=200, content=None)
    documents_client.upload_withdraw_application.return_value = {'url': 's3://url'}

    data = {
        'application': application_xml,
        'withdraw_uuid': withdraw_uuid,
        'cr_serial_number': '001',
        'fn_serial_number': '002'
    }
    response = test_client.post(
        '/v1/withdraw/upload-application',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            's3_url': 's3://url'
        })
    )

    fnsreg_client.upload_withdraw_application.assert_called_with(application_xml,
                                                                 withdraw_uuid=withdraw_uuid)

    _, kwargs = documents_client.upload_withdraw_application.call_args
    assert kwargs['cr_sn'] == '001'
    assert kwargs['fn_sn'] == '002'
    assert kwargs['doc'].read() == application_xml


def test_check_status_rejected_withdraw(
        test_client,
        fnsreg_client
):
    withdraw_uuid = '4242-test'

    fnsreg_client.get_withdraw_status.return_value = WithdrawStatusResponse(
        status_code=200,
        body={'status': 'rejected'}
    )

    data = {
        'withdraw_uuid': withdraw_uuid,
        'cr_serial_number': '001',
        'fn_serial_number': '002'
    }
    response = test_client.post(
        '/v1/withdraw/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'rejected'
        })
    )


def test_check_status_in_process_withdraw(
        test_client,
        fnsreg_client
):
    withdraw_uuid = '4242-test'

    fnsreg_client.get_withdraw_status.return_value = WithdrawStatusResponse(
        status_code=200,
        body={'status': 'in_process'}
    )

    data = {
        'withdraw_uuid': withdraw_uuid,
        'cr_serial_number': '001',
        'fn_serial_number': '002'
    }
    response = test_client.post(
        '/v1/withdraw/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'in_process'
        })
    )


def test_check_status_success_withdraw(
        test_client,
        fnsreg_client,
        documents_client
):
    withdraw_uuid = '4242-test'

    fnsreg_client.get_withdraw_status.return_value = WithdrawStatusResponse(
        status_code=200,
        body={
            'status': 'success',
            'documentBase64': 'ZG9j'  # doc
        }
    )

    documents_client.upload_withdraw_card.return_value = {'url': 's3://url'}

    data = {
        'withdraw_uuid': withdraw_uuid,
        'cr_serial_number': '001',
        'fn_serial_number': '002'
    }
    response = test_client.post(
        '/v1/withdraw/check-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'success'
        })
    )

    _, kwargs = documents_client.upload_withdraw_card.call_args
    assert kwargs['cr_sn'] == '001'
    assert kwargs['fn_sn'] == '002'
    assert kwargs['doc'].read() == 'doc'
