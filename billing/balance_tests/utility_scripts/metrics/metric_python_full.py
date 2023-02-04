# -*- coding: utf-8 -*-

import datetime

import processor_python_full as processor
from mutils import Steps, Utils, GraphitSender

MONTH_FORMATS = ['YYYY-MM', 'MM-YYYY', 'YYYY.MM', 'MM.YYYY']
NOW = datetime.datetime.now()

KEYS = ['py_percentage', 'py_automated', 'py', 'my_percentage', 'mt_automated', 'mt']
SOURCE_ONE = 'balance_qa_metrics_python_release'
SOURCE_TTL = 'balance_qa_metrics_python_ttl'
# METRIC_ONE = 'one_day.%(host)s.balance_qa_metrics_python_release.%(name)s %(metric)s %(time)s'
# METRIC_TTL = 'one_day.%(host)s.balance_qa_metrics_python_ttl.%(name)s %(metric)s %(time)s'


# Запуск:      | Раз в день, с запуском ночной регрессии
# Предусловие: |
# Данные:      | всегда отправляются данные за сегодня
# Команда запуска:
# python metric_python.py

def do(base, n, force):
    total_stats, release_stats = processor.get_all_tickets()
    print total_stats
    print release_stats

    dt = int(Utils.to_timestamp(total_stats['date']))
    for first in ['all', 'py', 'mt']:
        for second in ['all', 'automated', 'open', 'percentage']:
            Steps.sender(source=SOURCE_TTL, name='{0}_{1}'.format(first, second), value=total_stats[first][second],
                         time=dt, tpl=GraphitSender.DEFAULT_TPL)

    for release in release_stats:
        dt = int(Utils.to_timestamp(release_stats[release]['date']))
        for first in ['all', 'py', 'mt']:
            for second in ['all', 'automated', 'open', 'percentage', 'tasks']:
                Steps.sender(source=SOURCE_ONE, name='{0}_{1}'.format(first, second),
                             value=release_stats[release][first][second], time=dt, tpl=GraphitSender.DEFAULT_TPL)


if __name__ == "__main__":
    do(None, None, None)
    pass
