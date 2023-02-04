# -*- coding: utf-8 -*-

import datetime
import copy
import pytest
from hamcrest import has_entries

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions, \
    ContractCommissionType

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

FIX_DISCOUNT_POLICY = 8
DISCOUNT_PCT = 11

DEFAULT_CONTRACT_PARAMS = {'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                           'DT': HALF_YEAR_BEFORE_NOW_ISO,
                           'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                           'DISCOUNT_POLICY_TYPE': FIX_DISCOUNT_POLICY,
                           'CONTRACT_DISCOUNT': str(DISCOUNT_PCT)
                           }

DIRECT_TURKEY_FIRM_TRY = Contexts.DIRECT_FISH_TRY_CONTEXT.new(contract_type=ContractCommissionType.TR_OPT_AGENCY)
DIRECT_KZ_FIRM_KZT = Contexts.DIRECT_FISH_KZ_CONTEXT.new(contract_type=ContractCommissionType.KZ_OPT_AGENCY)
DIRECT_KZ_FIRM_KZT_QUASI = Contexts.DIRECT_FISH_KZ_CONTEXT.new(contract_type=ContractCommissionType.KZ_OPT_AGENCY,
                                                               product=Products.DIRECT_KZT_QUASI)
DIRECT_FISH_SW_EUR = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(contract_type=ContractCommissionType.SW_OPT_CLIENT)


def create_invoice(order_owner, invoice_owner, person_id, contract_id, context):
    campaigns_list = [
        {'client_id': order_owner, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
    ]

    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=order_owner,
                                                                            agency_id=invoice_owner if invoice_owner != order_owner else None,
                                                                            person_id=person_id,
                                                                            invoice_dt=NOW,
                                                                            campaigns_list=campaigns_list,
                                                                            contract_id=contract_id,
                                                                            paysys_id=context.paysys.id,
                                                                            credit=getattr(context, 'credit', 0))

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, orders_list[0]['ServiceOrderID'],
                                      {context.product.type.code: 100}, 0, NOW)
    return invoice_id


@pytest.mark.parametrize('context', [
    # DIRECT_TURKEY_FIRM_TRY,
    DIRECT_KZ_FIRM_KZT,
    pytest.mark.smoke(
        DIRECT_KZ_FIRM_KZT_QUASI)])
def test_fix_discount(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    if context not in [DIRECT_KZ_FIRM_KZT,DIRECT_KZ_FIRM_KZT_QUASI] :
        person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    else:
        client_id_for_payer = steps.ClientSteps.create({'IS_AGENCY': 0})
        person_id = steps.PersonSteps.create(client_id_for_payer, context.person_type.code)
        db.balance().execute('''UPDATE t_person SET CLIENT_ID = :agency_id WHERE id =:person_id''',
                             {'agency_id': agency_id, 'person_id': person_id})
    contract_params = copy.deepcopy(DEFAULT_CONTRACT_PARAMS)
    contract_params.update({'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id})
    contract_id, _ = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    invoice_id = create_invoice(order_owner=client_id, invoice_owner=agency_id, person_id=person_id,
                                contract_id=contract_id,
                                context=context)
    utils.check_that(db.get_consumes_by_invoice(invoice_id)[0],
                     has_entries({'static_discount_pct': DISCOUNT_PCT}),
                     step=u'Проверяем сумму и скидку в заявке')


@pytest.mark.parametrize('context', [
    DIRECT_FISH_SW_EUR])
def test_fix_discount_sw_opr_cl(context):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = copy.deepcopy(DEFAULT_CONTRACT_PARAMS)
    contract_params.pop('DISCOUNT_POLICY_TYPE')
    contract_params.update({'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'DISCOUNT_FIXED': 20})
    contract_id, _ = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)

    invoice_id = create_invoice(order_owner=client_id, invoice_owner=client_id, person_id=person_id,
                                contract_id=contract_id,
                                context=context)
    utils.check_that(db.get_consumes_by_invoice(invoice_id)[0],
                     has_entries({'static_discount_pct': 20}),
                     step=u'Проверяем сумму и скидку в заявке')
