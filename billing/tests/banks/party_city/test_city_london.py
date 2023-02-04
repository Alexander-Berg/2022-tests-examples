from datetime import datetime

import pytest

from bcl.banks.registry import CityLondon
from bcl.banks.common.swift_mx import Statuses
from bcl.core.views.rpc import Rpc
from bcl.core.models.states import EXPORTED_H2H, PROCESSING, ACCEPTED_H2H, DECLINED_BY_BANK, COMPLETE
from bcl.core.models import Currency
from bcl.exceptions import ValidationError


def test_statement_parser(parse_statement_fixture, get_source_payment, build_payment_bundle, get_assoc_acc_curr):
    _, account, _ = get_assoc_acc_curr(CityLondon, account='13044246', curr=Currency.USD)
    bundle = build_payment_bundle(
        CityLondon, payment_dicts=[
            {
                'ground': 'PAY(16071909)Payment invoice by Constractors services for processing information',
                't_inn': '12345', 'currency_id': 933, 'number': '17', 'date': datetime(2021, 8, 24), 'summ': '100'
            }
        ], account={'number': account.number}
    )
    register, payments = parse_statement_fixture(
        'citibank_london_allday.xml', CityLondon, account.number, 'USD')[0]

    assert not register.intraday
    assert len(payments) == 3

    assert payments[1].payment == bundle.payments[0]

    statement_oebs = Rpc()._get_allday(register.account.number, register.statement_date)

    assert statement_oebs['statement_body'][0]['name'] == 'BelGo Corp LLC'
    assert statement_oebs['statement_body'][0]['doc_date'] == '2021-08-26'
    assert statement_oebs['statement_body'][0]['doc_number'] == '16'
    assert statement_oebs['statement_body'][0]['direction'] == 'OUT'
    assert statement_oebs['statement_body'][0]['ground'] == 'PAY(16071908)Payment invoice by Con stractors services for processing i nformation BENE TIN UNP 193416131'
    assert statement_oebs['statement_body'][0]['summ'] == '41.07'
    assert statement_oebs['statement_body'][0]['account'] == 'BY71PJCB30120623701000000933'

    assert statement_oebs['statement_body'][1]['name'] == 'BelGo Corp LLC'
    assert statement_oebs['statement_body'][1]['doc_date'] == '2021-08-26'

    assert statement_oebs['statement_body'][2]['name'] == '1/Uber ML B.V'
    assert statement_oebs['statement_body'][2]['doc_date'] == '2021-08-26'
    assert statement_oebs['statement_body'][2]['doc_number'] == 'P3830257 520 1'
    assert statement_oebs['statement_body'][2]['direction'] == 'IN'
    assert statement_oebs['statement_body'][2]['ground'] == 'O/O 1/Uber ML B.V                  PAY(16088465)TRANSFER OF OWN FUNDS'
    assert statement_oebs['statement_body'][2]['account'] == '6550045512'

    register, payments = parse_statement_fixture('citybank_london_intraday.xml', CityLondon, '12345678', 'EUR')[0]

    assert register.intraday
    assert register.is_valid
    assert len(payments) == 1

    statement_oebs = Rpc()._get_intraday(register.account.number, register.statement_date)

    assert statement_oebs['statement_body'][0]['name'] == 'HEREU SERVICIOS MEDICOS S.L.'
    assert statement_oebs['statement_body'][0]['doc_date'] == '2018-07-10'
    assert statement_oebs['statement_body'][0]['doc_number'] == 'NOTPROVIDED'
    assert statement_oebs['statement_body'][0]['direction'] == 'IN'
    assert statement_oebs['statement_body'][0]['ground'] == 'REMITTANCE INFORMATION'
    assert statement_oebs['statement_body'][0]['account'] == 'ES1111111111111111111111'


@pytest.mark.parametrize(
    'external_status, internal_status',
    [
        (Statuses.STATUS_VERIFIED_FORMAT, ACCEPTED_H2H),
        (Statuses.STATUS_ACCEPTED_PARTIAL, PROCESSING),
        (Statuses.STATUS_REJECTED, DECLINED_BY_BANK),
    ]
)
def test_status_parser_level1(
    external_status, internal_status, read_fixture, get_payment_bundle, get_source_payment, build_payment_bundle
):
    with pytest.raises(ValidationError):
        build_payment_bundle(CityLondon, payment_dicts=[{'ground': '0' * 140}], account={'number': '1' * 20})

    with pytest.raises(ValidationError):
        build_payment_bundle(
            CityLondon, payment_dicts=[{'ground': '0' * 121, 't_inn': '12345'}], account={'number': '1' * 20})

    bundle = build_payment_bundle(
        CityLondon, payment_dicts=[{'ground': '0' * 110, 't_inn': '12345'}], account={'number': '1' * 20})

    assert f'<Ustrd>{bundle.payments[0].ground[:35]}</Ustrd>' in bundle.tst_compiled
    assert f'<Ustrd>0000000000000000 BENE TIN UNP 12345</Ustrd>' in bundle.tst_compiled

    bundle = get_payment_bundle(
        [
            {'number': '42835', 'status': EXPORTED_H2H, 'ground': '1' * 121, 't_inn': '12345'},
            {'number': '42836', 'status': EXPORTED_H2H}
        ],
        associate=CityLondon, id=123456
    )

    xml = read_fixture('city_london_statuses_file_level.xml', decode='utf-8').replace('ACTC', external_status)
    syncer = CityLondon.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    assert len(status['payments']) == 0

    bundle.refresh_from_db()
    assert bundle.status == internal_status

    assert bundle.payments[0].status == EXPORTED_H2H

    xml = read_fixture('city_london_statuses_transaction_level.xml', decode='utf-8')
    syncer = CityLondon.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    assert len(status['payments']) == 1

    bundle.refresh_from_db()
    assert bundle.status == internal_status

    assert bundle.payments[0].status == COMPLETE
    assert bundle.payments[1].status == EXPORTED_H2H

    xml = read_fixture(
        'city_london_statuses_transaction_level.xml', decode='utf-8'
    ).replace('ACSP', 'RJCT').replace('42835', '42836')

    syncer = CityLondon.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    assert len(status['payments']) == 1

    bundle.refresh_from_db()
    assert bundle.status == internal_status

    assert bundle.payments[0].status == COMPLETE
    assert bundle.payments[1].status == DECLINED_BY_BANK


def test_payment_synchronizer():

    class MockCaller:

        associate = CityLondon
        on_date = datetime(2020, 12, 4)

    filter_func = CityLondon.payment_synchronizer.connector(
        caller=MockCaller()
    ).status_filename_get_filter()

    fnames = [
        'ACK_WLXML.5789832.2020120420210511024658',
        'ACK_WLXML.5789832.2020120320210511024658',
        'ACCEPT_WLXML.5789832.2020120420210511024658',
        'REJECT_WLXML.5789832.2020120420210511024658',
        'TEST_WLXML.5789832.2020120420210511024658',
    ]

    names = list(map(filter_func, fnames))
    assert names == [
        'ACK',
        False,
        'ACCEPT',
        'REJECT',
        False,
    ]
