import pytest
from unittest.mock import MagicMock

import random

from staff.gap.workflows.startrek_mixin import change_gap_ticket_status


@pytest.mark.parametrize(
    'action',
    ['approve', 'cancel', 'reopen', 'will_not_fix'],
)
def test_perform_gap_action_skip_action(action: str):
    issue_comment = (None, action)
    issue_key = f'issue_key_{random.random()}'
    tag = f'tag_{random.random()}'

    startrek_ctl = MagicMock()

    change_gap_ticket_status(random.randint(3000, 6000), startrek_ctl, issue_comment, issue_key, tag)

    startrek_ctl.change_state.assert_not_called()


def test_perform_gap_action():
    action = f'action_{random.random()}'
    issue_comment = (None, action)
    issue_key = f'issue_key_{random.random()}'
    tag = f'tag_{random.random()}'

    startrek_ctl = MagicMock()

    change_gap_ticket_status(random.randint(3000, 6000), startrek_ctl, issue_comment, issue_key, tag)

    startrek_ctl.change_state.assert_called_once_with(action)
