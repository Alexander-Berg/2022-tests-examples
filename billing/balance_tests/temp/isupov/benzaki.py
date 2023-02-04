# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from balance.balance_objects import Context
from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to


CONTRACT_START_DT = utils.Date.first_day_of_month() - relativedelta(months=3)

SrvBenzakiPenalty = Services.Service(id=643, name=u'Драйв: Штрафы заправщиков',
                                     token='drive_refueller_fines_844627927accf92c7f9ee9abc5292c9d')

SrvBenzakiSpendable = Services.Service(id=644, name=u'Драйв: Выплаты заправщикам',
                                       token='drive_refueller_spendable_4641685f86ce2246251a2f4e2e716aa1')

BENZAKI_CONTEXT = Context().new(
        # common
        name='BENZAKI_CONTEXT',
        service=SrvBenzakiPenalty,
        person_type=PersonTypes.UR,
        firm=Firms.DRIVE_30,
        currency=Currencies.RUB,
        payment_currency=Currencies.RUB,
        nds=NdsNew.ZERO,
        invoice_type=InvoiceType.PERSONAL_ACCOUNT,
        contract_type=ContractSubtype.GENERAL,

        tpt_payment_type='correction_commission',
        tpt_paysys_type_cc='extra_profit',
)


BENZAKI_SPENDABLE_CONTEXT = BENZAKI_CONTEXT.new(
        # common
        name='BENZAKI_SPENDABLE_CONTEXT',
        service=SrvBenzakiSpendable,
        nds=NdsNew.ZERO,
        contract_type=ContractSubtype.SPENDABLE,

        tpt_payment_type='drive_fueler',
        tpt_paysys_type_cc='yadrive',
)

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)

PAGE_BENZAKI = Pages.Page(id=11005, desc=u'Драйв: Выплаты заправщикам', payment_type='drive_fueler')

# AMOUNTS = [{'type': PAGE_BENZAKI, 'payment_sum': Decimal('100.1'), 'refund_sum': Decimal('95.9')}]


def test_benzaki_vvvvvvvvvvvvvvvvvvvery_simple():
    penalty_client_id, _, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(
        BENZAKI_CONTEXT, is_postpay=1, additional_params={'start_dt': CONTRACT_START_DT})

    penalty_invoice_id, penalty_invoice_eid = steps.InvoiceSteps.get_invoice_ids(penalty_client_id)

    spendable_client_id, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        BENZAKI_SPENDABLE_CONTEXT, is_postpay=1, client_id=penalty_client_id,
        additional_params={'start_dt': CONTRACT_START_DT,
                           'link_contract_id': penalty_contract_id})

    steps.ExportSteps.export_oebs(client_id=penalty_client_id)
    steps.ExportSteps.export_oebs(contract_id=penalty_contract_id)
    steps.ExportSteps.export_oebs(contract_id=spendable_contract_id)
    steps.ExportSteps.export_oebs(invoice_id=penalty_invoice_id)

    # Расходная платёжа, выплатка
    payment_dt = datetime.now()
    default_amount = D('667.66')
    payment_type = BENZAKI_SPENDABLE_CONTEXT.tpt_payment_type
    transaction_type = TransactionType.PAYMENT
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(spendable_client_id, payment_dt, default_amount,
                                                                        payment_type,
                                                                        BENZAKI_SPENDABLE_CONTEXT.service.id,
                                                                        transaction_type=transaction_type)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None

    expected_data = [steps.SimpleApi.create_expected_tpt_row(BENZAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             trust_refund_id=trust_refund_id,
                                                             payment_type=payment_type, amount=default_amount,
                                                             trust_id=payment_data[0]['trust_id']
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    steps.ExportSteps.export_oebs(transaction_id=str(thirdparty_transaction_id))

    # Расходная платёжа, расплатка
    payment_dt = datetime.now()
    default_amount = D('1.00')
    payment_type = BENZAKI_SPENDABLE_CONTEXT.tpt_payment_type
    transaction_type = TransactionType.REFUND
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(spendable_client_id, payment_dt, default_amount,
                                                                        payment_type,
                                                                        BENZAKI_SPENDABLE_CONTEXT.service.id,
                                                                        transaction_type=transaction_type)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None

    expected_data = [steps.SimpleApi.create_expected_tpt_row(BENZAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             trust_refund_id=trust_refund_id,
                                                             payment_type=payment_type, amount=default_amount,
                                                             trust_id=payment_data[0]['trust_id']
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    steps.ExportSteps.export_oebs(transaction_id=str(thirdparty_transaction_id))

    # Доходная платёжа
    payment_dt = datetime.now()
    default_amount = D('666.66')
    payment_type = 'card'
    transaction_type = TransactionType.REFUND
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(penalty_client_id, payment_dt, default_amount,
                                                                        payment_type,
                                                                        BENZAKI_CONTEXT.service.id,
                                                                        transaction_type=transaction_type)

    steps.ExportSteps.create_export_record(side_payment_id, classname=Export.Classname.SIDE_PAYMENT)
    steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None
    expected_data = [steps.SimpleApi.create_expected_tpt_row(BENZAKI_CONTEXT, spendable_client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             trust_refund_id=trust_refund_id,
                                                             amount=default_amount,
                                                             trust_id=payment_data[0]['trust_id'],
                                                             invoice_eid=penalty_invoice_eid
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
    # steps.ExportSteps.export_oebs(transaction_id=str(thirdparty_transaction_id))

    # Партнерский акта
    # create_completions(spendable_client_id, spendable_person_id, spendable_contract_id, FIRST_MONTH)
    #
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, payment_dt)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_data = [steps.CommonData.create_expected_pad(BENZAKI_SPENDABLE_CONTEXT, spendable_client_id,
                                                          spendable_contract_id, payment_dt,
                                                          partner_reward=default_amount,
                                                          nds=BENZAKI_SPENDABLE_CONTEXT.nds,
                                                          page_id=PAGE_BENZAKI.id,
                                                          description=PAGE_BENZAKI.desc, type_id=6)]
    utils.check_that(act_data, contains_dicts_equal_to(expected_data), u'Сравниваем данные из акта с шаблоном')

    # Обратнопартнерский акта
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(penalty_client_id, penalty_contract_id, payment_dt)
    act_data = steps.ActsSteps.get_act_data_by_client(penalty_client_id)
    act_id = steps.ActsSteps.get_all_act_data(penalty_client_id)[0]['id']
    steps.ExportSteps.export_oebs(act_id=act_id)
    # assert len(act_data) == 1
    # assert D(act_data[0]['amount']) == default_amount
#
#
# def create_completions(client_id, person_id, contract_id, dt, coef=1):
#     for item in AMOUNTS:
#         steps.SimpleApi.create_fake_tpt_data(BENZAKI_SPENDABLE_CONTEXT, client_id, person_id, contract_id,
#                                              dt,
#                                              [{'amount': coef * item['payment_sum'],
#                                                'transaction_type': TransactionType.PAYMENT,
#                                                'payment_type': item['type'].payment_type},
#                                               {'amount': coef * item['refund_sum'],
#                                                'transaction_type': TransactionType.REFUND,
#                                                'payment_type': item['type'].payment_type}])
