# coding=utf-8
__author__ = 'atkaya'

from datetime import datetime

from balance import balance_steps as steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import PersonTypes, Services, Paysyses, Products, User, Firms, ContractCommissionType
import balance.balance_db as db
import btestlib.utils as utils

LOGIN_ACTS_SEARCH = User(1397115433, 'yndx-static-balance-acts-1')
LOGIN_ACTS_AGENCY = User(1397730467, 'yndx-static-balance-acts-2')
LOGIN_ACTS_PH = User(1397740673, 'yndx-static-balance-acts-3')
LOGIN_ACTS_RECONCILIATION_REP = User(1397778220, 'yndx-static-balance-acts-4')


def test_acts_xml():
    client_id = steps.ClientSteps.create()

    steps.UserSteps.link_user_and_client(LOGIN_ACTS_SEARCH, client_id)

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'
                                             })
    person_ur_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                            {'name': u'тестовое юр лицо 1',
                                             'email': 'test-ur-2@balance.ru',
                                             'inn': '7865109488',
                                             'postaddress': u'Льва Толстого, 16',
                                             'postcode': '119021'
                                             })

    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    _, _, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=client_id,
                                           person_id=person_ur_id,
                                           start_dt=datetime(2021, 1, 1),
                                           additional_params={'EXTERNAL_ID': 'test_acts-1'})

    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 35}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1)))
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id)
    steps.InvoiceSteps.pay(invoice_id_1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_1, {'Bucks': 20}, 0,
                                      campaigns_dt=datetime(2021, 1, 1))

    steps.OverdraftSteps.set_force_overdraft(client_id, Services.MARKET.id, 1000000, firm_id=Firms.MARKET_111.id)
    service_order_id_2 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.MARKET.id, service_id=Services.MARKET.id)
    orders_list_2 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_2, 'Qty': 10}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1)))
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ur_id, Paysyses.BANK_UR_RUB.id, overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_2, {'Bucks': 8}, 0,
                                      campaigns_dt=datetime(2021, 1, 1))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 1, 31))

    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_2, {'Bucks': 10}, 0,
                                      campaigns_dt=datetime(2021, 2, 28))
    act_id = steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 2, 28))[0]
    steps.ActsSteps.set_payment_term_dt(act_id, datetime(2099, 2, 1))

    service_order_id_3 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_3, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_3 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_3, 'Qty': 15}]
    request_id_3 = steps.RequestSteps.create(client_id, orders_list_3,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 3, 1)))
    invoice_id_3, _, _ = steps.InvoiceSteps.create(request_id_3, person_ph_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_3)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 1}, 0,
                                      campaigns_dt=datetime(2021, 3, 1))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 2}, 0,
                                      campaigns_dt=datetime(2021, 3, 2))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 3}, 0,
                                      campaigns_dt=datetime(2021, 3, 3))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 4}, 0,
                                      campaigns_dt=datetime(2021, 3, 4))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 5}, 0,
                                      campaigns_dt=datetime(2021, 3, 5))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 6}, 0,
                                      campaigns_dt=datetime(2021, 3, 6))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 7}, 0,
                                      campaigns_dt=datetime(2021, 3, 7))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 8}, 0,
                                      campaigns_dt=datetime(2021, 3, 8))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 9}, 0,
                                      campaigns_dt=datetime(2021, 3, 9))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 10}, 0,
                                      campaigns_dt=datetime(2021, 3, 10))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 11}, 0,
                                      campaigns_dt=datetime(2021, 3, 11))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))


def test_agency_pf():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.UserSteps.link_user_and_client(LOGIN_ACTS_AGENCY, agency_id)
    last_day_prev_month = utils.Date.get_last_day_of_previous_month()

    db.balance().execute("update t_passport set is_main = 1 where passport_id = :passport_id",
                         {'passport_id': LOGIN_ACTS_AGENCY.uid})
    person_ph_id = steps.PersonSteps.create(agency_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'
                                             })
    person_ur_id = steps.PersonSteps.create(agency_id, PersonTypes.UR.code,
                                            {'name': u'тестовое юр лицо',
                                             'email': 'test-ur@balance.ru',
                                             'inn': '7865109488',
                                             'postaddress': u'Льва Толстого, 16',
                                             'postcode': '119021',
                                             'delivery-type': '3'
                                             })
    subclient_1 = steps.ClientSteps.create()
    login_of_subclient = User(1397740033, 'balancesubcl')
    steps.UserSteps.link_user_and_client(login_of_subclient, subclient_1)
    subclient_2 = steps.ClientSteps.create()

    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(subclient_1, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id, params={'AgencyID': agency_id})
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 10}]
    request_id_1 = steps.RequestSteps.create(agency_id, orders_list_1,
                                             additional_params={'InvoiceDesireDT': last_day_prev_month})
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ur_id, Paysyses.BANK_UR_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_1, {'Bucks': 10}, 0,
                                      campaigns_dt=last_day_prev_month)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=last_day_prev_month)[0]

    service_order_id_2 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(subclient_2, service_order_id_2, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id, params={'AgencyID': agency_id})
    orders_list_2 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_2, 'Qty': 67}]
    request_id_2 = steps.RequestSteps.create(agency_id, orders_list_2)
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ph_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_2)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_2, {'Bucks': 67}, 0)
    steps.ActsSteps.generate(agency_id, force=1)

    # steps.ExportSteps.export_oebs(client_id=subclient_1)
    # steps.ExportSteps.export_oebs(client_id=agency_id, person_id=person_ur_id,
    #                               invoice_id=invoice_id_1, act_id=act_id)


def test_acts_ph():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(LOGIN_ACTS_PH, client_id)

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'
                                             })

    service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 1}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_ph_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id, {'Bucks': 1}, 0)
    steps.ActsSteps.generate(client_id, force=1)


def test_reconc_report():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(LOGIN_ACTS_RECONCILIATION_REP, client_id)
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 1',
                              'email': 'test-ur-1@balance.ru',
                              'inn': '7865109488',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 2',
                              'email': 'test-ur-2@balance.ru',
                              'inn': '7836398764',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 3',
                              'email': 'test-ur-3@balance.ru',
                              'inn': '7884482864',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 4',
                              'email': 'test-ur-4@balance.ru',
                              'inn': '7896345781',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-5',
                              'email': 'test-ur-5@balance.ru',
                              'inn': '7838171769',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-6',
                              'email': 'test-ur-6@balance.ru',
                              'inn': '7838317834',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-7',
                              'email': 'test-ur-7@balance.ru',
                              'inn': '7866390635',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-8',
                              'email': 'test-ur-8@balance.ru',
                              'inn': '7866390635',
                              })
