# coding: utf-8

"""
BALANCE-32285
"""

import pytest

from balance.publisher.mako_render import _inflect_tpl
from balance.xmlizer import getxmlizer
from balance.utils.xml2json import xml2json_auto

from tests import object_builder as ob


class TestPlugin(object):
    @pytest.fixture(name='inflector')
    def get_inflector_plugin(self, app):
        return getattr(app, 'inflector', None)

    def test_inflect(self, inflector):
        assert inflector is not None
        assert inflector.is_available()
        assert inflector.inflect(u'кофе') == u'кофе'
        assert inflector.inflect(u'слово') == u'слова'
        assert inflector.inflect(u'Сергей') == u'Сергея'
        assert inflector.inflect(u'генеральный', 'ins') == u'генеральным'
        assert inflector.inflect(u'директор', 'pl,acc') == u'директоров'

    def test_inflect_fio(self, inflector):
        assert inflector is not None
        assert inflector.is_available()
        assert inflector.inflect_fio(u'Соловьянов Виталий Александрович') == u'Соловьянова Виталия Александровича'
        assert inflector.inflect_fio(u'Иванов Иван Иванович') == u'Иванова Ивана Ивановича'


class TestInflectTpl(object):
    def test_inflect(self):
        inflect = _inflect_tpl('Inflector')
        assert inflect(u'кофе', 'gen') == u'кофе'
        assert inflect(u'слово', 'gen') == u'слова'
        assert inflect(u'Сергей', 'gen') == u'Сергея'

    def test_inflect_fio(self):
        inflect_fio = _inflect_tpl('FioInflector')
        assert inflect_fio(u'Соловьянов Виталий Александрович', 'gen') == u'Соловьянова Виталия Александровича'
        assert inflect_fio(u'Иванов Иван Иванович', 'gen') == u'Иванова Ивана Ивановича'


def test_person_xmlizer(session):
    person = ob.PersonBuilder.construct(session, signer_person_name=u'Иванов Иван Иванович',
                                        signer_position_name=u'Генеральный директор')
    person_xml = getxmlizer(person).xmlize()
    person_json = xml2json_auto(person_xml)
    assert person_json['inflector']['signer-person-name'] == u'Иванова Ивана Ивановича'
    assert person_json['inflector']['signer-position-name'] == u'Генерального директора'
