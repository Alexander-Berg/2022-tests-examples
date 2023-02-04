# coding=utf-8
from btestlib import secrets
from btestlib.secrets import get_secret, Robots, UsersPwd

__author__ = 'igogor'

from collections import namedtuple
from datetime import datetime
from decimal import Decimal
from balance import balance_db as db

from enum import Enum

import btestlib.utils as utils
from btestlib.environments import TrustDbNames, TrustApiUrls


# oebs_org_id - T_FIRM_EXPORT.OEBS_ORG_ID
class Firms(utils.ConstantsContainer):
    Firm = utils.namedtuple_with_defaults('Firm', ['id', 'name', 'inn', 'oebs_org_id'])
    constant_type = Firm

    YANDEX_1 = Firm(id=1, name=u'ООО «Яндекс»', inn=u'7736207543', oebs_org_id=121)
    YANDEX_UA_2 = Firm(id=2, name=u'ООО «Яндекс.Украина»')
    KAZNET_3 = Firm(id=3, name=u'ТОО «KazNet Media (КазНет Медиа)»')
    YANDEX_INC_4 = Firm(id=4, name=u'Yandex Inc')
    BELFACTA_5 = Firm(id=5, name=u'СООО «Белфакта Медиа»')
    EUROPE_BV_6 = Firm(id=6, name=u'Yandex Europe B.V.')
    EUROPE_AG_7 = Firm(id=7, name=u'Yandex Europe AG', oebs_org_id=263)
    YANDEX_TURKEY_8 = Firm(id=8, name=u'Yandex Turkey')
    KINOPOISK_9 = Firm(id=9, name=u'ООО «Кинопоиск»', oebs_org_id=48762)
    AUTO_RU_10 = Firm(id=10, name=u'ООО «АВТО.РУ Холдинг»')
    VERTICAL_12 = Firm(id=12, name=u'ООО «Яндекс.Вертикали»', oebs_org_id=64553)
    TAXI_13 = Firm(id=13, name=u'ООО «Яндекс.Такси»', oebs_org_id=64552)
    AUTO_RU_AG_14 = Firm(id=14, name=u'Auto.ru AG')
    SERVICES_AG_16 = Firm(id=16, name=u'Yandex Services AG', oebs_org_id=264)
    OFD_18 = Firm(id=18, name=u'ООО "Яндекс.ОФД"')
    TAXI_BV_22 = Firm(id=22, name=u'Yandex.Taxi B.V.', oebs_org_id=64621)
    TAXI_UA_23 = Firm(id=23, name=u'ООО «Яндекс.Такси Украина»', oebs_org_id=89471)
    TAXI_KAZ_24 = Firm(id=24, name=u'ТОО «Яндекс.Такси Казахстан»', oebs_org_id=94969)
    KZ_25 = Firm(id=25, name=u'ТОО «Яндекс.Казахстан»', oebs_org_id=97308)
    TAXI_AM_26 = Firm(id=26, name=u'Yandex.Taxi AM LLP', oebs_org_id=100110)
    REKLAMA_BEL_27 = Firm(id=27, name=u'Яндекс.Реклама', oebs_org_id=107441)
    ZEN_28 = Firm(id=28, name=u'ООО «Дзен.Платформа»', oebs_org_id=108190)
    YANDEX_NV_29 = Firm(id=29, name=u'Yandex Europe N.V.')
    DRIVE_30 = Firm(id=30, name=u'ООО "Яндекс.Драйв"', oebs_org_id=118724)
    TAXI_CORP_KZT_31 = Firm(id=31, name=u'ТОО "Яндекс.Такси Корп"', oebs_org_id=114867)
    FOOD_32 = Firm(id=32, name=u'ООО "Яндекс.Еда"', oebs_org_id=127556)
    HK_ECOMMERCE_33 = Firm(id=33, name=u'Yandex E-commerce Limited', oebs_org_id=149533)
    SHAD_34 = Firm(id=34, name=u'АНО ДПО «ШАД»', oebs_org_id=55562)
    YANDEX_GO_ISRAEL_35 = Firm(id=35, name=u'YANDEX.GO ISRAEL Ltd', oebs_org_id=159875)
    YANGO_ISRAEL_1090 = Firm(id=1090, name=u'Yango.Taxi Ltd', oebs_org_id=6586046)
    MARKET_111 = Firm(id=111, name=u'ООО «Яндекс.Маркет»', inn=u'7704357909', oebs_org_id=64554)
    CLOUD_112 = Firm(id=112, name=u'ООО «Облачные Технологии Яндекс»»')
    AUTOBUS_113 = Firm(id=113, name=u'ООО «Яндекс.Автобусы»', oebs_org_id=99766)
    HEALTH_114 = Firm(id=114, name=u'ООО «Клиника Яндекс.Здоровье»', oebs_org_id=119318)
    UBER_115 = Firm(id=115, name=u'Uber ML B.V.', oebs_org_id=115098)
    UBER_AZ_116 = Firm(id=116, name=u'UBER AZARBAYCAN MMC', oebs_org_id=115309)
    JAMS_120 = Firm(id=120, name=u'ООО «Яндекс.Пробки»»', oebs_org_id=201)
    MEDIASERVICES_121 = Firm(id=121, name=u'ООО «Яндекс.Медиасервисы»', oebs_org_id=139355)
    TAXI_CORP_ARM_122 = Firm(id=122, name=u'Yandex.Taxi Corp AM LLC', oebs_org_id=142924)
    CLOUD_123 = Firm(id=123, name=u'ООО «Яндекс.Облако»', oebs_org_id=145543)
    CLOUD_KZ = Firm(id=1020, name=u'ТОО "Яндекс.Облако Казахстан"', oebs_org_id=272037)
    GAS_STATIONS_124 = Firm(id=124, name=u'ООО «Яндекс.Заправки»', oebs_org_id=150405)
    MLU_EUROPE_125 = Firm(id=125, name=u'MLU Europe B.V.', oebs_org_id=199232)
    MLU_AFRICA_126 = Firm(id=126, name=u'MLU Africa B.V.', oebs_org_id=184377)
    YANDEX_GO_SRL_127 = Firm(id=127, name=u'Yandex.Go S.R.L', oebs_org_id=220654)
    BELGO_CORP_128 = Firm(id=128, name=u'ООО "БелГо Корп"', oebs_org_id=239214)
    LAVKA = Firm(id=129, name=u'ООО «Яндекс.Лавка»', oebs_org_id=127556)
    LOGISTICS_130 = Firm(id=130, name=u'ООО «Яндекс.Логистика»', oebs_org_id=111012)
    CLOUD_KZT = Firm(id=1020, name=u'ТОО "Яндекс.Облако Казахстан"', oebs_org_id=272037)
    K50 = Firm(id=1000, name=u'OOO "К50"', oebs_org_id=197805)
    SAS_DELI_1080 = Firm(id=1080, name=u'S.A.S. Deli International', oebs_org_id=289921)
    DELI_INT_LIM_1083 = Firm(id=1083, name=u'Deli International Limited', oebs_org_id=5141205)
    MICRO_MOBILITY = Firm(id=1086, name=u'ООО "Яндекс.Микромобильность"', oebs_org_id=197801)
    SIMPLE_SERVICES = Firm(id=1087, name=u'ООО «Простые сервисы»', oebs_org_id=None)
    UBER_1088 = Firm(id=1088, name=u'Uber ML B.V. BYN', oebs_org_id=281118)
    TAXI_CORP_AM = Firm(id=1091, name=u'ООО «Яндекс Деливери ЭйЭм»', oebs_org_id=666666) # на момент разработкки нет oebs_org_id
    YANDEX_DELIVERY_BY = Firm(id=1092, name=u'Yandex Delivery BY', oebs_org_id=6586070)
    YANDEX_DELIVERY_KZ = Firm(id=1093, name=u'ТОО «Яндекс Деливери Кейзет»', oebs_org_id=6584945)
    FOODTECH_DELIVERY_BV = Firm(id=1094, name=u'Foodtech & Delivery BV', oebs_org_id=6593441)
    MARKET_ISRAEL_1097 = Firm(id=1097, name=u'Yango Market Israel Ltd', oebs_org_id=6599577)
    BEYOND = Firm(id=1297, name=u'Beyond ML Limited liability company', oebs_org_id=None)  # в проде id=1891

    ZAPRAVKI_KZ_1096 = Firm(id=1096, name=u'ТОО «Яндекс.Заправки Казахстан»', oebs_org_id=6583515)
    SPB_SOFTWARE_1483 = Firm(
        id=1216, # 1483 в проде
        name=u'Spb Software Limited',
        oebs_org_id=239214,
    )
    DIRECT_CURSUS_1101 = Firm(id=1101, name=u"Direct Cursus Computer Systems Trading", oebs_org_id=239214)
    TAXI_CORP_KGZ_1100 = Firm(id=1100, name=u'ОсОО «Яндекс.Такси Корп»"', oebs_org_id=666666)
    WIND_1099 = Firm(id=1099, name=u'Wind Tel-Aviv (By-Byke) Ltd', oebs_org_id=6584927)

    YANGO_DELIVERY = Firm(id=1890, name=u'Yango Delivery', oebs_org_id=6963607)
    YANGO_DELIVERY_BEOGRAD_1898 = Firm(id=1898, name=u'Yango Delivery doo Beograd', oebs_org_id=6998766)
    YANGO_CHILE_SPA = Firm(id=1889, name=u'Yango Chile Spa', oebs_org_id=6859230)

    UBER_SYSTEMS_BEL = Firm(id=1928, name=u'ООО «Убер Системс Бел»', oebs_org_id=111042)
    DOUBLE_CLOUD_INC = Firm(id=1896, name=u'DoubleCloud Inc.', oebs_org_id=None)
    DOUBLE_CLOUD_GMBH = Firm(id=1897, name=u'DoubleCloud GmbH', oebs_org_id=None)
    YANDEX_LOG_OZB = Firm(id=1931, name=u'ООО «YANDEX LOG OZB»', oebs_org_id=7922871)


FIRM_BY_ID = {c.id: c for c in Firms.values() if isinstance(c, tuple)}


class Managers(utils.ConstantsContainer):
    Manager = namedtuple('Manager', 'uid code')

    constant_type = Manager

    # менеджеры продаж (manager_type=1, is_sales=1)
    # SOME_MANAGER = Manager(uid='244916211', code=21902)
    SOME_MANAGER = Manager(uid='98700241', code=21902)
    PERANIDZE = Manager(uid='98700241', code=20453)
    MANAGER_WO_PASSPORT_ID = Manager(uid='1120000000039514', code=28067)

    PRINT_FORM_MANAGER = Manager(uid='16571028', code=0)

    # менеджеры БО (manager_type=2, is_backoffice=1)
    FETISOVA = Manager(uid='91351977', code=20368)

    # менеджеры РСЯ (manager_type=3)
    NIGAI = Manager(uid='29786964', code=20160)

    # менеджеры Дистрибуции (manager_type=4)
    VECHER = Manager(uid='3692781', code=1048)

    # уволенный менеджер
    FIRED_MANAGER = Manager(uid='289982538', code=23285)


class PersonTypes(utils.ConstantsContainer):
    PersonType = namedtuple('PersonType', 'code')

    constant_type = PersonType

    # firm 1
    UR = PersonType(code='ur')
    YT = PersonType(code='yt')
    PH = PersonType(code='ph')
    YTPH = PersonType(code='ytph')
    YT_KZU = PersonType(code='yt_kzu')
    YT_KZP = PersonType(code='yt_kzp')
    ENDBUYER_UR = PersonType(code='endbuyer_ur')
    ENDBUYER_YT = PersonType(code='endbuyer_yt')
    ENDBUYER_PH = PersonType(code='endbuyer_ph')

    PH_AUTORU = PersonType(code='ph_autoru')
    UR_AUTORU = PersonType(code='ur_autoru')
    # firm 2
    UA = PersonType(code='ua')
    PU = PersonType(code='pu')
    # firm 4
    USU = PersonType(code='usu')
    USP = PersonType(code='usp')
    US_YT = PersonType(code='us_yt')
    US_YT_PH = PersonType(code='us_ytph')
    # firm 5
    BYU = PersonType(code='byu')
    BYP = PersonType(code='byp')
    # firm 7
    SW_UR = PersonType(code='sw_ur')
    SW_YT = PersonType(code='sw_yt')
    SW_YTPH = PersonType(code='sw_ytph')
    SW_PH = PersonType(code='sw_ph')
    BY_YTPH = PersonType(code='by_ytph')
    # firm 8
    TRU = PersonType(code='tru')
    TRP = PersonType(code='trp')
    # firm 25
    KZU = PersonType(code='kzu')
    KZP = PersonType(code='kzp')
    # firm 26
    AM_UR = PersonType(code='am_jp')
    AM_PH = PersonType(code='am_np')
    AM_YT = PersonType(code='am_yt')
    AM_YTPH = PersonType(code='am_ytph')

    EU_YT = PersonType(code='eu_yt')
    EU_UR = PersonType(code='eu_ur')

    # firm 33
    HK_YT = PersonType(code='hk_yt')
    HK_UR = PersonType(code='hk_ur')
    HK_YTPH = PersonType(code='hk_ytph')

    # firm 35
    IL_UR = PersonType(code='il_ur')

    # firm 116
    AZ_UR = PersonType(code='az_ur')

    # firm 127
    RO_UR = PersonType(code='ro_ur')

    FR_UR = PersonType(code='fr_ur')

    # firm 1083
    GB_UR = PersonType(code='gb_ur')

    # firm 31
    UR_YTKZ = PersonType(code='ur_ytkz')

    # firm 1100
    KG_UR = PersonType(code='kg_ur')

    # firm 1101
    UAE_UR = PersonType(code='uae_ur')
    UAE_YTUR = PersonType(code='uae_ytur')

    # firm 1894
    SK_UR = PersonType(code='sk_ur')

    # firm 1099
    IL_PH = PersonType(code='il_ph')

    # firm 1898
    SRB_UR = PersonType(code='srb_ur')

    # firm 1889
    CL_UR = PersonType(code='cl_ur')

    # firm 1897
    DE_UR = PersonType(code='de_ur')
    DE_PH = PersonType(code='de_ph')
    DE_YT = PersonType(code='de_yt')
    DE_YTPH = PersonType(code='de_ytph')

    # firm 1931
    UZB_UR = PersonType(code='uzb_ur')


PERSON_TYPES_LIST = PersonTypes.values()


class ServiceSchemaParams(utils.ConstantsContainer):
    TRUST_PRICE = 'TRUST_PRICE'
    SERVICE_PRICE = 'SERVICE_PRICE'
    PARTNER_PRICE = 'PARTNER_PRICE'
    IS_PARTNER = 'IS_PARTNER'
    TWO_PARTNER = 'TWO_PARTNER'
    EXTERNAL_PARTNER = 'EXTERNAL_PARTNER'
    REQUIRE_PRODUCT = 'REQUIRE_PRODUCT'


class ServiceSchemas(utils.ConstantsContainer):
    ServiceSchema = utils.namedtuple_with_defaults('ServiceSchema', [ServiceSchemaParams.TRUST_PRICE,
                                                                     ServiceSchemaParams.SERVICE_PRICE,
                                                                     ServiceSchemaParams.PARTNER_PRICE,
                                                                     ServiceSchemaParams.IS_PARTNER,
                                                                     ServiceSchemaParams.TWO_PARTNER,
                                                                     ServiceSchemaParams.EXTERNAL_PARTNER,
                                                                     ServiceSchemaParams.REQUIRE_PRODUCT])

    constant_type = ServiceSchema

    TRUST_AS_PROCESSING = ServiceSchema(0, 1, 0, 0, 0, 0, 0)
    SERVICE_PRICING_WITH_PRODUCT = ServiceSchema(0, 1, 0, 0, 0, 0, 1)
    TRUST_PRICING_WITHOUT_PARTNER = ServiceSchema(1, 0, 0, 0, 0, 0, 1)
    TRUST_PRICING_WITH_PARTNER = ServiceSchema(1, 0, 0, 1, 0, 0, 1)
    SERVICE_PRICING_WITH_PARTNER = ServiceSchema(0, 1, 0, 1, 0, 0, 1)
    SERVICE_PRICING_WITH_EXT_PARTNER = ServiceSchema(0, 1, 0, 1, 0, 1, 1)
    PARTNER_PRICING = ServiceSchema(0, 0, 1, 1, 0, 1, 1)
    SERVICE_PRICING_WITH_TWO_PARTNERS = ServiceSchema(0, 1, 0, 1, 1, 0, 1)


class Services(utils.ConstantsContainer):
    class Service(utils.namedtuple_with_defaults('Service', ['id', 'name', 'token', 'with_return_path', 'schema',
                                                             'trust_dbname', 'trust_xmlrpc', 'use_batch_export'],
                                                 default_values={'with_return_path': False,
                                                                 'trust_dbname': TrustDbNames.BS_ORACLE,
                                                                 'trust_xmlrpc': TrustApiUrls.XMLRPC_ORA,
                                                                 'use_batch_export': False})):
        """
        with_return_path - передает ли сервис параметр return_path при вызове CreateBasket
        Установлен эмпирическим путем Славой (@vaclav) по грепам логов
        [130, 151] - сервисы всегда передают return_path
        [7, 115, 117, 120, 135, 137] - сервисы никогда не передают return_path
        [23, 116, 118, 119, 126, 131] - при определенных условиях передают return_path
        (например оплата компенсицией или оплата через страничку траст,
        условия довольно туманны, поэтому в настоящий момент в тестах для этих сервисов передаем return_path)
        """
        __slots__ = ()

        def __str__(self):
            return u'{} ({})'.format(Services.name(self), self.id)

    constant_type = Service

    CATALOG1 = Service(id=5, name=u'Каталог1')
    CATALOG2 = Service(id=6, name=u'Каталог2')
    DIRECT = Service(id=7, name=u'Direct', token='PPC_d5bc1fbe74a173f4688d24fc6bf8399a',
                     schema=ServiceSchemas.TRUST_AS_PROCESSING)
    MARKET = Service(id=11, name=u'Маркет')
    CATALOG3 = Service(id=16, name=u'Каталог3')
    MUSIC = Service(id=23, name=u'Music', token='music_039128f74eaa55f94617c329b4c06e65', with_return_path=True,
                    schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER)
    MUSIC_TARIFFICATOR = Service(id=711, name=u'Яндекс.Музыка new', token='music_new_3cc7bf8206dd1002da385ae3e96e8831',
                                 with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT)
    MUSIC_MEDIASERVICE = Service(
        id=685, name=u'Яндекс.Подписка на Музыку',  token='music_mediaservices_ef5bbb17a203ede7e95bac7bdda685b',
        with_return_path=True, schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER)
    MUSIC_MEDIASERVICE_TARIFFICATOR = Service(
        id=714, name=u'Яндекс.Подписка на Музыку new', token='music_mediaservices_new_db1da1e33255ea8b31a2b9a31f076743',
        with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT)
    DISK_PLUS_NEW = Service(
        id=715, name=u'Yandex.Disk.Plus new', token='yandex_disk_plus_new_1dabec5d70c14a44a38cef60184ec15e',
        with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT)
    DISK_PLUS = Service(
        id=660, name=u'Yandex.Disk.Plus', token='yandex_disk_plus_56681923b9c10b23a65907f7ea1b178e',
        with_return_path=True, schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER)
    MUSIC_PROMO = Service(id=123, name=u'Яндекс.Музыка: Промокоды')
    OFD = Service(id=26, name=u'Яндекс.ОФД')
    SHOP = Service(id=35, name=u'Разовые продажи')
    GEO = Service(id=37, name=u'Геоконтекст', token='geocontext_1acbe8aff135e26e637a420494ed5148')
    TOLOKA = Service(id=42, name=u'Толока')
    TECHNOLOGIES = Service(id=45, name=u'Технологии Яндекса')
    METRICA = Service(id=48, name=u'Метрика')
    RIT = Service(id=50, name=u'рит')
    MEDIA_BANNERS = Service(id=67, name=u'Баннерокрутилка')
    MEDIA_70 = Service(id=70, name=u'Медийка', token='mediaselling_6920aeacf4b1cb1954512f0763fe01eb')
    BAYAN = Service(id=77, name=u'Баян')
    DSP = Service(id=80, name=u'DSP')
    REALTY = Service(id=81, name=u'Недвижимость')
    REALTY_COMM = Service(id=82, name=u'Яндекс.Недвижимость коммерческая')
    RABOTA = Service(id=90, name=u'Работа')
    TOURS = Service(id=98, name=u'Туры')  # сервис больше неактивен - BALANCEDUTY-1730
    AUTORU = Service(id=99, name=u'Авто.Ру', token='autoru_8407d392865745c588a2c4fff80a7dfa')
    DOSTAVKA_101 = Service(id=101, name=u'Доставка')
    ADFOX = Service(id=102, name=u'AdFox', token='adfox_84054dc156936de25e20ebd839ef5625')
    VZGLYAD = Service(id=103, name=u'Яндекс.Взгляд')
    TELEPHONY = Service(id=104, name=u'Яндекс.Взгляд')
    NAVI = Service(id=110, name=u'Навигатор', token='navigator_ca99f5b4fee80946bc5884c1d058af0a')

    BANKI = Service(id=113)
    KUPIBILET = Service(id=114, name=u'Купибилет')
    STORE = Service(id=115, name=u'Store', token='mobile_yastore_22f7f32c7bd262ba38b31e755127399c',
                    schema=ServiceSchemas.TRUST_PRICING_WITH_PARTNER)
    DISK = Service(id=116, name=u'Disk', token='yandex_disk_99bc52562c956690cbec7f0abdac9865', with_return_path=True,
                   schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER,
                   trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    PARKOVKI = Service(id=117, name=u'Parking', token='parkovki_001e4c8648f5e61c0976ee6ce5ea644b',
                       schema=ServiceSchemas.PARTNER_PRICING)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5, но оставлю, чтобы simple_api не ругались
    MARKETPLACE = Service(id=119, name=u'Marketplace', token='marketplace_925fed8397ca3a78ba41e28493ea8a61',
                          with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_EXT_PARTNER)
    DOSTAVKA = Service(id=120, name=u'Dostavka', token='dostavka_pay_af2643478f5d38263ad745125cd46c00',
                       schema=ServiceSchemas.SERVICE_PRICING_WITH_TWO_PARTNERS)
    YAC = Service(id=122, name=u'YAC', token='yac_a3392e4d97b0d057a57453dbbad0313d',
                  schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER)

    TAXI_111 = Service(id=111, name=u'Яндекс.Такси', token='taxi_03391071342573399a5a6d368cfa6c9d')
    # use_batch_export больше не используется, оставляю для истории на несколько релизов, после будет удален
    TAXI = Service(id=124, name=u'Яндекс.Такси: Платежи', token='taxifee_8c7078d6b3334e03c1b4005b02da30f4',
                   schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                   trust_dbname=TrustDbNames.BS_XG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG_TAXI,
                   use_batch_export=True, )
    UBER = Service(id=125, name=u'Яндекс.Убер: Платежи', token='ubertaxi_4ea0f5fc283dc942c27bc7ae022e8821',
                   trust_dbname=TrustDbNames.BS_XG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG_TAXI,
                   use_batch_export=True, )
    TAXI_VEZET = Service(id=666, name=u'Такси.Везёт', token='taxi_vezyot_560e8cd3e3d8d33843c636d4eadd59f2',
                         trust_dbname=TrustDbNames.BS_XG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG_TAXI,
                         use_batch_export=True, )
    TAXI_RUTAXI = Service(id=667, name=u'Такси.РуТакси', token='taxi_rutaxi_1694503e08145f8d81a8c948103ea995',
                          trust_dbname=TrustDbNames.BS_XG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG_TAXI,
                          use_batch_export=True, )
    TAXI_DELIVERY_CASH = Service(id=1161, name=u'Яндекс.Доставка: Оплаты наличными, комиссия',
                                 token='delivery_dd51ffa0f4f13ecc82ca12dbf2b039c9')
    TAXI_DELIVERY_PAYMENTS = Service(id=1162, name=u'Яндекс.Доставка: Платежи (агентский)',
                            token='deliveryfee_84a2c708418224b3cf9943c07f0cdbf3',
                            use_batch_export=True, )
    TAXI_DELIVERY_CARD = Service(id=1163, name=u'Яндекс.Доставка: Оплаты картой, комиссия',
                                 token='deliverycomsn_b7926100617b2c7fb357dafc13cf176a')
    TAXI_DELIVERY_SPENDABLE = Service(id=1164, name=u'Яндекс.Доставка: Выплаты таксопаркам',
                                      token='delivery_donate_b60d6c2c990107842ed9d24cf69589c1',
                                      use_batch_export=True, )
    TAXI_CORP = Service(id=135, name=u'Яндекс.Корпоративное Такси', token='taxi_corp_adfb605b109d525fceca9590b0c7d7e9',
                        schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                        trust_dbname=TrustDbNames.BS_XG, trust_xmlrpc=TrustApiUrls.XMLRPC_ORA)
    TAXI_CORP_CLIENTS = Service(id=650, name=u'Яндекс.Корпоративное Такси (Клиенты)',
                                token='taxi_corp_clients_d94678491f195f6158d72da30f205ea8')
    TAXI_CORP_PARTNERS = Service(id=651, name=u'Яндекс.Корпоративное Такси (Партнеры)',
                                 token='taxi_corp_partners_d624a85be85b612782c4cf2891eacfcf')
    GAMES_PAYMENTS = Service(id=658, name=u'Яндекс.Игры',
                             token='yandex_games_b9b09e9dcc29ef3063e10482b942aa59')
    LOGISTICS_CLIENTS = Service(id=718, name=u'Яндекс.Логистика: доход (клиенты)',
                                token='logistics_clients_145a0cd0dcfc1397e372f23351385639')
    LOGISTICS_PAYMENTS = Service(id=1040, name=u'Яндекс.Логистика: Платежи',
                                 token='logistics_payments_76cd41773a444c3705eccc7b4ff2dcd7')
    LOGISTICS_PARTNERS = Service(id=719, name=u'Яндекс.Логистика: расход (исполнители)',
                                 token='logistics_partners_10f7612ccfb60247f7db9d18594ac464')
    TAXI_SAMOKAT = Service(id=1122, name=u'Яндекс.Самокаты: Физлица (прием платежей)',
                           token='taxi_samokat_payments_8f584fd387b458595cfd57cb0cea2df8')
    TAXI_SAMOKAT_CARD = Service(id=1179, name=u'Самокаты: безналичные платежи',
                                token='taxi_samokat_noncash_5cab11c57eccccc4155eae3db58e4497')
    LOGISTICS_LK = Service(id=1124, name=u'Яндекс.Логистика: доступ к ЛК и доп.услуги',
                           token='logistics_lk_486a888fe0043a4dd4a1205203b3d3c8')

    TAXI_DONATE = Service(id=137, name=u'Яндекс.Такси: Выплаты таксопаркам',
                          token='taxi_donate_275ca7a85e3e80a74e470f22a88279f6',
                          schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                          trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    TAXI_BREND = Service(id=203, name=u'Яндекс.Такси: Обязательства брендирования')
    TAXI_128 = Service(id=128, name=u'Яндекс.Такси: Оплаты картой, комиссия',
                       trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    TAXI_REQUEST = Service(id=222, name=u'Яндекс.Такси (поручение)',
                           schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                           trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)

    SHAD = Service(id=127, name=u'SHAD', token='shad_conference_99c3670169e94a7495c21e65119aac28',
                   schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER)
    APIKEYS = Service(id=129, name=u'Кабинет Разработчика')
    APIKEYSAGENTS = Service(id=659, name=u'Кабинет Разработчика (агенты)')
    REALTYPAY = Service(id=130, name=u'Realty', token='realty_pay_93ad97c77eaf24df828f4955e73b8a9e',
                        with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT)
    TICKETS = Service(id=118, name=u'Яндекс.Билеты', token='tickets_f4ac4122ee48c213eec816f4d7944ea6',
                      with_return_path=True,
                      schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                      trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    EVENTS_TICKETS = Service(id=126, name=u'Билеты на мероприятия',
                             token='events_tickets_923c891a6334317f77c44d6733b69519', with_return_path=True,
                             schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                             trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    EVENTS_TICKETS_NEW = Service(id=131, name=u'Events Tickets NEW',
                                 token='events_tickets_new_dba8d372fda49cf9838708209d72bf02', with_return_path=True,
                                 schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                                 trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    EVENTS_TICKETS3 = Service(id=638, name=u'Яндекс.Билеты на мероприятия (схема 3)',
                              token='events_tickets_full_payout_96bc86a05890f7192ca148c7b68de2cf',
                              with_return_path=True,
                              schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                              trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    SUBAGENCY_EVENTS_TICKETS = Service(id=325, name=u'Субагенты. Билеты на мероприятия',
                                       token='subagency_events_tickets_d983f0705b049cdcf6daa1b7244ad94e',
                                       )
    SUBAGENCY_EVENTS_TICKETS_NEW = Service(id=326, name=u'Субагенты. Events Tickets NEW',
                                           token='events_tickets_new_dba8d372fda49cf9838708209d72bf02',
                                           )
    SUBAGENCY_EVENTS_TICKETS3 = Service(id=327, name=u'Субагенты. Яндекс.Билеты на мероприятия (схема 3)',
                                        token='events_tickets_full_payout_96bc86a05890f7192ca148c7b68de2cf',
                                        )
    MEDIA_ADVANCE = Service(id=328, name=u'Авансы.Яндекс.Билеты',
                            token='events_tickets_full_payout_96bc86a05890f7192ca148c7b68de2cf'
                            )

    VENDORS = Service(id=132, name=u'Рекомендованные магазины')
    ZEN = Service(id=134, name=u'Дзен')
    ZEN_IP_PAYMENT = Service(id=696, name=u'Дзен: выплаты ИП')
    ZEN_UR_ORG_PAYMENT = Service(id=695, name=u'Дзен: выплаты ООО')

    PASSPORT = Service(id=138, name=u'Passport', token='passport_d056025fbea3c4700729c5b96b0ff97b',
                       schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # MARKET_PARTNERS = Service(id=139, name=u'Доставка партнёрами Маркета',
    #                           token='market_partners_3b5d29dcbec1c567682579245770a6d0')
    TRANSLATE = Service(id=140, name=u'Translator', token='translate_c721c2844057c849fd9039049ba132cf',
                        schema=ServiceSchemas.TRUST_AS_PROCESSING)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # ADAPTER_DEVELOPER = Service(id=141, name=u'Адаптер Девелопер')
    # ADAPTER_RITEILER = Service(id=142, name=u'Адаптер Ритейлер')
    CLOUD_143 = Service(id=143, name=u'Cloud', token='cloud_3752c735922f2705f15c947866592e7e',
                        schema=ServiceSchemas.TRUST_AS_PROCESSING)
    CLOUD_MARKETPLACE_144 = Service(id=144, name=u'CloudMarketplace',
                                    token='cloud_marketplace_b5408e0357db1d1015b79951eef22d45',
                                    schema=ServiceSchemas.TRUST_AS_PROCESSING)
    HOSTING_SERVICE = Service(id=1131, name=u'Услуги хостинга', token='hosting_d090ca05b2ddc729af013f798317f85a')
    BUSES = Service(id=151, name=u'Buses', token='desperate_poincare_663806e6cb667222740cb8637eda715f',
                    with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                    trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    BUSES_2 = Service(id=602, name=u'Яндекс.Автобусы: Платежи',
                      token='buses_pay_26643c0072e9f3665a8e33fa04c1c905',
                      trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # BUSES_2_0 = Service(id=205, name=u'Яндекс.Автобусы 2.0 (SaaS)',
    #                     trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    YDF = Service(id=152, name=u'YDF', token='awesome_roentgen_44d5c277cb05a96ea429aa4c5b6f85da',
                  schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # HEALTH = Service(id=153, name=u'Яндекс.Медицина', token='high_edison_3b4b6e3fe99987fdf249729dacdd3295')
    CLOUD_154 = Service(id=154, name=u'Yandex.Cloud', token='pedantic_darwin_82e5af868f007f4e94af12571fd2a63d',
                        schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT)
    SERVICE_FOR_PROCESSINGS_TESTING = Service(id=155, name=u'determined_thompson',
                                              token='determined_thompson_37f56c8a7ec291282307753cd8fa9ab1',
                                              schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    MEDIA_BANNERS_167 = Service(id=167, name=u'Яндекс: Баннерокрутилка PDA')
    SCORING = Service(id=168, name=u'Скоринг')
    MEDICINE_PAY = Service(id=170, name=u'Яндекс.Телемедицина.Платежи',
                           token='medicine_pay_dc61dc6b3a6f89c5aadf6dfec684710e',
                           schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER, with_return_path=True,
                           trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    UFS = Service(id=171, name=u'Яндекс.Расписания', token='ticket_to_ride_1cfa278d19fb17d6396318ac235d826c',
                  schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    TRAVEL_ELECTRIC_TRAIN_CPPK = Service(id=716, name=u'Я.Путешествия электрички ЦППК',
                                         token='travel_electric_train_cppk_3b3e54cc302c049ea42bc79ee816b60c',
                                         schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5, но оставлю, чтобы simple_api не ругались
    NEW_MARKET = Service(id=172, name=u'Yandex_MarketPlace_New',
                         token='marketplacenew_565c4965602c775f02fdb131268652ac',
                         with_return_path=True, schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    DIRECT_TUNING = Service(id=177, name=u'direct_tuning')
    PRACTICUM = Service(id=178, token=u'practicum_494026a5026585599147db7372c35f3b')
    TALENTS = Service(id=179, token=u'practicum_494026a5026585599147db7372c35f3b')
    TELEMEDICINE2 = Service(id=270, name=u'Яндекс.Телемедицина')
    MEDIANA = Service(id=201, name=u'Яндекс.Медиана')
    CONNECT = Service(id=202, name=u'CONNECT', token='connect_c3efa38f7aa8b2b6bc7ed282cdf135ff')

    TELEMEDICINE_DONATE = Service(id=204, name=u'Яндекс.Телемедицина: Промокоды',
                                  token='medicine_donate_d64f83ad25034186cca0c2ac5b241488')
    MARKET_ANALYTICS = Service(id=206, name=u'Маркет.Аналитика')
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # COROBA = Service(id=210, name=u'Маркетинг. Короба')
    DMP = Service(id=280, name=u'DMP')
    SPAMDEF = Service(id=470)
    PUBLIC = Service(id=560)
    ZEN_SALES = Service(id=561, name=u'Дзен Продажи')
    KINOPOISK_PLUS = Service(id=600, name=u'Кинопоиск.Плюс', token='kinopoisk_plus_27242cf1eeb3969cd67bf28435be5261',
                             schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT,
                             trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    KINOPOISK_AMEDIATEKA = Service(id=635, name=u'Кинопоиск: подписка на амедиатеку',
                                   token='kinopoisk_amediateka_26a6f9c418ef81ea6669fa789b443b45',
                                   schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER,
                                   trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    KINOPOISK_AMEDIATEKA_TARIFFICATOR = Service(
        id=713, name=u'Кинопоиск: подписка на амедиатеку new',
        token='kinopoisk_amediateka_new_9b113047287144313fa6b4454b1d0a64',
        schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT,
        trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)

    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # BUSES_DONATE = Service(id=601, name=u'Яндекс.Автобусы: Выплаты',
    #                        token='buses_donate_d390ef0b27297664644e1736ce19860a')
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # MARKET_SHIPPING_PAY = Service(id=603, name=u'Доставка партнёрами Маркета: платежи',
    #                               token='market_shipping_pay_e48a2cd2704785ef17dfc956f308c1ac',
    #                               schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    DRIVE = Service(id=604, name=u'CARSHARING', token='carsharing_67c8df8c4a6ef03decdfd0f174d16641',
                    schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT,
                    trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    UBER_ROAMING = Service(id=605, name=u'Яндекс.Убер: Платежи в роуминге',
                           token='ubertaxi_roaming_8f0cbb8d35468f87bdae164f17e09011',
                           schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                           trust_dbname=TrustDbNames.BS_XG, trust_xmlrpc=TrustApiUrls.XMLRPC_ORA)
    AEROEXPRESS = Service(id=607, name=u'Аэроэкспресс платежи',
                          token='aeroexpress_payments_fffa3ace12358989c9d38eff4a442692')
    PLUS = Service(id=608, name=u'Яндекс.Плюс: платежи', token='yandex_plus_bd21b8db20e243ccb5edd216cb63c2bb',
                   schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER)
    BLUE_MARKET_SUBSIDY = Service(id=609, name=u'Синий Маркет. Субсидии',
                                  token='blue_market_subsidy_0b634330e3697ad34d74f0fe5af88230',
                                  schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    BLUE_MARKET_PAYMENTS = Service(id=610, name=u'Синий Маркет. Платежи',
                                   token='blue_market_payments_5fac16d65c83b948a5b10577f373ea7c',
                                   schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    QUASAR = Service(id=611, name=u'Яндекс.Станция', token='yandex_quasar_e1e7cd7684199fc4c930d62040a84d3b',
                     schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                     trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    BLUE_MARKET = Service(id=612, name=u'Синий маркет. Услуги.')
    BLUE_MARKET_REFUNDS = Service(id=613, name=u'Синий Маркет. Возвраты',
                                  token='blue_market_refunds_01ffa15ed858f597e4295447aacfd8b5',
                                  schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    RED_MARKET_PAYMENTS = Service(id=614, name=u'Красный Маркет. Платежи',
                                  token='red_market_payments_d387604a6664b49905edf636271ccd17',
                                  schema=ServiceSchemas.SERVICE_PRICING_WITH_EXT_PARTNER,
                                  with_return_path=True)
    RED_MARKET_BALANCE = Service(id=620, name=u'Красный Маркет. Платежи. Расчеты в балансе',
                                 token='red_market_balance_9220756d7eb772645679f907ba2500a3',
                                 schema=ServiceSchemas.SERVICE_PRICING_WITH_EXT_PARTNER,
                                 with_return_path=True, trust_dbname=TrustDbNames.BS_PG,
                                 trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5, но оставлю, чтобы simple_api не ругались
    RED_MARKET_SUBSIDY = Service(id=615, name=u'Красный Маркет. Субсидии',
                                 token='red_market_subsidy_ae74a5673d21c22e0c02fc09d2ea2909',
                                 schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    QUASAR_SRV = Service(id=616, name=u'Яндекс. Станция. Услуги',
                         token='yandex_quasar_srv_11cc4e664d61213de900d8a9a42137ea',
                         schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER,
                         trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    AFISHA_MOVIEPASS = Service(id=617, name=u'Afisha.MoviePass',
                               token='afisha_moviepass_4cd6a9031adfc1958d2d4c3f0d0e486e',
                               schema=ServiceSchemas.TRUST_PRICING_WITH_PARTNER)
    AFISHA_MOVIEPASS_TARIFFICATOR = Service(id=712, name=u'Afisha.MoviePass new',
                                            token='afisha_moviepass_new_9c947a8999b2ca80ebb90778d2bc9261',
                                            schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # RED_MARKET_SERVICES = Service(id=618, name=u'Красный маркет. Услуги.')
    SCOUTS = Service(id=619, name=u'Яндекс. Такси: Скауты')
    GAS_STATIONS = Service(id=621, name=u'Яндекс.Заправки', token='zapravki_ec6942354de13b309fd5324e965a94f9',
                           schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    MESSENGER = Service(id=625, name=u'Мессенджер: Платежи', token='messenger_4e7abffd245bf1a7cae68fb4fd5b5ce4')
    TAXI_SVO = Service(id=626, name=u'Яндекс.Такси: Шереметьево',
                       token='taxi_stand_svo_3afafce21c51919fd919d430ecc281c7')
    PHONE = Service(id=627, name=u'Яндекс.Телефон', token='yandex_phone_3d8796759c6184062a3a86e3e5bd088a', )

    FOOD_SERVICES = Service(id=628, name=u'Яндекс.Еда: Услуги', token='food_srv_041d8ecd903b118c4e78b65eb3033ba3', )
    FOOD_PAYMENTS = Service(id=629, name=u'Яндекс.Еда: Платежи', token='food_payment_c808ddc93ffec050bf0624a4d3f3707c', )

    FOOD_MERCURY_SERVICES = Service(id=1177, name=u'Еда.Меркурий:Услуги',
                                    token='inplace_payment_4129bb346d3632c9b3598703379e3fa4', )
    FOOD_MERCURY_PAYMENTS = Service(id=1176, name=u' Еда.Меркурий: Платежи',
                                    token='inplace_srv_0eab639db8f8282828b5982305f13ac5', )

    USLUGI = Service(id=710, name=u'Яндекс.Услуги: Платежи', token='uslugi_payments_ccb141ebaf825dae57e7a267f3a3f234', )

    FOOD_PHARMACY_SERVICES = Service(id=675, name=u'Еда.Аптеки: Услуги', token='food_pharmacy_srv_93bc598d9a8529a3695961c2110c1525', )
    EDA_HELP_PAYMENTS = Service(id=676, name=u'Помощь Рядом (округления)', token='food_pharmacy_payment_e4f51d813bf51b0aeefdf79c5ed93fb6', )

    REST_SITES_SERVICES = Service(id=721, name=u'Сайты Ресторанам: Услуги', token='restaurant_sites_srv_55dbb5c01721b3970389c466c30053d3', )
    REST_SITES_PAYMENTS = Service(id=722, name=u'Сайты Ресторанам: Платежи', token='restaurant_sites_payment_d5857c12b454cf96230dcce999601ee9', )

    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # ADDAPPTER_2_0 = Service(id=630, name=u'Адаптер 2.0', token='addappter_2_14866f5febcf19d18b6f6561559a319e')

    ZAXI = Service(id=636, name=u'Яндекс.Заправки: Продажа топлива', token='zaxi_8a1c092b0c3222c7ada0a1351d734006')
    ZAXI_DELIVERY = Service(id=1185, name=u'Яндекс.Заправки: Продажа топлива (Доставки)', token='zaxi_delivery_d4978f24329c3bf6a0b40a367f8877ef')

    ZAXI_SPENDABLE = Service(id=637, name=u'Яндекс.Заправки: Выплаты АЗС',
                             token='zaxi_spendable_e098c7c2e9d75c1823699e4b396727a6')
    ZAXI_AGENT_COMMISSION = Service(id=1172, name=u'Яндекс.Заправки: Комиссионная схема', token='zapravki_commission_598217e8bd3e58a46d1b66da1c368f0d')
    ZAXI_SELFEMPLOYED_TIPS = Service(id=1121, name=u'Яндекс.Заправки: Самозанятые', token='zapravki_smz_tips_128c750aec4acc81f272da16e388a96f')
    ZAXI_SELFEMPLOYED_SPENDABLE = Service(id=1120, name=u'Яндекс.Заправки: Выплаты самозанятым',
                             token='zapravki_smz_spendable_a11b5807a6def1c5412448e30d8f167d')

    HOTELS = Service(id=640, name=u'Яндекс.Отели', token='travel_hotels_8205326d79ebd0512137ee2fb1339166')
    TRAVEL = Service(id=641, name=u'Яндекс.Путешествия', token='travel_13053db5aca26bd52c46b0665b639e47')

    REFUELLER_PENALTY = Service(id=643, name=u'Драйв: Штрафы заправщиков',
                                token='drive_refueller_fines_844627927accf92c7f9ee9abc5292c9d')
    REFUELLER_SPENDABLE = Service(id=644, name=u'Драйв: Выплаты заправщикам',
                                  token='drive_refueller_spendable_4641685f86ce2246251a2f4e2e716aa1')

    FOOD_COURIER = Service(id=645, name=u'Еда: Курьеры (прием платежей)',
                           token='eat_delivery_agent_4d53f26a1bf3e99b340c27626e3a8395')
    FOOD_COURIER_SUBSIDY = Service(id=646, name=u'Еда: Курьеры (выплаты)',
                                   token='eat_delivery_spendable_6dac0edb606df4e7406901798126adb4')
    BUG_BOUNTY = Service(id=207, name=u'Bug Bounty', token='bug_bounty_e51444a529c49d34fbc780881dd06462')
    INVESTMENTS = Service(id=648, name=u'Инвестиции', token='investments_f8a5160e9ac81a7b93e690bc321d6ca8',
                          schema=ServiceSchemas.SERVICE_PRICING_WITH_PRODUCT,
                          trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)
    SUPERCHECK = Service(id=655, name=u'Яндекс.Суперчек', token='super_check_36bd30c032b235bb2ba0b9f8effce703',
                         schema=ServiceSchemas.SERVICE_PRICING_WITH_PARTNER)
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # PURPLE_MARKET = Service(id=665, name=u'Фиолетовый маркет. Услуги', token='purple_services_a041836389c6d7d27924b7e7db6190c2')
    FOOD_CORP = Service(id=668, name=u'Яндекс.Корпоративная Еда', token='eda_corp_599a6ebed1a8c363ef1b531687f43ea1')

    FOOD_SHOPS_SERVICES = Service(id=661, name=u'Еда.Магазины: Услуги', token='food_shops_srv_f8a33cb2016741c048751db5b80ceee9')
    FOOD_SHOPS_PAYMENTS = Service(id=662, name=u'Еда.Магазины: Платежи', token='food_shops_payment_fcb21ffaacdafaadf83a3b6e8f060a38')
    LAVKA_COURIER = Service(id=663, name=u'Лавка: Курьеры (прием платежей)',
                            token='lavka_delivery_agent_999da24a26f4a0ffb90e10478ca8bfe5')
    LAVKA_COURIER_SUBSIDY = Service(id=664, name=u'Лавка: Курьеры (выплаты)',
                                    token='lavka_delivery_spendable_a7891172907a953df2e7c0d73d21b96d')

    DISK_B2B = Service(id=671, name=u'Яндекс для бизнеса', token='yandex_b2b_78ff6ba112e5b01825a3ce85cb2d73c6')

    DRIVE_B2B = Service(id=672, name=u'Яндекс.Драйв для бизнеса', token='drive_b2b_1d09aec8d23682ba55cb6caa1aa246cd')

    DRIVE_CORP = Service(id=702, name=u'Копроративный Яндекс.Драйв', token='drive_corp_71c8c3bf4baf0ab9ffbcbe942b116697')

    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # PERSEY = Service(id=683, name=u'Яндекс.Помощь', token='health_help_e85ec25405151c5a84ac0754bf371531')

    MAIL_PRO = Service(id=690, name=u'Яндекс.ПочтаПро', token='yandex_mail_pro_e74ea476bb892c65754883afd5c69f17',
                       with_return_path=True, schema=ServiceSchemas.TRUST_PRICING_WITHOUT_PARTNER,
                       trust_dbname=TrustDbNames.BS_PG, trust_xmlrpc=TrustApiUrls.XMLRPC_PG)

    CORP_DISPATCHING = Service(id=697, name=u'Яндекс.Корпоративное Такси: Диспетчерские услуги',
                               token='taxi_corp_dispatchig_2b3ae66fb5cf391a9e223a1fedc86695')

    DELIVERY_DISPATCHING = Service(id=1168, name=u'Яндекс.Доставка РБ:Дисп.услуги',
                                   token='delivery_corp_dispatchig_4601634bde3aebdc9167dd1c6c631256')

    INGAME_PURCHASES = Service(id=677, name=u'Внутриигровые покупки', token='ingame_purchases_3912b4cc6476958af912ac1ba41488bc')

    FOOD_PICKER = Service(id=699, name=u'Еда: Магазины_Пикеры (прием платежей)',
                          token='eat_pick_agent_d1f7b10d730564b038ab3cdf6f687d2d')

    CLOUD_REFERAL = Service(id=693, name=u'Облака: реферальная программа (выплаты)',
                            token='cloud_referal_spendable_9b6a42891302456c484e711403e46ba8')

    FOOD_PICKER_BUILD_ORDER = Service(id=706, name=u'Еда: Магазины_Пикеры (сборка)',
                                      token='eat_pick_build_order_170d59c8e475d6c349bd91bf98e810d7')

    PLUS_2_0_INCOME = Service(id=703, name=u'Плюс 2.0 Доход',
                              token='plus_2_0_income_42ad86597beb9040e6c08e06ab10d16a')

    PLUS_2_0_EXPENDITURE = Service(id=704, name=u'Плюс 2.0 Расход',
                                   token='plus_2_0_expenditure_8c0d174f8443b1563c392c95d5c79d19')

    DRIVE_PLUS_2_0 = Service(id=705, name=u'Драйв Плюс 2.0',
                             token='drive_plus_2_0_a9745e2f4b12caead066e7ca111f3e3a')

    DIGITAL_COROBA = Service(id=724, name=u'Digital-короба',
                             token='digital_coroba_d8d6e903a982204922647c3cb92ca2bb')

    PVZ = Service(id=725, name=u'Партнёрский пункт выдачи заказов',
                  token='pvz_890dcfdff811352a14a3f73a402f12cf')

    PRACTICUM_SPENDABLE = Service(id=1041, name=u'Яндекс.Практикум: выплаты',
                        token='practicum_spendable_3135977656754babfc372287018738ce')

    Y_PAY = Service(id=1042, name=u'Yandex.Pay: Plus Backend',
                        token='yandex_pay_plus_backend_f8c528b768e7421ee0e3258995201aaa')

    HEALTH_PAYMENTS_PH = Service(id=723, name=u'Яндекс.Здоровье (платежи от физ.лиц)',
                                 token='health_payments_ph_256a8e339399f1848ee11a12215b67d5')

    MARKET_BLUE_AGENCY = Service(id=1020, name=u'Синий Маркет. Услуги Агентствам',
                                 token='blue_market_for_agency _d1c26239fbf0b1d180a27a1e40b14200')

    SORT_CENTER = Service(id=1060, name=u'Маркет Сортировочные центры',
                          token='sort_center_fa37dc32fd4a0d827511f484ad83b885')

    MARKET_DELIVERY_SERVICES = Service(id=1100, name=u'Яндекс.Маркет: Курьерские службы 3P',
                                       token='market_delivery_3abb6a8fca5ee3f1aebbb1c19ef59fed')

    ANNOUNCEMENT = Service(id=1061, name=u'Яндекс.Объявления',
                           token='yandex_announcement_9ea23f1a04d2edf846ef490a7da5481c')

    K50 = Service(id=1000, name=u'K50', token='K50_86b0a0d0e2d67f688423669a73247025')

    BILLING_MARKETING_SERVICES = Service(id=1126, name=u'Биллинг маркетинговых услуг',
                                         token='billing_marketing_services_026447404ea665b77c6470c4328da627')

    NEWS_PAYMENT = Service(id=1127, name=u'Яндекс.Новости',
                           token='news_9673fb91a5c856773838a80819468ed0')

    LAVKA_NEW = Service(id=1156, name=u'Лавка new',
                        token='food_shops_payment_2_a17ad51c5a0fbd870500594e3b5577c4')
    EDA_NEW = Service(id=1154, name=u'Еда new',
                      token='eat_delivery_agent_2_6d7b563a913d7949dc308e1563300342')
    DRIVE_NEW = Service(id=1157, name=u'Драйв new',
                        token='drive_sco_plus_2f85fde18c02124cc24159268d0ab8d0')

    PROCESSING = Service(id=1166, name=u'Processing',
                         token='processing_72fb37a9f6cc71c0a889207ce066946cs')

    TIPS_PLUS = Service(id=1159, name=u'Чаевые (прием платежей)',
                        token="cashback_tips_plus_f69fc455c8497ebe11fb565edefb0df9")

    ADVERTISEMENT = Service(id=1132, name=u'Реклама от внешних сеток',
                               token='advertisement_fc9f65fe903405a74059bc0900c8fffc')

    ZAXI_UNIFIED_CONTRACT = Service(id=1171, name=u'Яндекс.Такси (агентский сервис Единого Договора)',
                                    token='taxi_corp_clients_agent_contract_aeb47e685ae3e9eca3c8c9008fcbbeac')

    BLACK_MARKET = Service(id=1173, name=u'Яндекс.Маркет для бизнеса', token='market_business_83d5d496c77e728d74f072c635c2a2bd')

    TAXI_CORP_CLIENTS_USN_GENERAL = Service(id=1181, name=u'ЯТ.КК.УСН доходный',
                                            token='corptaxi_usn_rev_2ef7c4883bc99f7a5b5314513adab8db')
    TAXI_CORP_CLIENTS_USN_SPENDABLE = Service(id=1182, name=u'ЯТ.КК.УСН расходный',
                                              token='corptaxi_usn_exp_5802eb1f68c1bfe34ee33d56f82de298')
    TAXI_CORP_CLIENTS_USN_AGENT = Service(id=1183, name=u'ЯТ.КК.УСН агентский',
                                          token='corptaxi_usn_agn_46b292a989acb069155171dab49f7c94')

    DOUBLE_CLOUD_GENERAL = Service(id=1207, name=u'DoubleCloud',
                                   token='doublecloud_rev_agr_3a7972ca3165016a89246c0aa027f416')

    MAGISTRALS_SENDER_COMMISSION = Service(id=1187, name=u'ТС. Доходный. Оплата Запроса',
                                           token='ts_revenue_fd3870d9667a32752c90fb202ce8ee33')
    MAGISTRALS_SENDER_AGENT = Service(id=1188, name=u'ТС. Агентский Грузоотправитель',
                                      token='ts_agent_3b6ecb8a2a067ef9b52f56c9c11b0260')
    MAGISTRALS_CARRIER_COMMISSION = Service(id=1189, name=u'ТС. Доходный. Комиссия',
                                            token='ts_commision_d4275fddcd5605426b079e1d166843c5')
    MAGISTRALS_CARRIER_AGENT = Service(id=1191, name=u'ТС. Агентский. Перевозчик',
                                       token='ts_agent_per_0bd53e3023d33ea8875237bd700b0203')

    PAYMENT_BRAND = Service(id=1209, name=u'Сервис для выплат за брендирование',
                            token='payment_brand_941972862619af313982b26e2c010b1d')

    TEST_SERVICE = Service(id=9999, name=u'Тестовый сервис')


SERVICE_BY_ID = {c.id: c for c in Services.values() if isinstance(c, tuple)}
ALL_SERVICE_IDS = [c.id for c in Services.values()]

UNIFIED_CORP_CONTRACT_SERVICES = [
    Services.TAXI_CORP_CLIENTS.id,
    Services.TAXI_CORP.id,
    Services.FOOD_CORP.id,
    Services.DRIVE_B2B.id,
]


TRUST_BILLING_SERVICE_MAP = {
    Services.MUSIC_TARIFFICATOR.id: Services.MUSIC,
    Services.MUSIC_MEDIASERVICE_TARIFFICATOR.id: Services.MUSIC_MEDIASERVICE,
    Services.AFISHA_MOVIEPASS_TARIFFICATOR.id: Services.AFISHA_MOVIEPASS,
    Services.KINOPOISK_AMEDIATEKA_TARIFFICATOR.id: Services.KINOPOISK_AMEDIATEKA,
    Services.DISK_PLUS_NEW.id: Services.DISK_PLUS,
}


class ProductTypes(utils.ConstantsContainer):
    ProductType = namedtuple('ProductType', 'code')
    constant_type = ProductType

    BUCKS = ProductType(code='Bucks')
    MONEY = ProductType(code='Money')
    DAYS = ProductType(code='Days')
    UNITS = ProductType(code='Units')
    SHOWS = ProductType(code='Shows')
    CLICKS = ProductType(code='Clicks')
    MONTHS = ProductType(code='Months')


class Product(object):
    def __init__(self, id=None, service=None, type=None, multicurrency_type=None, mdh_id=None):
        """id may be None, pass mdh_id to get id from db"""
        self.mdh_id = mdh_id
        self.id = id
        self.service = service
        self.type = type
        self.multicurrency_type = multicurrency_type

    # Не нужно так делать
    # def id_by_mdh_id(self):
    #     products = db.balance().execute("select id from bo.t_product where mdh_id=:mdh_id", {"mdh_id": self.mdh_id})
    #     if products:
    #         return products[0]['id']


# a-vasin: service для продукта указан тот, который используется в 99% случаев
# в целом, конечно, такой однозначный мапинг не может существовать
class Products(utils.ConstantsContainer):
    class __metaclass__(type):
        def __init__(cls, cl_name, bases, dct):
            # Так в общем-то тоже, этот код нужно перенести в инициализацию тестов
            products_w_mdh_id = [p for p in dct.values() if isinstance(p, Product) and p.mdh_id and not p.id]
            assert len(products_w_mdh_id) < 1000
            mdh_ids = ', '.join("'%s'" % p.mdh_id for p in products_w_mdh_id)
            db_products = db.balance().execute(
                "select mdh_id, id from bo.t_product where mdh_id in ({})".format(mdh_ids))
            mdh_id2id = {p['mdh_id']: p['id'] for p in db_products}
            for p in products_w_mdh_id:
                p.id = mdh_id2id.get(p.mdh_id)
            type.__init__(cls, cl_name, bases, dct)
    constant_type = Product

    # CATALOG1 = 5
    CATALOG1 = Product(id=506566, service=Services.CATALOG1, type=ProductTypes.UNITS, multicurrency_type=None)
    CATALOG1_87 = Product(id=87, service=Services.CATALOG1, type=ProductTypes.UNITS, multicurrency_type=None)

    # CATALOG2 = 6
    CATALOG2 = Product(id=506567, service=Services.CATALOG2, type=ProductTypes.UNITS, multicurrency_type=None)
    CATALOG2_1636 = Product(id=1636, service=Services.CATALOG2, type=ProductTypes.UNITS, multicurrency_type=None)

    # DIRECT = 7
    DIRECT_FISH = Product(id=1475, service=Services.DIRECT, type=ProductTypes.BUCKS,
                          multicurrency_type=ProductTypes.MONEY)
    DIRECT_RUB = Product(id=503162, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_USD = Product(id=503163, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_UZS = Product(id=511002, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_TRY = Product(id=503354, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_CHF = Product(id=503353, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_EUR = Product(id=503164, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_KZT = Product(id=503166, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_KZT_508892 = Product(id=508892, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_KZT_QUASI = Product(id=508756, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_BYN = Product(id=507529, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    # DIRECT_BYN = Product(id=512236, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_BYN_QUASI = Product(id=507529, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_GBP = Product(id=512808, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    DIRECT_UAH = Product(id=503165, service=Services.DIRECT, type=ProductTypes.MONEY, multicurrency_type=None)
    MEDIA_DIRECT_FISH = Product(id=508569, service=Services.DIRECT, type=ProductTypes.BUCKS,
                                multicurrency_type=ProductTypes.MONEY)
    MEDIA_DIRECT_CHAST = Product(id=10000035, service=Services.DIRECT, type=ProductTypes.BUCKS,
                                 multicurrency_type=ProductTypes.MONEY)
    MEDIA_DIRECT_RUB = Product(id=508587, service=Services.DIRECT, type=ProductTypes.MONEY,
                               multicurrency_type=ProductTypes.MONEY)
    VIDEO_DIRECT_RUB = Product(id=509420, service=Services.DIRECT, type=ProductTypes.MONEY,
                               multicurrency_type=ProductTypes.MONEY)

    # MARKET = 11
    MARKET = Product(id=2136, service=Services.MARKET, type=ProductTypes.BUCKS, multicurrency_type=None)
    MARKET_TEST_10000000 = Product(mdh_id='4d20203a-7c82-4c66-9dce-a317ae388371',
                                      service=Services.MARKET, type=ProductTypes.BUCKS,
                                      multicurrency_type=None)
    MARKET_TEST_10000004 = Product(mdh_id='2ed547f7-6c85-45e6-a628-6fb4ffd99f1c',
                                   service=Services.MARKET, type=ProductTypes.BUCKS,
                                   multicurrency_type=None)
    MARKET_TEST_10000008 = Product(mdh_id='ff8abed3-fe95-4213-ad0e-0079c307055f',
                                   service=Services.MARKET, type=ProductTypes.BUCKS,
                                   multicurrency_type=None)
    MARKET_TEST_10000012 = Product(mdh_id='e4ad4858-7dbc-437b-b7b3-549ee0f3ed26',
                                   service=Services.MARKET, type=ProductTypes.BUCKS,
                                   multicurrency_type=None)

    # MUSIC = 23
    MUSIC = Product(id=503782, service=Services.MUSIC, type=ProductTypes.MONEY, multicurrency_type=None)
    MUSIC_SUBSCRIBE = Product(id=504084, service=Services.MUSIC, type=ProductTypes.MONEY, multicurrency_type=None)
    MUSIC_WO_NDS = Product(id=66600004, service=Services.MUSIC, type=ProductTypes.MONEY, multicurrency_type=None)
    MUSIC_503356 = Product(id=503356, service=Services.MUSIC, type=ProductTypes.MONTHS, multicurrency_type=None)
    MUSIC_505135 = Product(id=505135, service=Services.MUSIC, type=ProductTypes.MONTHS, multicurrency_type=None)

    # MUSIC = 685
    MUSIC_MEDIASERVICE = Product(id=511549, service=Services.MUSIC_MEDIASERVICE, type=ProductTypes.MONEY,
                                 multicurrency_type=None)
    MUSIC_MEDIASERVICE_SUBSCRIBE = Product(id=511549, service=Services.MUSIC_MEDIASERVICE, type=ProductTypes.MONEY,
                                           multicurrency_type=None)
    # MUSIC_TARIFFICATOR = 711
    MUSIC_TARIFFICATOR = Product(id=511914, service=Services.MUSIC_TARIFFICATOR, type=ProductTypes.MONEY,
                                 multicurrency_type=None)
    MUSIC_TARIFFICATOR_DISK = Product(mdh_id='612e0f9c-a3d7-4d02-be0b-a31a57242da4',
                                      service=Services.MUSIC_TARIFFICATOR, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    # MUSIC_MEDIASERVICE_TARIFFICATOR = 714
    MUSIC_MEDIASERVICE_TARIFFICATOR = Product(id=711549, service=Services.MUSIC_MEDIASERVICE_TARIFFICATOR,
                                              type=ProductTypes.MONEY, multicurrency_type=None)

    # OFD = 26
    OFD_YEAR = Product(id=508472, service=Services.OFD, type=ProductTypes.DAYS, multicurrency_type=None)
    OFD_BUCKS = Product(id=508468, service=Services.OFD, type=ProductTypes.BUCKS, multicurrency_type=None)

    # SHOP = 35
    SPEECHKIT = Product(id=503995, service=Services.SHOP, type=ProductTypes.UNITS, multicurrency_type=None)
    PRAKTICUM_INFORMATION_PROGRAMS = Product(
        id=513634,
        service=Services.SHOP,
        type=ProductTypes.MONEY,
        multicurrency_type=None
    )
    TEST_AM_PRODUCT = Product(id=513697, service=Services.SHOP, type=ProductTypes.MONEY, multicurrency_type=None)

    HK_TEST_PRODUCT = Product(id=513677, service=Services.SHOP, type=ProductTypes.MONEY, multicurrency_type=None)

    # GEO = 37
    GEO = Product(id=502952, service=Services.GEO, type=ProductTypes.DAYS, multicurrency_type=None)
    GEO_2 = Product(id=502950, service=Services.GEO, type=ProductTypes.DAYS, multicurrency_type=None)
    GEO_3 = Product(id=502986, service=Services.GEO, type=ProductTypes.DAYS, multicurrency_type=None)  # UA, Price = 400
    GEO_4 = Product(id=502918, service=Services.GEO, type=ProductTypes.DAYS, multicurrency_type=None)
    GEO_508261 = Product(id=508261, service=Services.GEO, type=ProductTypes.DAYS, multicurrency_type=None)
    GEO_508260 = Product(id=508260, service=Services.GEO, type=ProductTypes.BUCKS, multicurrency_type=None)
    GEO_508258 = Product(id=508258, service=Services.GEO, type=ProductTypes.BUCKS, multicurrency_type=None)
    GEO_509780 = Product(id=509780, service=Services.GEO, type=ProductTypes.BUCKS, multicurrency_type=None)
    GEO_510792 = Product(id=510792, service=Services.GEO, type=ProductTypes.BUCKS, multicurrency_type=None)
    GEO_510794 = Product(id=510794, service=Services.GEO, type=ProductTypes.BUCKS, multicurrency_type=None)

    # TOLOKA = 42
    TOLOKA = Product(id=507130, service=Services.TOLOKA, type=ProductTypes.BUCKS, multicurrency_type=None)

    # METRICA = 48
    METRICA = Product(id=503369, service=Services.METRICA, type=ProductTypes.BUCKS, multicurrency_type=None)

    # BAYAN = 67
    BAYAN = Product(id=2584, service=Services.MEDIA_BANNERS, type=ProductTypes.SHOWS,
                    multicurrency_type=None)  # UA, Price = 125

    # MEDIA = 70
    MEDIA_2 = Product(id=502941, service=Services.MEDIA_70, type=ProductTypes.DAYS, multicurrency_type=None)
    MEDIA_8MD = Product(id=503112, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                        multicurrency_type=None)  # UA, Price = 9

    MEDIA_509291 = Product(id=509291, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                           multicurrency_type=None)
    MEDIA = Product(id=503123, service=Services.MEDIA_70, type=ProductTypes.SHOWS, multicurrency_type=None)
    MEDIA_KZ = Product(id=509579, service=Services.MEDIA_70, type=ProductTypes.DAYS, multicurrency_type=None)  # KZ
    MEDIA_BY = Product(id=509562, service=Services.MEDIA_70, type=ProductTypes.DAYS, multicurrency_type=None)  # KZ
    # = Product(id=503328,    type=ProductTypes.DAYS,  multicurrency_type=None)
    MEDIA_0MD_2 = Product(id=504033, service=Services.MEDIA_70, type=ProductTypes.SHOWS, multicurrency_type=None)  # UA,
    MEDIA_3 = Product(id=504194, service=Services.MEDIA_70, type=ProductTypes.DAYS,
                      multicurrency_type=None)  # Unit=month, Price = 29000
    MEDIA_FOR_AUTORU = Product(id=505039, service=Services.MEDIA_70, type=ProductTypes.SHOWS, multicurrency_type=None)
    MEDIA_0MD = Product(id=506561, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                        multicurrency_type=None)  # UA, Price = 40
    MEDIA_KZ_AUCTION = Product(id=506964, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                               multicurrency_type=None)  # KZ
    MEDIA_507784 = Product(id=507784, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                           multicurrency_type=None)
    MEDIA_508125 = Product(id=508125, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                           multicurrency_type=None)

    MEDIA_508868 = Product(id=508868, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                           multicurrency_type=None)

    MEDIA_31MD_MONEY = Product(id=10000015, service=Services.MEDIA_70, type=ProductTypes.MONEY, multicurrency_type=None)
    MEDIA_10MD = Product(id=10000016, service=Services.MEDIA_70, type=ProductTypes.SHOWS,
                         multicurrency_type=None)  # UA, Price = 6000
    MEDIA_31MD = Product(id=100000000, service=Services.MEDIA_70, type=ProductTypes.MONEY, multicurrency_type=None)
    MEDIA_4 = Product(id=507249, service=Services.MEDIA_70, type=ProductTypes.SHOWS, multicurrency_type=None)
    MEDIA_2543_GROUP = Product(id=507937, service=Services.MEDIA_70, type=ProductTypes.SHOWS, multicurrency_type=None)
    MEDIA_UZ = Product(id=508705, service=Services.MEDIA_70, type=ProductTypes.SHOWS, multicurrency_type=None)

    # REALTY = 81
    REALTY = Product(id=505151, service=Services.REALTY, type=ProductTypes.BUCKS, multicurrency_type=None)
    REALTY2 = Product(id=503937, service=Services.REALTY, type=ProductTypes.BUCKS, multicurrency_type=None)

    REALTY_COMM = Product(id=507211, service=Services.REALTY_COMM, type=ProductTypes.BUCKS, multicurrency_type=None)

    # RABOTA = 90
    RABOTA = Product(id=506655, service=Services.RABOTA, type=ProductTypes.BUCKS, multicurrency_type=None)

    # AUTO = 97 - сервис влит в AutoRu(98)

    # TOURS = 98
    # TOURS = Product(id=504939, service=Services.TOURS, type=ProductTypes.BUCKS, multicurrency_type=None)
    # TOURS_NONRES = Product(id=507188, service=Services.TOURS, type=ProductTypes.BUCKS, multicurrency_type=None)
    # TOURS_505507 = Product(id=505057, service=Services.TOURS, type=ProductTypes.BUCKS, multicurrency_type=None)
    # TOURS_506624 = Product(id=506624, service=Services.TOURS, type=ProductTypes.BUCKS, multicurrency_type=None)
    # TOURS_506587 = Product(id=506587, service=Services.TOURS, type=ProductTypes.BUCKS, multicurrency_type=None)
    # AUTO_RU = 99
    AUTORU = Product(id=505206, service=Services.AUTORU, type=ProductTypes.DAYS, multicurrency_type=None)
    AUTORU_505123 = Product(id=505123, service=Services.AUTORU, type=ProductTypes.MONEY, multicurrency_type=None)
    AUTORU_508999 = Product(id=508999, service=Services.AUTORU, type=ProductTypes.MONEY, multicurrency_type=None)
    AUTORU_2 = Product(id=504601, service=Services.AUTORU, type=ProductTypes.DAYS, multicurrency_type=None)
    AUTORU_TIRES_AND_DISKS = Product(id=504697, service=Services.AUTORU, type=ProductTypes.DAYS,
                                     multicurrency_type=None)

    # ADFOX = 102
    ADFOX = Product(id=507853, service=Services.ADFOX, type=ProductTypes.UNITS, multicurrency_type=None)
    ADFOX_505170 = Product(id=505170, service=Services.ADFOX, type=ProductTypes.SHOWS, multicurrency_type=None)
    # Product(id=505176, type=, multicurrency_type=None)
    # Product(id=504402, type=, multicurrency_type=None)
    # Product(id=505173, type=, multicurrency_type=None)

    # VZGLYAD = 103
    VZGLYAD = Product(id=508996, service=Services.VZGLYAD, type=ProductTypes.UNITS, multicurrency_type=None)
    VZGLYAD_BUCKS = Product(id=509386, service=Services.VZGLYAD, type=ProductTypes.BUCKS, multicurrency_type=None)

    # TELEPHONY
    TELEPHONY = Product(id=510956, service=Services.TELEPHONY, type=ProductTypes.BUCKS, multicurrency_type=None)
    # NAVI  110
    NAVI = Product(id=508674, service=Services.NAVI, type=ProductTypes.BUCKS, multicurrency_type=None)
    NAVI_510710 = Product(id=510710, service=Services.NAVI, type=ProductTypes.BUCKS, multicurrency_type=None)
    NAVI_509961 = Product(id=509961, service=Services.NAVI, type=ProductTypes.BUCKS, multicurrency_type=None)

    # 111
    # ? = Product(id=503409, type=, multicurrency_type=None)

    # KUPIBILET = 114
    KUPIBILET = Product(id=502981, service=Services.KUPIBILET, type=ProductTypes.BUCKS, multicurrency_type=None)

    # DISK = 116
    DISK_RUB_WITH_NDS = Product(id=509175, service=Services.DISK, type=ProductTypes.MONEY, multicurrency_type=None)
    DISK_USD_WO_NDS = Product(id=509176, service=Services.DISK, type=ProductTypes.MONEY, multicurrency_type=None)

    # MUSIC_PROMO = 123
    MUSIC_PROMO = Product(id=503801, service=Services.MUSIC_PROMO, type=ProductTypes.MONEY, multicurrency_type=None)

    # 128
    # ? = Product(id=505142, type=, multicurrency_type=None)
    APIKEYS = Product(id=508202, service=Services.VENDORS, type=ProductTypes.BUCKS, multicurrency_type=None)
    APIKEYS_TEST = Product(id=508229, service=Services.APIKEYS, type=ProductTypes.BUCKS, multicurrency_type=None)
    APIKEYS_ROUTES_COURIER_MINIMAL = Product(id=512327, service=Services.APIKEYS, type=ProductTypes.MONEY, multicurrency_type=None)
    APIKEYS_ROUTES_COURIER_OVERLIMIT = Product(id=512330, service=Services.APIKEYS, type=ProductTypes.MONEY, multicurrency_type=None)
    # VENDORS = 132
    VENDOR = Product(id=507175, service=Services.VENDORS, type=ProductTypes.BUCKS, multicurrency_type=None)
    VENDOR_BRAND = Product(id=509281, service=Services.VENDORS, type=ProductTypes.BUCKS, multicurrency_type=None)

    # VENDOR_2 = Product(id=507013, type=, multicurrency_type=None)

    MARKET_ANALYTICS = Product(id=509758, service=Services.MARKET_ANALYTICS, type=ProductTypes.BUCKS,
                               multicurrency_type=None)

    # CORP TAXI = 135
    CORP_TAXI_RUB = Product(id=507154, service=Services.TAXI_CORP, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_KZT = Product(id=509192, service=Services.TAXI_CORP, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_MIN_COST = Product(id=509703, service=Services.TAXI_CORP, type=ProductTypes.MONEY,
                                 multicurrency_type=None)
    # CORP TAXI CLIENTS 650
    CORP_TAXI_CLIENTS_RUB = Product(id=509966, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_CLIENTS_KZT = Product(id=509963, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_CLIENTS_ILS = Product(id=510013, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_CLIENTS_KGS = Product(mdh_id='71efc2ce-54b0-44d1-9a23-8d955aa0d0da', service=Services.TAXI_CORP_CLIENTS,
                                    type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_DELIVERY_KZT = Product(id=510991, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_DELIVERY_ILS = Product(id=511203, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_CARGO_RUB = Product(id=510062, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY, multicurrency_type=None)
    CORP_TAXI_DELIVERY_RUB = Product(id=510789, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY,
                                     multicurrency_type=None)
    CORP_TAXI_CLIENTS_MIN_COST_RUB = Product(id=509965, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None)
    CORP_TAXI_CLIENTS_BYN = Product(id=511458, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY,
                                    multicurrency_type=None)
    CORP_TAXI_CARGO_BYN = Product(id=511460, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY,
                                  multicurrency_type=None)
    CORP_TAXI_DELIVERY_BYN = Product(id=511461, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY,
                                     multicurrency_type=None)
    CORP_TAXI_CLIENTS_MIN_COST_BYN = Product(id=511459, service=Services.TAXI_CORP_CLIENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None)
    # LOGISTICS CLIENTS 718
    LOGISTICS_CLIENTS_DELIVERY_RUB = Product(id=511701, service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None)
    LOGISTICS_CLIENTS_DELIVERY_ILS = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None, mdh_id='8b740761-49ee-464c-a651-384cde3f1574')
    LOGISTICS_CLIENTS_DELIVERY_BYN = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None, mdh_id='9c322314-cad9-4dc4-8557-28b3da6aa509')
    LOGISTICS_CLIENTS_DELIVERY_KZT = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None, mdh_id='730e9a73-b092-49fc-83ee-576749f4614b')
    LOGISTICS_CLIENTS_CARGO_RUB = Product(id=511702, service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                          multicurrency_type=None)
    LOGISTICS_CLIENTS_CARGO_ILS = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                          multicurrency_type=None, mdh_id='6ade879a-5014-4012-bca2-e7f3fd1b2d3a')
    LOGISTICS_CLIENTS_CARGO_BYN = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                          multicurrency_type=None, mdh_id='10397d43-2a1e-44e3-b189-6bcd74ffa9f3')
    LOGISTICS_CLIENTS_CARGO_KZT = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                          multicurrency_type=None, mdh_id='3a15730a-f9c5-45ad-b093-7d217c8dc26c')

    LOGISTICS_LK_MAIN_RUB = Product(service=Services.LOGISTICS_LK, type=ProductTypes.MONEY, multicurrency_type=None,
                                    mdh_id='0bb32230-9349-4d38-8d1a-2fd3183e6b5f')

    # MARKET_PARTNERS = 139
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # MARKET_PARTNERS = Product(id=508244, service=Services.MARKET_PARTNERS, type=ProductTypes.BUCKS,
    #                           multicurrency_type=None)

    # CLOUD 143
    # todo-igogor после того как появится 509071 надо тестить с ним. От клауда был загон что это их основной продукт
    # CLOUD = Product(id=508563, service=Services.CLOUD_143, type=ProductTypes.UNITS, multicurrency_type=None)
    CLOUD = Product(id=509071, service=Services.CLOUD_143, type=ProductTypes.MONEY, multicurrency_type=None)
    CLOUD_KZ = Product(service=Services.CLOUD_143, type=ProductTypes.MONEY, multicurrency_type=None,
                       mdh_id='50b81149-279d-480d-a927-d1c97a1ab350'
                       )
    # CLOUD = Product(id=66600011, service=Services.CLOUD_143, type=ProductTypes.MONEY,
    #                 multicurrency_type=None)
    CLOUD_TEST = Product(id=508835, service=Services.CLOUD_143, type=ProductTypes.UNITS, multicurrency_type=None)
    # Hosting service 1131
    HOSTING_SERVICE = Product(service=Services.HOSTING_SERVICE, type=ProductTypes.MONEY,
                              multicurrency_type=None, mdh_id='acd9a979-b610-4b6a-b04b-873d22fca7be')


    # 170
    TELEMEDICINE2 = Product(id=508331, service=Services.TELEMEDICINE2, type=ProductTypes.MONEY, multicurrency_type=None)
    TELEMEDICINE2_WO_NDS = Product(id=510760, service=Services.TELEMEDICINE2, type=ProductTypes.MONEY,
                                   multicurrency_type=None)

    # 177
    DIRECT_TUNING_1 = Product(id=508498, service=Services.DIRECT_TUNING, type=ProductTypes.BUCKS,
                              multicurrency_type=None)
    DIRECT_TUNING_2 = Product(id=508496, service=Services.DIRECT_TUNING, type=ProductTypes.BUCKS,
                              multicurrency_type=None)
    DIRECT_TUNING_3 = Product(id=508497, service=Services.DIRECT_TUNING, type=ProductTypes.BUCKS,
                              multicurrency_type=None)

    # 178
    # заменен после переналивки 18.11
    PRACTICUM = Product(id=511308, service=Services.PRACTICUM, type=ProductTypes.BUCKS,
                        multicurrency_type=None)
    PRACTICUM_511328 = Product(id=511328, service=Services.PRACTICUM, type=ProductTypes.BUCKS,
                        multicurrency_type=None)

    # 201
    MEDIANA = Product(id=508386, service=Services.MEDIANA, type=ProductTypes.BUCKS, multicurrency_type=None)

    PRODUCT_TEST_SERVICE = Product(id=99999999, service=None, type=ProductTypes.BUCKS, multicurrency_type=None)

    # 202 connect
    CONNECT = Product(id=508507, service=Services.CONNECT, type=ProductTypes.UNITS, multicurrency_type=None)
    CONNECT_508516 = Product(id=508516, service=Services.CONNECT, type=ProductTypes.UNITS, multicurrency_type=None)
    CONNECT_MONEY = Product(id=508529, service=Services.CONNECT, type=ProductTypes.MONEY, multicurrency_type=None)

    # 205 autobus
    # deprecated https://st.yandex-team.ru/BALANCE-35902#600a9005041095675b87aab5
    # AUTOBUS_SERVICES = Product(id=508665, service=Services.BUSES_2_0, type=ProductTypes.MONEY, multicurrency_type=None)
    # AUTOBUS_BELARUS_SERVICES = Product(id=509325, service=Services.BUSES_2_0, type=ProductTypes.MONEY,
    #                                    multicurrency_type=None)

    # KINOPOISK.PLUS = 600
    KINOPOISK_WITH_NDS = Product(id=509167, service=Services.KINOPOISK_PLUS, type=ProductTypes.MONEY,
                                 multicurrency_type=None)
    KINOPOISK_WO_NDS = Product(id=509168, service=Services.KINOPOISK_PLUS, type=ProductTypes.MONEY,
                               multicurrency_type=None)

    # CARSHARING (DRIVE) = 604
    CARSHARING_WITH_NDS_1 = Product(id=509177, service=Services.DRIVE, type=ProductTypes.MONEY,
                                    multicurrency_type=None)
    CARSHARING_WO_NDS = Product(id=50917701, service=Services.DRIVE, type=ProductTypes.MONEY,
                                multicurrency_type=None)  # TODO Поменять id после переналивки с прода select * from bo.t_partner_product where service_id = 604;
    CARSHARING_508845 = Product(id=508845, service=Services.DRIVE, type=ProductTypes.CLICKS,
                                multicurrency_type=None)

    # 610 blue market
    MARKET_BLUE_PAYMENTS = Product(id=508898, service=Services.BLUE_MARKET_PAYMENTS, type=ProductTypes.MONEY,
                                   multicurrency_type=None)
    MARKET_PURPLE_PAYMENTS_USD = Product(id=510820, service=Services.BLUE_MARKET_PAYMENTS, type=ProductTypes.MONEY,
                                         multicurrency_type=None)
    MARKET_PURPLE_PAYMENTS_EUR = Product(id=510824, service=Services.BLUE_MARKET_PAYMENTS, type=ProductTypes.MONEY,
                                         multicurrency_type=None)

    # 611 station alisa
    QUASAR = Product(id=509026, service=Services.QUASAR, type=ProductTypes.MONEY,
                     multicurrency_type=None)

    # 616 station alisa
    QUASAR_SRV = Product(id=509101, service=Services.QUASAR_SRV, type=ProductTypes.MONEY,
                         multicurrency_type=None)

    # 620 red market
    RED_MARKET = Product(id=509232, service=Services.RED_MARKET_BALANCE, type=ProductTypes.MONEY,
                         multicurrency_type=None)

    # 625 messenger
    MESSENGER = Product(id=509336, service=Services.MESSENGER, type=ProductTypes.MONEY,
                        multicurrency_type=None)
    # 171 UFS insurance
    UFS_INSURANCE_PAYMENTS = Product(id=509392, service=Services.UFS, type=ProductTypes.MONEY,
                                     multicurrency_type=None)

    OFFER_PLACEMENT_MONEY = Product(id=508926, service=Services.BLUE_MARKET, type=ProductTypes.MONEY,
                                    multicurrency_type=None)
    OFFER_PLACEMENT_BUCKS = Product(id=508942, service=Services.BLUE_MARKET, type=ProductTypes.BUCKS,
                                    multicurrency_type=None)
    OFFER_PLACEMENT_BUCKS_SHOPS = Product(id=508895, service=Services.BLUE_MARKET, type=ProductTypes.BUCKS,
                                          multicurrency_type=None)
    FULFILLMENT_PICKUP = Product(id=508930, service=Services.BLUE_MARKET, type=ProductTypes.MONEY,
                                 multicurrency_type=None)
    FULFILLMENT = Product(id=508943, service=Services.BLUE_MARKET, type=ProductTypes.BUCKS, multicurrency_type=None)
    FULFILLMENT_ONE_UNIT_MONEY = Product(id=508929, service=Services.BLUE_MARKET, type=ProductTypes.MONEY,
                                         multicurrency_type=None)
    FULFILLMENT_ONE_UNIT_BUCKS = Product(id=508944, service=Services.BLUE_MARKET, type=ProductTypes.BUCKS,
                                         multicurrency_type=None)
    PURPLE_MARKET_FULFILLMENT_USD = Product(id=510819, service=Services.BLUE_MARKET, type=ProductTypes.BUCKS,
                                            multicurrency_type=None)
    PURPLE_MARKET_FULFILLMENT_EUR = Product(id=510826, service=Services.BLUE_MARKET, type=ProductTypes.BUCKS,
                                            multicurrency_type=None)

    ZEN_OOO = Product(id=509735, service=Services.ZEN_SALES, type=ProductTypes.BUCKS, multicurrency_type=None)

    # 628
    FOOD_REST_SERVICES_RUB = Product(id=509365, service=Services.FOOD_SERVICES, type=ProductTypes.MONEY,
                                     multicurrency_type=None)
    FOOD_REST_SERVICES_PICKUP_RUB = Product(id=511260, service=Services.FOOD_SERVICES, type=ProductTypes.MONEY,
                                            multicurrency_type=None)
    FOOD_REST_SERVICES_BYN = Product(service=Services.FOOD_SERVICES, type=ProductTypes.MONEY, multicurrency_type=None,
                                     mdh_id='a3d874c8-a5ca-469f-b552-dc4237c9dd0a'
                                     )
    FOOD_REST_SERVICES_KZT = Product(id=510745, service=Services.FOOD_SERVICES, type=ProductTypes.MONEY,
                                     multicurrency_type=None)

    # 629
    FOOD_REST_PAYMENTS_RUB = Product(id=509367, service=Services.FOOD_PAYMENTS, type=ProductTypes.MONEY,
                                     multicurrency_type=None)
    FOOD_REST_PAYMENTS_BYN = Product(service=Services.FOOD_PAYMENTS, type=ProductTypes.MONEY, multicurrency_type=None,
                                     mdh_id='ad1f3429-01df-493a-b16d-0972294c5ec3'
                                     )
    FOOD_REST_PAYMENTS_KZT = Product(id=510747, service=Services.FOOD_PAYMENTS, type=ProductTypes.MONEY,
                                     multicurrency_type=None)
    FOOD_REST_PAYMENTS_RUB_CORP = Product(id=510856, service=Services.FOOD_PAYMENTS, type=ProductTypes.MONEY,
                                          multicurrency_type=None)

    # 645
    FOOD_COURIER_RUB = Product(id=509894, service=Services.FOOD_COURIER, type=ProductTypes.MONEY,
                               multicurrency_type=None)
    FOOD_COURIER_BYN = Product(service=Services.FOOD_COURIER, type=ProductTypes.MONEY, multicurrency_type=None,
                               mdh_id='0977cb18-b68e-4dbd-b197-d872fcd7d581'
                               )
    FOOD_COURIER_KZT = Product(id=510748, service=Services.FOOD_COURIER, type=ProductTypes.MONEY,
                               multicurrency_type=None)
    FOOD_COURIER_RUB_CORP = Product(id=510857, service=Services.FOOD_COURIER, type=ProductTypes.MONEY,
                                    multicurrency_type=None)

    # 661
    FOOD_SHOPS_SERVICES_RUB = Product(id=510851, service=Services.FOOD_SHOPS_SERVICES, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    FOOD_SHOPS_SERVICES_BYN = Product(id=510867, service=Services.FOOD_SHOPS_SERVICES, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    FOOD_SHOPS_SERVICES_KZT = Product(id=510866, service=Services.FOOD_SHOPS_SERVICES, type=ProductTypes.MONEY,
                                      multicurrency_type=None)

    # 662
    FOOD_SHOPS_PAYMENTS_RUB = Product(id=510868, service=Services.FOOD_SHOPS_PAYMENTS, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    FOOD_SHOPS_PAYMENTS_BYN = Product(id=510870, service=Services.FOOD_SHOPS_PAYMENTS, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    FOOD_SHOPS_PAYMENTS_KZT = Product(id=510869, service=Services.FOOD_SHOPS_PAYMENTS, type=ProductTypes.MONEY,
                                      multicurrency_type=None)

    # 663
    LAVKA_GOOD_ILS = Product(mdh_id='0a5daf48-4054-4714-9b81-ddc440de59f4',service=Services.LAVKA_COURIER, type=ProductTypes.MONEY,
                             multicurrency_type=None)
    LAVKA_DELIVERY_ILS = Product(mdh_id='30f93b86-02ac-4c8e-853e-432c020b41a4', service=Services.LAVKA_COURIER,
                                 type=ProductTypes.MONEY, multicurrency_type=None)
    LAVKA_TIPS_ILS = Product(mdh_id='989a2749-aa69-4487-b035-a05b850c86da', service=Services.LAVKA_COURIER,
                             type=ProductTypes.MONEY, multicurrency_type=None)
    LAVKA_GOOD_WO_NDS_ILS = Product(mdh_id='ab5e2d08-de98-4589-a9d0-9e8606115710', service=Services.LAVKA_COURIER,
                                    type=ProductTypes.MONEY, multicurrency_type=None)

    # 721
    REST_SITES_SERVICES_RUB = Product(id=511750, service=Services.REST_SITES_SERVICES, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    # 722
    REST_SITES_PAYMENTS_RUB = Product(id=511752, service=Services.REST_SITES_PAYMENTS, type=ProductTypes.MONEY,
                                      multicurrency_type=None)
    REST_SITES_PAYMENTS_RUB_CORP = Product(id=511754, service=Services.REST_SITES_PAYMENTS, type=ProductTypes.MONEY,
                                           multicurrency_type=None)

    # BugBounty 207
    BUG_BOUNTY = Product(id=20701, service=Services.BUG_BOUNTY, type=ProductTypes.MONEY, multicurrency_type=None)

    FOOD_CORP_RUB = Product(id=510858, service=Services.FOOD_CORP, type=ProductTypes.MONEY, multicurrency_type=None)
    FOOD_CORP_MIN_COST = Product(id=510859, service=Services.FOOD_CORP, type=ProductTypes.MONEY,
                                 multicurrency_type=None)

    # 671 Yandex B2B (aka Disk B2B)
    DISK_B2B_RUB = Product(id=511165, service=Services.DISK_B2B, type=ProductTypes.MONEY, multicurrency_type=None)

    # 672 Yandex Drive B2B
    DRIVE_B2B_RUB = Product(id=511217, service=Services.DRIVE_B2B, type=ProductTypes.MONEY, multicurrency_type=None)

    # 675
    FOOD_PHARMACY_SERVICES_RUB = Product(id=511286, service=Services.FOOD_PHARMACY_SERVICES, type=ProductTypes.MONEY,
                                         multicurrency_type=None)

    # 676
    EDA_HELP_PAYMENTS_RUB = Product(service=Services.EDA_HELP_PAYMENTS, type=ProductTypes.MONEY,
                                    multicurrency_type=None, mdh_id=u'8d59769a-d3c7-42cf-9d75-72a63718cefc')

    # 690 Yandex Mail PRO
    MAILPRO_RUB_WITH_NDS = Product(id=511374, service=Services.MAIL_PRO, type=ProductTypes.MONEY,
                                   multicurrency_type=None)
    MAILPRO_USD_WITHOUT_NDS = Product(id=512912, service=Services.MAIL_PRO, type=ProductTypes.MONEY,
                                   multicurrency_type=None)

    # 697 Corp taxi dispatching
    CORP_DISPATCHING_B2B_TRIPS_ACCESS_PAYMENT = Product(id=511462, service=Services.CORP_DISPATCHING,
                                                        type=ProductTypes.MONEY, multicurrency_type=None)

    # 1168 DELIVERY dispatching
    DELIVERY_DISPATCHING_B2B_TRIPS_ACCESS_PAYMENT = Product(service=Services.DELIVERY_DISPATCHING,
                                                            type=ProductTypes.MONEY, multicurrency_type=None,
                                                            mdh_id='18e3bdf5-e7da-4238-9dc4-3b01bd3b6323')

    # 699
    FOOD_PICKER_RUB = Product(id=511489, service=Services.FOOD_PICKER, type=ProductTypes.MONEY,
                              multicurrency_type=None)
    FOOD_PICKER_RUB_CORP = Product(id=511490, service=Services.FOOD_PICKER, type=ProductTypes.MONEY,
                                   multicurrency_type=None)

    # 706
    FOOD_PICKER_BUILD_ORDER_RUB = Product(id=511565, service=Services.FOOD_PICKER_BUILD_ORDER,
                                          type=ProductTypes.MONEY, multicurrency_type=None)
    FOOD_PICKER_BUILD_ORDER_RUB_CORP = Product(id=511567, service=Services.FOOD_PICKER_BUILD_ORDER,
                                               type=ProductTypes.MONEY, multicurrency_type=None)

    # 124 ZAXI DEPOSIT
    ZAXI_DEPOSIT_RUB = Product(service=Services.TAXI,
                               type=ProductTypes.MONEY, multicurrency_type=None,
                               mdh_id='01710a53-b146-40d2-8840-c745f151d6d7')
    ZAXI_DEPOSIT_KZT = Product(service=Services.TAXI,
                               type=ProductTypes.MONEY, multicurrency_type=None,
                               mdh_id='ff8f18e3-1a07-4536-a4e8-0578eb1e2c9a')
    ZAXI_DELIVERY_DEPOSIT_RUB = Product(service=Services.TAXI_DELIVERY_PAYMENTS,
                                        type=ProductTypes.MONEY, multicurrency_type=None,
                                        mdh_id='b6738bed-ece5-42c8-9fa6-676936e24c7d')

    K50_MAIN_RUB = Product(mdh_id='d25ff24c-fdd8-4449-aa03-64b5cbe91f29',
                           service=Services.K50, type=ProductTypes.MONEY, multicurrency_type=None)
    K50_GENERATOR_RUB = Product(id=512262, service=Services.K50, type=ProductTypes.MONEY, multicurrency_type=None)
    K50_OPTIMIZATOR_RUB = Product(id=512263, service=Services.K50, type=ProductTypes.MONEY, multicurrency_type=None)
    K50_TRACKER_RUB = Product(id=512264, service=Services.K50, type=ProductTypes.MONEY, multicurrency_type=None)
    K50_BI_RUB = Product(id=512265, service=Services.K50, type=ProductTypes.MONEY, multicurrency_type=None)
    SCOOTER_RENT_FARE_RUB = Product(service=Services.TAXI_SAMOKAT, type=ProductTypes.MONEY, multicurrency_type=None,
                                    mdh_id='a9db192d-b7db-4369-ae0e-2b752fda75d5')
    SCOOTER_PAYMENT_ILS = Product(service=Services.TAXI_SAMOKAT, type=ProductTypes.MONEY,
                                  multicurrency_type=None, mdh_id='e6914935-0528-45aa-9c25-0f9777f9d34c')
    SCOOTER_FINE_ILS = Product(service=Services.TAXI_SAMOKAT, type=ProductTypes.MONEY,
                               multicurrency_type=None, mdh_id='90f832fb-db62-489b-9ae0-a10504a6591c')

    # 1126
    MARKETING_PROMO_YANDEX_MARKET = Product(service=Services.BILLING_MARKETING_SERVICES, type=ProductTypes.MONEY,
                                            mdh_id=u'2623ec31-09fa-4fa9-a634-9479b4955405')
    # 1161
    TAXI_DELIVERY_CASH_ORDER_RUB = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='f5c131b4-06b1-41d5-a565-ea301d5e17b0')
    TAXI_DELIVERY_CASH_ORDER_ILS = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='ae3dd14b-b9f8-4f75-b26f-a198f2a14a9b')
    TAXI_DELIVERY_CASH_ORDER_KZT = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='c309e1bf-7698-49e8-a029-b3c21b4abb6c')
    TAXI_DELIVERY_CASH_ORDER_USD = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='0d13a498-7557-41e6-bb75-6bf152c5092c')

    TAXI_DELIVERY_CASH_ORDER_EUR = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='71fc97b5-270b-4181-a63f-5e3a33e15617')
    TAXI_DELIVERY_CASH_ORDER_BYN = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='aa935683-cd38-458f-90b2-903b4352cdcb')

    TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_RUB = Product(service=Services.TAXI_DELIVERY_CASH,
                                                      type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='1d66cdb6-6517-4aaf-96bf-7a7e9e3922c7')
    TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_ILS = Product(service=Services.TAXI_DELIVERY_CASH,
                                                      type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='b788e912-49ef-4843-b27d-62fd8de9abd3')
    TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_KZT = Product(service=Services.TAXI_DELIVERY_CASH,
                                                      type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='c309e1bf-7698-49e8-a029-b3c21b4abb6c')
    TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_USD = Product(service=Services.TAXI_DELIVERY_CASH,
                                                      type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='9fde605c-b4b9-40fe-a92f-82bdee3f38ac')
    TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_EUR = Product(service=Services.TAXI_DELIVERY_CASH,
                                                      type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='d53f8f12-df86-48b1-b433-5cd9a6348369')
    TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_BYN = Product(service=Services.TAXI_DELIVERY_CASH,
                                                      type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='5595e075-d979-410e-a1c5-6e1cb4ea494b')
    TAXI_DELIVERY_CASH_CARGO_ORDER_RUB = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                                 multicurrency_type=None,
                                                 mdh_id='f5c131b4-06b1-41d5-a565-ea301d5e17b0')
    TAXI_DELIVERY_CASH_DELIVERY_ORDER_ILS = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                                    multicurrency_type=None,
                                                    mdh_id='8e6defd3-ee78-4955-a709-7bed175e7d31')
    TAXI_DELIVERY_CASH_DELIVERY_ORDER_KZT = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                                    multicurrency_type=None,
                                                    mdh_id='0732896a-8c53-4d5c-bc8e-e630300b050b')
    TAXI_DELIVERY_CASH_DELIVERY_ORDER_USD = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                                    multicurrency_type=None,
                                                    mdh_id='7eeae1a5-26ee-41d0-b5c0-71b5d834bea6')
    TAXI_DELIVERY_CASH_DELIVERY_ORDER_EUR = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                                    multicurrency_type=None,
                                                    mdh_id='69208d7a-ffad-48b4-9f73-b22399884073')
    TAXI_DELIVERY_CASH_DELIVERY_ORDER_BYN = Product(service=Services.TAXI_DELIVERY_CASH, type=ProductTypes.MONEY,
                                                    multicurrency_type=None,
                                                    mdh_id='78af1e98-4caf-4d51-acb2-62f184d5925a')
    # 1163
    TAXI_DELIVERY_CARD_ORDER_RUB = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='820f31a1-d1b1-4426-88fb-8ca40a2fc4c4')
    TAXI_DELIVERY_CARD_ORDER_ILS = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='8349bd4b-12b0-4227-bf72-6bd016542232')
    TAXI_DELIVERY_CARD_ORDER_KZT = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='84b2b932-f232-4a57-b06d-c1c9f3cd327d')
    TAXI_DELIVERY_CARD_ORDER_USD = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='3e240e5c-023d-4c34-8496-c566a3843b00')
    TAXI_DELIVERY_CARD_ORDER_EUR = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='61c7e62c-94f5-4a79-915f-d309580d4f2e')
    TAXI_DELIVERY_CARD_ORDER_BYN = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                           multicurrency_type=None,
                                           mdh_id='5120480a-0591-4a2f-b8e6-4597f293671d')
    TAXI_DELIVERY_CARD_HIRING_WITH_CAR_RUB = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                     multicurrency_type=None,
                                                     mdh_id='82a0863c-4323-4855-b2ad-4f2b93486b7d')
    TAXI_DELIVERY_CARD_HIRING_WITH_CAR_ILS = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                     multicurrency_type=None,
                                                     mdh_id='3f978040-f35d-46c2-874a-90939199de7a')
    TAXI_DELIVERY_CARD_HIRING_WITH_CAR_KZT = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                     multicurrency_type=None,
                                                     mdh_id='9d1fac66-ca6d-40ea-a351-6d810afdb6bd')
    TAXI_DELIVERY_CARD_HIRING_WITH_CAR_USD = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                     multicurrency_type=None,
                                                     mdh_id='8e43af53-57a1-478c-9b9f-4adce9e3739c')
    TAXI_DELIVERY_CARD_HIRING_WITH_CAR_EUR = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                     multicurrency_type=None,
                                                     mdh_id='a620a3eb-0ed4-4a01-beac-685e401a5ddc')
    TAXI_DELIVERY_CARD_HIRING_WITH_CAR_BYN = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                     multicurrency_type=None,
                                                     mdh_id='b76289ac-e4ee-4511-a2bf-04a5f6626279')
    TAXI_DELIVERY_CARD_CARGO_ORDER_RUB = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                 multicurrency_type=None,
                                                 mdh_id='c58e20e2-0b33-477b-a5e3-0dafee25f826')
    TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_ILS = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='255a160d-b37b-488e-9f7f-655253f5351e')
    TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_KZT = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='a48eb822-a3ed-40e8-9a72-09e9d882cdcf')
    TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_USD = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='4b3b9f2e-3100-4fd9-8a30-ad2f4133ec0b')
    TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_EUR = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='fef51b04-80bc-4fb7-b3a3-d17a7cbd9448')
    TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_BYN = Product(service=Services.TAXI_DELIVERY_CARD, type=ProductTypes.MONEY,
                                                      multicurrency_type=None,
                                                      mdh_id='ed980294-b564-46fe-842d-0041fa3c3ac4')
    # 1162
    TAXI_DELIVERY_PAYMENT_MAIN_RUB = Product(service=Services.TAXI_DELIVERY_PAYMENTS, type=ProductTypes.MONEY,
                                              multicurrency_type=None,
                                              mdh_id='7383978e-854d-470a-935b-c9b40e18a5b8')
    TAXI_DELIVERY_PAYMENT_MAIN_ILS = Product(service=Services.TAXI_DELIVERY_PAYMENTS, type=ProductTypes.MONEY,
                                              multicurrency_type=None,
                                              mdh_id='b3aac700-0a6f-41f7-911d-a335fdc8cbf0')
    TAXI_DELIVERY_PAYMENT_MAIN_KZT = Product(service=Services.TAXI_DELIVERY_PAYMENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None,
                                             mdh_id='55c0fdeb-242c-42b6-a8dc-769c0f8c8be0')
    TAXI_DELIVERY_PAYMENT_MAIN_USD = Product(service=Services.TAXI_DELIVERY_PAYMENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None,
                                             mdh_id='9ae474e6-2d1a-45af-9cc7-ae4caea27863')
    TAXI_DELIVERY_PAYMENT_MAIN_EUR = Product(service=Services.TAXI_DELIVERY_PAYMENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None,
                                             mdh_id='a233e68b-45e9-4f4c-8ad9-9328fcca3a10')
    TAXI_DELIVERY_PAYMENT_MAIN_BYN = Product(service=Services.TAXI_DELIVERY_PAYMENTS, type=ProductTypes.MONEY,
                                             multicurrency_type=None,
                                             mdh_id='53eabc34-3767-4b89-b5e0-a91c3c0a9ff8')

    BLUE_MARKET_GLOBAL_FEE_ILS = Product(service=Services.BLUE_MARKET, type=ProductTypes.MONEY,
                                         mdh_id='c79b1feb-fc22-4152-8c95-acf8948801aa')
    BLUE_MARKET_GLOBAL_DELIVERY_ILS = Product(service=Services.BLUE_MARKET, type=ProductTypes.MONEY,
                                              mdh_id='0ec15b0b-3b74-4cee-8896-9e6f578279cc')
    BLUE_MARKET_GLOBAL_AGENCY_COMMISSION_ILS = Product(service=Services.BLUE_MARKET, type=ProductTypes.MONEY,
                                                       mdh_id='035b78d3-df91-45c5-9fcc-2893671f09ed')

    # 1176
    FOOD_MERCURY_PAYMENTS_RUB = Product(service=Services.FOOD_MERCURY_PAYMENTS, type=ProductTypes.MONEY,
                                        multicurrency_type=None, mdh_id='29d25109-43b8-45b8-a5cd-f847ee033d01')
    # 1177
    FOOD_MERCURY_SERVICES_RUB = Product(service=Services.FOOD_MERCURY_SERVICES, type=ProductTypes.MONEY,
                                        multicurrency_type=None, mdh_id='052c8867-cde8-44e9-b0cc-ccb5f91da0bb')
    MAGISTRALS_SENDER_MAIN_RUB = Product(service=Services.MAGISTRALS_SENDER_COMMISSION, type=ProductTypes.MONEY,
                                         multicurrency_type=None, mdh_id='60206d94-74ba-445c-9706-8c0b07b5341a')
    MAGISTRALS_CARRIER_MAIN_RUB = Product(service=Services.MAGISTRALS_CARRIER_COMMISSION, type=ProductTypes.MONEY,
                                          multicurrency_type=None, mdh_id='ee295d94-5866-451f-9518-1db7244b1c1d')

    # 1181
    CORP_TAXI_USN_GENERAL_RUB = Product(service=Services.TAXI_CORP_CLIENTS_USN_GENERAL, type=ProductTypes.MONEY,
                                        multicurrency_type=None, mdh_id='0391daab-cd0e-4745-ae0b-c0b544347218')
    # 1183
    CORP_TAXI_USN_AGENT_RUB = Product(service=Services.TAXI_CORP_CLIENTS_USN_AGENT, type=ProductTypes.MONEY,
                                      multicurrency_type=None, mdh_id='5710733c-0b08-4f64-a381-cffe99779000')


    # 1898
    DELIVERY_CLIENT_B2B_LOGISTICS_PAYMENT = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                                    multicurrency_type=None,
                                                    mdh_id='05201a3c-e80d-4802-b386-5241be4a85c8')
    CARGO_CLIENT_B2B_LOGISTICS_PAYMENT = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                                 multicurrency_type=None, mdh_id='d90fccf9-a700-48d8-8977-922b0bdd8a71')
    B2B_AGENT_LOGISTICS_REVENUE = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                          multicurrency_type=None, mdh_id='a417723a-5d28-4e63-a3b1-f40b9273206c')
    USER_ON_DELIVERY_PAYMENT_FEE = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                           multicurrency_type=None, mdh_id='994e7c77-1d0b-405a-8bda-89a303e3c6f1')

    # 1889
    DELIVERY_CLIENT_B2B_LOGISTICS_PAYMENT_CLP = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                                        multicurrency_type=None, mdh_id='04939597-7e04-43fd-852d-78165cf9ecf8')
    B2B_AGENT_LOGISTICS_REVENUE_CLP = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                              multicurrency_type=None, mdh_id='3960dcab-e3d1-48d7-bd6f-ad7f66e0b990')
    USER_ON_DELIVERY_PAYMENT_FEE_CLP = Product(service=Services.LOGISTICS_CLIENTS, type=ProductTypes.MONEY,
                                               multicurrency_type=None, mdh_id='904fb770-3ad1-42a2-a143-caf6fe55aad2')

    # 1207
    DOUBLE_CLOUD_US_NDS_0 = Product(service=Services.DOUBLE_CLOUD_GENERAL, type=ProductTypes.MONEY,
                                    multicurrency_type=None, mdh_id='bed72d28-8b73-4c2b-b512-4a70ae20b358')

    DOUBLE_CLOUD_DE_NDS_20 = Product(service=Services.DOUBLE_CLOUD_GENERAL, type=ProductTypes.MONEY,
                                     multicurrency_type=None, mdh_id='0c95c773-b750-4485-bc65-22dde1357b1b')

    DOUBLE_CLOUD_DE_NDS_0 = Product(service=Services.DOUBLE_CLOUD_GENERAL, type=ProductTypes.MONEY,
                                    multicurrency_type=None, mdh_id='96ff06db-c0f5-43cf-8846-0e51a3a02b4d')


class Currencies(utils.ConstantsContainer):
    Currency = namedtuple('Currency', 'iso_code, char_code, num_code, iso_num_code, central_bank_code')
    # @attr.s
    # class Currency(object):
    #     iso_code = attr.ib(default=None)
    #     char_code = attr.ib(default=None)
    #     num_code = attr.ib(default=None)
    #     iso_num_code = attr.ib(default=None)
    #     central_bank_code = attr.ib(default=None)

    constant_type = Currency

    RUB = Currency(iso_code='RUB', char_code='RUR', num_code=810, iso_num_code=643, central_bank_code=1000)
    USD = Currency(iso_code='USD', char_code='USD', num_code=840, iso_num_code=840, central_bank_code=None)
    EUR = Currency(iso_code='EUR', char_code='EUR', num_code=978, iso_num_code=978, central_bank_code=1001)
    CHF = Currency(iso_code='CHF', char_code='CHF', num_code=756, iso_num_code=756, central_bank_code=1001)
    BYN = Currency(iso_code='BYN', char_code='BYN', num_code=933, iso_num_code=933, central_bank_code=None)
    GBP = Currency(iso_code='GBP', char_code='GBP', num_code=826, iso_num_code=826, central_bank_code=None)

    # TODO: uah AND UAH - не всё так просто
    UAH = Currency(iso_code='UAH', char_code='UAH', num_code=980, iso_num_code=980, central_bank_code=1003)
    uah = Currency(iso_code='UAH', char_code='uah', num_code=980, iso_num_code=980, central_bank_code=1003)

    # TODO: try AND TRY - не всё так просто
    TRY = Currency(iso_code='TRY', char_code='TRY', num_code=949, iso_num_code=949, central_bank_code=1002)
    try_ = Currency(iso_code='TRY', char_code='try', num_code=949, iso_num_code=949, central_bank_code=1002)

    # TODO: kzt AND KZT - не всё так просто
    KZT = Currency(iso_code='KZT', char_code='KZT', num_code=398, iso_num_code=398, central_bank_code=None)
    kzt = Currency(iso_code='KZT', char_code='kzt', num_code=398, iso_num_code=398, central_bank_code=None)

    AED = Currency(iso_code='AED', char_code='AED', num_code=-1, iso_num_code=784, central_bank_code=None)
    AMD = Currency(iso_code='AMD', char_code='AMD', num_code=51, iso_num_code=51, central_bank_code=1005)
    AUD = Currency(iso_code='AUD', char_code='AUD', num_code=-1, iso_num_code=-1, central_bank_code=None)
    AZN = Currency(iso_code='AZN', char_code='AZN', num_code=944, iso_num_code=944, central_bank_code=1015)
    BGN = Currency(iso_code='BGN', char_code='BGN', num_code=-1, iso_num_code=-1, central_bank_code=None)
    BRL = Currency(iso_code='BRL', char_code='BRL', num_code=-1, iso_num_code=-1, central_bank_code=None)
    BYR = Currency(iso_code='BYR', char_code='BYR', num_code=-1, iso_num_code=-1, central_bank_code=None)
    CAD = Currency(iso_code='CAD', char_code='CAD', num_code=-1, iso_num_code=-1, central_bank_code=None)
    CNY = Currency(iso_code='CNY', char_code='CNY', num_code=-1, iso_num_code=-1, central_bank_code=None)
    CZK = Currency(iso_code='CZK', char_code='CZK', num_code=-1, iso_num_code=-1, central_bank_code=None)
    DKK = Currency(iso_code='DKK', char_code='DKK', num_code=-1, iso_num_code=-1, central_bank_code=None)
    ETB = Currency(iso_code='ETB', char_code='ETB', num_code=-1, iso_num_code=-1, central_bank_code=None)
    GEL = Currency(iso_code='GEL', char_code='GEL', num_code=981, iso_num_code=981, central_bank_code=None)
    GHS = Currency(iso_code='GHS', char_code='GHS', num_code=936, iso_num_code=936, central_bank_code=None)
    IDR = Currency(iso_code='IDR', char_code='IDR', num_code=-1, iso_num_code=-1, central_bank_code=None)
    ILS = Currency(iso_code='ILS', char_code='ILS', num_code=376, iso_num_code=376, central_bank_code=None)
    INR = Currency(iso_code='INR', char_code='INR', num_code=-1, iso_num_code=-1, central_bank_code=None)
    JPY = Currency(iso_code='JPY', char_code='JPY', num_code=-1, iso_num_code=-1, central_bank_code=None)
    HUF = Currency(iso_code='HUF', char_code='HUF', num_code=-1, iso_num_code=-1, central_bank_code=None)
    HKD = Currency(iso_code='HKD', char_code='HKD', num_code=-1, iso_num_code=-1, central_bank_code=None)
    HRK = Currency(iso_code='HRK', char_code='HRK', num_code=-1, iso_num_code=-1, central_bank_code=None)
    KES = Currency(iso_code='KES', char_code='KES', num_code=-1, iso_num_code=-1, central_bank_code=None)
    KGS = Currency(iso_code='KGS', char_code='KGS', num_code=417, iso_num_code=417, central_bank_code=1010)
    KRW = Currency(iso_code='KRW', char_code='KRW', num_code=-1, iso_num_code=-1, central_bank_code=None)
    MDL = Currency(iso_code='MDL', char_code='MDL', num_code=498, iso_num_code=498, central_bank_code=1009)
    MNT = Currency(iso_code='MNT', char_code='MNT', num_code=-1, iso_num_code=-1, central_bank_code=None)
    MXN = Currency(iso_code='MXN', char_code='MXN', num_code=-1, iso_num_code=-1, central_bank_code=None)
    MYR = Currency(iso_code='MYR', char_code='MYR', num_code=-1, iso_num_code=-1, central_bank_code=None)
    NOK = Currency(iso_code='NOK', char_code='NOK', num_code=578, iso_num_code=578, central_bank_code=None)
    NGN = Currency(iso_code='NGN', char_code='NGN', num_code=-1, iso_num_code=-1, central_bank_code=None)
    NZD = Currency(iso_code='NZD', char_code='NZD', num_code=-1, iso_num_code=-1, central_bank_code=None)
    PEN = Currency(iso_code='PEN', char_code='PEN', num_code=604, iso_num_code=604, central_bank_code=None)
    PHP = Currency(iso_code='PHP', char_code='PHP', num_code=-1, iso_num_code=-1, central_bank_code=None)
    PLN = Currency(iso_code='PLN', char_code='PLN', num_code=-1, iso_num_code=-1, central_bank_code=None)
    RON = Currency(iso_code='RON', char_code='RON', num_code=946, iso_num_code=946, central_bank_code=1024)
    RSD = Currency(iso_code='RSD', char_code='RSD', num_code=941, iso_num_code=941, central_bank_code=1012)
    SAR = Currency(iso_code='SAR', char_code='SAR', num_code=-1, iso_num_code=-1, central_bank_code=None)
    SEK = Currency(iso_code='SEK', char_code='SEK', num_code=752, iso_num_code=752, central_bank_code=None)
    SGD = Currency(iso_code='SGD', char_code='SGD', num_code=-1, iso_num_code=-1, central_bank_code=None)
    THB = Currency(iso_code='THB', char_code='THB', num_code=-1, iso_num_code=-1, central_bank_code=None)
    TJS = Currency(iso_code='TJS', char_code='TJS', num_code=-1, iso_num_code=-1, central_bank_code=None)
    TMT = Currency(iso_code='TMT', char_code='TMT', num_code=-1, iso_num_code=-1, central_bank_code=None)
    TWD = Currency(iso_code='TWD', char_code='TWD', num_code=-1, iso_num_code=-1, central_bank_code=None)
    UZS = Currency(iso_code='UZS', char_code='UZS', num_code=860, iso_num_code=860, central_bank_code=1100)
    XAF = Currency(iso_code='XAF', char_code='XAF', num_code=950, iso_num_code=950, central_bank_code=1100)
    XDR = Currency(iso_code='XDR', char_code='XDR', num_code=-1, iso_num_code=-1, central_bank_code=None)
    XOF = Currency(iso_code='XOF', char_code='XOF', num_code=-1, iso_num_code=-1, central_bank_code=None)
    ZAR = Currency(iso_code='ZAR', char_code='ZAR', num_code=710, iso_num_code=710, central_bank_code=None)
    AOA = Currency(iso_code='AOA', char_code='AOA', num_code=973, iso_num_code=973, central_bank_code=1100)
    ZMW = Currency(iso_code='ZMW', char_code='ZMW', num_code=967, iso_num_code=967, central_bank_code=None)
    BOB = Currency(iso_code='BOB', char_code='BOB', num_code=68, iso_num_code=68, central_bank_code=None)
    CDF = Currency(iso_code='CDF', char_code='CDF', num_code=976, iso_num_code=976, central_bank_code=None)
    DZD = Currency(iso_code='DZD', char_code='DZD', num_code=4217, iso_num_code=4217, central_bank_code=None)
    CLP = Currency(iso_code='CLP', char_code='CLP', num_code=152, iso_num_code=152, central_bank_code=None)


CURRENCY_NUM_CODE_ISO_MAP = {c.num_code: c.iso_code for c in Currencies.__dict__.values() if isinstance(c, tuple)}



class Paysys(object):
    def __init__(self, id, currency, firm, instant=0, name=u''):
        self.id = id
        self.currency = currency
        self.firm = firm
        self.name = name
        self.instant = instant

    def with_firm(self, firm):
        new_id = firm.id * 100000 + self.id
        return Paysys(id=new_id, currency=self.currency, firm=firm, name=self.name, instant=self.instant)

    # базовый id способа оплаты (без фирмы бизнес-юнита в id): 1201003 -> 1003
    @property
    def base_id(self):
        return self.id % 100000

    def __repr__(self):
        return "(id={id}, firm={firm_id}, {currency})".format(id=self.id, firm_id=self.firm.id,
                                                              currency=self.currency.iso_code)


class Paysyses(utils.ConstantsContainer):
    constant_type = Paysys

    # todo-igogor имена вроде неверные

    # firm 1
    YM_PH_RUB = Paysys(id=1000, currency=Currencies.RUB, firm=Firms.YANDEX_1, name=u'Яндекс деньги', instant=1)
    BANK_PH_RUB = Paysys(id=1001, currency=Currencies.RUB, firm=Firms.YANDEX_1, name=u'Банк для физических лиц')
    CC_PH_RUB = Paysys(id=1002, currency=Currencies.RUB, firm=Firms.YANDEX_1, name=u'Кредитной картой', instant=1)
    BANK_UR_RUB = Paysys(id=1003, currency=Currencies.RUB, firm=Firms.YANDEX_1, name=u'Банк для юридических лиц')
    BANK_YT_USD = Paysys(id=1013, currency=Currencies.USD, firm=Firms.YANDEX_1, name=u"Банк для нерезидентов (доллары)")
    BANK_YT_RUB = Paysys(id=1014, currency=Currencies.RUB, firm=Firms.YANDEX_1, name=u"Банк для нерезидентов (рубли)")
    BANK_YT_EUR = Paysys(id=1023, currency=Currencies.EUR, firm=Firms.YANDEX_1, name=u"Банк для нерезидентов (евро)")
    BANK_YT_RUB_AGENCY = Paysys(id=1025, currency=Currencies.RUB, firm=Firms.YANDEX_1, name=u"Банк для нерезидента, через агентство (рубли)")
    BANK_YT_USD_AGENCY = Paysys(id=1026, currency=Currencies.USD, firm=Firms.YANDEX_1, name=u"Банк для нерезидента, через агентство (доллары)")
    BANK_PH_USD = Paysys(id=1029, currency=Currencies.USD, firm=Firms.KINOPOISK_9,
                         name=u'Банк для физических лиц (США), доллары США')
    CC_UR_RUB = Paysys(id=1033, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                       name=u'Корпоративная банковская карта юридического лица')

    BANK_YTUR_KZT = Paysys(id=1060, currency=Currencies.KZT, firm=Firms.YANDEX_1,
                           name=u"Банк для юридических лиц без НДС, Россия (тенге)")
    BANK_YTPH_KZT = Paysys(id=1061, currency=Currencies.KZT, firm=Firms.YANDEX_1,
                           name=u"Банк для физических лиц без НДС, Россия (тенге)")
    BANK_YT_RUB_WITH_NDS = Paysys(id=11069, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                                  name=u"Банк для нерезидентов (рубли, НДС облагается)")
    WEBMONEY_WMR = Paysys(id=1052, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                          name=u"WebMoney (WMR)")

    BANK_YT_BYN = Paysys(id=1100, currency=Currencies.BYN, firm=Firms.YANDEX_1,
                         name=u"Банк для нерезидентов (бел. рубли)")
    PAYPAL_PH_RUB = Paysys(id=1103, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                           name=u"Paypal для физ.лиц")
    BANK_UR_RUB_WO_NDS = Paysys(id=1117, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                                name=u"Банк для юридических лиц без НДС")
    # a-vasin: пока это заглушка
    BANK_PH_RUB_WO_NDS = Paysys(id=1128, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                                name=u"Банк для физических лиц без НДС")

    # firm 2
    BANK_UA_UR_UAH = Paysys(id=1017, currency=Currencies.UAH, firm=Firms.YANDEX_UA_2,
                            name=u'Банк для юридических лиц, Украина (гривны)')
    BANK_UA_PH_UAH = Paysys(id=1018, currency=Currencies.UAH, firm=Firms.YANDEX_UA_2,
                            name=u'Банк для физических лиц, Украина (гривны)')
    CC_PU_UAH = Paysys(id=1031, currency=Currencies.UAH, firm=Firms.YANDEX_UA_2,
                       name=u'Кредитной картой (Украина)')
    WEBMONEY_WMU = Paysys(id=1022, currency=Currencies.RUB, firm=Firms.YANDEX_1,
                          name=u"WebMoney Украина (WMU)")

    # firm 4
    BANK_US_UR_USD = Paysys(id=1028, currency=Currencies.USD, firm=Firms.YANDEX_INC_4,
                            name=u'Банк для юридических лиц (США), доллары США')
    BANK_US_PH_USD = Paysys(id=1029, currency=Currencies.USD, firm=Firms.YANDEX_INC_4,
                            name=u'Банк для физических лиц (США), доллары США')
    BANK_US_YT_UR_USD = Paysys(
        id=11201,
        currency=Currencies.USD,
        firm=Firms.YANDEX_INC_4,
        name=u'Банк для юр.лиц, USD, нерезидент, США'
    )
    BANK_US_YT_PH_USD = Paysys(
        id=11203,
        currency=Currencies.USD,
        firm=Firms.YANDEX_INC_4,
        name=u'Банк для физ.лиц, USD, нерезидент, США'
    )

    CC_US_YT_UR_USD = Paysys(
        id=11200,
        currency=Currencies.USD,
        firm=Firms.YANDEX_INC_4,
        name=u'Банковская карта для юр.лиц, USD, нерезидент, США',
        instant=1,
    )
    CC_US_YT_PH_USD = Paysys(
        id=11202,
        currency=Currencies.USD,
        firm=Firms.YANDEX_INC_4,
        name=u'Банковская карта для физ.лиц, USD, нерезидент, США',
        instant=1,
    )
    PAYPAL_USP_USD = Paysys(id=1064, currency=Currencies.USD, firm=Firms.YANDEX_INC_4,
                            name=u'Paypal для физ.лиц')
    PAYPAL_USU_USD = Paysys(id=1065, currency=Currencies.USD, firm=Firms.YANDEX_INC_4,
                            name=u'Paypal для юр.лиц')

    # firm 5
    BANK_BY_PH_BYN_BELFACTA = Paysys(id=1102, currency=Currencies.BYN, firm=Firms.BELFACTA_5,
                                     name=u'Банк для физических лиц(Белоруссия)')

    # firm 7
    BANK_SW_UR_EUR = Paysys(id=1043, currency=Currencies.EUR, firm=Firms.EUROPE_AG_7)
    BANK_SW_UR_USD = Paysys(id=1044, currency=Currencies.USD, firm=Firms.EUROPE_AG_7)
    BANK_SW_UR_CHF = Paysys(id=1045, currency=Currencies.CHF, firm=Firms.EUROPE_AG_7)
    BANK_SW_UR_GBP = Paysys(id=1152, currency=Currencies.GBP, firm=Firms.EUROPE_AG_7)

    BANK_SW_YT_EUR = Paysys(id=1046, currency=Currencies.EUR, firm=Firms.EUROPE_AG_7)
    BANK_SW_YT_USD = Paysys(id=1047, currency=Currencies.USD, firm=Firms.EUROPE_AG_7)
    BANK_SW_YT_CHF = Paysys(id=1048, currency=Currencies.CHF, firm=Firms.EUROPE_AG_7)
    BANK_SW_YT_TRY = Paysys(id=11153, currency=Currencies.TRY, firm=Firms.EUROPE_AG_7)

    BANK_SW_PH_EUR = Paysys(id=1066, currency=Currencies.EUR, firm=Firms.EUROPE_AG_7)

    BANK_SW_YTPH_EUR = Paysys(id=1069, currency=Currencies.EUR, firm=Firms.EUROPE_AG_7)
    BANK_SW_YTPH_TRY = Paysys(id=11154, currency=Currencies.TRY, firm=Firms.EUROPE_AG_7)
    CC_SW_YTPH_USD = Paysys(id=1076, currency=Currencies.USD, firm=Firms.EUROPE_AG_7)
    CC_BY_YTPH_RUB = Paysys(id=1075, currency=Currencies.RUB, firm=Firms.EUROPE_AG_7,
                            name=u'Банковская карта для нерезидентов - рубли', instant=1)
    CC_WSTORE_INAPP = Paysys(id=1078, currency=Currencies.USD, firm=Firms.YANDEX_1,
                             name=u'Windows Phone Store InApp Purchase в долларах', instant=1)
    CC_SW_PH_CHF = Paysys(id=1082, currency=Currencies.CHF, firm=Firms.EUROPE_AG_7)
    CC_SW_UR_CHF = Paysys(id=1085, currency=Currencies.CHF, firm=Firms.EUROPE_AG_7)
    CC_SW_YT_USD = Paysys(id=1086, currency=Currencies.USD, firm=Firms.EUROPE_AG_7)
    CC_SW_YT_CHF = Paysys(id=1088, currency=Currencies.CHF, firm=Firms.EUROPE_AG_7)
    CC_SW_YT_GBP = Paysys(id=1160, currency=Currencies.GBP, firm=Firms.EUROPE_AG_7, instant=1)
    CC_SW_YT_TRY = Paysys(id=11155, currency=Currencies.TRY, firm=Firms.EUROPE_AG_7)

    CC_SW_YTPH_TRY = Paysys(id=11156, currency=Currencies.TRY, firm=Firms.EUROPE_AG_7)

    CC_YT_UZS = Paysys(id=1133, currency=Currencies.UZS, firm=Firms.EUROPE_AG_7, instant=1)
    YM_BY_YTPH_RUB = Paysys(id=11152, currency=Currencies.RUB, firm=Firms.EUROPE_AG_7, instant=1)

    SW_PH_CHF = Paysys(id=1068, currency=Currencies.CHF, firm=Firms.EUROPE_AG_7)

    # firm 8
    BANK_TR_UR_TRY = Paysys(id=1050, currency=Currencies.TRY, firm=Firms.YANDEX_TURKEY_8,
                            name=u'Банк для юридических лиц, Турция (лиры)')
    BANK_TR_PH_TRY = Paysys(id=1051, currency=Currencies.TRY, firm=Firms.YANDEX_TURKEY_8,
                            name=u'Банк для физических лиц, Турция (лиры)')
    CC_TRU_TRY = Paysys(id=1055, currency=Currencies.TRY, firm=Firms.YANDEX_TURKEY_8,
                        name=u'Кредитная карта для юр. лиц (Турция)')
    CC_TRP_TRY = Paysys(id=1056, currency=Currencies.TRY, firm=Firms.YANDEX_TURKEY_8,
                        name=u'Кредитная карта для физ. лиц (Турция)')

    # firm 9
    BANK_PH_RUB_KINOPOISK = Paysys(id=901001, currency=Currencies.RUB, firm=Firms.KINOPOISK_9,
                                   name=u'Банк для физических лиц', instant=0)
    BANK_PH_RUB_WO_NDS_KINOPOISK = Paysys(id=901128, currency=Currencies.RUB, firm=Firms.KINOPOISK_9,
                                          name=u'Банк для физических лиц без НДС', instant=0)
    BANK_UR_RUB_KINOPOISK = Paysys(id=901003, currency=Currencies.RUB, firm=Firms.KINOPOISK_9,
                                   name=u'Банк для юридических лиц')

    # firm 12
    BANK_PH_RUB_VERTICAL = Paysys(id=1201001, currency=Currencies.RUB, firm=Firms.VERTICAL_12,
                                  name=u'Банк для физических лиц')

    BANK_UR_RUB_VERTICAL = Paysys(id=1201003, currency=Currencies.RUB, firm=Firms.VERTICAL_12,
                                  name=u'Банк для юридических лиц')

    CC_RUB_UR_TRUST_AUTORU = Paysys(id=1206430011019291, currency=Currencies.RUB, firm=Firms.VERTICAL_12,
                                    name=u'Банк для юридических лиц')

    CC_RUB_PH_TRUST_AUTORU = Paysys(id=1206430011015052, currency=Currencies.RUB, firm=Firms.VERTICAL_12,
                                    name=u'Банк для юридических лиц')

    # firm 13
    BANK_PH_RUB_TAXI = Paysys(id=1301001, currency=Currencies.RUB, firm=Firms.TAXI_13,
                              name=u'Банк для юридичеких лиц')
    BANK_UR_RUB_TAXI = Paysys(id=1301003, currency=Currencies.RUB, firm=Firms.TAXI_13,
                              name=u'Банк для юридичеких лиц')
    CARD_UR_RUB_TAXI = Paysys(id=1301033, currency=Currencies.RUB, firm=Firms.TAXI_13,
                              name=u'Кредитная карта (Юр. Лица)')
    CE_TAXI = Paysys(id=1301006, currency=Currencies.RUB, firm=Firms.TAXI_13,
                     name=u'Сертификат')
    CE_TAXI_DELIVERY = Paysys(id=13001006, currency=Currencies.RUB, firm=Firms.LOGISTICS_130,
                              name=u'Сертификат')
    # todo пейсисы такого типа лучше сюда не прописывать, а создавать непосредственно там где они нужны
    # firm 14
    BANK_SW_UR_EUR_AUTORU_AG = Paysys(id=1401043, currency=Currencies.EUR, firm=Firms.AUTO_RU_AG_14,
                                      name=u'Банк для юридических лиц, евро (резиденты, Швейцария)')
    # BANK_SW_UR_USD_AUTORU_AG = Paysys(id=1401047, currency=Currencies.USD, firm=Firms.AUTO_RU_AG_14,
    #                                   name=u'Банк для юридических лиц, доллары (нерезиденты, Швейцария)')
    # BANK_SW_UR_CHF_AUTORU_AG = BANK_SW_UR_CHF.with_firm(Firms.AUTO_RU_AG_14)

    BANK_SW_YT_EUR_AUTORU_AG = Paysys(id=1401046, currency=Currencies.EUR, firm=Firms.AUTO_RU_AG_14,
                                      name=u'Банк для юридических лиц, евро (нерезиденты, Швейцария)')
    BANK_SW_YT_USD_AUTORU_AG = Paysys(id=1401047, currency=Currencies.USD, firm=Firms.AUTO_RU_AG_14,
                                      name=u'Банк для юридических лиц, доллары (нерезиденты, Швейцария)')
    BANK_SW_YT_CHF_AUTORU_AG = Paysys(id=1401048, currency=Currencies.CHF, firm=Firms.AUTO_RU_AG_14,
                                      name=u'Банк для юридических лиц, франки (нерезиденты, Швейцария)')

    # firm 16
    PAYPAL_SW_YTPH_USD = Paysys(id=1099, currency=Currencies.USD, firm=Firms.SERVICES_AG_16,
                                name=u'Paypal для физ.лиц')
    BANK_SW_YTPH_USD = Paysys(id=1601070, currency=Currencies.USD, firm=Firms.SERVICES_AG_16,
                              name=u'Банк для физ.лиц, USD, нерезидент, Швейцария')

    BANK_SW_UR_EUR_SAG = Paysys(id=1601043, currency=Currencies.EUR, firm=Firms.SERVICES_AG_16,
                                name=u'Банк для юридических лиц, евро (Швейцария)')

    BANK_SW_YT_EUR_SAG = Paysys(id=1601046, currency=Currencies.EUR, firm=Firms.SERVICES_AG_16,
                                name=u'Банк для юридических лиц, евро (нерезиденты, Швейцария)')
    BANK_SW_YT_USD_SAG = Paysys(id=1601047, currency=Currencies.USD, firm=Firms.SERVICES_AG_16,
                                name=u'Банк для юридических лиц, доллары (нерезиденты, Швейцария)')

    # firm 18
    PROMOCODE_OFD = Paysys(id=1127, currency=Currencies.RUB, firm=Firms.OFD_18, name=u'Промокод ОФД')
    BANK_UR_RUB_OFD = Paysys(id=1801003, currency=Currencies.RUB, firm=Firms.OFD_18, name=u'Банк для юридических лиц',
                             )

    # firm 22
    BANK_UR_USD_TAXI_BV = Paysys(id=2201041, currency=Currencies.USD, firm=Firms.TAXI_BV_22,
                                 name=u'Банк для юридических лиц доллары (нерезиденты, Голландия)')
    BANK_UR_EUR_TAXI_BV = Paysys(id=2201039, currency=Currencies.EUR, firm=Firms.TAXI_BV_22,
                                 name=u'Банк для юридических лиц евро (нерезиденты, Голландия)')
    BANK_UR_NOK_TAXI_BV = Paysys(id=1212, currency=Currencies.NOK, firm=Firms.TAXI_BV_22,
                                 name=u'Банк для юридических лиц норвежские кроны (нерезиденты, Голландия)')
    BANK_UR_BYN_TAXI_BV = Paysys(id=2201145, currency=Currencies.BYN, firm=Firms.TAXI_BV_22,
                                 name=u'Банк для юридических лиц бел. рубли (нерезиденты, Голландия)')

    # firm 24
    BANK_KZ_UR = Paysys(id=2401020, currency=Currencies.KZT, firm=Firms.TAXI_KAZ_24,
                        name=u'Банк для юридических лиц, Казахстан (тенге)')
    BANK_KZ_UR_WO_NDS = Paysys(id=1131, currency=Currencies.KZT, firm=Firms.TAXI_KAZ_24,
                               name=u'Банк для юридических лиц, Казахстан (тенге)')

    # firm 25
    CC_KZ_UR_TG = Paysys(id=1120, currency=Currencies.KZT, firm=Firms.KZ_25,
                         name=u'Кредитная карта для юр. лиц (Казахстан)')

    CC_KZ_PH_TG = Paysys(id=1121, currency=Currencies.KZT, firm=Firms.KZ_25,
                         name=u'Кредитная карта для физ. лиц (Казахстан)', instant=1)

    BANK_KZ_UR_TG = Paysys(id=2501020, currency=Currencies.KZT, firm=Firms.KZ_25,
                           name=u'Банк для юр лиц, тенге')
    BANK_KZ_PH_TG = Paysys(id=2501021, currency=Currencies.KZT, firm=Firms.KZ_25,
                           name=u'Банк для физ лиц, тенге')
    YM_KZ_PH_TG = Paysys(id=11154, currency=Currencies.KZT, firm=Firms.KZ_25, instant=1)

    # firm 26
    BANK_ARM_UR = Paysys(id=1122, currency=Currencies.AMD, firm=Firms.TAXI_AM_26,
                         name=u'Банк для юр. лиц (Армения)')

    # firm 27
    CC_BY_UR_BYN = Paysys(id=1125, currency=Currencies.BYN, firm=Firms.REKLAMA_BEL_27,
                          name=u'Банковская карта (для юр. лиц, Белоруссия)', instant=1)
    CC_BY_PH_BYN = Paysys(id=1126, currency=Currencies.BYN, firm=Firms.REKLAMA_BEL_27,
                          name=u'Банковская карта (для физ. лиц, Белоруссия)', instant=1)

    BANK_BY_UR_BYN = Paysys(id=2701101, currency=Currencies.BYN, firm=Firms.REKLAMA_BEL_27,
                            name=u'Банк для юридических лиц (Белоруссия)')

    BANK_BY_PH_BYN = Paysys(id=2701102, currency=Currencies.BYN, firm=Firms.REKLAMA_BEL_27,
                            name=u'Банк для физических лиц (Белоруссия)')

    YM_BY_PH_BYN = Paysys(id=11153, currency=Currencies.BYN, firm=Firms.REKLAMA_BEL_27, instant=1)

    # firm 30
    BANK_PH_RUB_CARSHARING = Paysys(id=3001001, currency=Currencies.RUB, firm=Firms.DRIVE_30,
                                    name=u'Банк для физических лиц')
    BANK_PH_WO_NDS_RUB_CARSHARING = Paysys(id=3001128, currency=Currencies.RUB, firm=Firms.DRIVE_30,
                                           name=u'Банк для физических лиц без НДС')
    BANK_UR_RUB_CARSHARING = Paysys(id=3001003, currency=Currencies.RUB, firm=Firms.DRIVE_30,
                                    name=u'Банк для юридических лиц')
    BANK_UR_WO_NDS_REFUELLER = Paysys(id=3001117, currency=Currencies.RUB, firm=Firms.DRIVE_30,
                                      name=u'Банк для физических лиц без НДС')

    # firm 31
    BANK_UR_KZT_TAXI_CORP = Paysys(id=3101020, currency=Currencies.KZT, firm=Firms.TAXI_CORP_KZT_31,
                                   name=u'Банк для юридических лиц, Казахстан (тенге)')
    CARD_UR_KZT_TAXI_CORP = Paysys(id=3101120, currency=Currencies.KZT, firm=Firms.TAXI_CORP_KZT_31,
                                   name=u'Кредитная карта для юр. лиц (Казахстан)')
    CE_KZT_TAXI_CORP = Paysys(id=3111058, currency=Currencies.KZT, firm=Firms.TAXI_CORP_KZT_31,
                              name=u'Сертификат (Казахстан)')

    BANK_UR_KZT_TAXI_CORP_UR_YTKZ = Paysys(id=3080, currency=Currencies.KZT, firm=Firms.TAXI_CORP_KZT_31,
                                           name=u'Банк для юридических лиц, KZT, нерезидент Казахстан, резидент Россия')

    # firm 32
    BANK_UR_FOOD_RUB = Paysys(id=3201003, currency=Currencies.RUB, firm=Firms.FOOD_32,
                              name=u'Банк для юридических лиц')
    CARD_UR_FOOD_RUB = Paysys(id=3201033, currency=Currencies.RUB, firm=Firms.FOOD_32,
                              name=u'Кредитная карта (Юр. Лица)')

    # firm 33
    BANK_HK_UR_USD = Paysys(id=1144, currency=Currencies.USD, firm=Firms.HK_ECOMMERCE_33,
                            name=u'Банк для юридических лиц, доллары (резиденты, Гонгконг)')
    BANK_HK_YT_USD = Paysys(id=1143, currency=Currencies.USD, firm=Firms.HK_ECOMMERCE_33,
                            name=u'Банк для юридических лиц, доллары (нерезиденты, Гонгконг)')
    BANK_HK_UR_EUR = Paysys(id=1149, currency=Currencies.EUR, firm=Firms.HK_ECOMMERCE_33,
                            name=u'Банк для юридических лиц, евро (резиденты, Гонгконг)')
    BANK_HK_YT_EUR = Paysys(id=1148, currency=Currencies.EUR, firm=Firms.HK_ECOMMERCE_33,
                            name=u'Банк для юридических лиц, евро (нерезиденты, Гонгконг)')

    # firm 34
    BANK_UR_SHAD_RUR = Paysys(id=3401003, currency=Currencies.RUB, firm=Firms.SHAD_34,
                              name=u'Банк для юридических лиц')
    BANK_UR_YT_SHAD_RUR = Paysys(
        id=3401014,
        currency=Currencies.RUB,
        firm=Firms.SHAD_34,
        name=u'Банк для юр.лиц, RUB, нерезидент, Россия',
    )

    # firm 35
    BANK_IL_UR_ILS = Paysys(id=1200, currency=Currencies.ILS, firm=Firms.YANDEX_GO_ISRAEL_35,
                            name=u'Банк для юридических лиц, шекели (Израиль)')
    BANK_IL_UR_WO_NDS_ILS = Paysys(id=1201, currency=Currencies.ILS, firm=Firms.YANDEX_GO_ISRAEL_35,
                                   name=u'Банк для юридических лиц резидентов без НДС, шекели (Израиль)')

    # firm 111
    BANK_YT_BYN_MARKET = Paysys(id=11101100, currency=Currencies.BYN, firm=Firms.MARKET_111,
                                name=u'Банк для нерезидентов (бел. рубли)')
    BANK_YT_KZT_TNG_MARKET = Paysys(id=11101060, currency=Currencies.KZT, firm=Firms.MARKET_111,
                                    name=u'Банк для юридических лиц без НДС (тенге)')

    BANK_UR_RUB_MARKET = Paysys(id=11101003, currency=Currencies.RUB, firm=Firms.MARKET_111,
                                name=u'Банк для юридических лиц')

    BANK_YT_USD_MARKET = Paysys(id=11101013, currency=Currencies.KZT, firm=Firms.MARKET_111,
                                name=u'Банк для юридических лиц')

    BANK_YT_EUR_MARKET = Paysys(id=11101023, currency=Currencies.KZT, firm=Firms.MARKET_111,
                                name=u'Банк для юридических лиц')

    BANK_YT_RUB_MARKET = Paysys(id=11101014, currency=Currencies.KZT, firm=Firms.MARKET_111,
                                name=u'Банк для юридических лиц')

    # firm 112

    CC_RUB_UR_TRUST_CLOUD = Paysys(id=11206430011019291, currency=Currencies.RUB, firm=Firms.CLOUD_112,
                                   name=u'Банк для юридических лиц')

    # from 113
    BANK_UR_RUB_AUTOBUS = Paysys(id=11301003, currency=Currencies.RUB, firm=Firms.AUTOBUS_113,
                                 name=u'Банк для юридических лиц')

    # firm 114
    BANK_UR_RUB_MEDICINE = Paysys(id=11401003, currency=Currencies.RUB, firm=Firms.HEALTH_114,
                                  name=u'Банк для юридических лиц')
    BANK_UR_RUB_MEDICINE_WO_NDS = Paysys(id=11401117, currency=Currencies.RUB, firm=Firms.HEALTH_114,
                                         name=u'Банк для юридических лиц без НДС')
    BANK_PH_RUB_MEDICINE = Paysys(id=11401001, currency=Currencies.RUB, firm=Firms.HEALTH_114,
                                  name=u'Банк для физических лиц')
    BANK_PH_RUB_MEDICINE_WO_VAT = Paysys(id=11401128, currency=Currencies.RUB, firm=Firms.HEALTH_114,
                                  name=u'Банк для физических лиц без НДС')

    # firm 115
    BANK_UR_UBER_USD = Paysys(id=11501041, currency=Currencies.USD, firm=Firms.UBER_115,
                              name=u'Банк для юридических лиц доллары (нерезиденты, Голландия)')
    BANK_UR_UBER_BYN = Paysys(id=1145, currency=Currencies.BYN, firm=Firms.UBER_115,
                              name=u'Банк для юридических лиц бел. рубли (нерезиденты, Голландия)')

    # firm 116
    BANK_AZ_AZN = Paysys(id=1250, currency=Currencies.AZN, firm=Firms.UBER_AZ_116,
                         name=u'Банк для юридических лиц, манат (Азербайджан)')

    # firm 121
    YM_MEDIASERVICES = Paysys(id=12101000, currency=Currencies.RUB, firm=Firms.MEDIASERVICES_121,
                              name=u'Яндекс.Деньги')
    BANK_PH_RUB_MEDIASERVICES = Paysys(id=12101001, currency=Currencies.RUB, firm=Firms.MEDIASERVICES_121,
                                       name=u'Банк для физических лиц')
    BANK_UR_RUB_MEDIASERVICES = Paysys(id=12101003, currency=Currencies.RUB, firm=Firms.MEDIASERVICES_121,
                                       name=u'Банк для юридических лиц')
    BANK_UR_RUB_WO_NDS_MEDIASERVICES = Paysys(id=12101117, currency=Currencies.RUB, firm=Firms.MEDIASERVICES_121,
                                              name=u'Банк для юридических лиц без НДС')
    CC_WSTORE_INAPP_MEDIASERVICES = Paysys(id=12101078, currency=Currencies.USD, firm=Firms.MEDIASERVICES_121,
                                           name=u'Windows Phone Store InApp Purchase в долларах')

    # firm 122
    BANK_AM_YT_RUB_TAXI_CORP_ARM = Paysys(id=12202400, currency=Currencies.RUB, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, RUB, нерезидент, Армения')
    BANK_AM_YT_USD_TAXI_CORP_ARM = Paysys(id=12202401, currency=Currencies.USD, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, USD, нерезидент, Армения')
    BANK_AM_YT_EUR_TAXI_CORP_ARM = Paysys(id=12202402, currency=Currencies.EUR, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, EUR, нерезидент, Армения')
    BANK_AM_YT_AMD_TAXI_CORP_ARM = Paysys(id=12202403, currency=Currencies.AMD, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, AMD, нерезидент, Армения')
    BANK_AM_YT_BYN_TAXI_CORP_ARM = Paysys(id=2409, currency=Currencies.BYN, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, BYN, нерезидент, Армения')
    BANK_AM_YT_NOK_TAXI_CORP_ARM = Paysys(id=2408, currency=Currencies.NOK, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, NOK, нерезидент, Армения')
    BANK_AM_UR_AMD_TAXI_CORP_ARM = Paysys(id=12201122, currency=Currencies.AMD, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, AMD, резидент, Армения')
    BANK_AM_UR_RUB_TAXI_CORP_ARM = Paysys(id=12202410, currency=Currencies.RUB, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, RUB, Армения')
    BANK_AM_UR_USD_TAXI_CORP_ARM = Paysys(id=12202411, currency=Currencies.USD, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, USD, Армения')
    BANK_AM_UR_EUR_TAXI_CORP_ARM = Paysys(id=12202412, currency=Currencies.EUR, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, EUR, Армения')
    BANK_AM_UR_BYN_TAXI_CORP_ARM = Paysys(id=2417, currency=Currencies.BYN, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, BYN, Армения')
    BANK_AM_UR_NOK_TAXI_CORP_ARM = Paysys(id=2416, currency=Currencies.NOK, firm=Firms.TAXI_CORP_ARM_122,
                                          name=u'Банк для юр.лиц, NOK, Армения')

    # firm 123
    BANK_UR_RUB_CLOUD = Paysys(id=12301003, currency=Currencies.RUB, firm=Firms.CLOUD_123,
                               name=u'Банк для юридических лиц')
    BANK_PH_RUB_CLOUD = Paysys(id=12301001, currency=Currencies.RUB, firm=Firms.CLOUD_123,
                               name=u'Банк для физических лиц')

    CC_RUB_PH_TRUST_CLOUD = Paysys(id=12306430011015052, currency=Currencies.RUB, firm=Firms.CLOUD_123,
                                   name=u'Банк для юридических лиц')

    # firm 124
    BANK_UR_RUB_GAS_STATIONS = Paysys(id=12401003, currency=Currencies.RUB, firm=Firms.GAS_STATIONS_124,
                                      name=u'Банк для юридических лиц')
    BANK_PH_RUB_GAS_STATIONS = Paysys(id=12401001, currency=Currencies.RUB, firm=Firms.GAS_STATIONS_124,
                                      name=u'Банк для физических лиц')

    CC_RUB_PH_TRUST_GAS_STATIONS = Paysys(id=12306430011015052, currency=Currencies.RUB, firm=Firms.GAS_STATIONS_124,
                                          name=u'Банк для юридических лиц')

    # firm 125
    BANK_EUR_MLU_EUROPE_EU_YT = Paysys(id=12501039, currency=Currencies.EUR, firm=Firms.MLU_EUROPE_125,
                                       name=u'Банк для юридических лиц евро (нерезиденты, Голландия)')
    BANK_USD_MLU_EUROPE_EU_YT = Paysys(id=12501041, currency=Currencies.USD, firm=Firms.MLU_EUROPE_125,
                                       name=u'Банк для юридических лиц доллары (нерезиденты, Голландия)')
    BANK_RON_MLU_EUROPE_EU_YT = Paysys(id=1147, currency=Currencies.RON, firm=Firms.MLU_EUROPE_125,
                                       name=u'Банк для юридических лиц леи (нерезиденты, Голландия)')
    BANK_SEK_MLU_EUROPE_EU_YT = Paysys(id=1147123, currency=Currencies.SEK, firm=Firms.MLU_EUROPE_125,
                                       name=u'Банк для юридических лиц шведские кроны (нерезиденты, Голландия)')

    # firm 126
    BANK_UR_USD_MLU_AFRICA = Paysys(id=12601041, currency=Currencies.USD, firm=Firms.MLU_AFRICA_126,
                                    name=u'Банк для юридических лиц доллары (нерезиденты, Голландия)')

    # firm 127
    BANK_RON_YANDEX_GO_SRL_RO_UR = Paysys(id=1150, currency=Currencies.RON, firm=Firms.YANDEX_GO_SRL_127,
                                          name=u'Банк для юридических лиц, леи (резиденты, Румыния)')

    # firm 128
    BANK_UR_BYN_BELGO_CORP = Paysys(id=12801101, currency=Currencies.BYN, firm=Firms.BELGO_CORP_128,
                                   name=u'Банк для юридических лиц (Белоруссия)')
    CARD_UR_BYN_BELGO_CORP = Paysys(id=12801125, currency=Currencies.BYN, firm=Firms.BELGO_CORP_128,
                                    name=u'Банковская карта (для юр. лиц, Белоруссия)')

    # firm 129
    BANK_UR_RUB_LAVKA = Paysys(id=12901003, currency=Currencies.RUB, firm=Firms.LAVKA,
                               name=u'Банк для юридических лиц (Лавка)')

    # firm 130
    BANK_UR_RUB_LOGISTICS = Paysys(id=13001003, currency=Currencies.RUB, firm=Firms.LOGISTICS_130,
                                   name=u'Банк для юридических лиц')
    BANK_UR_RUB_WO_NDS_LOGISTICS = Paysys(id=13001117, currency=Currencies.RUB, firm=Firms.LOGISTICS_130,
                                   name=u'Банк для юр.лиц, RUB, резидент, Россия')
    CARD_UR_RUB_LOGISTICS = Paysys(id=13001033, currency=Currencies.RUB, firm=Firms.LOGISTICS_130,
                                   name=u'Кредитная карта (Юр. Лица)')
    BANK_PH_RUB_LOGISTICS = Paysys(id=13001001, currency=Currencies.RUB, firm=Firms.LOGISTICS_130,
                                   name=u'Банк для физических лиц')

    # firm 1020
    BANK_UR_KZT_CLOUD = Paysys(id=102001020, currency=Currencies.KZT, firm=Firms.CLOUD_KZ,
                               name=u'Банк для юридических лиц, Казахстан (тенге)')
    BANK_PH_KZT_CLOUD = Paysys(id=102001021, currency=Currencies.KZT, firm=Firms.CLOUD_KZ,
                               name=u'Банк для физических лиц, Казахстан (тенге)')

    # firm 1000
    BANK_UR_RUB_K50 = Paysys(id=100001003, currency=Currencies.RUB, firm=Firms.K50, name=u'Банк для юридических лиц')
    BANK_YT_RUB_K50 = Paysys(id=100001014, currency=Currencies.RUB, firm=Firms.K50,
                             name=u'Банк для нерезидентов (рубли)')
    BANK_YTPH_RUB_K50 = Paysys(id=100001253, currency=Currencies.RUB, firm=Firms.K50,
                             name=u'Банк для физиков-нерезидентов (рубли)')

    # firm 1080
    BANK_FR_UR_EUR_BANK_NDS = Paysys(id=2080, currency=Currencies.EUR, firm=Firms.SAS_DELI_1080,
                                     name=u'Банк для юридических лиц, евро (Франция)')
    BANK_FR_UR_EUR_BANK_NO_NDS = Paysys(id=2081, currency=Currencies.EUR, firm=Firms.SAS_DELI_1080,
                                        name=u'Банк для юридических лиц, евро (Франция), без НДС')

    # firm 1083
    BANK_GB_UR_GBP_BANK_NDS = Paysys(id=2083, currency=Currencies.GBP, firm=Firms.DELI_INT_LIM_1083,
                                     name=u'Банк для юридических лиц, фунты стерлингов (Великобритания)')
    BANK_GB_UR_GBP_BANK_NO_NDS = Paysys(id=2084, currency=Currencies.GBP, firm=Firms.DELI_INT_LIM_1083,
                                        name=u'Банк для юридических лиц, фунты стерлингов (Великобритания), без НДС')

    # firm 1086
    BANK_UR_RUB_MICRO_MOBILITY = Paysys(id=108601003, currency=Currencies.RUB, firm=Firms.MICRO_MOBILITY,
                              name=u'Банк для юридичеких лиц')
    BANK_PH_RUB_MICRO_MOBILITY = Paysys(id=108601001, currency=Currencies.RUB, firm=Firms.MICRO_MOBILITY,
                              name=u'Банк для физических лиц')
    BANK_UR_RUB_TIPS_PLUS = Paysys(id=108701003, currency=Currencies.RUB, firm=Firms.SIMPLE_SERVICES,
                              name=u'Банк для юридичеких лиц')

    # firm 1088
    BANK_UR_UBER_BYN_USD = Paysys(id=108801041, currency=Currencies.USD, firm=Firms.UBER_1088,
                              name=u'Банк для юридических лиц доллары (нерезиденты, Голландия)')
    BANK_UR_UBER_BYN_BYN = Paysys(id=108801145, currency=Currencies.BYN, firm=Firms.UBER_1088,
                              name=u'Банк для юридических лиц бел. рубли (нерезиденты, Голландия)')

    # firm 1090
    BANK_YANGO_IL_UR_ILS = Paysys(id=109001200, currency=Currencies.ILS, firm=Firms.YANGO_ISRAEL_1090,
                                  name=u'Банк для юридических лиц, шекели (Израиль)')

    # firm 1092
    BANK_BYU_DELIVERY_BY_BYN = Paysys(id=109201101, currency=Currencies.BYN, firm=Firms.YANDEX_DELIVERY_BY,
                                      name=u'Банк для юр.лиц, BYN, резидент, Беларусь')

    BANK_KZU_DELIVERY_KZ_KZT = Paysys(id=109301020, currency=Currencies.KZT, firm=Firms.YANDEX_DELIVERY_KZ,
                                      name=u'Банк для юр.лиц, KZT, резидент, Казахстан')

    # firm 1097
    BANK_IL_UR_ILS_MARKET = Paysys(id=109701200, currency=Currencies.ILS, firm=Firms.MARKET_ISRAEL_1097,
                                   name=u'Банк для юр.лиц, IL, резидент, Израиль')
    BANK_IL_UR_ILS_WO_NDS_MARKET = Paysys(id=109701201, currency=Currencies.ILS, firm=Firms.MARKET_ISRAEL_1097,
                                          name=u'Банк для юр.лиц, IL, резидент, Израиль без НДС')
    # firm 1094
    BANK_EU_YT_FOODTECH_DELIVERY_USD = Paysys(id=109401041, currency=Currencies.USD, firm=Firms.FOODTECH_DELIVERY_BV,
                                              name=u'Банк для юр.лиц, USD, нерезидент, Нидерланды')
    BANK_EU_YT_FOODTECH_DELIVERY_EUR = Paysys(id=109401039, currency=Currencies.EUR, firm=Firms.FOODTECH_DELIVERY_BV,
                                              name=u'Банк для юр.лиц, EUR, нерезидент, Нидерланды')
    BANK_EU_YT_FOODTECH_DELIVERY_BYN = Paysys(id=109401145, currency=Currencies.BYN, firm=Firms.FOODTECH_DELIVERY_BV,
                                              name=u'Банк для юр.лиц, BYN, нерезидент, Нидерланды')

    # firm 1096
    BANK_UR_KZT_ZAPRAVKI_KZ = Paysys(id=109601020, currency=Currencies.KZT, firm=Firms.TAXI_CORP_KZT_31,
                                     name=u'Банк для юр.лиц, KZT, резидент, Казахстан')

    # firm 1890
    BANK_IL_UR_ILS_YANGO_DELIVERY = Paysys(id=189001200, currency=Currencies.ILS, firm=Firms.YANGO_DELIVERY,
                                           name=u'Банк для юридических лиц, шекели (Израиль)')
    # firm 1896
    BANK_USU_USD = Paysys(id=2094, currency=Currencies.USD, firm=Firms.DOUBLE_CLOUD_INC,
                          name=u'Банк для юридических лиц США, Доллары')
    BANK_USP_USD = Paysys(id=2095, currency=Currencies.USD, firm=Firms.DOUBLE_CLOUD_INC,
                          name=u'Банк для физических лиц США, Доллары')

    BANK_USYT_USD = Paysys(id=189611201, currency=Currencies.USD, firm=Firms.DOUBLE_CLOUD_INC,
                           name=u'Банк для юр.лиц, USD, нерезидент, США')
    BANK_USYTPH_USD = Paysys(id=189611203, currency=Currencies.USD, firm=Firms.DOUBLE_CLOUD_INC,
                             name=u'Банк для физ.лиц, USD, нерезидент, США')

    # firm 1896
    BANK_DEUR_EUR = Paysys(id=2090, currency=Currencies.EUR, firm=Firms.DOUBLE_CLOUD_GMBH,
                           name=u'Банк для юридических лиц Германии, Евро')
    BANK_DEPH_EUR = Paysys(id=2091, currency=Currencies.EUR, firm=Firms.DOUBLE_CLOUD_GMBH,
                           name=u'Банк для физических лиц Германии, Евро')

    BANK_DEYT_EUR = Paysys(id=2092, currency=Currencies.EUR, firm=Firms.DOUBLE_CLOUD_GMBH,
                           name=u'Банк для юр.лиц, EUR, нерезидент, Германия')
    BANK_DEYTPH_EUR = Paysys(id=2093, currency=Currencies.EUR, firm=Firms.DOUBLE_CLOUD_GMBH,
                             name=u'Банк для физ.лиц, EUR, нерезидент, Германия')

    # firm 1483
    CC_HK_YT_SPB_SOFTWARE_EUR = Paysys(
        id=11208,
        currency=Currencies.EUR,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банковская карта для юр.лиц, EUR, нерезидент, Гонконг',
    )

    BANK_HK_YT_SPB_SOFTWARE_EUR_GENERATED = Paysys(
        id=121601148,
        currency=Currencies.EUR,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банк для юр.лиц, EUR, нерезидент, Гонконг',
    )

    CC_HK_YTPH_SPB_SOFTWARE_EUR = Paysys(
        id=11210,
        currency=Currencies.EUR,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банковская карта для физ.лиц, EUR, нерезидент, Гонконг',
    )

    BANK_HK_YTPH_SPB_SOFTWARE_EUR = Paysys(
        id=11211,
        currency=Currencies.EUR,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банк для физ.лиц, EUR, нерезидент, Гонконг',
    )

    CC_HK_YT_SPB_SOFTWARE_USD = Paysys(
        id=11212,
        currency=Currencies.USD,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банковская карта для юр.лиц, USD, нерезидент, Гонконг',
    )

    BANK_HK_YT_SPB_SOFTWARE_USD_GENERATED = Paysys(
        id=121601143,
        currency=Currencies.USD,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банк для юр.лиц, USD, нерезидент, Гонконг',
    )

    CC_HK_YTPH_SPB_SOFTWARE_USD = Paysys(
        id=11214,
        currency=Currencies.USD,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банковская карта для физ.лиц, USD, нерезидент, Гонконг',
    )

    BANK_HK_YTPH_SPB_SOFTWARE_USD = Paysys(
        id=11215,
        currency=Currencies.USD,
        firm=Firms.SPB_SOFTWARE_1483,
        name=u'Банк для физ.лиц, USD, нерезидент, Гонконг',
    )

    # firm 1100
    BANK_UR_KGS_TAXI_CORP_KGZ = Paysys(id=2085, currency=Currencies.KGS, firm=Firms.TAXI_CORP_KGZ_1100,
                                       name=u'Банк для юридических лиц, сомы (Киргизия)')

    # firm 1011
    CC_UAE_UR_AED = Paysys(
        id=11204,
        currency=Currencies.AED,
        firm=Firms.DIRECT_CURSUS_1101,
        name=u'Банковская карта для юр.лиц, AED, резидент, ОАЭ'
    )

    BANK_UAE_UR_AED = Paysys(
        id=11205,
        currency=Currencies.AED,
        firm=Firms.DIRECT_CURSUS_1101,
        name=u'Банк для юр.лиц, AED, резидент, ОАЭ'
    )

    CC_UAE_UR_YT_AED = Paysys(
        id=11206,
        currency=Currencies.AED,
        firm=Firms.DIRECT_CURSUS_1101,
        name=u'Банковская карта для юр.лиц, AED, нерезидент, ОАЭ'
    )

    BANK_UAE_UR_YT_AED = Paysys(
        id=11207,
        currency=Currencies.AED,
        firm=Firms.DIRECT_CURSUS_1101,
        name=u'Банк для юр.лиц, AED, нерезидент, ОАЭ'
    )

    # firm 1891
    BANK_AM_YT_EUR = Paysys(id=2402, currency=Currencies.EUR, firm=Firms.BEYOND,
                                   name=u'Банк для юр.лиц, EUR, нерезидент, Армения')

    # firm 1099
    BANK_IL_PH_ILS_SAMOKAT = Paysys(id=2099, currency=Currencies.ILS, firm=Firms.WIND_1099,
                                    name = u'Банк для физических лиц, шекели (Израиль)')

    # firm 1898
    BANK_SRB_UR_RSD = Paysys(id=2086, currency=Currencies.RSD, firm=Firms.YANGO_DELIVERY_BEOGRAD_1898,
                             name=u'Банк для юридических лиц, динары (Сербия)')
    # firm 1889
    BANK_CL_UR_CLP = Paysys(id=2088, currency=Currencies.CLP, firm=Firms.YANGO_CHILE_SPA,
                            name=u'Банк для юридических лиц, песо (Чили)')

    # firm 1928
    BANK_UR_UBER_BEL_BYN = Paysys(id=192801101, currency=Currencies.BYN, firm=Firms.UBER_SYSTEMS_BEL,
                                  name=u'Банк для юр.лиц, BYN, резидент, Беларусь')
    BANK_PH_UBER_BEL_BYN = Paysys(id=192801102, currency=Currencies.BYN, firm=Firms.UBER_SYSTEMS_BEL,
                                  name=u'Банк для физ.лиц, BYN, резидент, Беларусь')

    # firm 1889
    BANK_UZB_UR_UZS = Paysys(id=2102, currency=Currencies.UZS, firm=Firms.YANDEX_LOG_OZB,
                             name=u'Банк для юридических лиц, узбекские сумы (Узбекистан)')
    BANK_UZB_UR_RUB = Paysys(id=2103, currency=Currencies.RUB, firm=Firms.YANDEX_LOG_OZB,
                             name=u'Банк для юридических лиц, рубли (Узбекистан)')
    BANK_UZB_UR_USD = Paysys(id=2104, currency=Currencies.USD, firm=Firms.YANDEX_LOG_OZB,
                             name=u'Банк для юридических лиц, доллары (Узбекистан)')


PAYSYS_BY_ID = {c.id: c for c in Paysyses.values()}


class PaymentMethods(utils.ConstantsContainer):
    PaymentMethod = namedtuple('PaymentMethod', 'id,cc,name')
    constant_type = PaymentMethod

    COMPOSITE = PaymentMethod(id=1525, cc='composite', name='Composite Payment')
    VIRTUAL = PaymentMethod(id=1526, cc='virtual', name='Virtual payments')
    VIRTUAL_BNPL = PaymentMethod(id=1526, cc='virtual::bnpl', name='Virtual bnpl')
    BANK = PaymentMethod(id=1001, cc='bank', name='Bank Payment')
    CARD = PaymentMethod(id=1101, cc='card', name='Credit Card')
    YAMONEY_WALLET = PaymentMethod(id=1201, cc='yamoney_wallet', name='Yandex.Money')
    WEBMONEY_WALLET = PaymentMethod(id=1202, cc='webmoney_wallet', name='WebMoney')
    QIWI_WALLET = PaymentMethod(id=1203, cc='qiwi_wallet', name='QIWI Wallet')
    PAYPAL_WALLET = PaymentMethod(id=1204, cc='paypal_wallet', name='Paypal')
    MOBILE = PaymentMethod(id=1301, cc='mobile', name='Mobile Commerce')
    MOSPARKING_ACCOUNT = PaymentMethod(id=1302, cc='mosparking_account', name='Moscow Parking Account')
    APPLE_INAPP = PaymentMethod(id=1401, cc='apple_inapp', name='AppStore InApp Purchase')
    GOOGLE_INAPP = PaymentMethod(id=1402, cc='google_inapp', name='Google Play InApp Purchase')
    YASTORE_INAPP = PaymentMethod(id=1403, cc='yastore_inapp', name='Yandex.Store InApp Purchase')
    WSTORE_INAPP = PaymentMethod(id=1404, cc='wstore_inapp', name='Windows Phone Store InApp Purchase')
    BARTER = PaymentMethod(id=1501, cc='barter', name='Barter')
    COMPENSATION = PaymentMethod(id=1502, cc='compensation', name='Compensation')
    CASH = PaymentMethod(id=1503, cc='cash', name='Cash')
    CERTIFICATE = PaymentMethod(id=1504, cc='certificate', name='Certificate')
    PROMOCODE = PaymentMethod(id=1505, cc='promocode', name='Promocode')
    YASTORE_BONUS = PaymentMethod(id=1506, cc='yastore_bonus', name='Yandex.Store Bonus Account')
    COMPENSATION_DISCOUNT = PaymentMethod(id=1509, cc='compensation_discount', name='Compensation for discount')
    COUPON = PaymentMethod(id=1507, cc='coupon', name='Coupons')
    SUBSIDY = PaymentMethod(id=1508, cc='subsidy', name='Subsidy')
    NEW_PROMOCODE = PaymentMethod(id=1510, cc='new_promocode', name='New promocode scheme')
    BRANDING_SUBSIDY = PaymentMethod(id=1512, cc='branding_subsidy', name='Branding subsidies')
    GUARANTEE_FEE = PaymentMethod(id=1513, cc='guarantee_fee', name='Guarantee fee')
    TRIP_BONUS = PaymentMethod(id=1514, cc='trip_bonus', name='Trip bonuses')
    PERSONNEL_BONUS = PaymentMethod(id=1515, cc='personnel_bonus', name='Personnel bonuses')
    DISCOUNT_TAXI = PaymentMethod(id=1516, cc='discount_taxi', name='Discounts_taxi')
    SUPPORT_COUPON = PaymentMethod(id=1517, cc='support_coupon', name='Support coupons')
    BOOKING_SUBSIDY = PaymentMethod(id=1518, cc='booking_subsidy', name='Subsidies&Booking')
    PAID_PROMOCODE = PaymentMethod(id=1511, cc='paid_promocode', name='Paid promocode')
    TAXI_WALLET_CREDIT = PaymentMethod(id=1519, cc='taxi_wallet_credit', name='TaxiWalletCredit')
    TAXI_WALLET_DEBIT = PaymentMethod(id=1520, cc='taxi_wallet_debit', name='TaxiWalletDebit')
    MARKET_SBOL_CERT = PaymentMethod(id=1521, cc='market_sbol_cert', name='MarketSBOLCertificateBuy')
    MARKET_CERTIFICATE = PaymentMethod(id=1522, cc='market_certificate', name='MarketCertificatePay')
    ZAXI_FUEL = PaymentMethod(id=1523, cc='zaxi_fuel', name='Fuel transaction for Zapravki on Taxi')
    ZAXI_DEPOSIT = PaymentMethod(id=1524, cc='zaxi_deposit', name='Deposit payment for Zaparavki on Taxi')
    SPASIBO = PaymentMethod(id=1527, cc='spasibo', name='spasibo')
    SPASIBO_CASHBACK = PaymentMethod(id=1528, cc='spasibo_cashback', name='spasibo_cashback')
    AFISHA_FAKE_REFUND = PaymentMethod(id=1537, cc='afisha_fake_refund', name='Afisha refunds from partner')
    AFISHA_CERTIFICATE = PaymentMethod(id=1538, cc='afisha_certificate', name='Afisha certificate')
    YANDEX_ACCOUNT_WITHDRAW = PaymentMethod(id=1534, cc='yandex_account_withdraw',
                                            name='Yandex.Account withdraw operation')
    YANDEX_ACCOUNT_TOPUP = PaymentMethod(id=1535, cc='yandex_account_topup', name='Yandex.Account topup operation')
    CREDIT = PaymentMethod(id=1541, cc='credit', name='Credit')
    CREDIT_CESSION = PaymentMethod(id=1541, cc='credit::cession', name='Credit Cession')



class CurrencyRateSource(utils.ConstantsContainer):
    RateSource = namedtuple('RateSource', 'id, name, oebs_rate_src')
    constant_type = RateSource

    CBR = RateSource(1000, u'Central Bank of the Russian Federation', 1000)
    ECB = RateSource(1001, u'European Central Bank', 1020)
    TCMB = RateSource(1002, u'Central Bank of the Republic of Turkey', 1021)
    NBU = RateSource(1003, u'National Bank of Ukraine', 1022)
    BOC = RateSource(1004, u'Public Bank of China', 1280)
    CBA = RateSource(1005, u'Central Bank of Armenia', 1300)
    NBRB = RateSource(1006, u'National Bank of the Republic of Belarus', 1301)
    NBKZ = RateSource(1007, u'National Bank of Kazakhstan', 1340)
    NBGE = RateSource(1008, u'National Bank of Georgia', 1420)
    BNM = RateSource(1009, u'National Bank of Moldova', 1480)
    NBKR = RateSource(1010, u'National Bank of the Kyrgyz Republic', 1540)
    CBU = RateSource(1011, u'Central Bank of the Republic of Uzbekistan', 1560)
    NBS = RateSource(1012, u'National Bank of Serbia', 1561)
    NBT = RateSource(1013, u'National Bank of Tajikistan', 1562)
    BOM = RateSource(1014, u'Central Bank of Mongolia', 1563)
    CBAR = RateSource(1015, u'Central Bank of the Republic of Azerbaijan', 1564)
    CNB = RateSource(1016, u'Czech National Bank', 1600)
    NBP = RateSource(1017, u'National Bank of Poland', 1601)
    BOI = RateSource(1018, u'Bank of Israel', 1160)
    NBE = RateSource(1019, u'National Bank of Ethiopia', 1603)
    CBN = RateSource(1020, u'Central Bank of Nigeria', 1604)
    BCEAO = RateSource(1021, u'Central Bank of West African States', None)
    SARB = RateSource(1022, u'South African Reserve Bank', None)
    CBK = RateSource(1023, u'Central Bank of Kenya', None)
    BNR = RateSource(1024, u'National Bank of Romania', None)
    MNB = RateSource(1025, u'Hungarian National Bank', None)
    BNB = RateSource(1026, u'Bulgarian National Bank', None)
    BOE = RateSource(1027, u'Bank of England', 1720)
    cbuae = RateSource(1030, u'Central Bank of the United Arab Emirates', 1900)
    OANDA = RateSource(1100, u'OANDA Corporation', 1580)
    BALANCE = RateSource(1111, u'Balance', 1023)


# t_firm_country
class Regions(utils.ConstantsContainer):
    Region = namedtuple('Region', 'id,name,currency,rate_scr_id')
    constant_type = Region

    US = Region(id=84, name=u'США', currency=Currencies.USD, rate_scr_id=None)
    GR = Region(id=96, name=u'Германия', currency=None, rate_scr_id=None)
    LT = Region(id=117, name=u'Литва', currency=Currencies.EUR, rate_scr_id=CurrencyRateSource.ECB)
    FIN = Region(id=123, name=u'Финляндия', currency=Currencies.EUR, rate_scr_id=CurrencyRateSource.ECB)
    SW = Region(id=126, name=u'Швейцария', currency=Currencies.EUR, rate_scr_id=None)
    BY = Region(id=149, name=u'Беларусь', currency=Currencies.BYN, rate_scr_id=CurrencyRateSource.NBRB)
    KZ = Region(id=159, name=u'Казахстан', currency=Currencies.KZT, rate_scr_id=CurrencyRateSource.NBKZ)
    AZ = Region(id=167, name=u'Азербайджан', currency=Currencies.AZN, rate_scr_id=CurrencyRateSource.CBAR)
    ARM = Region(id=168, name=u'Армения', currency=Currencies.AMD, rate_scr_id=CurrencyRateSource.CBA)
    GEO = Region(id=169, name=u'Грузия', currency=Currencies.GEL, rate_scr_id=CurrencyRateSource.NBGE)
    UZB = Region(id=171, name=u'Узбекистан', currency=Currencies.UZS, rate_scr_id=CurrencyRateSource.CBU)
    EST = Region(id=179, name=u'Эстония', currency=None, rate_scr_id=None)
    UA = Region(id=187, name=u'Украина', currency=Currencies.UAH, rate_scr_id=CurrencyRateSource.NBU)
    LAT = Region(id=206, name=u'Латвия', currency=Currencies.EUR, rate_scr_id=CurrencyRateSource.ECB)
    KGZ = Region(id=207, name=u'Киргизия', currency=Currencies.KGS, rate_scr_id=CurrencyRateSource.NBKR)
    MD = Region(id=208, name=u'Молдова', currency=Currencies.MDL, rate_scr_id=CurrencyRateSource.BNM)
    RU = Region(id=225, name=u'Россия', currency=Currencies.RUB, rate_scr_id=CurrencyRateSource.CBR)
    TR = Region(id=983, name=u'Турция', currency=Currencies.TRY, rate_scr_id=CurrencyRateSource.TCMB)
    ABH = Region(id=29386, name=u'Абхазия', currency=None, rate_scr_id=None)
    OST = Region(id=29387, name=u'Южная Осетия', currency=None, rate_scr_id=None)
    SSD = Region(id=108137, name=u'Южный Судан', currency=None, rate_scr_id=None)
    CIV = Region(id=20733, name=u'Кот-д’Ивуар', currency=Currencies.XOF, rate_scr_id=CurrencyRateSource.BCEAO)
    ISR = Region(id=181, name=u'Израиль', currency=Currencies.ILS, rate_scr_id=CurrencyRateSource.BOI)
    GHA = Region(id=20802, name=u'Гана', currency=Currencies.GHS, rate_scr_id=CurrencyRateSource.OANDA)
    RO = Region(id=10077, name=u'Румыния', currency=Currencies.RON, rate_scr_id=CurrencyRateSource.BNR)
    RS = Region(id=180, name=u'Сербия', currency=Currencies.RSD, rate_scr_id=CurrencyRateSource.NBS)
    ZA = Region(id=10021, name=u'ЮАР', currency=Currencies.ZAR, rate_scr_id=CurrencyRateSource.SARB)
    NOR = Region(id=119, name=u'Норвегия', currency=Currencies.NOK, rate_scr_id=CurrencyRateSource.ECB)
    SWE = Region(id=127, name=u'Швеция', currency=Currencies.SEK, rate_scr_id=CurrencyRateSource.ECB)
    CMR = Region(id=20736, name=u'Камерун',  currency=Currencies.XAF, rate_scr_id=CurrencyRateSource.OANDA)
    SEN = Region(id=21441, name=u'Сенегал',  currency=Currencies.XOF, rate_scr_id=CurrencyRateSource.BCEAO)
    FR = Region(id=124, name=u'Франция',  currency=Currencies.EUR, rate_scr_id=CurrencyRateSource.ECB)
    GB = Region(id=102, name=u'Великобритания', currency=Currencies.GBP, rate_scr_id=CurrencyRateSource.BOE)
    ZAM = Region(id=21196, name=u'Замбия', currency=Currencies.ZMW, rate_scr_id=CurrencyRateSource.OANDA)
    ANG = Region(id=21182, name=u'Ангола', currency=Currencies.AOA, rate_scr_id=CurrencyRateSource.OANDA)
    BOL = Region(id=10015, name=u'Боливия', currency=Currencies.BOB, rate_scr_id=CurrencyRateSource.OANDA)
    COG = Region(id=21198, name=u'Республика Конго', currency=Currencies.XAF, rate_scr_id=CurrencyRateSource.OANDA)
    COD = Region(id=20762, name=u'Демократическая Республика Конго', currency=Currencies.CDF,
                 rate_scr_id=CurrencyRateSource.OANDA)
    DZA = Region(id=20826, name=u'Алжир', currency=Currencies.DZD, rate_scr_id=CurrencyRateSource.OANDA)
    MXC = Region(id=20271, name=u'Мексика', currency=Currencies.MXN, rate_scr_id=CurrencyRateSource.ECB)
    PER = Region(id=21156, name=u'Перу', currency=Currencies.PEN, rate_scr_id=CurrencyRateSource.OANDA)
    UAE = Region(id=210, name=u'Объединенные Арабские Эмираты', currency=Currencies.AED,
                 rate_scr_id=CurrencyRateSource.cbuae)
    CL = Region(id=20862, name=u'Чили', currency=Currencies.CLP,
                rate_scr_id=CurrencyRateSource.OANDA)


# Вид договора (t_contract_types)
class ContractSubtype(Enum):
    GENERAL = 0
    DISTRIBUTION = 1
    SPENDABLE = 2
    PARTNERS = 3
    AFISHA = 4
    GEOCONTEXT = 5
    ACQUIRING = 6


class RsyaContractType(utils.ConstantsContainer):
    constant_type = int

    NEW_OFFER_RSYA = 7
    SSP = 8
    OFFER = 9
    LICENSE = 10


# Тип договора
class ContractCommissionType(Enum):
    def __init__(self, id, display_name):
        self.id = id
        self.display_name = display_name

    # !! названия этих констант используются в виде строк, поэтому лучше не рефакторить
    NO_AGENCY = (0, u'Не агентский')
    COMMISS = (1, u'Комиссионный')
    PARTNER = (2, u'Партнерский')
    WO_COUNT = (3, u'Без уч. в расчетах')
    PR_AGENCY = (4, u'Прямой агентский')
    OPT_AGENCY = (6, u'Оптовый агентский')
    OPT_CLIENT = (7, u'Оптовый клиентский')
    KZ = (8, u'Казахстан')
    OFFER = (9, u'Договор-оферта')

    OFD_WO_COUNT = (14, u'ОФД Без уч. в расчетах')
    OPT_AGENCY_RB = (16, u'Оптовый агентский с РБ')
    UA_OPT_CLIENT = (17, u'Украина: оптовый клиентский')
    USA_OPT_CLIENT = (18, u'США: оптовый клиентский')
    USA_OPT_AGENCY = (19, u'США: оптовый агентский')

    UA_COMMISS = (21, u'Украина: комиссионный')
    SW_OPT_CLIENT = (22, u'Швейцария: Оптовый клиентский')
    SW_OPT_AGENCY = (23, u'Швейцария: Оптовый агентский')
    UA_PR_AGENCY = (24, u'Украина: прямой агентский')
    TR_OPT_AGENCY = (25, u'Турция: Оптовый агентский')
    TR_OPT_CLIENT = (26, u'Турция: Оптовый клиентский')
    UA_OPT_AGENCY_PREM = (27, u'Украина: оптовый агентский, премия')

    GARANT_RU = (40, u'Гарантийное письмо Россия')
    GARANT_BEL = (41, u'Гарантийное письмо Беларусь')
    GARANT_UA = (42, u'Гарантийное письмо Украина')
    GARANT_KZT = (43, u'Гарантийное письмо Казахстан')

    BRAND = (50, u'Соглашение о рекламном бренде')

    ATTORNEY = (60, u'Доверенность')
    OPT_AGENCY_PREM = (61, u'Оптовый агентский, премия')
    BEL_OPT_AGENCY_PREM = (62, u'Яндекс.Реклама: оптовый агентский, премия')

    AUTO_OPT_AGENCY_PREM = (70, u'Авто.ру: Оптовый агентский, премия')
    AUTO_NO_AGENCY = (71, u'Авто.ру: Не агентский')
    LICENSE = (72, u'Лицензионный')

    KZ_NO_AGENCY = (300, u'Яндекс.Казахстан: Не агентский')
    KZ_COMMISSION = (301, u'Казахстан: комиссионный')
    KZ_OPT_AGENCY = (306, u'Яндекс.Казахстан: Оптовый агентский')

    BEL_NO_AGENCY = (400, u'Яндекс.Реклама: Не агентский')
    BEL_PR_AGENCY = (404, u'Яндекс.Реклама: Прямой агентский')
    BEL_OPT_AGENCY = (406, u'Яндекс.Реклама: Оптовый агентский')

    # метод для поддержки старого кода (когда в create_contract_new тип договора передается строкой)
    @classmethod
    def get_by_name(cls, constant_or_name):
        if isinstance(constant_or_name, Enum):
            return constant_or_name
        else:
            return cls[constant_or_name.upper()]


class ContractPaymentType(utils.ConstantsContainer):
    constant_type = int
    PREPAY = 2
    POSTPAY = 3


class SpendablePaymentType(utils.ConstantsContainer):
    constant_type = int
    MONTHLY = 1
    QUARTERLY = 2


class AwardsScaleType(object):
    NO = 0
    BASE_2015 = 1
    PREMIUM_2015 = 2
    SPEC_PROJECT = 3
    AUTO = 4
    GEO = 5
    AUTO_RU = 7
    BASE_REGIONS_2015 = 8
    AUTO_RU_2015 = 9
    REALTY_REGIONS_2017 = 10
    AUDIO_ADV_2015 = 11
    MARKET_2016 = 12
    MARKET_REGIONS_2016 = 13
    MARKET_SPEC_PROJECT = 14
    OFD_WO_COUNT = 15
    REALTY_2017 = 16
    MEDIA_VERTICAL_2017 = 17
    RADIO_2017 = 18
    INTERCO_2017 = 19
    BEL_PREM = 20
    BASE_SPB = 21
    PROF_AG = 25
    BASE_AG = 26
    LIGHT_AG = 27


class CommissionType(object):
    NEYM = 59
    KZ = 60
    KOK_MEDIA = 49
    INDIVIDUAL_CONDITION = 33
    AUTO = 56
    BASE = 47
    MARKET_CPC_CPA = 52
    IMHO = 54
    UK_BASE = 43
    NON_RESIDENT = 57
    UK_MARKET_CPC_CPA = 53
    MEDIA_IMHO = 51
    HANDBOOK = 50
    UK = 55
    PROFESSIONAL = 48
    SPECIAL_MEDIA = 37


class BrandType(object):
    MEDIA = 70
    AUTO_RU = 99
    DIRECT = 77
    DIRECT_TECH = 7


class ContractCreditType(object):
    BY_TERM = 1
    BY_TERM_AND_SUM = 2


class Collateral(utils.ConstantsContainer):
    constant_type = int

    # продление
    PROLONG = 80
    # расторжение
    TERMINATE = 90
    # перевод на предоплату
    DO_PREPAY = 100
    # изменение сервисов
    CHANGE_SERVICES = 1001
    # прочее
    OTHER = 1003
    # изменение состава бренда
    BRAND_CHANGE = 1026
    # изменение кредита
    CHANGE_CREDIT = 1004
    DO_POSTPAY_LS = 1033
    SUBCLIENT_CREDIT_LIMIT = 1035
    CHANGE_COMMISSION_PCT = 1045
    # тестовый период DSP
    TEST_PERIOD = 1049
    # смена процентов за АВ и консультацию
    CHANGE_TELEMED_PCT = 1051
    # признак ценный клиент для AdFox
    VIP_CLIENT = 1053
    # Частные сделки, комиссия агентства
    PRIVATE_DEALS = 1070
    # Изменение процента вознаграждения со страховок
    INSURANCE_REWARD_PCT_CHANGE = 1090
    # Изменение процента удержания налога (для Такси в Израиле)
    ISRAEL_TAX_PCT_CHANGE_GENERAL = 1094
    # расторжение договора дистрибуции
    DISTR_TERMINATION = 3060
    # изменение минималки предоплата
    CHANGE_ADV_PAYMENT = 2222
    # изменение размера комиссии
    COMMISSION_CHANGE = 2223
    # изменение размера комиссии за refund
    COMMISSION_REFUND_CHANGE = 2224
    # изменение минималки постоплата
    CHANGE_MIN_COST = 1038
    # DMP для AdFox
    DMP = 1666
    # добавление заправок в единый договор
    CHANGE_SERVICES_ZAPRAVKI = 1101

    # Collaterals for SPENDABLE
    # прочее
    NDS_CHANGE = 7010
    ETC = 7020
    SPENDABLE_TERMINATION = 7050
    ISRAEL_TAX_PCT_CHANGE_SPENDABLE = 7070


class Permissions(object):
    ADMIN_ACCESS_0 = 0
    CREATE_BANK_PAYMENTS_3 = 3
    PROCESS_ALL_INVOICES = 8
    MANAGE_BAD_DEBTS_21 = 21
    MANAGERS_OPERATIONS_23 = 23
    EDIT_CLIENT_28 = 28
    REDIRECT_TO_YANDEX_TEAM_36 = 36
    ADDITIONAL_FUNCTIONS_47 = 47
    NEW_UI_EARLY_ADOPTER_62 = 62
    WITHDRAW_CONSUMES_POSTPAY = 66
    WITHDRAW_CONSUMES_PREPAY = 67
    BILLING_SUPPORT_1100 = 1100
    DO_INVOICE_REFUND = 4004
    DO_INVOICE_REFUND_TRUST = 11161
    VIEW_INVOICES = 11105
    CLOSE_INVOICE = 11222
    OEBS_REEXPORT_INVOICE = 11223
    NEW_UI_EARLY_64 = 64
    PERSON_POST_ADDRESS_EDIT = 38
    PERSON_EXT_EDIT = 46
    EDIT_PERSONS = 26


class Roles(object):
    ADMIN_0 = 0
    AGENCY_2 = 2
    CLIENT_3 = 3
    SUPPORT_17 = 17
    DOCS_EXTRA_5 = 5
    READ_ONLY_12 = 12


class Passport(object):
    def __init__(self, url, descr, domain='ru'):
        self._url = url
        self._descr = descr
        self.domain = domain

    @property
    def url(self):
        return self._url.format(self.domain)

    @property
    def descr(self):
        return u'{} [{}]'.format(self._descr, self.domain)

    def with_domain(self, domain):
        return Passport(self._url, self._descr, domain)


class Passports(utils.ConstantsContainer):
    constant_type = Passport

    PROD = Passport('https://passport.yandex.{}/', u'Яндекс.Паспорт')
    TEST = Passport('https://passport-test.yandex.{}/', u'Яндекс.Паспорт (passport-test)')
    INTERNAL = Passport('https://passport.yandex-team.{}/', u'Яндекс.Паспорт (yandex-team)')


class User(object):
    def __init__(self, uid, login, password=secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD)):
        self.uid = uid  # aka passport_id
        self.login = login
        self.password = password

    def __repr__(self):
        return '{login} ({uid})'.format(login=self.login, uid=self.uid)


# здесь храним только часто используемых пользователей
# других пользователей для тестов брать отсюда https://wiki.yandex-team.ru/Testirovanie/FuncTesting/Billing/testlogins/
class Users(utils.ConstantsContainer):
    constant_type = User

    # логины из продакшен паспорта
    YB_ADM = User(1602414239, 'yb-adm-new', get_secret(*UsersPwd.YB_ADM_NEW_PWD_SECRET))
    YT_REG_CQR5 = User(38622751, 'yandex-team-reg-cqR5', get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD))

    CLIENT_TESTBALANCE_MAIN = User(424854100, 'client-testbalance-main')
    CLIENT_TESTBALANCE_NOT_MAIN = User(424854247, 'client-testbalance-not-main')
    CLIENT_TESTBALANCE_ACCOUNTANT = User(424854395, 'client-testbalance-accountant')

    WEB_TEST_USER1 = User(589060129, 'yndx-web-test-uid-1')
    WEB_TEST_USER2 = User(589060147, 'yndx-web-test-uid-2')
    WEB_TEST_USER3 = User(589060165, 'yndx-web-test-uid-3')
    WEB_TEST_USER4 = User(589060176, 'yndx-web-test-uid-4')
    WEB_TEST_USER5 = User(589060182, 'yndx-web-test-uid-5')

    # логины из доменного паспорта
    TESTUSER_BALANCE1 = User(1120000000008037, 'testuser-balance1', get_secret(*Robots.TESTUSER_BALANCE1_PWD))
    TESTUSER_BALANCE2 = User(1120000000008038, 'testuser-balance2', get_secret(*Robots.TESTUSER_BALANCE2_PWD))
    TESTUSER_BALANCE3 = User(1120000000017701, 'testuser-balance3', get_secret(*Robots.TESTUSER_BALANCE3_PWD))
    TESTUSER_BALANCE4 = User(1120000000017702, 'testuser-balance4', get_secret(*Robots.TESTUSER_BALANCE4_PWD))

    MANAGER = User(124116458, 'mngclientuid1', get_secret(*UsersPwd.CLT_MANAGER_PWD))

    HERMIONE_CLIENT = User(1490838356, 'yb-hermione-ci-1', get_secret(*UsersPwd.HERMIONE_CI_PWD_NEW))
    TMP_TEST_USER = User(1576140626, 'yb-temp-user-1', get_secret(*UsersPwd.HERMIONE_CI_PWD_NEW))


class Card(object):
    # a-vasin: пока валиден только 2019 год для оплаты через web для рбс
    def __init__(self, number, cvv, card_holder, month=12, year=datetime.now().year + 2, password=''):
        self.number = number
        self.cvv = cvv
        self.card_holder = card_holder
        self.month = month
        self.year = year
        self.password = password


class Cards(utils.ConstantsContainer):
    constant_type = Card

    VALID_3DS = Card('4111 1111 1111 1111', '123', "RBS TEST", 12, 2024, '12345678')
    VALID_3DS_2_DIGIT_YEAR = Card('4111 1111 1111 1111', '123', "RBS TEST", 12, 24, '12345678')


class Emails(utils.ConstantsContainer):
    constant_type = str

    DUMMY_EMAIL = 'asdlkfjasklasdfa@yandex-team.ru'


class Nds(utils.ConstantsContainer):
    constant_type = int

    DEFAULT = 18
    YANDEX_RESIDENT = 20
    NOT_RESIDENT = 0
    SAG_RESIDENT = 8
    EUROPE_AG_RESIDENT = 7.7
    UKRAINE = 20
    BELARUS = 20
    ARMENIA = 0
    KAZAKHSTAN = 12
    TAXI_KAZAKHSTAN = 0
    ZERO = 0
    NONE = None

    @staticmethod
    def get(nds):
        return 0 if nds is None else nds

    @staticmethod
    def get_pct(nds):
        if nds is None:
            return 1

        return utils.fraction_from_percent(nds)


class NdsNew(object):
    class NdsPolicy(object):
        def __init__(self, nds_id):
            self.nds_id = nds_id

        @utils.memoize
        def pct_on_dt(self, dt):
            if self.nds_id is None:
                return 0

            pct = db.balance().execute(
                ''' select nds_pct from bo.v_nds_pct
                    where ndsreal_id = :nds_id and from_dt <= :dt and :dt < to_dt
                ''', dict(dt=dt, nds_id=self.nds_id)
            )[0]['nds_pct']
            return Decimal(pct)

        @utils.memoize
        def koef_on_dt(self, dt):
            if self.nds_id is None:
                return 1

            koef = db.balance().execute(
                ''' select nds_koef from bo.v_nds_pct
                    where ndsreal_id = :nds_id and from_dt <= :dt and :dt < to_dt
                ''', dict(dt=dt, nds_id=self.nds_id)
            )[0]['nds_koef']
            return Decimal(koef)

        def get_pct(self):
            if self.nds_id is None:
                return 1

        def __nonzero__(self):
            return True if self.nds_id not in (0, None) else False

    DEFAULT = NdsPolicy(18)
    YANDEX_RESIDENT = NdsPolicy(18)
    NOT_RESIDENT = NdsPolicy(0)
    SAG_RESIDENT = NdsPolicy(8)
    EUROPE_AG_RESIDENT = NdsPolicy(8)
    EUROPE_AG_NON_RESIDENT = NdsPolicy(0)
    UKRAINE = NdsPolicy(20)
    BELARUS = NdsPolicy(620)
    ARMENIA = NdsPolicy(0)
    KAZAKHSTAN = NdsPolicy(12)
    TAXI_KAZAKHSTAN = NdsPolicy(0)
    HK_RESIDENT = NdsPolicy(0)
    ISRAEL = NdsPolicy(17)
    AZARBAYCAN = NdsPolicy(16718)
    ZERO = NdsPolicy(0)
    NONE = NdsPolicy(None)
    ROMANIA = NdsPolicy(19)
    FR = NdsPolicy(27)
    GB = NdsPolicy(28)
    KG = NdsPolicy(29)
    RS = NdsPolicy(18018)
    CL = NdsPolicy(31)
    UZB = NdsPolicy(32)


class PaysysType(object):
    MONEY = 'yamoney'
    WALLET = 'wallet1'
    YANDEX = 'yandex'
    TAXI = 'yataxi'
    MARKET = 'yamarket'
    MARKET_PLUS = 'yamarketplus'
    ALFA = 'alfa'
    DELIVERY = 'delivery'
    FARMA = 'farma'
    PAYPAL = 'paypal'
    ECOMMPAY_EUROPE = 'ecommpay_europe'
    INSURANCE = 'insurance'
    SUBPARTNER = 'subpartner'
    PAYTURE = 'payture'
    SPASIBO = 'spasibo'
    FUEL_HOLD = 'fuel_hold'
    FUEL_HOLD_PAYMENT = 'fuel_hold_payment'
    FUEL_FACT = 'fuel_fact'
    EXTRA_PROFIT = 'extra_profit'
    DRIVE = 'yadrive'
    ZAPRAVKI = 'yazapravki'
    BANK_CREDIT = 'bank_credit'
    YAEDA = 'yaeda'
    RED_IN_BLUE = 'redinblue'
    NETTING_WO_NDS = 'netting_wo_nds'
    AFISHA_CERTIFICATE = 'afisha_certificate'
    FAKE_REFUND = 'fake_refund'
    FAKE_REFUND_CERTIFICATE = 'fake_refund_cert'
    SBERBANK = 'sberbank'
    SORT_CENTER = 'sort_center'
    MARKET_DELIVERY = 'market-delivery'
    VIRTUAL_BNPL = 'bnpl'
    NEWS_PAYMENT = 'news_payment'
    PROMOCODE = 'promocode'
    NEW_PROMOCODE = 'new_promocode'
    CREDIT_CESSION = 'credit_cession'
    MARKETING_PROMO = 'marketing_promo'
    CERTIFICATE_PROMO = 'certificate_promo'


class PaymentType(object):
    ACCOUNT_CORRECTION = 'account_correction'
    APPLE_TOKEN = 'apple_token'
    BALALAYKA_PAYMENT = 'balalayka_payment'
    BOOKING_SUBSIDY = 'booking_subsidy'
    BRANDING_SUBSIDY = 'branding_subsidy'
    CARD = 'card'
    CASH = 'cash'
    CASHRUNNER = 'cashrunner'
    COMPENSATION = 'compensation'
    COMPENSATION_DISCOUNT = 'compensation_discount'
    COMPENSATION_PROMO = 'compensation_promocode'
    CORPORATE = 'corporate'
    CORRECTION_COMMISSION = 'correction_commission'
    CORRECTION_NETTING = 'correction_commission'
    COST = 'cost'
    COUPON = 'coupon'
    CORP_COUPON = 'corp_coupon'
    DEPOSIT = 'deposit'
    DEPOSIT_PAYOUT = 'deposit_payout'
    DIRECT_CARD = 'direct_card'
    DISCOUNT_TAXI = 'discount_taxi'
    DRIVE_FUELLER = 'drive_fueler'
    DRYCLEAN = 'dryclean'
    GUARANTEE_FEE = 'guarantee_fee'
    GROCERY_COURIER_DELIVERY = 'grocery_courier_delivery'
    GROCERY_COURIER_COUPON = 'grocery_courier_coupon'
    MARKETING = 'marketing'
    MARKETING_PROMO = 'marketing_promocode'
    NEW_PROMOCODE = 'new_promocode'
    OUR_REFUND = 'our_refund'
    PAYMENT_NOT_RECEIVED = 'payment_not_received'
    PERSONNEL_BONUS = 'personnel_bonus'
    PREPAID = 'prepaid'
    PRICE_DIFFERENCE = 'price_difference'
    REFUEL = 'refuel'
    REWARD = 'reward'
    SCOUT = 'scout'
    SCOUT_SZ = 'scout_sz'
    SPASIBO = 'spasibo'
    SUBSIDY = 'subsidy'
    CORP_SUBSIDY = 'corp_subsidy'
    SUPPORT_COUPON = 'support_coupon'
    SVO = 'taxi_stand_svo'
    TRIP_BONUS = 'trip_bonus'
    ZEN_PAYMENT = 'zen_payment'
    CORP_TAXI_PARTNER_TRIP_PAYMENT = 'park_b2b_trip_payment'
    SBERBANK_CREDIT = 'sberbank_credit'
    CORP_TAXI_CARGO = 'cargo_park_b2b_trip_payment'
    CORP_TAXI_DELIVERY = 'delivery_park_b2b_trip_payment'
    DRIVER_REFERRALS = 'driver_referrals'
    CARGO_SUBSIDY = 'cargo_subsidy'
    CARGO_COUPON = 'cargo_coupon'
    SCOUT_CARGO_SUBSIDY = 'cargo_scout'
    SCOUT_CARGO_SZ_SUBSIDY = 'cargo_scout_sz'
    DELIVERY_SUBSIDY = 'delivery_subsidy'
    DELIVERY_COUPON = 'delivery_coupon'
    PARTNERS_LEARNING_CENTER = 'partners_learning_center'
    PARTNERS_MOTIVATION_PROGRAM = 'partners_motivation_program'
    AFISHA_CERTIFICATE = 'afisha_certificate'
    FAKE_REFUND = 'fake_refund'
    FAKE_REFUND_CERTIFICATE = 'fake_refund_cert'
    CERTIFICATE_PROMOCODE = 'certificate_promocode'
    MARKETING_PROMOCODE = 'marketing_promocode'
    FEE = 'fee'
    COST_INSURANCE = 'cost_insurance'
    REWARD_INSURANCE = 'reward_insurance'
    PROMOCODE = 'promocode'
    YANDEX_ACCOUNT_WITHDRAW = 'yandex_account_withdraw'
    YANDEX_ACCOUNT_TOPUP = 'yandex_account_topup'
    REWARD_ACCOUNT_WITHDRAW = 'reward_account_withdraw'
    LOGISTICS_CARGO = 'cargo_park_b2b_logistics_payment'
    LOGISTICS_DELIVERY = 'delivery_park_b2b_logistics_payment'
    DOOH_FIX = 'dooh_fix'
    DOOH_RENT = 'dooh_rent'
    DOOH_BONUS = 'dooh_bonus'
    PVZ_REWARD = 'pvz_reward'
    PVZ_DROPOFF = 'pvz_dropoff'
    PVZ_BRANDED_DECORATION = 'pvz_branded_decoration'
    PRACTICUM_FLOW_1 = 'practicum_flow_1'
    PRACTICUM_FLOW_2 = 'practicum_flow_2'
    PRACTICUM_1 = 'practicum_1'
    PRACTICUM_2 = 'practicum_2'
    PRACTICUM_MATH_MID_1 = 'practicum_math_kids_1'
    PRACTICUM_MATH_MID_2 = 'practicum_math_kids_2'
    ACC_SORTING_REWARD = 'acc_sorting_reward'
    ACC_STORING_REWARD = 'acc_storing_reward'
    ACC_SORTING_RETURN_REWARD = 'acc_sorting_return_reward'
    ACC_STORING_RETURN_REWARD = 'acc_storing_return_reward'
    ACC_CAR_DELIVERY = 'acc_car_delivery'
    ACC_TRUCK_DELIVERY = 'acc_truck_delivery'
    ACC_LOAD_UNLOAD = 'acc_load_unload'
    BNPL = 'bnpl'
    ZAXI_SELFEMPLOYED = 'zapravki_smz'
    NEWS_PAYMENT = 'news_payment'
    ACC_YA_WITHDRAW = 'acc_ya_withdraw'
    ACC_SUBSIDY = 'acc_subsidy'
    ACC_DELIVERY_SUBSIDY = 'acc_delivery_subsidy'
    PAY_YA_WITHDRAW = 'pay_ya_withdraw'
    PAY_SUBSIDY = 'pay_subsidy'
    PAY_DELIVERY_SUBSIDY = 'pay_delivery_subsidy'
    ACC_SORTING_REWARD = 'acc_sorting_reward'
    ACC_SORTING_RETURN_REWARD = 'acc_sorting_return_reward'
    ACC_STORING_RETURN_REWARD = 'acc_storing_return_reward'
    ACC_CAR_DELIVERY = 'acc_car_delivery'
    ACC_TRUCK_DELIVERY = 'acc_truck_delivery'
    ACC_LOAD_UNLOAD = 'acc_load_unload'


class FoodProductType(object):
    GOODS = 'goods'
    MP_DELIVERY = 'mp_delivery'
    NATIVE_DELIVERY = 'native_delivery'
    SUBSIDY = 'subsidy'
    PICKUP = 'pickup'
    RETAIL = 'retail'
    TP_ORDER_PROCESSING = 'third_party_order_processing'


class TransactionType(utils.ConstantsContainer):
    _TransactionType = namedtuple('_TransactionType', 'name sign')
    constant_type = _TransactionType

    PAYMENT = _TransactionType('payment', 1)
    REFUND = _TransactionType('refund', -1)


class PartnerPaymentType(object):
    WALLET = 'wallet'
    EMAIL = 'email'
    SCOUT = 'scout'
    SCOUT_SZ = 'scout_sz'
    SVO = 'taxi_stand_svo'


class Pages(utils.ConstantsContainer):
    Page = namedtuple('Page', 'id, desc, payment_type')

    constant_type = Page

    DMP = Page(id=10008, desc=u'Услуги DMP', payment_type=None)

    COROBA = Page(id=10500, desc=u'Яндекс Такси. Фиксированное вознаграждение', payment_type=None)

    SUBSIDY = Page(id=10800, desc=u'Яндекс Такси. Субсидии водителям', payment_type=PaymentType.SUBSIDY)
    COUPON = Page(id=10700, desc=u'Яндекс Такси. Компенсации водителям', payment_type=PaymentType.COUPON)
    CARGO_COUPON = Page(id=10702, desc=u'Промокоды Яндекс.Такси (support coupons)', payment_type=PaymentType.CARGO_COUPON)
    DELIVERY_COUPON = Page(id=10703, desc=u'Промокоды Яндекс.Доставка (support coupons)', payment_type=PaymentType.DELIVERY_COUPON)
    BRANDING_SUBSIDY = Page(id=10803, desc=u'Branding subsidy', payment_type=PaymentType.BRANDING_SUBSIDY)
    GUARANTEE_FEE = Page(id=10804, desc=u'Guarantee fee', payment_type=PaymentType.GUARANTEE_FEE)
    TRIP_BONUS = Page(id=10805, desc=u'Trip bonus', payment_type=PaymentType.TRIP_BONUS)
    PERSONAL_BONUS = Page(id=10806, desc=u'Personnel bonus', payment_type=PaymentType.PERSONNEL_BONUS)
    DISCOUNT_TAXI = Page(id=10807, desc=u'Discount taxi', payment_type=PaymentType.DISCOUNT_TAXI)
    SUPPORT_COUPON = Page(id=10808, desc=u'Support coupon', payment_type=PaymentType.SUPPORT_COUPON)
    BOOKING_SUBSIDY = Page(id=10809, desc=u'Booking subsidy', payment_type=PaymentType.BOOKING_SUBSIDY)
    SCOUTS = Page(id=11003, desc=u'Яндекс.Такси: Скауты. Начисления', payment_type=PaymentType.SCOUT)
    SCOUTS_SZ = Page(id=11004, desc=u'Яндекс.Такси: Скауты. Начисления', payment_type=PaymentType.SCOUT_SZ)
    SCOUT_CARGO_SUBSIDY = Page(id=11009, desc=u'Яндекс.Такси Скауты – Субсидии', payment_type=PaymentType.SCOUT_CARGO_SUBSIDY)
    SCOUT_CARGO_SZ_SUBSIDY = Page(id=11010, desc=u'Яндекс.Такси Скауты,самозанятые – Субсидии', payment_type=PaymentType.SCOUT_CARGO_SZ_SUBSIDY)
    REFUELLER = Page(id=11005, desc=u'Драйв: Выплаты заправщикам', payment_type=PaymentType.DRIVE_FUELLER)
    DRYCLEAN = Page(id=10811, desc=u'Яндекс Такси. Субсидии (химчистка)', payment_type=PaymentType.DRYCLEAN)
    CASHRUNNER = Page(id=10810, desc=u'Яндекс Такси. Субсидии (неоплаченный заказ)',
                      payment_type=PaymentType.CASHRUNNER)
    BUSES_SUBSIDY = Page(id=10802, desc=u'Яндекс Автобусы. Субсидии', payment_type=PaymentType.SUBSIDY)
    CARGO_SUBSIDY = Page(id=10812, desc=u'Субсидии.Яндекс Такси (booking)', payment_type=PaymentType.CARGO_SUBSIDY)
    DELIVERY_SUBSIDY = Page(id=10813, desc=u'Субсидии.Яндекс Доставка (booking)', payment_type=PaymentType.DELIVERY_SUBSIDY)

    CORP_TAXI = Page(id=10600, desc=u'Яндекс Корпоративное Такси',
                     payment_type=PaymentType.CASH)  # может быть не только CASH в реальности
    CORP_TAXI_PARTNERS = Page(id=10600, desc=u'Яндекс Корпоративное Такси',
                              payment_type=PaymentType.CORP_TAXI_PARTNER_TRIP_PAYMENT)
    CORP_TAXI_CARGO = Page(id=10601, desc=u'Корпоративные продажи', payment_type=PaymentType.CORP_TAXI_CARGO)
    CORP_TAXI_DELIVERY = Page(id=10602, desc=u'Доставка Корпоративные продажи', payment_type=PaymentType.CORP_TAXI_DELIVERY)

    LOGISTICS_CARGO = Page(id=71901, desc=u'Грузоперезовки корпклиенты Логистика', payment_type=PaymentType.LOGISTICS_CARGO)
    LOGISTICS_DELIVERY = Page(id=71902, desc=u'Доставка корпоративные продажи Логистика', payment_type=PaymentType.LOGISTICS_DELIVERY)

    TELEMED_COUPON = Page(id=10701, desc=u'Яндекс Телемедицина. Компенсации', payment_type=None)

    MARKETPLACE = Page(id=10900, desc=u'Маркет. Маркетинговые услуги', payment_type=None)
    BLUEMARKETSUBSIDY = Page(id=10901, desc=u'Синий маркет. Субсидии', payment_type=PaymentType.SUBSIDY)
    BLUEMARKET_ACC_SUBSIDY = Page(id=10901, desc=u'Синий маркет. Субсидии (УВ)', payment_type=PaymentType.ACC_SUBSIDY)
    BLUEMARKET_ACC_PLUS = Page(id=60902, desc=u'Синий маркет. Плюс 2.0 (УВ)', payment_type=PaymentType.ACC_YA_WITHDRAW)
    YANDEX_ACCOUNT_WITHDRAW = Page(id=60902, desc=u'Синий маркет. Плюс 2.0', payment_type=PaymentType.YANDEX_ACCOUNT_WITHDRAW)
    REDMARKET = Page(id=10902, desc=u'Красный маркет. Субсидии', payment_type=None)

    ZEN = Page(id=11000, desc=u'Дзен. Начисления', payment_type=None)
    ZEN_PAYMENTS = Page(id=11001, desc=u'Дзен. Выплаты', payment_type=None)
    ZEN_IP_PAYMENTS = Page(id=69601, desc=u'Дзен: Выплаты ИП', payment_type=None)
    ZEN_UR_ORG_PAYMENTS = Page(id=69501, desc=u'Дзен: Выплаты ООО', payment_type=None)

    CLOUD_MARKETPLACE = Page(id=11101, desc=u'Маркетплейс Яндекс.Облако', payment_type=None)
    CLOUD_MARKETPLACE_SAG = Page(id=11102, desc=u'Маркетплейс Яндекс.Облако для SAG', payment_type=None)

    SPASIBO = Page(id=60901, desc=u'Синий маркет. Спасибо', payment_type=PaymentType.SPASIBO)

    ADDAPTER2 = Page(id=63001, desc=u'Мотивация продавцов: установки', payment_type=None)

    FOOD_SUBSIDY = Page(id=11006, desc=u'Еда: Курьеры (выплаты)', payment_type=PaymentType.SUBSIDY)
    FOOD_COUPON = Page(id=100106, desc=u'Еда: Курьеры (выплаты), купоны', payment_type=PaymentType.COUPON)
    CORP_FOOD_SUBSIDY = Page(id=11011, desc=u'Корпоративная Еда: Курьеры (выплаты)', payment_type=PaymentType.CORP_SUBSIDY)
    CORP_FOOD_COUPON = Page(id=100109, desc=u'Корпоративная Еда: Курьеры (выплаты), купоны', payment_type=PaymentType.CORP_COUPON)

    LAVKA_SUBSIDY = Page(id=66401, desc=u'Лавка: Курьеры (выплаты)', payment_type=PaymentType.GROCERY_COURIER_DELIVERY)
    LAVKA_COUPON = Page(id=66402, desc=u'Лавка: Курьеры (выплаты), купоны', payment_type=PaymentType.GROCERY_COURIER_COUPON)

    BUG_BOUNTY = Page(id=20701, desc=u'Bug Bounty, вознаграждение', payment_type=None)

    PARTNERS_LEARNING_CENTER = Page(id=11012, desc=u'Такси: Центр Обучения Водителей', payment_type=PaymentType.PARTNERS_LEARNING_CENTER)
    PARTNERS_MOTIVATION_PROGRAM = Page(id=11013, desc=u'Такси: Мотивационные Программы', payment_type=PaymentType.PARTNERS_MOTIVATION_PROGRAM)

    INGAME_PURCHASES = Page(id=67701, desc=u'Внутриигровые покупки (выплаты)', payment_type=None)
    ADVERTISEMENT = Page(id=113201, desc=u'Дополнительные технические услуги на Платформе игр Яндекса', payment_type=None)

    CLOUD_REFERAL = Page(id=69301, desc=u'Облака: реферальная программа (выплаты)', payment_type=PaymentType.REWARD)

    DOOH_FIX = Page(id=72401, desc=u'Аренда крыши', payment_type=PaymentType.DOOH_FIX)
    DOOH_RENT = Page(id=72402, desc=u'Компенсация за аренду короба', payment_type=PaymentType.DOOH_RENT)
    DOOH_BONUS = Page(id=72403, desc=u'Бонусы', payment_type=PaymentType.DOOH_BONUS)

    PVZ_REWARD = Page(id=72501, desc=u'Выдача Отправлений', payment_type=PaymentType.PVZ_REWARD)
    PVZ_DROPOFF = Page(id=72503, desc=u'Прием и последующая передача Отправлений в СД', payment_type=PaymentType.PVZ_DROPOFF)
    PVZ_BRANDED_DECORATION = Page(id=72506, desc=u'Услуга по оформлению пунктов выдачи', payment_type=PaymentType.PVZ_BRANDED_DECORATION)

    PRACTICUM_FLOW_1 = Page(id=1041001, desc=u'PRKT ENG PR НМА Реклама и ролики', payment_type=PaymentType.PRACTICUM_FLOW_1)
    PRACTICUM_FLOW_2 = Page(id=1041002, desc=u'Flow PR Услуги PR - агентств', payment_type=PaymentType.PRACTICUM_FLOW_2)
    PRACTICUM_1 = Page(id=1041008, desc=u'PRKT 1 DA 1.5/2 RUS НМА Аутсорс', payment_type=PaymentType.PRACTICUM_1)
    PRACTICUM_2 = Page(id=1041009, desc=u'PRKT 1 DA 1.5/2 RUS НМА Аутсорс Докап-ия', payment_type=PaymentType.PRACTICUM_2)
    PRACTICUM_MATH_MID_1 = Page(id=1041295, desc=u'Math kids PR НМА Реклама и ролики', payment_type=PaymentType.PRACTICUM_MATH_MID_1)
    PRACTICUM_MATH_MID_2 = Page(id=1041296, desc=u'Math kids PR Услуги PR - агентств', payment_type=PaymentType.PRACTICUM_MATH_MID_1)

    ACC_SORTING_REWARD = Page(id=106001, desc=u'Сортировка отправлений', payment_type=PaymentType.ACC_SORTING_REWARD)
    ACC_SORTING_RETURN_REWARD = Page(id=106002, desc=u'Сортировка возвратных отправлений', payment_type=PaymentType.ACC_SORTING_RETURN_REWARD)
    ACC_STORING_RETURN_REWARD = Page(id=106003, desc=u'Хранение возвратный отправлений', payment_type=PaymentType.ACC_STORING_RETURN_REWARD)
    ACC_STORING_REWARD = Page(id=106004, desc=u'Хранение отправлений', payment_type=PaymentType.ACC_STORING_REWARD)

    ACC_CAR_DELIVERY = Page(id=110001, desc=u'Доставка легковая', payment_type=PaymentType.ACC_CAR_DELIVERY)
    ACC_TRUCK_DELIVERY = Page(id=110002, desc=u'Доставка грузовая', payment_type=PaymentType.ACC_TRUCK_DELIVERY)
    ACC_LOAD_UNLOAD = Page(id=110003, desc=u'Погрузка-Разгрузка', payment_type=PaymentType.ACC_LOAD_UNLOAD)

    ZAXI_SELFEMPLOYED = Page(id=112001, desc=u'Услуги по поддержке качества и доступности Сервиса', payment_type=PaymentType.ZAXI_SELFEMPLOYED)

    NEWS_PAYMENT = Page(id=112701, desc=u'Яндекс.Новости Дайджест', payment_type=PaymentType.NEWS_PAYMENT)


class InvoiceType(object):
    PREPAYMENT = 'prepayment'
    OVERDRAFT = 'overdraft'
    FICTIVE = 'fictive'
    REPAYMENT = 'repayment'
    PERSONAL_ACCOUNT = 'personal_account'
    FICTIVE_PERSONAL_ACCOUNT = 'fictive_personal_account'
    Y_INVOICE = 'y_invoice'
    CHARGE_NOTE = 'charge_note'


class ActType(object):
    GENERIC = 'generic'
    INTERNAL = 'internal'


class Export(object):
    class Classname(object):
        CLIENT = 'Client'
        PERSON = 'Person'
        PRODUCT = 'Product'
        CONTRACT = 'Contract'
        CONTRACT_COLLATERAL = 'ContractCollateral'
        INVOICE = 'Invoice'
        ACT = 'Act'
        TRANSACTION = 'ThirdPartyTransaction'
        CORRECTION = 'ThirdPartyCorrection'
        PAYMENT = 'Payment'
        MANAGER = 'Manager'
        ZEN_PAYMENT = 'ZenPayment'
        OEBS_CPF = 'OebsCashPaymentFact'
        BALALAYKA_PAYMENT = 'BalalaykaPayment'
        SIDE_PAYMENT = 'SidePayment'
        PARTNER_COMPLETIONS_RESOURCE = 'PartnerCompletionsResource'
        INVOICE_REFUND = 'InvoiceRefund'

    class Type(object):
        OEBS = 'OEBS'
        OEBS_API = 'OEBS_API'
        OVERDRAFT = 'OVERDRAFT'
        MIGRATE_TO_CURRENCY = 'MIGRATE_TO_CURRENCY'
        UA_TRANSFER = 'UA_TRANSFER'
        CONTRACT_NOTIFY = 'CONTRACT_NOTIFY'
        PROCESS_COMPLETION = 'PROCESS_COMPLETION'
        MONTH_PROC = 'MONTH_PROC'
        THIRDPARTY_TRANS = 'THIRDPARTY_TRANS'
        PROCESS_PAYMENTS = 'PROCESS_PAYMENTS'
        PARTNER_COMPL = 'PARTNER_COMPL'
        TRUST_API = 'TRUST_API'
        PARTNER_FAST_BALANCE = 'PARTNER_FAST_BALANCE'


class ExportNG(object):
    class Type(object):
        LOGBROKER_ZAXI = 'LOGBROKER-ZAXI'
        LOGBROKER_CONTRACT = 'LOGBROKER-CONTRACT'
        LOGBROKER_PLACE = 'LOGBROKER-PLACE'
        LOGBROKER_CONSUME = 'LOGBROKER-CONSUME'
        LOGBROKER_PARTNER_FAST_BALANCE = 'LOGBROKER-PARTNER-FAST-BALANCE'


class PaystepPaymentResultText(object):
    SUCCESS_PAYMENT = u"Ваш платеж успешно завершен!"
    DECLINED_BY_BANK = u"Операция отклонена Вашим банком."
    PAYMENTS_TEMPORARY_UNAVAILABLE = u"Извините. Проведение платежей временно недоступно, попробуйте позже."
    CARD_LIMITATIONS = u"Оплата услуг компании Яндекс по Вашей карте запрещена. Обратитесь, пожалуйста, в службу поддержки."
    BAD_TRANSACTION = u"Транзакция отклонена по причине того, что размер платежа превысил установленные лимиты."
    INVALID_CARD_PARAMS = u"Указаны неправильные параметры карты"
    NOT_ENOUGH_FUNDS = u"На Вашей карте недостаточно денег, чтобы осуществить данный платеж."
    TRANSACTIONS_LIMIT_EXCEEDED = u"Превышено допустимое количество транзакций"
    TRANSACTION_DECLINED = u"Транзакция отклонена"


class Processings(object):
    ALPHA = 'ALPHA'
    PRIVAT = 'PRIVAT'
    ING = 'ING'
    SAFERPAY = 'SAFERPAY'
    BILDERLINGS = 'BLIDERLINGS'
    PRIOR = 'PRIOR'
    TRUST = 'TRUST'


class Domains(object):
    RU = 'ru'
    COM = 'com'
    COM_TR = 'com.tr'
    KZ = 'kz'
    BY = 'by'


class FirmTaxi(utils.ConstantsContainer):
    Taxi = namedtuple('Taxi', 'id,country,currency,person_type')
    constant_type = Taxi

    YANDEX_TAXI = Taxi(id=Firms.TAXI_13.id, country=Regions.RU.id, currency=Currencies.RUB, person_type='ur')
    YANDEX_TAXI_UKRAINE = Taxi(id=Firms.TAXI_UA_23.id, country=Regions.UA.id, currency=Currencies.UAH, person_type='ua')
    YANDEX_TAXI_ARMENIA = Taxi(id=Firms.TAXI_AM_26.id, country=Regions.ARM.id,
                               currency=Currencies.AMD, person_type='am_jp')
    YANDEX_TAXI_KAZ = Taxi(id=Firms.TAXI_KAZ_24.id, country=Regions.KZ.id, currency=Currencies.KZT, person_type='kzu')
    YANDEX_TAXI_BV = Taxi(id=Firms.TAXI_BV_22.id, country=Regions.KZ.id, currency=Currencies.USD, person_type='eu_yt')


class OfferConfirmationType(Enum):
    NO = 'no'
    MIN_PAYMENT = 'min-payment'


class TaxiOrderType(object):
    commission = 'order'
    childchair = 'childchair'
    commission_correction = 'commission_correction'
    driver_workshift = 'driver_workshift'
    hiring_with_car = 'hiring_with_car'
    subsidy = 'subvention'
    marketplace_advert_call = 'marketplace_advert_call'
    subsidy_tlog = 'subvention'
    promocode_tlog = 'coupon'
    cargo_order = 'cargo_order'
    cargo_driver_workshift = 'cargo_driver_workshift'
    cargo_hiring_with_car = 'cargo_hiring_with_car'
    delivery_order = 'delivery_order'
    delivery_driver_workshift = 'delivery_driver_workshift'
    delivery_hiring_with_car = 'delivery_hiring_with_car'


class BlueMarketOrderType(object):
    fee = 'fee'
    ff_processing = 'ff_processing'
    ff_storage_billing = 'ff_storage_billing'
    ff_surplus_supply = 'ff_surplus_supply'
    ff_withdraw = 'ff_withdraw'
    ff_xdoc_supply = 'ff_xdoc_supply'
    sorting = 'sorting'
    installment = 'installment'
    global_delivery = 'global_delivery'
    global_agency_commission = 'global_agency_commission'
    global_fee = 'global_fee'


class BlueMarketingServicesOrderType(object):
    marketing_promo_for_sales = 'marketing_promo_for_sales'
    marketing_promo_tv = 'marketing_promo_tv'
    marketing_promo_web = 'marketing_promo_web'
    marketing_promo_mailing = 'marketing_promo_mailing'
    marketing_promo_yandex_market = 'marketing_promo_yandex_market'


class CorpTaxiOrderType(utils.ConstantsContainer):
    constant_type = str

    commission = 'client_b2b_trip_payment'
    cargo_commission = 'cargo_client_b2b_trip_payment'
    delivery_commission = 'delivery_client_b2b_trip_payment'

    # 697 сервис
    dispatching_commission = 'b2b_trips_access_payment'


class LogisticsClientsOrderType(utils.ConstantsContainer):
    constant_type = str

    delivery = 'delivery_client_b2b_logistics_payment'
    cargo = 'cargo_client_b2b_logistics_payment'
    revenue = 'b2b_agent_logistics_revenue'


class CorpEdaOrderType(utils.ConstantsContainer):
    constant_type = str

    MAIN = 'food_payment'


class DriveB2BOrderType(utils.ConstantsContainer):
    constant_type = str

    MAIN = 'client_b2b_drive_payment'


class K50OrderType(utils.ConstantsContainer):
    constant_type = str

    MAIN = 'main'
    GENERATOR = 'k50_generator'
    OPTIMIZATOR = 'k50_optimisator'
    TRACKER = 'k50_tracker'
    BI = 'k50_bi'

class PlaceType(utils.ConstantsContainer):
    constant_type = int

    RSYA = 3
    DISTRIBUTION = 8
    ZEN = 20


class CollateralPrintFormType(utils.ConstantsContainer):
    PrintFormType = namedtuple('PrintFormType', 'id,name,prefix')
    constant_type = PrintFormType

    BILATERAL = PrintFormType(0, u'Двустороннее ДС', u'')
    UNILATERAL = PrintFormType(1, u'Одностороннее ДС (Уведомление)', u'У')
    ANNEX = PrintFormType(2, u'Приложение', u'П')
    NONE = PrintFormType(3, u'Без печатной формы', u'Ф')


class OEBSOperationType(utils.ConstantsContainer):
    constant_type = str

    ACTIVITY = 'ACTIVITY'
    ONLINE = 'ONLINE'
    INSERT = 'INSERT'
    CORRECTION_NETTING = 'CORRECTION_NETTING'
    INSERT_NETTING = 'INSERT_YA_NETTING'
    INSERT_FUEL_HOLD = 'INSERT_FUEL_HOLD'
    INSERT_FUEL_FACT = 'INSERT_FUEL_FACT'
    INSERT_FUEL_HOLD_RETURN = 'INSERT_FUEL_HOLD_RETURN'
    NONE = None
    SF_AVANS = 'SF_AVANS'


class OEBSSourceType(utils.ConstantsContainer):
    constant_type = str

    LOST_SUMS = 'Невыясненные суммы'


class ClientCategories(utils.ConstantsContainer):
    constant_type = int

    AGENCY = 1
    CLIENT = 0


class ContractAttributeType(utils.ConstantsContainer):
    constant_type = str

    STR = 'value_str'
    NUM = 'value_num'
    DT = 'value_dt'
    CLOB = 'value_clob'


class ServiceCode(utils.ConstantsContainer):
    constant_type = str

    AGENT_REWARD = 'AGENT_REWARD'
    DEPOSITION = 'DEPOSITION'
    YANDEX_SERVICE = 'YANDEX_SERVICE'
    YANDEX_SERVICE_WO_VAT = 'YANDEX_SERVICE_WO_VAT'


# дублируем ContractCommissionType ?
class ContractType(utils.ConstantsContainer):
    constant_type = int

    NOT_AGENCY = 0
    LICENSE = 72


class DistributionContractType(utils.ConstantsContainer):
    constant_type = int

    REVSHARE = 1
    DOWNLOADS_INSTALLS = 2
    UNIVERSAL = 3
    AGILE = 4
    GROUP = 5
    OFFER = 6
    GROUP_OFFER = 7
    CHILD_OFFER = 8


class PromocodeClass(utils.ConstantsContainer):
    FIXED_SUM = 'FixedSumBonusPromoCodeGroup'
    FIXED_QTY = 'FixedQtyBonusPromoCodeGroup'
    SCALE = 'ScaleAmountsBonusPromoCodeGroup'
    FIXED_DISCOUNT = 'FixedDiscountPromoCodeGroup'
    LEGACY_PROMO = 'LegacyPromoCodeGroup'
    ACT_BONUS = 'ActBonusPromoCodeGroup'


class CompletionSource(utils.ConstantsContainer):
    BUSES2 = 'buses2'
    ADDAPPTER2 = 'addappter2'
    AVIA_RS = 'avia_rs'
    CLOUD = 'cloud'


class OrderIDLowerBounds(utils.ConstantsContainer):
    LOWER_BOUND_S_REQUEST_ORDER_ID = 1324836609


class CountryRegion(utils.ConstantsContainer):
    RUS = u'77000000000'


class ServiceFee(utils.ConstantsContainer):
    SERVICE_FEE_NONE = None
    SERVICE_FEE_1 = '1'
    SERVICE_FEE_2 = '2'


class YTSourceName(utils.ConstantsContainer):
    TAXI_COMPLETIONS_SOURCE_NAME = 'taxi_aggr_tlog'
    TAXI_SUBVENTIONS_SOURCE_NAME = 'taxi_subvention'
    MARKET_SUBVENTIONS_SOURCE_NAME = 'market_subvention'
    BLUE_MARKET_PAYMENT = 'blue_market_payment'
    BLUE_MARKET_SUBSIDY = 'blue_market_subsidy'
    MARKET_COURIER_EXPENSES_SOURCE_NAME = 'market_courier_expenses'


class YTDefaultPath(utils.ConstantsContainer):
    TAXI_SUBVENTIONS_YT_ROOT_PATH = '//home/balance-test/test/tlog/subventions/'
    TAXI_COMPLETIONS_YT_ROOT_PATH = '//home/balance-test/test/tlog/completions/'
    MARKET_SUBVENTIONS_YT_ROOT_PATH = '//home/balance-test/test/tlog/market_subventions/'
    BLUE_MARKET_PAYMENT_YT_ROOT_PATH = '//home/balance-test/test/tlog/blue_market_payment/'
    BLUE_MARKET_SUBSIDY_YT_ROOT_PATH = '//home/balance-test/test/tlog/blue_market_subsidy/'
    MARKET_COURIER_EXPENSES_YT_ROOT_PATH = '//home/balance-test/test/tlog/market_courier_expenses/'


class StagerProject(utils.ConstantsContainer):
    constant_type = str

    AVIA = 'avia'
    TAXI = 'taxi'
    CLOUD = 'cloud'
    HOSTING_SERVICE = 'hosting_service'
    DRIVE = 'drive'
    EDA = 'eda'
    BLUE_MARKET = 'blue_market'
    BLUE_MARKETING_SERVICES = 'blue_marketing_services'


class YtCluster(utils.ConstantsContainer):
    constant_type = str

    HAHN = 'hahn'
    ARNOLD = 'arnold'
    HUME = 'hume'


class TvmClientIds(object):
    # В идеале, все тесты должны ходить из под этого приложения
    # (то есть все src должны указывать на него).
    BALANCE_TESTS = 2000013
    YB_MEDIUM = 2000601
    MDS_PROXY = 2001900
