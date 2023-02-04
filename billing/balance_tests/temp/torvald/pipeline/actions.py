# -*- coding: utf-8 -*-

import xmlrpclib

import parse
import requests
from bs4 import BeautifulSoup

from btestlib.secrets import get_secret, Tokens
from common import telegram_ids, telegram_chat_ids
from utility_scripts.startrek.startrek_client import Startrek
from utils import AlarmException


def get_startrek_connection():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru",
                      token=get_secret(*Tokens.PIPELINER_OAUTH_TOKEN))
    return client


def create_ticket(**kwargs):
    client = get_startrek_connection()
    new_ticket = client.issues.create(**kwargs),


def get_tickets(query):
    client = get_startrek_connection()
    tickets = client.issues.find(query)
    return tickets


def get_ticket(ticket_key):
    client = get_startrek_connection()
    tickets = client.issues.find(keys=[ticket_key])
    assert len(tickets) == 1
    return tickets[0]

def get_main_ticket(queue, pipeline_type):
    query = u'''Queue: {} AND testComment: {} AND status: !closed'''.format(queue, pipeline_type)
    tickets = get_tickets(query)
    assert len(tickets) == 1
    return tickets[0]


def get_responsibles(param=1):
    # TODO: add realization
    if param == 1:
        return {'test': 'torvald', 'dev': 'aikawa', 'admin': 'sandyk'}
    if param == 2:
        return {'test': 'atkaya', 'dev': 'torvald', 'admin': 'aikawa'}


# Startrek ------------------------------------------------------------------------------

class Actions(object):
    context = dict()

    def __init__(self, ticket):

        self.context['ticket'] = ticket
        self.context.update({key: getattr(self, key) for key in self.__class__.__dict__.keys() if
                             not key.startswith('__')})

    def comment(self, template, summonee=None):
        ticket = self.context['ticket']
        text = template.format(**self.context)
        comment = ticket.comments.create(text=text)
        if summonee:
            comment.update(summonee=summonee)
        return True

    def comment_values(self, template):
        # raise AlarmException
        ticket = self.context['ticket']
        comments = reversed(ticket.comments.get_all())
        for comment in comments:
            params = parse.parse(template, comment.text)
            if params:
                # TODO: validate not named params
                if params.fixed:
                    print 'Something went wrong: we have fixed params'
                self.context.update(params.named)
        return True

    def change_status(self, status):
        ticket = self.context['ticket']
        status_by_transition = {transition.to.key: transition.id for transition in ticket.transitions.get_all()}
        if status not in status_by_transition:
            print 'Unsupported transition'
            return
        ticket.transitions.get(status_by_transition[status]).execute()

    def has_status(self, status):
        ticket = self.context['ticket']
        return ticket.status.key == status

    def assign(self, login):
        ticket = self.context['ticket']
        ticket.update(assignee=login)

    # Other ------------------------------------------------------------------------------

    def raise_exception(self, exc):
        raise exc

    def check(self, flag):
        return bool(flag)

    def executeSQL(self, query, host='ts1f', user='balance'):
        endpoint = 'http://greed-{}.yandex.ru:30702/xmlrpc'.format(host)
        rpc = xmlrpclib.ServerProxy(endpoint, allow_none=1, use_datetime=1)
        return rpc.Balance.ExecuteSQL(user, query)

    # Teamcity ------------------------------------------------------------------------------

    def start_teamcity(self, confs):
        result = dict()
        for conf in confs:
            response = requests.request('post', 'https://teamcity.yandex-team.ru/app/rest/buildQueue',
                                        headers={'Content-Type': 'application/xml'},
                                        data='<build><buildType id="{}"/></build>'.format(conf),
                                        auth=('dostupbot', '#dasBott'), timeout=60 * 5)
            parsed = BeautifulSoup(response.content, 'xml')
            result[conf] = parsed.build['id']
        self.context.update(result)
        return True

    def is_task_finished(self, task):
        task = self.context[task]
        response = requests.request('get',
                                    'https://teamcity.yandex-team.ru/app/rest/buildQueue/{}'.format(task),
                                    auth=('dostupbot', '#dasBott'), timeout=60 * 5)
        parsed = BeautifulSoup(response.content, 'xml')
        return parsed.build['state'] == u'finished'

    def is_task_passed(self, task):
        task = self.context[task]
        result = requests.request('get', 'https://teamcity.yandex-team.ru/app/rest/buildQueue/{}'.format(task),
                                  auth=('dostupbot', '#dasBott'), timeout=60 * 5)
        parsed = BeautifulSoup(result.content, 'xml')
        return parsed.build['status'] == u'SUCCESS'

    def is_tasks_finished(self, system, tasks):
        if system == 'teamcity':
            return all([self.is_task_finished(task) for task in tasks])

    def is_tasks_passed(self, system, tasks):
        if system == 'teamcity':
            if not self.is_tasks_finished(system='teamcity', tasks=tasks):
                return False
            for task in tasks:
                if not self.is_task_passed(task=task):
                    raise AlarmException
            return True

    # Aqua ------------------------------------------------------------------------------

    def start_aqua(self, confs):

        # TODO
        import random
        return {conf: random.randint(10000, 20000) for conf in confs}

    # Telegram ---------------------------------------------------------------------------

    def send(self, msg, recepient=None):

        ids = telegram_ids
        ids.update(telegram_chat_ids)
        if recepient not in ids:
            print 'Unknown recepient or chat'
            return
        response = requests.request('post',
                                    'https://api.telegram.org/bot404767360:AAFEpHQ9XfrBT6_K0hT5uP07a6-SuVDhJ7U/sendMessage',
                                    params={'chat_id': ids[recepient], 'text': msg, 'parse_mode': 'Markdown'},
                                    timeout=60 * 5)


if __name__ == "__main__":
    a = Actions(1)
    print(a.start_teamcity(['Billing_Autotesting_PythonTests_Smoke']))
    print(a.is_tasks_finished('teamcity', [10009594, 10008906, 9991776, 10009183], None, None))
    print(a.is_tasks_passed('teamcity', [10009594, 10008906, 9991776, 10009183], None, None))
    print(a.is_tasks_passed('teamcity', [10009594], None, None))
