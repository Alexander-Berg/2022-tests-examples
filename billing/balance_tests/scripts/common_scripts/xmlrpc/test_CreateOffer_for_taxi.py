# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

from hamcrest import equal_to
import pytest
from pytest import param

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Nds, FirmTaxi, PersonTypes, Regions, Services
from btestlib.data import defaults
from btestlib.matchers import equal_to_casted_dict

PASSPORT_ID = defaults.PASSPORT_UID
MANAGER_UID = defaults.Managers.PERANIDZE.uid
MANEGER_CODE = defaults.Managers.PERANIDZE.code
DEFAULT_MIN_COMMSSION_SUM = D('1000')
WO_NDS_RECEIPT = -1
SOME_CITY_GEOBASE_ID = 163

EXPECTED_COMMON_DATA = {
    'CONTRACT_TYPE': 9,
    'IS_ACTIVE': 1,
    'IS_CANCELLED': 0,
    'IS_FAXED': 0,
    'IS_SIGNED': 1,
    'IS_SUSPENDED': 0,
    'IS_DEACTIVATED': 0,
    'MANAGER_CODE': MANEGER_CODE,
}

COMMON_PARAMS = {
    'manager_uid': MANAGER_UID,
    'personal_account': 1,
    'offer_confirmation_type': 'min-payment',
    'deactivation_term': 5,
    'deactivation_amount': 100
}

COMMON_PARAMS_POSTPAY = {
    'payment_term': 10,
    'payment_type': 3,
    'service_min_cost': DEFAULT_MIN_COMMSSION_SUM
}

COMMON_PARAMS_PREPAY = {
    'payment_type': 2,
    'advance_payment_sum': DEFAULT_MIN_COMMSSION_SUM,
}

contract_start_dt = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
    day=1) - datetime.timedelta(days=200)


def expected_data_preparation(firm_data, person_id, services, payment_type,
                              contract_id, contract_eid, nds_for_receipt, start_dt,
                              commission_pct, netting, netting_pct):
    if not start_dt:
        start_dt = utils.Date.nullify_time_of_date(datetime.datetime.today())
    expected_data = utils.copy_and_update_dict(EXPECTED_COMMON_DATA,
                                               {
                                                   'FIRM_ID': firm_data.id,
                                                   'COUNTRY': firm_data.country,
                                                   'CURRENCY': firm_data.currency.char_code,
                                                   'DT': start_dt,
                                                   'EXTERNAL_ID': contract_eid,
                                                   'ID': contract_id,
                                                   'PAYMENT_TYPE': payment_type,
                                                   'PERSON_ID': person_id,
                                                   'SERVICES': set(services),
                                                   'NETTING': netting if netting else 0,
                                                   'OFFER_ACCEPTED': 0
                                               })

    if firm_data.country != Regions.RU.id:
        # дописываем пустое поле "GEOBASE_REGION" для не-России
        expected_data["GEOBASE_REGION"] = None

    if nds_for_receipt is not None:
        expected_data.update({'NDS_FOR_RECEIPT': nds_for_receipt})
    if commission_pct is not None:
        expected_data.update({'PARTNER_COMMISSION_PCT2': commission_pct})
    if netting_pct is not None:
        expected_data.update({'NETTING_PCT': netting_pct})
    return expected_data


# payment_type = 2 - prepay, payment_type = 3 - postpay
@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize(
    'firm_data, payment_type, start_dt, services, netting, netting_pct, nds_for_receipt, commission_pct, service_min_cost',
    [
        pytest.mark.smoke(
            (FirmTaxi.YANDEX_TAXI, 2, None, [128, 124, 125, 605, 111, 626], None, None, Nds.DEFAULT, None, None)),
        pytest.mark.smoke((FirmTaxi.YANDEX_TAXI_BV, 2, None, [128, 124, 111], None, None, None, D('0'), None)),
        (FirmTaxi.YANDEX_TAXI_KAZ, 2, None, [128, 124, 605, 125], None, None, None, D('0.01'), None),
        (FirmTaxi.YANDEX_TAXI_ARMENIA, 2, None, [128, 124], None, None, None, D('0.01'), None),
        (FirmTaxi.YANDEX_TAXI_UKRAINE, 2, None, [128, 124], None, None, None, D('20'), None),
        (FirmTaxi.YANDEX_TAXI, 3, None, [128, 124, 125, 605, 111, 626], None, None, Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI, 3, contract_start_dt, [128, 124, 125, 605, 111, 626],
         None, None, Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI, 3, None, [128, 124, 125, 605, 111, 626], 1, D('0.01'), Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI, 3, None, [128, 124, 125, 605, 111, 626], 1, D('300'), Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI, 3, None, [128, 124, 125, 605, 111, 626], None, None, Nds.NOT_RESIDENT, None, None),
        (FirmTaxi.YANDEX_TAXI, 3, None, [128, 124, 125, 605, 111, 626], None, None, WO_NDS_RECEIPT, None, None),
        (FirmTaxi.YANDEX_TAXI_BV, 3, None, [128, 111], None, None, None, None, None),
        (FirmTaxi.YANDEX_TAXI, 3, None, [128, 124, 125, 605, 111, 626], None, None, Nds.DEFAULT, None, D('0')),
    ],
    ids=[
        'CreateOffer with Yandex Taxi LLC prepay wo netting nds_for_receipt = 18',
        'CreateOffer with Yandex Taxi BV prepay and commission_pct = 0',
        'CreateOffer with Yandex Taxi KZT prepay and commission_pct = 0,01',
        'CreateOffer with Yandex Taxi ARM prepay and commission_pct = 0,01',
        'CreateOffer with Yandex Taxi UKR prepay and commission_pct = 20',
        'CreateOffer with Yandex Taxi LLC postpay wo netting nds_for_receipt = 18',
        'CreateOffer with Yandex Taxi LLC postpay wo netting with specified start dt nds_for_receipt = 18',
        'CreateOffer with Yandex Taxi LLC postpay with netting pct = 0,01 nds_for_receipt = 18',
        'CreateOffer with Yandex Taxi LLC postpay with netting pct = 300 nds_for_receipt = 18',
        'CreateOffer with Yandex Taxi LLC postpay wo netting nds_for_receipt = 0',
        'CreateOffer with Yandex Taxi LLC postpay wo netting nds_for_receipt = -1 (wo nds)',
        'CreateOffer with Yandex Taxi BV postpay wo 124',
        'CreateOffer with Yandex Taxi LLC postpay with service_min_cost = 0'
    ]
)
def test_check_taxi_create_offer(firm_data, payment_type, start_dt, services, netting, netting_pct,
                                 nds_for_receipt, commission_pct, service_min_cost):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, firm_data.person_type)

    params = utils.copy_and_update_dict(COMMON_PARAMS, {
        'client_id': client_id,
        'person_id': person_id,
        'currency': firm_data.currency.char_code,
        'firm_id': firm_data.id,
        'country': firm_data.country,
        'services': services
    })

    if payment_type == 2:
        params.update(COMMON_PARAMS_PREPAY)
    elif payment_type == 3:
        params.update(COMMON_PARAMS_POSTPAY)

    if start_dt:
        params.update({'start_dt': start_dt})
    if nds_for_receipt is not None:
        params.update({'nds_for_receipt': nds_for_receipt})
    if netting:
        params.update({'netting': netting,
                       'netting_pct': netting_pct, })
    if commission_pct is not None:
        params.update({'partner_commission_pct2': commission_pct})
    if service_min_cost is not None:
        params.update({'service_min_cost': service_min_cost})

    # создаем договор
    contract_id, contract_eid = steps.ContractSteps.create_offer(params)

    # забираем данные по договору (этим же методом забирает сервис)
    contract_data = api.medium().GetClientContracts({'ClientID': client_id})[0]

    # подготавливаем ожидаемые данные
    expected_data = expected_data_preparation(firm_data, person_id, services, payment_type,
                                              contract_id, contract_eid, nds_for_receipt, start_dt, commission_pct,
                                              netting, netting_pct)
    if start_dt == contract_start_dt:
        expected_data.update({'IS_DEACTIVATED': 1,
                              'IS_ACTIVE': 0,
                              'IS_SUSPENDED': 1})
    # сравниваем платеж с шаблоном
    utils.check_that(contract_data, equal_to_casted_dict(expected_data),
                     'Сравниваем данные по договору с шаблоном')


@reporter.feature(Features.XMLRPC)
@pytest.mark.parametrize(
    'firm_data, error_text, payment_type, services, netting, netting_pct, nds_for_receipt, commission_pct, param_to_remove',
    [
        (FirmTaxi.YANDEX_TAXI, u"Rule violation: 'Не заполнена ставка для НДС'", 2, [128, 124, 111, 125, 605], None,
         None,
         None, None, None),
        (FirmTaxi.YANDEX_TAXI_BV, u"Rule violation: 'Не заполнен процент комиссии с карт'", 2, [128, 124], None,
         None, Nds.ZERO, None, None),
        (
                FirmTaxi.YANDEX_TAXI_ARMENIA, u"Rule violation: 'Не заполнен процент комиссии с карт'", 2, [128, 124],
                None,
                None, Nds.ZERO, None, None),
        (
                FirmTaxi.YANDEX_TAXI_KAZ, u"Rule violation: 'Не заполнен процент комиссии с карт'", 2,
                [128, 124, 125, 605],
                None,
                None, Nds.ZERO, None, None),
        (
                FirmTaxi.YANDEX_TAXI_UKRAINE, u"Rule violation: 'Не заполнен процент комиссии с карт'", 2, [128, 124],
                None,
                None, Nds.ZERO, None, None),
        (FirmTaxi.YANDEX_TAXI_BV,
         u"Rule violation: 'Значение процента комиссии (20.01%) выходит за пределы допустимого интервала [0, 20].'",
         2, [128, 124], None, None, Nds.ZERO, D('20.01'), None),
        (FirmTaxi.YANDEX_TAXI,
         u"Rule violation: 'Значение процента взаимозачёта (0.0%) выходит за пределы допустимого интервала (0, 300].'",
         2, [128, 124, 111, 125, 605], 1, D('0'), Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI,
         u"Rule violation: 'Значение процента взаимозачёта (300.01%) выходит за пределы допустимого интервала (0, 300].'",
         2, [128, 124, 111, 125, 605], 1, D('300.01'), Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI_BV, u"Rule violation: 'Не выбрана страна'", 2, [128, 124, 111], None, None, Nds.ZERO,
         D('1'), 'country'),
        (FirmTaxi.YANDEX_TAXI,
         u"procedure '_CreateCommonContract': parameter[1]: hash item 'netting_pct': invalid decimal value: None", 2,
         [128, 124, 111], 1, None, Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI,
         u"Rule violation: 'Должны быть подключены оба сервиса на Яндекс.Такси платежи картой'", 2, [124, 125, 605], 1,
         D('1'), Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI,
         u"Rule violation: 'Должны быть подключены оба сервиса на Яндекс.Такси платежи картой'", 2, [128], 1,
         D('1'), Nds.DEFAULT, None, None),
        (FirmTaxi.YANDEX_TAXI,
         u"'Rule violation: 'Выбраны не все платежные сервисы (Яндекс.Такси: Платежи, Яндекс.Убер: Платежи, Такси.Везёт, Такси.РуТакси)'",
         2, [128, 124, 111, 125], None, None,
         Nds.ZERO, None, None),
        (FirmTaxi.YANDEX_TAXI,
         u"Rule violation: 'Выбраны не все платежные сервисы (Яндекс.Такси: Платежи, Яндекс.Убер: Платежи, Яндекс.Убер: Платежи в роуминге)'",
         2, [128, 124, 111, 605], None, None,
         Nds.ZERO, None, None),
        (FirmTaxi.YANDEX_TAXI_KAZ,
         u"Rule violation: 'Выбраны не все платежные сервисы (Яндекс.Такси: Платежи, Яндекс.Убер: Платежи, Яндекс.Убер: Платежи в роуминге)'",
         2, [128, 124, 125], None,
         None, Nds.ZERO, 0, None),
        (FirmTaxi.YANDEX_TAXI_KAZ,
         u"Rule violation: 'Выбраны не все платежные сервисы (Яндекс.Такси: Платежи, Яндекс.Убер: Платежи, Яндекс.Убер: Платежи в роуминге)'",
         2, [128, 124, 605], None,
         None, Nds.ZERO, 0, None),
        (FirmTaxi.YANDEX_TAXI_BV,
         u"Rule violation: 'Нельзя выбрать сервисы Яндекс.Убер: Платежи и Яндекс.Убер: Платежи в роуминге в Yandex.Taxi B.V.'",
         2, [128, 124, 125], None,
         None, Nds.ZERO, 0, None),
        (FirmTaxi.YANDEX_TAXI_BV,
         u"Rule violation: 'Нельзя выбрать сервисы Яндекс.Убер: Платежи и Яндекс.Убер: Платежи в роуминге в Yandex.Taxi B.V.'",
         2, [128, 124, 605], None,
         None, Nds.ZERO, 0, None),
        (FirmTaxi.YANDEX_TAXI_ARMENIA,
         u"Rule violation: 'В фирме Yandex.Taxi AM LLP нельзя выбрать сервисы Яндекс.Убер: Платежи, Яндекс.Убер: Платежи в роуминге'",
         2, [128, 124, 125], None,
         None, Nds.ZERO, 0, None),
        (FirmTaxi.YANDEX_TAXI_ARMENIA,
         u"Rule violation: 'В фирме Yandex.Taxi AM LLP нельзя выбрать сервисы Яндекс.Убер: Платежи, Яндекс.Убер: Платежи в роуминге'",
         2, [128, 124, 605], None,
         None, Nds.ZERO, 0, None),
        (FirmTaxi.YANDEX_TAXI,
         u"Rule violation: 'Яндекс.Такси: Шереметьево может быть выбрано только вместе с Яндекс.Такси'",
         2, [128, 124, 125, 605, 626], None, None, Nds.DEFAULT, None, None),
    ],
    ids=[
        'CreateOffer with Yandex Taxi LLC wo nds_for_receipt',
        'CreateOffer with Yandex Taxi BV wo commission pct',
        'CreateOffer with Yandex Taxi ARM wo commission pct',
        'CreateOffer with Yandex Taxi KZT wo commission pct',
        'CreateOffer with Yandex Taxi UKR wo commission pct',
        'CreateOffer with Yandex Taxi BV with commission pct > 20',
        'CreateOffer with Yandex Taxi LLC with netting pct = 0',
        'CreateOffer with Yandex Taxi LLC with netting pct > 300',
        'CreateOffer with Yandex Taxi BV wo country',
        'CreateOffer with Yandex Taxi LLC with netting wo netting pct',
        'CreateOffer with Yandex Taxi LLC with 124, 125, 605 wo 128',
        'CreateOffer with Yandex Taxi LLC with 128 wo 124',
        'CreateOffer with Yandex Taxi LLC with 124 wo 605',
        'CreateOffer with Yandex Taxi LLC with 124 wo 125',
        'CreateOffer with Yandex Taxi KZT with 124 wo 605',
        'CreateOffer with Yandex Taxi KZT with 124 wo 125',
        'CreateOffer with Yandex Taxi BV with 125',
        'CreateOffer with Yandex Taxi BV with 605',
        'CreateOffer with Yandex Taxi ARM with 125',
        'CreateOffer with Yandex Taxi ARM with 605',
        'CreateOffer with Yandex Taxi SVO wo 111'
    ]
)
def test_check_errors_taxi_create_offer(firm_data, error_text, payment_type, services, netting, netting_pct,
                                        nds_for_receipt, commission_pct, param_to_remove):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, firm_data.person_type)

    params = COMMON_PARAMS.copy()
    params.update({
        'client_id': client_id,
        'person_id': person_id,
        'currency': firm_data.currency.char_code,
        'firm_id': firm_data.id,
        'country': firm_data.country,
        'services': services
    })

    if payment_type == 2:
        params.update(COMMON_PARAMS_PREPAY)
    elif payment_type == 3:
        params.update(COMMON_PARAMS_POSTPAY)

    if nds_for_receipt is not None:
        params.update({'nds_for_receipt': nds_for_receipt})
    if netting:
        params.update({'netting': netting,
                       'netting_pct': netting_pct, })
    if commission_pct is not None:
        params.update({'partner_commission_pct2': commission_pct})

    if param_to_remove:
        params.pop(param_to_remove)

    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_offer(params)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     equal_to(error_text))


@reporter.feature(Features.CORP_TAXI, Features.SPENDABLE, Features.TAXI, Features.XMLRPC)
@pytest.mark.parametrize('firm_data, services, person_type_code, is_spendable', [
        param(FirmTaxi.YANDEX_TAXI_UKRAINE,
              [Services.TAXI_128, Services.UBER], PersonTypes.UA.code, 0, id="GENERAL"),
        param(FirmTaxi.YANDEX_TAXI_BV, [Services.TAXI_CORP], PersonTypes.EU_YT.code, 1, id="SPENDABLE")])
def test_check_geo_region_contract_field_correct_storage(firm_data, services, person_type_code, is_spendable):
    client_id = steps.ClientSteps.create()

    is_partner = '1' if is_spendable else '0'
    person_id = steps.PersonSteps.create(client_id, person_type_code, {'is-partner': is_partner})

    params = COMMON_PARAMS.copy()
    params.update({'client_id': client_id,
                   'person_id': person_id,
                   'currency': firm_data.currency.char_code,
                   'firm_id': firm_data.id,
                   'country': firm_data.country,
                   'nds': Nds.DEFAULT,
                   'payment_sum': 8000,
                   'pay_to': 5,
                   'geobase_region': SOME_CITY_GEOBASE_ID,
                   'services': [s.id for s in services],
                   'nds_for_receipt': Nds.DEFAULT})

    if is_spendable:
        params.update({'ctype': 'SPENDABLE'})
    else:
        params.update({'payment_type': 2,
                       'advance_payment_sum': DEFAULT_MIN_COMMSSION_SUM,
                       'partner_commission_pct2': 0})

    steps.ContractSteps.create_offer(params)

    get_client_params = {'ClientID': client_id}
    if is_spendable:
        get_client_params.update({'ContractType': 'SPENDABLE'})

    contract_data = api.medium().GetClientContracts(get_client_params)

    utils.check_that(len(contract_data), equal_to(1),
                     u'Проверяем, что по запросу нашёлся единственный созданный ранее договор')
    utils.check_that(contract_data[0]["GEOBASE_REGION"], equal_to(SOME_CITY_GEOBASE_ID),
                     u'Проверяем, что полученное значение региона в договоре равно значению, заданному при создании договора')
