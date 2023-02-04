# -*- coding: utf-8 -*-
import itertools

import btestlib.utils as utils
from btestlib.constants import ContractCommissionType, PersonTypes
from temp.aikawa.Balance.contracts.contracts_rules import CommonContract, check_param, \
    PARAM_NEEDED_EXCEPTION

to_iso = utils.Date.date_to_iso_format

minimal_attrs = ['CLIENT_ID', 'PERSON_ID', 'MANAGER_CODE', 'PAYMENT_TYPE', 'SERVICES', 'DT', 'FINISH_DT']

CONTRACT_CONTEXT = CommonContract.new(type=ContractCommissionType.LICENSE,
                                      name='LICENSE',
                                      minimal_attrs=minimal_attrs,
                                      client_contract=True,
                                      person_type=PersonTypes.UR)


def group_by_contract_state(data):
    data = sorted(data, key=lambda d: d[2])
    return itertools.groupby(data, lambda d: d[2])


def test_minimal_attrs_set():
    adds = {'COMMISSION': CONTRACT_CONTEXT.type.id}
    check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='COMMISSION', changeable=True)
    for param in minimal_attrs:
        check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param,
                    strictly_needed=PARAM_NEEDED_EXCEPTION[param])
