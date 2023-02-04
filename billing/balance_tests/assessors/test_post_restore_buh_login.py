# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime
from balance import balance_steps as steps
from balance import balance_db as db
from btestlib.constants import Firms, User, ContractCommissionType, Services, Products, \
    PersonTypes, Paysyses
from temp.igogor.balance_objects import Contexts

LOGIN_BUH_LOGIN = User(1396191351, 'yndx-static-buh-login')
LOGIN_BUG_LOGIN_INVOICE_CHANGE = User(1397800035, 'yndx-static-buh-login-inv')


def test_buh_login():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, PersonTypes.UR.code, {'name': 'Плательщик 1'})
    steps.PersonSteps.create(agency_id, PersonTypes.UR.code, {'name': 'Плательщик 2'})
    steps.PersonSteps.create(agency_id, PersonTypes.UR.code, {'name': 'Плательщик 3'})
    steps.PersonSteps.create(agency_id, PersonTypes.UR.code, {'name': 'Плательщик 4'})
    steps.PersonSteps.create(agency_id, PersonTypes.UR.code, {'name': 'Плательщик 5'})
    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              'EXTERNAL_ID': 'test_buh_login-1',
              }
    _, _, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=agency_id,
                                           person_id=person_id,
                                           start_dt=datetime.datetime(2021, 1, 1),
                                           services=[Services.DIRECT.id, Services.MEDIA_70.id],
                                           contract_type=ContractCommissionType.COMMISS.id,
                                           finish_dt=datetime.datetime(2099, 1, 1),
                                           additional_params=params)
    steps.UserSteps.link_accountant_and_client(LOGIN_BUH_LOGIN, agency_id)
    subclient_id = steps.ClientSteps.create({'NAME': 'Субклиент первый'})

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(DIRECT_YANDEX.service.id)
    steps.OrderSteps.create(subclient_id, service_order_id,
                            service_id=DIRECT_YANDEX.service.id,
                            product_id=DIRECT_YANDEX.product.id,
                            params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': DIRECT_YANDEX.service.id, 'ServiceOrderID': service_order_id,
         'Qty': 600, 'BeginDT': datetime.datetime.now()})
    request_id = steps.RequestSteps.create(agency_id, orders_list)
    invoice_id_pa, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                    DIRECT_YANDEX.paysys.id,
                                                    contract_id=contract_id,
                                                    credit=1)
    steps.CampaignsSteps.do_campaigns(DIRECT_YANDEX.service.id, service_order_id, {'Bucks': 100}, 0)

    orders_list = []
    service_order_id_2 = steps.OrderSteps.next_id(Services.MEDIA_70.id)
    steps.OrderSteps.create(subclient_id, service_order_id_2,
                            service_id=Services.MEDIA_70.id,
                            product_id=Products.MEDIA_2.id,
                            params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id_2,
         'Qty': 20, 'BeginDT': datetime.datetime.now()})
    request_id = steps.RequestSteps.create(agency_id, orders_list,
                                           additional_params={
                                               'InvoiceDesireDT': datetime.datetime(2021, 3, 1, 0, 0, 0)})
    invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                            DIRECT_YANDEX.paysys.id,
                                                            contract_id=contract_id,
                                                            credit=0)
    steps.InvoiceSteps.pay_fair(invoice_id_prepayment)
    steps.CampaignsSteps.do_campaigns(Services.MEDIA_70.id, service_order_id_2, {'Days': 20}, 0)
    steps.ActsSteps.generate(agency_id, force=1)


def test_free_logins():
    buh_passports = [1397741276, 1397741642, 1397741986,
                     1397742380, 1397742656, 1397742973,
                     1397743351, 1397743710, 1397744022,
                     1397744435]
    for passport in buh_passports:
        db.balance().execute("delete from t_role_client_user where passport_id = :passport",
                             {'passport': passport})


def test_no_invoice_change():
    client_id = steps.ClientSteps.create()
    # steps.UserSteps.link_user_and_client(LOGIN_BUG_LOGIN_INVOICE_CHANGE, client_id)
    steps.UserSteps.link_accountant_and_client(LOGIN_BUG_LOGIN_INVOICE_CHANGE, client_id)

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 1}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_ph_id, Paysyses.CC_PH_RUB.id)
