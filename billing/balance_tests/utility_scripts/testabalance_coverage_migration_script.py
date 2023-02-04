#-*- coding: utf-8 -*-
import decimal
import pprint

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
             AND (Summary: "[Python]" OR Summary: "[MT]")
             AND  Resolution: !Invalid Resolution: !Дубликат'''
    tickets = client.issues.find(query)
    return [ticket for ticket in tickets if
            (ticket.summary.startswith(u'[Python]') or ticket.summary.startswith(u'[MT]'))]


def get_resolved(tickets):
    return [ticket for ticket in tickets if ticket.status.key in [u'resolved', u'closed']]


def get_all_links(tickets):
    links = []
    for ticket in tickets:
        links.extend([item.object for item in ticket.links.get_all() if item.object.key.startswith(u'BALANCE')])
    for item in links:
        prefix = u''
        tags = item.tags
        if u'automated' not in item.tags:
            prefix = u'[!]'
            tags.append(u'automated')
            item.update(tags=tags)
        reporter.log('{0:<4} {1:<40} {2}'.format(prefix, item.tags, item.key))
    pass

if __name__ == '__main__':
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token='AVImKxsAAASRwc_Qgs6sRX-SxEfr8Ljrhw')
    tickets = get_all_tickets()
    resolved = get_resolved(tickets)
    get_all_links(resolved)
    pass