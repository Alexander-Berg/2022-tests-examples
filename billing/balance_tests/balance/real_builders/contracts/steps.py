# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D


from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType, Services, Currencies, NdsNew, PaymentMethods, Regions
from .. import common_defaults

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW


CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY,
                                               person_params=common_defaults.FIXED_UR_PARAMS,
                                               payment_method=PaymentMethods.BANK.cc,
                                               currency=Currencies.RUB,
                                               nds=NdsNew.DEFAULT.pct_on_dt(NOW),
                                               region=Regions.RU,
                                               contract_services=[Services.DIRECT.id],)


def create_base_contract(contract_params, is_agency=0, context=CONTEXT):
    client_id = steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME, 'IS_AGENCY': is_agency})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=context.person_params)
    contract_params.update({'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    return client_id, person_id, contract_id, contract_eid
