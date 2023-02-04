# -*- coding: utf-8 -*-
import pytest
import datetime
from sqlalchemy.testing import emits_warning
from tests import object_builder as ob
from billing.contract_iface.contract_meta import collateral_types
from balance.processors.oebs_api.wrappers.contract import ContractWrapper
import balance.constants as cst

MONTH_BEFORE = datetime.datetime.now() - datetime.timedelta(days=30)


@emits_warning(
    "Usage of the 'collection append' operation is not currently "
    "supported within the execution stage of the flush process"
)
@emits_warning(
    "Attribute history events accumulated on 1 previously clean instances "
    "within inner-flush event handlers have been reset, and will not result in database updates"
)
def test_distr_child_contract_to_oebs_export(session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    parent_contract = ob.ContractBuilder.construct(
        session,
        ctype='DISTRIBUTION',
        firm=cst.FirmId.YANDEX_OOO,
        contract_type=cst.DistributionContractType.FIXED,
    )
    contract = ob.ContractBuilder.construct(
        session,
        ctype='DISTRIBUTION',
        firm=cst.FirmId.YANDEX_OOO,
        contract_type=cst.DistributionContractType.FIXED,
    )
    contract.col0.parent_contract_id = parent_contract.id

    contract.append_collateral(
        dt=MONTH_BEFORE,
        is_signed=MONTH_BEFORE,
        collateral_type=collateral_types['DISTRIBUTION'][3300],
        memo='m',
    )

    contract.append_collateral(
        dt=MONTH_BEFORE,
        is_signed=MONTH_BEFORE,
        collateral_type=collateral_types['DISTRIBUTION'][3300],
        memo='m',
    )
    session.flush()

    parent_info = ContractWrapper(parent_contract).get_info()
    assert len(parent_info['collaterals']) == 3
