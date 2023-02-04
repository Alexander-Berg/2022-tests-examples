import datetime
import json

import pytest

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import ContractCommissionType, Collateral
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                              collateral_type=Collateral.SUBCLIENT_CREDIT_LIMIT)


# @pytest.mark.no_parallel('fast_payment_overdraft')
@pytest.mark.parametrize('context', [DIRECT])
def test_client_credit_float(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(agency_id, 'ur')

    contract_params = {'CLIENT_ID': agency_id,
                       'PERSON_ID': person_id,
                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'PAYMENT_TYPE': 3,
                       'CREDIT_TYPE': 2}

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type,
                                                                        contract_params)
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id),
                      "client": "{0}".format(client_id),
                      "client_limit": "1000.44",
                      "client_payment_term": "45",
                      "client_credit_type": "1",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': NOW_ISO,
                         'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                         }

    steps.ContractSteps.create_collateral(context.collateral_type, collateral_params)

    service_order_id = steps.OrderSteps.next_id(context.service.id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 10}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]
    steps.CloseMonth.update_limits(NOW, 1, [agency_id])
