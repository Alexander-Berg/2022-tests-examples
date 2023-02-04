# -*- coding: utf-8 -*-

import datetime
import os

import processor_weighted_quality as processor
from mutils import Steps, Utils, GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = ['billing', 'support', 'high', 'mid', 'low']
SOURCE = 'balance_qa_metrics_weighted'
# METRIC = 'one_day.%(host)s.balance_qa_metrics_weighted.%(name)s %(metric)s %(time)s'

FILENAME = 'csv_files/weighted_quality.csv'
ABS_FILENAME = os.path.join(os.path.dirname(__file__), FILENAME)
BACKUP_FOLDER = 'backup/'


# Запуск:      | раз в месяц
# Предусловие: | пропущенные баги в ST размечены приоритетами
# Данные:      | всегда отправляются за все месяцы, начиная с 2014-12
# Команда запуска:
# python metric_weighted_quality.py --base '2016-09' --n '1' [--force '1']

def calculate_data(generate_from):
    # Запускаем анализ статистики из ST
    calculated_raw_stats = processor.analyze_data()

    # Преобразуем формат wighted_processor (словарь со namedtuple Stats в значениях) в словарь словарей с правильными ключами
    calculated_stats = {Utils.last_day_of_month(Utils.str_to_date(key)): {'billing': str(value.billing),
                                                                          'support': str(value.support),
                                                                          'high': str(value.high),
                                                                          'mid': str(value.mid),
                                                                          'low': str(value.low),
                                                                          }
                        for key, value in calculated_raw_stats.iteritems()}

    return calculated_stats


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
    # argv.append('23')
    # argv.append('--force')
    # argv.append('1')

    # Парсим аргументы из командной строки
    base, n, force = Steps.parse_arguments()

    do(base, n, force)
    pass
