# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Firms, User, ContractCommissionType, Services, Products
from temp.igogor.balance_objects import Contexts

LOGIN_CONSUMES_HISTORY = User(1395405169, 'yndx-static-balance-consumes')


def test_new_pa_for_consumes_history():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              }
    _, person_id, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=agency_id,
                                           start_dt=datetime.datetime(2021, 1, 1),
                                           services=[Services.DIRECT.id, Services.MEDIA_70.id],
                                           contract_type=ContractCommissionType.COMMISS.id,
                                           finish_dt=datetime.datetime(2099, 1, 1),
                                           additional_params=params)
    steps.UserSteps.link_user_and_client(LOGIN_CONSUMES_HISTORY, agency_id)
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

    db.balance().execute("update t_operation set dt = :dt where invoice_id = :invoice_id and type_id = 10002",
                         {'invoice_id': invoice_id_pa, 'dt': datetime.datetime(2021, 3, 4, 5, 0, 0)})

    steps.CampaignsSteps.do_campaigns(DIRECT_YANDEX.service.id, service_order_id, {'Bucks': 9.34}, 0)
    steps.ActsSteps.generate(agency_id, force=1)
    invoice_y_id, _ = steps.InvoiceSteps.get_invoice_ids(agency_id, type='y_invoice')
    steps.InvoiceSteps.pay_fair(invoice_y_id)
    # делаем возврат
    steps.InvoiceSteps.pay_fair(invoice_y_id, payment_sum=-5.23)

    db.balance().execute(
        "update t_operation set dt = :dt where invoice_id = :invoice_id",
        {'invoice_id': invoice_y_id, 'dt': datetime.datetime(2021, 3, 14, 9, 0, 0)})

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(Services.MEDIA_70.id)
    steps.OrderSteps.create(subclient_id_2, service_order_id,
                            service_id=Services.MEDIA_70.id,
                            product_id=Products.MEDIA_2.id,
                            params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id,
         'Qty': 1.44, 'BeginDT': datetime.datetime.now()})
    request_id = steps.RequestSteps.create(agency_id, orders_list,
                                           additional_params={
                                               'InvoiceDesireDT': datetime.datetime(2021, 3, 1, 3, 4, 5)})
    invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                            DIRECT_YANDEX.paysys.id,
                                                            contract_id=contract_id,
                                                            credit=0)
    steps.InvoiceSteps.pay_fair(invoice_id_prepayment)
    db.balance().execute(
        "update t_operation set dt = :dt where invoice_id = :invoice_id",
        {'invoice_id': invoice_id_prepayment, 'dt': datetime.datetime(2021, 3, 16, 9, 0, 1)})
