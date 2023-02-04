# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.CERTIFICATE, Features.COMPENSATION),
              pytest.mark.tickets('BALANCE-22186')]

region_params = [
{'region_id': 84, 'region_name': u'США', 'firm_id':1, 'paysys_id_ce': 1006, 'paysys_id_co': 1007, 'service_id':7, 'product_id':1475},
{'region_id': 126, 'region_name': u'Швейцария', 'firm_id':7, 'paysys_id_ce': 11057, 'paysys_id_co': 11051, 'service_id':7, 'product_id':1475},
{'region_id': 149, 'region_name': u'Беларусь', 'firm_id':7, 'paysys_id_ce': 11057, 'paysys_id_co': 11054, 'service_id':7, 'product_id':1475},
{'region_id': 159, 'region_name': u'Казахстан', 'firm_id':3, 'paysys_id_ce': 11058, 'paysys_id_co': 11052, 'service_id':7, 'product_id':1475},
{'region_id': 181, 'region_name': u'Израиль', 'firm_id':7, 'paysys_id_ce': 11057, 'paysys_id_co': 11051, 'service_id':7, 'product_id':1475},
{'region_id': 187, 'region_name': u'Украина', 'firm_id':2, 'paysys_id_ce': 11059, 'paysys_id_co': 11053, 'service_id':7, 'product_id':1475},
{'region_id': 225, 'region_name': u'Россия', 'firm_id':1, 'paysys_id_ce': 1006, 'paysys_id_co': 1007, 'service_id':7, 'product_id':1475},
{'region_id': 983, 'region_name': u'Турция', 'firm_id':8, 'paysys_id_ce': 11048, 'paysys_id_co': 11054, 'service_id':7, 'product_id':1475},
 ]

region_list_ids = [x['region_name'] for x in region_params]

def pay_with_certificate(region_param):
    region_id = region_param['region_id']
    service_id = region_param['service_id']
    product_id = region_param['product_id']
    client_id = steps.ClientSteps.create({'REGION_ID':region_id})
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id, service_id=service_id)
    try:
        invoice_id_ce = steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id, sum=10, type = 'ce')
    except Exception, exc:
        reporter.log(exc)
        if steps.CommonSteps.get_exception_code(exc) == 'PAYSYS_NOT_FOUND':
            invoice_id_ce = 'PAYSYS_NOT_FOUND'
        elif steps.CommonSteps.get_exception_code(exc) == 'NOT_FOUND':
            invoice_id_ce = 'NOT_FOUND'
    try:
        invoice_id_co = steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id, sum=10, type = 'co')
    except Exception, exc:
        reporter.log(exc)
        if steps.CommonSteps.get_exception_code(exc) == 'PAYSYS_NOT_FOUND':
            invoice_id_co = 'PAYSYS_NOT_FOUND'
        elif steps.CommonSteps.get_exception_code(exc) == 'NOT_FOUND':
            invoice_id_co = 'NOT_FOUND'
    return invoice_id_ce, invoice_id_co


@pytest.mark.parametrize('region_param', region_params
    , ids=region_list_ids)
def test_pay_with_certificate(region_param):
    invoice_id_ce, invoice_id_co = pay_with_certificate(region_param)
    if invoice_id_ce == 'PAYSYS_NOT_FOUND' or invoice_id_co == 'PAYSYS_NOT_FOUND':
        assert region_param['region_name'] in [u'США']
    elif invoice_id_ce == 'NOT_FOUND':
        assert region_param['region_name'] in [u'Беларусь']
    else:
        assert region_param['paysys_id_ce'] == db.get_invoice_by_id(invoice_id_ce)[0]['paysys_id']
        assert region_param['firm_id'] == db.get_invoice_by_id(invoice_id_ce)[0]['firm_id']
        assert region_param['paysys_id_co'] == db.get_invoice_by_id(invoice_id_co)[0]['paysys_id']
        assert region_param['firm_id'] == db.get_invoice_by_id(invoice_id_co)[0]['firm_id']

if __name__ == "__main__":
    pytest.main("ce_cc_firm.py -v")