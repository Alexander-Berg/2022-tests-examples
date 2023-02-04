import uuid
import random
import collections
from unittest import mock
from typing import Optional
import multiprocessing.dummy

import pytest


@pytest.fixture
def multiprocessing_pool_mock():
    with mock.patch('multiprocessing.Pool', new=multiprocessing.dummy.Pool) as m:
        yield m


@pytest.fixture
def default_id() -> int:
    return random.randint(1, 99999)


@pytest.fixture
def default_uuid() -> str:
    return uuid.uuid4().hex


@pytest.fixture
def tracker_mock(default_id, default_uuid):
    TrackerIssue = collections.namedtuple(
        'TrackerIssue',
        (
            'self',
            'id',
            'key',
            'queue',
            'status',
            'priority',
            'summary',
            'description',
            'createdBy',
            'assignee',
            'followers',
            'components',
            'tags',
        ),
    )
    TrackerComment = collections.namedtuple('TrackerComment', 'self,id,longId,createdBy,text,summonees')
    TrackerUser = collections.namedtuple('TrackerUser', 'login')
    TrackerObject = collections.namedtuple('TrackerObject', 'key')
    TrackerComponent = collections.namedtuple('TrackerComponent', 'name')

    def add_comment(
        text,
        summonees: Optional[list[str]] = None,
        attachments: Optional[list[str]] = None,
    ):
        return TrackerComment(
            self='',
            id=default_id,
            longId=default_uuid,
            text=text,
            createdBy=TrackerUser(login='somebody'),
            summonees=summonees and list(map(TrackerUser, summonees)),
        )

    def add_issue(
        queue: str,
        summary: str,
        description: Optional[str] = None,
        assignee: Optional[str] = None,
        components: Optional[list[str]] = None,
        tags: Optional[list[str]] = None,
        attachments: Optional[list[str]] = None,
        key: Optional[str] = None,
    ):
        return TrackerIssue(
            self='',
            id=default_uuid,
            key=key or f'{queue}-7',
            queue=TrackerObject(queue),
            status=TrackerObject('open'),
            priority=TrackerObject('normal'),
            summary=summary,
            description=description or '',
            createdBy=TrackerUser('somebody'),
            assignee=TrackerUser(assignee),
            followers=(),
            components=components and list(map(TrackerComponent, components)),
            tags=tags,
        )

    def get_issue(key):
        issue = add_issue(
            key=key,
            queue=key.split('-')[0],
            summary='any summary',
            description='any description',
            assignee='any-assignee',
        )
        return issue

    with mock.patch('startrek_client.Startrek') as m:
        m().issues[None].comments.create.side_effect = add_comment
        m().issues.create.side_effect = add_issue
        m().issues.get.side_effect = get_issue
        yield m
