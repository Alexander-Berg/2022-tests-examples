import pytest
import re

from unittest.mock import patch, call

from intranet.femida.src.candidates.choices import CONSIDERATION_EXTENDED_STATUSES
from intranet.femida.src.candidates.considerations.controllers import get_relevant_consideration
from intranet.femida.src.communications.choices import MESSAGE_STATUSES
from intranet.femida.src.interviews.choices import (
    AA_TYPES,
    INTERVIEW_ROUND_PLANNERS,
    INTERVIEW_ROUND_STATUSES,
    INTERVIEW_ROUND_TYPES,
    INTERVIEW_STATES,
    INTERVIEW_TYPES,
)
from intranet.femida.src.interviews.tasks import interview_round_finish_planning_task

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import get_mocked_event, AnyFrom


pytestmark = pytest.mark.django_db


def _create_draft_interview(round, **kwargs):
    if 'application' not in kwargs:
        kwargs['application'] = f.ApplicationFactory(
            candidate=round.candidate,
            consideration=round.consideration,
        )
    return f.InterviewFactory(
        round=round,
        candidate=round.candidate,
        consideration=round.consideration,
        created_by=round.created_by,
        state=INTERVIEW_STATES.draft,
        interviewer=None,
        **kwargs,
    )


@pytest.mark.parametrize('need_notify_candidate', (True, False))
@patch('intranet.femida.src.interviews.yang.controllers.update_event_task.delay')
@patch('intranet.femida.src.interviews.yang.controllers.send_email_to_candidate')
@patch('intranet.femida.src.interviews.yang.controllers.InterviewRoundPlanningFinishedNotification')
@patch('intranet.femida.src.interviews.yang.controllers.notify_about_interview_create')
@patch('intranet.femida.src.interviews.yang.forms.get_event', get_mocked_event)
def test_finish_planning_task(mocked_common_notify, mocked_recruiter_notify,
                              mocked_candidate_notify, mocked_update_event_task,
                              need_notify_candidate):
    candidate = f.create_heavy_candidate()
    message_text = 'See you at {interview_datetime}'
    expected_message_rgx = re.compile(r'See you at \d{2}\.\d{2}.\d{4} \d{2}:\d{2}')
    consideration = get_relevant_consideration(candidate.id)
    interview_round = f.InterviewRoundFactory(
        type=INTERVIEW_ROUND_TYPES.onsite,
        status=INTERVIEW_ROUND_STATUSES.planning,
        planner=INTERVIEW_ROUND_PLANNERS.yang,
        candidate=candidate,
        consideration=consideration,
        message=(
            f.MessageFactory(text=message_text)
            if need_notify_candidate
            else None
        ),
    )
    interview = _create_draft_interview(interview_round, type=INTERVIEW_TYPES.regular)
    aa_interview = _create_draft_interview(
        round=interview_round,
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.ml,
        application=None,
    )
    wrong_interview = _create_draft_interview(interview_round, type=INTERVIEW_TYPES.regular)
    interviewer = f.UserFactory()
    aa_interviewer = f.create_aa_interviewer(AA_TYPES.ml)

    interviews_data = [
        dict(
            id=interview.id,
            state=INTERVIEW_STATES.assigned,
            interviewer=interviewer.username,
            event_id=1,
        ),
        dict(
            id=aa_interview.id,
            state=INTERVIEW_STATES.assigned,
            interviewer=aa_interviewer.username,
            event_id=2,
        ),
        dict(
            id=wrong_interview.id,
            state=INTERVIEW_STATES.cancelled,
        ),
    ]

    interview_round_finish_planning_task(interview_round.id, {'interviews': interviews_data})

    interview_round.refresh_from_db()
    interview.refresh_from_db()
    wrong_interview.refresh_from_db()
    aa_interview.refresh_from_db()
    consideration.refresh_from_db()

    assert interview_round.status == INTERVIEW_ROUND_STATUSES.planned
    assert interview.state == INTERVIEW_STATES.assigned
    assert interview.interviewer == interviewer
    assert interview.event_id == 1
    assert interview.event_start_time is not None
    assert aa_interview.state == INTERVIEW_STATES.assigned
    assert aa_interview.interviewer == aa_interviewer
    assert aa_interview.event_id == 2
    assert aa_interview.event_start_time is not None
    assert wrong_interview.state == INTERVIEW_STATES.cancelled
    assert wrong_interview.interviewer is None
    assert wrong_interview.event_id is None
    assert wrong_interview.event_start_time is None
    assert consideration.extended_status == CONSIDERATION_EXTENDED_STATUSES.interview_assigned

    mocked_recruiter_notify.assert_called_once_with(
        instance=interview_round,
        interviews=[interview, aa_interview],
    )
    mocked_recruiter_notify().send.assert_called_once()
    not_empty = AnyFrom(exclude=[None, {}])
    mocked_common_notify.assert_has_calls([
        call(i, interview_round.created_by, not_empty, not_empty)
        for i in (interview, aa_interview)
    ])
    assert mocked_candidate_notify.call_count == need_notify_candidate
    if need_notify_candidate:
        assert interview.round.message.status == MESSAGE_STATUSES.sending
        assert expected_message_rgx.match(interview_round.message.text)

    mocked_update_event_task.assert_has_calls((
        call(interview.event_id, not_empty),
        call(aa_interview.event_id, not_empty),
    ))
    assert mocked_update_event_task.call_count == 2


@patch('intranet.femida.src.interviews.yang.forms.get_event', get_mocked_event)
@patch('intranet.femida.src.interviews.yang.controllers.InterviewRoundPlanningFinishedNotification')
@patch('intranet.femida.src.interviews.yang.controllers.notify_about_interview_create')
def test_finish_planning_task_all_cancelled(mocked_notify, mocked_recruiter_notify):
    interview_round = f.InterviewRoundFactory(
        type=INTERVIEW_ROUND_TYPES.screening,
        status=INTERVIEW_ROUND_STATUSES.planning,
        planner=INTERVIEW_ROUND_PLANNERS.yang,
    )
    interview = _create_draft_interview(interview_round, type=INTERVIEW_TYPES.screening)
    interviews_data = [
        dict(id=interview.id, state=INTERVIEW_STATES.cancelled),
    ]

    interview_round_finish_planning_task(interview_round.id, {'interviews': interviews_data})

    interview_round.refresh_from_db()
    interview.refresh_from_db()

    assert interview_round.status == INTERVIEW_ROUND_STATUSES.cancelled
    assert interview.state == INTERVIEW_STATES.cancelled
    assert not mocked_notify.called
    mocked_recruiter_notify.assert_called_once_with(
        instance=interview_round,
        interviews=[],
    )
    mocked_recruiter_notify().send.assert_called_once()
