# coding: utf-8

"""
Тестирование реестров
'Быстрая схема':
1. Имеем входные данные. Это некий фиксированный список реестров и список соответствующих им платежей
   Список реестров для тестирования получаем запросом
   select * from T_INCOMING_MAIL where MSG_PATH LIKE '%host' где host=greed-ts1f или greed-tm1f
   Если требуется запустить тест для конкретного реестра(-ов),
   надо подправить запрос в db_steps.get_registers_for_tests()
   Платежи лежат в отдельных файлах в registers/payments/
   Также есть скрип сохранения платежей registers/load_payments.py
2. Обнуляем записи о наличии реестров по платежам (register_id, register_line_id в t_payments)
   N.B. сейчас этот шаг не делаем, так как появился флаг --rewrite
   N.B. с сентября 2017 опять делаем
3. Инициализируем разбор реестров
4. Ждем пока реестры сматчатся
5. Проверяем, что во всех платежах появились register_id, register_line_id


https://st.yandex-team.ru/TRUST-2069
"""
import csv

import pytest

import btestlib.reporter as reporter
from btestlib import matchers
from btestlib.utils import wait_until
from simpleapi.common import logger
from simpleapi.data import features
from simpleapi.data import stories
from simpleapi.data import marks
from simpleapi.steps import db_steps
from simpleapi.tests.registers.utils import chunks

__author__ = 'fellow'

log = logger.get_logger()


def ids_registers(val):
    return 'id={id}, src={src}'.format(**val)


def get_actual_registers_for_test():
    registers = db_steps.bo().get_registers_for_tests()

    # Несколко реестроов включим в GreenLine
    marked_registers = [marks.simple_internal_logic(registers[i]) if i in (0, 1, 2) else registers[i]
                        for i in range(len(registers))]

    return marked_registers


def get_payments(register_path):
    import os

    with reporter.step(u'Получаем список всех платежей из реестра'):
        __location__ = os.path.realpath(os.path.join(os.getcwd(), os.path.dirname(__file__)))

        payments = []
        with open(os.path.join(__location__, register_path)) as register_file:
            reg_reader = csv.reader(register_file)
            for row in reg_reader:
                payments.append(row[0])

        return payments


def clear_register_ids_for_payments(payments):
    with reporter.step(u'Зануляем каждому платежу register_id и register_line_id'):
        for payments_chunk in chunks(payments):
            db_steps.bo().delete_register_ids_from_payments(payments_chunk)


def initialize_registers_proc(register_id):
    with reporter.step(u'Запускаем парсер реестров'):
        db_steps.bo().set_incoming_mail_status(register_id, status=0)


def get_register_payments(payments):
    with reporter.step(u'Получаем список платежей с данными реестра'):
        return db_steps.bo().get_register_payments(payments)


def wait_for_registers(payments):
    def is_all_registers_complete(payment_registers):
        payments_with_registers = _get_count_of_payments_with_registers(payment_registers)
        all_payments = len(payment_registers)
        log.debug('All payments count {}'.format(all_payments))
        log.debug('Payments with registers count {}'.format(payments_with_registers))
        return payments_with_registers == all_payments

    with reporter.step(u'Ожидаем, пока сматчатся платежи'):
        return wait_until(lambda: get_register_payments(tuple(payments)),
                          success_condition=matchers.matcher_for(is_all_registers_complete,
                                                                 descr='All payment registers complete'),
                          timeout=5 * 60)


def _get_count_of_payments_with_registers(payment_registers):
    return len(filter(lambda x: x['register_id'] is not None and
                                x['register_line_id'] is not None,
                      payment_registers))


# Больше не нужно
# Появился флаг --rewrite
# С ним разборщик не будет смотреть на сматченность платежей, а просто записывать новый разобранный реестр в пеймент
# @pytest.fixture(scope='class')
# def initialize_before_test():
#     for register in register_paths.keys():
#         payments = get_payments('payments/{}.csv'.format(register))
#
#         DB.new_instance(Schemas.BO).delete_register_id(tuple(payments))


@reporter.feature(features.General.Registers)
class TestsRegister(object):
    # pytestmark = marks.simple_internal_logic - tests not stable, now commented

    @reporter.story(stories.General.BaseCycle)
    @pytest.mark.parametrize('register', get_actual_registers_for_test(), ids=ids_registers)
    def test_register_full_cycle(self, register):
        payments = get_payments('payments/{}.csv'.format(register['id']))
        clear_register_ids_for_payments(payments)

        initialize_registers_proc(register['id'])
        db_steps.bo().wait_registers_export_done(register['id'])

        for payments_chunk in chunks(payments):
            wait_for_registers(payments_chunk)


if __name__ == '__main__':
    pytest.main()
