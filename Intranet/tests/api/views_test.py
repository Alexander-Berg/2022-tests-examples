from mock import patch, Mock
import pytest

from django.core.urlresolvers import reverse

from staff.gap.api.exceptions import NoGapWithThisIssueKeyError
from staff.gap.api.views.gap_state_views import gap_confirm_by_issue


@pytest.mark.django_db
def test_gap_confirm_by_issue(rf, ya_user):
    issue_key = 'ABC-123'
    gap_id = '123'
    mocked_get_gap_id_by_issue_key = Mock(return_value=gap_id)
    request = rf.post(reverse('gap:api-gap-confirm-by-issue', kwargs={'issue_key': issue_key}))
    request.user = ya_user

    with patch('staff.gap.api.views.gap_state_views.gap_action') as patched_gap_action:
        with patch('staff.gap.api.views.gap_state_views.get_gap_id_by_issue_key', mocked_get_gap_id_by_issue_key):

            gap_confirm_by_issue(request, issue_key)

            mocked_get_gap_id_by_issue_key.assert_called_once_with(issue_key)
            patched_gap_action.assert_called_once_with(ya_user, gap_id, 'confirm_gap')


@pytest.mark.django_db
def test_gap_confirm_by_issue_not_found(rf, ya_user):
    issue_key = 'ABC-123'
    mocked_get_gap_id_by_issue_key = Mock(side_effect=NoGapWithThisIssueKeyError)
    request = rf.post(reverse('gap:api-gap-confirm-by-issue', kwargs={'issue_key': issue_key}))
    request.user = ya_user

    with patch('staff.gap.api.views.gap_state_views.gap_action') as patched_gap_action:
        with patch('staff.gap.api.views.gap_state_views.get_gap_id_by_issue_key', mocked_get_gap_id_by_issue_key):

            response = gap_confirm_by_issue(request, issue_key)

            assert response.status_code == 404
            mocked_get_gap_id_by_issue_key.assert_called_once_with(issue_key)
            patched_gap_action.assert_not_called()
