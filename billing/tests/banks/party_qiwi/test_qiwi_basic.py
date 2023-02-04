import json
from datetime import datetime
from time import sleep

import pytest
from requests import ReadTimeout

from bcl.banks.party_qiwi.common import QiwiException, PICKUP_REMOTE_ID
from bcl.banks.party_qiwi.payment_sender import PaymentSender
from bcl.banks.party_qiwi.payment_synchronizer import QiwiPaymentSynchronizer
from bcl.banks.registry import Qiwi
from bcl.core.models import states, Service
from bcl.core.models.enums import Currency
from bcl.exceptions import ScheduledException

ACC_NUM = '17'
ACC_REMOTE_ID = 'toloka'


@pytest.fixture
def get_qiwi_bundle(build_payment_bundle, get_assoc_acc_curr):

    def get_qiwi_bundle_(payment_dicts=None):
        _, acc, _ = get_assoc_acc_curr(Qiwi, account={'number': ACC_NUM, 'remote_id': ACC_REMOTE_ID})

        return build_payment_bundle(Qiwi, payment_dicts=payment_dicts, account=acc, h2h=True)

    return get_qiwi_bundle_


def test_payment_automation(
    get_qiwi_bundle, response_mock, run_task, dss_signing_mock, init_user, django_assert_num_queries):

    use_sandbox = False
    nap_delay = 10

    def mock_call(response_data, sync_func, url='create'):
        with response_mock(
            f'POST https://api-test.contactpay.io/gateway/v1/withdraw/{url} -> 200 : {response_data}',
            bypass=use_sandbox
        ):
            sync_func()

    bundle = get_qiwi_bundle(payment_dicts=[{
        't_acc_type': 5, 'ground': '[mock_server,success]', 'service_id': Service.ZEN
    }])

    # отсутствие доступа
    mock_call(
        '{"data": null,"error_code": 15,"message": "Shop ip <IP> denied","result": false}',
        lambda: run_task('process_bundles')
    )

    bundle.refresh_from_db()
    assert bundle.status == states.ERROR
    assert not bundle.remote_id

    # штатная отправка
    bundle = get_qiwi_bundle(payment_dicts=[{'t_acc_type': 5, 'ground': '[mock_server,success]'}])
    data = bundle.tst_compiled

    assert data['shop_id'] == ACC_NUM
    assert data['payway'] == 'qiwi_topup_rub_to_wallet[mock_server,success]'

    with django_assert_num_queries(12) as _:
        mock_call(
            '{"result": true, "message": "Ok", "error_code": 0, "data": {"id": 1, "balance": 0, '
            '"payee_receive": 0, "ps_currency": 978, "shop_payment_id": "string", "shop_write_off": 0, "status": 2}}',
            lambda: run_task('process_bundles')
        )

    bundle.refresh_from_db()
    assert bundle.status == states.FOR_DELIVERY
    assert bundle.remote_id

    use_sandbox and sleep(nap_delay)

    # синхронизация статусов
    sync = QiwiPaymentSynchronizer(bundle)
    result = {
        'result': True, 'message': 'Ok', 'error_code': 0, 'data': {
            'id': bundle.remote_id, 'status': 2, 'ps_currency': 978, 'shop_currency': 978, 'payee_receive': 0,
            'shop_write_off': 0, 'shop_payment_id': 'b144de15-09c0-4ff7-98ae-03dc01ddb041'
        }
    }
    with django_assert_num_queries(6) as _:
        mock_call(json.dumps(result), sync.run, 'shop_payment_status')

    bundle.refresh_from_db()
    assert bundle.status == states.PROCESSING
    assert bundle.remote_id

    # ошибка доступа
    with pytest.raises(ScheduledException):
        mock_call(
            '{"data": null,"error_code": 15,"message": "Shop ip <IP> denied","result": false}',
            sync.run, 'shop_payment_status'
        )

    use_sandbox and sleep(nap_delay)

    result['data']['status'] = 5

    mock_call(json.dumps(result), sync.run, 'shop_payment_status')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.remote_id

    use_sandbox and sleep(nap_delay)


def test_read_timeout(get_qiwi_bundle, response_mock, run_task, monkeypatch):

    use_sandbox = False

    # эмулируем таймаут чтения
    bundle = get_qiwi_bundle(payment_dicts=[{'t_acc_type': 5, 'ground': 'timeout'}])
    send_back = PaymentSender.send

    def send_mock(self, contents):
        raise ReadTimeout('bogus')

    PaymentSender.send = send_mock
    try:
        run_task('process_bundles')
    finally:
        PaymentSender.send = send_back

    bundle.refresh_from_db()
    assert bundle.status == states.FOR_DELIVERY
    assert bundle.remote_id == PICKUP_REMOTE_ID

    # проверка проставления фиктивного ид и на стадии повторного создания
    bundle.remote_id = ''
    bundle.processing_after_dt = datetime(2022, 1, 1)
    bundle.save()

    with response_mock(
        'POST https://api-test.contactpay.io/gateway/v1/withdraw/create -> 400:'
        '{"data":null,"error_code":6,"message":"Shop_payment_id \'1\' is not unique for shop(id=17)","result":false}',
        bypass=use_sandbox
    ):
        run_task('process_bundles')

    bundle.refresh_from_db()
    assert bundle.status == states.FOR_DELIVERY
    assert bundle.remote_id == PICKUP_REMOTE_ID

    pay = bundle.payments[0]
    assert pay.status == states.PROCESSING
    assert '[6] Передан неуникальный идентификатор операции на стороне магазина' in pay.processing_notes

    # проставление нефиктивного remote_id в ходе синхронизации
    bundle.processing_after_dt = datetime(2022, 1, 1)
    bundle.save()

    with response_mock(
        'POST https://api-test.contactpay.io/gateway/v1/withdraw/shop_payment_status -> 200:'
        '{"result": true, "message": "Ok", "error_code": 0, "data": { "id": "yyy", "status": 5, '
        '"ps_currency": 978, "shop_currency": 978, "payee_receive": 0,"shop_write_off": 0, '
        '"shop_payment_id": "rrr"}}',
        bypass=use_sandbox
    ):
        run_task('process_bundles')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.remote_id == 'yyy'


def test_balance_getter(get_assoc_acc_curr, response_mock, django_assert_num_queries):
    associate = Qiwi

    _, acc, _ = get_assoc_acc_curr(associate=associate, account={'number': ACC_NUM, 'remote_id': ACC_REMOTE_ID, 'currency_code': Currency.EUR})

    getter = Qiwi.balance_getter(accounts=[acc])
    with response_mock(
        f'POST https://api-test.contactpay.io/gateway/v1/shop_balance -> 200 :'
        f'{{"result":true,"message":"Ok","error_code":0,"data":{{"shop_id":"{ACC_NUM}",'
        f'"balances":[{{"currency":{Currency.EUR},"available":100.3,"hold":0,"frozen":10000.1}}]}}}}'
    ):
        with django_assert_num_queries(0) as _:
            balance = getter.run(account=acc)

    assert balance[0] == 100.3


def test_balance_getter_error(get_assoc_acc_curr, response_mock):
    associate = Qiwi

    _, acc, _ = get_assoc_acc_curr(associate=associate, account={'number': ACC_NUM, 'remote_id': ACC_REMOTE_ID, 'currency_code': Currency.EUR})

    getter = Qiwi.balance_getter(accounts=[acc])
    res = {
        'data': None, 'error_code': 10,
        'message': 'Incorrect values for parameters: shop_id="value is not a valid integer"', 'result': False
    }
    with response_mock(
        f'POST https://api-test.contactpay.io/gateway/v1/shop_balance -> 200 : {json.dumps(res)}'
    ):
        with pytest.raises(QiwiException):
            getter.run(account=acc)
