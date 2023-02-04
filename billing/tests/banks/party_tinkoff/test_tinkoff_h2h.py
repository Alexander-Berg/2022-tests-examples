import json
import uuid
from datetime import datetime, timedelta, date
from decimal import Decimal

import pytest

from bcl.banks.balance_getter import BalanceGetter
from bcl.banks.party_tinkoff.balance_getter import TinkoffBalanceGetter
from bcl.banks.party_tinkoff.common import TinkoffConnector
from bcl.banks.party_tinkoff.registry_operator import (
    TinkoffCardRegistry, TinkoffSalaryRegistry, TinkoffRegistryOperator,
)
from bcl.banks.party_tinkoff.statement_downloader import TinkoffStatementDownloader
from bcl.banks.registry import Tinkoff
from bcl.core.models import QuerySet, StatementRegister, states, StatementPayment, Payment
from bcl.exceptions import SalaryUserHandledError

BASE_URL = 'https://secured-openapi.business.tinkoff.ru/api/v1/'


@pytest.mark.skipif(not isinstance(Tinkoff.balance_getter, TinkoffBalanceGetter), reason='Временно отключен')
def test_balance_getter(get_assoc_acc_curr, response_mock):
    _, account, _ = get_assoc_acc_curr(Tinkoff, account='40802810500000206666')
    account.org.connection_id = Tinkoff.connector.settings_alias_default
    account.org.save()

    _, account2, _ = get_assoc_acc_curr(Tinkoff, account='40802810500000206669', org='test_not_connected')
    _, account3, _ = get_assoc_acc_curr(Tinkoff, account='40802810500000206668')

    assert not account.balance
    assert not account2.balance
    assert not account3.balance

    with response_mock(
        f'GET {BASE_URL}bank-accounts -> 500 : '
        '{"errorId": "asdq3412", "errorMessage": "Непредвиденная ошибка", "errorCode": "INTERNAL_ERROR"}'
    ):
        BalanceGetter.update_accounts_balance(accounts=QuerySet(account))

    def refresh_accounts():
        for acc in [account, account2, account3]:
            acc.refresh_from_db()

    refresh_accounts()
    assert not account.balance
    assert not account2.balance
    assert not account3.balance

    response_data = [
        {
            'accountNumber': account.number,
            'currency': '643',
            'balance': {
                'otb': 45089,
                'authorized': 0,
                'pendingPayments': 0,
                'pendingRequisitions': 0
            }
        }
    ]

    with response_mock(
        f'GET {BASE_URL}bank-accounts -> 200 :{json.dumps(response_data)}'
    ):
        BalanceGetter.update_accounts_balance(accounts=QuerySet(account))
    refresh_accounts()
    assert account.balance == response_data[0]['balance']['otb']
    assert not account2.balance
    assert not account3.balance


def test_statement_downloader(
        get_assoc_acc_curr, read_fixture, get_source_payment, mock_post, init_user, response_mock):
    """Проверяет загрузку итоговой выписки."""
    init_user(robot=True)
    testuser = init_user('testuser')

    _, account, _ = get_assoc_acc_curr(Tinkoff.id, account='40702810510000309857')
    account.org.connection_id = TinkoffConnector.settings_alias_default
    account.org.save()

    _, account2, _ = get_assoc_acc_curr(Tinkoff.id, account='40702810510000309859', org='test_not_connected')
    source_payment = get_source_payment({
        'ground': 'Назначение платежа',
        'summ': '10.00',
        'number': '1234',
        'f_acc': account.number,
        'status': states.EXPORTED_ONLINE
    }, associate=Tinkoff)

    statement_data = read_fixture('statement.json').decode('utf-8')

    yesterday_str = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')

    downloader = TinkoffStatementDownloader(final=True)
    with response_mock(
        f'GET {BASE_URL}bank-statement/'
        f'?accountNumber={account.number}&from={yesterday_str}&till={yesterday_str} -> 200 : {statement_data}'
    ):
        statements = downloader.process()

    assert len(statements) == 1
    statement = statements[0]

    statement.user = testuser
    statement.save()

    parser = Tinkoff.statement_dispatcher.get_parser(statement)
    parser.process()

    register: StatementRegister = StatementRegister.objects.all()[0]

    assert register.statement_date == date(2020, 9, 1)
    assert register.closing_balance == 30
    assert register.account.number == account.number
    assert register.status == 1

    payments = StatementPayment.objects.all()
    payments = sorted(payments, key=lambda p: int(p.trans_id))

    assert len(payments) == 4
    payment: StatementPayment = payments[0]

    assert [payments[0].summ, payments[1].summ] == [Decimal('10'), Decimal('20')]

    assert payment.is_out
    assert payment.is_out != payments[1].is_out

    assert payment.get_info_account() != account.number
    assert payment.get_info_inn() == '77043409'
    assert payment.get_info_bik() == '044525974'
    assert payment.get_operation_type() == '1'
    assert payment.date_valuated == date(2020, 9, 1)

    def check_payment(payment_oebs, provedpay):
        payment_oebs.refresh_from_db()

        assert payment_oebs.statementpayment_set.all()[0].number == provedpay.number
        assert payment_oebs.is_complete

    check_payment(source_payment, payments[0])

    assert payments[-1].get_info_inn() == ''
    assert payments[-2].number == ''


def test_statement_downloader_wo_payment(get_assoc_acc_curr, read_fixture, response_mock, init_user):
    """Проверяет загрузку выписки без платежей"""

    init_user(robot=True)
    testuser = init_user('testuser')

    _, account, _ = get_assoc_acc_curr(Tinkoff.id, account='40702810510000309857')
    account.org.connection_id = TinkoffConnector.settings_alias_default
    account.org.save()

    statement_data = {
        'accountNumber': account.number,
        'saldoIn': 10, 'income': 0, 'outcome': 0, 'saldoOut': 10, 'operation': []
    }

    yesterday_str = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')

    downloader = TinkoffStatementDownloader(final=True)
    with response_mock(
        f'GET {BASE_URL}bank-statement/?accountNumber={account.number}&from={yesterday_str}&till={yesterday_str} -> '
        f'200 : {json.dumps(statement_data)}'
    ):
        statements = downloader.process()

    assert len(statements) == 1
    statement = statements[0]

    statement.user = testuser
    statement.save()

    parser = Tinkoff.statement_dispatcher.get_parser(statement)
    parser.process()

    register: StatementRegister = StatementRegister.objects.filter(statement=statement)[0]
    assert register.is_valid
    assert register.payment_cnt == 0

    assert len(StatementPayment.objects.filter(register=register)) == 0


def test_statement_downloader_error(get_assoc_acc_curr, read_fixture, response_mock, init_user):
    init_user(robot=True)

    _, account, _ = get_assoc_acc_curr(Tinkoff.id, account='40702810510000309857')
    account.org.connection_id = TinkoffConnector.settings_alias_default
    account.org.save()

    yesterday_str = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')

    with response_mock(
        f'GET {BASE_URL}bank-statement/?accountNumber={account.number}&from={yesterday_str}&till={yesterday_str} -> 500 '
        ': {"errorId": "asdq3412", "errorMessage": "Непредвиденная ошибка", "errorCode": "INTERNAL_ERROR"}'
    ):
        statements = TinkoffStatementDownloader(final=True).process()

    assert len(statements) == 0


def test_card_registry(response_mock, get_salary_registry):

    address_of_residence = {
        'city': {'name': 'Москва', 'short_name': 'г'}, 'index': '115522',
        'region': {'name': 'Москва', 'short_name': 'г'},
        'street': {'name': 'Дубосековская', 'short_name': 'ул'},
        'country': {'code': '', 'name': 'Российская Федерация', 'short_name': ''},
        'district': {'name': 'Москва', 'short_name': ''},
        'settlement': {'name': 'Москва', 'short_name': ''},
        'house_block': '', 'house_number': '13', 'apartment_number': '2'
    }
    address_of_work = {
        'city': {'name': 'Москва', 'short_name': 'г'}, 'index': '119021',
        'region': {'name': 'Москва', 'short_name': 'г'},
        'street': {'name': 'Льва Толстого', 'short_name': 'ул'},
        'country': {'code': '', 'name': 'Российская Федерация', 'short_name': ''},
        'district': {'name': '', 'short_name': ''},
        'settlement': {'name': '', 'short_name': ''},
        'house_block': '', 'house_number': '16', 'apartment_number': ''
    }

    registry = get_salary_registry(
        Tinkoff, TinkoffCardRegistry, employees=[
            {
                'record_id': '1',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'patronymic': '',
                'resident': True,
                'embossed_text': {'field1': 'IVANOV', 'field2': 'IVAN'},
                'citizenship': 'Россия',
                'sex': 'M',
                'mobile_phone': '89197218253',
                'place_of_birthday': {'city': {'name': 'Москва'}},
                'birthday_date': datetime(1993, 10, 22),
                'position': 'разработчик',
                'identify_card': {
                    'number': '111333', 'series': '1111', 'card_type_code': 21, 'issue_date': datetime(2018, 10, 23),
                    'issued_by': 'РОВД "123" России',
                    'card_type': 'Паспорт гражданина Российской Федерации',
                    'subdivision_code': '777-123'
                },
                'address_of_residence': address_of_residence,
                "address_of_work": address_of_work,
                'address': address_of_residence,
            },
            {
                'record_id': '2',
                'first_name': 'Петр',
                'last_name': 'Иванов',
                'patronymic': '',
                'resident': True,
                'embossed_text': {'field1': 'IVANOV', 'field2': 'IVAN'},
                'sex': 'M',
                'citizenship': 'Россия',
                'mobile_phone': '89197242113',
                'place_of_birthday': {'city': {'name': 'Москва'}},
                'birthday_date': datetime(1993, 10, 24),

                'identify_card': {
                    'number': '111222', 'series': '1111', 'card_type_code': 21, 'issue_date': datetime(2018, 10, 23),
                    'issued_by': 'РОВД "123" России',
                    'card_type': 'Паспорт гражданина Российской Федерации', 'subdivision_code': '777-123'
                },
                'position': 'разработчик',
                'address_of_residence': address_of_residence,
                "address_of_work": address_of_work,
                'address': address_of_residence,
            }
        ]
        , automated_contract=True
    )
    registry.set_status(states.FOR_DELIVERY)
    org = registry.contract.org
    org.connection_id = 'eda'
    org.save()

    TinkoffRegistryOperator.send(registry=registry)

    registry.refresh_from_db()
    assert registry.status == states.FOR_DELIVERY
    assert registry.status_to_oebs() == 0
    assert not registry.sent

    registry.processing_retries = 3
    registry.save()

    TinkoffRegistryOperator.send(registry=registry)

    registry.refresh_from_db()
    assert registry.is_error
    assert registry.status_to_oebs() == 0
    assert not registry.sent

    card_type = registry.employees[0].identify_card['card_type']

    registry.employees[0].identify_card['card_type'] = 'test'
    registry.save()

    registry.set_status(states.FOR_DELIVERY)
    with pytest.raises(SalaryUserHandledError):
        TinkoffRegistryOperator.process(registry=registry)

    registry.refresh_from_db()
    assert registry.status == states.FOR_DELIVERY
    assert registry.status_to_oebs() == 0
    assert not registry.sent

    registry.employees[0].identify_card['card_type'] = card_type
    registry.save()

    response = {
        'employeeResults': [
            {
                'number': 1,
                'firstName': 'Иван',
                'lastName': 'Иванов',
                'middleName': 'Иванович',
                'status': 'QUEUED'
            },
            {
                'number': 2,
                'firstName': 'Иван',
                'lastName': 'Иванов',
                'middleName': 'Иванович',
                'status': 'ERROR',
                'errors': [
                    {
                        'fieldName': 'Дата рождения',
                        'errorDescription': 'Вам должно быть от 14 до 100 лет'
                    },
                    {
                        'fieldName': 'Адрес',
                        'errorDescription': 'Отсутствует адрес данного типа'
                    }
                ]
            }
        ]
    }

    with response_mock([
        f'POST {BASE_URL}salary/employees/create -> 200 : {{"correlationId": "{registry.registry_id}"}}',
        f'GET {BASE_URL}salary/employees/create/result/?correlationId={registry.registry_id} -> 200 : '
        f'{json.dumps(response)}'
    ]):
        TinkoffRegistryOperator.send(registry=registry)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.status_to_oebs() == 0
    assert registry.employees[0].result == 'QUEUED'
    assert registry.employees[1].result
    assert registry.employees_rejected == 1

    with response_mock(
        f'GET {BASE_URL}salary/employees/create/result/?correlationId={registry.registry_id} -> 200 : '
        f'{json.dumps(response)}'
    ):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.employees[0].result == 'QUEUED'
    assert registry.employees[1].result

    response['employeeResults'][0]['status'] = 'CREATED'
    response['employeeResults'][0]['employeeId'] = 217

    with response_mock(
        f'GET {BASE_URL}salary/employees/create/result/?correlationId={registry.registry_id} -> 200 : '
        f'{json.dumps(response)}'
    ):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.status_to_oebs() == 0

    response = {
        'employees': [
            {
                'id': 217,
                'status': 'DRAFT',
                'firstName': 'Иван',
                'lastName': 'Иванов',
                'middleName': 'Иванович',
                'birthDate': '1967-12-25',
                'bankInfo': {
                    'accountNumber': '40802123456789012345',
                    'agreementNumber': '1234567890'
                }
            }
        ]
    }

    with response_mock(f'POST {BASE_URL}salary/employees/list -> 200 : {json.dumps(response)}'):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.status_to_oebs() == 0
    assert registry.employees[0].result == response['employees'][0]['status']

    response['employees'][0]['status'] = 'ACTIVE'

    with response_mock(f'POST {BASE_URL}salary/employees/list -> 200 : {json.dumps(response)}'):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.status == states.REGISTER_ANSWER_LOADED
    assert registry.status_to_oebs() == 1
    assert registry.employees[0].result == 'счетОткрыт'
    assert registry.employees[0].personal_account == response['employees'][0]['bankInfo']['accountNumber']
    assert registry.employees_rejected == 1
    assert registry.employees_responded == 2


def test_salary_registry(response_mock, get_salary_registry, django_assert_num_queries):

    def create_registry():
        reg = get_salary_registry(
            Tinkoff, TinkoffSalaryRegistry, employees=[
                {
                    'record_id': '1',
                    'first_name': 'Рафаэль',
                    'last_name': 'Гегамян',
                    'patronymic': 'Гегамович',
                    'currency_code': 'RUB',
                    'amount': Decimal('0.01'),
                    'personal_account': '40817810800000114090',
                },
            {
                    'record_id': '2',
                    'first_name': 'Иван',
                    'last_name': 'Иванов',
                    'patronymic': '',
                    'currency_code': 'RUB',
                    'amount': Decimal(30),
                    'personal_account': '12345678901234567899',
                    'target_bik': '5678',
                    'card_number': '6677788',
                    'amount_deduct': Decimal('5.05')
                }
            ]
            , automated_contract=True
        )
        reg.set_status(states.FOR_DELIVERY)
        return reg

    registry = create_registry()
    org = registry.contract.org
    org.connection_id = 'eda'
    org.save()

    # отправляем реестр
    with response_mock(
        f'POST {BASE_URL}salary/payment-registry/create -> 200 : {{"correlationId": "{registry.registry_id}"}}'
    ) as resp_mock:
        with django_assert_num_queries(5) as _:
            TinkoffRegistryOperator.send(registry=registry)

        request_data = resp_mock.calls[0].request.body.decode()
        assert '"sum": 0.01' in request_data
        assert '"collectionAmount": 5.05' in request_data

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.employees_rejected == 0
    assert registry.employees_responded == 0

    # вернулась ошибка
    with response_mock(
        f'GET {BASE_URL}salary/payment-registry/create/result/?correlationId={registry.registry_id} -> 200 : ' +
        str(json.dumps({
            'paymentRegistryId': 1,
            'status': 'ERROR',
            'error': {
                'fieldName': 'Ошибка создания платежного реестра',
                'errorDescription': 'Непредвиденная ошибка. Пожалуйста, попробуйте позже.'
            },
            'paymentErrors': [
                {
                    'number': 1,
                    'accountNumber': '40802123456789012345',
                    'errors': [
                        {
                            'fieldName': 'Номер счета',
                            'errorDescription': 'Неверный номер счёта'
                        },
                        {
                            'fieldName': 'Номер договора',
                            'errorDescription': 'Номер договора не найден'
                        }
                    ]
                }
            ]
        }))
    ):
        with django_assert_num_queries(3) as _:
            TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.employees_rejected == 1
    assert registry.employees_responded == 1
    assert registry.is_error
    assert registry.status_to_oebs() == 0

    # далее проверка позитивного сценария
    registry = create_registry()

    with response_mock(
        f'POST {BASE_URL}salary/payment-registry/create -> 200 : {{"correlationId": "{registry.registry_id}"}}'
    ):
        TinkoffRegistryOperator.send(registry=registry)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.status_to_oebs() == 0

    # банк поставил реестр в очередь на регистрацию
    with response_mock(
        f'GET {BASE_URL}salary/payment-registry/create/result/?correlationId={registry.registry_id} -> 200 : '
        '{"status": "QUEUED"}'
    ):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.sent
    assert registry.status_to_oebs() == 0

    # банк зарегистировал реестр
    with response_mock(
        f'GET {BASE_URL}salary/payment-registry/create/result/?correlationId={registry.registry_id} -> 200 : '
        '{"paymentRegistryId": 1, "status": "CREATED"}'
    ):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_exported_h2h
    assert registry.remote_id
    assert registry.employees_rejected == 0
    assert registry.employees_responded == 0

    # запрашиваем проведение реестра
    with response_mock(
        f'POST {BASE_URL}salary/payment-registry/submit -> 200 : {{"correlationId": "{registry.registry_id}"}}'
    ):
        with django_assert_num_queries(4) as _:
            TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.employees_rejected == 0
    assert registry.employees_responded == 0
    assert registry.is_signed
    assert registry.status_to_oebs() == 0

    with response_mock([
        # получили подтверждение, что реестр готов к оплате
        f'GET {BASE_URL}salary/payment-registry/submit/result/?correlationId={registry.registry_id} -> 200 : '
        '{"paymentRegistryId": 12, "status": "ACCEPTED", "paymentErrors": []}',

        # инициируем оплату
        f'POST {BASE_URL}payment/payment-registry/pay -> 201 : test',
    ]
    ):
        with django_assert_num_queries(4) as _:
            TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_paid
    assert registry.remote_id
    assert registry.employees_rejected == 0
    assert registry.employees_responded == 0

    # реестр частично проведён
    with response_mock(
        f'GET {BASE_URL}salary/payment-registry/{registry.remote_id} -> 200 : ' +
        str(json.dumps({
            'status': 'PART_EXEC',
            'companyAccountNumber': '40816810800000122870',
            'paymentsCount': 2,
            'totalSum': 1000,
            'payments': [
                {
                    'number': 1,
                    'status': 'EXECUTED',
                    'accountNumber': '40802678901234567890',
                },
                {
                    'number': 2,
                    'status': 'ACCEPTED',
                    'accountNumber': '40802678901234567890',
                }
            ]
        }))
    ):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.is_paid
    assert registry.remote_id
    assert registry.employees_rejected == 0
    assert registry.employees_responded == 1

    # реестр полностью проведён
    with response_mock(
        f'GET {BASE_URL}salary/payment-registry/{registry.remote_id} -> 200 : ' +
        str(json.dumps({
            'status': 'EXECUTED',
            'companyAccountNumber': '40816810800000122870',
            'loadDate': '2015-05-09T12:30',
            'paymentsCount': 2,
            'totalSum': 1000,
            'payments': [
                {
                  'number': 1,
                  'status': 'EXECUTED',
                  'accountNumber': '40802678901234567890',
                },
                {
                  'number': 2,
                  'status': 'EXECUTED',
                  'accountNumber': '40802678901234567890',
                }
            ]
        }))
    ):
        TinkoffRegistryOperator.status_get(associate=Tinkoff)

    registry.refresh_from_db()
    assert registry.status == states.REGISTER_ANSWER_LOADED
    assert registry.remote_id
    assert registry.employees_rejected == 0
    assert registry.employees_responded == 2


def test_not_automated_contract(response_mock, get_salary_registry, get_salary_contract):
    contract = get_salary_contract(Tinkoff, number='123455666')
    registry = get_salary_registry(Tinkoff, TinkoffSalaryRegistry, contract_number=contract.number)
    registry.set_status(states.FOR_DELIVERY)

    TinkoffRegistryOperator.send(registry=registry)
    registry.refresh_from_db()

    assert registry.status == states.FOR_DELIVERY

    registry.processing_retries = 3
    registry.save()

    TinkoffRegistryOperator.send(registry=registry)
    registry.refresh_from_db()

    assert registry.is_error
    assert 'не поддерживает автоматическую отправку' in registry.processing_notes


def test_selfemployeed(build_payment_bundle, response_mock):

    selfempl = Payment.PAYOUT_TYPE_SELFEMPLOYED
    payment_params = {
        'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ', 'ground': 'тестовое назначение',
        't_fio': 'Иванов|Иван|Иванович'
    }
    bundle = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=[payment_params], h2h=True
    )
    org = bundle.org
    org.connection_id = 'eda'
    org.save()
    compiled = bundle.tst_compiled
    assert 'registryCreateType' in compiled

    # регистрируем реестр
    with response_mock(
        f'POST {BASE_URL}self-employed/payment-registry/create -> 200 :{{"correlationId": "{bundle.uuid}"}}'
    ):
        Tinkoff.automate_payments(bundle=bundle)

    bundle.refresh_from_db()
    assert bundle.is_exported_h2h

    correlation_id = bundle.remote_id_parsed.correlation_id_create

    # ответ с ошибкой
    with response_mock(
        f'GET {BASE_URL}self-employed/payment-registry/create/result/?correlationId={correlation_id} -> 200 :' +
        str(json.dumps({
            'paymentRegistryId': 1,
            'status': 'ERROR',
            'error': {
                'fieldName': 'Ошибка создания платежного реестра',
                'errorDescription': 'Непредвиденная ошибка. Пожалуйста, попробуйте позже.'
            },
            'paymentErrors': [
                {
                    'number': 1,
                    'accountNumber': '40802123456789012345',
                    'errors': [
                        {
                            'fieldName': 'Номер счета',
                            'errorDescription': 'Неверный номер счёта'
                        },
                        {
                            'fieldName': 'Номер договора',
                            'errorDescription': 'Номер договора не найден'
                        }
                    ]
                }
            ]
        }))
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_error

    bundle = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=[payment_params for _ in range(2)], h2h=True
    )

    # проба регистрации с попадением на ограничение типа 429
    with response_mock(
        f'POST {BASE_URL}self-employed/payment-registry/create -> 429:'
        '{"errorMessage": "Слишком много запросов. Попробуйте позже", '
        '"errorCode": "TOO_MANY_REQUESTS", "errorId": "884289dfb0"}'
    ):
        Tinkoff.automate_payments(bundle=bundle)

    bundle.refresh_from_db()
    assert bundle.is_processing_ready

    # регистрируем реестр снова
    with response_mock(
        f'POST {BASE_URL}self-employed/payment-registry/create -> 200 :{{"correlationId": "{bundle.uuid}"}}'
    ):
        Tinkoff.automate_payments(bundle=bundle)

    bundle.refresh_from_db()
    assert bundle.is_exported_h2h

    correlation_id = bundle.remote_id_parsed.correlation_id_create

    # ошибка создания реестра со стороны банка: Internal Error
    with response_mock(
        f'GET {BASE_URL}self-employed/payment-registry/create/result/?correlationId={correlation_id} -> 200 :' +
        str(json.dumps({
            'status': 'ERROR',
            'error': {
                'fieldName': 'Ошибка создания платежного реестра',
                'errorDescription': 'Непредвиденная ошибка. Пожалуйста, попробуйте позже.'
            },
            'paymentErrors': []
        }))
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_processing_ready

    # регистрируем реестр без ошибки
    with response_mock(
        f'POST {BASE_URL}self-employed/payment-registry/create -> 200 :{{"correlationId": "{bundle.uuid}"}}'
    ):
        Tinkoff.automate_payments(bundle=bundle)

    bundle.refresh_from_db()
    assert bundle.is_exported_h2h

    correlation_id = bundle.remote_id_parsed.correlation_id_create

    # проверяем статус: реестр в ожидании регистрации
    with response_mock(
        f'GET {BASE_URL}self-employed/payment-registry/create/result/?correlationId={correlation_id} -> 200 :'
        '{"status": "QUEUED"}'
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_exported_h2h
    assert bundle.sent

    # банк зарегистрировал реестр
    with response_mock(
        f'GET {BASE_URL}self-employed/payment-registry/create/result/?correlationId={correlation_id} -> 200 :'
        '{"paymentRegistryId": 1, "status": "CREATED"}'
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_exported_h2h
    assert bundle.remote_id_parsed.id

    # запрашиваем проведение реестра
    with response_mock(
        f'POST {BASE_URL}self-employed/payment-registry/submit -> 200 : {{"correlationId": "{bundle.uuid}"}}'
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_signed

    # ошибка подписания на стороне банка
    sign_result = {
        'paymentRegistryId': 4551,
        'status': 'ERROR',
        'error': {
            'errorCode': 'PAYMENT_ORDER_SUBMISSION_FAILED',
            'errorMessage': ('TCP idle-timeout encountered on connection to [ops-lb.tcsbank.ru:3305], '
                             'no bytes passed in the last 180 seconds')
        },
        'paymentErrors': []
    }
    correlation_id = bundle.remote_id_parsed.correlation_id_submit

    with response_mock([
        f'GET {BASE_URL}self-employed/payment-registry/submit/result/?correlationId={correlation_id} -> 200 :'
        f'{json.dumps(sign_result)}',

    ]
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_exported_h2h

    # снова запрашиваем проведение реестра
    with response_mock(
        f'POST {BASE_URL}self-employed/payment-registry/submit -> 200 : {{"correlationId": "{bundle.uuid}"}}'
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_signed

    sign_result = {
        'paymentRegistryId': 12,
        'status': 'ACCEPTED',
        'paymentErrors': []
    }
    correlation_id = bundle.remote_id_parsed.correlation_id_submit

    # утверждение и отправка реестра на оплату
    with response_mock([
        # получили подтверждение, что реестр готов к оплате
        f'GET {BASE_URL}self-employed/payment-registry/submit/result/?correlationId={correlation_id} -> 200 :'
        f'{json.dumps(sign_result)}',

        # инициируем оплату
        f'POST {BASE_URL}payment/payment-registry/pay -> 201 :test',
        f'GET {BASE_URL}payment/{bundle.id} -> 200 :{json.dumps(sign_result)}',
    ]
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    remote_id = bundle.remote_id_parsed.id

    assert bundle.is_paid
    assert remote_id

    # реестр частично исполнен
    with response_mock(
        f'GET {BASE_URL}self-employed/payment-registry/{remote_id} -> 200 :' +
        str(json.dumps({
            'status': 'PART_EXEC',
            'companyAccountNumber': bundle.account.number,
            'paymentsCount': 2,
            'totalSum': float(bundle.summ),
            'payments': [
                {
                    'number': bundle.payments[0].number,
                    'status': 'EXECUTED',
                    'accountNumber': '40802678901234567890',
                },
                {
                    'number': bundle.payments[1].number,
                    'status': 'ACCEPTED',
                    'accountNumber': '40802678901234567890',
                }
            ]
        }))
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.is_paid
    assert bundle.payments[0].is_complete
    assert bundle.payments[1].is_processing

    # реестр исполнен полностью
    with response_mock(
        f'GET {BASE_URL}self-employed/payment-registry/{remote_id} -> 200 :' +
        str(json.dumps({
            'status': 'EXECUTED',
            'companyAccountNumber': '40816810800000122870',
            'loadDate': '2015-05-09T12:30',
            'paymentsCount': 2,
            'totalSum': 1000,
            'payments': [
                {
                  'number': bundle.payments[0].number,
                  'status': 'EXECUTED',
                  'accountNumber': '40802678901234567890',
                },
                {
                  'number': bundle.payments[1].number,
                  'status': 'EXECUTED',
                  'accountNumber': '40802678901234567890',
                }
            ]
        }))
    ):
        Tinkoff.automate_payments_sync()

    bundle.refresh_from_db()
    assert bundle.status == states.ACCEPTED_H2H
    assert bundle.payments[0].is_complete
    assert bundle.payments[1].is_complete

    Tinkoff.automate_payments_sync()

    payment_params1 = {
        'payout_type': selfempl, 't_name': 'Иванов Иван Иванович ',
        't_fio': 'Иванов|Иван|Иванович', 'oper_code': '12346'
    }
    bundles = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'}, payment_dicts=[payment_params, payment_params1], h2h=True
    )

    assert len(bundles) == 2


def test_sync_candidates(build_payment_bundle, response_mock):

    payment_params = {
        'payout_type': Payment.PAYOUT_TYPE_SELFEMPLOYED,
        't_name': 'Иванов Иван Иванович ',
        'ground': 'тестовое назначение',
        't_fio': 'Иванов|Иван|Иванович',
    }
    bundle1 = build_payment_bundle(
        associate=Tinkoff, account={'number': 'TECH000777'},
        payment_dicts=[payment_params], h2h=True
    )
    org = bundle1.org
    org.connection_id = 'eda'
    org.save()

    bundle2 = build_payment_bundle(
        associate=Tinkoff, account=bundle1.account,
        payment_dicts=[payment_params], h2h=True
    )

    for bundle in (bundle1, bundle2):
        bundle.status = states.EXPORTED_H2H
        bundle.sent = True
        bundle.remote_id_parsed.correlation_id_create = f'{uuid.uuid4()}'
        bundle.save()

    with response_mock([
        f'GET {BASE_URL}self-employed/payment-registry/create/result/?correlationId='
        f'{bundle1.remote_id_parsed.correlation_id_create} -> 429:'
        '{"errorMessage": "Слишком много запросов. Попробуйте позже", '
        '"errorCode": "TOO_MANY_REQUESTS", "errorId": "884289dfb0"}',

        f'GET {BASE_URL}self-employed/payment-registry/create/result/?correlationId='
        f'{bundle2.remote_id_parsed.correlation_id_create} -> 200:'
        '{"status": "QUEUED"}',

    ]):
        Tinkoff.automate_payments_sync()

    bundle1.refresh_from_db()
    bundle2.refresh_from_db()

    assert bundle1.is_exported_h2h
    assert bundle1.processing_notes == ''

    assert bundle2.is_exported_h2h
    assert bundle2.processing_notes == '[QUEUED] '
