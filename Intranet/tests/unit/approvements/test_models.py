import pytest

from waffle.testutils import override_switch

from ok.approvements.choices import APPROVEMENT_STAGE_STATUSES

from tests import factories as f
from tests.factories import approve_stage

pytestmark = pytest.mark.django_db


def _check_current_stages(approvement, stages):
    assert approvement.current_stages == stages


def _update_and_check_current_stages(approvement, stages):
    approvement.next_stages.update(status=APPROVEMENT_STAGE_STATUSES.current)
    _check_current_stages(approvement, stages)


@pytest.fixture(params=(True, False))
def check_current_stages(request):
    with override_switch('enable_stage_status_current', active=request.param):
        yield _update_and_check_current_stages if request.param else _check_current_stages


@pytest.mark.parametrize('comment_id,expected_hash', (
    ('12345', '#12345'),
    ('', ''),
    (None, ''),
))
def test_approvement_url(comment_id, expected_hash):
    approvement = f.ApprovementFactory(
        object_id='ISSUE-1',
        tracker_comment_id=comment_id,
    )
    assert approvement.url == 'https://st.test.yandex-team.ru/ISSUE-1' + expected_hash


def test_current_stages_simple_approvement(check_current_stages):
    approvement = f.ApprovementFactory(is_parallel=False)
    stages = f.ApprovementStageFactory.create_batch(3, approvement=approvement)

    check_current_stages(approvement, [stages[0]])

    approve_stage(stages[0])
    check_current_stages(approvement, [stages[1]])


def test_current_stages_parallel_approvement(check_current_stages):
    approvement = f.ApprovementFactory(is_parallel=True)
    stages = f.ApprovementStageFactory.create_batch(3, approvement=approvement)

    check_current_stages(approvement, stages)

    approve_stage(stages[0])
    check_current_stages(approvement, stages[1:])


@pytest.mark.parametrize('need_approvals', (1, 2))
def test_current_stages_complex_stage(check_current_stages, need_approvals):
    complex_stage = f.create_complex_stage(['u1', 'u2'], need_approvals=need_approvals)
    stages = list(complex_stage.stages.all())

    check_current_stages(complex_stage.approvement, stages)


@pytest.mark.parametrize('need_approvals', (1, 2))
def test_current_stages_complex_stage_finished(check_current_stages, need_approvals):
    complex_stage = f.create_complex_stage(
        approvers=['u1', 'u2'],
        need_approvals=need_approvals,
        is_approved=True,
    )
    approvement = complex_stage.approvement
    current_stage = f.ApprovementStageFactory(
        approvement=approvement,
        position=complex_stage.position + 100,
    )

    check_current_stages(approvement, [current_stage])


def test_current_stages_complex_stage_partially_finished(check_current_stages):
    complex_stage = f.create_parent_stage(need_approvals=2)
    f.create_child_stage(complex_stage, is_approved=True)
    current_stage = f.create_child_stage(complex_stage)
    
    approvement = complex_stage.approvement
    f.ApprovementStageFactory(approvement=approvement, position=complex_stage.position + 100)

    check_current_stages(approvement, [current_stage])
