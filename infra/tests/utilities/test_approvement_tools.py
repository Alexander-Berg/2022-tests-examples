import dataclasses
from unittest.mock import Mock

import pytest

from walle.clients import ok, startrek
from walle.util.approvement_tools import (
    ApproveClient,
    ApprovalTicketResolution,
    CreateStartrekTicketRequest,
    CreateOkApprovementRequest,
    CreateStartrekCommentRequest,
)


class TestApprovementTools:

    OK_APPROVEMENT_UUID_TEMPLATE = "approvement-uuid-for-{}"
    STARTREK_COMMENT_ID: ApproveClient.TStartrekCommentId = 42

    @pytest.mark.parametrize(
        "client_request, expected_result",
        [
            (
                CreateStartrekTicketRequest(
                    queue="FOO",
                    type="issue-type",
                    summary="summary",
                    tags=("bar", "baz"),
                    parent="FOOPARENT-1",
                    description="test",
                ),
                "FOO-1",
            ),
        ],
    )
    def test_create_startrek_ticket(self, mp, client_request, expected_result):
        startrek_client_mock = self._get_startrek_client_mock(create_issue_request=client_request)
        mp.function(startrek.get_client, return_value=startrek_client_mock)

        approve_client = ApproveClient(startrek_client=startrek.get_client())
        assert approve_client.create_startrek_ticket(client_request) == expected_result
        startrek_client_mock.create_issue.assert_called_once_with(issue_params=dataclasses.asdict(client_request))

    @pytest.mark.parametrize(
        "client_request, expected_result",
        [
            (
                CreateOkApprovementRequest(
                    ticket_key="FOO-1",
                    text="create-ok-approvement-text",
                    author="author-login",
                    approvers=("approver-login-foo", "approver-login-bar"),
                    groups=("group-login-foo", "group-login-bar"),
                ),
                OK_APPROVEMENT_UUID_TEMPLATE.format("FOO-1"),
            ),
        ],
    )
    def test_create_ok_approvement(self, mp, client_request, expected_result):
        ok_client_mock = self._get_ok_client_mock(create_approvement_request=client_request)
        mp.function(ok.get_client, return_value=ok_client_mock)

        approve_client = ApproveClient(ok_client=ok.get_client())
        assert approve_client.create_ok_approvement(client_request) == expected_result
        ok_client_mock.create_approvement.assert_called_once_with(
            ticket_key=client_request.ticket_key,
            text=client_request.text,
            author=client_request.author,
            approvers=client_request.approvers,
            groups=client_request.groups,
        )

    @pytest.mark.parametrize(
        "client_request, expected_result",
        [
            (
                CreateStartrekCommentRequest(
                    issue_id="FOO-1",
                    text="create-startrek-comment-text",
                    summonees=("sommonee-login-foo", "sommonee-login-bar"),
                ),
                STARTREK_COMMENT_ID,
            )
        ],
    )
    def test_create_startrek_comment(self, mp, client_request, expected_result):
        startrek_client_mock = self._get_startrek_client_mock(create_comment_request=client_request)
        mp.function(startrek.get_client, return_value=startrek_client_mock)

        approve_client = ApproveClient(startrek_client=startrek.get_client())
        assert approve_client.create_startrek_comment(client_request) == expected_result
        startrek_client_mock.add_comment.assert_called_once_with(
            issue_id=client_request.issue_id, text=client_request.text, summonees=list(client_request.summonees)
        )

    @pytest.mark.parametrize(
        "client_request, expected_result",
        [
            (
                OK_APPROVEMENT_UUID_TEMPLATE.format("FOO-1"),
                (ok.ApprovementStatus.IN_PROGRESS, ok.ApprovementResolution.NO_RESOLUTION),
            )
        ],
    )
    def test_get_ok_approvement_status_resolution(self, mp, client_request, expected_result):
        ok_client_mock = self._get_ok_client_mock(get_approvement_status_resolution_request=client_request)
        mp.function(ok.get_client, return_value=ok_client_mock)

        approve_client = ApproveClient(ok_client=ok.get_client())
        assert approve_client.get_ok_approvement_status_resolution(client_request) == expected_result
        ok_client_mock.get_approvement.assert_called_once_with(uuid=client_request)

    @pytest.mark.parametrize(
        "client_request, current_ticket_status, close_ticket_must_be_called",
        [
            (("FOO-1", ApprovalTicketResolution.SUCCESSFUL), startrek.TicketStatus.OPEN, True),
            (("FOO-1", ApprovalTicketResolution.SUCCESSFUL), startrek.TicketStatus.CLOSED, False),
        ],
    )
    def test_close_startrek_ticket(self, mp, client_request, current_ticket_status, close_ticket_must_be_called):
        startrek_client_mock = self._get_startrek_client_mock(
            close_ticket_request=client_request, current_ticket_status=current_ticket_status
        )
        mp.function(startrek.get_client, return_value=startrek_client_mock)

        startrek_ticket_id, resolution = client_request

        approve_client = ApproveClient(startrek_client=startrek.get_client())
        assert approve_client.close_startrek_ticket(startrek_ticket_id, resolution) is None
        startrek_client_mock.get_issue.assert_called_once_with(issue_id=startrek_ticket_id)

        if close_ticket_must_be_called:
            startrek_client_mock.close_issue.assert_called_once_with(
                issue_id=startrek_ticket_id, transition="closed", resolution=resolution
            )

    @pytest.mark.parametrize(
        "client_request, current_approvement_status, close_approvement_must_be_called",
        [
            (OK_APPROVEMENT_UUID_TEMPLATE.format("FOO-1"), ok.ApprovementStatus.IN_PROGRESS, True),
            (OK_APPROVEMENT_UUID_TEMPLATE.format("FOO-1"), ok.ApprovementStatus.CLOSED, False),
        ],
    )
    def test_close_ok_approvement(
        self, mp, client_request, current_approvement_status, close_approvement_must_be_called
    ):
        ok_client_mock = self._get_ok_client_mock(
            close_approvement_request=client_request, current_approvement_status=current_approvement_status
        )
        mp.function(ok.get_client, return_value=ok_client_mock)

        approve_client = ApproveClient(ok_client=ok.get_client())
        assert approve_client.close_ok_approvement(client_request) is None
        ok_client_mock.get_approvement.assert_called_once_with(uuid=client_request)

        if close_approvement_must_be_called:
            ok_client_mock.close_approvement.assert_called_once_with(uuid=client_request)

    def _get_ok_client_mock(
        self,
        create_approvement_request: CreateOkApprovementRequest = None,
        get_approvement_status_resolution_request: ApproveClient.TOkUuid = None,
        close_approvement_request: ApproveClient.TOkUuid = None,
        current_approvement_status: str = None,
    ) -> Mock:
        ok_client_mock = Mock()

        base_approvement_dict = {  # These are required arguments to create 'ok.Approvement()' object.
            "approvement_id": None,
            "text": None,
            "stages": [],
        }

        if create_approvement_request is not None:
            approvement_dict = base_approvement_dict | {
                "uuid": self.OK_APPROVEMENT_UUID_TEMPLATE.format(create_approvement_request.ticket_key)
            }
            ok_client_mock.create_approvement.return_value = ok.Approvement(**approvement_dict)

        if get_approvement_status_resolution_request is not None:
            approvement_dict = base_approvement_dict | {
                "status": ok.ApprovementStatus.IN_PROGRESS,
                "resolution": ok.ApprovementResolution.NO_RESOLUTION,
            }
            ok_client_mock.get_approvement.return_value = ok.Approvement(**approvement_dict)

        if close_approvement_request is not None:
            approvement_dict = base_approvement_dict | {"status": current_approvement_status}
            ok_client_mock.get_approvement.return_value = ok.Approvement(**approvement_dict)
            ok_client_mock.close_approvement.return_value = None  # Not important.

        return ok_client_mock

    def _get_startrek_client_mock(
        self,
        create_issue_request: CreateStartrekTicketRequest = None,
        create_comment_request: CreateStartrekCommentRequest = None,
        close_ticket_request: (ApproveClient.TStartrekTicketKey, ApprovalTicketResolution) = None,
        current_ticket_status: str = None,
    ) -> Mock:
        startrek_client_mock = Mock()

        if create_issue_request is not None:
            startrek_client_mock.create_issue.return_value = {"key": f"{create_issue_request.queue}-1"}

        if create_comment_request is not None:
            startrek_client_mock.add_comment.return_value = {"id": self.STARTREK_COMMENT_ID}

        if close_ticket_request is not None:
            startrek_client_mock.get_issue.return_value = {"status": {"key": current_ticket_status}}
            startrek_client_mock.close_issue.return_value = None  # Not important.

        return startrek_client_mock
