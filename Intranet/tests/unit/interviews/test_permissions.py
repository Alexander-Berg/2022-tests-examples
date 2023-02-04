import pytest

from constance.test import override_config

from intranet.femida.src.interviews.choices import INTERVIEW_TYPES, AA_TYPES
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.interviews.workflow import InterviewWorkflow

from intranet.femida.tests import factories as f


@override_config(INTERVIEW_REVIEW_PROF_SPHERE_IDS='100500,100501')
@pytest.mark.parametrize('interview_type, prof_sphere_id, result', (
    (INTERVIEW_TYPES.screening, 100500, True),
    (INTERVIEW_TYPES.regular, 100500, True),
    (INTERVIEW_TYPES.screening, 100, False),
    (INTERVIEW_TYPES.regular, 100, False),
    (INTERVIEW_TYPES.final, 100500, False),
))
def test_send_to_review_permission(interview_type, prof_sphere_id, result):
    interviewer = f.create_user()
    interview = f.InterviewFactory(
        state=Interview.STATES.finished,
        type=interview_type,
        interviewer=interviewer,
        candidate=f.create_candidate_with_consideration(),
        application__vacancy__profession__professional_sphere__id=prof_sphere_id,
    )
    workflow = InterviewWorkflow(
        instance=interview,
        user=f.create_user(),
    )
    assert workflow.get_action('send_to_review').has_permission() is result


@pytest.mark.parametrize('perm, aa_type, result', (
    ('aa_perm', AA_TYPES.canonical, True),
    ('aa_frontend_perm', AA_TYPES.frontend, False),
    ('aa_management_perm', AA_TYPES.management, False),
    ('aa_ml_perm', AA_TYPES.ml, False),
    ('aa_mlp_perm', AA_TYPES.mlp, True),
))
def test_aa_send_to_review_permission(perm, aa_type, result):
    interview = f.InterviewFactory(
        state=Interview.STATES.finished,
        type=INTERVIEW_TYPES.aa,
        aa_type=aa_type,
        grade=3,
        comment='comment',
        interviewer=f.create_user(),
        candidate=f.create_candidate_with_consideration(),
        application=None,
    )
    workflow = InterviewWorkflow(
        instance=interview,
        user=f.create_user_with_perm(perm_codename=perm),
    )
    assert workflow.get_action('send_to_review').has_permission() is result
