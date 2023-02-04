import datetime
import pprint
import pytest
import hamcrest

from balance import balance_steps as steps
from btestlib.constants import Firms, Services, Products
import btestlib.utils as utils
import balance.balance_db as db

DT = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))

dt = datetime.datetime.now()

KZ_FIRM = Firms.KZ_25.id

MEDIA = Services.MEDIA_70.id
DIRECT = Services.DIRECT.id
MEDIASELLING = Services.BAYAN.id
BAYAN = Services.MEDIA_BANNERS.id
GEO = Services.GEO.id
MARKET = Services.MARKET.id

MEDIA_PRODUCT = Products.MEDIA
DIRECT_PRODUCT = Products.DIRECT_FISH
MEDIASELLING_PRODUCT = Products.BAYAN
BAYAN_PRODUCT = Products.BAYAN
GEO_PRODUCT = Products.GEO
MARKET_PRODUCT = Products.MARKET.id

SERVICE_PRODUCT_MAP = {DIRECT: DIRECT_PRODUCT,
                       MEDIA: MEDIA_PRODUCT,
                       MEDIASELLING: MEDIASELLING_PRODUCT,
                       BAYAN: BAYAN_PRODUCT,
                       GEO: GEO_PRODUCT}

PERSON_TYPE = 'kzu'
# PAYSYS_ID = 1003
# SERVICE_ID = 7
# SERVICE_ID = 11
# PRODUCT_ID = 1475
# PRODUCT_ID = 2136
MSR = 'Bucks'
PERSONS_ID = [
    # 1772094,
    # 1786338,
    1851233,
    # 4108748
    # 1868929,
    # 1870623,
    # 1902379,
    # 2127812
]


# PERSONS_ID = [740670,
# 930507,
# 1067768,
# 1103492,
# 1118746,
# 1156666,
# 1223921,
# 1227055,
# 1274858,
# 1289439,
# 1300329,
# 1304620,
# 1344906,
# 1440011,
# 1442666,
# 1444292,
# 1456543,
# 1468201,
# 1538281,
# 1542875,
# 1551910,
# 1584292,
# 1595028,
# 1600606,
# 1613896,
# 1627511,
# 1649033,
# 1661746,
# 1664217,
# 1696694,
# 1702762,
# 1736377,
# 1749157,
# 1772094,
# 1786338,
# 1851233,
# 1868929,
# 1870623,
# 1902379,
# 2127812,
# 2184367,
# 2211113,
# 2251421,
# 2258017,
# 2317873,
# 2369105,
# 2381756,
# 2393146,
# 2416788,
# 2425187,
# 2445423,
# 2452894,
# 2453376,
# 2470087,
# 2471806,
# 2479205,
# 2493815,
# 2530987,
# 2620821,
# 2640058,
# 2646573,
# 2653129,
# 2704643,
# 2735330,
# 2741918,
# 2761040,
# 2767780,
# 2773691,
# 2808507,
# 2873053,
# 2894802,
# 2912248,
# 2921068,
# 2956167,
# 3076665,
# 3083306,
# 3089410,
# 3094169,
# 3146422,
# 3149929,
# 3161104,
# 3173502,
# 3189622,
# 3190691,
# 3191214,
# 3198907,
# 3205417,
# 3211608,
# 3218438,
# 3239506,
# 3242936,
# 3243283,
# 3272550,
# 3307173,
# 3328304,
# 3343161,
# 3377547,
# 3384563,
# 3408453,
# 3429280,
# 3484502,
# 3587294,
# 3635941,
# 3779036,
# 3792020,
# 3811066,
# 3828190,
# 3902602,
# 3903591,
# 3906873,
# 3914703,
# 3918004,
# 3941093,
# 3960506,
# 3963134,
# 3966200,
# 3975696,
# 3976877,
# 3979669,
# 3983629,
# 3990639,
# 4003090,
# 4019116,
# 4019905,
# 4036874,
# 4038861,
# 4041157,
# 4041436,
# 4055926,
# 4061609,
# 4066645,
# 4072922,
# 4076195,
# 4087537,
# 4089861,
# 4096271,
# 4112110,
# 4112334,
# 4112442,
# 4116039,
# 4120507,
# 4126062,
# 4133314,
# 4134098,
# 4144701,
# 4155000,
# 4155440,
# 4158913,
# 4162469,
# 4166120,
# 4180587,
# 4191322,
# 4194020,
# 4212045,
# 4212361,
# 4216956,
# 4218463,
# 4220695,
# 4223972,
# 4223995,
# 4224454,
# 4224623,
# 4225022,
# 4225129,
# 4225752,
# 4225764,
# 4226291,
# 4226803,
# 4227203,
# 4229131]

# contract_id = None

# CONTRACT_TYPE = 'KZ_NO_AGENCY'
# CONTRACT_TYPE = 'KZ_OPT_AGENCY'

# Firms.KAZNET_3
@pytest.mark.parametrize('service_id', [
    DIRECT,
    # MEDIA,
    # MEDIASELLING,
    # BAYAN
])
@pytest.mark.parametrize('params', [
    {'PERSON_TYPE': 'byp', 'REGION_ID': 149, 'PAYSYS_ID': 2501020, 'with_contract': True, 'is_agency': 0},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 2501020, 'with_contract': False, 'is_agency': 0},
    #
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 2501020, 'with_contract': True, 'is_agency': 0},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 2501020, 'with_contract': False, 'is_agency': 0},
    #
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 0},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': False, 'is_agency': 0},
    #
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 0},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 1120, 'with_contract': False, 'is_agency': 0},
    # #
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 2501020, 'with_contract': True, 'is_agency': 1},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 2501020, 'with_contract': False, 'is_agency': 1},
    # #
    # # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 2501020, 'with_contract': True, 'is_agency': 1},
    # # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 2501020, 'with_contract': False, 'is_agency': 1},
    # #
    # # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 1},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': False, 'is_agency': 1},
    #
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 1},
    # {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 1120, 'with_contract': False, 'is_agency': 1},

    # {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 0},
    # {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 0},

    # {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 2501021, 'with_contract': True, 'is_agency': 0},
    # {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 0},
    #
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1121, 'with_contract': True, 'is_agency': 0},
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 1121, 'with_contract': False, 'is_agency': 0},
    #
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1121, 'with_contract': True, 'is_agency': 0},
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 1121, 'with_contract': False, 'is_agency': 0},
    #
    # {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 2501021, 'with_contract': True, 'is_agency': 1},
    # {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 1},
    #
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 2501021, 'with_contract': True, 'is_agency': 1},
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 1},
    #
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1121, 'with_contract': True, 'is_agency': 1},
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 1121, 'with_contract': False, 'is_agency': 1},
    #
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1121, 'with_contract': True, 'is_agency': 1},
    #     {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 1121, 'with_contract': False, 'is_agency': 1}
])
def test_BEL_firm_direct_client(params, service_id):
    client_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': params['is_agency']})
    person_id = steps.PersonSteps.create(client_id, params['PERSON_TYPE'])

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=SERVICE_PRODUCT_MAP[service_id].id, service_id=service_id,
                                           service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 0.5, 'BeginDT': dt}
        ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
        #
        # contract_id = None
        #
        # if params['with_contract']:
        #     contract_params = {'CLIENT_ID': client_id,
        #                        'PERSON_ID': person_id,
        #                        'DT': YEAR_BEFORE_NOW_ISO,
        #                        'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
        #                        'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
        #                        'SERVICES': [service_id],
        #                        'PAYMENT_TYPE': 2,
        #                        'FIRM': Firms.KZ_25.id,
        #                        # 'PERSONAL_ACCOUNT': 1,
        #                        # 'LIFT_CREDIT_ON_PAYMENT': 0,
        #                        # 'PERSONAL_ACCOUNT_FICTIVE': 1,
        #                        }
        #     CONTRACT_TYPE = 'KZ_NO_AGENCY' if params['is_agency'] == 1 else 'KZ_OPT_AGENCY'
        #
        #     contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)
        #
        # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
        #                                              paysys_id=params['PAYSYS_ID'],
        #                                              credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
        # steps.InvoiceSteps.turn_on(invoice_id)
        #
        # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {SERVICE_PRODUCT_MAP[service_id].type.code: 1}, 0, dt)
        #
        # # act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
        #
        # steps.CommonSteps.export('OEBS', 'Client', client_id)
        # # if params['with_contract']:
        # # steps.CommonSteps.export('OEBS', 'Contract', contract_id)
        # # steps.CommonSteps.export('OEBS', 'Manager', 20453)
        # steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
        # steps.CommonSteps.export('OEBS', 'Person', person_id)
        # # steps.CommonSteps.export('OEBS', 'Act', act_id)
        #
        # invoice = db.get_invoice_by_id(invoice_id)[0]
        # utils.check_that(invoice['firm_id'], hamcrest.equal_to(KZ_FIRM))
        # utils.check_that(invoice['iso_currency'], hamcrest.equal_to('KZT'))


@pytest.mark.parametrize('service_id', [
    DIRECT,
    # MEDIA,
    # MEDIASELLING,
    # BAYAN
])
@pytest.mark.parametrize('params', [
    {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 2501020, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 2501020, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 2501020, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 2501020, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzu', 'REGION_ID': None, 'PAYSYS_ID': 1120, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 2501021, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 2501021, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 2501021, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1121, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 1121, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1121, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'kzp', 'REGION_ID': None, 'PAYSYS_ID': 1121, 'with_contract': False, 'is_agency': 0},
])
def test_KZ_firm_non_direct_client(params, service_id):
    if service_id in [MEDIA, MEDIASELLING, BAYAN] and params['PAYSYS_ID'] in [2501020, 2501021]:
        return
    client_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': 0})
    agency_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, params['PERSON_TYPE'])

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=SERVICE_PRODUCT_MAP[service_id].id, service_id=service_id,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 0.5, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)

    contract_id = None

    if params['with_contract']:
        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id,
                           'DT': YEAR_BEFORE_NOW_ISO,
                           'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                           'SERVICES': [service_id],
                           'PAYMENT_TYPE': 2,
                           'FIRM': Firms.KZ_25.id,
                           'DISCOUNT_POLICY_TYPE': 17
                           }
        CONTRACT_TYPE = 'KZ_NO_AGENCY' if params['is_agency'] == 1 else 'KZ_OPT_AGENCY'

        contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)

    # pprint.pprint(steps.RequestSteps.get_request_choices(request_id))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=params['PAYSYS_ID'],
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    steps.InvoiceSteps.turn_on(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {SERVICE_PRODUCT_MAP[service_id].type.code: 1}, 0, dt)

    # steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=dt)[0]

    steps.CommonSteps.export('OEBS', 'Client', client_id)
    steps.CommonSteps.export('OEBS', 'Client', agency_id)
    if params['with_contract']:
        steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    steps.CommonSteps.export('OEBS', 'Act', act_id)


@pytest.mark.parametrize('service_id', [
    DIRECT,
    #     MEDIA,
    #     MEDIASELLING,
    #     BAYAN,
    #     GEO
])
@pytest.mark.parametrize('params', [
    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1060, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1060, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': None, 'PAYSYS_ID': 1060, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': None, 'PAYSYS_ID': 1060, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1060, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1060, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': None, 'PAYSYS_ID': 1060, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzu', 'REGION_ID': None, 'PAYSYS_ID': 1060, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1061, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': None, 'PAYSYS_ID': 1061, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1061, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': None, 'PAYSYS_ID': 1061, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1061, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': None, 'PAYSYS_ID': 1061, 'with_contract': False, 'is_agency': 0},

    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': 159, 'PAYSYS_ID': 1061, 'with_contract': True, 'is_agency': 0},
    {'PERSON_TYPE': 'yt_kzp', 'REGION_ID': None, 'PAYSYS_ID': 1061, 'with_contract': False, 'is_agency': 0},
])
def test_KZ_firm_non_direct_client_to_first_firm(params, service_id):
    if service_id in [MEDIA, MEDIASELLING, BAYAN] and params['PAYSYS_ID'] in [2501020, 2501021]:
        return
    client_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': 0})
    agency_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, params['PERSON_TYPE'])

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=SERVICE_PRODUCT_MAP[service_id].id, service_id=service_id,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 0.5, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)

    contract_id = None

    if params['with_contract']:
        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id,
                           'DT': YEAR_BEFORE_NOW_ISO,
                           'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                           'SERVICES': [service_id],
                           'PAYMENT_TYPE': 2,
                           'FIRM': Firms.YANDEX_1.id,
                           'DISCOUNT_POLICY_TYPE': 0
                           }

        CONTRACT_TYPE = 'OPT_AGENCY'
        contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)

    # pprint.pprint(steps.RequestSteps.get_request_choices(request_id))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=params['PAYSYS_ID'],
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    steps.InvoiceSteps.turn_on(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {SERVICE_PRODUCT_MAP[service_id].type.code: 1}, 0, dt)

    # steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=dt)[0]

    # steps.CommonSteps.export('OEBS', 'Client', client_id)
    # steps.CommonSteps.export('OEBS', 'Client', agency_id)
    # if params['with_contract']:
    #     steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    # steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    # steps.CommonSteps.export('OEBS', 'Act', act_id)


@pytest.mark.parametrize('params', [
    {'PERSON_TYPE': 'kzu', 'REGION_ID': 159, 'PAYSYS_ID': 1120, 'with_contract': True, 'is_agency': 1, 'service_id': MEDIA},
])
def test_KZ_firm_two_contract(params):
    service_id = params['service_id']
    product_id = 503058
    agency_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': params['is_agency']})
    client_id = steps.ClientSteps.create({'REGION_ID': params['REGION_ID'], 'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, params['PERSON_TYPE'])
    person_id_2 = steps.PersonSteps.create(agency_id, 'yt_kzu')
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 0.5, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)

    contract_id = None

    if params['with_contract']:
        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id,
                           'DT': YEAR_BEFORE_NOW_ISO,
                           'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                           'SERVICES': [service_id],
                           'PAYMENT_TYPE': 2,
                           'FIRM': Firms.KZ_25.id
                           }
        CONTRACT_TYPE = 'KZ_NO_AGENCY' if params['is_agency'] == 1 else 'KZ_OPT_AGENCY'
        contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)

        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id_2,
                           'DT': YEAR_BEFORE_NOW_ISO,
                           'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                           'SERVICES': [service_id],
                           'PAYMENT_TYPE': 2,
                           'FIRM': Firms.YANDEX_1.id,
                           'DISCOUNT_POLICY_TYPE': 0
                           }

        CONTRACT_TYPE = 'OPT_AGENCY'
        contract_id_2, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)

    pprint.pprint(steps.RequestSteps.get_request_choices(request_id))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=params['PAYSYS_ID'],
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    steps.InvoiceSteps.turn_on(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Shows': 2}, 0, dt)

    # steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=dt)[0]

    steps.CommonSteps.export('OEBS', 'Client', agency_id)
    if params['with_contract']:
        steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    steps.CommonSteps.export('OEBS', 'Act', act_id)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_2,
                                                 paysys_id=1060,
                                                 credit=0, contract_id=contract_id_2, overdraft=0, endbuyer_id=None)

    # steps.InvoiceSteps.pay(invoice_id)
    steps.InvoiceSteps.turn_on(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Shows': 200}, 0, dt)

    # steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=dt)[0]

    steps.CommonSteps.export('OEBS', 'Client', agency_id)
    if params['with_contract']:
        steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    steps.CommonSteps.export('OEBS', 'Act', act_id)
