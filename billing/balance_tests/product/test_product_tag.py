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


class TestProductAttrs(BalanceTest):
    def _create_prod(self, tags):
        prod = ProductBuilder().build(self.session).obj
        for tag in tags:
            prod.tags.append(Tag(tag, prod))
        self.session.flush()
        return prod

    def test_create_tag(self):
        prod = ProductBuilder().build(self.session).obj
        tag = 'test1'

        tag_mapper = Tag(tag, prod)
        self.session.add(tag_mapper)
        self.session.flush()
        self.assertItemsEqual(prod.tags, [tag_mapper])
        self.assertEqual(prod.tags[0], tag_mapper)

        tag_mapper2 = Tag(tag, prod)
        self.session.add(tag_mapper2)
        with self.assertRaises(FlushError):
            self.session.flush()

    def test_tags(self):
        prod1 = self._create_prod(['hash1', 'hash2'])
        prod2 = self._create_prod(['hash2', 'hash3'])
        self.assertListEqual(sorted(Product.from_tags(self.session, tags=['hash1'])), [prod1])
        self.assertListEqual(sorted(Product.from_tags(self.session, tags=['hash2'])), sorted([prod1, prod2]))
        self.assertListEqual(Product.from_tags(self.session, tags=['hash3']), [prod2])
        self.assertListEqual(Product.from_tags(self.session, tags=['hash4']), [])