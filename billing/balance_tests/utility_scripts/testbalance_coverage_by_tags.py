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
ctx.prec = 3

def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def get_all_tickets():
    query = u'''Queue: Баланс AND Tags: py, mt'''
    tickets = client.issues.find(query)
    tickets_with_data = {}
    for ticket in tickets:
        fixVersion = ticket.fixVersions[-1].name if ticket.fixVersions else 'NoVersion'
        tickets_with_data[fixVersion] = {'py': {'all': D('0'), 'automated': D('0')}, 'mt': {'all': D('0'), 'automated': D('0')}}
    for ticket in tickets:
        fixVersion = ticket.fixVersions[-1].name if ticket.fixVersions else 'NoVersion'
        if u'py' in ticket.tags:
            tickets_with_data[fixVersion]['py']['all'] += D('1')
            if u'automated' in ticket.tags:
                tickets_with_data[fixVersion]['py']['automated'] += D('1')
        if u'mt' in ticket.tags:
            tickets_with_data[fixVersion]['mt']['all'] += D('1')
            if u'automated' in ticket.tags:
                tickets_with_data[fixVersion]['mt']['automated'] += D('1')


    ttl = {'py': {'not_processed': D('0'), 'processed': D('0')},
           'mt': {'not_processed': D('0'), 'processed': D('0')}}
    for version in sorted(tickets_with_data.keys()):
        reporter.log('{}'.format(version), )
        for cluster in reversed(tickets_with_data[version].keys()):
            all = tickets_with_data[version][cluster]['all']
            processed = tickets_with_data[version][cluster]['automated']
            not_processed = all - processed
            processed_percentage = (processed / all) * D('100') if all else None
            # Add to totals
            ttl[cluster]['processed'] += processed
            ttl[cluster]['not_processed'] += not_processed
            reporter.log('\t{}: {:<6} ({}\{})'.format(cluster,
                                               '{}%'.format(
                                                   processed_percentage) if processed_percentage is not None else '',
                                                      processed,
                                                      all), )
        print('')
    reporter.log(('-' * 46))
    py_total = D(ttl['py']['processed'])+D(ttl['py']['not_processed'])
    py_percentage = (D(ttl['py']['processed']) / py_total) * D('100')
    mt_total = D(ttl['mt']['processed'])+D(ttl['mt']['not_processed'])
    mt_percentage = (D(ttl['mt']['processed']) / mt_total) * D('100')
    reporter.log(('TOTAL\tPy: {:<6} ({}\{})\tMT: {:<6} ({}\{})'.format(py_percentage,
                                                                       D(ttl['py']['processed']),
                                                                       py_total,
                                                                       mt_percentage,
                                                                       D(ttl['mt']['processed']),
                                                                       mt_total)))
    reporter.log(('\nActual on: {}'.format(datetime.datetime.now().date())))
    pass

if __name__ == '__main__':
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token='AVImKxsAAASRwc_Qgs6sRX-SxEfr8Ljrhw')
    get_all_tickets()
    pass