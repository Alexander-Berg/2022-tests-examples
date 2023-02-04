# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime
import itertools

import pytest
from sqlalchemy import orm

from autodasha.db import mapper as a_mapper
from autodasha.solver_cl import ChangeContract
from tests.autodasha_tests.functional_tests import case_utils

from tests.autodasha_tests.functional_tests.solvers.change_contract.common import func_test_patches

# модули с тесткейсами для инициализации метакласса
import cases


@func_test_patches
def do_test(session, issue_data, monkeypatch=None):
    queue_object, st_issue, case = issue_data

    required_res = case.get_result()

    if hasattr(case, 'current_date'):
        monkeypatch.setattr(
            'autodasha.solver_cl.change_contract.approve.AbstractImplApproveRequest._get_today',
            lambda s: case.current_date
        )

    if monkeypatch:
        current_date = getattr(case, 'current_date', datetime.datetime.today())
        issue_dt = getattr(case, 'issue_dt', datetime.datetime.today())
        monkeypatch.setattr(
            'autodasha.solver_cl.change_contract.approve.AbstractImplApproveRequest._is_outdated',
            lambda _, __: current_date - issue_dt > datetime.timedelta(3)
        )

    solver = ChangeContract(queue_object, st_issue)
    res = solver.solve()

    if required_res is None:
        assert res.commit is False
        assert res.delay is False
        assert res.issue_report is None
        return

    #assert res.commit is True
    assert res.delay == required_res.delay

    report = res.issue_report

    if not report.comments:
        assert not required_res.comments
    else:
        comments = '\n'.join(cmt.text or '' for cmt in report.comments)
        assert set(required_res.comments) == case_utils.prepare_comment(comments)
    assert required_res.transition == report.transition
    assert required_res.assignee == report.assignee
    summonees = set(itertools.chain.from_iterable(cmt.summonees or [] for cmt in report.comments))
    assert required_res.summonees == (summonees or None)

    for attr_name, attr_val in required_res.state:
        if attr_name in ('external_id', ):
            assert getattr(required_res.contract, attr_name) == attr_val, attr_name
        elif attr_name == 'is_cancelled':
            assert bool(getattr(required_res.col, attr_name)) == bool(attr_val), attr_name
        else:
            assert getattr(required_res.col, attr_name) == attr_val, attr_name

    try:
        export_queue = session.query(a_mapper.QueueObject). \
            filter(a_mapper.QueueObject.issue == queue_object.issue,
                   a_mapper.QueueObject.processor == 'EXPORT_CHECK'). \
            one()
    except orm.exc.NoResultFound:
        assert not (required_res.c_exports or required_res.col_exports)
    else:
        c_exports = {obj.object_id for obj in export_queue.proxies if obj.classname == 'Contract'}
        col_exports = {obj.object_id for obj in export_queue.proxies if obj.classname == 'ContractCollateral'}
        assert c_exports | col_exports == {obj.object_id for obj in export_queue.proxies}

        assert c_exports == set(required_res.c_exports)
        assert col_exports == set(required_res.col_exports)

    return required_res


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractChangeDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_change(session, issue_data):
    do_test(session, issue_data)


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractChangeWRulesDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_change_w_rules(session, issue_data):
    do_test(session, issue_data)


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractCheckDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_check(session, issue_data):
    do_test(session, issue_data)


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractPADBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_pa(session, issue_data):
    required_res = do_test(session, issue_data)

    assert required_res.pa_state == [(pa_, pa_.hidden) for pa_, _ in required_res.pa_state]
    if required_res.cn_state:
        assert required_res.cn_state == [(cn_, cn_.hidden) for cn_, _ in required_res.cn_state]


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractApproveDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_approve(session, issue_data, monkeypatch):
    do_test(session, issue_data, monkeypatch)


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractMultipleContractsDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_multi(session, issue_data):
    do_test(session, issue_data)


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in cases.AbstractAfterChecksDBTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test_db_after_checks(session, issue_data):
    do_test(session, issue_data)
