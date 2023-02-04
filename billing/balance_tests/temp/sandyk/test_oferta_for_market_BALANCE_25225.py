# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import equal_to


from balance import balance_steps as steps
from btestlib import utils as utils
from balance.features import Features
from btestlib.constants import Managers, Firms, Services, Currencies, ContractPaymentType
import balance.balance_db as db
import btestlib.reporter as reporter


pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.CONTRACT),
    pytest.mark.tickets('BALANCE-25225')
]

START_DT= datetime.datetime.now()
MANAGER_UID = Managers.SOME_MANAGER.uid
FIRM_ID = Firms.MARKET_111.id
SERVICE_ID = Services.NEW_MARKET.id
CURRENCY = Currencies.RUB.char_code
COMMISSION_PCT = 3


def default_data():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    return {'client_id':client_id, 'person_id': person_id, 'firm_id': FIRM_ID,
        'manager_uid': MANAGER_UID, 'services': [SERVICE_ID],
        'currency': CURRENCY, 'start_dt': START_DT, 'commission_pct': COMMISSION_PCT}


@pytest.mark.parametrize("params, name, is_ls",[
    ({'payment_type': ContractPaymentType.PREPAY}, 'prepayment', []),
    ({'payment_term':10,'payment_type': ContractPaymentType.POSTPAY},'postpayment_without_ls', [{'value_num': 0}]),
    ({'payment_term':10,'payment_type': ContractPaymentType.POSTPAY, 'personal_account':0 },'postpayment_ls_0', [{'value_num': 0}]),
    ({'payment_term':10,'payment_type': ContractPaymentType.POSTPAY, 'personal_account':1 },'postpayment_ls_1', [{'value_num': 1}])
])
@pytest.mark.params_format('{name}', name=lambda x: x)
def test_marketplace_oferta(params, name, is_ls):
         contract_id = steps.ContractSteps.create_offer(dict(default_data(), **params))['ID']
         check_ls = db.balance().execute('''select a.value_num from T_CONTRACT_COLLATERAL c
            join T_CONTRACT_ATTRIBUTES a on c.id = a.COLLATERAL_ID
            where code = 'PERSONAL_ACCOUNT' and c.CONTRACT2_ID = {contract_id}'''.format(contract_id=contract_id),
         descr=u'Проверяем флаг PERSONAL_ACCOUNT')
         utils.check_that(check_ls, equal_to(is_ls))
