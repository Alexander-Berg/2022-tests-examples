# coding: utf-8

import datetime

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import constants as const
from btestlib.constants import Paysyses, PersonTypes, Currencies, ContractCommissionType
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()
SW_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_CHF,
                                                  currency=Currencies.CHF,
                                                  contract_type=ContractCommissionType.SW_OPT_AGENCY)

BY_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.BYU, paysys=Paysyses.BANK_BY_UR_BYN,
                                                  currency=Currencies.BYN,
                                                  contract_type=ContractCommissionType.BEL_OPT_AGENCY)


@pytest.mark.parametrize('context', [SW_CONTEXT, BY_CONTEXT])
def test_collateral_do_prepay_old_ls(context):
    ctx = context
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, ctx.person_type.code)

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': const.ContractPaymentType.POSTPAY,
        'SERVICES': [ctx.service.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': ctx.currency.num_code,
        'CREDIT_TYPE': const.ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
        'PERSONAL_ACCOUNT': 1,
        'PERSONAL_ACCOUNT_FICTIVE': 1
    }

    contract_id, _ = steps.ContractSteps.create_contract_new(ctx.contract_type,
                                                             contract_params)
    steps.ExportSteps.export_oebs(contract_id=contract_id, client_id=client_id)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 100}, 0, NOW)

    steps.ActsSteps.generate(client_id, force=1, date=NOW)
    y_inv = db.get_y_invoices_by_fpa_invoice(invoice_id)[0]['id']
    steps.InvoiceSteps.pay(y_inv)
    receipt = db.get_receipt_by_invoice(y_inv)
    assert receipt != []
