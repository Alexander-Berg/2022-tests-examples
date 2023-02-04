# -*- coding: utf-8 -*-
import datetime as dt
import decimal

from sqlalchemy.orm.exc import FlushError
import pytest

from tests.base import BalanceTest
from tests.object_builder import (
    get_big_number,
    Getter,
    ProductBuilder,
    ProductGroupBuilder,
    ActivityBuilder
)
from balance.mapper import (
    Product,
    ProductUnit,
    ProductName,
    TaxPolicy,
    TaxPolicyPct,
    Tax,
    Price,
    Tag,
    Language,
    Country,
)
from balance import muzzle_util as ut
from balance import exc

D = decimal.Decimal


class TestProduct(BalanceTest):

    def setUp(self):
        super(TestProduct, self).setUp()

        self.product = ProductBuilder(
            name='Test languages',
            fullname='Fullname test languages'
        ).build(self.session).obj

        langs = self.session.query(Language).all()
        self.names = []

        for lang in langs:
            name = ProductName(
                lang=lang,
                product_id=self.product.id,
                product_name=self.product.name + lang.code,
                product_fullname=self.product.fullname + lang.code,
            )
            self.names.append(name)
            self.session.add(name)

        self.session.flush()

    def test_names(self):
        self.assertEqual(sorted(self.names), sorted(self.product.names))

    def test_name_by_lang_code(self):
        for name in self.product.names:
            self.assertEqual(name,
                self.product.name_by_lang_code(name.lang.code))

        self.assertRaises(ut.NO_PRODUCT_NAME_FOR_LANG,
                self.product.name_by_lang_code, 'test-lang')

        self.assertEqual(None,
                self.product.name_by_lang_code('test-lang', 0))
