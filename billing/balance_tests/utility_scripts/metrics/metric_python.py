# -*- coding: utf-8 -*-

import datetime
import os

import processor_python as processor
from mutils import Steps, GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = ['py_percentage', 'py_automated', 'py', 'my_percentage', 'mt_automated', 'mt']
SOURCE = 'balance_qa_metrics_python'
# METRIC = 'one_day.%(host)s.%(source)s.%(name)s %(metric)s %(time)s'

FILENAME = 'csv_files/python.csv'
ABS_FILENAME = os.path.join(os.path.dirname(__file__), FILENAME)
BACKUP_FOLDER = 'backup/'


# Запуск:      | Раз в день, с запуском ночной регрессии
# Предусловие: |
# Данные:      | всегда отправляются данные за сегодня
# Команда запуска:
# python metric_python.py

def calculate_data():
    # Запускаем пересчёт статистики по тегам на текущую дату
    current_stats = processor.get_all_tickets()
    return current_stats


def do(base, n, force):
    # Получаем предыдущие сохраненные данные из .csv
    stats = Steps.get_daily_data_from_file(ABS_FILENAME, KEYS, date_format='%Y-%m-%d')

    # Пересчитываем статистику
    raw_stats = calculate_data()
    period, values = raw_stats[0], raw_stats[1:]
    last_stat = {period: dict(zip(KEYS, tuple(str(value) for value in values)))}
    stats.update(last_stat)

    # Сохраняем актуализированную статистику (допонительно делаем backup)
    Steps.save_actualized_stats(stats, KEYS, ABS_FILENAME, BACKUP_FOLDER, date_format='%Y-%m-%d')

    # Отправляем данные только за текущий день в графит (для этого list_to_send = [period]
    Steps.send_data(source=SOURCE, stats=stats, list_to_send=[period], tpl=GraphitSender.DEFAULT_TPL)


if __name__ == "__main__":
    do(None, None, None)
    pass
