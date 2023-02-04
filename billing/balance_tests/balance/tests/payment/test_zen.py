# -*- coding: utf-8 -*-

from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Export, PartnerPaymentType
from btestlib.data.partner_contexts import (ZEN_SPENDABLE_CONTEXT, ZEN_SPENDABLE_SERVICES_AG_CONTEXT,
                                            ZEN_SPENDABLE_NEW_CONTEXT,
                                            ZEN_SPENDABLE_IP_CONTEXT, ZEN_SPENDABLE_UR_ORG_CONTEXT)
from btestlib.matchers import contains_dicts_with_entries

START_DT = utils.Date.first_day_of_month() - relativedelta(months=2)
AMOUNT = Decimal('1000.1')

@pytest.mark.parametrize('context, use_integration', [
    pytest.param(ZEN_SPENDABLE_CONTEXT, True),
    pytest.param(ZEN_SPENDABLE_CONTEXT, False),
    pytest.param(ZEN_SPENDABLE_IP_CONTEXT, True),
    pytest.param(ZEN_SPENDABLE_IP_CONTEXT, False, marks=pytest.mark.skip('only use with integration')),
    pytest.param(ZEN_SPENDABLE_UR_ORG_CONTEXT, True),
    pytest.param(ZEN_SPENDABLE_UR_ORG_CONTEXT, False, marks=pytest.mark.skip('only use with integration')),
    pytest.param(ZEN_SPENDABLE_NEW_CONTEXT, True),
    pytest.param(ZEN_SPENDABLE_NEW_CONTEXT, False),
    pytest.param(ZEN_SPENDABLE_SERVICES_AG_CONTEXT, True),
    pytest.param(ZEN_SPENDABLE_SERVICES_AG_CONTEXT, False),
], ids=lambda c: c.name)
def test_zen_export_transaction(context, use_integration):
    if use_integration:
        partner_integration_params = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT
    else:
        partner_integration_params = None
    client_id, person_id, contract_id = create_offer(context, partner_integration_params=partner_integration_params)
    payment_id, transaction_id = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                                                   PartnerPaymentType.WALLET,
                                                                                   context.service.id,
                                                                                   paysys_type_cc=context.tpt_paysys_type_cc)
    transaction_id = None if partner_integration_params is None else transaction_id
    tpt_class = Export.Classname.BALALAYKA_PAYMENT if context.name in (
        'ZEN_SPENDABLE_CONTEXT',
        'ZEN_SPENDABLE_NEW_CONTEXT',
        'ZEN_SPENDABLE_SERVICES_AG_CONTEXT') else Export.Classname.SIDE_PAYMENT
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      tpt_class)

    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
            payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    # не проверяем поле
    payment_data[0].update({'payment_id': None})

    expected_payments = create_expected_payment(client_id, person_id, contract_id, context, START_DT,
                                                transaction_id=transaction_id)

    utils.check_that(payment_data, contains_dicts_with_entries(expected_payments),
                     u"Проверяем наличие ожидаемых платежей")


def create_offer(context, partner_integration_params=None):
    # Переделать после перевода физиков в ООО Яндекс, связанный договор в АГ станет не нужен
    # Сейчас же второй договор с АГ создается без учета конфигурации
    client_id = steps.ClientSteps.create()

    main_person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                              params={'is-partner': '1'})
    _, _, main_contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                            client_id=client_id, is_offer=1,
                                                                            person_id=main_person_id,
                                                                            partner_integration_params=partner_integration_params,
                                                                            additional_params={
                                                                                'start_dt': START_DT})

    if context.link_contract_context_name is not None:
        link_context = context.get_context_by_name(context.link_contract_context_name)
        linked_person_id = steps.PersonSteps.create(client_id, link_context.person_type.code,
                                                    params={'is-partner': '1'})
        _, _, linked_contract_id, _ = steps.ContractSteps.create_partner_contract(link_context,
                                                                                  client_id=client_id, is_offer=1,
                                                                                  person_id=linked_person_id,
                                                                                  partner_integration_params=None,
                                                                                  additional_params={
                                                                                      'start_dt': START_DT,
                                                                                      'link_contract_id': main_contract_id})
    return client_id, main_person_id, main_contract_id


def create_expected_payment(partner_id, person_id, contract_id, context, dt, transaction_id=None):
    amount = utils.dround(AMOUNT / Decimal('0.87'), 5) if context.name == 'ZEN_SPENDABLE_CONTEXT' else AMOUNT
    amount = ((amount * context.nds.koef_on_dt(dt)).quantize(Decimal('1.00')) if not context.source_with_nds
              else amount)
    # internal = None if context.name != 'ZEN_SPENDABLE_SERVICES_AG_CONTEXT' else 1
    internal = context.tpt_internal

    expected_data = steps.SimpleApi.create_expected_tpt_row(context, partner_id, contract_id, person_id,
                                                            transaction_id, None, amount=amount, internal=internal)
    return [expected_data]
