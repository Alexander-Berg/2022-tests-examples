#-*- coding: utf-8 -*-
import datetime
import decimal
import pprint
from decimal import Decimal as D

import btestlib.reporter as reporter
from startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

ctx = decimal.getcontext()
ctx.prec = 4

def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def get_all_tickets():
    query = u'''queue: "Тестирование Баланса"
                AND  (Summary: "[Python]" OR Summary: "[MT]")'''
    tickets = client.issues.find(query)
    tickets_with_data = []
    for ticket in tickets:
        # Throw out 'invalid' tickets
        invalid_ticket = ticket.resolution and (ticket.resolution.key in [u'invalid', u'duplicate'])
        correct_cluster = ticket.summary.startswith(u'[Python]') or ticket.summary.startswith(u'[MT]')
        in_version = ticket.fixVersions
        if not invalid_ticket and correct_cluster and in_version:
                tickets_with_data.append({'ticket': ticket,
                                          'cluster': ticket.summary[1:3],
                                          'key': ticket.key,
                                          'summary': ticket.summary,
                                          'assignee': ticket.assignee.login,
                                          'status': ticket.status.key,
                                          'resolution': ticket.resolution.key if ticket.resolution else None,
                                          'fixVersions': ticket.fixVersions[-1].name if ticket.fixVersions else None,
                                          'parent': ticket.parent
                                        }
                                    )
    return tickets_with_data


def get_ticket_status(ticket):
    NOT_PROCESSED = [u'new', u'open', u'needInfo']
    PROCESSED = [u'resolved']
    # 'open' if status in NOT_PROCESSED status list OR 'closed' with resolution 'won'tFix'
    if (ticket['status'] in NOT_PROCESSED) or (ticket['status'] == u'closed' and ticket['resolution'] in [u"won'tFix"]):
        status = 'not_processed'
    # 'processed' if status in PROCESSED status list OR 'closed' with resolutions ['fixed', 'invalid']
    elif (ticket['status'] in PROCESSED) or (ticket['status'] == u'closed' and ticket['resolution'] in [u'fixed', u'invalid']):
        status = 'processed'
    else:
        status = 'unknown'
    return status


def group_tickets(tickets_with_data):
    # tickets_by_version = collections.defaultdict(dict)
    tickets_by_version = {}
    for ticket in tickets_with_data:
        status = get_ticket_status(ticket)
        if ticket['fixVersions'] not in tickets_by_version:
            tickets_by_version[ticket['fixVersions']] = {u'Py': {'not_processed': [], 'processed': [], 'unknown': []},
                                                         u'MT': {'not_processed': [], 'processed': [], 'unknown': []}}
        tickets_by_version[ticket['fixVersions']][ticket['cluster']][status].append(ticket)
    return tickets_by_version

def get_stats(tickets_by_version):
    ttl = {u'Py': {'not_processed': 0, 'processed': 0, 'unknown': 0},
           u'MT': {'not_processed': 0, 'processed': 0, 'unknown': 0}}
    for version in sorted(tickets_by_version.keys()):
        reporter.log('{}'.format(version), )
        for cluster in reversed(tickets_by_version[version].keys()):
            not_processed = D(len(tickets_by_version[version][cluster]['not_processed']))
            processed = D(len(tickets_by_version[version][cluster]['processed']))
            unknown = D(len(tickets_by_version[version][cluster]['unknown']))
            all_tickets = processed + not_processed
            processed_percentage = (processed / all_tickets if all_tickets else 1) * D('100')
            # Add to totals
            ttl[cluster]['processed'] += processed
            ttl[cluster]['not_processed'] += not_processed
            ttl[cluster]['unknown'] += unknown
            reporter.log('\t{}: {:<6} ({}\{})'.format(cluster,
                                           '{}%'.format(processed_percentage),
                                                      processed,
                                                      all_tickets), )
        reporter.log('')
    reporter.log(('-' * 46))
    py_total = D(ttl[u'Py']['processed'])+D(ttl[u'Py']['not_processed'])
    py_percentage = (D(ttl[u'Py']['processed']) / py_total) * D('100')
    mt_total = D(ttl[u'MT']['processed'])+D(ttl[u'MT']['not_processed'])
    mt_percentage = (D(ttl[u'MT']['processed']) / mt_total) * D('100')
    reporter.log(('TOTAL\tPy: {:<6} ({}\{})\tMT: {:<6} ({}\{})'.format(py_percentage,
                                                                       D(ttl[u'Py']['processed']),
                                                                       py_total,
                                                                       mt_percentage,
                                                                       D(ttl[u'MT']['processed']),
                                                                       mt_total)))
    reporter.log(('\nActual on: {}'.format(datetime.datetime.now().date())))


if __name__ == '__main__':
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token='AVImKxsAAASRwc_Qgs6sRX-SxEfr8Ljrhw')
    tickets_with_data = get_all_tickets()
    tickets_by_version = group_tickets(tickets_with_data)
    get_stats(tickets_by_version)
    pass