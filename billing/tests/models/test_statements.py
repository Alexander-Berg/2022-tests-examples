from datetime import datetime
from decimal import Decimal
from functools import partial
from uuid import uuid4

from bcl.banks.common.statement_parser.base import PaymentsMatcher
from bcl.banks.registry import Sber
from bcl.core.models import Direction
from bcl.core.models import StatementRegister, Account, Statement, StatementPayment, states
from bcl.toolbox.utils import DateUtils


def test_mds(get_statement, mock_mds):

    statement = get_statement(b'inner', associate=Sber)
    assert statement.mds_realm == 'statements'
    assert statement.mds_ident == f'statements/{statement.id}'
    meta = statement.mds_meta
    assert 'id' in meta
    assert 'dt' in meta
    assert statement.mds_path is None
    assert statement.raw.startswith(b'PK')

    statement.mds_move()
    assert statement.mds_path is not None
    assert statement.raw == b''

    data = statement.mds_fetch()
    assert data.startswith(b'PK')
    assert statement.raw == data
    assert statement.mds_path

    assert statement.zip_raw == b'inner'

    data = statement.mds_fetch(restore=True)
    assert data.startswith(b'PK')
    assert statement.raw == data
    assert statement.mds_path is None


def test_register_sum_check(get_proved):

    proved_pay = get_proved(
        Sber, register_kwargs=dict(status=StatementRegister.STATUS_UNKNOWN))[0]

    accounts = Account.objects.all()
    assert len(accounts) == 1

    account = accounts[0]
    assert account.register_final is None

    register = proved_pay.register

    assert not register.is_valid

    register.sum_check(
        payments_in=[proved_pay],
        payments_out=[],
        balance_opening=Decimal('0'),
        balance_closing=Decimal('123'))

    assert register.is_valid

    account.refresh_from_db()

    assert account.register_final == register


def test_statement_scheduled_order(get_statement):
    for i in range(10):
        get_statement(str(i), Sber.id)

    for i in range(10):
        with Statement.scheduled(loud=True) as st:
            assert st.zip_raw == bytes(str(i), 'ascii')


def test_statement_scheduled_group_lock(get_statement):
    group = uuid4()
    st1 = get_statement('group_text1', Sber.id, group=group)
    st2 = get_statement('group_text2', Sber.id, group=group, status=Statement.state_processing_delayed)
    get_statement('text1', Sber.id)

    assert not st2.is_processing_ready

    with Statement.scheduled(loud=True) as st:
        assert st.zip_raw == b'group_text1'

    # После обработки первого элемента группы, к обработке назначается следующий в группе.
    st1.refresh_from_db()
    st2.refresh_from_db()
    assert st1.is_processing_done
    assert st2.is_processing_ready

    with Statement.scheduled(loud=True) as st:
        assert st.zip_raw == b'group_text2'

    with Statement.scheduled(loud=True) as st:
        assert st.zip_raw == b'text1'

    with Statement.scheduled(loud=True) as st:
        assert not st


def test_statement_retry(get_statement, monkeypatch):

    monkeypatch.setattr('bcl.core.models.Statement.get_scheduling_retry_delay', lambda _: 0)

    statement = get_statement('wrong text', Sber.id)

    while True:
        with Statement.scheduled() as st:
            if not st:
                break
            st.process()

    statement.refresh_from_db()
    assert statement.processing_retries == 1


def test_cleanup_stale(time_shift, get_statement):

    assert not Statement.cleanup_stale()

    def build(*, set_status=True):
        """
        :rtype: Statement
        """
        statement = get_statement('statement', Sber.id)
        statement.schedule()

        if set_status:
            statement.set_status(statement.state_processing_now)

        return statement

    statement1 = build(set_status=False)
    statement2 = build()
    statement3 = build()

    with time_shift(4 * 60 * 60):
        statements = Statement.cleanup_stale()
        assert len(statements) == 2

        statement1.refresh_from_db()
        statement2.refresh_from_db()
        statement3.refresh_from_db()

        for statament in (statement1, statement2, statement3):
            assert statament.status != statament.state_processing_now


def test_statement_delete(get_statement, get_proved, get_source_payment, get_assoc_acc_curr):
    acc_num = '00000000000000000'

    get_assoc_acc_curr(Sber, account=acc_num)

    pay1 = get_proved(Sber, acc_num=acc_num, register_kwargs={'statement_date': datetime(2018, 2, 1)})[0]

    statement = get_statement('statement', Sber.id)
    pay2 = get_proved(
        Sber, acc_num=acc_num,
        register_kwargs={'statement_date': datetime(2018, 2, 2), 'statement': statement})[0]

    pay2_oebs = get_source_payment()
    pay2_oebs.statement_payment = pay2
    pay2_oebs.save()

    pay2.payment= pay2_oebs
    pay2.save()

    account = pay1.register.account
    # Проставим второй регистр финальным. Чтобы после удаления выписки на его место встал первый.
    account.register_final = pay2.register
    account.save()

    statement.deep_delete()

    account.refresh_from_db()
    pay2_oebs.refresh_from_db()

    assert StatementRegister.objects.count() == 1
    assert StatementPayment.objects.count() == 1
    assert not pay2_oebs.was_held
    assert account.register_final == pay1.register


class TestPaymentsMatcher:

    def test_filter_function(self, get_proved):

        proved_pays = get_proved(associate=Sber, proved_pay_kwargs=[
            {'direction': Direction.OUT},
            {'direction': Direction.OUT, 'number': '44'},
        ])

        matcher = PaymentsMatcher(proved_pays, filter_func=lambda register, proved: proved.number == '44')
        filtered = matcher._filter_basic(matcher.proved_pays)
        assert len(filtered) == 1
        assert filtered[0] == proved_pays[1]

    def test_exact_linking(self, get_proved, get_source_payment):

        pays = [
            get_source_payment({
                'status': states.EXPORTED_ONLINE,
                'number': 555,
                'summ': Decimal('123'),
            }, associate=Sber) for _ in range(2)
        ]

        proved_2 = get_proved(associate=Sber, proved_pay_kwargs=[
            {'number': 555, 'direction': Direction.OUT},
        ], acc_num='40702810800000007671')
        proved_2[0].payment_id_exact = pays[0].id

        result = PaymentsMatcher.run(proved_2)
        proved_pay, candidates = result.popitem()
        assert len(candidates) == 1

    def test_basic(self, get_proved):

        # Проверка базовой фильтрации.
        proved_pays = get_proved(associate=Sber)
        assert PaymentsMatcher.run(proved_pays) == {}

        # Проверка отсутствия кандидатов.
        proved_pays = get_proved(associate=Sber, proved_pay_kwargs=[{'direction': Direction.OUT},])
        assert PaymentsMatcher.run(proved_pays) == {}

    def test_salary_only(self, get_source_payment, get_proved):
        # Проверка короткого пути, при наличии исключительно зарплатных платежей.
        pay = get_source_payment({'salary_id': '1020', 'status': states.EXPORTED_ONLINE}, associate=Sber)
        proved = get_proved(associate=Sber, proved_pay_kwargs=[
            {
                'direction': Direction.OUT,
                'number': '44',
                'summ': pay.summ,
                'info': {'06': 'зарплата по реестру №1020'}
            },
        ])
        result = PaymentsMatcher.run(proved, sync_status=True, with_salary=True)
        assert len(result) == 1
        result_values = list(result.values())
        assert len(result_values[0]) == 1

    def test_complex_multidate(self, get_source_payment, get_proved):

        get_src = partial(get_source_payment, associate=Sber)

        date1 = DateUtils.days_from_now(9).date()

        pay1 = get_src({'date': date1})
        pay2 = get_src({'date': DateUtils.days_from_now(19)})  # Охват +10 дней в прошлое.

        proved = get_proved(associate=Sber, proved_pay_kwargs=[
            {
                'direction': Direction.OUT,
                'number': pay1.number,
                'summ': pay1.summ,
                'date': date1,
            },
            {
                'direction': Direction.OUT,
                'number': pay2.number,
                'summ': pay2.summ,
                'date': DateUtils.days_from_now(11).date(),
            },
        ], acc_num='40702810800000007671')

        result = PaymentsMatcher.run(proved)

        assert len(result) == 2
        for idx, (proved, candidates) in enumerate(result.items()):
            assert len(candidates) == 1

            if idx == 0:
                assert proved.date == candidates[0].date

            else:
                assert proved.date != candidates[0].date

    def test_complex(self, get_source_payment, get_proved, django_assert_max_num_queries):

        get_src = partial(get_source_payment, associate=Sber)

        pay1 = get_src()
        pay2 = get_src()  # Свяжется.
        pay3 = get_src({'number': pay1.number})
        pay4 = get_src()
        get_src({'number': pay4.number, 'currency_id': Decimal('20.20')})
        get_src({'number': pay4.number, 'summ': Decimal('10.20')})
        pay7 = get_src({'status': states.COMPLETE})  # Свяжется.
        pay8 = get_src({'salary_id': '1020', 'status': states.EXPORTED_ONLINE})  # Свяжется зарплатный.
        get_src({'salary_id': '1040', 'status': states.EXPORTED_ONLINE})  # Зарплатный. Не свяжется из-за суммы.

        proved = get_proved(associate=Sber, proved_pay_kwargs=[
            {},  # Не исходящий.
            {  # Свяжется.
                'direction': Direction.OUT,
                'number': pay2.number,
                'summ': pay2.summ,
                'problem': 'непроблема',
                'status': states.DECLINED_BY_BANK,
            },
            {'direction': Direction.OUT, 'number': 'falsy345'},  # Номер не поддерживается.
            {'direction': Direction.OUT, 'number': pay4.number},  # Не пройдёт по сумме.
            {'direction': Direction.OUT, 'number': pay3.number},  # Нет прямого соответствия.
            {  # Свяжется.
                'direction': Direction.OUT,
                'number': pay7.number,
                'summ': pay7.summ,
                'status': states.ERROR,
            },
            {  # Свяжется зарплатный.
                'direction': Direction.OUT,
                'number': '44',
                'summ': pay8.summ,
                'info': {'06': 'зарплата по реестру №1020'}
            },
            {  # Зарплатный без исходного.
                'direction': Direction.OUT,
                'number': '45',
                'info': {'06': 'зарплата по реестру №1040'}
            },
            {
                'direction': Direction.OUT,
                'number': pay4.number,
                'summ': pay4.summ,
            },

        ], acc_num='40702810800000007671')

        with django_assert_max_num_queries(10):
            # begin 3, select 2, update 4
            result = PaymentsMatcher.run(proved, sync_status=True, with_salary=True)

        assert len(result) == 7
        result_values = list(result.values())
        assert len(result_values[0]) == 0
        assert len(result_values[1]) == 1
        assert len(result_values[2]) == 0
        assert len(result_values[3]) == 2
        assert len(result_values[4]) == 1
        assert len(result_values[5]) == 1
        assert len(result_values[6]) == 0

        proved_linked_2 = proved[1]
        proved_linked_2.refresh_from_db()
        assert proved_linked_2.payment_id == pay2.id
        pay2.refresh_from_db()
        assert pay2.status == states.DECLINED_BY_BANK  # Статус перенесён из сверочного.
        assert pay2.processing_notes == 'непроблема'

        proved_linked_7 = proved[5]
        proved_linked_7.refresh_from_db()
        assert proved_linked_7.payment_id == pay7.id
        pay7.refresh_from_db()
        assert pay7.status == states.COMPLETE  # Статус НЕ перенесён из сверочного.

        proved_salary = proved[6]
        proved_salary.refresh_from_db()
        assert proved_salary.payment_id == pay8.id
