# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest
import datetime
from balance import balance_db as db
from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.constants import Currencies, \
    Firms, PersonTypes, ContractCommissionType, Services, Products, Paysyses
from temp.igogor.balance_objects import Contexts
from btestlib import utils

def create_prepayment_invoice():
    service_id = 7
    product_id = 1475
    paysys_id = 1001
    client_id = steps.ClientSteps.create()
    # agency_id = 393872
    person_id = steps.PersonSteps.create(client_id, 'ph')
    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id, service_id=service_id) #, params={'AgencyID': agency_id})

    service_order_id1 = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id1, product_id=product_id, service_id=service_id)
    # service_order_id2 = steps.OrderSteps.next_id(service_id)
    # steps.OrderSteps.create(client_id, service_order_id2, product_id=product_id, service_id=service_id)
    # service_order_id3 = steps.OrderSteps.next_id(7)
    # steps.OrderSteps.create(client_id, service_order_id3, product_id=503162, service_id=7)
    # service_order_id4 = steps.OrderSteps.next_id(7)
    # steps.OrderSteps.create(client_id, service_order_id4, product_id=1475, service_id=7)
    # service_order_id5 = steps.OrderSteps.next_id(7)
    # steps.OrderSteps.create(client_id, service_order_id5, product_id=503162, service_id=7)
    # service_order_id6 = steps.OrderSteps.next_id(7)
    # steps.OrderSteps.create(client_id, service_order_id6, product_id=1475, service_id=7)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 1},
                   {'ServiceID': service_id, 'ServiceOrderID': service_order_id1, 'Qty': 13},
                   # {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': 48},
                   # {'ServiceID': 7, 'ServiceOrderID': service_order_id3, 'Qty': 11},
                   # {'ServiceID': 7, 'ServiceOrderID': service_order_id4, 'Qty': 1},
                   # {'ServiceID': 7, 'ServiceOrderID': service_order_id5, 'Qty': 3},
                   # {'ServiceID': 7, 'ServiceOrderID': service_order_id6, 'Qty': 8},
                   ]

    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'FirmID': 1})
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id=paysys_id)
    steps.InvoiceSteps.pay_fair(invoice_id)

create_prepayment_invoice()

TOLOKA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4, service=Services.TOLOKA,
                                                currency=Currencies.USD,
                                              contract_type=ContractCommissionType.USA_OPT_CLIENT)
service_id = 70
product_id = 1475
paysys_id_usu = 1028
paysys_id_usp = 1029
person_type = 'usu'
paysys_id = paysys_id_usu
# client_id = steps.ClientSteps.create()
client_id = 188723581
# steps.ClientSteps.link(client_id, 'clientuid41')
# person_id = steps.PersonSteps.create(client_id, person_type)
# service_order_id = steps.OrderSteps.next_id(service_id)
# steps.OrderSteps.create(client_id, service_order_id, product_id=product_id, service_id=service_id) #, params={'AgencyID': client_id})
# orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 10.1},]
# request_id = steps.RequestSteps.create(client_id, orders_list)
# _, _, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(TOLOKA,
#                                                                                        client_id=client_id, person_id=person_id,
#                                                                                        # contract_type=ContractCommissionType.USA_OPT_CLIENT.id,
#                                                                                        postpay=True,
#                                                                                        old_pa=True,
#                                                                                        # finish_dt=NOW + timedelta(
#                                                                                        #     days=180),
#                                                                                        # additional_params=params
#                                                                               )
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id=paysys_id,
                                             # contract_id=contract_id, credit=1
                                             # )
# steps.InvoiceSteps.pay_fair(invoice_id)
# steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 2.34}, 0)
# steps.ActsSteps.generate(client_id, force=1)

TOLOKA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.JAMS_120, service=Services.TOLOKA,
                                                               contract_type=ContractCommissionType.NO_AGENCY)

# product_id = 507130
# service_id = 42
# dt = datetime.datetime(2021, 3, 9)
# # client_id = 189070801
# client_id = steps.ClientSteps.create()
# steps.ClientSteps.link(client_id, 'clientuid41')
# person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
# _, _, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(TOLOKA, postpay=False, client_id=client_id, person_id=person_id)

# orders_list = []
# service_order_id = steps.OrderSteps.next_id(service_id)
# steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
# orders_list.append(
#     {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 1, 'BeginDT': datetime.datetime.now()})
# request_id = steps.RequestSteps.create(client_id, orders_list, {'InvoiceDesireDT': dt})
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 3406430011015052)


def new_pa_for_consumes_history():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, 'clientuid41')
    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              }
    _, person_id, contract_id, _ = steps.ContractSteps.\
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=agency_id,
                                           start_dt = datetime.datetime(2021,1,1),
                                           services = [Services.DIRECT.id, Services.MEDIA_70.id],
                                           contract_type=ContractCommissionType.COMMISS.id,
                                           finish_dt=datetime.datetime(2099,1,1),
                                           additional_params=params)
    # steps.ClientSteps.link(agency_id, 'yndx-static-balance-consumes')
    subclient_id_1 = steps.ClientSteps.create({'NAME': 'Субклиент первый'})
    subclient_id_2 = steps.ClientSteps.create({'NAME': 'Субклиент второй'})

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(DIRECT_YANDEX.service.id)
    steps.OrderSteps.create(subclient_id_1, service_order_id,
                            service_id=DIRECT_YANDEX.service.id,
                            product_id=DIRECT_YANDEX.product.id,
                            params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': DIRECT_YANDEX.service.id, 'ServiceOrderID': service_order_id,
         'Qty': 10.23, 'BeginDT': datetime.datetime.now()})
    request_id = steps.RequestSteps.create(agency_id, orders_list)

    invoice_id_pa, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                    DIRECT_YANDEX.paysys.id,
                                                    contract_id=contract_id,
                                                    credit=1)

    # db.balance().execute("update t_operation set dt = :dt where invoice_id = :invoice_id and type_id = 10002",
    #                      {'invoice_id': invoice_id_pa, 'dt': datetime.datetime(2021,3,4,5,0,0)})
    #
    steps.CampaignsSteps.do_campaigns(DIRECT_YANDEX.service.id, service_order_id, {'Bucks': 9.34}, 0)
    steps.ActsSteps.generate(agency_id, force=1)
    request_id = steps.RequestSteps.create(agency_id, orders_list)
    # invoice_y_id, _ = steps.InvoiceSteps.get_invoice_ids(agency_id, type='y_invoice')
    # steps.InvoiceSteps.pay_fair(invoice_y_id)
    # # делаем возврат
    # steps.InvoiceSteps.pay_fair(invoice_y_id, payment_sum=-5.23)
    #
    # db.balance().execute(
    #     "update t_operation set dt = :dt where invoice_id = :invoice_id",
    #     {'invoice_id': invoice_y_id, 'dt': datetime.datetime(2021,3,14,9,0,0)})
    #
    # orders_list = []
    # service_order_id = steps.OrderSteps.next_id(Services.MEDIA_70.id)
    # steps.OrderSteps.create(subclient_id_2, service_order_id,
    #                         service_id=Services.MEDIA_70.id,
    #                         product_id=Products.MEDIA_2.id,
    #                         params={'AgencyID': agency_id})
    # orders_list.append(
    #     {'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id,
    #      'Qty': 1.44, 'BeginDT': datetime.datetime.now()})
    # request_id = steps.RequestSteps.create(agency_id, orders_list,
    #                                        additional_params={'InvoiceDesireDT': datetime.datetime(2021,3,1,3,4,5)})
    # invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id,
    #                                                 DIRECT_YANDEX.paysys.id,
    #                                                 contract_id=contract_id,
    #                                                 credit=0)
    # steps.InvoiceSteps.pay_fair(invoice_id_prepayment)
    # db.balance().execute(
    #     "update t_operation set dt = :dt where invoice_id = :invoice_id",
    #     {'invoice_id': invoice_id_prepayment, 'dt': datetime.datetime(2021,3,16,9,0,1)})

    # params = {'CREDIT_TYPE': 1,
    #           'LIFT_CREDIT_ON_PAYMENT': 1,
    #           'EXTERNAL_ID': 'test-contract-credits-01/1',
    #           }
    # создаем первый договор для работы с субклиентами резидентами
    # client_id, person_id, contract_id, _ = steps.ContractSteps.\
    #     create_general_contract_by_context(DIRECT_YANDEX, postpay=True)
    #
    # client_id = 147492190
    # person_id = 14311486
    # contract_id = 2993021
    # # steps.ClientSteps.link(client_id, 'clientuid41')

    # orders_list = []
    # service_order_id = steps.OrderSteps.next_id(DIRECT_YANDEX.service.id)
    # steps.OrderSteps.create(client_id, service_order_id, service_id=DIRECT_YANDEX.service.id, product_id=DIRECT_YANDEX.product.id)
    # orders_list.append(
    #     {'ServiceID': DIRECT_YANDEX.service.id, 'ServiceOrderID': service_order_id, 'Qty': 1, 'BeginDT': datetime.datetime.now()})
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, DIRECT_YANDEX.paysys.id, contract_id=contract_id, credit=1)
    # service_order_id = 62753423
    # steps.CampaignsSteps.do_campaigns(DIRECT_YANDEX.service.id, service_order_id, {'Bucks': 0.34}, 0)
    # steps.ActsSteps.generate(client_id, force=1)

    # y_invoice = 134385035
    # steps.InvoiceSteps.pay_fair(y_invoice)

    # fictive_pa = 134384997
    # steps.InvoiceSteps.make_rollback_ai(fictive_pa, amount=0.1)

    # prepayment_invoice_id = 134322157
    # steps.InvoiceSteps.pay_fair(prepayment_invoice_id)

# new_pa_for_consumes_history()
# orders_list = []
# service_order_id = 46159552
# service_id = 7
# contract_id = 647932
# agency_id = 5131
# person_id = 7141579
# subclient_id = 65849591
# subclient_id = steps.ClientSteps.create()
# service_order_id = steps.OrderSteps.next_id(service_id)
# steps.OrderSteps.create(subclient_id, service_order_id, service_id=service_id,
#                         product_id=503162, params={'AgencyID': agency_id})
# orders_list.append(
#     {'ServiceID': service_id, 'ServiceOrderID': service_order_id,
#      'Qty': 360, 'BeginDT': datetime.datetime.now()})
# request_id = steps.RequestSteps.create(agency_id, orders_list,
#                                        )
# invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id,
#                                                 1003,
#                                                 contract_id=contract_id,
#                                                 credit=1)

