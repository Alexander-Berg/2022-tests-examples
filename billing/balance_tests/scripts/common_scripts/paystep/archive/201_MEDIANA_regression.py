# -*- coding: utf-8 -*-
__author__ = 'torvald'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Currencies, Services, Firms, Products, Paysyses

pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)
]

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
# NOW = datetime.datetime(2017, 1, 5)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

QTY = 25

USD = Currencies.USD.num_code
EUR = Currencies.EUR.num_code
CHF = Currencies.CHF.num_code

MEDIANA = Services.MEDIANA


# Тестирование:
# - Продажа от ООО Яндекс прямым клиентам - резидентам РФ, физикам и юрикам, оферта
# - Способы оплаты: банк, кредитная карта, Я.Деньги - в рублях РФ
# - Акты сервиса - односторонние
# - При наличии договора по оферте нельзя выставиться
# - Договора - не агентский, предоплата/постоплата, кредит по сроку, новая лицевая схема
# - Текста оферты пока нет - протестируем отдельно
# - Продукт - фишка, цена - 1р. за фишку с НДС
# - Заказы, счета, открутки, акты, выгрузка документов в OEBS

@pytest.mark.parametrize('p', [
    # По оферте
    utils.aDict({'credit': 0}),

    # "Не агентский" договор, постоплата (агентский ЛС), партнёрский кредит
    utils.aDict({'contract_type': 'no_agency',
                 'contract_params': utils.aDict({'SERVICES': [MEDIANA.id],
                                                 'FIRM': Firms.YANDEX_1.id,
                                                 'PAYMENT_TYPE': 3,
                                                 'CURRENCY': Currencies.RUB.num_code,
                                                 'PERSONAL_ACCOUNT': 1,
                                                 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                 'CREDIT_LIMIT_SINGLE': 5000000,
                                                 }),
                 'credit': 1}),

])
def test_smoke_MEDIANA(p):
    service = MEDIANA
    product = Products.MEDIANA

    agency_id = None
    client_id = steps.ClientSteps.create()
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = steps.PersonSteps.create(invoice_owner, 'ur')

    # Договор
    contract_id = None
    if p.get('contract_type', None):
        default_contract_params = {'PERSON_ID': person_id,
                                   'CLIENT_ID': invoice_owner,
                                   'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                   'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO}
        default_contract_params.update(p.contract_params)
        contract_id, _ = steps.ContractSteps.create_contract_new(p.contract_type, default_contract_params)

    credit = p.get('credit', 0)

    service_order_id = steps.OrderSteps.next_id(product.service.id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, product.id, product.service.id,
                                       {'AgencyID': agency_id})
    orders_list = [{'ServiceID': product.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': NOW})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id,
                                                 person_id=person_id,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 contract_id=contract_id,
                                                 credit=credit,
                                                 overdraft=0)

    # Для кредитных (старая схема ЛС) можно оплачивать сам счёт.
    # Особая логика оплаты (как для фиктивных и Агентских ЛС не нужна
    if not credit:
        steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(product.service.id, orders_list[0]['ServiceOrderID'],
                                      {product.type.code: QTY}, 0, NOW)

    acts = steps.ActsSteps.generate(invoice_owner, force=1, date=NOW)
    act_id = acts[0]

    repayment_invoice_id = None
    if credit:
        repayment_invoice_id = db.get_y_invoices_by_fpa_invoice(invoice_id)[0]['id']

    collateral_id = None
    if contract_id:
        collateral_id = db.get_collaterals_by_contract(contract_id)[0]['id']

    steps.ExportSteps.export_oebs(client_id=invoice_owner, contract_id=contract_id, collateral_id=collateral_id,
                                  invoice_id=repayment_invoice_id or invoice_id, person_id=person_id,
                                  act_id=act_id)


if __name__ == '__main__':
    pass
    pytest.main('-v test_OFD_smoke.py --publish_results')
