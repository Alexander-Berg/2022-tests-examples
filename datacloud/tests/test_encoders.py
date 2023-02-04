import sys
import binascii
import unittest
from datacloud.dev_utils.id_value import encoders

VERSION = sys.version_info[0]


class TestEncoders(unittest.TestCase):
    def test_encode_phone(self):
        self.assertEqual(encoders.encode_phone('79876543210'), 18285589332495197703)
        self.assertEqual(encoders.encode_phone('+79876543210'), 18285589332495197703)
        self.assertEqual(encoders.encode_phone('42'), 14676716070737729163)
        with self.assertRaises(TypeError):
            encoders.encode_phone(42)
        with self.assertRaises(TypeError):
            encoders.encode_phone(None)
        with self.assertRaises(ValueError):
            encoders.encode_phone('')

    def test_encode_email(self):
        self.assertEqual(encoders.encode_email('sample@ya.ru'), 12829478727818662638)
        self.assertIsNone(encoders.encode_email('random-string'))
        self.assertIsNone(encoders.encode_email(''))

    def test_encode_yuid(self):
        self.assertEqual(encoders.encode_yuid('987654321'), 17686369824986535547)
        self.assertEqual(encoders.encode_yuid(987654321), 17686369824986535547)
        self.assertEqual(encoders.encode_yuid(''), 3816898026917488607)

    def test_encode_cid(self):
        self.assertEqual(encoders.encode_cid('987654321'), 5535130788024384385)
        with self.assertRaises(AttributeError):
            encoders.encode_cid(987654321)
        self.assertEqual(encoders.encode_cid(''), 7203772011789518145)

    def test_partially_encode_cookie(self):
        self.assertEqual(encoders.partially_encode_cookie('test-vendor', 'aa-bb-cc'), '7253c8caecf14c0e2ee90f5d7bea82bb')
        self.assertEqual(encoders.partially_encode_cookie('', 'aa-bb-cc'), '3c89d7a9ecadd21806a5a2bb04716e8b')
        self.assertEqual(encoders.partially_encode_cookie('test-vendor', ''), 'ea8b19e8b5203f1e445b6cfb75097e6b')

    def test_encode_cookie(self):
        self.assertEqual(encoders.encode_cookie('test-vendor', 'aa-bb-cc'), 10888523802438453143)
        self.assertEqual(encoders.encode_cookie('', 'aa-bb-cc'), 16779493652197831954)
        self.assertEqual(encoders.encode_cookie('test-vendor', ''), 13846670503027176723)

    def test_encode_id_value(self):
        # 5D41402ABC4B2A76B9719D911017C592 == md5('hello')
        self.assertEqual(encoders.encode_id_value('5D41402ABC4B2A76B9719D911017C592'), 12420065638740975035)
        if VERSION == 2:
            with self.assertRaises(TypeError):
                # First character in hash was removed
                self.assertEqual(encoders.encode_id_value('D41402ABC4B2A76B9719D911017C592'), 12420065638740975035)
        if VERSION == 3:
            with self.assertRaises(binascii.Error):
                self.assertEqual(encoders.encode_id_value('D41402ABC4B2A76B9719D911017C592'), 12420065638740975035)
        with self.assertRaises(IndexError):
            self.assertEqual(encoders.encode_id_value(''), 12420065638740975035)
