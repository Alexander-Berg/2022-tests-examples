# -*- coding: utf-8 -*-

import datetime
import os

import processor_trust_python as processor
from mutils import Steps, GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = ['need_automation', 'automated']
SOURCE = 'balance_qa_metrics_python_trust'
# METRIC = 'one_day.%(host)s.%(source)s.%(name)s %(metric)s %(time)s'

# Запуск:      | Раз в день, с запуском ночной регрессии
# Предусловие: |
# Данные:      | всегда отправляются данные за сегодня
# Команда запуска:
# python metric_python.py

def calculate_data():
    # Запускаем пересчёт статистики по тегам на текущую дату
    current_stats = processor.get_all_tickets()
    return current_stats


def do(base=None, n=None, force=None):

    # Пересчитываем статистику
    stats = calculate_data()
    period, values = stats[0], stats[1:]
    stats = {period: dict(zip(KEYS, tuple(str(value) for value in values)))}

    # Отправляем данные только за текущий день в графит (для этого list_to_send = [period]
    Steps.send_data(source=SOURCE, stats=stats, list_to_send=[period], tpl=GraphitSender.DEFAULT_TPL)


if __name__ == "__main__":
    do(None, None, None)
    pass
