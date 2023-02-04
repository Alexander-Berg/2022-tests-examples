from datetime import datetime
from decimal import Decimal

import pytest

from bcl.banks.registry import Acba
from bcl.core.models import Currency, states, PaymentsBundle
from bcl.exceptions import ValidationError


def test_payment_creator(get_payment_bundle, get_source_payment):

    attrs = {
        'f_acc': '321',
        't_acc': '123',
        'number': '444',
        'date': datetime(2017, 5, 26),
        'summ': '5125.52',
        'currency_id': Currency.AMD,
        'ground': 'Ground'
    }

    payments = [get_source_payment(attrs)]

    with pytest.raises(ValidationError):
        Acba.payment_dispatcher.get_creator(get_payment_bundle(payments)).create_bundle()

    attrs['ground'] = 'Назначение'
    payments = [get_source_payment(attrs)]

    attrs['summ'] = '5125.50'
    attrs['ground'] = ''
    payments.append(get_source_payment(attrs))

    attrs['ground'] = 'Ground'
    payments.append(get_source_payment(attrs))

    bundle = get_payment_bundle(payments)

    compiled = Acba.payment_dispatcher.get_creator(bundle).create_bundle()
    bundle = PaymentsBundle.objects.get(id=bundle.id)
    payments[0].refresh_from_db()
    payments[1].refresh_from_db()

    assert len(bundle.payments) == 1
    assert payments[0].status == states.BCL_INVALIDATED
    assert 'Кириллица не допускается в валютных платежах' in payments[0].processing_notes
    assert 'Платёж не должен содержать более одной значащей цифры после запятой' in payments[0].processing_notes

    assert payments[1].status == states.BCL_INVALIDATED
    assert 'поле "Назначение платежа" (ground)\nЗначение обязательно, но не указано' in payments[1].processing_notes

    assert compiled == (
        "<?xml version='1.0' encoding='utf-8' standalone='yes'?>\n"
        '<As_Import-Export_File><PayOrd CAPTION="Documents (Payment Inside of RA)">'
        '<PayOrd DOCNUM="444" DOCDATE="26/05/17" PAYERACC="321" PAYER="OOO Яндекс" BENACC="123" '
        'BENEFICIARY="ООО &quot;Кинопортал&quot;" '
        'AMOUNT="5125.50" CURRENCY="AMD" DETAILS="Ground"/></PayOrd></As_Import-Export_File>')


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_acba.txt', associate=Acba, acc='220410510832000', curr='AMD')[0]

    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 9

    assert payments[0].summ == Decimal('500000.00')
    assert payments[0].number == '11'
    assert payments[0].get_info_purpose() == (
        '437, 110000903 Payment as of Agreement 1816262/21 (onlayn banking 57839600) statcogh  IDRAM  SPY'
    )

    assert payments[8].summ == Decimal('400000.00')
    assert payments[8].number == '1'
    assert payments[8].get_info_purpose() == (
        'Phokhantcoum hashvin (onlayn banking 57817059) phokhantcogh  YANDEQS.TAQSI EYEM  SPY'
    )
