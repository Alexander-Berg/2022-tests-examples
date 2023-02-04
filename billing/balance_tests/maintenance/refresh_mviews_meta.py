# coding: utf-8
__author__ = 'yuelyasheva'

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter


def test_refresh_mview_manager():
    refresh_mview('t_manager')


def refresh_mview(mview):
    with reporter.step(u'Обновляем ' + mview):
        query = "begin \
                    dbms_mview.refresh('bo." + mview + "', 'C'); \
                end;"
        db.meta().execute(query)