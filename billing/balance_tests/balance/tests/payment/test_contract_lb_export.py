from datetime import datetime

import balance.balance_api as api
from balance import balance_steps as steps
from btestlib import reporter
from balance.features import Features

from btestlib.data.partner_contexts import FOOD_CORP_CONTEXT, ExportNG


@reporter.feature(Features.TRUST)
def test_contract_logbroker_enqueue():
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(FOOD_CORP_CONTEXT)
    _, _, export_results = api.test_balance().ProcessNgExportQueue(ExportNG.Type.LOGBROKER_CONTRACT, [contract_id])
    assert all([ok and obj['ver_id'] == 0 for ok, obj in export_results])
    # Update extprop field
    val = False
    for _ in range(5):
        api.test_balance().UpdateContractExtpropFields(contract_id, {'offer_accepted': val})
        val = not val
    _, _, export_results = api.test_balance().ProcessNgExportQueue(ExportNG.Type.LOGBROKER_CONTRACT,
                                                                   [contract_id])
    assert all([ok and obj['ver_id'] == 4 for ok, obj in export_results])
