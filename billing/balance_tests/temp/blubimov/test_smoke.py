# coding: utf-8

from pprint import pprint

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_web
from btestlib import environments


def test_print_environment_urls():
    cur_env = environments.balance_env()
    pprint(vars(cur_env))


def test_medium():
    steps.ClientSteps.create()


def test_test_xmlrpc():
    api.test_balance().GetHost()


def test_db():
    db.balance().execute('SELECT * FROM t_client WHERE id = :id', {'id': 403501})


def test_meta():
    db.meta().execute('SELECT * FROM t_client WHERE id = :id', {'id': 403501})


def test_auth():
    with balance_web.Driver():
        pass


def test_web():
    with balance_web.Driver() as driver:
        balance_web.AdminInterface.ClientEditPage.open(driver, 403501)
