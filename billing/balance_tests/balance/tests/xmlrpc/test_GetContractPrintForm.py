# coding: utf-8
__author__ = 'a-vasin'

import xmlrpclib

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, not_none, equal_to

import balance.balance_api as api
import balance.balance_steps as steps
from balance.features import Features
from btestlib import reporter, passport_steps
from btestlib.data.partner_contexts import *
from btestlib.constants import DistributionContractType, PersonTypes

pytestmark = [
    reporter.feature(Features.CONTRACT, Features.COLLATERAL, Features.INVOICE_PRINT_FORM),
    pytest.mark.tickets('BALANCE-27910')
]


# Описания и сами шаблоны лежат тут: https://wiki.yandex-team.ru/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/
# Что увидеть описание шаблона нужно удалить его номер из ссылки и дописать начало ссылки https://wiki.yandex-team.ru
# У нас названия шаблонов сформированы так: <название станицы>_<номер шаблона на странице>
# Комментарии у шаблонов - номер и название шаблона из документации баланса
# Документация в балансе: https://wiki.yandex-team.ru/balance/printdocuments/print-form-templates/

class ContractTemplate(object):
    TAXI_CORP_CLIENT_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/1/'
    TAXI_CORP_CLIENT_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/2/'
    TAXI_CORP_CLIENT_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/4/'

    # 21 Оказание рекламных услуг
    TAXI_MARKETING_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-ob-okazanii-reklamnyx-uslug/Dogovor-ob-okazanii-reklamnyx-uslug/'

    # 22 Такси/Расш. сотр.
    TAXI_EXTCOOP_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-rasshirennogo-sotrudnichestva/2/'

    # 23 Корп. такси/Парт. схема
    TAXI_CORP_PARTNER_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Partnerskaja-sxema/1/'

    TAXI_GEO_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Gruzija/1/'
    TAXI_GEO_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Gruzija/3/'

    TAXI_ARM_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Armenija/1/'
    TAXI_ARM_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Armenija/Dogovor-na-oplatu-kartojjagentskijj/'
    TAXI_ARM_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Armenija/Dogovor-na-marketingovye-uslugiposle-avtomatizacii/'

    TAXI_KAZ_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/1/'
    TAXI_KAZ_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/2/'
    TAXI_KAZ_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/3/'

    TAXI_MDA_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Moldavija/1/'
    TAXI_MDA_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Moldavija/2/'

    TAXI_KGZ_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kirgizija/1/'
    TAXI_KGZ_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kirgizija/2/'

    TAXI_BLR_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Belorussija/1/'
    TAXI_BLR_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Belorussija/2/'

    # 17 Такси нерез./Латвия/Доступ
    TAXI_LVA_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Latvija/1/'
    # 18 Такси нерез./Латвия/Маркетинговые услуги
    TAXI_LVA_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Latvija/2/'

    # 19 Такси нерез./Узбекистан/Доступ
    TAXI_UZB_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uzbekistan/1/'
    # 20 Такси нерез./Узбекистан/Маркетинговые услуги
    TAXI_UZB_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uzbekistan/2/'

    # Такси нерез./Азербайждан/Доступ
    TAXI_AZE_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uber-Azerbajjdzhan-/1/'
    # Такси нерез./Азербайждан/Маркетинговые услуги
    TAXI_AZE_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uber-Azerbajjdzhan-/2/'

    TAXI_EU_MLU_ROMANIA_OFFER = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Rumynija/-Dogovor-na-dostup-k-servisu-i-na-oplatu-kartojj-Rumynija/'
    TAXI_EU_MLU_ROMANIA_SPENDABLE = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Rumynija/Informacionno-reklamnyjj-avtomatizirovannyjj-dogovor-Rumynija/'

    TAXI_AFRICA_MLU_GHANA_OFFER = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Africa-B.V.-Gana/Dogovor-na-dostup-k-servisu-i-na-oplatu-kartojj-Gana/'
    TAXI_AFRICA_MLU_GHANA_SPENDABLE = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Africa-B.V.-Gana/Informacionno-reklamnyjj-avtomatizirovannyjj-dogovor-Gana/'

    TAXI_AFRICA_MLU_SOUTH_AFRICA_OFFER = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Africa-B.V.-JuAR/Dogovor-na-dostup-k-servisu-i-na-oplatu-kartojj/'
    TAXI_AFRICA_MLU_SOUTH_AFRICA_SPENDABLE = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Africa-B.V.-JuAR/Informacionno-reklamnyjj-avtomatizirovannyjj-dogovor/'

    TAXI_GO_ISRAEL_OFFER = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Yandex.Go-Israel-LTD-Izrail/Dogovor-na-dostup-k-servisu-i-na-oplatu-kartojj-Taksopark/'
    TAXI_GO_ISRAEL_SPENDABLE = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Yandex.Go-Israel-LTD-Izrail/Informacionno-reklamnyjj-avtomatizirovannyjj-dogovor-Taksopark/'
    TAXI_CORP_GO_ISRAEL_GENERAL = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Yandex.Go-Israel-LTD-Izrail/Dogovor-s-Korp.-klientom/'
    TAXI_CORP_GO_ISRAEL_SPENDABLE = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Yandex.Go-Israel-LTD-Izrail/Na-korp.-perevozki-Taksopark/'

    TAXI_CORP_POSTPAY_NEW_ITEM = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/Dogovor-s-korporativnym-klientom-novyjj-predmet/'
    TAXI_CORP_PREPAY_NEW_ITEM = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/Dogovor-s-korporativnym-klientom-predoplata-novyjj-predmet/'

    CLOUD_1 = '/sales/processing/Billing-agreements/OOO-Jandeks/Oblako/Dogovor-prjamojj/'
    CLOUD_ENTERPRISE = '/sales/processing/Billing-agreements/OOO-Jandeks/Oblako/Klientskijj-jenterprajjz/'
    CLOUD_WARGAME = '/sales/processing/Billing-agreements/OOO-Jandeks/Oblako/Prjamojj-Partner-Vargejjm/'
    CLOUD_YT = '/sales/processing/Billing-agreements/OOO-Jandeks/Oblako/Nerezident/'

    MEDIASERVICES_AFISHA_SINGLE = '/sales/processing/Billing-agreements/OOO-Jandeks.Mediaservisy/BILETY/Dogovor/Promokody/Afisha/Razovyjj/'
    MEDIASERVICES_AFISHA_MULTI = '/sales/processing/Billing-agreements/OOO-Jandeks.Mediaservisy/BILETY/Dogovor/Promokody/Afisha/Mnogorazovyjj/'
    MEDIASERVICES_MUSIC = '/sales/processing/Billing-agreements/OOO-Jandeks.Mediaservisy/Ja.Muzyka/Dogovor-Muzyka/-Promokody-RF/'

    FOOD_CORP_POSTPAY = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Korp.-Eda/Dogovor-s-klientom-na-korp.-edupostoplata/'
    FOOD_CORP_PREPAY = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Korp.-Eda/Dogovor-s-klientom-na-korp.-edupredoplata/'

    FLEXIBLE_YANDEX = '/sales/processing/Billing-agreements/OOO-Jandeks/Distribucija-GD-Jandeks/'
    FLEXIBLE_SERVICES_AG = '/sales/processing/Billing-agreements/OOO-Jandeks/Distribucija-GD-Services-AG/'

    UNIVERSAL_YANDEX = '/sales/processing/Billing-agreements/OOO-Jandeks/Distribucija-UDD-Jandeks/'
    UNIVERSAL_SERVICES_AG = '/sales/processing/Billing-agreements/OOO-Jandeks/UDD-Services-AG/'


class CollateralTemplate(object):
    # 1 Расторжение Такси (согл.)
    # для этого шаблона кроме оферты, должен быть еще расходный договор на 135 сервис на этого же клиента
    TAXI_COL_1 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dopolnitelnye-soglashenie/1/'
    # 2 Расторжение Такси (увед.)
    TAXI_COL_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dopolnitelnye-soglashenie/2/'
    # 4 Скидка в рамках "Мой город" (описание тут https://wiki.yandex-team.ru/sales/processing/billing-agreements/bju-jandeks-taksi/dopolnitelnye-soglashenie/)
    TAXI_COL_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-ob-okazanii-reklamnyx-uslug/2.-Dopolnitelnoe-soglashenie-k-Dogovoru-ob-okazanii-uslug/'

    # 3 Расторжение Такси корп. клиент (увед.)
    TAXI_CORP_CLIENT_6 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/-Uvedomlenie-o-rastorzhenii-dogovora-s-korp.-klientom/'
    # 7 Продление договора с корп. клиентом
    TAXI_CORP_CLIENT_7 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/-DS-na-prodlenie-dogovora-s-korp.-klientom/'
    # 8 Изменение сроков оплаты
    TAXI_CORP_CLIENT_8 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/-DS-na-izmenenie-srokov-oplaty/'
    # 9 Расторжение Такси корп. клиент задолженность (согл.)
    TAXI_CORP_CLIENT_9 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/9.-Soglashenie-o-rastorzhenii-s-korp-klientom-klientskaja-sxema-zadolzhennost/'
    # ДС на новые услуги Грузовой и Курьер
    TAXI_CORP_CLIENT_DELIVERY = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-Korporativnoe-Taksi/Klientskaja-sxema/DS-na-novye-uslugi-Gruzovojj-i-Kurer/'

    # 5 Первое ДС к ДРС (без поручителей)
    TAXI_EXTCOOP_2_WO_WARR = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-rasshirennogo-sotrudnichestva/pervoe-ds-bez-poruch/'
    # 5 Первое ДС к ДРС (с поручителями)
    TAXI_EXTCOOP_2_WITH_WARR = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-rasshirennogo-sotrudnichestva/pervoe-ds-s-poruch/'
    # 6 Последующие ДС к ДРС (без поручителей)
    TAXI_EXTCOOP_3_WO_WARR = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-rasshirennogo-sotrudnichestva/vtoroe-ds-bez-poruch/'
    # 6 Последующие ДС к ДРС (с поручителями)
    TAXI_EXTCOOP_3_WITH_WARR = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Dogovor-rasshirennogo-sotrudnichestva/vtoroe-ds-s-poruch/'

    # 15 Такси нерез./Армения/Доступ - расторжение (согл.)
    TAXI_ARM_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Armenija/4/'
    # 16 Такси нерез./Армения/Агент. - расторжение (согл.)
    TAXI_ARM_5 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Armenija/5/'
    # 17 Такси нерез./Армения/Маркетинг. - расторжение (согл.)
    TAXI_ARM_6 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Armenija/6/'

    # 10 Такси нерез./Казахстан/Доступ - расторжение (согл.)
    TAXI_KAZ_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/4/'
    # 13 Такси нерез./Казахстан/Маркетинг. - расторжение (согл.)
    TAXI_KAZ_5 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/5/'
    # 11 Такси нерез./Казахстан/Агент. - расторжение
    TAXI_KAZ_6 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/6/'
    TAXI_KAZ_7 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/7/'
    TAXI_KAZ_8 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/8/'
    TAXI_KAZ_9 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kazaxstan/9/'

    # 18 Такси нерез./Грузия/Доступ - расторжение (согл.)
    TAXI_GEO_2 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Gruzija/2/'
    # 19 Такси нерез./Грузия/Маркетинг. - расторжение (согл.)
    TAXI_GEO_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Gruzija/4/'

    # 20 Такси нерез./Молдавия/Доступ - расторжение (согл.)
    TAXI_MDA_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Moldavija/3/'
    # 21 Такси нерез./Молдавия/Маркетинг. - расторжение (согл.)
    TAXI_MDA_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Moldavija/4/'

    # 22 Такси нерез./Киргизия/Доступ - расторжение (согл.)
    TAXI_KGZ_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kirgizija/3/'
    # 23 Такси нерез./Киргизия/Маркетинг. - расторжение (согл.)
    TAXI_KGZ_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Kirgizija/4/'

    # 24 Такси нерез./Беларусь/Доступ - расторжение (согл.)
    TAXI_BLR_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Belorussija/3/'
    # 25 Такси нерез./Беларусь/Маркетинг. - расторжение (согл.)
    TAXI_BLR_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Belorussija/4/'

    # 26 Такси нерез./Латвия/Доступ - расторжение (согл.)
    TAXI_LVA_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Latvija/3/'
    # 27 Такси нерез./Латвия/Маркетинг. - расторжение (согл.)
    TAXI_LVA_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Latvija/4/'

    # 28 Такси нерез./Узбекистан/Доступ - расторжение (согл.)
    TAXI_UZB_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uzbekistan/3/'
    # 29 Такси нерез./Узбекистан/Маркетинг. - расторжение (согл.)
    TAXI_UZB_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uzbekistan/4/'

    # Такcи нерез./Азербайджан/Доступ - расторжение (согл.)
    TAXI_AZE_3 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uber-Azerbajjdzhan-/3.Rastorzhenie-dogovora-na-dostup-k-servisu-i-na-oplatu-kartojj/'
    # Такcи нерез./Азербайджан/Маркетинг. - расторжение (согл.)
    TAXI_AZE_4 = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/Uber-Azerbajjdzhan-/rastorzhenie-avtomaticheskijj-dogovor-na-marketingovye-uslugi-/'

    TAXI_EU_MLU_ROMANIA_OFFER_TERMINATION = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Rumynija/Rastorzhenie-dogovora-na-dostup-k-servisu-Rumynija/'
    TAXI_EU_MLU_ROMANIA_SPENDABLE_OTHER = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Rumynija/Rastorzhenie-informacionno-reklamnogo-avtomatizirovannogo-dogovora-Rumynija/'

    TAXI_AFRICA_MLU_GHANA_OFFER_TERMINATION = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Africa-B.V.-Gana/-Rastorzhenie-dogovora-na-dostup-k-servisu-Gana/'
    TAXI_AFRICA_MLU_GHANA_SPENDABLE_OTHER = '/sales/processing/Billing-agreements/BJu-Jandeks-Taksi/Usluga-dostupa-k-Servisu-Jandeks-Taksinerezidenty/MLU-Africa-B.V.-Gana/Rastorzhenie-informacionno-reklamnogo-avtomatizirovannogo-dogovora-Gana/'

    MEDIASERVICES_OTHER = '/sales/processing/Billing-agreements/OOO-Jandeks.Mediaservisy/BILETY/2.-Prilozhenie-na-skidki-po-biletam/'
    MEDIASERVICES_PROMO_PLUS = '/sales/processing/billing-agreements/ooo-jandeks.mediaservisy/ja.muzyka/prilozhenie-dlja-promokodnyx-dogovorov-rf-pljus/'


START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
FINISH_DT = START_DT + relativedelta(months=6)
TODAY_ISO = utils.Date.nullify_time_of_date(datetime.now()).isoformat()


class PFMode(Enum):
    CONTRACT = "contract"
    COLLATERAL = "collateral"


class ContractPrintFormCases(Enum):
    TAXI_CORP_POST_1 = (
        ContractTemplate.TAXI_CORP_CLIENT_1,
        lambda: create_contract_corp_taxi(ContractTemplate.TAXI_CORP_CLIENT_1, FINISH_DT)
    )
    TAXI_CORP_POST_2 = (
        ContractTemplate.TAXI_CORP_CLIENT_4,
        lambda: create_contract_corp_taxi(ContractTemplate.TAXI_CORP_CLIENT_4, FINISH_DT)
    )
    TAXI_CORP_POST_NONE = (
        ContractTemplate.TAXI_CORP_CLIENT_1,
        lambda: create_contract_corp_taxi(None, FINISH_DT)
    )
    TAXI_CORP_POST_NEW_ITEM = (
        ContractTemplate.TAXI_CORP_POSTPAY_NEW_ITEM,
        lambda: create_contract_corp_taxi(ContractTemplate.TAXI_CORP_POSTPAY_NEW_ITEM, FINISH_DT)
    )
    TAXI_CORP_PRE = (
        ContractTemplate.TAXI_CORP_CLIENT_2,
        lambda: create_contract_corp_taxi(ContractTemplate.TAXI_CORP_CLIENT_2, FINISH_DT, is_postpay=0)
    )
    TAXI_CORP_PRE_NEW_ITEM = (
        ContractTemplate.TAXI_CORP_PREPAY_NEW_ITEM,
        lambda: create_contract_corp_taxi(ContractTemplate.TAXI_CORP_PREPAY_NEW_ITEM, FINISH_DT, is_postpay=0)
    )

    TAXI_EXTCOOP_1 = (
        ContractTemplate.TAXI_EXTCOOP_1,
        lambda: create_taxi_postpay_ext_coop()
    )

    # OFF: get_contract_print_form возвращает отличную от 200 и 404 ошибку
    # TAXI_CORP_PARTNER_1 = (
    #     ContractTemplate.TAXI_CORP_PARTNER_1,
    #     lambda: create_taxi_spendable_from_defaults(SpendableContractDefaults.TAXI_CORP)
    # )

    TAXI_GEO_PRE = (
        ContractTemplate.TAXI_GEO_1,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.GEO)
    )
    TAXI_GEO_SPENDABLE = (
        ContractTemplate.TAXI_GEO_3,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.GEO,
                                 ContractTemplate.TAXI_GEO_3)
    )

    # OFF: get_contract_print_form возвращает отличную от 200 и 404 ошибку
    # TAXI_ARM_PRE = (
    #     ContractTemplate.TAXI_ARM_1,
    #     lambda: create_taxi_prepay(Firms.TAXI_BV_22, Regions.ARM, PersonTypes.EU_YT)
    # )

    TAXI_ARM_POST = (
        ContractTemplate.TAXI_ARM_2,
        lambda: create_taxi_postpay(TAXI_ARM_CONTEXT)
    )

    # OFF: get_contract_print_form возвращает отличную от 200 и 404 ошибку
    # TAXI_ARM_SPENDABLE = (
    #     ContractTemplate.TAXI_ARM_3,
    #     lambda: create_taxi_spendable_donate(Firms.TAXI_BV_22, Regions.ARM, PersonTypes.EU_YT)
    # )

    # проверяются в test_linked_contracts
    # TAXI_KZ_PRE = (
    #     TAXI_KZ_PRE_TEMPLATE,
    #     lambda: create_taxi_prepay(Firms.TAXI_BV_22, Regions.KZ, PersonTypes.EU_YT)
    # )
    # TAXI_KZ_SPENDABLE = (
    #     TAXI_KZ_SPENDABLE_TEMPLATE,
    #     lambda: create_taxi_spendable(Firms.TAXI_BV_22, Regions.KZ, PersonTypes.EU_YT, TAXI_KZ_SPENDABLE_TEMPLATE)
    # )

    TAXI_KZ_POST = (
        ContractTemplate.TAXI_KAZ_2,
        lambda: create_taxi_postpay(TAXI_KZ_CONTEXT)
    )

    # OFF: get_contract_print_form возвращает отличную от 200 и 404 ошибку
    # TAXI_MD_PRE = (
    #     ContractTemplate.TAXI_MDA_1,
    #     lambda: create_taxi_prepay(Firms.TAXI_BV_22, Regions.MD, PersonTypes.EU_YT)
    # )

    # Использование этой фирмы в Молдавии запрещено https://st.yandex-team.ru/BALANCE-34661
    # TAXI_MD_SPENDABLE = (
    #     ContractTemplate.TAXI_MDA_2,
    #     lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.MD)
    # )

    TAXI_KGZ_PRE = (
        ContractTemplate.TAXI_KGZ_1,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.KGZ)
    )
    TAXI_KGZ_SPENDABLE = (
        ContractTemplate.TAXI_KGZ_2,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.KGZ)
    )

    # Использование этой фирмы в Белорусии запрещено https://st.yandex-team.ru/BALANCE-34661
    # TAXI_BY_PRE = (
    #     ContractTemplate.TAXI_BLR_1,
    #     lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.BY)
    # )
    # TAXI_BY_SPENDABLE = (
    #     ContractTemplate.TAXI_BLR_2,
    #     lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.BY)
    # )

    TAXI_LVA_1 = (
        ContractTemplate.TAXI_LVA_1,
        lambda: create_taxi_prepay(TAXI_BV_LAT_EUR_CONTEXT, Regions.LAT)
    )
    TAXI_LVA_2 = (
        ContractTemplate.TAXI_LVA_2,
        lambda: create_spendable(TAXI_BV_LAT_EUR_CONTEXT, TAXI_BV_LAT_EUR_CONTEXT_SPENDABLE, Regions.LAT)
    )

    TAXI_UZB_1 = (
        ContractTemplate.TAXI_UZB_1,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.UZB)
    )
    TAXI_UZB_2 = (
        ContractTemplate.TAXI_UZB_2,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.UZB)
    )

    TAXI_AZE_1 = (
        ContractTemplate.TAXI_AZE_1,
        lambda: create_taxi_prepay(TAXI_UBER_BV_AZN_USD_CONTEXT, Regions.AZ)
    )

    TAXI_AZE_2 = (
        ContractTemplate.TAXI_AZE_2,
        lambda: create_spendable(TAXI_UBER_BV_AZN_USD_CONTEXT, TAXI_UBER_BV_AZN_USD_CONTEXT_SPENDABLE, Regions.AZ)
    )

    TAXI_EU_MLU_RO_OFFER = (
        ContractTemplate.TAXI_EU_MLU_ROMANIA_OFFER,
        lambda: create_taxi_offer_prepay(TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT)
    )

    TAXI_EU_MLU_RO_SPENDABLE = (
        ContractTemplate.TAXI_EU_MLU_ROMANIA_SPENDABLE,
        lambda: create_spendable(TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE, Regions.RO)
    )

    TAXI_AFRICA_MLU_GHA_OFFER = (
        ContractTemplate.TAXI_AFRICA_MLU_GHANA_OFFER,
        lambda: create_taxi_offer_prepay(TAXI_GHANA_USD_CONTEXT)
    )

    TAXI_AFRICA_MLU_GHA_SPENDABLE = (
        ContractTemplate.TAXI_AFRICA_MLU_GHANA_SPENDABLE,
        lambda: create_spendable(TAXI_GHANA_USD_CONTEXT, TAXI_GHANA_USD_CONTEXT_SPENDABLE, Regions.GHA)
    )

    TAXI_AFRICA_MLU_ZA_OFFER = (
        ContractTemplate.TAXI_AFRICA_MLU_SOUTH_AFRICA_OFFER,
        lambda: create_taxi_offer_prepay(TAXI_ZA_USD_CONTEXT)
    )

    TAXI_AFRICA_MLU_ZA_SPENDABLE = (
        ContractTemplate.TAXI_AFRICA_MLU_SOUTH_AFRICA_SPENDABLE,
        lambda: create_spendable(TAXI_ZA_USD_CONTEXT, TAXI_ZA_USD_CONTEXT_SPENDABLE, Regions.ZA)
    )

    TAXI_GO_ISRAEL_OFFER = (
        ContractTemplate.TAXI_GO_ISRAEL_OFFER,
        lambda: create_taxi_offer_prepay(TAXI_ISRAEL_CONTEXT)
    )

    TAXI_GO_ISRAEL_SPENDABLE = (
        ContractTemplate.TAXI_GO_ISRAEL_SPENDABLE,
        lambda: create_spendable(TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE, Regions.ISR)
    )

    TAXI_CORP_GO_ISRAEL_GENERAL = (
        ContractTemplate.TAXI_CORP_GO_ISRAEL_GENERAL,
        lambda: create_contract_corp_taxi(context=TAXI_CORP_ISRAEL_CONTEXT)
    )

    TAXI_CORP_GO_ISRAEL_SPENDABLE = (
        ContractTemplate.TAXI_CORP_GO_ISRAEL_SPENDABLE,
        lambda: create_spendable(TAXI_CORP_ISRAEL_CONTEXT, TAXI_CORP_ISRAEL_CONTEXT_SPENDABLE, Regions.ISR)
    )

    CLOUD_1 = (
        ContractTemplate.CLOUD_1,
        lambda: create_cloud_contract(CLOUD_RU_CONTEXT, print_template=None)
    )

    CLOUD_ENTERPRISE = (
        ContractTemplate.CLOUD_ENTERPRISE,
        lambda: create_cloud_contract(CLOUD_RU_CONTEXT, print_template=ContractTemplate.CLOUD_ENTERPRISE)
    )

    CLOUD_WARGAME = (
        ContractTemplate.CLOUD_WARGAME,
        lambda: create_cloud_contract(CLOUD_RU_CONTEXT, print_template=ContractTemplate.CLOUD_WARGAME)
    )

    CLOUD_YT = (
        ContractTemplate.CLOUD_YT,
        lambda: create_cloud_contract(CLOUD_AG_CONTEXT)
    )

    MEDIASERVICES_AFISHA_SINGLE = (
        ContractTemplate.MEDIASERVICES_AFISHA_SINGLE,
        lambda: create_contract(MEDIASERVICES_SHOP_CONTEXT, ContractTemplate.MEDIASERVICES_AFISHA_SINGLE)
    )

    MEDIASERVICES_AFISHA_MULTI = (
        ContractTemplate.MEDIASERVICES_AFISHA_MULTI,
        lambda: create_contract(MEDIASERVICES_SHOP_CONTEXT, ContractTemplate.MEDIASERVICES_AFISHA_MULTI)
    )

    MEDIASERVICES_MUSIC = (
        ContractTemplate.MEDIASERVICES_MUSIC,
        lambda: create_contract(MEDIASERVICES_SHOP_CONTEXT, ContractTemplate.MEDIASERVICES_MUSIC)
    )

    FOOD_CORP_POSTPAY = (
        ContractTemplate.FOOD_CORP_POSTPAY,
        lambda: create_contract(FOOD_CORP_CONTEXT, is_postpay=True)
    )

    FOOD_CORP_PREPAY = (
        ContractTemplate.FOOD_CORP_PREPAY,
        lambda: create_contract(FOOD_CORP_CONTEXT, is_postpay=False)
    )

    FLEXIBLE_YANDEX = (
        ContractTemplate.FLEXIBLE_YANDEX,
        lambda: create_distribution_contract(DistributionContractType.AGILE, Firms.YANDEX_1, PersonTypes.UR.code)
    )

    FLEXIBLE_SERVICES_AG = (
        ContractTemplate.FLEXIBLE_SERVICES_AG,
        lambda: create_distribution_contract(DistributionContractType.AGILE, Firms.SERVICES_AG_16, PersonTypes.SW_YT.code)
    )

    UNIVERSAL_YANDEX = (
        ContractTemplate.UNIVERSAL_YANDEX,
        lambda: create_distribution_contract(DistributionContractType.UNIVERSAL, Firms.YANDEX_1, PersonTypes.UR.code)
    )

    UNIVERSAL_SERVICES_AG = (
        ContractTemplate.UNIVERSAL_SERVICES_AG,
        lambda: create_distribution_contract(DistributionContractType.UNIVERSAL, Firms.SERVICES_AG_16, PersonTypes.SW_YT.code)
    )

    def __init__(self, expected_template, create_contract):
        self.expected_template = expected_template
        self.create_contract = create_contract


class CollateralPrintFormCases(Enum):
    TAXI_PRE_TERMINATION_COL_BI = (
        CollateralTemplate.TAXI_COL_1,
        lambda: create_taxi_offer_prepay(with_spendable_corp=True),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_POST_TERMINATION_COL_BI = (
        CollateralTemplate.TAXI_COL_1,
        lambda: create_taxi_offer_postpay(with_spendable_corp=True),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_PRE_TERMINATION_COL_UNI = (
        CollateralTemplate.TAXI_COL_2,
        lambda: create_taxi_offer_prepay(),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.UNILATERAL)
    )
    TAXI_POST_TERMINATION_COL_UNI = (
        CollateralTemplate.TAXI_COL_2,
        lambda: create_taxi_offer_postpay(),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.UNILATERAL)
    )
    TAXI_COL_3_PRE = (
        CollateralTemplate.TAXI_COL_3,
        lambda: create_taxi_prepay(TAXI_RU_CONTEXT, Regions.RU),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_COL_3_POST = (
        CollateralTemplate.TAXI_COL_3,
        lambda: create_taxi_postpay(TAXI_RU_CONTEXT),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.BILATERAL)
    )

    TAXI_CORP_CLIENT_6_POST = (
        CollateralTemplate.TAXI_CORP_CLIENT_6,
        lambda: create_contract_corp_taxi(None, FINISH_DT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.UNILATERAL)
    )
    TAXI_CORP_CLIENT_6_PRE = (
        CollateralTemplate.TAXI_CORP_CLIENT_6,
        lambda: create_contract_corp_taxi(None, FINISH_DT, 0),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.UNILATERAL)
    )
    TAXI_CORP_CLIENT_7_POST = (
        CollateralTemplate.TAXI_CORP_CLIENT_7,
        lambda: create_contract_corp_taxi(None, FINISH_DT),
        lambda cid: create_taxi_prolong_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_CORP_CLIENT_7_PRE = (
        CollateralTemplate.TAXI_CORP_CLIENT_7,
        lambda: create_contract_corp_taxi(None, FINISH_DT, 0),
        lambda cid: create_taxi_prolong_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_CORP_CLIENT_8 = (
        CollateralTemplate.TAXI_CORP_CLIENT_8,
        lambda: create_contract_corp_taxi(None, FINISH_DT),
        lambda cid: create_taxi_change_credit_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_CORP_CLIENT_9_POST = (
        CollateralTemplate.TAXI_CORP_CLIENT_9,
        lambda: create_contract_corp_taxi(None, FINISH_DT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_CORP_CLIENT_9_PRE = (
        CollateralTemplate.TAXI_CORP_CLIENT_9,
        lambda: create_contract_corp_taxi(None, FINISH_DT, 0),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_CORP_CLIENT_DELIVERY_PRE = (
        CollateralTemplate.TAXI_CORP_CLIENT_DELIVERY,
        lambda: create_contract_corp_taxi(None, FINISH_DT, 0),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.BILATERAL,
                                            CollateralTemplate.TAXI_CORP_CLIENT_DELIVERY)
    )
    TAXI_CORP_CLIENT_DELIVERY_POST = (
        CollateralTemplate.TAXI_CORP_CLIENT_DELIVERY,
        lambda: create_contract_corp_taxi(None, FINISH_DT),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.BILATERAL)
    )

    TAXI_EXTCOOP_2_WO_WARR = (
        CollateralTemplate.TAXI_EXTCOOP_2_WO_WARR,
        lambda: create_taxi_postpay_ext_coop(),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_EXTCOOP_2_WITH_WARR = (
        CollateralTemplate.TAXI_EXTCOOP_2_WITH_WARR,
        lambda: create_taxi_postpay_ext_coop(),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.BILATERAL,
                                            CollateralTemplate.TAXI_EXTCOOP_2_WITH_WARR)
    )
    TAXI_EXTCOOP_3_WO_WARR = (
        CollateralTemplate.TAXI_EXTCOOP_3_WO_WARR,
        lambda: create_taxi_postpay_ext_coop(),
        lambda cid: create_taxi_prolong_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_EXTCOOP_3_WITH_WARR = (
        CollateralTemplate.TAXI_EXTCOOP_3_WITH_WARR,
        lambda: create_taxi_postpay_ext_coop(),
        lambda cid: create_taxi_prolong_collateral(cid, CollateralPrintFormType.BILATERAL,
                                                   CollateralTemplate.TAXI_EXTCOOP_3_WITH_WARR)
    )

    TAXI_KAZ_4 = (
        CollateralTemplate.TAXI_KAZ_4,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.KZ),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_KAZ_5 = (
        CollateralTemplate.TAXI_KAZ_5,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.KZ),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )
    TAXI_KAZ_6 = (
        CollateralTemplate.TAXI_KAZ_6,
        lambda: create_taxi_postpay(TAXI_KZ_CONTEXT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_PRE_NEW_EDITION_ACCESS = (
        CollateralTemplate.TAXI_KAZ_7,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.KZ),
        lambda cid: create_taxi_change_services_collateral(cid, CollateralPrintFormType.UNILATERAL, [Services.TAXI.id])
    )
    TAXI_POST_NEW_EDITION_INFO = (
        CollateralTemplate.TAXI_KAZ_8,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.KZ),
        lambda cid: create_taxi_spendable_etc_collateral(cid, CollateralTemplate.TAXI_KAZ_8)
    )
    TAXI_POST_NEW_EDITION_AGENCY = (
        CollateralTemplate.TAXI_KAZ_9,
        lambda: create_taxi_postpay(TAXI_KZ_CONTEXT),
        lambda cid: create_taxi_change_services_collateral(cid, CollateralPrintFormType.UNILATERAL,
                                                           [Services.TAXI.id, Services.TAXI_128.id, Services.UBER.id,
                                                            Services.UBER_ROAMING.id])
    )

    TAXI_ARM_4 = (
        CollateralTemplate.TAXI_ARM_4,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.ARM),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_ARM_5 = (
        CollateralTemplate.TAXI_ARM_5,
        lambda: create_taxi_postpay(TAXI_ARM_CONTEXT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_ARM_6 = (
        CollateralTemplate.TAXI_ARM_6,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.ARM),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    TAXI_GEO_2 = (
        CollateralTemplate.TAXI_GEO_2,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.GEO),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_GEO_4 = (
        CollateralTemplate.TAXI_GEO_4,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.GEO),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    #  Использование этой фирмы в Молдавии запрещено https://st.yandex-team.ru/BALANCE-34661
    # TAXI_MDA_3 = (
    #     CollateralTemplate.TAXI_MDA_3,
    #     lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.MD),
    #     lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    # )
    # TAXI_MDA_4 = (
    #     CollateralTemplate.TAXI_MDA_4,
    #     lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.MD),
    #     lambda cid: create_taxi_spendable_etc_collateral(cid)
    # )

    TAXI_KGZ_3 = (
        CollateralTemplate.TAXI_KGZ_3,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.KGZ),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_KGZ_4 = (
        CollateralTemplate.TAXI_KGZ_4,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.KGZ),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    # Использование этой фирмы в Белорусии запрещено https://st.yandex-team.ru/BALANCE-34661
    # TAXI_BLR_3 = (
    #     CollateralTemplate.TAXI_BLR_3,
    #     lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.BY),
    #     lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    # )
    # TAXI_BLR_4 = (
    #     CollateralTemplate.TAXI_BLR_4,
    #     lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.BY),
    #     lambda cid: create_taxi_spendable_etc_collateral(cid)
    # )

    TAXI_LVA_3 = (
        CollateralTemplate.TAXI_LVA_3,
        lambda: create_taxi_prepay(TAXI_BV_LAT_EUR_CONTEXT, Regions.LAT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_LVA_4 = (
        CollateralTemplate.TAXI_LVA_4,
        lambda: create_spendable(TAXI_BV_LAT_EUR_CONTEXT, TAXI_BV_LAT_EUR_CONTEXT_SPENDABLE, Regions.LAT),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    TAXI_UZB_3 = (
        CollateralTemplate.TAXI_UZB_3,
        lambda: create_taxi_prepay(TAXI_BV_GEO_USD_CONTEXT, Regions.UZB),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    TAXI_UZB_4 = (
        CollateralTemplate.TAXI_UZB_4,
        lambda: create_spendable(TAXI_BV_GEO_USD_CONTEXT, TAXI_BV_GEO_USD_CONTEXT_SPENDABLE, Regions.UZB),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    TAXI_AZE_3 = (
        CollateralTemplate.TAXI_AZE_3,
        lambda: create_taxi_prepay(TAXI_UBER_BV_AZN_USD_CONTEXT, Regions.AZ),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )
    # TAXI_AZE_4 = (
    #     CollateralTemplate.TAXI_AZE_4,
    #     lambda: create_taxi_spendable_donate(Firms.UBER_115, Regions.AZ, PersonTypes.EU_YT),
    #     lambda cid: create_taxi_spendable_etc_collateral(cid)
    # )

    TAXI_EU_MLU_RO_OFFER_TERMINATION = (
        CollateralTemplate.TAXI_EU_MLU_ROMANIA_OFFER_TERMINATION,
        lambda: create_taxi_offer_prepay(TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )

    TAXI_EU_MLU_RO_SPENDABLE_OTHER = (
        CollateralTemplate.TAXI_EU_MLU_ROMANIA_SPENDABLE_OTHER,
        lambda: create_spendable(TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT_SPENDABLE, Regions.RO),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    TAXI_AFRICA_MLU_GHA_OFFER_TERMINATION = (
        CollateralTemplate.TAXI_AFRICA_MLU_GHANA_OFFER_TERMINATION,
        lambda: create_taxi_offer_prepay(TAXI_GHANA_USD_CONTEXT),
        lambda cid: create_termination_collateral(cid, CollateralPrintFormType.BILATERAL)
    )

    TAXI_AFRICA_MLU_GHA_SPENDABLE_OTHER = (
        CollateralTemplate.TAXI_AFRICA_MLU_GHANA_SPENDABLE_OTHER,
        lambda: create_spendable(TAXI_GHANA_USD_CONTEXT, TAXI_GHANA_USD_CONTEXT_SPENDABLE, Regions.GHA),
        lambda cid: create_taxi_spendable_etc_collateral(cid)
    )

    MEDIASERVICES_OTHER = (
        CollateralTemplate.MEDIASERVICES_OTHER,
        lambda: create_contract(MEDIASERVICES_SHOP_CONTEXT),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.ANNEX,
                                            CollateralTemplate.MEDIASERVICES_OTHER)
    )

    MEDIASERVICES_PROMO_PLUS = (
        CollateralTemplate.MEDIASERVICES_PROMO_PLUS,
        lambda: create_contract(MEDIASERVICES_SHOP_CONTEXT),
        lambda cid: create_other_collateral(cid, CollateralPrintFormType.ANNEX,
                                            CollateralTemplate.MEDIASERVICES_PROMO_PLUS)
    )

    def __init__(self, expected_template, create_contract, create_collateral):
        self.expected_template = expected_template
        self.create_contract = create_contract
        self.create_collateral = create_collateral


@pytest.mark.parametrize('print_form_case', ContractPrintFormCases, ids=lambda pfc: pfc.name)
def test_get_contract_print_form(print_form_case):
    contract_ids = print_form_case.create_contract()
    if isinstance(contract_ids, int):
        contract_ids = [contract_ids, ]
    for contract_id in contract_ids:
        barcode = get_barcode(contract_id)
        utils.check_that(barcode, not_none(), u"Проверяем, что штрихкод не пуст")

        pdf = get_contract_print_form(contract_id, PFMode.CONTRACT)
        utils.check_that(pdf, not_none(), u'Проверяем, что метод что-то вернул')

        actual_print_template = get_print_template(contract_id)
        utils.check_that(actual_print_template, equal_to(print_form_case.expected_template),
                         u'Проверяем, что проставлен правильный шаблон')


@pytest.mark.parametrize('print_form_case', CollateralPrintFormCases, ids=lambda pfc: pfc.name)
def test_get_collateral_print_form(print_form_case):
    contract_ids = print_form_case.create_contract()
    if isinstance(contract_ids, int):
        contract_ids = [contract_ids, ]
    for contract_id in contract_ids:
        collateral_id = print_form_case.create_collateral(contract_id)

        barcode = get_barcode_collateral(collateral_id)
        utils.check_that(barcode, not_none(), u"Проверяем, что штрихкод не пуст")

        pdf = get_contract_print_form(collateral_id, PFMode.COLLATERAL)
        utils.check_that(pdf, not_none(), u'Проверяем, что метод что-то вернул')

        actual_print_template = get_collateral_print_template(collateral_id)
        utils.check_that(actual_print_template, equal_to(print_form_case.expected_template),
                         u'Проверяем, что проставлен правильный шаблон')


def test_missing_print_form_collateral():
    contract_id = create_taxi_prepay(TAXI_RU_CONTEXT, Regions.RU)
    collateral_id = create_termination_collateral(contract_id, CollateralPrintFormType.BILATERAL)

    with pytest.raises(xmlrpclib.Fault) as exc:
        get_contract_print_form(collateral_id, PFMode.COLLATERAL)

    expected_error = 'attribute "PRINT_TEMPLATE" not found'
    utils.check_that(exc.value.faultString, contains_string(expected_error), u'Проверяем текст ошибки')

    barcodes = get_barcode_collateral(collateral_id)
    utils.check_that(barcodes, not_none(), u"Проверяем отсутствие штрихкода")


def test_wrong_print_form_mode():
    contract_id = create_taxi_offer_prepay()
    collateral_id = create_termination_collateral(contract_id, CollateralPrintFormType.UNILATERAL)

    with pytest.raises(xmlrpclib.Fault) as exc:
        get_contract_print_form(collateral_id, PFMode.CONTRACT)

    expected_error = "Contract id={} not found".format(collateral_id)
    utils.check_that(exc.value.faultString, contains_string(expected_error), u'Проверяем текст ошибки')


def test_missing_finish_dt():
    contract_ids = create_contract_corp_taxi(ContractTemplate.TAXI_CORP_CLIENT_4)
    for contract_id in contract_ids:
        with pytest.raises(xmlrpclib.Fault) as exc:
            get_contract_print_form(contract_id)

        expected_error = "unsupported operand type(s) for -: 'NoneType' and 'datetime.timedelta'"
        utils.check_that(exc.value.faultString, contains_string(expected_error), u'Проверяем текст ошибки')


# Выгрузка barcode проверяется в тестах на выгрузку Договоров
# def test_contract_barcode_oebs_export():
#     contract_ids = create_contract_corp_taxi(None, FINISH_DT, is_export_needed=True)
#     for contract_id in contract_ids:
#         balance_barcode = get_barcode(contract_id)
#         oebs_barcode = get_oebs_barcode_contract(contract_id)
#         utils.check_that(balance_barcode, equal_to(oebs_barcode), u"Проверяем, что штрихкоды совпадают")


# Выгрузка barcode проверяется в тестах на выгрузку Допников
# def test_collateral_barcode_oebs_export():
#     contract_id = create_taxi_offer_prepay(is_export_needed=True)
#
#     collateral_id = create_termination_collateral(contract_id, CollateralPrintFormType.BILATERAL)
#     steps.ExportSteps.export_oebs(contract_id=contract_id)
#
#     balance_barcode = get_barcode_collateral(collateral_id)
#     oebs_barcode = get_oebs_barcode_collateral(collateral_id)
#     utils.check_that(balance_barcode, equal_to(oebs_barcode), u"Проверяем, что штрихкоды совпадают")


def test_erase_contract_print_form():
    contract_ids = create_contract_corp_taxi(None, FINISH_DT)
    for contract_id in contract_ids:
        get_contract_print_form(contract_id, PFMode.CONTRACT)
        is_page_not_found = is_print_form_not_found(ContractTemplate.TAXI_CORP_CLIENT_1, PFMode.CONTRACT, contract_id)
        utils.check_that(is_page_not_found, equal_to(False),
                         u'Проверяем, что на странице не отображется 404 (страница есть)')

        erase_print_form(contract_id, PFMode.CONTRACT)
        is_page_not_found = is_print_form_not_found(ContractTemplate.TAXI_CORP_CLIENT_1, PFMode.CONTRACT, contract_id)
        utils.check_that(is_page_not_found, equal_to(True), u'Проверяем, что на странице отображется 404 (страницы нет)')


def test_erase_collateral_print_form():
    contract_id = create_taxi_prepay(TAXI_RU_CONTEXT, Regions.RU)
    collateral_id = create_other_collateral(contract_id, CollateralPrintFormType.BILATERAL)

    get_contract_print_form(collateral_id, PFMode.COLLATERAL)
    is_page_not_found = is_print_form_not_found(CollateralTemplate.TAXI_COL_3, PFMode.COLLATERAL, collateral_id)
    utils.check_that(is_page_not_found, equal_to(False),
                     u'Проверяем, что на странице не отображется 404 (страница есть)')

    erase_print_form(collateral_id, PFMode.COLLATERAL)
    is_page_not_found = is_print_form_not_found(CollateralTemplate.TAXI_COL_3, PFMode.COLLATERAL, collateral_id)
    utils.check_that(is_page_not_found, equal_to(True), u'Проверяем, что на странице отображется 404 (страницы нет)')


# В шаблоне для коммерческого договора и расходного для donate зашиты параметры для связанных договоров в шаблонах
def test_linked_contracts():
    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_BV_GEO_USD_CONTEXT,
                                                                               is_postpay=0,
                                                                               full_person_params=True,
                                                                               additional_params={
                                                                                   'start_dt': START_DT,
                                                                                   'country': Regions.GEO.id
                                                                               })

    _, _, donate_contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_BV_GEO_USD_CONTEXT_SPENDABLE,
                                                                              client_id=client_id,
                                                                              additional_params={'start_dt': START_DT,
                                                                                                 'link_contract_id': contract_id,
                                                                                                 'country': Regions.GEO.id,
                                                                                                 'print_template': None})
    corp_context = CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED.new(
        firm=Firms.TAXI_BV_22,
        person_type=PersonTypes.EU_YT
    )
    steps.ContractSteps.create_partner_contract(corp_context, client_id=client_id,
                                                additional_params={'start_dt': START_DT,
                                                                   'country': Regions.RU.id,
                                                                   'link_contract_id': contract_id})

    pdf = get_contract_print_form(contract_id, PFMode.CONTRACT)
    utils.check_that(pdf, not_none(), u'Проверяем, что метод что-то вернул')

    pdf = get_contract_print_form(donate_contract_id, PFMode.CONTRACT)
    utils.check_that(pdf, not_none(), u'Проверяем, что метод что-то вернул')


# -----------------------------------
# Utils

def is_print_form_not_found(print_template, mode, contract_id):
    with reporter.step(u"Проверяем что ПФ не отображается на странице для {}: {}".format(mode.value, contract_id)):
        url = 'https://wiki.yandex-team.ru{}test/{}-{}/'.format(print_template, mode.value, contract_id)

        session = passport_steps.auth_session(Users.TESTUSER_BALANCE3, Passports.INTERNAL)
        response = session.get(url, verify=False)

        return 'Page cannot be found' in response.text


def create_taxi_prepay(context, region):
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=0,
                                                                       additional_params={'start_dt': START_DT,
                                                                                          'country': region.id})
    return contract_id


def create_taxi_postpay(context):
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, additional_params={'start_dt': START_DT})
    return contract_id


def create_taxi_postpay_ext_coop():
    context = TAXI_RU_CONTEXT.new(
        contract_services=[Services.SHOP.id, Services.TAXI_BREND.id],
        use_create_contract=True,
    )
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, additional_params={
        'start_dt': START_DT,
        'finish_dt': FINISH_DT
    })
    return contract_id


def create_spendable(context_general, context_spendable, region, print_template=None):
    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context_general,
                                                                       additional_params={'start_dt': START_DT,
                                                                                          'country': region.id})
    _, _, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(context_spendable, client_id=client_id,
                                                                       additional_params={'start_dt': START_DT,
                                                                                          'link_contract_id': contract_id,
                                                                                          'print_template': print_template,
                                                                                          'country': region.id})
    return spendable_contract_id


def create_cloud_contract(context, print_template=None):
    context = context.new(
        use_create_contract=True
    )

    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()
    params = {
        'start_dt': START_DT,
        'projects': [project_uuid],
        'print_template': print_template
    }

    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, additional_params=params)
    return contract_id


def create_contract(context, print_template=None, is_postpay=True):
    context = context.new(
        use_create_contract=True
    )

    params = {
        'start_dt': START_DT,
        'print_template': print_template
    }

    _, _, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params, is_postpay=is_postpay)
    return contract_id


def create_contract_corp_taxi(print_template=None, finish_dt=None, is_postpay=1, is_export_needed=False,
                              context=CORP_TAXI_RU_CONTEXT_GENERAL):
    context = context.new(
        use_create_contract = True
    )
    contract_services = [
        [Services.TAXI_CORP_CLIENTS.id],
        [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id],
    ]
    contract_ids = []
    for services in contract_services:
        context = context.new(contract_services=services)
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                   additional_params={
            'start_dt': START_DT,
            'finish_dt': finish_dt,
            'print_template': print_template
        })

        if is_export_needed:
            steps.ExportSteps.export_oebs(client_id=client_id,
                                          person_id=person_id,
                                          contract_id=contract_id)
        contract_ids.append(contract_id)

    return contract_ids


def get_print_template(contract_id):
    return steps.ContractSteps.get_attribute(contract_id, ContractAttributeType.STR, 'PRINT_TEMPLATE')


def get_barcode(contract_id, only_first=True):
    return steps.ContractSteps.get_attribute(contract_id, ContractAttributeType.NUM, 'PRINT_TPL_BARCODE', only_first)


def get_barcode_collateral(collateral_id, return_value=True):
    return steps.ContractSteps.get_attribute_collateral(collateral_id, ContractAttributeType.NUM,
                                                        'PRINT_TPL_BARCODE', return_value)


def remove_print_template(contract_id):
    with reporter.step(u'Удаляем PRINT_TEMPLATE параметр договора: {}'.format(contract_id)):
        query = "DELETE FROM T_CONTRACT_ATTRIBUTES WHERE ID=(SELECT ca.id FROM T_CONTRACT2 c " \
                "JOIN T_CONTRACT_COLLATERAL cc ON c.ID=cc.CONTRACT2_ID " \
                "JOIN T_CONTRACT_ATTRIBUTES ca ON ca.attribute_batch_id=cc.attribute_batch_id " \
                "WHERE c.ID=:contract_id AND ca.CODE='PRINT_TEMPLATE')"
        params = {'contract_id': contract_id}
        db.balance().execute(query, params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)


def get_collateral_print_template(collateral_id):
    with reporter.step(u'Получаем PRINT_TEMPLATE параметр допника: {}'.format(collateral_id)):
        query = "SELECT VALUE_STR FROM T_CONTRACT_COLLATERAL cc " \
                "JOIN T_CONTRACT_ATTRIBUTES ca ON ca.attribute_batch_id=cc.attribute_batch_id " \
                "WHERE cc.ID=:collateral_id AND ca.CODE='PRINT_TEMPLATE'"
        params = {'collateral_id': collateral_id}
        return db.balance().execute(query, params)[0]['value_str']


def create_taxi_offer_postpay(with_spendable_corp=False):
    client_id, _, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_RU_CONTEXT, is_offer=1,
                                                                               additional_params={
                                                                                   'start_dt': START_DT
                                                                               })

    if with_spendable_corp:
        steps.ContractSteps.create_partner_contract(CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, client_id=client_id,
                                                    additional_params={
                                                        'start_dt': START_DT,
                                                    })
    return contract_id


def create_taxi_offer_prepay(context=TAXI_RU_CONTEXT_CLONE, is_export_needed=False, with_spendable_corp=False):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=1, is_postpay=0,
                                                                               additional_params={
                                                                                   'start_dt': START_DT
                                                                               })
    if with_spendable_corp:
        steps.ContractSteps.create_partner_contract(CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, client_id=client_id,
                                                    additional_params={
                                                        'start_dt': START_DT,
                                                    })

    if is_export_needed:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id)

    return contract_id


def create_termination_collateral(contract_id, print_form_type):
    return _create_collateral(Collateral.TERMINATE, contract_id, print_form_type)


def create_taxi_change_services_collateral(contract_id, print_form_type, services=None):
    params = {
        'SERVICES': services,
        'PARTNER_COMMISSION_PCT2': 20
    }

    return _create_collateral(Collateral.CHANGE_SERVICES, contract_id, print_form_type, additional_params=params)


def create_other_collateral(contract_id, print_form_type, print_template=None):
    return _create_collateral(Collateral.OTHER, contract_id, print_form_type, print_template)


def create_taxi_prolong_collateral(contract_id, print_form_type, print_template=None):
    return _create_collateral(Collateral.PROLONG, contract_id, print_form_type, print_template)


def create_taxi_change_credit_collateral(contract_id, print_form_type, print_template=None):
    return _create_collateral(Collateral.CHANGE_CREDIT, contract_id, print_form_type, print_template)


def _create_collateral(collateral_type, contract_id, print_form_type, print_template=None, additional_params=None):
    params = {
        'CONTRACT2_ID': contract_id,
        'DT': (START_DT + relativedelta(days=1)).isoformat(),
        'FINISH_DT': FINISH_DT.isoformat(),
        'IS_FAXED': TODAY_ISO,
        'IS_BOOKED': TODAY_ISO,
        'IS_SIGNED': TODAY_ISO,
        'PRINT_FORM_TYPE': print_form_type.id,
        'NUM': print_form_type.prefix + u"01"
    }

    if additional_params is not None:
        params.update(additional_params)

    if print_template is not None:
        params['PRINT_TEMPLATE'] = print_template
        remove_params = None
    else:
        remove_params = ['PRINT_TEMPLATE']

    collateral_id = steps.ContractSteps.create_collateral(collateral_type, params, remove_params=remove_params)

    return collateral_id


def create_taxi_spendable_etc_collateral(contract_id, print_template=None):
    params = {
        'CONTRACT2_ID': contract_id,
        'DT': (START_DT + relativedelta(days=1)).isoformat(),
        'FINISH_DT': FINISH_DT.isoformat(),
        'IS_FAXED': TODAY_ISO,
        'IS_BOOKED': TODAY_ISO,
        'IS_SIGNED': TODAY_ISO,
        'NUM': u"01",
    }

    if print_template is not None:
        params['PRINT_TEMPLATE'] = print_template
        remove_params = None
    else:
        remove_params = ['PRINT_TEMPLATE']

    collateral_id = steps.ContractSteps.create_collateral(Collateral.ETC, params, remove_params=remove_params)

    return collateral_id


def create_distribution_contract(contract_type, firm, person_type):
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type=person_type)
    contract_id, external_id = steps.DistributionSteps.create_full_contract(
        contract_type,
        client_id,
        person_id,
        tag_id,
        dt=START_DT,
        service_start_dt=START_DT,
        firm=firm
    )
    return contract_id


def get_contract_print_form(contract_id, mode=PFMode.CONTRACT):
    with reporter.step(u'Вызываем GetContractPrintForm для договора: {}'.format(contract_id)):
        return api.medium().GetContractPrintForm(contract_id, mode.value)


# a-vasin: ручка игнорит подписанность договора и всегда удаляет html с ПФ
def erase_print_form(contract_id, mode=PFMode.CONTRACT):
    with reporter.step(u"Удаляем печатную форму для договора: {}".format(contract_id)):
        api.medium().EraseContractPrintForm(contract_id, mode.value)


def get_oebs_barcode_collateral(collateral_id, firm=Firms.TAXI_13):
    with reporter.step(u"Получаем штрихкод из OEBS для допника: {}".format(collateral_id)):
        query = "SELECT b.barcode_id barcode FROM apps.xxcmn_mon_doc_barcodes b, apps.xxoke_contract_dop_all d " \
                "WHERE  b.barcode_type_id = 10600 AND d.k_line_id = b.reference_id AND d.reference_id = :collateral_id"
        params = {'collateral_id': collateral_id}
        return db.oebs().execute_oebs(firm.id, query, params)[0]['barcode']


def get_oebs_barcode_contract(contract_id):
    with reporter.step(u"Получаем штрихкод из OEBS для договора: {}".format(contract_id)):
        return get_oebs_barcode_collateral(steps.ContractSteps.get_collateral_id(contract_id))
