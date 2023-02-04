# -*- coding: utf-8 -*-

from __future__ import with_statement

import datetime
import json
import xmlrpclib
from functools import partial

import mock
import pytest
import hamcrest

from balance.actions.nirvana.task import process_nirvana_task_item
from balance.queue_processor import process_object
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_SERVICE_ID
)
from balance import exc, mapper
from tests import (
    object_builder as ob,
    tutils as tut
)


@pytest.fixture(name='task')
def create_task(session):
    task_type = ob.NirvanaTaskTypeBuilder.construct(session)
    return ob.NirvanaTaskBuilder.construct(
        session,
        task_type=task_type,
        run_id=session.now().isoformat(),
        metadata=None
    )


def create_task_item(task):
    ti = ob.NirvanaTaskItemBuilder.construct(
        task.session,
        task=task,
        metadata='some meatadata',
        processed=1
    )

    ob.ExportBuilder.construct(
        task.session,
        state=0,
        rate=666,
        type='NIRVANA_TASK_ITEM',
        classname='NirvanaTaskItem',
        object_id=ti.id,
        output='some_exprot_error'
    )
    return ti


@pytest.mark.parametrize(
    'status',
    [
        'IN PROGRESS',
        'PARTITIAL SUCCESS',
        'SUCCESS',
        'FAILED'
    ]
)
def test_check_task_status(xmlrpcserver, session, task, status):
    task_items = []
    for i in range(6):
        ti = create_task_item(task)
        task_items.append(ti)
        if status != 'IN PROGRESS':
            ex = ti.exports['NIRVANA_TASK_ITEM']
            ex.state = 1

    if status == 'PARTITIAL SUCCESS':
        for idx, ti in enumerate(task_items):
            if idx % 2 == 0:
                ti.output = {
                    'success_report': 'hello_%s' % idx
                }
            else:
                ti.output = {
                    'fail_report': 'world_%s' % idx
                }
    elif status == 'SUCCESS':
        for idx, ti in enumerate(task_items):
            ti.output = {
                'success_report': 'hello_%s' % idx
            }
    elif status == 'FAILED':
        for idx, ti in enumerate(task_items):
            ti.output = {
                'fail_report': 'world_%s' % idx
            }

    report = xmlrpcserver.CheckTaskStatus(task.id)

    hamcrest.assert_that(
        report,
        hamcrest.has_entries(
            status=status
        )
    )

    expected_success_report = []
    expected_fail_report = []

    if status == 'PARTITIAL SUCCESS':
        expected_success_report = []
        expected_fail_report = []
        for idx in range(len(task_items)):
            if idx % 2 == 0:
                expected_success_report.append({
                        'input_data': 'some meatadata',
                        'result': 'hello_%s' % idx
                    }
                )
            else:
                expected_fail_report.append(
                    {
                        'input_data': 'some meatadata',
                        'result': 'world_%s' % idx
                    }
                )
        hamcrest.assert_that(
            sorted(report['success_report']),
            hamcrest.equal_to(
                sorted(expected_success_report)
            )
        )
        hamcrest.assert_that(
            sorted(report['failed_report']),
            hamcrest.equal_to(
                sorted(expected_fail_report)
            )
        )

    if status == 'SUCCESS':
        for idx in range(len(task_items)):
            expected_success_report.append({
                    'input_data': 'some meatadata',
                    'result': 'hello_%s' % idx
                }
            )
        hamcrest.assert_that(
            sorted(report['success_report']),
            hamcrest.equal_to(
                sorted(expected_success_report)
            )
        )

    if status == 'FAILED':
        for idx in range(len(task_items)):
            expected_fail_report.append({
                    'input_data': 'some meatadata',
                    'result': 'world_%s' % idx
                }
            )
        hamcrest.assert_that(
            sorted(report['failed_report']),
            hamcrest.equal_to(
                sorted(expected_fail_report)
            )
        )


def test_not_found(xmlrpcserver):
    wrong_task_id = 666
    with pytest.raises(xmlrpclib.Fault) as exc_info:
        xmlrpcserver.CheckTaskStatus(wrong_task_id)

    hamcrest.assert_that(
        tut.get_exception_code(exc_info.value, 'msg'),
        hamcrest.equal_to(
            'Object not found: %s' % wrong_task_id
        )
    )

