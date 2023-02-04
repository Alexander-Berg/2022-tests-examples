# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from core.utils import blackbox


@pytest.fixture
def blackbox_user_kwargs():
    return {'uid': 1, 'fields': {'aliases': [('1', 'login')]}}


def test_blackbox_user_empty_email(blackbox_user_kwargs):
    user = blackbox.BlackboxUser(**blackbox_user_kwargs)
    assert user.email is None
