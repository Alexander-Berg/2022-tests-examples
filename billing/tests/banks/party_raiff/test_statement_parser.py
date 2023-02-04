from decimal import Decimal

from bcl.banks.party_raiff.statement_downloader import RaiffStatementDownloaderFactor
from bcl.banks.registry import Raiffeisen, RaiffeisenSpb
from bcl.core.models import Direction, EXPORTED_H2H, ERROR, StatementPayment


def test_statement_parser_multi_account_with_payment_check(parse_statement_fixture, get_source_payment):

    def check_payment(payment, direction, doc_number, summ, t_acc, oebs_payment=None):
        assert payment.direction == direction
        assert payment.number == doc_number
        assert payment.summ == summ
        assert payment.info['03'] == t_acc
        assert payment.payment == oebs_payment

    source_payment = get_source_payment({
        'summ': '120029.2',
        'ground': 'Пополнение счета корпоративных карт 48105, 24/7. НДС не облагается',
        'status': EXPORTED_H2H,
        'f_acc': '40702810000000013449',
        'number': '4918'

    }, associate=Raiffeisen)

    data = parse_statement_fixture(
        'statement/many_acc_equal_payments.txt', associate=Raiffeisen,
        encoding='cp1251', acc=['40702810300001423589', '40702810000000013449', '40702810500000048105'], curr='RUB'
    )

    data.sort(key=lambda x: str(x[0].account))

    assert len(data) == 3
    register, payments = data[2]
    statement = register.statement
    assert statement.type == statement.TYPE_FINAL
    assert register.account.number == '40702810500000048105'
    assert register.is_valid
    assert len(payments) == 3

    check_payment(payments[0], Direction.IN, '4918', Decimal('120029.20'), '40702810000000013449')
    check_payment(payments[1], Direction.IN, '608819', Decimal('350000'), '30233810001000015035')
    check_payment(payments[2], Direction.IN, '5687', Decimal('480000'), '40702810000000013449')

    register, payments = data[0]
    assert register.account.number == '40702810000000013449'
    assert register.is_valid
    assert len(payments) == 2

    check_payment(payments[0], Direction.OUT, '4918', Decimal('120029.2'), '40702810500000048105', source_payment)
    check_payment(payments[1], Direction.OUT, '5687', Decimal('480000'), '40702810500000048105')

    register, payments = data[1]
    assert register.account.number == '40702810300001423589'
    assert register.is_valid
    assert len(payments) == 2

    check_payment(payments[0], Direction.OUT, '52438', Decimal('242'), '40101810045250010041')
    check_payment(payments[1], Direction.IN, '322187', Decimal('3456.05'), '70606810800983120819')


def test_statement_parser_invalid_multi_account(parse_statement_fixture):
    data = parse_statement_fixture(
        'statement/statement_many_acc_invalid.txt', associate=Raiffeisen,
        encoding='cp1251', acc=['40702810300001423589', '40702810000000013449', '40702810500000048105'], curr='RUB'
    )
    assert len(data) == 3
    register, payments = data[0]
    statement = register.statement
    assert statement.type == statement.TYPE_FINAL
    assert statement.status == ERROR
    assert statement.processing_notes == 'Ошибка сверки'


def test_statement_parser_multi_account(parse_statement_fixture):

    data = parse_statement_fixture(
        'statement/1c_statement_multi_account.txt', associate=Raiffeisen,
        encoding='cp1251', acc=['40702810800000042128', '40702810900000042125'], curr='RUB'
    )

    for register, payments in data:
        statement = register.statement
        assert statement.type == statement.TYPE_FINAL
        assert register.is_valid


def test_statement_parser_intraday(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement/1c_intraday.txt', associate=Raiffeisen, encoding='cp1251', acc='40702810000000013449', curr='RUB'
    )[0]

    statement = register.statement
    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid


def test_zero_statement_register_is_not_unknown(parse_statement_fixture):
    register, _ = parse_statement_fixture(
        'statement/statement_empty.xml', Raiffeisen, '40702978300000007893', 'RUB',
        encoding='utf-8')[0]

    assert register.is_valid
    assert not register.is_unknown


def test_statement_parser_intraday_1(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'statement/1c_intraday.txt', associate=Raiffeisen, encoding='cp1251', acc='40702810000000013449', curr='RUB'
    )[0]

    statement = register.statement
    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert register.associate_id == Raiffeisen.id

    register, payments = parse_statement_fixture(
        'statement/1c_intraday.txt', associate=RaiffeisenSpb,
        encoding='cp1251', acc='40702810000000013450',
        mutator_func=lambda text: text.replace('40702810000000013449', '40702810000000013450'), curr='RUB'
    )[0]

    statement = register.statement
    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert register.associate_id == RaiffeisenSpb.id


def test_parse_factor(get_assoc_acc_curr, run_task, response_mock, time_freeze, init_user, get_source_payment):
    init_user(robot=True)
    associate = Raiffeisen

    _, account, _ = get_assoc_acc_curr(associate, account='TECH12345')
    payment = get_source_payment({
        'number_src': 'bf1d1868-a98e-4ae5-976f-0c4e21e0d510',
        'f_acc': account.number
    }, associate=associate)

    with response_mock([
        # авторизация для факторинга
        'POST https://extest.openapi.raiffeisen.ru/token -> 200:'
        '{"id_token": "1111", "access_token": "2222"}',

        # потом собственно выписка
        'GET https://extest.openapi.raiffeisen.ru/payables-finance/orders/statement-by-date?statementDate=2022-07-12 '
        '-> 200: {"doctype": "statement_final", "items": [{"meta": {"account": "TECH12345", '
        '"statement_date": "2022-07-12", "created": "2022-07-12T06:53:37.433211"}, "payments": ['
        '{"payee": {"bank": {"bik": "044525700"},"inn": "7704357909","name": "ЯНДЕКС.МАРКЕТ"},'
        '"number": "bf1d1868-a98e-4ae5-976f-0c4e21e0d510", "amount": 152.0, '
        '"purpose": "Оплата по контракту 111111 от 31.12.2021", "status": 1, "dc": "D"},'
        '{"payee": {"bank": {"bik": "044525700"},"inn": "7704357909","name": "ЯНДЕКС.МАРКЕТ"},'
        '"number": "bf1d1868-a98e-4ae5-976f-0c4e21e0d510", "amount": 52.0, '
        '"purpose": "Входящий", "status": 1, "dc": "C"}'
        ']}]}',

    ]):
        with time_freeze('2022-07-13'):
            statements_factor = RaiffStatementDownloaderFactor.process(
                associate=Raiffeisen,
            )
            assert len(statements_factor) == 1
            statement = statements_factor[0]
            assert b'TECH12345' in statement.zip_raw

    run_task('process_statements')
    statement.refresh_from_db()
    register = statement.statementregister_set.first()
    assert register.is_valid
    assert register.payment_cnt == 2
    assert register.summ == Decimal('100')

    payment_proved: StatementPayment = register.statementpayment_set.first()
    assert payment_proved.number == 'bf1d1868-a98e-4ae5-976f-0c4e21e0d510'
    assert payment_proved.payment == payment
