"""Tests host task stages management."""

import copy

from walle.operations_log.constants import Operation
from walle.stages import Stage, set_uids, get_by_uid, get_next, get_parent


def test_set_uids():
    stages = [
        Stage(
            uid="1",
            name="prepare",
            stages=[
                Stage(uid="1.1", name="set-vlan"),
            ],
        ),
        Stage(
            uid="2",
            name="deploy",
            stages=[
                Stage(uid="2.1", name="reboot-pxe"),
                Stage(uid="2.2", name="deploy-lui"),
            ],
        ),
        Stage(uid="3", name="monitor"),
    ]

    expected_stages = copy.deepcopy(stages)
    _clear_uids(stages)
    assert stages != expected_stages

    assert stages is set_uids(stages)
    assert stages == expected_stages


def test_uid_promise_resolves_after_uid_set():
    child_stage = Stage(name="set-vlan")
    stage = Stage(name="prepare", stages=[child_stage])

    child_stage_uid = child_stage.get_uid()
    stage_uid = stage.get_uid()

    set_uids([stage])

    assert str(stage_uid) == stage.uid
    assert str(child_stage_uid) == child_stage.uid


def test_uid_promise_resolves_on_saving(walle_test):
    stage1 = Stage(name="mock-1")
    stage2 = Stage(name="mock-2", params={"stage1_uid": stage1.get_uid()})

    stages = set_uids([stage1, stage2])
    host = walle_test.mock_host(overrides={"status": Operation.REBOOT.host_status}, task_kwargs={"stages": stages})
    host.reload()

    assert host.task.stages[1].params["stage1_uid"] == stage1.uid


def test_get():
    stages = set_uids(
        [
            Stage(
                name="prepare",
                stages=[
                    Stage(name="set-vlan"),
                ],
            ),
            Stage(
                name="deploy",
                stages=[
                    Stage(name="reboot-pxe"),
                    Stage(name="deploy-lui"),
                ],
            ),
            Stage(name="monitor"),
        ]
    )

    assert get_by_uid(stages, "1") is stages[0]
    assert get_by_uid(stages, "1.1") is stages[0].stages[0]
    assert get_by_uid(stages, "2") is stages[1]
    assert get_by_uid(stages, "2.1") is stages[1].stages[0]
    assert get_by_uid(stages, "2.2") is stages[1].stages[1]
    assert get_by_uid(stages, "3") is stages[2]

    assert get_next(stages, "1") is stages[1]
    assert get_next(stages, "1.1") is None
    assert get_next(stages, "2") is stages[2]
    assert get_next(stages, "2.1") is stages[1].stages[1]
    assert get_next(stages, "2.2") is None
    assert get_next(stages, "3") is None

    assert get_parent(stages, "1") is None
    assert get_parent(stages, "1.1") is stages[0]
    assert get_parent(stages, "2") is None
    assert get_parent(stages, "2.1") is stages[1]
    assert get_parent(stages, "2.2") is stages[1]
    assert get_parent(stages, "3") is None


def _clear_uids(stages):
    for stage in stages:
        del stage.uid
        if stage.stages:
            _clear_uids(stage.stages)
