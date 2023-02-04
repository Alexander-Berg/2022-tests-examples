# -*- coding: utf-8 -*-
import datetime
import pytest
import json

from balance import balance_db as db
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import Products, Paysyses, ContractCommissionType, ContractPaymentType, Services, Collateral, \
    ContractCreditType
from balance.features import Features
from btestlib.data.defaults import Date

NOW = datetime.datetime.now()
DT_20_NDS = datetime.datetime(2019, 1, 1)


@pytest.mark.smoke
def test_agency_credit_is_0():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, 'ur')
    client_id = steps.ClientSteps.create()
    contract_params = {'CLIENT_ID': agency_id,
                       'PERSON_ID': person_id,
                       'DT': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                       'FINISH_DT': utils.Date.date_to_iso_format(NOW + relativedelta(days=180)),
                       'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                       'CREDIT_LIMIT_SINGLE': 1}

    contract_with_agency_id, contract_eid = \
        steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM, contract_params)

    # Выдаем индивидуальный кредитный лимит клиенту client_1
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id),
                      "client": "{0}".format(client_id),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_with_agency_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(NOW),
                         'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                         }

    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    service_order_id_1 = steps.OrderSteps.next_id(service_id=service_id)
    order_id_1 = steps.OrderSteps.create(client_id, service_order_id_1, service_id=service_id,
                                         product_id=product_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_1, 'Qty': 20, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_with_agency_id, overdraft=0,
                                                 endbuyer_id=None)

