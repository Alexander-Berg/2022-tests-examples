import pytest
import six

from walle._tasks import sb_helpers
from walle._tasks.stages import get_power_on_stages, get_ssh_reboot_stages, get_reboot_stages
from walle.constants import SshOperation
from walle.stages import Stages, StageTerminals
from walle.util.tasks import StageBuilder


@pytest.mark.parametrize(["pxe", "result_params"], [(None, None), ("test", {"pxe": "test"})])
def test_get_power_on_stages_without_check_post_code(pxe, result_params):
    sb = get_power_on_stages(pxe=pxe)

    assert (
        [stage.name for stage in sb.get_stages()] == [Stages.POWER_ON]
        and [stage.stages for stage in sb.get_stages()] == [None]
        and [stage.params for stage in sb.get_stages()] == [result_params]
    )


@pytest.mark.parametrize(
    ["pxe", "check_post_code", "result_params"],
    [
        (None, False, [{'check_post_code': True}, {'soft': False}, None]),
        ("test", True, [{'check_post_code': True, "pxe": "test"}, {'soft': False}, {"pxe": "test"}]),
    ],
)
def test_get_power_on_stages_with_check_post_code(pxe, check_post_code, result_params):
    sb = get_power_on_stages(check_post_code=True, pxe=pxe)

    assert len(sb.get_stages()) == 1
    assert [stage.name for stage in sb.get_stages()] == [Stages.POWER_ON_COMPOSITE]

    child_stages = sb.get_stages()[0].stages

    assert (
        [stage.name for stage in child_stages] == [Stages.POWER_ON, Stages.POWER_OFF, Stages.POWER_ON]
        and [stage.stages for stage in child_stages] == [None, None, None]
        and [stage.params for stage in child_stages] == result_params
    )


@pytest.mark.parametrize(
    ["terminators", "result_params"],
    [({"test": "value"}, {"check_post_code": False}), (None, {"check_post_code": False})],
)
def test_get_ssh_reboot_stages_without_check_post_code(terminators, result_params):
    sb = get_ssh_reboot_stages(terminators=terminators)

    assert (
        [stage.name for stage in sb.get_stages()] == [Stages.SSH_REBOOT]
        and [stage.stages for stage in sb.get_stages()] == [None]
        and [stage.params for stage in sb.get_stages()] == [result_params]
        and [stage.terminators for stage in sb.get_stages()] == [terminators]
    )


@pytest.mark.parametrize(
    ["terminators", "result_params"],
    [
        (None, [{'check_post_code': True}, {'soft': False}, None]),
        ({"test": "value"}, [{'check_post_code': True}, {'soft': False}, None]),
    ],
)
def test_get_ssh_reboot_stages_with_check_post_code(terminators, result_params):
    sb = get_ssh_reboot_stages(terminators=terminators, check_post_code=True)

    assert len(sb.get_stages()) == 1
    assert [stage.name for stage in sb.get_stages()] == [Stages.SSH_REBOOT_COMPOSITE]

    child_stages = sb.get_stages()[0].stages

    assert (
        [stage.name for stage in child_stages] == [Stages.SSH_REBOOT, Stages.POWER_OFF, Stages.POWER_ON]
        and [stage.stages for stage in child_stages] == [None, None, None]
        and [stage.params for stage in child_stages] == result_params
        and [stage.terminators for stage in child_stages] == [terminators, None, None]
    )


class MockRebootTaskArgs:
    def __init__(self, ssh=SshOperation.FALLBACK, check_post_code=False, without_ipmi=False, reason=None):
        self.ssh = ssh
        self.check_post_code = check_post_code
        self.without_ipmi = without_ipmi
        self.reason = reason


def compare_stages(result_stages, test_stages):
    if not result_stages and not test_stages:
        return
    assert (
        [stage.name for stage in result_stages] == [stage.name for stage in test_stages]
        and [stage.stages for stage in result_stages] == [stage.stages for stage in test_stages]
        and [stage.params for stage in result_stages] == [stage.params for stage in test_stages]
        and [stage.terminators for stage in result_stages] == [stage.terminators for stage in test_stages]
    )
    for result_stage, test_stage in six.moves.zip_longest(result_stages, test_stages):
        compare_stages(result_stage.stages, test_stage.stages)


@pytest.mark.parametrize(["check_post_code"], [(True,), (False,)])
def test_get_reboot_stages_with_ssh_only(check_post_code):
    task_args = MockRebootTaskArgs(ssh=SshOperation.ONLY, check_post_code=check_post_code)
    result = get_reboot_stages(task_args)

    test_sb = StageBuilder()
    with test_sb.nested(Stages.REBOOT) as reboot_stages:
        reboot_stages.add_stages(get_ssh_reboot_stages(check_post_code=check_post_code, terminators=None).get_stages())

    compare_stages(result.get_stages(), test_sb.get_stages())


@pytest.mark.parametrize(
    ["check_post_code", "terminators"],
    [
        (True, {StageTerminals.FAIL: StageTerminals.COMPLETE_PARENT}),
        (False, {StageTerminals.FAIL: StageTerminals.SKIP}),
    ],
)
def test_get_reboot_stages_with_ssh_fallback(check_post_code, terminators):
    task_args = MockRebootTaskArgs(ssh=SshOperation.FALLBACK, check_post_code=check_post_code)
    result = get_reboot_stages(task_args)

    test_sb = StageBuilder()
    with test_sb.nested(Stages.REBOOT) as reboot_stages:
        reboot_stages.add_stages(
            get_ssh_reboot_stages(check_post_code=check_post_code, terminators=terminators).get_stages()
        )
        sb_helpers.power_off(reboot_stages, soft=True)
        power_on_sb = get_power_on_stages(check_post_code=task_args.check_post_code)
        reboot_stages.add_stages(power_on_sb.get_stages())

    compare_stages(result.get_stages(), test_sb.get_stages())


@pytest.mark.parametrize(["check_post_code"], [(True,), (False,)])
def test_get_reboot_stages_with_ssh_forbid(check_post_code):
    task_args = MockRebootTaskArgs(ssh=SshOperation.FORBID, check_post_code=check_post_code)
    result = get_reboot_stages(task_args)

    test_sb = StageBuilder()
    with test_sb.nested(Stages.REBOOT) as reboot_stages:
        sb_helpers.power_off(reboot_stages, soft=True)
        power_on_sb = get_power_on_stages(check_post_code=task_args.check_post_code)
        reboot_stages.add_stages(power_on_sb.get_stages())

    compare_stages(result.get_stages(), test_sb.get_stages())


def test_get_reboot_stages_without_ipmi():
    task_args = MockRebootTaskArgs(ssh=SshOperation.FORBID, without_ipmi=True)
    result = get_reboot_stages(task_args)

    test_sb = StageBuilder()
    with test_sb.nested(Stages.REBOOT) as reboot_stages:
        sb_helpers.itdc_reboot(reboot_stages)

    compare_stages(result.get_stages(), test_sb.get_stages())
