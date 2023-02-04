# coding=utf-8
from hamcrest import has_entry

from btestlib.utils import CheckMode
from simpleapi.common import logger
from simpleapi.xmlrpc import balance_test_xmlrpc as balance_test

__author__ = 'fellow'

log = logger.get_logger()


@CheckMode.result_matches(has_entry('status', 'success'))
def find_in_log(path, timestamp, regexp):
    log.debug(u'Try to find in log: {} by pattern: {} and time: {}'
              .format(path, regexp, timestamp))
    return balance_test.find_in_log(path, timestamp, regexp)


@CheckMode.result_matches(has_entry('status', 'success'))
def find_config(path):
    log.debug(u"Try to get config '{}' content".format(path))
    return balance_test.find_config(path)


def get_test_sequence_name(service):
    log.debug(u'Get sequence name for service {}'.format(service))
    return balance_test.get_test_sequence_name(service.id)
