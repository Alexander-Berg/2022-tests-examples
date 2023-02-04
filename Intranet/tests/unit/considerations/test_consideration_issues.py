import json
import pytest

from constance.test import override_config
from django.contrib.auth.models import AnonymousUser

from intranet.femida.src.candidates.choices import (
    CONSIDERATION_STATUSES,
    CONSIDERATION_EXTENDED_STATUSES,
)
from intranet.femida.src.candidates.consideration_issues.base import IssueTypesRegistry
from intranet.femida.src.candidates.consideration_issues.issue_types import (
    ExtendedStatusChangedIssueType,
    InterviewNotFinishedIssueType,
    CandidateModifiedIssueType,
)
from intranet.femida.src.candidates.helpers import blank_modify_candidate
from intranet.femida.src.candidates.models import Consideration
from intranet.femida.src.candidates.signals import consideration_status_changed
from intranet.femida.src.candidates.tasks import update_consideration_issues_task
from intranet.femida.src.interviews.choices import INTERVIEW_TYPES, INTERVIEW_STATES
from intranet.femida.src.interviews.workflow import InterviewWorkflow
from intranet.femida.src.permissions.context import context
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises, eager_task


def test_update_consideration_issues_task():
    assert IssueTypesRegistry.get_issue_types()
    with assert_not_raises():
        update_consideration_issues_task()


@eager_task(
    'intranet.femida.src.candidates.consideration_issues.signals.resolve_consideration_issue_task',
)
def test_anonymous_resolve(mocked):
    consideration = f.ConsiderationFactory()
    consideration_issue = f.ConsiderationIssueFactory(
        consideration=consideration,
        type=ExtendedStatusChangedIssueType.type_name,
    )
    context.init(AnonymousUser())
    with assert_not_raises():
        consideration_status_changed.send(sender=Consideration, consideration=consideration)
    consideration_issue.refresh_from_db()
    assert consideration_issue.is_resolved
    assert consideration_issue.resolved_by is None


@eager_task(
    'intranet.femida.src.candidates.consideration_issues.signals.resolve_consideration_issue_task',
)
def test_resolve_extended_status_changed(mocked):
    recruiter = f.create_recruiter()
    interview = f.InterviewFactory(
        type=INTERVIEW_TYPES.regular,
        state=INTERVIEW_STATES.assigned,
        consideration__extended_status=CONSIDERATION_EXTENDED_STATUSES.interview_assigned,
    )
    consideration = interview.consideration
    consideration_issue = f.ConsiderationIssueFactory(
        consideration=consideration,
        type=ExtendedStatusChangedIssueType.type_name,
    )
    context.init(recruiter)
    workflow = InterviewWorkflow(instance=interview, user=recruiter)
    workflow.perform_action('cancel')

    consideration.refresh_from_db()
    consideration_issue.refresh_from_db()
    assert consideration.extended_status == CONSIDERATION_EXTENDED_STATUSES.in_progress
    assert consideration_issue.is_resolved
    assert consideration_issue.resolved_by == recruiter


@eager_task(
    'intranet.femida.src.candidates.consideration_issues.signals.resolve_consideration_issue_task',
)
@pytest.mark.parametrize('is_interview1_in_danger, is_interview2_in_danger', (
    (False, True),
    (True, False),
    (True, True),
))
@pytest.mark.freeze_time
@override_config(INTERVIEW_FINISH_SLA=json.dumps({'any': {'danger': 5, 'warning': 3}}))
def test_resolve_interview_not_finished(mocked, is_interview1_in_danger, is_interview2_in_danger):
    prof_sphere = f.ProfessionalSphereFactory(id=100500)  # чтобы не создавать review тикет
    danger_threshold = InterviewNotFinishedIssueType.thresholds['any']['danger']
    event_time1 = (
        shifted_now(days=-danger_threshold)
        if is_interview1_in_danger
        else shifted_now(minutes=-30)
    )
    event_time2 = (
        shifted_now(days=-danger_threshold)
        if is_interview2_in_danger
        else shifted_now(minutes=-30)
    )
    interview1 = f.InterviewFactory(
        type=INTERVIEW_TYPES.regular,
        state=INTERVIEW_STATES.estimated,
        event_start_time=event_time1,
        application__vacancy__professional_sphere=prof_sphere,
    )
    interview2 = f.InterviewFactory(
        type=INTERVIEW_TYPES.regular,
        state=INTERVIEW_STATES.estimated,
        consideration=interview1.consideration,
        event_start_time=event_time2,
        application__vacancy__professional_sphere=prof_sphere,
    )
    consideration_issue = f.ConsiderationIssueFactory(
        consideration=interview1.consideration,
        type=InterviewNotFinishedIssueType.type_name,
    )
    interviewer1 = interview1.interviewer
    interviewer2 = interview2.interviewer

    workflow = InterviewWorkflow(instance=interview1, user=interviewer1)
    workflow.perform_action('finish')
    consideration_issue.refresh_from_db()
    assert consideration_issue.is_resolved is not is_interview2_in_danger
    assert consideration_issue.resolved_by == (None if is_interview2_in_danger else interviewer1)
    if is_interview2_in_danger:
        workflow = InterviewWorkflow(instance=interview2, user=interviewer2)
        workflow.perform_action('finish')
        consideration_issue.refresh_from_db()
        assert consideration_issue.is_resolved
        assert consideration_issue.resolved_by == interviewer2


@eager_task(
    'intranet.femida.src.candidates.consideration_issues.signals.resolve_consideration_issue_task',
)
@override_config(CANDIDATE_MODIFIED_SLA=json.dumps({'any': {'danger': 30, 'warning': 10}}))
def test_resolve_candidate_modified(mocked):
    recruiter = f.create_recruiter()
    consideration = f.ConsiderationFactory(state=CONSIDERATION_STATUSES.in_progress)
    consideration_issue = f.ConsiderationIssueFactory(
        consideration=consideration,
        type=CandidateModifiedIssueType.type_name,
    )
    context.init(recruiter)
    blank_modify_candidate(consideration.candidate)
    consideration_issue.refresh_from_db()
    assert consideration_issue.is_resolved
    assert consideration_issue.resolved_by == recruiter
