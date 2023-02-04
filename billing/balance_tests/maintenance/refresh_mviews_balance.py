# coding: utf-8
__author__ = 'yuelyasheva'

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter


def test_refresh_mview_products():
    refresh_mview('t_product_group')
    refresh_mview('t_activity_type')
    refresh_mview('t_product')
    refresh_mview('t_price')


def test_refresh_mview_taxes():
    refresh_mview('t_tax')
    refresh_mview('t_tax_policy_pct')
    refresh_mview('t_prod_season_coeff')


def test_refresh_mview_managers():
    refresh_mview('t_manager')
    refresh_mview('t_manager_inner')
    refresh_mview('t_manager_group_inner')
    refresh_mview('t_manager_group')
    refresh_mview('t_group_manager_link')


def test_refresh_mview_rates():
    refresh_mview('t_currency_rate_v2')


def refresh_mview(mview):
    with reporter.step(u'Обновляем ' + mview):
        query = "begin \
                    dbms_mview.refresh('bo." + mview + "', 'C'); \
                end;"
        db.balance().execute(query)

