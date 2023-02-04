# -*- coding: utf-8 -*-
import datetime
import decimal
import pprint

import btestlib.reporter as reporter
from btestlib import secrets
from utility_scripts.startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

QUEUES = ['BALANCE', 'SPIRIT', 'APIKEYS', 'TRUST', 'MEDVED', 'BCL', 'CHECK', 'PCIDSS']

ctx = decimal.getcontext()
ctx.prec = 3


def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def get_all_tickets():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru",
                      token=secrets.get_secret(*secrets.Tokens.PIPELINER_OAUTH_TOKEN))

    # query = u'''(queue: Balance
    #              OR queue: SPIRIT
    #              OR queue: APIKEYS
    #              OR queue: TRUST
    #              OR queue: MEDVED
    #              OR queue: BCL
    #              OR queue: CHECK
    #              OR queue: PCIDSS)
    #              and (status: Тестируется OR status: "Можно тестировать")
    #              and tags: !"without_test"'''
    # tickets = client.issues.find(query)
    # print 'LENNNNNNNNNNNNNNNNNNN: {}'.format(len(tickets))
    #
    # stats = {queue: 0 for queue in QUEUES}
    #
    # print 'STAAAAAAAAAAAAAAAAATS: {}'.format(stats)
    #
    # for queue in QUEUES:
    #     stats[queue] = len([ticket for ticket in tickets if queue in ticket.key])
    #
    # now = datetime.datetime.now()
    # return [(datetime.datetime(now.year, now.month, now.day), queue, stats[queue]) for queue in QUEUES]

    balance_q = u'''queue: Balance 
                    AND (status: Тестируется OR status: "Можно тестировать") 
                    AND tags: !"without_test" 
                    AND Type: !Хотфикс
                    AND Components: !"Commision (комиссия)"'''

    commisions_q = u'''queue: Balance 
                       AND (status: Тестируется OR status: "Можно тестировать") 
                       AND tags: !"without_test" 
                       AND Components: "Commision (комиссия)"'''

    apikeys_q = u'''queue: APIKEYS 
                    AND (status: Тестируется OR status: "Можно тестировать") 
                    AND tags: !"without_test" 
                    AND Type: !Релиз'''

    bcl_q = u'''queue: BCL 
                AND (status: Тестируется OR status: "Можно тестировать") 
                AND tags: !"without_test" 
                AND Type: !Выкладка '''

    check_q = u'''queue: CHECK 
                  AND (status: Тестируется OR status: "Можно тестировать") 
                  AND tags: !"without_test" 
                  AND Type: !Релиз
                  AND Type: !Хотфикс'''

    pairs = [('BALANCE', balance_q),
             ('COMM', commisions_q),
             ('APIKEYS', apikeys_q),
             ('BCL', bcl_q),
             ('CHECK', check_q)]

    stats = {}

    for queue, query in pairs:
        tickets = client.issues.find(query)
        print '{}: {}'.format(len(tickets), queue)

        stats[queue] = len(tickets)

    now = datetime.datetime.now()
    return [(datetime.datetime(now.year, now.month, now.day), queue, count) for queue, count in stats.items()]


if __name__ == '__main__':
    get_all_tickets()
    pass
