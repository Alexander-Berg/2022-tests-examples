# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

import pytest

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.constants import Services, Products, Firms

pytestmark = [pytest.mark.priority('mid')
    , reporter.feature(Features.ACT, Features.INVOICE, Features.COMMON, Features.CONTRACT)
              ]

# PH_AUTO_RU = 1092
# UR_AUTO_RU = 1091
# Авто.Ру мигрировали в Вертикали
PH_AUTO_RU = 1201001
UR_AUTO_RU = 1201003

AUTO_RU_SERVICE_ID = Services.AUTORU.id
AUTO_RU = Products.AUTORU
# AUTO_RU = Products.Product(id=504697, type=ProductTypes.DAYS, multicurrency_type=None)

MEDIA_SERVICE_ID = Services.MEDIA_70.id
MEDIA = Products.MEDIA_FOR_AUTORU

QTY = 100
BASE_DT = datetime.now()
dt_delta = timedelta

to_iso = utils.Date.date_to_iso_format
NOW = datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))


@pytest.mark.parametrize('p', [
    #
    utils.aDict({'description': u'UR',
                 'person_type': 'ur',
                 'paysys_id': UR_AUTO_RU}),
    #
    utils.aDict({'description': u'PH',
                 'person_type': 'ph',
                 'paysys_id': PH_AUTO_RU})
],
                         ids=lambda x: x['description'])
def test_AutoRU_offer_smoke(p):
    client_id = None or steps.ClientSteps.create()
    # steps.ClientSteps.link(client_id, 'yndx-toloka')

    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, p.person_type)

    contract_id = None

    service_id = AUTO_RU_SERVICE_ID
    product = AUTO_RU

    service_order_id = steps.OrderSteps.next_id(AUTO_RU_SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product.id,
                            params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, p.paysys_id, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {product.type.code: QTY}, 0, BASE_DT)
    acts = steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = acts[0]

    # ---------------------------------------------------------------------------------------------------------------
    # MT: https://st.yandex-team.ru/BALANCE-24082
    # Ключевые значения для наборов параметров:
    # 1: UR - ПФ для юриков
    # 2: PH - Квитанция для физиков
    # ВАЖНО: у Авто.Ру 2 оферты: 48 (для большинства продуктов) и 49 (для продукта Шины и Диски (504697))

    return {'person_id': person_id, 'contract_id': contract_id, 'request_id': request_id, 'paysys_id': p.paysys_id,
            'invoice_id': invoice_id, 'act_id': act_id}


@pytest.mark.parametrize('p', [
    # Предоплата по предоплатному договору
    utils.aDict({'description': u'Prepayment by prepay contract',
                 'agency_creator': lambda: None,
                 'person_type': 'ur',
                 'contract_type': 'auto_ru_non_agency',
                 'contract_params': {'SERVICES': [AUTO_RU_SERVICE_ID],
                                     'FIRM': Firms.VERTICAL_12.id,
                                     'SCALE': 56,
                                     },
                 'credit': 0,
                 'paysys_id': UR_AUTO_RU}),
    #
    pytest.mark.smoke(utils.aDict({'description': u'Fictive personal account by postpay contract',
                                   'agency_creator': lambda: steps.ClientSteps.create({'IS_AGENCY': 1}),
                                   'person_type': 'ur',
                                   'contract_type': 'opt_agency_prem_post',
                                   'contract_params': {'SERVICES': [AUTO_RU_SERVICE_ID],
                                                       'FIRM': Firms.VERTICAL_12.id,
                                                       'SCALE': 56,
                                                       'PERSONAL_ACCOUNT': 1,
                                                       'CREDIT_LIMIT_SINGLE': 500000,
                                                       'LIFT_CREDIT_ON_PAYMENT': 1,
                                                       'PERSONAL_ACCOUNT_FICTIVE': 1
                                                       },
                                   'credit': 1,
                                   'paysys_id': UR_AUTO_RU})),

    utils.aDict({'description': u'Prepayment by postpay contract',
                 'agency_creator': lambda: steps.ClientSteps.create({'IS_AGENCY': 1}),
                 'person_type': 'ur',
                 'contract_type': 'auto_ru_post',
                 'contract_params': {'SERVICES': [AUTO_RU_SERVICE_ID],
                                     'FIRM': Firms.VERTICAL_12.id,
                                     'SCALE': 56,
                                     'PERSONAL_ACCOUNT': 1,
                                     'CREDIT_LIMIT_SINGLE': 500000,
                                     'LIFT_CREDIT_ON_PAYMENT': 1,
                                     'PERSONAL_ACCOUNT_FICTIVE': 1
                                     },
                 'credit': 0,
                 'paysys_id': UR_AUTO_RU}),
],
                         ids=lambda x: x['description'])
def test_AutoRU_contract_smoke(p):
    client_id = steps.ClientSteps.create()
    agency_id = p.agency_creator()
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = steps.PersonSteps.create(invoice_owner, p.person_type)

    contract_params = {'CLIENT_ID': invoice_owner,
                       'PERSON_ID': person_id,
                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO
                       }
    contract_params.update(p.contract_params)

    contract_id, _ = steps.ContractSteps.create_contract(p.contract_type, contract_params)
    # contract_id = None

    service_id = AUTO_RU_SERVICE_ID
    product = AUTO_RU

    service_order_id = steps.OrderSteps.next_id(AUTO_RU_SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product.id,
                            params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, p.paysys_id, credit=p.credit,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {product.type.code: QTY}, 0, BASE_DT)
    acts = steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    act_id = acts[0]

    # ---------------------------------------------------------------------------------------------------------------
    # MT: https://st.yandex-team.ru/BALANCE-24082
    # Ключевые значения для наборов параметров:
    # 1: Предоплата по договору
    # 2: Постоплата по договору (для случая Агентского ЛС - специально достаём id Ы-счёта
    # ВАЖНО: у Авто.Ру 2 оферты: 48 (для большинства продуктов) и 49 (для продукта Шины и Диски (504697))

    if p.credit == 1:
        invoice_id = db.get_y_invoices_by_fpa_invoice(invoice_id)[0]

    return {'person_id': person_id, 'contract_id': contract_id, 'request_id': request_id, 'paysys_id': p.paysys_id,
            'invoice_id': invoice_id, 'act_id': act_id}


def test_AutoRU_in_Vertical_with_SpecProjects_for_Media():
    client_id = None or steps.ClientSteps.create()
    # steps.ClientSteps.link(client_id, 'yndx-toloka')

    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                          'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                          'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                          # 'is_signed': None,
                                                          'SERVICES': [MEDIA_SERVICE_ID],
                                                          'FIRM': Firms.VERTICAL_12.id,
                                                          'SCALE': 3,
                                                          # 'COMMISSION_TYPE': 48,
                                                          # 'NON_RESIDENT_CLIENTS': 0,
                                                          # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                          # 'REPAYMENT_ON_CONSUME': 0,
                                                          'PERSONAL_ACCOUNT': 1,
                                                          'CREDIT_LIMIT_SINGLE': 500000,
                                                          'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })
    # contract_id = None

    service_id = MEDIA_SERVICE_ID
    product = MEDIA

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product.id,
                            params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, UR_AUTO_RU, credit=1, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {product.type.code: QTY}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    acts = db.get_acts_by_invoice(invoice_id)
    pass
