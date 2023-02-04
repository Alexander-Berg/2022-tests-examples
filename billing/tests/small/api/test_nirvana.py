"""
Тестирование backend.nirvana
"""

import pytest
import requests
import tenacity

from unittest import mock
from django.conf import settings

from billing.dcsaap.backend.project.const import APP_PREFIX
from billing.dcsaap.backend.core.enum import FlowType
from billing.dcsaap.backend.core.models import Check
from billing.dcsaap.backend.api import nirvana


@pytest.fixture
def nirvana_api_mock():
    with mock.patch(f'{APP_PREFIX}.api.nirvana.NirvanaApi', autospec=True) as m:
        nirvana_api_mock = m.return_value
        yield nirvana_api_mock

    m.assert_called()


class TestStartInstance:
    @pytest.fixture(autouse=True)
    def setup(self):
        self.workflow_id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        self.instance_id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        self.nirvana_oauth_token = "token"

    @pytest.fixture(autouse=True)
    def dont_wait_when_retry(self):
        with mock.patch('time.sleep') as m:
            yield m

    def test_ok(self, nirvana_api_mock, some_check):
        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)
        res = nirvana_facade.start_instance(self.instance_id)

        assert res == nirvana_api_mock.start_workflow.return_value

        nirvana_api_mock.start_workflow.assert_called_once_with(workflow_instance_id=self.instance_id)

    def test_with_override_ok(self, nirvana_api_mock):
        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)

        override_parameters = {
            "param1": 123,
            "param2": "aboba",
        }

        with mock.patch.object(nirvana.NirvanaFacade, '_merge_parameters') as m:
            nirvana_facade.start_instance(self.instance_id, override_parameters)
            m.assert_called_once()

        nirvana_api_mock.get_global_parameters.assert_called_once_with(workflow_instance_id=self.instance_id)
        nirvana_api_mock.set_global_parameters.assert_called_once_with(
            workflow_instance_id=self.instance_id, param_values=m.return_value
        )

    @pytest.mark.parametrize(
        'exception',
        [
            pytest.param(nirvana.RPCException(70), id='quota_exceeded'),
            pytest.param(requests.exceptions.ConnectionError, id='connection_error'),
        ],
    )
    def test_retry_ok(self, nirvana_api_mock, exception):
        retry_count = 4
        nirvana_api_mock.start_workflow.side_effect = [exception] * retry_count + [mock.DEFAULT]

        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)
        res = nirvana_facade.start_instance(self.instance_id)

        assert nirvana_api_mock.start_workflow.call_count == retry_count + 1
        assert res == nirvana_api_mock.start_workflow.return_value

    @pytest.mark.parametrize(
        'exception',
        [
            pytest.param(nirvana.RPCException(60), id='some_rpc_exception'),
            pytest.param(KeyError(), id='some_exception'),
        ],
    )
    def test_retry_fail(self, nirvana_api_mock, exception):
        nirvana_api_mock.start_workflow.side_effect = exception

        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)
        with pytest.raises(type(exception)):
            nirvana_facade.start_instance(self.instance_id)

        nirvana_api_mock.start_workflow.assert_called_once()

    def test_retry_exceeded(self, nirvana_api_mock):
        nirvana_api_mock.start_workflow.side_effect = nirvana.RPCException(70)

        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)
        with pytest.raises(tenacity.RetryError):
            nirvana_facade.start_instance(self.instance_id)


class TestMakeInstance:
    @pytest.fixture(autouse=True)
    def setup(self, some_check):
        self.nirvana_oauth_token = "token"
        some_check.instance_id = None

    @pytest.mark.parametrize(
        'workflow_id_field, instance_id_field, flow_type',
        [
            pytest.param(
                field_names['workflow_id_field'], field_names['instance_id_field'], flow_type, id=flow_type.value
            )
            for flow_type, field_names in Check.flow_types().items()
        ],
    )
    def test_with_instance(
        self, nirvana_api_mock, some_check: Check, flow_type: FlowType, workflow_id_field: str, instance_id_field: str
    ):
        workflow_id, instance_id = 'aaaa', 'bbbb'
        setattr(some_check, workflow_id_field, workflow_id)
        setattr(some_check, instance_id_field, instance_id)
        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)

        new_instance_id = nirvana_facade.make_instance(some_check, flow_type)

        nirvana_api_mock.clone_workflow_instance.assert_called_once_with(
            target_workflow_id=workflow_id, workflow_instance_id=instance_id
        )

        assert new_instance_id == nirvana_api_mock.clone_workflow_instance.return_value

    @pytest.mark.parametrize(
        'workflow_id_field, operation_id_field, flow_type',
        [
            pytest.param(
                field_names['workflow_id_field'], field_names['operation_id_field'], flow_type, id=flow_type.value
            )
            for flow_type, field_names in Check.flow_types().items()
        ],
    )
    def test_with_operation(
        self, nirvana_api_mock, some_check: Check, flow_type: FlowType, workflow_id_field: str, operation_id_field: str
    ):
        workflow_id, operation_id = 'aaaa', 'cccc'
        setattr(some_check, workflow_id_field, workflow_id)
        setattr(some_check, operation_id_field, operation_id)
        nirvana_facade = nirvana.NirvanaFacade(self.nirvana_oauth_token)

        new_instance_id = nirvana_facade.make_instance(some_check, flow_type)

        nirvana_api_mock.create_workflow_instance.assert_called_once()

        assert new_instance_id == nirvana_api_mock.create_workflow_instance.return_value

        call_kwargs = nirvana_api_mock.create_workflow_instance.call_args.kwargs

        assert call_kwargs['workflow_id'] == workflow_id
        assert call_kwargs['workflow_instance']['meta']['quotaProjectId'] == settings.NIRVANA_QUOTA_PROJECT_ID
