# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import mock
from hamcrest.core.base_matcher import BaseMatcher
from lxml import etree
from yb_snout_api.resources.muzzle.bridge.utils.files import (
    get_xml_xslt_for_request,
    XSlTransformation,
)


def create_content_for_xml(cotent_xml='<content/>'):
    """Создаём содержимое тестового xml файла. """
    return etree.XML(
        (
            '<?xml version="1.0"?>'
            '<page>'
            '{}'
            '</page>'
        ).format(cotent_xml),
    )


def create_xslt_tree(template_str=''):
    """Создаём тестовый xslt."""
    xml = etree.ElementTree(
        etree.XML(
            (
                '<?xml version="1.0"?>'
                '<!DOCTYPE xsl:stylesheet SYSTEM "symbols.ent">'
                '<xsl:stylesheet version="1.0" '
                'xmlns:xsl="http://www.w3.org/1999/XSL/Transform" '
                'xmlns:x="http://www.yandex.ru/xscript" '
                'xmlns:snout="http://balance.yandex.ru/snout">'
                '<xsl:template match="/">'
                '{}'
                '</xsl:template>'
                '</xsl:stylesheet>'
            ).format(template_str),
        ),
    )
    xml.getroot().attrib["extension-element-prefixes"] = "x"
    return XSlTransformation(xml)


class XMLMatcher(BaseMatcher):
    """Матчер проверяет, что переданный элемент содержит дочерние теги.
    Дочерний тег описывается словарём, ключ - имя вложенного тега, значение - текст вложенного тега.
        Для xml элемента:
            <content_tag>
                <variable>
                    <name>n1</name>
                    <value>1</value>
                </variable>
                <variable>
                    <name>n2</name>
                    <value>2</value>
                </variable>
            </content_tag>
        Матчер:
        [{'name': 'n1', 'value': '1'}, {'name': 'n2', 'value': '2'}]
    """

    def __init__(self, tags_discriptions):
        """tags_discriptions: List с описанием тегов которые должны быть в переданном элементе xml.
       [{'Имя тега1': 'текст в теле тега1', 'Имя тега2': 'текст в теле тега2', ...}, {}, {}]
        """
        self.tags_discriptions = tags_discriptions

    def _matches(self, xml_element):
        xml_element = xml_element if xml_element is not None else []
        child_discriptions = [{cc.tag: cc.text for cc in child} for child in xml_element]
        self.not_found = [x for x in self.tags_discriptions if x not in child_discriptions]
        return len(self.not_found) == 0

    def describe_to(self, description):
        description.append_text('Child contains: {}. '.format(str(self.not_found)))


def get_file_loader_mock(content=None, xslt=None):
    """Возвращает контент менеджер для замоканного метода get_xml_xslt_for_request.
    метод будет возвращать указанный контент и xslt для запроса self.TEST_URL.
    """
    content = content if content is not None else create_content_for_xml()
    xslt = xslt if xslt is not None else create_xslt_tree()

    def get_xml_xslt_for_request_mock(subdir, file_name, load_xslt=True):
        if file_name == 'test_file.xml':
            return content, xslt
        else:
            return get_xml_xslt_for_request(subdir, file_name, load_xslt)

    return mock.patch(
        'yb_snout_api.resources.muzzle.bridge.logic.get_xml_xslt_for_request',
        new=get_xml_xslt_for_request_mock,
    )
