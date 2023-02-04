# coding: utf-8

import pytest

from balance import balance_web as web

__author__ = 'fellow'

def test_generate():
    login_base = 'yndx-test-balance-assessor-'
    password = '19Assessment20?'

    with web.Driver() as driver:
        for i in range(2, 100):
            login = login_base + str(i)

            passport_page = web.OtherInterface.PassportPage.open(driver)
            passport_page.fill_reg_form(login, password)

if __name__ == '__main__':
    pytest.main()