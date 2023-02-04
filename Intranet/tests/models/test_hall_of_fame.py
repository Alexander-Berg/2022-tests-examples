# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from core import models
from core.tests import factory
from core.utils import dates


def test_hall_of_fame_published_manager(published_hall_of_fame_group):
    published = models.HallOfFameGroup.published
    previous_month = dates.get_previous_month()
    assert published.count() == 1
    assert published.last_year() == previous_month.year
    assert published.last_month() == previous_month.month


def test_hall_of_fame_publish(hall_of_fame_group, reporter):
    hall_of_fame_group.publish()
    assert not hall_of_fame_group.is_published
    hall_of_fame_group.add_member(reporter)
    hall_of_fame_group.publish()
    assert hall_of_fame_group.is_published


@pytest.fixture
def published_hall_of_fame_group(db):
    return factory.HallOfFameGroupFactory.create(is_published=True)
