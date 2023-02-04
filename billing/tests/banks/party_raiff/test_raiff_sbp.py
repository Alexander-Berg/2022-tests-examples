import pytest
from bcl.banks.registry import Raiffeisen
from bcl.core.models import Service, states, Payment, StatementPayment

ACC_NUM = '098765789087'


@pytest.fixture
def get_raiff_bundle(build_payment_bundle, get_assoc_acc_curr):

    def get_raiff_bundle_(payment_dicts=None):
        _, acc, _ = get_assoc_acc_curr(Raiffeisen, account={'number': ACC_NUM, 'sbp_payments': True})

        org = acc.org
        org.connection_id = 'probki'
        org.save()

        return build_payment_bundle(Raiffeisen, payment_dicts=payment_dicts, account=acc, h2h=True)

    return get_raiff_bundle_


def test_timeout_handling(get_raiff_bundle, response_mock, run_task):
    bundle = get_raiff_bundle(payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA}])

    resp = '''
        {
          "id": "%s",
          "account": "40700000000000000000",
          "payouts": [
            {
              "id": "%s",
              "some": {"contract": "1234567/89012"},
              "status":{"value":"DECLINED","declineReason": "TIMEOUT","date":"2019-07-11T17:45:13+03:00"}
            }]
        }
        ''' % (bundle.id, bundle.payments[0].number)

    with response_mock([
        f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles -> 200:{resp}',
        f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 200:{resp}',
    ]):
        run_task('process_bundles')
        run_task('raiff_statuses')

    bundle.refresh_from_db()
    payment = bundle.payments[0]
    assert bundle.status == states.DECLINED_BY_BANK  # все (1) платежи невалидны
    assert payment.status == states.CANCELLED


def test_payment_automation(get_raiff_bundle, response_mock, run_task, django_assert_num_queries):
    bundle = get_raiff_bundle(payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA}])

    assert set(bundle.tst_compiled['payouts'][0]['payoutParams'].keys()) == {'inn', 'phone', 'bankAlias'}

    use_sandbox = False
    raiff_answer = (
        '''
        {
          "id": "%s",
          "account": "40700000000000000000",
          "payouts": [
            {
              "id": "1404fhr7i272a2",
              "amount": 1110.01,
              "currency": "RUB",
              "paymentDetails": "Выплата от страховой компании",
              "payoutMethod": "SBP",
              "payoutParams":{"phone":"79191234567","bankAlias":"RAIFFEISEN"},"extra":{"contract":"1234567/89012"},
              "incomeTypeCode": "1",
              "extra": {"contract": "1234567/89012"},
              "status":{"value":"IN_PROGRESS","declineReason": "RECEIVER_ACCOUNT_ERROR","date":"2019-07-11T17:45:13+03:00"},
              "createDate":"2019-07-11T17:45:13+03:00"
            }]
        }
        ''' % bundle.id
    )

    # отправка пакета
    with response_mock(
        [f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles -> 200 : {raiff_answer}'],
        bypass=use_sandbox
    ):
        run_task('process_bundles')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.payments[0].status == states.EXPORTED_H2H

    # получение статуса В обработке.
    with response_mock(
        [f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 200 : {raiff_answer}'],
        bypass=use_sandbox
    ):
        with django_assert_num_queries(8) as _:
            run_task('raiff_statuses')
            bundle.refresh_from_db()

        assert bundle.status == states.PROCESSING
        assert bundle.payments[0].status == states.PROCESSING

    # получение отказа банка
    raiff_answer = raiff_answer.replace('IN_PROGRESS', 'DECLINED')

    with response_mock(
        [f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 200 : {raiff_answer}'],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')
        bundle.refresh_from_db()
        assert bundle.status == states.DECLINED_BY_BANK
        assert bundle.payments[0].status == states.DECLINED_BY_BANK
        assert bundle.payments[0].processing_notes == '[DECLINED] RECEIVER_ACCOUNT_ERROR'

    bundle = get_raiff_bundle(
        payment_dicts=[{
            'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA, 't_fio': 'test1|test2|'
        }]
    )

    assert set(
        bundle.tst_compiled['payouts'][0]['payoutParams'].keys()
    ) == {'inn', 'phone', 'bankAlias', 'firstName', 'middleName', 'lastName'}

    # платежи проведены банком
    raiff_answer = raiff_answer.replace('DECLINED', 'COMPLETED').replace('declineReason', 'test')

    with response_mock(
        [f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles -> 200 : {raiff_answer}'],
        bypass=use_sandbox
    ):
        run_task('process_bundles')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.payments[0].status == states.EXPORTED_H2H

    with response_mock(
        [f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 200 : {raiff_answer}'],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')
        bundle.refresh_from_db()

        assert bundle.status == states.ACCEPTED_H2H
        assert bundle.payments[0].status == states.COMPLETE


def test_payment_automation_failure(get_raiff_bundle, response_mock, run_task, django_assert_num_queries, time_shift):

    bundle = get_raiff_bundle(payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA}])

    use_sandbox = False

    def process():

        with response_mock(
            f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles -> 200 : '
            '{"code":"ERROR.INVALID_REQUEST","message":"Account is required"}',
            bypass=use_sandbox
        ):
            run_task('process_bundles')

        bundle.refresh_from_db()

    process()
    assert bundle.status == states.ERROR
    assert bundle.payments[0].status == states.ERROR

    bundle = get_raiff_bundle(payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA}])

    with response_mock(
        f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles -> 200 : '
        '{"id":"1404fhr7i272a2","account":40700000000000000000,"amount":1110.01,"currency":"RUB",'
        '"payoutMethod":"SBP","payoutParams":{"phone":"79191234567","bankAlias":"RAIFFEISEN"},'
        '"extra":{"contract":"1234567/89012"},"status":{"value":"IN_PROGRESS","date":"2019-07-11T17:45:13+03:00"},'
        '"createDate":"2019-07-11T17:45:13+03:00"}',
        bypass=use_sandbox
    ):
        run_task('process_bundles')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.payments[0].status == states.EXPORTED_H2H

    with response_mock(
        f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 500 :Service unavailable',
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.payments[0].status == states.EXPORTED_H2H

    with response_mock(
        f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 200 : '
        '{"code":"ERROR.PAYOUT_NOT_FOUND","message":"Выплата с Id 5788917 не найдена"}',
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

        bundle.refresh_from_db()
        assert bundle.status == states.DECLINED_BY_BANK
        assert bundle.payments[0].status == states.DECLINED_BY_BANK

    bundle = get_raiff_bundle(payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.OEBS}])

    with response_mock(
        f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles -> 404 : '
        '{"code":"ERROR.BANK_NOT_FOUND","message":"Банк с alias SPB_TEST_CODE не найден"}',
        bypass=use_sandbox
    ):
        run_task('process_bundles')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.payments[0].status == states.EXPORTED_H2H

    with response_mock(
        f'GET https://test.ecom.raiffeisen.ru/api/payout/v1/payout-bundles/{bundle.id} -> 404 : '
        '{"code":"ERROR.PAYOUT_NOT_FOUND","message":"Выплата с Id 5788917 не найдена"}',
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

        bundle.refresh_from_db()
        assert bundle.status == states.DECLINED_BY_BANK
        assert bundle.payments[0].status == states.DECLINED_BY_BANK


def test_statement_parser(
    get_raiff_bundle, response_mock, run_task, django_assert_num_queries, parse_statement_fixture, get_assoc_acc_curr
):
    account_number = '40702810400000167186'
    _, acc, _ = get_assoc_acc_curr(Raiffeisen, account={'number': account_number, 'sbp_payments': True})

    bundle = get_raiff_bundle(
        payment_dicts=[{
            'payout_type': 3,
            't_acc': '79198762536',
            'service_id': Service.TOLOKA,
            'status': states.EXPORTED_H2H,
            'summ': 1110.01,
            'f_acc': account_number
        }]
    )

    payment = Payment.objects.get(bundle_id=bundle.id)
    payment.status = states.EXPORTED_H2H
    payment.save()

    register, payments = parse_statement_fixture(
        'statement/sbp_statement.xml', associate=Raiffeisen,
        encoding='utf-8', acc=[account_number], curr='RUB', mutator_func=lambda x: x.replace('5788930', str(bundle.id))
    )[0]
    linked_payment = StatementPayment.getone(register__id=register.id, number=payment.number)

    assert linked_payment.payment == payment
