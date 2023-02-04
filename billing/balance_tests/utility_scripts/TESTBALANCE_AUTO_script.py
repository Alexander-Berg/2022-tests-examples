#-*- coding: utf-8 -*-
import pprint

import btestlib.reporter as reporter
from startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))

release = '2.79'
input_data = [
    (23109, 'py', 'atkaya'),
    (23035, 'py', 'atkaya'),
    (23053, 'py', 'atkaya'),
    (22816, 'py', 'atkaya'),

    (22596, 'py', 'aikawa'),
    (23040, 'py', 'aikawa'),
    (23016, 'py', 'aikawa'),
    (23013, 'py', 'aikawa'),
    (23024, 'py', 'aikawa'),
    #
    (23107, 'py', 'torvald'),
    (22495, 'py', 'torvald'),
    # (22876, 'mt', 'torvald'),

    # (22965, 'mt', 'sandyk'),
    # (22801, 'mt', 'sandyk'),
    # (22759, 'mt', 'sandyk'),
    # (22786, 'py', 'sandyk'),
             ]

def create_release (release):
    query = u'queue: TESTBALANCE and summary: "Релиз {0}"'.format(release)
    testb_release_task = client.issues.find(query)
    balance_query = u'queue: BALANCE and summary: "релиз {0}"'.format(release)
    b_release_task = client.issues.find(balance_query)
    if not testb_release_task:
        testb_release_task = client.issues.create(
            queue='TESTBALANCE',
            summary='Релиз {0}'.format(release),
            type={'name': 'Task'},
            description='Parent task for automatization in {0} release'.format(release),
            assignee='igogor',
            fixVersions=release)
        reporter.log(('TESTBALANCE release task created'))
    else:
        testb_release_task = testb_release_task[0]
    return testb_release_task

def create_auto_tasks (testb_release_task, input_data):
    for input_item in input_data:
        query = 'queue: BALANCE AND Key: BALANCE-{0}'.format(input_item[0])
        ticket = client.issues.find(query)[0]
        tag=input_item[1]
        if   tag == 'py': summary_prefix = '[Python]: '
        elif tag == 'mt': summary_prefix = '[MT]: '
        assignee = input_item[2]
        tb_task = client.issues.create(
            queue=u'TESTBALANCE',
            summary=u'{0} {1} {2}'.format(summary_prefix,ticket.key,ticket.summary),
            type={'name': 'Task'},
            description=u'https://st.yandex-team.ru/{0} \n {1}'.format(ticket.key,
                                                            input_item[3] if len(input_item)>3 else None),
            assignee=assignee,
            parent=testb_release_task.key)
        tb_task.update(fixVersions=release)
        ticket.update(tags=tag)
        reporter.log(('Created task {0} for {1}'.format(tb_task, ticket)))

if __name__ == '__main__':
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token='AVImKxsAAASRwc_Qgs6sRX-SxEfr8Ljrhw')
    testb_release_task = create_release (release)
    create_auto_tasks (testb_release_task, input_data)
    pass