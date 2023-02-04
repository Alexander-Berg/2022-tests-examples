# -*- coding: utf-8 -*-
from django.db import models
from django.test.utils import override_settings
from events.common_app.makers.published import (
    make_for_current_site_type,
    make_not_for_current_site_type,
)

from events.common_app.mixins import PublishedMixin
from events.common_app.helpers import TestConditionBase


class CarModel(PublishedMixin, models.Model):
    name = models.CharField(max_length=100, blank=True, null=True)

    class Meta:
        app_label = 'common_app'


class TestPublishedIsForCurrentSiteType(TestConditionBase):
    model = CarModel
    queryset_method_name = 'filter_for_current_site_type'
    instance_property_name = 'is_for_current_site_type'

    def create_instance(self):
        return CarModel.objects.create()

    @override_settings(IS_INTERNAL_SITE=True)
    def test_should_be_not_published_for__internal_site__if_not_published_internal(self):
        self.instance.is_published_internal = False
        self.instance.save()

        msg = 'на внутреннем сайте модель с is_published_internal=False не должна быть опубликована'
        self.assertConditionFalse(msg=msg)

    @override_settings(IS_INTERNAL_SITE=True)
    def test_should_be_published_for__internal_site__if_published_internal(self):
        self.instance.is_published_internal = True
        self.instance.save()

        msg = 'на внутреннем сайте модель с is_published_internal=True должна быть опубликована'
        self.assertConditionTrue(msg=msg)

    @override_settings(IS_INTERNAL_SITE=False)
    def test_should_be_not_published_for__external_site__if_not_published_external(self):
        self.instance.is_published_external = False
        self.instance.save()

        msg = 'на внешнем сайте модель с is_published_external=False не должна быть опубликована'
        self.assertConditionFalse(msg=msg)

    @override_settings(IS_INTERNAL_SITE=False)
    def test_should_be_published_for__external_site__if_published_external(self):
        self.instance.is_published_external = True
        self.instance.save()

        msg = 'на внешнем сайте модель с is_published_external=True должна быть опубликована'
        self.assertConditionTrue(msg=msg)

    @override_settings(IS_INTERNAL_SITE=True)
    def test_make_for_current_site_type__for_internal_site(self):
        self.instance.is_published_internal = False
        self.instance.save()

        make_for_current_site_type(self.instance)  # BANG!

        msg = ('make_for_current_site_type должен делать инстанс модели для текущего типа сайта, '
               'если текущий сайт - внутренний')
        self.assertConditionTrue(msg=msg)

    @override_settings(IS_INTERNAL_SITE=False)
    def test_make_for_current_site_type__for_external_site(self):
        self.instance.is_published_external = False
        self.instance.save()

        make_for_current_site_type(self.instance)  # BANG!

        msg = ('make_for_current_site_type должен делать инстанс модели для текущего типа сайта, '
               'если текущий сайт - внешний')
        self.assertConditionTrue(msg=msg)

    @override_settings(IS_INTERNAL_SITE=True)
    def test_make_not_for_current_site_type__for_internal_site(self):
        self.instance.is_published_internal = True
        self.instance.save()

        make_not_for_current_site_type(self.instance)  # BANG!

        msg = ('make_not_for_current_site_type должен делать инстанс модели не для текущего типа сайта, '
               'если текущий сайт - внутренний')
        self.assertConditionFalse(msg=msg)

    @override_settings(IS_INTERNAL_SITE=False)
    def test_make_not_for_current_site_type__for_external_site(self):
        self.instance.is_published_external = True
        self.instance.save()

        make_not_for_current_site_type(self.instance)  # BANG!

        msg = ('make_not_for_current_site_type должен делать инстанс модели не для текущего типа сайта, '
               'если текущий сайт - внешний')
        self.assertConditionFalse(msg=msg)
