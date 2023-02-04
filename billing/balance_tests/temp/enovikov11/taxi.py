import xmlrpclib
from btestlib.constants import Services
from balance import balance_steps as steps
from datetime import datetime
import balance.balance_api as api
import balance.balance_db as db
from btestlib.data.partner_contexts import TAXI_BV_KZ_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_ARM_USD_CONTEXT, \
    TAXI_BV_UZB_USD_CONTEXT, TAXI_BV_RS_EUR_CONTEXT, TAXI_BV_EST_EUR_CONTEXT, TAXI_BV_LT_EUR_CONTEXT, TAXI_BV_LAT_EUR_CONTEXT, \
    TAXI_BV_FIN_EUR_CONTEXT, TAXI_BV_KGZ_USD_CONTEXT, TAXI_BV_MD_EUR_CONTEXT, TAXI_BV_CIV_EUR_CONTEXT, TAXI_UBER_BV_AZN_USD_CONTEXT, \
    TAXI_UBER_BV_BY_BYN_CONTEXT, TAXI_GHANA_USD_CONTEXT

DEFAULT_QTY = 50
EXPECTED_INVOICE_TYPE = 'charge_note'


def create_invoice(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=1)

    service_order_id = api.medium().GetOrdersInfo({'ContractID': contract_id})[0]['ServiceOrderID']
    orders_list = [{'ServiceID': Services.TAXI_111.id, 'ServiceOrderID': service_order_id, 'Qty': DEFAULT_QTY, 'BeginDT': datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': datetime.now(), 'InvoiceDesireType': 'charge_note'})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0, contract_id=contract_id)

    db.balance().execute("select type from t_invoice where id = " + str(invoice_id))[0]['type']

    return invoice_id


fop_fixtures_contexts = [
    ["invoice.kz.taxi-bv.multicols", TAXI_BV_KZ_USD_CONTEXT],
    ["invoice.ge.taxi-bv.multicols", TAXI_BV_GEO_USD_CONTEXT],
    ["invoice.am.taxi-bv.multicols", TAXI_BV_ARM_USD_CONTEXT],
    ["invoice.uz.taxi-bv.multicols", TAXI_BV_UZB_USD_CONTEXT],
    ["invoice.rs.taxi-bv.multicols", TAXI_BV_RS_EUR_CONTEXT],
    ["invoice.ee.taxi-bv.multicols", TAXI_BV_EST_EUR_CONTEXT],
    ["invoice.lt.taxi-bv.multicols", TAXI_BV_LT_EUR_CONTEXT],
    ["invoice.lv.taxi-bv.multicols", TAXI_BV_LAT_EUR_CONTEXT],
    ["invoice.fi.taxi-bv.multicols", TAXI_BV_FIN_EUR_CONTEXT],
    ["invoice.kg.taxi-bv.multicols", TAXI_BV_KGZ_USD_CONTEXT],
    ["invoice.md.taxi-bv.multicols", TAXI_BV_MD_EUR_CONTEXT],
    ["invoice.ci.taxi-bv.multicols", TAXI_BV_CIV_EUR_CONTEXT],

    ["invoice.az.uber-ml-bv.multicols", TAXI_UBER_BV_AZN_USD_CONTEXT],
    ["invoice.be.uber-ml-bv.multicols", TAXI_UBER_BV_BY_BYN_CONTEXT],

    ["invoice.gh.mlu-africa-bv.multicols", TAXI_GHANA_USD_CONTEXT]
]


results = []


for [fixture_name, context] in fop_fixtures_contexts:
    try:
        result = create_invoice(context)
    except xmlrpclib.Fault:
        result = None

    results.append([fixture_name, result])


print('\n\n')
for [fixture_name, result] in results:
    if result is None:
        print(fixture_name + ' fail')
    else:
        print(fixture_name + ' ' + str(result) + ' https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=' + str(result) + ' https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=' + str(result))


# invoice.kz.taxi-bv.multicols 121017341 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017341 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017341
# invoice.ge.taxi-bv.multicols 121017346 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017346 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017346
# invoice.am.taxi-bv.multicols 121017390 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017390 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017390
# invoice.uz.taxi-bv.multicols 121017349 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017349 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017349
# invoice.rs.taxi-bv.multicols 121017352 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017352 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017352
# invoice.ee.taxi-bv.multicols 121017355 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017355 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017355
# invoice.lt.taxi-bv.multicols 121017393 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017393 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017393
# invoice.lv.taxi-bv.multicols 121017358 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017358 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017358
# invoice.fi.taxi-bv.multicols 121017361 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017361 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017361
# invoice.kg.taxi-bv.multicols 121017398 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017398 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017398
# invoice.md.taxi-bv.multicols 121017401 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017401 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017401
# invoice.ci.taxi-bv.multicols 121017364 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017364 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017364

# invoice.az.uber-ml-bv.multicols 121017365 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017365 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017365
# invoice.be.uber-ml-bv.multicols 121017366 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017366 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017366

# invoice.gh.mlu-africa-bv.multicols 121017369 https://admin-balance.greed-tm.paysys.yandex.ru/invoice-publish.xml?ft=html&object_id=121017369 https://user-balance.greed-tm.paysys.yandex-team.ru/invoice-for-output.xml?ft=html&object_id=121017369
