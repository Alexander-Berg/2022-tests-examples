import json
from collections import namedtuple
from datetime import datetime
from decimal import Decimal
from time import sleep

import pytest
from freezegun import freeze_time

from bcl.banks.party_tochka.common import TochkaException
from bcl.banks.registry import Tochka
from bcl.core.models import StatementRegister, states, Statement

COMPANY_ID = '494e8864-fd3b-4e9b-8590-afd28d8f9ae1'
ACC_NUM = '40702810901500062714'


@pytest.fixture
def sign_right(dss_signing_right, robot):

    dss_signing_right(
        associate=Tochka,
        serial='01de325f0033ab80b942e1a141795f0a75',
        autosigning=True,
        level=1,
        username=robot.username,
    )


@pytest.fixture
def get_tochka_bundle(build_payment_bundle, get_assoc_acc_curr):

    def get_tochka_bundle_(payment_dicts=None):
        _, acc, _ = get_assoc_acc_curr(Tochka, account=ACC_NUM)

        acc.remote_id = COMPANY_ID
        acc.save()

        return build_payment_bundle(Tochka, payment_dicts=payment_dicts, account=acc, h2h=True)

    return get_tochka_bundle_


def test_connector(response_mock, dss_signing_mock, get_tochka_bundle):

    dss_signing_mock()
    bundle = get_tochka_bundle()
    caller = Tochka.payment_dispatcher.creators[0](bundle)

    with response_mock([

        'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 500 : '
        '{"jsonrpc": "2.0", "error": {"code": -32000, "message": "Server error", '
        '"data": "InterfaceError: connection already closed"}, "id": "e7d2b6ba-6c59-49f0-95d1-44cd74da3006"}',

        'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 500 : Backend Unavailable'

    ]):
        with pytest.raises(TochkaException) as e:
            Tochka.connector_dispatcher.get_connector(caller=caller).bundle_register('dummy')

        assert (
            'Code: -32000. Message: Server error. InterfaceError: connection already closed. Request'
            in str(e.value))

        with pytest.raises(TochkaException) as e:
            Tochka.connector_dispatcher.get_connector(caller=caller).bundle_register('dummy')

        assert 'Code: -1. Message:  Backend Unavailable. . Request' in str(e.value)


def test_testcall(sign_right, response_mock, dss_signing_mock):

    bundle = namedtuple('BundleMock', ['payments'])(payments=[])

    use_sandbox = False
    dss_signing_mock(bypass=use_sandbox)

    with response_mock(
        'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :'
        '{"jsonrpc": "2.0", "result": "HelloWorld"}',
        bypass=use_sandbox
    ):
        response = Tochka.connector_dispatcher.get_connector(caller=Tochka.payment_sender(bundle)).test_request()
        assert response.success


def test_balance_getter(sign_right, get_assoc_acc_curr, dss_signing_mock, response_mock, init_user):

    associate = Tochka
    if not associate.active:
        return

    _, acc, _ = get_assoc_acc_curr(associate, account=ACC_NUM)

    use_sandbox = False
    dss_signing_mock(bypass=use_sandbox)

    init_user(robot=True)

    with response_mock(
        'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :'
        '{"jsonrpc": "2.0", "result": {"available": 999984.34}, "id": "a4135043-0e91-4aca-973e-d13f0dc35947"}',
        bypass=use_sandbox
    ):
        associate.balance_getter.update_accounts_balance()

    acc.refresh_from_db()
    assert acc.balance == Decimal('999984.34')


def test_statement_automation(
        get_tochka_bundle, sign_right, get_assoc_acc_curr, dss_signing_mock, response_mock, run_task):

    associate = Tochka

    _, acc, _ = get_assoc_acc_curr(associate, account=ACC_NUM)

    use_sandbox = False
    dss_signing_mock(bypass=use_sandbox)

    response = json.dumps({'jsonrpc': '2.0',
           'result': {
               'bankCode': '044525999', 'accountCode': '40702810901500062714', 'currency': 'RUB',
                'startDate': '2020-03-04', 'endDate': '2020-03-04', 'summaryDebetTurnover': '1',
                'summaryCreditTurnover': '1', 'balanceIncoming': '1000000.02', 'balanceOutgoing': '1000000.02',
                'summaryDebetTurnoverNat': '1', 'summaryCreditTurnoverNat': '1', 'changed_flag': 1,
                'balanceIncomingNat': '1000000.02', 'balanceOutgoingNat': '1000000.02',
                'lastOperationDate': '2020-02-27', 'days': {
                    '2020-03-04': {
                        'creditTurnover': '1', 'debetTurnover': '1', 'balanceOutgoing': '1000000.02',
                        'balanceIncoming': '1000000.02', 'creditTurnoverNat': '1', 'debetTurnoverNat': '1',
                        'balanceOutgoingNat': '1000000.02', 'balanceIncomingNat': '1000000.02', 'operations': [
                           {'operationDate': '2020-03-04', 'transactionTypeCode': '01', 'documentNumber': '630',
                            'documentDate': '2020-03-04', 'bankCorrespondentAccount': '30101810845250000999',
                            'bankName': 'ТОЧКА ПАО БАНКА "ФК ОТКРЫТИЕ"', 'bankCode': '044525999',
                            'name': 'ООО "ОФИСМАНИЯ"', 'taxCode': '7718815845', 'taxReasonCode': '771801001',
                            'selfSideTaxCode': '7704340310', 'accountCode': '40702810301500018591', 'credit': False,
                            'amount': '1', 'amountNat': '1', 'desription': 'Платёж без НДС', 'priority': 5,
                            'paymentCode': '0', 'transactionId': '515406052;1',
                            'servicePayKey': 'slizering-206cde5e43ac4594848a6b9c7695903b'},
                           {'operationDate': '2020-03-04', 'transactionTypeCode': '01', 'documentNumber': '631',
                            'documentDate': '2020-03-04', 'bankCorrespondentAccount': '30101810845250000999',
                            'bankName': 'ТОЧКА ПАО БАНКА "ФК ОТКРЫТИЕ"', 'bankCode': '044525999',
                            'name': 'ООО "Бэнтэн Эксперт"', 'taxCode': '5902996879', 'taxReasonCode': '590201001',
                            'selfSideTaxCode': '7704340310', 'accountCode': '40702810914270002130', 'credit': True,
                            'amount': '1', 'amountNat': '1', 'desription': 'Платёж без НДС', 'priority': 5,
                            'paymentCode': '0', 'transactionId': '515406054;1',
                            'servicePayKey': 'slizering-f58d795d7a0a44b0baabc2431e02fa55'}]
                    }
               }},
           'id': 'f714954a-dd00-40c6-9801-2869a02e38cf'})

    # проверим, что суммы в json-float разберём и сохраним в Decimal
    response = response.replace('"balanceIncoming": "1000000.0"', '"balanceIncoming": 1000000.02')

    with freeze_time(datetime(2020, 3, 5)), response_mock(
        f'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :{response}',
        bypass=use_sandbox
    ):
        run_task('tochka_statements')

    run_task('process_statements')

    registers = list(StatementRegister.objects.all())
    assert len(registers) == 1
    register = registers[0]
    assert register.is_valid
    assert register.statement.status == states.STATEMENT_PROCESSED
    assert register.opening_balance == Decimal('1000000.020000')

    response = response.replace('1000000.0', '1000001.0')

    with freeze_time(datetime(2020, 3, 5)), response_mock(
        f'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :{response}',
        bypass=use_sandbox
    ):
        run_task('tochka_statements')

    run_task('process_statements')

    registers = list(StatementRegister.objects.all())
    assert len(registers) == 1

    statements = list(Statement.objects.all())
    statements = statements[1]
    assert statements.status == states.ERROR
    assert 'Ошибка сверки' in statements.processing_notes

    # Далее проверяем промежуточные выписки.
    with freeze_time(datetime(2020, 3, 5)), response_mock(
        "POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :"
        f"{response.replace('2020-03-04', '2020-03-05')}",
        bypass=use_sandbox
    ):
        run_task('tochka_statements_intraday')
        run_task('process_statements')


def test_payment_automation(
    get_tochka_bundle, sign_right, response_mock, run_task, dss_signing_mock, init_user,
    django_assert_num_queries,
):

    bundle = get_tochka_bundle(payment_dicts=[{}, {}])
    data = bundle.tst_compiled

    assert bundle.status == states.FOR_DELIVERY
    assert data['client_account'] == ACC_NUM
    assert data['registry_number'] == bundle.number

    use_sandbox = False
    dss_signing_mock(bypass=use_sandbox)

    response_status = json.dumps({'jsonrpc': '2.0', 'result': {'registry_status': 'COMPLETE', 'payments': [
        {
            'document_number': '1',
            'payment_id': '46717dab-0ab0-4b0e-897f-c0bf45c12ea4', 'purpose': 'Назначение',
            'recipient_credentials': {
                'recipient_name': 'ООО "Кинопортал"', 'recipient_inn': '7725713770',
                'recipient_kpp': '987654321', 'recipient_bik': '044525593',
                'recipient_bank_name': 'АО БАНК2', 'recipient_account': '40702810301400002360',
                'recipient_corr_acc': '30101810200000000593'},
            'payer_credentials': {
                'client_bik': '044525999', 'client_account': '40702810901500062714',
                'client_inn': '7704340310'},
            'amount': '152.00', 'payment_status': 'AWAITING',
            'description': 'В ожидании (Это платеж который находится у нас в очереди на исполнение)'
        },
        {'document_number': 'dummy'},
        {'document_number': '2', 'payment_status': 'dummy'},
        {
            'document_number': '2',
            'payment_id': '46717dab-0ab0-4b0e-897f-c0bf45c12ea5',
            'payment_status': 'UNKNOWN',
            'description': 'Не известно (все пошло не по плану и требуется ручное вмешательство)'
        },
    ]}, 'id': '52802997-e3c6-42d4-a0e9-1e57fed68db7'})

    init_user(robot=True)

    with response_mock(
        'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :'
        '{"jsonrpc": "2.0", "result": {"registry_id": "43ef3060-9490-4765-bbe6-e5171ce0c657"}, '
        '"id": "8456c4d0-273a-4f23-a61f-660864ec5e63"}',
        bypass=use_sandbox
    ):
        run_task('process_bundles')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.remote_id

    with response_mock(
        'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :'
        '{"jsonrpc": "2.0", "result": {"registry_id": "43ef3060-9490-4765-bbe6-e5171ce0c657"}, '
        '"id": "8456c4d0-273a-4f23-a61f-660864ec5e63"}',
        bypass=use_sandbox
    ):
        # проверяем, что не будет повторной регистрации ранее отправленного пакета (ключ - registry_number)
        # Точка при этом отвечает также, как и первой попытке регистрации.
        remote_id = bundle.remote_id
        bundle.remote_id = ''
        bundle.status = states.FOR_DELIVERY
        bundle.save()
        run_task('process_bundles')

        bundle.refresh_from_db()
        assert bundle.status == states.EXPORTED_H2H
        assert bundle.remote_id == remote_id

    with response_mock(
        f'POST https://stage.tochka.com/api/v1/b2b-registry/masspay/jsonrpc -> 200 :{response_status}',
        bypass=use_sandbox
    ):

        use_sandbox and sleep(10)

        with django_assert_num_queries(7) as _:
            run_task('tochka_statuses')

    bundle.refresh_from_db()

    assert bundle.status == states.ACCEPTED_H2H
    assert bundle.remote_responses

    payment = bundle.payments[0]
    assert payment.processing_notes == (
        '[AWAITING] В ожидании (Это платеж который находится у нас в очереди на исполнение)')
    assert payment.status == states.PROCESSING

    payment = bundle.payments[1]
    assert payment.status == states.ERROR
    assert payment.processing_notes == (
        '[UNKNOWN] Не известно (все пошло не по плану и требуется ручное вмешательство) '
        'Внешний ID платежа: 46717dab-0ab0-4b0e-897f-c0bf45c12ea5'
    )


def test_1c_statement_parser(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement.txt', Tochka, '40702810901500062714', 'RUB')[0]

    assert not register.intraday
    assert len(payments) == 1


def test_1c_payment_creator(build_payment_bundle):
    bundle = build_payment_bundle(Tochka)
    payment = bundle.payments[0]

    assert f'ПлательщикРасчСчет={payment.f_acc}' in bundle.tst_compiled
    assert f'ПолучательРасчСчет={payment.t_acc}' in bundle.tst_compiled
