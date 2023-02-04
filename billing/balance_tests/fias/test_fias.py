# -*- coding: utf-8 -*-

from balance.mapper import Fias
from tests.balance_tests.fias.fias_common import create_fias_city, create_fias_row, REGION, DISTRICT


def test_oebs_postaddress(session):
    city, _ = create_fias_city(session, formal_name=u'Москва')
    result = Fias.oebs_post_address(fias=None, street_name=u'ул. Советская', suffix=u'д. 7')
    assert result == u'ул. Советская, д. 7'


def test_oebs_postaddress_full(session):
    region = create_fias_row(session, formal_name=u'Регион', short_name=u'р.', obj_level=REGION)
    district = create_fias_row(session, formal_name=u'Район', short_name=u'р.', obj_level=DISTRICT, parent_fias=region)
    city, _ = create_fias_city(session, formal_name=u'Москва', parent_fias=district)
    result = Fias.oebs_post_address(fias=city, postcode='123456', street_name=u'ул. Советская',
                                    suffix=u'д. 7')
    assert result == u'123456, Регион р., Район р., г. Москва, ул. Советская, д. 7'
