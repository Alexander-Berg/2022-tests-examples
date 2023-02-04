import pytest
import datetime
from tests import object_builder as ob

from billing.contract_iface import contract_meta

NOW = datetime.datetime.now()


def flatten_collateral_list():
    result = []
    for contract_type, collateral_types in contract_meta.collateral_types.iteritems():
        for collateral_type in collateral_types:
            result.append((contract_type, collateral_type))
    return result


@pytest.mark.parametrize('contract_params, is_signed', [({'is_signed': None}, False),
                                                        ({'is_signed': NOW}, True),
                                                        ({'is_signed': None, 'is_faxed': NOW}, True),
                                                        ({'is_signed': None, 'ctype': 'PARTNERS',
                                                          'collateral_type_id': 2090}, True),
                                                        ({'is_signed': None, 'ctype': 'PARTNERS',
                                                          'collateral_type_id': 2100}, True),

                                                        ])
def test_signed(session, contract_params, is_signed):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    assert contract.collaterals[0].signed is is_signed


def test_cc_wo_type_class_name(session):
    contract = ob.ContractBuilder().build(session).obj
    assert contract.collaterals[0].class_name is None


@pytest.mark.parametrize('contract_type, collateral_type_id', flatten_collateral_list())
def test_cc_w_type_class_name(session, contract_type, collateral_type_id):
    contract = ob.ContractBuilder(ctype=contract_type, collateral_type_id=collateral_type_id).build(session).obj
    if collateral_type_id in [2090, 2100]:
        assert contract.collaterals[0].class_name == 'ANNOUNCEMENT'
    else:
        assert contract.collaterals[0].class_name == 'COLLATERAL'


@pytest.mark.parametrize('contract_params, is_active', [({'is_signed': None}, False),
                                                        ({'is_signed': NOW}, True),
                                                        ({'is_signed': None, 'is_faxed': NOW}, True),
                                                        ({'is_signed': NOW, 'is_cancelled': NOW}, False),

                                                        ({'is_signed': None, 'is_faxed': NOW, 'is_cancelled': NOW},
                                                         False),
                                                        ])
def test_active(session, contract_params, is_active):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    assert contract.collaterals[0].active is is_active
