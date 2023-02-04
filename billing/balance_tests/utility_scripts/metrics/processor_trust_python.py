# -*- coding: utf-8 -*-
import datetime
import pprint

import btestlib.reporter as reporter
from btestlib.secrets import get_secret, Tokens
from utility_scripts.startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']


def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def get_all_tickets():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru",
                      token=get_secret(*Tokens.PIPELINER_OAUTH_TOKEN))
    query = u'''Queue: TRUST AND (Tags: need_automated OR Tags: automated_release OR Tags: automated)'''
    tickets = client.issues.find(query)
    need_keys = set([ticket.key for ticket in tickets])
    automated_keys = set([ticket.key for ticket in tickets if {u'automated', u'automated_release'} & set(ticket.tags)])
    incorrect = automated_keys - need_keys
    reporter.log('"automated" tickets without "need_automated": {}'.format(incorrect))

    now = datetime.datetime.now()
    return datetime.datetime(now.year, now.month, now.day), len(need_keys), len(automated_keys)


if __name__ == '__main__':
    get_all_tickets()
    pass
