import json
import pytest

from unittest.mock import patch

from constance import config
from django.conf import settings

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.interviews.choices import INTERVIEW_TYPES
from intranet.femida.src.interviews.controllers import (
    create_assignments_from_presets,
    notify_candidate_about_preparation,
)
from intranet.femida.src.interviews.models import Assignment

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_create_assignments_by_presets(django_assert_num_queries):
    interviews_with_presets = [
        f.InterviewFactory(preset=f.create_preset_with_problems(problems_count=2)),
        f.InterviewFactory(preset=f.create_preset_with_problems(problems_count=3)),
    ]
    f.InterviewFactory()

    with django_assert_num_queries(2):
        # 1. prefetch_related_objects
        # 2. bulk_create
        create_assignments_from_presets(interviews_with_presets)

    assert Assignment.objects.all().count() == 5


@pytest.mark.parametrize('department_id, interview_type, with_email, called', (
    (settings.YANDEX_SEARCH_DEPARTMENT_ID, INTERVIEW_TYPES.screening, True, True),
    (1000000, INTERVIEW_TYPES.screening, True, False),
    (settings.YANDEX_SEARCH_DEPARTMENT_ID, INTERVIEW_TYPES.final, True, False),
    (settings.YANDEX_SEARCH_DEPARTMENT_ID, INTERVIEW_TYPES.screening, False, False),
))
@patch('intranet.femida.src.communications.controllers.send_email_to_candidate')
def test_notification_about_preparation(send_email_mock, department_id, interview_type,
                                        with_email, called):
    profession = f.ProfessionFactory()
    candidate = f.CandidateFactory()
    if with_email:
        f.CandidateContactFactory(
            candidate=candidate,
            type=CONTACT_TYPES.email,
        )
    department = f.DepartmentFactory(
        ancestors=[department_id],
    )
    vacancy = f.VacancyFactory(
        department=department,
        profession=profession,
    )
    application = f.ApplicationFactory(
        candidate=candidate,
        vacancy=vacancy,
    )
    interview = f.InterviewFactory(
        type=interview_type,
        application=application,
    )
    message_template = f.MessageTemplateFactory()
    config.SCREENING_INVITATION_TEMPLATES_BY_PROFESSION = json.dumps({
        str(profession.id): str(message_template.id),
    })
    notify_candidate_about_preparation(interview, candidate)
    assert send_email_mock.called == called
