# coding: utf-8
import csv

from simpleapi.steps import db_steps

__author__ = 'fellow'

'''
Загрузка платежей по реестрам
Файлы с платежами грузятся в папку payments рядом с тестами
Название файла формируется из t_incoming_mail.msg_path отрезанием первой и последней части
Например, для реестра /large/test_samples/ybprocess.msg.1477613853913726463.greed-ts1f платежи загрузятся в файл
ybprocess.msg.1477613853913726463.cvs
Существующие фалы с теми же названиями перезапишутся

Список реестров, которые будут обработаны возвращается запросом:
SELECT * FROM T_INCOMING_MAIL WHERE MSG_PATH LIKE '%host';

'''

host = 'greed-ts1f'


def write_to_file(payments, filename):
    import os

    __location__ = os.path.realpath(os.path.join(os.getcwd(), os.path.dirname(__file__)))

    with open(os.path.join(__location__, filename), 'wb') as csvfile:
        payments_writer = csv.writer(csvfile)
        for payment in payments:
            payments_writer.writerow([payment])


def load_payments():
    registers = db_steps.bo().get_registers_for_tests(host=host)
    for register in registers:
        payments = db_steps.bo().get_all_payments_of_register(register['id'])
        if not isinstance(payments, (list, tuple)):
            payments = (payments,)
        payments_ids = [p['id'] for p in payments]
        write_to_file(payments_ids, 'payments/{}.csv'.format(register['id']))


if __name__ == '__main__':
    load_payments()
