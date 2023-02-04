#-*- coding: utf-8 -*-
import pprint

import btestlib.reporter as reporter
from startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))

input_data = [
            (639, 'sandyk', u'comment')
           # ,(21949, 'sandyk', u'comment)
             ]

def create_auto_tasks (input_data):
    for input_item in input_data:
        query = 'queue: BCL AND Key: BCL-{0}'.format(input_item[0])
        ticket = client.issues.find(query)[0]
        tag = 'bcl'
        summary_prefix = '[BCL]: '
        assignee = input_item[1]
        tb_task = client.issues.create(
            queue=u'TESTBALANCE',
            summary=u'{0} {1} {2}'.format(summary_prefix,ticket.key,ticket.summary),
            type={'name': 'Task'},
            description=u'https://st.yandex-team.ru/{0} \n {1}'.format(ticket.key,
                                                            input_item[2] if len(input_item)>2 else None),
            assignee=assignee)
        ticket.update(tags=tag)
        reporter.log(('Created task {0} for {1}'.format(tb_task, ticket)))


if __name__ == '__main__':
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token='c654ff06d1f04f05808cb85f58e98db2')
    create_auto_tasks(input_data)
    pass