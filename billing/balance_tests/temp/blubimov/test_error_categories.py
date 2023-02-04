# coding: utf-8

import inspect
import sys

import pytest
from hamcrest import equal_to
from selenium.common.exceptions import WebDriverException, TimeoutException

from btestlib import utils


# ================= Noise =================

# msg отличается от 1_1
def test_noise_web_driver_exception_1_3():
    raise WebDriverException(msg='Session [dfkdjfds-3670-487b-8ab4-0927ab811a57] was terminated due to SO_TIMEOUT')


# msg такое же как в 1_1
def test_noise_web_driver_exception_1_2():
    raise WebDriverException(msg='Session [de907aee-3670-487b-8ab4-0927ab811a57] was terminated due to SO_TIMEOUT')


def test_noise_web_driver_exception_1_1():
    raise WebDriverException(msg='Session [de907aee-3670-487b-8ab4-0927ab811a57] was terminated due to SO_TIMEOUT')


def test_noise_web_driver_exception_2():
    raise WebDriverException(
        msg='Session [b40d3eeb-3477-4da1-bcbd-fccc77535d79] not available and is not among the last 1000 terminated sessions.')


def test_noise_xmlrpc_error():
    raise utils.XmlRpc.XmlRpcError('Traceback in XmlRpc response is not None',
                                   'ORA-30006: resource busy; acquire with WAIT timeout expired')


def test_noise_fault_timeout():
    from xmlrpclib import Fault
    raise Fault(-1, "Error: Timeout\nDescription: 300._ Traceback: _OMITTED_")


def test_noise_http_error():
    utils.Web.HttpError.raise_error(u'На странице отображается http-ошибка', '504 Gateway Timeout')


def test_noise_balance_error():
    utils.Web.BalanceError.raise_error(u'На странице отображается ошибка баланса', True, None, None, None)


# ================= Probable Noise =================

def test_prob_noise_expat_error():
    from xml.parsers.expat import ExpatError
    raise ExpatError('not well-formed (invalid token): line 67, column 22')


def test_prob_noise_url_error():
    from urllib2 import URLError
    raise URLError('[Errno 101] Network is unreachable')


def test_prob_noise_protocol_error():
    from xmlrpclib import ProtocolError
    raise ProtocolError("greed-ts1f.yandex.ru:8027/simple/xmlrpc", 502, 'Bad Gateway', {})


def test_prob_noise_timeout_exception():
    raise TimeoutException(
        u"Не произошел редирект на страницу yandex.ru в течение 120 секунд после оплаты на странице trust")


def test_prob_noise_condition_not_occurred_1():
    raise utils.ConditionHasNotOccurred("Condition '<False>'(Waiting for data on page) has not occurred in 120 seconds",
                                        0)


def test_prob_noise_condition_not_occurred_2():
    raise utils.ConditionHasNotOccurred(
        "Condition 'a dictionary containing ['status': 'success']' has not occurred in 240 seconds", 0)


# ================= Environment Errors =================

def test_environment_xmlrpc_error():
    # steps.ClientSteps.create()
    raise utils.XmlRpc.XmlRpcError('Traceback in XmlRpc response is not None',
                                   u'ORA-20000: ORA-20000: Ошибка: Попытка создания акта не в открытом периоде!')


# ================= Broken tests =================

def test_broken_tests_parse_error():
    from xml.etree.ElementTree import ParseError
    raise ParseError('no element found: line 1, column 0')


clsmembers = inspect.getmembers(sys.modules['exceptions'], inspect.isclass)
exception_subclasses = {r[1] for r in clsmembers if r[1] not in [KeyboardInterrupt, AssertionError]}


@pytest.mark.parametrize('exception_subclass', exception_subclasses)
def test_broken_tests_exception_subs(exception_subclass):
    if exception_subclass == UnicodeTranslateError:
        raise UnicodeTranslateError(u'unicode_error', 2, 3, 'str_error')
    elif exception_subclass == UnicodeDecodeError:
        raise UnicodeDecodeError('1', '2', 3, 4, '5')
    elif exception_subclass == UnicodeEncodeError:
        raise UnicodeEncodeError('1', u'2', 3, 4, '5')
    else:
        raise exception_subclass('My error')


def test_broken_tests_indentation_error():
    raise IndentationError('My error')


# ================= Generators =================

# print ',\n'.join(["'{}: '".format(c.__name__) for c in sorted(exception_subclasses, key=lambda c: c.__name__)])

# def test_gen_cat():
#     from balance.tests.blubimov_local import error_categories
#     error_categories.generate_categories_json()

# ================= Product defects =================

def test_product_http_error():
    utils.Web.HttpError.raise_error(u'На странице отображается http-ошибка', '502 Bad Gateway')


def test_product_balance_error():
    utils.Web.BalanceError.raise_error(u'На странице отображается ошибка баланса', False, None, None, None)


def test_product_service_error():
    raise utils.ServiceError(u'Ошибка сервиса')


def test_product_check_that():
    utils.check_that(1, equal_to(2))


# ================= Test defects =================

def test_test_defect():
    raise utils.TestsError(u'Ошибка теста')


def test_test_check_condition():
    utils.check_condition(1, equal_to(2))


def test_test_unsuccessful_response():
    raise utils.UnsuccessfulResponse('UnsuccessfulResponse')
