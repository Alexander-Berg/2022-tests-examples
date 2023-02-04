"""Tests completion of host preparing task."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
)
from walle import restrictions
from walle.hosts import HostState
from walle.stages import Stages, Stage
from walle.util.deploy_config import DeployConfigPolicies


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.COMPLETE_PREPARING))


@pytest.mark.parametrize("with_custom_provisioning", (True, False))
@pytest.mark.parametrize("with_custom_restrictions", (True, False))
@pytest.mark.parametrize("extra_vlans", (None, [], [1, 2, 3]))
def test_complete(test, with_custom_provisioning, with_custom_restrictions, extra_vlans):
    default_restrictions = [restrictions.AUTOMATION]
    project = test.mock_project({"id": "some-id", "default_host_restrictions": default_restrictions})

    deploy_config_policy = DeployConfigPolicies.DISKMANAGER

    params = {}
    if with_custom_provisioning:
        params.update(
            {
                "provisioner": test.host_provisioner,
                "config": test.host_deploy_config,
                "deploy_config_policy": deploy_config_policy,
            }
        )
    if with_custom_restrictions:
        params["restrictions"] = [restrictions.AUTOMATED_REDEPLOY]
    if extra_vlans is not None:
        params["extra_vlans"] = extra_vlans

    host = test.mock_host(
        {
            "project": project.id,
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.COMPLETE_PREPARING, stage_params=params),
        }
    )
    assert host.provisioner is None
    assert host.config is None
    assert host.deploy_config_policy is None
    assert host.restrictions is None
    assert host.extra_vlans is None

    handle_host(host)

    # host.set_state(HostState.ASSIGNED, issuer=host.task.owner, audit_log_id=host.task.audit_log_id)
    if with_custom_provisioning:
        host.provisioner = params["provisioner"]
        host.config = params["config"]
        host.deploy_config_policy = deploy_config_policy
    if with_custom_restrictions:
        host.restrictions = params["restrictions"]
    else:
        host.restrictions = default_restrictions
    if extra_vlans is not None:
        host.extra_vlans = extra_vlans
    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()
