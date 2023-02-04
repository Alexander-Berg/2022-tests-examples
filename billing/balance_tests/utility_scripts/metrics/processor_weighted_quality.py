# -*- coding: utf-8 -*-
import collections
import pprint

import btestlib.reporter as reporter
from btestlib.secrets import get_secret, Tokens
from utility_scripts.startrek.startrek_client import Startrek

ST_TOKEN = get_secret(*Tokens.PIPELINER_OAUTH_TOKEN)

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

BILLING_PRIORITIES = [u'B0', u'B1', u'B2', u'B3']
SUPPORT_PRIORITIES = [u'S0', u'S1', u'S2', u'S3']


def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def process_ticket(ticket):
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    key, dt, billing, support, summary = ticket
    query = 'queue: BALANCE AND Key: {0}'.format(key)
    tkt = client.issues.find(query)[0]
    reporter.log('\t\t{0} - {1}'.format(key, tkt.tags))
    msg = '{}: '.format(tkt.key)
    # reporter.log(('Start process: {}'.format(tkt.key)))
    tags_list = tkt.tags
    if billing and support:
        msg += 'B{}, S{} >>> {}'.format(billing, support, tags_list)
        # reporter.log(('... Stats: B{}, S{}; tags_list: {}'.format(billing, support, tags_list)))
        if not ((set(tags_list) & set(BILLING_PRIORITIES)) or (set(tags_list) & set(SUPPORT_PRIORITIES))):
            tags_list.append(u'B{0}'.format(billing))
            tags_list.append(u'S{0}'.format(support))
            # reporter.log(('... [SUCCESS] Set {} for {}'.format(tags_list, tkt.key)))
            msg = '[SUCCESS] - ' + msg
            tkt.update(tags=tags_list)
        else:
            # reporter.log(('... [ALREADY]: Stats already processed'))
            msg = '[ALREADY] - ' + msg
    else:
        # reporter.log(('... [NO_DATA]: no billing or support data'))
        msg = '[NO_DATA] - ' + msg
    reporter.log(msg)


def analyze_data():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    query = '''queue: Баланс AND (type: Bug OR type: Sub-bug)
    AND Created: >= "23-07-2012"
    AND Status: !New Status: !Open Status: !Open
    AND Stage: !Testing
    AND (Resolution: !Duplicate Resolution: !"Won't fix" Resolution: !"Will Not Fix" Resolution: !"Can't reproduce" Resolution: !"Invalid")
    AND "Internal Design": !"Да"
    AND "Test Scope": !"Нет"
    AND (Security: empty() OR Security: "Нет")
    AND ("tags": "B0" OR "tags": "B1" OR "tags": "B2" OR "tags": "B3")
    AND ("tags": "S0" OR "tags": "S1" OR  "tags": "S2" OR "tags": "S3")
    "Sort by": created desc'''
    tickets = client.issues.find(query)

    class Stats(object):
        def __init__(self, billing=0, support=0, high=0, mid=0, low=0):
            self.billing = billing
            self.support = support
            self.high = high
            self.mid = mid
            self.low = low

    stats = Stats()

    priority_by_month = collections.defaultdict(Stats)
    for ticket in tickets:
        billing_priority_tag = [item for item in ticket.tags if item in BILLING_PRIORITIES][0]
        support_priority_tag = [item for item in ticket.tags if item in SUPPORT_PRIORITIES][0]
        month = ticket.createdAt[:7]
        billing = int(billing_priority_tag[1])
        support = int(support_priority_tag[1])
        sum = billing + support
        priority_by_month[month].billing += billing
        priority_by_month[month].support += support

        if sum >= 4:
            priority_by_month[month].high += sum
        elif billing == 2 or support == 2 or sum >= 3:
            priority_by_month[month].mid += sum
        else:
            priority_by_month[month].low += sum

    month_list = priority_by_month.keys()
    month_list.sort()
    for month in month_list:
        reporter.log('{}: {} {} ({} + {} + {})'.format(month, priority_by_month[month].billing,
                                                       priority_by_month[month].support,
                                                       priority_by_month[month].high,
                                                       priority_by_month[month].mid,
                                                       priority_by_month[month].low,
                                                       ))

    return priority_by_month


if __name__ == '__main__':
    # with open('C:\\torvald\_PROCESS\METRICS\Failed_list', 'r') as data:
    #     tickets = data.readlines()
    #     tickets = [row.split('\t') for row in tickets if row.startswith('BALANCE')]
    #     for ticket in tickets:
    #         process_ticket(ticket)
    stats = analyze_data()
    month_list = stats.keys()
    month_list.sort()
    for month in month_list:
        reporter.log('{}: {} {} ({} + {} + {})'.format(month, stats[month].billing,
                                                       stats[month].support,
                                                       stats[month].high,
                                                       stats[month].mid,
                                                       stats[month].low,
                                                       ))
