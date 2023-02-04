import pytest

from intranet.femida.src.candidates.controllers import (
    _calculate_extended_status_by_challenges,
    _calculate_extended_status_by_interviews,
)
from intranet.femida.src.candidates.choices import (
    CONSIDERATION_EXTENDED_STATUSES,
    CHALLENGE_STATUSES,
)
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.interviews.choices import INTERVIEW_TYPES


CES = CONSIDERATION_EXTENDED_STATUSES
IS = Interview.STATES
IT = INTERVIEW_TYPES
CHS = CHALLENGE_STATUSES


@pytest.mark.parametrize('test_case', [
    (
        [],
        None,
    ),
    (
        [CHS.assigned],
        CES.challenge_assigned,
    ),
    (
        [CHS.pending_review],
        CES.challenge_pending_review,
    ),
    (
        [CHS.finished],
        CES.challenge_finished),
    (
        [CHS.assigned, CHS.pending_review],
        CES.challenge_pending_review),
    (
        [CHS.assigned, CHS.finished],
        CES.challenge_assigned),
    (
        [CHS.assigned, CHS.pending_review, CHS.finished],
        CES.challenge_pending_review,
    ),
])
def test_calculate_extended_status_by_challenges(test_case):
    challenge_statuses, expected_status = test_case
    challenges = [{'status': status} for status in challenge_statuses]
    assert expected_status == _calculate_extended_status_by_challenges(challenges)


@pytest.mark.parametrize('test_case', [
    (
        [],
        None,
    ),
    (
        [(IT.aa, IS.assigned)],
        CES.interview_assigned,
    ),
    (
        [(IT.aa, IS.finished)],
        CES.interview_finished,
    ),
    (
        [(IT.aa, IS.assigned), (IT.aa, IS.finished)],
        CES.interview_assigned,
    ),
    (
        [(IT.aa, IS.estimated), (IT.aa, IS.finished)],
        CES.interview_assigned,
    ),
    (
        [(IT.hr_screening, IS.assigned), (IT.screening, IS.assigned)],
        CES.screening_assigned,
    ),
    (
        [(IT.hr_screening, IS.assigned), (IT.regular, IS.estimated), (IT.final, IS.finished)],
        CES.final_finished,
    ),
])
def test_calculate_extended_status_by_interviews(test_case):
    interviews_data, expected_status = test_case
    interviews = [{
        'type': item[0],
        'state': item[1],
    } for item in interviews_data]
    assert expected_status == _calculate_extended_status_by_interviews(interviews)
