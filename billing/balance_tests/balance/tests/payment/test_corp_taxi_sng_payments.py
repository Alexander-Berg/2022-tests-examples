# -*- coding: utf-8 -*-

__author__ = 'atkaya', 'yuelyasheva'

from datetime import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import TransactionType, NdsNew, Firms, Services
from btestlib.data.partner_contexts import CORP_TAXI_KZ_CONTEXT_SPENDABLE, CORP_TAXI_KZ_CONTEXT_GENERAL, \
    CORP_TAXI_ARM_CONTEXT_SPENDABLE, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, \
    CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Cash

PAYMETHOD = Cash()

DEFAULT_PRICE = D('5000.01')
SERVICE_PRICE = D('0.1')

# облагается ли НДС сама фирма
NDS_FIRM = {Firms.TAXI_CORP_ARM_122: NdsNew.ZERO, Firms.TAXI_CORP_KZT_31: NdsNew.KAZAKHSTAN}


def create_clients_persons_contracts(user, nds, context_spendable, context_general=None):
    # создаем клиента и плательщиков (обычный и партнер) для таксопарка
    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(context_spendable.service)
    service_product_id_fee = steps.SimpleApi.create_service_product(context_spendable.service, taxi_client_id,
                                                                    service_fee=1)

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    additional_params = {
        'nds': nds.nds_id
    }

    _, taxi_person_partner_id, taxi_contract_spendable_id, _ = steps.ContractSteps.create_partner_contract(
        context_spendable, client_id=taxi_client_id,
        additional_params=additional_params)

    # для Армении сейчас все проводится руками, договора с корпом нет
    if context_general:
        _, _, corp_contract_id, _ = steps.contract_steps.ContractSteps.create_partner_contract(
            context_general, client_id=corp_client_id,
            additional_params=additional_params)

    return service_product_id, corp_client_id, taxi_contract_spendable_id, taxi_client_id, taxi_person_partner_id, \
           service_product_id_fee


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# платеж с таксопраком
@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.PAYMENT, Features.TRUST, Features.CORPORATE)
@pytest.mark.parametrize('nds, context_spendable, context_general, is_refund', [
    (NdsNew.KAZAKHSTAN, CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, False),
    (NdsNew.NOT_RESIDENT, CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, False),
    (NdsNew.NOT_RESIDENT, CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED, None, False),
    (NdsNew.KAZAKHSTAN, CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, True),
    (NdsNew.NOT_RESIDENT, CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, True),
    (NdsNew.NOT_RESIDENT, CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED, None, True)
], ids=[
    'Kazakhstan Default VAT',
    'Kazakhstan 0 VAT',
    'Armenia',
    'Kazakhstan Default VAT refund',
    'Kazakhstan 0 VAT refund',
    'Armenia refund'
])
def test_corp_taxi_payment(switch_to_trust, nds, context_spendable, context_general, is_refund, get_free_user):
    switch_to_trust(service=context_spendable.service)
    user = get_free_user()

    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id, service_product_id_fee = create_clients_persons_contracts(user, nds,
                                                                                                      context_spendable,
                                                                                                      context_general)

    # создаем платеж
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context_spendable.service,
                                                       [service_product_id, service_product_id_fee],
                                                       prices_list=[DEFAULT_PRICE, SERVICE_PRICE],
                                                       paymethod=PAYMETHOD,
                                                       currency=context_spendable.payment_currency,
                                                       user=user,
                                                       order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    if is_refund:
        # создаем рефанд
        trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(context_spendable.service, service_order_id_list,
                                                                             trust_payment_id,
                                                                             delta_amount_list=[DEFAULT_PRICE,
                                                                                                SERVICE_PRICE])

        # запускаем обработку рефанда
        steps.CommonPartnerSteps.export_payment(refund_id)
    else:
        trust_refund_id = None

    # если таксопарк на УСН (НЕ облагается НДС), а сама фирма на ОСН (облагается НДС),
    # то будем накидывать НДС фирмы на client_amount
    nds_for_amount = NDS_FIRM[context_spendable.firm].koef_on_dt(datetime.today()) if nds == NdsNew.NOT_RESIDENT else 1

    # формируем шаблон для сравнения
    expected_template = steps.SimpleApi.create_expected_tpt_data(context_spendable, taxi_client_id,
                                                                 taxi_contract_spendable_id,
                                                                 taxi_person_partner_id, trust_payment_id, payment_id, [
                                                                     {
                                                                         'amount': SERVICE_PRICE,
                                                                         'internal': 1,
                                                                         'client_amount': SERVICE_PRICE * nds_for_amount,
                                                                         'client_id': corp_client_id
                                                                     },
                                                                     {
                                                                         'amount': DEFAULT_PRICE,
                                                                         'client_amount': DEFAULT_PRICE * nds_for_amount,
                                                                         'client_id': corp_client_id,
                                                                         # основную строку платежа для Армении не грузим в оебс
                                                                         'internal': 1 if context_spendable.firm==Firms.TAXI_CORP_ARM_122 else None
                                                                     }
                                                                 ],
                                                                 trust_refund_id=trust_refund_id)

    # проверяем платеж или рефанд
    payment_data = steps.CommonPartnerSteps.\
        get_thirdparty_transaction_by_payment_id(payment_id, TransactionType.REFUND if is_refund else TransactionType.PAYMENT)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_template), 'Сравниваем платеж с шаблоном')
