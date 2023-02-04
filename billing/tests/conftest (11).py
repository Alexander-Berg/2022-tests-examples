import pytest
import billing.hot.faas.tasklets.deploy_faas.proto.deploy_faas_pb2 as faas_proto

from unittest.mock import Mock
from billing.hot.faas.tasklets.deploy_faas.impl import BillingDeployFaasImpl
from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.stage import Stage
from billing.hot.faas.tasklets.deploy_faas.impl.enums import Env
from tasklet.api.tasklet_pb2 import JobStatement
from yp import data_model
from google.protobuf.json_format import MessageToDict
from copy import deepcopy


class SbClientMock(Mock):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.resources = kwargs["resources"]

    def __getitem__(self, item):
        self.resource_id = item
        return self

    def resource(self):
        return self

    def __resource(self, resource_id):
        for res in self.resources:
            if res.id == resource_id:
                return res

    def read(self):
        return {
            "file_name": f"{self.resource_id}/faas",
            "owner": "BILLING-CI",
            "id": self.resource_id,
            "type": "FAAS_RESOURCE",
            "description": "FaaS for megagega-default#r123123",
            "md5": "verysafehash123",
            "attributes": self.__resource(self.resource_id).attributes,
        }


@pytest.fixture(params=list(Env))
def env(request):
    return request.param


@pytest.fixture
def faas_input(faas_resources, env):
    return faas_proto.Input(
        config=faas_proto.Input.Config(
            stage_id="test-stage",
            awacs_namespace="faas-test-namespace",
            clusters=["sas", "vla"],
            logbroker_topics=faas_proto.Input.LogbrokerTopics(nginx_topic="test/topic", faas_topic="test/topic"),
            network_macro="_BILLING_DEPLOY_FAAS_TEST_NETS_",
            secrets={
                "tvm": faas_proto.Input.SecretData(secret_uuid="sec-tvm-test", attributes={"tvm_client_id": "123"}),
                "certificate": faas_proto.Input.SecretData(secret_uuid="sec-cer-test"),
            },
            dry_run=False,
            release_type=env.value,
        ),
        faas_resources=faas_resources,
    )


@pytest.fixture()
def base_spec():
    return MessageToDict(
        data_model.TStageSpec(account_id="abc:cba", revision=1),  # noqa
        preserving_proto_field_name=True,
    )


@pytest.fixture
def mock_yp_client(spec):
    mock_yp_client = Mock()
    _inner = deepcopy(spec)
    mock_yp_client.get_object.return_value = [_inner]
    return mock_yp_client


@pytest.fixture
def mock_vault_client():
    vault_client = Mock()
    vault_client.get_version.return_value = {
        "version": "test_version",
        "secret_name": "test_secret_name",
    }
    vault_client.create_token.return_value = [
        "test_delegation_token",
        "test_secret_data",
    ]
    vault_client.get_token_info.return_value = {"token_info": {"token_uuid": "test_token_uuid"}}
    return vault_client


@pytest.fixture
def mock_sb_client(faas_resources):
    sandbox_client = Mock()
    sandbox_client.resource = SbClientMock(resources=faas_resources)
    return sandbox_client


@pytest.fixture
def mock(faas_tasklet):
    # mock() is appended at the beginning of the sequence
    #  as tasklet ctor creates deploy progress and mocking it before running tasklet is useless - it will be overwritten
    #  by ctor.
    def mocker():
        faas_tasklet.deploy_progress = Mock()

    return mocker


@pytest.fixture
def default_sequence_tasklet(faas_tasklet, mock):
    def sequence():
        return [
            Stage("mock clients", mock),
            Stage("testing filter input", faas_tasklet._filter_input_resources),
            Stage("testing get spec", faas_tasklet._get_current_spec),
            Stage("testing handle du", faas_tasklet._handle_du),
            Stage("testing prepare draft", faas_tasklet._prepare_draft),
            Stage("testing send draft", faas_tasklet._send_draft),
        ]

    faas_tasklet.get_sequence = sequence
    return faas_tasklet


@pytest.fixture
def tasklet_with_sequence(faas_tasklet, sequence_func):
    """
    Sets tasklet sequence.
    Sequence must be set as local pytest.fixture.
    :param faas_tasklet: tasklet to use
    :param sequence_func: function that returns an array of Stages.
    :return:
    """
    faas_tasklet.get_sequence = sequence_func
    return faas_tasklet


@pytest.fixture
def faas_tasklet(faas_input, mock_yp_client, mock_vault_client, mock_sb_client):
    def empty_func():
        pass

    # def mock():
    #     faas_tasklet.deploy_progress = Mock()

    faas_tasklet = BillingDeployFaasImpl(model=JobStatement(name="testing"))

    faas_tasklet.input = faas_input

    faas_tasklet.awacs_client = Mock()
    faas_tasklet.yp_client = mock_yp_client
    faas_tasklet.sb_client = mock_sb_client
    faas_tasklet.vault_client = mock_vault_client

    faas_tasklet._wait_for_commit = empty_func
    faas_tasklet._wait_for_deploy = empty_func

    return faas_tasklet
