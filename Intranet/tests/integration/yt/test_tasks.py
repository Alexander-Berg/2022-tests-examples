import pytest

from collections import OrderedDict
from unittest.mock import patch, MagicMock, ANY

from intranet.femida.src.interviews.choices import (
    AA_TYPES,
    INTERVIEW_ROUND_PLANNERS,
    INTERVIEW_ROUND_STATUSES,
    INTERVIEW_TYPES,
)
from intranet.femida.src.yt.tasks import save_interview_round_data_in_yt

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import AnyOrderList


pytestmark = pytest.mark.django_db


@patch('intranet.femida.src.yt.base.yt.write_table')
@patch('intranet.femida.src.yt.base.yt', MagicMock())
def test_save_interview_round_data_in_yt(mocked_write):
    recruiter = f.UserFactory(username='recruiter')
    interview_round = f.InterviewRoundFactory(
        created_by=recruiter,
        office__name_ru='Красная Роза',
        is_strict_order=True,
        lunch_duration=35,
        comment='Коммент для асессоров',
    )
    regular_interview = interview_round.interviews.create(
        created_by=recruiter,
        type=INTERVIEW_TYPES.regular,
    )
    regular_interview.potential_interviewers.add(
        f.UserFactory(username='interviewer-1'),
        f.UserFactory(username='interviewer-2'),
    )
    aa_interview = interview_round.interviews.create(
        created_by=recruiter,
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.ml,
    )
    interview_round.time_slots.create(
        start='2020-08-31T09:00:00Z',
        end='2020-08-31T15:00:00Z',
    )

    save_interview_round_data_in_yt(interview_round.id)

    interview_round.refresh_from_db()
    assert interview_round.planner == INTERVIEW_ROUND_PLANNERS.yang
    assert interview_round.status == INTERVIEW_ROUND_STATUSES.planning

    mocked_write.assert_called_once_with(ANY, [OrderedDict({
        'timestamp': ANY,
        'id': interview_round.id,
        'candidate_id': interview_round.candidate_id,
        'recruiter': 'recruiter',
        'office': 'Красная Роза',
        'time_slots': [OrderedDict({
            'start': '2020-08-31T09:00:00Z',
            'end': '2020-08-31T15:00:00Z',
        })],
        'interviews': [
            OrderedDict({
                'id': regular_interview.id,
                'type': INTERVIEW_TYPES.regular,
                'aa_type': None,
                'potential_interviewers': AnyOrderList(['interviewer-1', 'interviewer-2']),
            }),
            OrderedDict({
                'id': aa_interview.id,
                'type': INTERVIEW_TYPES.aa,
                'aa_type': AA_TYPES.ml,
                'potential_interviewers': [],
            }),
        ],
        'is_strict_order': True,
        'lunch_duration': 35,
        'comment': 'Коммент для асессоров',
    })])
