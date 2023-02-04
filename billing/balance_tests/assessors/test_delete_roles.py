# coding=utf-8
__author__ = 'yuelyasheva'

import pytest
import balance.balance_db as db
from test_post_restore_acts import LOGIN_ACTS_PH, LOGIN_ACTS_AGENCY, LOGIN_ACTS_SEARCH, LOGIN_ACTS_RECONCILIATION_REP
from test_post_restore_buh_login import LOGIN_BUH_LOGIN, LOGIN_BUG_LOGIN_INVOICE_CHANGE
from test_post_restore_assessors_data import LOGIN_EMPTY, LOGIN_SETTLEMENTS, LOGIN_REPRESENTATIVE, \
    LOGIN_WITHOUT_CLIENT, LOGIN_ORDER_PAGE
from test_post_restore_consumes_history import LOGIN_CONSUMES_HISTORY
from test_post_restore_invoice import LOGIN_Y_INVOICE, LOGIN_INVOICE_LINKS_CHECKS, LOGIN_INVOICE_DETAILS_CHECKS, \
    LOGIN_INVOICE_GENERAL_CHECKS, LOGIN_INVOICE_WITH_ACT_PAGE, LOGIN_210_OPERATIONS_CHECKS, \
    LOGIN_VARIOUS_OPERATIONS_CHECKS, LOGIN_CHARGE_NOTE, LOGIN_OVERDUE_OVERDRAFT
from test_post_restore_invoices import LOGIN_INVOICES_SUM, LOGIN_INVOICES_SEARCH, LOGIN_INVOICES_ELS_AND_DEBTS
from test_post_restore_orders import LOGIN_ORDERS_SEARCH

FIXED_USERS = [LOGIN_ACTS_PH, LOGIN_ACTS_AGENCY, LOGIN_ACTS_SEARCH, LOGIN_ACTS_RECONCILIATION_REP, LOGIN_BUH_LOGIN,
                LOGIN_BUG_LOGIN_INVOICE_CHANGE, LOGIN_EMPTY, LOGIN_SETTLEMENTS, LOGIN_REPRESENTATIVE,
                LOGIN_WITHOUT_CLIENT, LOGIN_ORDER_PAGE, LOGIN_CONSUMES_HISTORY, LOGIN_Y_INVOICE,
                LOGIN_INVOICE_LINKS_CHECKS, LOGIN_INVOICE_DETAILS_CHECKS, LOGIN_INVOICE_GENERAL_CHECKS,
                LOGIN_INVOICE_WITH_ACT_PAGE, LOGIN_210_OPERATIONS_CHECKS, LOGIN_VARIOUS_OPERATIONS_CHECKS,
                LOGIN_CHARGE_NOTE, LOGIN_OVERDUE_OVERDRAFT, LOGIN_INVOICES_SUM, LOGIN_INVOICES_SEARCH,
                LOGIN_INVOICES_ELS_AND_DEBTS, LOGIN_ORDERS_SEARCH]


def test_delete_roles_in_fixed_logins():
    for user in FIXED_USERS:
        db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': user.uid})


@pytest.mark.parametrize('num', range(1, 101))
def test_restore_assessors_logins(num):
    login_template = 'yndx-balance-assessor-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_test_passport_logins(num):
    login_template = 'yndx-test-balance-assessor-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 100))
def test_restore_assessors_fixed_logins(num):
    login_template = 'yndx-balance-assessor-fixed-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 101))
def test_restore_assessors_ytpayers(num):
    login_template = 'yndx-tst-ytpayer-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 101))
def test_restore_assessors_invoice_contracts(num):
    login_template = 'yndx-tst-invcontr-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})



@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_test_passport_logins(num):
    login_template = 'yndx-tst-repayment-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_test_passport_logins(num):
    login_template = 'yndx-tst-free-funds-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 20))
def test_restore_assessors_static_logins(num):
    login_template = 'yndx-static-balance-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 31))
def test_restore_assessors_printform_logins(num):
    login_template = 'yndx-static-yb-printforms-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_repayment_logins(num):
    login_template = 'yndx-static-yb-repayment-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_free_funds_logins(num):
    login_template = 'yndx-static-yb-free-funds-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_deferpays_logins(num):
    login_template = 'yndx-static-deferpays-{}'
    passport_id = db.balance().execute("select passport_id from t_passport where login = :login",
                             {'login': login_template.format(num)})[0]['passport_id']
    db.balance().execute("delete from t_role_user where passport_id = :passport_id",
                             {'passport_id': passport_id})
