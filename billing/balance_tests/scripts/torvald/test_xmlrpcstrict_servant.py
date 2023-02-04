# coding=utf-8
__author__ = 'sandyk'

import pytest

import balance.balance_api as api
import btestlib.reporter as reporter
from balance.features import Features

CN = 'balance-qa@ld.yandex.ru'
INCORRECT_CN = 'incorrect-balance-qa@ld.yandex.ru '


@pytest.mark.priority('mid')
@reporter.feature(Features.XMLRPC)
def test_call_granted_method():
    '''
    ToDo
    '''
    # ToDo set CN
    xmlrpcstrict_connect = api.get_xmlrpcstrict_server2()

    result = xmlrpcstrict_connect.Balance.GetCurrencyRatesByPerion('EUR',
                                                                   '2015-02-01',
                                                                   '2015-02-10')
    assert len(result) > 0


def test_restricted_method_without_cn():
    pass


def test_restricted_method_with_incorrect_cn():
    pass

    # ToDo 2 cert
