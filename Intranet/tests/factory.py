# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import unicode_literals

import factory

from django.db.models import signals
from factory import fuzzy

from core import models
from core.utils import dates


class ReporterFactory(factory.DjangoModelFactory):

    class Meta:
        model = models.Reporter

    uid = factory.Sequence(int)


class PaymentInfoFactory(factory.DjangoModelFactory):
    reporter = factory.SubFactory(ReporterFactory)

    class Meta:
        model = models.PaymentInfo


class NewPaymentInfoFactory(factory.DjangoModelFactory):
    reporter = factory.SubFactory(ReporterFactory)

    class Meta:
        model = models.PaymentInfo


@factory.django.mute_signals(signals.pre_save, signals.post_save)
class RewardFactory(factory.DjangoModelFactory):
    staff_uid = 1
    ticket_created = dates.get_previous_month()
    reporter = factory.SubFactory(ReporterFactory)

    class Meta:
        model = models.Reward


class ProtocolFactory(factory.DjangoModelFactory):
    staff_uid = fuzzy.FuzzyInteger(1)

    class Meta:
        model = models.Protocol


class VulnerabilityFactory(factory.DjangoModelFactory):
    points = fuzzy.FuzzyInteger(1)

    class Meta:
        model = models.Vulnerability


class FinancialUnitFactory(factory.DjangoModelFactory):

    class Meta:
        model = models.FinancialUnit


class ProductFactory(factory.DjangoModelFactory):
    financial_unit = factory.SubFactory(FinancialUnitFactory)

    class Meta:
        model = models.Product


class BadgeFactory(factory.DjangoModelFactory):
    is_automatic = True
    check_interval = models.Badge.CI_DAY

    class Meta:
        model = models.Badge


class UserBadgeFactory(factory.DjangoModelFactory):

    class Meta:
        model = models.UserBadge


class HallOfFameGroupFactory(factory.DjangoModelFactory):
    month = dates.get_previous_month().month
    year = dates.get_previous_month().year

    class Meta:
        model = models.HallOfFameGroup


class MailTemplateFactory(factory.DjangoModelFactory):

    class Meta:
        model = models.MailTemplate


class EditablePageFactory(factory.DjangoModelFactory):
    order = fuzzy.FuzzyInteger(1)
    slug = fuzzy.FuzzyText(length=models.EditablePage.SLUG_MAXLEN)

    class Meta:
        model = models.EditablePage


class PageTranslationFactory(factory.DjangoModelFactory):
    title_ru = fuzzy.FuzzyText()
    text_ru = fuzzy.FuzzyText()

    class Meta:
        model = models.PageTranslation
