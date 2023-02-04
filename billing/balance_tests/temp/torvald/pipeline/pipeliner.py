# -*- coding: utf-8 -*-

import copy

from actions import Actions, get_main_ticket, create_ticket, get_tickets, get_ticket  # get_responsibles
from btestlib.secrets import get_secret, Tokens
from common import defaults
from temp.torvald.pipeline.utils import load_pipeline
# from temp.torvald.pipeline.balance.pipelines import templates
from utility_scripts.startrek.startrek_client import Startrek
from utils import AlarmException, SeveralAllowedTransitionsException


# TODO: add logging insted of print
# TODO: global try\except ?
# TODO: get real dev\test\admin\mng
# Получение списка тикетов попадающих в выкладку для хот-фикса и релиза
# Получать owner при смене статуса для отправки сообщения в личку.

class Pipeline(object):
    state = None
    pipeline = None
    owner = None

    def __init__(self, pipeline, ticket, state=None):

        # Ticket
        self.ticket = ticket
        ticket_state = ticket.otrsTicket
        if ticket_state:
            if ticket_state in pipeline.keys():
                self.state = ticket_state
            else:
                raise Exception('TICKET IN UNKNOWN STATE!')
        else:
            ticket.update(otrsTicket=self.state)

        self.pipeline = pipeline
        self.owner = pipeline[self.state]['owner']
        self.alarm = bool(self.ticket.crashId)
        self.actions = Actions(self.ticket)

        self.actions.context.update(
            {key: value for key, value in templates.__dict__.items() if not key.startswith('__')})

    def go(self):
        try:
            current_state = self.pipeline[self.state]
            self.owner = current_state['owner']
            self.alarm = bool(self.ticket.crashId)
            edges = current_state.get('edges', dict())
            print '\tTransitions: {}'.format(edges.keys())

            # In alarm case we shouldn't do anything till problem will be solved
            if self.alarm:
                print 'ALARM! Resolve failures to proceed transitions'
                return

            if not edges:
                print 'Successfully finished'
                return

            # Try conditions for all transitions to check multiple allowed transitions.
            allowed_transitions = []
            for edge, transition in edges.items():
                print '\tChanging to {} state...'.format(edge)

                result = True
                for condition in transition.get('conditions', []):
                    status = eval(condition, globals(), self.actions.context)
                    result = result and status

                if result:
                    print '\t\tGRANTED'
                    allowed_transitions.append((edge, transition))
                else:
                    print '\t\tDENIED'

            if not allowed_transitions:
                print 'No allowed transition for this run'
                return

            if len(allowed_transitions) > 1:
                raise SeveralAllowedTransitionsException

            edge, transition = allowed_transitions[0]

            for action in transition.get('actions', []):
                print action
                eval(action, globals(), self.actions.context)

            self.state = edge
            self.ticket.update(otrsTicket=self.state)

            # При смене состояния назначаем тикет на владельца нового состояния
            # self.actions.assign(self.pipeline[self.state]['owner'])
            return

        except AlarmException:
            # TODO: NeedInfo should be added to all statuses in ST admin panel.
            msg = 'Hello {}, fix me, please'.format(self.owner)
            self.actions.send(msg, self.owner)
            self.actions.send(msg, 'test_chat')
            self.ticket.update(crashId=u'alarm')

        except Exception as exc:
            msg = 'Hello {}, exception!'.format(self.owner)
            self.actions.send(msg, self.owner)
            self.actions.send(msg, 'test_chat')
            self.ticket.update(crashId=u'alarm')

    def __repr__(self):
        edges = self.pipeline[self.state].get('edges', dict())
        return 'current: {}\nowner: {}\nnext: {}'.format(self.state, self.owner, edges.keys())


def fill_defaults(pipeline):
    responsibles = get_responsibles()

    for state, content in pipeline.items():
        owner = content.get('owner', defaults.owner)
        # Превращаем 'dev \ test \ admin' в логины в поле owner
        content['owner'] = responsibles.get(owner, owner)
        for edge, transition in content.get('edges', dict()).items():
            for attr in ['notify', 'conditions', 'actions']:
                transition[attr] = transition.get(attr, defaults.edges[attr]) or defaults.edges[attr]
            # Превращаем 'dev \ test \ admin' в логины в поле owner
            transition['notify'] = [responsibles.get(member, member) for member in transition['notify']]
    return pipeline


def restore_pipeline(pipeline):
    # Doesn't support several levels of
    for state, content in pipeline.items():
        if 'origin' in content:
            origin = content['origin']
            origin_pipeline = load_pipeline(origin)
            pipeline.pop(state)
            origin_pipeline.update(pipeline)
            pipeline = origin_pipeline
    return pipeline


def restore_preciselly(pipeline):
    # Include states from other pipelines or common pipeline
    full_pipeline = copy.deepcopy(pipeline)
    for state, content in pipeline.items():
        if 'origin' in content:
            origin = content['origin']
            finish = content['finish']
            origin_pipeline = load_pipeline(origin)
            included_states = set(
                [origin_state for origin_state in origin_pipeline[state].get('edges', dict()).keys() if
                 origin_state != finish])
            included_states.add(state)
            while included_states:
                origin_state = included_states.pop()
                full_pipeline[origin_state] = origin_pipeline[origin_state]
                included_states.update(
                    set([transition for transition in origin_pipeline[origin_state].get('edges', dict()).keys() if
                         transition not in [finish, origin_state]
                         ]))
            full_pipeline[finish] = origin_pipeline[finish]
    return full_pipeline


# ----------------------------------------------------------------------------------------------------------

# Должны находиться в каждом репозитории
QUEUE = u'BALANCE'
DEFAULT_PIPELINE_PATH = 'balance/'

CONTROL_TICKET_PARAMS = {'balance_release': {'description': '',
                                             'followers': None,
                                             'followingMaillists': None,
                                             'parent': None,  # TODO: научиться получать текущий релиз
                                             'priority': 4,
                                             'qaEngineer': None,
                                             'queue': 'BALANCE',
                                             'summary': 'test',
                                             'tags': None,
                                             'type': 215,
                                             'otrsTicket': 'INITIAL',  # pipelineStatus
                                             'crashId': None,  # alarm
                                             'testComment': 'balance_release'  # pipeline
                                             },
                         'balance_hotfix': {}
                         }


def init(pipeline_type):
    params = CONTROL_TICKET_PARAMS.get(pipeline_type, None)
    if not params:
        print 'Unknown pipeline type'
    create_ticket(**params)


def get(pipeline_type):
    query = u'''Queue: {} AND testComment: {} AND status: !closed'''.format(QUEUE, pipeline_type)
    tickets = get_tickets(query)
    return [ticket.key for ticket in tickets]


# TODO: transitions to closed from any status
def terminate(ticket_key):
    pass


def move(ticket_key):
    ticket = get_ticket(ticket_key)
    pipeline_type = ticket.testComment
    pipeline = load_pipeline(DEFAULT_PIPELINE_PATH + pipeline_type + '.yml')
    restore_pipeline(pipeline)
    fill_defaults(pipeline)
    p = Pipeline(pipeline, ticket=ticket)
    p.go()


if __name__ == "__main__":
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru",
                      token=get_secret(*Tokens.PIPELINER_OAUTH_TOKEN))

    ticket = client.issues.find(keys=['TESTBALANCE-1523'])
    status_by_transition = {transition.to.key: transition.id for transition in ticket.transitions.get_all()}
    assert status in status_by_transition
    ticket.transitions.get(status_by_transition[status]).execute()
    # ------------------------------------
    ticket = get_main_ticket(u'TESTBALANCE', u'mnclose_testing')
    pipeline = load_pipeline('monthclose/pipelines/mnclose_testing.yml')
    restore_pipeline(pipeline)
    fill_defaults(pipeline)
    p = Pipeline(pipeline, ticket=ticket)
    for n in xrange(8):
        p.go()
    pass
