# -*- coding: utf-8 -*-

import datetime
import copy
import pytest
from hamcrest import has_entries

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions, \
    ContractCommissionType, Services

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT_USD_SW_OPT = Contexts.DIRECT_FISH_USD_CONTEXT.new(contract_type=ContractCommissionType.SW_OPT_AGENCY,
                                                         person_type=PersonTypes.SW_UR)

# DIRECT = 7
# DIRECT_PRODUCT = Product(7, 1475, 'Bucks', 'Money')
# RUB = 810
UZB_DISCOUNT_POLICY = 16

PAYSYS_MAP = {Currencies.USD.num_code: 1044}

DEFAULT_CONTRACT_PARAMS = {'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                           'DISCOUNT_POLICY_TYPE': UZB_DISCOUNT_POLICY}

SW_OPT_AGENCY_CONTRACT = DEFAULT_CONTRACT_PARAMS.update({'SERVICES': [Services.DIRECT.id],
                                                         'CURRENCY': Currencies.USD.num_code})


def prepare_budget(**kwargs):
    acts = kwargs['acts']
    agency_id = kwargs['agency_id']
    person_id = kwargs['person_id']
    contract_id = kwargs['contract_id']
    paysys_id = kwargs['paysys_id']
    credit = kwargs['credit']
    product = kwargs.get('product', False)
    service = kwargs.get('service', False)
    for completions, dt in acts:
        # Создаём отдельного субклиента для каждого счёта:
        tmp_client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'REGION_ID': 149})
        # Счёт выставляем на сумму, отличаюущуюся от желаемой суммы акта:
        qty_for_invoice = completions

        # Выставляем счёт на указанную сумму, оплачиваем его, откручиваем и актим
        campaigns_list = [
            {'client_id': tmp_client_id, 'service_id': service.id, 'product_id': product.id,
             'qty': qty_for_invoice,
             'begin_dt': dt}
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(tmp_client_id,
                                                                                person_id,
                                                                                campaigns_list,
                                                                                paysys_id,
                                                                                dt,
                                                                                agency_id=agency_id,
                                                                                credit=0,
                                                                                contract_id=contract_id,
                                                                                overdraft=0,
                                                                                manager_uid=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(DIRECT_USD_SW_OPT.service.id, orders_list[0]['ServiceOrderID'],
                                          {product.type.code: completions}, 0, dt)
        steps.ActsSteps.generate(agency_id, 1, dt)


def checker(agency_id, contract_id, client_id, qty, product, request_dt):
    # Проверяем ответ метода EstimateDiscount
    print product.id
    result = steps.DiscountSteps.estimate_discount(
        {'ClientID': agency_id, 'PaysysID': 1044, 'ContractID': contract_id},
        [{'ProductID': product.id, 'ClientID': client_id, 'Qty': qty, 'ID': 1,
          'BeginDT': request_dt, 'RegionID': None, 'discard_agency_discount': 0}])
    reporter.log(result)
    result = steps.DiscountSteps.get_client_discounts_all({'ClientID': agency_id, 'DT': request_dt})
    reporter.log(result)


@pytest.mark.parametrize('contract_params', [(DEFAULT_CONTRACT_PARAMS)])
@pytest.mark.parametrize('contract_type', [ContractCommissionType.SW_OPT_AGENCY])
@pytest.mark.parametrize('product', [(Products.DIRECT_FISH)])
@pytest.mark.parametrize('params', [

    {'additional_contract_params': {'DT': '2017-01-01T00:00:00',
                                    'DEAL_PASSPORT': '2017-01-01T00:00:00',
                                    'CONTRACT_DISCOUNT': '11'},

     'acts': [(19513, datetime.datetime(2017, 2, 5, 0, 0, 0)),
              (19513, datetime.datetime(2017, 2, 10, 0, 0, 0)),
              (19513, datetime.datetime(2017, 2, 15, 0, 0, 0)),
              (19513, datetime.datetime(2017, 2, 25, 0, 0, 0)),
              (19513, datetime.datetime(2017, 2, 28, 0, 0, 0))
              ],
     'target_invoice': (Products.DIRECT_FISH, 100, datetime.datetime(2017, 3, 11, 0, 0, 0)),
     'target_currency': Currencies.USD,
     'expected_discount_pct': 11
     },
]
                         )
def test_2018_UZB(contract_params, contract_type, product, params):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, PersonTypes.SW_UR.code, {})
    contract_params = copy.deepcopy(contract_params)
    contract_params.update(params['additional_contract_params'])
    contract_params.update({'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id})
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    contract_currency = contract_params['CURRENCY']
    prepare_budget(acts=params['acts'], agency_id=invoice_owner, person_id=person_id,
                   contract_id=contract_id, paysys_id=PAYSYS_MAP[contract_currency], credit=0, product=product,
                   service=DIRECT_USD_SW_OPT.service)

    product, qty, request_dt = params['target_invoice']

    checker(agency_id, contract_id, client_id, qty, product, request_dt)

    campaigns_list = [{'service_id': DIRECT_USD_SW_OPT.service.id, 'product_id': product.id, 'qty': qty, 'begin_dt': request_dt}]

    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=PAYSYS_MAP[params['target_currency']],
                                                                  invoice_dt=request_dt,
                                                                  agency_id=agency_id,
                                                                  credit=0,
                                                                  contract_id=contract_id,
                                                                  overdraft=0,
                                                                  )
    steps.InvoiceSteps.pay(invoice_id)

    utils.check_that(db.get_consumes_by_invoice(invoice_id)[0],
                     has_entries({'static_discount_pct': params['expected_discount_pct']}),
                     step=u'Проверяем сумму и скидку в заявке')
