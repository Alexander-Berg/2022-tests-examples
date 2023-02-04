# -*- coding: utf-8 -*-
import datetime

from mock import patch

from balance import mapper
from balance.actions.nirvana.operations.populate_nirvana_mnclose_sync import populate_nirvana_mnclose_sync
from tests import object_builder as ob


def create_nirvana_cloned_workflow(session):
    return ob.NirvanaClonedWorkflowBuilder().build(session).obj


def create_block_parameters(task_id, month):
    return {
        'parameters': [
            {'parameter': 'mnclose_task', 'value': task_id},
            {'parameter': 'month', 'value': month},
        ]
    }


def create_not_mnclose_block_parameters():
    return {
        'parameters': [
            {'parameter': 'some_parameter', 'value': 1},
            {'parameter': 'some_parameter2', 'value': 2},
        ]
    }


MONTH_FORMAT = '%m.%y'


def task_exists(session, task_id, dt):
    return session.query(mapper.NirvanaMnCloseSync) \
        .filter(mapper.NirvanaMnCloseSync.task_id == task_id) \
        .filter(mapper.NirvanaMnCloseSync.dt == dt) \
        .exists()


@patch('nirvana_api.api.NirvanaApi.__new__')
def test_populate_nirvana_mnclose_sync_with_noexisting(nirvana_api_new, session):
    nirvana_api_instance_mock = nirvana_api_new.return_value

    first_task_id = 'adfox_acts'
    second_task_id = 'mnclose_task_id_2'
    month = '08.19'
    dt = datetime.datetime.strptime(month, MONTH_FORMAT)

    nirvana_api_instance_mock.get_block_parameters.return_value = [
        create_block_parameters(first_task_id, month),
        create_block_parameters(second_task_id, month),
    ]

    nirvana_server = 'nirvana_server'
    workflow_instance_id = 'workflow-instance-id'

    populate_nirvana_mnclose_sync(session, nirvana_server, workflow_instance_id)

    first_task_exists = task_exists(session, first_task_id, dt)
    second_task_exists = task_exists(session, second_task_id, dt)

    assert first_task_exists and second_task_exists


@patch('nirvana_api.api.NirvanaApi.__new__')
def test_populate_nirvana_mnclose_sync_with_existing(nirvana_api_new, session):
    nirvana_api_instance_mock = nirvana_api_new.return_value

    existing_task = ob.NirvanaMnCloseSyncBuilder().build(session).obj

    first_task_id = 'adfox_acts'
    second_task_id = 'mnclose_task_id_2'
    existing_task_id = existing_task.task_id

    dt = existing_task.dt
    month = dt.strftime(MONTH_FORMAT)

    nirvana_api_instance_mock.get_block_parameters.return_value = [
        create_block_parameters(first_task_id, month),
        create_block_parameters(second_task_id, month),
        create_block_parameters(existing_task_id, month),
    ]

    nirvana_server = 'nirvana_server'
    workflow_instance_id = 'workflow-instance-id'

    populate_nirvana_mnclose_sync(session, nirvana_server, workflow_instance_id)

    first_task_exists = task_exists(session, first_task_id, dt)
    second_task_exists = task_exists(session, second_task_id, dt)

    assert first_task_exists and second_task_exists


@patch('nirvana_api.api.NirvanaApi.__new__')
def test_populate_nirvana_mnclose_sync_ignores_other_blocks(nirvana_api_new, session):
    nirvana_api_instance_mock = nirvana_api_new.return_value

    first_task_id = 'adfox_acts'
    second_task_id = 'mnclose_task_id_2'
    month = '08.19'
    dt = datetime.datetime.strptime(month, MONTH_FORMAT)

    nirvana_api_instance_mock.get_block_parameters.return_value = [
        create_block_parameters(first_task_id, month),
        create_block_parameters(second_task_id, month),
        create_not_mnclose_block_parameters(),
    ]

    nirvana_server = 'nirvana_server'
    workflow_instance_id = 'workflow-instance-id'

    populate_nirvana_mnclose_sync(session, nirvana_server, workflow_instance_id)

    first_task_exists = task_exists(session, first_task_id, dt)
    second_task_exists = task_exists(session, second_task_id, dt)

    assert first_task_exists and second_task_exists

