import pytest
from unittest.mock import patch

from random import random

from staff.gap.controllers.startrek import StartrekCtl, StartrekCtlException


def test_get_issue_no_issue_returned():
    issue_key = f'issue_key-{random()}'

    with pytest.raises(StartrekCtlException):
        with patch('staff.gap.controllers.startrek.get_issue', return_value=None):
            target = StartrekCtl(issue_key=issue_key)

            assert target.issue
