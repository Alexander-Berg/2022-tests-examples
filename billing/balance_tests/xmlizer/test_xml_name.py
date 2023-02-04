# -*- coding: utf-8 -*-

import datetime as dt

from balance.mapper import DomainObject
from balance.xmlizer import domain2xml, xml_name
from tests.object_builder import *


class ABC(DomainObject):
    def __init__(self):
        self.id = 100
        self.name = u'Педро'
        self.email = None
        self.x = 123
        self.dt = dt.datetime(2007, 4, 10, 11, 37, 05)


class TestDomainXMLizer(object):
    def test_spec_name(self):
        assert ut.is_spec_name('_a')
        assert ut.is_spec_name('a_')
        assert ut.is_spec_name('_a_')
        assert ut.is_spec_name('_')
        assert ut.is_spec_name('a')
        assert not ut.is_spec_name('aa')
        assert not ut.is_spec_name('a_a')

    def test_xml_name(self):
        assert 'ab-c' == xml_name('Ab_c')

    def test_domain2xml(self):
        root = domain2xml(ABC())
        assert 'abc' == root.tag
        assert u'100' == root.find('id').text
        assert u'Педро' == root.find('name').text
        assert '2007-04-10T11:37:05' == root.find('dt').text
        assert 'XEP' == root.find('name').get('is_null', 'XEP')
        assert root.find('email').text is None
        assert u'1' == root.find('email').get('is_null', 'XEP')
        assert not root.find('fax')
        assert not root.find('x')
        assert not root.find('contract_service')

    def test_xml_name_1(self):
        assert xml_name("Camel_Cased4ThingQ_5") == 'camel-cased4-thing-q-5'

        assert xml_name("Camel_CasedThingQ5") == 'camel-cased-thing-q5'

        assert xml_name("camel_cased_thing") == 'camel-cased-thing'

        assert xml_name("Abc") == 'abc'

        assert xml_name("ABC") == 'abc'
