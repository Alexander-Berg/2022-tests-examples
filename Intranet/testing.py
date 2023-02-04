# -*- coding: utf-8 -*-

import httplib2
import urllib.parse
import unittest
from django.test.client import Client
import django.test
from at.api.yaru.atomgen import YAPI_NS

from .encoding import force_str

def http_request(method, uri, **kw):
    h = httplib2.Http()
    response, content = h.request(uri, method, **kw)
    return response, content

NSMAP = dict(
    atom = 'http://www.w3.org/2005/Atom',
    y = YAPI_NS,
    x = 'http://www.w3.org/1999/xhtml',
    )


class TestCase(django.test.TestCase):
    def setUp(self):
        self.client = Client(SERVER_NAME='yandex.net')

    def assertStatus(self, response, expected_status):
        self.assertEqual(response.status_code, expected_status)

    def abs_url(self, path):
        return 'http://yandex.net%s' % path

    def assertXPath(self, element, xpath, value=None, namespaces=None):
        namespaces = namespaces or NSMAP
        self.assertTrue(element is not None)
        found = element.xpath(xpath, namespaces=namespaces)
        self.assertNotEqual(found, None)
        if isinstance(found, bool):
            self.assertTrue(found)
        else:
            self.assertTrue(len(found) > 0)
            result = found[0]
            self.assertTrue(result)
            if value is not None:
                self.assertEqual(result, value)


class AtomEntryTestCase(TestCase):
    ''' Проверки соответствия нормам Atom Syndication Format
    и Atom Publishing Protocol.
    '''

    def assertAtomEntry(self, entry):
        ''' Проверка условий на atom:entry, согласно
        http://www.atomenabled.org/developers/syndication/atom-format-spec.php#element.entry
        '''

        X = lambda x: self.assertXPath(entry, x)

        # atom:entry elements MUST contain one or more atom:author elements,
        # unless the atom:entry contains an atom:source element that contains
        # an atom:author element or, in an Atom Feed Document, the atom:feed
        # element contains an atom:author element itself.
        #
        # Мы atom:source не поддерживаем (будем?), проверяем entry
        # по требованиям отдельного документа
        X('./atom:author/atom:name/text()')

        # atom:entry elements MUST NOT contain more than one atom:content element.
        X('count(./atom:content) <= 1')

        # atom:entry elements MUST contain exactly one atom:id element.
        X('./atom:id/text()')

        # atom:entry elements that contain no child atom:content element
        # MUST contain at least one atom:link element with a rel attribute value
        # of "alternate".
        #
        # ещё должен быть непустой атрибут href
        X('./atom:content or ./atom:link[@rel=alternate]/@href')

        #TODO: atom:entry elements MUST NOT contain more than one atom:link element
        # with a rel attribute value of "alternate" that has the same
        # combination of type and hreflang attribute values.
        #X('')

        # atom:entry elements MUST NOT contain more than one atom:published element.
        X('count(./atom:published) <= 1')

        # atom:entry elements MUST NOT contain more than one atom:rights element.
        X('count(./atom:rights) <= 1')

        # atom:entry elements MUST NOT contain more than one atom:source element.
        X('count(./atom:source) <= 1')

        #XXX: нужно ли?
        # atom:entry elements MUST contain an atom:summary element in
        # either of the following cases:
        # * the atom:entry contains an atom:content that has a "src" attribute
        #   (and is thus empty).
        # * the atom:entry contains content that is encoded in Base64; i.e.,
        #   the "type" attribute of atom:content is a MIME media type [MIMEREG],
        #   but is not an XML media type [RFC3023], does not begin with "text/",
        #   and does not end with "/xml" or "+xml".
        #X('')

        # atom:entry elements MUST NOT contain more than one atom:summary element.
        X('count(./atom:summary) <= 1')

        # atom:entry elements MUST contain exactly one atom:title element.
        X('count(./atom:title) = 1')

        # atom:entry elements MUST contain exactly one atom:updated element.
        X('count(./atom:updated) = 1')

class ExternalHttpTestCase(unittest.TestCase):
    ''' Внешний тест (тест внешних зависимостей).

    Поддерживает http-запросы (self.{GET,POST}) к внешним сервисам.
    '''

    HOST = 'http://transvaal.yandex.ru:40000'

    def GET(self, url):
        scheme, netloc, path, _, _ = urllib.parse.urlsplit(url)
        request_url = (scheme and netloc) and url or urllib.parse.urljoin(self.HOST, url)
        return http_request('GET', request_url)

    def POST(self, url, **kw):
        scheme, netloc, path, _, _ = urllib.parse.urlsplit(url)
        request_url = (scheme and netloc) and url or urllib.parse.urljoin(self.HOST, url)
        return http_request('POST', request_url, **kw)

    def assertStatus(self, response, status):
        self.assertEqual(response.status, status)

    def assertContains(self, what, content):
        self.assertTrue(what in content)


#TODO: вынести в отдельный модуль, к тестам не имеет отношения
def encode_multipart_formdata(fields, files):
    """
    fields is a sequence of (name, value) elements for regular form fields.
    files is a sequence of (name, filename, value) elements for data to be uploaded as files
    Return (content_type, body) ready for httplib.HTTP instance
    """
    BOUNDARY = '----------ThIs_Is_tHe_bouNdaRY_$'
    CRLF = '\r\n'
    L = []
    for (key, value) in fields:
        L.append('--' + BOUNDARY)
        L.append('Content-Disposition: form-data; name="%s"' % key)
        if isinstance(value, str):
            L.append('Content-Type: text/plain; charset=utf-8')
        L.append('')
        L.append(force_str(value))
    for (key, filename, value) in files:
        L.append('--' + BOUNDARY)
        L.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
        L.append('Content-Type: text/html; charset=utf-8')
        L.append('')
        L.append(value)
    L.append('--' + BOUNDARY + '--')
    L.append('')
    body = CRLF.join(L)
    content_type = 'multipart/form-data; boundary=%s' % BOUNDARY
    return content_type, body
