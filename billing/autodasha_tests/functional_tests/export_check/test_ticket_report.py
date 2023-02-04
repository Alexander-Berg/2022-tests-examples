# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock
import pytest

from autodasha.core.api.tracker import IssueTransitions
from autodasha.tools.check_export.reporter import BaseTicketReporter
from balance import muzzle_util as ut
from tests.autodasha_tests.functional_tests.export_check.db_check_utils import (
    get_act, get_invoice, get_client, get_person, get_collateral, get_contract, export_object
)


@pytest.fixture
def config():
    return ut.Struct(responsible_manager='mscnad7')


@pytest.fixture
def tracker():
    return mock.MagicMock()


@pytest.fixture
def mock_group():
    return ut.Struct(issue=ut.Struct(key='666'))


@pytest.fixture
def queue_object(request, session, queue_object):
    for type_ in request.param:
        if type_ == 'Invoice':
            export_object(queue_object, get_invoice(session))
        elif type_ == 'Act':
            export_object(queue_object, get_act(session))
        elif type_ == 'Person':
            export_object(queue_object, get_person(session))
        elif type_ == 'Client':
            export_object(queue_object, get_client(session), type_='CLIENT_BATCH')
        elif type_ == 'Contract':
            contract = get_contract(session)
            export_object(queue_object, contract)
            export_object(queue_object, contract.col0)
        elif type_ == 'ContractCollateral':
            col = get_collateral(session)
            export_object(queue_object, col)
            export_object(queue_object, col.contract)
            export_object(queue_object, col.contract.col0)
        else:
            raise ValueError('wtf is that?')

    return queue_object


@pytest.fixture
def reporter(queue_object, config, tracker):
    return BaseTicketReporter.get_reporter(queue_object.type, config, tracker)


def set_issue(reporter, issue):
    reporter.tracker.configure_mock(get_issue=lambda _: issue)


def get_mock_reporter(group, config, tracker):
    types = {obj.type for obj in group.objects}
    if len(types) == 1:
        group_type, = types
    else:
        group_type = None
    group.type = group_type
    return BaseTicketReporter.get_reporter(group_type, config, tracker)


def get_report_info(issue):
    calls = issue.method_calls
    assert len(calls) == 1
    (name, args, kwargs), = calls
    assert name == 'commit_changes'
    assert len(args) == 1
    return args[0]


@mock.patch('autodasha.core.api.tracker.Issue', lambda x: x)
class TestTicketReporter(object):

    def test_good_default(self, mock_group, config, tracker):
        issue = mock.MagicMock(assignee='autodasha', status='waiting')
        mock_group.objects = [mock.MagicMock(type='OEBS')]

        reporter = get_mock_reporter(mock_group, config, tracker)
        set_issue(reporter, issue)

        reporter.report(mock_group, None)

        ri = get_report_info(issue)
        assert ri.comment == 'Все объекты успешно выгружены в ОЕБС.'
        assert ri.transition == IssueTransitions.fixed
        assert ri.assignee is None
        assert not ri.summonees

    def test_good_client_batch(self, mock_group, config, tracker):
        issue = mock.MagicMock(assignee='autodasha', status='waiting', author='solnyshkom')
        mock_group.objects = [mock.MagicMock(type='CLIENT_BATCH')]

        reporter = get_mock_reporter(mock_group, config, tracker)
        set_issue(reporter, issue)

        reporter.report(mock_group, None)

        ri = get_report_info(issue)
        assert ri.comment == 'Расчёт успешно выполнен.'
        assert ri.transition == IssueTransitions.none
        assert ri.assignee == 'solnyshkom'
        assert not ri.summonees

    def test_good_mixed(self, mock_group, config, tracker):
        issue = mock.MagicMock(assignee='autodasha', status='waiting')
        mock_group.objects = [mock.MagicMock(type='CLIENT_BATCH'), mock.MagicMock(type='OEBS')]

        reporter = get_mock_reporter(mock_group, config, tracker)
        set_issue(reporter, issue)

        reporter.report(mock_group, None)

        ri = get_report_info(issue)
        assert ri.comment == 'Все объекты успешно обработаны.'
        assert ri.transition == IssueTransitions.fixed
        assert ri.assignee is None
        assert not ri.summonees

    def test_good_assignee(self, mock_group, config, tracker):
        issue = mock.MagicMock(assignee='mscnad7', status='waiting')
        mock_group.objects = [mock.MagicMock(type='OEBS')]

        reporter = get_mock_reporter(mock_group, config, tracker)
        set_issue(reporter, issue)

        reporter.report(mock_group, [])

        ri = get_report_info(issue)
        assert ri.comment == 'Все объекты успешно выгружены в ОЕБС.'
        assert ri.transition == IssueTransitions.none
        assert ri.assignee is None
        assert not ri.summonees

    def test_good_status(self, mock_group, config, tracker):
        issue = mock.MagicMock(assignee='autodasha', status='need_info')
        mock_group.objects = [mock.MagicMock(type='OEBS')]

        reporter = get_mock_reporter(mock_group, config, tracker)
        set_issue(reporter, issue)

        reporter.report(mock_group, None)

        ri = get_report_info(issue)
        assert ri.comment == 'Все объекты успешно выгружены в ОЕБС.'
        assert ri.transition == IssueTransitions.fixed
        assert ri.assignee is None
        assert not ri.summonees

    @pytest.mark.parametrize('queue_object', [(['Invoice', 'Act', 'Person', 'Client'])],
                             indirect=True, ids=lambda args: ','.join(args))
    def test_bad_default(self, queue_object, reporter):
        issue = mock.MagicMock(assignee='autodasha', status='waiting')
        set_issue(reporter, issue)

        act, client, invoice, person = sorted(queue_object.proxies, key=lambda p: p.classname)
        errors = [
            (act, 'Act'),
            (invoice, 'Invoice'),
            (person, 'Person'),
            (client, 'Completed dropping table bo.t_act. Oh wait, shi~')
        ]
        reporter.report(queue_object, errors)

        report_rows = [
            'Акт %s (счёт %s) не выгрузился в ОЕБС.' % (act.object.external_id, act.object.invoice.external_id),
            'Счёт %s не выгрузился в ОЕБС.' % invoice.object.external_id,
            'Плательщик %s (ID %s) не выгрузился в ОЕБС.' % (person.object.name, person.object.id),
            'Не выполнился CLIENT_BATCH ID %s: Completed dropping table bo.t_act. Oh wait, shi~.' % client.object_id,
        ]

        ri = get_report_info(issue)
        assert ri.comment.split('\n') == report_rows
        assert ri.transition == IssueTransitions.none
        assert ri.assignee == 'mscnad7'
        assert not ri.summonees

    @pytest.mark.parametrize('queue_object', [(['Contract'])], indirect=True, ids=lambda args: ','.join(args))
    def test_bad_default_contract(self, queue_object, reporter):
        issue = mock.MagicMock(assignee='autodasha', status='waiting')
        set_issue(reporter, issue)

        contract, _ = sorted(queue_object.proxies, key=lambda p: p.classname == 'Contract', reverse=True)
        errors = [(contract, 'AAAAAAAAA')]
        reporter.report(queue_object, errors)

        report = 'Договор %s не выгрузился в ОЕБС.' % contract.object.external_id

        ri = get_report_info(issue)
        assert ri.comment == report
        assert ri.transition == IssueTransitions.none
        assert ri.assignee == 'mscnad7'
        assert not ri.summonees

    @pytest.mark.parametrize('queue_object', [(['Contract'])], indirect=True, ids=lambda args: ','.join(args))
    def test_bad_default_col0(self, queue_object, reporter):
        issue = mock.MagicMock(assignee='autodasha', status='waiting')
        set_issue(reporter, issue)

        contract, col0 = sorted(queue_object.proxies, key=lambda p: p.classname == 'Contract', reverse=True)
        errors = [(col0, 'AAAAAAAAA')]
        reporter.report(queue_object, errors)

        report = 'Нулевое ДС (договор %s) не выгружено в ОЕБС.' % contract.object.external_id

        ri = get_report_info(issue)
        assert ri.comment == report
        assert ri.transition == IssueTransitions.none
        assert ri.assignee == 'mscnad7'
        assert not ri.summonees

    @pytest.mark.parametrize('queue_object', [(['ContractCollateral'])], indirect=True, ids=lambda args: ','.join(args))
    def test_bad_default_col(self, queue_object, reporter):
        issue = mock.MagicMock(assignee='autodasha', status='waiting')
        set_issue(reporter, issue)

        cols = filter(lambda p: p.classname == 'ContractCollateral', queue_object.proxies)
        col, col0 = sorted(cols, key=lambda p: p.object.collateral_type_id is None)

        errors = [(col, 'AAAAAAAAA')]
        reporter.report(queue_object, errors)

        report = 'ДС №%s (договор %s) не выгружено в ОЕБС.' % (col.object.num, col.object.contract.external_id)

        ri = get_report_info(issue)
        assert ri.comment == report
        assert ri.transition == IssueTransitions.none
        assert ri.assignee == 'mscnad7'
        assert not ri.summonees

    @pytest.mark.parametrize('queue_object', [(['Invoice'])], indirect=True, ids=lambda args: ','.join(args))
    def test_bad_status(self, queue_object, reporter):
        issue = mock.MagicMock(assignee='autodasha', status='open')
        set_issue(reporter, issue)

        invoice, = queue_object.proxies
        errors = [(invoice, 'Invoice')]
        reporter.report(queue_object, errors)

        ri = get_report_info(issue)
        assert ri.comment == 'Счёт %s не выгрузился в ОЕБС.' % invoice.object.external_id
        assert ri.transition == IssueTransitions.none
        assert ri.assignee == 'mscnad7'
        assert not ri.summonees

    @pytest.mark.parametrize('queue_object', [(['Invoice'])], indirect=True, ids=lambda args: ','.join(args))
    def test_bad_assignee(self, queue_object, reporter):
        issue = mock.MagicMock(assignee='some_dick', status='waiting')
        set_issue(reporter, issue)

        invoice, = queue_object.proxies
        errors = [(invoice, 'Invoice')]
        reporter.report(queue_object, errors)

        ri = get_report_info(issue)
        assert ri.comment == 'Счёт %s не выгрузился в ОЕБС.' % invoice.object.external_id
        assert ri.transition == IssueTransitions.none
        assert ri.assignee is None
        assert ri.summonees == ['mscnad7']
