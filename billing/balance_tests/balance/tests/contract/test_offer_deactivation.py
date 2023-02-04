# coding: utf-8
__author__ = 'a-vasin'

# https://st.yandex-team.ru/BALANCE-25835

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import equal_to

from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import reporter
from btestlib import utils
from btestlib.constants import ContractSubtype, Currencies, OfferConfirmationType, \
    OEBSOperationType, Services, OEBSSourceType
from btestlib.data.partner_contexts import *

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
DEFAULT_TIMEOUT = 5
DEFAULT_TERM = 10
DEFAULT_AMOUNT = {
    Currencies.RUB: 100,
    Currencies.EUR: 5,
    Currencies.USD: 5,
    Currencies.BYN: 100
}


@pytest.mark.parametrize("time_shift, is_suspended", [
    (DEFAULT_TIMEOUT, 1),
    (DEFAULT_TIMEOUT - 1, 0),
], ids=['AFTER_TIMEOUT', 'BEFORE_TIMEOUT'])
def test_deactivation_date(time_shift, is_suspended):
    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT)

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=time_shift))
    check_contract_deactivation_state(client_id, is_suspended)


@pytest.mark.parametrize("transactions, is_suspended, context", [
    pytest.param([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)], 0, TAXI_RU_CONTEXT,
                 marks=[pytest.mark.smoke(), pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_3_Taxi))]),
    ([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)], 0, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP),
    ([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)], 0, DRIVE_B2B_CONTEXT),
    pytest.mark.smoke(([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)], 0,
                       CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED)),

    ([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.INSERT, None)], 0, TAXI_RU_CONTEXT),

    pytest.mark.smoke(
            ([(DEFAULT_AMOUNT[Currencies.RUB] - 1, OEBSOperationType.ONLINE, None)], 1, TAXI_RU_CONTEXT)),

    ([(DEFAULT_AMOUNT[Currencies.RUB] - 1, OEBSOperationType.ONLINE, None)], 1, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP),
    ([(DEFAULT_AMOUNT[Currencies.RUB] - 1, OEBSOperationType.ONLINE, None)], 1, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED),
    pytest.param([(DEFAULT_AMOUNT[Currencies.RUB] - 1, OEBSOperationType.INSERT, None)], 1, TAXI_RU_CONTEXT, marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_2_Taxi))),
    pytest.param([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.NONE, None)], 1, TAXI_RU_CONTEXT, marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_3_Taxi))),
    ([
         (DEFAULT_AMOUNT[Currencies.RUB] / 3 + 1, OEBSOperationType.INSERT, None),
         (DEFAULT_AMOUNT[Currencies.RUB] / 3 + 1, OEBSOperationType.ONLINE, None),
         (DEFAULT_AMOUNT[Currencies.RUB] / 3 + 1, OEBSOperationType.ACTIVITY, OEBSSourceType.LOST_SUMS),
     ], 1, TAXI_RU_CONTEXT),
    ([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ACTIVITY, OEBSSourceType.LOST_SUMS)], 1, TAXI_RU_CONTEXT),
    ([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ACTIVITY, None)], 1, TAXI_RU_CONTEXT),
    pytest.param([(DEFAULT_AMOUNT[Currencies.RUB] - 1, OEBSOperationType.ONLINE, None)], 1, FOOD_CORP_CONTEXT, marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_5_Eda))),
    pytest.param([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)], 0, FOOD_CORP_CONTEXT, marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_5_Eda))),
    ([(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)], 0, DRIVE_CORP_CONTEXT),
], ids=[
    'ONLINE Taxi',
    'ONLINE Corporate Taxi 650',
    'ONLINE Corporate Taxi 135 and 650',
    'ONLINE Drive B2b',
    'INSERT',
    'ONLINE_LESSER Taxi',
    'ONLINE_LESSER Corp Taxi 650',
    'ONLINE_LESSER Corp Taxi 135 and 650',
    'INSERT_LESSER',
    'NONE',
    'INSERT_AND_ONLINE_AND_ACTIVITY',
    'ACTIVITY_LOST_FUNDS',
    'ACTIVITY_WITHOUT_SOURCE_TYPE',
    'ONLINE Corporate Food 668',
    'ONLINE_LESSER Corporate Food 668',
    'ONLINE Drive Corp',
])
def test_deactivation_transactions(transactions, is_suspended, context):
    client_id, _, contract_id = create_offer(context)
    create_transactions(client_id, transactions, CONTRACT_START_DT)

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT))
    check_contract_deactivation_state(client_id, is_suspended)


@pytest.mark.parametrize("context", [
    (TAXI_BV_GEO_USD_CONTEXT),
    (TAXI_BV_LAT_EUR_CONTEXT)
], ids=lambda c: c.iso_code)
@pytest.mark.parametrize("offset, is_suspended", [
    (0, 0),
    (-1, 1)
], ids=['EQUAL', 'LESSER'])
def test_deactivation_currency(context, offset, is_suspended):
    client_id, _, contract_id = create_offer(context)
    create_transactions(client_id, [(DEFAULT_AMOUNT[context.currency] + offset, OEBSOperationType.ONLINE, None)],
                        CONTRACT_START_DT)

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT))
    check_contract_deactivation_state(client_id, is_suspended)


@pytest.mark.parametrize('context, payment_in_different_invoices',
                         [
                             pytest.param(TAXI_RU_CONTEXT, True,
                                          marks=[pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_2_Taxi)),
                                                 pytest.mark.audit(reporter.feature(AuditFeatures.RV_C03_3_Taxi))]),
                             (CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, False),
                         ],
                         ids=[
                             'Taxi with payments in different invoices',
                             'Taxi corporate',
                         ])
def test_reactivation(context, payment_in_different_invoices):
    deactivation_dt = CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT)

    client_id, _, contract_id = create_offer(context)

    steps.TaxiSteps.process_offer_activation(contract_id, deactivation_dt)
    check_contract_deactivation_state(client_id, 1)

    if payment_in_different_invoices:
        _, invoice_eid1, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, context.service)
        _, invoice_eid2, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, Services.TAXI_111)
        for invoice in [invoice_eid1, invoice_eid2]:
            steps.TaxiSteps.create_cash_payment_fact(invoice, DEFAULT_AMOUNT[Currencies.RUB] / 2,
                                                     deactivation_dt + relativedelta(days=1), OEBSOperationType.ONLINE)
    else:
        create_transactions(client_id, [(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)],
                            deactivation_dt + relativedelta(days=1))

    steps.TaxiSteps.process_offer_activation(contract_id, deactivation_dt + relativedelta(days=1))
    check_contract_deactivation_state(client_id, 0)


# смотрим, что не удается реактивировать оферту, которая только is_suspended
def test_is_suspended_reactivation():
    client_id, person_id, contract_id, _ = steps.ContractSteps. \
        create_partner_contract(TAXI_RU_CONTEXT, is_offer=1,
                                additional_params={'offer_confirmation_type': OfferConfirmationType.MIN_PAYMENT.value,
                                                   'start_dt': CONTRACT_START_DT})
    steps.ContractSteps.insert_attribute(contract_id, 'IS_SUSPENDED', value_dt=CONTRACT_START_DT)
    create_transactions(client_id, [(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)],
                        CONTRACT_START_DT + relativedelta(days=6))
    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=6))
    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts[0]['IS_SUSPENDED'], equal_to(1), u'Проверяем состояние договора')


# Стоит совместить с первым тестом, не проверяя работу для дефолтного параметра, а проверяя только то, что в случае,
# если не задать значение - будет проставлен дефолт
def deactivation_term():
    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT, deactivation_term=DEFAULT_TERM)
    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TERM - 1))
    check_contract_deactivation_state(client_id, 0)

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TERM))
    check_contract_deactivation_state(client_id, 1)


# Стоит совместить с первым тестом, не проверяя работу для дефолтного параметра, а проверяя только то, что в случае,
# если не задать значение - будет проставлен дефолт
def test_deactivation_amount():
    deactivation_amount = DEFAULT_AMOUNT[Currencies.RUB] + Decimal('1.5')

    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT, deactivation_amount=deactivation_amount)
    create_transactions(client_id, [(DEFAULT_AMOUNT[Currencies.RUB], OEBSOperationType.ONLINE, None)],
                        CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT))
    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT))
    check_contract_deactivation_state(client_id, 1)

    create_transactions(client_id, [(Decimal('1.5'), OEBSOperationType.ONLINE, None)],
                        CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT + 1))
    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT + 1))
    check_contract_deactivation_state(client_id, 0)


def test_offer_confirmation_type_no():
    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT, offer_confirmation_type=OfferConfirmationType.NO)

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT))
    check_contract_deactivation_state(client_id, 0)


# если оферта создана сразу деативированной, то она не активируется, если нет поступлений
def test_offer_deactivated_by_creation():
    deactivation_term = 50
    deactivation_amount = Decimal('1000')

    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT, deactivation_term=deactivation_term,
                                             deactivation_amount=deactivation_amount,
                                             is_deactivated=True)

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TIMEOUT))
    check_contract_deactivation_state(client_id, 1)


@pytest.mark.tickets('BALANCE-29086')
@reporter.feature(Features.CONTRACT, Features.OFFER)
def test_no_deactivation_if_activated_once():
    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT,
                                             deactivation_term=DEFAULT_TERM,
                                             deactivation_amount=DEFAULT_AMOUNT[Currencies.RUB])

    with reporter.step(u'Добавляем в базу запись о том, что договор-офферта был активирована'):
        steps.CommonSteps.set_extprops('Contract', contract_id, 'offer_accepted', {'value_num': 1})

    steps.TaxiSteps.process_offer_activation(contract_id, CONTRACT_START_DT + relativedelta(days=DEFAULT_TERM + 1))
    check_contract_deactivation_state(client_id, 0)


@pytest.mark.tickets('BALANCE-29086')
@reporter.feature(Features.CONTRACT, Features.OFFER)
def test_no_deactivation_in_case_of_refund_if_activated():
    DEACTIVATION_AMOUNT = DEFAULT_AMOUNT[Currencies.RUB]
    LAST_DAY_WITHOUT_ACTIVATION = CONTRACT_START_DT + relativedelta(days=DEFAULT_TERM)
    FIRST_DAY_ACTIVATION_NEEDED = CONTRACT_START_DT + relativedelta(days=DEFAULT_TERM + 1)

    client_id, _, contract_id = create_offer(TAXI_RU_CONTEXT, deactivation_amount=DEACTIVATION_AMOUNT,
                                             deactivation_term=DEFAULT_TERM)

    _, invoice_eid, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, Services.TAXI_111)

    # производим оплату необходимой для активации контракта суммы
    steps.TaxiSteps.create_cash_payment_fact(invoice_eid, DEACTIVATION_AMOUNT,
                                             LAST_DAY_WITHOUT_ACTIVATION, OEBSOperationType.INSERT)

    steps.TaxiSteps.process_offer_activation(contract_id, LAST_DAY_WITHOUT_ACTIVATION)
    check_contract_deactivation_state(client_id, 0)

    # возврат - теперь не счету сумма, меньше необходимой для активации
    steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -1,
                                             FIRST_DAY_ACTIVATION_NEEDED, OEBSOperationType.INSERT)

    steps.TaxiSteps.process_offer_activation(contract_id, FIRST_DAY_ACTIVATION_NEEDED)
    check_contract_deactivation_state(client_id, 0)


# ---------------------------------------
# Utils

def create_offer(context, offer_confirmation_type=OfferConfirmationType.MIN_PAYMENT,
                 deactivation_term=None, deactivation_amount=None, is_deactivated=None):
    with reporter.step(u'Подготавливаем договор такси с помощью CreateOffer'):
        additional_params = {'offer_confirmation_type': offer_confirmation_type.value,
                             'start_dt': CONTRACT_START_DT}
        if deactivation_term:
            additional_params.update({'offer_activation_due_term': deactivation_term})
        if deactivation_amount:
            additional_params.update({'offer_activation_payment_amount': deactivation_amount})
        if is_deactivated:
            additional_params.update({'is_suspended': 1, 'is_deactivated': 1})

        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=1,
                                                                                           additional_params=additional_params)
        return client_id, person_id, contract_id


# def ycreate_offer(contract_data=GenDefParams.YANDEX_TAXI,
#                  currency=Currencies.RUB,
#                  services=None,
#                  offer_confirmation_type=OfferConfirmationType.MIN_PAYMENT,
#                  deactivation_term=None, deactivation_amount=None, is_deactivated=None,
#                  client_id=None, person_id=None):
#     with reporter.step(u'Подготавливаем договор такси с помощью CreateOffer'):
#         if not client_id:
#             client_id = steps.ClientSteps.create()
#         if not person_id:
#             person_id = steps.PersonSteps.create(client_id, contract_data['PERSON_TYPE'])
#         additional_params = {'offer_confirmation_type': offer_confirmation_type.value,
#                              'start_dt': CONTRACT_START_DT,
#                              'currency': currency}
#         if deactivation_term:
#             additional_params.update({'offer_activation_due_term': deactivation_term})
#         if deactivation_amount:
#             additional_params.update({'offer_activation_payment_amount': deactivation_amount})
#         if services:
#             additional_params.update({'services': services})
#         if is_deactivated:
#             additional_params.update({'is_suspended': 1, 'is_deactivated': 1})
#
#         if Services.TAXI_CORP.id in contract_data['CONTRACT_PARAMS']['SERVICES']:
#             additional_params.update({'ctype': 'GENERAL'})
#         contract_id, _, _ = steps.ContractSteps.create_person_and_offer_with_additional_params(client_id,
#                                                                                                contract_data,
#                                                                                                # remove_params=remove_params,
#                                                                                                additional_params=additional_params,
#                                                                                                is_offer=1,
#                                                                                                person_id=person_id)
#         return client_id, person_id, contract_id


def create_transactions(client_id, transactions, dt):
    invoice_eid = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['external_id']

    for amount, type, source_type in transactions:
        steps.TaxiSteps.create_cash_payment_fact(invoice_eid, amount, dt, type, source_type=source_type)


def check_contract_deactivation_state(client_id, is_suspended):
    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts[0]['IS_SUSPENDED'], equal_to(is_suspended), u'Проверяем состояние договора')
    utils.check_that(contracts[0]['IS_DEACTIVATED'], equal_to(is_suspended), u'Проверяем состояние договора')
