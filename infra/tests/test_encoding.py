# -*- coding: utf-8 -*-
from __future__ import absolute_import

from agent.encoding import safe_unicode, safe_str


class TestSafeUnicode(object):

    def test_when_str(self):
        assert safe_unicode('foo') == u'foo'
        assert isinstance(safe_unicode('foo'), unicode)
        assert safe_unicode('бар') == u'бар'
        assert isinstance(safe_unicode('бар'), unicode)

    def test_when_unicode(self):
        assert safe_unicode(u'бар') == u'бар'
        assert isinstance(safe_unicode(u'бар'), unicode)

    def test_when_encoding_utf8(self):
        s = 'The quiæk fåx jømps øver the lazy dåg'
        res = safe_unicode(s)
        assert isinstance(res, unicode)

    def test_when_containing_high_chars(self):
        s = 'The quiæk fåx jømps øver the lazy dåg'
        res = safe_unicode(s, encoding='ascii')
        assert isinstance(res, unicode)
        # because we should replace chars by default
        assert len(s) == len(res)

    def test_when_not_string(self):
        o = object()
        assert safe_unicode(o) == repr(o)

    def test_when_unrepresentable(self):

        class O(object):  # noqa: E742

            def __repr__(self):
                raise KeyError('foo')

        assert '<Unrepresentable' in safe_unicode(O())
        assert isinstance(safe_unicode(O()), unicode)


class TestSafeStr(object):

    def test_when_str(self):
        assert safe_str('foo') == 'foo'
        assert isinstance(safe_str('foo'), str)
        assert safe_str('бар') == 'бар'
        assert isinstance(safe_str('бар'), str)

    def test_when_unicode(self):
        assert safe_str(u'бар') == 'бар'
        assert isinstance(safe_str(u'бар'), str)

    def test_when_encoding_utf8(self):
        s = 'The quiæk fåx jømps øver the lazy dåg'
        res = safe_str(s)
        assert isinstance(res, str)

    def test_when_containing_high_chars(self):
        s = 'The quiæk fåx jømps øver the lazy dåg'
        res = safe_str(s, encoding='ascii')
        assert isinstance(res, str)
        # because we should replace chars by default
        assert len(s) == len(res)

    def test_when_not_string(self):
        o = object()
        assert safe_str(o) == repr(o)

    def test_when_unrepresentable(self):

        class O(object):  # noqa: E742

            def __repr__(self):
                raise KeyError('foo')

        assert '<Unrepresentable' in safe_str(O())
        assert isinstance(safe_str(O()), str)

    def test_when_support_str_protocol(self):

        class O(object):  # noqa: E742

            def __str__(self):
                return 'бар'

        assert 'бар' == safe_str(O())
        assert isinstance(safe_str(O()), str)

        assert u'бар' == safe_unicode(O())
        assert isinstance(safe_unicode(O()), unicode)
