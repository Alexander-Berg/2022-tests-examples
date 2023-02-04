# coding: utf-8

import pytest
from hamcrest import equal_to

from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as const
from btestlib import reporter
from btestlib import utils
from btestlib.constants import ContractCreditType
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts


@reporter.feature(Features.CONTRACT, Features.CREDIT, Features.COLLATERAL, Features.PAYSTEP, Features.PREPAYMENT)
@pytest.mark.tickets('BALANCE-27059')
@pytest.mark.parametrize('data', [
    utils.aDict({
        'collateral_params': {'DT': Date.TODAY_ISO,
                              'IS_SIGNED': Date.TODAY_ISO},
        'is_error_expected': False,
    }),
    utils.aDict({
        'collateral_params': {'DT': Date.TODAY_ISO,
                              'IS_SIGNED': None},
        'is_error_expected': True,
    }),
    utils.aDict({
        'collateral_params': {'DT': utils.Date.to_iso(utils.Date.shift_date(Date.TODAY, days=1)),
                              'IS_SIGNED': Date.TODAY_ISO},
        'is_error_expected': True,
    }),
])
def test_collateral_do_post_pay(data):
    ctx = Contexts.DIRECT_FISH_RUB_CONTEXT
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, ctx.person_type.code)

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': const.ContractPaymentType.PREPAY,
        'SERVICES': [ctx.service.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': ctx.currency.num_code,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(const.ContractCommissionType.OPT_CLIENT, contract_params)

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'CREDIT_TYPE': ContractCreditType.BY_TERM,
                         'CREDIT_LIMIT_SINGLE': 1000
                         }
    collateral_params.update(data.collateral_params)
    steps.ContractSteps.create_collateral(const.Collateral.DO_POSTPAY_LS, collateral_params)

    campaigns_list = [
        {'client_id': client_id, 'service_id': ctx.service.id, 'product_id': ctx.product.id, 'qty': 10},
    ]

    create_invoice = lambda credit: steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=ctx.paysys.id,
                                                                            credit=credit,
                                                                            contract_id=contract_id)

    if not data.is_error_expected:
        create_invoice(credit=1)
    else:
        create_invoice(credit=0)
        with pytest.raises(Exception) as exc:
            create_invoice(credit=1)
        utils.check_that(steps.CommonSteps.get_exception_code(exc.value), equal_to('CREDIT_LIMIT_EXEEDED'))
