# coding=utf-8

import balance.balance_api as api


def test_fetch_restricted_domains():
    """ Просто проверяем, что задача в тесте отрабатывает. """
    api.test_balance().FetchRestrictedDomains()
