# -*- coding: utf-8 -*-

# какой тип котракта использовать?

import datetime
import pytest
import hamcrest

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import ContractCommissionType, Firms, ContractPaymentType, Products, Services, Paysyses, \
    Currencies, Regions
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                          currency=Currencies.KZT)

MARKET_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.DIRECT_KZT,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.YT_KZU,
                                                          paysys=Paysyses.BANK_YT_KZT_TNG_MARKET, region=Regions.RU,
                                                          currency=Currencies.KZT)

DIRECT_YANDEX_FIRM_YT_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT,
                                                                 firm=Firms.KZ_25, person_type=PersonTypes.YT_KZU,
                                                                 paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                                 currency=Currencies.KZT,
                                                                 contract_type=ContractCommissionType.OPT_AGENCY)

QTY = 100
NOW = datetime.datetime.now()
PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
PREVIOUS_MONTH_FIRST_DAY_ISO = utils.Date.date_to_iso_format(PREVIOUS_MONTH_LAST_DAY.replace(day=1))
NOW_ISO = utils.Date.date_to_iso_format(NOW)


def get_taxes(product_id, currency, region_id, resident):
    taxes = db.balance().execute('''SELECT *
                                FROM (SELECT
                                        t_tax_policy_pct.nds_pct       AS nds_pct,
                                        t_tax_policy_pct.nsp_pct       AS nsp_pct,
                                        t_tax_policy_pct.id       AS id
                                      FROM t_tax_policy_pct
                                      WHERE t_tax_policy_pct.tax_policy_id = (SELECT TAX_POLICY_ID
                                                                              FROM t_tax
                                                                                LEFT OUTER JOIN t_currency ON t_currency.num_code = t_tax.currency_id
                                                                                LEFT OUTER JOIN t_tax_policy ON t_tax_policy.id = t_tax.tax_policy_id
                                                                              WHERE
                                                                                {0} = t_tax.product_id AND t_tax.hidden = 0 AND t_tax.dt <= sysdate
                                                                                AND
                                                                                (t_tax.tax_policy_id IS NULL AND t_currency.char_code = '{1}' OR
                                                                                 t_tax.tax_policy_id IS NOT NULL AND t_tax_policy.hidden = 0 AND
                                                                                 {2} = t_tax_policy.region_id AND
                                                                                 t_tax_policy.resident = {3})) AND t_tax_policy_pct.hidden = 0 AND
                                            t_tax_policy_pct.dt <= sysdate
                                      ORDER BY t_tax_policy_pct.dt DESC)
                                WHERE ROWNUM <= 1'''.format(product_id, currency, region_id, resident))
    return taxes[0]['id'], taxes[0]['nds_pct'], taxes[0]['nsp_pct']


@pytest.mark.parametrize('context, params, expected',
                         [
                             (DIRECT_KZ_FIRM_KZU,
                              {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': True,
                               'person_resident': 1, 'tax_policy_pct_id': 7}, {'is_exception': True}),
                             # выключено https://st.yandex-team.ru/BALANCE-33848
                             # (MARKET_KZ_FIRM_KZU,
                             #  {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': True,
                             #   'person_resident': 0, 'tax_policy_pct_id': 3}, {'is_exception': False})
                         ])
def test_consume_with_nds(context, params, expected):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id)
    order = db.get_order_by_id(order_id)[0]
    utils.check_that(order['product_currency'], hamcrest.equal_to('KZT'))
    utils.check_that(order['product_iso_currency'], hamcrest.equal_to('KZT'))
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    tax_policy_pct_id = db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id']
    utils.check_that(tax_policy_pct_id, hamcrest.equal_to(params['tax_policy_pct_id']))
    tax_policy_pct_id_new, nds_pct, nsp_pct = get_taxes(params['new_product'].id, context.currency.char_code,
                                                        context.region.id,
                                                        resident=params['person_resident'])
    try:
        order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                           product_id=params['new_product'].id,
                                           params={"ForceProductChange": params['ForceProductChange'],
                                                   'OrderID': order_id})
        utils.check_that(expected['is_exception'], hamcrest.equal_to(False))
        utils.check_that(int(nds_pct) + int(nsp_pct), hamcrest.equal_to(0))
        tax_policy_pct_id_new_db = db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id']
        utils.check_that(tax_policy_pct_id_new, hamcrest.equal_to(tax_policy_pct_id_new_db))
        order = db.get_order_by_id(order_id)[0]
        utils.check_that(order['product_currency'], hamcrest.equal_to(None))
        utils.check_that(order['product_iso_currency'], hamcrest.equal_to(None))
    except Exception, exc:
        print exc
        utils.check_that('ORDER_PRODUCT_IMMUTABLE',
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'code')))
        utils.check_that(expected['is_exception'], hamcrest.equal_to(True))
        utils.check_that(int(nds_pct) + int(nsp_pct), hamcrest.is_not(0))
        tax_policy_pct_id_new_db = db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id']
        utils.check_that(tax_policy_pct_id_new_db, hamcrest.equal_to(7))


@pytest.mark.parametrize('context, params, expected',
                         [
                             # (DIRECT_KZ_FIRM_KZU,
                             #  {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': True,
                             #   'person_resident': 1}, {'is_exception': False}),
                             # выключено https://st.yandex-team.ru/BALANCE-33848
                             # (MARKET_KZ_FIRM_KZU,
                             #  {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': True,
                             #   'person_resident': 0}, {'is_exception': False})
                         ])
def consume_with_nds_certificate(context, params, expected):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id)
    order = db.get_order_by_id(order_id)[0]
    utils.check_that(order['product_currency'], hamcrest.equal_to('KZT'))
    utils.check_that(order['product_iso_currency'], hamcrest.equal_to('KZT'))
    invoice_id = steps.InvoiceSteps.pay_with_certificate_or_compensation(order_id, 112)
    tax_policy_pct_id = db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id']
    utils.check_that(tax_policy_pct_id, hamcrest.equal_to(281))
    tax_policy_pct_id_new, nds_pct, nsp_pct = get_taxes(params['new_product'].id, context.currency.char_code,
                                                        context.region.id,
                                                        resident=params['person_resident'])
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=params['new_product'].id,
                                       params={"ForceProductChange": params['ForceProductChange'],
                                               'OrderID': order_id})
    utils.check_that(expected['is_exception'], hamcrest.equal_to(False))
    utils.check_that(int(nds_pct) + int(nsp_pct), hamcrest.equal_to(0))
    order = db.get_order_by_id(order_id)[0]
    utils.check_that(order['product_currency'], hamcrest.equal_to(None))
    utils.check_that(order['product_iso_currency'], hamcrest.equal_to(None))


@pytest.mark.parametrize('context, params, expected', [
    # выключено https://st.yandex-team.ru/BALANCE-33848
    # (DIRECT_KZ_FIRM_KZU, {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': True},
    #  {'is_exception': False}),
    (DIRECT_KZ_FIRM_KZU, {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': False},
     {'is_exception': True}),
    (DIRECT_KZ_FIRM_KZU, {'new_product': Products.DIRECT_RUB, 'ForceProductChange': True},
     {'is_exception': True}),
    (DIRECT_KZ_FIRM_KZU.new(product=Products.DIRECT_RUB),
     {'new_product': Products.DIRECT_KZT_QUASI, 'ForceProductChange': True},
     {'is_exception': True}),
])
def test_change_product_in_order(context, params, expected):
    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id)
    try:
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=params['new_product'].id,
                                params={"ForceProductChange": params['ForceProductChange'], 'OrderID': order_id})
        utils.check_that(expected['is_exception'], hamcrest.equal_to(False))
    except Exception, exc:
        utils.check_that('ORDER_PRODUCT_IMMUTABLE',
                         hamcrest.equal_to(steps.CommonSteps.get_exception_code(exc, 'code')))
        utils.check_that(expected['is_exception'], hamcrest.equal_to(True))
