import datetime
import json

import pytest
from pytest import param
from hamcrest import contains_string, equal_to, not_none, is_not, is_in

import btestlib.reporter as reporter
import btestlib.utils as utils
import balance.balance_db as db
from balance import balance_api as api
from balance.features import Features
from btestlib.constants import CompletionSource
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib.constants import TransactionType, Export, PaymentType

DT = utils.Date.nullify_time_of_date(datetime.datetime.today())
OLD_START_DT = '2000-01-01'
OLD_END_DT = '2000-01-02'


# result = api.test_balance().GetPartnerCompletions(
#     {
#         'start_dt': DT-relativedelta(days=1),
#         'end_dt': DT,
#         'completion_source': 'travel'
#     }
# )
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227942)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227943)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227840)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227841)

# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227842)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227843)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227844)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, 1366227845)

# steps.CommonPartnerSteps.generate_partner_acts_fair(10341427, DT)
# yt_client = steps.YTSteps.create_yt_client()
#
# def prepare_yt_row():
#     return [{
#         'client_id': 0,
#         'currency': 'USD',
#         'dt': '2019-11-08T12:00:00',
#         'orig_transaction_id': None,
#         'partner_id': 110146753,
#         'payment_type': 'cost',
#         'price': '8700.00',
#         'service_order_id': 'YA-1881-0602-6419',
#         'transaction_id': 109,
#         'transaction_type': 'payment',
#         'trust_payment_id': '5d808bc3fbacea4f27ddbc6e',
#         'update_dt': '2019-10-08T12:00:00'
#     },
#         {
#             'client_id': 0,
#             'currency': 'USD',
#             'dt': '2019-11-08T12:00:00',
#             'orig_transaction_id': None,
#             'partner_id': 110146753,
#             'payment_type': 'reward',
#             'price': '1300.00',
#             'service_order_id': 'YA-1881-0602-6419',
#             'transaction_id': 110,
#             'transaction_type': 'payment',
#             'trust_payment_id': '5d808bc3fbacea4f27ddbc6e',
#             'update_dt': '2019-10-08T12:00:00'
#         }
#     ]
#
# steps.YTSteps.create_data_in_yt(yt_client, '//tmp/expedia', prepare_yt_row())
