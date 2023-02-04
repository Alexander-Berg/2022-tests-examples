from datetime import timedelta, datetime
from functools import partial

import pytest
from django.conf import settings
from freezegun import freeze_time

from bcl.banks.monitors import StatementFinalsMonitor, StatementParseMonitor, BundleCreateMonitor, \
    DeclinedPaymentsMonitor, ProblemsMonitor, StatementZeroIntradayMonitor, \
    StatementIntradayMonitor, \
    StatementInvalidBalanceMonitor, \
    SuspiciousPaymentsMonitor
from bcl.banks.registry import Raiffeisen, AlfaKz, YooMoney, Sber, Ing, Unicredit
from bcl.banks.tasks import monitor_statement
from bcl.core.models import states, Direction, Statement, StatementRegister
from bcl.toolbox.utils import DateUtils


mark_xfail = pytest.mark.xfail(
    condition=settings.ARCADIA_RUN,
    reason='В тестах А mailoutbox может быть очищен конкурентом')


@mark_xfail
def test_statement_monitor_final(read_fixture, get_statement, get_assoc_acc_curr, mailoutbox):
    accounts = {}

    for i in range(10):
        num = str(i)
        _, acc, _ = get_assoc_acc_curr(Raiffeisen, account=str(num).zfill(20))
        accounts[num] = acc

    date = DateUtils.yesterday().date()

    for i in range(5):
        # создание финальных выписок для первых 5-ти счетов
        statement = get_statement('qwerty', Raiffeisen)
        StatementRegister(
            statement=statement, account=accounts[str(i)], statement_date=date,
            intraday=0, status=StatementRegister.STATUS_VALID, associate_id=Raiffeisen.id
        ).save()

    monitor = StatementFinalsMonitor({'associate': Raiffeisen, 'date_from': date})

    result = monitor.check()

    assert set(account.number for account in result) == {
        '00000000000000000005',
        '00000000000000000006',
        '00000000000000000007',
        '00000000000000000008',
        '00000000000000000009'
    }

    message = monitor.compose(result)

    assert (
        '5 fakedorg RUB\n6 fakedorg RUB\n7 fakedorg RUB\n8 fakedorg RUB\n9 fakedorg RUB\n'
        in message.replace('0000000000000000000', '')
    )

    monitor.run()
    assert len(mailoutbox) == 1


@mark_xfail
def test_statement_invalid_balance_monitor(read_fixture, get_statement, get_assoc_acc_curr, mailoutbox):
    _, acc, _ = get_assoc_acc_curr(Unicredit, account='1298376')

    statement = get_statement('qwerty', Unicredit)
    StatementRegister(
        statement=statement, account=acc, statement_date=DateUtils.yesterday().date(),
        intraday=0, status=StatementRegister.STATUS_ERROR, associate_id=Unicredit.id
    ).save()

    monitor = StatementInvalidBalanceMonitor(
        {'associate': Unicredit, 'statement': statement, 'balance_prev_closing': 11, 'balance_opening': 12}
    )

    monitor.run()
    assert len(mailoutbox) == 1


@mark_xfail
def test_monitor_statement_task_h2h(mailoutbox, get_assoc_acc_curr):

    get_assoc_acc_curr(Raiffeisen, account='12345678901234567890')
    get_assoc_acc_curr(Unicredit, account='789012')

    with freeze_time('2018-12-12'):  # предыдущий день не праздник
        monitor_statement('test')

    assert len(mailoutbox) == 2
    assert Raiffeisen.title in mailoutbox[0].subject  # Raiff
    assert 'Яндекс (№' in mailoutbox[1].subject  # Unic
    with freeze_time('2018-12-15'):  # текущий день - выходной, предыдущий - рабочий
        monitor_statement('test')

    assert len(mailoutbox) == 4
    assert '2018-12-14' in mailoutbox[2].subject  # Raiff
    assert '2018-12-14' in mailoutbox[3].subject  # Unic

    with freeze_time('2018-12-16'):  # текущий день - выходной, предыдущий - выходной
        monitor_statement('test')

    assert len(mailoutbox) == 4


@mark_xfail
def test_monitor_statement_task_manual(mailoutbox, get_assoc_acc_curr):

    get_assoc_acc_curr(Sber, account='1'*20)
    get_assoc_acc_curr(Ing, account='2'*20)

    with freeze_time('2018-12-12'):  # предыдущий день не праздник
        monitor_statement('test', h2h=False)

    assert len(mailoutbox) == 1
    assert Sber.title in mailoutbox[0].subject

    with freeze_time('2018-12-10'):  # предыдущий день выходной
        monitor_statement('test', h2h=False)

    assert len(mailoutbox) == 2
    assert '2018-12-09' in mailoutbox[1].subject


@mark_xfail
def test_monitor_statement_intraday(mailoutbox, get_statement, mocker, get_assoc_acc_curr, time_shift):

    mocker.patch('time.sleep', lambda x: None)

    _, acc, _ = get_assoc_acc_curr(Unicredit, account='123456')

    monitor = StatementIntradayMonitor({'associate': Unicredit, 'accounts': [acc.number]})

    monitor.run()
    assert len(mailoutbox) == 1

    assert Unicredit.title in mailoutbox[0].subject
    assert 'внутридневные' in mailoutbox[0].subject
    assert '123456 fakedorg RUB\n' in mailoutbox[0].body

    with time_shift(2*60*60, backwards=True):
        statement = get_statement('qwerty', Unicredit, final=False)
        StatementRegister(
            statement=statement, account=acc, statement_date=datetime.utcnow(),
            intraday=1, status=StatementRegister.STATUS_VALID, associate_id=Unicredit.id,
        ).save()

    monitor.run()
    assert len(mailoutbox) == 1


@mark_xfail
def test_monitor_zero_statement_intraday_triggered(mailoutbox, get_statement, mocker, get_assoc_acc_curr):

    mocker.patch('time.sleep', lambda x: None)

    _, acc, _ = get_assoc_acc_curr(Unicredit, account='123456')

    statement = get_statement('qwerty', Unicredit, final=False)
    StatementRegister(
        statement=statement, account=acc, statement_date=datetime(2018, 12, 5, 12, 30, 0),
        intraday=1, payment_cnt=0, status=StatementRegister.STATUS_VALID, associate_id=Unicredit.id
    ).save()

    statement = get_statement('qwerty', Unicredit, final=False)
    StatementRegister(
        statement=statement, account=acc, statement_date=datetime(2018, 12, 5, 13, 30, 0),
        intraday=1, payment_cnt=0, status=StatementRegister.STATUS_VALID, associate_id=Unicredit.id
    ).save()

    monitor = StatementZeroIntradayMonitor({'associate': Unicredit, 'accounts': ['123456']})

    monitor.run()
    assert len(mailoutbox) == 1

    assert Unicredit.title in mailoutbox[0].subject
    assert 'нулевая внутридневная выписка' in mailoutbox[0].subject
    assert '123456 fakedorg RUB\n' in mailoutbox[0].body


@mark_xfail
def test_monitor_zero_statement_intraday_not_triggered(mailoutbox, get_statement, mocker, get_assoc_acc_curr):

    mocker.patch('time.sleep', lambda x: None)

    def create_register(date, payment_cnt):
        statement = get_statement('qwerty', Unicredit)
        StatementRegister(
            associate_id=Unicredit.id,
            statement=statement, account=acc, statement_date=date,
            intraday=1, payment_cnt=payment_cnt, status=StatementRegister.STATUS_VALID
        ).save()

    _, acc, _ = get_assoc_acc_curr(Unicredit, account='123456')

    create_register(datetime(2018, 12, 5, 12, 30, 0), 0)
    create_register(datetime(2018, 12, 5, 13, 30, 0), 10)
    create_register(datetime(2018, 12, 5, 14, 30, 0), 0)

    monitor = StatementZeroIntradayMonitor({'associate': Unicredit, 'accounts': ['123456']})

    monitor.run()

    assert len(mailoutbox) == 0


@mark_xfail
def test_monitor_statement_intraday_holiday(mailoutbox, get_statement, mocker, get_assoc_acc_curr):

    from freezegun import freeze_time

    mocker.patch('time.sleep', lambda x: None)

    _, acc, _ = get_assoc_acc_curr(Unicredit, account='123456')

    def create_register(date, payment_cnt):
        statement = get_statement('qwerty', Unicredit)
        StatementRegister(
            associate_id=Unicredit.id,
            statement=statement, account=acc, statement_date=date,
            intraday=1, payment_cnt=payment_cnt, status=StatementRegister.STATUS_VALID
        ).save()

    with freeze_time('2018-12-23'):  # предыдущий день не праздник
        create_register(datetime(2018, 12, 23, 12, 30, 0), 0)
        create_register(datetime(2018, 12, 23, 13, 30, 0), 0)
        monitor_zero = StatementZeroIntradayMonitor({'associate': Unicredit, 'accounts': ['123456']})
        monitor_intraday = StatementZeroIntradayMonitor({'associate': Unicredit, 'accounts': ['123456']})
        monitor_zero.run()
        monitor_intraday.run()

    assert len(mailoutbox) == 0


def test_statement_parse_monitor(mocker):

    func_send_data = mocker.patch('bcl.toolbox.notifiers.GraphiteNotifier.send_data')

    with StatementParseMonitor.timeit(Sber, type=lambda: 'I') as mon_ctx:
        mon_ctx.items_count = 30

    assert func_send_data.call_count == 2  # Отправили время и скорость.

    arg = func_send_data.call_args_list[0][0][2]
    assert '.balalayka.bank.sber.s_type.I.metric.parsing_time' in arg

    with StatementParseMonitor.timeit(Sber, type=lambda: 'A'):
        pass

    assert func_send_data.call_count == 3  # Отправили только время.


def test_statement_bundle_monitor(mocker):

    func_send_data = mocker.patch('bcl.toolbox.notifiers.GraphiteNotifier.send_data')

    with BundleCreateMonitor.timeit(Sber) as mon_ctx:
        mon_ctx.items_count = 30

    assert func_send_data.call_count == 2  # Отправили время и скорость.

    arg = func_send_data.call_args_list[0][0][2]
    assert '.balalayka.bank.sber.bundle.metric.time' in arg

    with BundleCreateMonitor.timeit(Sber):
        pass

    assert func_send_data.call_count == 3  # Отправили только время.


@mark_xfail
def test_declined_payments_monitor(mailoutbox, get_source_payment_mass, init_user):
    payments = get_source_payment_mass(5, Sber, attrs={'status': states.DECLINED_BY_BANK, 'user': init_user('idlesign')})

    payments[0].status = states.EXPORTED_H2H
    payments[0].save()

    dummy = init_user('dummy')

    for idx in range(1, 3):
        payments[idx].user = dummy
        payments[idx].save()

    DeclinedPaymentsMonitor.invoke(associate=Sber, payments=payments)

    assert len(mailoutbox) == 2  # 2 получателя

    assert mailoutbox[0].alternatives[0][0].count('<tr>') == 3  # Два платежа + заголовок
    assert mailoutbox[1].alternatives[0][0].count('<tr>') == 3  # Два платежа + заголовок


@mark_xfail
def test_problems_monitor(sitemessages, get_source_payment_mass, get_proved, init_user, get_assoc_acc_curr):

    payments = get_source_payment_mass(
        3, AlfaKz, attrs={'status': states.EXPORTED_ONLINE, 'statement_payment': None, 'user': init_user('idlesign')})

    user = init_user('dummy')
    payments[1].user = user
    payments[1].save()

    _, acc, _ = get_assoc_acc_curr(AlfaKz, account='acc1000')
    acc.org.problem_tracking = True
    acc.org.save()

    statement = Statement(associate_id=AlfaKz.id, user=user)
    statement.save()

    register = StatementRegister()
    register.statement = statement
    register.account = acc
    register.associate_id = AlfaKz.id
    register.save()

    pay1 = get_proved(Raiffeisen, proved_pay_kwargs={'direction': Direction.OUT})[0]
    pay1.set_info(name='somename')
    pay1.register = register
    pay1.save()

    ProblemsMonitor.invoke(date_till=datetime.now() + timedelta(days=2))

    messages = sitemessages()
    assert len(messages) == 1
    body = messages[0].context['stext_']

    assert '<h3>@idlesign</h3>' in body
    assert '>1<' in body
    assert '<h3>@dummy</h3>' in body
    assert 'Кинопортал' in body

    assert body.count('<tr>') == 6


problems_get_for_payments_oebs = partial(
    ProblemsMonitor.get_for_payments,
    date_from=DateUtils.yesterday(), date_till=datetime.now() + timedelta(days=2))


class TestProblemOEBS:

    def test_problems_exists(self, get_source_payment, get_payment_bundle):
        payment = get_source_payment({'status': states.EXPORTED_ONLINE, 'statement_payment': None}, associate=AlfaKz)
        get_payment_bundle([payment])
        problem_payments = problems_get_for_payments_oebs()
        assert payment in problem_payments

    def test_problems_not_exists(self, get_source_payment, get_payment_bundle):
        payment = get_source_payment({'status': states.COMPLETE, 'statement_payment': None}, associate=AlfaKz)
        get_payment_bundle([payment])
        problem_payments = problems_get_for_payments_oebs()
        assert payment not in problem_payments

    def test_yoomoney_problems_not_exists(self, get_source_payment, get_payment_bundle):
        payment = get_source_payment({'status': states.BUNDLED, 'statement_payment': None}, associate=YooMoney)
        get_payment_bundle([payment])
        problem_payments = problems_get_for_payments_oebs()
        assert payment not in problem_payments

    def test_firm_without_problems(self, get_source_payment, get_payment_bundle, get_assoc_acc_curr):
        _, acc, _ = get_assoc_acc_curr(AlfaKz.id)
        org = acc.org
        org.problem_tracking = False
        org.save()
        payment = get_source_payment(
            {'status': states.BUNDLED, 'statement_payment': None, 'f_acc': acc.number},
            associate=AlfaKz)

        get_payment_bundle([payment], associate=AlfaKz, account=acc)
        problem_payments = problems_get_for_payments_oebs()
        assert payment not in problem_payments


def test_suspicious_payments_monitoring(get_source_payment):
    """Проверяем формирование выборки подозрительных платежей для соломона."""
    for status in (states.OTHER, states.PROCESSING):
        payment = get_source_payment()
        payment.status = status
        payment.save()

    check_result = SuspiciousPaymentsMonitor({}).check()
    _, data = SuspiciousPaymentsMonitor({}).compose(check_result)[0]

    assert len(data) == 2
    metric_processing = SuspiciousPaymentsMonitor.notifier_cls.metric(
        'suspicious_payments', {
            'status': 'processing',
            'account_number': '40702810800000007671',
            'service_name': 'oebs',
            'bank_name': 'sber'
        }, 1)

    metric_other = SuspiciousPaymentsMonitor.notifier_cls.metric(
        'suspicious_payments', {
            'status': 'other',
            'account_number': '40702810800000007671',
            'service_name': 'oebs',
            'bank_name': 'sber'
        }, 1)

    assert data[0]==metric_processing
    assert data[1]==metric_other
