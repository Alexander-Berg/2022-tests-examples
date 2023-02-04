# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from django.core.urlresolvers import reverse

from app.tests import views as testcase
from core import models
from core.tests import factory


class CreateProductTest(testcase.MessagesMixin, testcase.YauthAdminTestCase):
    url_name = 'product:create'
    
    def setUp(self):
        self.unit = factory.FinancialUnitFactory.create()
        self.form_data = {
            'title': 'title',
            'financial_unit': self.unit.pk,
        }
        super(CreateProductTest, self).setUp()

    def test_product_created(self):
        response = self._post_request()
        assert response.status_code == 302
        product = models.Product.objects.last()
        assert product.financial_unit == self.unit
