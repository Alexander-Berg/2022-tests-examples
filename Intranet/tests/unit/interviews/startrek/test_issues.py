import json
import pytest

from constance.test import override_config
from unittest.mock import patch, ANY

from django.conf import settings

from intranet.femida.src.interviews.choices import INTERVIEW_TYPES, AA_TYPES
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.interviews.workflow import InterviewWorkflow

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import fake_create_aarev_issue


@pytest.mark.parametrize('interview_type, aa_type, perm', (
    ('aa', 'canonical', 'aa_perm'),
    ('aa', 'frontend', None),
    ('aa', 'management', None),
    ('aa', 'ml', None),
    ('aa', 'architecture', None),
    ('aa', 'dev_ops', None),
    ('aa', 'to', None),
    ('aa', 'sd', None),
    ('aa', 'lite', None),
    ('aa', 'easy', None),
    ('aa', 'analytic', None),
))
@patch('intranet.femida.src.interviews.startrek.issues.create_issue')
@patch('intranet.femida.src.interviews.startrek.issues.inflect_fio', lambda *x, **y: 'First Last')
@override_config(**{
    'REVIEW_AA_TYPE_PROBABILITY': json.dumps({
        'canonical': 100,
    }),
})
def test_create_aa_review_issue(mock, interview_type, aa_type, perm):
    mock.return_value = fake_create_aarev_issue()

    interviewer = f.create_aa_interviewer(aa_type, perm)
    interview = f.InterviewFactory(
        state=Interview.STATES.estimated,
        type=getattr(INTERVIEW_TYPES, interview_type, None),
        aa_type=getattr(AA_TYPES, aa_type, None),
        grade=3,
        comment='comment',
        interviewer=f.create_aa_interviewer(aa_type, perm),
        candidate=f.create_candidate_with_consideration(),
        application=None,
    )
    workflow = InterviewWorkflow(
        instance=interview,
        user=interviewer,
    )
    finish_action = workflow.get_action('finish')
    finish_action.perform()
    assert mock.called


@pytest.mark.parametrize('profession_id,reviewer', (
    (1, 'default_reviewer'),
    (2, 'frontend_reviewer'),
))
@patch('intranet.femida.src.interviews.startrek.issues.create_issue')
@patch('intranet.femida.src.interviews.startrek.issues.inflect_fio', lambda *x, **y: 'First Last')
@override_config(**{
    'REVIEW_SCREENING_PROBABILITY': 100,
})
def test_create_screening_review_issue(mock, profession_id, reviewer):
    mock.return_value = fake_create_aarev_issue()

    interviewer = f.create_user()
    f.create_interview_reviewer(username='default_reviewer')
    f.create_interview_reviewer(
        username='frontend_reviewer',
        group_id=settings.AA_TYPE_TO_GROUP_ID[AA_TYPES.frontend],
    )

    prof_sphere = f.ProfessionalSphereFactory(id=1)
    profession = f.ProfessionFactory(id=profession_id, professional_sphere=prof_sphere)
    interview = f.InterviewFactory(
        state=Interview.STATES.estimated,
        type=INTERVIEW_TYPES.screening,
        grade=3,
        comment='comment',
        interviewer=interviewer,
        candidate=f.create_candidate_with_consideration(),
        application__vacancy__professional_sphere=prof_sphere,
        application__vacancy__profession=profession,
    )
    workflow = InterviewWorkflow(
        instance=interview,
        user=interviewer,
    )
    finish_action = workflow.get_action('finish')
    finish_action.perform()

    inessential_params = ('queue', 'summary', 'description', 'followers', 'unique', 'tags')
    mock.assert_called_once_with(
        assignee=reviewer,
        interviewer=interviewer.username,
        **{k: ANY for k in inessential_params},
    )


@pytest.mark.parametrize('interview_type, prof_sphere_id, existing_count, result', (
    (INTERVIEW_TYPES.screening, 1, 2, True),
    (INTERVIEW_TYPES.screening, 1, 3, False),
    (INTERVIEW_TYPES.screening, 2, 1, False),
    (INTERVIEW_TYPES.regular, 1, 2, True),
    (INTERVIEW_TYPES.regular, 1, 3, False),
    (INTERVIEW_TYPES.regular, 2, 1, False),
    (INTERVIEW_TYPES.aa, 1, 1, False),
    (INTERVIEW_TYPES.aa, 2, 1, False),
))
@patch('intranet.femida.src.interviews.startrek.issues.create_issue')
@patch('intranet.femida.src.interviews.startrek.issues.inflect_fio', lambda *x, **y: 'First Last')
@override_config(**{
    'REVIEW_SCREENING_PROBABILITY': 0,
    'REVIEW_AA_TYPE_PROBABILITY': json.dumps({
        'canonical': 0,
    }),
})
def test_create_review_issue_for_newbie(mock, interview_type,
                                        prof_sphere_id, existing_count, result):
    mock.return_value = fake_create_aarev_issue()

    interviewer = f.create_user()
    application = f.ApplicationFactory(
        vacancy__profession__professional_sphere=f.ProfessionalSphereFactory(id=prof_sphere_id),
    )
    interview_data = {
        'state': Interview.STATES.finished,
        'type': interview_type,
        'aa_type': AA_TYPES.canonical if interview_type == INTERVIEW_TYPES.aa else None,
        'grade': 3,
        'comment': 'comment',
        'interviewer': interviewer,
        'candidate': application.candidate,
        'application': application,
        'consideration': application.consideration,
    }
    f.InterviewFactory.create_batch(existing_count, **interview_data)

    interview_data['state'] = Interview.STATES.estimated
    interview = f.InterviewFactory(**interview_data)

    workflow = InterviewWorkflow(
        instance=interview,
        user=interviewer,
    )
    finish_action = workflow.get_action('finish')
    finish_action.perform()
    assert mock.called is result
    mock.reset_mock()
