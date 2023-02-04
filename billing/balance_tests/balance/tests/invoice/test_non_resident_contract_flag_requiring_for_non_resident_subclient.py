# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import equal_to

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features, AuditFeatures
from btestlib import utils as utils

__author__ = 'torvald'

pytestmark = [pytest.mark.priority('mid'),
              pytest.mark.audit,
              reporter.feature(Features.NON_RESIDENT, Features.CREDIT, Features.INVOICE,
                               AuditFeatures.TR_C19)]

BASE_DT = datetime.datetime.now()
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

DIRECT_PRODUCT = Product(7, 1475, 'Bucks', 'Money')
PAYSYS_ID = 1026
QTY = 118

manager_uid = '244916211'


def data_generator(non_resident_clients):
    client_id = None or steps.ClientSteps.create()
    query = "UPDATE t_client SET FULLNAME = 'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 WHERE ID = :client_id"
    query_params = {'client_id': client_id}
    db.balance().execute(query, query_params)
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('comm_post',
                                                         {'CLIENT_ID': invoice_owner,
                                                          'PERSON_ID': person_id,
                                                          'DT': to_iso(BASE_DT - dt_delta(days=180)),
                                                          'FINISH_DT': to_iso(BASE_DT + dt_delta(days=720)),
                                                          'IS_SIGNED': to_iso(BASE_DT - dt_delta(days=180)),
                                                          'SERVICES': [7],
                                                          'NON_RESIDENT_CLIENTS': non_resident_clients,
                                                          'REPAYMENT_ON_CONSUME': 0,
                                                          'PERSONAL_ACCOUNT': 1,
                                                          'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })

    steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(DIRECT_PRODUCT.service_id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=DIRECT_PRODUCT.service_id,
                            product_id=DIRECT_PRODUCT.id,
                            params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
    orders_list.append({'ServiceID': DIRECT_PRODUCT.service_id, 'ServiceOrderID': service_order_id,
                        'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
def test_non_resident_contract_flag_requiring_for_non_resident_subclient_exception():
    try:
        data_generator(non_resident_clients=0)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'INCOMPATIBLE_INVOICE_PARAMS'))


def test_non_resident_contract_flag_requiring_for_non_resident_subclient_successfull():
    data_generator(non_resident_clients=1)
