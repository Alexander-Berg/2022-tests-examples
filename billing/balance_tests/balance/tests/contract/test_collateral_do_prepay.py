# coding: utf-8

import pytest
from hamcrest import equal_to

from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import constants as const
from btestlib import reporter
from btestlib import utils
from btestlib.constants import Paysyses, PersonTypes, Currencies, ContractCommissionType, Firms
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MARKET = Contexts.MARKET_RUB_CONTEXT.new(firm=Firms.MARKET_111)


# Нельзя выставиться в кредит, если по постоплатному договору есть действующее ДС о переводе на предоплату
# Хорошо бы еще проверять для Украинских договоров с видом кредита "по сроку" и "по сроку и сумме",
# т.к. в них лимит кредита кладется в другие поля, но pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
@reporter.feature(Features.CONTRACT, Features.CREDIT, Features.COLLATERAL, Features.PAYSTEP, Features.PREPAYMENT)
@pytest.mark.tickets('BALANCE-27059')
@pytest.mark.parametrize('context, data', [
    pytest.param(DIRECT,
                 utils.aDict({
                     'collateral_params': {'DT': Date.TODAY_ISO,
                                           'IS_SIGNED': Date.TODAY_ISO},
                     'is_error_expected': True,
                 }),
                 id='Direct',
                 marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11))),
    pytest.param(MARKET,
             utils.aDict({
                 'collateral_params': {'DT': Date.TODAY_ISO,
                                       'IS_SIGNED': Date.TODAY_ISO},
                 'is_error_expected': True,
             }),
             id='Market',
             marks=pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11))),
    pytest.param(DIRECT, utils.aDict({
        'collateral_params': {'DT': Date.TODAY_ISO,
                              'IS_SIGNED': None},
        'is_error_expected': False,
    }), id='Not signed collateral'),
    pytest.param(DIRECT, utils.aDict({
        'collateral_params': {'DT': utils.Date.to_iso(utils.Date.shift_date(Date.TODAY, days=1)),
                              'IS_SIGNED': Date.TODAY_ISO},
        'is_error_expected': False,
    }), id='Collateral in future'),
])
def test_collateral_do_prepay(context, data):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': const.ContractPaymentType.POSTPAY,
        'SERVICES': [context.service.id],
        'DT': Date.TODAY_ISO,
        'FIRM': context.firm.id,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': context.currency.num_code,
        'CREDIT_TYPE': const.ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(const.ContractCommissionType.OPT_CLIENT, contract_params)

    collateral_params = {'CONTRACT2_ID': contract_id}
    collateral_params.update(data.collateral_params)
    steps.ContractSteps.create_collateral(const.Collateral.DO_PREPAY, collateral_params)

    campaigns_list = [
        {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 10},
    ]

    create_invoice = lambda credit: steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=context.paysys.id,
                                                                            credit=credit,
                                                                            contract_id=contract_id)

    if not data.is_error_expected:
        create_invoice(credit=1)
    else:
        create_invoice(credit=0)
        with pytest.raises(Exception) as exc:
            create_invoice(credit=1)
        utils.check_that(steps.CommonSteps.get_exception_code(exc.value), equal_to('CREDIT_LIMIT_EXEEDED'))


@pytest.mark.parametrize('context', [
    Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_CHF,
                                         currency=Currencies.CHF, contract_type=ContractCommissionType.SW_OPT_AGENCY),
    Contexts.DIRECT_FISH_RUB_CONTEXT.new(contract_type=ContractCommissionType.OPT_AGENCY)])
@pytest.mark.parametrize('data', [
    utils.aDict({
        'collateral_params': {'DT': Date.TODAY_ISO,
                              'IS_SIGNED': Date.TODAY_ISO},
        'is_error_expected': False
    }),
    # utils.aDict({
    #     'collateral_params': {'DT': Date.TODAY_ISO,
    #                           'IS_SIGNED': None},
    #     'is_error_expected': False,
    # }),
])
def test_collateral_do_prepay_old_LS(data, context):
    ctx = context
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, ctx.person_type.code)

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': const.ContractPaymentType.POSTPAY,
        'SERVICES': [ctx.service.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25,
        'CURRENCY': ctx.currency.num_code,
        'CREDIT_TYPE': const.ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
        'PERSONAL_ACCOUNT': 1,
        'PERSONAL_ACCOUNT_FICTIVE': 1,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(ctx.contract_type,
                                                             contract_params)

    collateral_params = {'CONTRACT2_ID': contract_id}
    collateral_params.update(data.collateral_params)
    steps.ContractSteps.create_collateral(const.Collateral.DO_PREPAY, collateral_params)
    #
    # campaigns_list = [
    #     {'client_id': client_id, 'service_id': ctx.service.id, 'product_id': ctx.product.id, 'qty': 10},
    #
    # ]
    # # steps.InvoiceSteps.create_force_invoice(client_id=client_id,
    # #                                         person_id=person_id,
    # #                                         campaigns_list=campaigns_list,
    # #                                         paysys_id=ctx.paysys.id,
    # #                                         credit=0,
    # #                                         contract_id=contract_id)
    #
    # steps.InvoiceSteps.create_force_invoice(client_id=client_id,
    #                                         person_id=person_id,
    #                                         campaigns_list=campaigns_list,
    #                                         paysys_id=ctx.paysys.id,
    #                                         credit=1,
    #                                         contract_id=contract_id)


@pytest.mark.parametrize('data', [
    utils.aDict({
        'collateral_params': {'DT': Date.TODAY_ISO,
                              'IS_SIGNED': Date.TODAY_ISO},
        'is_error_expected': False
    }),
])
def test_collateral_do_prepay_new_LS(data):
    ctx = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, ctx.person_type.code)

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': const.ContractPaymentType.POSTPAY,
        'SERVICES': [ctx.service.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': ctx.currency.num_code,
        'CREDIT_TYPE': const.ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
        'PERSONAL_ACCOUNT': 1,
        'PERSONAL_ACCOUNT_FICTIVE': 1,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(const.ContractCommissionType.NO_AGENCY,
                                                             contract_params)

    collateral_params = {'CONTRACT2_ID': contract_id}
    collateral_params.update(data.collateral_params)
    steps.ContractSteps.create_collateral(const.Collateral.DO_PREPAY, collateral_params)

    campaigns_list = [
        {'client_id': client_id, 'service_id': ctx.service.id, 'product_id': ctx.product.id, 'qty': 10},

    ]

    steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                            person_id=person_id,
                                            campaigns_list=campaigns_list,
                                            paysys_id=ctx.paysys.id,
                                            credit=0,
                                            contract_id=contract_id)
