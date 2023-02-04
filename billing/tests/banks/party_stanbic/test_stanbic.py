from decimal import Decimal

import pytest

from bcl.banks.registry import Stanbic, Standard
from bcl.exceptions import UserHandledException


@pytest.mark.parametrize('associate', [Stanbic, Standard])
def test_payment_creator(associate, get_payment_bundle, get_source_payment):

    payments = [
        get_source_payment({
            't_name': 'Employe 1',
            't_acc': 'CI1980100191800000133083',
            't_bic': '001606',
            'summ': Decimal(550000),
            'ground': 'Salaire Fevrier 2019',
        }),
        get_source_payment({
            't_name': 'Employe 4',
            't_acc': 'CI0970100191800000135723',
            't_bic': '000865',
            'summ': Decimal(375000),
            'ground': 'Salaire Fevrier 2019 some long purpose presumably not fit for the bank and must be cut',
        })
    ]

    compiled = associate.payment_dispatcher.get_creator(
        get_payment_bundle(payments)
    ).create_bundle()

    assert compiled == (
        f'Employe 1,CI1980100191800000133083,001606,550000,Salaire Fevrier 2019 @{payments[0].number}\r\n'
        f'Employe 4,CI0970100191800000135723,000865,375000,Salaire Fevrier 2019 some lon @{payments[1].number}\r\n'
    )


@pytest.mark.parametrize('associate', [Stanbic, Standard])
def test_statement_bogus(associate, parse_statement_fixture):

    with pytest.raises(UserHandledException) as e:
        parse_statement_fixture(
            'bogus,data,\n', associate, '918000000032', 'XOF',
            from_file=False
        )

    assert 'Неподдерживаемый формат выписки' in f'{e.value}'


@pytest.mark.parametrize('associate', [Stanbic, Standard])
def test_statement(associate, parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement.csv', associate, '918000000032', 'XOF',
        encoding=associate.statement_dispatcher.parsers[0].encoding)[0]

    assert register.is_valid
    assert str(register.statement_date) == '2022-03-08'
    assert register.summ == Decimal('1')
    assert register.opening_balance == Decimal('1935')
    assert register.closing_balance == Decimal('1934')

    assert len(payments) == 19
    assert payments[0].number == '9898'
    assert payments[1].number == ''
