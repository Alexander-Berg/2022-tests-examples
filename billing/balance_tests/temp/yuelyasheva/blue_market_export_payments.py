from datetime import datetime
from decimal import Decimal
import uuid

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, Firms, Managers, Currencies, TransactionType, PaymentType, ContractSubtype, \
    NdsNew, PaysysType
from btestlib.data import simpleapi_defaults
from btestlib.data.defaults import SpendableContractDefaults as SpendableDefParams
from btestlib.matchers import contains_dicts_with_entries, equal_to
from btestlib.utils import XmlRpc
from simpleapi.common.payment_methods import Cash, LinkedCard
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS
import balance.balance_db as db


# select * from t_payment where  trust_payment_id = '5c794fde910d395e905d5615';
def find_payment_by_trust_payment_id(trust_payment_id):
    query = "select id, amount, postauth_amount from t_payment where  trust_payment_id = :trust_payment_id"
    params = {
        'trust_payment_id': trust_payment_id
    }
    return db.balance().execute(query, params)

def find_transaction_by_payment_id(payment_id):
    query = "select id, payment_id, trust_payment_id, transaction_type, service_id, order_id, total_sum, amount, " \
            "yandex_reward_wo_nds, yandex_reward, payment_type from t_thirdparty_transactions where payment_id = :payment_id"
    params = {
        'payment_id': payment_id
    }
    return db.balance().execute(query, params)

trust_payment_id_list = ['5c99f590910d3978568e8652']

payments_list = []
for trust_payment_id in trust_payment_id_list:
    payment = find_payment_by_trust_payment_id(trust_payment_id)
    transaction = []
    if len(payment) == 0:
        payments_list.append({'trust_payment_id': trust_payment_id, 'payment_id': None})
    for i in range(len(payment)):
        payments_list.append({'trust_payment_id': trust_payment_id, 'payment_id': payment[i]['id']})
        steps.CommonPartnerSteps.export_payment(payment[i]['id'])
        find_transaction_by_payment_id(payment[i]['id'])
print payments_list
