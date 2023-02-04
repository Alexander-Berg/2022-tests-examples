import json
from datetime import timedelta, datetime
from decimal import Decimal

from django.utils import timezone

from bcl.banks.balance_getter import BalanceGetter, PresumableBalanceGetter
from bcl.banks.party_sber.registry_operator import SberbankSalaryRegistry
from bcl.banks.registry import Sber, Ing
from bcl.core.models import Account, states, Currency
from bcl.toolbox.utils import DateUtils


def test_update_accounts_balance(
        monitor_tester, get_proved, get_source_payment, django_assert_num_queries, time_shift):

    shift = 60 * 60 * 24 * 2

    def update_balance(payment_sum):

        with time_shift(shift, backwards=True):
            get_source_payment({'f_acc': acc_num1, 'status': states.BUNDLED, 'summ': Decimal(payment_sum)})
            get_source_payment({'f_acc': acc_num2, 'status': states.BUNDLED, 'summ': Decimal(payment_sum)})

            with django_assert_num_queries(5) as q_context:  # 5 запросов для любого количества счетов.
                errors = BalanceGetter.update_accounts_balance(notify=True)

            account.refresh_from_db()

        return errors

    def generate_account(acc_num, amount):

        with time_shift(shift, backwards=True):
            proved_pay = get_proved(
                Sber, acc_num=acc_num,
                register_kwargs={'closing_balance': Decimal(amount)},
                proved_pay_kwargs={'summ': Decimal(amount)},
            )[0]
            register = proved_pay.register
            register.statement_date = DateUtils.yesterday()
            register.save()

            register.sum_check(
                payments_in=[proved_pay],
                payments_out=[],
                balance_opening=0,
                balance_closing=amount)

            assert register.is_valid

            account = Account.objects.get(number=acc_num)

            account.notify_balance_min = Decimal('1000')
            account.notify_balance_to = 'some@some.where'
            account.save()

            assert not account.balance
            assert account.register_final == register

        return account

    acc_num1 = '1007009'
    acc_num2 = 'additional'
    amount = '100000'

    account = generate_account(acc_num1, amount)
    generate_account(acc_num2, amount)

    monitor_messages = monitor_tester('bcl.banks.balance_getter.MinimalBalanceMonitor')
    errors = update_balance('5')
    assert account.balance == Decimal('99995')
    assert not errors
    assert not monitor_messages

    errors = update_balance(amount)
    assert account.balance == Decimal('-5.00')
    assert not errors
    assert len(monitor_messages) == 2

    errors = update_balance('20')
    assert account.balance == Decimal('-25.00')
    assert not errors
    # По-прежнему два сообщения (по кол-ву счетов): после того, как баланс единожды перешагнул границу
    # в последующие разы оповещения не отправляются.
    assert len(monitor_messages) == 2


def test_statement_with_payment_in_balance(read_fixture, get_statement, get_assoc_acc_curr, get_source_payment):
    associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001005386')
    BalanceGetter.update_accounts_balance()
    account.refresh_from_db()
    assert not account.balance

    payment_sum = Decimal('115')
    get_source_payment(
        {'f_acc': account.number, 'status': states.EXPORTED_ONLINE, 'summ': Decimal(payment_sum)},
        associate=Ing.id)
    get_source_payment(
        {'f_acc': account.number, 'status': states.EXPORTED_ONLINE, 'currency_id': Currency.USD},
        associate=Ing.id)

    statement = get_statement(
        read_fixture('ing_final.txt').replace(
            b'170127', f"{(datetime.now() - timedelta(days=2)).strftime('%y%m%d')}".encode()
        ),
        Ing.id
    )

    parser = Ing.statement_dispatcher.get_parser(statement)

    parser.validate_permissions([account], final=True)
    parser.process()
    statement_end_amount = Decimal('8279239.85')
    BalanceGetter.update_accounts_balance()
    account.refresh_from_db()
    acc_turnover = json.loads(account.turnover)

    assert str(payment_sum) in acc_turnover[str(Currency.RUB)]
    assert '152' in acc_turnover[str(Currency.USD)]
    assert account.balance == statement_end_amount - payment_sum


def test_statement_without_payment_in_balance(read_fixture, get_statement, get_assoc_acc_curr, get_source_payment):

    associate, account, _ = get_assoc_acc_curr(Ing.id, account='40702810300001005386')
    BalanceGetter.update_accounts_balance()

    account.refresh_from_db()
    assert not account.balance

    get_source_payment(
        {'f_acc': account.number, 'status': states.BUNDLED},
        associate=Ing.id)

    statement = get_statement(
        read_fixture('ing_final.txt').replace(
            b'170127', str.encode((datetime.now() + timedelta(days=1)).strftime('%y%m%d'))),
        Ing.id)

    statement_end_amount = Decimal('8279239.85')

    parser = Ing.statement_dispatcher.get_parser(statement)
    parser.validate_permissions([account], final=True)
    parser.process()

    BalanceGetter.update_accounts_balance()

    account.refresh_from_db()
    assert account.balance == statement_end_amount


def test_presumable_balance_getter(
    get_assoc_acc_curr, get_statement, fake_statements, init_user, time_freeze,
    get_salary_registry, django_assert_num_queries
):

    init_user(robot=True)

    now = timezone.now()

    def refresh():
        for account in accounts:
            account.refresh_from_db()

    associate, account1, _ = get_assoc_acc_curr(Sber, account='1')
    fake_statements(associate, account=account1, parse=True)

    _, account2, _ = get_assoc_acc_curr(associate, account='2')
    fake_statements(associate, account=account2, parse=True)

    _, account3, _ = get_assoc_acc_curr(associate, account='3')
    fake_statements(associate, account=account3, parse=True)

    accounts = [account1, account2, account3]

    get_salary_registry(
        associate, SberbankSalaryRegistry,
        contract_account=account3.number,
        employees=[
            {
                'record_id': '1',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '40817810718351353196',
                'amount': '1200.0'
            },
            {
                'record_id': '2',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '40817810360311219953',
                'amount': '800.0'
            },
            {
                'record_id': '3',
                'first_name': 'a',
                'last_name': 'b',
                'patronymic': 'c',
                'currency_code': 'RUB',
                'personal_account': '40817810340013194306',
                'amount': '11568.02'
            }
        ]
    )

    refresh()
    account1.register_final.statement_date = now - timedelta(days=544)
    account1.register_final.save()

    account2.register_final.statement_date = now - timedelta(days=546)
    account2.register_final.save()

    account3.register_final.statement_date = now - timedelta(days=544)
    account3.register_final.save()

    refresh()
    with django_assert_num_queries(6) as q_context:
        # бОльшее количество запросов обусловлено характером теста.
        # Здесь проверяется функция .sums без связки счетов с регистрами.
        # В обычном случае счета подготавливаются в функции update_accounts_balance, и лишних запросов не происходит.
        sums = PresumableBalanceGetter(accounts=accounts).sums

    assert sums == {
        account1: (Decimal('-608.000000'), {643: Decimal('304')}),
        account2: (Decimal('-304.000000'), {}),
        account3: (Decimal('-14176.020000'), {643: Decimal('304')}),
    }
