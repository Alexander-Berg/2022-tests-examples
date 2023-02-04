# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from balance.balance_objects import Context
from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to

# from balance.balance_templates import Clients, Persons
from btestlib.data import person_defaults


CONTRACT_START_DT = utils.Date.first_day_of_month() - relativedelta(months=3)

SrvEdakiAgent = Services.Service(id=645, name=u'Еда: Курьеры (прием платежей)',
                                 token='eat_delivery_agent_4d53f26a1bf3e99b340c27626e3a8395')

SrvEdakiSpendable = Services.Service(id=646, name=u'Еда: Курьеры (выплаты)',
                                     token='eat_delivery_spendable_6dac0edb606df4e7406901798126adb4')

EDAKI_CONTEXT = Context().new(
        # common
        name='EDAKI_CONTEXT',
        service=SrvEdakiAgent,
        person_type=PersonTypes.UR,
        firm=Firms.FOOD_32,
        currency=Currencies.RUB,
        payment_currency=Currencies.RUB,
        nds=NdsNew.ZERO,
        invoice_type=InvoiceType.PERSONAL_ACCOUNT,
        contract_type=ContractSubtype.GENERAL,

        tpt_payment_type='card',
        tpt_paysys_type_cc='payture',
)


EDAKI_SPENDABLE_CONTEXT = EDAKI_CONTEXT.new(
        # common
        name='EDAKI_SPENDABLE_CONTEXT',
        service=SrvEdakiSpendable,
        nds=NdsNew.ZERO,
        contract_type=ContractSubtype.SPENDABLE,

        tpt_payment_type='subsidy',
        tpt_paysys_type_cc='subsidy',
)

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)

PAGE_BENZAKI_SUBSIDY = Pages.Page(id=11006, desc=u'Еда: Курьеры (выплаты)', payment_type='subsidy')
PAGE_BENZAKI_COUPON = Pages.Page(id=100106, desc=u'Еда: Курьеры (выплаты), купоны', payment_type='coupon')

# AMOUNTS = [{'type': PAGE_BENZAKI, 'payment_sum': Decimal('100.1'), 'refund_sum': Decimal('95.9')}]


def create_person(context, client_id):
    return steps.PersonSteps.create(client_id, context.person_type.code,
                                    full=True,
                                    inn_type=person_defaults.InnType.RANDOM,
                                    name_type=person_defaults.NameType.RANDOM,
                                    params={'ownership_type': 'SELFEMPLOYED',
                                            'is-partner':
                                                str(
                                                    1 if context.contract_type == ContractSubtype.SPENDABLE else 0)},
                                    )


def test_edaki_vvvvvvvvvvvvvvvvvvvery_simple():
    partner_commission_pct2 = D('1.066666')
    agent_client_id = steps.ClientSteps.create()
    agent_person_id = create_person(EDAKI_CONTEXT, agent_client_id)
    agent_client_id, agent_person_id, agent_contract_id, _ = steps.ContractSteps.create_partner_contract(
        EDAKI_CONTEXT,
        client_id=agent_client_id, person_id=agent_person_id,
        is_postpay=1, additional_params={'start_dt': CONTRACT_START_DT,
                                         'partner_commission_pct2': str(partner_commission_pct2),
                                         'offer_confirmation_type': 'no',
                                         'personal_account': '1'

                                         })

    agent_invoice_id, agent_invoice_eid = steps.InvoiceSteps.get_invoice_ids(agent_client_id)

    spendable_person_id = create_person(EDAKI_SPENDABLE_CONTEXT, agent_client_id)
    spendable_client_id, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        EDAKI_SPENDABLE_CONTEXT, is_postpay=1, client_id=agent_client_id, person_id=spendable_person_id,
        additional_params={'start_dt': CONTRACT_START_DT})

    steps.ExportSteps.export_oebs(client_id=agent_client_id)

    steps.ExportSteps.export_oebs(person_id=agent_person_id)
    steps.ExportSteps.export_oebs(contract_id=agent_contract_id)
    steps.ExportSteps.export_oebs(invoice_id=agent_invoice_id)

    steps.ExportSteps.export_oebs(person_id=spendable_person_id)
    steps.ExportSteps.export_oebs(contract_id=spendable_contract_id)

    default_amount = D('666.66')
    raise 6666

    # Расходная платёжа, субсидия
    payment_dt = datetime.now()

    payment_type = EDAKI_SPENDABLE_CONTEXT.tpt_payment_type
    transaction_type = TransactionType.PAYMENT
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(spendable_client_id, payment_dt,
                                                                           default_amount,
                                                                           payment_type,
                                                                           EDAKI_SPENDABLE_CONTEXT.service.id,
                                                                           transaction_type=transaction_type)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None

    expected_data = [steps.SimpleApi.create_expected_tpt_row(EDAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             trust_refund_id=trust_refund_id,
                                                             payment_type=payment_type, amount=default_amount,
                                                             trust_id=payment_data[0]['trust_id'],
                                                             oebs_org_id=127556,
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    # steps.ExportSteps.export_oebs(transaction_id=str(thirdparty_transaction_id))

    # Расходная платёжа, купония
    payment_dt = datetime.now()
    payment_type = 'coupon'
    transaction_type = TransactionType.PAYMENT
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(spendable_client_id, payment_dt,
                                                                           default_amount,
                                                                           payment_type,
                                                                           EDAKI_SPENDABLE_CONTEXT.service.id,
                                                                           transaction_type=transaction_type)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None

    expected_data = [steps.SimpleApi.create_expected_tpt_row(EDAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             trust_refund_id=trust_refund_id,
                                                             payment_type=payment_type, amount=default_amount,
                                                             trust_id=payment_data[0]['trust_id'],
                                                             oebs_org_id=127556, paysys_type_cc='coupon'
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    # steps.ExportSteps.export_oebs(transaction_id=str(thirdparty_transaction_id))

    # Агентская платёжа
    payment_dt = datetime.now()
    default_reward = (default_amount*(partner_commission_pct2/D(100))).quantize(D('0.01'))
    payment_type = 'card'
    transaction_type = TransactionType.PAYMENT
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(agent_client_id, payment_dt, default_amount,
                                                                           payment_type,
                                                                           EDAKI_CONTEXT.service.id,
                                                                           transaction_type=transaction_type)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None
    expected_data = [steps.SimpleApi.create_expected_tpt_row(EDAKI_CONTEXT, agent_client_id,
                                                             agent_contract_id, agent_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             trust_refund_id=trust_refund_id,
                                                             amount=default_amount,
                                                             trust_id=payment_data[0]['trust_id'],
                                                             oebs_org_id=127556,
                                                             yandex_reward=default_reward,
                                                             # invoice_eid=agent_invoice_eid
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    # steps.ExportSteps.export_oebs(transaction_id=str(thirdparty_transaction_id))

    # Партнерский акта
    # create_completions(spendable_client_id, spendable_person_id, spendable_contract_id, FIRST_MONTH)
    #
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, payment_dt)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_data = [steps.CommonData.create_expected_pad(EDAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                          spendable_contract_id, payment_dt,
                                                          partner_reward=default_amount,
                                                          nds=EDAKI_SPENDABLE_CONTEXT.nds,
                                                          page_id=PAGE_BENZAKI_SUBSIDY.id,
                                                          description=PAGE_BENZAKI_SUBSIDY.desc, type_id=6),
                     steps.CommonData.create_expected_pad(EDAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                          spendable_contract_id, payment_dt,
                                                          partner_reward=default_amount,
                                                          nds=EDAKI_SPENDABLE_CONTEXT.nds,
                                                          page_id=PAGE_BENZAKI_COUPON.id,
                                                          description=PAGE_BENZAKI_COUPON.desc, type_id=6)
                     ]
    utils.check_that(act_data, contains_dicts_equal_to(expected_data), u'Сравниваем данные из акта с шаблоном')

    # Обратнопартнерский акта
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(agent_client_id, agent_contract_id, payment_dt)
    act_data = steps.ActsSteps.get_act_data_by_client(agent_client_id)

    assert len(act_data) == 1
    assert D(act_data[0]['amount']) == default_reward
