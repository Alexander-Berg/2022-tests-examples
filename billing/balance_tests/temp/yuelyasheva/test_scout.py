# -*- coding: utf-8 -*-

import datetime
import random
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.matchers import contains_dicts_equal_to
from dateutil.relativedelta import relativedelta
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Services, Nds, SpendablePaymentType, Export, Currencies, Managers, Pages, \
    PartnerPaymentType

delta = datetime.timedelta

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)


MANAGER = Managers.SOME_MANAGER
payload = '{"db"="5055e1c619914eef979fef011fa5b912";"scout_id"="mvoronovubervrz";"scout_name"="uber \u0432\u043e' \
          '\u0440\u043e\u043d\u043e\u0432";"uuid"="5055e1c619914eef979fef011fa5b912_mvoronovubervrz"}'


def create_general_client_and_contract(context, start_dt):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [service.id for service in context.services],
        'FIRM': context.firm.id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id
    })
    return client_id, contract_id


def create_client_and_contract(context, nds, start_dt=FIRST_MONTH, payment_type=SpendablePaymentType.MONTHLY,
                               remove_params=None):
    client_id, contract_id = create_general_client_and_contract(context, start_dt)

    spendable_person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'is-partner': '1'})
    spendable_contract_id, _ = steps.ContractSteps.create_contract('spendable_taxi_donate', {
        'CLIENT_ID': client_id,
        'PERSON_ID': spendable_person_id,
        'FIRM': context.firm.id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'NDS': str(Nds.get(nds)),
        'PAYMENT_TYPE': payment_type,
        'LINK_CONTRACT_ID': contract_id,
        'CURRENCY': context.currency.iso_num_code,
        'SERVICES': [Services.SCOUTS.id],
    }, remove_params=remove_params)

    return client_id, spendable_person_id, spendable_contract_id


def create_expected_payment_act(client_id, contract_id, dt, amount, currency=Currencies.RUB):
    return steps.CommonData.create_expected_partner_act_data(client_id, contract_id, dt, description=Pages.SCOUTS.desc,
                                                             currency=currency, partner_reward=amount, page_id=Pages.SCOUTS.id, type_id=4)


@pytest.mark.parametrize('p', [
    utils.aDict({'sidepayments': [('400', FIRST_MONTH, 'RUB'),
                                  ('800', FIRST_MONTH, 'RUB')]})
])
def test_scout(p):
    context = Contexts.TAXI_RU_CONTEXT

    client_id, person_id, contract_id = create_client_and_contract(context, context.nds.nds_id)

    total_amount = 0
    for amount, dt, currency in p.sidepayments:
        side_payment_id = steps.PartnerSteps.create_sidepayment_transaction(client_id, dt, amount,
                                                                            PartnerPaymentType.SCOUT,
                                                                            Services.SCOUTS.id, payload=payload)

        thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
            side_payment_id)

        tpt = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)

        total_amount += int(amount)/(1+context.nds.nds_id/100.)

    steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    collateral_id = steps.ContractSteps.get_collateral_id(contract_id)
    steps.CommonSteps.export('OEBS', 'ContractCollateral', collateral_id)
    #steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', str(thirdparty_transaction_id))

    expected_amount = utils.dround(total_amount, 5)
    expected_acts = [
        create_expected_payment_act(client_id, contract_id, FIRST_MONTH, amount=expected_amount)
    ]

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)
    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    utils.check_that(acts, contains_dicts_equal_to(expected_acts), u"Проверяем, что акты соответствуют ожидаемым")

if __name__ == '__main__':
    pass