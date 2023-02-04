# coding=utf-8

"""
Максимально простые тесты, чтобы убедиться, что oebs-gate работает,
а так же, что настройки подключения к базе и права в ней корректные.
"""
import pytest
from hamcrest import equal_to, greater_than_or_equal_to

from balance import balance_steps as steps
from btestlib import utils


# hard-code prod client
CLIENT_ID = 6436285
CONTRACT_ID = 200272
CONTRACT_EID = u'РС-28111-01/15'


# @pytest.mark.timeout(300)
@pytest.mark.long
def test_get_partner_documents():

    res = steps.api.oebs_gate().server.GetPartnerDocuments({'ClientID': CLIENT_ID})
    utils.check_that(len(res), equal_to(1))
    contract_info = res[0]
    utils.check_that(contract_info['contract_eid'], CONTRACT_EID)


# @pytest.mark.timeout(300)
def test_get_partner_act_headers():
    res = steps.api.oebs_gate().server.GetPartnerActHeaders({'ClientID': CLIENT_ID})
    utils.check_that(len(res), greater_than_or_equal_to(1))
    res = sorted(res, key=lambda r: r['period_start_date'])
    row = res[0]
    utils.check_that(row['contract_id'], equal_to(str(CONTRACT_ID)))
