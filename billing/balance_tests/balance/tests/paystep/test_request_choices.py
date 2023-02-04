# -*- coding: utf-8 -*-
import datetime

import pytest

from balance import balance_steps as steps
from balance import balance_db as db
from balance.tests.paystep.request_choices_params.practicum_us_yt import gen_practicum_us_yt_test_cases
from balance.tests.paystep.request_choices_params.shop_hk_yt import get_hk_yt_spb_software_cases
from btestlib import utils as utils
from btestlib.constants import (
    Paysyses,
    PersonTypes,
    Services,
    Products,
    ContractCommissionType,
    Firms,
    ContractPaymentType,
    Regions,
    Currencies,
)
from simpleapi.matchers import deep_equals as de
from simpleapi.steps import check_steps as checks
from temp.igogor.balance_objects import Contexts
from balance.tests.paystep.utils import (
    post_pay_contract_personal_account_fictive,
    prepay_contract
)

"""
Тест проверяет настройки платежных политик. В словарях типа
      {'region_id': Regions.BY.id,
      'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.BEL_OPT_AGENCY_PREM)
                                                               )]},
можно описать сущности, которые влияют на
набор способов оплаты в каждом конкретном случае:
- регион клиента
- категория клиента
- категория плательщика
- сервис
- договор

После проверки доступных способов оплаты тест выставит счет и создаст акт, их свойства можно описать в контексте.
Если раскомментировать последнюю строку, протолкнет все в ОЕБС.
"""
NOW = datetime.datetime.now()

PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
PREVIOUS_MONTH_FIRST_DAY_ISO = utils.Date.date_to_iso_format(PREVIOUS_MONTH_LAST_DAY.replace(day=1))
NOW_ISO = utils.Date.date_to_iso_format(NOW + datetime.timedelta(days=30))

QTY = 2

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
DIRECT_RUB = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(product=Products.DIRECT_EUR)
DIRECT_VIDEO = Contexts.DIRECT_FISH_RUB_CONTEXT.new(product=Products.VIDEO_DIRECT_RUB)
DIRECT_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(product=Products.DIRECT_BYN)
DIRECT_GBP = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(product=Products.DIRECT_GBP)
DIRECT_CHF = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(product=Products.DIRECT_CHF)
MARKET = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET)
MEDIASELLING = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.BAYAN, product=Products.DIRECT_BYN)
MEDIANA_BYN_PRODUCT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.DIRECT_BYN)
MEDIANA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.MEDIANA)
MEDIA_AUTORU_PRODUCT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_70,
                                                            product=Products.MEDIA_FOR_AUTORU)
MEDIA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_70, product=Products.MEDIA)
MEDIA_BY = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_70, product=Products.MEDIA_BY)
GEO = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO)
GEO_BY = DIRECT_BYN.new(service=Services.GEO, product=Products.GEO_508260)

# TOURS = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.TOURS, product=Products.TOURS_NONRES)
AUTORU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.AUTORU, product=Products.AUTORU)
DIRECT_TUNING = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT_TUNING, product=Products.DIRECT_TUNING_1)
VENDORS = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VENDORS, product=Products.VENDOR)
MARKET_ANALYTICS = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET_ANALYTICS,
                                                        product=Products.MARKET_ANALYTICS)
OFD_OFD_YEAR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.OFD, product=Products.OFD_YEAR)
TELEPHONY = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.TELEPHONY, product=Products.TELEPHONY)
REALTY_COMM = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.REALTY_COMM, product=Products.REALTY_COMM)
TOLOKA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.TOLOKA, product=Products.TOLOKA)
KUPIBILET = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.KUPIBILET, product=Products.KUPIBILET)
NAVIGATOR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.NAVI, product=Products.NAVI)
VZGLYAD = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VZGLYAD, product=Products.VZGLYAD)
VZGLYAD_KZ = VZGLYAD.new(product=Products.VZGLYAD_BUCKS)
CONNECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CONNECT, product=Products.CONNECT)
ZEN_SALES = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.ZEN_SALES, product=Products.ZEN_OOO)
PRACTICUM = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.PRACTICUM, product=Products.PRACTICUM)


WITHOUT_INVOICE_PARAMS = [

    ('DIRECT_TRY_TO_SW_w_contract',
     DIRECT_CHF.new(with_contract=True,
                    contract_number=0,
                    person_type=PersonTypes.SW_YT,
                    paysys=Paysyses.BANK_SW_YT_TRY,
                    credit=0),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                    person_type=PersonTypes.SW_YT,
                                    firm_id=Firms.EUROPE_AG_7.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.TRY.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'7': {'TRY': {'sw_yt': [11153, 11155]}}},
                               'without_contract': {'1': {'RUR': {'ph': [1000, 2100]}},  # 1052
                                                    '7': {'CHF': {'sw_ph': [1068, 1082, 1108],
                                                                  'sw_ur': [1045, 1085, 1109],
                                                                  'sw_yt': [1048, 1088, 1111],
                                                                  'sw_ytph': [1071, 1079, 1110]},
                                                          'UZS': {'by_ytph': [1133]}}}}
      }),

    pytest.mark.smoke(('DIRECT_CHF_client_without_contract_SW_UR_person',
                       DIRECT_CHF.new(person_type=PersonTypes.SW_UR,
                                      paysys=Paysyses.BANK_SW_UR_GBP),
                       {'with_agency': False},
                       {'offer_type_id': 14,
                        'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 2100]}},  # 1052
                                                                      '7': {'CHF': {'sw_ph': [1068, 1082, 1108],
                                                                                    'sw_ur': [1045, 1085, 1109],
                                                                                    'sw_yt': [1048, 1088, 1111],
                                                                                    'sw_ytph': [1071, 1079, 1110]},
                                                                            'UZS': {'by_ytph': [1133]}}}}
                        })),

    pytest.mark.smoke(('DIRECT_GBP_client_without_contract_SW_YT_person',
                       DIRECT_GBP.new(person_type=PersonTypes.SW_YT,
                                      paysys=Paysyses.CC_SW_YT_GBP),
                       {'with_agency': False},
                       {'offer_type_id': 15,
                        'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 2100]}},  # 1052
                                                                      '7': {'GBP': {'sw_ph': [1151, 1153, 1154],
                                                                                    'sw_ur': [1152, 1155, 1156],
                                                                                    'sw_yt': [1157, 1160, 1162],
                                                                                    'sw_ytph': [1158, 1159, 1161]},
                                                                            'UZS': {'by_ytph': [1133]}}}
                                                 }})),

    pytest.mark.smoke(('DIRECT_GBP_client_without_contract_SW_UR_person',
                       DIRECT_GBP.new(person_type=PersonTypes.SW_UR,
                                      paysys=Paysyses.BANK_SW_UR_GBP),
                       {'with_agency': False},
                       {'offer_type_id': 14,
                        'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 2100]}},  # 1052
                                                                      '7': {'GBP': {'sw_ph': [1151, 1153, 1154],
                                                                                    'sw_ur': [1152, 1155, 1156],
                                                                                    'sw_yt': [1157, 1160, 1162],
                                                                                    'sw_ytph': [1158, 1159, 1161]},
                                                                            'UZS': {'by_ytph': [1133]}}}
                                                 }})),

    pytest.mark.smoke(('DIRECT_client_without_contract_UR_person',
                       DIRECT.new(person_type=PersonTypes.UR,
                                  paysys=Paysyses.BANK_UR_RUB),
                       {'with_agency': False},
                       {'offer_type_id': 38,
                        'expected_paysys_list': {'without_contract': {'1': {'EUR': {'yt': [1023]},
                                                                            'RUR': {'ph': [1000,
                                                                                           1001,
                                                                                           1002,
                                                                                           # 1052,
                                                                                           2100,
                                                                                           2200,
                                                                                           ],
                                                                                    'ur': [1003, 1033],
                                                                                    'yt': [1014]},
                                                                            'USD': {'yt': [1013]}},
                                                                      '25': {'KZT': {'kzp': [1121, 2501021],
                                                                                     'kzu': [1120, 2501020]}},
                                                                      '27': {'BYN': {'byp': [1126, 2701102],
                                                                                     'byu': [1125, 2701101]}},
                                                                      '4': {'USD': {'usp': [1029, 1064],
                                                                                    'usu': [1028, 1065]}},
                                                                      '7': {'CHF': {'sw_ph': [1068, 1082, 1108],
                                                                                    'sw_ur': [1045, 1085, 1109],
                                                                                    'sw_yt': [1048, 1088, 1111],
                                                                                    'sw_ytph': [1071, 1079, 1110]},
                                                                            'EUR': {'sw_ph': [1066, 1081, 1112],
                                                                                    'sw_ur': [1043, 1084, 1113],
                                                                                    'sw_yt': [1046, 1087, 1115],
                                                                                    'sw_ytph': [1069, 1077, 1114]},
                                                                            'GBP': {'sw_ph': [1151, 1153, 1154],
                                                                                    'sw_ur': [1152, 1155, 1156],
                                                                                    'sw_yt': [1157, 1160, 1162],
                                                                                    'sw_ytph': [1158, 1159, 1161]},
                                                                            'RUR': {'by_ytph': [1075]},
                                                                            'TRY': {'sw_yt': [11153, 11155],
                                                                                    'sw_ytph': [11154, 11156]},
                                                                            'USD': {'sw_ph': [1067, 1080, 1104],
                                                                                    'sw_ur': [1044, 1083, 1105],
                                                                                    'sw_yt': [1047, 1086, 1107],
                                                                                    'sw_ytph': [1070, 1076, 1106]},
                                                                            'UZS': {'by_ytph': [1133]}}}}})),

    ('MARKET_client_without_contract_UR_person',
     MARKET.new(person_type=PersonTypes.UR,
                paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False},
     {'offer_type_id': 41,
      'expected_paysys_list': {'without_contract': {'111': {'BYN': {'yt': [11101100]},
                                                            'EUR': {'yt': [11101023]},
                                                            'KZT': {'yt_kzp': [11101061],
                                                                    'yt_kzu': [11101060]},
                                                            'RUR': {'ph': [11101000,
                                                                           11101001,
                                                                           11101002],  # 11101052
                                                                    'ur': [11101003, 11101033],
                                                                    'yt': [11101014]},
                                                            'USD': {'yt': [11101013]}},
                                                    '4': {'USD': {'usu': [1028]}},
                                                    '7': {'CHF': {'sw_ph': [1068, 1082],
                                                                  'sw_ur': [1045, 1085],
                                                                  'sw_yt': [1048, 1088],
                                                                  'sw_ytph': [1071, 1079]},
                                                          'EUR': {'sw_ph': [1066, 1081],
                                                                  'sw_ur': [1043, 1084],
                                                                  'sw_yt': [1046, 1087],
                                                                  'sw_ytph': [1069, 1077]},
                                                          'RUR': {'by_ytph': [1075]},
                                                          'USD': {'sw_ph': [1067, 1080],
                                                                  'sw_ur': [1044, 1083],
                                                                  'sw_yt': [1047, 1086],
                                                                  'sw_ytph': [1070, 1076]
                                                                  }}}}}),

    ('TELEPHONY_client_without_contract_PH_person',
     TELEPHONY.new(person_type=PersonTypes.PH,
                   paysys=Paysyses.BANK_PH_RUB),
     {'with_agency': False},
     {'offer_type_id': 86,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002], 'ur': [1003, 1033]
                                                                  }}}}}),

    ('MARKET_agency_prepay_contract_111_RUB_UR',
     MARKET.new(with_contract=True,
                contract_number=0,
                credit=1,
                paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.MARKET_111.id,
                                                               service_list=[Services.MARKET.id],
                                                               currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'111': {'RUR': {'ur': [11101003]
                                                                 }}}}}),

    (
        'PRACTICUM_client_without_contract_UR_person',
        PRACTICUM.new(
            person_type=PersonTypes.UR,
            paysys=Paysyses.BANK_UR_RUB,
        ),
        {
            'with_agency': False,
            'region_id': Regions.RU.id,
        },
        {
            'offer_type_id': 98,
            'expected_paysys_list': {
                'without_contract': {
                    str(Firms.SHAD_34.id): {
                        'RUR': {
                            PersonTypes.UR.code: [Paysyses.BANK_UR_SHAD_RUR.id],
                        },
                    },
                },
            },
        },
    ),
    ('DIRECT_video_client_without_contract_UR_person',
     DIRECT_VIDEO.new(person_type=PersonTypes.UR,
                      paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False},
     {'offer_type_id': 38,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000,
                                                                         1001,
                                                                         1002,
                                                                         # 1052,
                                                                         2100,
                                                                         2200,
                                                                         ],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),

    ('DIRECT_client_without_contract_region_UZB',
     DIRECT.new(person_type=PersonTypes.BY_YTPH,
                paysys=Paysyses.CC_YT_UZS),
     {'with_agency': False,
      'region_id': Regions.UZB.id},
     {'offer_type_id': 76,
      'expected_paysys_list': {'without_contract': {'1': {'EUR': {'yt': [1023]},
                                                          'RUR': {'yt': [1014]},
                                                          'USD': {'yt': [1013]}},
                                                    '7': {'RUR': {'by_ytph': [1075]},
                                                          'UZS': {'by_ytph': [1133]
                                                                  }}}}}),

    ('VZGLYAD_client_without_contract',
     VZGLYAD.new(person_type=PersonTypes.UR,
                 paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False},
     {'offer_type_id': 69,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),

    ('VZGLYAD_client_without_contract',
     VZGLYAD.new(person_type=PersonTypes.PH,
                 paysys=Paysyses.BANK_PH_RUB),
     {'with_agency': False},
     {'offer_type_id': 69,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),

    ('VZGLYAD_agency_without_contract',
     VZGLYAD.new(person_type=PersonTypes.UR,
                 paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': True},
     {'offer_type_id': 69,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),

    ('VZGLYAD_client_with_contract_1_RUB_UR',
     VZGLYAD.new(with_contract=True,
                 contract_number=0,
                 credit=0),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.YANDEX_1.id,
                                    service_list=[Services.VZGLYAD.id],
                                    currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003, 1033]
                                                               }}}}}),

    ('VZGLYAD_agency_with_contract_1_RUB_UR',
     VZGLYAD.new(with_contract=True,
                 contract_number=0,
                 credit=0),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.PR_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.YANDEX_1.id,
                                    service_list=[Services.VZGLYAD.id],
                                    currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003, 1033]}}},
                               'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]}}}}}),

    ('VZGLYAD_client_with_contract_25_KZT_KZ',
     VZGLYAD_KZ.new(person_type=PersonTypes.KZU,
                    paysys=Paysyses.BANK_KZ_UR_TG,
                    with_contract=True,
                    contract_number=0,
                    credit=0),
     {'with_agency': False,
      'region_id': Regions.KZ.id,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.KZ_NO_AGENCY,
                                    person_type=PersonTypes.KZU,
                                    firm_id=Firms.KZ_25.id,
                                    service_list=[Services.VZGLYAD.id],
                                    currency=Currencies.KZT.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'25': {'KZT': {'kzu': [1120, 2501020]
                                                                }}}}}),

    ('VZGLYAD_agency_with_contract_25_KZT_KZ',
     VZGLYAD_KZ.new(person_type=PersonTypes.KZU,
                    paysys=Paysyses.BANK_KZ_UR_TG,
                    with_contract=True,
                    contract_number=0,
                    credit=0),
     {'with_agency': True,
      'region_id': Regions.KZ.id,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.KZ_COMMISSION,
                                    person_type=PersonTypes.KZU,
                                    firm_id=Firms.KZ_25.id,
                                    service_list=[Services.VZGLYAD.id],
                                    currency=Currencies.KZT.num_code,
                                    additional_params={'COMMISSION_TYPE': 60})]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'25': {'KZT': {'kzu': [2501020]}}},
                               'without_contract': {'25': {'KZT': {'kzp': [1121, 2501021], 'kzu': [1120, 2501020]}}}}
      }),

    ('GEO_client_with_contract_27_BYN_BUY',
     GEO_BY.new(person_type=PersonTypes.BYU,
                paysys=Paysyses.BANK_BY_UR_BYN,
                with_contract=False,
                contract_number=0,
                credit=0),
     {'with_agency': False,
      'region_id': Regions.BY.id},
     {'offer_type_id': 82,
      'expected_paysys_list': {'without_contract': {'27': {'BYN': {'byp': [1126, 2701102], 'byu': [1125, 2701101]}}}}}),

    ('MEDIA_BY_client_with_contract_27_BYN_BUY',
     MEDIA_BY.new(person_type=PersonTypes.BYU,
                  paysys=Paysyses.BANK_BY_UR_BYN,
                  with_contract=False,
                  contract_number=0,
                  credit=0),
     {'with_agency': False,
      'region_id': Regions.BY.id},
     {'offer_type_id': 82,
      'expected_paysys_list': {'without_contract': {'27': {'BYN': {'byp': [2701102], 'byu': [2701101]}}}}}),

    ('NAVI_agency_without_contract',
     NAVIGATOR.new(person_type=PersonTypes.UR,
                   paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': True},
     {'offer_type_id': 114,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1001, 1002], 'ur': [1003, 1033]}}}}}),

    ('NAVI_agency_with_contract',
     NAVIGATOR.new(person_type=PersonTypes.SW_UR,
                   paysys=Paysyses.BANK_SW_UR_USD,
                   with_contract=True,
                   credit=1,
                   product=Products.NAVI_510710),
     {'region_id': Regions.SW.id,
      'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.SW_OPT_AGENCY,
                                                               person_type=PersonTypes.SW_UR,
                                                               firm_id=Firms.EUROPE_AG_7.id,
                                                               service_list=[Services.NAVI.id],
                                                               currency=Currencies.USD.num_code,
                                                               additional_params={'DISCOUNT_POLICY_TYPE': 16,
                                                                                  'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25}
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'7': {'USD': {'sw_ur': [1044]}}}}}),

    ('NAVI_client_with_contract',
     NAVIGATOR.new(person_type=PersonTypes.SW_UR,
                   paysys=Paysyses.BANK_SW_UR_EUR,
                   with_contract=True,
                   credit=1,
                   product=Products.NAVI_509961),
     {'region_id': Regions.SW.id,
      'with_agency': False,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                               person_type=PersonTypes.SW_UR,
                                                               firm_id=Firms.EUROPE_AG_7.id,
                                                               service_list=[Services.NAVI.id],
                                                               currency=Currencies.EUR.num_code,
                                                               additional_params={'DISCOUNT_POLICY_TYPE': 16}
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'7': {'EUR': {'sw_ur': [1043]}}}}}),

    ('MARKET_ANALYTICS_client_with_contract',
     MARKET_ANALYTICS.new(person_type=PersonTypes.UR, with_contract=True),
     {'with_agency': False, 'region_id': Regions.RU.id,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.MARKET_111.id,
                                    service_list=[Services.MARKET_ANALYTICS.id],
                                    currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {
          'with_contract': {'111': {'RUR': {'ur': [11101003]
                                            }}}}}),

    pytest.mark.smoke(
        ('DIRECT_with_BY_region_client_without_contract',
         DIRECT.new(person_type=PersonTypes.BYU,
                    paysys=Paysyses.BANK_BY_UR_BYN),
         {'region_id': Regions.BY.id,
          'with_agency': False},
         {'offer_type_id': 61,
          'expected_paysys_list': {'without_contract': {'27': {'BYN': {'byp': [1126, 2701102],
                                                                       'byu': [1125, 2701101]
                                                                       }}}}})),

    ('DIRECT_with_BY_region_agency_without_contract',
     DIRECT.new(person_type=PersonTypes.BYU,
                paysys=Paysyses.BANK_BY_UR_BYN),
     {'region_id': Regions.BY.id,
      'with_agency': True},
     {'offer_type_id': 61,
      'expected_paysys_list': {'without_contract': {'27': {'BYN': {'byp': [1126, 2701102],
                                                                   'byu': [1125, 2701101]
                                                                   }}}}}),

    ('DIRECT_with_BY_region_agency_with_contract_1_BYN_BUY_PR_AGENCY',
     DIRECT.new(person_type=PersonTypes.BYU,
                paysys=Paysyses.BANK_BY_UR_BYN,
                with_contract=True,
                credit=1),
     {'region_id': Regions.BY.id,
      'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.BEL_PR_AGENCY,
                                                               person_type=PersonTypes.BYU,
                                                               firm_id=Firms.REKLAMA_BEL_27.id,
                                                               service_list=[Services.DIRECT.id],
                                                               currency=Currencies.BYN.num_code,
                                                               additional_params={'DISCOUNT_POLICY_TYPE': 16}
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'27': {'BYN': {'byu': [1125, 2701101]}}}}}),

    ('DIRECT_with_BY_region_agency_with_contract_1_BYN_BUY_OPT_AGENCY_PREM',
     DIRECT.new(person_type=PersonTypes.BYU,
                paysys=Paysyses.BANK_BY_UR_BYN,
                with_contract=True,
                credit=0),
     {'region_id': Regions.BY.id,
      'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.BEL_OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.BYU,
                                                               firm_id=Firms.REKLAMA_BEL_27.id,
                                                               service_list=[Services.DIRECT.id],
                                                               currency=Currencies.BYN.num_code,
                                                               additional_params={'DISCOUNT_POLICY_TYPE': 16}
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'27': {'BYN': {'byu': [1125, 2701101]}}}}}),

    ('DIRECT_with_KZ_region_agency_with_postpay_contract',
     DIRECT.new(person_type=PersonTypes.KZU,
                paysys=Paysyses.BANK_KZ_UR_TG,
                with_contract=True,
                contract_number=0,
                credit=0
                ),
     {'with_agency': True,
      'region_id': Regions.KZ.id,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.KZ_OPT_AGENCY,
                                                               person_type=PersonTypes.KZU,
                                                               firm_id=Firms.KZ_25.id,
                                                               service_list=[Services.DIRECT.id],
                                                               currency=Currencies.KZT.num_code,
                                                               additional_params={'COMMISSION_TYPE': 60})]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'25': {'KZT': {'kzu': [1120, 2501020]
                                                                }}}}}),

    ('MARKET_with_BY_region_agency_without_contract',
     MARKET.new(person_type=PersonTypes.YT,
                paysys=Paysyses.BANK_YT_BYN_MARKET),
     {'region_id': Regions.BY.id,
      'with_agency': True},
     {'offer_type_id': 109,
      'expected_paysys_list': {'without_contract': {'111': {'BYN': {'yt': [11101100]},
                                                            'EUR': {'yt': [11101023]},
                                                            'RUR': {'yt': [11101014]},
                                                            'USD': {'yt': [11101013]}},
                                                    '7': {'RUR': {'by_ytph': [1075]}}}}}),

    ('DIRECT_agency_without_contract',
     DIRECT.new(person_type=PersonTypes.KZP,
                paysys=Paysyses.CC_KZ_PH_TG),
     {'with_agency': True},
     {'offer_type_id': 57,
      'expected_paysys_list': {'without_contract': {'1': {'EUR': {'yt': [1023]},
                                                          'RUR': {'ph': [1000,
                                                                         1001,
                                                                         1002,
                                                                         # 1052,
                                                                         2100,
                                                                         2200,
                                                                         ],
                                                                  'ur': [1003, 1033,
                                                                         ],
                                                                  'yt': [1014]},
                                                          'USD': {'yt': [1013]}},
                                                    '25': {'KZT': {'kzp': [1121, 2501021],
                                                                   'kzu': [1120, 2501020]}},
                                                    '27': {'BYN': {'byp': [1126, 2701102],
                                                                   'byu': [1125, 2701101]}},
                                                    '4': {'USD': {'usp': [1029, 1064],
                                                                  'usu': [1028, 1065]}},
                                                    '7': {'CHF': {'sw_ph': [1068, 1082, 1108],
                                                                  'sw_ur': [1045, 1085, 1109],
                                                                  'sw_yt': [1048, 1088, 1111],
                                                                  'sw_ytph': [1071, 1079, 1110]},
                                                          'EUR': {'sw_ph': [1066, 1081, 1112],
                                                                  'sw_ur': [1043, 1084, 1113],
                                                                  'sw_yt': [1046, 1087, 1115],
                                                                  'sw_ytph': [1069, 1077, 1114]},
                                                          'RUR': {'by_ytph': [1075]},
                                                          'UZS': {'by_ytph': [1133]},
                                                          'GBP': {'sw_ph': [1151, 1154],
                                                                  'sw_ur': [1152, 1156],
                                                                  'sw_yt': [1157, 1160],
                                                                  'sw_ytph': [1158, 1159]},
                                                          'TRY': {'sw_yt': [11153, 11155],
                                                                  'sw_ytph': [11154, 11156]},
                                                          'USD': {'sw_ph': [1067, 1080, 1104],
                                                                  'sw_ur': [1044, 1083, 1105],
                                                                  'sw_yt': [1047, 1086, 1107],
                                                                  'sw_ytph': [1070, 1076, 1106]}}}}}),

    pytest.mark.smoke(
        ('DIRECT_with_KZ_region_agency_without_contract',
         DIRECT.new(person_type=PersonTypes.KZP,
                    paysys=Paysyses.CC_KZ_PH_TG),
         {'with_agency': True,
          'region_id': Regions.KZ.id
          },
         {'offer_type_id': 57,
          'expected_paysys_list': {'without_contract': {'25': {'KZT': {'kzp': [1121, 2501021],
                                                                       'kzu': [1120, 2501020]
                                                                       }}}}})),

    ('DIRECT_with_KZ_region_agency_with_contract_commission_type_60',
     DIRECT.new(person_type=PersonTypes.KZU,
                paysys=Paysyses.BANK_KZ_UR_TG,
                with_contract=True,
                contract_number=0,
                credit=0
                ),
     {'with_agency': True,
      'region_id': Regions.KZ.id,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.KZ_COMMISSION,
                                    person_type=PersonTypes.KZU,
                                    firm_id=Firms.KZ_25.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.KZT.num_code,
                                    additional_params={'COMMISSION_TYPE': 60})]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'25': {'KZT': {'kzu': [2501020]
                                                                }}}}}),

    ('DIRECT_with_KZ_region_client_without_contract',
     DIRECT.new(person_type=PersonTypes.KZP,
                paysys=Paysyses.CC_KZ_PH_TG),
     {'region_id': Regions.KZ.id,
      'with_agency': False},
     {'offer_type_id': 57,
      'expected_paysys_list': {'without_contract': {'25': {'KZT': {'kzp': [1121, 2501021],
                                                                   'kzu': [1120, 2501020]
                                                                   }}}}}),

    pytest.mark.smoke(
        ('DIRECT_without_region_client_person_BY_YTPH_without_contract',
         DIRECT.new(person_type=PersonTypes.BY_YTPH,
                    paysys=Paysyses.CC_BY_YTPH_RUB),
         {'with_agency': False,
          'persons': [{'type': PersonTypes.BY_YTPH}]},
         {'offer_type_id': 76,
          'expected_paysys_list': {'without_contract': {'1': {'EUR': {'yt': [1023]},
                                                              'RUR': {'yt': [1014]},
                                                              'USD': {'yt': [1013]}},
                                                        '27': {'BYN': {'byp': [1126, 2701102],
                                                                       'byu': [1125, 2701101]}},
                                                        '7': {'RUR': {'by_ytph': [1075]},
                                                              'UZS': {'by_ytph': [1133]
                                                                      }}}}})),

    ('DIRECT_without_region_agency_person_BYU',
     DIRECT.new(person_type=PersonTypes.BYU,
                paysys=Paysyses.BANK_BY_UR_BYN),
     {'with_agency': True,
      'persons': [{'type': PersonTypes.BYU}]},
     {'offer_type_id': 61,
      'expected_paysys_list': {'without_contract': {'27': {'BYN': {'byp': [1126, 2701102],
                                                                   'byu': [1125, 2701101]
                                                                   }}}}}),

    ('DIRECT_client_region_RU_person_BYU_contract_1_RUR_UR',
     DIRECT.new(with_contract=True,
                contract_number=0,
                credit=0),
     {'region_id': Regions.RU.id,
      'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.OPT_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.YANDEX_1.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003, 1033]}}},
                               'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002,
                                                                         # 1052,
                                                                         2100, 2200],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),

    ('DIRECT_agency_prepay_contract_27_BYN_BYU',
     DIRECT.new(with_contract=True,
                contract_number=0,
                credit=0,
                paysys=Paysyses.BANK_BY_UR_BYN),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.BEL_OPT_AGENCY_PREM,
                                    person_type=PersonTypes.BYU,
                                    firm_id=Firms.REKLAMA_BEL_27.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.BYN.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'27': {'BYN': {'byu': [1125, 2701101]
                                                                }}}}}),

    ('REALTY_COMM_agency_prepay_contract_1_RUB_UR',
     REALTY_COMM.new(with_contract=True,
                     contract_number=0,
                     credit=1,
                     paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.VERTICAL_12.id,
                                                               service_list=[Services.REALTY_COMM.id],
                                                               currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'12': {'RUR': {'ur': [1201003]}}},
                               'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),
    ('DIRECT_agency_postpay_contract_1_RUB_UR',
     DIRECT.new(with_contract=True,
                contract_number=0,
                credit=1,
                paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.YANDEX_1.id,
                                                               service_list=[Services.DIRECT.id],
                                                               currency=Currencies.RUB.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003]
                                                               }}}}}),
    ('DIRECT_agency_contract_27_BYN_BYU',
     DIRECT.new(with_contract=True,
                contract_number=0,
                credit=1,
                paysys=Paysyses.BANK_BY_UR_BYN),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.BEL_OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.BYU,
                                                               firm_id=Firms.REKLAMA_BEL_27.id,
                                                               service_list=[Services.DIRECT.id],
                                                               currency=Currencies.BYN.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'27': {'BYN': {'byu': [1125, 2701101]
                                                                }}}}}),

    ('DIRECT_agency_prepay_contract_4_USD_USP',
     DIRECT.new(with_contract=True,
                credit=False,
                paysys=Paysyses.BANK_US_PH_USD),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                    person_type=PersonTypes.USP,
                                    firm_id=Firms.YANDEX_INC_4.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.USD.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'4': {'USD': {'usp': [1029, 1064]
                                                               }}}}}),

    ('DIRECT_TUNING_client',
     DIRECT_TUNING.new(with_contract=False,
                       paysys=Paysyses.BANK_UR_RUB,
                       person_type=PersonTypes.UR),
     {'with_agency': False},
     {'offer_type_id': 60,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),

    ('DIRECT_agency_prepay_contract_4_USD_USU',
     DIRECT.new(with_contract=True,
                paysys=Paysyses.BANK_US_UR_USD,
                description='AUTORU_with_postpay_contract'),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.USA_OPT_AGENCY,
                                    person_type=PersonTypes.USU,
                                    firm_id=Firms.YANDEX_INC_4.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.USD.num_code,
                                    additional_params={'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0}
                                    )]
      },
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'4': {'USD': {'usu': [1028, 1065]
                                                               }}
                                                 }}}),

    # ('TOURS_agency_contract_14_EUR_SW_YT',
    #  TOURS.new(with_contract=True,
    #            credit=1,
    #            paysys=Paysyses.BANK_SW_YT_EUR_AUTORU_AG),
    #  {'with_agency': True,
    #   'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.SW_OPT_AGENCY,
    #                                                            person_type=PersonTypes.SW_YT,
    #                                                            firm_id=Firms.AUTO_RU_AG_14.id,
    #                                                            service_list=[Services.TOURS.id],
    #                                                            currency=Currencies.EUR.num_code,
    #                                                            additional_params={
    #                                                                'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
    #                                                            }
    #                                                            )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_yt': [1401046]
    #                                                             }}}}}),

    # ('TOURS_agency_contract_14_CHF_SW_YT',
    #  TOURS.new(with_contract=True,
    #            credit=0,
    #            paysys=Paysyses.BANK_SW_YT_CHF_AUTORU_AG),
    #  {'with_agency': True,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_AGENCY,
    #                                 person_type=PersonTypes.SW_YT,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.CHF.num_code,
    #                                 additional_params={'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0}
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'CHF': {'sw_yt': [1401048]
    #                                                             }}}}}),

    # ('TOURS_client_region_US_contract_14_EUR_SW_YT',
    #  TOURS.new(with_contract=True,
    #            paysys=Paysyses.BANK_SW_YT_EUR_AUTORU_AG),
    #  {'with_agency': False,
    #   'region_id': Regions.US.id,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_CLIENT,
    #                                 person_type=PersonTypes.SW_YT,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.EUR.num_code,
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_yt': [1401046]
    #                                                             }}}}}),

    # ('TOURS_client_region_SW_contract_14_EUR_SW_UR',
    #  TOURS.new(with_contract=True,
    #            contract_number=0,
    #            credit=0,
    #            paysys=Paysyses.BANK_SW_UR_EUR_AUTORU_AG),
    #  {'with_agency': False,
    #   'region_id': Regions.SW.id,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_CLIENT,
    #                                 person_type=PersonTypes.SW_UR,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.EUR.num_code,
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_ur': [Paysyses.BANK_SW_UR_EUR_AUTORU_AG.id]
    #                                                             }}}}}),

    # ('TOURS_client_region_SW_contract_14_EUR_SW_UR',
    #  TOURS.new(with_contract=True,
    #            credit=1,
    #            paysys=Paysyses.BANK_SW_UR_EUR_AUTORU_AG),
    #  {'with_agency': False,
    #   'region_id': Regions.SW.id,
    #   'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.SW_OPT_CLIENT,
    #                                                            person_type=PersonTypes.SW_UR,
    #                                                            firm_id=Firms.AUTO_RU_AG_14.id,
    #                                                            service_list=[Services.TOURS.id],
    #                                                            currency=Currencies.EUR.num_code,
    #                                                            additional_params={
    #                                                                'DEAL_PASSPORT': PREVIOUS_MONTH_FIRST_DAY_ISO})]},
    #
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_ur': [Paysyses.BANK_SW_UR_EUR_AUTORU_AG.id]
    #                                                             }}}}}),

    ('MEDIANA_client',
     MEDIANA.new(with_contract=False,
                 paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False},
     {'offer_type_id': 58,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]

                                                                  }}}}}),

    ('REALTY_KOMM_client_contract_1_RUB_UR',
     REALTY_COMM.new(with_contract=False,
                     paysys=Paysyses.BANK_UR_RUB,
                     credit=0,
                     ),
     {'with_agency': False,
      'persons': [{'type': PersonTypes.UR}]},
     {'offer_type_id': 75,
      'expected_paysys_list': {'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),
    ('MEDIANA_client_contract_1_RUB_UR',
     MEDIANA.new(with_contract=True,
                 paysys=Paysyses.BANK_UR_RUB,
                 credit=1,
                 ),
     {'with_agency': False,
      'contracts':
          [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.NO_AGENCY,
                                                      person_type=PersonTypes.UR,
                                                      firm_id=Firms.YANDEX_1.id,
                                                      service_list=[Services.MEDIANA.id],
                                                      currency=Currencies.RUB.num_code,
                                                      )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003, 1033]
                                                               }}}}}),

    ('AUTORU_client_contract_12_RUB_ur',
     AUTORU.new(with_contract=True,
                contract_number=0,
                paysys=Paysyses.BANK_UR_RUB_VERTICAL),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.AUTO_NO_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.VERTICAL_12.id,
                                    service_list=[Services.AUTORU.id],
                                    currency=Currencies.RUB.num_code,
                                    )]
      },
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'12': {'RUR': {'ur': [1201003, 1201033]}}},
                               'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),

    ('AUTORU_agency_contract_12_RUB_ur_credit',
     AUTORU.new(with_contract=True,
                credit=True,
                paysys=Paysyses.BANK_UR_RUB_VERTICAL),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.VERTICAL_12.id,
                                                               service_list=[Services.AUTORU.id],
                                                               currency=Currencies.RUB.num_code,
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'12': {'RUR': {'ur': [1201003]}}},
                               'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),

    ('MEDIA_AUTORU_PRODUCT_agency_contract_12_RUB_ur',
     MEDIA_AUTORU_PRODUCT.new(with_contract=True,
                              credit=True,
                              paysys=Paysyses.BANK_UR_RUB_VERTICAL,
                              description='AUTORU_with_postpay_contract'),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.VERTICAL_12.id,
                                                               service_list=[Services.MEDIA_70.id],
                                                               currency=Currencies.RUB.num_code,
                                                               additional_params={
                                                                   'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 3}
                                                               )]

      },
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'12': {'RUR': {'ur': [1201003]}}},
                               'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]
                                                                  }}}}}),
    ('MARKET_client_person_YT_KZU',
     MARKET.new(paysys=Paysyses.BANK_YT_KZT_TNG_MARKET,
                person_type=PersonTypes.YT_KZU),
     {'with_agency': False,
      'persons': [{'type': PersonTypes.YT_KZU}]},
     {'offer_type_id': 42,
      'expected_paysys_list': {'without_contract': {'111': {'KZT': {'yt_kzp': [11101061],
                                                                    'yt_kzu': [11101060]
                                                                    }}}}}),

    ('MARKET_client_prepay_contract_111_RUB_ur',
     MARKET.new(with_contract=True,
                paysys=Paysyses.BANK_UR_RUB_MARKET,
                person_type=PersonTypes.UR),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.MARKET_111.id,
                                    service_list=[Services.MARKET.id],
                                    currency=Currencies.RUB.num_code,
                                    )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'111': {'RUR': {'ur': [11101003, 11101033]
                                                                 }}}}}),

    ('OFD_OFD_YEAR_client_person_UR',
     OFD_OFD_YEAR.new(paysys=Paysyses.BANK_UR_RUB_OFD,
                      person_type=PersonTypes.UR),
     {'with_agency': False,
      'persons': [{'type': PersonTypes.UR}]},
     {'offer_type_id': 53,
      'expected_paysys_list': {'without_contract': {'18': {'RUR': {'ur': [1801003]
                                                                   }}}}}),

    ('OFD_OFD_YEAR_client_prepay_contract_18_RUB_UR',
     OFD_OFD_YEAR.new(with_contract=True,
                      paysys=Paysyses.BANK_UR_RUB_MARKET,
                      person_type=PersonTypes.UR),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.OFD_18.id,
                                    service_list=[Services.OFD.id],
                                    currency=Currencies.RUB.num_code,
                                    )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'18': {'RUR': {'ur': [1801003]}}},
                               'without_contract': {'18': {'RUR': {'ur': [1801003]
                                                                   }}}}}),

    ('OFD_OFD_YEAR_client_postpay_contract_18_RUB_UR',
     OFD_OFD_YEAR.new(with_contract=True,
                      paysys=Paysyses.BANK_UR_RUB_MARKET,
                      credit=0,
                      person_type=PersonTypes.UR),
     {'with_agency': False,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.NO_AGENCY,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.OFD_18.id,
                                                               service_list=[Services.OFD.id],
                                                               currency=Currencies.RUB.num_code,
                                                               additional_params={'PARTNER_CREDIT': 1},

                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'18': {'RUR': {'ur': [1801003]}}},
                               'without_contract': {'18': {'RUR': {'ur': [1801003]
                                                                   }}}}}),

    ('OFD_OFD_YEAR_client_postpay_contract_18_RUB_UR_credit',
     OFD_OFD_YEAR.new(with_contract=True,
                      paysys=Paysyses.BANK_UR_RUB_MARKET,
                      credit=1,
                      person_type=PersonTypes.UR),
     {'with_agency': False,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.NO_AGENCY,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.OFD_18.id,
                                                               service_list=[Services.OFD.id],
                                                               currency=Currencies.RUB.num_code,
                                                               additional_params={'PARTNER_CREDIT': 1}
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'18': {'RUR': {'ur': [1801003]}}},
                               'without_contract': {'18': {'RUR': {'ur': [1801003]
                                                                   }}}}}),

    ('OFD_OFD_YEAR_client_wo_count_contract_18_RUB_UR',
     OFD_OFD_YEAR.new(with_contract=True,
                      paysys=Paysyses.BANK_UR_RUB_MARKET,
                      credit=0,
                      person_type=PersonTypes.UR),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.OFD_WO_COUNT,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.OFD_18.id,
                                    service_list=[Services.OFD.id],
                                    currency=Currencies.RUB.num_code,
                                    )]},
     {'offer_type_id': 53,
      'expected_paysys_list': {'with_contract': {'18': {'RUR': {'ur': [1801003]}}},
                               'without_contract': {'18': {'RUR': {'ur': [1801003]
                                                                   }}}}}),
    ('REALTY_COMM_client_person_UR',
     REALTY_COMM.new(paysys=Paysyses.BANK_UR_RUB_VERTICAL,
                     person_type=PersonTypes.UR),
     {'with_agency': False,
      'persons': [{'type': PersonTypes.PH}]},
     {'offer_type_id': 75,
      'expected_paysys_list': {'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),

    ('TOLOKA_agency_contract_to_Inc_firm',
     TOLOKA.new(paysys=Paysyses.BANK_US_UR_USD,
                with_contract=True,
                person_type=PersonTypes.USU),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.USA_OPT_AGENCY,
                                    person_type=PersonTypes.USU,
                                    firm_id=Firms.YANDEX_INC_4.id,
                                    service_list=[Services.TOLOKA.id],
                                    currency=Currencies.USD.num_code,
                                    additional_params={'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0})]
      },
     {'offer_type_id': 0,
      'expected_paysys_list': {}}),

    ('TOLOKA_agency_postpay_contract_to_Inc_firm',
     TOLOKA.new(paysys=Paysyses.BANK_US_UR_USD,
                person_type=PersonTypes.USU,
                with_contract=True,
                credit=1,
                ),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.USA_OPT_AGENCY,
                                                               person_type=PersonTypes.USU,
                                                               firm_id=Firms.YANDEX_INC_4.id,
                                                               service_list=[Services.TOLOKA.id],
                                                               currency=Currencies.USD.num_code,
                                                               additional_params={
                                                                   'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0}
                                                               )]
      },
     {'offer_type_id': 0,
      'expected_paysys_list': {}}),

    ('TOLOKA_client_region_GR_person_SW_YT',
     TOLOKA.new(paysys=Paysyses.BANK_SW_YT_USD_SAG,
                person_type=PersonTypes.SW_YT),
     {'with_agency': False,
      'region_id': Regions.GR.id,
      'persons': [{'type': PersonTypes.SW_YT}]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'without_contract': {'16': {'USD': {'sw_yt': [1601047, 1601086, 1601107],
                                                                   'sw_ytph': [1099, 1601070, 1601076],
                                                                   }}
                                                    }}}),

    ('DIRECT_client_prepay_contract_4_USD_USP',
     DIRECT.new(with_contract=True,
                paysys=Paysyses.BANK_US_PH_USD),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                    person_type=PersonTypes.USP,
                                    firm_id=Firms.YANDEX_INC_4.id,
                                    service_list=[Services.DIRECT.id],
                                    currency=Currencies.USD.num_code, )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'4': {'USD': {'usp': [1029, 1064]}}},
                               'without_contract': {'4': {'USD': {'usp': [1029, 1064],
                                                                  'usu': [1028, 1065]
                                                                  }}}}}),

    ('DIRECT_client_postpay_contract_4_USD_USP',
     DIRECT.new(with_contract=True,
                credit=True,
                paysys=Paysyses.BANK_US_PH_USD),
     {'with_agency': False,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                                               person_type=PersonTypes.USP,
                                                               firm_id=Firms.YANDEX_INC_4.id,
                                                               service_list=[Services.DIRECT.id],
                                                               currency=Currencies.USD.num_code, )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'4': {'USD': {'usp': [1029, 1064]}}},
                               'without_contract': {'4': {'USD': {'usp': [1029, 1064],
                                                                  'usu': [1028, 1065]

                                                                  }}}}}),
    # ('TOURS_agency',
    #  TOURS.new(with_contract=False,
    #            paysys=Paysyses.BANK_UR_RUB,
    #            product=Products.TOURS_505507,
    #            person_type=PersonTypes.UR),
    #  {'with_agency': True},
    #  {'offer_type_id': 38,
    #   'expected_paysys_list': {}}),

    # ('TOURS_client_contract_1_RUB_UR',
    #  TOURS.new(with_contract=True,
    #            paysys=Paysyses.BANK_UR_RUB,
    #            product=Products.TOURS_506624),
    #  {'with_agency': True,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
    #                                 person_type=PersonTypes.UR,
    #                                 firm_id=Firms.YANDEX_1.id,
    #                                 service_list=[Services.TOURS.id, Services.SHOP.id],
    #                                 currency=Currencies.RUB.num_code)]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003, 1033]}}}
    #                            }}),

    # ('TOURS_agency_contract_14_USD_SW_YT',
    #  TOURS.new(with_contract=True,
    #            paysys=Paysyses.BANK_SW_YT_USD_AUTORU_AG),
    #  {'with_agency': True,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_AGENCY,
    #                                 person_type=PersonTypes.SW_YT,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.USD.num_code,
    #                                 additional_params={'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0}
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'USD': {'sw_yt': [1401047]
    #                                                             }}}}}),
    #
    # ('TOURS_agency_contract_14_CHF_SW_YT',
    #  TOURS.new(with_contract=True,
    #            credit=0,
    #            paysys=Paysyses.BANK_SW_YT_CHF_AUTORU_AG),
    #  {'with_agency': True,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_AGENCY,
    #                                 person_type=PersonTypes.SW_YT,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.CHF.num_code,
    #                                 additional_params={'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0}
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'CHF': {'sw_yt': [1401048]
    #                                                             }}}}}),
    #
    # ('TOURS_client_region_US_contract_14_EUR_SW_YT',
    #  TOURS.new(with_contract=True,
    #            paysys=Paysyses.BANK_SW_YT_EUR_AUTORU_AG),
    #  {'with_agency': False,
    #   'region_id': Regions.US.id,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_CLIENT,
    #                                 person_type=PersonTypes.SW_YT,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.EUR.num_code,
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_yt': [1401046]
    #                                                             }}}}}),

    # ('TOURS_client_region_SW_contract_14_EUR_SW_UR',
    #  TOURS.new(with_contract=True,
    #            contract_number=0,
    #            credit=0,
    #            paysys=Paysyses.BANK_SW_UR_EUR_AUTORU_AG),
    #  {'with_agency': False,
    #   'region_id': Regions.SW.id,
    #   'contracts': [prepay_contract(contract_type=ContractCommissionType.SW_OPT_CLIENT,
    #                                 person_type=PersonTypes.SW_UR,
    #                                 firm_id=Firms.AUTO_RU_AG_14.id,
    #                                 service_list=[Services.TOURS.id],
    #                                 currency=Currencies.EUR.num_code,
    #                                 )]},
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_ur': [Paysyses.BANK_SW_UR_EUR_AUTORU_AG.id]
    #                                                             }}}}}),

    # ('TOURS_client_region_SW_contract_14_EUR_SW_UR',
    #  TOURS.new(with_contract=True,
    #            credit=1,
    #            paysys=Paysyses.BANK_SW_UR_EUR_AUTORU_AG),
    #  {'with_agency': False,
    #   'region_id': Regions.SW.id,
    #   'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.SW_OPT_CLIENT,
    #                                                            person_type=PersonTypes.SW_UR,
    #                                                            firm_id=Firms.AUTO_RU_AG_14.id,
    #                                                            service_list=[Services.TOURS.id],
    #                                                            currency=Currencies.EUR.num_code,
    #                                                            additional_params={
    #                                                                'DEAL_PASSPORT': PREVIOUS_MONTH_FIRST_DAY_ISO})]},
    #
    #  {'offer_type_id': 0,
    #   'expected_paysys_list': {'with_contract': {'14': {'EUR': {'sw_ur': [Paysyses.BANK_SW_UR_EUR_AUTORU_AG.id]
    #                                                             }}}}}),

    ('MEDIANA_client',
     MEDIANA.new(with_contract=False,
                 paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False},
     {'offer_type_id': 58,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                                                  'ur': [1003, 1033]

                                                                  }}}}}),

    ('AUTORU_client',
     AUTORU.new(with_contract=False,
                paysys=Paysyses.BANK_UR_RUB_VERTICAL,
                description='AUTORU_without_contract'),
     {'with_agency': False},
     {'offer_type_id': 48,
      'expected_paysys_list': {'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),

    ('AUTORU_agency_contract_12_RUB_ur',
     AUTORU.new(with_contract=True,
                credit=False,
                paysys=Paysyses.BANK_UR_RUB_VERTICAL),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.VERTICAL_12.id,
                                                               service_list=[Services.AUTORU.id],
                                                               currency=Currencies.RUB.num_code,
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'12': {'RUR': {'ur': [1201003]}}},
                               'without_contract': {'12': {'RUR': {'ph': [1201001, 1201002],
                                                                   'ur': [1201003, 1201033]
                                                                   }}}}}),

    ('MARKET_agency_postpay_contract_111_RUB_ur',
     MARKET.new(with_contract=True,
                credit=1,
                paysys=Paysyses.BANK_UR_RUB_MARKET,
                person_type=PersonTypes.UR),
     {'with_agency': True,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.OPT_AGENCY,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.MARKET_111.id,
                                                               service_list=[Services.MARKET.id],
                                                               currency=Currencies.RUB.num_code,
                                                               additional_params={'DISCOUNT_POLICY_TYPE': None}
                                                               )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'111': {'RUR': {'ur': [11101003, 11101033]
                                                                 }}}}}),

    ('MEDIA_agency_prepay_contract_27_BYN_BYU',
     MEDIA.new(with_contract=True,
               contract_number=0,
               credit=0,
               paysys=Paysyses.BANK_BY_UR_BYN,
               product=Products.MEDIA_BY),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.BEL_OPT_AGENCY_PREM,
                                    person_type=PersonTypes.BYU,
                                    firm_id=Firms.REKLAMA_BEL_27.id,
                                    service_list=[Services.MEDIA_70.id],
                                    currency=Currencies.BYN.num_code)]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'27': {'BYN': {'byu': [2701101]}}},
                               'without_contract': {'27': {'BYN': {'byp': [2701102], 'byu': [2701101]
                                                                   }}}}}),  ##BALANCE-30633

    ('MEDIA_agency_prepay_contract_25_KZT_KZU',
     MEDIA.new(with_contract=True,
               contract_number=0,
               credit=0,
               paysys=Paysyses.BANK_KZ_UR_TG,
               product=Products.MEDIA_KZ),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.KZ_OPT_AGENCY,
                                    person_type=PersonTypes.KZU,
                                    firm_id=Firms.KZ_25.id,
                                    service_list=[Services.MEDIA_70.id],
                                    currency=Currencies.KZT.num_code,
                                    additional_params={
                                        'DISCOUNT_POLICY_TYPE': 8,
                                        'CONTRACT_DISCOUNT': str(30)})]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'25': {'KZT': {'kzu': [2501020]
                                                                }}}}}),

    ('SHOP_prepay_contract_34_SHAD_UR',
     DIRECT.new(with_contract=True,
                credit=0,
                service=Services.SHOP),
     {'with_agency': False,
      'region_id': 225,
      'firm_id': Firms.SHAD_34.id,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.SHAD_34.id,
                                    service_list=[Services.SHOP.id],
                                    currency=Currencies.RUB.num_code, )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'34': {'RUR': {'ur': [3401003]}}},
                               'without_contract': {'34': {'RUR': {'ph': [3401001],
                                                                   'ur': [3401003],
                                                                   'yt': [3401014]}}}}}),

    ('ZEN_SALES_client_without_contract_UR_person',
     ZEN_SALES.new(person_type=PersonTypes.UR,
                   paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False},
     {'offer_type_id': 80,
      'expected_paysys_list': {'without_contract': {'1': {'RUR': {'ph': [1001, 1002], 'ur': [1003, 1033],
                                                                  'yt': [1014, 1255], 'ytph': [1253, 1254]}}}}
      }
     ),

    ('ZEN_SALES_client_prepay_contract_PH',
     ZEN_SALES.new(with_contract=True,
                   paysys=Paysyses.BANK_PH_RUB),
     {'with_agency': False,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.NO_AGENCY,
                                    person_type=PersonTypes.PH,
                                    firm_id=Firms.YANDEX_1.id,
                                    service_list=[Services.ZEN_SALES.id],
                                    currency=Currencies.RUB.num_code, )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ph': [1001]}}},
                               'without_contract': {'1': {'RUR': {'ph': [1001, 1002], 'ur': [1003, 1033]}}}
                               }}),

    ('ZEN_SALES_client_postpay_contract_UR',
     ZEN_SALES.new(with_contract=True,
                   credit=True,
                   paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': False,
      'contracts': [post_pay_contract_personal_account_fictive(contract_type=ContractCommissionType.NO_AGENCY,
                                                               person_type=PersonTypes.UR,
                                                               firm_id=Firms.YANDEX_1.id,
                                                               service_list=[Services.ZEN_SALES.id],
                                                               currency=Currencies.RUB.num_code, )]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003]}}},
                               'without_contract': {'1': {'RUR': {'ur': [1003, 1033], 'ph': [1001, 1002]}}}
                               }}),

    ('ZEN_SALES_agency_prepay_contract_UR',
     ZEN_SALES.new(with_contract=True,
                   contract_number=0,
                   credit=0,
                   paysys=Paysyses.BANK_UR_RUB),
     {'with_agency': True,
      'contracts': [prepay_contract(contract_type=ContractCommissionType.OPT_AGENCY,
                                    person_type=PersonTypes.UR,
                                    firm_id=Firms.YANDEX_1.id,
                                    service_list=[Services.ZEN_SALES.id],
                                    currency=Currencies.RUB.num_code,
                                    additional_params={'DISCOUNT_POLICY_TYPE': 0})]},
     {'offer_type_id': 0,
      'expected_paysys_list': {'with_contract': {'1': {'RUR': {'ur': [1003]}}},
                               'without_contract': {'1': {'RUR': {'ph': [1001, 1002], 'ur': [1003, 1033]}}}
                               }}),
]

# WITHOUT_INVOICE_PARAMS.extend(gen_practicum_us_yt_test_cases())
WITHOUT_INVOICE_PARAMS.extend(get_hk_yt_spb_software_cases())


def create_invoice(order_owner, invoice_owner, context, contracts_persons):
    if not getattr(context, 'with_contract', False):
        person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code, {'verified-docs': '1'})
    else:
        if contracts_persons:
            contract_number = getattr(context, 'contract_number', 0)
            person_id = contracts_persons[contract_number][1]

    campaigns_list = [
        {'client_id': order_owner, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
    ]
    contract_id = None if not getattr(context, 'with_contract', False) else contracts_persons[contract_number][0]
    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=order_owner,
                                                                            agency_id=invoice_owner if invoice_owner != order_owner else None,
                                                                            person_id=person_id,
                                                                            invoice_dt=PREVIOUS_MONTH_LAST_DAY,
                                                                            campaigns_list=campaigns_list,
                                                                            contract_id=contract_id,
                                                                            paysys_id=context.paysys.id,
                                                                            credit=getattr(context, 'credit', 0))
    if not getattr(context, 'credit', 0):
        if context.paysys.instant:
            steps.InvoiceSteps.turn_on(invoice_id)
        else:
            steps.InvoiceSteps.pay(invoice_id)
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(context.service.id, order['ServiceOrderID'],
                                          {context.product.type.code: 100}, 0, PREVIOUS_MONTH_LAST_DAY)
    return invoice_id, contract_id, person_id


def create_contract(client_id, contract_params):
    person_id = steps.PersonSteps.create(client_id, contract_params['person'].code)
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [Services.DIRECT.id],
        'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
        'FINISH_DT': NOW_ISO,
        'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
        'DEAL_PASSPORT': PREVIOUS_MONTH_FIRST_DAY_ISO
    }
    contract_params_default.update(contract_params['contract_params'])
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_params['contract_type'], contract_params_default,
                                                             prevent_oebs_export=True)
    return contract_id, person_id


def create_simple_invoice(client_id, context):
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    campaigns_list = [
        {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
    ]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=context.paysys.id,
                                                                  contract_id=None,
                                                                  prevent_oebs_export=False)
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  invoice_id=invoice_id)
    return invoice_id


@pytest.mark.parametrize('description, context, params, expected', WITHOUT_INVOICE_PARAMS,
                         ids=lambda description, context, params, expected: description)
def test_pcps_with_invoice_or_act(description, context, params, expected):
    client_id = steps.ClientSteps.create({'REGION_ID': params.get('region_id', None)})
    if params['with_agency']:
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': params.get('region_id', None)})
    else:
        agency_id = None

    for person in params.get('persons', []):
        steps.PersonSteps.create(agency_id or client_id, person['type'].code)

    contracts_persons = []
    for contract in params.get('contracts', []):
        contract_, person_id = create_contract(agency_id or client_id, contract)
        contracts_persons.append((contract_, person_id))
    steps.ClientSteps.link(agency_id or client_id, 'aikawa-test-10')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': PREVIOUS_MONTH_LAST_DAY}]
    request_id = steps.RequestSteps.create(client_id=agency_id or client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': NOW,
                                                              'FirmID': params.get('firm_id', None)})

    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formated_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    checks.check_that(formated_request_choices, de.deep_equals_to(expected['expected_paysys_list']))

    if formated_request_choices:
        invoice_id, contract_id, person_id = create_invoice(
            client_id, agency_id or client_id, context, contracts_persons,
        )
        deferpays = db.get_deferpays_by_invoice(invoice_id)
        if deferpays:
            db.balance().execute(
                '''UPDATE t_deferpay SET ISSUE_DT = :dt WHERE invoice_id = :invoice_id''',
                {'invoice_id': invoice_id, 'dt': PREVIOUS_MONTH_LAST_DAY},
            )
        act_id = steps.ActsSteps.generate(agency_id or client_id, force=1, date=NOW)[0]
        invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']
        assert db.get_invoice_by_id(invoice_id)[0]['offer_type_id'] == expected['offer_type_id']
        # if agency_id:
        #     steps.ExportSteps.export_oebs(client_id=agency_id)
        # steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id, invoice_id=invoice_id,
        #                               person_id=person_id, act_id=act_id, manager_id=None)
