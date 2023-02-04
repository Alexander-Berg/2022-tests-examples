from decimal import Decimal

from bcl.banks.registry import PingPong
from bcl.core.models import states, Service, Currency


def test_payment_creator(build_payment_bundle):

    compiled = build_payment_bundle(
        PingPong,
        payment_dicts=[{'f_acc': 'one', 't_acc': 'two', 'ground': 'latin'}],
        account={'number': 'one'},
        service=True,
    ).tst_compiled

    assert compiled == {'currency': 'RUB', 'seller_id': 'two', 'amount': '152.00', 'payment_id': 1}


def test_get_bundle_contents(build_payment_bundle):
    associate = PingPong
    bundle = build_payment_bundle(associate, service=Service.MARKET, h2h=True)
    contents = associate.payment_dispatcher.get_creator(bundle).get_bundle_contents()
    assert isinstance(contents, dict)


def test_sender_probe(get_assoc_acc_curr, response_mock):

    associate = PingPong
    _, account, _ = get_assoc_acc_curr(associate, account='123')

    account.remote_id = 'market'

    sender = associate.payment_sender

    def do_probe(*, status, body):

        with response_mock(f'POST https://test-ppapi.pingpongx.com/account/status -> {status} :{body}'):
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

    # Пользователь активен.
    result = do_probe(
        status=200,
        body='{"apiName": "/account/status","code": "0000","message": '
             '"SUCCESS","data": {"seller_id": "xxx","status": "Approved"}}')

    assert result.status_bcl == states.EXPORTED_H2H
    assert result.status_remote == 'Approved'
    assert 'Approved' in result.status_remote_hint

    # Пользователь неактивен.
    result = do_probe(
        status=200,
        body='{"apiName": "/account/status","code": "0000","message": '
             '"SUCCESS","data": {"seller_id": "xxx","status": "Pending"}}')

    assert result.status_bcl == states.ERROR
    assert result.status_remote == 'Pending'
    assert 'Pending' in result.status_remote_hint


def test_synchronizer(build_payment_bundle, response_mock):
    associate = PingPong

    bundle = build_payment_bundle(
        associate, payment_dicts=[{'f_acc': 'one', 't_acc': 'two'}], service=True, account={'number': 'one'}
    )

    bundle.account.remote_id = 'market'

    syncer = associate.payment_synchronizer(bundle)

    with response_mock(
        'POST https://test-ppapi.pingpongx.com/payment/status -> 400 :'
        '{"apiName": "/payment/status","code": "0000","message": "SUCCESS","data": '
        '{"seller_id": "xxxxxxx","payment_id": "xxxxxxx","status": "Failure"}}'
    ):
        result = syncer.run()

    assert result == states.ERROR


def test_synchronizer_positive(build_payment_bundle, response_mock):

    associate = PingPong

    bundle = build_payment_bundle(
        associate, payment_dicts=[{'f_acc': 'one', 't_acc': 'two'}], service=True, account={'number': 'one'}
    )

    bundle.account.remote_id = 'market'

    syncer = associate.payment_synchronizer(bundle)

    with response_mock(
        'POST https://test-ppapi.pingpongx.com/payment/status -> 200 :'
        '{"apiName": "/payment/status","code": "0000","message": "SUCCESS", '
        '"data": {"seller_id": "xxxxxxx","payment_id": "xxxxxxx","status": "Success"}}'
    ):
        result = syncer.run()

    bundle.refresh_from_db()

    assert result == bundle.status == bundle.payments[0].status == states.EXPORTED_H2H


def test_sender(build_payment_bundle, response_mock):
    associate = PingPong

    bundle = build_payment_bundle(
        associate, payment_dicts=[{'f_acc': 'one', 't_acc': 'two'}], service=True, account={'number': 'one'})

    bundle.account.remote_id = 'market'

    sender = associate.payment_sender(bundle)

    with response_mock(
        'POST https://test-ppapi.pingpongx.com/payment/credit -> 400 :'
        '{"apiName": "/payment/credit","code": "4102","message": "PAYMENT_EXIST"}'
    ):
        status = sender.send(bundle.tst_compiled)

    bundle.refresh_from_db()

    assert status == bundle.status == bundle.payments[0].status == states.ERROR
    assert len(bundle.remote_responses) == 1
    assert bundle.payments[0].remote_responses

    # Далее позитив.

    bundle = build_payment_bundle(
        associate, payment_dicts=[
            {'f_acc': 'one', 't_acc': 'tester', 'currency_id': 840, 'summ': Decimal('20.0')}],
        service=True, account={'number': 'one'})

    bundle.account.remote_id = 'market'

    sender = associate.payment_sender(bundle)

    with response_mock(
        'POST https://test-ppapi.pingpongx.com/payment/credit -> 200 :'
        '{"apiName": "/payment/credit","code": "0000","message": "SUCCESS", '
        '"data": {"seller_id": "xxxxxxx","payment_id": "xxxxxxx"}}'
    ):
        status = sender.send(bundle.tst_compiled)

    bundle.refresh_from_db()

    assert status == states.PROCESSING


def test_balance_getter(get_assoc_acc_curr, response_mock):
    associate = PingPong

    _, acc, _ = get_assoc_acc_curr(associate, account={'currency_code': Currency.USD})

    acc.remote_id = 'market'

    getter = associate.balance_getter(accounts=[acc])

    with response_mock(
        'POST https://test-ppapi.pingpongx.com/account/balance -> 200 :'
        '{"apiName": "/account/balance","code": "0000","message": "SUCCESS", '
        '"data": {"balanceList": [{"balance": "10200.00","currency": "USD"}]}}'
    ):
        balance = getter.run(account=acc)

    assert str(balance[0]) == '10200.00'


def test_statement_parser(fake_statements_quickcheck):
    fake_statements_quickcheck(associate=PingPong)
