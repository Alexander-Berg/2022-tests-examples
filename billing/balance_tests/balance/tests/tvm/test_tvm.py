# coding: utf-8
import xmlrpclib

import pytest
from hamcrest import contains_string

from balance import balance_api
from balance.features import Features
from btestlib import utils, utils_tvm, reporter
from btestlib.constants import TvmClientIds


@pytest.fixture(scope='module')
def medium_tvm():
    """ Кэшируем тикет, чтобы не ходить на каждый тест за ним. Они сейчас живут по 12 часов. """
    ticket = utils_tvm.get_tvm_ticket_cloud()
    return balance_api.medium_tvm(ticket=ticket)


@reporter.feature(Features.CLOUD, Features.TVM)
def test_cloud_accessible_method(medium_tvm):
    """ Просто проверяем, метод доступен (вызов не падает) """
    medium_tvm.FindClient({'ClientID': 666})


@reporter.feature(Features.CLOUD, Features.TVM)
def test_cloud_inaccessible_method(medium_tvm):
    """ Проверяем, что облакам запрещено звать эту ручку """
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_tvm.GetManagersInfo([666])
    expected_message = u'Method Balance.GetManagersInfo call is forbidden for {} TVM application. ' \
                       u'You should request access from Yandex.Balance team.'.format(TvmClientIds.BALANCE_TESTS)
    utils.check_that(str(exc_info.value), contains_string(expected_message))


INVALID_TICKET_ERROR = 'Service ticket validation has been failed. You should request proper ticket from TVM service.'


@reporter.feature(Features.TVM)
def test_cannot_access_with_wrong_ticket():
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        balance_api.medium_tvm(ticket='some_bullshit').FindClient({'ClientID': 666})
    error_string = str(exc_info.value)
    utils.check_that(error_string, contains_string('Service ticket validation has failed'))
    utils.check_that(error_string, contains_string('You should request proper ticket from TVM service'))


def test_no_ticket():
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        balance_api.medium_tvm(ticket=None).FindClient({'ClientID': 666})
    error_string = str(exc_info.value)
    utils.check_that(error_string, contains_string('Service ticket is not specified'))
