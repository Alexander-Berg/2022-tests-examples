# coding: utf-8
import pytest

from review.core.logic.bulk.helpers import extract_login_from_reviewers


def test_extract_login_from_reviewers_is_stable():
    received = extract_login_from_reviewers([
        {'login': 'user1'},
        [
            {'login': 'user3'},
            {'login': 'user2'},
        ],
    ])
    expected = ["user1", ["user2", "user3"]]
    assert received == expected
