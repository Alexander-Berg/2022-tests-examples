# coding: utf-8
__author__ = 'atkaya'

import time

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string

import btestlib.utils
from balance import balance_steps as steps
from balance.features import Features
import btestlib.reporter as reporter
from btestlib.data.partner_contexts import *
from btestlib.data.simpleapi_defaults import DEFAULT_USER, TrustPaymentCases, DEFAULT_PAYMENT_JSON_TEMPLATE
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.data.defaults import user_ip

# !!!!!!!!! BALANCE-39255 - отключено проведение платежей !!!!!!!!!!

# тесты, которые вставляют данные в t_payment, t_ccard_bound_payment, t_refund напрямую
# создание партнера и сервисного продукта все еще происходит через траст,
# но можно в будущем тоже уйти (нужна запись в t_service_product как минимум)
# честно через траст проверяется минимальный набор платежей такси в test_taxi_payments

DEFAULT_JSON_SERVICE_ORDER_ID_NUMBER = 22222222
DEFAULT_JSON_SOURCE_ID = 333333
DEFAULT_JSON_QTY = "0.00"

pytestmark = [reporter.feature(Features.TRUST, Features.PAYMENT, Features.TAXI)]

CONTRACT_START_DT = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(months=1)

# кейсы по такси подобраны исходя из реальных использований в проде
CASES = [
    pytest.mark.smoke((TrustPaymentCases.TAXI_RU_124, TAXI_RU_CONTEXT)),
    pytest.mark.smoke((TrustPaymentCases.TAXI_RU_125, TAXI_RU_CONTEXT)),
    # pytest.mark.smoke((TrustPaymentCases.TAXI_RU_605, TAXI_RU_CONTEXT)), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_USD_124, TAXI_BV_GEO_USD_CONTEXT),

    (TrustPaymentCases.TAXI_EUR_124, TAXI_BV_LAT_EUR_CONTEXT),

    (TrustPaymentCases.TAXI_AZN_125, TAXI_UBER_BV_AZN_USD_CONTEXT), # с конвертацией из AZN в USD
    (TrustPaymentCases.TAXI_AZN_125, TAXI_UBER_BV_BYN_AZN_USD_CONTEXT), # с конвертацией из AZN в USD
    # (TrustPaymentCases.TAXI_AZN_605, TAXI_UBER_BV_AZN_USD_CONTEXT), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_BYN_124, TAXI_UBER_BV_BY_BYN_CONTEXT),
    (TrustPaymentCases.TAXI_BYN_124, TAXI_UBER_BV_BYN_BY_BYN_CONTEXT),
    (TrustPaymentCases.TAXI_BYN_125, TAXI_UBER_BV_BY_BYN_CONTEXT),
    (TrustPaymentCases.TAXI_BYN_125, TAXI_UBER_BV_BYN_BY_BYN_CONTEXT),
    # (TrustPaymentCases.TAXI_BYN_605, TAXI_UBER_BV_BY_BYN_CONTEXT), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_KZT_124, TAXI_KZ_CONTEXT),
    (TrustPaymentCases.TAXI_KZT_125, TAXI_KZ_CONTEXT),
    # (TrustPaymentCases.TAXI_KZT_605, TAXI_KZ_CONTEXT), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_AMD_124, TAXI_ARM_CONTEXT),

    (TrustPaymentCases.TAXI_ILS_124, TAXI_ISRAEL_CONTEXT),

    (TrustPaymentCases.TAXI_AZN_125, TAXI_AZARBAYCAN_CONTEXT),
    (TrustPaymentCases.TAXI_AZN_124, TAXI_AZARBAYCAN_CONTEXT),

    (TrustPaymentCases.TAXI_GHS_124, TAXI_GHANA_USD_CONTEXT),
    (TrustPaymentCases.TAXI_GHS_125, TAXI_GHANA_USD_CONTEXT),
    # (TrustPaymentCases.TAXI_GHS_605, TAXI_GHANA_USD_CONTEXT), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_BOB_124, TAXI_BOLIVIA_USD_CONTEXT),

    (TrustPaymentCases.TAXI_RU_124_COMPENSATION, TAXI_RU_CONTEXT), # проверка компенсации
    (TrustPaymentCases.TAXI_RU_125_COMPENSATION, TAXI_RU_CONTEXT), # проверка компенсации
    (TrustPaymentCases.TAXI_RU_605_COMPENSATION, TAXI_RU_CONTEXT), # проверка компенсации

    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT),
    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT),
    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT),
    (TrustPaymentCases.TAXI_RON_124, TAXI_YANDEX_GO_SRL_CONTEXT),
    #     (TrustPaymentCases.TAXI_RON_124_COMPENSATION, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT),
    #     (TrustPaymentCases.TAXI_RON_124_COMPENSATION, TAXI_MLU_EUROPE_ROMANIA_USD_CONTEXT),
    #     (TrustPaymentCases.TAXI_RON_124_COMPENSATION, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT),


    (TrustPaymentCases.TAXI_KZT_124, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
    (TrustPaymentCases.TAXI_KZT_125, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
    # (TrustPaymentCases.TAXI_KZT_605, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_ZAR_124, TAXI_ZA_USD_CONTEXT),

    (TrustPaymentCases.TAXI_NOR_124, TAXI_BV_NOR_NOK_CONTEXT),

    (TrustPaymentCases.TAXI_SWE_124, TAXI_MLU_EUROPE_SWE_SEK_CONTEXT),
]

# тесты на проверку платежа, если валюта платежа отличается от валюты договора, то будет конвертация,
# курс подбирается на дату start_dt в платеже,
# источник сейчас берется по валюте платежа
@pytest.mark.parametrize('payment_data, context', CASES, ids=lambda p, c: p.name +'-'+ c.name)
def test_payment(payment_data, context):
    # подготавливаем данные для вставки платежа
    data_for_payment = prepare_test_data_for_payment(payment_data)

    # создаем клиента через траст и сервисный продукт
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(data_for_payment.service)
    # создаем плательщика и договор (поля в договор тянутся из контекста)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                               additional_params={
                                                                                   'start_dt': CONTRACT_START_DT})

    data_for_payment = data_for_payment.new(client_id=client_id, person_id=person_id, contract_id=contract_id)
    # создаем платеж
    create_payment(data_for_payment)

    # получаем данные по платежу
    actual_payment_data = steps.CommonPartnerSteps.get_all_thirdparty_data_by_payment_id(data_for_payment.payment_id)
    # подготавливаем ожидаемые данные
    expected_payment_data = create_expected_payment_data(context, data_for_payment)
    # сравниваем данные
    utils.check_that(actual_payment_data, contains_dicts_with_entries([expected_payment_data]),
                     'Сравниваем платеж с шаблоном')


# тесты на проверку возвратов
@pytest.mark.parametrize('payment_data, context', CASES, ids=lambda p, c: p.name +'-'+ c.name)
def test_refund(payment_data, context):
    # подготавливаем данные для вставки платежа
    data_for_payment = prepare_test_data_for_payment(payment_data, with_refund=True)

    # создаем клиента через траст и сервисный продукт
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(data_for_payment.service)
    # создаем плательщика и договор (поля в договор тянутся из контекста)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                               additional_params={
                                                                                   'start_dt': CONTRACT_START_DT})

    data_for_payment = data_for_payment.new(client_id=client_id, person_id=person_id, contract_id=contract_id)
    # создаем возврат
    create_refund(data_for_payment)

    # получаем данные по платежу
    actual_refund_data = steps.CommonPartnerSteps.get_all_thirdparty_data_by_payment_id(data_for_payment.payment_id,
                                                                                       transaction_type=TransactionType.REFUND)
    # подготавливаем ожидаемые данные
    expected_refund_data = create_expected_payment_data(context, data_for_payment, is_refund=True)
    # сравниваем данные
    utils.check_that(actual_refund_data, contains_dicts_with_entries([expected_refund_data]),
                     'Сравниваем возврат с шаблоном')


CASES_YANDEX_REWARD = [
    (TrustPaymentCases.TAXI_USD_124, TAXI_BV_GEO_USD_CONTEXT, Decimal('0')), #если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_USD_124, TAXI_BV_GEO_USD_CONTEXT, Decimal('1')), #АВ минималка 0.01
    (TrustPaymentCases.TAXI_AZN_125, TAXI_UBER_BV_AZN_USD_CONTEXT, Decimal('0')), #если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_AZN_125, TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, Decimal('0')), #если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_AZN_125, TAXI_UBER_BV_AZN_USD_CONTEXT, Decimal('1')), #АВ минималка 0.01
    (TrustPaymentCases.TAXI_AZN_125, TAXI_UBER_BV_BYN_AZN_USD_CONTEXT, Decimal('1')), #АВ минималка 0.01

    (TrustPaymentCases.TAXI_AZN_124, TAXI_AZARBAYCAN_CONTEXT, Decimal('0')),  # если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_AZN_124, TAXI_AZARBAYCAN_CONTEXT, Decimal('1')),  # АВ минималка 0.01
    (TrustPaymentCases.TAXI_AZN_125, TAXI_AZARBAYCAN_CONTEXT, Decimal('0')),  # если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_AZN_125, TAXI_AZARBAYCAN_CONTEXT, Decimal('1')),  # АВ минималка 0.01

    (TrustPaymentCases.TAXI_AMD_124, TAXI_ARM_CONTEXT, Decimal('0')), #АВ минималка 1
    (TrustPaymentCases.TAXI_KZT_124, TAXI_KZ_CONTEXT, Decimal('0')), #АВ минималка 15
    (TrustPaymentCases.TAXI_ILS_124, TAXI_ISRAEL_CONTEXT, Decimal('0')), #если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_ILS_124, TAXI_ISRAEL_CONTEXT, Decimal('1')), #АВ минималка 0.01

    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, Decimal('0')),  # если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT, Decimal('1')),  # АВ минималка 0.01
    (TrustPaymentCases.TAXI_RON_124, TAXI_YANDEX_GO_SRL_CONTEXT, Decimal('0')),  # если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_RON_124, TAXI_YANDEX_GO_SRL_CONTEXT, Decimal('1')),  # АВ минималка 0.01

    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, Decimal('0')),  # если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_RON_124, TAXI_MLU_EUROPE_ROMANIA_RON_CONTEXT, Decimal('1')),  # АВ минималка 0.01

    (TrustPaymentCases.TAXI_GHS_124, TAXI_GHANA_USD_CONTEXT, Decimal('0')), #если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_GHS_124, TAXI_GHANA_USD_CONTEXT, Decimal('1')), #АВ минималка 0.01

    (TrustPaymentCases.TAXI_BOB_124, TAXI_BOLIVIA_USD_CONTEXT, Decimal('0')), #если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_BOB_124, TAXI_BOLIVIA_USD_CONTEXT, Decimal('1')), #АВ минималка 0.01

    (TrustPaymentCases.TAXI_KZT_124, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, Decimal('0')), # минималка 15
    (TrustPaymentCases.TAXI_KZT_125, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, Decimal('1')), # минималка 15
    # (TrustPaymentCases.TAXI_KZT_605, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, Decimal('0')), # отключаем UBER_ROAMING https://wiki.yandex-team.ru/users/atkaya/obsoleteservices

    (TrustPaymentCases.TAXI_ZAR_124, TAXI_ZA_USD_CONTEXT, Decimal('0')),  # если в договоре 0, АВ 0
    (TrustPaymentCases.TAXI_ZAR_124, TAXI_ZA_USD_CONTEXT, Decimal('1')),  # АВ минималка 0.01
    (TrustPaymentCases.TAXI_NOR_124, TAXI_BV_NOR_NOK_CONTEXT, Decimal('0')),
    (TrustPaymentCases.TAXI_SWE_124, TAXI_MLU_EUROPE_SWE_SEK_CONTEXT, Decimal('0')),
]
# тесты на проверку АВ в разных случаях и фирмах,
# дефолтные сценарии проверяются с основным тестом по платежам (применение процента из договора)
# для Такси РФ: АВ всегда 0, в договоре нет процента
# для Taxi BV, Uber ML BV, YANDEX.GO ISRAEL Ltd и UBER AZARBAYCAN MMC: если в договоре 0, то АВ 0, иначе по проценту из договора, минималка 0.01
# для Такси Армении: по проценту из договора, минималка 1
# для Такси Казахстан: по проценту из договора, минималка 15
@pytest.mark.parametrize('payment_data, context, partner_commission_pct', CASES_YANDEX_REWARD, ids=lambda d, c, p: d.name +'-'+ c.name+ ' pct='+str(p))
def test_yandex_reward_check(payment_data, context, partner_commission_pct):
    # подготавливаем данные для вставки платежа
    data_for_payment = prepare_test_data_for_payment(payment_data)
    # берем маленькие суммы платежа, чтобы АВ по расчету получалось близко к 0
    data_for_payment = data_for_payment.new(amount=Decimal('0.3'), price=Decimal('0.3'))

    # создаем клиента через траст и сервисный продукт
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(data_for_payment.service)
    # создаем плательщика и договор (поля в договор тянутся из контекста) с указанным в параметризации процентом
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                               additional_params={
                                                                                   'start_dt': CONTRACT_START_DT,
                                                                                   'partner_commission_pct2': partner_commission_pct
                                                                               })
    context = context.new(special_contract_params={'partner_commission_pct2': partner_commission_pct})

    data_for_payment = data_for_payment.new(client_id=client_id, person_id=person_id, contract_id=contract_id)
    # создаем платеж
    create_payment(data_for_payment)
    # получаем данные по платежу
    actual_payment_data = steps.CommonPartnerSteps.get_all_thirdparty_data_by_payment_id(data_for_payment.payment_id)
    # подготавливаем ожидаемые данные
    expected_payment_data = create_expected_payment_data(context, data_for_payment)
    # сравниваем данные
    utils.check_that(actual_payment_data, contains_dicts_with_entries([expected_payment_data]),
                     'Сравниваем платеж с шаблоном')


# тест на проверку смены процента за АВ через дс
def test_taxi_yandex_reward_pct_change():
    # проверяем на одном сценарии, взят Taxi BV т.к. у него есть процент в договоре
    context = TAXI_BV_GEO_USD_CONTEXT
    payment_data = TrustPaymentCases.TAXI_USD_124
    contract_dt = CONTRACT_START_DT - relativedelta(months=1)
    pct_new = Decimal('17.9')

    # подготавливаем данные для вставки платежа
    data_for_payment = prepare_test_data_for_payment(payment_data)

    # создаем клиента через траст и сервисный продукт
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(data_for_payment.service)
    # создаем плательщика и договор (поля в договор тянутся из контекста)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                               additional_params={
                                                                                   'start_dt': contract_dt,
                                                                               })
    # создаем дс на изменение процента комиссии
    steps.ContractSteps.create_collateral(Collateral.CHANGE_COMMISSION_PCT, {
        'CONTRACT2_ID': contract_id,
        'DT': CONTRACT_START_DT,
        'IS_SIGNED': CONTRACT_START_DT.isoformat(),
        'PARTNER_COMMISSION_PCT2': pct_new
    })

    context = context.new(special_contract_params={'partner_commission_pct2': pct_new})

    data_for_payment = data_for_payment.new(client_id=client_id, person_id=person_id,
                                            contract_id=contract_id)
    # создаем платеж
    create_payment(data_for_payment)
    # получаем данные по платежу
    actual_payment_data = steps.CommonPartnerSteps.get_all_thirdparty_data_by_payment_id(data_for_payment.payment_id)
    # подготавливаем ожидаемые данные
    expected_payment_data = create_expected_payment_data(context, data_for_payment)
    # сравниваем данные
    utils.check_that(actual_payment_data, contains_dicts_with_entries([expected_payment_data]),
                     'Сравниваем платеж с шаблоном')


# тест на проверку ошибки при обработке платежа, когда сервис в договоре был отключен
def test_taxi_collateral_disable_124_service():
    # проверяем на одном сценарии для ООО Яндекс Такси
    payment_data = TrustPaymentCases.TAXI_RU_124
    context = TAXI_RU_CONTEXT

    # подготавливаем данные для вставки платежа
    data_for_payment = prepare_test_data_for_payment(payment_data)

    # создаем клиента через траст и сервисный продукт
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(data_for_payment.service)
    # создаем плательщика и договор (поля в договор тянутся из контекста)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, client_id=client_id,
                                                                               additional_params={
                                                                                   'start_dt': CONTRACT_START_DT})
    # создаем дс на изменение сервисов, оставляем только 111 сервис
    steps.ContractSteps.create_collateral(Collateral.CHANGE_SERVICES, {
        'CONTRACT2_ID': contract_id,
        'DT': CONTRACT_START_DT + relativedelta(days=1),
        'IS_SIGNED': (CONTRACT_START_DT + relativedelta(days=1)).isoformat(),
        'SERVICES': [Services.TAXI_111.id]
    })

    data_for_payment = data_for_payment.new(client_id=client_id, person_id=person_id, contract_id=contract_id)
    # создаем платеж
    create_payment(data_for_payment, with_export=False)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(data_for_payment.payment_id)
    # проверяем ошибку обработки платежа
    expected_error = 'TrustPayment({}) delayed: no active contracts found for client {}'.format(data_for_payment.payment_id, client_id)
    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u'Проверяем текст ошибки')


# -------------------------Utils------------------------------------------------------------------------

def prepare_test_data_for_payment(payment_data, with_refund=False):
    dt = datetime.now().replace(microsecond=0)

    data = payment_data.new(
            dt=dt, #t_payment.dt
            payment_row_dt=dt - relativedelta(seconds=1), #dt из json'а в t_payment.payment_rows
            payment_dt=dt - relativedelta(seconds=4), #t_payment.dt
            start_dt_utc=dt - relativedelta(days=3), #start_dt_utc из json'а в t_payment.payment_rows
            payment_row_update_dt=dt - relativedelta(seconds=30), #update_dt из json'а в t_payment.payment_rows
            postauth_dt=dt + relativedelta(seconds=8), #t_payment.postauth_dt
            trust_payment_id=steps.SimpleApi.generate_fake_trust_payment_id(),
            payment_id=get_payment_id(),
            purchase_token=get_purchase_token(),
            service_order_id=get_service_order_id(),
            default_user=DEFAULT_USER,
            json_id=get_json_id()
    )

    if with_refund:
        data = data.new(
            trust_refund_id = steps.SimpleApi.generate_fake_trust_payment_id(),
            payment_id_for_refund = get_payment_id(),
            cancel_dt = dt + relativedelta(seconds=15), #t_payment.cancel_dt
            dt_for_refund = dt + relativedelta(seconds=13), #t_payment.dt
            payment_dt_for_refund = dt + relativedelta(seconds=14) #t_payment.payment_dt
        )
    return data


def prepare_json_for_payment(data):
    # выбираем id сервисного продукта партнера (по нему определяется партнер, которому идет выплата)
    service_product = \
        db.balance().execute("select id from t_service_product where partner_id = :client_id",
                             {'client_id': data.client_id})[0][
            'id']

    # подготавливаем json для вставки в t_payment.payment_rows
    json_for_payment = json.dumps([{"fiscal_nds": "",
                                    "fiscal_inn": "",
                                    "fiscal_title": "",
                                    "price": str(data.price),
                                    "id": data.json_id,
                                    "amount": str(data.amount),
                                    "source_id": DEFAULT_JSON_SOURCE_ID,
                                    "cancel_dt": None,
                                    "order": {
                                        "region_id": data.region_id.id,
                                        "contract_id": "",
                                        "update_dt": time.mktime(data.payment_row_update_dt.timetuple()),
                                        "text": "",
                                        "price": "",
                                        "service_order_id_number": DEFAULT_JSON_SERVICE_ORDER_ID_NUMBER,
                                        "start_dt_utc": time.mktime(data.start_dt_utc.timetuple()),
                                        "clid": "",
                                        "service_order_id": data.service_order_id,
                                        "service_product_id": service_product,
                                        "start_dt_offset": data.dt_offset,
                                        "service_id": data.service.id,
                                        "dt": time.mktime(data.payment_row_dt.timetuple()),
                                        "passport_id": data.default_user.id_,
                                        "commission_category": str(data.commission_category)},
                                    "quantity": DEFAULT_JSON_QTY}])
    return json_for_payment


def get_payment_id():
    return db.balance().sequence_nextval('s_payment_id')


def get_purchase_token():
    return db.balance().sequence_nextval('S_TEST_TRUST_PAYMENT_ID')


def get_service_order_id():
    return db.balance().sequence_nextval('s_request_order_id')


def get_json_id():
    return db.balance().sequence_nextval('s_request_order_id')


def insert_payment(data, json_for_payment, with_refund=False):
    # вставляем данные по платежу в t_payment и t_ccard_bound_payment

    query_insert_to_payment = "Insert into t_payment (ID, DT, CREATOR_UID, PAYSYS_CODE, AMOUNT," \
                              "CURRENCY,PAYMENT_DT,USER_IP,RESP_CODE,RESP_DESC,SERVICE_ID,SOURCE_SCHEME," \
                              "TERMINAL_ID,TRANSACTION_ID,POSTAUTH_DT,POSTAUTH_AMOUNT,RRN," \
                              "TRUST_PAYMENT_ID,PURCHASE_TOKEN,PAYMENT_ROWS,EXPORT_FROM_TRUST,CANCEL_DT) " \
                              "values (:payment_id,:dt,:passport_id,'TRUST',:amount," \
                              ":currency,:payment_dt,:user_ip,'success','paid ok',:service_id,'bs'," \
                              "'96013105','h62m8wiroe4dxxq0ludd',:postauth_dt,:amount,'58985'," \
                              ":trust_payment_id,:purchase_token,TO_CLOB(:json_for_payment),:export_from_trust," \
                              ":cancel_dt)"

    query_insert_ccard_bound_payment = "Insert into t_ccard_bound_payment (ID,PAYMENT_METHOD,TRUST_PAYMENT_ID," \
                                       "PURCHASE_TOKEN, START_DT,USER_PHONE," \
                                       "USER_EMAIL,POSTAUTH_DT,POSTAUTH_AMOUNT,RRN)  " \
                                       "values (:payment_id,:payment_method,:trust_payment_id," \
                                       ":purchase_token,:dt,'+79999999999'," \
                                       "'test@test.ru',:postauth_dt,:amount,'58985')"

    params = {'payment_id': data.payment_id, 'passport_id': str(data.default_user.id_),
              'json_for_payment': json_for_payment,
              'amount': data.amount, 'service_id': data.service.id, 'trust_payment_id': data.trust_payment_id,
              'user_ip': user_ip, 'currency': data.currency.char_code, 'export_from_trust': datetime.now(),
              'dt': data.dt, 'payment_dt': data.payment_dt,
              'postauth_dt': data.postauth_dt, 'purchase_token': data.purchase_token,
              'payment_method': data.payment_method, 'cancel_dt': data.cancel_dt}

    db.balance().execute(query_insert_to_payment, params)
    db.balance().execute(query_insert_ccard_bound_payment, params)
    return


def insert_refund(data, json_for_payment):
    # вставляем данные по рефанду в t_payment и t_refund
    query_insert_to_payment = "Insert into t_payment (ID, DT, PAYSYS_CODE, AMOUNT," \
                              "CURRENCY,PAYMENT_DT,RESP_CODE,SERVICE_ID,SOURCE_SCHEME," \
                              "PAYMENT_ROWS,REFUND_TO) " \
                              "values (:payment_id,:dt,'REFUND',:amount," \
                              ":currency,:payment_dt,'success',:service_id,'bs'," \
                              "TO_CLOB(:json_for_payment),'paysys')"

    query_insert_refund = "Insert into T_REFUND (ID,DESCRIPTION," \
                          "ORIG_PAYMENT_ID,TRUST_REFUND_ID) " \
                          "values (:payment_id,'test1'," \
                          ":orig_payment_id,:trust_refund_id)"

    params = {'payment_id': data.payment_id_for_refund, 'json_for_payment': json_for_payment,
              'amount': data.amount, 'service_id': data.service.id, 'currency': data.currency.char_code,
              'dt': data.dt_for_refund, 'payment_dt': data.payment_dt_for_refund, 'payment_method': data.payment_method,
              'orig_payment_id': data.payment_id, 'trust_refund_id': data.trust_refund_id}

    db.balance().execute(query_insert_to_payment, params)
    db.balance().execute(query_insert_refund, params)
    return


def create_payment(data, with_export=True):
    # подготавливаем json для t_payment.payment_rows
    json_for_payment = prepare_json_for_payment(data)
    # вставляем данные по платежу в t_payment и t_ccard_bound_payment
    insert_payment(data, json_for_payment)
    # всталяем строку в t_export для созданного платежа
    steps.ExportSteps.create_export_record(data.payment_id, Export.Classname.PAYMENT, Export.Type.THIRDPARTY_TRANS)
    if with_export:
        # разбираем платеж в t_export
        steps.CommonPartnerSteps.export_payment(data.payment_id)


def create_refund(data):
    # подготавливаем json для t_payment.payment_rows
    json_for_payment = prepare_json_for_payment(data)
    # вставляем данные по платежу в t_payment и t_ccard_bound_payment
    insert_payment(data, json_for_payment, with_refund=True)
    # вставляем данные по рефанду в t_payment и t_refund
    insert_refund(data, json_for_payment)
    # всталяем строку в t_export для созданного рефанда
    steps.ExportSteps.create_export_record(data.payment_id_for_refund, Export.Classname.PAYMENT,
                                           Export.Type.THIRDPARTY_TRANS)
    # разбираем рефанд в t_export
    steps.CommonPartnerSteps.export_payment(data.payment_id_for_refund)


def create_expected_payment_data(context, data, is_refund=False):
    # если валюта платежа отличается от валюты договора, то конвертируем в валюту договора amount и yandex_reward
    # (total_sum остается в валюте платежа, но это поле мы не проверяем в тесте)
    # курс подбираем по дате поездки в start_dt_utc + offset
    # источник курса подбираем по ЦБ страны договора
    order_dt = data.start_dt_utc + relativedelta(hours=data.dt_offset)
    currency_rate = steps.CurrencySteps.get_currency_rate(order_dt, context.currency.char_code,
                                                          data.currency.char_code,
                                                          context.currency_rate_src.id)
    data.amount = round(data.amount / currency_rate, 2)

    additional_params = {
        'amount': data.amount,
        'dt': data.dt_for_refund if is_refund else data.postauth_dt,
        'transaction_dt': data.dt_for_refund if is_refund else data.payment_dt,
        'yandex_reward': get_yandex_reward(context, data, is_refund=is_refund),
        'service_order_id_str': data.service_order_id,
        'order_service_id': data.service.id,
        'service_id': data.service.id
    }

    if data.payment_method == PaymentType.COMPENSATION:
        additional_params.update({
            'paysys_type_cc': PaysysType.YANDEX,
            'payment_type': PaymentType.COMPENSATION
        })
    return steps.SimpleApi.create_expected_tpt_row(context, data.client_id, data.contract_id, data.person_id,
                                                   data.trust_payment_id, data.payment_id,
                                                   trust_refund_id= data.trust_refund_id if is_refund else None,
                                                   **additional_params)


def get_yandex_reward(context, data, is_refund=False):
    pct = context.special_contract_params['partner_commission_pct2'] \
        if context.special_contract_params.has_key('partner_commission_pct2') \
        else Decimal('0')
    yandex_reward = max(context.min_commission, round(Decimal(data.amount) * pct / Decimal('100'), context.precision))
    if data.payment_method == PaymentType.COMPENSATION or is_refund:
        yandex_reward=None
    if pct == Decimal('0') and not is_refund \
            and context.firm in (Firms.TAXI_BV_22, Firms.UBER_115, Firms.YANDEX_GO_ISRAEL_35, Firms.UBER_AZ_116,
                                 Firms.MLU_EUROPE_125, Firms.MLU_AFRICA_126, Firms.YANDEX_GO_SRL_127):
        yandex_reward = Decimal('0')

    # BALANCE-33907: С 1 мая включаем добавление НДС на АВ в Такси Казахстане и Азербайджане
    if yandex_reward and context.firm in (Firms.UBER_AZ_116, Firms.TAXI_KAZ_24, Firms.TAXI_CORP_KZT_31)\
            and data.payment_dt >= datetime(2020, 5, 1):
        yandex_reward = btestlib.utils.get_sum_with_nds(yandex_reward, context.nds.pct_on_dt(data.payment_dt))

    return yandex_reward
