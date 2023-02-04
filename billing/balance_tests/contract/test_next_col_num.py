import datetime

from tests.object_builder import ContractBuilder


def test_next_col_num(session):
    SOME_COLLATERAL_ID = 1006
    contract = ContractBuilder().build(session).obj
    assert contract.next_col_num() == u'01'
    contract.col0.is_cancelled = 1
    assert contract.next_col_num() == u'01'
    contract.col0.is_cancelled = 0
    from billing.contract_iface.cmeta import general
    col1 = contract.append_collateral(datetime.datetime.now(), general.collateral_types[SOME_COLLATERAL_ID])
    assert contract.next_col_num() == u'02'
    col2 = contract.append_collateral(datetime.datetime.now(), general.collateral_types[SOME_COLLATERAL_ID])
    assert contract.next_col_num() == u'03'
    col1.is_cancelled = 1
    assert contract.next_col_num() == u'03'
    col2.is_cancelled = 1
    col1.is_cancelled = 0
    assert contract.next_col_num() == u'02'
    col1.is_cancelled = 1
    col1.is_signed = datetime.datetime.now()
    assert contract.next_col_num() == u'02'
