# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt
import collections

import pytest
import mock

from rep.core.kpi.parameters import TicketParameters, TicketWrapper

import billing.reports.tests.kpi.utils as test_utils


def log_item(login, dt_, **kwargs):
    return login, dt_, kwargs


@pytest.fixture
def issue(request):
    if isinstance(request.param, dict):
        inp_params = request.param
    elif isinstance(request.param, collections.Iterable):
        inp_params = dict(request.param)
    else:
        raise TypeError('wtf is that?')

    params = dict(
        key='KEY-123',
        status='open',
        type='good',
        priority=2,
        assignee=None,
        createdBy='pupkin',
        components=[1],
    )
    params.update(inp_params)
    key = params.get('key')
    params['queue'] = key.split('-')[0]

    return test_utils.create_issue(params.iteritems())


@pytest.mark.parametrize('issue', [dict(
    key='SATAN-666',
    components=[1, 2, 3],
    tags=['a', 'b', 'c'],
    createdAt='2017-01-15T12:13:14.666+0000',
    updatedAt='2017-02-14T11:56:04.006+0000',
    resolvedAt='2017-01-01T01:01:01.060+0000',
    deadline='2017-02-02',
    assignee='some_guy',
    createdBy='other_guy',
    status='open',
    type='PizzaOrder',
    priority=-7
)], indirect=True, ids=[''])
def test_basics(issue):
    parameters = TicketParameters(issue)

    assert parameters.get('key') == 'SATAN-666'
    assert parameters.get('components') == [1, 2, 3]
    assert parameters.get('tags') == ['a', 'b', 'c']
    assert parameters.get('create_dt') == dt.datetime(2017, 1, 15, 15, 13, 14, 666000)
    assert parameters.get('update_dt') == dt.datetime(2017, 2, 14, 14, 56, 04, 6000)
    assert parameters.get('resolve_dt') == dt.datetime(2017, 1, 1, 4, 1, 1, 60000)
    assert parameters.get('deadline') == dt.datetime(2017, 2, 2)
    assert parameters.get('assignee') == 'some_guy'
    assert parameters.get('author') == 'other_guy'
    assert parameters.get('status') == 'open'
    assert parameters.get('type') == 'PizzaOrder'
    assert parameters.get('priority_id') == -7


@pytest.mark.parametrize('issue', [dict(
    key='GOD-777',
    resolvedAt=None,
    deadline=None,
)], indirect=True, ids=[''])
def test_empty_dt(issue):
    parameters = TicketParameters(issue)

    assert parameters.get('key') == 'GOD-777'
    assert parameters.get('resolve_dt') is None
    assert parameters.get('deadline') is None


@pytest.mark.parametrize('issue', [dict(
    key='DUDE-111',
    assignee=None,
    tags=[],
    components=[],
    changelog=[
        log_item('me', '2017-01-15T12:13:14.666+0000', assignee=(None, 'me')),
        log_item('me', '2017-01-15T12:13:14.667+0000', createdBy=('me', None)),
    ]
)], indirect=True, ids=[''])
def test_empty_wrapped(issue):
    parameters = TicketParameters(issue)

    assert parameters.get('assignee') is None
    assert parameters.get('tags') == []
    assert parameters.get('components') == []

    change1, change2 = parameters.st_ticket.changelog
    assert change1.fields.assignee.from_ is None
    assert change1.fields.assignee.to.id == 'me'

    assert change2.fields.createdBy.from_.id == 'me'
    assert change2.fields.createdBy.to is None


def get_ids(tests):
    return [t_[0]['id'] for t_ in tests]


worktime_intervals_tests = [
    (
        dict(
            id='until_now',
            createdAt='2017-01-11T08:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[]
        ),
        (None, None),
        [
            (dt.datetime(2017, 1, 11, 11), dt.datetime(2017, 1, 11, 13, 13))
        ]
    ),
    (
        dict(
            id='from',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T14:00:00.000+0000', status=('open', 'resolved')),
            ]
        ),
        (dt.datetime(2017, 1, 10, 13), None),
        [
            (dt.datetime(2017, 1, 10, 13), dt.datetime(2017, 1, 10, 17, 0, 0)),
        ]
    ),
    (
        dict(
            id='to',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T14:00:00.000+0000', status=('open', 'resolved')),
            ]
        ),
        (None, dt.datetime(2017, 1, 10, 13)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 10, 13)),
        ]
    ),
    (
        dict(
            id='from_to',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T14:00:00.000+0000', status=('open', 'resolved')),
            ]
        ),
        (dt.datetime(2017, 1, 10, 12, 30, 31), dt.datetime(2017, 1, 10, 13, 14, 15)),
        [
            (dt.datetime(2017, 1, 10, 12, 30, 31), dt.datetime(2017, 1, 10, 13, 14, 15)),
        ]
    ),
    (
        dict(
            id='over_night',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[]
        ),
        (None, dt.datetime(2017, 1, 11, 10, 6, 6)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 11, 10, 6, 6)),
        ]
    ),
    (
        dict(
            id='resolved',
            createdAt='2017-01-10T08:00:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T13:06:06.000+0000', status=('open', 'resolved'))
            ]
        ),
        (None, None),
        [
            (dt.datetime(2017, 1, 10, 11), dt.datetime(2017, 1, 10, 16, 6, 6))
        ]
    ),
    (
        dict(
            id='reopened',
            createdAt='2017-01-11T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-11T08:06:06.000+0000', status=('open', 'resolved')),
                log_item('pupkin', '2017-01-11T08:30:00.000+0000', status=('resolved', 'open')),
            ]
        ),
        (None, None),
        [
            (dt.datetime(2017, 1, 11, 10), dt.datetime(2017, 1, 11, 11, 6, 6)),
            (dt.datetime(2017, 1, 11, 11, 30), dt.datetime(2017, 1, 11, 13, 13)),
        ]
    ),
    (
        dict(
            id='type',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T08:10:00.000+0000', type=('good', 'bad')),
                log_item('pupkin', '2017-01-10T10:01:02.000+0000', type=('bad', 'good')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 10, 11, 10)),
            (dt.datetime(2017, 1, 10, 13, 1, 2), dt.datetime(2018, 1, 1)),
        ]
    ),
    (
        dict(
            id='type_status',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T08:10:00.000+0000', type=('good', 'bad')),
                log_item('pupkin', '2017-01-10T08:30:00.000+0000', status=('open', 'needInfo')),
                log_item('pupkin', '2017-01-10T10:01:02.000+0000', status=('needInfo', 'open')),
                log_item('pupkin', '2017-01-10T10:30:06.000+0000', type=('bad', 'good')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 10, 11, 10)),
            (dt.datetime(2017, 1, 10, 13, 30, 6), dt.datetime(2018, 1, 1)),
        ]
    ),
    (
        dict(
            id='type_status_simultaneous',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T08:10:00.000+0000', type=('good', 'bad'), status=('open', 'needInfo')),
                log_item('pupkin', '2017-01-10T10:01:02.000+0000', status=('needInfo', 'open'), type=('bad', 'good')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 10, 11, 10)),
            (dt.datetime(2017, 1, 10, 13, 1, 2), dt.datetime(2018, 1, 1)),
        ]
    ),
    (
        dict(
            id='assignee',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('some_dick', '2017-01-10T09:00:00.000+0000', assignee=('pupkin', 'some_dick')),
                log_item('pupkin', '2017-01-10T10:30:00.000+0000', assignee=('some_dick', 'pupkin')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 10, 12)),
            (dt.datetime(2017, 1, 10, 13, 30), dt.datetime(2018, 1, 1)),
        ]
    ),
    (
        dict(
            id='empty_assignee',
            createdAt='2017-01-10T07:00:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('some_dick', '2017-01-10T09:06:06.000+0000', assignee=('pupkin', None)),
                log_item('pupkin', '2017-01-10T10:36:06.000+0000', assignee=(None, 'pupkin')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 10, 10), dt.datetime(2017, 1, 10, 12, 6, 6)),
            (dt.datetime(2017, 1, 10, 13, 36, 6), dt.datetime(2018, 1, 1)),
        ]
    ),
    (
        dict(
            id='queue',
            key='KEY-123',
            queue='KEY',
            createdAt='2017-01-11T08:30:00.000+0000',
            status='open',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('some_dick', '2017-01-11T09:00:00.000+0000', queue=('YKE', 'KEY')),
                log_item('pupkin', '2017-01-11T10:30:00.000+0000', queue=('KEY', 'EKY')),
                log_item('pupkin', '2017-01-11T12:00:06.000+0000', queue=('EKY', 'KEY')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 11, 12), dt.datetime(2017, 1, 11, 13, 30)),
            (dt.datetime(2017, 1, 11, 15, 0, 6), dt.datetime(2018, 1, 1)),
        ]
    ),
    (
        dict(
            id='priority',
            key='KEY-123',
            queue='KEY',
            createdAt='2017-01-11T08:35:00.000+0000',
            status='open',
            type='good',
            priority=1,
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-11T09:06:06.000+0000', priority=(3, 1)),
                log_item('pupkin', '2017-01-11T10:30:00.000+0000', priority=(1, 2)),
                log_item('pupkin', '2017-01-11T12:00:08.000+0000', priority=(2, 1)),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 11, 11, 35), dt.datetime(2017, 1, 11, 12, 6, 6)),
            (dt.datetime(2017, 1, 11, 13, 30), dt.datetime(2017, 1, 11, 15, 0, 8)),
        ]
    ),
    (
        dict(
            id='queue_assignee_status',
            key='KEY-123',
            queue='KEY',
            createdAt='2017-01-11T08:30:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('some_dick', '2017-01-11T09:00:00.000+0000',
                         queue=('KEY', 'YKE'), assignee=('pupkin', 'some_dick')),
                log_item('some_dick', '2017-01-11T10:00:00.000+0000', status=('open', 'closed')),
                log_item('pupkin', '2017-01-11T11:30:00.000+0000',
                         status=('closed', 'open'), queue=('YKE', 'KEY'), assignee=('some_dick', 'pupkin')),
                log_item('pupkin', '2017-01-11T12:00:06.000+0000', status=('open', 'resolved')),
            ]
        ),
        (None, dt.datetime(2018, 1, 1)),
        [
            (dt.datetime(2017, 1, 11, 11, 30), dt.datetime(2017, 1, 11, 12)),
            (dt.datetime(2017, 1, 11, 14, 30), dt.datetime(2017, 1, 11, 15, 0, 6)),
        ]
    ),
    (
        dict(
            id='invalid_start_type',
            createdAt='2017-01-10T08:00:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T10:00:00.000+0000', type=('bad', 'good')),
                log_item('pupkin', '2017-01-10T13:33:33.000+0000', status=('open', 'resolved'))
            ]
        ),
        (None, None),
        [
            (dt.datetime(2017, 1, 10, 13), dt.datetime(2017, 1, 10, 16, 33, 33))
        ]
    ),
    (
        dict(
            id='invalid_start_no_assignee',
            createdAt='2017-01-10T08:00:00.000+0000',
            status='resolved',
            type='good',
            assignee='pupkin',
            changelog=[
                log_item('pupkin', '2017-01-10T10:33:33.000+0000', assignee=(None, 'pupkin')),
                log_item('pupkin', '2017-01-10T13:33:33.000+0000', status=('open', 'resolved'))
            ]
        ),
        (None, None),
        [
            (dt.datetime(2017, 1, 10, 13, 33, 33), dt.datetime(2017, 1, 10, 16, 33, 33))
        ]
    ),
]


@pytest.mark.parametrize('issue, dt_interval, result', worktime_intervals_tests,
                         ids=get_ids(worktime_intervals_tests), indirect=['issue'])
def test_worktime_intervals(issue, dt_interval, result):
    wrap = TicketWrapper(issue)

    with mock.patch('datetime.datetime', wraps=dt.datetime, now=lambda: dt.datetime(2017, 1, 11, 13, 13)):
        intervals = list(wrap.get_open_intervals(*dt_interval))

    assert intervals == result


worktime_tests = [
    (dict(
        id='simple',
        createdAt='2017-01-10T06:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=2,
        changelog=[
            log_item('pupkin', '2017-01-10T08:00:00.000+0000', status=('open', 'resolved')),
        ]), (3600, [(1, 0)])
    ),
    (dict(
        id='over_night',
        createdAt='2017-01-10T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=2,
        changelog=[
            log_item('pupkin', '2017-01-11T08:00:00.000+0000', status=('open', 'resolved')),
        ]), (10 * 3600, [(1, 0)])
    ),
    (dict(
        id='weekend',
        createdAt='2017-01-05T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=2,
        changelog=[
            log_item('pupkin', '2017-01-07T08:00:00.000+0000', status=('open', 'resolved')),
        ]), (0, [(1, 0)])
    ),
    (dict(
        id='reopened',
        createdAt='2017-01-10T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=2,
        changelog=[
            log_item('pupkin', '2017-01-10T07:30:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-11T08:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-11T10:00:00.000+0000', status=('open', 'resolved')),
        ]), (9000, [(1, 0)])
    ),
    (dict(
        id='simple_overtime',
        createdAt='2017-01-10T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=3,
        changelog=[
            log_item('pupkin', '2017-01-10T08:30:00.000+0000', status=('open', 'resolved')),
        ]), (5400, [(1, 1800)])
    ),
    (dict(
        id='overtime_components',
        createdAt='2017-01-09T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1, 2],
        priority=2,
        changelog=[
            log_item('pupkin', '2017-01-12T09:00:00.000+0000', status=('open', 'resolved')),
        ]), ((3 * 9 + 2) * 3600, [(1, 2 * 3600), (2, (2 * 9 + 2) * 3600)])
    ),
    (dict(
        id='low_priority',
        createdAt='2017-01-09T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=1,
        changelog=[
            log_item('pupkin', '2017-01-12T09:00:00.000+0000', priority=(2, 1)),
            log_item('pupkin', '2017-01-19T11:00:00.000+0000', status=('open', 'resolved')),
        ]), ((3 * 9 + 2) * 3600, [(1, None)])
    ),
    (dict(
        id='other_queue',
        createdAt='2017-01-10T07:30:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=3,
        changelog=[
            log_item('some_dick', '2017-01-10T09:00:00.000+0000', priority=(1, 3)),
            log_item('some_dick', '2017-01-10T09:10:00.000+0000', queue=('SOME_STUFF', 'KEY')),
            log_item('some_dick', '2017-01-10T09:30:00.000+0000', assignee=('some_dick', 'pupkin')),
            log_item('pupkin', '2017-01-10T13:00:00.000+0000', status=('open', 'needInfo')),
            log_item('pupkin', '2017-01-10T15:00:00.000+0000', status=('needInfo', 'open')),
            log_item('pupkin', '2017-01-10T15:20:00.000+0000', status=('open', 'resolved')),
        ]),
     ((3 * 60 + 50) * 60, [(1, (2 * 60 + 50) * 60)])
    ),
    (dict(
        id='many_queues',
        key='SOME_STUFF-666',
        createdAt='2017-01-10T07:30:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=2,
        changelog=[
            log_item('some_dick', '2017-01-10T09:00:56.000+0000', queue=('KEY', 'SOME_STUFF')),
            log_item('some_dick', '2017-01-10T10:00:00.000+0000', queue=('SOME_STUFF', 'OTHER_STUFF')),
            log_item('some_dick', '2017-01-11T07:30:00.000+0000', queue=('OTHER_STUFF', 'KEY')),
            log_item('some_dick', '2017-01-11T15:00:10.000+0000', queue=('KEY', 'SOME_STUFF')),
            log_item('some_dick', '2017-01-12T07:06:06.666+0000', status=('open', 'resolved')),
        ]),
     (9 * 3600 + 66, [(1, 0)])
    ),
]


@pytest.mark.parametrize('issue, result', worktime_tests, ids=get_ids(worktime_tests), indirect=['issue'])
def test_worktime(issue, result):
    parameters = TicketParameters(issue)

    req_worktime, req_overtimes = result

    with mock.patch('datetime.datetime', wraps=dt.datetime, now=lambda: dt.datetime(2017, 1, 12, 12, 13)):
        worktime = parameters.get('worktime')

    assert worktime == req_worktime
    assert set(parameters.get('regulations_overtime')) == set(req_overtimes)


first_reaction_tests = [
    (dict(
        id='no_change',
        createdAt='2017-01-11T08:00:00.000+0000',
        resolvedAt=None,
        changelog=[]
    ), (None, None)),  # см. патч на dt.datetime.now
    (dict(
        id='resolved_by_other',
        createdAt='2017-01-10T07:00:00.000+0000',
        resolvedAt='2017-01-10T08:30:00.000+0000',
        changelog=[
            log_item('other', '2017-01-10T08:30:00.000+0000', status=('open', 'resolved'))
        ]
    ), (None, None)),
    (dict(
        id='forced_user',
        createdAt='2017-01-12T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('autodasha', '2017-01-12T14:00:03.000+0000'),
            log_item('autodasha', '2017-01-13T01:01:03.000+0000')
        ]
    ), (3603, dt.datetime(2017, 1, 12, 17, 0, 3))),
    (dict(
        id='real_user',
        createdAt='2017-01-12T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('pupkin', '2017-01-12T14:01:03.000+0000'),
            log_item('autodasha', '2017-01-13T01:01:03.000+0000')
        ]
    ), (3663, dt.datetime(2017, 1, 12, 17, 1, 3))),
    (dict(
        id='other_user',
        createdAt='2017-01-12T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('medved', '2017-01-12T14:00:00.000+0000'),
            log_item('pupkin', '2017-01-12T15:00:00.000+0000'),
        ]
    ), (7200, dt.datetime(2017, 1, 12, 18))),
    (dict(
        id='over_weekend',
        createdAt='2017-01-13T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('pupkin', '2017-01-16T14:06:06.000+0000'),
        ]
    ), ((10 * 60 + 6) * 60 + 6, dt.datetime(2017, 1, 16, 17, 6, 6))),
    (dict(
        id='only_alien_queue',
        key='ALIEN-666',
        createdAt='2017-01-10T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('pupkin', '2017-01-11T15:56:00.000+0000'),
        ]
    ), (None, None)),
    (dict(
        id='from_queue_alien',
        key='KEY-666',
        createdAt='2017-01-10T13:01:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('alien', '2017-01-11T12:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('autodasha', '2017-01-11T15:06:06.000+0000'),
        ]
    ), ((3 * 60 + 6) * 60 + 6, dt.datetime(2017, 1, 11, 18, 6, 6))),
    (dict(
        id='from_queue_self',
        key='KEY-666',
        createdAt='2017-01-10T13:02:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('pupkin', '2017-01-11T12:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('pupkin', '2017-01-11T15:56:00.000+0000'),
        ]
    ), ((3 * 60 + 56) * 60, dt.datetime(2017, 1, 11, 18, 56))),
    (dict(
        id='to_queue_alien_before_reaction',
        key='ALIEN-666',
        createdAt='2017-01-10T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('alien', '2017-01-11T12:00:00.000+0000', queue=('KEY', 'ALIEN')),
            log_item('pupkin', '2017-01-11T15:56:00.000+0000'),
        ]
    ), (None, None)),
    (dict(
        id='to_queue_alien_after_reaction',
        key='ALIEN-666',
        createdAt='2017-01-10T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('pupkin', '2017-01-11T12:00:01.000+0000'),
            log_item('alien', '2017-01-11T15:56:00.000+0000', queue=('KEY', 'ALIEN')),
        ]
    ), (8 * 3600 + 1, dt.datetime(2017, 1, 11, 15, 0, 1))),
    (dict(
        id='to_queue_self',
        key='ALIEN-666',
        createdAt='2017-01-10T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('pupkin', '2017-01-11T15:56:07.000+0000', queue=('KEY', 'ALIEN')),
        ]
    ), ((11 * 60 + 56) * 60 + 7, dt.datetime(2017, 1, 11, 18, 56, 7))),
    (dict(
        id='multiple_queues',
        key='KEY-666',
        createdAt='2017-01-10T13:00:00.000+0000',
        resolvedAt='2017-01-12T08:00:00.000+0000',
        assignee='some_dick',
        changelog=[
            log_item('alien', '2017-01-10T14:56:07.000+0000'),
            log_item('alien', '2017-01-11T06:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('alien', '2017-01-11T12:56:00.000+0000', queue=('KEY', 'ALIEN')),
            log_item('pupkin', '2017-01-11T14:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('autodasha', '2017-01-11T14:13:13.000+0000'),
            log_item('autodasha', '2017-01-12T08:00:00.000+0000', status=('open', 'closed')),
        ]
    ), ((6 * 60 + 9) * 60 + 13, dt.datetime(2017, 1, 11, 17, 13, 13))),
    (dict(
        id='after_resolved',
        createdAt='2017-01-12T13:00:00.000+0000',
        resolvedAt='2017-01-13T10:00:00.000+0000',
        changelog=[
            log_item('medved', '2017-01-13T10:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-13T11:00:00.000+0000'),
        ]
    ), (None, None)),
    (dict(
        id='reopened',
        createdAt='2017-01-12T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('medved', '2017-01-12T15:00:00.000+0000', status=('open', 'resolved')),
            log_item('medved', '2017-01-13T10:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-13T11:00:00.000+0000'),
        ]
    ), (3 * 3600, dt.datetime(2017, 1, 13, 14))),
    (dict(
        id='reaction_while_closed',
        createdAt='2017-01-12T13:00:00.000+0000',
        resolvedAt=None,
        changelog=[
            log_item('medved', '2017-01-12T15:15:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-12T15:30:00.000+0000'),
            log_item('medved', '2017-01-13T10:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-13T11:00:00.000+0000'),
        ]
    ), (2 * 3600 + 15 * 60, dt.datetime(2017, 1, 12, 18, 30))),
    (dict(
        id='other_queue_closed',
        createdAt='2017-01-12T13:00:00.000+0000',
        key='KEY-666',
        resolvedAt=None,
        changelog=[
            log_item('medved', '2017-01-12T15:15:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-12T15:30:00.000+0000'),
            log_item('medved', '2017-01-13T10:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-13T11:00:00.000+0000'),
            log_item('medved', '2017-01-13T12:00:00.000+0000', queue=('ALIEN', 'KEY')),
        ]
    ), (None, None)),
    (dict(
        id='late_component',
        key='KEY-666',
        createdAt='2017-01-10T13:00:00.000+0000',
        resolvedAt='2017-01-12T08:00:00.000+0000',
        assignee='some_dick',
        components=[1, 5, 7],
        changelog=[
            log_item('alien', '2017-01-10T14:56:07.000+0000'),
            log_item('alien', '2017-01-11T06:00:00.000+0000', components=([], [666])),
            log_item('alien', '2017-01-11T12:56:00.000+0000', components=([666], [])),
            log_item('pupkin', '2017-01-11T14:00:00.000+0000', components=([], [1, 5, 7, 666])),
            log_item('autodasha', '2017-01-11T14:13:13.000+0000'),
            log_item('autodasha', '2017-01-12T08:00:00.000+0000', status=('open', 'closed')),
        ]
    ), ((0 * 60 + 13) * 60 + 13, dt.datetime(2017, 1, 11, 17, 13, 13))),
]


@pytest.mark.parametrize('issue, result', first_reaction_tests, ids=get_ids(first_reaction_tests), indirect=['issue'])
def test_first_reaction(issue, result):
    req_time, req_dt = result

    parameters = TicketParameters(issue)
    with mock.patch('datetime.datetime', wraps=dt.datetime, now=lambda: dt.datetime(2017, 1, 11, 13, 13)):
        assert parameters.get('first_reaction_time') == req_time
        assert parameters.get('first_reaction_dt') == req_dt


first_resolve_tests = [
    (dict(
        id='not_resolved',
        createdAt='2017-01-18T09:00:00.000+0000',
        status='open',
        type='good',
        assignee='pupkin',
        components=[1],
        priority=1,
        changelog=[]),
     (None, None, 0)
    ),
    (dict(
        id='workday2workday-same',
        createdAt='2017-01-09T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-09T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (7200, dt.datetime(2017, 1, 9, 12), 1)
    ),
    (dict(
        id='workday2workday-not_same',
        createdAt='2017-01-09T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-10T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (11 * 3600, dt.datetime(2017, 1, 10, 12), 0)
    ),
    (dict(
        id='workday2workday-reopened',
        createdAt='2017-01-09T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-09T09:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-09T12:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-10T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (7200, dt.datetime(2017, 1, 9, 12), 1)
    ),
    (dict(
        id='workday_late2workday_late-before_weekend',
        createdAt='2017-01-13T16:01:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T17:00:00.000+0000', status=('open', 'resolved')),
        ]), (0, dt.datetime(2017, 1, 13, 20), 1)
    ),
    (dict(
        id='workday_late2weekend',
        createdAt='2017-01-13T16:01:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-14T12:00:00.000+0000', status=('open', 'resolved')),
        ]), (0, dt.datetime(2017, 1, 14, 15), 1)
    ),
    (dict(
        id='workday2workday_late-same',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T19:00:00.000+0000', status=('open', 'resolved')),
        ]), (9 * 3600, dt.datetime(2017, 1, 13, 22), 1)
    ),
    (dict(
        id='workday_early2workday-same',
        createdAt='2017-01-13T01:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'resolved')),
        ]), (3600, dt.datetime(2017, 1, 13, 11), 1)
    ),
    (dict(
        id='workday_early2workday-not_same',
        createdAt='2017-01-12T01:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'resolved')),
        ]), (10 * 3600, dt.datetime(2017, 1, 13, 11), 0)
    ),
    (dict(
        id='workday_late2workday-not_same',
        createdAt='2017-01-12T17:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T10:00:00.000+0000', status=('open', 'resolved')),
        ]), (3 * 3600, dt.datetime(2017, 1, 13, 13), 1)
    ),
    (dict(
        id='workday_late2workday-over_weekend',
        createdAt='2017-01-13T17:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-16T10:00:00.000+0000', status=('open', 'resolved')),
        ]), (3 * 3600, dt.datetime(2017, 1, 16, 13), 1)
    ),
    (dict(
        id='workday_late2workday_late-not_same',
        createdAt='2017-01-12T16:01:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T17:00:00.000+0000', status=('open', 'resolved')),
        ]), (9 * 3600, dt.datetime(2017, 1, 13, 20), 1)
    ),
    (dict(
        id='workday_late2workday_early',
        createdAt='2017-01-12T16:01:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-13T06:00:00.000+0000', status=('open', 'resolved')),
        ]), (0, dt.datetime(2017, 1, 13, 9), 1)
    ),
    (dict(
        id='workday2weekend',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-14T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (9 * 3600, dt.datetime(2017, 1, 14, 12), 1)
    ),
    (dict(
        id='weekend2weekend-reopened',
        createdAt='2017-01-05T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-06T09:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-09T12:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-10T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (0, dt.datetime(2017, 1, 6, 12), 1)
    ),
    (dict(
        id='weekend2workday',
        createdAt='2017-01-05T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-09T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (7200, dt.datetime(2017, 1, 9, 12), 1)
    ),
    (dict(
        id='weekend2workday-long',
        createdAt='2017-01-05T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-10T09:00:00.000+0000', status=('open', 'resolved')),
        ]), (11 * 3600, dt.datetime(2017, 1, 10, 12), 0)
    ),
    (dict(
        id='weekend2workday_early-long',
        createdAt='2017-01-05T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-10T05:00:00.000+0000', status=('open', 'resolved')),
        ]), (9 * 3600, dt.datetime(2017, 1, 10, 8), 0)
    ),
    (dict(
        id='weekend2workday-first_day',
        createdAt='2017-01-15T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-16T12:00:00.000+0000', status=('open', 'resolved')),
        ]), (5 * 3600, dt.datetime(2017, 1, 16, 15), 1)
    ),
    (dict(
        id='weekend_late2workay-first-day',
        createdAt='2017-01-15T17:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-16T12:00:00.000+0000', status=('open', 'resolved')),
        ]), (5 * 3600, dt.datetime(2017, 1, 16, 15), 1)
    ),
    (dict(
        id='weekend_late2workday-long',
        createdAt='2017-01-15T17:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-17T12:00:00.000+0000', status=('open', 'resolved')),
        ]), (14 * 3600, dt.datetime(2017, 1, 17, 15), 0)
    ),
    (dict(
        id='over_weekend',
        createdAt='2017-01-13T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-16T12:00:00.000+0000', status=('open', 'resolved')),
        ]), (9 * 3600, dt.datetime(2017, 1, 16, 15), 0)
    ),
    (dict(
        id='over_weekend_early',
        createdAt='2017-01-13T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-16T05:00:00.000+0000', status=('open', 'resolved')),
        ]), (4 * 3600, dt.datetime(2017, 1, 16, 8), 1)
    ),
    (dict(
        id='over_weekend_late',
        createdAt='2017-01-13T20:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-16T05:00:00.000+0000', status=('open', 'resolved')),
        ]), (0, dt.datetime(2017, 1, 16, 8), 1)
    ),
    (dict(
        id='weekend2weekend-over_week',
        createdAt='2017-01-15T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-21T13:00:00.000+0000', status=('open', 'resolved')),
        ]), (45 * 3600, dt.datetime(2017, 1, 21, 16), 0)
    ),
    (dict(
        id='from_other_queue-same_day',
        createdAt='2017-01-10T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('some_dick', '2017-01-13T13:00:00.000+0000', queue=('DICK_QUEUE', 'KEY')),
            log_item('pupkin', '2017-01-13T15:00:00.000+0000', status=('open', 'resolved')),
        ]), (2 * 3600, dt.datetime(2017, 1, 13, 18), 1)
    ),
    (dict(
        id='from_other_queue-long',
        createdAt='2017-01-10T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('some_dick', '2017-01-12T13:00:00.000+0000', queue=('DICK_QUEUE', 'KEY')),
            log_item('pupkin', '2017-01-13T14:00:00.000+0000', status=('open', 'resolved')),
        ]), (10 * 3600, dt.datetime(2017, 1, 13, 17), 0)
    ),
    (dict(
        id='reopened_from_other_queue',
        createdAt='2017-01-10T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('some_dick', '2017-01-11T13:00:00.000+0000', status=('open', 'resolved')),
            log_item('some_dick', '2017-01-13T13:00:00.000+0000',
                     queue=('DICK_QUEUE', 'KEY'), status=('resolved', 'open')),
            log_item('pupkin', '2017-01-13T14:27:00.000+0000', status=('open', 'resolved')),
        ]), (87 * 60, dt.datetime(2017, 1, 13, 17, 27), 1)
    ),
    (dict(
        id='resolved_other_queue',
        key='ALIEN-321',
        createdAt='2017-01-10T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-10T13:00:00.000+0000'),
            log_item('pupkin', '2017-01-11T07:15:00.000+0000', queue=('KEY', 'ALIEN')),
            log_item('some_dick', '2017-01-12T15:17:00.000+0000', status=('open', 'resolved')),
        ]), (None, None, 0)
    ),
    (dict(
        id='multiple_queues-same_day',
        key='KEY-321',
        createdAt='2017-01-10T10:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('some_dick', '2017-01-11T10:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('pupkin', '2017-01-11T12:00:00.000+0000', queue=('KEY', 'ALIEN')),
            log_item('some_dick', '2017-01-11T13:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('pupkin', '2017-01-11T13:30:00.000+0000', status=('open', 'resolved')),
        ]), (150 * 60, dt.datetime(2017, 1, 11, 16, 30), 1)
    ),
    (dict(
        id='multiple_queues-long',
        key='KEY-321',
        createdAt='2017-01-10T12:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-10T15:16:17.000+0000', queue=('KEY', 'ALIEN')),
            log_item('some_dick', '2017-01-13T10:00:00.000+0000', queue=('ALIEN', 'KEY')),
            log_item('pupkin', '2017-01-13T10:50:49.000+0000', status=('open', 'resolved')),
        ]), (3 * 3600 + 60 * 66 + 66, dt.datetime(2017, 1, 13, 13, 50, 49), 0)
    ),
]


@pytest.mark.parametrize('issue, result', first_resolve_tests, ids=get_ids(first_resolve_tests), indirect=['issue'])
def test_first_resolve(issue, result):
    parameters = TicketParameters(issue)

    req_time, req_dt, req_first_day = result

    with mock.patch('datetime.datetime', wraps=dt.datetime, now=lambda: dt.datetime(2017, 1, 18, 12, 13)):
        assert parameters.get('first_resolve_time') == req_time
        assert parameters.get('first_resolve_dt') == req_dt
        assert parameters.get('resolved_first_day') == req_first_day


was_reopened_tests = [
    (dict(
        id='no_changes',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='open',
        type='good',
        changelog=[
        ]), 0
    ),
    (dict(
        id='need_info',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='needInfo',
        type='good',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'needInfo')),
        ]), 0
    ),
    (dict(
        id='reopened',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='open',
        type='good',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-13T09:00:00.000+0000', status=('resolved', 'open')),
        ]), 1
    ),
    (dict(
        id='resolved',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='resolved',
        type='good',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'resolved')),
        ]), 0
    ),
    (dict(
        id='reopened_resolved',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='resolved',
        type='good',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-13T09:00:00.000+0000', status=('resolved', 'open')),
            log_item('pupkin', '2017-01-13T10:00:00.000+0000', status=('open', 'resolved')),
        ]), 1
    ),
    (dict(
        id='reopened_needinfo',
        createdAt='2017-01-13T07:00:00.000+0000',
        status='needInfo',
        type='good',
        changelog=[
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-13T09:00:00.000+0000', status=('resolved', 'needInfo')),
        ]), 1
    ),
    (dict(
        id='other_queue',
        key='KEY-123',
        createdAt='2017-01-12T07:00:00.000+0000',
        status='open',
        type='good',
        changelog=[
            log_item('someone', '2017-01-12T08:00:00.000+0000', status=('open', 'resolved')),
            log_item('someone', '2017-01-12T08:30:00.000+0000', status=('resolved', 'open')),
            log_item('someone', '2017-01-13T08:00:00.000+0000', queue=('ABYR', 'KEY')),
        ]), 0
    ),
    (dict(
        id='other_queue_moved',
        key='KEY-123',
        createdAt='2017-01-12T07:00:00.000+0000',
        status='open',
        type='good',
        changelog=[
            log_item('pupkin', '2017-01-12T08:00:00.000+0000', status=('open', 'resolved')),
            log_item('pupkin', '2017-01-13T08:00:00.000+0000', queue=('KEY', 'ABYR')),
            log_item('pupkin', '2017-01-13T09:00:00.000+0000', status=('resolved', 'open')),
            log_item('someone', '2017-01-13T10:00:00.000+0000', queue=('ABYR', 'KEY')),
        ]), 1
    ),
]


@pytest.mark.parametrize('issue, result', was_reopened_tests, ids=get_ids(was_reopened_tests), indirect=['issue'])
def test_was_reopened(issue, result):
    parameters = TicketParameters(issue)

    assert parameters.get('was_reopened') == result


deadline_tests = [
    (dict(
        id='no_deadline',
        createdAt='2017-01-11T07:00:00.000+0000',
        deadline=None,
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-11T08:00:00.000+0000', status=('open', 'resolved')),
        ]), None
    ),
    (dict(
        id='no_overtime',
        createdAt='2017-01-11T07:00:00.000+0000',
        deadline='2017-01-12',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-12T08:00:00.000+0000', status=('open', 'resolved')),
        ]), 0
    ),
    (dict(
        id='overtime',
        createdAt='2017-01-11T07:00:00.000+0000',
        deadline='2017-01-11',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-12T08:30:00.000+0000', status=('open', 'resolved')),
        ]), 5400
    ),
    (dict(
        id='not_resolved',
        createdAt='2017-01-17T08:00:00.000+0000',
        deadline='2017-01-17',
        status='open',
        type='good',
        assignee='pupkin',
        changelog=[]), 0
    ),
    (dict(
        id='overtime_early',
        createdAt='2017-01-11T07:00:00.000+0000',
        deadline='2017-01-11',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-12T06:00:00.000+0000', status=('open', 'resolved')),
        ]), 0
    ),
    (dict(
        id='overtime_weekend',
        createdAt='2017-01-13T07:00:00.000+0000',
        deadline='2017-01-13',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-15T12:00:00.000+0000', status=('open', 'resolved')),
        ]), 0
    ),
    (dict(
        id='overtime_needinfo',
        createdAt='2017-01-10T07:00:00.000+0000',
        deadline='2017-01-12',
        status='resolved',
        type='good',
        assignee='pupkin',
        changelog=[
            log_item('pupkin', '2017-01-12T12:00:00.000+0000', status=('open', 'needInfo')),
            log_item('pupkin', '2017-01-13T10:00:00.000+0000', status=('needInfo', 'open')),
            log_item('pupkin', '2017-01-13T12:01:32.000+0000', status=('open', 'resolved')),
        ]), 7292
    ),
]


@pytest.mark.parametrize('issue, result', deadline_tests, ids=get_ids(deadline_tests), indirect=['issue'])
def test_deadline_overtime(issue, result):
    parameters = TicketParameters(issue)

    with mock.patch('datetime.datetime', wraps=dt.datetime, now=lambda: dt.datetime(2017, 1, 17, 12, 13)):
        assert parameters.get('deadline_overtime') == result


is_external_tests = [
    (dict(
        id='internal',
        createdAt='2017-01-10T07:00:00.000+0000',
        resolvedAt='2017-01-20T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='pupkin'
    ), 0
    ),
    (dict(
        id='external',
        createdAt='2017-01-10T07:00:00.000+0000',
        resolvedAt='2017-01-20T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='shlupkin'
    ), 1),
    (dict(
        id='was_internal',
        createdAt='2017-01-10T07:00:00.000+0000',
        resolvedAt='2017-03-20T07:00:00.000+0000',
        status='resolved',
        type='good',
        assignee='dupkin'
    ), 1)
]


@pytest.mark.parametrize('issue, result', is_external_tests, ids=get_ids(is_external_tests), indirect=['issue'])
def test_is_external(issue, result):
    parameters = TicketParameters(issue)

    assert parameters.get('is_external') == result
