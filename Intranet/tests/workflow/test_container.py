# coding: utf-8

import mock
import pytest
import json
from collections import deque

from idm.core.workflow.sandbox.manager.group import GroupWrapper as IDMGroupWrapper
from idm.core.workflow.sandbox.container.context import UserWorkflowContext


from idm.core.workflow.sandbox.container.runner import Runner
from idm.core.workflow.sandbox.manager.context import UserWorkflowContext
from idm.core.workflow.sandbox.serializable import serialize
from django.utils.encoding import force_bytes


pytestmark = [pytest.mark.django_db]


class MockedConnection(object):
    def __init__(self, in_queue, out_queue):
        self.in_queue = in_queue
        self.out_queue = out_queue
        self.messages = []

    def get_data(self):
        return self.in_queue.popleft()

    def send_data(self, code, data):
        data = force_bytes(json.dumps(data))
        self.out_queue.append((code, data))
        self.messages.append((code, data))

    def connect(self, *args, **kwargs):
        pass


@pytest.mark.parametrize('valid_case', [True, False])
def test_username_validation(arda_users, simple_system, department_structure, valid_case):
    brackets = ('', '') if valid_case else ('[', ']')
    code = """
approvers = any_from({}groupify(105).get_all_responsibles(){} + ['sauron'], priority=1)
""".format(*brackets)
    context = UserWorkflowContext()
    role_data = {'role': 'manager'}
    requester = arda_users.frodo
    user = arda_users.frodo
    node = simple_system.nodes.get(slug='manager')
    ignore_approvers = False
    context.preprocess(role_data=role_data, system=simple_system, user=user, requester=requester, node=node,
                       ignore_approvers=ignore_approvers)
    input_data = serialize({
        'code': code,
        'context': context,
    })
    container_queue = deque([])
    group = IDMGroupWrapper(department_structure.fellowship)
    # Запишем заранее все нужные ответы от запускалки
    executor_queue = deque([
        (0, input_data),
        (10, serialize(group)),
        (10, serialize(group.get_all_responsibles()))
    ])
    fake_connection = MockedConnection(executor_queue, container_queue)
    with mock.patch('idm.core.workflow.sandbox.connection.Connection.send_data') as mocked_send_data:
        with mock.patch('idm.core.workflow.sandbox.connection.Connection.get_data') as mocked_get_data:
            mocked_send_data.side_effect = fake_connection.send_data
            mocked_get_data.side_effect = fake_connection.get_data
            runner = Runner('/socket.sock')
            runner.connection = fake_connection
            runner.wait_and_run_workflow()
    # Задаем два вопроса
    questions = [json.loads(message[1])['value']['name'] for message in container_queue if message[0] == 1]
    assert questions == ['groupify', 'get_all_responsibles']
    if valid_case:
        assert container_queue[2][0] == 0
        approvers = json.loads(container_queue[-1][1])['value']['approvers']
        assert approvers['type'] == 'AnyApprover'
        assert len(approvers['value']['approvers']) == 5
    else:
        assert container_queue[2][0] == 2
        response = json.loads(container_queue[2][1])
        assert response['type'] == 'WorkflowSyntaxError'
        assert response['value']['exception'] == "['Username must be a string.']"


def test_syntax_error(arda_users, simple_system, department_structure):
    code = 'approvers = [{(!!]})'
    context = UserWorkflowContext()
    role_data = {'role': 'manager'}
    requester = arda_users.frodo
    user = arda_users.frodo
    node = simple_system.nodes.get(slug='manager')
    ignore_approvers = False
    context.preprocess(role_data=role_data, system=simple_system, user=user, requester=requester, node=node,
                       ignore_approvers=ignore_approvers)
    input_data = serialize({
        'code': code,
        'context': context,
    })
    container_queue = deque([])
    executor_queue = deque([(0, input_data)])
    fake_connection = MockedConnection(executor_queue, container_queue)
    with mock.patch('idm.core.workflow.sandbox.connection.Connection.send_data') as mocked_send_data:
        with mock.patch('idm.core.workflow.sandbox.connection.Connection.get_data') as mocked_get_data:
            mocked_send_data.side_effect = fake_connection.send_data
            mocked_get_data.side_effect = fake_connection.get_data
            runner = Runner('/socket.sock')
            runner.connection = fake_connection
            runner.wait_and_run_workflow()
    status, response = container_queue[0]
    response = json.loads(response)
    assert status == 2
    assert response['type'] == 'WorkflowSyntaxError'
    assert 'code was compiled probably from here' in response['value']['exception']
    assert code in response['value']['exception']
