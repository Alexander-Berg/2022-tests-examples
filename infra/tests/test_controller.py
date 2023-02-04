from unittest import mock

from infra.deploy_queue_controller.lib import actions, yp_client, queue_controller, http

import pytest


pytestmark = pytest.mark.asyncio

STAGES = 10
TICKETS_PER_STAGE = 20

ACL = [{
    'action': 'allow',
    'subjects': ['test'],
    'permissions': ['write'],
}]


def make_project(client) -> str:
    return client.create_object(object_type="project", attributes={
        'spec': {
            'account_id': 'tmp'
        }
    })


def make_stage(client, project_id) -> str:
    return client.create_object(object_type="stage", attributes={
        "meta": {
            'acl': ACL,
            'project_id': project_id,
        },
        'labels': {
            'separate_du_revision': True,
        },
        'spec': {
            'account_id': 'tmp',
            'revision': 1,
            'dynamic_resources': {
                'dr1': {
                    'deploy_unit_ref': 'du1',
                    'dynamic_resource': {
                        'update_window': 1,
                        'revision': 1,
                        'deploy_groups': [
                            {
                                'mark': 'all',
                            }
                        ],
                    },
                },
            },
            'deploy_units': {
                'du1': {
                    'replica_set': {
                        'replica_set_template': {
                            'pod_template_spec': {
                                'spec': {
                                    'pod_agent_payload': {
                                        'spec': {
                                            'resources': {
                                                'static_resources': [
                                                    {
                                                        'id': 'ref1',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
        },
        'status': {
            'revision': 1,
            'dynamic_resources': {
                'dr1': {
                    'current_target': {
                        'deploy_unit_ref': 'du1',
                        'dynamic_resource': {
                            'update_window': 1,
                            'revision': 1,
                            'deploy_groups': [
                                {
                                    'mark': 'all',
                                }
                            ],
                        },
                    },
                    'status': {
                        'revision': 1,
                        'ready': {
                            'pod_count': 1,
                            'condition': {
                                'status': 'true',
                            }
                        },
                    },
                },
            },
            'deploy_units': {
                'du1': {
                    'replica_set': {
                        'revision_id': 1,
                    },
                    'target_revision': 1,
                }
            }
        }
    })


@pytest.fixture(scope='function')
def with_stages(yp_env):
    project_id = make_project(yp_env)
    return [
        make_stage(yp_env, project_id)
        for idx in range(STAGES)
    ]


def make_release_rule(client, stage_id: str) -> str:
    return client.create_object(object_type="release_rule", attributes={
        "meta": {
            "acl": ACL,
            "stage_id": stage_id,
        },
        "spec": {
            'selector_source': yp_client.data_model.TReleaseRuleSpec.ESelectorSource.CUSTOM,
            'sandbox': {
                'task_type': 'test-task-type',
                'resource_types': ['test-resource-type'],
            },
            'patches': {
                'test-patch': {
                    'sandbox': {
                        'sandbox_resource_type': 'test-resource-type'
                    }
                }
            },
            "auto_commit_policy": {"type": yp_client.data_model.TAutoCommitPolicy.MAINTAIN_ACTIVE_TRUNK},
        }
    })


def make_dr_release_rule(client, stage_id: str) -> str:
    return client.create_object(object_type="release_rule", attributes={
        "meta": {
            "acl": ACL,
            "stage_id": stage_id,
        },
        "spec": {
            'selector_source': yp_client.data_model.TReleaseRuleSpec.ESelectorSource.CUSTOM,
            'sandbox': {
                'task_type': 'test-task-type',
                'resource_types': ['test-resource-type'],
            },
            'patches': {
                'test-patch': {
                    'sandbox': {
                        'sandbox_resource_type': 'test-resource-type',
                        'dynamic': {
                            'dynamic_resource_id': 'dr1',
                            'deploy_group_mark': 'all',
                        },
                    },
                },
            },
            "auto_commit_policy": {"type": yp_client.data_model.TAutoCommitPolicy.MAINTAIN_ACTIVE_TRUNK},
        }
    })


@pytest.fixture(scope='function')
def with_release_rules(yp_env, with_stages):
    return {
        stage_id: make_release_rule(yp_env, stage_id)
        for stage_id in with_stages
    }


@pytest.fixture(scope='function')
def with_dr_release_rules(yp_env, with_stages):
    return {
        stage_id: make_dr_release_rule(yp_env, stage_id)
        for stage_id in with_stages
    }


def make_sandbox_release(client) -> str:
    return client.create_object(object_type='release', attributes={
        'spec': {
            'sandbox': {
                'task_type': 'test-task-type',
                'task_id': '42',
                'release_type': 'stable',
                'resources': [{
                    'resource_id': '42',
                    'type': 'test-resource-type',
                    'skynet_id': '12345',
                    'http_url': '12345',
                    'arch': 'none',
                    'file_md5': '12345',
                }]
            }
        }
    })


@pytest.fixture(scope='function')
def with_sandbox_release(yp_env) -> str:
    return make_sandbox_release(yp_env)


def make_ticket(client, rule_id, release_id, stage_id):
    return client.create_object(object_type='deploy_ticket', attributes={
        "meta": {
            'stage_id': stage_id,
            'acl': ACL,
        },
        "spec": {
            'release_rule_id': rule_id,
            "release_id": release_id,
            "patches": {
                "1": {
                    "sandbox": {
                        "sandbox_resource_type": "test-resource-type",
                        "static": {
                            "deploy_unit_id": "du1",
                            "static_resource_ref": "ref1",
                        },
                    },
                },
            },
        },
        "status": {
            "patches": {
                "1": {},
            }
        }
    })


def make_dr_ticket(client, rule_id, release_id, stage_id):
    return client.create_object(object_type='deploy_ticket', attributes={
        "meta": {
            'stage_id': stage_id,
            'acl': ACL,
        },
        "spec": {
            'release_rule_id': rule_id,
            "release_id": release_id,
            "patches": {
                '1': {
                    'sandbox': {
                        'sandbox_resource_type': 'test-resource-type',
                        'dynamic': {
                            'dynamic_resource_id': 'dr1',
                            'deploy_group_mark': 'all',
                        },
                    },
                },
            },
        },
        "status": {
            "patches": {
                "1": {},
            }
        }
    })


@pytest.fixture(scope='function')
def with_tickets(yp_env, with_release_rules, with_sandbox_release):
    client = yp_env
    tickets = {}
    for stage_id, rule_id in with_release_rules.items():
        tickets[stage_id] = [
            make_ticket(client, rule_id, with_sandbox_release, stage_id)
            for _ in range(TICKETS_PER_STAGE)
        ]
    return tickets


@pytest.fixture(scope='function')
def with_dr_tickets(yp_env, with_dr_release_rules, with_sandbox_release):
    client = yp_env
    tickets = {}
    for stage_id, rule_id in with_dr_release_rules.items():
        tickets[stage_id] = [
            make_dr_ticket(client, rule_id, with_sandbox_release, stage_id)
            for _ in range(TICKETS_PER_STAGE)
        ]
    return tickets


@pytest.fixture(scope='function')
def statistics():
    return http.StatisticsServer(port=0)


@pytest.fixture(scope='function')
def controller(yp_env, statistics):
    client = yp_client.YpClient(yp_env.create_grpc_object_stub())
    controller = queue_controller.QueueController(client, 5000, 'test', statistics)
    yield controller


@pytest.mark.usefixtures("with_tickets")
async def test_collect_tickets(controller, with_stages, with_release_rules):
    ts = await controller.yp_client.generate_timestamp()
    assert ts > 0

    total = 0

    def add_ticket(*args, **kwargs):
        nonlocal total
        total += 1

    release_rule_mock = mock.Mock()
    release_rule_mock.add_ticket.side_effect = add_ticket

    await controller._poll_tickets(
        timestamp=ts,
        batch_size=controller.batch_size,
        stages={stage: mock.Mock() for stage in with_stages},
        release_rules={rr: release_rule_mock for rr in with_release_rules.values()},
    )
    assert total == STAGES * TICKETS_PER_STAGE


async def test_iteration_trunk(controller, with_tickets, with_release_rules, with_sandbox_release, yp_env):
    actions_to_perform = None
    old_perform = actions.Actions.perform

    def mock_perform(self, *args, **kwargs):
        nonlocal actions_to_perform
        actions_to_perform = (self.cancelled_tickets, self.committed_tickets, self.waiting_tickets)
        return old_perform(self, *args, **kwargs)

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert len(cancelled) == STAGES * (TICKETS_PER_STAGE - 1)
        assert len(performed) == STAGES
        assert not waiting

        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert not performed
        assert not waiting

        stage_id, rule_id = next(iter(with_release_rules.items()))
        for _ in range(5):
            make_ticket(yp_env, rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert not performed
        assert len(waiting) == 5


async def test_iteration_trunk_force(controller, with_tickets, with_release_rules, with_sandbox_release, yp_env):
    actions_to_perform = None
    old_perform = actions.Actions.perform

    def mock_perform(self, *args, **kwargs):
        nonlocal actions_to_perform
        actions_to_perform = (self.cancelled_tickets, self.committed_tickets, self.waiting_tickets)
        return old_perform(self, *args, **kwargs)

    stage_id, rule_id = next(iter(with_release_rules.items()))
    yp_env.update_object(object_identity=rule_id, object_type="release_rule", set_updates=[{
        "path": "/spec/auto_commit_policy/maintain_active_trunk_options",
        "value": {"deployment_termination_policy": "terminate"},
    }])

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert len(cancelled) == STAGES * (TICKETS_PER_STAGE - 1)
        assert len(performed) == STAGES
        assert not waiting

        stage_id, rule_id = next(iter(with_release_rules.items()))
        for _ in range(5):
            make_ticket(yp_env, rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert len(cancelled) == 4
        assert len(performed) == 1
        assert not waiting


async def test_iteration_dynamic_resource(
    controller,
    with_dr_tickets, with_dr_release_rules,
    with_release_rules,
    with_sandbox_release,
    yp_env,
):
    actions_to_perform = None
    old_perform = actions.Actions.perform

    def mock_perform(self, *args, **kwargs):
        nonlocal actions_to_perform
        actions_to_perform = (self.cancelled_tickets, self.committed_tickets, self.waiting_tickets)
        return old_perform(self, *args, **kwargs)

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert len(cancelled) == STAGES * (TICKETS_PER_STAGE - 1)
        assert len(performed) == STAGES
        assert not waiting

        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert not performed
        assert not waiting

        stage_id, dr_rule_id = next(iter(with_dr_release_rules.items()))
        rule_id = with_release_rules[stage_id]

        for _ in range(5):
            make_ticket(yp_env, rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert len(cancelled) == 4
        assert len(performed) == 1
        assert not waiting

        for _ in range(5):
            make_dr_ticket(yp_env, dr_rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert not performed
        assert len(waiting) == 5


async def test_iteration_sequential(controller, with_release_rules, with_tickets, with_sandbox_release, yp_env):
    yp_env.update_objects(
        {
            "object_identity": rr,
            "object_type": "release_rule",
            "set_updates": [{
                "path": "/spec/auto_commit_policy/type",
                "value": "sequential_commit",
            }]
        }
        for rr in with_release_rules.values()
    )

    actions_to_perform = None
    old_perform = actions.Actions.perform

    def mock_perform(self, *args, **kwargs):
        nonlocal actions_to_perform
        actions_to_perform = (self.cancelled_tickets, self.committed_tickets, self.waiting_tickets)
        return old_perform(self, *args, **kwargs)

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert len(waiting) == STAGES * (TICKETS_PER_STAGE - 1)
        assert len(performed) == STAGES
        assert not cancelled

        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not waiting
        assert not performed
        assert not cancelled

        stage_id, rule_id = next(iter(with_release_rules.items()))
        for _ in range(5):
            make_ticket(yp_env, rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert not performed
        assert len(waiting) == 5


async def test_duplicate_commit_prevention(controller, yp_env):
    release = make_sandbox_release(yp_env)
    stage = make_stage(yp_env, make_project(yp_env))
    release_rule = make_release_rule(yp_env, stage)
    ticket = make_ticket(yp_env, release_rule, release, stage)

    yp_env.update_object(
        object_type="deploy_ticket",
        object_identity=ticket,
        set_updates=[{
            'path': '/status/action',
            'value': {
                "type": "commit",
                "message": "test commit",
                "reason": "ALREADY_COMMITTED",
            },
        }],
    )

    actions_to_perform = None
    old_perform = actions.Actions.perform

    def mock_perform(self, *args, **kwargs):
        nonlocal actions_to_perform
        actions_to_perform = (self.cancelled_tickets, self.committed_tickets, self.waiting_tickets)
        return old_perform(self, *args, **kwargs)

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        await controller.iterate()

        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform

        assert not waiting
        assert not performed
        assert not cancelled


async def test_skip_corrupted_patches(controller, with_tickets, with_stages, yp_env):
    old_perform = actions.Actions.perform
    exc = None

    async def mock_perform(self, *args, **kwargs):
        nonlocal exc

        yp_env.update_object(
            object_type="stage",
            object_identity=with_stages[0],
            remove_updates=[{
                'path': '/spec/deploy_units/du1',
            }]
        )

        try:
            await old_perform(self, *args, **kwargs)
        except Exception as e:
            exc = e
            raise

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        await controller.iterate()
        if exc is not None:
            raise exc


async def test_commit_dr_on_active_stage(
    controller,
    with_stages,
    with_dr_release_rules,
    with_release_rules,
    with_sandbox_release,
    yp_env,
):
    actions_to_perform = None
    old_perform = actions.Actions.perform

    def mock_perform(self, *args, **kwargs):
        nonlocal actions_to_perform
        actions_to_perform = (self.cancelled_tickets, self.committed_tickets, self.waiting_tickets)
        return old_perform(self, *args, **kwargs)

    with mock.patch.object(actions.Actions, 'perform', mock_perform):
        stage_id, dr_rule_id = next(iter(with_dr_release_rules.items()))
        rule_id = with_release_rules[stage_id]

        make_ticket(yp_env, rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert len(performed) == 1
        assert not waiting

        ts = await controller.yp_client.generate_timestamp()
        stages = await controller._poll_stages(ts, controller.batch_size)
        assert stages[stage_id].is_in_progress()

        make_dr_ticket(yp_env, dr_rule_id, with_sandbox_release, stage_id)

        await controller.iterate()
        assert isinstance(actions_to_perform, tuple) and len(actions_to_perform) == 3, "Update not performed"
        cancelled, performed, waiting = actions_to_perform
        assert not cancelled
        assert len(performed) == 1
        assert not waiting
