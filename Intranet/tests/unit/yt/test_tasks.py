import pytest

from unittest.mock import patch, MagicMock

from intranet.femida.src.interviews.choices import (
    INTERVIEW_ROUND_STATUSES,
    INTERVIEW_ROUND_PLANNERS,
)
from intranet.femida.src.yt.tasks import get_processed_interview_round_data_from_yt

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@patch('intranet.femida.src.yt.base.yt', MagicMock())
@pytest.mark.parametrize('status, planner, called', (
    (INTERVIEW_ROUND_STATUSES.new, INTERVIEW_ROUND_PLANNERS.yang, False),
    (INTERVIEW_ROUND_STATUSES.planning, INTERVIEW_ROUND_PLANNERS.yang, True),
    (INTERVIEW_ROUND_STATUSES.planning, INTERVIEW_ROUND_PLANNERS.femida, False),
))
@patch('intranet.femida.src.yt.tasks.interview_round_finish_planning_task.delay')
def test_get_processed_interview_round_data_from_yt(mocked_task, status, planner, called):
    interview_round = f.InterviewRoundFactory(status=status, planner=planner)
    data = {'id': interview_round.id}
    patched_data = patch(
        target='intranet.femida.src.yt.tasks.ProcessedInterviewRoundYTTable.read',
        return_value=[data],
    )
    with patched_data:
        get_processed_interview_round_data_from_yt()
    assert mocked_task.called is called
    if called:
        mocked_task.assert_called_once_with(interview_round.id, data)
