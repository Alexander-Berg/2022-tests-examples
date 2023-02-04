# -*- coding: utf-8 -*-

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib.data import defaults as default
from btestlib.data.partner_contexts import *

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)

PASSPORT_ID = default.PASSPORT_UID


@pytest.mark.parametrize('context_general, context_spendable, nds', [
    pytest.mark.smoke(
        (TAXI_RU_DELIVERY_CONTEXT, TAXI_RU_DELIVERY_CONTEXT_SPENDABLE, NdsNew.YANDEX_RESIDENT)),
    (TAXI_RU_DELIVERY_CONTEXT, TAXI_RU_DELIVERY_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_DELIVERY_ISRAEL_CONTEXT, TAXI_DELIVERY_ISRAEL_CONTEXT_SPENDABLE, NdsNew.ISRAEL),
    (TAXI_DELIVERY_ISRAEL_CONTEXT, TAXI_DELIVERY_ISRAEL_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT, TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE, NdsNew.ISRAEL),
    (TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT, TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_KZ_CONTEXT, TAXI_DELIVERY_KZ_CONTEXT_SPENDABLE, NdsNew.KAZAKHSTAN),
    (TAXI_DELIVERY_KZ_CONTEXT, TAXI_DELIVERY_KZ_CONTEXT_SPENDABLE, NdsNew.ZERO),
    (TAXI_DELIVERY_GEO_USD_CONTEXT, TAXI_DELIVERY_GEO_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_UZB_USD_CONTEXT, TAXI_DELIVERY_UZB_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ARM_USD_CONTEXT, TAXI_DELIVERY_ARM_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_KGZ_USD_CONTEXT, TAXI_DELIVERY_KGZ_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_GHA_USD_CONTEXT, TAXI_DELIVERY_GHA_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ZAM_USD_CONTEXT, TAXI_DELIVERY_ZAM_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_AZ_USD_CONTEXT, TAXI_DELIVERY_AZ_USD_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_EST_EUR_CONTEXT, TAXI_DELIVERY_EST_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_BY_BYN_CONTEXT, TAXI_DELIVERY_BY_BYN_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_KZ_EUR_CONTEXT, TAXI_DELIVERY_KZ_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_BY_EUR_CONTEXT, TAXI_DELIVERY_BY_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ISR_EUR_CONTEXT, TAXI_DELIVERY_ISR_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_UZB_EUR_CONTEXT, TAXI_DELIVERY_UZB_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_CMR_EUR_CONTEXT, TAXI_DELIVERY_CMR_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ARM_EUR_CONTEXT, TAXI_DELIVERY_ARM_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_KGZ_EUR_CONTEXT, TAXI_DELIVERY_KGZ_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_GHA_EUR_CONTEXT, TAXI_DELIVERY_GHA_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_SEN_EUR_CONTEXT, TAXI_DELIVERY_SEN_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_MXC_EUR_CONTEXT, TAXI_DELIVERY_MXC_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_TR_EUR_CONTEXT, TAXI_DELIVERY_TR_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_PER_EUR_CONTEXT, TAXI_DELIVERY_PER_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ZA_EUR_CONTEXT, TAXI_DELIVERY_ZA_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_UAE_EUR_CONTEXT, TAXI_DELIVERY_UAE_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ANG_EUR_CONTEXT, TAXI_DELIVERY_ANG_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_ZAM_EUR_CONTEXT, TAXI_DELIVERY_ZAM_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_CIV_EUR_CONTEXT, TAXI_DELIVERY_CIV_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_AZ_EUR_CONTEXT, TAXI_DELIVERY_AZ_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_MD_EUR_CONTEXT, TAXI_DELIVERY_MD_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_DELIVERY_RS_EUR_CONTEXT, TAXI_DELIVERY_RS_EUR_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_UBER_BEL_BYN_CONTEXT, TAXI_UBER_BEL_BYN_CONTEXT_SPENDABLE, NdsNew.NOT_RESIDENT),
    (TAXI_UBER_BEL_BYN_CONTEXT_NDS, TAXI_UBER_BEL_BYN_CONTEXT_NDS_SPENDABLE, NdsNew.BELARUS)
], ids=lambda g, s, nds: s.name + '_NDS id = {}'.format(nds.nds_id))
def test_taxi_delivery_donate_acts(context_general, context_spendable, nds):
    client_id, person_id, contract_id = create_client_and_contract(context_general, context_spendable, nds)


# ------------------------------------------------------------
# Utils
def create_client_and_contract(context_general, context_spendable, nds, start_dt=FIRST_MONTH,
                               payment_type=SpendablePaymentType.MONTHLY,
                               is_spendable_unsigned=False):

    additional_params = {'start_dt': start_dt}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context_general, additional_params=additional_params
    )
    additional_params.update({'nds': nds.nds_id, 'payment_type': payment_type, 'link_contract_id': contract_id})

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(context_spendable,
                                                                                                   client_id=client_id,
                                                                                                   unsigned=is_spendable_unsigned,
                                                                                                   additional_params=additional_params)

    return client_id, spendable_person_id, spendable_contract_id
