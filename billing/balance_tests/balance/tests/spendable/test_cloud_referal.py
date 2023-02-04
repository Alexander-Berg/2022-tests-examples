# -*- coding: utf-8 -*-
__author__ = 'isupov'

from decimal import Decimal
import random

from dateutil.relativedelta import relativedelta

import pytest

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import reporter, utils
from btestlib.constants import Export, PaymentType, NdsNew
from btestlib.data.partner_contexts import CLOUD_REFERAL_CONTEXT
from btestlib.matchers import contains_dicts_equal_to

START_DT = utils.Date.first_day_of_month() - relativedelta(months=2)
AMOUNT = Decimal('1000.1')


@pytest.mark.parametrize('partner_integration_params',
                         [steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT],
                         ids=['PARTNER_INTEGRATION'])
@pytest.mark.parametrize('context', [CLOUD_REFERAL_CONTEXT],
                         ids=lambda c: c.name)
@pytest.mark.parametrize('nds', [NdsNew.DEFAULT, NdsNew.ZERO],
                         ids=lambda nds: 'nds_id' + str(nds.nds_id))
def test_cloud_referal_act(context, partner_integration_params, nds):
    context = context.new(name='%s-nds_id_%s_r%s' % (context.name, nds.nds_id, random.randint(66666, 6666666)), nds=nds)
    client_id, _, contract_id = create_offer(context=context,
                                             partner_integration_params=partner_integration_params)
    side_payment_id, transaction_id = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                                                        PaymentType.REWARD,
                                                                                        context.service.id)
    api.medium().UpdatePayment({'ServiceID': context.service.id, 'TransactionID': transaction_id},
                               {'PayoutReadyDT': START_DT})

    # запускаем обработку сайдпеймента:
    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)

    expected_acts = [
        create_expected_payment_act_new(context, client_id, contract_id, START_DT, AMOUNT)
    ]

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(acts, contains_dicts_equal_to(expected_acts), u"Проверяем, что акты соответствуют ожидаемым")


# -----------------------------
# Utils
def create_offer(context, partner_integration_params=None):
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params={'is-partner': '1'})
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, client_id=client_id, is_offer=1, person_id=person_id,
        partner_integration_params=partner_integration_params,
        additional_params={'start_dt': START_DT})

    return client_id, person_id, contract_id


def create_expected_payment_act_new(context, client_id, contract_id, dt, amount):
    reward_wo_nds = (amount / context.nds.koef_on_dt(dt)).quantize(Decimal('1.00000'))
    act_data = steps.CommonData.create_expected_pad(context, client_id, contract_id, dt,
                                                    partner_reward=reward_wo_nds, nds=context.nds)
    return act_data
