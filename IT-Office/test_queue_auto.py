import datetime
import time
import pytest
from source.feedback import FeedbackClass, FEEDBACK_CONFIG
from source.queue_auto.queue_auto import (_is_ticket_appl_for_close,
                        UserSideRobot)
from startrek_client import Startrek
from source.tests.fixtures import (FakeFabric,
                            FakeFabricIterator)
from source.tests.testdata import (USER_SIDE_AUTOMATIC_TESTDATA)

class FakeStatus():
    def __init__(self, **kwargs):
        self.fake_id = kwargs.get('id')

    @property
    def id(self):
        return self.fake_id

class FakeComment():
    def __init__(self, **kwargs):
        self.faked_created_by = kwargs.get('created_by')
        self.faked_text = kwargs.get('text')

    @property
    def text(self):
        return self.faked_text

    @property
    def createdBy(self):
        return self.faked_created_by

class FakeChangeOneLog():
    def __init__(self, **kwargs):
        self.kwargs = kwargs

    @property
    def fields(self):
        return self.kwargs.get('fields')

    @property
    def updatedAt(self):
        return self.kwargs.get('updatedAt')

class FakeIssue():
    def __init__(self, **kwargs):
        self.kwargs=kwargs
        self.fake_key = kwargs.get('key')
        self.fake_log = kwargs.get('changelog')
        self.fake_comments = kwargs.get('comments', [])
        self.fake_created_by = kwargs.get('createdBy')

    @property
    def key(self):
        return self.fake_key

    @property
    def changelog(self):
        return self.fake_log

    @property
    def comments(self):
        return self.fake_comments

    @property
    def createdBy(self):
        return self.fake_created_by

    def __str__(self):
        return self.fake_key

    def __repr__(self):
        return self.fake_key

class MockIssues():
    def __init__(self):
        self.fake_issues = []

    def add_fake_issues(self, **kwargs):
        self.fake_issues = kwargs.get('issues_list', [])

    def create(self, **kwargs):
        return FakeIssue(**kwargs)

    def find(self, queue=None, **kwargs):
        return self.fake_issues

@pytest.fixture()
def mock_st(monkeypatch):
    def mockreturn(self, **kwargs):
        self.issues = MockIssues()
        return None

    monkeypatch.setattr(Startrek, '__init__', mockreturn)

@pytest.fixture()
def mock_sleep(monkeypatch):
    def mockreturn(self, queue=None):
        return

    monkeypatch.setattr(time, 'sleep', mockreturn)

def test_ticket_to_close(mock_st):
    changelog = [
        FakeChangeOneLog(
        fields =
             [{'field': FakeStatus(id='status'), 'from': FakeStatus(id='6'),
               'to': FakeStatus(id='2')}],
         updatedAt = "2019-01-14T13:32:45.783+0000")
    ]

    issue = FakeIssue(changelog=changelog)
    assert _is_ticket_appl_for_close(issue) == True

def test_ticket_to_early_close(mock_st):
    today = datetime.datetime.now().strftime('%Y-%m-%d')
    update_today = "{}T13:32:45.783+0000".format(today)

    changelog = [
        FakeChangeOneLog(
        fields =
             [{'field': FakeStatus(id='status'), 'from': FakeStatus(id='6'),
               'to': FakeStatus(id='2')}],
         updatedAt = update_today)
    ]

    issue = FakeIssue(changelog=changelog)
    assert _is_ticket_appl_for_close(issue) == False

good_comment = FakeComment(
        created_by=FakeStatus(id='robot-help'),
        text="""Коллеги, пожалуйста, заполните форму обратной связи по решению этого тикета, это займет меньше минуты: https://hd.yandex-team.ru/feedback/?t=HDRFS-1
Если есть какие-то вещи, которые хочется обсудить, вы всегда можете написать/позвонить/дойти до укого:meidji или укого:ekaritskiy.
Спасибо.""")

good_comment2 = FakeComment(
        created_by=FakeStatus(id='robot-help'),
        text="""Коллеги, пожалуйста, заполните форму обратной связи по решению этого тикета, это займет меньше минуты: https://hd.yandex-team.ru/feedback/?t=HDRFS-2
Если есть какие-то вещи, которые хочется обсудить, вы всегда можете написать/позвонить/дойти до укого:agrebenyuk или укого:chist-anton.
Спасибо.""")

good_comment3 = FakeComment(
        created_by=FakeStatus(id='robot-help'),
        text="""Коллеги, пожалуйста, заполните форму обратной связи по решению этого тикета, это займет меньше минуты: https://hd.yandex-team.ru/feedback/?t=HDRFS-2
Если есть какие-то вещи, которые хочется обсудить, вы всегда можете написать/позвонить/дойти до укого:meidji.
Спасибо.""")

bad_comment = FakeComment(
        created_by=FakeStatus(id='robot-help'),
        text='Коллеги, пожалуйста, заполните форму обратной связи по'
    )

good_issue = FakeIssue(
    key='HDRFS-1',
    comments=[good_comment]
)
good_issue2 = FakeIssue(
    key='HDRFS-2',
    comments=[good_comment2]
)
good_issue3 = FakeIssue(
    key='HDRFS-2',
    comments=[good_comment3]
)
bad_issue = FakeIssue(
    key='HDRFS-3',
    comments=[bad_comment]
)

test_data = [
    ({"issues": [good_issue, good_issue2]},
     {"result": set()}),
    ({"issues": [good_issue, bad_issue]},
     {"result": {bad_issue}}),
    ({"issues": [bad_issue, good_issue2]},
     {"result": {bad_issue}}),
(   {"issues": [good_issue3, good_issue2]},
     {"result": set()}),
]

@pytest.mark.parametrize("input_data, ex_result", test_data)
def test_feedback_last_comment(mock_st, input_data, ex_result):
    worker = FeedbackClass(queue='HDRFS',
                           add_filter=None,
                           text=FEEDBACK_CONFIG['HDRFS']['text'],
                           old_texts=FEEDBACK_CONFIG['HDRFS']['old_texts'],
                           token='')

    worker.client.issues.add_fake_issues(issues_list=input_data["issues"])
    result = worker.collect_issues()
    assert result == ex_result["result"]

@pytest.mark.parametrize("input_data, ex_result", USER_SIDE_AUTOMATIC_TESTDATA)
def test_on_user_side(mock_st, mock_sleep, input_data, ex_result):
    robot = UserSideRobot()

    issue = FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id=input_data['author']),
        changelog = input_data['changelog'],
        comments = input_data['comments'],
        transitions = input_data.get("transitions"),
        fixVersions = input_data.get("fixVersions")
    )
    robot._process_issue(issue)
    assert issue.comments.created_dict == ex_result.get('comment')
    if input_data.get("transitions"):
        for transition in list(issue.transitions.transition_list.keys()):
            assert issue.transitions[transition].created_dict == ex_result.get('transition')