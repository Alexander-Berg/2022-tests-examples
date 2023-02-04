# coding: utf-8

import datetime as d

import pytest
from dateutil.relativedelta import relativedelta
from enum import Enum
from hamcrest import equal_to

from balance import balance_steps as steps
from balance import balance_db as db
from balance import balance_web as web
from balance.features import Features
from btestlib import constants as c
from btestlib import utils, reporter
from temp.igogor import balance_objects as o

pytestmark = [
    reporter.feature(Features.DISCOUNT),
    pytest.mark.tickets('BALANCE-28470,BALANCE-30638')
    # https://wiki.yandex-team.ru/balance/tz/discounts2018/#shvejjcarija
]


class SwDiscountPolicies(Enum):
    NO_DISCOUNT = '0'  # type: SwDiscountPolicies
    FIXED = '8'  # type: SwDiscountPolicies
    REST_CIS = '24'  # type: SwDiscountPolicies


CURRENT_MONTH_START = utils.Date.first_day_of_month(d.datetime.now())
PREV_MONTH_START = CURRENT_MONTH_START + relativedelta(months=-1)
DISCOUNT_START = d.datetime(2021, 1, 1)

CONTEXT = o.Contexts.DIRECT_MONEY_USD_CONTEXT.new(
    name='REST_CIS_DISCOUNT_CONTEXT',
    person_type=c.PersonTypes.SW_UR,
    paysys=c.Paysyses.BANK_SW_UR_USD,
    region=c.Regions.UZB,
    currency=c.Currencies.USD,
    # contract_dt=DISCOUNT_START + relativedelta(days=-1),
    contract_dt=DISCOUNT_START,
    discount_policy=SwDiscountPolicies.REST_CIS,
    fixed_discount=None,
    agency_template=lambda: {'IS_AGENCY': 1, 'REGION_ID': c.Regions.SW.id},
    client_template=lambda context: {'IS_AGENCY': 0, 'REGION_ID': context.region.id},
    contract_template=lambda context, agency_id, person_id: (
        c.ContractCommissionType.SW_OPT_AGENCY,
        utils.remove_false({'CLIENT_ID': agency_id,
                            'PERSON_ID': person_id,
                            'DT': utils.Date.to_iso(context.contract_dt),
                            'FINISH_DT': utils.Date.to_iso(context.contract_dt + relativedelta(years=3)),
                            'IS_SIGNED': utils.Date.to_iso(d.datetime.now()),
                            'SERVICES': [context.service.id, c.Services.MARKET.id],
                            'CURRENCY': context.currency.num_code,
                            'DISCOUNT_POLICY_TYPE': context.discount_policy.value,
                            'CONTRACT_DISCOUNT': context.fixed_discount,
                            'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25
                            })),
    order_template=lambda context_, client_id, agency_id, service_order_id: (
        client_id, service_order_id, context_.product.id, context_.service.id, {'AgencyID': agency_id}
    ),
    request_order_template=lambda context_, service_order_id, qty: {'ServiceID': context_.service.id,
                                                                    'ServiceOrderID': service_order_id,
                                                                    'Qty': qty,
                                                                    'BeginDT': d.datetime.now()},
)

CONTEXT_MEDIA = o.Contexts.DIRECT_MONEY_USD_CONTEXT.new(
    service=c.Services.MEDIA_70,
    product=c.Products.MEDIA_UZ,
    name='REST_CIS_DISCOUNT_CONTEXT',
    person_type=c.PersonTypes.SW_YT,
    paysys=c.Paysyses.BANK_SW_YT_USD,
    region=c.Regions.UZB,
    currency=c.Currencies.USD,
    firm=c.Firms.EUROPE_AG_7,
    # contract_dt=DISCOUNT_START + relativedelta(days=-1),
    contract_dt=DISCOUNT_START,
    discount_policy=SwDiscountPolicies.REST_CIS,
    fixed_discount=None,
    agency_template=lambda: {'IS_AGENCY': 1, 'REGION_ID': c.Regions.UZB.id},
    client_template=lambda context: {'IS_AGENCY': 0, 'REGION_ID': context.region.id},
    contract_template=lambda context, agency_id, person_id: (
        c.ContractCommissionType.SW_OPT_AGENCY,
        utils.remove_false({'CLIENT_ID': agency_id,
                            'PERSON_ID': person_id,
                            'DT': utils.Date.to_iso(context.contract_dt),
                            'FINISH_DT': utils.Date.to_iso(context.contract_dt + relativedelta(years=1)),
                            'IS_SIGNED': utils.Date.to_iso(d.datetime.now()),
                            'SERVICES': [context.service.id, c.Services.MARKET.id],
                            'CURRENCY': context.currency.num_code,
                            'DISCOUNT_POLICY_TYPE': context.discount_policy.value,
                            'CONTRACT_DISCOUNT': context.fixed_discount,
                            'FIRM': context.firm.id,
                            'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25
                            })),
    order_template=lambda context_, client_id, agency_id, service_order_id: (
        client_id, service_order_id, context_.product.id, context_.service.id, {'AgencyID': agency_id}
    ),
    request_order_template=lambda context_, service_order_id, qty: {'ServiceID': context_.service.id,
                                                                    'ServiceOrderID': service_order_id,
                                                                    'Qty': qty,
                                                                    'BeginDT': d.datetime.now()},
)


@pytest.fixture(scope='module', autouse=True)
def _set_up_switch_date():
    # BALANCE-36740: Надо учитывать скидки только после переключения
    with reporter.step(u'Переключаем скидочную шкалу полностью на новую'):
        queries = [
            """
            update bo.t_scale_points
            set end_dt = :dt
            where 1=1
            and scale_code = 'rest_cis_agency_discount'
            and end_dt is not null
            """,
            """
            update bo.t_scale_points
            set start_dt = :dt
            where 1=1
            and scale_code = 'rest_cis_agency_discount'
            and start_dt is not null
            """,
        ]
        for query in queries:
            db.balance().execute(query, {'dt': DISCOUNT_START})


def _setup_test_recalculation_date(contract_dt_shift):
    with reporter.step(u'Подготавливаем акт в первом месяце действия договора'):
        context = CONTEXT.new(contract_dt=CURRENT_MONTH_START + contract_dt_shift)
        agency_id, person_id, contract_id = prepare_agency(context)
        client_id = steps.ClientSteps.create(context.client_template(context))

        act_dt = (DISCOUNT_START if context.contract_dt.date() <= DISCOUNT_START.date()
                  else context.contract_dt)
        prepare_budget(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id, client_id=client_id,
                       act_qty_date_pairs=[(1000.0, act_dt)])

        return context, agency_id, person_id, contract_id, client_id, act_dt


@pytest.mark.parametrize('contract_dt_shift, day_of_month, expected_dsc', [
    pytest.param(relativedelta(days=0), 9,
                 {'next_budget': '300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '0', 'budget': '0',
                  'budget_discount': '0', 'discount': '0', 'next_discount': '10', 'type': 'budget'},
                 id='not_shifted_not_recalculated_on_9th_day'),
    pytest.param(relativedelta(days=0), 10, {'next_budget': '1300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '13',
                      'budget': '928.51', 'budget_discount': '13', 'discount': '13', 'next_discount': '15', 'type': 'budget'},
                 id='not_shifted_recalculated_on_10th_day'),
    # some do not work at the first day of month (contract with shifted start is not active)
    # base shift makes no difference (no retrocalculation for discount)
    # pytest.param(relativedelta(days=1), 9,
    #              {'next_budget': '300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '0', 'budget': '0',
    #               'budget_discount': '0', 'discount': '0', 'next_discount': '10', 'type': 'budget'},
    #              id='shifted_not_recalculated_on_9th_day'),
    # pytest.param(relativedelta(days=1), 10, {'next_budget': '1300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '13',
    #                   'budget': '928.51', 'budget_discount': '13', 'discount': '13', 'next_discount': '15', 'type': 'budget'},
    #              id='shifted_recalculated_on_10th_day'),
])
@pytest.mark.smoke
def test_recalculation_date(contract_dt_shift, day_of_month, expected_dsc):
    with reporter.step(u'Были подготовлены акты в первом месяце действия договора'):
        data = _setup_test_recalculation_date(contract_dt_shift)
        reporter.attach(u'Подготовленные данные',
                        dict(zip("agency_id, person_id, contract_id, client_id, act_dt".split(", "), data[1:])))
        context, agency_id, person_id, contract_id, client_id, act_dt = data

    request_dt = act_dt + relativedelta(months=1, day=day_of_month)
    # open_paypreview(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
    #                 client_id=client_id, request_dt=request_dt)
    dsc_data = get_discount(context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=100.11)
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


def _setup_test_budget_periods(contract_dt_shift):
    with reporter.step(u'Подготавливаем акты в 5 месяцах начиная с предыдущего'):
        context = CONTEXT.new(contract_dt=CURRENT_MONTH_START + contract_dt_shift)
        agency_id, person_id, contract_id = prepare_agency(context)
        client_id = steps.ClientSteps.create(context.client_template(context))

        prepare_budget(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id, client_id=client_id,
                       act_qty_date_pairs=[(10 ** (i + 2),
                                            context.contract_dt + relativedelta(months=i, days=10))
                                           for i in range(5)])

        return context, agency_id, person_id, contract_id, client_id


@pytest.mark.parametrize('contract_dt_shift, request_dt_shift, expected_dsc', [
    pytest.param(relativedelta(days=0),
                 relativedelta(months=1, day=9),
                 {'next_budget': '300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '0', 'budget': '0',
                  'budget_discount': '0', 'discount': '0', 'next_discount': '10', 'type': 'budget'},
                 id='Not_shifted_Less_than_month_No_budget'),
    pytest.param(relativedelta(days=0),
                 relativedelta(months=1, day=10),
                 {'next_budget': '300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '0', 'budget': '92.85',
                  'budget_discount': '0', 'discount': '0', 'next_discount': '10', 'type': 'budget'},
                 id='Not_shifted_One_month_Its_budget'),
    pytest.param(relativedelta(days=0),
                 relativedelta(months=2, day=15),
                 {'next_budget': '650', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '10',
                  'budget': '510.68', 'budget_discount': '10', 'discount': '10', 'next_discount': '13', 'type': 'budget'},
                 id='Not_shifted_Two_monts_Average_for_two'),
    pytest.param(relativedelta(days=0),
                 relativedelta(months=3, day=20),
                 {'next_budget': '6500', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '20',
                  'budget': '3125.97', 'budget_discount': '20', 'discount': '20', 'next_discount': '30', 'type': 'budget'},
                 id='Not_shifted_Three_months_Average_for_three'),
    pytest.param(relativedelta(days=0),
                 relativedelta(months=4, day=25),
                 {'budget_discount': '50', 'discount': '50', 'name': 'rest_cis_agency_discount_policy',
                  'type': 'budget', 'AgencyDiscountPct': '50', 'budget': '27855.15666666666666666666667'},
                 id='Not_shifted_Four_months_Average_for_last_three'),
    # some do not work at the first day of month (contract with shifted start is not active)
    # base shift makes no difference (no retrocalculation for discount)
    # pytest.param(relativedelta(days=1),
    #              relativedelta(months=1, day=9),
    #              {'next_budget': '300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '0', 'budget': '0',
    #               'budget_discount': '0', 'discount': '0', 'next_discount': '10', 'type': 'budget'},
    #              id='Shifted_Less_than_month_No_budget'),
    # pytest.param(relativedelta(days=1),
    #              relativedelta(months=1, day=10),
    #              {'next_budget': '300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '0', 'budget': '92.85',
    #               'budget_discount': '0', 'discount': '0', 'next_discount': '10', 'type': 'budget'},
    #              id='Shifted_One_month_Its_budget'),
    # pytest.param(relativedelta(days=1),
    #              relativedelta(months=2, day=15),
    #              {'next_budget': '1300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '13',
    #               'budget': '928.51', 'budget_discount': '13', 'discount': '13', 'next_discount': '15', 'type': 'budget'},
    #              id='Shifted_Two_months_Average_for_last_one'),
    # pytest.param(relativedelta(days=1),
    #              relativedelta(months=3, day=20),
    #              {'next_budget': '6500', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '20',
    #               'budget': '4503.25', 'budget_discount': '20', 'discount': '20', 'next_discount': '30', 'type': 'budget'},
    #              id='Shifted_Three_months_Average_for_last_two'),
    # pytest.param(relativedelta(days=1),
    #              relativedelta(months=4, day=25),
    #              {'budget_discount': '50', 'discount': '50', 'name': 'rest_cis_agency_discount_policy',
    #               'type': 'budget', 'AgencyDiscountPct': '50', 'budget': '27762.30333333333333333333333'},
    #              id='Shifted_Four_months_Average_for_last_three'),
])
def test_budget_periods(contract_dt_shift, request_dt_shift, expected_dsc):
    with reporter.step(u'В фикстуре были подготовлены акты в 5 месяцах начиная с предыдущего'):
        data = _setup_test_budget_periods(contract_dt_shift)
        reporter.attach(u'Подготовленные данные',
                        dict(zip("agency_id, person_id, contract_id, client_id".split(", "), data[1:])))
        context, agency_id, person_id, contract_id, client_id = data

    request_dt = CURRENT_MONTH_START.replace(day=10) + request_dt_shift
    # open_paypreview(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
    #                 client_id=client_id, request_dt=request_dt)
    dsc_data = get_discount(context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=1000.0)
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


@pytest.mark.parametrize('contract_dt, expected_dsc', [
    pytest.param(PREV_MONTH_START,
                 {'next_budget': '1300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '13',
                  'budget': '928.51', 'budget_discount': '13', 'discount': '13', 'next_discount': '15',
                  'type': 'budget'},
                 id='Contract_on_1st_day_Month_in_budget'),
    pytest.param(PREV_MONTH_START + relativedelta(day=2),
                 {'next_budget': '1300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '13', 'budget': '928.51',
                  'budget_discount': '13', 'discount': '13', 'next_discount': '15', 'type': 'budget'},
                 id='Contract_on_2nd_day_Month_not_in_budget'),
])
def test_contract_start_month(contract_dt, expected_dsc):
    context = CONTEXT.new(contract_dt=contract_dt)
    agency_id, person_id, contract_id = prepare_agency(context)
    client_id = steps.ClientSteps.create(context.client_template(context))

    act_qty = 1000.0
    act_dt = PREV_MONTH_START + relativedelta(months=1, days=-1)
    prepare_budget(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id, client_id=client_id,
                   act_qty_date_pairs=[(act_qty, act_dt)])

    request_dt = PREV_MONTH_START + relativedelta(months=1, day=10)
    # open_paypreview(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
    #                 client_id=client_id, request_dt=request_dt)
    dsc_data = get_discount(context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=1000.0)
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


def test_only_direct_in_budget():
    context = CONTEXT.new(contract_dt=CURRENT_MONTH_START)
    agency_id, person_id, contract_id = prepare_agency(context)
    client_id = steps.ClientSteps.create(context.client_template(context))

    act_dt = CURRENT_MONTH_START
    market_context = CONTEXT.new(service=c.Services.MARKET, product=c.Products.MARKET)
    prepare_budget(market_context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
                   client_id=client_id,
                   act_qty_date_pairs=[(1000, act_dt)])
    prepare_budget(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
                   client_id=client_id,
                   act_qty_date_pairs=[(1000, act_dt)])

    request_dt = act_dt + relativedelta(months=1, day=10)
    # open_paypreview(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
    #                 client_id=client_id, request_dt=request_dt)
    dsc_data = get_discount(context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=1000.0)

    expected_dsc = {'next_budget': '1300', 'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '13',
                    'budget': '928.51', 'budget_discount': '13', 'discount': '13', 'next_discount': '15',
                    'type': 'budget'}
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


def test_only_media_in_budget():
    context = CONTEXT_MEDIA.new(contract_dt=CURRENT_MONTH_START)
    agency_id, person_id, contract_id = prepare_agency(context)
    client_id = steps.ClientSteps.create(context.client_template(context))

    act_dt = CURRENT_MONTH_START
    prepare_budget(context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
                   client_id=client_id,
                   act_qty_date_pairs=[(1000000, act_dt)])

    request_dt = act_dt + relativedelta(months=1, day=10)
    dsc_data = get_discount(context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=1000000.0)

    expected_dsc = {'next_budget': '650', 'next_discount': '13',
                    'name': 'rest_cis_agency_discount_policy', 'AgencyDiscountPct': '10',
                    'budget': '300', 'budget_discount': '10', 'discount': '10',
                    'type': 'budget'}
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


def test_no_discount():
    no_discount_context = CONTEXT.new(discount_policy=SwDiscountPolicies.NO_DISCOUNT)
    agency_id, person_id, contract_id = prepare_agency(no_discount_context)

    client_id = steps.ClientSteps.create(no_discount_context.client_template(no_discount_context))

    act_dt = CURRENT_MONTH_START
    prepare_budget(no_discount_context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
                   client_id=client_id,
                   act_qty_date_pairs=[(10000, act_dt)])

    request_dt = act_dt + relativedelta(months=1, day=10)
    # open_paypreview(no_discount_context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
    #                 client_id=client_id, request_dt=request_dt)
    dsc_data = get_discount(no_discount_context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=1000.0)

    expected_dsc = {'AgencyDiscountPct': '0'}
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


def test_fixed_discount():
    fixed_discount_context = CONTEXT.new(discount_policy=SwDiscountPolicies.FIXED, fixed_discount="19")
    agency_id, person_id, contract_id = prepare_agency(fixed_discount_context)

    client_id = steps.ClientSteps.create(fixed_discount_context.client_template(fixed_discount_context))

    act_dt = CURRENT_MONTH_START
    prepare_budget(fixed_discount_context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
                   client_id=client_id,
                   act_qty_date_pairs=[(10000, act_dt)])

    request_dt = act_dt + relativedelta(months=1, day=10)
    # open_paypreview(fixed_discount_context, agency_id=agency_id, person_id=person_id, contract_id=contract_id,
    #                 client_id=client_id, request_dt=request_dt)
    dsc_data = get_discount(fixed_discount_context, agency_id=agency_id, request_dt=request_dt,
                            contract_id=contract_id, client_id=client_id, request_qty=1000.0)

    expected_dsc = {'AgencyDiscountPct': '19', 'discount': '19', 'type': 'fixed', 'name': 'contract_fixed'}
    utils.check_that(dsc_data, equal_to(expected_dsc),
                     step=u'Проверяем значения скидки и бюджета')


def prepare_agency(context_):
    agency_id = steps.ClientSteps.create(context_.agency_template())
    person_id = steps.PersonSteps.create(agency_id, context_.person_type.code)

    contract_type, contract_params = context_.contract_template(context_, agency_id, person_id)
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    return agency_id, person_id, contract_id


def prepare_budget(context_, agency_id, person_id, contract_id, client_id, act_qty_date_pairs):
    # credit = kwargs['credit']
    for qty, dt in act_qty_date_pairs:
        act_subclient_id = client_id or steps.ClientSteps.create(context_.client_template(context_))

        # Выставляем счёт на указанную сумму, оплачиваем его, откручиваем и актим
        campaigns_list = [{'client_id': act_subclient_id, 'service_id': context_.service.id,
                           'product_id': context_.product.id, 'qty': qty, 'begin_dt': dt}]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(act_subclient_id,
                                                                                person_id,
                                                                                campaigns_list,
                                                                                context_.paysys.id,
                                                                                dt,
                                                                                agency_id=agency_id,
                                                                                credit=0,
                                                                                contract_id=contract_id,
                                                                                overdraft=0,
                                                                                manager_uid=context_.manager.uid)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(context_.service.id, orders_list[0]['ServiceOrderID'],
                                          {context_.product.type.code: qty}, 0, dt)
        steps.ActsSteps.generate(agency_id, 1, dt)


def open_paypreview(context_, agency_id, person_id, contract_id, client_id, request_dt):
    service_order_id = steps.OrderSteps.next_id(context_.service.id)
    order_id = steps.OrderSteps.create(*context_.order_template(context_, client_id, agency_id, service_order_id))

    request_id = steps.RequestSteps.create(agency_id,
                                           [context_.request_order_template(context_, service_order_id, qty=1000.0)],
                                           additional_params={'InvoiceDesireDT': request_dt})

    with web.Driver() as driver:
        web.ClientInterface.PaypreviewPage.open(driver, request_id=request_id, person_id=person_id,
                                                paysys_id=context_.paysys.id, contract_id=contract_id)
        pass


def get_discount(context_, agency_id, request_dt, contract_id, client_id, request_qty):
    # def get_discount(agency_id, request_dt, dsc_policy):
    estimate_data = steps.DiscountSteps.estimate_discount(
        {'ClientID': agency_id, 'PaysysID': context_.paysys.id, 'ContractID': contract_id},
        [{'ProductID': context_.product.id, 'ClientID': client_id, 'Qty': request_qty, 'ID': 1,
          'BeginDT': request_dt, 'RegionID': None, 'discard_agency_discount': 0}])

    result = utils.remove_false({'AgencyDiscountPct': estimate_data.get('AgencyDiscountPct', False)})
    if 'AgencyDiscountProof' in estimate_data:
        result.update({key: value for key, value in estimate_data['AgencyDiscountProof'].iteritems() if
                       key in ['budget', 'budget_discount', 'discount', 'name', 'next_budget', 'next_discount',
                               'type']})

    return result
