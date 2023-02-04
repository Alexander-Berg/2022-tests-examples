# coding: utf-8

import balance.balance_db as db


def test_clear_oebs_api_queue():
    db.balance().execute(query="update t_export set state = 1 where type = 'OEBS_API' and state = 0")
