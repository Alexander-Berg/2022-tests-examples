# -*- coding: utf-8 -*-

import datetime
import os

import processor_quality as processor
from mutils import Steps, Utils, GraphitSender


MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = ['ttl', 'tasks', 'issues', 'man', 'auto', 'py', 'mt', 'bugs', 'done', 'open']
SOURCE = 'balance_qa_metrics'
# METRIC = 'one_day.%(host)s.balance_qa_metrics.%(name)s %(metric)s %(time)s'

FILENAME = 'csv_files/data.csv'
ABS_FILENAME = os.path.join(os.path.dirname(__file__), FILENAME)
BACKUP_FOLDER = 'backup/'


# Запуск:      | раз в месяц
# Предусловие: | ST актуализирован в соответствии с запросами.
# Данные:      | всегда отправляются за base период и n-1 предыдущих месяцев
# Команда запуска:
# python metric_quality.py --base '2016-09' --n '1' [--force '1']

def calculate_data(generate_from):
    # Формируем названия вспомогательных файлов: для общей и свёрнутой статистик
    filename = 'data_{0}_{1}.dat'.format(generate_from.strftime('%Y-%m-%d'),
                                         NOW.strftime('%Y-%m-%dT%H_%M_%S'))
    out_filename = '{0}2'.format(filename)

    # Запускаем пересчёт статистики (получаем общую, сворачиваем её и формитируем в необходимом виде)
    processor.get_metrics(base_dt=generate_from, output=filename)
    processor.reduce_data(input=filename, output=out_filename)
    calculated_raw_stats = processor.analyze_data(out_filename)

    # Дополняем статистику до формата keys и убираем месяца ДО generate_from
    calculated_stats = Steps.process_raw_stats(calculated_raw_stats, KEYS, generate_from)

    return calculated_stats


def do(base, n, force):
    # Определяем месяцы, за которые нужно отправить статистику (n месяцев, начиная с base)
    list_to_send = Utils.get_list_to_send(base, n)

    # Получаем предыдущие сохраненные данные из .csv
    stats = Steps.get_monthly_data_from_file(ABS_FILENAME, KEYS)

    # Пересчитываем статистику если force=1 или если данных за нужный месяц нет в .csv файле
    stats = Steps.actualize_stats(stats, list_to_send, calculate_data, force)

    # Сохраняем актуализированную статистику (допонительно делаем backup)
    # Steps.save_actualized_stats(stats, KEYS, ABS_FILENAME, BACKUP_FOLDER)

    # Отправляем данные за месяца, определённые в list_to_send в графит
    Steps.send_data(source=SOURCE, stats=stats, list_to_send=list_to_send, tpl=GraphitSender.DEFAULT_TPL)


if __name__ == "__main__":
    # import sys
    # argv = sys.argv
    # argv.append('--base')
    # argv.append('2016-12')
    # argv.append('--n')
    # argv.append('60')
    # argv.append('--force')
    # argv.append('1')

    # Парсим аргументы из командной строки
    base, n, force = Steps.parse_arguments()

    do(base, n, force)
    pass
