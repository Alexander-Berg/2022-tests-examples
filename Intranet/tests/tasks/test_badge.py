# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import datetime

import factory as factory_boy
import pytest

from django.db.models import signals
from django.conf import settings

from app.tasks import badge as badge_tasks
from core import models
from core.tests import factory
from core import triggers
from core.utils import dates


def test_yandex_sponsored_badge(reporter,
                                yandex_sponsored_badge,
                                many_rewards):
    badge_tasks.assign_automatic_badges()
    assert reporter.badges.count() == 1
    assert reporter.badges.first() == yandex_sponsored_badge


def test_deprive_overdue_yandex_sponsored_badge(reporter,
                                                yandex_sponsored_badge,
                                                overdue_yandex_sponsored_award):
    assert models.UserBadge.active.filter(
        user=reporter, badge=yandex_sponsored_badge).exists()
    badge_tasks.deprive_overdue_yandex_sponsored_awards(
        yandex_sponsored_badge.id)
    assert not models.UserBadge.active.filter(
        user=reporter, badge=yandex_sponsored_badge).exists()


def test_best_verified_bug(reporter, another_reporter,
                           best_verified_bug_badge,
                           reward):
    best_verified_bug_badge.award(another_reporter)
    badge_tasks.assign_automatic_badges()
    assert best_verified_bug_badge.active_holders.count() == 1
    assert reporter.badges.count() == 1
    assert reporter.badges.first() == best_verified_bug_badge


@pytest.fixture
def yandex_sponsored_badge(db):
    trigger_id = triggers.registry.get_trigger_id_by_name('yandex_sponsored')
    return factory.BadgeFactory.create(trigger=trigger_id)


@pytest.fixture
def many_rewards(db, reporter):
    rewards = factory.RewardFactory.create_batch(
        settings.YANDEX_SPONSORED_THRESHOLD, reporter=reporter)
    with factory_boy.django.mute_signals(signals.pre_save, signals.post_save):
        for reward in rewards:
            reward.payment_created = dates.today()
            reward.save()


@pytest.fixture
def overdue_yandex_sponsored_award(db, reporter, yandex_sponsored_badge):
    award = models.UserBadge.objects.create(
        user=reporter, badge=yandex_sponsored_badge)
    award.activated = dates.today() - datetime.timedelta(days=30)
    award.save()
    return award


@pytest.fixture
def best_verified_bug_badge(db):
    trigger_id = triggers.registry.get_trigger_id_by_name('best_verified_bug')
    return factory.BadgeFactory.create(trigger=trigger_id, is_exclusive=True)
