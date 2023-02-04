# -*- coding: utf-8 -*-
import datetime

import pytest
from nirvana_api import ExecutionResult, ExecutionStatus

from balance import mncloselib, mapper
from cluster_tools.nirvana_execute import execute_workflow_instance
from tests import object_builder as ob


@pytest.fixture
def args():
    class Args:
        def __init__(self):
            self.workflow_instance_id = 'workflow-instance-to-be-cloned'
            self.sleep_seconds = 0
            self.mnclose_task = 'mnclose_task'
            self.global_parameters = ['first_param=123', 'second_param=345']
            self.nirvana_server = 'test.nirvana.yandex-team.ru'

    return Args()


def create_nirvana_cloned_workflow(session):
    return ob.NirvanaClonedWorkflowBuilder().build(session).obj


def query_nirvana_cloned_workflow(session, original_id, mnclose_task):
    current_dt = datetime.datetime.now()
    current_month = datetime.datetime(current_dt.year, current_dt.month, 1)
    return session.query(mapper.NirvanaClonedWorkflow). \
        filter(mapper.NirvanaClonedWorkflow.original_id == original_id). \
        filter(mapper.NirvanaClonedWorkflow.mnclose_task == mnclose_task). \
        filter(mapper.NirvanaClonedWorkflow.dt == current_month).first()


def test_execute_workflow_instance_new(nirvana_api_mock, session, args):
    """Ещё не клонировали workflow instance, должны склонировать, записать в базу, настроить параметры, запустить,
    подождать успеха и закрыть.
    """
    cloned_instance_id = 'cloned-instance-id'

    nirvana_api_mock.mock_clone(cloned_instance_id)
    nirvana_api_mock.mock_state({'result': ExecutionResult.success, 'status': ExecutionStatus.completed})

    result = execute_workflow_instance(session, args)

    # Должны были склонировать WorkflowInstance
    nirvana_api_mock.clone_workflow_instance.assert_called_once()

    # Должны были добавить новый NirvanaClonedWorkflow
    nirvana_cloned_workflow = query_nirvana_cloned_workflow(session, args.workflow_instance_id, args.mnclose_task)
    assert nirvana_cloned_workflow.instance_id == cloned_instance_id

    # Должны были установить глобальные параметры
    nirvana_api_mock.set_global_parameters.assert_called_once()

    # Должны были запустить Workflow
    nirvana_api_mock.start_workflow.assert_called_once()

    # Должны были закрыть задачу в MnClose
    assert isinstance(result, mncloselib.ReturnActionResolve)


def test_execute_workflow_instance_already_cloned_but_not_started(nirvana_api_mock, session, args):
    """Уже склонировали workflow instance, но не запустили, должны настроить параметры,
    запустить, подождать успеха и закрыть.
    """
    already_cloned_instance_id = create_nirvana_cloned_workflow(session).instance_id

    execution_state = {'result': ExecutionResult.undefined, 'status': ExecutionStatus.undefined}
    nirvana_api_mock.mock_state(execution_state)

    result = execute_workflow_instance(session, args)

    # Не должны были клонировать WorkflowInstance
    nirvana_api_mock.clone_workflow_instance.assert_not_called()

    # Не должны были добавлять новый NirvanaClonedWorkflow
    nirvana_cloned_workflow = query_nirvana_cloned_workflow(session, args.workflow_instance_id, args.mnclose_task)
    assert nirvana_cloned_workflow.instance_id == already_cloned_instance_id

    # Должны были установить глобальные параметры
    nirvana_api_mock.set_global_parameters.assert_called_once()

    # Должны были запустить Workflow
    nirvana_api_mock.start_workflow.assert_called_once()

    # Должны были закрыть задачу в MnClose
    assert isinstance(result, mncloselib.ReturnActionResolve)


def test_execute_workflow_instance_already_cloned_and_started(nirvana_api_mock, session, args):
    """Уже склонировали workflow instance и запустили, должны подождать успеха и закрыть.
    """
    already_cloned_instance_id = create_nirvana_cloned_workflow(session).instance_id

    execution_state = {'result': ExecutionResult.undefined, 'status': ExecutionStatus.running}
    nirvana_api_mock.mock_state(execution_state)

    result = execute_workflow_instance(session, args)

    # Не должны были клонировать WorkflowInstance
    nirvana_api_mock.clone_workflow_instance.assert_not_called()

    # Не должны были добавлять новый NirvanaClonedWorkflow
    nirvana_cloned_workflow = query_nirvana_cloned_workflow(session, args.workflow_instance_id, args.mnclose_task)
    assert nirvana_cloned_workflow.instance_id == already_cloned_instance_id

    # Не должны были установливать глобальные параметры
    nirvana_api_mock.set_global_parameters.assert_not_called()

    # Не должны были запускать Workflow
    nirvana_api_mock.start_workflow.assert_not_called()

    # Должны были закрыть задачу в MnClose
    assert isinstance(result, mncloselib.ReturnActionResolve)


def test_execute_workflow_instance_already_failed(nirvana_api_mock, session, args):
    """Уже склонировали workflow instance и запустили, но он упал.
    Должны склонировать заново, перезаписать запись в бд, выставить параметры, запустить, подождать успеха и закрыть.
    """
    create_nirvana_cloned_workflow(session)

    execution_state = {'result': ExecutionResult.failure, 'status': ExecutionStatus.completed}
    nirvana_api_mock.mock_state(execution_state)

    new_cloned_instance_id = 'new-cloned-instance-id'
    nirvana_api_mock.mock_clone(new_cloned_instance_id)

    result = execute_workflow_instance(session, args)

    # Должны были склонировать WorkflowInstance
    nirvana_api_mock.clone_workflow_instance.assert_called_once()

    # Должны были перезаписать NirvanaClonedWorkflow
    nirvana_cloned_workflow = query_nirvana_cloned_workflow(session, args.workflow_instance_id, args.mnclose_task)
    assert nirvana_cloned_workflow.instance_id == new_cloned_instance_id

    # Должны были установить глобальные параметры
    nirvana_api_mock.set_global_parameters.assert_called_once()

    # Должны были запустить Workflow
    nirvana_api_mock.start_workflow.assert_called_once()

    # Должны были закрыть задачу в MnClose
    assert isinstance(result, mncloselib.ReturnActionResolve)


def test_execute_workflow_instance_running_and_failed(nirvana_api_mock, session, args):
    """Уже склонировали workflow instance и запустили, но в процессе ожидание он упал. Должны приостановить задачу.
    """
    already_cloned_instance_id = create_nirvana_cloned_workflow(session).instance_id

    execution_state = {'result': ExecutionResult.undefined, 'status': ExecutionStatus.running}
    nirvana_api_mock.mock_state(execution_state, success=False)

    result = execute_workflow_instance(session, args)

    # Не должны были клонировать WorkflowInstance
    nirvana_api_mock.clone_workflow_instance.assert_not_called()

    # Не должны были добавлять новый NirvanaClonedWorkflow
    nirvana_cloned_workflow = query_nirvana_cloned_workflow(session, args.workflow_instance_id, args.mnclose_task)
    assert nirvana_cloned_workflow.instance_id == already_cloned_instance_id

    # Не должны были установливать глобальные параметры
    nirvana_api_mock.set_global_parameters.assert_not_called()

    # Не должны были запускать Workflow
    nirvana_api_mock.start_workflow.assert_not_called()

    # Должны были приостановить задачу в MnClose
    assert isinstance(result, mncloselib.ReturnActionStall)


def test_execute_workflow_instance_already_exist_with_different_task(nirvana_api_mock, session, args):
    """Уже есть такой workflow instance, но с другим mnclose таском.
    Должны склонировать, записать в базу, настроить параметры, запустить, подождать успеха и закрыть.
    """
    create_nirvana_cloned_workflow(session)

    cloned_instance_id = 'freshly-cloned-instance-id'

    nirvana_api_mock.mock_clone(cloned_instance_id)
    nirvana_api_mock.mock_state({'result': ExecutionResult.success, 'status': ExecutionStatus.completed})

    args.mnclose_task = 'another_mnclose_task'
    result = execute_workflow_instance(session, args)

    # Должны были склонировать WorkflowInstance
    nirvana_api_mock.clone_workflow_instance.assert_called_once()

    # Должны были добавить новый NirvanaClonedWorkflow
    nirvana_cloned_workflow = query_nirvana_cloned_workflow(session, args.workflow_instance_id, args.mnclose_task)
    assert nirvana_cloned_workflow.instance_id == cloned_instance_id

    # Должны были установить глобальные параметры
    nirvana_api_mock.set_global_parameters.assert_called_once()

    # Должны были запустить Workflow
    nirvana_api_mock.start_workflow.assert_called_once()

    # Должны были закрыть задачу в MnClose
    assert isinstance(result, mncloselib.ReturnActionResolve)
