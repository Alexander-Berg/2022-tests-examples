from datetime import date
from decimal import Decimal

from bcl.banks.registry import RaiffeisenSrb
from bcl.core.models import Currency


def test_payment_creator(build_payment_bundle):

    # валютный платёж
    compiled = build_payment_bundle(RaiffeisenSrb, payment_dicts=[{
        'oper_code': '241',
        'contract_dt': date(2021, 1, 2),
    }]).tst_compiled

    assert '<Iznos>152.00</Iznos>' in compiled
    assert 'GodinaUgovora>2021' in compiled
    assert '<OpisTransakcije>Air transport' in compiled

    # платёж локальный (в динарах)
    compiled = build_payment_bundle(RaiffeisenSrb, payment_dicts=[{
        'oper_code': '220',
        'currency_id': Currency.RSD,
        'ground': 'myground',
    }]).tst_compiled

    assert '<Iznos>152.00</Iznos>' in compiled
    assert '<SifraPlacanja>220' in compiled
    assert '<SvrhaDoznake>myground<' in compiled


def test_statement_parser(parse_statement_fixture):

    # динары
    register, payments = parse_statement_fixture(
        'statement_raiff_srb_dinar.xml', RaiffeisenSrb, '265110031000008888', 'RSD')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 2

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == Decimal('70.73')
    assert pay.get_info_purpose() == 'PROMET ROBE I USLUGA FINALNA POTROSNJA'

    # валюта
    register, payments = parse_statement_fixture(
        'statement_raiff_srb_foreign.xml', RaiffeisenSrb, '265100000000066666', 'EUR')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 2

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == Decimal('70.73')
    assert pay.get_info_purpose() == 'Knjiženje priliva po loro doznaci za pravna lica'
