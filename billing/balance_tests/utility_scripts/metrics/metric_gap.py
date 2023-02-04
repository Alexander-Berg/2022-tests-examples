# -*- coding: utf-8 -*-

import datetime
import os

from mutils import Steps, Utils, GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = ['trust', 'partner', 'balance', 'bank_clients', 'ui', 'comm', 'reports', 'dcs', 'tel', 'apikeys'
        'test_balance', 'test_dbm', 'autotest_balance', 'autotest_trust']
SOURCE = 'balance_qa_metrics_gap'
# METRIC = 'one_day.%(host)s.balance_qa_metrics_gap.%(name)s %(metric)s %(time)s'

FILENAME = 'csv_files/gap.csv'
ABS_FILENAME = os.path.join(os.path.dirname(__file__), FILENAME)
BACKUP_FOLDER = 'backup/'


# Запуск:      | раз в месяц
# Предусловие: | данные из gap собраны в gap.csv (вручную)
# Данные:      | всегда отправляются за base период и n-1 предыдущих месяцев
# Команда запуска:
# python metric_gap.py --base '2016-09' --n '1' [--force '1']

def calculate_data(generate_from):
    return dict()


def do(base, n, force):
    # Определяем месяцы, за которые нужно отправить статистику (n месяцев, начиная с base)
    list_to_send = Utils.get_list_to_send(base, n)

    # Получаем предыдущие сохраненные данные из .csv
    stats = Steps.get_monthly_data_from_file(ABS_FILENAME, KEYS)

    # Пересчитываем статистику если force=1 или если данных за нужный месяц нет в .csv файле
    stats = Steps.actualize_stats(stats, list_to_send, calculate_data, force)

    # Сохраняем актуализированную статистику (допонительно делаем backup)
    Steps.save_actualized_stats(stats, KEYS, ABS_FILENAME, BACKUP_FOLDER)

    # Отправляем данные за месяца, определённые в list_to_send в графит
    Steps.send_data(source=SOURCE, stats=stats, list_to_send=list_to_send, tpl=GraphitSender.DEFAULT_TPL)


if __name__ == "__main__":
    # import sys
    # argv = sys.argv
    # argv.append('--base')
    # argv.append('2016-10')
    # argv.append('--n')
    # argv.append('34')
    # argv.append('--force')
    # argv.append('0')

    # Парсим аргументы из командной строки
    base, n, force = Steps.parse_arguments()

    # Запускаем расчёт метрики
    do(base, n, force)
    pass
