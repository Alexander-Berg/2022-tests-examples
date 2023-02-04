# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest

import balance.tests.paystep.paystep_common_steps as PaystepSteps
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import Services, Products
from simpleapi.matchers import deep_equals as de
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()

PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)

VENDORS = Contexts.VENDORS_FISH_RUB_CONTEXT.new()
# SCORING = VENDORS.new(service=Services.SCORING)
REALTY = VENDORS.new(service=Services.REALTY)
OFD = VENDORS.new(service=Services.OFD)
TOLOKA = VENDORS.new(service=Services.TOLOKA, product=Products.TOLOKA)
BAYAN = VENDORS.new(service=Services.MEDIA_BANNERS)
DSP = VENDORS.new(service=Services.DSP)
BANKI = VENDORS.new(service=Services.BANKI)
KUPIBILET = VENDORS.new(service=Services.KUPIBILET)
APIKEYS = VENDORS.new(service=Services.APIKEYS, product=Products.APIKEYS)
TRANSLATE = VENDORS.new(service=Services.TRANSLATE)
DIRECT_TUNING = VENDORS.new(service=Services.DIRECT_TUNING)
MEDIANA = VENDORS.new(service=Services.MEDIANA)
CATALOG1 = VENDORS.new(service=Services.CATALOG1)
CATALOG2 = VENDORS.new(service=Services.CATALOG2)
DIRECT = VENDORS.new(service=Services.DIRECT)
MARKET = VENDORS.new(service=Services.MARKET)
CATALOG3 = VENDORS.new(service=Services.CATALOG3)
GEO = VENDORS.new(service=Services.GEO)
METRICA = VENDORS.new(service=Services.METRICA)
RIT = VENDORS.new(service=Services.RIT)
MEDIA = VENDORS.new(service=Services.MEDIA_70)
MEDIASELLING = VENDORS.new(service=Services.BAYAN)
REALTY_KOMM = VENDORS.new(service=Services.REALTY_COMM, product=Products.REALTY_COMM)
RABOTA = VENDORS.new(service=Services.RABOTA)
# TOURS = VENDORS.new(service=Services.TOURS)
AUTORU = VENDORS.new(service=Services.AUTORU)
DOSTAVKA_101 = VENDORS.new(service=Services.DOSTAVKA_101)
ADFOX = VENDORS.new(service=Services.ADFOX)
TAXI_111 = VENDORS.new(service=Services.TAXI_111)
TAXI_128 = VENDORS.new(service=Services.TAXI_128)
CONNECT = VENDORS.new(service=Services.CONNECT)
NAVI = VENDORS.new(service=Services.NAVI, product=Products.NAVI)
BUSES_2_0 = VENDORS.new(service=Services.BUSES_2_0)
DMP = VENDORS.new(service=Services.DMP)

'''https://wiki.yandex-team.ru/balance/tz/offers/
 для сервисов, у которых 1 строка вида  "сервис - # Запрещено выставляться по оферте"
 ПРОВЕРКИ:
 - Агенства не имеют доступных способов оплаты
 - Клиенты не имеют доступных способов оплаты'''

SERVICES_WITHOUT_OFFER = [REALTY,
                          MEDIASELLING,
                          # TOURS,
                          ]

'''Добавляем сервисы c офертой, у которых агенства не могут выставляться без договора (признак contract_needed)
 и не имеют признака allowed_agency_without_contract
 select id from t_service where id in (
 select distinct service_id from t_paysys_service) and contract_needed = 1 and
 not exists(SELECT object_id
                    FROM bo.t_extprops
                    WHERE classname = 'Service'
                          AND attrname = 'allowed_agency_without_contract'
 and object_id = t_service.id) and id not in (81, 168, 132);
 ПРОВЕРКИ:
 - Агенства не имеют доступных способов оплаты
 - Клиенты имеют доступные способы оплаты'''

SERVICES_AGENCY_CONTRACT_NEEDED = [OFD,
                                   TOLOKA,
                                   BAYAN,
                                   DSP,
                                   BANKI,
                                   KUPIBILET,
                                   TRANSLATE,
                                   DIRECT_TUNING,
                                   MEDIANA,
                                   VENDORS]

'''
Ага! Апикейз должен быть выше, но у него в апикейзе превентивно создается ЛС.
Без этого счета тест бесмысленен и беспощаден.
'''

SERVICES_AGENCY_CONTRACT_NEEDED_FORCED_FIRM = [APIKEYS]

'''
SELECT id
FROM bs.T_SERVICE
WHERE id NOT IN (7, 115, 120, 121, 140, 152, 153, 154, 155, 156, 157);
Партнерские сервисы из bs схемы, за исключением тех, которые могут ходить на пейстеп.
Не участвуют в тестах.
'''
TRUST_SERVICE = [23, 115, 116, 117, 118, 119, 122, 123, 124, 125, 126, 127, 130, 131, 135, 136, 137, 138, 139, 151, 170,
                 171, 172, 600, 601, 603]

'''
Список сервисов без единой записи в t_paysys_service
Не участвуют в тестах, не работаю никак вообще при выставлении без договора.
'''
WITHOUT_PAYSYS_SERVICE_SETTINGS_SERVICES = [1, 2, 3, 4, 8, 9, 10, 15, 17, 45, 46, 49, 65, 97, 112, 120, 121, 133, 134,
                                            141, 142, 143, 152, 153, 154, 155, 156, 157, 167, 190, 200, 203, 204, 210,
                                            270, 470, 555, 556, 560, 602]

'''
Партнерские сервисы из bo схемы.
Не участвуют в тестах, покрыты где-то еще.

'''
PARTNER_SERVICE = [TAXI_111,
                   TAXI_128,
                   ADFOX,
                   DMP,
                   CONNECT,
                   DOSTAVKA_101,
                   RIT,
                   BUSES_2_0,
                   DMP]

'''
Все остальные сервисы, у которых нет убедительных причин не иметь доступных способов оплат на пейстепе, найдены
методом исключения всех сервисов выше. Список составлен из тех сервисов, у которых есть признак
allowed_agency_without_contract и всех-всех оставшихся.
 ПРОВЕРКИ:
 - Агенства имеют доступные способы оплаты
 - Клиенты имеют доступные способы оплаты
'''

SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT = [DIRECT,
                                                 MARKET,
                                                 GEO
                                                 ]
OTHER_SERVICES = [
    CATALOG1,
    CATALOG2,
    MEDIA,
    REALTY_KOMM,
    RABOTA,
    AUTORU,
    NAVI
]

OTHER_SERVICES.extend(SERVICES_WITH_ALLOWED_AGENCY_WITHOUT_CONTRACT)
COMMON_SERVICES = OTHER_SERVICES

'''
Магазин - сервис с особой схемой
Не проверяется
'''

MAGAZIN = [35]

'''
ОЧЕНЬ особые случаи:
- Сервис из заказа с продуктом с группой 2543 ведет себя как 167 сервис. У 167 сервиса
проставлен признак contract_needed, у сервиса работа - нет.
'''

VSOP = [RABOTA.new(product=Products.MEDIA_2543_GROUP)]


def create_order_and_request(context, with_agency):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1}) if with_agency else None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=order_owner, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 2,
         'BeginDT': PREVIOUS_MONTH_LAST_DAY}]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': PREVIOUS_MONTH_LAST_DAY})
    return request_id


@pytest.mark.parametrize('with_agency', [True, False])
@pytest.mark.parametrize('context', SERVICES_WITHOUT_OFFER)
def test_services_no_offer_at_all(context, with_agency):
    request_id = create_order_and_request(context, with_agency=with_agency)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = PaystepSteps.format_request_choices(request_choices)
    utils.check_that(formatted_request_choices, hamcrest.empty())


@pytest.mark.parametrize('context', SERVICES_AGENCY_CONTRACT_NEEDED)
@pytest.mark.parametrize('with_agency', [False, True])
def test_services_agency_contract_needed(context, with_agency):
    request_id = create_order_and_request(context, with_agency)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = PaystepSteps.format_request_choices(request_choices)
    if with_agency:
        # агенству не доступны никакие способы оплаты
        utils.check_that(formatted_request_choices, hamcrest.empty())
    else:
        utils.check_that(bool(formatted_request_choices), hamcrest.equal_to(True))
    contract_needed_value = db.get_service_by_id(context.service.id)[0]['contract_needed']
    utils.check_that(contract_needed_value, hamcrest.equal_to(1))


@pytest.mark.parametrize('with_agency', [False, True])
@pytest.mark.parametrize('context', COMMON_SERVICES)
def test_agency_and_client_are_available(context, with_agency):
    request_id = create_order_and_request(context, with_agency)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = PaystepSteps.format_request_choices(request_choices)
    assert bool(formatted_request_choices) is True


@pytest.mark.parametrize('with_agency', [False, True])
@pytest.mark.parametrize('context', VSOP)
def test_agency_and_client_are_available_VSOP(context, with_agency):
    request_id = create_order_and_request(context, with_agency)
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = PaystepSteps.format_request_choices(request_choices)
    print formatted_request_choices
    if with_agency:
        # агенству не доступны никакие способы оплаты
        utils.check_that(formatted_request_choices, hamcrest.empty())
    else:
        utils.check_that(bool(formatted_request_choices), hamcrest.equal_to(True))
    contract_needed_value = db.get_service_by_id(context.service.id)[0]['contract_needed']
    utils.check_that(contract_needed_value, hamcrest.equal_to(0))
