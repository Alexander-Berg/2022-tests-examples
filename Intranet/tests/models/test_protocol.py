# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import factory as factory_boy
import pytest
from django.db.models import signals

from core.tests import factory
from core.utils import dates


def test_rewards_in_protocol(reward, protocol):
    assert protocol.rewards_total == 1
    assert protocol.rewards_processed == 0
    reward.status = reward.ST_FINISHED
    with factory_boy.django.mute_signals(signals.pre_save, signals.post_save):
        reward.save()
    assert protocol.rewards_processed == 1


@pytest.fixture
def second_protocol(db, protocol, reporter):
    protocol = factory.ProtocolFactory.create()
    reward = factory.RewardFactory.create()
    reward.payment_created = dates.yesterday()
    reward.protocol = protocol
    with factory_boy.django.mute_signals(signals.pre_save, signals.post_save):
        reward.save()
    return protocol
