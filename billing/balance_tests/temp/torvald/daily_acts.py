# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest

from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
BASE_DT = datetime.datetime.now()

QTY = 250

AUTO_RU_AG = 14

PRODUCT = Product(7, 1475, 'Units', 'Money')
PAYSYS_ID = 1003

USD = 840
EUR = 978
CHF = 756

manager_uid = '244916211'


@pytest.mark.parametrize('p', [
    # utils.aDict({'agency_creator': lambda: steps.ClientSteps.create({'IS_AGENCY': 1}),
    #              'person_type': 'ur',
    #              'contract_type': 'opt_agency_post',
    #              'contract_params': {'SERVICES': [7],
    #                                  'REPAYMENT_ON_CONSUME': 0,
    #                                  'PERSONAL_ACCOUNT': 0,
    #                                  'LIFT_CREDIT_ON_PAYMENT': 0,
    #                                  'PERSONAL_ACCOUNT_FICTIVE': 0},
    #              'credit': 1,
    #              'overdraft': 0,
    #              'month_close_type': 0,  # daily
    #              # ----------- con_qty | comp_qty | act_qty | inv | dps ------------------
    #              'invoices': [(D('100'), D('0'),    D('0'),    0,    0),
    #                           (D('100'), D('75.5'), D('0'),    1,    0),
    #                           (D('100'), D('100'),  D('0'),    1,    0),
    #                           (D('100'), D('75.5'), D('50'),   1,    0),
    #                           (D('100'), D('100'),  D('20'),   1,    0),
    #                           (D('100'), D('100'),  D('100'),  0,    0)]
    #              }),

    utils.aDict({'agency_creator': lambda: None,
                 'person_type': 'ur',
                 # 'contract_type': 'no_agency',
                 # 'contract_params': {'SERVICES': [7],
                 #                     'REPAYMENT_ON_CONSUME': 0,
                 #                     'PERSONAL_ACCOUNT': 0,
                 #                     'LIFT_CREDIT_ON_PAYMENT': 0,
                 #                     'PERSONAL_ACCOUNT_FICTIVE': 0},
                 'credit': 0,
                 'overdraft': 0,
                 'month_close_type': 0,  # daily
                 # ----------- con_qty | comp_qty | act_qty | inv | dps ------------------
                 'invoices': [(D('100'), D('0'), D('0'), 0, 0),
                              (D('100'), D('75.5'), D('0'), 1, 0),
                              (D('100'), D('100'), D('0'), 1, 0),
                              (D('100'), D('75.5'), D('50'), 1, 0),
                              (D('100'), D('100'), D('20'), 1, 0),
                              (D('100'), D('100'), D('100'), 0, 0)]
                 })
])
def test_daily_act_enqueue(p):
    # Создаём клиента \ агентство и определяем владельцев заказа и счёта
    client_id = steps.ClientSteps.create()
    # steps.ClientSteps.set_force_overdraft(client_id, 7, 10000000)
    agency_id = p.agency_creator()

    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Для владельца счёта создаём плательщика
    person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

    # Для владельца счёта создаём договор (если передан)
    contract_id = None
    if 'contract_type' in p:
        default_contract_params = {'PERSON_ID': person_id,
                                   'CLIENT_ID': invoice_owner,
                                   'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                   'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO}
        default_contract_params.update(p.contract_params)

        contract_id, _ = steps.ContractSteps.create_contract(p.contract_type, default_contract_params)

        # contract_id, _ = steps.ContractSteps.create_contract('usa_comm', {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
        #                                                'DT'       : '2015-04-30T00:00:00',
        #                                                'FINISH_DT': '2017-06-30T00:00:00',
        #                                                'IS_SIGNED': '2015-01-01T00:00:00',
        #                                                'SERVICES': [7],
        #                                                'PERSONAL_ACCOUNT': 1,
        #                                                'CREDIT_LIMIT_SINGLE': 5000000
        #                                                })

        # steps.ContractSteps.create_collateral(1033,{'contract2_id': contract_id, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})

    product = PRODUCT

    # Обходим список счетов, отсортировав по наличия act_qty - для
    for qty, completions, act_qty, _, _ in p.invoices:
        orders_list = []
        service_order_id = steps.OrderSteps.next_id(product.service_id)
        steps.OrderSteps.create(order_owner, service_order_id, service_id=product.service_id, product_id=product.id,
                                params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
        orders_list.append(
            {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': BASE_DT})

        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,
                                                     credit=p.credit, contract_id=contract_id,
                                                     overdraft=p.overdraft)

        steps.InvoiceSteps.pay(invoice_id)

        if act_qty > D('0'):
            steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id, {'Bucks': act_qty, 'Money': 0}, 0,
                                              BASE_DT)
            steps.ActsSteps.create(invoice_id)

        steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id, {'Bucks': completions, 'Money': 0}, 0,
                                          BASE_DT)

    steps.ActsSteps.enqueue([invoice_owner], force=0, date=BASE_DT)

    query = "select input from t_export where type = 'MONTH_PROC' and object_id = {}".format(invoice_owner)
    value = steps.CommonSteps.get_pickled_value(query)
    pass


if __name__ == '__main__':
    pass
