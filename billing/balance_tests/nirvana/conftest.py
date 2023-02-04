# -*- coding: utf-8 -*-

import copy
import uuid

import mock
import pytest

from nirvana_api import ExecutionResult, ExecutionStatus

from tests import object_builder as ob


@pytest.fixture()
def nirvana_block(session):
    rv = ob.NirvanaBlockBuilder.construct(
        session,
        request={
            'context': {
                'changeStatusCallbackURL': 'http://some_url/',
            },
            'operation': {
                'logs': {},
            },
            'data': {
                'log': {
                    'source': 'some_source',
                    'uri': 'some_uri',
                },
            }
        },

    )
    return rv


class NirvanaApiMock(object):
    """
    Небольшая обертка над MagickMock для упрощения тестирования взаимодействия Нирваной
    """

    def __init__(self, origin):
        self.mock_origin = origin
        # Сохраняем ответ от вызова NirvanaApi()
        self.mock = self.mock_origin.return_value

    def __getattr__(self, item):
        return getattr(self.mock, item)

    @property
    def mock_instance_id(self):
        """
        Возвращает случайный instance_id
        """
        return str(uuid.uuid4())

    def mock_clone(self, instance_id=None):
        if instance_id is None:
            instance_id = self.mock_instance_id

        self.mock.clone_workflow_instance.return_value = instance_id
        return instance_id

    def mock_state(self, execution_state=None, success=True):
        if execution_state is None:
            execution_state = {'result': ExecutionResult.success, 'status': ExecutionStatus.completed}

        result = ExecutionResult.success if success else ExecutionResult.failure

        def get_execution_state(*args, **kwargs):
            if execution_state['result'] in (ExecutionResult.undefined, ExecutionResult.failure):
                state = copy.copy(execution_state)
                execution_state['result'] = result
                execution_state['status'] = ExecutionStatus.completed
                return state
            return execution_state

        self.mock.get_execution_state.side_effect = get_execution_state
        return execution_state


@pytest.fixture
def nirvana_api_mock():
    with mock.patch('nirvana_api.api.NirvanaApi.__new__') as m:
        yield NirvanaApiMock(m)


@pytest.fixture()
def export(session, nirvana_block):
    nirvana_block.enqueue('NIRVANA_BLOCK')
    return nirvana_block.exports['NIRVANA_BLOCK']
