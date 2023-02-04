import pytest

from bcl.banks.registry import VtbRu, VtbMsk
from bcl.core.models.models_payments import Payment
from bcl.toolbox.xls import XlsReader


@pytest.mark.parametrize("associate", [VtbRu, VtbMsk])
def test_payment_creator(associate, get_payment_bundle, get_source_payment):
    compiled = associate.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()])).create_bundle()

    assert 'ПоказательДаты' in compiled


@pytest.mark.parametrize("associate", [VtbRu, VtbMsk])
def test_empty_statement(associate, parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'vtb_ru_empty_statement.txt', associate, '40702840700060021883', 'RUB',
        encoding=associate.statement_dispatcher.parsers[0].encoding
    )[0]

    assert len(payments) == 0

    assert register.opening_balance == '10000.00'
    assert register.closing_balance == '10000.00'

    assert register.is_valid


def test_payment_creator_factor(build_payment_bundle, get_source_payment):
    bundle = build_payment_bundle(VtbRu, payment_dicts=[{'payout_type': Payment.PAYOUT_TYPE_FACTOR}])
    bundle_file = XlsReader.from_bytes(bundle.tst_compiled)
    rows = list(bundle_file.iter_rows())

    assert len(rows[0]) == 10
    assert rows[0][-1] == 'Назначение платежа'

    assert rows[1][0] == f'{bundle.payments[0].number}'


def test_statement_parser(fake_statements_quickcheck):
    fake_statements_quickcheck(associate=VtbRu)
