import pytest

from intranet.femida.src.applications.helpers import count_applications_per_hiring_stage
from intranet.femida.src.interviews.choices import (
    APPLICATION_PROPOSAL_STATUSES,
    APPLICATION_HIRING_STAGES,
    INTERVIEW_TYPES,
)
from intranet.femida.src.interviews.models import Application, Interview

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_count_applications_per_hiring_stage():
    # proposed
    f.ApplicationFactory.create(
        proposal_status=APPLICATION_PROPOSAL_STATUSES.undefined,
    )

    # team_is_interested
    f.ApplicationFactory.create(
        proposal_status=APPLICATION_PROPOSAL_STATUSES.accepted,
    )

    # interview_assigned - нет очек, незавершенный финал
    f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.assigned,
    )

    # interview_assigned - незавершенная очка, нет финалов
    f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.assigned,
    )

    # interview_assigned - незавершенная очка и незавершенный финал
    a = f.ApplicationFactory.create()
    f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.assigned,
        application=a,
    )
    f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.assigned,
        application=a,
    )

    # interview_assigned - незавершенная очка и завершенный финал
    a = f.ApplicationFactory.create()
    f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.assigned,
        application=a,
    )
    f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.finished,
        application=a,
    )

    # interview_assigned - завершенная очка и незавершенный финал
    a = f.ApplicationFactory.create()
    f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.finished,
        application=a,
    )
    f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.assigned,
        application=a,
    )

    # interview_finished - нет очек, завершенный финал
    f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.finished,
    )

    # interview_finished - завершенная очка, нет финалов
    f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.finished,
    )

    # interview_finished - завершенная очка и завершенный финал
    a = f.ApplicationFactory.create()
    f.create_interview(
        type=INTERVIEW_TYPES.regular,
        state=Interview.STATES.finished,
        application=a,
    )
    f.create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.finished,
        application=a,
    )

    qs = Application.unsafe.all()
    counts = count_applications_per_hiring_stage(qs)

    assert counts.get(APPLICATION_HIRING_STAGES.proposed) == 1
    assert counts.get(APPLICATION_HIRING_STAGES.team_is_interested) == 1
    assert counts.get(APPLICATION_HIRING_STAGES.interview_assigned) == 5
    assert counts.get(APPLICATION_HIRING_STAGES.interview_finished) == 3
