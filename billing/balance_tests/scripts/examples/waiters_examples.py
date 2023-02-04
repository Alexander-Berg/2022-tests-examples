# coding: utf-8

import btestlib.reporter as reporter
from btestlib import matchers
from btestlib import utils

'''
В модуле содержатся примеры использования метода btestlib/utils.wait_until(),
а также метода btestlib/matchers.matcher_for(condition)

В примере для наглядности matcher_for используется для простого условия типа item['status'] == 'error'
На практике для таких простых случаев лучше использоавть стандартные хамкрестовские матчеры.
Данный метод актуально использовать когда нужно создать матчер на основе некоторго сложного условия,
которое не описывается (или сложно описывается) комбинацией стандартных матчеров.
'''

__author__ = 'fellow'


def condition_negative(item):
    return item['status'] == 'error'


def condition_positive(item):
    return item['status'] == 'success'


def check_this_one(status):
    res = {'status': status}
    reporter.log(res)
    return res


def you_can_wait_a_long_long_time():
    """
    Если условие так и не наступило, бросится исключение
    """
    utils.wait_until(lambda: check_this_one(status='not_defined'),
                     success_condition=matchers.matcher_for(condition_positive, descr='condition positive'),
                     failure_condition=matchers.matcher_for(condition_negative, descr='condition negative'),
                     timeout=10, sleep_time=5)


def shit_happens():
    """
    Здесь мы не будем ждать 10 секунд, а прервемся сразу же
    как только состояние проверяемого объекта станет заведомо некорректным
    """
    utils.wait_until(lambda: check_this_one(status='error'),
                     success_condition=matchers.matcher_for(condition_positive, descr='condition positive'),
                     failure_condition=matchers.matcher_for(condition_negative, descr='condition negative'),
                     timeout=10, sleep_time=5)


def unicorns_exist():
    """
    Говорят, что единороги существуют
    """
    utils.wait_until(lambda: check_this_one(status='success'),
                     success_condition=matchers.matcher_for(condition_positive, descr='condition positive'),
                     failure_condition=matchers.matcher_for(condition_negative, descr='condition negative'),
                     timeout=10, sleep_time=5)


def no_negative_mathers_no_problems():
    """
    matcher_negative - необязательный параметр
    """
    utils.wait_until(lambda: check_this_one(status='not_defined'),
                     success_condition=matchers.matcher_for(condition_positive, descr='condition positive'),
                     timeout=10, sleep_time=5)


if __name__ == '__main__':
    # you_can_wait_a_long_long_time()
    # shit_happens()
    # unicorns_exist()
    no_negative_mathers_no_problems()
