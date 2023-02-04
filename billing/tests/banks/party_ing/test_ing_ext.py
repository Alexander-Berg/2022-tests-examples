import pytest

from bcl.banks.common.swift_mx import Statuses
from bcl.banks.registry import IngNl, IngRo
from bcl.core.models import EXPORTED_H2H, DECLINED_BY_BANK, PROCESSING, ACCEPTED_H2H


def test_status_parser_fail(read_fixture, get_payment_bundle, get_source_payment):

    bundle = get_payment_bundle([{'number': '428', 'status': EXPORTED_H2H}], associate=IngNl, id=2323347)

    xml = read_fixture('ing_ext_statuses_fail.xml', decode='utf-8')
    syncer = IngNl.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    bundle.refresh_from_db()
    payment1 = bundle.payments[0]

    assert len(status['payments']) == 1
    assert bundle.status == DECLINED_BY_BANK
    assert payment1.status == DECLINED_BY_BANK
    assert payment1.processing_notes == 'Отклонено\nNARR: Intervention\nNARR: Error\nXT73: Invalid country code'


@pytest.mark.parametrize(
    'external_status, internal_status',
    [
        (Statuses.STATUS_VERIFIED_REQ, ACCEPTED_H2H),
        (Statuses.STATUS_RECIEVED, PROCESSING),
        (Statuses.STATUS_VERIFIED_FORMAT, PROCESSING),
    ]
)
def test_status_parser_done(external_status, internal_status, read_fixture, get_payment_bundle, get_source_payment):

    bundle = get_payment_bundle([{'number': '428', 'status': EXPORTED_H2H}], associate=IngNl, id=2323347)

    xml = read_fixture('ing_ext_statuses_done.xml', decode='utf-8').replace('RCVD', external_status)
    syncer = IngNl.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    bundle.refresh_from_db()

    assert len(status['payments']) == 0
    assert bundle.status == internal_status


def test_statements(parse_statement_fixture):

    associate = IngNl

    result = parse_statement_fixture('ing_ext_statement.xml', associate, 'NL35INGB0650977351', 'EUR')

    register, pays = result[0]
    assert register.is_valid
    assert len(pays) == 25
    assert pays[0].get_info_account() == 'LV02HABA0551043868619'

    assert pays[1].get_info_purpose() == 'some/more info from the bank'  # Из AddtlNtryInf

    assert pays[3].number == '1666478036FS'  # Из AddtlTxInf
    assert '1666478036FS' in pays[3].get_info_purpose()

    assert pays[19].number == '5'  # Из Refs/EndToEndId
    assert 'EU-1526343950-1' in pays[19].get_info_purpose()


def test_statement_intraday(parse_statement_fixture):

    associate = IngNl

    result = parse_statement_fixture('ing_ext_052.xml', associate, 'NL10INGB0005481795', 'EUR')

    register, pays = result[0]
    assert register.is_valid
    assert len(pays) == 2
    assert pays[0].get_info_account() == 'NL89INGB0748393188'


def test_reversal(verify_statement_parse):
    _, _, registers = verify_statement_parse(
        associate=IngRo,
        accounts=['91470040/8229458910'],
        statement_file='ing_ro_reversal.txt'
    )
    assert len(registers) == 1
    assert registers[0][0].is_valid
