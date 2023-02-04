from decimal import Decimal

from bcl.banks.registry import Dabrabyt


def test_payment_creator(get_payment_bundle, get_source_payment):

    compiled = Dabrabyt.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment({
        'f_acc': 'BY24MMBN30120430000109330000',
        'f_swiftcode': '111111',
        't_swiftcode': '22222222XXX',
    })])).create_bundle()

    assert 'поручение (рубли)' in compiled
    assert '^UNN=7705713772^' in compiled
    assert '^UNNRec=7725713770^' in compiled
    assert '^MFO1=111111^' in compiled
    assert '^MFO2=22222222^' in compiled
    assert '^OchPlat=22^' in compiled


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement.txt', Dabrabyt, 'BY24MMBN30120430000109330000', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert len(payments) == 11

    pay = payments[0]
    assert pay.is_out
    assert pay.summ == Decimal('8554')
