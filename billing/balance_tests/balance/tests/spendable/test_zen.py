# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

from decimal import Decimal

from dateutil.relativedelta import relativedelta
from hamcrest import empty, instance_of

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import reporter, utils
from btestlib.constants import Services, Currencies, PlaceType, Export, CurrencyRateSource, PartnerPaymentType, Pages
from btestlib.data.partner_contexts import (ZEN_SPENDABLE_CONTEXT, ZEN_SPENDABLE_SERVICES_AG_CONTEXT,
                                            ZEN_SPENDABLE_NEW_CONTEXT,
                                            ZEN_SPENDABLE_IP_CONTEXT, ZEN_SPENDABLE_UR_ORG_CONTEXT)
from btestlib.matchers import contains_dicts_equal_to

START_DT = utils.Date.first_day_of_month() - relativedelta(months=2)
AMOUNT = Decimal('1000.1')


@pytest.mark.parametrize('partner_integration_params',
                         [steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT, None],
                         ids=['PARTNER_INTEGRATION', 'WO_PARTNER_INTEGRATION'])
@pytest.mark.parametrize('context', [
                                     ZEN_SPENDABLE_CONTEXT,
                                     ZEN_SPENDABLE_IP_CONTEXT,
                                     ZEN_SPENDABLE_UR_ORG_CONTEXT,
                                     ZEN_SPENDABLE_NEW_CONTEXT,
                                     ZEN_SPENDABLE_SERVICES_AG_CONTEXT],
                         ids=lambda c: c.name)
def test_zen_act(context, partner_integration_params):
    client_id, _, contract_id = create_offer(context=context,
                                             partner_integration_params=partner_integration_params)
    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                                      PartnerPaymentType.WALLET, context.service.id)
    steps.ExportSteps.create_export_record(payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, payment_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    expected_acts = []
    if context.partner_acts:
        expected_acts = [
            create_expected_payment_act_new(context, client_id, contract_id, START_DT, AMOUNT)
        ]

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(acts, contains_dicts_equal_to(expected_acts), u"Проверяем, что акты соответствуют ожидаемым")


# -----------------------------
# Utils
def create_offer(context, partner_integration_params=None):
    # Переделать после перевода физиков в ООО Яндекс, связанный договор в АГ станет не нужен
    # Сейчас же второй договор с АГ должен создаваться без учета конфигурации
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


def create_expected_payment_act_new(context, client_id, contract_id, dt, amount):
    reward_wo_nds = ((amount / context.nds.koef_on_dt(dt)).quantize(Decimal('1.00000')) if context.source_with_nds
                     else amount)
    act_data = steps.CommonData.create_expected_pad(context, client_id, contract_id, dt,
                                                    partner_reward=reward_wo_nds, nds=context.nds)
    return act_data
