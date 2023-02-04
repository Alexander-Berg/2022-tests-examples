#-*- coding: utf-8 -*-

import datetime
import collections

from utility_scripts.startrek.startrek_client import Startrek
from utility_scripts import defaults

RELEASE = '2.86'

# TODO: confirmed? other statuses
NOT_PROCESSED = 'not_processed'
PROCESSED = 'processed'
TESTED = 'tested'
OTHER = 'other'

STATUS_MAP = {u'new': NOT_PROCESSED,
              u'open': NOT_PROCESSED,
              u'inProgress': NOT_PROCESSED,
              u'cvs': PROCESSED,
              u'testing': PROCESSED,
              u'tested': TESTED,
              u'needTestReview': TESTED,
              u'resolved': TESTED,
              u'closed': TESTED,
              u'needInfo': OTHER,
              u'testPending': OTHER,
            }

def st_date_format(date):
    return date.strftime('%Y-%m-%d')

def get_full_release_data(release):
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=defaults.ST_TOKEN)
    queue = 'Queue: Баланс AND "Fix Version": 2.86 AND Type: !Deployment Type: !DDL AND Tags: !without_test'
    tickets = client.issues.find(queue.format(**{'release': release}))
    return tickets

def get_stats(tickets):
    stats = {}
    stats['total'] = {NOT_PROCESSED: 0, PROCESSED: 0, TESTED: 0, OTHER: 0}
    for ticket in tickets:
        stats[ticket.assignee.login] = {NOT_PROCESSED: 0, PROCESSED: 0, TESTED: 0, OTHER: 0}
    for ticket in tickets:
        status_group = STATUS_MAP[ticket.status.key]
        stats[ticket.assignee.login][status_group] += 1
        stats['total'][status_group] += 1
    return stats

def format_stats(stats):
    totals = stats.pop('total')
    devs = stats.keys()
    devs.sort()
    row_template = '|{0:^12}{1:^4}{2:^4}{3:^4}{4:^4}\n'
    divider = '-'*28+'\n'
    formatted = row_template.format('login',
                                    'opn',
                                    'tst',
                                    'don',
                                    'oth')
    formatted += divider
    for dev in devs:
        formatted += row_template.format(dev,
                                         stats[dev][NOT_PROCESSED],
                                         stats[dev][PROCESSED],
                                         stats[dev][TESTED],
                                         stats[dev][OTHER])
    formatted += divider
    formatted += row_template.format('TOTAL',
                                     totals[NOT_PROCESSED],
                                     totals[PROCESSED],
                                     totals[TESTED],
                                     totals[OTHER])
    pass



if __name__ == "__main__":
    update_dt = datetime.datetime(2016, 7, 14)
    tickets = get_full_release_data(RELEASE)
    stats = get_stats(tickets)
    format_stats(stats)