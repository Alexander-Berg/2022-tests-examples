from datetime import datetime

from bcl.banks.registry import Raiffeisen
from bcl.core.models import states, StatementPayment


class TestPaymentCeator:

    def test_create_tax_payment(self, get_payment_bundle, get_source_payment):
        payment = get_source_payment()
        bundle = get_payment_bundle([payment])

        creator = Raiffeisen.payment_dispatcher.get_creator(bundle)
        compiled = creator.create_bundle()

        assert 'ПоказательДаты=12-11-2017' in compiled


def test_salary_statements(parse_statement_fixture, get_source_payment):
    payroll_num = '279131'

    source_payment = get_source_payment({
        'payroll_num': payroll_num,
        'summ': '15.00',
        'status': states.EXPORTED_H2H,

    }, associate=Raiffeisen)

    Raiffeisen.register_payment_hook_before_save(source_payment)
    source_payment.save()

    assert source_payment.salary_id == payroll_num

    def simulate_salary_pay(text):
        text = text.replace(
            'Налог на доходы физических лиц',
            '{VO70060} Перечисление зарплаты за октябрь 2017 г. согласно платежной ведомости №279131 ')
        return text

    register, payments = parse_statement_fixture(
        'raiff_salary_payments.txt', Raiffeisen, '40702810500001400742', 'RUB',
        mutator_func=simulate_salary_pay,
        encoding='cp1251')[0]

    payment = payments[0]
    source_payment.refresh_from_db()

    assert source_payment.statementpayment_set.all()[0].number == payment.number


def test_statement_parser_intraday(parse_statement_fixture):
    statement_params = dict(
        associate=Raiffeisen, encoding=Raiffeisen.statement_dispatcher.parsers[0].encoding,
        acc='40702810000000013449', curr='RUB'
    )

    def replace_date(text):
        return text.replace('19.07.2018', datetime.now().strftime('%d.%m.%Y'))

    register, payments = parse_statement_fixture(
        'raiff_intraday.txt', mutator_func=replace_date, **statement_params
    )[0]
    statement = register.statement

    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert len(payments) == 33
    assert len(StatementPayment.objects.all()) == 33
