# -*- coding: utf-8 -*-

from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, Services, Paysyses
from balance.real_builders import common_defaults
from balance.real_builders.invoices.steps import create_base_invoice

CONTEXT = Contexts.MARKET_RUB_CONTEXT.new(
    firm=Firms.MARKET_111,
    service=Services.MARKET_ANALYTICS,
    person_params=common_defaults.FIXED_UR_PARAMS,
    paysys=Paysyses.BANK_UR_RUB
)

create_base_invoice(context=CONTEXT)


# from temp.igogor.balance_objects import Contexts
# from btestlib.constants import Firms, Services, PersonTypes, Products, Regions, ContractCommissionType, Currencies
# from balance.tests.paystep.test_request_choices import test_pcps_with_invoice_or_act, prepay_contract
#
# MARKET_ANALYTICS = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET_ANALYTICS,
#                                                         product=Products.MARKET_ANALYTICS)
#
# test_pcps_with_invoice_or_act('MARKET_ANALYTICS_client_with_contract',
#                               MARKET_ANALYTICS.new(person_type=PersonTypes.UR, with_contract=True),
#                               {'with_agency': False, 'region_id': Regions.RU.id,
#                                'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
#                                                              person_type=PersonTypes.UR,
#                                                              firm_id=Firms.MARKET_111.id,
#                                                              service_list=[Services.MARKET_ANALYTICS.id],
#                                                              currency=Currencies.RUB.num_code)]},
#                               {'offer_type_id': 0,
#                                'expected_paysys_list': {
#                                    'with_contract': {'111': {'RUR': {'ur': [11101003]
#                                                                      }}}}})
