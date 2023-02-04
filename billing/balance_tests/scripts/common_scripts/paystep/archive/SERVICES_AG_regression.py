# -*- coding: utf-8 -*-

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

pytestmark = [pytest.mark.priority('mid'),
              pytest.mark.tickets('BALANCE-23943'),
              reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)]

QTY = 100
FIRM_ID = 16
BASE_DT = datetime.datetime.now()
shift = datetime.timedelta
START_DT = str(BASE_DT.strftime("%Y-%m-%d")) + 'T00:00:00'


@pytest.mark.parametrize('person_region', [
    {'description': u'Toloka (Russian region: 142734)',
     'id': '142734',
     'service_id': 42,
     'paysys_id': 1601047,
     'product_id': 507130,
     'is_contract': 0},

    {'description': u'Toloka (Germany: 96))',
     'id': '96',
     'service_id': 42,
     'paysys_id': 1601047,
     'product_id': 507130,
     'is_contract': 0},

    {'description': u'Services AG (service: kupi.bilet))',
     'id': '96',
     'service_id': 114,
     'paysys_id': 1601046,
     'product_id': 502981,
     'is_contract': 1}],
                         ids=lambda x: x['description'])
def test_servicesAG_regression(person_region):
    client_id = None or steps.ClientSteps.create()
    steps.CommonSteps.export('OEBS', 'Client', client_id)

    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt', params={'region': person_region['id']})

    if person_region['is_contract'] == 1:
        contract_id, _ = steps.ContractSteps.create_contract_new('sw_opt_client',
                                                                 {'CLIENT_ID': invoice_owner,
                                                                  'PERSON_ID': person_id,
                                                                  'DT': START_DT,
                                                                  'IS_SIGNED': START_DT,
                                                                  'SERVICES': [person_region['service_id']],
                                                                  'FIRM': FIRM_ID,
                                                                  'PAYMENT_TYPE': 2,
                                                                  'CURRENCY': 978})
        steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    else:
        contract_id = None

    service_order_id = steps.OrderSteps.next_id(person_region['service_id'])
    steps.OrderSteps.create(order_owner, service_order_id, service_id=person_region['service_id'],
                            product_id=person_region['product_id'], params={'AgencyID': agency_id})
    service_order_id2 = steps.OrderSteps.next_id(person_region['service_id'])
    steps.OrderSteps.create(order_owner, service_order_id2, service_id=person_region['service_id'],
                            product_id=person_region['product_id'], params={'AgencyID': agency_id})
    service_order_id3 = steps.OrderSteps.next_id(person_region['service_id'])
    steps.OrderSteps.create(order_owner, service_order_id3, service_id=person_region['service_id'],
                            product_id=person_region['product_id'], params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': person_region['service_id'], 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, person_region['paysys_id'], credit=0,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(person_region['service_id'], service_order_id, {'Bucks': QTY, 'Money': 0}, 0,
                                      BASE_DT)
    acts = steps.ActsSteps.generate(client_id, force=0, date=BASE_DT)
    act_id = acts[0]


    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    # Плательщик выгружается только вместе с другими объектами (счётом или договором)
    steps.CommonSteps.export('OEBS', 'Person', person_id)
    steps.CommonSteps.export('OEBS', 'Act', act_id)

    act_id = acts[0]

    return {'person_id': person_id, 'contract_id': contract_id, 'request_id': request_id,
            'paysys_id': person_region['paysys_id'], 'invoice_id': invoice_id, 'act_id': act_id}


# ---------------------------------------------------------------------------------------------------------------
# MT: https://st.yandex-team.ru/BALANCE-23997

# from btestlib.data import defaults
# from btestlib import environments as env
#
# # Для каждого
# paypreview = "{0}https://balance.greed-{1}.yandex.ru/paypreview.xml?person_id={2}&request_id={3}" \
#              "&paysys_id={4}&contract_id={5}&coupon=&mode=ci"
# reporter.log((paypreview.format(defaults.AUTO_PREFIX, env.balance_env().name, person_id, request_id, PAYSYS_ID,)
#                          contract_id if contract_id else ''))
#
# # Только для предоплатных счетов (credit = 0)
# invoice_print_form = "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&object_id={2}"
# reporter.log((invoice_print_form.format(defaults.AUTO_PREFIX, env.balance_env().name, invoice_id)))
#
# # Для каждого
# invoice_ci = "{0}https://balance.greed-{1}.yandex.ru/invoice.xml?invoice_id={2}"
# reporter.log((invoice_ci.format(defaults.AUTO_PREFIX, env.balance_env().name, invoice_id)))
#
# # Toлько для постоплатных счетов (credit = 1)
# act_print_form = "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&rt=act&object_id={2}"
# reporter.log((act_print_form.format(defaults.AUTO_PREFIX, env.balance_env().name, act_id)))
#
# # Для каждого
# act_ereport = "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&rt=erep&object_id={2}"
# reporter.log((act_ereport.format(defaults.AUTO_PREFIX, env.balance_env().name, act_id)))

# ---------------------------------------------------------------------------------------------------------------

if __name__ == "__main__":
    pytest.main('-v')
