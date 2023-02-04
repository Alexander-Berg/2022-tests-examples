# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import factory
import pytest

from django.db.models import signals

from app.tasks import hall_of_fame
from core import models
from core.utils import dates


def test_hall_of_fame_member_is_added(previous_month_reward,
                                      hall_of_fame_group):
    hall_of_fame.generate_for_previous_month()
    assert previous_month_reward.reporter in hall_of_fame_group.members.all()
    

def test_discarded_reward_is_ignored(discarded_reward,
                                     hall_of_fame_group):
    hall_of_fame.generate_for_previous_month()
    assert not hall_of_fame_group.members.exists()


@pytest.fixture
def discarded_reward(db, reward):
    reward.status = models.Reward.ST_DISCARDED
    with factory.django.mute_signals(signals.pre_save, signals.post_save):
        reward.save()
    return reward


@pytest.fixture
def previous_month_reward(db, reward):
    reward.payment_created = dates.get_previous_month()
    with factory.django.mute_signals(signals.pre_save, signals.post_save):
        reward.save()
    return reward


@pytest.fixture
def hall_of_fame_group(db):
    previous_month = dates.get_previous_month()
    return models.HallOfFameGroup.objects.create(
        year=previous_month.year, month=previous_month.month)
