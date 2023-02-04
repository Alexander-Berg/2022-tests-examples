# coding: utf-8


import btestlib.reporter as reporter

from balance import balance_steps as steps
from dateutil.relativedelta import relativedelta

import balance.balance_db as db
from btestlib import utils

def test_resolve_dcs_bua_hands():
    query = 'SELECT * FROM BO.T_NIRVANA_MNCLOSE_SYNC WHERE task_id = \'dcs_bua_hands\' and dt = :current_month'
    params = {'current_month': utils.Date.first_day_of_month()}
    res = db.balance().execute(query, params)
    if not res:
        query = 'INSERT INTO BO.T_NIRVANA_MNCLOSE_SYNC (TASK_ID, DT, STATUS, CHANGED_DT, OPENABLE_IN_MNCLOSE) ' \
                'VALUES (\'dcs_bua_hands\', TO_DATE(:current_month, \'DD.MM.YYYY HH24:MI:SS\'), \'resolved\', ' \
                'TO_DATE(:current_month, \'DD.MM.YYYY HH24:MI:SS\'), 0)'
        params = {'current_month': utils.Date.first_day_of_month()}
        db.balance().execute(query, params)