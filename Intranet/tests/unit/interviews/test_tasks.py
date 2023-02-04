import pytest

from django.conf import settings
from django.utils import timezone
from unittest.mock import patch

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.interviews.choices import INTERVIEW_TYPES, AA_TYPES
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.interviews.tasks import send_interview_survey_task, _get_is_full_survey
from intranet.femida.src.offers.choices import FORM_TYPES, OFFER_STATUSES
from intranet.femida.src.vacancies.choices import VACANCY_TYPES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.fixture
def full_survey_interviews():
    prof_sphere_dev = f.ProfessionalSphereFactory(id=settings.DEVELOPMENT_PROF_SPHERE_ID)
    application_dev = f.ApplicationFactory(
        vacancy__profession__professional_sphere=prof_sphere_dev,
    )
    prof_sphere_other = f.ProfessionalSphereFactory(id=settings.DEVELOPMENT_PROF_SPHERE_ID + 1)
    application_other = f.ApplicationFactory(
        vacancy__profession__professional_sphere=prof_sphere_other,
    )
    return [
        # в разработку
        f.create_interview(
            application=application_dev,
            state=Interview.STATES.finished,
        ),
        # разработческое AA
        f.create_interview(
            type=INTERVIEW_TYPES.aa,
            aa_type=AA_TYPES.canonical,
            application=application_other,
            state=Interview.STATES.finished,
        ),
    ]


@pytest.mark.parametrize('is_full_survey', [True, False])
@patch('intranet.femida.src.interviews.tasks.send_email.delay')
def test_send_interview_survey_task(mocked_send_email, is_full_survey):
    consideration = f.ConsiderationFactory()
    interview = f.create_interview(
        consideration=consideration,
        event_start_time=timezone.now(),
        state=Interview.STATES.finished,
    )
    f.AssignmentFactory(interview=interview)
    f.AssignmentFactory(interview=interview)
    f.create_interview(
        consideration=consideration,
        state=Interview.STATES.finished,
    )
    f.CandidateContactFactory(candidate_id=consideration.candidate_id, type=CONTACT_TYPES.email)

    patched_full_survey = patch(
        target='intranet.femida.src.interviews.tasks._get_is_full_survey',
        return_value=is_full_survey,
    )
    with patched_full_survey:
        send_interview_survey_task(consideration.id)
    mocked_send_email.assert_called_once()


@pytest.mark.parametrize('offer_form_type, offer_status, is_international_vacancy, is_sent', (
    (FORM_TYPES.russian, OFFER_STATUSES.closed, True, False),
    (FORM_TYPES.russian, OFFER_STATUSES.closed, False, True),
    (FORM_TYPES.international, OFFER_STATUSES.closed, False, False),
    (FORM_TYPES.international, OFFER_STATUSES.deleted, False, True),
))
@patch('intranet.femida.src.interviews.tasks.send_email.delay')
def test_send_interview_survey_task_international(mocked_send_email, offer_form_type, offer_status,
                                                  is_international_vacancy, is_sent):
    vacancy = f.VacancyFactory(geography_international=is_international_vacancy)
    application = f.ApplicationFactory(vacancy=vacancy)
    f.create_interview(
        application=application,
        state=Interview.STATES.finished,
    )
    f.create_offer(
        application=application,
        form_type=offer_form_type,
        status=offer_status,
    )
    f.CandidateContactFactory(candidate_id=application.candidate_id, type=CONTACT_TYPES.email)

    send_interview_survey_task(application.consideration_id)
    assert mocked_send_email.called is is_sent


@patch('intranet.femida.src.interviews.tasks.send_email.delay')
def test_send_interview_survey_task_no_corresponding_interviews(mocked_send_email):
    consideration = f.ConsiderationFactory()
    # не finished
    f.create_interview(
        consideration=consideration,
        state=Interview.STATES.cancelled,
    )
    # screening
    f.create_interview(
        consideration=consideration,
        type=INTERVIEW_TYPES.hr_screening,
    )
    # Не тот consideration
    f.create_interview(state=Interview.STATES.finished)

    send_interview_survey_task(consideration.id)
    mocked_send_email.assert_not_called()


def test_is_full_survey(full_survey_interviews):
    assert _get_is_full_survey(full_survey_interviews)


def test_short_survey_if_any_false_case(full_survey_interviews):
    short_survey_interview = f.create_interview(
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.management,
        state=Interview.STATES.finished,
    )
    full_survey_interviews.append(short_survey_interview)
    assert not _get_is_full_survey(full_survey_interviews)


def test_short_survey_aa_management():
    i = f.create_interview(
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.management,
        state=Interview.STATES.finished,
    )
    assert not _get_is_full_survey([i])


def test_short_survey_internship():
    a = f.ApplicationFactory(
        vacancy__type=VACANCY_TYPES.internship,
    )
    interview = f.create_interview(
        application=a,
        state=Interview.STATES.finished,
    )
    assert not _get_is_full_survey([interview])


def test_short_survey_no_department():
    prof_sphere = f.ProfessionalSphereFactory(id=settings.DEVELOPMENT_PROF_SPHERE_ID)
    a = f.ApplicationFactory(
        vacancy__profession__professional_sphere=prof_sphere,
        vacancy__department=None,
    )
    interview = f.create_interview(
        application=a,
        state=Interview.STATES.finished,
    )
    assert not _get_is_full_survey([interview])


def test_short_survey_not_development_sphere():
    prof_sphere = f.ProfessionalSphereFactory(id=settings.DEFAULT_PROF_SPHERE_ID)
    application = f.ApplicationFactory(vacancy__profession__professional_sphere=prof_sphere)
    interview = f.create_interview(
        application=application,
        state=Interview.STATES.finished,
    )
    assert not _get_is_full_survey([interview])
