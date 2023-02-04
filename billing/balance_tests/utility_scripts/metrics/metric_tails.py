# -*- coding: utf-8 -*-

import datetime

import processor_tails as processor
from mutils import Steps, Utils, GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = processor.QUEUES
SOURCE = 'balance_qa_metrics_tails'


# Запуск:      | Раз в день, с запуском ночной регрессии
# Предусловие: |
# Данные:      | всегда отправляются данные за сегодня
# Команда запуска:
# python metric_tails.py

def calculate_data():
    # Запускаем пересчёт статистики по тегам на текущую дату
    current_stats = processor.get_all_tickets()
    return current_stats


def do(base=None, n=None, force=None):
    # Cчитаем статистику
    stats = calculate_data()

    for dt, queue, value in stats:
        ts = int(Utils.to_timestamp(dt))
        Steps.sender(source=SOURCE, name=queue, value=value, time=ts, tpl=GraphitSender.DEFAULT_TPL)


if __name__ == "__main__":
    do(None, None, None)
    pass
