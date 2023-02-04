from decimal import Decimal

from bcl.banks.registry import Sber, Raiffeisen
from bcl.core.models import Direction, SwiftIdentificationCode
from bcl.toolbox.utils import DateUtils


def test_allday(api_client, get_assoc_acc_curr, get_proved, django_assert_num_queries):

    _, account1, _ = get_assoc_acc_curr(Raiffeisen, account='10')
    _, account2, _ = get_assoc_acc_curr(Sber, account='20')

    old = [{'info': {'01': 'old'}}]

    get_proved(associate=Raiffeisen, acc_num=account1.number, proved_pay_kwargs=(old * 2))
    get_proved(
        associate=Raiffeisen, acc_num=account1.number,
        register_kwargs={'opening_balance': Decimal('10.15'), 'closing_balance': Decimal('22.23')},
        proved_pay_kwargs=[{}, {}])

    get_proved(associate=Sber, acc_num=account2.number, proved_pay_kwargs=(old * 2))
    get_proved(associate=Sber, acc_num=account2.number, proved_pay_kwargs=(old * 3))
    get_proved(associate=Sber, acc_num=account2.number, proved_pay_kwargs=([{}] * 4))

    with django_assert_num_queries(4):
        response = api_client.get(
            f'/api/statements/?accounts=["10","20"]&on_date={DateUtils.format_nice(with_time=False)}')

    assert response.ok
    response = response.json
    assert not response['errors']

    statements = response['data']['items']
    assert len(statements) == 2

    assert statements[0]['balance_opening'] == '10.15'
    assert statements[0]['balance_closing'] == '22.23'

    payments = statements[0]['payments']
    assert len(payments) == 2
    assert payments[0]['date_valuated']  # Специальный случай для зарплатного проекта Райфа.

    assert statements[1]['balance_opening'] == '0.00'
    assert statements[1]['balance_closing'] == '0.00'

    payments = statements[1]['payments']
    assert len(payments) == 4
    assert 'date_valuated' not in payments[0]


def test_intraday(api_client, get_assoc_acc_curr, get_proved, django_assert_num_queries):

    _, account1, _ = get_assoc_acc_curr(Raiffeisen, account='10')
    _, account2, _ = get_assoc_acc_curr(Sber, account='20')

    get_proved(
        associate=Raiffeisen,
        acc_num=account1.number,
        register_kwargs={'intraday': True},
        proved_pay_kwargs=[
            {'intraday': True},
            {'intraday': True, 'direction': Direction.OUT},
            {'intraday': True,
             'direction': Direction.OUT,
             'trans_code': SwiftIdentificationCode.CHARGE},
        ]
    )
    # Платежи этой выписки не будут учтены, потому что она итоговая.
    get_proved(associate=Raiffeisen, acc_num=account1.number)

    get_proved(
        associate=Sber,
        acc_num=account2.number,
        register_kwargs={'intraday': True},
        proved_pay_kwargs=([{'intraday': True}] * 5)
    )

    with django_assert_num_queries(3):
        response = api_client.get(
            f'/api/statements/?accounts=["10","20"]&on_date={DateUtils.format_nice(with_time=False)}&intraday=1')

    assert response.ok
    response = response.json
    assert not response['errors']

    statements = response['data']['items']
    assert len(statements) == 2

    assert statements[0]['account'] == '10'
    assert 'balance_opening' not in statements[0]
    assert len(statements[0]['date']) == 10
    assert statements[0]['turnover_ct'] == '123.00'
    assert statements[0]['turnover_dt'] == '246.00'

    payments = statements[0]['payments']
    assert len(payments) == 3
    assert payments[0]['direction'] == Direction.aliases[Direction.IN]
    assert 'date_valuated' not in payments[0]
    assert payments[1]['direction'] == Direction.aliases[Direction.OUT]
    assert payments[2]['direction'] == Direction.aliases[Direction.OUT_FREE]

    assert statements[1]['account'] == '20'
    assert statements[1]['turnover_ct'] == '615.00'
    assert statements[1]['turnover_dt'] == '0.00'

    payments = statements[1]['payments']
    assert len(payments) == 5
    assert 'date_valuated' not in payments[0]
