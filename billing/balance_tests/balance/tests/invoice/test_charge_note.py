# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib.data.partner_contexts import *
from btestlib.matchers import equal_to

DEFAULT_QTY = 50
EXPECTED_INVOICE_TYPE = 'charge_note'


def context4restaurant(ctx):
    return ctx.new(service=ctx.commission_service, )


params = [
    pytest.param(CLOUD_RU_CONTEXT, Products.CLOUD_TEST.id, 1, None, False, id='Charge note for Cloud',
                 marks=pytest.mark.smoke()),
    pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, None, 0, None, False,
                 id='Charge note for Corp taxi 135 and 650 Russia',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi)), pytest.mark.smoke()]),
    pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, None, 0, None, False, id='Charge note for Corp taxi 650 Russia',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi)), pytest.mark.smoke()]),
    pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, None, 0, Paysyses.CARD_UR_RUB_TAXI.id, False,
                 id='Charge note for Corp taxi 650 Russia card',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi))),
    pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, None, 0, Paysyses.CARD_UR_RUB_TAXI.id, False,
                 id='Charge note for Corp taxi 135 and 650 Russia card',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi))),
    pytest.param(CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, None, 0, None, False, id='Charge note for Corp taxi Kazakhstan',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi)), pytest.mark.smoke()]),
    pytest.param(CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, None, 0, Paysyses.CARD_UR_KZT_TAXI_CORP.id, False,
                 id='Charge note for Corp taxi Kazakhstan card',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi))),
    pytest.param(CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED, None, 0, None, False, id='Charge note for Corp taxi Belarus',
                 marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4_Taxi)), pytest.mark.smoke()]),
    pytest.param(CORP_TAXI_KGZ_CONTEXT_GENERAL, None, 0, None, False, id='Charge note for Corp taxi KGZ'),

    # пока нет терминалов, после заведения добавить paypolicy_paymethod
    # pytest.param(CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED, None, 0, Paysyses.CARD_UR_BYN_BELGO_CORP.id, False,
    #  id='Charge note for Corp taxi Belarus card'),
    pytest.param(CORP_DISPATCHING_BY_CONTEXT, None, 0, None, False, id='Charge note for Corp dispatching Belarus',
                 marks=pytest.mark.smoke()),
    # пока нет терминалов, после заведения добавить paypolicy_paymethod
    # pytest.param(CORP_DISPATCHING_BY_CONTEXT, None, 0, Paysyses.CARD_UR_BYN_BELGO_CORP.id, False,
    #  id='Charge note for Corp dispatching Belarus card'),

    pytest.param(ZAXI_RU_CONTEXT, None, 0, None, False, id='Charge note for Corp gas stations prepay'),
    pytest.param(context4restaurant(FOOD_RESTAURANT_CONTEXT), None, 1, None, True,
                 id='Charge note for Food restaurant services postpay RU'),
    pytest.param(context4restaurant(FOOD_SHOPS_CONTEXT), None, 1, None, True,
                 id='Charge note for Food Shops services postpay RU'),
    pytest.param(context4restaurant(REST_SITES_CONTEXT), None, 1, None, True,
                 id='Charge note for Restaurant sites services postpay RU'),
    pytest.param(FOOD_CORP_CONTEXT, None, 0, None, False, id='Charge note for Corp food'),
    pytest.param(DRIVE_B2B_CONTEXT, None, 0, None, None, id='Charge note for Drive B2B prepay'),
    pytest.param(DRIVE_B2B_CONTEXT, None, 1, None, None, id='Charge note for Drive B2B postpay'),
    pytest.param(TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT, None, 1, None, True,
                 id='Charge note for TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT postpay'),
    pytest.param(TAXI_MLU_EUROPE_CONGO_EUR_CONTEXT, None, 1, None, True,
                 id='Charge note for TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT postpay'),
    pytest.param(TAXI_MLU_EUROPE_ALGERIA_EUR_CONTEXT, None, 1, None, True,
                 id='Charge note for TAXI_MLU_EUROPE_CAMEROON_EUR_CONTEXT postpay'),
    pytest.param(CORP_TAXI_USN_RU_CONTEXT, None, 0, None, True, id='Charge note for CORP_TAXI_USN_RU_CONTEXT prepay'),
    pytest.param(CORP_TAXI_USN_RU_CONTEXT, None, 1, None, True, id='Charge note for CORP_TAXI_USN_RU_CONTEXT postpay'),
]


@reporter.feature(Features.AUTOBUS, Features.DRIVE_B2B, Features.INVOICE)
@pytest.mark.tickets('BALANCE-28374')
@pytest.mark.parametrize('context, product_id, is_postpay, special_paysys, request_choises',
                         params,
                         )
def test_charge_note(context, product_id, is_postpay, special_paysys, request_choises):
    client_id, person_id, contract_id = create_contract(context, is_postpay)
    service_id = context.service.id if context.service != Services.TAXI_CORP else Services.TAXI_CORP_CLIENTS.id
    invoice_type = create_invoice(service_id, client_id,
                                  person_id, contract_id, product_id,
                                  special_paysys or context.paysys.id, request_choises=request_choises)
    utils.check_that(invoice_type, equal_to(EXPECTED_INVOICE_TYPE), 'Проверяем, что создается квитанция.')


# ---------utils------------------------------------------------

def create_contract(context, is_postpay):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay)
    return client_id, person_id, contract_id


def search_main_service_order_id(contract_id, service_id):
    orders_info = api.medium().GetOrdersInfo({'ContractID': contract_id})
    if service_id:
        orders_info = filter(lambda oi: oi['ServiceID'] == service_id, orders_info)
    if len(orders_info) > 1:
        orders_info = [info for info in orders_info if info['IsGroupRoot']]
        assert len(orders_info) == 1
    return orders_info[0]['ServiceOrderID']


def create_invoice(service_id, client_id, person_id, contract_id, product_id, paysys_id, request_choises=False):
    if product_id:
        service_order_id = steps.OrderSteps.next_id(service_id)
        steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                service_id=service_id)
    else:
        service_order_id = search_main_service_order_id(contract_id, service_id)

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': DEFAULT_QTY,
                    'BeginDT': datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': datetime.now(),
                                                              'InvoiceDesireType': 'charge_note'})
    if request_choises:
        paysys_id = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                    'RequestID': request_id})['paysys_list'][0]['id']

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                 credit=0, contract_id=contract_id)

    invoice_type = db.balance().execute("select type from t_invoice where id = " + str(invoice_id))[0]['type']
    return invoice_type
