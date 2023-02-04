from datetime import datetime

import pytest

from bcl.banks.party_raiff.common import RaiffFactoringConnector
from bcl.banks.registry import Raiffeisen
from bcl.core.models import states, Payment, StatementRegister, StatementPayment
from bcl.exceptions import ValidationError

ACC_NUM = '1234566788999'

payment_dict = {
    'payout_type': Payment.PAYOUT_TYPE_FACTOR,
    't_acc': '11122333455556666666',
    't_ogrn': '1122334455667',
}


@pytest.fixture
def get_raiff_bundle(build_payment_bundle, get_assoc_acc_curr):

    def get_raiff_bundle_(payment_dicts=None):
        _, acc, _ = get_assoc_acc_curr(Raiffeisen, account={'number': ACC_NUM})

        return build_payment_bundle(Raiffeisen, payment_dicts=payment_dicts, account=acc, h2h=True)

    return get_raiff_bundle_


@pytest.fixture
def mock_auth(monkeypatch):
    monkeypatch.setattr(RaiffFactoringConnector, '_auth', lambda *args, **kwargs: ('1234', '5678'))


def get_raiff_response_send(payment_uuid, status, errors=''):
    """Ответ на запрос регистрации пакета платежей - order-package."""
    response = (
        '{"orders": [{"id": "26415d4a-de6b-11eb-ba80-0242ac134586","referenceId": "UUID", "paymentRequisite": '
        '{"account": "11122333455556666666","correspondentAccount": "30101810200000000700", "bic": "044525700", '
        '"recipientBankName": "АО «Райффайзенбанк»", "purpose": "Оплата по контракту 111111 от 31.12.2021"}, '
        '"maturityDate": "2021-12-31", "orderSum": 123.45, "currencyCode": "RUR", "debtSum": 123.45, "state": '
        '{"status": "APPROVED", "statusDate": "2021-12-31T12:12:12.111111" errors}, "description": null}], '
        '"packageId": "26415d4a-de6b-11eb-ba80-0333ac130004"}'
    )
    response = response.replace('UUID', payment_uuid)
    response = response.replace('APPROVED', status)
    response = response.replace('errors', errors)

    return response


def get_raiff_response_status(payment_uuid, status, errors=''):
    """Ответ на запрос статуса пакета платежей - by-packet-id."""

    response = (
        '[  {  "creditor": '
        '{  "fullNameRus": "ОАО Поставляю все",  "inn": "7704357909",  "ogrn": "116774649139",  "kpp": "770435790"  }, '
        '"currencyCode": "RUB",  "debtSum": 123.45,  '
        '"id": "bf1d1868-a98e-4ae5-976f-0c4e21e0d510",  "maturityDate": "2021-07-10",  '
        '"orderDate": "2021-07-10",  "orderSum": 123.45,  '
        '"payment": {  "number": "123",  "amount": "123.45",  "date": "2021-07-10"  },  '
        '"paymentRequisite": {  "account": "11122333455556666666",  '
        '"correspondentAccount": "30101810200000000700",  "bic": "044525700",  '
        '"recipientBankName": "АО «Райффайзенбанк»",  '
        '"purpose": "Оплата по контракту 111111 от 31.12.2021"  },  '
        '"referenceId": "UUID",  '
        '"state": {  "status": "FETCHED",  "statusDate": "2021-07-10T06:53:37.433211" errors'
        '}  }  ]'
    )
    response = response.replace('UUID', payment_uuid)
    response = response.replace('FETCHED', status)
    response = response.replace('errors', errors)

    return response


def test_payment_automation(
    get_raiff_bundle, response_mock, run_task, django_assert_num_queries, dss_signing_mock, init_user, read_fixture,
    dss_signing_right, mock_auth
):
    bundle = get_raiff_bundle(payment_dicts=[payment_dict])
    assert not bundle.remote_id

    use_sandbox = False
    dss_signing_right(associate=Raiffeisen, level=1, autosigning=True, username='ekkostina')

    dss_signing_mock(bypass=use_sandbox)
    raiff_answer_send = get_raiff_response_send(bundle.payments[0].number_src, 'APPROVED')

    with response_mock(
        f'POST https://extest.openapi.raiffeisen.ru/payables-finance/order-package -> 200 : {raiff_answer_send}',
        bypass=use_sandbox
    ):
        with django_assert_num_queries(15) as _:
            run_task('process_bundles')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.payments[0].status == states.EXPORTED_H2H
    assert bundle.remote_id

    raiff_answer_status = get_raiff_response_status(bundle.payments[0].number_src, 'SENT_TO_CHECK')

    with response_mock(
        'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/'
        f'{bundle.remote_id} -> 200:{raiff_answer_status}',
        bypass=use_sandbox
    ):
        with django_assert_num_queries(6) as _:
            run_task('raiff_statuses')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.payments[0].status == states.PROCESSING

    raiff_answer_status = raiff_answer_status.replace('SENT_TO_CHECK', 'FINANCED')

    with response_mock(
        f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/'
        f'{bundle.remote_id} -> 200:{raiff_answer_status}',
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')
        bundle.refresh_from_db()

        payment = bundle.payments[0]
        assert payment.status == states.COMPLETE
        assert payment.remote_id == '123'

    def make_payment_and_check_status(errors, *, status=states.REVOKED, bundle_status=states.ACCEPTED_H2H):
        bundle = get_raiff_bundle(payment_dicts=[payment_dict])
        raiff_answer_send = get_raiff_response_send(bundle.payments[0].number_src, 'FETCHED')
        raiff_answer_status = get_raiff_response_status(bundle.payments[0].number_src, 'REJECTED', errors=errors)

        with response_mock(
            [
                f'POST https://extest.openapi.raiffeisen.ru/payables-finance/order-package -> 200:{raiff_answer_send}',
                'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/'
                f'26415d4a-de6b-11eb-ba80-0333ac130004 -> 200:{raiff_answer_status}'
            ],
            bypass=use_sandbox
        ):
            run_task('process_bundles')
            run_task('raiff_statuses')

        bundle.refresh_from_db()
        assert bundle.status == bundle_status
        assert bundle.payments[0].status == status

    make_payment_and_check_status(
        ', "errors": [{"code": "ERROR_1004", "description": "test"}]',
        bundle_status=states.DECLINED_BY_BANK,
    )

    make_payment_and_check_status(
        ',"errors": [{"code": "ERROR_1001", "description": "test"}]',
        status=states.DECLINED_BY_BANK,
        bundle_status=states.DECLINED_BY_BANK,
    )

    make_payment_and_check_status(
        ',"errors": [{"code": "ERROR_1001", "description": "test"}, {"code": "ERROR_1004", "description": "test"}]',
        status=states.DECLINED_BY_BANK,
        bundle_status=states.DECLINED_BY_BANK,
    )


def test_payment_automation_failure(
    get_raiff_bundle, response_mock, run_task, django_assert_num_queries, dss_signing_mock, mock_auth, dss_signing_right
):
    bundle = get_raiff_bundle(payment_dicts=[payment_dict])

    use_sandbox = False
    dss_signing_mock(bypass=use_sandbox)
    dss_signing_right(associate=Raiffeisen, level=1, autosigning=True, username='ekkostina')

    def process(code=500):

        with response_mock(
            [
                f'POST https://extest.openapi.raiffeisen.ru/payables-finance/order-package -> {code} : '
                '{"message": "Atribute is mandatory"}'
            ],
            bypass=use_sandbox
        ):
            run_task('process_bundles')

        bundle.refresh_from_db()

    process()
    assert bundle.status == states.FOR_DELIVERY
    assert bundle.payments[0].status == states.BUNDLED

    bundle.processing_retries = 8
    bundle.processing_after_dt = datetime.utcnow()
    bundle.save()

    process()
    assert bundle.status == states.ERROR
    assert bundle.payments[0].status == states.ERROR

    bundle = get_raiff_bundle(payment_dicts=[payment_dict])
    process(400)
    assert bundle.status == states.ERROR
    assert bundle.payments[0].status == states.ERROR

    bundle = get_raiff_bundle(payment_dicts=[payment_dict])
    raiff_answer = get_raiff_response_send(bundle.payments[0].number_src, 'SENT_TO_CHECK')

    with response_mock(
        [f'POST https://extest.openapi.raiffeisen.ru/payables-finance/order-package -> 200 : {raiff_answer}'],
        bypass=use_sandbox
    ):
        run_task('process_bundles')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.payments[0].status == states.EXPORTED_H2H

    with response_mock(
        [f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle.remote_id} -> 500 : '
         'Service unavailable'],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.payments[0].status == states.EXPORTED_H2H

    with response_mock(
        [f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle.remote_id} -> 400 : '
         '{"message": "Atribute is mandatory"}'],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
    assert bundle.payments[0].status == states.EXPORTED_H2H


def test_partitioner(get_source_payment_mass):
    batch_one = get_source_payment_mass(
        502, Raiffeisen, attrs={'f_acc': '1234', 'payout_type': Payment.PAYOUT_TYPE_FACTOR}
    )

    payments = Raiffeisen.payment_dispatcher.partition_payments(batch_one)

    assert len(payments) == 2
    assert len(payments[1]) == 2


def test_different_destinations(get_raiff_bundle, get_assoc_acc_curr):
    _, acc, _ = get_assoc_acc_curr(Raiffeisen, account={'number': '11122333455556666666'})
    bundles = get_raiff_bundle(
        payment_dicts=[
            payment_dict,
            {'payout_type': None, 'f_acc': '11122333455556666666', 't_ogrn': '1122334455667'}
        ]
    )

    assert len(bundles) == 2
    assert set([
        bundle.destination for bundle in bundles
    ]) == {bundles[0].DESTINATION_FACTOR, bundles[0].DESTINATION_H2H}


def test_faked_statement(
    get_raiff_bundle, response_mock, run_task, django_assert_num_queries, dss_signing_mock, init_user, read_fixture,
    dss_signing_right, mock_auth, fake_statements, stabilize_payment_dt, time_freeze
):
    use_sandbox = False
    dss_signing_right(associate=Raiffeisen, level=1, autosigning=True, username='ekkostina')

    dss_signing_mock(bypass=use_sandbox)

    init_user(robot=True)

    bundle_complete = get_raiff_bundle(
        payment_dicts=[payment_dict]
    )
    bundle_complete.remote_id = '26415d4a-de6b-11eb-ba80-0333ac130009'

    bundle = get_raiff_bundle(
        payment_dicts=[payment_dict]
    )
    bundle.remote_id = '26415d4a-de6b-11eb-ba80-0333ac130004'
    bundles = [bundle_complete, bundle]

    for bundle_ in bundles:
        bundle_.status = states.EXPORTED_H2H
        bundle_.sent = True
        bundle_.save()

    raiff_answer = get_raiff_response_status(bundle.payments[0].number_src, 'FINANCED')
    raiff_answer2 = get_raiff_response_status(bundle_complete.payments[0].number_src, 'FINANCED')

    with response_mock(
        [
            f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle_complete.remote_id}'
            f' -> 200:{raiff_answer2}',
            f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle.remote_id} -> 200:'
            f'{raiff_answer}'
        ],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

    for bundle_ in bundles:
        bundle_.refresh_from_db()
        assert bundle_.payments[0].status == states.COMPLETE

    raiff_answer = raiff_answer.replace('FINANCED', 'RETURNED')

    with response_mock(
        [
            f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle_complete.remote_id}'
            f' -> 200:{raiff_answer2}',
            f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle.remote_id} -> 200:'
            f'{raiff_answer}'
        ],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

    bundle.refresh_from_db()
    assert bundle.payments[0].status == states.RETURNED

    bundle_complete.refresh_from_db()
    assert bundle_complete.payments[0].status == states.COMPLETE

    with response_mock(
        [
            f'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/by-packet-id/{bundle_complete.remote_id}'
            f' -> 200:{raiff_answer2}'
        ],
        bypass=use_sandbox
    ):
        run_task('raiff_statuses')

    # фиксируем даты, дабы обеспечить постоянство в тестах
    with stabilize_payment_dt('2021-12-01', bundles=bundles) as now:

        with django_assert_num_queries(22) as _:
            statement = fake_statements(
                associate=Raiffeisen,
                account=bundle.account,
                parse=True,
                generate_payments=False,
                on_date=now,
            )[0]

    register = StatementRegister.objects.get(statement_id=statement.id)
    assert register.payment_cnt == 2
    assert register.is_valid

    payments = StatementPayment.objects.filter(register_id=register.id)
    for pay in payments:
        if pay.is_in:
            assert pay.number == f'{bundle.payments[0].number}'
        else:
            assert pay.number == f'{bundle_complete.payments[0].number}'


def test_payment_validation(get_payment_bundle, get_source_payment):
    oebs_payment1 = get_source_payment({
        'payout_type': Payment.PAYOUT_TYPE_FACTOR, 'f_bic': Raiffeisen.bid, 't_bic': '', 't_ogrn': '213', 't_cacc': '',
    })

    with pytest.raises(ValidationError) as e:
        Raiffeisen.payment_dispatcher.get_creator(
            get_payment_bundle([oebs_payment1], h2h=True)
        ).create_bundle()

    assert 'БИК получателя' in e.value.msg
    assert 't_cacc' in e.value.msg
    assert 'поле "ОГРН получателя" (t_ogrn)\nНеверная длина 3 (мин. 13; макс. 15)' in e.value.msg
