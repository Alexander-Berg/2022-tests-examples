# coding=utf-8

import unittest
import maps.carparks.tools.dap_snippets.lib.common as common


class Utf8MessageExceptionTests(unittest.TestCase):
    def test_no_message(self):
        e = common.Utf8MessageException()
        assert str(e) == 'None'
        assert repr(e) == "Utf8MessageException(None,)"

    def test_ascii_message(self):
        e = common.Utf8MessageException('Message')
        assert str(e) == 'Message'
        assert repr(e) == "Utf8MessageException('Message',)"

    def test_unicode_message(self):
        e = common.Utf8MessageException(u'Сообщение')

        assert str(e) == u'Сообщение'.encode('utf-8')
        assert ('Exception: {}'.format(e)
                == u'Exception: Сообщение'.encode('utf-8'))

        # repr of utf-8 string has an escaped byte representation
        assert repr(e) == "Utf8MessageException('\\xd0\\xa1\\xd0\\xbe\\xd0\\xbe" \
                          "\\xd0\\xb1\\xd1\\x89\\xd0\\xb5\\xd0\\xbd\\xd0\\xb8" \
                          "\\xd0\\xb5',)"

    def test_utf_8_message(self):
        e = common.Utf8MessageException(u'Сообщение'.encode('utf-8'))

        assert str(e) == u'Сообщение'.encode('utf-8')
        assert ('Exception: {}'.format(e)
                == u'Exception: Сообщение'.encode('utf-8'))

        assert repr(e) == "Utf8MessageException('\\xd0\\xa1\\xd0\\xbe\\xd0\\xbe" \
                          "\\xd0\\xb1\\xd1\\x89\\xd0\\xb5\\xd0\\xbd\\xd0\\xb8" \
                          "\\xd0\\xb5',)"
