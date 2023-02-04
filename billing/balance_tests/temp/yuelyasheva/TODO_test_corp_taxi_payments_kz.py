# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to, contains_string

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import secrets
from btestlib import utils
from btestlib.constants import Services, TransactionType
from btestlib.data import defaults
from btestlib.data import simpleapi_defaults
from btestlib.matchers import has_entries_casted
from simpleapi.common.payment_methods import Cash
from simpleapi.data.uids_pool import User

SERVICE = Services.TAXI_CORP
PAYMETHOD = Cash()

contract_start_dt = datetime.datetime.today().replace(day=1)

common_template = {
    'amount': simpleapi_defaults.DEFAULT_PRICE,
    'currency': defaults.taxi_corp()['DEFAULT_CURRENCY'],
    'partner_currency': defaults.taxi_corp()['DEFAULT_PARTNER_CURRENCY'],
    'service_id': defaults.taxi_corp()['SERVICE_ID'],
    'paysys_type_cc': defaults.taxi_corp()['PAYSYS_TYPE_CC'],
    'commission_currency': defaults.taxi_corp()['DEFAULT_COMMISSION_CURRENCY'],
    'yandex_reward': None,
    'amount_fee': None,
    'internal': None,
    'oebs_org_id': defaults.taxi_corp()['OEBS_ORG_ID'],
    'iso_currency': defaults.taxi_corp()['DEFAULT_ISO_CURRENCY'],
    'partner_iso_currency': defaults.taxi_corp()['DEFAULT_ISO_PARTNER_CURRENCY'],
    'commission_iso_currency': defaults.taxi_corp()['DEFAULT_ISO_COMMISSION_CURRENCY'],
    'invoice_eid': None,
    'row_paysys_commission_sum': None
}

order_sum = defaults.taxi_corp()['ORDER_SUM']

USER_EMPTY_CORP_TAXI = User(313834851, 'atkaya-test-3', None)


def get_order_id(trust_payment_id):
    with reporter.step(u'Получаем id заказа для платежа: {}'.format(trust_payment_id)):
        query = "SELECT ro.PARENT_ORDER_ID order_id " \
                "FROM T_CCARD_BOUND_PAYMENT cc JOIN T_REQUEST_ORDER ro ON cc.REQUEST_ID=ro.REQUEST_ID " \
                "WHERE cc.TRUST_PAYMENT_ID=:trust_payment_id"
        params = {'trust_payment_id': trust_payment_id}
        return db.balance().execute(query, params)[0]['order_id']


def create_clients_persons_contracts(user):
    # создаем клиента и плательщиков (обычный и партнер) для таксопарка
    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
    taxi_person_id = steps.PersonSteps.create(taxi_client_id, 'kzu')
    taxi_person_partner_id = steps.PersonSteps.create(taxi_client_id, 'kzu', {'is-partner': '1'})

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()
    corp_person_id = steps.PersonSteps.create(corp_client_id, 'kzu')

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    # создаем договоры (коммерческий с таксопарком, расходный с таксопарком и коммерческий с корпоративным клиентом)
    taxi_contract_id, taxi_contract_spendable_id, corp_contract_id = steps.TaxiSteps.create_contracts_for_corp_taxi(
        contract_start_dt,
        taxi_client_id, taxi_person_id, taxi_person_partner_id,
        corp_client_id, corp_person_id, currency='KZT')

    return service_product_id, corp_client_id, taxi_contract_spendable_id, \
           taxi_client_id, taxi_person_partner_id


def create_clients_persons_contracts_taxi_wo_nds(user):
    # создаем клиента и плательщиков (обычный и партнер) для таксопарка
    # taxi_client_id, service_product_id = steps.SimpleApi.create_taxi_partner_and_corp_product()
    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
    taxi_person_id = steps.PersonSteps.create(taxi_client_id, 'ur', {'kpp': '234567891'})
    taxi_person_partner_id = steps.PersonSteps.create(taxi_client_id, 'ur', {'is-partner': '1'})

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()
    corp_person_id = steps.PersonSteps.create(corp_client_id, 'ur', {'kpp': '234567890'})

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    # создаем договоры (коммерческий с таксопарком, расходный с таксопарком и коммерческий с корпоративным клиентом)
    taxi_contract_id, taxi_contract_spendable_id, corp_contract_id = steps.TaxiSteps.create_contracts_for_corp_taxi(
        contract_start_dt,
        taxi_client_id, taxi_person_id, taxi_person_partner_id,
        corp_client_id, corp_person_id, taxi_nds='0')

    return service_product_id, corp_client_id, taxi_contract_spendable_id, \
           taxi_client_id, taxi_person_partner_id


def clear_client(user):
    query = "UPDATE T_ACCOUNT SET CLIENT_ID=NULL WHERE PASSPORT_ID=:uid"
    params = {'uid': user.id_}
    db.balance().execute(query, params)


# платеж с таксопраком с ндс = 18
@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_corp_taxi_payment(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(defaults.taxi_corp()['UID_TO_LINK'], defaults.taxi_corp()['LOGIN_TO_LINK'], None)

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             paymethod=PAYMETHOD,
                                             user=user,
                                             order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # формируем шаблон для сравнения
    expected_template = common_template.copy()
    expected_template.update({'client_id': corp_client_id,
                              'contract_id': taxi_contract_spendable_id,
                              'partner_id': taxi_client_id,
                              'payment_id': payment_id,
                              'payment_type': defaults.taxi_corp()['PAYMENT_TYPE'],
                              'person_id': taxi_person_partner_id,
                              'transaction_type': defaults.taxi_corp()['TRANSACTION_TYPE_FOR_PAYMENT'],
                              'trust_id': trust_payment_id,
                              'trust_payment_id': trust_payment_id,
                              'client_amount': simpleapi_defaults.DEFAULT_PRICE
                              })

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж с шаблоном')


# компенсация с таксопраком с ндс = 18
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE, Features.COMPENSATION)
@pytest.mark.tickets('BALANCE-22114')
@pytest.mark.smoke
def test_corp_taxi_compensation(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363578, 'yb-atst-user-5', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user)

    # создаем платеж компенсацию
    _, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_compensation(SERVICE, service_product_id, user=user,
                                            order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # формируем шаблон для сравнения
    expected_template = common_template.copy()
    expected_template.update({'client_id': corp_client_id,
                              'contract_id': taxi_contract_spendable_id,
                              'partner_id': taxi_client_id,
                              'payment_id': payment_id,
                              'payment_type': defaults.taxi_corp()['PAYMENT_TYPE_COMPENSATION'],
                              'person_id': taxi_person_partner_id,
                              'transaction_type': defaults.taxi_corp()['TRANSACTION_TYPE_FOR_PAYMENT'],
                              'trust_id': trust_payment_id,
                              'trust_payment_id': trust_payment_id,
                              'client_amount': 0
                              })

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж компенсацию с шаблоном')


# рефанд с таксопраком с ндс = 18
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE, Features.REFUND)
@pytest.mark.tickets('BALANCE-22114')
@pytest.mark.smoke
def test_corp_taxi_refund(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363598, 'yb-atst-user-6', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD,
                                             order_dt=utils.Date.moscow_offset_dt())
    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id)

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    # формируем шаблон для сравнения
    expected_template = common_template.copy()
    expected_template.update({'client_id': corp_client_id,
                              'contract_id': taxi_contract_spendable_id,
                              'partner_id': taxi_client_id,
                              'payment_id': payment_id,
                              'payment_type': defaults.taxi_corp()['PAYMENT_TYPE'],
                              'person_id': taxi_person_partner_id,
                              'transaction_type': defaults.taxi_corp()['TRANSACTION_TYPE_FOR_REFUND'],
                              'trust_id': trust_refund_id,
                              'trust_payment_id': trust_payment_id,
                              'client_amount': simpleapi_defaults.DEFAULT_PRICE
                              })

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем рефанд с шаблоном')


# платеж с таксопраком с ндс = 0
@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_corp_taxi_payment_wo_nds(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363623, 'yb-atst-user-7', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts_taxi_wo_nds(user)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD,
                                             order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # формируем шаблон для сравнения
    expected_template = common_template.copy()
    expected_template.update({'client_id': corp_client_id,
                              'contract_id': taxi_contract_spendable_id,
                              'partner_id': taxi_client_id,
                              'payment_id': payment_id,
                              'payment_type': defaults.taxi_corp()['PAYMENT_TYPE'],
                              'person_id': taxi_person_partner_id,
                              'transaction_type': defaults.taxi_corp()['TRANSACTION_TYPE_FOR_PAYMENT'],
                              'trust_id': trust_payment_id,
                              'trust_payment_id': trust_payment_id,
                              'client_amount': simpleapi_defaults.DEFAULT_PRICE * D('1.18')
                              })

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж с шаблоном')


# компенсация с таксопраком с ндс = 0
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE, Features.COMPENSATION)
@pytest.mark.tickets('BALANCE-22114')
def test_corp_taxi_compensation_wo_nds(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363645, 'yb-atst-user-8', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts_taxi_wo_nds(user)

    # создаем платеж компенсацию
    _, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_compensation(SERVICE, service_product_id, user=user,
                                            order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # формируем шаблон для сравнения
    expected_template = common_template.copy()
    expected_template.update({'client_id': corp_client_id,
                              'contract_id': taxi_contract_spendable_id,
                              'partner_id': taxi_client_id,
                              'payment_id': payment_id,
                              'payment_type': defaults.taxi_corp()['PAYMENT_TYPE_COMPENSATION'],
                              'person_id': taxi_person_partner_id,
                              'transaction_type': defaults.taxi_corp()['TRANSACTION_TYPE_FOR_PAYMENT'],
                              'trust_id': trust_payment_id,
                              'trust_payment_id': trust_payment_id,
                              'client_amount': 0
                              })

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем платеж компенсацию с шаблоном')


# рефанд с таксопраком с ндс = 0
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST,
                  Features.CORPORATE, Features.REFUND)
@pytest.mark.tickets('BALANCE-22114')
def test_corp_taxi_refund_wo_nds(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363660, 'yb-atst-user-9', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts_taxi_wo_nds(user)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD,
                                             order_dt=utils.Date.moscow_offset_dt())

    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id)

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    # формируем шаблон для сравнения
    expected_template = common_template.copy()
    expected_template.update({'client_id': corp_client_id,
                              'contract_id': taxi_contract_spendable_id,
                              'partner_id': taxi_client_id,
                              'payment_id': payment_id,
                              'payment_type': defaults.taxi_corp()['PAYMENT_TYPE'],
                              'person_id': taxi_person_partner_id,
                              'transaction_type': defaults.taxi_corp()['TRANSACTION_TYPE_FOR_REFUND'],
                              'trust_id': trust_refund_id,
                              'trust_payment_id': trust_payment_id,
                              'client_amount': simpleapi_defaults.DEFAULT_PRICE * D('1.18')
                              })

    # проверяем платеж
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND)[0]

    utils.check_that(payment_data, has_entries_casted(expected_template), 'Сравниваем рефанд с шаблоном')


# если отсутствует платеж с нужным типом в t_partner_taxi_stat
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_payment_without_start_ts(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363340, 'yb-atst-user-10', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD)

    # запускаем обработку платежа
    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        api.test_balance().ExportObject('THIRDPARTY_TRANS', 'Payment', payment_id)

    export_error = xmlrpc_error.value.response

    order_id = get_order_id(trust_payment_id)

    # ожидаемая ошибка
    expected_error = 'TrustPayment({}) delayed: Taxi start_dt not found for order "{}"'.format(payment_id, order_id)

    # сравниваем ошибки
    utils.check_that(export_error, equal_to(expected_error), 'Сравниваем ошибки')


# нет расходного договора, ошибка в платеже
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_invalid_payment_contract_absense(switch_to_trust):
    switch_to_trust(service=SERVICE)
    user = User(436363356, 'yb-atst-user-11', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))

    taxi_order_dt = utils.Date.moscow_offset_dt()

    # создаем клиента и плательщиков (обычный и партнер) для таксопарка
    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
    taxi_person_id = steps.PersonSteps.create(taxi_client_id, 'ur', {'kpp': '234567891'})
    taxi_person_partner_id = steps.PersonSteps.create(taxi_client_id, 'ur', {'is-partner': '1'})

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()
    corp_person_id = steps.PersonSteps.create(corp_client_id, 'ur', {'kpp': '234567890'})

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    # создаем договоры (коммерческий с таксопарком, расходный с таксопарком и коммерческий с корпоративным клиентом)
    taxi_contract_id, _ = steps.ContractSteps.create_contract('taxi_postpay',
                                                              {'CLIENT_ID': taxi_client_id,
                                                               'PERSON_ID': taxi_person_id,
                                                               'DT': contract_start_dt})

    corp_contract_id, _ = steps.ContractSteps.create_contract('taxi_corporate',
                                                              {'CLIENT_ID': corp_client_id,
                                                               'PERSON_ID': corp_person_id,
                                                               'DT': contract_start_dt})

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD,
                                             order_dt=taxi_order_dt)

    # ожидаемая ошибка
    expected_error = 'TrustPayment(' + str(payment_id) + ') delayed: no active contracts ' \
                                                         'found for client ' + str(taxi_client_id) + ' on ' + str(
        taxi_order_dt.strftime("%Y-%m-%d %H:%M:%S")) + ' for currency RUR'

    # запускаем обработку платежа
    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        api.test_balance().ExportObject('THIRDPARTY_TRANS', 'Payment', payment_id)

    export_error = xmlrpc_error.value.response

    # сравниваем ошибки
    utils.check_that(export_error, equal_to(expected_error), 'Сравниваем ошибки')


# uid без клиента
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_payment_invalid_uid(switch_to_trust):
    switch_to_trust(service=SERVICE)
    _, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
    clear_client(USER_EMPTY_CORP_TAXI)

    with pytest.raises(Exception) as export_exception:
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             user=USER_EMPTY_CORP_TAXI,
                                             paymethod=PAYMETHOD,
                                             order_dt=utils.Date.moscow_offset_dt())

    expected_error = 'Client not found for order'

    utils.check_that(export_exception.value.message, contains_string(expected_error), u'Проверяем текст ошибки')
