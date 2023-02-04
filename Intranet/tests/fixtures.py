# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

import datetime

import factory as factory_boy
import pytest

from django.db.models import signals
from django.conf import settings

from core import triggers
from core.tests import factory


@pytest.fixture
def reporter(db):
    return factory.ReporterFactory.create()


@pytest.fixture
def another_reporter(db, reporter):
    return factory.ReporterFactory.create()


@pytest.fixture
def vulnerability(db):
    return factory.VulnerabilityFactory.create()


@pytest.fixture
def reward(db, reporter, vulnerability):
    return factory.RewardFactory.create(reporter=reporter,
                                        vulnerability_type=vulnerability)


@pytest.fixture
def protocol(db, reward):
    protocol = factory.ProtocolFactory.create()
    reward.protocol = protocol
    with factory_boy.django.mute_signals(signals.pre_save, signals.post_save):
        reward.save()
    return protocol


@pytest.fixture
def product(db):
    return factory.ProductFactory.create()


@pytest.fixture
def payment_info(db):
    return factory.PaymentInfoFactory.create(account_holder='John Smith')


@pytest.fixture
def hall_of_fame_group(db):
    return factory.HallOfFameGroupFactory.create()


@pytest.fixture
def yandex_sponsored_badge(db):
    trigger_id = triggers.registry.get_trigger_id_by_name('yandex_sponsored')
    return factory.BadgeFactory.create(
        trigger=trigger_id,
        name_en=settings.YANDEX_SPONSORED_BADGE_NAME)


@pytest.fixture
def yandex_sponsored_reporter(db, yandex_sponsored_badge):
    reporter = factory.ReporterFactory.create()
    factory.UserBadgeFactory.create(user=reporter,
                                    badge=yandex_sponsored_badge)
    return reporter

@pytest.fixture
def former_yandex_sponsored_reporter(db, yandex_sponsored_badge):
    reporter = factory.ReporterFactory.create()
    award = factory.UserBadgeFactory.create(user=reporter,
                                            badge=yandex_sponsored_badge)
    exceeded_sponsored_duration = settings.YANDEX_SPONSORED_DURATION + 1
    award.activated -= datetime.timedelta(days=exceeded_sponsored_duration)
    award.deactivated = datetime.datetime.now() - datetime.timedelta(seconds=1)
    award.save()
    return reporter
