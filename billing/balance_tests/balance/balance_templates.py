# coding: utf-8
import urlparse
from datetime import datetime
from decimal import Decimal

from btestlib import utils
from btestlib.constants import Services, PersonTypes, Regions, Currencies, Paysyses, Products, Managers, Firms, Nds, \
    CurrencyRateSource, ContractType
from btestlib.data import contract_defaults
from btestlib.data.simpleapi_defaults import ThirdPartyData
from temp.igogor import balance_objects as obj


class Clients(utils.ConstantsContainer):
    constant_type = obj.Client

    DEFAULT = obj.Client().new(is_agency=False, NAME='balance_test {}'.format(datetime.now()), EMAIL='client@in-fo.ru',
                               PHONE='+7 (911) 111-22-33', FAX='+7 (911) 444-55-66', URL='http://client.info/',
                               CITY='Some City')
    NON_CURRENCY = DEFAULT.new()
    DIRECT_CURRENCY_RUB = DEFAULT.new(region=Regions.RU, currency=Currencies.RUB, service=Services.DIRECT,
                                      migrate_to_currency=datetime(2000, 1, 1))
    DIRECT_CURRENCY_USD = DIRECT_CURRENCY_RUB.new(region=Regions.US, currency=Currencies.USD)


class Persons(utils.ConstantsContainer):
    constant_type = obj.Person

    UR = obj.Person().new(type=PersonTypes.UR, dict_params={"delivery-type": "4",  ## Юр.лицо РФ
                                                            "bik": "044525440",
                                                            "live-signature": "1",
                                                            "client_id": "14295543",
                                                            "invalid-bankprops": "0",
                                                            "delivery-city": "NSK",
                                                            "postaddress": u"а/я Кладр 0",
                                                            "edo-type": "",
                                                            "authority-doc-details": "g",
                                                            "fax": "+7 812 5696286",
                                                            "invalid-address": "1",
                                                            "vip": "1",
                                                            "representative": "tPLLK",
                                                            "email": "m-SC@qCWF.rKU",
                                                            "ogrn": "379956466494603",
                                                            "longname": u"000 WBXG",
                                                            "address": u"Улица 4",
                                                            "legaladdress": "Avenue 5",
                                                            "inn": "7879679858",
                                                            "postcode": "191025",
                                                            "kpp": "767726208",
                                                            "revise-act-period-type": "",
                                                            "authority-doc-type": u"Свидетельство о регистрации",
                                                            "phone": "+7 812 3990776",
                                                            "name": u"Юр. лицо или ПБОЮЛqdZd",
                                                            "s_signer-position-name": "President",
                                                            "signer-person-gender": "W",
                                                            "account": "40702810437116329435",
                                                            "signer-person-name": "Signer RR",
                                                            })


def from_blocks(*blocks):
    # type: (list) -> dict
    if not blocks:
        blocks = [contract_defaults.contract_left,
                  contract_defaults.contract_right,
                  contract_defaults.contract_signed,
                  contract_defaults.contract_postpay,
                  contract_defaults.adfox]
    source_tuples = urlparse.parse_qsl(''.join(blocks), True)
    # Convert to dict
    return {key: value.decode('utf-8') for (key, value) in source_tuples}


class Contracts(utils.ConstantsContainer):
    constant_type = obj.Contract

    # todo-igogor мнится мне здесь заполнятся некоторые именованные параметры типа id и перестанут быть NOT_SET
    DEFAULT = obj.Contract().new(dict_params=from_blocks())  # type: obj.Contract


class Collaterals(utils.ConstantsContainer):
    constant_type = obj.Collateral

    @staticmethod
    def default(collateral_type_id):
        return obj.Collateral().new(dict_params=from_blocks(
            contract_defaults.collaterals_header.format(collateral_type_id),
            contract_defaults.collaterals_footer.format(collateral_type_id)))

    # todo-igogor это костыльно и надо выпилить и упростить.
    @staticmethod
    def by_id(collateral_type_id):
        # type: (int) -> obj.Collateral
        return obj.Collateral().new(type_id=collateral_type_id, dict_params=from_blocks(
            contract_defaults.get_collateral_template_by_name(collateral_type_id)))


class Contexts(object):
    pass  # todo-igogor надо переделать контексты как только их формат устаканится
    DIRECT_FISH_RUB_CONTEXT = obj.Context().new(name='DIRECT_FISH_RUB',
                                                service=Services.DIRECT,
                                                paysys=Paysyses.BANK_UR_RUB,
                                                product=Products.DIRECT_FISH,
                                                price=Decimal('30.0'),
                                                manager=Managers.SOME_MANAGER,
                                                client_template=Clients.NON_CURRENCY,
                                                person_template=Persons.UR,
                                                # todo-igogor эта строчка кмк не нужна
                                                person_type=Persons.UR.type)

    # todo fix price
    DIRECT_FISH_YT_RUB_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_YT_RUB',
                                                             person_type=PersonTypes.YT,
                                                             paysys=Paysyses.BANK_YT_RUB,
                                                             price=Decimal('30.0') / Decimal('1.18'))

    # todo fix price
    DIRECT_FISH_UAH_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_UAH',
                                                          person_type=PersonTypes.UA,
                                                          paysys=Paysyses.BANK_UA_UR_UAH)

    # todo fix price
    DIRECT_FISH_KZ_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_FISH_KZ',
                                                         person_type=PersonTypes.KZU,
                                                         paysys=Paysyses.BANK_KZ_UR_TG)

    # todo fix price
    DIRECT_BYN_BYU_CONTEXT = DIRECT_FISH_RUB_CONTEXT.new(name='DIRECT_BYN_BYU_CONTEXT',
                                                         person_type=PersonTypes.BYU,
                                                         paysys=Paysyses.BANK_BY_UR_BYN)


    DIRECT_FISH_USD_CONTEXT = obj.Context().new(name='DIRECT_FISH_USD',
                                                service=Services.DIRECT,
                                                client_template=Clients.NON_CURRENCY,
                                                person_type=PersonTypes.USU, paysys=Paysyses.BANK_US_UR_USD,
                                                product=Products.DIRECT_FISH,
                                                price=Decimal('0.41'),
                                                manager=Managers.SOME_MANAGER)

    # todo fix price
    DIRECT_FISH_SW_EUR_CONTEXT = obj.Context().new(name='DIRECT_FISH_SW_EUR',
                                                   service=Services.DIRECT,
                                                   client_template=Clients.NON_CURRENCY,
                                                   person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_EUR,
                                                   product=Products.DIRECT_FISH,
                                                   price=Decimal('0.41'),
                                                   manager=Managers.SOME_MANAGER)

    # todo fix price
    DIRECT_FISH_TRY_CONTEXT = obj.Context().new(name='DIRECT_FISH_TRY',
                                                service=Services.DIRECT,
                                                client_template=Clients.NON_CURRENCY,
                                                person_type=PersonTypes.TRU, paysys=Paysyses.BANK_TR_UR_TRY,
                                                product=Products.DIRECT_FISH,
                                                price=Decimal('0.41'),
                                                manager=Managers.SOME_MANAGER)

    DIRECT_MONEY_RUB_CONTEXT = obj.Context().new(name='DIRECT_MONEY_RUB',
                                                 service=Services.DIRECT,
                                                 client_template=Clients.DIRECT_CURRENCY_RUB,
                                                 person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                                 product=Products.DIRECT_RUB,
                                                 price=Decimal('1'),
                                                 manager=Managers.SOME_MANAGER)

    DIRECT_MONEY_USD_CONTEXT = obj.Context().new(name='DIRECT_MONEY_USD',
                                                 service=Services.DIRECT,
                                                 client_template=Clients.DIRECT_CURRENCY_USD,
                                                 person_type=PersonTypes.USU, paysys=Paysyses.BANK_US_UR_USD,
                                                 product=Products.DIRECT_USD,
                                                 price=Decimal('1'),
                                                 manager=Managers.SOME_MANAGER)

    MARKET_RUB_CONTEXT = obj.Context().new(name='MARKET_RUB',
                                           service=Services.MARKET,
                                           client_template=Clients.NON_CURRENCY,
                                           person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                           product=Products.MARKET,
                                           price=Decimal('30.0'),
                                           manager=Managers.SOME_MANAGER)

    MARKET_BLUE_CONTEXT = obj.Context().new(name='MARKET_BLUE',
                                            service=[Services.BLUE_MARKET_PAYMENTS, Services.BLUE_MARKET],
                                            client_template=Clients.NON_CURRENCY,
                                            person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                            product=Products.MARKET_BLUE_PAYMENTS,
                                            price=Decimal('100'),
                                            manager=Managers.SOME_MANAGER)

    DIRECT_TUNING_FISH_RUB_CONTEXT = obj.Context().new(name='DIRECT_TUNING_FISH_RUB',
                                                       service=Services.DIRECT_TUNING,
                                                       client_template=Clients.NON_CURRENCY,
                                                       person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                                       product=Products.DIRECT_TUNING_1,
                                                       price=Decimal('0.41'),
                                                       manager=Managers.SOME_MANAGER)

    CATALOG1_CONTEXT = obj.Context().new(name='CATALOG1_CONTEXT',
                                         service=Services.CATALOG1,
                                         client_template=Clients.NON_CURRENCY,
                                         person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                         product=Products.CATALOG1,
                                         price=Decimal('30.0'),
                                         manager=Managers.SOME_MANAGER)

    # todo fix price
    ADFOX_CONTEXT = obj.Context().new(name='ADFOX_CONTEXT',
                                      service=Services.ADFOX,
                                      client_template=Clients.NON_CURRENCY,
                                      person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                      product=Products.ADFOX,
                                      price=Decimal('30.0'),
                                      manager=Managers.SOME_MANAGER)

    # todo fix price
    SPEECHKIT_CONTEXT = obj.Context().new(name='SPEECHKIT_CONTEXT',
                                          service=Services.SHOP,
                                          client_template=Clients.NON_CURRENCY,
                                          person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                          product=Products.SPEECHKIT,
                                          price=Decimal('30.0'),
                                          manager=Managers.SOME_MANAGER)

    TAXI_FISH_RUB_CONTEXT = obj.Context().new(name='TAXI_FISH_RUB',
                                              service=Services.TAXI,
                                              client_template=Clients.NON_CURRENCY,
                                              person_type=PersonTypes.UR, paysys=Paysyses.BANK_UR_RUB,
                                              product=Products.DIRECT_FISH,
                                              price=Decimal('30.0'),
                                              manager=Managers.SOME_MANAGER)

    TOLOKA_FISH_USD_CONTEXT = obj.Context().new(name='TOLOKA_FISH_USD',
                                                service=Services.TOLOKA,
                                                client_template=Clients.NON_CURRENCY,
                                                person_type=PersonTypes.SW_YT, paysys=Paysyses.BANK_SW_YT_USD,
                                                product=Products.TOLOKA,
                                                price=Decimal('30.0'),
                                                manager=Managers.SOME_MANAGER)

    VENDORS_FISH_RUB_CONTEXT = obj.Context().new(name='VENDORS_FISH_RUB',
                                                 service=Services.VENDORS,
                                                 client_template=Clients.NON_CURRENCY,
                                                 person_type=PersonTypes.SW_YT, paysys=Paysyses.BANK_SW_YT_USD,
                                                 product=Products.VENDOR,
                                                 price=Decimal('30.0'),
                                                 manager=Managers.SOME_MANAGER)

    VENDORS_FISH_UR_RUB_CONTEXT = obj.Context().new(name='VENDORS_FISH_RUB',
                                                    service=Services.VENDORS,
                                                    client_template=Clients.NON_CURRENCY,
                                                    person_type=PersonTypes.UR, paysys=Paysyses.BANK_SW_YT_USD,
                                                    product=Products.VENDOR,
                                                    price=Decimal('30.0'),
                                                    manager=Managers.SOME_MANAGER)

    # TAXI
    TAXI_RU_CONTEXT = obj.Context().new(
        name='TAXI_RU_CONTEXT',
        service=Services.TAXI,
        client_template=None,
        person_type=PersonTypes.UR,
        paysys=Paysyses.BANK_UR_RUB_TAXI,
        product=None,
        price=None,
        manager=None
    ).new(
        firm=Firms.TAXI_13,
        currency=Currencies.RUB,
        payment_currency=Currencies.RUB,
        region=Regions.RU,
        nds=Nds.DEFAULT,
        third_party_data=ThirdPartyData.TAXI,
        services=[Services.TAXI, Services.UBER, Services.UBER_ROAMING, Services.TAXI_111, Services.TAXI_128],
        commission_pct=Decimal('0.01'),
        min_commission=Decimal('0.01'),
        currency_rate_source=CurrencyRateSource.CBR,
        precision=2
    )

    TAXI_BV_ARM_USD_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_BV_ARM_CONTEXT',
        person_type=PersonTypes.EU_YT,
        firm=Firms.TAXI_BV_22,
        currency=Currencies.USD,
        payment_currency=Currencies.AMD,
        region=Regions.ARM,
        nds=Nds.NONE,
        paysys=Paysyses.BANK_UR_USD_TAXI_BV,
        services=[Services.TAXI, Services.TAXI_111, Services.TAXI_128],
        third_party_data=ThirdPartyData.TAXI_BV,
        commission_pct=Decimal('3.5'),
        min_commission=Decimal('0'),
        currency_rate_source=CurrencyRateSource.CBA,
        precision=2
    )

    TAXI_BV_GEO_USD_CONTEXT = TAXI_BV_ARM_USD_CONTEXT.new(
        name='TAXI_BV_GEO_USD_CONTEXT',
        region=Regions.GEO,
    )

    TAXI_BV_EST_USD_CONTEXT = TAXI_BV_ARM_USD_CONTEXT.new(
        name='TAXI_BV_EST_USD_CONTEXT',
        region=Regions.EST,
    )

    TAXI_BV_KZ_EUR_CONTEXT = TAXI_BV_ARM_USD_CONTEXT.new(
        name='TAXI_BV_KZ_CONTEXT',
        currency=Currencies.EUR,
        payment_currency=Currencies.KZT,
        region=Regions.KZ,
        paysys=Paysyses.BANK_UR_EUR_TAXI_BV,
        currency_rate_source=CurrencyRateSource.NBKZ
    )

    TAXI_BV_LAT_EUR_CONTEXT = TAXI_BV_KZ_EUR_CONTEXT.new(
        name='TAXI_BV_LAT_EUR_CONTEXT',
        region=Regions.LAT,
    )

    TAXI_BV_MD_EUR_CONTEXT = TAXI_BV_KZ_EUR_CONTEXT.new(
        name='TAXI_BV_MD_EUR_CONTEXT',
        region=Regions.MD,
    )

    TAXI_KZ_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_KZ_CONTEXT',
        person_type=PersonTypes.KZU,
        currency=Currencies.KZT,
        payment_currency=Currencies.KZT,
        firm=Firms.TAXI_KAZ_24,
        region=Regions.KZ,
        paysys=Paysyses.BANK_KZ_UR_WO_NDS,
        third_party_data=ThirdPartyData.TAXI_KZ,
        nds=Nds.ZERO,
        services=[Services.TAXI, Services.UBER, Services.UBER_ROAMING],
        commission_pct=Decimal('2.44'),
        min_commission=Decimal('15'),
        currency_rate_source=CurrencyRateSource.NBKZ,
        precision=0
    )

    TAXI_ARM_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_ARM_CONTEXT',
        person_type=PersonTypes.AM_UR,
        currency=Currencies.AMD,
        payment_currency=Currencies.AMD,
        firm=Firms.TAXI_AM_26,
        region=Regions.ARM,
        paysys=Paysyses.BANK_ARM_UR,
        third_party_data=ThirdPartyData.TAXI_ARMENIA,
        nds=Nds.ZERO,
        services=[Services.TAXI],
        commission_pct=Decimal('2.44'),
        min_commission=Decimal('1'),
        currency_rate_source=CurrencyRateSource.CBA,
        precision=0
    )

    TAXI_UBER_BY_CONTEXT = TAXI_RU_CONTEXT.new(
        name='TAXI_UBER_BY_CONTEXT',
        person_type=PersonTypes.EU_YT,
        currency=Currencies.USD,
        payment_currency=Currencies.BYN,
        firm=Firms.UBER_115,
        region=Regions.BY,
        paysys=Paysyses.BANK_UR_UBER_USD,
        third_party_data=ThirdPartyData.TAXI_UBER_BY,
        nds=Nds.NONE,
        services=[Services.TAXI, Services.UBER, Services.UBER_ROAMING, Services.TAXI_111, Services.TAXI_128],
        commission_pct=Decimal('2.44'),
        min_commission=Decimal('0'),
        currency_rate_source=CurrencyRateSource.NBRB,
        precision=2
    )

    TAXI_UBER_AZ_CONTEXT = TAXI_UBER_BY_CONTEXT.new(
        name='TAXI_UBER_AZ_CONTEXT',
        payment_currency=Currencies.AZN,
        region=Regions.AZ,
        paysys=Paysyses.BANK_UR_UBER_USD,
        third_party_data=ThirdPartyData.TAXI_UBER_AZ,
        services=[Services.UBER, Services.UBER_ROAMING, Services.TAXI_111, Services.TAXI_128],
        currency_rate_source=CurrencyRateSource.CBAR
    )

    # Contexts for product mapping
    KINOPOISK_PLUS_CONTEXT = TAXI_RU_CONTEXT.new(
        name='KINOPOISK_PLUS_CONTEXT',
        person_type=PersonTypes.PH,
        firm=Firms.KINOPOISK_9,
        third_party_data=ThirdPartyData.MUSIC,
        service=Services.KINOPOISK_PLUS,
        contracts=[
            {
                'contract_type': ContractType.NOT_AGENCY,
                'currency': Currencies.RUB,
                'nds': Nds.YANDEX_RESIDENT,
                'product': Products.KINOPOISK_WITH_NDS,
                'paysys': Paysyses.BANK_PH_RUB_KINOPOISK
            },
            {
                'contract_type': ContractType.LICENSE,
                'currency': Currencies.RUB,
                'nds': Nds.NONE,
                'product': Products.KINOPOISK_WO_NDS,
                'paysys': Paysyses.BANK_PH_RUB_WO_NDS_KINOPOISK
            }
        ]
    )

    MUSIC_CONTEXT = TAXI_RU_CONTEXT.new(
        name='MUSIC_CONTEXT',
        person_type=PersonTypes.PH,
        firm=Firms.YANDEX_1,
        third_party_data=ThirdPartyData.MUSIC,
        service=Services.MUSIC,
        contracts=[
            {
                'contract_type': ContractType.NOT_AGENCY,
                'currency': Currencies.RUB,
                'nds': Nds.YANDEX_RESIDENT,
                'product': Products.MUSIC,
                'paysys': Paysyses.BANK_PH_RUB
            }
        ]
    )

    CARSHARING_CONTEXT = TAXI_RU_CONTEXT.new(
        name='CARSHARING_CONTEXT',
        person_type=PersonTypes.PH,
        firm=Firms.DRIVE_30,
        third_party_data=ThirdPartyData.MUSIC,
        service=Services.DRIVE,
        contracts=[
            {
                'contract_type': ContractType.NOT_AGENCY,
                'currency': Currencies.RUB,
                'nds': Nds.YANDEX_RESIDENT,
                'product': Products.CARSHARING_WITH_NDS_1,
                'paysys': Paysyses.BANK_PH_RUB_CARSHARING
            }
        ]
    )

    DISK_CONTEXT = TAXI_RU_CONTEXT.new(
        name='DISK_CONTEXT',
        person_type=PersonTypes.PH,
        firm=Firms.YANDEX_1,
        third_party_data=ThirdPartyData.MUSIC,
        service=Services.DISK,
        contracts=[
            {
                'contract_type': ContractType.NOT_AGENCY,
                'currency': Currencies.RUB,
                'nds': Nds.YANDEX_RESIDENT,
                'product': Products.DISK_RUB_WITH_NDS,
                'paysys': Paysyses.BANK_PH_RUB
            },
            {
                'contract_type': ContractType.NOT_AGENCY,
                'currency': Currencies.USD,
                'nds': Nds.NONE,
                'product': Products.DISK_USD_WO_NDS,
                'paysys': Paysyses.BANK_US_PH_USD
            }
        ]
    )

    PRACTICUM_US_YT_UR = obj.Contexts.PRACTICUM_US_YT_UR

    PRACTICUM_US_YT_PH = obj.Contexts.PRACTICUM_US_YT_PH

    # PARTNERS
    PARTNERS_DEFAULT = obj.Context(
        name='PARTNERS_RU',
        service=None,
        client_template=None,
        person_type=PersonTypes.UR,
        paysys=None,
        product=None,
        price=None,
        manager=Managers.NIGAI
    ).new(
        firm=Firms.YANDEX_1,
        currency=Currencies.RUB,
        nds=Nds.DEFAULT,
        default_partner_pct=43,
        default_market_api_pct=50,
        default_payment_type=2,
        default_open_date=0,
        default_search_forms=0,
        default_reward_type=1,
        create_offer_params={'ctype': 'PARTNERS', 'currency': Currencies.RUB.iso_code,
                             'firm_id': Firms.YANDEX_1.id, 'manager_uid': Managers.NIGAI.uid}
    )
