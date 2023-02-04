import pytest
import billing.hot.faas.tasklets.deploy_faas.proto.deploy_faas_pb2 as faas_proto

from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.stage import Stage
from billing.hot.faas.tasklets.deploy_faas.impl.enums import Env
from billing.hot.faas.tasklets.deploy_faas.impl import FatalException
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from yp import data_model
from copy import deepcopy


class TestFilterInputResourcesBase:
    @pytest.fixture
    def sequence_func(self, faas_tasklet):
        return lambda: [Stage("testing filter input", faas_tasklet._filter_input_resources)]

    @pytest.fixture
    def spec(self, base_spec):
        return deepcopy(base_spec)


class TestResourceWithIncorrectTitle(TestFilterInputResourcesBase):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            SandboxResource(
                id=1,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "namespaces": '["tests"]',
                    "tenant": "veryveryveryveryveryveryveryveryveryvery-long-tenant-name-like-reaaaaaaly-long",
                    "instance": "deprecated",
                },
            )
        ]

    def test(self, tasklet_with_sequence):
        with pytest.raises(FatalException):
            tasklet_with_sequence.run()


class TestFilterInputResource(TestFilterInputResourcesBase):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            # deprecated version of the resources - must be skipped.
            SandboxResource(
                id=1,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "namespaces": '["tests"]',
                    "tenant": "tests",
                    "instance": "deprecated",
                },
            ),
            # correct resource.
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "namespaces": '["tests"]',
                    "instance": "correct",
                    "tenant": "tests",
                    "current_enviroment": env.value,
                },
            ),
            # incorrect resource, current_enviroment does not match - must be skipped.
            SandboxResource(
                id=3,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "namespaces": '["tests"]',
                    "tenant": "tests",
                    "instance": "correct",
                    "current_enviroment": Env.DEVELOPMENT.value if env != Env.DEVELOPMENT else Env.TESTING.value,
                },
            ),
        ]

    @pytest.fixture
    def correct_resources(self, tasklet_with_sequence):
        cluster_settings = {name: {"amount": 1} for name in tasklet_with_sequence.input.config.clusters}

        return {
            "faas-tests-deprecated": {
                "cluster_settings": cluster_settings,
                "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:1",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "namespaces": '["tests"]',
            },
            "faas-tests-correct": {
                "cluster_settings": cluster_settings,
                "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:3",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "namespaces": '["tests"]',
            },
        }

    def test(self, tasklet_with_sequence, correct_resources):
        tasklet_with_sequence.run()
        assert tasklet_with_sequence.context.instance_faas_resource_data == correct_resources


class TestResourceDoesNotHaveTenantAttribute(TestFilterInputResourcesBase):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            # resource does not have "tenant" field
            SandboxResource(
                id=1,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "namespaces": '["tests"]',
                    "instance": "deprecated",
                },
            ),
        ]

    def test(self, tasklet_with_sequence):
        with pytest.raises(FatalException):
            tasklet_with_sequence.run()


class TestMinimalRevision(TestFilterInputResourcesBase):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            SandboxResource(
                id=1,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "deprecated",
                },
            ),
            SandboxResource(
                id=1,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "3",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "deprecated",
                },
            ),
        ]

    @pytest.fixture
    def faas_input(self, faas_resources):
        return faas_proto.Input(
            config=faas_proto.Input.Config(
                stage_id="test-stage",
                awacs_namespace="faas-test-namespace",
                clusters=["man", "sas", "vla"],
                logbroker_topics=faas_proto.Input.LogbrokerTopics(nginx_topic="test/topic", faas_topic="test/topic"),
                network_macro="_BILLING_DEPLOY_FAAS_TEST_NETS_",
                secrets={
                    "tvm": faas_proto.Input.SecretData(secret_uuid="sec-tvm-test", attributes={"tvm_client_id": "123"}),
                    "certificate": faas_proto.Input.SecretData(secret_uuid="sec-cer-test"),
                },
                dry_run=False,
                release_type="testing",
                minimal_revision=2,
            ),
            faas_resources=faas_resources,
        )

    def test(self, tasklet_with_sequence):
        with pytest.raises(FatalException):
            tasklet_with_sequence.run()
