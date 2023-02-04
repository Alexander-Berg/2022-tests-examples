# coding: utf-8
__author__ = 'a-vasin'

from dateutil.relativedelta import relativedelta

import balance.balance_db as db
from btestlib import utils


def test_unified_account_enqueue_update():
    query = "UPDATE bo.t_job SET next_dt = :date WHERE id = 'unified_account_enqueue'"
    params = {'date': utils.Date.first_day_of_month() + relativedelta(months=1, days=1)}
    db.balance().execute(query, params)
