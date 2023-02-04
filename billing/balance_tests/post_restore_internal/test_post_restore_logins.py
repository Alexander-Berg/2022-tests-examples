# coding: utf-8
__author__ = "atkaya"

import pytest

from btestlib.constants import Users
import balance.balance_steps as steps
import maintenance.test_restore_logins as maintenance_restore

def get_passport_by_login(template, num):
    login = template.format(num)
    data = steps.api.medium().GetPassportByLogin(0, login)

    if not data:
        assert 'No such user in passport: {}'.format(login)

@pytest.mark.parametrize('num', range(1, 101))
def test_restore_assessors_logins(num):
    login_template = 'yndx-balance-assessor-{}'
    get_passport_by_login(login_template, num)

@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_test_passport_logins(num):
    login_template = 'yndx-test-balance-assessor-{}'
    get_passport_by_login(login_template, num)

@pytest.mark.parametrize('num', range(1, 100))
def test_restore_assessors_fixed_logins(num):
    login_template = 'yndx-balance-assessor-fixed-{}'
    get_passport_by_login(login_template, num)

@pytest.mark.parametrize('num', range(100, 1000))
def test_restore_atst_user_logins(num):
    login_template = 'yb-atst-user-{}'
    get_passport_by_login(login_template, num)

@pytest.mark.parametrize('num', range(1, 101))
def test_restore_assessors_ytpayers(num):
    login_template = 'yndx-tst-ytpayer-{}'
    get_passport_by_login(login_template, num)

@pytest.mark.parametrize('num', range(1, 101))
def test_restore_assessors_invoice_contracts(num):
    login_template = 'yndx-tst-invcontr-{}'
    get_passport_by_login(login_template, num)

@pytest.mark.parametrize('num', range(1, 55))
def test_restore_hermione_clients(num):
    login_template = 'yb-atst-herm-client-{}'
    get_passport_by_login(login_template, num)

# Метод не работает для логинов из доменного паспорта.
@pytest.mark.parametrize('login', [item.login for item in Users.values()
                                   if not item.login.startswith('testuser-balance')])
def test_restore_custom_logins(login):
    data = steps.api.medium().GetPassportByLogin(0, login)

    if not data:
        assert 'No such user in passport: {}'.format(login)


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_test_passport_logins(num):
    login_template = 'yndx-tst-repayment-{}'
    get_passport_by_login(login_template, num)


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_test_passport_logins(num):
    login_template = 'yndx-tst-free-funds-{}'
    get_passport_by_login(login_template, num)


@pytest.mark.parametrize('num', range(1, 20))
def test_restore_assessors_static_logins(num):
    login_template = 'yndx-static-balance-{}'
    get_passport_by_login(login_template, num)


@pytest.mark.parametrize('num', range(1, 31))
def test_restore_assessors_printform_logins(num):
    login_template = 'yndx-static-yb-printforms-{}'
    get_passport_by_login(login_template, num)


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_repayment_logins(num):
    login_template = 'yndx-static-yb-repayment-{}'
    get_passport_by_login(login_template, num)


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_free_funds_logins(num):
    login_template = 'yndx-static-yb-free-funds-{}'
    get_passport_by_login(login_template, num)


@pytest.mark.parametrize('num', range(1, 11))
def test_restore_assessors_deferpays_logins(num):
    login_template = 'yndx-static-deferpays-{}'
    get_passport_by_login(login_template, num)


def test_create_main_login():
    maintenance_restore.test_create_main_login()


@pytest.mark.no_parallel
def test_create_not_main_login():
    maintenance_restore.test_create_not_main_login()


@pytest.mark.no_parallel
def test_create_accountant():
    maintenance_restore.test_create_accountant()

def test_link_endbuyers_login():
    data = steps.api.medium().GetPassportByLogin(0, 'yb-static-balance-7')
    steps.ClientSteps.link(7551050, 'yb-static-balance-7')