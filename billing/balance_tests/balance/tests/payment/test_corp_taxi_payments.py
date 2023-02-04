# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from copy import deepcopy

import pytest
from hamcrest import equal_to, contains_string

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import NdsNew, TransactionType, Services, Export, PaymentType
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, CORP_TAXI_RU_CONTEXT_SPENDABLE, \
    CORP_TAXI_RU_CONTEXT_SPENDABLE_DECOUP, CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Cash
from simpleapi.data.uids_pool import User

PAYMETHOD = Cash()

contract_start_dt = datetime.datetime.today().replace(day=1)

#TODO: free_users: перенести в defaults
USER_EMPTY_CORP_TAXI = User(313834851, 'atkaya-test-3', None)

PAYMENT_TYPES = [
    PaymentType.CORP_TAXI_PARTNER_TRIP_PAYMENT,
    PaymentType.CORP_TAXI_CARGO,
    PaymentType.CORP_TAXI_DELIVERY,
]


def create_clients_persons_contracts(user, contract_services):
    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(Services.TAXI_CORP)
    context_spendable = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(contract_services=contract_services)
    context_general = CORP_TAXI_RU_CONTEXT_GENERAL.new(contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id])

    _, taxi_person_id, taxi_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context_spendable, client_id=taxi_client_id,
                                additional_params={'start_dt': contract_start_dt})

    corp_client_id, corp_person_id, corp_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context_general,
                                additional_params={'start_dt': contract_start_dt})

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    return service_product_id, corp_client_id, taxi_contract_id, \
        taxi_client_id, taxi_person_id


def create_clients_persons_contracts_sidepayments(context=None):
    client_id = steps.ClientSteps.create()
    if context:
        _, person_id, contract_id, _ = steps.ContractSteps. \
            create_partner_contract(context, client_id=client_id,
                                    additional_params={'start_dt': contract_start_dt})
    else:
        context_spendable = CORP_TAXI_RU_CONTEXT_SPENDABLE.new(contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id])
        context_general = CORP_TAXI_RU_CONTEXT_GENERAL.new(contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id])

        _, person_id, contract_id, _ = steps.ContractSteps. \
            create_partner_contract(context_spendable, client_id=client_id,
                                    additional_params={'start_dt': contract_start_dt})

        corp_client_id, corp_person_id, corp_contract_id, _ = steps.ContractSteps. \
            create_partner_contract(context_general,
                                    additional_params={'start_dt': contract_start_dt})

    return client_id, person_id, contract_id


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
@pytest.mark.parametrize('nds', [NdsNew.DEFAULT, NdsNew.NOT_RESIDENT], ids=['Default nds', '0 nds'])
def test_corp_taxi_payment(nds, switch_to_trust, get_free_user):
    contract_services = [Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id]

    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    user = get_free_user()

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user, contract_services)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(Services.TAXI_CORP,
                                             service_product_id,
                                             paymethod=PAYMETHOD,
                                             user=user,
                                             order_dt=utils.Date.moscow_offset_dt())

    steps.CommonPartnerSteps.export_payment(payment_id)

    client_amount = simpleapi_defaults.DEFAULT_PRICE
    # TODO: брать дату koef_on_dt не текущую, а дату транзакции
    if nds == NdsNew.NOT_RESIDENT:
        client_amount *= nds.koef_on_dt(datetime.datetime.now())

    expected_payment = steps.SimpleApi.create_expected_tpt_row(CORP_TAXI_RU_CONTEXT_SPENDABLE, taxi_client_id,
                                                               taxi_contract_spendable_id,
                                                               taxi_person_partner_id, trust_payment_id, payment_id,
                                                               client_id=corp_client_id, client_amount=client_amount,
                                                               service_order_id_str=str(service_order_id),
                                                               )
    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)

    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.smoke
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_SPENDABLE_DECOUP,
    CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP,
])
def test_corp_taxi_sidepayment(context):
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments(context=context)
    for payment_type in PAYMENT_TYPES:
        # создаем сайдпеймент
        side_payment_id, transaction_id_payment = \
            steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                              simpleapi_defaults.DEFAULT_PRICE,
                                                              payment_type,
                                                              Services.TAXI_CORP_PARTNERS.id,
                                                              currency=context.currency,
                                                              extra_dt_0=datetime.datetime.now())

        # запускаем обработку сайдпеймента:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT)

        expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id,
                                                                   contract_id,
                                                                   person_id, transaction_id_payment,
                                                                   side_payment_id,
                                                                   payment_type=payment_type)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_payment_id,
                                                                                         source='sidepayment')
        utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                         'Сравниваем платеж с шаблоном')


@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
def test_no_active_contract_payment(switch_to_trust, get_free_user):
    contract_services = [Services.TAXI_CORP_PARTNERS.id]

    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    user = get_free_user()

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user, contract_services)

    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(Services.TAXI_CORP,
                                             service_product_id,
                                             paymethod=PAYMETHOD,
                                             user=user,
                                             order_dt=utils.Date.moscow_offset_dt())

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(payment_id)
    expected_error = 'TrustPayment({}) delayed: no active contracts found for client {}'.format(payment_id,
                                                                                                taxi_client_id)

    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE, Features.REFUND)
@pytest.mark.tickets('BALANCE-22114')
@pytest.mark.parametrize('nds', [NdsNew.DEFAULT, NdsNew.NOT_RESIDENT], ids=['Default nds', '0 nds'])
def test_corp_taxi_refund(nds, switch_to_trust, get_free_user):
    contract_services = [Services.TAXI_CORP.id, Services.TAXI_CORP_PARTNERS.id]

    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    user = get_free_user()

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user, contract_services)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(CORP_TAXI_RU_CONTEXT_SPENDABLE.service,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD,
                                             order_dt=utils.Date.moscow_offset_dt())
    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(CORP_TAXI_RU_CONTEXT_SPENDABLE.service, service_order_id,
                                                               trust_payment_id)

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    client_amount = simpleapi_defaults.DEFAULT_PRICE
    # TODO: брать дату koef_on_dt не текущую, а дату транзакции
    if nds == NdsNew.NOT_RESIDENT:
        client_amount *= nds.koef_on_dt(datetime.datetime.now())

    expected_payment = steps.SimpleApi.create_expected_tpt_row(CORP_TAXI_RU_CONTEXT_SPENDABLE, taxi_client_id,
                                                               taxi_contract_spendable_id,
                                                               taxi_person_partner_id, trust_payment_id, payment_id,
                                                               trust_refund_id,
                                                               client_id=corp_client_id, client_amount=client_amount,
                                                               service_order_id_str=str(service_order_id),
                                                               )
    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                     transaction_type=TransactionType.REFUND)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')


# @pytest.mark.parametrize('payment_type', PAYMENT_TYPES)
@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_SPENDABLE_DECOUP,
    CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP,
])
def test_corp_taxi_sidepayment_refund(context):
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments(context)

    for payment_type in PAYMENT_TYPES:
        # создаем сайд пеймент
        side_payment_id, transaction_id_payment = \
            steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                              simpleapi_defaults.DEFAULT_PRICE,
                                                              payment_type,
                                                              Services.TAXI_CORP_PARTNERS.id,
                                                              currency=context.currency,
                                                              extra_dt_0=datetime.datetime.now())

        # создаем рефанд в сайд пеймент
        side_refund_id, transaction_id_refund = \
            steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                              simpleapi_defaults.DEFAULT_PRICE,
                                                              payment_type,
                                                              Services.TAXI_CORP_PARTNERS.id,
                                                              currency=context.currency,
                                                              extra_dt_0=datetime.datetime.now(),
                                                              transaction_type=TransactionType.REFUND,
                                                              orig_transaction_id=transaction_id_payment)

        # запускаем обработку рефанда сайдпеймента:
        steps.ExportSteps.create_export_record_and_export(side_refund_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT)

        expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id,
                                                                   contract_id,
                                                                   person_id, transaction_id_payment,
                                                                   side_refund_id, transaction_id_refund,
                                                                   payment_type=payment_type)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_refund_id,
                                                                                         transaction_type=TransactionType.REFUND,
                                                                                         source='sidepayment')
        utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                         'Сравниваем платеж с шаблоном')


@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
def test_no_active_contract_refund(switch_to_trust, get_free_user):
    contract_services = [Services.TAXI_CORP_PARTNERS.id]

    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    user = get_free_user()

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user, contract_services)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(CORP_TAXI_RU_CONTEXT_SPENDABLE.service,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD,
                                             order_dt=utils.Date.moscow_offset_dt())
    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(CORP_TAXI_RU_CONTEXT_SPENDABLE.service, service_order_id,
                                                               trust_payment_id)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(refund_id)
    expected_error = 'Refund({}) delayed: no active contracts found for client {}'.format(refund_id, taxi_client_id)
    utils.check_that(xmlrpc_error.value.response, contains_string(expected_error), u"Проверяем текст ошибки")


# платеж без start_ts
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_payment_without_start_ts(switch_to_trust, get_free_user):
    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    # user = User(436363340, 'yb-atst-user-10', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
    user = get_free_user()

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user,
                                                                              contract_services=[Services.TAXI_CORP.id,
                                                                                                 Services.TAXI_CORP_PARTNERS.id])

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(CORP_TAXI_RU_CONTEXT_SPENDABLE.service,
                                             service_product_id,
                                             user=user,
                                             paymethod=PAYMETHOD)

    # запускаем обработку платежа
    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        api.test_balance().ExportObject('THIRDPARTY_TRANS', 'Payment', payment_id)

    export_error = xmlrpc_error.value.response

    order_id = None  # для json строк id не передается

    # ожидаемая ошибка
    expected_error = 'TrustPayment({}) delayed: Taxi start_dt not found for order "{}"'.format(payment_id, order_id)

    # сравниваем ошибки
    utils.check_that(export_error, equal_to(expected_error), 'Сравниваем ошибки')


# нет расходного договора, ошибка в платеже
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.tickets('BALANCE-22114')
def test_invalid_payment_contract_absense(switch_to_trust, get_free_user):
    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    # user = User(436363356, 'yb-atst-user-11', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
    user = get_free_user()

    taxi_order_dt = utils.Date.moscow_offset_dt()

    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(
        CORP_TAXI_RU_CONTEXT_SPENDABLE.service)

    context_general = CORP_TAXI_RU_CONTEXT_GENERAL.new(
        special_contract_params=deepcopy(CORP_TAXI_RU_CONTEXT_GENERAL.special_contract_params),
        contract_services=[Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id])
    corp_client_id, corp_person_id, corp_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context_general,
                                additional_params={'start_dt': contract_start_dt})

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    # создаем платеж
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(CORP_TAXI_RU_CONTEXT_SPENDABLE.service,
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
@pytest.mark.tickets('BALANCE-34815')
def test_payment_invalid_uid(switch_to_trust):
    user = USER_EMPTY_CORP_TAXI
    switch_to_trust(service=CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    _, service_product_id = steps.SimpleApi.create_partner_and_product(CORP_TAXI_RU_CONTEXT_SPENDABLE.service)
    db.clear_client(user)

    _, _, _, payment_id = steps.SimpleApi.create_trust_payment(
        CORP_TAXI_RU_CONTEXT_SPENDABLE.service,
        service_product_id,
        user=user,
        paymethod=PAYMETHOD,
        order_dt=utils.Date.moscow_offset_dt())

    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        api.test_balance().ExportObject('THIRDPARTY_TRANS', 'Payment', payment_id)

    error = xmlrpc_error.value.response
    expected = 'TrustPayment({payment_id}) processing error: No client found for passport_id: {passport_id}'.format(
        payment_id=payment_id, passport_id=user.id_
    )

    utils.check_that(error, equal_to(expected), u'Проверяем текст ошибки')
