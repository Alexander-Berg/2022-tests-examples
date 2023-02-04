# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
import re

import pytest
from hamcrest import equal_to

from balance import balance_steps as steps
from balance import balance_web as web
from balance import balance_db as db
from balance.features import Features
from btestlib import reporter, utils
from btestlib.constants import Firms, ContractPaymentType, ContractCommissionType, PersonTypes
from btestlib.data.defaults import Order
from temp.igogor.balance_objects import Contexts

context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.PR_AGENCY,
                                               is_agency=1,
                                               person_type=PersonTypes.UR.code,
                                               contract_payment_type=ContractPaymentType.POSTPAY)

DT = datetime.datetime.now()
START_DT = utils.Date.nullify_time_of_date(DT).isoformat()
ORDER_QTY = 100
ORDERS_COUNT = 2
SUBCLIENT_COUNT = 2

pytestmark = [pytest.mark.tickets('TESTBALANCE-1657'),
              reporter.feature(Features.ACT, Features.CONTRACT)
              ]


def data_preparation():
    invoice_owner = steps.ClientSteps.create({'IS_AGENCY': 1})
    subclients = [steps.ClientSteps.create() for _ in range(SUBCLIENT_COUNT)]
    person_id = steps.PersonSteps.create(invoice_owner, context.person_type)
    contract_id = steps.ContractSteps.create_contract_new(
        context.contract_type,
        {'CLIENT_ID': invoice_owner,
         'PERSON_ID': person_id,
         'DT': START_DT,
         'IS_SIGNED': START_DT,
         'FIRM': context.firm.id,
         'SERVICES': [context.service.id],
         'PAYMENT_TYPE': context.contract_payment_type})[0]
    orders_list = []
    for order_owner in subclients:
        for _ in range(ORDERS_COUNT):
            service_order_id = steps.OrderSteps.next_id(context.service.id)
            steps.OrderSteps.create(order_owner, service_order_id, context.product.id, context.service.id,
                                    {'TEXT': 'Py_Test order',
                                     'AgencyID': invoice_owner,
                                     'ManagerUID': None})
            orders_list += Order.default_orders_list(service_order_id, service_id=context.service.id, qty=ORDER_QTY)
    request_id = steps.RequestSteps.create(invoice_owner,
                                           orders_list,
                                           additional_params={'InvoiceDesireDT': DT})
    return invoice_owner, person_id, subclients, contract_id, orders_list, request_id


def create_act(invoice_owner, orders_list):
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(context.service.id,
                                          order['ServiceOrderID'],
                                          campaigns_params={'Bucks': ORDER_QTY / 3},
                                          campaigns_dt=DT)
    return steps.ActsSteps.generate(invoice_owner, 1, DT)[0]


def set_docs_detailed_in_extprops(subclient_id, invoice_owner, is_detailed, is_separated):
    db.balance().execute(
        '''insert into t_extprops (id, object_id, classname, attrname, value_clob, update_dt, passport_id)
           values(s_extprops.nextval, :client_id, 'Client', 'agencies_printable_doc_types',
           :value_clob, sysdate, null)''',
        {'client_id': subclient_id,
         'value_clob': '{"' + str(invoice_owner) + '": [' + str(is_detailed).lower() + ',' + str(
             is_separated).lower() + ']}'})


def check_data_in_db(invoice_owner, acts_count, act_amt):
    acts_info = steps.ActsSteps.get_act_data_with_contract_by_client(invoice_owner)
    utils.check_that(len(acts_info), equal_to(acts_count))
    for act in acts_info:
        utils.check_that(act['act_sum'], equal_to(act_amt))


def check_data_in_web(invoice_id, acts_count, act_amt):
    with web.Driver() as driver:
        invoice_page = web.AdminInterface.InvoicePage.open(driver, invoice_id=invoice_id)
        acts_info = invoice_page.get_acts_history_data()
        utils.check_that(len(acts_info), equal_to(acts_count))
        for act in acts_info:
            utils.check_that(int(re.sub(' ', '', re.sub(',00', ' ', act['amount'].text))), equal_to(act_amt))


@pytest.mark.parametrize("acts_count, is_detailed, is_separated, act_amt",
                         [
                             (1, False, False, 3960),
                             (2, False, True, 1980),
                             (2, True, True, 1980),

                         ],
                         ids=[
                             'non_separated_act',
                             'separated_acts',
                             'separated_and_detailed_acts',
                         ]
                         )
def test_separation(acts_count, is_detailed, is_separated, act_amt):
    invoice_owner, person_id, subclients, contract_id, orders_list, request_id = data_preparation()
    invoice_id = steps.InvoiceSteps.create(request_id=request_id,
                                           person_id=person_id,
                                           paysys_id=context.paysys.id,
                                           contract_id=contract_id,
                                           credit=0,
                                           overdraft=0)[0]
    steps.InvoiceSteps.pay(invoice_id)
    if (is_detailed or is_separated):
        set_docs_detailed_in_extprops(subclients[0], invoice_owner, is_detailed, is_separated)
    create_act(invoice_owner, orders_list)
    check_data_in_db(invoice_owner, acts_count, act_amt)
    check_data_in_web(invoice_id, acts_count, act_amt)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize("is_detailed, is_separated",
                         [
                             (False, True),
                             (True, True),
                         ],
                         ids=[
                             'separated_docs',
                             'separated_and_detailed_docs',
                         ]
                         )
def test_exception(is_detailed, is_separated):
    invoice_owner, person_id, subclients, contract_id, orders_list, request_id = data_preparation()
    set_docs_detailed_in_extprops(subclients[0], invoice_owner, is_detailed, is_separated)
    try:
        steps.InvoiceSteps.create(request_id=request_id,
                                  person_id=person_id,
                                  paysys_id=context.paysys.id,
                                  contract_id=contract_id,
                                  credit=0,
                                  overdraft=0)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NONSEPARATED_DOCUMENTS'))
