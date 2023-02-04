from decimal import Decimal

import pytest

from bcl.banks.registry import Payoneer
from bcl.core.models import states, Service


@pytest.fixture
def synchronize_bundle(build_payment_bundle, response_mock):
    """Создаёт пакет и запускает для него синхронизацию статуса."""

    def synchronize_bundle_(*, status, body):
        bundle = build_payment_bundle(Payoneer, payment_dicts=[{'t_acc': 'two'}], service=True)

        bundle.account.remote_id = 'market-123'

        syncer = Payoneer.payment_synchronizer(bundle)

        with response_mock(
            f'GET https://api.sandbox.payoneer.com/v2/programs/123/payouts/{bundle.payments[0].number} '
            f'-> {status} :{body}'
        ):
            result = syncer.run()

        bundle.refresh_from_db()

        return bundle, result

    return synchronize_bundle_


def test_payment_creator(build_payment_bundle):

    compiled = build_payment_bundle(
        Payoneer,
        payment_dicts=[{'f_acc': 'one', 't_acc': 'two', 'ground': 'latin'}],
        account={'number': 'one'},
        service=True,
    ).tst_compiled

    assert compiled == {
        'amount': '152.00',
        'client_reference_id': '1',
        'payee_id': 'two',
        'group_id': 'one',
        'currency': 'RUB',
        'description': 'latin',
    }


def test_get_bundle_contents(build_payment_bundle):
    associate = Payoneer
    bundle = build_payment_bundle(associate, service=Service.TOLOKA, h2h=True)
    contents = associate.payment_dispatcher.get_creator(bundle).get_bundle_contents()
    assert isinstance(contents, dict)


def test_sender_probe(get_assoc_acc_curr, response_mock):

    _, account, _ = get_assoc_acc_curr(Payoneer, account='123')

    account.remote_id = 'market-123'

    sender = Payoneer.payment_sender

    def do_probe(*, status, body):

        with response_mock(
            'GET https://api.sandbox.payoneer.com/v2/programs/123/payees/dummy/status '
            f'-> {status} :{body}'
        ):
            response = sender.probe(
                transaction_id='xxx',
                purpose='xxx',
                account=account,
                recipient='dummy',
                currency_id=0,
                amount=0,
                params='',
                recipient_account_type='',
            )
            return response

    # Пользователь активен в программе.
    result = do_probe(
        status=200, body='{"audit_id":49946162,"code":0,"description":"Success","status":"ACTIVE"}')

    assert result.status_bcl == states.EXPORTED_H2H
    assert result.status_remote == 0
    assert '49946162' in result.status_remote_hint

    # Пользователь неактивен.
    result = do_probe(
        status=200, body='{"audit_id":49946163,"code":0,"description":"Success","status":"INACTIVE"}')

    assert result.status_bcl == states.ERROR
    assert result.status_remote == 0
    assert '49946163' in result.status_remote_hint


def test_callback_handler(mocker, build_payment_bundle):

    associate = Payoneer

    bundle = build_payment_bundle(associate, service=Service.TOLOKA, h2h=True)
    payment = bundle.payments[0]
    payment.t_acc = 'dummy'
    payment.status = states.PROCESSING
    payment.save()

    handler = associate.callback_handler(request=None, realm='toloka')
    patched = mocker.patch.object(handler, 'get_request')
    patched.return_value = {
        'PaymentId': '1020',
        'CancelPayment': 'true',
        'apuid': 'dummy',
        'IntPaymentId': str(payment.number),
    }

    result, status = handler.run()
    assert bundle.status == states.FOR_DELIVERY
    assert result == 'fine'
    assert status == 200

    bundle.refresh_from_db()
    payment.refresh_from_db()

    assert payment.status == states.ERROR
    assert payment.remote_responses[0]['PaymentId'] == '1020'
    assert bundle.status == states.ERROR
    assert bundle.remote_responses[0]['PaymentId'] == '1020'


def test_synchronizer(synchronize_bundle):

    # Сначала негатив.

    bundle, result = synchronize_bundle(
        status=400,
        body=('{"audit_id":49946162,"code":10311,'
              '"description":"Cannot find payout with the specified Client Reference ID"}')
    )
    assert result == states.ERROR

    # Далее позитив.

    bundle, result = synchronize_bundle(
        status=200,
        body=('{"audit_id":49970387,"code":0,"description":"Success","payout_id":"9327357",'
              '"status":"Funded","status_category":"Transferred","payee_id":"tester","'
              'payout_date":"2018-11-08T03:24:28.36","amount":20.00,"currency":"USD",'
              '"load_date":"2018-11-08T03:28:27.69"}')
    )

    assert result == bundle.status == bundle.payments[0].status == states.EXPORTED_H2H

    # Временная недоступность.
    bundle, result = synchronize_bundle(status=404, body='{}')
    assert result == bundle.status == bundle.payments[0].status == states.PROCESSING

    # Постоянная недоступность.
    bundle, result = synchronize_bundle(
        status=404,
        body=('{"audit_id": 70567422, "code": 10311, '
              '"description": "Cannot find payout with the specified Client Reference ID"}')
    )
    assert result == bundle.status == bundle.payments[0].status == states.ERROR

    # Позитив отклонения.

    bundle, result = synchronize_bundle(
        status=200,
        body=('{"audit_id":49970387,"code":0,"description":"Success",'
              '"payout_id":"9327357","status":"Cancelled","status_category":"Cancelled",'
              '"payee_id":"tester","payout_date":"2019-06-17T09:48:07.693",'
              '"amount":21.00,"currency":"USD"}')
    )

    assert result == bundle.status == bundle.payments[0].status == states.ERROR

    # повторная отправка платежа
    bundle, result = synchronize_bundle(
        status=400,
        body=('{"audit_id": 70567532, "code": 10304, '
              '"description": "Payout already exists"}')
    )

    assert result == bundle.status == bundle.payments[0].status == states.PROCESSING


def test_sender(build_payment_bundle, response_mock):

    bundle = build_payment_bundle(Payoneer, payment_dicts=[{'t_acc': 'two'}], service=True)

    bundle.account.remote_id = 'market-123'

    sender = Payoneer.payment_sender(bundle)

    with response_mock(
        'POST https://api.sandbox.payoneer.com/v2/programs/123/payouts -> 400 :'
        '{"audit_id":49954507,"code":10005,"description":"Payee was not found",'
        '"hint":"Please ensure that the payee has registered with Payoneer"}'
    ):
        status = sender.send(bundle.tst_compiled)

    bundle.refresh_from_db()

    assert status == bundle.status == bundle.payments[0].status == states.ERROR
    assert len(bundle.remote_responses) == 1
    assert bundle.payments[0].remote_responses

    # Далее позитив.

    bundle = build_payment_bundle(
        Payoneer, payment_dicts=[
            {'t_acc': 'tester', 'currency_id': 840, 'summ': Decimal('20.0')}], service=True)

    bundle.account.remote_id = 'market-123'

    sender = Payoneer.payment_sender(bundle)

    with response_mock(
        'POST https://api.sandbox.payoneer.com/v2/programs/123/payouts -> 200 :'
        '{"audit_id":49969015,"code":0,"description":"Success","payout_id":"9327357","amount":20.00,"currency":"USD"}'
    ):

        status = sender.send(bundle.tst_compiled)

    bundle.refresh_from_db()

    assert bundle.remote_id == '9327357'
    assert status == states.PROCESSING


def test_balance_getter(get_assoc_acc_curr, response_mock):

    _, acc, _ = get_assoc_acc_curr(Payoneer)

    acc.remote_id = 'market-123'

    getter = Payoneer.balance_getter(accounts=[acc])

    with response_mock(
        'GET https://api.sandbox.payoneer.com/v2/programs/123/balance -> 200 :'
        '{"audit_id":49952426,"code":0,"description":"Success","balance":10200.00,"currency":"USD","fees_due":0.00}'
    ):
        balance = getter.run(account=acc)

    assert str(balance[0]) == '10200.0'


def test_statement_parser(fake_statements_quickcheck):
    fake_statements_quickcheck(associate=Payoneer)
