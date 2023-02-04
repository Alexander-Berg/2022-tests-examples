import pytest

# import logging
from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.stage import Stage
from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.exceptions import (
    FatalException,
)
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from yp import data_model
from copy import deepcopy


class ClusterSettingsBaseSuite:
    @pytest.fixture
    def sequence_func(self, faas_tasklet):
        return lambda: [Stage("testing filter input", faas_tasklet._filter_input_resources)]

    @pytest.fixture
    def spec(self, base_spec):
        return deepcopy(base_spec)


class TestClusterSettingsDefault(ClusterSettingsBaseSuite):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            # cluster settings are not set, using default settings.
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "correct",
                    "current_enviroment": env.value,
                },
            ),
        ]

    @pytest.fixture
    def correct_resources(self, env):
        return {
            "faas-tests-correct": {
                "cluster_settings": {"sas": {"amount": 1}, "vla": {"amount": 1}},
                "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:2",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "namespaces": '["tests"]',
            }
        }

    def test(self, tasklet_with_sequence, correct_resources):
        tasklet_with_sequence.run()
        assert tasklet_with_sequence.context.instance_faas_resource_data == correct_resources


class TestClusterSettingsIncorrectSettings(ClusterSettingsBaseSuite):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            # cluster settings are incorrect, Fatal Exception should be thrown.
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "instance_settings": "{}",
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "correct",
                    "current_enviroment": env.value,
                },
            ),
        ]

    @pytest.fixture
    def correct_resources(self, env):
        return {
            "faas-tests-correct": {
                "cluster_settings": {"sas": {"amount": 1}, "vla": {"amount": 1}},
                "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:2",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "namespaces": '["tests"]',
            }
        }

    def test(self, tasklet_with_sequence):
        with pytest.raises(FatalException):
            tasklet_with_sequence.run()


class TestClusterSettingsCorrectSettings(ClusterSettingsBaseSuite):
    @pytest.fixture
    def faas_resources(self, env):
        return [
            # cluster settings are correct, but dcs are not set - using default deploy strategy.
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "instance_settings": '{"active": true}',
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "correct",
                    "current_enviroment": env.value,
                },
            ),
            # cluster setting are corrent: vla is correct dc, sas is not set - using default.
            # wtf cluster does not exist - skipping it.
            SandboxResource(
                id=3,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "instance_settings": '{"active": true, "dcs": [{"name": "vla", "amount": 5}, {"name": "wtf", "amount": 5}]}',
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "correctv2",
                    "current_enviroment": env.value,
                },
            ),
        ]

    @pytest.fixture
    def correct_resources(self, env):
        return {
            "faas-tests-correct": {
                "cluster_settings": {"sas": {"amount": 1}, "vla": {"amount": 1}},
                "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:2",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "namespaces": '["tests"]',
            },
            "faas-tests-correctv2": {
                "cluster_settings": {"sas": {"amount": 1}, "vla": {"amount": 5}},
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
