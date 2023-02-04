# coding=utf-8

import pytest

from tests import object_builder as ob


class TestProductNameBuilder(object):
    @pytest.mark.parametrize(
        ['product_product_name', 'expected_product_name_product_name'],
        [('Привет', 'Привет_1'),
         (u'Привет', u'Привет_1')],
        ids=['utf8', 'unicode']
    )
    def test_explicit_product(self, session, product_product_name, expected_product_name_product_name):
        product = ob.ProductBuilder(name=product_product_name)
        product_name = ob.ProductNameBuilder.construct(session, product=product)
        assert product_name.product_name == expected_product_name_product_name

    def test_no_product(self, session):
        product_name = ob.ProductNameBuilder.construct(session)
        assert product_name.product_name == 'Test Product_1'
