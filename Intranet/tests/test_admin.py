# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from django.contrib.admin import sites as admin_sites
from django.core.urlresolvers import reverse

from core import models
from core.admin import custom
from core.tests import factory


def test_reward_set_default_product_action(admin_request, orphaned_reward, department_product, department_acronym):
    reward_admin = custom.RewardAdmin(models.Reward, admin_sites.site)
    queryset = models.Reward.objects.all()
    reward_admin.set_default_product(admin_request, queryset)
    orphaned_reward.refresh_from_db()
    assert orphaned_reward.product.financial_unit.acronym == department_acronym


@pytest.fixture
def department_acronym():
    return 'SEMS'


@pytest.fixture
def orphaned_reward(reporter):
    return factory.RewardFactory.create(
        reporter=reporter,
        product=None,
        department=1,
    )


@pytest.fixture
def financial_unit(department_acronym):
    unit, _ = models.FinancialUnit.objects.get_or_create(acronym=department_acronym)
    return unit


@pytest.fixture
def department_product(financial_unit):
    return factory.ProductFactory(financial_unit=financial_unit)


@pytest.fixture
def admin_request(db, rf):
    from django.middleware.common import CommonMiddleware
    from django.contrib.sessions.middleware import SessionMiddleware
    from django.contrib.messages.middleware import MessageMiddleware
    request = rf.get(reverse('admin:core_reward_changelist'))
    for middleware in (CommonMiddleware, SessionMiddleware, MessageMiddleware):
        middleware().process_request(request)
    return request
