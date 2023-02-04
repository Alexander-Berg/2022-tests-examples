# -*- coding: utf-8 -*-

import mock

from autodasha.core.api.tracker import IssueTransitions, IssueStatuses, Issue


class TestCaseMeta(type):
    def __init__(cls, cl_name, bases, dct):
        type.__init__(cls, cl_name, bases, dct)

        repr_ = dct.get('_representation')
        base_cls = None
        for base_cls_ in reversed(cls.mro()):
            if getattr(base_cls_, '_cases', None) is not None:
                base_cls = base_cls_

        if base_cls and repr_ and repr_ != 'abstract':
            base_cls._cases.append(cls)


class AbstractMockTestCase(object):
    _default_lines = {}
    _representation = 'abstract'

    author = None
    last_resolved = None

    def _get_default_line(self, *args, **kwargs):
        assert args or kwargs
        if args:
            assert len(args) == 1
            k, = args
            return self._default_lines[k]

        if kwargs:
            assert len(kwargs) == 1
            (k, v), = kwargs.items()
            return self._default_lines[k] % v

    def get_data(self, mock_manager):
        raise NotImplementedError()

    def get_comments(self):
        return []

    def get_result(self):
        raise NotImplementedError()

    def __str__(self):
        return self._representation


class AbstractDBTestCase(object):
    _representation = 'abstract'

    summary = ''
    status = IssueStatuses.open
    resolution = None
    author = None
    issue_key = ''
    assignee = None
    issue_dt = None
    last_resolved = None
    followers = []
    type_id = None

    _description = ''

    def __str__(self):
        return self._representation

    def _get_data(self, session):
        raise NotImplementedError()

    def get_description(self, session):
        return self._description.format(**self._get_data(session))

    def setup_config(self, session, config):
        pass

    def get_comments(self):
        return []

    def get_result(self):
        raise NotImplementedError()

    def get_attachments(self):
        return []


class RequiredResult(object):
    def __init__(self, **kwargs):
        self.transition = IssueTransitions.none
        self.assignee = None
        self.comments = []
        self.type_id = None

        self.set_issue_changes(**kwargs)
        self.set_messages(**kwargs)
        self.set_object_states(**kwargs)

    def set_issue_changes(self, transition=IssueTransitions.fixed, assignee='autodasha', **kwargs):
        self.transition = transition
        self.assignee = assignee

    def set_messages(self, **kwargs):
        self.comments = kwargs.get('messages', [])

    def set_object_states(self, **kwargs):
        pass

    def add_message(self, message):
        self.comments.append(message)


def prepare_comment(comment):
    return set(map(unicode.strip, comment.strip().split('\n')))


def _fake_init(self, issue):
    self.st_issue = issue
    self.config = None


@mock.patch('autodasha.core.api.tracker.Issue.__init__', _fake_init)
def get_issue():
    cls_patcher = mock.patch.multiple(
        Issue,
        key=None,
        summary=None,
        description=None,
        status=None,
        resolution=None,
        dt=None,
        update_dt=None,
        followers=None,
        author=None,
        assignee=None,
        tags=None,
        comments=None,
        status_history=None,
        attachments=None,
        type_id=None,
        last_resolved=None,
        last_reopened=None,
        last_opened=None,
        available_transitions=None
    )
    cls_patcher.start()

    issue = Issue(None)
    issue.key = ''
    issue.summary = ''
    issue.description = ''
    issue.status = ''
    issue.resolution = None
    issue.dt = None
    issue.update_dt = None
    issue.followers = []
    issue.author = ''
    issue.assignee = None
    issue.tags = []
    issue.comments = []
    issue.status_history = []
    issue.attachments = []
    issue.last_opened = None
    issue.last_resolved = None
    issue.last_reopened = None

    return issue
