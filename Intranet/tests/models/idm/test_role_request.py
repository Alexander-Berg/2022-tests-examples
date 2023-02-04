# coding: utf-8


import mock
import pytest
import json
import re

from django.conf import settings
from ids.exceptions import BackendError
from django.conf import settings

from idm.core.models import Role, RoleRequest, Action
from idm.core.constants.action import ACTION
from idm.tests.templates.utils import render_template
from idm.tests.utils import set_workflow, mock_ids_repo, MockedTrackerIssue, assert_contains
from idm.notification.utils import get_issues_repository
from idm.api.exceptions import InternalAPIError
from idm.utils.curl import Response

pytestmark = pytest.mark.django_db


def test_get_approvers(simple_system, arda_users):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = ["legolas", "frodo"]')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role_request = RoleRequest.objects.get(role=role)
    assert set(role_request.get_approvers()) == {frodo, legolas}


def test_make_description_for_st_ticket(complex_system, arda_users):
    frodo = arda_users.frodo
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=complex_system,
        comment='Wanna be admin!',
        data={'project': 'subs', 'role': 'developer'},
        fields_data={'passport-login': 'frodo', 'field_1': '1'},
    )
    role_request = \
        RoleRequest.objects.select_related('requester', 'role__node', 'role__system', 'role__user').get(role=role)

    assert role_request.make_description_for_st_ticket() == render_template(
       'emails/approve_role.txt',
       {'user': frodo, 'requester': frodo, 'role': role, 'for_email': False, 'comment': role_request.comment},
   )


def test_make_summary_for_st_ticket(simple_system, arda_users):
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role_request = RoleRequest.objects.select_related('role__user', 'role__node', 'role__system').get(role=role)
    summary = role_request.make_summary_for_st_ticket()
    assert summary == 'Подтверждение роли. Simple система.'


def test_create_or_get_st_issue_get(simple_system, arda_users):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role_request = RoleRequest.objects.get(role=role)
    role_request.st_issue = 'abcd'
    role_request.save(update_fields=['st_issue'])
    st_issue = role_request.get_or_create_st_issue(legolas)
    assert st_issue == 'abcd'


@pytest.mark.parametrize('requester_is_none', [True, False])
def test_get_or_create_st_issue_create(simple_system, arda_users, requester_is_none):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role_request = RoleRequest.objects.select_related('requester', 'role__node', 'role__system', 'role__user').get(role=role)
    with mock_ids_repo('startrek2') as issues_repo:
        issues_repo.create.return_value = MockedTrackerIssue('abcd')
        initial_actions_count = Action.objects.count()

        subject_responsibles = list(role.get_subject().get_responsibles(with_heads=True))
        approvers = list(role_request.get_approvers())
        responsibles_for_system = list(role.system.get_responsibles())
        access = (
                [frodo]
                + subject_responsibles
                + approvers
                + responsibles_for_system
        )
        access = [user.username for user in access]
        description = role_request.make_description_for_st_ticket()
        summary = role_request.make_summary_for_st_ticket()

        issue = role_request.get_or_create_st_issue(legolas)
        assert mock.call(
            queue=settings.IDM_ST_QUEUE_FOR_DISCUSSION,
            access=access,
            assignee='frodo',
            followers=['frodo', 'legolas'],
            description=description,
            summary=summary,
        ) in issues_repo.create.call_args_list
        assert Action.objects.count() == initial_actions_count + 1
        action = Action.objects.get(action=ACTION.START_DISCUSSION)
        assert action is not None
        assert settings.IDM_ST_BASE_URL + issue in action.comment


def test_get_or_create_st_issue_create_with_bad_issue(simple_system, arda_users):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role_request = RoleRequest.objects.select_related('requester', 'role__system', 'role__node').get(role=role)
    with mock_ids_repo('startrek2') as issues_repo:
        def mocked_create(*args, **kwargs):
            raise BackendError(response=Response(500, 'abcd', {}, ''))

        issues_repo.create = mocked_create
        with pytest.raises(InternalAPIError, match='^abcd$'):
            role_request.get_or_create_st_issue(legolas)


def test_close_issue(simple_system, arda_users):
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["legolas"]')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    role_request = RoleRequest.objects.get(role=role)

    with mock_ids_repo('startrek2') as issues_repo:
        role_request.st_issue = 'abcd'
        mocked_issue = MockedTrackerIssue('abcd')
        issues_repo.get_one.return_value = mocked_issue
        role_request.close_issue()
        assert mock.call(
            {'id': 'abcd'},
        ) in issues_repo.get_one.call_args_list
        assert mock.call(
            resolution='fixed'
        ) in mocked_issue.execute.call_args_list
