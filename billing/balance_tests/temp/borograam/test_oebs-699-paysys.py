# -*- coding: utf-8 -*-
import pytest
from btestlib.data.partner_contexts import FOOD_COURIER_CONTEXT as context_645, FOOD_PICKER_CONTEXT as context_699
from btestlib.constants import PaymentType, PaysysType, Export
from balance import balance_steps as steps
from btestlib import utils
from decimal import Decimal
import balance.balance_db as db
import btestlib.reporter as reporter
from datetime import datetime


START_DT = utils.Date.first_day_of_month()
AMOUNT = Decimal('24.2')


@pytest.mark.parametrize("context, payment_type, paysys_type", [
    (context_645, PaymentType.CARD, 'yandex_market'),
    # (context_699, PaymentType.CARD, 'yandex_market'),
    # (context_699, 'picker_card', 'yandex_market'),
    (context_645, PaymentType.CARD, PaysysType.PAYTURE),
    #(context_699, 'picker_card', PaysysType.PAYTURE),
    #(context_699, 'yaeda picker_badge', PaysysType.PAYTURE),
    #(context_699, 'picker_market_promo', PaysysType.PAYTURE),
    #(context_699, 'yaeda picker_marketing', PaysysType.PAYTURE),
    #(context_699, 'yaeda&picker_corp', PaysysType.PAYTURE),
    #(context_699, 'picker_compens_promo', PaysysType.PAYTURE),
    (context_645, PaymentType.CARD, 'burger_king')
])
def test_temp_food_payments(context, payment_type, paysys_type):
    # todo rename test to test_yandex_market_paysys_type

    params = {
        'start_dt': START_DT
    }
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)
    payment_id, tpt_row_id = _temp_create_payment(
        client_id, person_id, contract_id, context, payment_type, paysys_type)

    steps.ExportSteps.export_oebs(
        contract_id=contract_id,
        client_id=client_id,
        person_id=person_id,
        transaction_id=tpt_row_id
    )
    _temp_report(context, paysys_type, payment_type, contract_id, payment_id, tpt_row_id)
    # todo check paysys_type?


def _temp_create_payment(client_id, person_id, contract_id,
                         context, payment_type, paysys_type_cc, amount=AMOUNT, yandex_reward=None):
    # START_DT = datetime(2020, 02, 26)  # utils.Date.first_day_of_month(datetime(2020, 02, 05))

    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()

    kwargs = dict(
        currency=context.currency,
        paysys_type_cc=paysys_type_cc,
        extra_str_1=service_order_id,
        transaction_id=transaction_id,
        payload="[]"
    )
    if yandex_reward is not None:
        kwargs['extra_num_0'] = yandex_reward

    side_payment_id, side_transaction_id = steps.PartnerSteps.create_sidepayment_transaction(
        client_id, START_DT, amount,
        payment_type, context.service.id,
        **kwargs
    )

    steps.ExportSteps.create_export_record_and_export(
        side_payment_id,
        Export.Type.THIRDPARTY_TRANS,
        Export.Classname.SIDE_PAYMENT
    )
    tpt_row_id = db.balance().execute(
        'select id from T_THIRDPARTY_TRANSACTIONS where PAYMENT_ID=:payment_id',
        dict(payment_id=side_payment_id))[0]['id']

    return side_payment_id, tpt_row_id


def deadcode():
    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()
    side_payment_id, side_transaction_id = steps.PartnerSteps.create_sidepayment_transaction(
            147765260, START_DT, 0,
            'card', 699,
            currency=context_699.currency,
            paysys_type_cc=PaysysType.PAYTURE,
            extra_str_1=service_order_id,
            transaction_id=transaction_id,
            payload="[]",
            extra_num_0=1
        )
    print side_payment_id + " <- payment | transaction -> " + side_transaction_id



def _temp_report(context, paysys_type, payment_type, contract_id,
                 payment_id, tpt_row_id, **kwargs):
    from collections import OrderedDict

    options = OrderedDict([
        ('service', context.service.id),
        ('paysys_type', paysys_type),
        ('payment_type', payment_type),
        ('contract_id', contract_id),
        ('payment', payment_id),
        ('row', tpt_row_id),
    ])
    for k, v in kwargs.items():
        options[k] = v

    reporter.log(', '.join(map(lambda item: '{} `{}`'.format(item[0], item[1]), options.items())))


@pytest.mark.parametrize("context, paysys_type, payment_type, contract_id, amount, commission_sum", (
        #(context_699, PaysysType.PAYTURE, 'card', 6300875, 0, 1),
        (context_699, PaysysType.PAYTURE, 'card', None, '0', 1),
))
def test_existing_contract_commission(context, paysys_type, payment_type,
                                      contract_id, amount, commission_sum):
    export_only_transaction = True

    if contract_id is None:
        params = {
            'start_dt': START_DT
        }
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, additional_params=params)
        export_only_transaction = False
    else:
        try:
            db_row = db.balance().execute(
                'select client_id, person_id from t_contract2 where id=:contract_id',
                dict(contract_id=contract_id))[0]
        except IndexError as e:
            raise Exception(u'Договора с таким id нет!')
        client_id, person_id = db_row['client_id'], db_row['person_id']

    payment_id, tpt_row_id = _temp_create_payment(
        client_id, person_id, contract_id, context,
        payment_type, paysys_type, amount=amount, yandex_reward=commission_sum)

    export_kwargs = dict(transaction_id=tpt_row_id)
    if not export_only_transaction:
        export_kwargs.update(
            contract_id=contract_id,
            client_id=client_id,
            person_id=person_id,
        )
    steps.ExportSteps.export_oebs(**export_kwargs)

    _temp_report(context, paysys_type, payment_type,
                 contract_id, payment_id, tpt_row_id, amount=amount, commission_sum=commission_sum)


@pytest.mark.parametrize("contract_id, context, payment_type", [
    # (6254326, context_699, 'card'),
    # (6254326, context_699, 'picker_card'),
    # (6254343, context_699, 'card'),
    # (6254343, context_699, 'picker_card'),
    # (6254343, context_699, 'picker_card'),
    (6269899, context_645, 'card'),
])
def test_temp_food_payment_existing_contract(contract_id, context, payment_type):
    paysys_type = PaysysType.PAYTURE

    db_row = db.balance().execute(
        'select client_id, person_id from t_contract2 where id=:contract_id',
        dict(contract_id=contract_id)
    )[0]
    client_id, person_id = db_row['client_id'], db_row['person_id']

    payment_id, tpt_row_id = _temp_create_payment(
        client_id, person_id, contract_id, context, payment_type, paysys_type)

    steps.ExportSteps.export_oebs(
        transaction_id=tpt_row_id
    )

    _temp_report(context, paysys_type, payment_type, contract_id, payment_id, tpt_row_id)


@pytest.mark.parametrize("tpt_row_id", [
    61277776390,  # 61277776440, 61277975280, 61277776380, 61277776400, 61277776430, 61277975260
])
def test_reexport_transactions(tpt_row_id):
    import balance.balance_db as db
    db_row = db.balance().execute(
        'select partner_id, person_id, contract_id from t_thirdparty_transactions where id=:id',
        dict(id=tpt_row_id)
    )[0]
    steps.ExportSteps.export_oebs(
        client_id=db_row['partner_id'],
        person_id=db_row['person_id'],
        contract_id=db_row['contract_id'],
        transaction_id=tpt_row_id
    )
