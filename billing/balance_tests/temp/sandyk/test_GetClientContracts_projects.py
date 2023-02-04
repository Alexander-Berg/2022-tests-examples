# coding: utf-8
__author__ = 'sandyk'

from decimal import Decimal
from datetime import datetime
import uuid

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_inanyorder, not_, has_key, has_item

import balance.balance_steps as steps
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance.features import Features
from btestlib.constants import Managers, Firms, Services, Currencies, ContractPaymentType
from btestlib.data.defaults import ContractDefaults
from btestlib.matchers import contains_dicts_with_entries


pytestmark = [
    pytest.mark.priority('mid'),
    reporter.feature(Features.CONTRACT,Features.XMLRPC),
    pytest.mark.tickets('BALANCE-25246')
]

MANAGER_UID = Managers.SOME_MANAGER.uid
PAYMENT_TYPE = ContractPaymentType.PREPAY
FIRM_ID = Firms.CLOUD_112.id
SERVICE_ID = Services.CLOUD.id
CURRENCY = Currencies.RUB.char_code

START_DT = datetime.today()


def default_data():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    return {'client_id': client_id, 'person_id': person_id, 'firm_id': FIRM_ID, 'manager_uid': MANAGER_UID, 'payment_type': PAYMENT_TYPE, 'services': [
        SERVICE_ID], 'currency': CURRENCY, 'projects': [str(uuid.uuid4()),
        str(uuid.uuid4())], 'start_dt': START_DT}


# @pytest.mark.parametrize("params, name, is_ls",[
#     ({'payment_type': ContractPaymentType.PREPAY}, 'prepayment', []),
#     ({'payment_term':PAYMENT_TERM,'payment_type': ContractPaymentType.POSTPAY},'postpayment_without_ls', [{'value_num': 0}]),
#     ({'payment_term':PAYMENT_TERM,'payment_type': ContractPaymentType.POSTPAY, 'personal_account':0 },'postpayment_ls_0', [{'value_num': 0}]),
#     ({'payment_term':PAYMENT_TERM,'payment_type': ContractPaymentType.POSTPAY, 'personal_account':1 },'postpayment_ls_1', [{'value_num': 1}])
# ])
# @pytest.mark.params_format('{name}', name=lambda x: x)

def test_add_project():
    contract_id = steps.ContractSteps.create_offer(default_data()[0])['ID']
    update_project('add', None, contract_id, [str(uuid.uuid4())])

def test_marketplace_oferta(params, name, is_ls):
         contract_id = steps.ContractSteps.create_offer(dict(default_data(), **params))['ID']
         check_ls = db.balance().execute('''select a.value_num from T_CONTRACT_COLLATERAL c
            join T_CONTRACT_ATTRIBUTES a on c.id = a.COLLATERAL_ID
            where code = 'PERSONAL_ACCOUNT' and c.CONTRACT2_ID = {contract_id}'''.format(contract_id=contract_id),
         descr=u'Проверяем флаг PERSONAL_ACCOUNT')
         utils.check_that(check_ls, equal_to(is_ls))