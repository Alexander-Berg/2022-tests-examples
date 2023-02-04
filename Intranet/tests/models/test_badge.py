# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from core import exceptions
from core import models
from core.tests import factory


def test_badge_deprive_exclusive_holder(taken_exclusive_badge):
    taken_exclusive_badge.deprive_exclusive_holder()
    assert taken_exclusive_badge.active_holders.count() == 0
    assert taken_exclusive_badge.inactive_holders.count() == 1


def test_badge_exclusive_holder_changes(free_exclusive_badge,
                                        taken_exclusive_badge,
                                        reporter,
                                        another_reporter):
    assert not free_exclusive_badge.exclusive_holder_changes(reporter)
    assert not taken_exclusive_badge.exclusive_holder_changes(reporter)
    assert taken_exclusive_badge.exclusive_holder_changes(another_reporter)


def test_userbadge_validation(taken_exclusive_badge, another_reporter):
    with pytest.raises(exceptions.ModelException) as exc:
        models.UserBadge.objects.create(
            badge=taken_exclusive_badge, user=another_reporter)


@pytest.fixture
def free_exclusive_badge(db):
    return factory.BadgeFactory.create(is_exclusive=True)


@pytest.fixture
def taken_exclusive_badge(db, reporter):
    exclusive_badge = factory.BadgeFactory.create(is_exclusive=True)
    models.UserBadge.objects.create(user=reporter, badge=exclusive_badge)
    return exclusive_badge
