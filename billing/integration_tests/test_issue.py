# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt
import time
import StringIO

import pytest

from autodasha.core.config import Config
from autodasha.core.api.tracker import IssueStatuses, IssueResolutions, IssueReport


@pytest.fixture
def issue(st_client, session, run_tag):
    Config(session)
    Config.STARTREK_NEW_COMMIT_SCHEME = True
    create_dt = dt.datetime.now()
    res = st_client.issues.create(
        queue='PAYSUP',
        summary='Тест атрибутов Автодаши',
        description='Тест атрибутов Автодаши',
        tags=[run_tag],
        components=['balance-support'],
        assignee='mscnad7',
        followers=['robot-balancereport'],
        type=56
    )
    res.create_dt = create_dt
    time.sleep(5)
    return res


def test_base_properties(issue, tracker, run_tag):
    update_dt = dt.datetime.now()
    issue.transitions.get('ps_resolve').execute(
        resolution=4,
        comment={'text': 'Роботы всего саппорта баланса объединяйтесь'}
    )

    time.sleep(5)
    wrap = tracker.get_issue(issue.key)
    assert wrap.id == issue.id
    assert wrap.key == issue.key
    assert wrap.summary == 'Тест атрибутов Автодаши'
    assert wrap.description == 'Тест атрибутов Автодаши'
    assert set(wrap.tags) == {run_tag}
    assert wrap.status == IssueStatuses.resolved
    assert wrap.resolution == IssueResolutions.duplicate
    assert wrap.author == 'autodasha'
    assert wrap.assignee == 'mscnad7'
    assert update_dt > wrap.dt >= issue.create_dt
    assert wrap.update_dt >= update_dt
    assert wrap.type_id == 56
    assert set(wrap.followers) >= {'robot-balancereport', 'autodasha'}

    comment, = wrap.comments
    assert comment['dt'] >= update_dt
    assert comment['author'] == 'autodasha'
    assert comment['text'] == 'Роботы всего саппорта баланса объединяйтесь'


def test_transitions(st_client, tracker, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        status=IssueStatuses.closed,
        resolution=IssueResolutions.fixed,
        type_=54
    )
    ri.append_comment('test0')
    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == IssueStatuses.closed
    assert check_issue.resolution.id == str(IssueResolutions.fixed)
    assert check_issue.type.id == '54'
    assert list(check_issue.comments)[0].text == 'test0'
    check_issue.transitions.get('reopen').execute()
    time.sleep(5)

    wrap = tracker.get_issue(issue.key)
    ri.status = IssueStatuses.closed
    ri.resolution = IssueResolutions.wont_fix
    ri.type = 71
    ri.comments = []
    ri.append_comment('test1')
    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == IssueStatuses.closed
    assert check_issue.resolution.id == str(IssueResolutions.wont_fix)
    assert check_issue.type.id == '71'
    assert list(check_issue.comments)[1].text == 'test1'
    check_issue.transitions.get('reopen').execute()
    time.sleep(5)

    wrap = tracker.get_issue(issue.key)
    ri.status = IssueStatuses.closed
    ri.resolution = IssueResolutions.duplicate
    ri.comments = []
    ri.append_comment('test2')
    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == IssueStatuses.closed
    assert check_issue.resolution.id == str(IssueResolutions.duplicate)
    assert list(check_issue.comments)[2].text == 'test2'
    check_issue.transitions.get('reopen').execute()
    time.sleep(5)

    wrap = tracker.get_issue(issue.key)
    ri.status = IssueStatuses.need_info
    ri.resolution = None
    ri.type = 54
    ri.comments = []
    ri.append_comment('test3')
    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == IssueStatuses.need_info
    assert not getattr(check_issue, 'resolution', None)
    assert check_issue.type.id == '54'
    assert list(check_issue.comments)[3].text == 'test3'
    check_issue.transitions.get('provide_info').execute()
    time.sleep(5)

    wrap = tracker.get_issue(issue.key)
    ri.status = IssueStatuses.need_info
    ri.resolution = IssueResolutions.fixed
    ri.comments = []
    ri.append_comment('test4')
    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == IssueStatuses.need_info
    assert check_issue.resolution.id == str(IssueResolutions.fixed)
    assert list(check_issue.comments)[4].text == 'test4'


def test_status_history(issue, tracker):
    resolve_dt = dt.datetime.now()
    issue.transitions.get('ps_resolve').execute(resolution=4)
    time.sleep(5)

    open_dt = dt.datetime.now()
    issue.transitions.get('reopen').execute()

    time.sleep(5)
    wrap = tracker.get_issue(issue.key)

    _, trans_resolve, trans_reopen = wrap.status_history

    assert open_dt > trans_resolve[0] >= resolve_dt
    assert trans_resolve[1] == IssueStatuses.new
    assert trans_resolve[2] == IssueStatuses.resolved

    assert trans_reopen[0] >= open_dt
    assert trans_reopen[1] == IssueStatuses.resolved
    assert trans_reopen[2] == IssueStatuses.new

    assert open_dt > wrap.last_resolved >= resolve_dt
    assert wrap.last_reopened >= open_dt
    assert wrap.last_opened == wrap.last_reopened


def test_last_reopened_empty(issue, tracker):
    wrap = tracker.get_issue(issue.key)
    assert wrap.last_reopened is None
    assert wrap.last_opened >= issue.create_dt


def test_report_attributes_comment(st_client, tracker, issue, run_tag):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee='robot-octopool',
        followers=['lightrevan'],
        tags=['new-tag'],
        comment='Превед',
        summonees=['robot-octopool']
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.assignee.id == 'robot-octopool'
    assert {f.id for f in check_issue.followers} >= {'robot-balancereport', 'autodasha', 'lightrevan'}
    assert set(check_issue.tags) == {run_tag, 'new-tag', 'auto-dev'}

    comment, = check_issue.comments
    assert comment.createdBy.id == 'autodasha'
    assert comment.text == 'Превед'
    assert {s.id for s in comment.summonees} == {'robot-octopool'}


def test_report_attributes_double_comment(st_client, tracker, issue, run_tag):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee='robot-octopool',
        followers=['lightrevan'],
        tags=['new-tag'],
        comment='Превед',
        summonees=['robot-octopool']
    )
    ri.append_comment('Медвед', comment_id=1)
    ri.summon('autodasha', comment_id=1)

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.assignee.id == 'robot-octopool'
    assert {f.id for f in check_issue.followers} >= {'robot-balancereport', 'autodasha', 'lightrevan'}
    assert set(check_issue.tags) == {run_tag, 'new-tag', 'auto-dev'}

    comment1, comment2 = check_issue.comments
    assert comment1.createdBy.id == 'autodasha'
    assert comment1.text == 'Превед'
    assert {s.id for s in comment1.summonees} == {'robot-octopool'}

    assert comment2.createdBy.id == 'autodasha'
    assert comment2.text == 'Медвед'
    assert {s.id for s in comment2.summonees} == {'autodasha'}


def test_report_triple_comment(st_client, tracker, issue, run_tag):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee=None,
        comment='Превед',
        summonees=['robot-octopool']
    )
    ri.append_comment('Медвед', comment_id=1)
    ri.summon('autodasha', comment_id=1)
    ri.append_comment('Превед шизофрения!', comment_id=2)
    ri.summon('robot-balancereport', comment_id=2)
    ri.summon('autodasha', comment_id=2)

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]

    comment1, comment2, comment3 = check_issue.comments
    assert comment1.createdBy.id == 'autodasha'
    assert comment1.text == 'Превед'
    assert {s.id for s in comment1.summonees} == {'robot-octopool'}

    assert comment2.createdBy.id == 'autodasha'
    assert comment2.text == 'Медвед'
    assert {s.id for s in comment2.summonees} == {'autodasha'}

    assert comment3.createdBy.id == 'autodasha'
    assert comment3.text == 'Превед шизофрения!'
    assert {s.id for s in comment3.summonees} == {'autodasha', 'robot-balancereport'}


def test_report_attributes_wo_assignee(st_client, tracker, issue, run_tag):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee=None,
        followers=['lightrevan'],
        tags=['new-tag'],
        comment='Превед',
        summonees=['robot-octopool']
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.assignee.id == 'mscnad7'
    assert {f.id for f in check_issue.followers} >= {'robot-balancereport', 'autodasha', 'lightrevan'}
    assert set(check_issue.tags) == {run_tag, 'new-tag', 'auto-dev'}

    comment, = check_issue.comments
    assert comment.createdBy.id == 'autodasha'
    assert comment.text == 'Превед'
    assert {s.id for s in comment.summonees} == {'robot-octopool'}


def test_report_attributes_wo_comment(st_client, tracker, issue, run_tag):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee='robot-octopool',
        followers=['lightrevan'],
        tags=['new-tag']
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.assignee.id == 'robot-octopool'
    assert {f.id for f in check_issue.followers} >= {'robot-balancereport', 'lightrevan'}
    assert set(check_issue.tags) == {run_tag, 'new-tag', 'auto-dev'}

    assert not list(check_issue.comments)


def test_summon_wo_comment(st_client, tracker, issue, run_tag):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        summonees=['robot-octopool']
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.assignee.id == 'autodasha'
    assert {f.id for f in check_issue.followers} >= {'robot-balancereport', 'autodasha'}

    comment, = check_issue.comments
    assert comment.text == ''
    assert {s.id for s in comment.summonees} == {'robot-octopool'}


def test_report_status(st_client, tracker, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        status=IssueStatuses.need_info
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == 'needInfo'
    assert check_issue.assignee.id == 'autodasha'


def test_report_status_resolution(st_client, tracker, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        status=IssueStatuses.resolved,
        resolution=IssueResolutions.duplicate
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.status.key == 'resolved'
    assert check_issue.resolution.id == '4'
    assert check_issue.assignee.id == 'autodasha'


def test_report_type(st_client, tracker, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        type_=54
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.type.id == '54'
    assert check_issue.assignee.id == 'autodasha'

    assert not list(check_issue.comments)


def test_report_type_comment(st_client, tracker, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        type_=54,
        comment='А у нас в квартире газ!'
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.type.id == '54'
    assert check_issue.assignee.id == 'autodasha'

    comment, = check_issue.comments
    assert comment.createdBy.id == 'autodasha'
    assert comment.text == 'А у нас в квартире газ!'


def test_report_status_type_comment(st_client, tracker, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        type_=54,
        status=IssueStatuses.resolved,
        resolution=IssueResolutions.duplicate,
        comment='А у нас в квартире газ!'
    )

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.type.id == '54'
    assert check_issue.status.key == 'resolved'
    assert check_issue.resolution.id == '4'
    assert check_issue.assignee.id == 'autodasha'

    comment, = check_issue.comments
    assert comment.createdBy.id == 'autodasha'
    assert comment.text == 'А у нас в квартире газ!'


def test_report_status_type_double_comment(st_client, tracker, app, issue):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        type_=54,
        status=IssueStatuses.resolved,
        resolution=IssueResolutions.duplicate,
        comment='А у нас в квартире газ!',
        summonees='robot-octopool'
    )
    ri.append_comment('Медвед', comment_id=1)
    ri.summon('autodasha', comment_id=1)

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]
    assert check_issue.type.id == '54'
    assert check_issue.status.key == 'resolved'
    assert check_issue.resolution.id == '4'
    assert check_issue.assignee.id == 'autodasha'

    comment1, comment2 = check_issue.comments
    assert comment1.createdBy.id == 'autodasha'
    assert comment1.text == 'А у нас в квартире газ!'
    assert {s.id for s in comment1.summonees} == {'robot-octopool'}

    assert comment2.createdBy.id == 'autodasha'
    assert comment2.text == 'Медвед'
    assert {s.id for s in comment2.summonees} == {'autodasha'}


# Тесты аттачей.
# Текущая версия REST-API стартрека не отдает список аттачей,
# приложенных к комментарию, можно получить только весь список аттачей в тикете.

@pytest.fixture()
def attachment_file(request):

    def generate():
        f = StringIO.StringIO('Я - текст в файле. А ты - нет.')
        return f
    return generate


def test_attachment_wrong_type(attachment_file):
    ri = IssueReport(
        assignee=None,
        comment='',
        summonees=['robot-octopool']
    )
    with pytest.raises(ValueError) as excinfo:
        ri.append_comment('Аттач добавляется в единственный коммент',
                          attachments=123)
    assert excinfo.value.message == 'Attachment should be StringIO.StringIO ' \
                                         'object or collection of such objects'

    with pytest.raises(ValueError) as excinfo:
        ri.append_comment('Аттач добавляется в единственный коммент',
                          attachments=[attachment_file(),
                                       'А я не StringIO и горжусь этим!'])
    assert excinfo.value.message == 'Every attachment should be ' \
                                    'StringIO.StringIO object'

    ri.append_comment('Просто коммент без атаача')
    with pytest.raises(ValueError) as excinfo:
        ri.append_comment('Аттач добавляется во второй коммент',
                          comment_id=1,
                          attachments='/Я/путь/к/аттачу/а/так/же/юникодная/строка')
    assert excinfo.value.message == 'Adding attachment by path to a file is not supported'


def test_attachment_in_first_comment_with_status_change(st_client, tracker, issue, attachment_file):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee=None,
        comment='Я коммент и буду жить в этом тикете',
        summonees=['robot-octopool'],
        status=IssueStatuses.resolved,
        resolution=IssueResolutions.duplicate,
    )
    ri.append_comment('Аттач добавляется в единственный коммент', attachments=attachment_file())

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]

    comment, = check_issue.comments
    attachments = list(check_issue.attachments)

    assert check_issue.status.key == 'resolved'
    assert check_issue.resolution.id == '4'
    assert comment.text == 'Я коммент и буду жить в этом тикете\n' \
                           'Аттач добавляется в единственный коммент'
    assert len(attachments) == 1


def test_attachment_in_several_comments_with_cyrillic_filename(st_client, tracker, issue, attachment_file):
    wrap = tracker.get_issue(issue.key)
    ri = IssueReport(
        assignee=None,
        comment='Я первый коммент без аттача',
        summonees=['robot-octopool']
    )
    attach = attachment_file()
    attach.name = 'Attach_second_comment.txt'
    ri.append_comment('Аттач добавляется во второй коммент',
                      comment_id=1,
                      attachments=attach)
    attach1, attach2 = attachment_file(), attachment_file()
    attach1.name = 'Ёё.txt'

    ri.append_comment('2 аттача (с кириллицей и без имени) добавляются во третий коммент',
                      comment_id=2,
                      attachments=[attach1, attach2])

    wrap.commit_changes(ri)
    time.sleep(5)

    check_issue = st_client.issues[issue.key]

    comment1, comment2, comment3 = check_issue.comments
    attachments = list(check_issue.attachments)

    assert comment1.text == 'Я первый коммент без аттача'
    assert comment2.text == 'Аттач добавляется во второй коммент'
    assert comment3.text == '2 аттача (с кириллицей и без имени) добавляются во третий коммент'
    assert len(attachments) == 3

    attachment_names = {attach.name for attach in attachments}
    assert attachment_names == {'Attach_second_comment.txt', 'YOyo.txt', 'file'}
