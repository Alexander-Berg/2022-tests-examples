import pytest

from constance import config

from intranet.femida.src.candidates.models import Consideration
from intranet.femida.src.interviews.choices import INTERVIEW_TYPES
from intranet.femida.src.interviews.helpers import InterviewVisibilityHelper
from intranet.femida.src.interviews.models import Interview

from intranet.femida.tests import factories as f


C_STATES = Consideration.STATES
I_STATES = Interview.STATES
I_TYPES = INTERVIEW_TYPES


def _get_old_interviewer(candidate):
    """
    Получаем юзера, который точно имеет доступ к кандидату.
    Это нужно, чтобы проверять именно видимость результатов секций, а не общие права.
    """
    cons = f.ConsiderationFactory.create(
        candidate=candidate,
        state=Consideration.STATES.archived,
    )
    interview = f.create_interview(consideration=cons, state=Interview.STATES.finished)
    return interview.interviewer


@pytest.mark.parametrize('cons_state, interview_state, interview_type, interview_role, result', (
    (C_STATES.in_progress, I_STATES.assigned, I_TYPES.regular, 'recruiter', True),
    (C_STATES.in_progress, I_STATES.assigned, I_TYPES.regular, 'interviewer', True),
    (C_STATES.in_progress, I_STATES.assigned, I_TYPES.screening, 'someone', False),
    (C_STATES.archived, I_STATES.finished, I_TYPES.regular, 'someone', True),
    (C_STATES.in_progress, I_STATES.finished, I_TYPES.screening, 'someone', True),
))
def test_active_consideration(cons_state, interview_state, interview_type, interview_role, result):
    consideration = f.ConsiderationFactory.create(state=cons_state)
    interview = f.create_interview(
        consideration=consideration,
        state=interview_state,
        type=interview_type,
    )
    if interview_role == 'recruiter':
        user = f.create_recruiter()
    elif interview_role == 'interviewer':
        user = interview.interviewer
    else:
        user = _get_old_interviewer(interview.candidate)

    helper = InterviewVisibilityHelper(user, interview.consideration)
    assert helper.is_visible(interview) == result


def test_finished_unbiased_interviews():
    consideration = f.ConsiderationFactory.create(state=C_STATES.in_progress)
    interview1 = f.create_interview(
        consideration=consideration,
        state=I_STATES.finished,
        type=I_TYPES.regular,
    )
    interview2 = f.create_interview(
        consideration=consideration,
        state=I_STATES.finished,
        type=I_TYPES.aa,
    )
    user = _get_old_interviewer(interview1.candidate)

    helper = InterviewVisibilityHelper(user, consideration)
    assert helper.is_visible(interview1) is True
    assert helper.is_visible(interview2) is True

    helper = InterviewVisibilityHelper(interview1.interviewer, consideration)
    assert helper.is_visible(interview1) is True
    assert helper.is_visible(interview2) is True

    helper = InterviewVisibilityHelper(interview2.interviewer, consideration)
    assert helper.is_visible(interview1) is True
    assert helper.is_visible(interview2) is True


def test_unfinished_unbiased_interviews():
    vacancy = f.create_active_vacancy()
    consideration = f.ConsiderationFactory.create(state=C_STATES.in_progress)
    f.create_application(
        vacancy=vacancy,
        candidate=consideration.candidate,
        consideration=consideration,
    )
    finished = f.create_interview(
        consideration=consideration,
        state=I_STATES.finished,
        type=I_TYPES.regular,
    )
    assigned = f.create_interview(
        consideration=consideration,
        state=I_STATES.assigned,
        type=I_TYPES.aa,
    )
    another_finished = f.create_interview(
        consideration=consideration,
        state=I_STATES.finished,
        type=I_TYPES.aa,
    )
    repeated_assigned = f.create_interview(
        consideration=consideration,
        state=I_STATES.assigned,
        type=I_TYPES.regular,
        interviewer=another_finished.interviewer,
    )
    assigned_final = f.create_interview(
        consideration=consideration,
        state=I_STATES.assigned,
        type=I_TYPES.final,
    )
    user = _get_old_interviewer(finished.candidate)

    helper = InterviewVisibilityHelper(user, consideration)
    assert helper.is_visible(finished) is False  # Нет назначенных - не видит завершенные
    assert helper.is_visible(assigned) is False  # Не видит незавершенные

    helper = InterviewVisibilityHelper(finished.interviewer, consideration)
    assert helper.is_visible(finished) is True  # Видит свою завершенную
    assert helper.is_visible(another_finished) is True  # Видит чужую завершенную
    assert helper.is_visible(assigned) is False  # Не видит незавершенные

    helper = InterviewVisibilityHelper(assigned.interviewer, consideration)
    assert helper.is_visible(finished) is False  # Не видит, т.к. есть своя незавершенная
    assert helper.is_visible(assigned) is True  # Своя секция

    helper = InterviewVisibilityHelper(repeated_assigned.interviewer, consideration)
    # Есть завершенная, но есть и незавершенная, поэтому не видит
    assert helper.is_visible(finished) is False
    assert helper.is_visible(another_finished) is True  # Свою завершенную видит

    helper = InterviewVisibilityHelper(assigned_final.interviewer, consideration)
    assert helper.is_visible(finished) is True  # Видит все завершенные, так как назначен финал
    assert helper.is_visible(assigned) is False  # Чужие назначенные не видит

    # Проверка видимости секций для нанимающих

    config.INTERVIEW_VISIBILITY_FOR_HIRING_TEAM = 'default'
    helper = InterviewVisibilityHelper(vacancy.hiring_manager, consideration)
    assert helper.is_visible(finished) is False  # Нет назначенных - не видит завершенные
    assert helper.is_visible(assigned) is False  # Не видит незавершенные

    config.INTERVIEW_VISIBILITY_FOR_HIRING_TEAM = 'visible_if_no_interview'
    helper = InterviewVisibilityHelper(vacancy.hiring_manager, consideration)
    assert helper.is_visible(finished) is True  # Видит завершенные
    assert helper.is_visible(assigned) is False  # Не видит незавершенные

    f.create_interview(consideration=consideration, interviewer=vacancy.hiring_manager)
    helper = InterviewVisibilityHelper(vacancy.hiring_manager, consideration)
    assert helper.is_visible(finished) is False  # Не видит, т.к. есть своя незавершенная
    assert helper.is_visible(assigned) is False  # Не видит незавершенные

    config.INTERVIEW_VISIBILITY_FOR_HIRING_TEAM = 'visible'
    helper = InterviewVisibilityHelper(vacancy.hiring_manager, consideration)
    assert helper.is_visible(finished) is True  # Видит завершенные
    assert helper.is_visible(assigned) is False  # Не видит незавершенные
