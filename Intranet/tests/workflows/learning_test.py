import pytest

from staff.gap.workflows.learning.workflow import LearningWorkflow
from staff.gap.workflows.choices import GAP_STATES as GS


@pytest.mark.django_db
def test_new_gap(gap_test):
    now = gap_test.mongo_now()

    base_gap = gap_test.get_base_gap(LearningWorkflow)

    gap = LearningWorkflow(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_gap(base_gap)

    gap_test.base_assert_new_gap(LearningWorkflow, now, base_gap, gap)

    assert gap['id'] == 1
    assert gap['state'] == GS.NEW
    assert gap['created_by_id'] == gap_test.DEFAULT_MODIFIER_ID
    assert gap['modified_by_id'] == gap_test.DEFAULT_MODIFIER_ID
